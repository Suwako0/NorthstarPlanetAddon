# Create 6.0.x 核心架构与动力学解析

> 适用环境：**Minecraft 1.21.1 / NeoForge 21.1.x / Create 6.0.10**
> 分析材料：Create `create-1.21.1-6.0.10-281-sources.jar`（maven.createmod.net），与仓库内 `libs/create-1.21.1-6.0.10.jar` 同属 6.0.10 线。
> 本文行号均以该 sources 包中的路径为准，路径根为 `com/simibubi/create/`。
>
> 配套文档（同目录）：
> - [`CREATE-ADDON-QUICK-REFERENCE.md`](CREATE-ADDON-QUICK-REFERENCE.md) —— **总索引**：文档地图、术语表、一分钟速查、本仓库差距、提交前清单
> - [`CREATE-KINETIC-TRANSMISSION.md`](CREATE-KINETIC-TRANSMISSION.md) —— 传动系统：`IRotate` 契约、倍率判定顺序、逐元件剖析、新增传动件模板
> - [`CREATE-STRESS-SYSTEM.md`](CREATE-STRESS-SYSTEM.md) —— 应力体系：公式、网络时机、`BlockStressValues`、源/消耗清单与两套模板、数值设计
> - [`CREATE-REGISTRATION-RENDER-PONDER.md`](CREATE-REGISTRATION-RENDER-PONDER.md) —— 注册 / Flywheel 渲染 / Ponder / 交互 / datagen / 起步清单
> - [`CREATE-CASE-STUDY-ENCHANTMENT-INDUSTRY.md`](CREATE-CASE-STUDY-ENCHANTMENT-INDUSTRY.md) —— 案例 A：附魔工业（CEI）如何扩展 Create
> - [`CREATE-CASE-STUDY-JETPACK.md`](CREATE-CASE-STUDY-JETPACK.md) —— 案例 B：喷气背包如何最大化复用 Create

---

## 0. 先记住这 10 条

1. **Create 的"动力"只有两个量：RPM（转速）和 SU（应力）**。方块之间传播的是"转速倍率 + 方向（符号）"，而应力是整张网络（`KineticNetwork`）的账本。
2. **一个动力方块 = 一个 `KineticBlockEntity`**；动力方块必须实现 `IRotate`（多数情况通过继承 `KineticBlock` 家族获得），否则不参与传播。
3. **传播是纯几何的**：`RotationPropagator` 按邻居坐标、`IRotate.hasShaftTowards(...)`、`IRotate.getRotationAxis(state)` 和齿轮规则推导连接；两个方块是否"连通"不靠注册表，靠这两块方块自己声明的轴/面。
4. **速度倍率用 `float` 表示，正负号表示转向**。同轴连接 `1`，两齿轮外啮合 `-1`，大齿轮带小齿轮 `2`（或小带大 `-0.5`）；齿轮箱/离合器/换向器通过 `SplitShaftBlockEntity.getRotationSpeedModifier(Direction face)` 或 `propagateRotationTo(...)` 做自定义倍率。
5. **应力 = 基准值 × |转速|**：消耗 `impactBase × |getTheoreticalSpeed()|`，容量 `capacityBase × |getGeneratedSpeed()|`。单位 SU，配置默认值即"每 1 RPM 的 SU 基准"。
6. **过载（overstress）不改网络数据，只让 `getSpeed()` 返回 0**：`KineticBlockEntity.getSpeed()` 在 `overStressed` 或服务器冻结 tick 时返回 0（`getTheoreticalSpeed()` 保留真实值），所有消耗装置因此停转。
7. **源（generator）与消费者（consumer）都只是 `KineticBlockEntity` 的两个虚方法**：`calculateAddedStressCapacity()` 与 `calculateStressApplied()`；源要在速度变化时主动 `updateGeneratedRotation()`。
8. **应力数值不是硬编码在 BE 里**，而是按方块对象查 `BlockStressValues`（`SimpleRegistry<Block, DoubleSupplier>`）。Create 自己的默认值在 `AllBlocks` 用 `CStress.setImpact/setCapacity` 写入配置；**附属模组不能调用 `CStress`（会抛 `IllegalStateException`），必须直接向 `BlockStressValues.IMPACTS/CAPACITIES` 注册**。
9. **Create 的 registrate 实例对外封闭**（`Create.registrate()` 会检查调用方包名并抛异常）。附属模组要自己 `CreateRegistrate.create(modid)`。
10. **渲染走 Flywheel 1.0 的 Visual 体系**（`KineticBlockEntityVisual` / `RotatingInstance`），传统 `BlockEntityRenderer` 仍可用但不是首选；Ponder 是独立库（`ponder-neoforge`），addon 用 `PonderRegistrationHelper` 注册场景。

---

## 1. 版本与技术栈

| 层 | 版本 / 说明 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.x（本仓库 `neo_version=21.1.249`） |
| Create | 6.0.10（`major.minor.patch`，6.0 起版本号规则改变） |
| Flywheel | 1.0.x（`flywheel-neoforge-1.21.1-1.0.6.jar`），Create 的动力渲染基于它 |
| Ponder | 独立库（`ponder-neoforge-1.0.82+mc1.21.1.jar`），6.0 起从 Create 内拆出 |
| Java | 21（`build.gradle` 的 toolchain / `options.release = 21`） |
| 其它前置 | Registrate、Catnip（`net.createmod.catnip`）、Ponder、Flywheel |

Create 6.0.x 是**大规模 API 重构**后的第一个大版本：

- 6.0.0 起：Ponder 独立；`BlockStressValues`、`MovementBehaviour`、`DisplaySource`、`ContraptionType` 等一批内部类转正为 API。
- 6.0.6 起：`ProcessingRecipe` 系列重构，**addon 需要跟着改**；datagen 重构出清晰的 addon API。
- 6.0.2 起：Create 的 registrate 实例被"调用方检查"锁住（见 `Create.java:213-217`）。

> 结论：写 Create 6.0.x 附属，**不要照抄 0.5.x/1.20.1 的教程**。0.5 的 `IStressable`、`StressConfig`、`BlockStressValues` 用法在 6.0 已经换了实现。

---

## 2. 代码地图（附魔工业等 addon 也遵循同样的分层）

```
com.simibubi.create
├── api/                    # 对外稳定 API（addon 优先使用）
│   ├── behaviour/          # movement / interaction / mounted storage 等行为注册
│   ├── boiler/ contraption/ connectivity/ equipment/ event/ packager/
│   ├── registrate/         # CreateRegistrateRegistrationCallback
│   ├── registry/           # SimpleRegistry（BlockStressValues 用的就是它）
│   ├── schematic/ stress/  # BlockStressValues
│   └── data/ effect/ ...
├── content/                # 玩法实现
│   ├── kinetics/           # ★ 动力学：base / transmission / simpleRelays / belt / gearbox /
│   │                       #   waterwheel / windmill / steamEngine / motor / crank / millstone /
│   │                       #   press / mixer / saw / drill / crusher / fan / ...
│   ├── contraptions/       # 动态结构（轴承、活塞、矿车等）
│   ├── logistics/          # 包裹/仓库
│   ├── fluids/ redstone/ equipment/ materials/ ...
├── foundation/             # 基础设施
│   ├── block/ blockEntity/ # IBE、SmartBlockEntity、SyncedBlockEntity
│   ├── data/               # CreateRegistrate、BuilderTransformers、BlockStateGen、ModelGen、TagGen
│   ├── ponder/ recipe/ render/ networking/ ...
├── infrastructure/         # 模组级：config、datagen、事件、ponder 场景
├── impl/ compat/           # 内部实现与兼容层
```

**读源码入口顺序建议**：`Create.java`（初始化顺序）→ `AllBlocks.java`（方块注册与应力值）→ `content/kinetics/base/`（契约）→ `RotationPropagator` + `KineticNetwork`（算法）→ 具体元件 → `foundation/data`（注册/渲染/数据生成）。

---

## 3. 模组骨架：注册与方块实体

### 3.1 入口与注册顺序

Create 的入口（`Create.java:105-166`）：

```java
@Mod(Create.ID)
public class Create {
    public Create(IEventBus eventBus, ModContainer modContainer) { onCtor(eventBus, modContainer); }

    public static void onCtor(IEventBus modEventBus, ModContainer modContainer) {
        REGISTRATE.registerEventListeners(modEventBus);
        AllCreativeModeTabs.register(modEventBus);
        AllBlocks.register();
        AllItems.register();
        AllFluids.register();
        AllBlockEntityTypes.register();
        AllRecipeTypes.register(modEventBus);
        AllPackets.register();
        AllAttachmentTypes.register(modEventBus);
        AllConfigs.register(ModLoadingContext.get(), modContainer);
        ...
        modEventBus.addListener(Create::init);        // FMLCommonSetupEvent
        modEventBus.addListener(Create::onRegister);  // RegisterEvent
    }
}
```

要点：

- 方块/物品/流体用 **Registrate** 的静态 `BlockEntry`/`ItemEntry` 字段注册；`registerEventListeners(modEventBus)` 一次性挂上。
- 自定义 registry（`BlockStressValues` 这类 `SimpleRegistry`）**不依赖事件**，在 mod 构造期注册即可（线程安全，见 `SimpleRegistry` 注释）。
- 服务端/客户端拆分：Create 用 `compat`、`client` 包 + `@EventBusSubscriber(Dist.CLIENT)`；addon 建议照做，避免客户端类在服务端加载。

### 3.2 方块：KineticBlock 家族

| 类 | 用途 | 关键行为 |
|---|---|---|
| `KineticBlock` | 动力方块基类，实现 `IRotate` | `onPlace`/`onRemove` 交给 `IBE`；`areStatesKineticallyEquivalent` 判断状态变化是否影响动力学 |
| `DirectionalKineticBlock` | 有 `FACING` 的定向动力方块 | 放置时优先朝向相邻动力方块的轴（`getPreferredFacing`，`DirectionalKineticBlock.java:28-58`） |
| `HorizontalKineticBlock` | 只有水平 `HORIZONTAL_FACING` | 同上，水平版 |
| `RotatedPillarKineticBlock` | 有 `AXIS` 的柱状动力方块（轴、磨石等） | 用 `AXIS` 决定旋转轴 |
| `AbstractShaftBlock` / `AbstractSimpleShaftBlock` | 轴类（含支架轴） | 自带轴连接/渲染逻辑 |

`IRotate` 是契约核心（`IRotate.java:144-150`）：

```java
public interface IRotate extends IWrenchable {
    boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face);
    Axis getRotationAxis(BlockState state);
    default SpeedLevel getMinimumRequiredSpeedLevel() { return SpeedLevel.NONE; }
    default boolean hideStressImpact() { return false; }
    default boolean showCapacityWithAnnotation() { return false; }
}
```

- `hasShaftTowards(..., face)`：**这个方块的 `face` 面能否像轴一样输出/输入旋转**。传播器用它判断"面对面"连接。
- `getRotationAxis(state)`：本方块的旋转轴；与 `hasShaftTowards` 必须自洽（轴垂直于面）。
- `SpeedLevel`：`SLOW(>=1 RPM)`、`MEDIUM(>=mediumSpeed，默认 30)`、`FAST(>=fastSpeed，默认 100)`，用于"最低转速要求"与 goggle 显示。
- `StressImpact.isEnabled()`：`!kinetics.disableStress`（服务端配置），所有应力计算与显示都受它控制。

`KineticBlock` 的两个易错生命周期（`KineticBlock.java:24-91`）：

```java
@Override
public void onPlace(...) { /* 同方块且动力学等价时 preventSpeedUpdate=2，避免整网重算 */ }

@Override
public void onRemove(...) { IBE.onRemove(pState, pLevel, pPos, pNewState); }

@Override
public void updateIndirectNeighbourShapes(...) {
    // 方块被放置/移动时：warnOfMovement(); clearKineticInformation(); updateSpeed = true;
}
```

### 3.3 方块实体：SmartBlockEntity 生命周期

动力 BE 继承链：

```
BlockEntity
└── SyncedBlockEntity            (sendData / notifyUpdate / 客户端同步)
    └── SmartBlockEntity         (behaviours / lazyTick / 生命周期钩子)
        └── KineticBlockEntity   (网络/速度/应力，implements IHaveGoggleInformation)
            ├── DirectionalShaftHalvesBlockEntity   (记录 source 方向)
            │   └── SplitShaftBlockEntity            (按面返回倍率)
            └── GeneratingKineticBlockEntity         (应力源)
```

`SmartBlockEntity` 的关键点（`foundation/blockEntity/SmartBlockEntity.java`）：

- 构造后由 `addBehaviours(List<BlockEntityBehaviour>)` 注入行为（`setLazyTickRate(10)` 默认，见第 49 行附近）。
- `initialize()`：**BE 被放置/加载后调用**，动力源在这里触发首次 `updateGeneratedRotation()`。
- `tick()`：每 tick；`lazyTick()` 每 `lazyTickRate` tick；`read/write` 走 `SyncedBlockEntity` 的 NBT + 客户端包。
- `sendData()`：标记需要同步到客户端；`notifyUpdate()`：通知邻居/触发比较器更新。
- `invalidate()`：区块卸载/被移除；`destroy()`：方块被破坏（掉落内容、断开动力学）。
- `IBE.onRemove` 是"方块被移除时正确断开 BE"的关键，`KineticBlock.onRemove` 已经调用它。

动力 BE 的 tick 骨架（`KineticBlockEntity.java:97-125`）：

```java
@Override
public void tick() {
    if (!level.isClientSide && needsSpeedUpdate())
        attachKinetics();                 // 首次/被移动后加入传播图
    super.tick();
    effects.tick();
    if (level.isClientSide) { tickAudio(); return; }

    if (validationCountdown-- <= 0) {     // 默认每 60 tick 校验一次
        validationCountdown = AllConfigs.server().kinetics.kineticValidationFrequency.get();
        validateKinetics();               // source 失效则 removeSource + detach
    }
    if (networkDirty) {
        if (hasNetwork()) getOrCreateNetwork().updateNetwork();
        networkDirty = false;
    }
}
```

---

## 4. 动力学核心对象

### 4.1 术语

| 术语 | 含义 | 代码 |
|---|---|---|
| 网络（network） | 一组互相连通、共享同一个"源速度"的动力方块 | `KineticNetwork`，按 `Long id` 存在 `TorquePropagator.networks` 里（每个 Level 一张表） |
| 源（source） | 生成转速的 BE（水车、风车、蒸汽机、手摇曲柄、创造马达……） | `isSource() = getGeneratedSpeed() != 0` |
| 成员（member） | 网络里除源以外的所有动力 BE | `KineticNetwork.members` |
| 转速 | RPM；符号表示方向 | `speed` / `getSpeed()` / `getTheoreticalSpeed()` |
| 应力容量 | 网络可提供的 SU | `capacity` / `KineticNetwork.currentCapacity` |
| 应力消耗 | 网络已占用的 SU | `stress` / `KineticNetwork.currentStress` |
| 过载 | 消耗 > 容量 | `overStressed`，`getSpeed()` 返回 0 |

### 4.2 数据结构

`TorquePropagator`（`content/kinetics/TorquePropagator.java`）只负责**按世界存网络表**：

```java
public class TorquePropagator {
    static Map<LevelAccessor, Map<Long, KineticNetwork>> networks = new HashMap<>();

    public void onLoadWorld(LevelAccessor world) { networks.put(world, new HashMap<>()); }
    public void onUnloadWorld(LevelAccessor world) { networks.remove(world); }

    public KineticNetwork getOrCreateNetworkFor(KineticBlockEntity be) {
        Long id = be.network;
        if (id == null) return null;
        Map<Long, KineticNetwork> map = networks.computeIfAbsent(be.getLevel(), $ -> new HashMap<>());
        return map.computeIfAbsent(id, $ -> { KineticNetwork n = new KineticNetwork(); n.id = be.network; return n; });
    }
}
```

`KineticNetwork` 维护两张表：`sources`（源 → 基准容量）与 `members`（成员 → 基准消耗），以及 `unloadedCapacity/unloadedStress/unloadedMembers`（区块未加载时的快照）。实际值每次都乘 `|speed|`：

```java
public float getActualCapacityOf(KineticBlockEntity be) {
    return sources.get(be) * getStressMultiplierForSpeed(be.getGeneratedSpeed());
}
public float getActualStressOf(KineticBlockEntity be) {
    return members.get(be) * getStressMultiplierForSpeed(be.getTheoreticalSpeed());
}
private static float getStressMultiplierForSpeed(float speed) { return Math.abs(speed); }
```

### 4.3 一次"接电"的完整流程

```
放置/加载 BE
  → KineticBlockEntity.initialize()：若已有 network，网络 initFromTE + addSilently
  → tick()：needsSpeedUpdate() → attachKinetics() → RotationPropagator.handleAdded()
      → propagateNewSource(当前BE)：遍历连通邻居，推导 newSpeed/oppositeSpeed
          → 方向冲突 → destroyBlock（"打掉方块的暴击"）
          → 邻居更快 → 以邻居为 source，递归传播
          → 自己更快 → 邻居改 source，递归
      → 若自己是源（isSource）→ 创建/沿用 network id
  → GeneratingKineticBlockEntity.updateGeneratedRotation()：
      applyNewSpeed(prev,new) → 建网 / 改网 / 成为新源
      notifyStressCapacityChange(calculateAddedStressCapacity())
      network.updateStressFor(this, calculateStressApplied()); network.updateStress()
  → KineticNetwork.sync() → 每个成员 updateFromNetwork(capacity, stress, size)
      → overStressed = capacity < stress && StressImpact.isEnabled()
      → 变化时 onSpeedChanged + sendData（客户端立刻看到停转/音效）
```

> 关键点：**"连接"是即时推导的，没有连接表**。`handleAdded` 只在方块加入时做一次局部传播；每次方块变更/速度变化再由 `updateGeneratedRotation` 和 `networkDirty` 更新。因此任何"能传动的方块"都必须让传播器"看得见"它——通过 `hasShaftTowards` 或 `propagateRotationTo`。

---

## 5. 速度传播算法（RotationPropagator）

### 5.1 判定顺序（`RotationPropagator.java:44-120`）

对一对相邻的 `from`/`to` 动力 BE：

```java
float getRotationSpeedModifier(from, to) {
    // 0) 自定义：先给 BE 一次覆盖机会（双向都可）
    float custom = from.propagateRotationTo(to, stateFrom, stateTo, diff, connectedByAxis, connectedByGears);
    if (custom != 0) return custom;

    // 1) 轴对轴（面对面、轴对齐）
    if (connectedByAxis) {
        float axisModifier = getAxisModifier(to, direction.getOpposite());
        if (axisModifier != 0) axisModifier = 1 / axisModifier;
        return getAxisModifier(from, direction) * axisModifier;
    }

    // 2) 链传动
    if (fromBlock instanceof ChainDriveBlock && toBlock instanceof ChainDriveBlock) { ... }

    // 3) 大齿轮 <-> 大齿轮（轴互相垂直）
    if (isLargeToLargeGear(stateFrom, stateTo, diff)) return sourceAxisDiff > 0 ^ targetAxisDiff > 0 ? -1 : 1;

    // 4) 大齿轮 <-> 小齿轮
    if (isLargeCog(stateFrom) && isSmallCog(stateTo) && isLargeToSmallCog(...)) return -2f;
    if (isLargeCog(stateTo)   && isSmallCog(stateFrom) && isLargeToSmallCog(...)) return -.5f;

    // 5) 小齿轮 <-> 小齿轮（相邻、轴不同）
    if (connectedByGears) {
        if (diff.distManhattan(ZERO) != 1) return 0;
        if (isLargeCog(stateTo)) return 0;
        if (direction.getAxis() == definitionFrom.getRotationAxis(stateFrom)) return 0;
        if (definitionFrom.getRotationAxis(stateFrom) == definitionTo.getRotationAxis(stateTo)) return -1;
    }
    return 0;
}
```

记住倍率语义：

| 连接 | 返回值 | 含义 |
|---|---|---|
| 同轴对接 | `1`（经 `axisModifier` 后） | 同向同速 |
| 小齿轮外啮合 | `-1` | 反向同速 |
| 大齿轮带小齿轮 | `-2` | 小轮转 2 倍、反向 |
| 小齿轮带大齿轮 | `-0.5` | 大轮转一半、反向 |
| 大齿轮-大齿轮（轴垂直） | `±1` | 反向/同向取决于轴差符号 |
| 齿轮箱换向面 | `±1`（`SplitShaftBlockEntity` 可给别的值） | 由 `getAxisModifier` 处理 |
| 链传动 | `ChainDriveBlock.getRotationSpeedModifier(...)` | 链轮比 |
| 自定义 | `propagateRotationTo` 的返回值 | 任意倍率；`0` 表示不接管 |

### 5.2 `connectedByAxis` 的精确条件

```java
boolean alignedAxes = true;
for (Axis axis : Axis.values())
    if (axis != direction.getAxis() && axis.choose(diff.getX(), diff.getY(), diff.getZ()) != 0)
        alignedAxes = false;

boolean connectedByAxis =
    alignedAxes
    && definitionFrom.hasShaftTowards(world, from.getBlockPos(), stateFrom, direction)
    && definitionTo.hasShaftTowards(world, to.getBlockPos(), stateTo, direction.getOpposite());
```

- 两块必须在同一坐标轴上相邻（`alignedAxes`）。
- 双方都要声明"朝向对方的那一面有轴"。
- 轴的**轴心线**必须与 `direction` 的轴一致（通常 `getRotationAxis` = `direction.getAxis()`），否则视觉/逻辑会错位。

### 5.3 `getAxisModifier`：齿轮箱/半轴的换向

（`RotationPropagator.java:157-170`）

```java
private static float getAxisModifier(KineticBlockEntity be, Direction direction) {
    if (!(be.hasSource() || be.isSource()) || !(be instanceof DirectionalShaftHalvesBlockEntity))
        return 1;
    Direction source = ((DirectionalShaftHalvesBlockEntity) be).getSourceFacing();

    if (be instanceof GearboxBlockEntity)
        return direction.getAxis() == source.getAxis()
            ? (direction == source ? 1 : -1)
            : (direction.getAxisDirection() == source.getAxisDirection() ? -1 : 1);

    if (be instanceof SplitShaftBlockEntity)
        return ((SplitShaftBlockEntity) be).getRotationSpeedModifier(direction);

    return 1;
}
```

- `DirectionalShaftHalvesBlockEntity` 记住 `source` 相对自己的方向（`getSourceFacing()`）。
- 齿轮箱：同轴的对面是 `-1`（换向），垂直轴按 axis direction 决定 `±1`。
- 离合器/换向器/序列齿轮箱（`SplitShaftBlockEntity` 子类）：直接给"面 → 倍率"。

### 5.4 冲突与"方块爆炸"

`propagateNewSource`（`RotationPropagator.java:219-300`）会处理这些情况：

- **方向冲突**：`newSpeed` 与邻居当前速度符号冲突（且都非 0）→ `world.destroyBlock(pos, true)`。
- **超速**：`|newSpeed|` 或 `|oppositeSpeed| > kinetics.maxRotationSpeed`（默认 256）→ 破坏。
- **抖动**：`flickerTally > 128`（短时间内频繁换向/启停）→ 破坏。
- **循环**：`currentTE.network.equals(neighbourTE.network)` 且 `|newSpeed|` 略大于 `|speedOfNeighbour|`（epsilon = speed/256²）→ 破坏，防止自激循环。

因此自建传动件时：**倍率符号必须全局一致**（否则玩家一放就会"炸方块"），**不要造闭环**。

### 5.5 移除与断线

`handleRemoved`（`RotationPropagator.java:309-331`）只处理"以被移除方块为 source 的邻居"，然后 `propagateMissingSource` 做 BFS：

- 清空旧子网所有成员的 source；
- 收集潜在新源（`isSource()` 的邻居）；
- 找到后以它为源重新传播。
- 找不到 → 整网停转（`removeSource()`）。

这解释了为什么"拆掉水车后整条线停转"：网络里没有源了。

### 5.6 自定义连接（addon 最常用的扩展点）

`KineticBlockEntity` 提供三个钩子（`KineticBlockEntity.java:543-592`）：

```java
public float propagateRotationTo(KineticBlockEntity target, BlockState stateFrom, BlockState stateTo,
                                 BlockPos diff, boolean connectedViaAxes, boolean connectedViaCogs) { return 0; }

public List<BlockPos> addPropagationLocations(IRotate block, BlockState state, List<BlockPos> neighbours) {
    if (!canPropagateDiagonally(block, state)) return neighbours;
    // 小齿轮默认允许"斜对角"传播（distSqr == 2）
}

public boolean isCustomConnection(KineticBlockEntity other, BlockState state, BlockState otherState) { return false; }
```

- `propagateRotationTo`：**优先于所有内置规则**；`0` = "不接管，继续走内置规则"。返回非 0 则直接作为倍率。
- `addPropagationLocations`：把"非 6 邻居"的位置加进候选（例如远程/斜向连接）。
- `isCustomConnection`：声明"我承认这条边存在"，用于 `isConnected` 判定；必须对称（双方任一声明即可）。

> 小齿轮能斜对角连接，就是因为 `addPropagationLocations` 默认实现会为 `ICogWheel.isSmallCog` 加入 `distSqr == 2` 的位置（`KineticBlockEntity.java:558-572`、`canPropagateDiagonally` 默认 `isSmallCog`）。

---

## 6. 应力（Stress）模型

### 6.1 公式

```
capacity = Σ_源 ( capacityBase(block) × |getGeneratedSpeed(源)| ) + unloadedCapacity
stress   = Σ_成员 ( impactBase(block) × |getTheoreticalSpeed(成员)| ) + unloadedStress
overStressed = capacity < stress && !kinetics.disableStress
```

- `capacityBase`/`impactBase` 通过 `BlockStressValues.getCapacity(block)` / `getImpact(block)` 取，**单位是"每 1 RPM 的 SU"**。
- `KineticBlockEntity.calculateStressApplied()` 默认读 `BlockStressValues.getImpact(getStressConfigKey())`；`getStressConfigKey()` 默认返回 `getBlockState().getBlock()`（`KineticBlockEntity.java:175-189`）。需要"同方块不同状态不同应力"时覆盖 `getStressConfigKey`。
- `calculateAddedStressCapacity()` 同理读 `BlockStressValues.getCapacity(...)`。
- 应力值在**网络层面**汇总，单个 BE 只提供基准值。

### 6.2 网络更新时机

| 时机 | 调用 |
|---|---|
| BE 加入网络 | `KineticNetwork.add(be)` → `sources/members.put` → `updateFromNetwork` → `be.networkDirty = true` |
| 区块加载（已有网络） | `KineticBlockEntity.initialize()` → `network.initFromTE(capacity, stress, size)` + `addSilently(...)` |
| 源容量变化 | `GeneratingKineticBlockEntity.notifyStressCapacityChange(cap)` → `network.updateCapacityFor(this, cap)` |
| 成员消耗变化 | `network.updateStressFor(this, stress)` → `updateStress()` |
| 网络脏标记 | `networkDirty` → `tick()` 里 `updateNetwork()` |
| 同步到成员 | `updateCapacity()`/`updateStress()` → `sync()` → `be.updateFromNetwork(...)` |

`updateFromNetwork`（`KineticBlockEntity.java:155-169`）：

```java
public void updateFromNetwork(float maxStress, float currentStress, int networkSize) {
    this.capacity = maxStress; this.stress = currentStress; this.networkSize = networkSize;
    boolean overStressed = maxStress < currentStress && StressImpact.isEnabled();
    setChanged();
    if (overStressed != this.overStressed) {
        float prevSpeed = getSpeed();
        this.overStressed = overStressed;
        onSpeedChanged(prevSpeed);   // 触发客户端音效/视觉
        sendData();
    }
}
```

### 6.3 过载的表现

```java
public float getSpeed() {
    if (overStressed || (level != null && level.tickRateManager().isFrozen()))
        return 0;
    return getTheoreticalSpeed();
}
```

- 所有逻辑必须用 `getSpeed()` 判断"现在是否在转"；`getTheoreticalSpeed()` 只用于应力计算/对比。
- 客户端由 `sendData` 收到 `overStressed`，`KineticEffectHandler` 播过载粒子/音效。
- `KineticBlockEntity.addToGoggleTooltip` 显示应力影响；`addToTooltip` 在过载时显示 `gui.stressometer.overstressed`。

### 6.4 `BlockStressValues`：addon 的唯一正确姿势

```java
public class BlockStressValues {
    public static final SimpleRegistry<Block, DoubleSupplier> IMPACTS = SimpleRegistry.create();
    public static final SimpleRegistry<Block, DoubleSupplier> CAPACITIES = SimpleRegistry.create();
    public static final SimpleRegistry<Block, GeneratedRpm> RPM = SimpleRegistry.create(); // 仅 tooltip

    public static double getImpact(Block block) { ... }
    public static double getCapacity(Block block) { ... }
    public static NonNullConsumer<Block> setGeneratorSpeed(int value) { ... }
}
```

- 直接注册：`BlockStressValues.IMPACTS.register(MY_BLOCK.get(), () -> myConfig.impact.get())`。
- 也可以 `registerProvider(...)` 按标签/规则批量给值。
- **不要**调用 `CStress.setImpact/setCapacity`：它内部 `assertFromCreate(builder)`，非 `create` 的 modid 会抛 `IllegalStateException`（`CStress.java:89-93`）。
- 生成 RPM 的 `BlockStressValues.RPM` 只用于 goggle/JEI 等 tooltip 显示，不参与计算；真实 RPM 由你的 `getGeneratedSpeed()` 决定。但**建议保持一致**，否则玩家看到的"XX SU @ RPM"会误导。

### 6.5 配置与速度等级

`CKinetics`（`infrastructure/config/CKinetics.java`）里与元件设计相关的项：

| 配置 | 默认 | 影响 |
|---|---|---|
| `disableStress` | false | 关掉整个应力系统（调试用） |
| `maxRotationSpeed` | 256 | 超过会被传播器直接破坏 |
| `kineticValidationFrequency` | 60 | 校验 source 是否有效的频率 |
| `mediumSpeed` / `fastSpeed` | 30 / 100 | `SpeedLevel` 阈值 |
| `mediumStressImpact` / `highStressImpact` | 4 / 8 | goggle 颜色档 |
| `mediumCapacity` / `highCapacity` | 256 / 1024 | 容量档 |
| `maxBeltLength` | 20 | 传送带最大长度 |
| `fanRotationArgmax` | 256 | 风扇达到最大效果所需 RPM |

> 注意 `CStress.java:98` 的注释 "this cost is doubled for every speed increase it receives" 是旧版遗留描述；**6.0.10 代码是 `× |speed|` 线性**（`KineticNetwork.getStressMultiplierForSpeed` 直接返回 `Math.abs(speed)`）。写文档/做数值时以代码为准。

---

## 7. 元件三分类

### 7.1 应力源（Generator）

共同点：继承 `GeneratingKineticBlockEntity`，覆盖 `getGeneratedSpeed()`，并在速度变化时调用 `updateGeneratedRotation()`。

| 方块 | 类 | 容量基准 | 生成 RPM | 动态因素 |
|---|---|---|---|---|
| 水车 | `WaterWheelBlockEntity` | 32 | 8 | 水流方向/大小（`flowScore`） |
| 大水车 | `LargeWaterWheelBlockEntity` | 128 | 4 | 同上，尺寸更大 |
| 风车轴承 | `WindmillBearingBlockEntity` | 512 | 16（可能更少） | 帆的数量 |
| 蒸汽机 | `SteamEngineBlockEntity` | 1024 | 64（可能更少） | 锅炉等级/被动/主动 |
| 手摇曲柄 | `HandCrankBlockEntity` | 8 | 32 | 玩家手摇，消耗饥饿 |
| 创造马达 | `CreativeMotorBlockEntity` | 16384 | 256（可调） | 滚动条设定 |

> 实际容量 = 基准 × RPM。例如水车 = 32 × 8 = 256 SU；蒸汽机 = 1024 × 64 = 65536 SU（满速）。

### 7.2 应力消耗装置（Consumer）

共同点：继承 `KineticBlockEntity`（或 `SmartBlockEntity` + 自己的转速逻辑），默认按 `BlockStressValues.IMPACTS` 扣应力；`tick()` 里用 `getSpeed()` 判断是否工作。

| 方块 | 类 | 消耗基准 | 最低转速 | 速度对行为的影响 |
|---|---|---|---|---|
| 磨石 | `MillstoneBlockEntity` | 4 | 无 | 处理速度 ∝ 转速（`getProcessingSpeed()`） |
| 机械压床 | `MechanicalPressBlockEntity` | 8 | 无 | 冲压间隔随转速缩短 |
| 机械搅拌器 | `MechanicalMixerBlockEntity` | 4 | 无 | 需要达到配方要求转速（`{speed}`） |
| 粉碎轮 | `CrushingWheelBlockEntity` | 8 | 无 | 转速影响效率/伤害 |
| 动力锯 | `SawBlockEntity` | 4 | 无 | 切割速度 |
| 动力钻头 | `DrillBlockEntity` | 4 | 无 | 挖掘速度 |
| 鼓风机 | `EncasedFanBlockEntity` | 2 | 无 | 处理/推送距离随转速 |
| 机械臂 | `ArmBlockEntity` | 2 | 无 | 动作速度 |
| 转盘 | `TurntableBlockEntity` | 4 | 无 | 旋转速度 |
| 链式传送带 | `ChainConveyorBlockEntity` | 1 | 无 | 移动速度 |

> 表中数值来自 `AllBlocks.java` 的 `CStress.setImpact(...)`；不同版本可能调整。**低转速能否工作**由各 BE 自行判断：例如搅拌器要求 `getSpeed() >= recipe 的 minSpeed`，磨石则是"只要有速度就按比例加快"。

### 7.3 传动件（Transmission）

见 `CREATE-KINETIC-COMPONENT-GUIDE.md` 第 4 章；速查：

| 元件 | 实现要点 | 倍率 |
|---|---|---|
| 轴 / 大轴 | `AbstractShaftBlock`，双面 `hasShaftTowards` | 1 |
| 小齿轮 | `CogWheelBlock` + `ICogWheel`，可斜对角 | 外啮合 -1 |
| 大齿轮 | 同上，`isLargeCog` | 带小 -2 / 被小带 -0.5 |
| 齿轮箱 | `GearboxBlock` + `GearboxBlockEntity`，`DirectionalShaftHalvesBlockEntity` | 同轴对面 -1，垂直 ±1 |
| 离合器 | `ClutchBlock` + `ClutchBlockEntity`（`SplitShaftBlockEntity`） | 断开时 0 |
| 换向器 | `GearshiftBlock` + `GearshiftBlockEntity` | 通电时 -1 |
| 序列齿轮箱 | `SequencedGearshiftBlockEntity` | 指令表逐条给倍率 |
| 链传动 | `ChainDriveBlock` + `ChainDriveBlockEntity` | 齿轮组比 |
| 转速控制器 | `SpeedControllerBlockEntity` | 直接设置网络目标速度（特殊路径） |
| 传送带 | `BeltBlock` + `BeltBlockEntity` | 两端轴取速度；不是纯传动件 |

---

## 8. 渲染、交互与 Ponder（概览）

### 8.1 Flywheel 1.0

Create 6.0 的旋转方块渲染走 Flywheel 的 **Visual** 体系：

- `KineticBlockEntityVisual<T extends KineticBlockEntity>`：动力 BE 的 Visual 基类。
- `RotatingInstance`：把"速度 → 每帧角度"应用到实例上（`KineticBlockEntity.getRotationAngleOffset(axis)` 可加偏移）。
- `ShaftVisual` / `OrientedRotatingVisual` / `SingleAxisRotatingVisual`：常用模板。
- BE 变化时调用 `VisualizationHelper.queueUpdate(this)`（`KineticBlockEntity.requestModelDataUpdate()` 已封装）。
- 传统 `BlockEntityRenderer`（如 `KineticBlockEntityRenderer`）仍可用，CEI 的 `GrindstoneDrainRenderer` 就是 BER；新元件优先 Visual。

### 8.2 交互

- `IWrenchable`（`IRotate` 已继承）：扳手旋转/拆卸。
- `IHaveGoggleInformation`：工程师护目镜信息；动力方块默认显示应力，`hideStressImpact()` 可隐藏。
- `ValueBox`/`ScrollValueBehaviour`：方块上的可调参数 UI（创造马达的转速就是 `KineticScrollValueBehaviour`）。
- `DirectBeltInputBehaviour`、`FilteringBehaviour` 等：物流/过滤行为，按需 `addBehaviours`。

### 8.3 Ponder

- Ponder 是独立库；addon 通过 `PonderRegistrationHelper` 注册场景（详见组件指南第 7 章）。
- 场景数据在 `assets/<modid>/ponder/`，语言键按 Ponder 约定生成。
- 6.0 的 `PonderSceneBuilder` API 与 0.5 有差异，务必按 6.0 源码写。

---

## 9. 附属模组如何接入（重点）

### 9.1 依赖声明

本仓库的做法（`build.gradle`）：

```gradle
def linkJars = fileTree('libs') { include '*.jar' }
dependencies {
    compileOnly linkJars
    runtimeOnly linkJars
}
```

- `libs/` 里放整包 jar（Create、Flywheel、Ponder、Registrate、Northstar 等），**compileOnly + runtimeOnly**：不进发布 jar，也不成为硬依赖。
- 代码里**允许直接 import Create 类**（编译期可见），但运行期要保证 Create 存在；对"可能不存在"的 mod（如 Apotheosis）才需要反射/隔离包。
- `neoforge.mods.toml` 里按需声明 `dependencies`：`type="required"` / `"optional"`，并写版本区间。
- **不要在 `mods.toml` 里声明 `create` 为 required 却又不打包它**——玩家必须自行安装 Create；如果我们的发布形态是"北极星星球整合包"，这没问题。

### 9.2 注册自己的东西

```java
public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID)
    .defaultCreativeTab(...)
    .setTooltipModifierFactory(item -> new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
        .andThen(TooltipModifier.mapNull(KineticStats.create(item))));
```

- 在 `@Mod` 构造器里 `REGISTRATE.registerEventListeners(modBus)`，然后 `MyBlocks.register()`、`MyBlockEntities.register()`。
- 方块用 `REGISTRATE.block(...)` 链式配置：`initialProperties`、`blockstate(...)`、`transform(...)`、`item()`、`lang(...)`。
- BE 用 `REGISTRATE.blockEntity("...", MyBE::new).validBlock(MyBlocks.X).register()`（或 `CreateRegistrate` 的 `blockEntity` builder，见 `AllBlockEntityTypes`）。
- 应力值：`BlockStressValues.IMPACTS.register(MyBlocks.X.get(), () -> config.impact.get())`。

### 9.3 软依赖与类加载

- 对**确定存在**的 Create：直接编译依赖最省事（本仓库 `libs/` 已具备）。
- 对**可能不存在**的 mod：
  - 单独包 + 只在检测到 mod 时 `Class.forName`/反射调用；
  - 或用 `ModList.get().isLoaded(...)` 做守卫，但**守卫不能保护类加载**——只要某个类引用了缺失类，加载该类就会 `NoClassDefFoundError`，所以"可选集成"要放在独立类/独立包里，且只在确认存在后触碰。
  - 参考 CEI 的 `integration.*` 包布局：每个可选 mod 一个包，主类只做条件注册。

### 9.4 与 Northstar（北极星星球）的关系

我们的模组是"北极星星球附属"，而北极星依赖 Create。实际落地建议：

- **把 Create 当作硬前置**（`mods.toml` 的 `dependencies` 里写 required），这样新元件可以直接继承 `KineticBlockEntity`/`GeneratingKineticBlockEntity`，不用反射。
- 新增元件放在独立包（例如 `com.suwako.northstarplanetaddon.create.kinetics`），与现有 `venus.compat.create` 的"探测式"代码分开；后者可以逐步替换为真实实现。
- 与星球机制结合时，优先做**应力源/传动**（更容易做出差异化，例如"火星沙尘风车"、"水星温差热机"），消耗装置则沿用 Create 的配方体系（`ProcessingRecipe`）以复用 JEI/Ponder/自动化。

### 9.5 兼容性红线

| 事项 | 结论 |
|---|---|
| 直接 `new` Create 的内部类 | 不要；用 API 包（`api.*`）与 `content.*` 里公开的基类 |
| 调用 `Create.registrate()` | 禁止（会抛异常） |
| 调用 `CStress.setImpact` | 禁止（非 create modid 抛异常） |
| 修改 Create 的 `KineticNetwork`/`RotationPropagator` | 不要；用 `propagateRotationTo`/`isCustomConnection` 扩展 |
| 自建高速/闭环传动 | 会被 `maxRotationSpeed` / flicker 保护破坏方块；设计时避开 |
| 在客户端类里引用服务端类（或反之） | 用 `@OnlyIn(Dist.CLIENT)`/`Dist` 分离，或 `CatnipServices.PLATFORM.executeOnClientOnly` |
| 依赖 `com.simibubi.create` 内部实现类 | 版本升级易碎；优先 `api.*`，其次 `content.kinetics.base.*` |

---

## 10. 建议的落地路线（针对本仓库）

1. **替换现有反射兼容层**：`venus/compat/create/CreateCompat.java` 目前只做探测；在 `libs/` 已有 Create jar 的前提下，可以直接 `compileOnly` 引用并实现真实的应力注册。
2. **先做一个"最小应力消耗方块"**：继承 `KineticBlockEntity` + 4 SU impact + 一个简单配方，跑通注册/渲染/tooltip。
3. **再做"最小应力源"**：继承 `GeneratingKineticBlockEntity` + 固定 RPM + 容量，验证 `updateGeneratedRotation` 与网络。
4. **最后做传动件**：从"同轴直传"开始，再加 `SplitShaftBlockEntity` 的换向/变速。
5. 每步都配 Ponder + goggle + JEI（如果涉及配方），并跑 `runData` 生成资源。

---

## 11. 参考文件索引（6.0.10 sources）

| 主题 | 文件 |
|---|---|
| 入口/初始化 | `Create.java` |
| 方块与应力值 | `AllBlocks.java`（`CStress.setImpact/setCapacity`、`BlockStressValues.setGeneratorSpeed`） |
| BE 基类 | `foundation/blockEntity/SmartBlockEntity.java`、`SyncedBlockEntity.java` |
| 动力 BE | `content/kinetics/base/KineticBlockEntity.java`、`GeneratingKineticBlockEntity.java` |
| 契约 | `content/kinetics/base/IRotate.java`、`KineticBlock.java`、`DirectionalKineticBlock.java`、`HorizontalKineticBlock.java`、`RotatedPillarKineticBlock.java` |
| 传播 | `content/kinetics/RotationPropagator.java`、`TorquePropagator.java` |
| 网络/应力 | `content/kinetics/KineticNetwork.java`、`api/stress/BlockStressValues.java` |
| 配置 | `infrastructure/config/CKinetics.java`、`CStress.java`、`AllConfigs.java` |
| 元件实现 | `content/kinetics/{waterwheel,windmill,steamEngine,motor,crank,millstone,press,mixer,saw,drill,crusher,fan,transmission,simpleRelays,gearbox,belt,chainDrive,speedController}/` |
| 渲染 | `content/kinetics/base/KineticBlockEntityVisual.java`、`RotatingInstance.java`、`ShaftVisual.java` |
| 数据/注册 | `foundation/data/CreateRegistrate.java`、`BuilderTransformers.java`、`api/registrate/CreateRegistrateRegistrationCallback.java` |
