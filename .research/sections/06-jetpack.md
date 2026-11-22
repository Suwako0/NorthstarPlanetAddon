# Create Jetpack 5.2.1 / FlightLib 4.0.36 源码技术分析

> 目标环境：**Create Jetpack 5.2.1（NeoForge，MC 1.21.1）+ Create 6.0.10 + FlightLib 4.0.36（jarjar 内嵌）**
> 源码来源（GitHub tag，与本仓库 `.research/src/jetpack` 的反编译产物一致）：
> - Create Jetpack：`.research/src/jetpack-github/CreateJetpack-5.2.1`
> - FlightLib：`.research/src/flightlib-github/FlightLib-4.0.36-1.21.1`
> 用途：作为「**小型 Create 附属模组**」的工程范例——它把 Create 的装备/空气/渲染/配方体系复用得很彻底，而不是重造一套。

---

## 0. 结论速览（先看这 10 条）

1. **它几乎不写渲染与方块逻辑**：`JetpackBlock extends BacktankBlock`（Create 的方块）、BE 直接用 **Create 的 `BacktankBlockEntity`**、渲染直接复用 **`BacktankRenderer` + `SingleAxisRotatingVisual` + Create 的 partial model**。`src/main/kotlin/.../block/JetpackBlock.kt:9-13`；`Content.kt:214-231`。
2. **它是"动力学方块"但零动力学成本**：因为 `BacktankBlock extends HorizontalKineticBlock`，喷气背包方块天然有轴可转；应力值**直接委托给 Create 的铜背罐**：`BlockStressValues.IMPACTS.register(it) { BlockStressValues.getImpact(AllBlocks.COPPER_BACKTANK.get()) }`。`Content.kt:147-151`；`create/src/.../equipment/armor/BacktankBlock.java:54`。
3. **物品也是继承 Create**：`JetpackItem extends BacktankItem implements IJetpack`，空气/耐久/附魔/护甲纹理全部走 Create（`BacktankUtil`、`LayeredArmorItem`、`AllArmorMaterials.COPPER`）。`item/JetpackItem.kt:27-38`。
4. **"空气"不是它自己的流体系统**：它给物品打 `#create:pressurized_air_sources` 标签，Create 的 `BacktankUtil.getAllWithAir` 就会自动把它算作气源，消耗用 `BacktankUtil.canAbsorbDamage(entity, secondsPerTank)`。`Content.kt:185`；`JetpackItem.kt:62-89`；`create/src/.../BacktankUtil.java:40-65, 128-139, 200-206`。
5. **飞行逻辑整体外包给 FlightLib**，喷气背包只实现 `IJetpack` 的 6 个数值方法 + 3 个判定方法。`item/JetpackItem.kt:39-103`。
6. **FlightLib 是 jarjar 内嵌的独立库**：`modInclude(libs.flightlib.neoforge)`，同时 `mods.toml` 里把 `flightlib` 声明为 **required**（因为它随主 jar 一起发）。`build.gradle.kts`；`neoforge.mods.toml`。
7. **配置是"COMMON 配置 + 自定义 payload 同步"**：服务端数值放 `ModConfig.Type.COMMON`，登录时和 `ModConfigEvent.Reloading` 时发 `SyncConfigMessage`，客户端用 `SyncedConfig` 覆盖本地值。`config/Configs.kt:45-62`；`config/SyncConfigMessage.kt`。
8. **客户端入口是独立的 `@Mod(dist = Dist.CLIENT)`**——这正是 5.2.1 的修复点（"client-only classes loaded for config screen on server"）。`NeoForgeClientEntrypoint.java`。
9. **扩展 Create 配方的标准范式**：自定义 `CopyComponentsMechanicalCraftingRecipe extends MechanicalCraftingRecipe`，用 `REGISTRATE.generic(name, Registries.RECIPE_SERIALIZER, ...)` 注册 serializer，JSON 里 `"type": "create_jetpack:copy_components_mechanical_crafting"`。`CopyComponentsMechanicalCraftingRecipe.kt`；`data/create_jetpack/recipe/jetpack.json`。
10. **代价是 Kotlin 全家桶**：硬依赖 `kotlinforforge [5.8,)`，并用第三方 Gradle 插件 `com.possible-triangle.neoforge`。**我们做动力学元件不需要付这个代价**（见第 7 章）。

---

## 1. 工程结构

### 1.1 三入口拆分

| 文件 | 作用 |
|---|---|
| `NeoForgeEntrypoint.java` | 主入口：`@Mod(MOD_ID)`，注册 registrate 事件监听 + `Content.register(container, modBus)` |
| `NeoForgeClientEntrypoint.java` | 客户端入口：`@Mod(value = MOD_ID, dist = Dist.CLIENT)`，注册按键显示与 Create 风格配置界面 `BaseConfigScreen` |
| `Constants.java` | `MOD_ID`、`LOGGER`、`REGISTRATE`（**自己 `CreateRegistrate.create`，不用 Create 的实例**） |

```java
public class Constants {
    public static final String MOD_ID = "create_jetpack";
    public static final Logger LOGGER = LogManager.getLogger("Create Jetpack");
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID);
}
```
`src/main/java/.../Constants.java:11-16`

```java
@Mod(Constants.MOD_ID)
public class NeoForgeEntrypoint {
    public NeoForgeEntrypoint(ModContainer container, IEventBus modBus) {
        Constants.REGISTRATE.registerEventListeners(modBus);
        Content.INSTANCE.register(container, modBus);
    }
}
```
`src/main/java/.../NeoForgeEntrypoint.java:8-15`

> 对照 Create 自己的 `Create.registrate()`（会检查调用方包名并抛 `UnsupportedOperationException`，`create/Create.java:213-217`），**addon 必须自建 registrate**，这一点和本文档 `CREATE-ADDON-CASE-STUDIES.md` 里 CEI 的做法一致。

### 1.2 集中注册：`Content.kt`

`Content.kt` 是一个 `object`（Kotlin 单例），在 `init` 块里先设置 registrate 的 tooltip 工厂：

```kotlin
object Content {
    init {
        REGISTRATE
            .defaultCreativeTab(null as ResourceKey<CreativeModeTab>?)
            .setTooltipModifierFactory {
                ItemDescription
                    .Modifier(it, FontHelper.Palette.STANDARD_CREATE)
                    .andThen(TooltipModifier.mapNull(KineticStats.create(it)))
            }
    }
```
`Content.kt:54-63`

这行 `KineticStats.create(it)` **直接复用 Create 的 goggle/JEI 应力统计 tooltip**——因为它的方块是动力学方块，物品 tooltip 也应该显示应力。**我们的动力学元件同理，应当照抄这行**。

注册清单（`Content.kt`）：

| 内容 | 方式 | 行 |
|---|---|---|
| 自定义配方 serializer | `REGISTRATE.generic(..., Registries.RECIPE_SERIALIZER, ...)` | 65-71 |
| 喷气背包物品 | `REGISTRATE.item("jetpack", ...).jetpackProperties()` | 73-84 |
| 可放置物品（`BacktankBlockItem`） | `.jetpackPlaceableProperties()` | 86-95 |
| 喷气背包方块 | `REGISTRATE.block("jetpack", ...).jetpackTransforms { }` | 97-102 |
| 下界合金版本（物品/放置物/方块） | 同上 ×3 | 104-134 |
| 方块实体 | `REGISTRATE.blockEntity("jetpack", BlockEntityFactory(::BacktankBlockEntity))` | 214-231 |
| 配置 + 网络 + 事件 | `Content.register(container, modBus)` | 233-260 |

**关键**：BE 用了 **Create 的类**：

```kotlin
val JETPACK_BLOCK_ENTITY =
    REGISTRATE
        .blockEntity("jetpack", BlockEntityBuilder.BlockEntityFactory(::BacktankBlockEntity))
        .visual {
            SimpleBlockEntityVisualizer.Factory { ctx, be, f ->
                val model = Models.partial(
                    if (be.blockState.`is`(NETHERITE_JETPACK_BLOCK)) AllPartialModels.NETHERITE_BACKTANK_SHAFT
                    else AllPartialModels.COPPER_BACKTANK_SHAFT,
                )
                SingleAxisRotatingVisual(ctx, be, f, model)
            }
        }.validBlocks(JETPACK_BLOCK, NETHERITE_JETPACK_BLOCK)
        .renderer { NonNullFunction { BacktankRenderer(it) } }
        .register()
```
`Content.kt:214-231`

> **这是"复用型附属"的核心技巧**：一个 BE 类型可以 `validBlocks` 多个方块；Visual 与 Renderer 都可以直接用 Create 的实现，只要你的方块状态结构与它兼容（都继承 `BacktankBlock` 即满足）。

### 1.3 构建系统

`build.gradle.kts` 使用第三方插件 `com.possible-triangle.neoforge`（作者自研），要点：

```kotlin
plugins { id("com.possible-triangle.neoforge") }
withKotlin()
neoforge { dataGen() }

dependencies {
    modInclude(libs.flightlib.neoforge)          // ★ jarjar 内嵌
    modImplementation(libs.registrate)
    modImplementation(variantOf(libs.create) { classifier("slim") }) { isTransitive = false }
    modImplementation(libs.ponder)
    modCompileOnly(libs.flywheel)
    modCompileOnly(libs.flightlib.api)
    modCompileOnly(libs.flightlib.neoforge.api)
}
```
`build.gradle.kts`

可复用仓库列表（Create 附属必看）：

```kotlin
maven("https://maven.createmod.net")  // create / ponder / flywheel
maven("https://mvn.devos.one/snapshots") // registrate
maven("https://maven.blamejared.com/")  // jei
maven("https://maven.theillusivec4.top/") // curios / caelus
```

> **注意 `classifier("slim")`**：Create 6.0.10 在 maven 上有 `create-1.21.1-6.0.10-XXX-slim.jar`（不带部分资源），Jetpack 用 slim 减小开发依赖体积。**我们已有完整 jar，无需此技巧**。

### 1.4 `mods.toml` 依赖声明

```toml
[[dependencies.${mod_id}]]
    modId = "create";  mandatory = true;  versionRange = "[6.0.0,)";  side = "BOTH"

[[dependencies.${mod_id}]]
    modId = "flightlib"; mandatory = true; versionRange = "[4.0.36,)"; side = "BOTH"

[[dependencies.${mod_id}]]
    modId="kotlinforforge"; mandatory=true; versionRange="[5.8,)"; side="BOTH"
```
`src/main/resources/META-INF/neoforge.mods.toml`

> **jarjar 库也要写成 required**（它随主 jar 一起分发，玩家不需要单独装）。这是 NeoForge jarjar 的正确配套。

---

## 2. 与 Create 的耦合点（全量）

`grep '^import com.simibubi.create'` 的结果（去重）：

| Create 类 | 用途 | 出处 |
|---|---|---|
| `foundation/data/CreateRegistrate` | 自建 registrate 实例 | `Constants.java:3` |
| `foundation/data/SharedProperties` | `copperMetal()` / `netheriteMetal()` 方块属性 | `Content.kt:100, 132` |
| `foundation/data/TagGen` | `pickaxeOnly()` 采掘标签 | `Content.kt:146` |
| `foundation/item/ItemDescription` | Create 风格 tooltip 段落 | `Content.kt:59-61` |
| `foundation/item/TooltipModifier` | tooltip 修饰器注册 | `Content.kt:61` |
| `foundation/item/KineticStats` | **goggle 应力统计 tooltip** | `Content.kt:61` |
| `foundation/item/LayeredArmorItem` | 分层护甲纹理（下界合金版走 `JetpackItem.Layered`） | `JetpackItem.kt:12, 110-124` |
| `foundation/particle/AirParticleData` | 推进粒子 | `JetpackItem.kt:13, 100` |
| `api/stress/BlockStressValues` | **应力值注册（委托给铜背罐）** | `Content.kt:11, 147-151` |
| `content/equipment/armor/AllArmorMaterials` | 复用 Create 的护甲材质档（`COPPER`） | `Content.kt:12, 78` |
| `content/equipment/armor/BacktankBlock` | 方块基类 | `JetpackBlock.kt:5, 11` |
| `content/equipment/armor/BacktankBlockEntity` | **直接复用 BE** | `Content.kt:13, 216` |
| `content/equipment/armor/BacktankItem` / `BacktankBlockItem` | 物品基类 / 可放置物品 | `JetpackItem.kt:10`；`Content.kt:14, 89` |
| `content/equipment/armor/BacktankRenderer` | **直接复用渲染器** | `Content.kt:15, 230` |
| `content/equipment/armor/BacktankUtil` | **空气系统（读/耗/上限/护甲槽扫描）** | `JetpackItem.kt:11, 67, 85-87` |
| `content/kinetics/base/SingleAxisRotatingVisual` | **Flywheel 旋转 Visual** | `Content.kt:17, 227` |
| `content/kinetics/crafter/MechanicalCraftingRecipe` | **扩展机械合成配方** | `CopyComponentsMechanicalCraftingRecipe.kt:6` |
| `AllTags.AllItemTags` | `PRESSURIZED_AIR_SOURCES` 标签 | `Content.kt:10, 185` |
| `com.simibubi.create.*`（通配角） | `AllBlocks` / `AllPartialModels` / `AllDataComponents` / `AllCreativeModeTabs` / `Create.asResource` | `Content.kt:9` 等 |

### 2.1 空气 / 背罐机制：真正值得抄的地方

Create 的 `BacktankUtil` 有一个**公开的供应器扩展点**：

```java
/**
 * Use this method to add custom entry points to the backtank item stack supplier, e.g. getting them from custom
 * slots or items.
 */
public static void addBacktankSupplier(Function<LivingEntity, List<ItemStack>> supplier) {
    BACKTANK_SUPPLIERS.add(supplier);
}
```
`create/src/.../content/equipment/armor/BacktankUtil.java:200-206`

默认供应器扫描护甲槽并按 `#create:pressurized_air_sources` 标签筛选：

```java
for (ItemStack itemStack : entity.getArmorSlots())
    if (AllTags.AllItemTags.PRESSURIZED_AIR_SOURCES.matches(itemStack))
        stacks.add(itemStack);
```
`BacktankUtil.java:41-47`

消耗入口（"用一格气换一次动作"）：

```java
public static boolean canAbsorbDamage(LivingEntity entity, int usesPerTank) {
    if (usesPerTank == 0) return true;
    if (entity instanceof Player p && p.isCreative()) return true;
    List<ItemStack> backtanks = getAllWithAir(entity);
    if (backtanks.isEmpty()) return false;
    int cost = Math.max(maxAirWithoutEnchants() / usesPerTank, 1);
    consumeAir(entity, backtanks.getFirst(), cost);
    return true;
}
```
`BacktankUtil.java:128-139`

Jetpack 侧只需要：

```kotlin
override fun isUsable(context: Context): Boolean {
    val tank = BacktankUtil.getAllWithAir(context.entity).firstOrNull() ?: return false
    val air = BacktankUtil.getAir(tank)
    val cost = BacktankUtil.maxAirWithoutEnchants().toFloat() / secondsPerTank(FlightAction.UP)
    return air >= cost
}

override fun onUse(context: Context, action: FlightAction) {
    if (context.level.gameTime % 20 != 0L) return     // 每秒扣一次
    BacktankUtil.canAbsorbDamage(context.entity, secondsPerTank(action))
}
```
`item/JetpackItem.kt:84-89, 62-68`

加上注册期的两个"声明"：

```kotlin
tag(ItemTags.CHEST_ARMOR)
tag(AllItemTags.PRESSURIZED_AIR_SOURCES.tag)   // ★ 让 Create 认它是气源
.tab(AllCreativeModeTabs.BASE_CREATIVE_TAB.key!!) { context, mod ->
    mod.accept(ItemStack(context.get()).apply {
        set(AllDataComponents.BACKTANK_AIR, BacktankUtil.maxAirWithoutEnchants())  // 创造物品栏给满气
    })
}
```
`Content.kt:184-192`

> **结论**：**不要自己造"压缩空气流体"**。给物品打 `#create:pressurized_air_sources` 标签 + 用 `BacktankUtil` 读写 `AllDataComponents.BACKTANK_AIR`，就自动获得：护甲槽扫描、排序（优先后消耗空罐）、创造模式免耗、护甲条显示、Curios 兼容（由 Create 的 Curios 集成补供应器）。**我们的"喷气背包式/呼吸设备式"元件可以直接照抄这一套。**

### 2.2 数据组件与掉落物保留

方块掉落时把 BE 的空气数据搬回物品：

```kotlin
loot { lt, block ->
    val builder = LootTable.lootTable()
    val survivesExplosion = ExplosionCondition.survivesExplosion()
    lt.add(block, builder.withPool(
        LootPool.lootPool().`when`(survivesExplosion).setRolls(ConstantValue.exactly(1F))
            .add(LootItem.lootTableItem(getItem())
                .apply(CopyComponentsFunction
                    .copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                    .include(AllDataComponents.BACKTANK_AIR)))
    ))
}
```
`Content.kt:153-174`

这**等价于** Create 自己的 `BuilderTransformers.backtank(...)` 里那段 loot 逻辑（`create/src/.../foundation/data/BuilderTransformers.java:441-456`）。**因为是照抄，所以行为完全一致**（含爆炸不摧毁的判定）。

> 我们的方块如果带"状态数据"（温度、充能、库存），掉落物保留就用 `CopyComponentsFunction.copyComponents(Source.BLOCK_ENTITY).include(...)`，别自己写 `dropContents`。

### 2.3 模型复用

方块模型直接以 Create 的背罐模型为 `parent`：

```json
{
  "credit": "Made with Blockbench",
  "parent": "create:block/copper_backtank/block",
  "textures": { "particle": "#0" },
  "elements": [ ... ]
}
```
`assets/create_jetpack/models/block/jetpack/block.json:1-6`

注册期再包一层贴图：

```kotlin
blockstate { c, p ->
    val model = p.models()
        .withExistingParent("block/${c.name}", p.modLoc("block/jetpack/block"))
        .texture("0", "block/${c.name}")
    p.horizontalBlock(c.entry, model)
}
```
`Content.kt:138-145`

> **对我们非常有用**：新增动力学元件时，若外形接近某个 Create 方块（轴、齿轮、机壳），`parent` 到 Create 的模型 + 换一张贴图，工作量骤降，风格还天然一致。注意模型必须保留 `#0` 这类贴图槽位定义。

---

## 3. 飞行逻辑（FlightLib）概览

### 3.1 分层

```
CreateJetpack（薄）
  JetpackItem : BacktankItem, IJetpack        ← 只提供数值与判定
  Content.register: 注册 item capability      ← NeoForgeFlightLib.ITEM_CAPABILITY
        │
        ▼
FlightLib（厚，独立库）
  api/      IJetpack、ISource、IFlightApi、FlightKey、FlightAction、ControlType、FlyingPose
  common/   JetpackLogic、ControlManager、ControlSender、ISettingsStorage、FlightLibNetwork
  neoforge/ NeoForgeEntrypoint、ForgeNetwork、ForgeRegistries、ForgeDataAttachment、ForgeSources、CuriosCompat
```

能力注册（让 `stack.item as IJetpack` 可取到喷气背包）：

```kotlin
modBus.addListener { event: RegisterCapabilitiesEvent ->
    event.registerItem(NeoForgeFlightLib.ITEM_CAPABILITY, { stack, _ ->
        stack.item as IJetpack
    }, JETPACK_ITEM, NETHERITE_JETPACK_ITEM)
}
```
`Content.kt:255-259`

### 3.2 `IJetpack` 契约（节选）

```kotlin
interface IJetpack {
    data class Context(val jetpack: IJetpack, val entity: LivingEntity,
                       val level: Level, val pose: FlyingPose, val source: ISource)

    fun activeType(context: Context): ControlType = ControlType.ALWAYS
    fun horizontalSpeed(context: Context): Double
    fun verticalSpeed(context: Context): Double
    fun acceleration(context: Context): Double
    fun hoverType(context: Context): ControlType
    fun hoverSpeed(context: Context): Double
    fun swimModifier(context: Context): Double
    fun elytraBoost(): Double = 1.25
    fun isValid(context: Context): Boolean
    fun isUsable(context: Context): Boolean
    fun onUse(context: Context, action: FlightAction)
    fun getThrusters(context: Context): List<Vec3>?   // null = 不画粒子
    fun createParticles(): ParticleOptions
}
```
`flightlib-api/src/main/kotlin/.../api/IJetpack.kt`

`ControlType` 三态：`ALWAYS` / `NEVER` / `TOGGLE`（`api/ControlType.kt`）；`FlightAction` 五种：`UP / DOWN / HOVER / BOOST_ELYTRA / BOOST_SWIMMING`（`api/FlightAction.kt`）。

Jetpack 的实现就是把配置塞进这些方法：

```kotlin
override fun hoverSpeed(context: Context): Double = Configs.SERVER.hoverSpeed
override fun verticalSpeed(context: Context): Double = Configs.SERVER.verticalSpeed
override fun activeType(context: Context): ControlType = ControlType.TOGGLE
override fun acceleration(context: Context): Double = Configs.SERVER.acceleration
override fun elytraBoost(): Double = Configs.SERVER.elytraBoost
```
`item/JetpackItem.kt:39-53`

### 3.3 "气源"抽象（`ISource`）

```kotlin
interface ISource {
    fun isDisabled(context: IJetpack.Context) = false
    fun interface Caster { fun get(value: Any): List<() -> IJetpack?> }
    fun interface Provider { fun get(entity: LivingEntity): List<Pair<Any, ISource>> }
}
```
`api/ISource.kt`

内置两类来源（`api/sources/`）：`EquipmentSource`（护甲槽）、`CuriosSource`（饰品槽）、`EntitySource`、`TrinketsSource`。判定示例：

```kotlin
override fun isValid(context: Context): Boolean =
    when (val source = context.source) {
        is EquipmentSource -> source.slot == EquipmentSlot.CHEST
        is CuriosSource -> true
        else -> false
    }
```
`item/JetpackItem.kt:77-82`

`EquipmentSource` 还处理冷却：

```kotlin
override fun isDisabled(context: IJetpack.Context): Boolean {
    val entity = context.entity
    return entity is Player && entity.cooldowns.isOnCooldown(stack.item)
}
```
`api/sources/EquipmentSource.kt`

> **对我们的启示**：如果你要做一个"随玩家装备生效"的 Create 元件（例如"工程师护目镜式强化"、"便携应力表"），FlightLib 的 `ISource` 抽象（护甲槽 + 饰品槽 + 实体）是很好的参考模型，但**不必引入这个库**——它的价值主要在飞行数学。

### 3.4 NeoForge 侧接线

```java
@Mod(Constants.MOD_ID)
public class NeoForgeEntrypoint {
    public NeoForgeEntrypoint(IEventBus modBus, Dist dist) {
        CommonClass.init();
        if (dist == Dist.CLIENT) clientInit(modBus);
        ForgeNetwork.register(modBus);
        ForgeRegistries.register(modBus);
        ForgeSources.register();
        CuriosCompat.register();
        ForgeDataAttachment.register(modBus);

        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Pre e) -> JetpackLogic.INSTANCE.onTick(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) ControlManager.INSTANCE.load(p);
        });
        // ChangedDimension / Respawn 同样 load(p)
    }
}
```
`flightlib-neoforge/src/main/java/.../NeoForgeEntrypoint.java`

状态持久化用 **NeoForge Data Attachment**：

```kotlin
val SETTINGS_ATTACHMENT = ATTACHMENT_TYPES.register("settings") { _ ->
    AttachmentType.builder(ISettingsStorage::DEFAULT)
        .serialize(ISettingsStorage.KEYS_CODEC)
        .copyOnDeath()
        .build()
}
```
`flightlib-neoforge/src/main/kotlin/.../ForgeDataAttachment.kt`

> `copyOnDeath()` 是关键：玩家的"引擎开关 / 悬停模式"状态在死亡后保留。**任何"玩家级开关状态"都应放 Data Attachment 而不是自己存文件。**

---

## 4. 配置体系（可直接照抄的模板）

### 4.1 三件套

```
IServerConfig（接口：所有可同步字段）
  ├── ServerConfig  ← ModConfigSpec 实现（本地文件）
  └── SyncedConfig  ← 纯 data class（网络解码结果）
Configs.SERVER = SYNCED_SERVER ?: LOCAL_SERVER   ← 单机/联机自动切换
```

```kotlin
object Configs {
    var SERVER_SPEC: ModConfigSpec; private set
    private var LOCAL_SERVER: ServerConfig
    internal var SYNCED_SERVER: IServerConfig? = null
    val SERVER: IServerConfig get() = SYNCED_SERVER ?: LOCAL_SERVER

    var CLIENT_SPEC: ModConfigSpec; private set
    var CLIENT: ClientConfig; private set
}
```
`config/Configs.kt:9-37`

### 4.2 配置项写法（NeoForge `ModConfigSpec`）

```kotlin
private val secondsPerTankValue =
    builder.defineInRange("air.seconds_per_tank", 450, 1, Integer.MAX_VALUE)
override val secondsPerTank get() = secondsPerTankValue.get()

private val verticalSpeedValue = builder.defineInRange("speed.vertical", 0.4, 0.01, 100.0)
private val heightAboveGroundLimitValue = builder.define("heightAboveGroundLimit", -1)
override val heightAboveGroundLimit: Int?
    get() = heightAboveGroundLimitValue.get().takeUnless { it < 0 }   // -1 = 不限制
```
`config/ServerConfig.kt`

> 命名用 `分组.字段`（`air.*` / `speed.*` / `features.*`），NeoForge 会自动建子目录。`-1` 表示"关掉该功能"是常见约定。

### 4.3 同步包（自定义 payload）

```kotlin
object Network {
    fun register(event: RegisterPayloadHandlersEvent) {
        val registrar = event.registrar("2.0")          // ★ 版本号
        registrar.playToClient(SyncConfigMessage.TYPE.type(), SyncConfigMessage.TYPE.codec()) { message, _ ->
            message.handle()
        }
    }
}
```
`config/Configs.kt:45-62`

```kotlin
class SyncConfigMessage(private val config: IServerConfig) : CustomPacketPayload {
    override fun type() = TYPE.type()
    companion object {
        val TYPE = CustomPacketPayload.TypeAndCodec(
            CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath(MOD_ID, "sync_config")),
            StreamCodec.of(SyncConfigMessage::encode, SyncConfigMessage::decode),
        )
        // encode/decode 手写 FriendlyByteBuf 读写（字段顺序必须严格对称）
    }
    fun handle() { Configs.SYNCED_SERVER = config }
}
```
`config/SyncConfigMessage.kt`

触发点：

```kotlin
FORGE_BUS.addListener { event: PlayerEvent.PlayerLoggedInEvent ->
    val player = event.entity
    if (player is ServerPlayer) Configs.syncConfig(player)
}
modBus.addListener { event: ModConfigEvent.Reloading ->
    if (event.config.type == ModConfig.Type.COMMON) Configs.syncConfig()
}
```
`Content.kt:244-253`

> **对我们的价值**：动力学元件的参数（应力值、转速上限）**必须在服务端生效并同步给客户端**，否则 goggle/ponder 显示会与真实值不符。这套"接口 + 本地实现 + 同步实现"的三件套是最小成本方案。

---

## 5. 资源与数据（无 datagen 的纯手写路线）

Jetpack **没有用 Create 的 datagen**，资源全是手写 JSON：

```
assets/create_jetpack/
├── icon.png
├── lang/{de_de,es_es,fr_fr,hu_hu,ja_jp,ko_kr,ru_ru,tr_tr,zh_cn}.json
├── models/block/jetpack/{block,item,thrusters}.json
└── textures/{block/jetpack.png, block/netherite_jetpack.png, gui/air_indicator.png, gui/controls.png}

data/
├── create_jetpack/recipe/{jetpack,netherite_jetpack,netherite_jetpack_upgrade,...}.json
├── create_jetpack/cold_sweat/item/insulator/netherite_jetpack.json
├── cold_sweat/tags/cold_sweat/item/insulator/drains_backtank.json
└── ...
```

语言与文案用 `addRawLang`（注册期直接塞 key，不需要 lang 文件生成）：

```kotlin
owner.addRawLang("item.${REGISTRATE.modid}.$name.tooltip", "")
owner.addRawLang("item.${REGISTRATE.modid}.$name.tooltip.summary", "Allows levitation using pressurized air")
owner.addRawLang("item.${REGISTRATE.modid}.$name.tooltip.control1", "Press [JUMP]")
owner.addRawLang("item.${REGISTRATE.modid}.$name.tooltip.action1", "Fly upwards")
```
`Content.kt:194-206`

> 这是 Create 的 `ItemDescription` tooltip 段落键格式（`tooltip.summary` / `tooltip.controlN` / `tooltip.actionN`）。**我们的动力学元件若想有 Create 风格的物品说明，照这个键格式写即可。**

配方（机械合成 + 组件复制）：

```json
{
  "type": "create_jetpack:copy_components_mechanical_crafting",
  "accept_mirrored": true,
  "pattern": [" PSP ", "PYXYP", "PCECP", " C C "],
  "key": { "X": { "item": "create:copper_backtank" }, ... },
  "result": { "id": "create_jetpack:jetpack" }
}
```
`data/create_jetpack/recipe/jetpack.json`

下界合金升级用原版 `smithing_transform`（`data/create_jetpack/recipe/netherite_jetpack_upgrade.json`）。

---

## 6. 扩展 Create 配方的范式（`CopyComponentsMechanicalCraftingRecipe`）

需求：用 Create 的**机械合成台**做喷气背包，同时**把原料背罐里的空气带过来**。

做法：继承 Create 的 `MechanicalCraftingRecipe`，在 `assemble` 里找背罐并复制组件。

```kotlin
class CopyComponentsMechanicalCraftingRecipe(
    group: String, category: CraftingBookCategory, pattern: ShapedRecipePattern,
    recipeOutput: ItemStack, acceptMirrored: Boolean,
) : MechanicalCraftingRecipe(group, category, pattern, recipeOutput, acceptMirrored) {

    override fun getSerializer(): RecipeSerializer<*> = Content.COPY_NBT_MECHANICAL_CRAFTING_SERIALIZER.get()

    private fun findUpgradable(container: CraftingInput): ItemStack? {
        val width = min(container.width(), width)
        for (x in 0 until width) for (y in 0 until min(container.height(), height)) {
            val stack = container.getItem(x + y * width)
            if (stack.item is BacktankItem) return stack
        }
        return null
    }

    override fun assemble(container: CraftingInput, registries: HolderLookup.Provider): ItemStack {
        val upgradeTarget = findUpgradable(container)
        val upgraded = super.assemble(container, registries)
        upgradeTarget?.let { upgraded.applyComponents(it.components) }   // ★ 组件迁移
        return upgraded
    }

    object Serializer : MechanicalCraftingRecipe.Serializer() {
        private val CODEC: MapCodec<MechanicalCraftingRecipe> =
            MechanicalCraftingRecipe.Serializer.CODEC.xmap(Serializer::from, Serializer::to)
        // override fun codec() = CODEC
    }
}
```
`CopyComponentsMechanicalCraftingRecipe.kt:18-63`

注册 serializer（**不用手写 DeferredRegister**）：

```kotlin
val COPY_NBT_MECHANICAL_CRAFTING_SERIALIZER =
    REGISTRATE.generic("copy_components_mechanical_crafting", Registries.RECIPE_SERIALIZER) {
        CopyComponentsMechanicalCraftingRecipe.Serializer
    }.register()
```
`Content.kt:65-71`

> **这是"addon 扩展 Create 配方"的教科书做法**：
> 1. 继承 Create 的配方类（不重造）；
> 2. 只覆盖 `assemble`（或 `matches`）；
> 3. 用 `Serializer` 的 `CODEC.xmap` 复用父类编解码，避免手写（容易和 Create 版本升级脱节）；
> 4. 用 `REGISTRATE.generic(...)` 注册。
>
> 我们的新机器如果要走 Create 的处理配方体系，同理：继承 `StandardProcessingRecipe` / `ProcessingRecipe`，**只改数据与行为**，别自己写 `RecipeType` 全家桶。

⚠ **注意**：Create 6.0.6 重构了 `ProcessingRecipe` 系列（`API Changes: Reworked ProcessingRecipe`，见 Create 6.0.6 changelog）。**版本区间要写 `[6.0.6,)` 或更精确**，否则老版本会炸。

---

## 7. 对我们（Create 动力学元件）的可复用结论

### 7.1 直接照抄

| 项 | 为什么 |
|---|---|
| `CreateRegistrate.create(MOD_ID)` + `registerEventListeners(modBus)` | Create 的 registrate 实例对外封闭 |
| `setTooltipModifierFactory(ItemDescription + KineticStats.create(item))` | 动力学元件的物品 tooltip 必须显示应力 |
| `.visual { SingleAxisRotatingVisual(...) }` + `.renderer { ... }` 双注册 | Flywheel 可用时走 Visual，不可用时走 BER |
| BE `validBlocks(a, b, ...)` | 一个 BE 类型服务多个方块变体 |
| `BlockValues.IMPACTS.register(block) { BlockValues.getImpact(其他方块) }` | **委托式应力值**——比硬编码更容易与 Create 平衡 |
| `CopyComponentsFunction.copyComponents(Source.BLOCK_ENTITY)` loot | 状态数据随方块掉落保留 |
| `addRawLang` + `tooltip.summary/controlN/actionN` | Create 风格物品说明 |
| 自建 config 三件套 + payload 同步 | 参数必须服务端权威 + 客户端可见 |
| 独立 `@Mod(dist = CLIENT)` 入口 | 避免客户端类在服务端加载 |

### 7.2 不要照抄

| 项 | 原因 |
|---|---|
| **Kotlin + kotlinforforge 硬依赖** | 动力学元件逻辑量不大，Java 足够；多一个硬依赖 = 多一个玩家装不上的风险（5.2.1 还因此出过"服务端加载客户端类"的 bug） |
| **第三方 Gradle 插件 `com.possible-triangle.neoforge`** | 我们已有 `net.neoforged.moddev` 2.0.144，功能完整 |
| **jarjar 内嵌 FlightLib** | 我们的元件不需要额外库；jarjar 会带来重定位与冲突排查成本 |
| **纯手写全部 JSON** | Create 6.0.6 起 datagen 有清晰 addon API；我们应走 `runData`（`build.gradle` 已有 `data` run 配置） |
| **用 `Create.asResource(...)` 引用 Create 资源做护甲纹理名** | 仅适用于"复刻 Create 资源"的场景，我们的元件应使用自己的纹理命名空间 |
| 把应力值写成字面量 | 参考本文档 03 章：一律 supplier + 配置 |

### 7.3 它没有做、我们需要做的

Jetpack 只用了 Create 的**装备/渲染/配方**，**完全没有碰动力学**（虽然方块是 `HorizontalKineticBlock`，但那是继承白送的）。所以对我们的元件：

1. **BE 层**：仍需继承 `KineticBlockEntity` / `GeneratingKineticBlockEntity`，实现 `calculateAddedStressCapacity` 或用 `BlockStressValues` 注册（见 `03-sources-consumers.md`）。
2. **传播层**：需要 `IRotate.hasShaftTowards` / `getRotationAxis` / `getRotationSpeedModifier`（见 `02-transmission.md`）。
3. **应力平衡**：Jetpack 的"委托给铜背罐"思路可以升级为"**委托给同档位 Create 方块**"——我们的新机器若要 4 SU，就 `register(block) { BlockStressValues.getImpact(AllBlocks.MILLSTONE.get()) }`，这样 Create 更新数值时我们自动跟随，**永远与上游平衡**。这是一个很实用的小技巧。
4. **Ponder**：Jetpack 没有 Ponder；我们的新元件**必须有**（Create 玩家的学习路径依赖它），见 `04-registration-render-ponder.md`。
5. **JEI**：涉及配方的机器要加 JEI 分类（Jetpack 未做）。

---

## 8. 坑与注意

| # | 坑 | 依据 | 规避 |
|---|---|---|---|
| 1 | 客户端类在服务端被加载 | 5.2.1 changelog："fix client-only classes being loaded for config screen on server" | 独立 `@Mod(dist = Dist.CLIENT)`；客户端逻辑只在该类里 |
| 2 | jarjar 库仍需写在 `mods.toml` 为 required | `neoforge.mods.toml` | 漏写会导致依赖解析/加载顺序问题 |
| 3 | Create 版本区间过宽 | 6.0.6 重构 `ProcessingRecipe` | 写 `[6.0.10,)` 这类精确下限，并在 CI 里对最低版本编译 |
| 4 | Kotlin 工具链版本联动 | `kotlinforforge [5.8,)` | 若必须用 Kotlin，锁死 kotlinforforge 小版本 |
| 5 | 自定义 payload 的字段顺序不对称 | `SyncConfigMessage.encode/decode` 手写 buf | encode/decode 必须严格镜像；加字段要同步两端 + 升 `registrar("x.y")` 版本 |
| 6 | 复用 Create BE 但方块状态不兼容 | `BacktankBlockEntity` 依赖 `BacktankBlock` 的状态 | 复用前先确认状态属性（`FACING`、`WATERLOGGED` 等）完全一致 |
| 7 | `canAbsorbDamage` 的 `usesPerTank` 语义是"每罐能用几次" | `BacktankUtil.java:128-139` 里 `cost = maxAir / usesPerTank` | 传"次数"而不是"每次消耗量"，写反会导致一次性空罐 |
| 8 | 创造物品栏里的物品不带状态 | `Content.kt:186-192` 需要手动 `set(...)` | 任何"默认带满值"的物品都要在 creative tab 的 supplier 里补组件 |
| 9 | 只测单机不测联机 | `SYNCED_SERVER ?: LOCAL_SERVER` 的设计就是为了联机 | 服务端数值改动必须走同步包，不能依赖客户端 config 文件 |

---

## 9. 待验证 / 未覆盖

- `ControlsDisplay.kt`（GUI 空气条与控制提示）只做了 `BacktankUtil` 读取的确认（`ControlsDisplay.kt:147-149`），未逐行分析其渲染管线；⚠ 若要做类似 HUD，建议直接读源码。
- `CopyComponentsMechanicalCraftingRecipe.Serializer` 里 `STREAM_CODEC` 被注释掉（`CopyComponentsMechanicalCraftingRecipe.kt:60` 附近），⚠ 说明它目前只走 `codec()`；如果你要支持网络同步配方（JEI 多人场景），需要自己补 `streamCodec()`。
- `curios_support_datapack.zip`（仓库根目录）是给 Curios 使用的数据包，未展开分析。
- FlightLib 的 `JetpackLogic` / `ControlManager` / `ControlSender` 只做了接线确认，飞行数学未逐行推导；⚠ 若要做"反重力/悬浮平台"类元件，需要另行精读。
