# Create: Enchantment Industry 2.5.4b 源码技术分析

> 目标读者：要写「北极星星球」Create 附属（新增**应力源 / 应力消耗 / 传动元件**）的我们。
> 分析对象：Create: Enchantment Industry（下称 CEI）2.5.4b，对应 Create 6.0.10 / MC 1.21.1 / NeoForge 21.1.228+。
> 源码形态：`.research/src/cei` 是**解包后的 sources jar**（含 `assets/` `data/` `META-INF/` `plus/`），不是 git 仓库，因此没有 `build.gradle`、没有 datagen 运行配置；所有"构建/依赖"结论只能从 `META-INF/neoforge.mods.toml` 与代码里的 import 反推。
> 行号以本次 `read` 输出为准。

## 0. 路径缩写与证据边界

| 缩写 | 实际路径 |
|---|---|
| `CEI/` | `.research/src/cei/plus/dragons/createenchantmentindustry/` |
| `CR/` | `.research/src/create/com/simibubi/create/` |
| `MODS.TOML` | `.research/src/cei/META-INF/neoforge.mods.toml` |

三条硬约束（阅读时请记住）：

1. **CEI 有一个必装的"上游库" `create_dragons_plus`（CDP）**。`CDPRegistrate`、`StressConfig`、`BlazeBlockEntity`、`ConfigurableFluidTank`、`FluidTankBehaviour`、`AdvancementBehaviour`、`CDPFluids`、`RecipeTypeInfo`、`IntegrationIngredient`、`@CodeReference` 全部来自 CDP，**CDP 源码不在 `.research/src` 里**。凡涉及 CDP 内部实现的地方我会标 ⚠，并在必要处附上 GitHub 原文作为旁证（版本可能与 1.11.9 有差，注意核对）。
2. CEI 的**唯一应力消耗机器是机械磨石与黄铜书架**；Blaze Enchanter / Forger / Printer **完全不吃应力**（吃经验流体）。这点决定了它对我们"应力源"参考价值有限，但它对"应力消耗+传动件"的示范非常完整。
3. CEI 的 `api/` 包只有两个类（`CEIRegistries`、`PrintingBehaviourProvider`），说明它的第一优先级不是做 API 开放，而是**做 Create 的深度整合**；我们要抄的是它的整合手法，不是它的 API 设计。

---

## 1. CEI 的模组骨架

### 1.1 包结构（顶层划分）

```
plus/dragons/createenchantmentindustry/
├── api/registry/          CEIRegistries（对外暴露的自定义 registry key）
├── client/                CEIClient(@Mod dist=CLIENT)、model/(PartialModels)、ponder/
├── common/                CEICommon(@Mod)、registry/、kinetics/、processing/、fluids/
├── config/                CEIConfig + 9 个 ConfigBase 子类
├── data/                  CEIData(datagen @Mod)、CEIRecipeProvider、CEIGenerateEntriesProvider
├── integration/           ModIntegration(枚举) + 5 个独立集成分区
│   ├── apotheosis/        改版后（Apotheosis 8.x，独立 mod id）
│   ├── apothic_enchanting/ 拆出版（Apothic Enchanting 1.6+，独立 mod id）
│   ├── sable/             Sable（子世界/物理 contraption）
│   ├── sable_apotheosis/  两者同装才生效
│   ├── touhou_little_maid/
│   └── jei/
├── mixin/                 11 个主 mixin + accessor
└── util/
```

**可复用点**：`client/` 与 `data/` 各用一个独立 `@Mod` 类（`CEIClient` / `CEIData`），而不是在主类里判断 `Dist`。这样 1.21 的 NeoForge 会在客户端/数据生成时才实例化它们，避免服务端加载 client 类（`CEIClient.java:30`、`CEIData.java:32`）。

### 1.2 `@Mod` 主类与初始化顺序（最关键的一节）

```java
// CEI/common/CEICommon.java:46-75
@Mod(CEICommon.ID)
public class CEICommon {
    public static final String ID = "create_enchantment_industry";
    public static final CDPRegistrate REGISTRATE = new CDPRegistrate(ID)
            .setTooltipModifier(item -> new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                    .andThen(TooltipModifier.mapNull(KineticStats.create(item))));

    public CEICommon(IEventBus modBus, ModContainer modContainer) {
        REGISTRATE.registerEventListeners(modBus);          // 1
        CEIFluids.register(modBus);                          // 2
        if (ModIntegration.APOTHIC_ENCHANTING.enabled() || ModIntegration.APOTHEOSIS.enabled()) {
            CEIAFluids.register(modBus);                     // 3 (条件)
            CEIACreativeModeTabs.register(modBus);           // 4 (条件)
        }
        CEIBlocks.register(modBus);                          // 5
        CEIBlockEntities.register(modBus);                   // 6
        CEIItems.register(modBus);                           // 7
        CEICreativeModeTabs.register(modBus);                // 8
        CEIRecipes.register(modBus);                         // 9
        CEIEnchantments.register(modBus);                    // 10
        CEIArmInterationPoints.register(modBus);             // 11
        CEIDataMaps.register(modBus);                        // 12
        CEIStats.register(modBus);                           // 13
        CEIMountedStorageTypes.register(modBus);             // 14
        CEIItemAttributes.register(modBus);                  // 15
        CEIPrintingBehaviours.register(modBus);              // 16
        modBus.register(this);                               // 17
        modBus.register(new CEIConfig(modContainer));        // 18  <-- 配置最后
        NeoForge.EVENT_BUS.addListener(CEICommon::serverStarted); // 19
    }
```

这张顺序表里有两处是**必须照抄的**：

| 顺序特征 | 证据 | 为什么必须 |
|---|---|---|
| `REGISTRATE.registerEventListeners(modBus)` 永远第一 | `CEICommon.java:54` | Registrate 的所有 `BlockEntry`/`ItemEntry` 静态字段注册回调都挂在这条总线上；晚于任何一次 `DeferredRegister` 也没事，但必须在本类任何静态初始化用到它之前（静态字段在类加载时就会读 `REGISTRATE`）。 |
| **`CEIConfig` 放在所有 `register(modBus)` 之后** | `CEICommon.java:60-73` | 应力值走"配置先行登记默认值，再注册 provider"的模型：`CEIBlocks` 的静态初始化里调 `CEIConfig.stress().setImpact(4.0)`（`CEIBlocks.java:67`），把默认值塞进 `StressConfig.defaultImpacts`；而 `CEIConfig` 构造时才 `registerAll(builder)` 真正 `define` 出配置项并 `BlockStressValues.IMPACTS.registerProvider(...)`。顺序颠倒 → 配置里不会出现这两台机器的应力项，`getImpact` 返回 null，机器按 0 应力跑。 |
| 空体 `register(modBus)` 方法存在只为"强制类初始化" | `CEIBlocks.java:187`、`CEIArmInterationPoints.java:41`、`CEIMountedStorageTypes.java:33` | 这些类的 body 里 `register` 是空方法，真正干活的是静态字段。调用它的唯一意义是**保证类加载时机**（以及把 `modBus.register(CEIBlockEntities.class)` 这类事件订阅排进总线）。想按依赖顺序初始化时，这是最省事的写法，但**极其隐晦**——我们自己写时建议改成 `CEIBlocks.init()` 之类命名并加注释。 |

事件订阅有三个层级，别混：

| 总线 | 用法 | 证据 |
|---|---|---|
| mod bus（`modBus`） | `modBus.register(this)` 收 `FMLCommonSetupEvent` / `RegisterEvent` | `CEICommon.java:72,77-90` |
| mod bus（静态类） | `modBus.register(CEIBlockEntities.class)` → 收 `RegisterCapabilitiesEvent` | `CEIBlockEntities.java:94-114` |
| NeoForge 游戏总线 | `NeoForge.EVENT_BUS.addListener(CEICommon::serverStarted)` | `CEICommon.java:74,80-82` |

`serverStarted` 里做的事很典型：**启动后检查玩家/整合包是否还在用已弃用的 data map，并打一次 warn**（`CEICommon.java:80-82` → `EnchantmentProcessingRules.warnLegacyDataMaps`，实现见 `CEI/processing/EnchantmentProcessingRules.java:41-60`）。这是一种"我们改过数据结构，但不想硬崩"的成熟做法，值得抄。

### 1.3 Registrate 实例：它没有自建，用的是 CDP 包装

```java
// CEI/common/CEICommon.java:49-51
public static final CDPRegistrate REGISTRATE = new CDPRegistrate(ID)
        .setTooltipModifier(item -> new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                .andThen(TooltipModifier.mapNull(KineticStats.create(item))));
```

对照 Create 自己的做法：

```java
// CR/Create.java:91-96
private static final CreateRegistrate REGISTRATE = CreateRegistrate.create(ID)
    .defaultCreativeTab((ResourceKey<CreativeModeTab>) null)
    .setTooltipModifierFactory(item ->
        new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
            .andThen(TooltipModifier.mapNull(KineticStats.create(item)))
    );
```

结论：

- **Create 原生就有 `com.simibubi.create.foundation.data.CreateRegistrate`**（`CR/foundation/data/CreateRegistrate.java:63`，工厂 `create(String modid)` 在 `:74`）。做附属**直接用 `CreateRegistrate.create(MODID)` 即可**，不需要 CDP。
- `CreateRegistrate` 已经替我们封装了 Create 的容器型 registry builder：
  - `mountedItemStorage(name, supplier)` `:158`
  - `mountedFluidStorage(name, supplier)` `:164`
  - `displaySource(...)` `:170` / `displayTarget(...)` `:176`
  - `blockEntity(name, factory)` `:130`
  - `registerEventListeners(bus)` `:108`
- CEI 额外用到的 `REGISTRATE.armInteractionPoint(name, factory)`（`CEI/common/registry/CEIArmInterationPoints.java:31-33`）**在 Create 原生 `CreateRegistrate` 里不存在**，是 CDP 加的糖。我们要自己做机械臂交互点，就照 `mountedFluidStorage` 的写法：

```java
// 模板：CR/foundation/data/CreateRegistrate.java:164-168
public <T extends MountedFluidStorageType<?>> SimpleBuilder<MountedFluidStorageType<?>, T, CreateRegistrate> mountedFluidStorage(String name, Supplier<T> supplier) {
    return this.entry(name, callback -> new SimpleBuilder<>(
        this, this, name, callback, CreateRegistries.MOUNTED_FLUID_STORAGE_TYPE, supplier
    ).byBlock(MountedFluidStorageType.REGISTRY));
}
```

把 `CreateRegistries.MOUNTED_FLUID_STORAGE_TYPE` 换成 `CreateRegistries.ARM_INTERACTION_POINT_TYPE`、`MountedFluidStorageType.REGISTRY` 换成 `ArmInteractionPointType` 的 block→type 映射即可（Create 的 `ArmInteractionPointType` 由各 Type 自己的 `canCreatePoint` 判定，因此不需要 `.byBlock`，这也是 CDP 那层糖存在的原因）。⚠ CDP `armInteractionPoint` 的内部实现未在源码内验证；Create 侧常量证据为 `CR/api/registry/CreateBuiltInRegistries.java:34`、`CR/content/kinetics/mechanicalArm/ArmInteractionPointType.java`。

- `setTooltipModifier`（CDP）vs `setTooltipModifierFactory`（Create）：同名语义，前者是 CDP 的简写。**我们自己用 Create 的就写 `setTooltipModifierFactory`**。

### 1.4 客户端与服务端的拆分

```java
// CEI/client/CEIClient.java:30-45
@Mod(value = CEICommon.ID, dist = Dist.CLIENT)
public class CEIClient {
    public CEIClient(IEventBus modBus) {
        CEIPartialModels.register();          // 见下方注释：必须在构造期
        modBus.addListener(CEIClient::setup);
    }
    public static void setup(final FMLClientSetupEvent event) {
        PonderIndex.addPlugin(new CEIPonderPlugin());
    }
}
```

CEI 在此处留了一段**极其值钱的注释**（`CEIClient.java:33-37`）：

> `CEIPartialModels` must be registered here, or when `PartialModelEventHandler#onRegisterAdditional` triggered, `PartialModel.ALL` won't include all partial model in 'some cases'. `AllPartialModels#ini` does not do this since `AllPartialModels` is already triggered at `AllBlocks.TRACK`. Issue: Creators-of-Create/Create#8259

含义：**Flywheel `PartialModel` 要在 mod 构造期（`@Mod` 构造函数内）就注册**，否则部分情况下不会进 `PartialModel.ALL`，表现为"模型随机不渲染"。我们自己加 `PartialModel` 时照抄：在 `@Mod(dist=CLIENT)` 构造器里 `MyPartialModels.register()`，方法体可以是空的（`CEI/client/model/CEIPartialModels.java:34`）—— 空方法的意义只是触发类静态初始化。同样的注释在 `CEI/integration/apothic_enchanting/common/CEIACommon.java:99-104` 又重复了一遍，说明这是踩过的坑。

`PartialModel` 定义本身只有三行一个（`CEIPartialModels.java:25-38`）：

```java
public static final PartialModel MECHANICAL_GRINDSTONE = block("mechanical_grindstone");
private static PartialModel block(String path) {
    return PartialModel.of(CEICommon.asResource("block/" + path));
}
```

### 1.5 Datagen（Create 6.0.6 重构后）

CEI 的 datagen 入口是**独立 `@Mod`**，并且用 `DatagenModLoader.isRunningDataGen()` 做门禁：

```java
// CEI/data/CEIData.java:32-59
@Mod(CEICommon.ID)
public class CEIData {
    public CEIData(IEventBus modBus) {
        if (!DatagenModLoader.isRunningDataGen()) return;
        REGISTRATE.registerBuiltinLocalization("interface");
        REGISTRATE.registerForeignLocalization();
        REGISTRATE.registerPonderLocalization(CEIPonderPlugin::new);
        REGISTRATE.registerExtraLocalization(CEIAdvancements::provideLang);
        modBus.register(this);
    }
    @SubscribeEvent
    public void generate(final GatherDataEvent event) {
        var generator = event.getGenerator();
        ...
        CEIGenerateEntriesProvider generatedEntriesProvider = new CEIGenerateEntriesProvider(output, lookupProvider);
        lookupProvider = generatedEntriesProvider.getRegistryProvider();
        generator.addProvider(event.includeServer(), generatedEntriesProvider);
        generator.addProvider(server, new CEIRecipeProvider(output, lookupProvider));
        generator.addProvider(server, new CEIAdvancements(output, lookupProvider));
    }
}
```

要点：

1. **不手写 `PackMetadataGenerator`/`BlockStateProvider` 全家桶**——方块模型/状态/掉落/语言都交给 Registrate 的 builder（`CEIBlocks.java` 里 `.blockstate(...)`、`.lang(...)`、`.loot(...)`），datagen 只挂两个 `RecipeProvider` 和一个 registry entries provider。
2. **Ponder 文本也跟着 datagen 走**：`REGISTRATE.registerPonderLocalization(CEIPonderPlugin::new)`（`CEIData.java:39`），所以 ponder 场景里写的英文句子会自动进 `en_us.json`。
3. **data map 的 provider 不走 `GatherDataEvent`**，走 Registrate 的 provider 钩子：`REGISTRATE.addDataGenerator(ProviderType.DATA_MAP, CEIDataMaps::generate)`（`CEI/common/registry/CEIDataMaps.java:117`）。
4. 注意 `CEIData` 的构造器参数没有 `ModContainer`——纯 data 类 mod 不需要配置。

Create 6.0.6 之后自带的 recipe builder 工具（`pressing/filling/emptying/compacting/cutting/crushing`、`manualApplication(...)`）在 CEI 里通过静态导入 CDP 的 `CreateRecipeBuilders` 使用（`CEI/data/CEIRecipeProvider.java:28-30`）。**这些在 Create 原生里叫 `com.simibubi.create.foundation.data.recipe.CreateRecipeBuilders`**（CDP 只是再导出）。⚠ CDP 的 `IntegrationIngredient`（`:220`）是 CDP 独有，用于"引用一个可能不存在的 mod 的物品"。

### 1.6 依赖声明与 mixin 装配

```toml
# MODS.TOML（节选）
[[mixins]] config="create_enchantment_industry.mixins.json"
[[mixins]] config="create_enchantment_industry.apothic_enchanting.mixins.json"
[[mixins]] config="create_enchantment_industry.apotheosis.mixins.json"
[[mixins]] config="create_enchantment_industry.sable.mixins.json"
[[mixins]] config="create_enchantment_industry.touhou_little_maid.mixins.json"

#[[accessTransformers]]
    #file="META-INF/accesstransformer.cfg"

[[dependencies.create_enchantment_industry]] modId="create"                type="required" versionRange="[6.0.10,)"
[[dependencies.create_enchantment_industry]] modId="create_dragons_plus"   type="required" versionRange="[1.11.9,)" ordering="AFTER"
[[dependencies.create_enchantment_industry]] modId="apothic_enchanting"    type="optional" versionRange="[1.6.1,)"  ordering="AFTER"
[[dependencies.create_enchantment_industry]] modId="apotheosis"            type="optional" versionRange="[8.7.0,)"  ordering="AFTER"
[[dependencies.create_enchantment_industry]] modId="sable"                 type="optional" versionRange="[2.0.3,)"  ordering="AFTER"
[[dependencies.create_enchantment_industry]] modId="touhou_little_maid"    type="optional" versionRange="[1.2.0,)" ordering="AFTER"
```

三个可抄的点：

- **每个可选集成分一个 mixin config**。理由：mixin config 一旦加载，其中任何一条注入失败（`defaultRequire: 1`，见 `create_enchantment_industry.mixins.json`）都会硬崩；把"只有装了 Apotheosis 才有意义"的注入单独隔离，能在无该 mod 时**整个 config 不加载**，从根上消除注入失败风险。
- `ordering="AFTER"` 对 Create 与 CDP 都写了——**附属改 Create 的东西必须排在 Create 之后**，否则 mixin 优先级争夺会出现"我们的注入先跑、Create 状态还没初始化"。
- AT 段被注释但 `META-INF/accesstransformer.cfg` 文件在包内（内容见下）。NeoForge 对 `META-INF/accesstransformer.cfg` 这个**默认路径会自动识别**，所以显式声明可省。⚠ 这条"默认路径自动生效"的行为建议我们在自己的项目里实测一次再依赖它；不确定时还是显式声明最稳。

```cfg
# META-INF/accesstransformer.cfg
public net.minecraft.world.entity.ExperienceOrb count
public net.minecraft.world.entity.ExperienceOrb tryMergeToExisting(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)Z
public net.minecraft.world.entity.ExperienceOrb repairPlayerItems(Lnet/minecraft/server/level/ServerPlayer;I)I
public-f net.minecraft.data.recipes.RecipeProvider getName()Ljava/lang/String;
```

这四条 AT 对应的正是 CEI 反复要的行为：**经验球计数/合并**、**用经验修玩家装备（Mending 语义）**、datagen 里读 recipe provider 名。

---

## 2. 逐个机械元件剖析

### 2.0 总表（先看这张表，后面逐条展开）

| 机器 | BlockEntity 父类 | 应力消耗/源 | 应力值来源 | 速度依赖 | 关键 Create API | 文件:行 |
|---|---|---|---|---|---|---|
| Mechanical Grindstone（磨石轮） | **直接用 `KineticBlockEntity`**（不写子类） | 消耗 4.0 SU@1RPM | `CEIConfig.stress().setImpact(4.0)` → CDP `StressConfig` → `BlockStressValues.IMPACTS` provider | 是：`|speed| >= 30` 才可交互，且只做粒子/音效 | `RotatedPillarKineticBlock`、`IBE`、`SingleAxisRotatingVisual`、`KineticBlockEntityRenderer` | `CEI/common/kinetics/grindstone/MechanicalGrindstoneBlock.java:54`、`CEI/common/registry/CEIBlocks.java:64-72`、`CEI/common/registry/CEIBlockEntities.java:49-54` |
| Grindstone Drain（磨石排水口） | `KineticBlockEntity` | 消耗 4.0 SU@1RPM | 同上（`CEIBlocks.java:76`） | 是：`getRelativeSpeed()` 只认与上方磨石**反向**的转速，`clamp(speed/24,1,128)` 作加工速率 | `SmartFluidTankBehaviour`、`DirectBeltInputBehaviour`、`ProcessingInventory`、`IFluidHandler` capability、`KineticBlockEntityRenderer` | `CEI/common/kinetics/grindstone/GrindstoneDrainBlockEntity.java:71,104-112,132-151,371` |
| Blaze Enchanter | `BlazeExperienceBlockEntity` → CDP `BlazeBlockEntity` | **不消耗应力** | — | **无**（靠经验流体当燃料，热量等级决定模式） | `SmartBlockEntity.addBehaviours`、`ScrollValueBehaviour`、`ValueBoxTransform.Sided`、`ArmInteractionPoint`、`BlazeBlockVisual`(Flywheel) | `CEI/common/processing/enchanter/BlazeEnchanterBlockEntity.java:68`、`.../EnchanterBehaviour.java:55` |
| Blaze Forger | 同上 | **不消耗应力** | — | 无 | 同上 + 自定义 `ScrollValueBehaviour` 多选菜单 | `CEI/common/processing/forger/BlazeForgerBlockEntity.java:73`、`.../BlazeForgerModeBehaviour.java:36` |
| Classic Blaze Enchanter（旧版机器） | 同上 | 不消耗应力 | — | 无 | 自定义 Flywheel visual（手抄 `BlazeBurnerVisual`） | `CEI/common/processing/classic_enchanter/ClassicBlazeEnchanterVisual.java:53` |
| Printer | `SmartBlockEntity` | 不消耗应力 | — | **无**（传送带速度决定节奏） | `BeltProcessingBehaviour`、`SmartFluidTankBehaviour`、`CenteredSideValueBoxTransform` | `CEI/common/fluids/printer/PrinterBlockEntity.java:64,76-88` |
| Brass Bookshelf（Apothic 集成） | `KineticBlockEntity` | 消耗 4.0 SU@1RPM | `CEIAConfig.server().stress().setImpact(4.0)`（独立的 stress 配置节） | 是：`isSpeedRequirementFulfilled()` 门控（`IRotate.SpeedLevel`） | `KineticBlockEntity` + 两个 `ScrollValueBehaviour` + `.asOptional()` 条件注册 | `CEI/integration/apothic_enchanting/common/kinetics/bookshelf/brass/BrassBookshelfBlockEntity.java:43,66-80`、`.../registry/CEIABlocks.java:57-66` |
| Experience Lantern（经验灯笼） | `SmartBlockEntity`（无动力） | 不消耗应力 | — | 无 | `MovementBehaviour.REGISTRY`、`MountedFluidStorageType`、contraption fluid storage | `CEI/common/fluids/lantern/ExperienceLanternMovementBehaviour.java:38`、`CEI/common/registry/CEIBlocks.java:175-185` |

**第一结论**：CEI 全部 8 台机器里只有 2 台接动力学，且**都只是"应力消耗 + 转速乘子"**，没有一台自制"传动元件"（齿轮/轴/离合器）。所以"传动元件怎么写"CEI 给不了现成答案，得看 Create 自己（`CR/content/kinetics/base/` 下 `IRotate`、`RotatedPillarKineticBlock`、`HorizontalKineticBlock`、`GeneratingKineticBlockEntity`）。

### 2.1 Mechanical Grindstone：把"零部件"做成纯方块，BE 用原版的

这是全文**最值得抄的一招**。方块：

```java
// CEI/common/kinetics/grindstone/MechanicalGrindstoneBlock.java:54
public class MechanicalGrindstoneBlock extends RotatedPillarKineticBlock implements IBE<KineticBlockEntity> {
```

注册：

```java
// CEI/common/registry/CEIBlocks.java:64-72
public static final BlockEntry<MechanicalGrindstoneBlock> MECHANICAL_GRINDSTONE = REGISTRATE
        .block("mechanical_grindstone", MechanicalGrindstoneBlock::new)
        .initialProperties(SharedProperties::stone)
        .transform(CEIConfig.stress().setImpact(4.0))     // <-- 应力在这里登记
        .transform(pickaxeOnly())
        .blockstate(BlockStateGen.axisBlockProvider(false))
        .item(MechanicalGrindStoneItem::new)
        .build()
        .register();

// CEI/common/registry/CEIBlockEntities.java:49-54
public static final BlockEntityEntry<KineticBlockEntity> MECHANICAL_GRINDSTONE = REGISTRATE
        .blockEntity("mechanical_grindstone", KineticBlockEntity::new)   // <-- 原版 BE
        .visual(() -> SingleAxisRotatingVisual.of(CEIPartialModels.MECHANICAL_GRINDSTONE), false)
        .renderer(() -> KineticBlockEntityRenderer::new)
        .validBlock(CEIBlocks.MECHANICAL_GRINDSTONE)
        .register();
```

- `KineticBlockEntity::new` 直接当工厂：**磨石轮自身没有任何状态**（没有库存、没有进度），所以不需要自定义 BE。
- `.visual(..., false)` 的第二个参数是"是否同时用 BER 渲染方块本体"（`false` = 只用 Flywheel visual 渲染一个旋转 partial model）。对比 `GRINDSTONE_DRAIN` 用 `true`（`CEIBlockEntities.java:57`），因为它要在 BER 里额外画物品和流体，且要借用磨石方块的状态来转。
- `blockstate(BlockStateGen.axisBlockProvider(false))` + `getRenderShape() → ENTITYBLOCK_ANIMATED`（`MechanicalGrindstoneBlock.java:141-144`）：**方块本体不渲染，全靠 BER/visual**。这是 Create 旋转件的标准做法。
- 交互逻辑全在方块里（`useItemOn`，`MechanicalGrindstoneBlock.java:64-134`）：
  - 空手右键 → 从下方 `GrindstoneDrainBlockEntity` 的 `Capabilities.ItemHandler.BLOCK` 用**魔法槽位 3000** 抽物品（`:68-88`），速度 ≥ 32 时对玩家造成伤害（`:78-82`）。
  - 速度 < 30 直接 `PASS`（`:91-92`）——**这是"应力消耗机器只在有速度时工作"的最省事实现**：不写 tick，只在交互时判速度。
  - 有速度时优先当"砂纸"（`SandPaperPolishingRecipe.canPolish/applyPolish`，`:95-111`），失败再当"砂轮"（`GrindstoneHelper.grindItem`，`:116-130`）。
- `hasShaftTowards` 只允许沿 `AXIS` 面接轴、`getRotationAxis` 返回 `AXIS`（`:147-164`）。

**坑**：`MechanicalGrindStoneItem` 被覆写成"放到 Item Drain 上就变成 Grindstone Drain"（`MechanicalGrindStoneItem.java:50-82`）。它自己构造 `BlockPlaceContext` 子类，`replaceClicked |= clickedDrain`（`:70`），然后再由配方 `createRecipe()` 生成一条 **`ManualApplicationRecipe`**（`:40-47`）—— 同一件事在"放置行为"和"JEI 展示"两处各写一遍。我们自己若做"放上去就变体"的机器，要记得**配方只是给 JEI 看的假数据**，真正生效的是 `getPlacementState`。

### 2.2 Stress：Create 6 的应力模型 + CEI 的正确接入方式

先看 Create 侧的**真实 API**：

```java
// CR/api/stress/BlockStressValues.java:10-32
public class BlockStressValues {
    /** Registry for suppliers of stress impacts. Determine the base impact at 1 RPM. */
    public static final SimpleRegistry<Block, DoubleSupplier> IMPACTS = SimpleRegistry.create();
    /** Registry for suppliers of stress capacities. Determine the base capacity at 1 RPM. */
    public static final SimpleRegistry<Block, DoubleSupplier> CAPACITIES = SimpleRegistry.create();
    /** Registry for generator RPM values. This is only used for tooltips; actual functionality is determined by the block. */
    public static final SimpleRegistry<Block, GeneratedRpm> RPM = SimpleRegistry.create();
    public static double getImpact(Block block) { ... }        // :24-27
    public static double getCapacity(Block block) { ... }      // :29-32
    public static NonNullConsumer<Block> setGeneratorSpeed(int value) { ... }              // :37-39
    public static NonNullConsumer<Block> setGeneratorSpeed(int value, boolean mayGenerateLess) { ... } // :44-46
}
```

`SimpleRegistry` 的关键语义（`CR/api/registry/SimpleRegistry.java:28-47`）：

- `register(K, V)`：**直接登记，优先级高于所有 provider**；同一 key 重复登记抛 `IllegalArgumentException`。
- `registerProvider(Provider<K,V>)`：**懒查询**，只有 `get()` 没命中直接登记时才问 provider，结果会缓存；`invalidate()` 才重算。

再看 Create 自己怎么用：

```java
// CR/infrastructure/config/AllConfigs.java:68-70   —— provider 只注册一次，覆盖所有"没显式登记"的方块
CStress stress = server().kinetics.stressValues;
BlockStressValues.IMPACTS.registerProvider(stress::getImpact);
BlockStressValues.CAPACITIES.registerProvider(stress::getCapacity);

// CR/AllBlocks.java:572-584   —— Create 自己的方块在 builder 里登记配置默认值
public static final BlockEntry<WaterWheelBlock> WATER_WHEEL = REGISTRATE.block("water_wheel", WaterWheelBlock::new)
    ...
    .transform(CStress.setCapacity(32))
    .onRegister(BlockStressValues.setGeneratorSpeed(8))
```

**`CStress` 对附属是禁区**——它自己就把门焊死了：

```java
// CR/infrastructure/config/CStress.java:71-93
public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> setImpact(double value) {
    return builder -> {
        assertFromCreate(builder);
        ResourceLocation id = Create.asResource(builder.getName());   // <-- 强制用 create 命名空间
        DEFAULT_IMPACTS.put(id, value);
        return builder;
    };
}
private static void assertFromCreate(BlockBuilder<?, ?> builder) {
    if (!builder.getOwner().getModid().equals(Create.ID)) {
        throw new IllegalStateException("Non-Create blocks cannot be added to Create's config.");
    }
}
```

所以**附属只有两条合法路线**：

| 路线 | 写法 | 优劣 |
|---|---|---|
| A. 直接登记常量 | `BlockStressValues.IMPACTS.register(MY_BLOCK.get(), () -> 4.0)` | 最简单，但不进配置文件、不可调 |
| B. 抄一份 `CStress` 到自己 mod（CEI 选的路） | 自己的 `StressConfig extends ConfigBase`，`registerAll` 里 `BlockStressValues.IMPACTS.registerProvider(this::getImpact)` | 应力值进自己的配置文件、可热重载、tooltip 显示正常 |

CEI 选 B，而且**没有自己写**，直接用 CDP 的 `StressConfig`：

```java
// CEI/config/CEIStressConfig.java:24-32
public class CEIStressConfig extends StressConfig {
    public CEIStressConfig() { super(CEICommon.ID); }
    @Override protected int getVersion() { return 1; }     // 升版本号 = 重置玩家配置
}

// CEI/config/CEIKineticsConfig.java:34
public final CEIStressConfig stressValues = nested(0, CEIStressConfig::new, Comments.stress);
```

CDP `StressConfig` 的实现（外部源码，⚠ 版本为 GitHub `main`，与 1.11.9 可能有细微差异）：

```java
// create_dragons_plus: plus/dragons/createdragonsplus/config/StressConfig.java（节选）
@CodeReference(value = CStress.class, source = "create", license = "mit")
public class StressConfig extends ConfigBase {
    @Override
    public void registerAll(Builder builder) {
        builder.comment(".", Comments.su, Comments.impact).push("impact");
        defaultImpacts.forEach((id, value) -> this.impacts.put(id, builder.define(id.getPath(), value)));
        builder.pop();
        builder.comment(".", Comments.su, Comments.capacity).push("capacity");
        defaultCapacities.forEach((id, value) -> this.capacities.put(id, builder.define(id.getPath(), value)));
        builder.pop();

        BlockStressValues.IMPACTS.registerProvider(this::getImpact);
        BlockStressValues.CAPACITIES.registerProvider(this::getCapacity);
    }
    public <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> setImpact(double value) {
        return builder -> {
            validateOwningMod(builder);      // 只允许自己 mod 的方块，和 CStress 同构
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(modid, builder.getName());
            defaultImpacts.put(id, value);
            return builder;
        };
    }
}
```

注意这里的**两个时间点分离**，这是整套机制的核心：

1. `setImpact(value)` 执行于**方块 builder 链**（类静态初始化期）→ 只往 `defaultImpacts` 塞默认值。
2. `registerAll(builder)` 执行于**配置对象构建期**（CEIConfig 构造，`CEICommon.java:73`）→ 用默认值 `define` 出配置项，并注册 provider。
3. 运行时 `getImpact(block)` 通过 `RegisteredObjectsHelper.getKeyOrThrow(block)` 反查 id，命中配置项则返回 `ConfigValue::get`，否则返回 `null`（→ provider 弃权 → 上层按 0 处理）。

**应力/容量与转速的换算**（这是我们必须理解的公式）：

```java
// CR/content/kinetics/KineticNetwork.java:165-175
public float getActualCapacityOf(KineticBlockEntity be) {
    return sources.get(be) * getStressMultiplierForSpeed(be.getGeneratedSpeed());
}
public float getActualStressOf(KineticBlockEntity be) {
    return members.get(be) * getStressMultiplierForSpeed(be.getTheoreticalSpeed());
}
private static float getStressMultiplierForSpeed(float speed) {
    return Math.abs(speed);
}
```

即：

- **消耗方**：`实际应力 = IMPACT(1RPM) × |theoreticalSpeed|`。所以 4.0 的磨石在 32 RPM 下吃 128 SU——这就是 `CStress.Comments.impact` 里那句 "this cost is doubled for every speed increase it receives" 的准确含义（每翻倍转速，实际消耗翻倍）。
- **源**：`实际容量 = CAPACITY(1RPM) × |generatedSpeed|`。水车 32 SU@1RPM、8 RPM → 256 SU。
- `KineticBlockEntity.calculateStressApplied()` 就是 `BlockStressValues.getImpact(getStressConfigKey())`（`CR/content/kinetics/base/KineticBlockEntity.java:179-183`），`calculateAddedStressCapacity()` 同理（`:185-189`）。`getStressConfigKey()`（`:175-177`）是**留给附属的覆写点**：多状态方块若想共用一个配置项，覆写它返回一个代表方块即可。

### 2.3 Grindstone Drain：一台"正确的"应力消耗机器长什么样

它几乎是把 Create 自己的 `MillstoneBlockEntity`/`CrushingWheel` 那套拆开重装。逐块看：

**(a) 继承与进度容器**

```java
// CEI/common/kinetics/grindstone/GrindstoneDrainBlockEntity.java:71-100
public class GrindstoneDrainBlockEntity extends KineticBlockEntity implements Clearable {
    public static final int GRINDING_TIME = 20;
    public ProcessingInventory inventory;                 // Create 自带
    protected SmartFluidTankBehaviour tank;
    private DirectBeltInputBehaviour beltInput;
    private AdvancementBehaviour advancement;

    public GrindstoneDrainBlockEntity(...) {
        super(type, pos, state);
        inventory = new ProcessingInventory(this::start) {          // onContentsChanged -> start
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                var space = tank.getPrimaryHandler().getSpace();
                int a = GrindstoneHelper.getExperienceFromItem(stack);
                int b = GrindstoneHelper.getExperienceFromGrindingRecipe(level, stack);
                if (a > space || b > space) return stack;           // 经验罐装不下就不收
                return super.insertItem(slot, stack, simulate);
            }
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (slot == 3000) { // IMPORTANT: 3000 is for internal usage ...
                    var result = getStackInSlot(0); clear(); return result;
                }
                return ItemStack.EMPTY;
            }
        }.withSlotLimit(true);
    }
```

**槽位 3000 的 hack**（`:93`，注释是作者自己写的 IMPORTANT）：`ProcessingInventory` 正常拒绝外部抽取，CEI 需要一个"外部可以掏空整台机器"的入口（上方磨石空手右键、`destroy()` 掉落，见 `:339`），于是约定 `slot == 3000` 表示"清空并返回全部"。代价是**这个 magic number 会跟着 API 语义跑**，我们自己要用就在自己侧封装成 `extractAll()`，别把 3000 泄漏到别的类。

**(b) Behaviour 装配（Create 的"能力即 Behaviour"模型）**

```java
// CEI/.../GrindstoneDrainBlockEntity.java:104-112
@Override
public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    super.addBehaviours(behaviours);                       // 必须先 super：KineticBlockEntity 有自己的
    tank = SmartFluidTankBehaviour.single(this, CEIConfig.fluids().mechanicalGrindstoneFluidCapacity.get());
    beltInput = new DirectBeltInputBehaviour(this).allowingBeltFunnels();
    advancement = new AdvancementBehaviour(this);          // CDP 的成就 behaviour
    behaviours.add(tank);
    behaviours.add(beltInput);
    behaviours.add(advancement);
}
```

四个 Behaviour 各管一件事，**这就是 Create 机器的标准装配表**：

| Behaviour | 负责 | 我们做机器时是否需要 |
|---|---|---|
| `SmartFluidTankBehaviour.single(this, capacity)` | 单罐流体 + 自动对外暴露 capability + goggle tooltip | 机器吃/产流体时必带 |
| `DirectBeltInputBehaviour(this).allowingBeltFunnels()` | 漏斗/传送带直插直取 + `tryExportingToBeltFunnel` | 需要跟传送带/漏斗交互时必带 |
| `ProcessingInventory`（不是 Behaviour，是字段） | 输入槽 + `remainingTime/recipeDuration/appliedRecipe` 三件套 | 有"加工时间"的机器必用 |
| `AdvancementBehaviour`（CDP） | 摆放者记录 + stat/trigger 奖励 | Create 原生无，可用 `Awardable`/自己写 |

**(c) capability 按面暴露**（这是最容易写错的地方）

```java
// CEI/.../GrindstoneDrainBlockEntity.java:114-124
public @Nullable IItemHandler getItemHandler(@Nullable Direction side) {
    if (side != Direction.DOWN) return inventory;   // 只有底面不给物品
    return null;
}
public @Nullable IFluidHandler getFluidHandler(@Nullable Direction side) {
    if (side == getBlockState().getValue(HorizontalKineticBlock.HORIZONTAL_FACING).getOpposite() || side == null)
        return tank.getCapability();
    return null;
}
```

然后在 `RegisterCapabilitiesEvent` 里挂上去（`CEIBlockEntities.java:98-114`）：

```java
@SubscribeEvent
public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
    event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, GRINDSTONE_DRAIN.get(), GrindstoneDrainBlockEntity::getItemHandler);
    event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,  GRINDSTONE_DRAIN.get(), GrindstoneDrainBlockEntity::getFluidHandler);
    ... // Printer / BlazeEnchanter / BlazeForger / ClassicBlazeEnchanter / ExperienceLantern
}
```

**注意：CEI 用的是 NeoForge capability，不是 Create 的 `FluidBehaviour`。** 6.x 里 Create 的流体机器（`SmartFluidTankBehaviour`）本身已经实现 `IFluidHandler`，pipe 网络扫的就是 capability，所以**接管道只要 capability 对了就自动通**，不需要额外的 pipe 适配代码。⚠ Create 另有 `com.simibubi.create.api.behaviour.BlockEntityBehaviour` 体系内的 `FluidManipulationBehaviour`（用于主动吞吐），CEI 没用到，我们如需"主动抽/灌"再看。

**(d) 转速耦合："只有两个反向转的轮子才磨"**

```java
// CEI/.../GrindstoneDrainBlockEntity.java:132-151
public float getRelativeSpeed() {
    float speed = getSpeed();
    if (speed == 0f) return 0f;
    var above = worldPosition.above();
    if (!(level.getBlockState(above).getBlock() instanceof MechanicalGrindstoneBlock grinderWheel)) return 0f;
    var facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
    if (grinderWheel.getRotationAxis(aboveState) != facing.getAxis()) return 0f;   // 轴必须同向
    float aboveSpeed = ...getSpeed();
    if (speed > 0f) return aboveSpeed < 0f ? Math.min(speed, -aboveSpeed) : 0f;
    else            return aboveSpeed > 0f ? Math.min(-speed, aboveSpeed) : 0f;   // 反向才算有效转速
}
```

这是**"机械结构耦合"的极佳范例**：两个独立方块实体、各自吃应力、通过"相对转速"合成一个加工速率。加工主循环（`tick()`，`:358-450`）：

```java
float processingSpeed = getRelativeSpeed();
if (processingSpeed == 0) return;                                   // 不转就不干活（省 CPU）
if (inventory.remainingTime == -1) { ... start(...) ; return; }
processingSpeed = Mth.clamp(processingSpeed / 24, 1, 128);          // :371 转速->速率，下限 1 上限 128
inventory.remainingTime -= processingSpeed;
if (inventory.remainingTime < 5 && !inventory.appliedRecipe) { applyRecipe(); ... }  // :377-387
```

`clamp(x/24, 1, 128)` 这个映射值得直接抄：**任何转速都至少 1 倍速，最高 128 倍速**，既避免 0 速卡死，也避免超高转速一帧出结果。

**(e) 完成后的输出三重降级**（`:389-449`）：先试传送带漏斗（`beltInput.tryExportingToBeltFunnel`），再试相邻 `DirectBeltInputBehaviour`（`BlockEntityBehaviour.get(level, outputPos, DirectBeltInputBehaviour.TYPE)`），最后才 `new ItemEntity` 弹出。**这是 Create 机器的标准输出顺序**，我们的机器照这个写就不会"东西卡在机器里"。

**(f) 渲染：BER 借用另一个方块的状态**

```java
// CEI/.../GrindstoneDrainRenderer.java:43-59
public class GrindstoneDrainRenderer extends KineticBlockEntityRenderer<GrindstoneDrainBlockEntity> {
    @Override protected void renderSafe(...) {
        renderItems(...); renderFluid(...); super.renderSafe(...);
    }
    @Override protected BlockState getRenderedBlockState(GrindstoneDrainBlockEntity be) {
        var axis = getRotationAxisOf(be);
        return CEIBlocks.MECHANICAL_GRINDSTONE.getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, axis);
    }
}
```

`KineticBlockEntityRenderer.getRenderedBlockState` 的默认实现返回 `be.getBlockState()`；CEI 覆写成"渲染磨石轮"——**一台机器要画另一个方块的旋转体时，不需要额外 visual，改这一个方法就行**。物品/流体则手绘（`renderItems` `:61-115`、`renderFluid` 用 `NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(...)` `:117-138`）。注意 renderer 直接读 `drain.tank`（`protected`，同包可见）——**方便但有耦合**，我们的 renderer 最好放在与 BE 同包或加 getter。

### 2.4 配方：`StandardProcessingRecipe` + 自动派生

```java
// CEI/common/kinetics/grindstone/GrindingRecipe.java:46-88
public class GrindingRecipe extends StandardProcessingRecipe<SingleRecipeInput> implements IAssemblyRecipe {
    public GrindingRecipe(ProcessingRecipeParams params) {
        super(CEIRecipes.GRINDING, params);
        if (fluidIngredients.size() + fluidResults.size() > 1)
            throw new IllegalArgumentException("Grinding recipe can only have either 1 fluid input or 1 fluid result");
    }
    public static StandardProcessingRecipe.Builder<GrindingRecipe> builder(ResourceLocation id) {
        return new StandardProcessingRecipe.Builder<>(GrindingRecipe::new, id);
    }
    @Override protected int getMaxInputCount()  { return 1; }
    @Override protected int getMaxOutputCount() { return 4; }
    @Override protected int getMaxFluidOutputCount() { return 1; }
    @Override protected boolean canSpecifyDuration() { return true; }
    @Override public boolean matches(SingleRecipeInput input, Level level) {
        return ingredients.getFirst().test(input.item());
    }
```

配套的 juice：

- **类型/序列化器注册**用 CDP 的 `RecipeTypeInfo`（`CEI/common/registry/CEIRecipes.java:35-47`）：

```java
private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, CEICommon.ID);
private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, CEICommon.ID);
public static final RecipeTypeInfo<GrindingRecipe> GRINDING = register("grinding", () -> new StandardProcessingRecipe.Serializer<>(GrindingRecipe::new));
```
  `StandardProcessingRecipe.Serializer` 是 Create 现成的，**我们加一条 Create 风味加工配方只要写一个类 + 一行 `RecipeTypeInfo`**。

- **`IAssemblyRecipe`**：让这条配方能被"序列装配（Sequenced Assembly）"复用（`GrindingRecipe.java:96-121`），`getDescriptionForAssembly()` 出文案、`addRequiredMachines()` 声明需要磨石+Item Drain、`getJEISubCategory()` 指到自己的 JEI 子分类。**做加工机器时这一层几乎白送**，值得接。

- **JEI 里"从 Create 已有配方反推"**：`fromPolishing(...)`（`:57-68`）把每条 `SandPaperPolishingRecipe` 复制成一条 grinding 配方，只在 JEI 展示（`CEIJeiPlugin.java:98-104`）。这样玩家在原版砂纸页就能看到"磨石也能做"。

- **顺序装配 + 普通配方双查**（`GrindstoneDrainBlockEntity.java:229-231`）：

```java
var grinding = SequencedAssemblyRecipe.getRecipe(level, input, CEIRecipes.GRINDING.getType(), GrindingRecipe.class);
if (grinding.isEmpty())
    grinding = recipeManager.getRecipeFor(CEIRecipes.GRINDING.getType(), input, level);
```

**这是 Create 附属处理加工配方的标配顺序**，别只写后半句。

### 2.5 Blaze Enchanter / Forger：不吃应力，用"Behaviour + ArmInteractionPoint"接进 Create

**(a) 继承链与热量等级**

```
BlazeEnchanterBlockEntity
  → BlazeExperienceBlockEntity (CEI, abstract)
    → plus.dragons.createdragonsplus...BlazeBlockEntity (CDP)   ⚠ CDP 源码未随附
      → (推测 SmartBlockEntity 体系 + 复用 BlazeBurnerBlock.HeatLevel)
```

`BlazeExperienceBlockEntity` 把"经验流体 = 燃料"这件事抽象出来（`CEI/common/fluids/experience/BlazeExperienceBlockEntity.java:56-137`）：

```java
@Override
public HeatLevel getHeatLevel() {
    if (getSpecialExperience() > 0) return HeatLevel.SEETHING;      // 超级经验 -> 最高档
    double experience = getNormalExperience();
    if (experience > 0) {
        boolean lowPercent = experience / getNormalTank().getCapacity() < 0.0125;
        return lowPercent ? HeatLevel.FADING : HeatLevel.KINDLED;
    }
    return HeatLevel.SMOULDERING;
}
public boolean consumeExperience(int amount, boolean special, boolean simulate) { ... }   // :128-137
```

**结论**：它的"动力"是**流体燃料 + 热量等级**，与应力网络零关系。做"星球机器要吃某种资源"时，这套"资源→HeatLevel→工作模式"的映射很值得抄（比自造 enum 省事，且白拿 Create Blaze Burner 的渲染/音效/ponder 素材）。

**(b) 两个罐（普通/超级）+ 容量配置**

```java
// CEI/.../BlazeEnchanterBlockEntity.java:98-107
@Override protected ConfigurableFluidTank createNormalTank(Consumer<FluidStack> cb) {
    return new ConfigurableFluidTank(CEIConfig.fluids().blazeEnchanterFluidCapacity.get(), cb)
            .allowInsertion(fluidStack -> fluidStack.is(CEIFluids.EXPERIENCE));
}
@Override protected ConfigurableFluidTank createSpecialTank(Consumer<FluidStack> cb) {
    return new ConfigurableFluidTank(CEIConfig.fluids().blazeEnchanterFluidCapacity.get(), cb)
            .forbidInsertion();                       // 超级罐只能靠"蛋糕/经验块"之类燃料灌
}
```

`ConfigurableFluidTank` / `FluidTankBehaviour` 是 CDP 的（支持"多罐、可换实现、可禁插入"）。Create 原生只有 `SmartFluidTankBehaviour`（单/多罐但不可换策略）。**我们要"某罐禁止管道输入"这种规则时，Create 原生做不到，得自己包一层 `IFluidHandler` 做过滤**（这是 CDP 存在的正当理由之一）。

**(c) Behaviour：滑动数值 + 模板槽**

```java
// CEI/common/processing/enchanter/EnchanterBehaviour.java:55-68
public class EnchanterBehaviour extends ScrollValueBehaviour implements IHaveGoggleInformation {
    public static final BehaviourType<EnchanterBehaviour> TYPE = new BehaviourType<>();
    public static final String LEVEL = "EnchantingLevel";
    public static final String TEMPLATE = "EnchantingTemplate";
    private final BlazeEnchanterBlockEntity enchanter;
    private ItemStack template = ItemStack.EMPTY;
    ...
    public EnchanterBehaviour(BlazeEnchanterBlockEntity enchanter, ValueBoxTransform transform, ValueBoxTransform.Sided templateItemTransform) {
        super(CEILang.translate("gui.blaze_enchanter.level").component(), enchanter, transform);
```

要点：

- `ScrollValueBehaviour` 的构造参数是 `(label, blockEntity, ValueBoxTransform)`；**`ValueBoxTransform` 决定"这个旋钮贴在哪一面、往哪偏移"**，CEI 每个机器都内联一个私有子类：

```java
// CEI/.../BlazeEnchanterBlockEntity.java:322-338
private static class EnchanterTransform extends ValueBoxTransform.Sided {
    @Override protected Vec3 getSouthLocation() { return VecHelper.voxelSpace(8, 8, 13.5); }
    @Override public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack poseStack) {
        float yRot = AngleHelper.horizontalAngle(getSide()) + 180;
        TransformStack.of(poseStack).rotateYDegrees(yRot);
    }
    @Override protected boolean isSideActive(BlockState state, Direction direction) {
        return direction.getAxis().isHorizontal();       // 四个侧面都有，顶/底没有
    }
}
```

- **"点击旋钮界面里放物品"**（模板槽）用 `onShortInteract`（`EnchanterBehaviour.java:152-171`）：手空 → 清空；手拿可附魔物品 → 装进模板；不合法 → `AllSoundEvents.DENY` + `displayClientMessage`。**这是 Create 里"让玩家往机器身上塞一个物品"的标准姿势**，不需要 GUI。
- **NBT 要写两遍**：`write(...)` 存 `LEVEL + TEMPLATE`，`writeSafe(...)` 只存 `LEVEL`（`:174-182`）——后者用于"剪贴板复制/结构安全 NBT"，**带 `ItemStack` 的字段一律不进 `writeSafe`**，否则复制机会复制出物品。这是 Create 机器的硬规矩。
- `onShortInteract` 里显式排除了扳手与机械臂（`:154-157`）：`if (AllItems.WRENCH.isIn(stack)) return;` / `if (AllBlocks.MECHANICAL_ARM.isIn(stack)) return;`。**不排会被扳手拆机逻辑吞掉**。
- goggle tooltip 由 Behaviour 自己实现（`:196-252`），BE 只是转发（`BlazeEnchanterBlockEntity.java:308-313`）：

```java
@Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
    boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
    added |= enchanter.addToGoggleTooltip(tooltip, isPlayerSneaking);
    return added;
}
```

**(d) 机械臂交互点**

```java
// CEI/common/processing/enchanter/BlazeEnchanterArmInteractionPoint.java:35-86
public class BlazeEnchanterArmInteractionPoint extends ArmInteractionPoint {
    @Override public ItemStack insert(ArmBlockEntity armBlockEntity, ItemStack stack, boolean simulate) {
        ...
        InteractionResultHolder<ItemStack> result = BlazeExperienceBlock.applyFuel(cachedState, level, pos, input, false, false, simulate);
        if (result.getResult().consumesAction()) { ... }        // 先当"燃料"吃
        else if (result.getResult() == InteractionResult.PASS) {
            return enchanter.insertItem(input, simulate);       // 不是燃料才当"待加工物品"
        }
        return input;
    }
    @Override public ItemStack extract(ArmBlockEntity armBlockEntity, int slot, int amount, boolean simulate) {
        return enchanter.extractItem(false, simulate);
    }
    @Override public int getSlotCount(ArmBlockEntity armBlockEntit) { return 1; }

    public static class Type extends ArmInteractionPointType {
        @Override public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
            return CEIBlocks.BLAZE_ENCHANTER.has(state);
        }
        @Nullable @Override public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
            return new BlazeEnchanterArmInteractionPoint(this, level, pos, state);
        }
    }
}
```

注册（`CEI/common/registry/CEIArmInterationPoints.java:30-39`）：

```java
public static final RegistryEntry<ArmInteractionPointType, BlazeEnchanterArmInteractionPoint.Type> BLAZE_ENCHANTER =
        REGISTRATE.armInteractionPoint("blaze_enchanter", BlazeEnchanterArmInteractionPoint.Type::new).register();
```

⚠ `armInteractionPoint(...)` 是 CDP 糖；Create 原生请用 `SimpleBuilder`（见 §1.3 模板），目标 registry 为 `CreateRegistries.ARM_INTERACTION_POINT_TYPE`（`CR/api/registry/CreateBuiltInRegistries.java:34`）。

**一个机械臂点 = 一个 `ArmInteractionPoint` 子类 + 一个 `ArmInteractionPointType` 子类 + 一行注册**。做"可被机械臂自动化的星球设备"时这是最划算的投入。

**(e) tick 与状态同步**

`BlazeEnchanterBlockEntity.tick()`（`:160-247`）的结构堪称模板：

1. `super.tick();`
2. 派生状态比对（`special` = 是否 SEETHING；`cursed` = 是否被方块挡住闪电），变了就 `update = true`（`:162-173`）。
3. **虚拟世界分支**（ponder / contraption）：`if (level.isClientSide() && isVirtual()) { ... }`，用 `ENCHANTING_TIME / 4` 快进（`:174-190`）。
4. 服务端正式分支：`if (!(level instanceof ServerLevel serverLevel)) return;`（`:191-192`）。
5. 消耗判定用 `consumeExperience(cost, special, true)` **simulate 一次**，成功才真正走倒计时，完成时再 `consumeExperience(cost, special, false)`（`:198` 与 `:232`）——**先试后扣**，避免"扣了经验但加工失败"。
6. 每步 `notifyUpdate()`；完成时 `nextSeed()` + `enchanter.update(heldItem)` + `notifyUpdate()`。

`notifyUpdate()` 是 `SmartBlockEntity` 的"发一次 client 同步包"，比 `sendData()` 更轻（只发增量）。CEI 在 tick 里大量用 `notifyUpdate()`，只在结构变化时 `sendData()`（`EnchanterBehaviour.setValue`，`:131-132`）。

序列化：`write/read` 覆写并带 `clientPacket` 标志（`BlazeEnchanterBlockEntity.java:141-157`），`heldItem` 用 `ItemStack.saveOptional/parseOptional`。**在 `read` 末尾调 `updateEnchanterIfLevelReady()`**（`:156`）——因为客户端 `read` 时 `level` 可能还没挂上，直接调会 NPE。

**(f) 渲染：Flywheel visual + BER 分工**

```java
// CEI/common/registry/CEIBlockEntities.java:71-76
public static final BlockEntityEntry<BlazeEnchanterBlockEntity> BLAZE_ENCHANTER = REGISTRATE
        .blockEntity("blaze_enchanter", BlazeEnchanterBlockEntity::new)
        .visual(() -> BlazeBlockVisual::new)               // CDP 提供的 blaze 主体 visual
        .renderer(() -> BlazeEnchanterRenderer::new)       // BER：帽子 + 手持物
        .validBlock(CEIBlocks.BLAZE_ENCHANTER)
        .register();
```

对比"旧版机器"自己手抄一份 visual：

```java
// CEI/common/processing/classic_enchanter/ClassicBlazeEnchanterVisual.java:51-53
@FieldsNullabilityUnknownByDefault
@CodeReference(value = BlazeBurnerVisual.class, source = "create", license = "mit")
public class ClassicBlazeEnchanterVisual extends AbstractBlockEntityVisual<ClassicBlazeEnchanterBlockEntity>
        implements SimpleDynamicVisual, SimpleTickableVisual {
```

`@CodeReference`（CDP 注解）是他们的**代码出处标注规范**：抄了 Create 的哪个类、什么 license。**强烈建议我们也建立这个习惯**（内部注释即可），否则半年后没人分得清哪段是自研、哪段抄来要跟着上游改。

visual 内部逻辑（`:73-179`）就是 Create `BlazeBurnerVisual` 的移植：按 `heatLevel/active` 换模型、`instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(model)).createInstance()`、火焰用 `AllInstanceTypes.SCROLLING` + `AllSpriteShifts`、每帧 `setIdentityTransform().translate(getVisualPosition())...setChanged()`、`_delete()` 里逐一 `delete()`。**要用 Flywheel visual，这就是完整的最小范式**。

### 2.6 Brass Bookshelf（Apothic 集成）：速度门控的应力消耗件

```java
// CEI/integration/apothic_enchanting/common/kinetics/bookshelf/brass/BrassBookshelfBlockEntity.java:43-80
public class BrassBookshelfBlockEntity extends KineticBlockEntity implements IHaveGoggleInformation, IHaveStatType {
    protected StatTypeBehaviour statType;      // ScrollValueBehaviour 子类：选属性类型
    protected StatValueBehaviour statValue;    // ScrollValueBehaviour 子类：调数值
    ...
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        this.statType = new StatTypeBehaviour(..., new StatTypeTransform());
        this.statValue = new StatValueBehaviour(..., new StatValueTransform(), statType -> ...上限..., false);
        behaviours.add(this.statType);
        behaviours.add(this.statValue);
    }
    public int eterna() { return statType.get() == ETERNA && isSpeedRequirementFulfilled() ? statValue.getValue() : 0; }
    ...
    public boolean allowTreasure() { return CEIAConfig.server().stats().brassBookshelfAllowTreasures.get() && isSpeedRequirementFulfilled(); }
```

两个关键技术点：

1. **`isSpeedRequirementFulfilled()` 是 Create 现成的"最低转速"门控**。来源：`KineticBlockEntity.isSpeedRequirementFulfilled()` 读 `IRotate.getMinimumRequiredSpeedLevel()` 的 `SpeedLevel`，比较 `|speed| >= level.getSpeedValue()`；`SpeedLevel` 三档 `NONE/MEDIUM/FAST`，且 `MEDIUM/FAST` 的门槛值来自 Create 配置（`fastSpeed`/`mediumSpeed`）——证据 `CR/content/kinetics/base/IRotate.java:19-23,47-57,148-150`。
   > 我们做"必须达到某转速才工作"的传动件时，**只需要在 Block 上实现 `IRotate` 并覆写 `getMinimumRequiredSpeedLevel()`**，BE 侧一句 `isSpeedRequirementFulfilled()` 搞定，goggle tooltip 还会自动显示速度需求条（`IRotate.java:76-92`）。
2. **两个 `ValueBoxTransform` 挂同一台机器的不同面**：`StatTypeTransform` 只用 `Direction.UP`（`:82-92`），`StatValueTransform` 用水平面（`:94-99`）。同一台机器想放多个旋钮，就这么区分。
3. 应力注册走集成专用的配置节：`CEIAConfig.server().stress().setImpact(4.0)`（`CEI/integration/apothic_enchanting/common/registry/CEIABlocks.java:62`），且 `CEIAStressConfig extends CDP StressConfig`（`CEI/integration/apothic_enchanting/config/CEIAStressConfig.java:24-28`）。**主包与集成包各有一套应力配置，互不干扰**——这点很干净：可选集成被卸载时，主包配置项不受影响。

条件注册用 Registrate 的 `.asOptional()`（`CEIABlocks.java:59`）：整块（block+item+blockstate+配方条件）在缺 mod 时不注册。⚠ `.asOptional()` 来自 CDP 的 `AbstractRegistrate` 扩展/Registrate 本身（Registrate 有 `asOptional`），我们若用 CreateRegistrate 需自行确认可用性；不可用时用 `if (ModList.get().isLoaded(...))` 包住整个静态块是更保底的做法（但那要求把集成类**放进独立类文件**，不能放在会被无条件加载的类里）。

### 2.7 Printer：纯 Behaviour 机器（连 tick 都省了）

```java
// CEI/common/fluids/printer/PrinterBlockEntity.java:64-88
public class PrinterBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final int PROCESSING_TIME = 50;
    protected SmartFluidTankBehaviour tank;
    private PrinterBehaviour printer;
    public int processingTicks = -1;
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tank = SmartFluidTankBehaviour.single(this, CEIConfig.fluids().printerFluidCapacity.get());
        printer = new PrinterBehaviour(this, tank, new CenteredSideValueBoxTransform((state, direction) -> state.getValue(PrinterBlock.FACING) == direction));
        BeltProcessingBehaviour processing = new BeltProcessingBehaviour(this)
                .whenItemEnters(this::onItemEnters)
                .whileItemHeld(this::onItemHeld);
        behaviours.add(tank); behaviours.add(printer); behaviours.add(processing); behaviours.add(advancement);
    }
```

- 它**没有覆盖 `tick()` 做加工**，加工全在 `BeltProcessingBehaviour` 回调里（`:104-179`）。`tick()` 只递减 `processingTicks`（`:194-199`）。
- `onItemEnters` 返回 `HOLD` 表示"拦住物品别往前跑"，`PASS` 表示"放行"。这套 `ProcessingResult` 三态是传送带上加工的通用语（`GrindingDrain` 的 `PASS/HOLD` 与此同源）。
- 上/下不对称：`getFluidHandler` 只对非底面暴露（`:90-94`），因为它在传送带上方。
- 成本完全由 data map 决定（见 §3.5），配方本身只描述"印什么"。

**这是"机器逻辑 = Behaviour"的极致形态**——如果我们做的元件没有"自身旋转/自身进度"的强需求，优先考虑 `BeltProcessingBehaviour` / `DirectBeltInputBehaviour`，能省一个 BE 状态机。

### 2.8 Experience Lantern：contraption 上的流体容器（传动无关但可抄）

```java
// CEI/common/registry/CEIBlocks.java:175-185
public static final BlockEntry<ExperienceLanternBlock> EXPERIENCE_LANTERN = REGISTRATE
        .block("experience_lantern", ExperienceLanternBlock::new)
        .initialProperties(SharedProperties::softMetal)
        .transform(pickaxeOnly())
        .transform(mountedFluidStorage(CEIMountedStorageTypes.EXPERIENCE_LANTERN))   // <-- contraption 上带流体
        .onRegister(block -> MovementBehaviour.REGISTRY.register(block, new ExperienceLanternMovementBehaviour()))
        .addLayer(() -> RenderType::cutoutMipped)
        .blockstate(...)
        .simpleItem()
        .register();
```

两处扩展点：

1. `MountedFluidStorageType.mountedFluidStorage(RegistryEntry)`（Create 原生静态工具，见 `CR/api/contraption/storage/fluid/MountedFluidStorageType.java:37`）：声明"这个方块被 contraption 搬走时，流体怎么存"。
2. `MovementBehaviour.REGISTRY.register(block, behaviour)`（`CR/api/behaviour/movement/MovementBehaviour.java:31`，`SimpleRegistry<Block, MovementBehaviour>`）：声明"被搬走之后每 tick 干什么"。`ExperienceLanternMovementBehaviour.tick` 在 contraption 里直接操作整台装置的流体存储：

```java
// CEI/common/fluids/lantern/ExperienceLanternMovementBehaviour.java:40-53
if (!context.world.isClientSide && context.world.getGameTime() % 10 == 0) {
    var effectiveAABB = new AABB(context.position.subtract(0.5d, 0.5d, 0.5d), context.position.add(0.5d, 0.5d, 0.5d)).inflate(0.5);
    drainExp(context.world, effectiveAABB, context.contraption.getStorage().getFluids());
}
```

注意 `% 10 == 0` 的节流 + 只在服务端跑。

### 2.9 其它机器（简述，避免篇幅失控）

| 类 | 是什么 | 值得注意的点 |
|---|---|---|
| `ClassicBlazeEnchanter*` | 旧版附魔机，用配置开关 + 配方条件控制是否注册（`CEICreativeModeTabs.java:63-64`、`CEIRecipeProvider.java:128,157`） | **"功能开关"要同时在：创造标签、配方条件、ponder 注册（`CEIPonderScenes.java:69-73`）、创造标签搜索可见性 四处判断**，漏一处玩家就能看到幽灵物品 |
| `InfuserBlockEntity` | Apothic 集成的注魔机 | `AddReloadListenerEvent` 挂 `RELOAD_LISTENER`（`CEIACommon.java:91-93`）：配方数据要跟着资源重载 |
| `GemCutterBlockEntity` | Apotheosis 集成切宝石 | 同上模式 |
| `AffixAugmentorBlockEntity` | 词缀强化机 | — |
| `EnderWovenBag*` | contraption 上的"绑怪袋" | 唯一用到**自定义网络包**的地方（§3.9） |

---

## 3. CEI 用到的 Create 扩展点清单

### 3.1 方块 / 方块实体注册

```java
// 方块：Registrate builder + Create 的共享属性与状态生成器
.block("x", XBlock::new)
.initialProperties(SharedProperties::copperMetal)   // Create 的材质预设：stone/wooden/softMetal/copperMetal/netheriteMetal
.transform(pickaxeOnly())
.blockstate(BlockStateGen.horizontalBlockProvider(true))
.item(AssemblyOperatorBlockItem::new)               // 装配操作机物品（goggle 图标 + 摆放提示）
.transform(customItemModel())
.register()
```
`AssemblyOperatorBlockItem` 的使用见 `CEIBlocks.java:102`（Printer）——**凡是"能被机械臂/传送带当加工站"的方块，物品用它**，会自动获得 Create 的"装配操作机"提示与 tooltip。

```java
// 方块实体：CreateBlockEntityBuilder（由 CreateRegistrate.blockEntity 提供）
REGISTRATE.blockEntity("x", XBlockEntity::new)
    .visual(() -> SingleAxisRotatingVisual.of(PARTIAL), false)  // Flywheel visual（可选）
    .renderer(() -> XRenderer::new)                             // BER（可选）
    .validBlock(MY_BLOCK)
    .register()
```
`SingleAxisRotatingVisual`（`CR/content/kinetics/base/SingleAxisRotatingVisual.java`，CEI 用 `CEIBlockEntities.java:51`）是"绕单轴旋转"的现成 visual，**传动件首选**。

`IBE<T>` 三件套（`MechanicalGrindstoneBlock.java:151-164`）：

```java
@Override public Class<KineticBlockEntity> getBlockEntityClass() { return KineticBlockEntity.class; }
@Override public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() { return CEIBlockEntities.MECHANICAL_GRINDSTONE.get(); }
@Override public Axis getRotationAxis(BlockState state) { return state.getValue(AXIS); }
```

### 3.2 应力值

| API | 签名 | CEI 用法 | 我们要用在哪 |
|---|---|---|---|
| `BlockStressValues.IMPACTS` | `SimpleRegistry<Block, DoubleSupplier>` | 经 CDP `StressConfig` provider | 应力消耗件 |
| `BlockStressValues.CAPACITIES` | 同上 | 同上 | 应力源 |
| `BlockStressValues.RPM` | `SimpleRegistry<Block, GeneratedRpm>` | 未用（CEI 无源） | 应力源 tooltip |
| `BlockStressValues.setGeneratorSpeed(int)` / `(int, boolean)` | `NonNullConsumer<Block>` | 未用 | 应力源：`.onRegister(...)` |
| `SimpleRegistry.register(K,V)` / `registerProvider(P)` | `CR/api/registry/SimpleRegistry.java:34,40` | 只经 CDP 间接用 | 二选一，别混 |
| `CStress.setImpact/setCapacity` | `CR/infrastructure/config/CStress.java:71,80` | **不能用** | 禁用（抛异常） |

**给我们的最小可用写法（不引入 CDP）**：

```java
// 应力消耗件：常量版（不写配置）
public static final BlockEntry<MyMachineBlock> MY_MACHINE = REGISTRATE
        .block("my_machine", MyMachineBlock::new)
        .initialProperties(SharedProperties::copperMetal)
        .onRegister(block -> BlockStressValues.IMPACTS.register(block, () -> 8.0))
        .item().build().register();

// 应力源：容量 + RPM tooltip
public static final BlockEntry<MyGeneratorBlock> MY_GENERATOR = REGISTRATE
        .block("my_generator", MyGeneratorBlock::new)
        .initialProperties(SharedProperties::copperMetal)
        .onRegister(block -> BlockStressValues.CAPACITIES.register(block, () -> 64.0))
        .onRegister(BlockStressValues.setGeneratorSpeed(32))     // 只影响 tooltip
        .item().build().register();
```

⚠ 注意 `SimpleRegistry.register` 重复登记会抛 `IllegalArgumentException`（`SimpleRegistry.java:32-34` 的 javadoc）。**热重载/开发环境反复初始化时容易踩**——用 `register` 前先确认没被注册过，或统一走"配置 provider"路线（provider 可重复 `registerProvider`？其 javadoc 也说重复注册 provider 会抛，但 provider 只在 `registerAll` 里注册一次，实际安全）。

### 3.3 动力学基类与速度等级

| 用途 | 基类/接口 | 关键覆写 | 证据 |
|---|---|---|---|
| 轴对齐旋转件（磨石/齿轮/辊） | `RotatedPillarKineticBlock` | `getRotationAxis`、`hasShaftTowards` | `MechanicalGrindstoneBlock.java:54,147-164` |
| 水平朝向旋转件（排水口/风车） | `HorizontalKineticBlock` | `getPreferredHorizontalFacing` | `GrindstoneDrainBlock.java:119-135` |
| 纯消费者 BE | `KineticBlockEntity` | `addBehaviours`、`tick`、`getStressConfigKey`（可选） | `GrindstoneDrainBlockEntity.java:71` |
| **应力源 BE** | `GeneratingKineticBlockEntity` | `getGeneratedSpeed`、`updateGeneratedRotation()`、`calculateAddedStressCapacity()`（默认读 CAPACITIES） | `CR/content/kinetics/base/GeneratingKineticBlockEntity.java:18,89,109`；`WaterWheelBlockEntity.java:41,224` |
| 最低转速要求 | `IRotate#getMinimumRequiredSpeedLevel()` → `SpeedLevel` | `NONE/MEDIUM/FAST` | `CR/content/kinetics/base/IRotate.java:19-23,148-150`；`BrassBookshelfBlockEntity.java:66-80` |
| 传动比/方向 | `IRotate#getRotationSpeedModifier(Direction)` ⚠ 未在 CEI 中出现 | — | Create 内大齿轮/离合器等在用 |

`GeneratingKineticBlockEntity` 已经替我们做了：`updateGeneratedRotation()`（按 `getGeneratedSpeed()` 重算并 `notifyStressCapacityChange(calculateAddedStressCapacity())`，`:89-110`）、goggle 里的应力条显示（`:57-75`）。**我们做应力源，只需继承它 + 覆写 `getGeneratedSpeed()`，容量交给 `BlockStressValues.CAPACITIES`**。

### 3.4 ProcessingRecipe 系列

| 组件 | 类/方法 | CEI 证据 |
|---|---|---|
| 配方基类 | `StandardProcessingRecipe<SingleRecipeInput>` + `ProcessingRecipeParams` | `GrindingRecipe.java:46-51` |
| 序列化器 | `StandardProcessingRecipe.Serializer<>(Ctor::new)` | `CEIRecipes.java:38` |
| 类型+序列化 DeferredRegister | `RecipeTypeInfo<T>(name, serializerSupplier, SERIALIZERS, TYPES)`（CDP） | `CEIRecipes.java:35-47` |
| 生成器 | `StandardProcessingRecipe.Builder<GrindingRecipe>` | `GrindingRecipe.java:53-55` |
| 装配复用 | `IAssemblyRecipe` | `GrindingRecipe.java:96-121` |
| 顺序装配查询 | `SequencedAssemblyRecipe.getRecipe(level, input, type, Class)` | `GrindstoneDrainBlockEntity.java:229` |
| 查询普通配方 | `level.getRecipeManager().getRecipeFor(type, input, level)` | 同上 `:231` |
| 非 Create 配方 | `PrintingRecipe`（自定义 `Recipe` + 自定义 Serializer，`CEI/fluids/printer/PrintingRecipe.java`） | `CEIRecipes.java:37` |

### 3.5 流体处理

| 需求 | API | 证据 |
|---|---|---|
| 机器自带罐 | `SmartFluidTankBehaviour.single(be, capacity)` / `.getPrimaryHandler()` / `.getCapability()` | `GrindstoneDrainBlockEntity.java:106,122` |
| 多罐 + 策略（禁插入等） | CDP `FluidTankBehaviour` + `ConfigurableFluidTank(allowInsertion/forbidInsertion)` | `BlazeExperienceBlockEntity.java:73` |
| 对外暴露 | `RegisterCapabilitiesEvent.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, X::getFluidHandler)` | `CEIBlockEntities.java:102-113` |
| 流体配方字段 | `getFluidIngredients()` / `getFluidResults()`（返回 `SizedFluidIngredient` / `FluidStack`） | `GrindstoneDrainBlockEntity.java:190-200` |
| 灌装/抽取判定 | `SizedFluidIngredient.test(fluid)` + `amount()` | 同上 `:179-188` |
| 流体定义 | Registrate `fluid(...)` builder：`.source/.block/.bucket/.tag(AllFluidTags.BOTTOMLESS_DENY)` | `CEIFluids.java:44-71` |
| 开放管道效果（洒出/接触效果） | `OpenPipeEffectHandler.REGISTRY.register(fluid, handler)`（在 `FMLCommonSetupEvent.enqueueWork` 里） | `CEIFluids.java:77-81` |
| 发射器行为 | `DispenserBlock.registerBehavior(bucket, StandardDispenserBehaviour.INSTANCE)`（CDP） | `CEIFluids.java:83-85` |
| 管道连接朝向提示 | `FluidPipeBlock.canConnectTo(level, pos, state, facing)`（用于放置时自动朝向管道） | `GrindstoneDrainBlock.java:127` |
| goggle 流体条 | `KineticBlockEntity.containedFluidTooltip(tooltip, sneaking, tank.getCapability())` | `GrindstoneDrainBlockEntity.java:455` |

**注意**：CEI **没有**实现 `FluidBehaviour` 之类的"主动泵";它只做 capability。如果我们的元件需要"自己从网络抽流体"，再看 Create 的 `FluidManipulationBehaviour`。

### 3.6 Behaviour 体系（CEI 用得最多的扩展面）

| Behaviour | 作用 | CEI 证据 | 备注 |
|---|---|---|---|
| `SmartBlockEntity.addBehaviours(List)` | 装配入口（必须先 `super`） | `GrindstoneDrainBlockEntity.java:104`、`PrinterBlockEntity.java:76` | `KineticBlockEntity` 覆写了它 |
| `SmartFluidTankBehaviour` | 罐 | 同上 | — |
| `DirectBeltInputBehaviour` | 漏斗/带直连 | `:107` | `.allowingBeltFunnels()` |
| `ProcessingInventory`（非 Behaviour） | 加工槽 + 三件套计时 | `:81-100` | 可匿名子类化改插入规则 |
| `BeltProcessingBehaviour` | 传送带上加工 | `PrinterBlockEntity.java:80-82` | `.whenItemEnters` / `.whileItemHeld` |
| `ScrollValueBehaviour` | 旋钮数值/枚举 | `EnchanterBehaviour.java:55`、`BlazeForgerModeBehaviour.java:36` | 子类要覆写 `getType()`、`write/read`、`createBoard`、`setValue` |
| `ValueBoxTransform.Sided` | 旋钮贴面与命中 | `BlazeEnchanterBlockEntity.java:322-338` | `getSouthLocation` 用 `VecHelper.voxelSpace(8,8,13.5)` |
| `CenteredSideValueBoxTransform` | 居中贴面（带朝向判断） | `PrinterBlockEntity.java:78-79` | 构造参数 `(state, dir) -> 判定` |
| `BehaviourType<T>` | 类型键 | `EnchanterBehaviour.java:56`、`BlazeForgerModeBehaviour.java:37` | 静态常量 |
| `IHaveGoggleInformation` | goggle 显示 | 多处 | 与 `super.addToGoggleTooltip` 串联 |
| `ValueSettingsBoard` + `ValueSettingsFormatter` | 旋钮界面 | `EnchanterBehaviour.java:141-149`、`BlazeForgerModeBehaviour.java:70-80` | 多选菜单用 `valueSetting.row()` |
| `AdvancementBehaviour`（CDP） | 统计/成就 | 多处 | Create 原生替代：自己写 |

`BlazeForgerModeBehaviour` 是"**把滑动数值当枚举选择器**"的完整示例（`:70-113`）：`createBoard` 里 `max = 0`、`rowCount = 枚举数`、每行一个 `Component`，`setValueSettings` 里 `setValue(valueSetting.row())`。**要做"模式切换旋钮"，照抄这 40 行即可**。

### 3.7 Contraption（被活塞/矿车搬走的机器）

| 需求 | API | CEI 证据 |
|---|---|---|
| 搬运后每 tick 行为 | `MovementBehaviour.REGISTRY.register(block, behaviour)` | `CEIBlocks.java:111,127,143,181` |
| 搬运实体内部流体 | `transform(mountedFluidStorage(TYPE_ENTRY))` + `MountedFluidStorageType` 子类 | `CEIBlocks.java:180`、`ExperienceLanternMountedFluidStorageType.java:28-38` |
| 搬运期访问装置存储 | `context.contraption.getStorage().getFluids()` | `ExperienceLanternMovementBehaviour.java:45` |
| 搬运坐标 | `context.position` | 同上 `:42` |
| 自定义 actor 的网络包 | `ContraptionEnderWovenBagPocketChangePacket` + `CEIAPackets` | 见 §3.9 |

⚠ `MountedItemStorageType` CEI 有注册类（`CEIItems`/`CEIMountedStorageTypes` 体系）但本版本只注册了流体；做"搬走时保住物品栏"要找 Create 的 `MountedItemStorageType`。

### 3.8 机械臂 / Ponder / JEI / Goggle

**Ponder**（`CEI/client/ponder/`）：三件套 + 注册表

```java
// CEI/client/ponder/CEIPonderPlugin.java:27-42
public class CEIPonderPlugin implements PonderPlugin {
    @Override public String getModId() { return CEICommon.ID; }
    @Override public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) { CEIPonderScenes.register(helper); }
    @Override public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) { CEIPonderTags.register(helper); }
}
```
```java
// CEI/client/ponder/CEIPonderScenes.java:40-57
PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> HELPER = helper.withKeyFunction(RegistryEntry::getId);
HELPER.forComponents(CEIBlocks.MECHANICAL_GRINDSTONE)
      .addStoryBoard("grindstone/basic", GrindstoneScene::basic, CEIPonderTags.EXPERIENCE_APPLIANCES)
      .addStoryBoard("grindstone/extra", GrindstoneScene::extra);
```
场景写法（`GrindstoneScene.java:38-78`）的要点：
- `scene.title(...)` / `configureBasePlate` / `showBasePlate` / `idle(10)` 的开场套路；
- 变速演示用 `scene.world().setKineticSpeed(select, -64)`（`:67-68`）—— **ponder 里可以直接改转速，这是展示传动逻辑的唯一手段**；
- 机器实体要 `markVirtual`（`:55`）才会走"虚拟世界"分支（对应 §2.5 的 `isVirtual()` 快进）；
- 加 `AllCreatePonderTags.CONTRAPTION_ACTOR`（`:79`）就能进 Create 的 contraption tag 页；加 `AllCreatePonderTags.ARM_TARGETS`（`:62,67`）就能进"机械臂可操作目标"页 —— **这两个 tag 白送给我们的机器曝光度**。

**JEI**（`CEI/integration/jei/category/CEIJeiPlugin.java`）：
- `@JeiPlugin` + `getPluginUid()`；
- `registerCategories` 加自己的 `RecipeCategory`；
- `registerRecipes` 里可以往**别人的**配方类型里塞条目（`:90-95` 往 Create 的 `item_application` / `mystery_conversion` 里塞 CEI 的条目）；
- `registerRecipeCatalysts` 声明"这台机器能做这类配方"（`:108-111`）。
- 复用 Create 的动画控件：`AnimatedGrindstone` / `AnimatedPrinter`（`CEI/integration/jei/category/grinding/AnimatedGrindstone.java` 等）。

**Goggle**：两类入口
1. BE 实现 `IHaveGoggleInformation#addToGoggleTooltip`（机器信息）；
2. Behaviour 也实现 `IHaveGoggleInformation`，由 BE 转发（`BlazeEnchanterBlockEntity.java:309-312`）。
3. 物品 tooltip 靠 `REGISTRATE.setTooltipModifier(...)` 挂 `KineticStats`（`CEICommon.java:50-51`）—— **应力/转速 tooltip 是工具自动加的**，前提是方块注册进了 `BlockStressValues`。

### 3.9 网络包

CEI 主包**没有自定义 payload**；唯一一个是集成包里的（`CEI/integration/apothic_enchanting/common/registry/CEIAPackets.java:26-35`）：

```java
public class CEIAPackets {
    @SubscribeEvent // on the mod event bus
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");     // 协议版本号
        registrar.playToClient(
                ContraptionEnderWovenBagPocketChangePacket.TYPE,
                ContraptionEnderWovenBagPocketChangePacket.STREAM_CODEC,
                ContraptionEnderWovenBagPocketChangePacket::handle);
    }
}
```
注册入口在 `CEI/integration/apothic_enchanting/common/CEIACommon.java:78`（`modBus.register(CEIAPackets.class)`）。客户端处理类 `EnderWovenBagClientPacketHandler`（`CEI/integration/apothic_enchanting/client/contraptions/actors/enderWovenBag/`）。

几点经验：
- 包投递是**为了 contraption 内 actor 的 UI 状态**，不是网络同步 BE（BE 用 `notifyUpdate()` 就够）。
- 客户端 handler 单独放 client 包，避免服务端加载 client 类。

### 3.10 Mixin 清单（目标类 → 注入点 → 为什么必须 mixin）

主 config（`create_enchantment_industry.mixins.json`，`priority: 1100`，`defaultRequire: 1`）：

| Mixin | 目标 | 注入 | 目的 / 为什么非 mixin 不可 | 文件:行 |
|---|---|---|---|---|
| `SmartBlockEntityMixin` | `SmartBlockEntity.destroy` | `@Inject(HEAD)` | 方块被拆时，把所有 `SmartFluidTankBehaviour`/`FluidTankBehaviour` 里的经验流体换算成经验球掉出来。`destroy()` 没有可订阅事件，且要拿 `getAllBehaviours()` | `mixin/SmartBlockEntityMixin.java:42-73` |
| `ConnectivityHandlerMixin` | `ConnectivityHandler.splitMultiAndInvalidate` | `@Inject(RETURN, ordinal=2)` + `@Inject(TAIL, @Local FluidStack dropped)` | 多块流体罐被拆开时，被"丢弃"的那部分流体要换成经验球；`@Local` 抓局部变量是唯一途径 | `mixin/ConnectivityHandlerMixin.java:36-61` |
| `CreateNBTProcessorsMixin` | `CreateNBTProcessors.clipboardProcessor` | `@Inject(HEAD)` | 复制粘贴方块时把"打印模板"从 NBT 里剥掉（防复制物品） | `mixin/CreateNBTProcessorsMixin.java` |
| `CrushingWheelControllerBlockEntityMixin` | `CrushingWheelControllerBlockEntity.tick` | `@Inject(@INVOKE Entity.hurt, shift=AFTER)` | 破碎轮碾死活物掉经验 | `mixin/CrushingWheelControllerBlockEntityMixin.java` |
| `DeployerFakePlayerMixin` | `DeployerFakePlayer.deployerKillsDoNotSpawnXP` | `@Inject(HEAD, cancellable)` | 让部署机击杀也能给经验（原逻辑明确"不生成 XP"） | `mixin/DeployerFakePlayerMixin.java` |
| `DeployerHandlerMixin` | `DeployerHandler.tryHarvestBlock` | `@Redirect(BlockState.spawnAfterBreak)` | 拦截方块破坏后的经验掉落 | `mixin/DeployerHandlerMixin.java` |
| `EnchantmentHelperMixin` | `EnchantmentHelper.getComponentType` | `@ModifyExpressionValue(ItemStack.is)` | 让"附魔模板"被原版附魔台/铁砧当可附魔物品 | `mixin/EnchantmentHelperMixin.java` |
| `FillingBySpoutMixin` | `FillingBySpout.canItemBeFilled/getRequiredAmountForItem/fillItem` | 3× `@Inject(@INVOKE GenericItemFilling..., cancellable)` | **让注液器能"用经验流体修耐久"（Mending）**；Create 的注液逻辑是硬编码的静态短路，没有扩展点 | `mixin/FillingBySpoutMixin.java` |
| `LightningBoltMixin` | `LightningBolt.tick` | `@Inject(@INVOKE clearCopperOnLightningStrike)` | 闪电给经验设备充电 | `mixin/LightningBoltMixin.java` |
| `PlayerMixin` | `Player.attack` | `@ModifyArg(fireSweepAttack)` + `@ModifyExpressionValue(isAutoSpinAttack)` | 扫击/旋风攻击的经验结算 | `mixin/PlayerMixin.java` |
| `FishingHookMixin` | `FishingHook.retrieve` | `@WrapOperation(Level.addFreshEntity)` | 钓鱼产出物替换/加经验 | `mixin/FishingHookMixin.java` |
| `accessor.CreateRecipeCategoryAccessor` | Create JEI 分类 | `@Accessor` | 读 JEI 分类内部字段 | `mixin/accessor/` |

集成 mixin：

| config | 目标 | 目的 |
|---|---|---|
| `apotheosis` | `BeltInventory.getBeltProcessingAtSegment`、`handleBeltProcessingAndCheckIfRemoved`（`@WrapMethod`/`@WrapOperation`）、`DepotBehaviour.tick` | 让"下层加工设备（lower processing appliance）"能被传送带/置物台识别 —— Create 的带子只认 `BeltProcessingBehaviour`，要插一种新设备必须改它的扫描逻辑 |
| `apotheosis` | `AffixHelper.getAffixes` | 超限（overlimit）词缀等级生效 |
| `apothic_enchanting` | `AllItemAttributeTypes.maxEnchanted`（`@Redirect Enchantment.getMaxLevel`）、`BasinBlockEntity.tick/onEmptied`、`BasinBlock.canSurvive` | 让 Create 的"最大附魔等级"过滤认识 Apothic 的扩展等级；让注魔机与 Create 搅拌盆联动 |
| `sable` | `SubLevelAssemblyHelper.moveBlocks`、`CaptureEntityBehaviour.lazyTick`、`ExperienceLanternBlockEntity.pullExp`、`ExperienceLightningCharger.findChargeableBlock/findLightningRodTarget` | 子世界（sub-level）里的坐标要换算/遍历子世界，否则装置在子世界里找不到经验球/避雷针 |
| `touhou_little_maid` | `ExperienceLanternBlockEntity`、`ExperienceLanternMovementBehaviour` | 让女仆实体也能被经验灯笼识别 |

**给我们的判据**：mixin 只在"目标行为是 `private`/`static` 硬编码、且没有事件/capability 可以插"时使用。CEI 里最典型无可替代的是 `FillingBySpoutMixin`（注液器 Mending）与 `SmartBlockEntityMixin`（拆卸掉落），其余多数是"扩展 Create 已有机器"的补丁。**我们做新机器时，几乎不需要 mixin Create**——这是它这套扩展点体系的设计目标。

### 3.11 Goggle / tooltip 三层结构（顺带总结）

| 层 | 实现 | 谁触发 |
|---|---|---|
| 物品 tooltip（应力/转速） | `REGISTRATE.setTooltipModifierFactory(... KineticStats.create(item))` | 客户端看物品 |
| 方块实体 goggle | `IHaveGoggleInformation#addToGoggleTooltip` | 戴工程师护目镜看方块 |
| Behaviour goggle | Behaviour 也实现 `IHaveGoggleInformation`，BE 转发 | 同上 |

CEI 的 goggle 文案统一走 `CEILang`（`CEI/util/CEILang.java`）+ `forGoggles(tooltip, indent)` 缩进约定，**这是 Create 系 tooltip 的排版标准**，不要自造。

---

## 4. CEI 踩过的坑与设计取舍

### 4.1 可选依赖的四层隔离（最重要的一节）

CEI 同时挂 5 个可选 mod，做法是**四层隔离**，一层都不能少：

| 层 | 做法 | 证据 |
|---|---|---|
| ① 元数据 | `mods.toml` 里 `type="optional"` + `ordering="AFTER"` | `MODS.TOML` |
| ② 类隔离 | 集成代码全部放在 `integration/<modid>/` 独立包，**主包只能通过 `ModIntegration` 枚举判断，绝不 import 集成类的类型** | `integration/ModIntegration.java:25-58` |
| ③ 注册隔离 | 集成自己的 `@Mod(ID)` 类，构造器里先判 `enabled()`，再 `modBus.register(new Common(...))`；`Common`/`Client` 都是静态内部类 | `CEIACommon.java:46-57,96-117` |
| ④ 资源/配方隔离 | 独立的 mixin config + datagen 条件（`ModLoadedCondition`）+ `.asOptional()` | `MODS.TOML`、`CEIDataMaps.java:155-194`、`CEIABlocks.java:59` |

```java
// CEI/integration/ModIntegration.java:25-58
public enum ModIntegration {
    APOTHIC_ENCHANTING(Constants.APOTHIC_ENCHANTING),
    APOTHEOSIS(Constants.APOTHEOSIS),
    TOUHOU_LITTLE_MAID(Constants.TOUHOU_LITTLE_MAID),
    SABLE(Constants.SABLE);
    public boolean enabled() { return ModList.get().isLoaded(id); }
    public ResourceLocation asResource(String path) { return ResourceLocation.fromNamespaceAndPath(id, path); }
    public ModLoadedCondition condition() { return new ModLoadedCondition(id); }   // datagen 用
}
```

```java
// CEI/integration/apothic_enchanting/common/CEIACommon.java:46-57
@Mod(CEICommon.ID)                                  // 同一个 modId，第二个 @Mod 类
public class CEIACommon {
    public static final CDPRegistrate REGISTRATE = CEICommon.REGISTRATE;   // 共用 registrate
    public CEIACommon(IEventBus modBus, ModContainer modContainer) {
        if (ModIntegration.APOTHIC_ENCHANTING.enabled()) {
            modBus.register(new Common(modBus, modContainer));
            if (FMLLoader.getDist() == Dist.CLIENT) modBus.register(new Client());
        }
    }
    public static class Common { @SubscribeEvent public void construct(final FMLConstructModEvent event) { CEIABlocks.register(modBus); ... } }
}
```

**关键设计**：NeoForge 允许同一个 modId 有多个 `@Mod` 类。CEI 用第二个 `@Mod` 类做"集成引导"，把集成注册推迟到 `FMLConstructModEvent`（`:69`）——这样**即使集成代码被类加载，也只加载 `CEIACommon` 这一个类**，`CEIABlocks`/`CEIAItems` 等真正碰 Apothic 类型的类只有 `enabled()` 为真时才会被触达。这是"无该 mod 时安全加载"的工业级做法。

⚠ 注意：`if (ModIntegration.APOTHIC_ENCHANTING.enabled())` 判断发生在**构造器**，而 `FMLLoader.getDist()` 判断插在中间——这类"半静态"判断逻辑很容易在不同加载顺序下出错，我们自己写时尽量把判定集中在一个地方并加日志。

### 4.2 Datagen 里的 optional 条件

三种写法，CEI 都用到了：

```java
// (1) data map 条目级条件 —— 直接给条目挂 ModLoadedCondition
// CEI/common/registry/CEIDataMaps.java:155-166
provider.builder(EXPERIENCE_FUEL)
        .add(CEIBlocks.SUPER_EXPERIENCE_BLOCK.getId(), ExperienceFuel.special(27), false)
        .add(ResourceLocation.fromNamespaceAndPath("create_sa", "heap_of_experience"),
                ExperienceFuel.normal(12), false,
                new ModLoadedCondition("create_sa"))
        .add(ResourceLocation.fromNamespaceAndPath("ars_nouveau", "experience_gem"),
                ExperienceFuel.normal(3), false,
                new ModLoadedCondition("ars_nouveau"));
```

```java
// (2) 配方级条件 —— builder 上的 whenModLoaded
// CEI/data/CEIRecipeProvider.java:218-227
GrindingRecipe.builder(CEICommon.asResource("create_sa/heap_of_experience"))
        .whenModLoaded("create_sa")
        .require(IntegrationIngredient.of("create_sa", "heap_of_experience"))
        .output(EXPERIENCE.get(), 12)
        .build(output);
```

```java
// (3) 配置开关级条件 —— 包一层 RecipeOutput，把 ICondition 塞进每个 accept
// CEI/data/CEIRecipeProvider.java:240-256
private static RecipeOutput withClassicBlazeEnchanterCondition(RecipeOutput output) {
    return new RecipeOutput() {
        @Override public Advancement.Builder advancement() { return output.advancement(); }
        @Override public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
            var feature = CEIConfig.features().classicBlazeEnchanter;
            var merged = new ICondition[conditions.length + 1];
            merged[0] = feature;
            System.arraycopy(conditions, 0, merged, 1, conditions.length);
            output.accept(id, recipe, advancement, merged);
        }
    };
}
```

第 (3) 种很聪明：**一个 `RecipeOutput` 装饰器 = 所有配方自动带同一个条件**，不用每条都写 `.withCondition(...)`（CEI 也确实两条路都用：`CEIRecipeProvider.java:157` 手工 `.withCondition(...)`）。

**经验**：条件用的物品引用必须是"字符串 id 化"的（`IntegrationIngredient.of("modid","path")` / `ResourceLocation.fromNamespaceAndPath`），**绝不能在 datagen 里 `new ItemStack(OtherModRegistry.ITEM.get())`**，否则 datagen 环境缺该 mod 会直接崩。

### 4.3 方块被 contraption 移动 / 破坏时的物品与数据保留

四个层面，CEI 分别给了方案：

| 场景 | 方案 | 证据 |
|---|---|---|
| 玩家用方块物品直接放置成"另一个方块" | 覆写 `BlockItem#useOn` + 自定义 `BlockPlaceContext#getPlacementState` | `MechanicalGrindStoneItem.java:49-82` |
| 剪贴板/结构复制需要知道"这台机器由哪些物品构成" | 实现 `SpecialBlockItemRequirement#getRequiredItems` 返回 `ItemRequirement` | `GrindstoneDrainBlock.java:158-162` |
| 创造模式中键取方块 | 覆写 `getCloneItemStack`（这里返回 Item Drain，因为本体是"改造过"的） | `GrindstoneDrainBlock.java:137-140` |
| 掉落物带着 BE 数据（如袋中实体） | loot table 里 `CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY).include(DATA_COMPONENT)` | `CEIABlocks.java:84-91` |
| BE 被拆 | 覆写 `destroy()` 手动 `Containers.dropItemStack` / `ItemHelper.dropContents` | `GrindstoneDrainBlockEntity.java:335-340`、`BlazeForgerBlockEntity.java:156-162` |
| 想"不拆方块直接换方块"（变成普通火焰） | `serverLevel.destroyBlock(pos,false)` + `setBlockAndUpdate` + `setRemoved()` | `BlazeEnchanterBlockEntity.java:209-215` |
| 多块结构被拆 | 必须 mixin `ConnectivityHandler.splitMultiAndInvalidate`（§3.10） | `ConnectivityHandlerMixin.java` |

另外 `BlazeEnchanterBlock.onRemove` 显式调 `IBE.onRemove(state, level, pos, newState)`（`BlazeEnchanterBlock.java:80-83`）——**自定义 BE 的方块一定要有这一句**，否则破方块时 BE 不会被正确移除（幽灵 BE、复制机常见根因）。

### 4.4 经验 / 流体：换算、耐久与容器

- **1 mB = 1 XP**，但**兼容其它 mod 的经验流体**（`ExperienceHelper.java:63-86`）：

```java
public static int getExperienceFromFluid(FluidStack fluid) {
    if (fluid.isEmpty()) return 0;
    if (fluid.is(CEIFluids.EXPERIENCE)) return fluid.getAmount();       // 自家：1:1
    int amount = fluid.getAmount();
    Integer unit = fluid.getFluidHolder().getData(CEIDataMaps.FLUID_UNIT_EXPERIENCE);  // 别人：查 data map 的"多少 mB = 1 XP"
    if (unit == null) return 0;
    return amount / unit;
}
```
  data map 里预置了 `cofh_core/experience=25`、`cyclic/xpjuice=20`、`enderio/xpjuice=20`、`industrialforegoing/essence=20`、`pneumaticcraft/memory_essence=20` 等（`CEIDataMaps.java:167-194`）。**这是处理"公共资源型流体"的标准手法：自家 1:1，别人用 data map 标定换算率**，我们做"星球能量流体"时可以照抄。
- **耐久修补**走 Mending 语义：`repairItem()` 用 `EnchantmentHelper.modifyDurabilityToRepairFromXp` + `stack.getXpRepairRatio()`（`:107-116`），`repairPlayerItems` 递归找可修装备（`:118-128`）。AT 打开了 `ExperienceOrb.repairPlayerItems`（§1.6）正是为了这条链路。
- **经验球与流体互转**：`orb.value`（AT 后 public）+ `new FluidStack(CEIFluids.EXPERIENCE, amount)`（`ExperienceLanternMovementBehaviour.java:93-108`）；反向用 `ExperienceHelper.award(amount, serverPlayer)`（`MechanicalGrindstoneBlock.java:130`）。
- **容器容量别硬编码**：`CEIConfig.fluids().printerFluidCapacity`（`CEIFluidsConfig.java:28-31`，带 `RequiresRestart.SERVER.asComment()`）——**容量这类字段要标"需重启"**，否则玩家改了配置以为立即生效。
- **罐装不下就别收料**：`ProcessingInventory.insertItem` 先算 `tank.getPrimaryHandler().getSpace()` 再决定收不收（`GrindstoneDrainBlockEntity.java:83-89`）。不做这个检查会出现"物品被吞、经验丢失"的经典 bug。

### 4.5 双 Apotheosis / Apothic 集成的取舍

`apotheosis` 8.x 把附魔部分拆成了独立 mod `apothic_enchanting`，CEI 同时支持两者：

- 两套独立包（`integration/apotheosis/`、`integration/apothic_enchanting/`），各自有 `@Mod` 引导类、registrate 资源、mixin config、config、data provider；
- 主包只在两件事上感知它们：`CEICommon.java:56-59` 的 fluids/creative tab 条件注册，以及 `EnchantmentProcessingRules.warnLegacyDataMaps`；
- `integration/sable_apotheosis/` 是"两者都装才有意义"的第三块（`CEISAXCommon.java`），**用第二层组合条件隔离**。

**教训**：可选集成一旦超过 2 个，包结构必须"一个 mod 一个包 + 一个 `@Mod` 引导 + 一个 mixin config + 一个 config 类"，否则必然变成 `if (loaded) {...}` 的意大利面。

### 4.6 弃用与兼容：data map 双轨 + 启动告警

CEI 有一整套"旧数据格式仍生效但会警告"的机制：

```java
// CEI/common/processing/EnchantmentProcessingRules.java:164-191
private static Integer legacyLevelExtension(Holder<Enchantment> enchantment) {
    Integer legacy = enchantment.getData(CEIDataMaps.SUPER_ENCHANTING_LEVEL_EXTENSION);
    if (legacy != null) {
        warnLegacyLevelExtension(1);
        // TODO Remove this legacy data map fallback after modpacks have migrated to enchantment_processing/rules.
        return legacy;
    }
    return null;
}
```

- 三个 `warnedLegacy*` 布尔保证**每种警告只打一次**（`:37-39,193-215`）；
- `ServerStartedEvent` 里统计一遍并批量告警（`CEICommon.java:74,80-82`）；
- 新机制（`ENCHANTMENT_PROCESSING_RULES`）与旧机制（三个独立 data map）并存，`getData` 优先新、回退旧、再回退配置默认值（`:62-99`）。

**这是"我们改过数据结构、整合包还没迁"时的标准三段落**：新路径优先 → 旧路径兼容 + 告警 → 配置兜底。我们自己早建这套，后期能省下大量 issue。

### 4.7 其它零散坑（都在注释/代码里）

| 坑 | 表现 | 证据 |
|---|---|---|
| 槽位 `3000` 约定 | 见 §2.3 | `GrindstoneDrainBlockEntity.java:93` |
| 客户端 `read` 时 `level` 可能为 null | 必须先判再 `update(...)` | `BlazeEnchanterBlockEntity.java:156,258-261` |
| `isVirtual()` 分支必须写 | 否则 ponder/contraption 里机器不工作或双倍扣料 | `BlazeEnchanterBlockEntity.java:174-190`、`PrinterBlockEntity.java:108-110` |
| `writeSafe` 不能写 ItemStack | 剪贴板复制会复制出物品 | `EnchanterBehaviour.java:174-182` |
| 扳手/机械臂点击要先排除 | 否则被 Create 的拆机逻辑吞掉 | `EnchanterBehaviour.java:154-157` |
| capability 要按面判方向 | 否则物品/流体从错面进出 | `GrindstoneDrainBlockEntity.java:114-124` |
| 带 `ItemStack` 字段的 BE 要 `destroy()` 掉落 | 否则拆机吞物品 | `BlazeEnchanterBlockEntity.java:132-138` |
| `@Local` 抓局部变量 | 拆多块容器要拿"被丢弃的 FluidStack" | `ConnectivityHandlerMixin.java:52` |
| 创造模式罐要判 `CreativeSmartFluidTank` | 否则无限流体被换成经验球刷屏 | `ConnectivityHandlerMixin.java:44,57` |
| 物品 tooltip 缺 `KineticStats` | 玩家看不到应力/转速 | `CEICommon.java:50-51` |
| ponder 注册要先判 datagen | datagen 环境 `getHolderLookupProvider()` 为 null | `CEIPonderScenes.java:82-89` |

---

## 5. 对我们的可复用结论（北极星星球附属）

我们要做三样：**应力源（发电机）、应力消耗（加工机）、传动元件（齿轮/轴/离合）**。CEI 能给的直接帮助不均等：

- **应力消耗**：CEI 有两台完整范例（磨石排水口、黄铜书架）→ **高价值，可整套抄**；
- **应力源**：CEI 一台都没有 → 只能从 Create 自身学（`GeneratingKineticBlockEntity`）；
- **传动元件**：CEI 一台都没有（它连齿轮都没加）→ 学 Create 的 `IRotate` + `RotatedPillarKineticBlock`/`HorizontalKineticBlock`，CEI 只能提供"注册与渲染"的周边范式。

### 5.1 应力源（发电机）推荐写法

```java
// 1) BE：继承 GeneratingKineticBlockEntity
public class StarCoreBlockEntity extends GeneratingKineticBlockEntity {
    public StarCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

    @Override
    public float getGeneratedSpeed() {
        // 返回 RPM（正负表示方向）；应力容量不在这里，走 BlockStressValues.CAPACITIES
        return CEIConfig-featureEnabled ? 64f : 0f;   // 建议：不满足条件时返回 0，网络自动停转
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);              // 必须
    }

    @Override
    public void tick() {
        super.tick();                                 // GeneratingKineticBlockEntity 会处理 source 维护
    }
}
```

```java
// 2) 注册：容量 + RPM tooltip
public static final BlockEntry<StarCoreBlock> STAR_CORE = REGISTRATE
        .block("star_core", StarCoreBlock::new)
        .initialProperties(SharedProperties::netheriteMetal)
        .transform(pickaxeOnly())
        .onRegister(block -> BlockStressValues.CAPACITIES.register(block, () -> 256.0))  // 256 SU @1RPM
        .onRegister(BlockStressValues.setGeneratorSpeed(64, true))                       // tooltip: 64 RPM，可能更低
        .transform(BlockStressValues-或自己配置的 setCapacity)
        .blockstate(BlockStateGen.axisBlockProvider(false))
        .item(...).build().register();
```

要点与陷阱：

1. **容量单位是"1 RPM 下的 SU"**，实际容量 = `CAPACITY × |generatedSpeed|`（`KineticNetwork.java:165-167`）。写 `256` + `64 RPM` → 实际 16384 SU，和 Create 创造马达同量级（`AllBlocks.java:557-570`：16384 + `setGeneratorSpeed(256,true)`）。
2. `setGeneratorSpeed` **只影响 tooltip**（`BlockStressValues.java:19-22` 注释明说 "only used for tooltips; actual functionality is determined by the block"）。**真正的转速来自 `getGeneratedSpeed()`，两处必须一致**，否则玩家看到的和实际不符。
3. 转速可变（例如随"星球日照"变化）时，改完 `getGeneratedSpeed()` 要调 `updateGeneratedRotation()`（`GeneratingKineticBlockEntity.java:89`），它会顺带 `notifyStressCapacityChange(...)`（`:109`）。
4. 想让"没燃料就停"，`getGeneratedSpeed()` 返回 `0` 即可，不要自己去 `setSpeed`。
5. **不要碰 `CStress`**（抛异常，`CStress.java:89-93`）。要么常量 `register`，要么抄一份 `StressConfig`（§2.2 的 CDP 实现，30 行就能移植到自己项目，注意 `@CodeReference` 注明出处）。
6. 若源有多个变体方块都要吃同一个配置项，覆写 `getStressConfigKey()` 返回代表方块（`KineticBlockEntity.java:175-177`）。

### 5.2 应力消耗 / 传动件：抄什么

**必抄（按优先级）**：

| # | 抄什么 | 为什么 | 出处 |
|---|---|---|---|
| 1 | 应力值走"自己的配置 + `BlockStressValues.registerProvider`" | 玩家能调、tooltip 正确、不触发 CStress 抛错 | §2.2 |
| 2 | 有"加工时间"的机器用 `ProcessingInventory` + `remainingTime/recipeDuration/appliedRecipe` 三件套 | Create 的通用进度语义，ponder/JEI/renderer 全都按这套读 | `GrindstoneDrainBlockEntity.java:81-100,365-387` |
| 3 | 转速→速率映射 `Mth.clamp(speed / 24, 1, 128)` | 有下限不卡死、有上限不瞬发 | `GrindstoneDrainBlockEntity.java:371` |
| 4 | 输出三重降级：带漏斗 → 相邻 `DirectBeltInputBehaviour` → 弹出物品 | 不丢货，自动接入 Create 物流 | `:394-449` |
| 5 | 用 `IRotate#getMinimumRequiredSpeedLevel()` + `isSpeedRequirementFulfilled()` 做速度门控 | 白拿 tooltip 速度条与 UI 提示 | `BrassBookshelfBlockEntity.java:66-80`、`IRotate.java:19-23,148-150` |
| 6 | capability 按面暴露（`getItemHandler(Direction)` / `getFluidHandler(Direction)`）+ `RegisterCapabilitiesEvent` | 管道/漏斗自动接入，无需额外适配 | §2.3(c) |
| 7 | BE 里 `addBehaviours` 先 `super`，再按需装 `SmartFluidTankBehaviour` / `DirectBeltInputBehaviour` / `BeltProcessingBehaviour` / `ScrollValueBehaviour` | 这是 Create 机器的"标准件" | §3.6 |
| 8 | 方块 `onRemove` 调 `IBE.onRemove(...)` | 防幽灵 BE | `BlazeEnchanterBlock.java:80-83` |
| 9 | 拆机掉落（`destroy()`）与 `writeSafe` 不带 ItemStack | 防吞物品 / 防复制 | §4.3 |
| 10 | Ponder 场景 + `AllCreatePonderTags.*` tag + JEI 分类 + catalyst | 曝光度与可玩性，成本极低 | §3.8 |

**渲染选型决策表**（照 CEI 的实际选择归纳）：

| 元件形态 | 选什么 | 例子 |
|---|---|---|
| 绕单轴转、无额外状态 | `.visual(() -> SingleAxisRotatingVisual.of(PARTIAL), false)`，不写 renderer | 磨石轮 `CEIBlockEntities.java:49-54` |
| 绕单轴转 + 要画物品/流体 | visual(`false`) **或** visual(`true`) + 自定义 `KineticBlockEntityRenderer`；若渲染的是**另一个方块**，覆写 `getRenderedBlockState` | 磨石排水口 `GrindstoneDrainRenderer.java:48-59` |
| 要画复杂动态（火焰/多部件/连击） | 手写 Flywheel visual（`AbstractBlockEntityVisual` + `SimpleDynamicVisual`），必要时 `@CodeReference` 抄 Create 的 | `ClassicBlazeEnchanterVisual.java:51-53` |
| 只是静态模型 + 一点贴片 | 纯 BER（`SmartBlockEntityRenderer` 或自定义） | `EXPERIENCE_HATCH` `CEIBlockEntities.java:61-65` |
| 需要"物品在机器里动" | BER 手绘（`renderItems`），进度/偏移从 BE 读 | `GrindstoneDrainRenderer.java:61-115` |

**传动元件（齿轮/轴/离合）额外建议**：CEI 没做，但 Create 的 `RotatedPillarKineticBlock` + `getRotationSpeedModifier(Direction)`（`IRotate`）就是完整的传动比/反向机制。我们的"星球传动件"如果只是"轴/齿轮"，直接 `extends RotatedPillarKineticBlock` + `SingleAxisRotatingVisual.of(AllPartialModels.SHAFT)` 之类的现成 partial 就能起量；**应力值给 0**（Create 的传动件本身不消耗应力，只有 `Cogwheel` 例外给极小值，⚠ 需查 `AllBlocks` 确认齿轮是否登记应力）。

### 5.3 不要抄什么，为什么

| 不要抄 | 原因 |
|---|---|
| **依赖 `create_dragons_plus`** | CEI 是 CDP 的"亲儿子"，我们不是。上面 90% 的甜头（`CDPRegistrate`、`StressConfig`、`ConfigurableFluidTank`、`AdvancementBehaviour`、`RecipeTypeInfo`、`IntegrationIngredient`、`@CodeReference`、`armInteractionPoint`）都要多一个硬依赖。**能 30 行自写的就自写**（`StressConfig` 就是），Create 原生 `CreateRegistrate` 已经覆盖注册需求。 |
| **空体 `register(modBus)` 强制初始化** | `CEIBlocks.java:187` 这种写法可读性极差，我们改成 `CEIBlocks.init()` 并在主类里按顺序调，配注释说明"顺序敏感"。 |
| **`slot == 3000` 魔法槽位** | 语义藏在一个 int 里，跨类复用必出 bug。我们封 `extractAll()` 方法。 |
| **在主包里堆 11 个 mixin** | 我们做新机器**不需要** mixin Create。只有"改 Create 已有机器行为"才需要，且必须独立 config + `defaultRequire` 考虑。 |
| **双轨 data map + 弃用回退**（一开始就上） | 那是**有存量玩家**的 mod 才付的税。我们新 mod 直接用唯一新格式，等真要改结构时再引入这套三段式。 |
| **`ModIntegration.enabled()` 散落在各处** | CEI 在 `CEICommon:56`、`CEIACommon:52`、`CEIDataMaps:155+`、`CEIRecipeProvider:219+` 到处判。我们集中到一处静态初始化（"能力探测"一次，缓存 boolean），diff 更好读。 |
| **用 `CEIConfig` 那样深三层嵌套 + 全静态 getter** | 可用但读起来跳来跳去（`CEIConfig.java:64-86` 有 8 个转发方法）。我们用 2 层（`server().stress`、`server().fluids`）就够。 |
| **复制 `BlazeBurnerVisual` 级别的复杂 visual** | 除非真的需要 blaze 火焰，否则用 `SingleAxisRotatingVisual` + BER，省一个 Flywheel 版本适配负担。 |

### 5.4 落地顺序 checklist（照 CEI 的骨架顺序改）

1. `MyRegistrate = CreateRegistrate.create(MODID).setTooltipModifierFactory(ItemDescription + KineticStats)`（照 `Create.java:91-96`）。
2. `MyStressConfig extends ConfigBase`（自己写 30 行，抄 CDP `StressConfig`，注明出处）+ 放进 `ServerConfig.nested(...)`。
3. `MyBlocks` 静态注册所有方块，**应力值在 builder 上 `.transform(myStress().setImpact/setCapacity(x))`**。
4. `MyBlockEntities`：`blockEntity(name, MyBE::new).visual(...).renderer(...).validBlock(...)`；**同一个类里用 `@SubscribeEvent registerCapabilities`**（照 `CEIBlockEntities.java:94-114`）。
5. `MyBlockEntities.register(modBus)` 里 `modBus.register(MyBlockEntities.class)`。
6. `MyItems` / `MyCreativeTab` / **`MyRecipes`（`RecipeTypeInfo` 自写或用 `DeferredRegister`）**。
7. 主类构造器顺序：`REGISTRATE.registerEventListeners` → 方块 → 方块实体 → 物品 → 创造标签 → 配方 → **配置最后**。
8. `client/@Mod(dist=CLIENT)`：构造期注册 `PartialModels`；`FMLClientSetupEvent` 里 `PonderIndex.addPlugin(...)`。
9. `data/@Mod`：`DatagenModLoader.isRunningDataGen()` 门禁 + `GatherDataEvent` 挂 recipe provider。
10. 可选集成：`ModIntegration` 枚举 + `integration/<modid>/` 包 + 第二个 `@Mod` 引导 + 独立 mixin config（**第一版可以先不做**）。
11. 最后补：ponder 场景（先做 2 个：应力源启动、消耗机加工）、JEI 分类（能省则省，先用 `registerRecipeCatalysts`）。

### 5.5 仍需验证的清单（⚠）

| # | 待验证项 | 建议验证方式 |
|---|---|---|
| 1 | CDP `BlazeBlockEntity` 的父类与生命周期（是否与 `BlazeBurnerBlockEntity` 同源） | 取 `create_dragons_plus-1.11.9` jar 反编译 `plus/dragons/createdragonsplus/common/processing/blaze/BlazeBlockEntity` |
| 2 | CDP `armInteractionPoint(...)` 的实际注册目标与是否自动 `byBlock` | 同上，看 `CDPRegistrate` |
| 3 | `META-INF/accesstransformer.cfg` 在 NeoForge 21.1 是否默认自动生效 | 拿一个最小附属打 jar，不写 `[[accessTransformers]]`，AT 一个 field 试 `javap`/运行时访问 |
| 4 | Create 的齿轮（`CogwheelBlock`）是否登记了应力消耗 | 查 `CR/AllBlocks.java` 中 `COGWHEEL` 段 |
| 5 | `IRotate#getRotationSpeedModifier` 在 6.0.10 的确切签名与默认实现 | 读 `CR/content/kinetics/base/IRotate.java` |
| 6 | CEI 的 `IntegrationIngredient`（CDP）与 Create 原生 `Ingredient` 在"物品不存在时"的行为差异 | 反编译 CDP `data/recipe/integration/IntegrationIngredient` |
| 7 | Registrate `asOptional()` 在 CreateRegistrate 下是否可用 | 读 `AbstractRegistrate`/`AbstractBuilder` 或直接试编译 |

---

## 附录 A：CEI 用到的 Create 类速查（按包）

| 包 | 类（CEI 实际引用） |
|---|---|
| `api.behaviour.movement` | `MovementBehaviour`（`REGISTRY`） |
| `api.behaviour.goggles` | `IHaveGoggleInformation` |
| `api.contraption.storage.fluid` | `MountedFluidStorageType`（`mountedFluidStorage` 工具） |
| `api.equipment.goggles` | `IHaveGoggleInformation`（同上，6.x 新位置） |
| `api.stress` | `BlockStressValues`（`IMPACTS`/`CAPACITIES`/`RPM`/`setGeneratorSpeed`） |
| `api.registry` | `SimpleRegistry`、`CreateRegistries`、`CreateBuiltInRegistries` |
| `api.effect` | `OpenPipeEffectHandler` |
| `api.schematic.requirement` | `SpecialBlockItemRequirement` |
| `content.kinetics.base` | `KineticBlockEntity`、`RotatedPillarKineticBlock`、`HorizontalKineticBlock`、`KineticBlockEntityRenderer`、`SingleAxisRotatingVisual`、`IRotate` |
| `content.kinetics.belt.behaviour` | `DirectBeltInputBehaviour`、`BeltProcessingBehaviour`、`TransportedItemStackHandlerBehaviour` |
| `content.kinetics.belt.transport` | `TransportedItemStack` |
| `content.kinetics.mechanicalArm` | `ArmBlockEntity`、`ArmInteractionPoint`、`ArmInteractionPointType` |
| `content.processing.burner` | `BlazeBurnerBlock.HeatLevel`、`BlazeBurnerRenderer`、`BlazeBurnerVisual`、`ScrollInstance` |
| `content.processing.recipe` | `ProcessingRecipeParams`、`StandardProcessingRecipe`、`ProcessingInventory` |
| `content.processing.sequenced` | `SequencedAssemblyRecipe`、`IAssemblyRecipe` |
| `content.processing` | `AssemblyOperatorBlockItem` |
| `content.equipment.sandPaper` | `SandPaperItem`、`SandPaperPolishingRecipe` |
| `content.kinetics.deployer` | `ManualApplicationRecipe` |
| `content.materials` | `ExperienceBlock`、`ExperienceNuggetItem` |
| `content.fluids.pipes` | `FluidPipeBlock`（放置朝向提示） |
| `content.fluids.tank` | `CreativeFluidTankBlockEntity.CreativeSmartFluidTank` |
| `foundation.blockEntity` | `SmartBlockEntity`、`CachedRenderBBBlockEntity`、`IMultiBlockEntityContainer` |
| `foundation.blockEntity.behaviour` | `BlockEntityBehaviour`、`BehaviourType`、`ValueBoxTransform`、`ValueSettingsBoard`、`ValueSettingsFormatter`、`CenteredSideValueBoxTransform` |
| `foundation.blockEntity.behaviour.fluid` | `SmartFluidTankBehaviour` |
| `foundation.blockEntity.behaviour.scrollValue` | `ScrollValueBehaviour` |
| `foundation.blockEntity.renderer` | `SmartBlockEntityRenderer` |
| `foundation.block` | `IBE` |
| `foundation.data` | `CreateRegistrate`、`BlockStateGen`、`AssetLookup`、`SharedProperties`、`ModelGen`、`TagGen`、`BuilderTransformers` |
| `foundation.fluid` | `SmartFluidTank`、`FluidHelper` |
| `foundation.item` | `ItemDescription`、`KineticStats`、`TooltipModifier`、`ItemHelper` |
| `foundation.utility` | `CreateLang` |
| `foundation.data.recipe` | `CreateRecipeBuilders`（pressing/filling/...）、`CommonMetal` |
| `infrastructure.config` | `CStress`（**禁用**）、`AllConfigs` |
| `infrastructure.ponder` | `AllCreatePonderTags`（`ARM_TARGETS`/`CONTRAPTION_ACTOR`） |
| `foundation.ponder` | `CreateSceneBuilder` |

## 附录 B：CEI 自己的工具类（可直接照搬的"小工具"）

| 类 | 作用 | 是否值得抄 |
|---|---|---|
| `util/CEILang.java` | 统一 i18n 入口（`translate/item/number/description/builder`） | 值得，Create 系 tooltip 排版基础 |
| `util/CEIStreamCodecs.java` / `CEICodecs.java` | 常用 codec 组合子 | 按需 |
| `util/CEIAdvancement.java` | 成就/触发器辅助 | 按需 |
| `util/CEIIntIntPair.java` | `Codec` 化的 int 对（data map 用） | 值得（写 data map 常见需求） |
| `util/CEIDyeFluids.java` | 16 色染料流体的 tag 生成 | 我们不做染料就不抄 |
| `common/processing/EnchantmentProcessingRules.java` | data map + 配置 + 弃用回退的"三段落" | **值得**（资源型数据的解析层范式） |
| `config/CEIConfig.java` | 三层 config 装配 + `ModConfigEvent` 分发 | 值得（但简化到 2 层） |

---

**结语**：CEI 2.5.4b 对"如何扩展 Create"给出的最有价值的三条经验是——

1. **动力学接入的正确姿势是"配置化应力 + 原生 KineticBlockEntity 家族 + Behaviour 装配"**，而不是自己搓一套应力系统；`CStress` 对附属关门这件事，逼出了 `BlockStressValues.IMPACTS.register / registerProvider` 这条正路（§2.2）。
2. **加工机器 = `ProcessingInventory` 三件套 + `Behaviour` 能力组合 + 输出三重降级**，逻辑写在 `tick()`，不要自造进度/输出系统（§2.3、§2.7）。
3. **可选集成必须四层隔离**（元数据 / 包 / `@Mod` 引导 / 资源条件），并且**能用扩展点就别 mixin**；CEI 那 11 个 mixin 全是"改 Create 已有机器"的补丁，我们做新机器时一个都不需要（§3.10、§4.1）。
