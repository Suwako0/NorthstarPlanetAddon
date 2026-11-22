# Create 6.0.10 附属开发技术分析：注册 / 渲染（Flywheel 1.0）/ Ponder / UI

> 分析对象：**Create 6.0.10**（MC 1.21.1 / NeoForge）+ **Create Enchantment Industry 2.5.4b**（下称 CEI）
> 证据来源：本仓库内已解包的源码，只读。
>
> **引用约定**：本文所有 `路径:行号` 均以 `.research/src/` 为根。
> — Create 侧路径省略前缀 `create/com/simibubi/create/`，记为 `…/content/kinetics/base/RotatingInstance.java:63`
> — CEI 侧省略前缀 `cei/plus/dragons/createenchantmentindustry/`，记为 `…/common/registry/CEIBlocks.java:64`
> 凡未在本仓库源码中出现、需要跨 jar 才能确认的结论，标注 **⚠ 待验证**。

---

## 0. 结论速查（先看这 10 条）

| # | 结论 | 证据 |
|---|---|---|
| 1 | **不能在 addon 里用 `Create.registrate()`**：它用 `StackWalker` 检查调用方包名，非 `com.simibubi.create` 直接抛 `UnsupportedOperationException` | `…/Create.java:213-217` |
| 2 | addon 正确做法：自己 `CreateRegistrate.create(modid)`（每个 modid 只能一次） | `…/foundation/data/CreateRegistrate.java:74-81`；`jetpack-github/CreateJetpack-5.2.1/…/Constants.java:13` |
| 3 | **不能用 `CStress.setImpact/setCapacity`**：内部 `assertFromCreate` 对非 Create 方块抛 `IllegalStateException` | `…/infrastructure/config/CStress.java:89-93` |
| 4 | addon 注册应力的官方 API 是 `BlockStressValues.IMPACTS/CAPACITIES.register(block, supplier)`；直接注册优先级高于 config provider | `…/api/stress/BlockStressValues.java:10-32`；`…/api/registry/SimpleRegistry.java:30-34` |
| 5 | Flywheel visual 只需在 `.blockEntity(...)` 上写 `.visual(() -> Factory, renderNormally)`，注册时机由 Create 内部挂在 `FMLClientSetupEvent` | `…/foundation/data/CreateBlockEntityBuilder.java:81-116` |
| 6 | `KineticBlockEntity.read()` 在 `clientPacket` 时自动 `VisualizationHelper.queueUpdate(this)`，**转速变化本身不需要你手动刷新 visual** | `…/content/kinetics/base/KineticBlockEntity.java:277-279` |
| 7 | `RotatingInstance.rotationalSpeed` 单位是 **度/秒**，由 `speed * SPEED_MULTIPLIER(=6)` 得出；角度累加由 Flywheel 着色器完成 | `…/content/kinetics/base/RotatingInstance.java:15,26,63-69` |
| 8 | Ponder 唯一入口是 `PonderIndex.addPlugin(new XxxPonderPlugin())`，在 `FMLClientSetupEvent` 里调用 | `…/client/CEIClient.java:42-44`；`…/foundation/ponder/CreatePonderPlugin.java:20-59` |
| 9 | `ValueBox`/`ScrollValueBehaviour` 的渲染与网络**全自动**：`ScrollValueRenderer.tick()` 已在 Create 的 client tick 里，`ValueSettingsInputHandler` 拦截全局右键 | `…/foundation/events/ClientEvents.java:166`；`…/foundation/blockEntity/behaviour/ValueSettingsInputHandler.java:27-95` |
| 10 | registrate 自己产出 blockstate/model/item model/lang/loot 的 datagen provider；mod 只需额外注册 recipe / advancement / tag / 动态注册表 | `…/data/CEIData.java:34-59` |

---

## 1. 注册层：CreateRegistrate 与 6.0.2+ 的封锁

### 1.1 为什么 addon 必须自建 registrate

`Create` 持有一个私有 `CreateRegistrate`，并且**显式禁止**外部使用：

```java
// …/Create.java:85-96
/**
 * <b>Other mods should not use this field!</b> If you are an addon developer, create your own instance of
 * {@link CreateRegistrate}.
 * ...
 */
private static final CreateRegistrate REGISTRATE = CreateRegistrate.create(ID)
	.defaultCreativeTab((ResourceKey<CreativeModeTab>) null)
	.setTooltipModifierFactory(item -> ... );
```

```java
// …/Create.java:213-217
public static CreateRegistrate registrate() {
	if (!STACK_WALKER.getCallerClass().getPackageName().startsWith("com.simibubi.create"))
		throw new UnsupportedOperationException("Other mods are not permitted to use create's registrate instance.");
	return REGISTRATE;
}
```

配套的替代 API 有两个：

1. **`CreateRegistrate.create(String modid)`**（`…/foundation/data/CreateRegistrate.java:74-81`）—— 构造并**自动向 `CreateRegistrateRegistrationCallback` 注册**这个实例，从而让 Create 那些"按 id 延迟绑定"的 API 生效。
2. **`CreateRegistrateRegistrationCallback.register(registry, id, callback)`**（`…/api/registrate/CreateRegistrateRegistrationCallback.java:14-17`）—— 当你想在"某个注册表里的某个 id 被注册"时执行回调，而不需要拿到 Create 的 registrate 实例。

```java
// …/api/registrate/CreateRegistrateRegistrationCallback.java:19-26
/**
 * Provide a {@link CreateRegistrate} instance to be used by the API.
 * Instances created by {@link CreateRegistrate#create(String)} will automatically be registered.
 * It is illegal to call this method more than once for the same mod ID.
 */
public static void provideRegistrate(CreateRegistrate registrate) { ... }
```

注意 `CreateRegistrate` 的构造器是 `protected`（`…/foundation/data/CreateRegistrate.java:70-72`），所以外部只能：

- 用静态工厂 `CreateRegistrate.create(modid)`；或
- 继承 `CreateRegistrate` 写自己的子类（CEI 走的就是这条）。

### 1.2 CEI 的实际做法（对照）

```java
// …/common/CEICommon.java:46-54
@Mod(CEICommon.ID)
public class CEICommon {
    public static final String ID = "create_enchantment_industry";
    public static final CDPRegistrate REGISTRATE = new CDPRegistrate(ID)
            .setTooltipModifierFactory(item -> new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                    .andThen(TooltipModifier.mapNull(KineticStats.create(item))));

    public CEICommon(IEventBus modBus, ModContainer modContainer) {
        REGISTRATE.registerEventListeners(modBus);
        ...
```

- CEI 用的是 `plus.dragons.createdragonsplus.common.CDPRegistrate`（来自 Create: Dragons Plus，**源码不在本仓库**）。它是 `CreateRegistrate` 的子类，额外提供了 `registerBuiltinLocalization`、`registerPonderLocalization`、`customStat`、`StressConfig` 等 helper。**⚠ 待验证**：`CDPRegistrate` 的确切父类与是否在构造器内调用 `provideRegistrate`（`CreateRegistrate` 的注释明确指出"子类必须自己把 registrate 交给 callback API"，`…/foundation/data/CreateRegistrate.java:76-80`）。
- CEI 自己**没有**调用 `provideRegistrate`，却大量使用 `REGISTRATE.registerPonderLocalization(...)`、`blockEntity(...).visual(...)` 等 Create 侧扩展，说明 `CDPRegistrate` 内部已代为处理（⚠ 待验证）。

**给出 addon 的两条可行路线**：

```java
// 路线 A（推荐，零外部依赖）：直接用 Create 的工厂
public final class MyAddon {
    public static final String ID = "my_addon";
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(ID)
        .setTooltipModifierFactory(item ->
            new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                .andThen(TooltipModifier.mapNull(KineticStats.create(item))));

    public MyAddon(IEventBus modBus) {
        REGISTRATE.registerEventListeners(modBus);
        MyBlocks.register();   // 触发类加载
        MyBlockEntities.register();
        ...
    }
}
```

```java
// 路线 B（要复用 CEI/CDP 那套 helper）：自己写子类，并在构造器里把子类自己交给 callback API
public class MyRegistrate extends CreateRegistrate {
    public MyRegistrate(String modid) {
        super(modid);                                // protected，可被子类调用
        CreateRegistrateRegistrationCallback.provideRegistrate(this); // 勿重复
    }
}
```

> `setTooltipModifierFactory` 是 Create 侧**唯一**推荐给 addon 的自动 tooltip 入口：它在 `accept(...)` 里为每个 `Registries.ITEM` 条目注册 `TooltipModifier`
> （`…/foundation/data/CreateRegistrate.java:112-127`）。配 `KineticStats.create(item)` 即可让所有 `IRotate` 方块物品自动获得应力/转速行。

### 1.3 CEI 的注册总表

```java
// …/common/CEICommon.java:53-75（构造函数内顺序）
REGISTRATE.registerEventListeners(modBus);   // 1. registrate 自己挂 DataGen / Register 监听
CEIFluids.register(modBus);                  // 2. 流体
CEIBlocks.register(modBus);                  // 3. 方块（此时也会拉入 BlockEntity 需要的 BlockEntry）
CEIBlockEntities.register(modBus);           // 4. BlockEntityType + capabilities
CEIItems.register(modBus);
CEICreativeModeTabs.register(modBus);
CEIRecipes.register(modBus);                 // 5. RecipeType/Serializer（用 DeferredRegister）
...
modBus.register(this);                       // 6. 自身（FMLCommonSetupEvent / RegisterEvent）
```

`CEIBlocks.register(modBus)` 本体是空的（`…/common/registry/CEIBlocks.java:187`）——它的作用纯粹是"类加载触发静态字段初始化"，registrate 的 `BlockEntry` 在静态初始化时就已经排队注册了。

---

## 2. 用 builder 链造一个动力方块

### 2.1 Create 真实样例 1：Millstone（最简化动力方块）

```java
// …/AllBlocks.java:675-683
public static final BlockEntry<MillstoneBlock> MILLSTONE = REGISTRATE.block("millstone", MillstoneBlock::new)
	.initialProperties(SharedProperties::stone)
	.properties(p -> p.mapColor(MapColor.METAL))
	.transform(pickaxeOnly())
	.blockstate((c, p) -> p.simpleBlock(c.getEntry(), AssetLookup.partialBaseModel(c, p)))
	.transform(CStress.setImpact(4.0))
	.item()
	.transform(customItemModel())
	.register();
```

拆解每一步：

| 步骤 | 方法 | 说明 |
|---|---|---|
| 方块本体 | `REGISTRATE.block(name, factory)` | `factory` 是 `NonNullFunction<Properties, T>` |
| 复制原版属性 | `.initialProperties(SharedProperties::stone)` | `SharedProperties` 只是几个原版方块代表：`stone()`=安山岩、`softMetal()`=金块、`copperMetal()`=铜块（`…/foundation/data/SharedProperties.java:10-28`） |
| 追加属性 | `.properties(p -> …)` | 可调用多次 |
| 工具/掉落 | `.transform(pickaxeOnly())` | `TagGen` 的 `NonNullUnaryOperator<BlockBuilder<B,P>>` |
| blockstate+model | `.blockstate((c, p) -> …)` | `RegistrateBlockstateProvider`；`AssetLookup.partialBaseModel(c,p)` → `models/block/<name>/block.json`（`…/foundation/data/AssetLookup.java:27-36`），`standardModel` → `models/block/<name>.json`（`:41-44`） |
| 应力 | `.transform(CStress.setImpact(4.0))` | **Create 内部专用，addon 不可用**，见 §2.3 |
| 物品 | `.item()` → `.transform(customItemModel())` | `customItemModel()` = `b -> b.model(AssetLookup::customItemModel).build()`（`…/foundation/data/ModelGen.java:32-35`），即 `models/block/millstone/item.json` |
| 提交 | `.register()` | 返回 `BlockEntry<T>` |

### 2.2 Create 真实样例 2：Crushing Wheel（axis 属性 + 非完整方块 + 额外 render layer）

```java
// …/AllBlocks.java:685-696
public static final BlockEntry<CrushingWheelBlock> CRUSHING_WHEEL =
	REGISTRATE.block("crushing_wheel", CrushingWheelBlock::new)
		.properties(p -> p.mapColor(MapColor.METAL))
		.initialProperties(SharedProperties::stone)
		.properties(BlockBehaviour.Properties::noOcclusion)
		.transform(pickaxeOnly())
		.blockstate((c, p) -> BlockStateGen.axisBlock(c, p, s -> AssetLookup.partialBaseModel(c, p)))
		.addLayer(() -> RenderType::cutoutMipped)
		.transform(CStress.setImpact(8.0))
		.item()
		.transform(customItemModel())
		.register();
```

- `.blockstate(BlockStateGen.axisBlock(...))`：`axis` 属性的三态 blockstate（`…/foundation/data/BlockStateGen.java:54-58` 起有 `axisBlockProvider/customItem` 系列）。动力方块的"绕哪个轴转"通常来自 `BlockStateProperties.AXIS` 或 `HORIZONTAL_FACING`/`FACING`。
- `.addLayer(() -> RenderType::cutoutMipped)`：注册额外渲染层（删除面的模型必须加）。
- `.properties(Properties::noOcclusion)`：否则方块不透光/渲染被裁。

### 2.3 应力值：addon 的正确写法

Create 的内部 `CStress` 会校验归属：

```java
// …/infrastructure/config/CStress.java:71-93
public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> setImpact(double value) {
	return builder -> {
		assertFromCreate(builder);
		ResourceLocation id = Create.asResource(builder.getName());
		DEFAULT_IMPACTS.put(id, value);
		return builder;
	};
}
...
private static void assertFromCreate(BlockBuilder<?, ?> builder) {
	if (!builder.getOwner().getModid().equals(Create.ID)) {
		throw new IllegalStateException("Non-Create blocks cannot be added to Create's config.");
	}
}
```

addon 走公开注册表：

```java
// …/api/stress/BlockStressValues.java:10-32
public static final SimpleRegistry<Block, DoubleSupplier> IMPACTS = SimpleRegistry.create();
public static final SimpleRegistry<Block, DoubleSupplier> CAPACITIES = SimpleRegistry.create();
public static final SimpleRegistry<Block, GeneratedRpm> RPM = SimpleRegistry.create();
```

Create 自己只是把它当作 provider 的查询层：

```java
// …/infrastructure/config/AllConfigs.java:68-70
CStress stress = server().kinetics.stressValues;
BlockStressValues.IMPACTS.registerProvider(stress::getImpact);
BlockStressValues.CAPACITIES.registerProvider(stress::getCapacity);
```

而 `SimpleRegistry` 的语义是"**直接注册永远优先于 provider**"（`…/api/registry/SimpleRegistry.java:29-34`），所以 addon 直接注册即可生效，且不会被 Create 的 config 覆盖：

```java
// addon 侧：必须在 BlockEntry 触发注册之后（即按类加载顺序放在注册代码末尾，或 FMLCommonSetupEvent）
public static void registerStress() {
    BlockStressValues.IMPACTS.register(MyBlocks.MY_MILL.get(), () -> 4.0);       // 消耗
    BlockStressValues.CAPACITIES.register(MyBlocks.MY_MOTOR.get(), () -> 256.0); // 产出
    BlockStressValues.RPM.register(MyBlocks.MY_MOTOR.get(),
        new BlockStressValues.GeneratedRpm(32, false));                          // 仅用于 tooltip
}
```

> CEI 通过 CDP 的 `CEIConfig.stress().setImpact(4.0)` 做同样的事（`…/common/registry/CEIBlocks.java:67,76`；`…/config/CEIStressConfig.java:24-32`）。
> `StressConfig` 的实现不在本仓库 —— **⚠ 待验证**：它是否在内部调用 `BlockStressValues.IMPACTS.register(...)`，还是只写自己的 config + provider。
> 如果只是 provider，那么它同样能用，因为 provider 也是 `SimpleRegistry` 支持的路径。

### 2.4 BlockEntityType 注册：`CreateBlockEntityBuilder`

Create 覆写了 registrate 的 `blockEntity(...)`，返回带 `visual/displaySource/displayTarget` 的构建器：

```java
// …/foundation/data/CreateRegistrate.java:130-140
@Override
public <T extends BlockEntity> CreateBlockEntityBuilder<T, CreateRegistrate> blockEntity(String name,
																						 BlockEntityFactory<T> factory) {
	return blockEntity(self(), name, factory);
}
```

Create 自己的样例（`…/AllBlockEntityTypes.java:253-295`）：

```java
// 轴类动力方块（shaft / cogwheel），visual 与 renderer 都给
public static final BlockEntityEntry<BracketedKineticBlockEntity> BRACKETED_KINETIC = REGISTRATE
	.blockEntity("simple_kinetic", BracketedKineticBlockEntity::new)
	.visual(() -> BracketedKineticBlockEntityVisual::create, false)
	.validBlocks(AllBlocks.SHAFT, AllBlocks.COGWHEEL, AllBlocks.LARGE_COGWHEEL)
	.renderer(() -> BracketedKineticBlockEntityRenderer::new)
	.register();

// 只有 shaft 几何的复用型
public static final BlockEntityEntry<KineticBlockEntity> ENCASED_SHAFT = REGISTRATE
	.blockEntity("encased_shaft", KineticBlockEntity::new)
	.visual(() -> SingleAxisRotatingVisual::shaft, false)
	.validBlocks(AllBlocks.ANDESITE_ENCASED_SHAFT, AllBlocks.BRASS_ENCASED_SHAFT, AllBlocks.ENCASED_CHAIN_DRIVE,
		AllBlocks.METAL_GIRDER_ENCASED_SHAFT)
	.renderer(() -> ShaftRenderer::new)
	.register();
```

CEI 的等价写法（`…/common/registry/CEIBlockEntities.java:49-60`）：

```java
public static final BlockEntityEntry<KineticBlockEntity> MECHANICAL_GRINDSTONE = REGISTRATE
        .blockEntity("mechanical_grindstone", KineticBlockEntity::new)
        .visual(() -> SingleAxisRotatingVisual.of(CEIPartialModels.MECHANICAL_GRINDSTONE), false)
        .renderer(() -> KineticBlockEntityRenderer::new)
        .validBlock(CEIBlocks.MECHANICAL_GRINDSTONE)
        .register();

public static final BlockEntityEntry<GrindstoneDrainBlockEntity> GRINDSTONE_DRAIN = REGISTRATE
        .blockEntity("grindstone_drain", GrindstoneDrainBlockEntity::new)
        .visual(() -> SingleAxisRotatingVisual.of(CEIPartialModels.MECHANICAL_GRINDSTONE), true)  // true = 仍然走原版 BER（渲染物品/流体时必需）
        .renderer(() -> GrindstoneDrainRenderer::new)
        .validBlock(CEIBlocks.GRINDSTONE_DRAIN)
        .register();
```

> `.visual(factory, renderNormally)` 的第二个参数语义见 §3.1：`renderNormally=true` 意味着 **Flywheel 可视化与原版 BER 同时运行**；若 BER 里 `renderSafe` 有 `if (VisualizationManager.supportsVisualization(...)) return;` 守卫（CEI 的 `GrindstoneDrainRenderer` 没有该守卫、`BlazeEnchanterRenderer` 有，见 `…/common/processing/enchanter/BlazeEnchanterRenderer.java:48-50`），则传 `false`。

### 2.5 capabilities

NeoForge 标准 `RegisterCapabilitiesEvent`，用 `BlockEntityEntry.get()` 拿 `BlockEntityType`：

```java
// …/common/registry/CEIBlockEntities.java:94-114
public static void register(IEventBus modBus) {
    modBus.register(CEIBlockEntities.class);   // @SubscribeEvent 挂在类上
}

@SubscribeEvent
public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
    event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
            GRINDSTONE_DRAIN.get(), GrindstoneDrainBlockEntity::getItemHandler);
    event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,
            GRINDSTONE_DRAIN.get(), GrindstoneDrainBlockEntity::getFluidHandler);
    ...
}
```

`getItemHandler(@Nullable Direction side)` 返回 `null` 表示该面不暴露（`…/common/kinetics/grindstone/GrindstoneDrainBlockEntity.java:114-124`）。

### 2.6 `BuilderTransformers`：现成的组合模板

`…/foundation/data/BuilderTransformers.java` 是 `NonNullUnaryOperator<BlockBuilder<B,P>>` 的集合，`transform()` 直接用。动力方块相关的：

| 模板 | 签名 | 行为要点 | 行号 |
|---|---|---|---|
| `encasedShaft(casing, casingShift)` | `<B extends EncasedShaftBlock, P>` | 包壳 shaft，带 connected textures + casing connectivity + item model | `:99-110` |
| `encasedCogwheel(casing, shift)` / `encasedLargeCogwheel(...)` | `<B extends EncasedCogwheelBlock, P>` | 走 `encasedCogwheelBase`，模型按 `TOP_SHAFT/BOTTOM_SHAFT` 组合 | `:183-224` |
| `encasedBase(b, drop)`（private） | `<B extends RotatedPillarKineticBlock, P>` | `noOcclusion` + `CStress.setNoImpact()` + `loot(dropOther)` | `:226-232` |
| `bearing(prefix, gearbox)` | `<B extends Block, P>` | 轴承族通用外观 + `CStress.setImpact(4.0)` | `:389` 起 |
| `cuckooClock()` | `<B extends Block, P>` | `CStress.setImpact(1)` + `ModelGen.customItemModel("cuckoo_clock","item")` | `:234-242` |
| `valveHandle(colour)` / `casing(...)` / `layeredCasing(...)` | `<B extends ..., CreateRegistrate>` | 注意泛型父类锁定为 `CreateRegistrate` | `:293,310,324` |

注意 `encasedBase` 里用了 `CStress`（`…/foundation/data/BuilderTransformers.java:230`），**因此 addon 不能直接 `transform(BuilderTransformers.encasedShaft(...))`**——它会在 `setNoImpact()` 处抛异常。CEI 也没用这些模板，而是自己写 `.blockstate(...)`（`…/common/registry/CEIBlocks.java:64-88`）。

---

## 3.「新建一个带方块实体的动力方块」完整步骤

### 3.1 接口与基类选择

```
Block
 └─ KineticBlock (abstract, implements IRotate)          …/content/kinetics/base/KineticBlock.java:17
     ├─ RotatedPillarKineticBlock  (AXIS)               …/content/kinetics/base/RotatedPillarKineticBlock.java
     ├─ HorizontalKineticBlock     (HORIZONTAL_FACING)   …/content/kinetics/base/HorizontalKineticBlock.java
     ├─ DirectionalKineticBlock    (FACING)              …/content/kinetics/base/DirectionalKineticBlock.java
     └─ DirectionalAxisKineticBlock                      …/content/kinetics/base/DirectionalAxisKineticBlock.java
```

`IBE<T>` 是"方块 ↔ BE"的胶水（`…/foundation/block/IBE.java:23-88`）。**必需实现只有两个**：

```java
// …/foundation/block/IBE.java:25-27
Class<T> getBlockEntityClass();
BlockEntityType<? extends T> getBlockEntityType();
```

其余全是 default，其中三个关键的：

- `newBlockEntity` → `getBlockEntityType().create(pos, state)`（`:62-65`）
- `getTicker` → 若 BE 是 `SmartBlockEntity` 子类，返回 `new SmartBlockEntityTicker<>()`（`:67-73`）；`SmartBlockEntityTicker` 会补 `setLevel` 再调 `tick()`（`…/foundation/blockEntity/SmartBlockEntityTicker.java:11-16`）
- `static onRemove(state, level, pos, newState)` → 调 `SmartBlockEntity.destroy()` + `removeBlockEntity`（`:43-56`）；**必须在 `Block.onRemove` 里调用它**，`KineticBlock` 已经做了（`…/content/kinetics/base/KineticBlock.java:45-48`）

### 3.2 SmartBlockEntity 生命周期表

| 时机 | 回调 | 源码 |
|---|---|---|
| 构造 | `addBehaviours(list)` 被调用，所有 behaviour 进 map | `…/foundation/blockEntity/SmartBlockEntity.java:46-54` |
| 首次 `read()`（含首次 `loadAdditional`） | `addBehavioursDeferred(list)` + `BlockEntityBehaviourEvent` | `:110-120` |
| 首次 `tick()` 且 `hasLevel()` | `initialize()`，随后 `forEachBehaviour(initialize)` + `lazyTick()` | `:74-78`、`:64-72` |
| 每 tick | lazyTick 计时器 → `lazyTick()`；然后 `forEachBehaviour(tick)` | `:74-86` |
| lazy 周期 | 默认 `lazyTickRate = 10`（构造时），`setLazyTickRate(n)` 可改 | `:49`、`:207-210` |
| 保存 | `write(tag, registries, clientPacket=false)` | `:93-96`、`:160-163` |
| 客户端包写出 | `writeClient(tag, registries)` → `write(..., true)` | `:170-174` |
| 客户端包读入 | `readClient(tag, registries)` → `read(..., true)` | `:165-168` |
| chunk 卸载 | `onChunkUnloaded()` 置 `chunkUnloaded=true` | `:127-131` |
| `setRemoved()` | `remove()`（若非 chunk 卸载）+ `invalidate()` | `:133-139` |
| 方块被替换/破坏 | `destroy()`（需 `IBE.onRemove` 调用） | `:153-158` |
| 同步 | `sendData()` / `notifyUpdate()` 来自 `SyncedBlockEntity` | `…/foundation/blockEntity/SyncedBlockEntity.java:59-67` |

`sendData()`/`notifyUpdate()` 的实现（`…/foundation/blockEntity/SyncedBlockEntity.java:59-67`）：

```java
public void sendData() {
	if (level instanceof ServerLevel serverLevel)
		serverLevel.getChunkSource().blockChanged(getBlockPos());   // 触发 ClientboundBlockEntityDataPacket
}

public void notifyUpdate() {
	setChanged();
	sendData();
}
```

> **实践规则**：只改客户端也要看到的数据 → `sendData()`；同时要落盘 → `notifyUpdate()`；纯服务端逻辑 → `setChanged()`。
> `getUpdateTag`/`getUpdatePacket`/`handleUpdateTag` 已由 `SyncedBlockEntity` 实现（`:27-46`），addon 不用重写。

### 3.3 KineticBlockEntity 关键点

```java
// …/content/kinetics/base/KineticBlockEntity.java:56-83
public class KineticBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, IHaveHoveringInformation {
	public @Nullable Long network;
	public @Nullable BlockPos source;
	public boolean networkDirty;
	public boolean updateSpeed;          // true 表示下一 tick 需要 attachKinetics()
	public int preventSpeedUpdate;
	protected float speed;
	protected float capacity;
	protected float stress;
	protected boolean overStressed;
	...
	public KineticBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
		super(typeIn, pos, state);
		effects = new KineticEffectHandler(this);
		updateSpeed = true;
	}
```

| 覆写点 | 用途 | 行号 |
|---|---|---|
| `getGeneratedSpeed()` | 发电机返回非 0；默认 0（= 从动件） | `:281-283` |
| `isSource()` | `getGeneratedSpeed() != 0` | `:285-287` |
| `getSpeed()` | `overStressed` 或 `tickRateManager().isFrozen()` 时返回 0 | `:289-293` |
| `onSpeedChanged(prev)` | 转速方向/零转变时重置 flicker；子类常在此更新状态 | `:191-197` |
| `initialize()` | 非客户端时把自身加入 `KineticNetwork` | `:85-95` |
| `remove()` | 从 network 移除并 `detachKinetics()` | `:199-207` |
| `write/read(tag, registries, clientPacket)` | 写 `Speed/Source/Network`；**read 末尾在 clientPacket 时自动 `VisualizationHelper.queueUpdate(this)`** | `:209-279`（关键 `:277-279`） |
| `attachKinetics()` | `RotationPropagator.handleAdded` | `:363-366` |
| `calculateStressApplied()` / `calculateAddedStressCapacity()` | 从 `BlockStressValues` 读 | `:179-189` |
| `addToGoggleTooltip` | 默认输出应力面板 | `:454-471` |

静态换算工具（`…/content/kinetics/base/KineticBlockEntity.java:508-519`）：

```java
public static float convertToDirection(float axisSpeed, Direction d) { ... }
public static float convertToLinear(float speed)  { return speed / 512f; }
public static float convertToAngular(float speed) { return speed * 360f / 60f / 20f; }  // rpm → deg/tick
```

### 3.4 完整骨架（addon 可直接改名使用）

```java
// ========== Block ==========
public class MyCrusherBlock extends HorizontalKineticBlock implements IBE<MyCrusherBlockEntity> {

    public MyCrusherBlock(Properties properties) {
        super(properties);
    }

    // ---- IRotate ----
    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();    // 水平朝向的轴
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == getRotationAxis(state);
    }

    @Override
    public SpeedLevel getMinimumRequiredSpeedLevel() {
        return SpeedLevel.MEDIUM;                              // 需求转速档，tooltip 会用
    }

    // ---- 可选：隐藏应力行（发电机/无消耗件）----
    // @Override public boolean hideStressImpact() { return true; }

    // ---- IBE ----
    @Override
    public Class<MyCrusherBlockEntity> getBlockEntityClass() {
        return MyCrusherBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends MyCrusherBlockEntity> getBlockEntityType() {
        return MyBlockEntities.MY_CRUSHER.get();               // 注意 .get() 而非 .getType()
    }
}
```

```java
// ========== BlockEntity ==========
public class MyCrusherBlockEntity extends KineticBlockEntity {

    public MyCrusherBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(20);                       // 需要惰性逻辑时才改
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);           // 父类默认空实现，但保留调用是惯例
        behaviours.add(new DirectBeltInputBehaviour(this));
        // 见 §6 的 ValueBox / FilteringBehaviour
    }

    @Override
    public void tick() {
        super.tick();                              // 必须：驱动 initialize/lazyTick/behaviour/kinetics
        if (level.isClientSide)
            return;
        float speed = getSpeed();
        if (speed == 0)
            return;
        // ... 按速度推进处理 ...
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        // tag.put... 自己的状态
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);  // 内部已在 clientPacket 时 queueUpdate visual
        // 读回自己的状态
    }
}
```

> **注意方法可见性**：`SmartBlockEntity` 的 `write/read` 是 `protected`（`:93`、`:110`），`saveAdditional/loadAdditional` 是 `final`（`:160-163`、`:122-125`），
> 所以 addon **必须**覆写 `write/read`，不能覆写 `saveAdditional`（会编译失败）。

### 3.5 注册顺序（一个方块的全套）

```java
public final class MyBlocks {
    public static final BlockEntry<MyCrusherBlock> MY_CRUSHER = MyAddon.REGISTRATE
        .block("my_crusher", MyCrusherBlock::new)
        .initialProperties(SharedProperties::stone)
        .properties(p -> p.noOcclusion().mapColor(MapColor.METAL))
        .transform(TagGen.pickaxeOnly())
        .blockstate(BlockStateGen.horizontalBlockProvider(true))
        .addLayer(() -> RenderType::cutoutMipped)
        .item()
        .transform(ModelGen.customItemModel())
        .register();

    public static void register() {
        // 应力放在这里之后（此时 BlockEntry 已排队注册）
        BlockStressValues.IMPACTS.register(MY_CRUSHER.get(), () -> 4.0);
    }
}

public final class MyBlockEntities {
    public static final BlockEntityEntry<MyCrusherBlockEntity> MY_CRUSHER = MyAddon.REGISTRATE
        .blockEntity("my_crusher", MyCrusherBlockEntity::new)
        .visual(() -> SingleAxisRotatingVisual.of(MyPartialModels.CRUSHER_HEAD), false)
        .renderer(() -> KineticBlockEntityRenderer::new)
        .validBlock(MyBlocks.MY_CRUSHER)
        .register();
}
```

`.validBlock(...)`/`.validBlocks(...)` 是 registrate `BlockEntityBuilder` 的方法；Create 侧额外提供 `.validBlocksDeferred(...)`（`…/foundation/data/CreateBlockEntityBuilder.java:50-63`），用于"方块集合还没构造完"的场景。

---

## 4. Flywheel 1.0 渲染链路

### 4.1 visual 的注册与实例化（源码为准）

```
CreateBlockEntityBuilder.visual(factory, renderNormally)          …/foundation/data/CreateBlockEntityBuilder.java:81-103
        │  CatnipServices.PLATFORM.executeOnClientOnly(() -> registerVisualizer())
        ▼
registerVisualizer()                                              …/foundation/data/CreateBlockEntityBuilder.java:105-116
        │  OneTimeEventReceiver.addModListener(getOwner(), FMLClientSetupEvent.class, $ -> { ... })
        ▼
SimpleBlockEntityVisualizer.builder(getEntry())
        .factory(visualFactory.get())
        .skipVanillaRender(be -> !renderNormally.test(be))
        .apply();
```

关键代码：

```java
// …/foundation/data/CreateBlockEntityBuilder.java:105-116
protected void registerVisualizer() {
	OneTimeEventReceiver.addModListener(getOwner(), FMLClientSetupEvent.class, $ -> {
		var visualFactory = this.visualFactory;
		if (visualFactory != null) {
			Predicate<@NotNull T> renderNormally = this.renderNormally;
			SimpleBlockEntityVisualizer.builder(getEntry())
				.factory(visualFactory.get())
				.skipVanillaRender(be -> !renderNormally.test(be))
				.apply();
		}
	});
}
```

结论：

1. **addon 不需要碰 `VisualizationHelper.registerVisualizer`**，也不需要自己写 `ClientEvents`。只要 `.visual(...)` 写过一次，Create 会在 `FMLClientSetupEvent` 调 `SimpleBlockEntityVisualizer`。
2. `skipVanillaRender(predicate)` = "什么时候**不**渲染原版 BER"。因为 `renderNormally` 传给了 `!renderNormally.test(be)`：
   - `visual(factory)` → 等价 `visual(factory, true)`（`:81-84`）→ `skipVanillaRender` 恒 `false` → **BER 照常跑**；
   - `visual(factory, false)` → `skipVanillaRender` 恒 `true` → **纯 Flywheel**（Create 大量动力方块都用 `false`）。
3. 显式刷新的唯一入口是 `VisualizationHelper.queueUpdate(BlockEntity)`，并且必须在客户端调用（Create 用 `CatnipServices.PLATFORM.executeOnClientOnly` 包一层）：

```java
// …/content/kinetics/base/KineticBlockEntity.java:277-279
if (clientPacket)
	CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> VisualizationHelper.queueUpdate(this));
```

> 也就是说：**转速（`Speed` NBT）变化 → 客户端 `read(clientPacket=true)` → 自动 queueUpdate**。你自己新增的、需要驱动的 visual 数据（比如"是否在加工"）若通过 `write(..., clientPacket=true)` 同步，也同样自动触发 `update()`。

### 4.2 `KineticBlockEntityVisual` 泛型与构造

```java
// …/content/kinetics/base/KineticBlockEntityVisual.java:13-21
public abstract class KineticBlockEntityVisual<T extends KineticBlockEntity> extends AbstractBlockEntityVisual<T> {

	public KineticBlockEntityVisual(VisualizationContext context, T blockEntity, float partialTick) {
		super(context, blockEntity, partialTick);
	}

	protected Direction.Axis rotationAxis() {
		return rotationAxis(blockState);
	}
```

三个静态工具非常重要（`:23-41`）：

```java
public static float rotationOffset(BlockState state, Axis axis, Vec3i pos)     // 相邻方块 22.5° 相位错开；大齿轮 11.25°
public static boolean shouldOffset(Axis axis, Vec3i pos)                       // (x+y+z)%2==0 才错开
public static Axis rotationAxis(BlockState blockState)                         // 从 IRotate 取轴，取不到退回 Y
public static void applyOverstressEffect(KineticBlockEntity be, RotatingInstance... instances)  // 过载染红/欠载染绿
```

`AbstractBlockEntityVisual` 来自 Flywheel（`dev.engine_room.flywheel.lib.visual`），提供 `instancerProvider()`、`getVisualPosition()`、`relight(...)`、字段 `blockEntity` / `blockState`。**该类源码不在本仓库**（Flywheel jar），⚠ 待验证其 `partialTick`、`Context` 等细节。

### 4.3 `RotatingInstance`：速度 → 角度

```java
// …/content/kinetics/base/RotatingInstance.java:14-35
public class RotatingInstance extends ColoredLitOverlayInstance {
	public static final float SPEED_MULTIPLIER = 6;

	public byte rotationAxisX;
	public byte rotationAxisY;
	public byte rotationAxisZ;
	public float x, y, z;
	/** Speed in degrees per second */
	public float rotationalSpeed;
	/** Offset in degrees */
	public float rotationOffset;
	/** Base rotation of the instance, applied before kinetic rotation */
	public final Quaternionf rotation = new Quaternionf();
```

`setup` 是核心（`:63-69`）：

```java
public RotatingInstance setup(KineticBlockEntity blockEntity, Axis axis, float speed) {
	var blockState = blockEntity.getBlockState();
	var pos = blockEntity.getBlockPos();
	return setRotationAxis(axis)
		.setRotationalSpeed(speed * RotatingInstance.SPEED_MULTIPLIER)
		.setRotationOffset(KineticBlockEntityVisual.rotationOffset(blockState, axis, pos) + blockEntity.getRotationAngleOffset(axis));
}
```

- **角度不在 Java 侧累加**：`rotationalSpeed`（度/秒）作为实例属性上传，着色器按时间积分。所以"转速变化"只需重新 `setRotationalSpeed` 并 `setChanged()`。
- 轴向量被压成 3 个 `byte`（`*127`，`:107-112`），因此只能用 ±1 的整数轴；斜轴需要把旋转烘进 `rotation` 四元数（`rotateTo(...)`，`:93-96`）。
- 颜色：`setColor(KineticBlockEntity)` 用 network id 生成稳定色（`colorFromBE`，`:41-45`），便于调试多网络。

### 4.4 visual 的实例化模板（Create 实现）

`SingleAxisRotatingVisual`（最常用）：

```java
// …/content/kinetics/base/SingleAxisRotatingVisual.java:18-59
public class SingleAxisRotatingVisual<T extends KineticBlockEntity> extends KineticBlockEntityVisual<T>
		implements SimpleTickableVisual {

	protected final RotatingInstance rotatingModel;

	public SingleAxisRotatingVisual(VisualizationContext context, T blockEntity, float partialTick, Direction from, Model model) {
		super(context, blockEntity, partialTick);
		rotatingModel = instancerProvider().instancer(AllInstanceTypes.ROTATING, model)
			.createInstance()
			.rotateToFace(from, rotationAxis())
			.setup(blockEntity)
			.setPosition(getVisualPosition());

		rotatingModel.setChanged();
	}

	/** Y 轴对齐的 partial model 直接用这个 */
	public static <T extends KineticBlockEntity> SimpleBlockEntityVisualizer.Factory<T> of(PartialModel partial) {
		return (context, blockEntity, partialTick) ->
			new SingleAxisRotatingVisual<>(context, blockEntity, partialTick, Models.partial(partial));
	}

	/** Z 轴对齐的 partial model（例如侧放的锯片） */
	public static <T extends KineticBlockEntity> SimpleBlockEntityVisualizer.Factory<T> ofZ(PartialModel partial) { ... }

	@Override public void update(float pt)        { rotatingModel.setup(blockEntity).setChanged(); }
	@Override public void tick(Context context)   { applyOverstressEffect(blockEntity, rotatingModel); }
	@Override public void updateLight(float pt)   { relight(rotatingModel); }
	@Override protected void _delete()            { rotatingModel.delete(); }
	@Override public void collectCrumblingInstances(Consumer<Instance> c) { c.accept(rotatingModel); }
}
```

`OrientedRotatingVisual`（模型朝向由 `FACING`/`HORIZONTAL_FACING` 决定）：

```java
// …/content/kinetics/base/OrientedRotatingVisual.java:31-57
public OrientedRotatingVisual(VisualizationContext context, T blockEntity, float partialTick,
							  Direction from, Direction to, Model model) {
	super(context, blockEntity, partialTick);
	rotatingModel = instancerProvider().instancer(AllInstanceTypes.ROTATING, model)
		.createInstance()
		.rotateToFace(from, to)          // 从 from 转到 to
		.setup(blockEntity)
		.setPosition(getVisualPosition());
	rotatingModel.setChanged();
}

public static <T extends KineticBlockEntity> SimpleBlockEntityVisualizer.Factory<T> of(PartialModel partial) {
	return (context, blockEntity, partialTick) -> {
		Direction facing = blockEntity.getBlockState().getValue(BlockStateProperties.FACING);
		return new OrientedRotatingVisual<>(context, blockEntity, partialTick, Direction.SOUTH, facing, Models.partial(partial));
	};
}

public static <T extends KineticBlockEntity> SimpleBlockEntityVisualizer.Factory<T> backHorizontal(PartialModel partial) { ... }
```

`ShaftVisual` 干脆是 12 行的薄包装（`…/content/kinetics/base/ShaftVisual.java:8-12`）。

### 4.5 一个最简"会转的方块"完整渲染模板

```java
// ---------- 客户端：partial model ----------
public final class MyPartialModels {
    // 对应 assets/<modid>/models/block/crusher_head.json
    public static final PartialModel CRUSHER_HEAD = PartialModel.of(
        ResourceLocation.fromNamespaceAndPath(MyAddon.ID, "block/crusher_head"));

    public static void register() {
        // 类加载即完成注册；见 §4.6 的坑
    }
}
```

```java
// ---------- 客户端：visual（转 + 过载染色 + 光照）----------
public class MyCrusherVisual extends KineticBlockEntityVisual<MyCrusherBlockEntity>
		implements SimpleTickableVisual {

    protected final RotatingInstance head;

    public MyCrusherVisual(VisualizationContext context, MyCrusherBlockEntity be, float partialTick) {
        super(context, be, partialTick);
        head = instancerProvider()
            .instancer(AllInstanceTypes.ROTATING, Models.partial(MyPartialModels.CRUSHER_HEAD))
            .createInstance()
            .rotateToFace(Direction.UP, rotationAxis())   // partial 是 Y 轴对齐
            .setup(be)
            .setPosition(getVisualPosition());
        head.setChanged();
    }

    @Override public void update(float pt)              { head.setup(blockEntity).setChanged(); }
    @Override public void tick(Context context)         { applyOverstressEffect(blockEntity, head); }
    @Override public void updateLight(float partialTick){ relight(head); }
    @Override protected void _delete()                  { head.delete(); }
    @Override public void collectCrumblingInstances(Consumer<Instance> c) { c.accept(head); }
}
```

```java
// ---------- 注册（= 全部接线）----------
.blockEntity("my_crusher", MyCrusherBlockEntity::new)
    .visual(() -> MyCrusherVisual::new, false)          // false → 纯 Flywheel，不跑 BER
    .renderer(() -> KineticBlockEntityRenderer::new)    // 给不支持 Flywheel 的环境兜底
    .validBlock(MyBlocks.MY_CRUSHER)
    .register();
```

`renderer` 的兜底逻辑在 Flywheel 不可用时生效：

```java
// …/content/kinetics/base/KineticBlockEntityRenderer.java:47-55
@Override
protected void renderSafe(T be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
	if (VisualizationManager.supportsVisualization(be.getLevel())) return;
	BlockState state = getRenderedBlockState(be);
	RenderType type = getRenderType(be, state);
	renderRotatingBuffer(be, getRotatedModel(be, state), ms, buffer.getBuffer(type), light);
}
```

`ShaftRenderer` 只是把 `getRenderedBlockState` 换成"一个按轴排布的 shaft 方块状态"（`…/content/kinetics/base/ShaftRenderer.java:12-15`）：

```java
@Override
protected BlockState getRenderedBlockState(KineticBlockEntity be) {
	return shaft(getRotationAxisOf(be));
}
```

### 4.6 客户端 / 服务端分离注意点

| 事项 | 正确写法 | 证据 |
|---|---|---|
| 客户端注册入口 | 单独 `@Mod(value = ID, dist = Dist.CLIENT)` 类，在构造器里注册 partial model / Ponder plugin | `…/client/CEIClient.java:30-40` |
| 从公共代码调客户端 API | `CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> ...)` | `…/foundation/data/CreateBlockEntityBuilder.java:96`、`KineticBlockEntity.java:108,278` |
| 判断当前侧 | `CatnipServices.PLATFORM.getEnv().isClient()` | `…/AllCreativeModeTabs.java` 内 `RegistrateDisplayItemsGenerator` 静态块 |
| 纯客户端方法标注 | `@OnlyIn(Dist.CLIENT)`（Create 仍在用） | `…/content/kinetics/millstone/MillstoneBlockEntity.java:72-73`、`CreateRegistrate.java:292-317` |
| 注册表 / 事件总线 | `modBus.register(...)`（`IEventBus`），NeoForge 用 `RegisterCapabilitiesEvent` 而非 `AttachCapabilitiesEvent` | `…/common/registry/CEIBlockEntities.java:94-114` |
| 数据生成只在 datagen 跑 | `if (!DatagenModLoader.isRunningDataGen()) return;` | `…/data/CEIData.java:35-36` |
| Ponder 场景里判断 datagen | `if (DatagenModLoader.isRunningDataGen()) return;`（避免 level 为 null 时取 registry） | `…/client/ponder/CEIPonderScenes.java:82-89` |

> ⚠ **`AllCreatable` 未文档化的坑**：`PartialModel` 必须在 `FMLClientSetupEvent` **之前**（或至少早于 `PartialModelEventHandler#onRegisterAdditional` 触发）完成类加载，否则 `PartialModel.ALL` 可能漏登记。CEI 的注释写得很明确：
> `…/client/CEIClient.java:33-38` —— "CEIPartialModels must be registered here, or when PartialModelEventHandler#onRegisterAdditional triggered, PartialModel.ALL won't include all partial model in 'some cases'"。所以 `MyPartialModels.register()` 要在 client 构造器里显式调一次。

---

## 5. Ponder：为 addon 注册场景

### 5.1 API 入口与三个 helper

Ponder 现在住在 **Create 的库 `net.createmod.ponder`**（不是 Create 本体），入口是 `PonderIndex`：

```java
// …/client/CEIClient.java:42-44
public static void setup(final FMLClientSetupEvent event) {
    PonderIndex.addPlugin(new CEIPonderPlugin());
}
```

插件接口四件套（`…/foundation/ponder/CreatePonderPlugin.java:20-59`）：

```java
public class CreatePonderPlugin implements PonderPlugin {
	@Override public String getModId() { return Create.ID; }

	@Override
	public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
		AllCreatePonderScenes.register(helper);
	}

	@Override
	public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
		AllCreatePonderTags.register(helper);
	}

	@Override
	public void registerSharedText(SharedTextRegistrationHelper helper) {
		helper.registerSharedText("rpm8", "8 RPM");
		...
	}

	@Override
	public void onPonderLevelRestore(PonderLevel ponderLevel) {
		PonderWorldBlockEntityFix.fixControllerBlockEntities(ponderLevel);
	}

	@Override
	public void indexExclusions(IndexExclusionHelper helper) {
		helper.excludeBlockVariants(ValveHandleBlock.class, AllBlocks.COPPER_VALVE_HANDLE.get());
		...
	}
}
```

CEI 的最小插件（`…/client/ponder/CEIPonderPlugin.java:27-41`）只实现 `getModId/registerScenes/registerTags` 三个。

### 5.2 场景注册：`withKeyFunction` + `forComponents` + `addStoryBoard`

```java
// …/client/ponder/CEIPonderScenes.java:40-57
public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
    PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> HELPER = helper.withKeyFunction(RegistryEntry::getId);

    helper.forComponents(AllItems.EXP_NUGGET.getId(), CEIItems.EXPERIENCE_BUCKET.getId(), AllBlocks.EXPERIENCE_BLOCK.getId())
            .addStoryBoard("experience/basic", ExperienceScene::basic, CEIPonderTags.EXPERIENCE_APPLIANCES)
            .addStoryBoard("experience/advance", ExperienceScene::advance, CEIPonderTags.SUPER_EXPERIENCE_APPLIANCES)
            .addStoryBoard("experience/prepare_for_super_enchant", ExperienceScene::prepare)
            .addStoryBoard("experience/beacon_base", ExperienceScene::beaconBase);

    HELPER.forComponents(CEIBlocks.MECHANICAL_GRINDSTONE)
            .addStoryBoard("grindstone/basic", GrindstoneScene::basic, CEIPonderTags.EXPERIENCE_APPLIANCES)
            .addStoryBoard("grindstone/extra", GrindstoneScene::extra);

    HELPER.forComponents(CEIBlocks.BLAZE_ENCHANTER)
            .addStoryBoard("enchanter", EnchanterScene::basic, CEIPonderTags.EXPERIENCE_APPLIANCES)
            .addStoryBoard("enchanter", EnchanterScene::superEnchant, CEIPonderTags.SUPER_EXPERIENCE_APPLIANCES);
}
```

- `helper.forComponents(ResourceLocation...)` 接受任意 id：`RegistryEntry.getId()`、`ItemProviderEntry.getId()`、裸 `ResourceLocation`。
- `addStoryBoard(String structurePath, BiConsumer<SceneBuilder, SceneBuildingUtil> scene, ResourceLocation... tags)`。
- **`structurePath` 对应 `assets/<modid>/ponder/<structurePath>.nbt`**（Create 的 `"shaft/relay"` 对应 `assets/create/ponder/shaft/relay.nbt`）。⚠ 待验证：本仓库只有 java 源码，没有 `assets/` 目录可以核对扩展名与路径规则。

### 5.3 Tag 注册

```java
// …/client/ponder/CEIPonderTags.java:40-58
public static final ResourceLocation EXPERIENCE_APPLIANCES = CEICommon.asResource("experience_appliances");

public static void register(PonderTagRegistrationHelper<ResourceLocation> helper) {
    PonderTagRegistrationHelper<RegistryEntry<?, ?>> entryHelper = helper.withKeyFunction(RegistryEntry::getId);

    helper.registerTag(EXPERIENCE_APPLIANCES)
            .addToIndex()
            .item(AllBlocks.EXPERIENCE_BLOCK.get(), true, false)
            .title("Experience Related")
            .description("Components which will be used when processing and applying Experience")
            .register();

    entryHelper.addToTag(EXPERIENCE_APPLIANCES)
            .add(AllBlocks.ITEM_DRAIN)
            .add(AllBlocks.SPOUT)
            .add(CEIBlocks.MECHANICAL_GRINDSTONE)
            ...
}
```

想挂进 Create 已有的分类，直接复用常量（`…/infrastructure/ponder/AllCreatePonderTags.java:21-41`）：

```java
import static com.simibubi.create.infrastructure.ponder.AllCreatePonderTags.KINETIC_APPLIANCES;
import static com.simibubi.create.infrastructure.ponder.AllCreatePonderTags.KINETIC_RELAYS;

entryHelper.addToTag(KINETIC_APPLIANCES).add(MyBlocks.MY_CRUSHER);
```

`addToTag` 的 `.add(...)` 接受 `ItemProviderEntry` / `BlockEntry` / `ItemEntry`（registrate 的 `RegistryEntry` 体系）。

### 5.4 场景内部：`CreateSceneBuilder`

`CreateSceneBuilder extends PonderSceneBuilder`（`…/foundation/ponder/CreateSceneBuilder.java:55-70`），在标准 `SceneBuilder` 之上加了 `effects()/world()/special()` 三组扩展：

提示：直接 `new CreateSceneBuilder(builder)` 包住传入的 `SceneBuilder` 即可用全部扩展（`…/client/ponder/scene/GrindstoneScene.java:40`）。

动力方块专用 API：

| 调用 | 作用 | 行号 |
|---|---|---|
| `scene.world().setKineticSpeed(Selection, float)` | 给选区写 `Speed` NBT | `…/foundation/ponder/CreateSceneBuilder.java:220-222` |
| `scene.world().multiplyKineticSpeed(Selection, float)` | 按倍率改 | `:224-226` |
| `scene.effects().rotationSpeedIndicator(BlockPos)` | 画转速粒子环（颜色=速度档） | `:118-120` |
| `scene.effects().rotationDirectionIndicator(BlockPos)` | 画转向粒子（橙/青） | `:122-124` |
| `scene.world().createItemOnBelt(BlockPos, Direction, ItemStack)` | 在传送带上"放"物品 | `:161-188` |
| `scene.world().animateBearing/movePulley/moveDeployer(...)` | 机械动画 | `:131-145` |
| `scene.special().birbOnSpinnyShaft(BlockPos)` | 鹦鹉站在轴上转 | `:347-350` |

`setKineticSpeed` 的实现细节值得注意——它同时改 `SpeedGaugeBlockEntity` 的显示值（`:228-236`）：

```java
public void modifyKineticSpeed(Selection selection, UnaryOperator<Float> speedFunc) {
	modifyBlockEntityNBT(selection, SpeedGaugeBlockEntity.class, nbt -> {
		float newSpeed = speedFunc.apply(nbt.getFloat("Speed"));
		nbt.putFloat("Value", SpeedGaugeBlockEntity.getDialTarget(newSpeed));
	});
	modifyBlockEntityNBT(selection, KineticBlockEntity.class, nbt -> {
		nbt.putFloat("Speed", speedFunc.apply(nbt.getFloat("Speed")));
	});
}
```

`CreateSceneBuilder` 还覆写了 `setBlocks/modifyBlocks/restoreBlocks`，让新生成的 `SmartBlockEntity` 自动 `markVirtual()`（`:306-329`）——否则 Ponder 世界里的 BE 会以为自己活在真实世界。

### 5.5 最小可运行示例

```java
// ---- 1) PonderPlugin（客户端入口）----
public class MyPonderPlugin implements PonderPlugin {
    @Override public String getModId() { return MyAddon.ID; }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PonderSceneRegistrationHelper<RegistryEntry<?, ?>> H = helper.withKeyFunction(RegistryEntry::getId);
        H.forComponents(MyBlocks.MY_CRUSHER)
            .addStoryBoard("my_crusher/intro", MyCrusherScene::intro,
                AllCreatePonderTags.KINETIC_APPLIANCES);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        // 若想自建分类：
        // helper.registerTag(MyAddon.asResource("my_category"))
        //       .addToIndex().item(MyBlocks.MY_CRUSHER.get(), true, false)
        //       .title("My Components").description("...").register();
    }
}
```

```java
// ---- 2) 场景本体 ----
public final class MyCrusherScene {

    public static void intro(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("my_crusher/intro", "Rotating with My Crusher");   // lang key 由 Ponder 生成
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        // 展示机器本体 + 一根连接轴
        scene.world().showSection(util.select().fromTo(2, 1, 2, 2, 1, 2), Direction.DOWN);
        scene.idle(10);

        BlockPos machine = util.grid().at(2, 1, 2);
        scene.world().setKineticSpeed(util.select().position(machine), 64);
        scene.effects().rotationDirectionIndicator(machine);
        scene.idle(10);

        scene.overlay().showText(60)
            .text("It needs Rotational Force from a shaft")
            .placeNearTarget()
            .attachKeyFrame()
            .pointAt(util.vector().centerOf(machine));

        scene.idle(40);
        scene.markAsFinished();
    }
}
```

```java
// ---- 3) 注册插件（client 构造器）----
@Mod(value = MyAddon.ID, dist = Dist.CLIENT)
public class MyAddonClient {
    public MyAddonClient(IEventBus modBus) {
        MyPartialModels.register();                  // §4.6
        modBus.addListener(MyAddonClient::setup);
    }
    public static void setup(final FMLClientSetupEvent event) {
        PonderIndex.addPlugin(new MyPonderPlugin());
    }
}
```

### 5.6 lang key

Ponder 的文本是**自动收集**的：`scene.title(path, "Title")` 与 `overlay().showText(n).text("...")` 会注册成 `ponder.<path>` 命名空间下的 key，由 Ponder 的 `LangAccess` 在 datagen 时输出。Create 侧：

```java
// …/infrastructure/data/CreateDatagen.java:102-107
private static void providePonderLang(BiConsumer<String, String> consumer) {
	// Register this since FMLClientSetupEvent does not run during datagen
	PonderIndex.addPlugin(new CreatePonderPlugin());

	PonderIndex.getLangAccess().provideLang(Create.ID, consumer);
}
```

CEI 用 registrate 的封装：

```java
// …/data/CEIData.java:37-40
REGISTRATE.registerBuiltinLocalization("interface");
REGISTRATE.registerForeignLocalization();
REGISTRATE.registerPonderLocalization(CEIPonderPlugin::new);
REGISTRATE.registerExtraLocalization(CEIAdvancements::provideLang);
```

> 若不使用 CDP，addon 自己在 datagen 的 `GatherDataEvent` 里照抄 `CreateDatagen` 的 `providePonderLang` 模式即可：先 `PonderIndex.addPlugin(...)`（datagen 期间不会跑 `FMLClientSetupEvent`），再 `PonderIndex.getLangAccess().provideLang(modid, consumer)`。
> ⚠ 待验证：`registerPonderLocalization` / `registerBuiltinLocalization` / `registerForeignLocalization` 属于 registrate（Create 的 fork）还是 CDP；本仓库无 registrate 源码。

---

## 6. UI / 交互：ValueBox 与 ScrollValueBehaviour

### 6.1 整体机制（为什么 addon 几乎零成本）

1. **渲染**：Create 的客户端 tick 里全局调 `ScrollValueRenderer.tick()`（`…/foundation/events/ClientEvents.java:166`），它会遍历所有 `SmartBlockEntity` 的 `ValueSettingsBehaviour` 并 `Outliner.showOutline`（`…/foundation/blockEntity/behaviour/scrollValue/ScrollValueRenderer.java:98`）。
2. **输入**：`ValueSettingsInputHandler` 是全局 `@EventBusSubscriber`，拦截 `PlayerInteractEvent.RightClickBlock`，只要 BE 上有 `ValueSettingsBehaviour` 且命中判定通过就 `cancel` 并进入交互（`…/foundation/blockEntity/behaviour/ValueSettingsInputHandler.java:27-95`）。
3. **同步**：客户端把结果打成 `ValueSettingsPacket` 发到服务端，服务端 `applySettings` 里遍历 behaviour、按 `netId()` 匹配后调 `setValueSettings(...)`（`…/foundation/blockEntity/behaviour/ValueSettingsPacket.java:56-71`）。

**结论：addon 不需要注册任何 renderer、packet 或 listener。**

### 6.2 `ScrollValueBehaviour` 的 builder 链

Create 的真实样例（`…/content/logistics/depot/EjectorBlockEntity.java:113-129`）：

```java
@Override
public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	super.addBehaviours(behaviours);
	behaviours.add(depotBehaviour = new DepotBehaviour(this));

	maxStackSize =
		new ScrollValueBehaviour(CreateLang.translateDirect("weighted_ejector.stack_size"), this, new EjectorSlot())
			.between(0, 64)
			.withFormatter(i -> i == 0 ? "*" : String.valueOf(i));
	behaviours.add(maxStackSize);
	...
}
```

可链式配置的方法（`…/foundation/blockEntity/behaviour/scrollValue/ScrollValueBehaviour.java`）：

| 方法 | 作用 | 行号 |
|---|---|---|
| `between(min, max)` | 取值范围 | `:85-89` |
| `withFormatter(Function<Integer,String>)` | 面板显示文本 | `:96-99` |
| `withCallback(Consumer<Integer>)` | **服务端**值变更回调 | `:80-83` |
| `withClientCallback(Consumer<Integer>)` | 客户端值变更回调（只做本地视觉） | `:75-78` |
| `requiresWrench()` | 只有手持扳手才显示/可点 | `:91-94` |
| `onlyActiveWhen(Supplier<Boolean>)` | 条件显示（例如只在有电时） | `:101-104` |
| `setValue(int)` | 程序内设值（含 clamp / 回调 / 同步） | `:106-114` |
| `getValue()` | 读值 | `:116-118` |

`setValue` 的同步语义（`:106-114`）：

```java
public void setValue(int value) {
	value = Mth.clamp(value, min, max);
	if (value == this.value)
		return;
	this.value = value;
	callback.accept(value);
	blockEntity.setChanged();
	blockEntity.sendData();       // ← 服务端→客户端
}
```

持久化：`isSafeNBT() = true`（`:58-61`），`write/read` 用固定 key `"ScrollValue"`（`:63-73`）。

### 6.3 位置：`ValueBoxTransform`

`ScrollValueBehaviour` 构造器第三个参数就是 `ValueBoxTransform`（`:45-56`）。常用两种：

```java
// 贴在某个面中央（最常见）
new CenteredSideValueBoxTransform((state, dir) ->
    dir.getAxis() != state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING).getAxis())
```

`CenteredSideValueBoxTransform`（`…/foundation/blockEntity/behaviour/CenteredSideValueBoxTransform.java:10-31`）：

```java
public CenteredSideValueBoxTransform(BiPredicate<BlockState, Direction> allowedDirections) { ... }

@Override protected Vec3 getSouthLocation() { return VecHelper.voxelSpace(8, 8, 15.5); }

@Override protected boolean isSideActive(BlockState state, Direction direction) {
	return allowedDirections.test(state, direction);
}
```

`ValueBoxTransform` 抽象契约（`…/foundation/blockEntity/behaviour/ValueBoxTransform.java:20-67`）：

```java
public abstract Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state);
public abstract void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms);
public boolean testHit(LevelAccessor level, BlockPos pos, BlockState state, Vec3 localHit) { ... } // 半径 scale/2 球体
public void transform(...) { ... }        // translate + rotate + scale
public boolean shouldRender(...) { ... }
public float getScale()     { return .5f; }
public float getFontScale() { return 1 / 64f; }
```

内嵌两个抽象类：`Dual`（同面双槽，`:69-93`）与 `Sided`（随面朝向，`:95-141`，`fromSide(Direction)` 切换当前面）。

### 6.4 完整示例：一个"档位"旋钮

```java
public class MyCrusherBlockEntity extends KineticBlockEntity {

    public ScrollValueBehaviour tier;

    public MyCrusherBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tier = new ScrollValueBehaviour(
                CreateLang.translateDirect("my_addon.crusher.tier"),           // label
                this,
                new CenteredSideValueBoxTransform((state, dir) ->                 // 只在四个侧面显示
                        dir.getAxis() != state.getValue(BlockHorizontal.FACING).getAxis()))
            .between(1, 4)
            .withFormatter(i -> "T" + i)
            .withCallback(v -> onTierChanged());                              // 只在服务端触发

        behaviours.add(tier);
        registerAwardables(behaviours, MyAdvancements.UPGRADE_CRUSHER);       // 可选
    }

    private void onTierChanged() {
        // 服务端：重算能力、发数据
        if (level != null && !level.isClientSide)
            sendData();
    }

    public int getTier() {
        return tier == null ? 1 : tier.getValue();
    }
}
```

配套 lang（`assets/<modid>/lang/en_us.json`，或走 registrate 的 `addRawLang`/lang datagen）：

```json
{
  "my_addon.crusher.tier": "Tier"
}
```

### 6.5 三个必须知道的约束

1. **每个 BE 只能有一个 `ScrollValueBehaviour`**：`ValueSettingsPacket` 用 `behaviourIndex != netId()` 选目标，而 `netId()` 默认 `0`（`…/foundation/blockEntity/behaviour/ValueSettingsBehaviour.java:96-98`）；只有 `FilteringBehaviour` 覆写为 `1`（`…/foundation/blockEntity/behaviour/filtering/FilteringBehaviour.java:426-429`）。
   → 需要两个数值槽时，自己写 `ScrollValueBehaviour` 子类并覆写 `netId()`。
2. **`ValueSettingsBehaviour` 在 `RightClickBlock` 里被拦截**（`ValueSettingsInputHandler.java:79-80`），所以方块自身的 `use` / `useItemOn` 要避开鼠标指向值槽的区域，或给 behaviour 加 `bypassesInput(ItemStack)`。
3. `testHit` 的命中半径由 `ValueBoxTransform.getScale()` 决定（`ValueBoxTransform.java:28-33`），槽位贴在方块内壁时注意 `getLocalOffset` 别超出 0..16 体素范围。

### 6.6 其它常用 behaviour

| 类 | 用途 | 典型用法 |
|---|---|---|
| `DirectBeltInputBehaviour` | 允许漏斗/传送带直接插入 | `new DirectBeltInputBehaviour(this)`；`…/content/kinetics/millstone/MillstoneBlockEntity.java:66` |
| `FilteringBehaviour` | 物品过滤槽（自带 ValueBox 渲染） | `new FilteringBehaviour(this, new SawFilterSlot()).forRecipes()`；`…/content/kinetics/saw/SawBlockEntity.java:112` |
| `SidedFilteringBehaviour` | 按面过滤 | `…/content/redstone/thresholdSwitch/ThresholdSwitchBlockEntity.java:309` |
| `SmartFluidTankBehaviour` | 流体槽 + capability | `SmartFluidTankBehaviour.single(this, capacity)`；`…/common/kinetics/grindstone/GrindstoneDrainBlockEntity.java:106` |
| `AdvancementBehaviour` | 触发进度 | `registerAwardables(behaviours, AllAdvancements.MILLSTONE)`；`…/foundation/blockEntity/SmartBlockEntity.java:242-250` |
| `EdgeInteractionBehaviour` / `InvManipulationBehaviour` / `TankManipulationBehaviour` | 机械臂/交互点 | 见 `…/foundation/blockEntity/behaviour/` 目录 |

---

## 7. Goggle tooltip

### 7.1 两个接口

```java
// …/api/equipment/goggles/IHaveGoggleInformation.java:19-28
public non-sealed interface IHaveGoggleInformation extends IHaveCustomOverlayIcon {
	default boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		return false;
	}
	...
}

// …/api/equipment/goggles/IHaveHoveringInformation.java:12-21
public non-sealed interface IHaveHoveringInformation extends IHaveCustomOverlayIcon {
	default boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		return false;
	}
}
```

区别：

- `IHaveGoggleInformation` → **戴护目镜**时的 HUD 面板（`addToGoggleTooltip`）。
- `IHaveHoveringInformation` → 不戴护目镜也显示的准星悬浮信息（`addToTooltip`）。
- 两者都继承 `IHaveCustomOverlayIcon`（`…/api/equipment/goggles/IHaveCustomOverlayIcon.java`），可以覆写 `getIcon(boolean)` 换掉左上角图标（默认 `AllItems.GOGGLES`）。

`KineticBlockEntity` 两个都实现了（`…/content/kinetics/base/KineticBlockEntity.java:56`）。

### 7.2 动力方块默认就显示应力

```java
// …/content/kinetics/base/KineticBlockEntity.java:454-487
@Override
public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
	boolean added = false;

	if (!StressImpact.isEnabled())
		return added;
	float stressAtBase = calculateStressApplied();
	if (Mth.equal(stressAtBase, 0))
		return added;

	CreateLang.translate("gui.goggles.kinetic_stats")
		.forGoggles(tooltip);

	addStressImpactStats(tooltip, stressAtBase);

	return true;
}

protected void addStressImpactStats(List<Component> tooltip, float stressAtBase) {
	CreateLang.translate("tooltip.stressImpact")
		.style(GRAY)
		.forGoggles(tooltip);

	float stressTotal = stressAtBase * Math.abs(getTheoreticalSpeed());

	CreateLang.number(stressTotal)
		.translate("generic.unit.stress")
		.style(ChatFormatting.AQUA)
		.space()
		.add(CreateLang.translate("gui.goggles.at_current_speed")
			.style(ChatFormatting.DARK_GRAY))
		.forGoggles(tooltip, 1);
}
```

`addToTooltip`（悬浮，不看护目镜）只在两种情况下输出（`:417-452`）：

1. `overStressed && AllConfigs.client().enableOverstressedTooltip.get()` → 输出 `gui.stressometer.overstressed` + `gui.contraptions.network_overstressed`；
2. 速度不足要求（`!isSpeedRequirementFulfilled() && getSpeed() != 0`）→ 输出 `tooltip.speedRequirement` + `gui.contraptions.not_fast_enough`。

### 7.3 什么时候要隐藏 / 覆盖

**隐藏应力**：覆写 `IRotate.hideStressImpact()`（默认 `false`，`…/content/kinetics/base/IRotate.java:152-154`）：

```java
// …/content/kinetics/motor/CreativeMotorBlock.java:54
@Override
public boolean hideStressImpact() { return true; }
```

它影响两处：

- 物品 tooltip：`KineticStats.getKineticStats(...)` 用 `showStressImpact` 决定是否输出应力行（`…/foundation/item/KineticStats.java:71-83`）；
- 若应力值为 0，则 `addToGoggleTooltip` 本来就 return false（`…/content/kinetics/base/KineticBlockEntity.java:461-462`），所以"不注册应力值"= 自动不显示。

**覆盖 / 扩展**：叠加自己的段，注意调用 `super` 保留默认行为（CEI 的做法）：

```java
// …/common/kinetics/grindstone/GrindstoneDrainBlockEntity.java:452-457
@Override
public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
    boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
    added |= this.containedFluidTooltip(tooltip, isPlayerSneaking, tank.getCapability());
    return added;
}
```

`containedFluidTooltip` 是接口里的默认实现，直接给出"流体容器 + 容量"面板（`…/api/equipment/goggles/IHaveGoggleInformation.java:30-82`）。

### 7.4 `CreateLang` 与缩进约定

`CreateLang` 是本 mod 的 `LangBuilder` 工厂（`…/foundation/utility/CreateLang.java:24-72`）：

```java
public static MutableComponent translateDirect(String key, Object... args) { return TranslateKeyGenerator...; }
public static LangBuilder builder()                                  { return new LangBuilder(Create.ID); }
public static LangBuilder blockName(BlockState state)                { ... }
public static LangBuilder itemName(ItemStack stack)                  { ... }
public static LangBuilder number(double d)                           { ... }
public static LangBuilder translate(String langKey, Object... args)  { ... }
```

goggle 面板里的关键约定是 **`.forGoggles(tooltip, indent)`**：

```java
CreateLang.translate("gui.goggles.kinetic_stats").forGoggles(tooltip);          // indent 0
CreateLang.translate("tooltip.stressImpact").style(GRAY).forGoggles(tooltip);   // indent 0
CreateLang.number(x).translate("generic.unit.stress").style(AQUA).forGoggles(tooltip, 1); // indent 1
```

因此 addon 自己加段落时，标题用 `forGoggles(tooltip)`，明细用 `forGoggles(tooltip, 1)`，才会和 Create 的面板缩进对齐。

常用 key（都是 Create 已经提供的，无需自己写 lang）：

| key | 含义 |
|---|---|
| `gui.goggles.kinetic_stats` | 转速/应力面板标题 |
| `tooltip.stressImpact` | 应力消耗（SU） |
| `tooltip.capacityProvided` | 应力产出 |
| `gui.goggles.at_current_speed` | "at current speed" |
| `gui.goggles.fluid_container` / `.capacity` | 流体容器 |
| `generic.unit.rpm` / `generic.unit.stress` / `generic.unit.millibuckets` | 单位 |
| `item.create.goggles` 等 | 物品名 |

长文本要折行，用 `TooltipHelper`：

```java
// …/foundation/item/TooltipHelper.java:56-79
public static List<Component> cutTextComponent(Component c, FontHelper.Palette palette);
public static List<Component> cutStringTextComponent(String s, FontHelper.Palette palette);
public static String makeProgressBar(int length, int filledLength);   // :38
public static void addHint(List<Component> tooltip, String hintKey, Object... messageParams); // :30
```

---

## 8. 数据生成（6.0.6 重构后的正确写法）

### 8.1 registrate 已经包了哪些

`CreateRegistrate` 的 `registerEventListeners(modBus)`（`…/foundation/data/CreateRegistrate.java:107-110`）转调 registrate 自身；registrate 内部在 `GatherDataEvent` 时按需添加：

- **blockstate / block model**（来自 `.blockstate(...)`）
- **item model**（来自 `.item().model(...)`）
- **lang**（来自 `.lang(...)`，以及 Create 的 `addDataGenerator(ProviderType.LANG, ...)`）
- **loot table**（来自默认/`.loot(...)`）
- **tags**（来自 `.tag(...)` / `addDataGenerator(ProviderType.BLOCK_TAGS, ...)`）

所以 addon **不需要**手写 `BlockStateProvider` / `ItemModelProvider` / `LanguageProvider` / `BlockLootTables`——除非要自定义动态逻辑。

`CreateDatagen` 里可以看到"Create 自己也只是补 lang 与 tag"：

```java
// …/infrastructure/data/CreateDatagen.java:72-86
private static void addExtraRegistrateData() {
	CreateRegistrateTags.addGenerators();

	Create.registrate().addDataGenerator(ProviderType.LANG, provider -> {
		BiConsumer<String, String> langConsumer = provider::add;
		provideDefaultLang("interface", langConsumer);
		provideDefaultLang("tooltips", langConsumer);
		AllAdvancements.provideLang(langConsumer);
		AllSoundEvents.provideLang(langConsumer);
		AllKeys.provideLang(langConsumer);
		providePonderLang(langConsumer);
		new TagLangGenerator(langConsumer).generate();
	});
}
```

tag 走 `ProviderType.BLOCK_TAGS / ITEM_TAGS / FLUID_TAGS / ENTITY_TAGS`（`…/infrastructure/data/CreateRegistrateTags.java:43-46`）。

### 8.2 `GatherDataEvent` 的挂载点

Create 用**两个优先级**：`HIGHEST` 只做 registrate 相关准备，`LOWEST` 做真正的 provider 注册（`…/Create.java:159-160`）：

```java
modEventBus.addListener(EventPriority.HIGHEST, CreateDatagen::gatherDataHighPriority);
modEventBus.addListener(EventPriority.LOWEST, CreateDatagen::gatherData);
```

```java
// …/infrastructure/data/CreateDatagen.java:33-70
public static void gatherDataHighPriority(GatherDataEvent event) {
	if (event.getMods().contains(Create.ID))
		addExtraRegistrateData();
}

public static void gatherData(GatherDataEvent event) {
	if (!event.getMods().contains(Create.ID)) return;

	DataGenerator generator = event.getGenerator();
	PackOutput output = generator.getPackOutput();
	CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
	...
	GeneratedEntriesProvider generatedEntriesProvider = new GeneratedEntriesProvider(output, lookupProvider);
	lookupProvider = generatedEntriesProvider.getRegistryProvider();     // ← 动态注册表要先挂
	generator.addProvider(event.includeServer(), generatedEntriesProvider);
	...
	generator.addProvider(event.includeServer(), new CreateStandardRecipeGen(output, lookupProvider));
	...
	if (event.includeServer())
		CreateRecipeProvider.registerAllProcessing(generator, output, lookupProvider);
}
```

CEI 的最小版（`…/data/CEIData.java:32-59`）：

```java
@Mod(CEICommon.ID)
public class CEIData {
    public CEIData(IEventBus modBus) {
        if (!DatagenModLoader.isRunningDataGen())
            return;                                     // 只在 datagen 模式实例化
        REGISTRATE.registerBuiltinLocalization("interface");
        REGISTRATE.registerForeignLocalization();
        REGISTRATE.registerPonderLocalization(CEIPonderPlugin::new);
        REGISTRATE.registerExtraLocalization(CEIAdvancements::provideLang);
        modBus.register(this);
    }

    @SubscribeEvent
    public void generate(final GatherDataEvent event) {
        var generator = event.getGenerator();
        var lookupProvider = event.getLookupProvider();
        var output = generator.getPackOutput();
        var server = event.includeServer();

        CEIGenerateEntriesProvider generatedEntriesProvider = new CEIGenerateEntriesProvider(output, lookupProvider);
        lookupProvider = generatedEntriesProvider.getRegistryProvider();   // 必须在其他 provider 之前

        generator.addProvider(server, generatedEntriesProvider);
        generator.addProvider(server, new CEIRecipeProvider(output, lookupProvider));
        generator.addProvider(server, new CEIAdvancements(output, lookupProvider));
    }
}
```

**CEI 的 data 包结构（范例）**：

```
data/
├─ CEIData.java                       # @Mod, GatherDataEvent 挂载；registrate 的 localization 注册
├─ CEIGenerateEntriesProvider.java    # DatapackBuiltinEntriesProvider —— 动态注册表（damage_type 等）
├─ CEIRecipeProvider.java             # 原版 recipe（shaped/shapeless/smithing/processing builder）
└─ CEIConditionalLootTables.java      # 条件掉落表
```

对照 `Create`：`infrastructure/data/CreateDatagen.java` + `infrastructure/data/GeneratedEntriesProvider.java` + `foundation/data/recipe/*`。

### 8.3 `ProcessingRecipe` 在 6.0.6 的变化要点

**变化 A：参数对象化（`ProcessingRecipeParams` 独立成类）**

```java
// …/content/processing/recipe/ProcessingRecipe.java:33-60
public abstract class ProcessingRecipe<I extends RecipeInput, P extends ProcessingRecipeParams> implements Recipe<I> {

	protected P params;
	protected NonNullList<Ingredient> ingredients;
	protected NonNullList<ProcessingOutput> results;
	protected NonNullList<SizedFluidIngredient> fluidIngredients;
	protected NonNullList<FluidStack> fluidResults;
	protected int processingDuration;
	protected HeatCondition requiredHeat;

	public ProcessingRecipe(IRecipeTypeInfo typeInfo, P params) {
		this.params = params;
		this.ingredients = params.ingredients;
		...
	}
```

**变化 B：codec 化（MapCodec + StreamCodec，不再手写 `fromJson/toJson/fromNetwork/toNetwork`）**

```java
// …/content/processing/recipe/StandardProcessingRecipe.java:45-69
public static class Serializer<R extends StandardProcessingRecipe<?>> implements RecipeSerializer<R> {
	private final Factory<R> factory;
	private final MapCodec<R> codec;
	private final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec;

	public Serializer(Factory<R> factory) {
		this.factory = factory;
		this.codec = ProcessingRecipe.codec(factory, ProcessingRecipeParams.CODEC);
		this.streamCodec = ProcessingRecipe.streamCodec(factory, ProcessingRecipeParams.STREAM_CODEC);
	}

	@Override public MapCodec<R> codec() { return codec; }
	@Override public StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() { return streamCodec; }
	public Factory<R> factory() { return factory; }
}
```

校验也进了 codec：`ProcessingRecipe.codec(...)` 里 `.validate(recipe -> ...)`，失败给出"`<类名> failed validation:` + 逐条错误"（`…/content/processing/recipe/ProcessingRecipe.java:215-226`），校验规则来自 `validate()`（`:84-115`）。

**变化 C：JSON 结构合并**（`ProcessingRecipeParams.CODEC`，`…/content/processing/recipe/ProcessingRecipeParams.java:43-65`）

```java
Codec.either(CreateCodecs.SIZED_FLUID_INGREDIENT, Ingredient.CODEC).listOf().fieldOf("ingredients")
Codec.either(FluidStack.CODEC, ProcessingOutput.CODEC).listOf().fieldOf("results")
Codec.INT.optionalFieldOf("processing_time", 0)
HeatCondition.CODEC.optionalFieldOf("heat_requirement", HeatCondition.NONE)
```

即：**物品与流体共用 `ingredients` / `results` 数组**，靠 `Either` 区分。旧的 `"fluidIngredients"` / `"fluidResults"` 字段名在 6.0.x 中不成立。

**变化 D：能力上限从 `getMax*Count()` 声明，由 `validate()` 强制**

```java
// …/content/processing/recipe/ProcessingRecipe.java:64-82
protected abstract int getMaxInputCount();
protected abstract int getMaxOutputCount();
protected boolean canRequireHeat()      { return false; }
protected boolean canSpecifyDuration()  { return false; }
protected int getMaxFluidInputCount()   { return 0; }
protected int getMaxFluidOutputCount()  { return 0; }
```

**变化 E：datagen 有了公开 API 基类 `api/data/recipe/`**

```
api/data/recipe/
├─ BaseRecipeProvider.java            # 通用底座（modid / all 列表 / buildRecipes）
├─ ProcessingRecipeGen.java           # create(...) 系列 + getRecipeType()/getBuilder(id) 抽象
├─ StandardProcessingRecipeGen.java   # 给 ProcessingRecipeParams 方言用，自动 new Builder
├─ MillingRecipeGen / CrushingRecipeGen / ...  # 每种内置类型一个
├─ MechanicalCraftingRecipeGen.java
├─ SequencedAssemblyRecipeGen.java
└─ DatagenMod.java                    # 跨模组 id 辅助（asResource/recipeId/ingotOf...）
```

```java
// …/api/data/recipe/ProcessingRecipeGen.java:34-44, 87-89
public abstract class ProcessingRecipeGen<P extends ProcessingRecipeParams, R extends ProcessingRecipe<?, P>,
		B extends ProcessingRecipeBuilder<P, R, B>> extends BaseRecipeProvider {

	protected GeneratedRecipe create(String namespace, Supplier<ItemLike> singleIngredient, UnaryOperator<B> transform) { ... }

	protected abstract IRecipeTypeInfo getRecipeType();
	protected abstract B getBuilder(ResourceLocation id);
```

```java
// …/api/data/recipe/StandardProcessingRecipeGen.java:22-34
public abstract class StandardProcessingRecipeGen<R extends StandardProcessingRecipe<?>>
		extends ProcessingRecipeGen<ProcessingRecipeParams, R, StandardProcessingRecipe.Builder<R>> {

	protected StandardProcessingRecipe.Serializer<R> getSerializer() { return getRecipeType().getSerializer(); }

	@Override
	protected Builder<R> getBuilder(ResourceLocation id) {
		return new StandardProcessingRecipe.Builder<>(getSerializer().factory(), id);
	}
}
```

用法（Create 自己的 `CreateMillingRecipeGen`，`…/foundation/data/recipe/CreateMillingRecipeGen.java:28-70`）：

```java
public final class CreateMillingRecipeGen extends MillingRecipeGen {
	GeneratedRecipe
		GRANITE = create(() -> Blocks.GRANITE, b -> b.duration(200).output(Blocks.RED_SAND)),
		WOOL    = create("wool", b -> b.duration(100).require(ItemTags.WOOL).output(Items.STRING)),
		BONE    = create(() -> Items.BONE, b -> b.duration(100)
			.output(Items.BONE_MEAL, 3)
			.output(.25f, Items.WHITE_DYE, 1)
			.output(.25f, Items.BONE_MEAL, 3));

	public CreateMillingRecipeGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, Create.ID);
	}
}
```

`getRecipeType()` 已在 `MillingRecipeGen` 里返回 `AllRecipeTypes.MILLING`（`…/api/data/recipe/MillingRecipeGen.java:52-55`）；自定义类型则自己实现。

### 8.4 自定义 ProcessingRecipe（CEI 的 `GrindingRecipe` 范例）

```java
// …/common/kinetics/grindstone/GrindingRecipe.java:46-93
public class GrindingRecipe extends StandardProcessingRecipe<SingleRecipeInput> implements IAssemblyRecipe {
    public GrindingRecipe(ProcessingRecipeParams params) {
        super(CEIRecipes.GRINDING, params);
        if (fluidIngredients.size() + fluidResults.size() > 1)
            throw new IllegalArgumentException("Grinding recipe can only have either 1 fluid input or 1 fluid result");
    }

    public static StandardProcessingRecipe.Builder<GrindingRecipe> builder(ResourceLocation id) {
        return new StandardProcessingRecipe.Builder<>(GrindingRecipe::new, id);
    }

    @Override protected int getMaxInputCount()          { return 1; }
    @Override protected int getMaxOutputCount()         { return 4; }
    @Override protected int getMaxFluidOutputCount()    { return 1; }
    @Override protected boolean canSpecifyDuration()     { return true; }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredients.getFirst().test(input.item());
    }
}
```

序列化器注册（`…/common/registry/CEIRecipes.java:34-47`）：

```java
private static final DeferredRegister<RecipeType<?>> TYPES =
        DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, CEICommon.ID);
private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
        DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, CEICommon.ID);

public static final RecipeTypeInfo<GrindingRecipe> GRINDING =
        register("grinding", () -> new StandardProcessingRecipe.Serializer<>(GrindingRecipe::new));
```

> `RecipeTypeInfo` 是 CDP 的封装；addon 可以直接用 `DeferredHolder<RecipeType<?>, RecipeType<?>>` + `DeferredHolder<RecipeSerializer<?>, ...>`，或实现 Create 的 `IRecipeTypeInfo`（`…/foundation/recipe/IRecipeTypeInfo.java`）。
> 因为 `ProcessingRecipe` 构造器要 `IRecipeTypeInfo`（`…/content/processing/recipe/ProcessingRecipe.java:48`），**自定义 recipe 类要么用 `IRecipeTypeInfo`，要么自己写一个实现它的 record**。

Create 自己的做法是把所有类型塞进一个 enum（`…/AllRecipeTypes.java`，实现 `IRecipeTypeInfo` + `StringRepresentable`），addon 不建议照抄 enum（枚举初始化顺序和 registrate 生命周期容易撞车）。

### 8.5 tag / lang / loot 的 addon 最小写法

```java
// tag：走 registrate 的 ProviderType
REGISTRATE.addDataGenerator(ProviderType.BLOCK_TAGS, prov ->
    prov.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(MyBlocks.MY_CRUSHER.getKey()));

// lang：单条补
REGISTRATE.addRawLang("my_addon.crusher.tier", "Tier");

// lang：整块补（推荐，避免逐条 addRawLang）
REGISTRATE.addDataGenerator(ProviderType.LANG, prov -> {
    prov.add("my_addon.crusher.tier", "Tier");
    prov.add("my_addon.ponder.my_crusher.intro.header", "Rotating with My Crusher");
});
```

```java
// loot：需要特殊掉落时（CEI 掉落"机械磨石 + 物品沥水架"两件）
// …/common/registry/CEIBlocks.java:81-87
.loot((loots, block) -> loots.add(block, LootTable.lootTable()
        .withPool(loots.applyExplosionCondition(MECHANICAL_GRINDSTONE, LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(MECHANICAL_GRINDSTONE))))
        .withPool(...)))
```

---

## 9. CreativeTab 注册

Create 用 `DeferredRegister<CreativeModeTab>` + 自己的 `RegistrateDisplayItemsGenerator`（按 registrate entry → tab 的 `IdentityHashMap` 反查）：

```java
// …/AllCreativeModeTabs.java（字段区）
private static final DeferredRegister<CreativeModeTab> REGISTER =
	DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Create.ID);

public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BASE_CREATIVE_TAB = REGISTER.register("base",
	() -> CreativeModeTab.builder()
		.title(Component.translatable("itemGroup.create.base"))
		.withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
		.icon(() -> AllBlocks.COGWHEEL.asStack())
		.displayItems(new RegistrateDisplayItemsGenerator(true, AllCreativeModeTabs.BASE_CREATIVE_TAB))
		.build());
```

配套机制在 `CreateRegistrate.accept(...)` 里：只要 `setCreativeTab(tab)` 设过，条目就被记进 `TAB_LOOKUP`（`…/foundation/data/CreateRegistrate.java:123-124`），`isInCreativeTab(entry, tab)` 供生成器查询（`:83-85`）。

CEI 走**显式列表**，更简单可靠（`…/common/registry/CEICreativeModeTabs.java:38-78`）：

```java
public class CEICreativeModeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CEICommon.ID);
    public static final Holder<CreativeModeTab> BASE = TABS.register("base", CEICreativeModeTabs::base);

    public static void register(IEventBus modBus) { TABS.register(modBus); }

    private static CreativeModeTab base(ResourceLocation id) {
        return CreativeModeTab.builder()
                .title(CEILang.description("itemGroup", id).component())     // → "itemGroup.create_enchantment_industry.base"
                .icon(BLAZE_ENCHANTER::asStack)
                .displayItems(CEICreativeModeTabs::buildBaseContents)
                .withTabsBefore(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "base"))
                .build();
    }

    private static void buildBaseContents(CreativeModeTab.ItemDisplayParameters p, CreativeModeTab.Output output) {
        output.accept(MECHANICAL_GRINDSTONE);
        ...
        output.accept(EXPERIENCE_CAKE_BASE, TabVisibility.SEARCH_TAB_ONLY);
    }
}
```

**建议**：addon 用 CEI 的显式列表式；或者更省事——把物品塞进 Create 已有 tab：

```java
import static com.simibubi.create.AllCreativeModeTabs.BASE_CREATIVE_TAB;
// 在 FMLCommonSetupEvent / BuildCreativeModeTabContentsEvent 里：
event.getTab().equals(BASE_CREATIVE_TAB.get()) → event.accept(MyBlocks.MY_CRUSHER);
```

（`BuildCreativeModeTabContentsEvent` 是 NeoForge 事件；⚠ 待验证 Create 是否已把 `BASE_CREATIVE_TAB` 暴露为可写 tab —— 它是 `public static final DeferredHolder`，可读可用。）

---

## 10. addon「起步清单」：从空项目到可放置 / 可转动 / 有 tooltip / 有 ponder 的动力方块

按顺序创建（`<pkg>` = 你的包名，`<id>` = modid）：

### 10.1 项目骨架

| # | 文件 | 要点 |
|---|---|---|
| 1 | `build.gradle` | NeoForge ModDevGradle，MC 1.21.1，Java 21；`implementation "com.simibubi.create:create-1.21.1:<ver>"` + `implementation "net.createmod.catnip:..."` + `implementation "dev.engine-room.flywheel:flywheel-neoforge-1.21.1:<ver>"`（坐标见 Create 的 maven）⚠ 待验证具体版本号 |
| 2 | `src/main/resources/META-INF/neoforge.mods.toml` | `modId`、`displayName`、`[[dependencies]]` 里 `modId="create"` + `type="required"`（顺序：neoforge → minecraft → create） |
| 3 | `src/main/java/<pkg>/MyAddon.java` | `@Mod(ID)`；持 `public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(ID).setTooltipModifierFactory(...)`；构造器里 `REGISTRATE.registerEventListeners(modBus)` 后依次调各 `register()` |

### 10.2 方块 + BE

| # | 文件 | 要点 |
|---|---|---|
| 4 | `common/registry/MyBlocks.java` | `BlockEntry` 链：`block → initialProperties(SharedProperties::stone) → properties(noOcclusion) → transform(TagGen.pickaxeOnly()) → blockstate(BlockStateGen.horizontalBlockProvider(true)) → addLayer(cutoutMipped) → item → transform(ModelGen.customItemModel()) → register()`；末尾 `BlockStressValues.IMPACTS.register(..., () -> 4.0)` |
| 5 | `common/MyCrusherBlock.java` | `extends HorizontalKineticBlock implements IBE<MyCrusherBlockEntity>`；实现 `getRotationAxis`、`hasShaftTowards`、`getMinimumRequiredSpeedLevel`、`getBlockEntityClass`、`getBlockEntityType` |
| 6 | `common/MyCrusherBlockEntity.java` | `extends KineticBlockEntity`；`addBehaviours`（`DirectBeltInputBehaviour` + `ScrollValueBehaviour`）；`tick()` 首行 `super.tick()`；覆写 `write/read`（`protected`!） |
| 7 | `common/registry/MyBlockEntities.java` | `blockEntity(name, Ctor::new).visual(() -> MyCrusherVisual::new, false).renderer(() -> KineticBlockEntityRenderer::new).validBlock(MyBlocks.MY_CRUSHER).register()`；同文件加 `@SubscribeEvent registerCapabilities` |
| 8 | `common/registry/MyPartialModelsHolder` 或放 client 包 | 见 10.3 #9；**注意必须在 client 入口显式 `register()`** |
| 9 | `resources/assets/<id>/models/block/my_crusher/block.json` + `/item.json` | `partialBaseModel` / `customItemModel` 期望的路径（`…/foundation/data/AssetLookup.java:27-54`） |
| 10 | `resources/assets/<id>/textures/block/*.png` | 模型贴图 |

### 10.3 客户端（渲染 / Ponder / tooltip）

| # | 文件 | 要点 |
|---|---|---|
| 11 | `client/MyAddonClient.java` | `@Mod(value = ID, dist = Dist.CLIENT)`；构造器里 `MyPartialModels.register()` + `modBus.addListener(MyAddonClient::setup)`；`setup(FMLClientSetupEvent)` 里 `PonderIndex.addPlugin(new MyPonderPlugin())` |
| 12 | `client/MyPartialModels.java` | `PartialModel.of(ResourceLocation.fromNamespaceAndPath(ID, "block/..."))` |
| 13 | `client/MyCrusherVisual.java` | `extends KineticBlockEntityVisual<MyCrusherBlockEntity> implements SimpleTickableVisual`；`instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(...)).createInstance().rotateToFace(Direction.UP, rotationAxis()).setup(be).setPosition(getVisualPosition())`；实现 `update/tick/updateLight/_delete/collectCrumblingInstances` |
| 14 | `client/ponder/MyPonderPlugin.java` | `implements PonderPlugin`；`getModId` / `registerScenes` / `registerTags` |
| 15 | `client/ponder/MyPonderScenes.java` | `helper.withKeyFunction(RegistryEntry::getId)` + `forComponents(...).addStoryBoard("my_crusher/intro", MyCrusherScene::intro, AllCreatePonderTags.KINETIC_APPLIANCES)` |
| 16 | `client/ponder/scene/MyCrusherScene.java` | `new CreateSceneBuilder(builder)`；`title/configureBasePlate/showBasePlate/showSection/setKineticSpeed/rotationDirectionIndicator/showText/markAsFinished` |
| 17 | `resources/assets/<id>/ponder/my_crusher/intro.nbt` | Ponder 场景结构（用 Create 的 `/ponder` 导出工具或手搓 schematic → nbt）⚠ 待验证导出流程 |
| 18 | goggle tooltip | 在 BE 里 `@Override addToGoggleTooltip` 并可选择 `IRotate.hideStressImpact()`（一般不用实现，除发电机） |

### 10.4 数据生成

| # | 文件 | 要点 |
|---|---|---|
| 19 | `data/MyData.java` | `@Mod(ID)`；`if (!DatagenModLoader.isRunningDataGen()) return;`；`modBus.register(this)`；`@SubscribeEvent generate(GatherDataEvent)` |
| 20 | `data/MyGeneratedEntriesProvider.java` | 仅在需要动态注册表（damage_type / 自定义 registry）时；必须**先** `lookupProvider = provider.getRegistryProvider()` 再 add 其它 provider |
| 21 | `data/MyRecipeProvider.java` | 原版配方（`ShapedRecipeBuilder` 等）；或直接用 registrate recipe provider |
| 22 | `data/recipe/MyGrindingRecipeGen.java`（可选） | `extends StandardProcessingRecipeGen<MyRecipe>`，重写 `getRecipeType()` / `getBuilder(id)`；在 `generate` 里 `generator.addProvider(server, gen)` |
| 23 | `resources/assets/<id>/lang/en_us.json` | 只放**非自动**的 key（registrate 会生成 `<block>.lang("...")` 的部分，`addRawLang` 部分自动进 lang） |
| 24 | `resources/data/<id>/tags/...` | 需要跨模组 tag 时手写，或 `ProviderType.BLOCK_TAGS` datagen |

### 10.5 验证顺序（不要跳）

1. `runData` → 检查 `build/generated/.../assets/<id>/blockstates|models|lang` 与 `data/<id>/recipe|tags|loot_table` 是否齐全。
2. `runClient` → 放置方块，看是否绕正确轴旋转；用手摇曲柄/创造马达给转速。
3. 戴护目镜 → 应出现 `Kinetic Stats` + `Stress Impact`（若注册了应力值）。
4. 手持扳手右键值槽 → 应弹出数值面板；改值后重进世界检查持久化（`ScrollValue` NBT）。
5. Ponder 索引里能找到场景；文本无 `missing lang`。
6. 过载测试：加大应力需求直到 `overStressed` → visual 应变红（`applyOverstressEffect`），悬浮信息应出现"overstressed"。

---

## 11. 风险清单 / ⚠ 待验证汇总

| # | 事项 | 影响 | 建议 |
|---|---|---|---|
| 1 | `CDPRegistrate` 的父类与 `provideRegistrate` 行为 | 若照抄 CEI 的 `new CDPRegistrate(ID)` 而不引入 CDP，会编译失败 | 用 `CreateRegistrate.create(ID)`；只有确实要 CDP helper 时才依赖它 |
| 2 | `CDP StressConfig.setImpact` 是否走 `BlockStressValues` 直接注册 | 决定 stress 值是否被 Create config 覆盖 | addon 直接 `BlockStressValues.IMPACTS.register(...)`，绕开 |
| 3 | `assets/<modid>/ponder/<path>.nbt` 的确切路径/扩展名 | Ponder 场景加载不到会静默空场景 | 用 Create 的 ponder 导出/调试流程验证一次 |
| 4 | Ponder lang key 的精确格式（`<modid>.ponder.<path>.header/.text_N`） | 手写 lang 会写错 | 不要手写：用 `PonderIndex.getLangAccess().provideLang(modid, consumer)` |
| 5 | `registerPonderLocalization` / `registerBuiltinLocalization` 的归属（registrate fork vs CDP） | 用错则 NoSuchMethod | 用 Create `CreateDatagen` 的 `providePonderLang` 直连写法 |
| 6 | Flywheel / Catnip / Registrate 的确切 maven 坐标与版本 | build 失败或 API 不匹配 | 从 Create 6.0.10 的 `POM`/`build.gradle` 或 CurseForge 文件页抄 |
| 7 | `AbstractBlockEntityVisual` 的完整契约（`Context`、`partialTick` 语义、`relight` 重载） | visual 编译不过 | 反编译 `flywheel-neoforge-1.21.1` jar 确认 |
| 8 | `BuildCreativeModeTabContentsEvent` 对 Create tab 的写入 | 物品进不了 Create 分页 | 优先自建 tab（CEI 模式） |
| 9 | `ValueSettingsBehaviour.netId()` 多实例限制 | 两个 Scroll 槽互相抢包 | 需要多槽时子类覆写 `netId()` |
| 10 | `IHaveGoggleInformation` 是 `non-sealed`，父接口 `sealed` | 不要试图"实现两个并自定义 icon 逻辑" | 覆写 `getIcon(boolean)` 即可 |

---

## 12. 一页速查：核心 API 索引

```text
# 注册
CreateRegistrate.create(modid)                                    …/foundation/data/CreateRegistrate.java:74
CreateRegistrateRegistrationCallback.register(registry,id,cb)     …/api/registrate/CreateRegistrateRegistrationCallback.java:15
REGISTRATE.block / blockEntity / item / fluid                     …/foundation/data/CreateRegistrate.java:130-260
CreateBlockEntityBuilder.visual / displaySource / displayTarget   …/foundation/data/CreateBlockEntityBuilder.java:65-116
BlockStressValues.IMPACTS / CAPACITIES / RPM                      …/api/stress/BlockStressValues.java:14-22

# 方块 / BE
IBE#getBlockEntityClass / getBlockEntityType                      …/foundation/block/IBE.java:25-27
IBE.onRemove                                                      …/foundation/block/IBE.java:47-56
SmartBlockEntity#addBehaviours / initialize / tick / lazyTick     …/foundation/blockEntity/SmartBlockEntity.java:56-88
SmartBlockEntity#read/write (protected)                           …/foundation/blockEntity/SmartBlockEntity.java:93-120
SyncedBlockEntity#sendData / notifyUpdate                         …/foundation/blockEntity/SyncedBlockEntity.java:59-67
KineticBlockEntity#getGeneratedSpeed / onSpeedChanged / updateSpeed …/content/kinetics/base/KineticBlockEntity.java:281-297
IRotate#getRotationAxis / hideStressImpact                        …/content/kinetics/base/IRotate.java:146-158

# 渲染
AllInstanceTypes.ROTATING                                         …/foundation/render/AllInstanceTypes.java:27
RotatingInstance#setup                                            …/content/kinetics/base/RotatingInstance.java:63
KineticBlockEntityVisual#rotationOffset / applyOverstressEffect   …/content/kinetics/base/KineticBlockEntityVisual.java:23-59
SingleAxisRotatingVisual#of / ofZ / shaft                         …/content/kinetics/base/SingleAxisRotatingVisual.java:42-64
OrientedRotatingVisual#of / backHorizontal                        …/content/kinetics/base/OrientedRotatingVisual.java:43-72
KineticBlockEntityRenderer#renderSafe（兜底）                     …/content/kinetics/base/KineticBlockEntityRenderer.java:47
VisualizationHelper.queueUpdate                                   …/content/kinetics/base/KineticBlockEntity.java:278

# Ponder
PonderIndex.addPlugin                                             …/client/CEIClient.java:43
PonderPlugin 接口                                                 …/foundation/ponder/CreatePonderPlugin.java:20-59
CreateSceneBuilder(effects/world/special)                         …/foundation/ponder/CreateSceneBuilder.java:55-82
setKineticSpeed / rotationDirectionIndicator / createItemOnBelt    …/foundation/ponder/CreateSceneBuilder.java:118-236
AllCreatePonderTags.KINETIC_RELAYS / KINETIC_APPLIANCES / …        …/infrastructure/ponder/AllCreatePonderTags.java:21-41

# UI
ScrollValueBehaviour(between/withFormatter/withCallback/setValue)  …/foundation/blockEntity/behaviour/scrollValue/ScrollValueBehaviour.java:75-118
ValueBoxTransform / Sided / Dual                                   …/foundation/blockEntity/behaviour/ValueBoxTransform.java:20-141
CenteredSideValueBoxTransform                                      …/foundation/blockEntity/behaviour/CenteredSideValueBoxTransform.java:10-31
ValueSettingsPacket#applySettings                                  …/foundation/blockEntity/behaviour/ValueSettingsPacket.java:56-71
ValueSettingsInputHandler（全局右键拦截）                          …/foundation/blockEntity/behaviour/ValueSettingsInputHandler.java:27-95

# Tooltip
IHaveGoggleInformation#addToGoggleTooltip / containedFluidTooltip  …/api/equipment/goggles/IHaveGoggleInformation.java:26-82
IHaveHoveringInformation#addToTooltip                              …/api/equipment/goggles/IHaveHoveringInformation.java:19-21
CreateLang.translate(...).forGoggles(tooltip, indent)              …/foundation/utility/CreateLang.java:24-72
TooltipHelper.cutTextComponent / makeProgressBar                   …/foundation/item/TooltipHelper.java:38-79
KineticStats.getKineticStats                                       …/foundation/item/KineticStats.java:62-142

# 数据生成
CreateDatagen.gatherData / gatherDataHighPriority                  …/infrastructure/data/CreateDatagen.java:33-70
CEIData（最小 addon datagen）                                      …/data/CEIData.java:32-59
StandardProcessingRecipeGen / ProcessingRecipeGen                  …/api/data/recipe/StandardProcessingRecipeGen.java:22
ProcessingRecipeParams.CODEC                                       …/content/processing/recipe/ProcessingRecipeParams.java:43
StandardProcessingRecipe.Serializer                                …/content/processing/recipe/StandardProcessingRecipe.java:45
GrindingRecipe（自定义 recipe 范例）                               …/common/kinetics/grindstone/GrindingRecipe.java:46
```

---

*文档结束。所有结论均可按 `路径:行号` 回溯到 `.research/src/` 下的只读源码；标 ⚠ 的条目需要跨 jar 验证后才能当作定论。*
