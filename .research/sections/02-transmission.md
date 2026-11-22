# Create 6.0.10 传动系统（Kinetics Transmission）源码技术分析

> 目标版本：**Create 6.0.10 / Minecraft 1.21.1 / NeoForge**
> 源码根：`E:\mc\ModCreator\NorthstarPlanetAddon\.research\src\create\com\simibubi\create`
> 本文所有「路径:行号」的相对基准是该根目录（即 `content/...`、`infrastructure/...`），行号以 `read` 工具显示为准。
> 引用代码片段为方便阅读已把源码的 tab 缩进规整为空格，**token 顺序与源码一致**。
> 用途：为「北极星星球附属」新增传动 / 应力元件提供可直接照抄的契约与模板。

---

## 0. 结论速览（先看这 12 条）

1. 传动不是「方块互相推送」，而是**方块实体（BE）+ 整数速度标量**组成的图：`KineticBlockEntity.speed`（rpm，带符号）+ `source`（谁推动我）+ `network`（Long id，应力结算域）。`content/kinetics/base/KineticBlockEntity.java:58-77`。
2. 连接与倍率**全部由一个私有静态方法**决定：`RotationPropagator.getRotationSpeedModifier(from, to)`，返回 `0` 即「不连」。`content/kinetics/RotationPropagator.java:44-120`。
3. 判定顺序严格为：**自定义 `propagateRotationTo` → 同轴 → 链传动 → 大-大齿轮 → 大-小齿轮(±2/∓0.5) → 小-小齿轮(-1) → 0**。`content/kinetics/RotationPropagator.java:71-119`。
4. 符号语义：**+1 = 同轴同向同速；-1 = 啮合反转同速；-2 = 大带小（小轮 2 倍且反向）；-0.5 = 小带大；0 = 断开**。
5. 网络传播靠「功率压制」而非求最短路：`propagateNewSource` 反复比较双向传送速度，慢的一方改认快的一方为 source；符号冲突直接 `destroyBlock`。`content/kinetics/RotationPropagator.java:219-300`。
6. 超过 `maxRotationSpeed`（默认 256 rpm）或 `flickerScore > 128` 会**直接摧毁方块**，这是自定义传动元件最容易踩的坑。`content/kinetics/RotationPropagator.java:33, 235-243`；`infrastructure/config/CKinetics.java:14`。
7. `hasShaftTowards(... face)` 的 `face` 是**从本块指向邻块**的方向，且判定时必须**双向互补**（本块 `face`，对面 `face.getOpposite()`）。`content/kinetics/RotationPropagator.java:64-66`。
8. `getAxisModifier` 是**第二套倍率系统**，只对 `DirectionalShaftHalvesBlockEntity`（齿轮箱）与 `SplitShaftBlockEntity`（离合/换向/序列）生效，且只在「该 BE 已有 source 或自己是 source」时才生效。`content/kinetics/RotationPropagator.java:157-170`。
9. 齿轮箱的四个侧面不是 `getRotationAxis` 上的面，而是**垂直于 AXIS 的四个面**（`face.getAxis() != AXIS`），这是全库唯一这样写的传动件。`content/kinetics/gearbox/GearboxBlock.java:59-67`。
10. 传送带**不是**倍率元件：它对自己的每一段用 `propagateRotationTo` 返回 1 绑定到同一 controller，再靠两端的「滑轮面」（`hasPulley()`）用标准同轴规则 ±1 接轴。`content/kinetics/belt/BeltBlockEntity.java:552-564`；`content/kinetics/belt/BeltBlock.java:118-133`。
11. 转速控制器是唯一在 `getConveyedSpeed` 层被特判的元件（不是 `getRotationSpeedModifier`），因为它是「自己产生速度」的源。`content/kinetics/RotationPropagator.java:122-136, 187-198`。
12. **`getRotationSpeedModifier` 是 private static，附属模组无法调用**；能用的公开入口只有 `RotationPropagator.isConnected(a, b)`。`content/kinetics/RotationPropagator.java:44, 402-408`。

---

## 1. 传动骨架总览

### 1.1 三个数据结构

```
KineticBlockEntity
├── float speed             // 本块的理论转速（rpm，带符号）；getSpeed() 在 overstressed/暂停时返回 0
├── BlockPos source          // 把我转起来的上游 BE 坐标；null = 我无源
├── Long network             // 应力结算域 id；来源是源的 worldPosition.asLong()
└── 查询派生量：getTheoreticalSpeed() / getSpeed() / hasSource() / isSource()
```
- `speed` / `source` / `network` 字段：`content/kinetics/base/KineticBlockEntity.java:58-62`。
- `getSpeed()` 与 `getTheoreticalSpeed()` 的区别：前者在 `overStressed` 或 `level.tickRateManager().isFrozen()` 时返回 0，后者恒返回字段值。**传播算法一律用 `getTheoreticalSpeed()`**。`content/kinetics/base/KineticBlockEntity.java:289-301`。
- `hasSource()` / `isSource()`（`getGeneratedSpeed() != 0`）：`content/kinetics/base/KineticBlockEntity.java:281-287, 303-305`。

`KineticNetwork` 是应力（stress）与容量（capacity）的结算域，与「谁转谁」的拓扑分离：
- `members: Map<BE, Float>` = 各成员 1 rpm 基准应力；`sources: Map<BE, Float>` = 各源 1 rpm 基准容量。`content/kinetics/KineticNetwork.java:13-14`。
- 实际应力 = 基准 × `|speed|`（`getStressMultiplierForSpeed` = `Math.abs(speed)`）。`content/kinetics/KineticNetwork.java:165-175`。
- 过载判定：`overStressed = capacity < stress && StressImpact.isEnabled()`。`content/kinetics/base/KineticBlockEntity.java:160, 269`。

### 1.2 网络容器

`TorquePropagator` 持有 `Map<LevelAccessor, Map<Long, KineticNetwork>>`，`getOrCreateNetworkFor(be)` 用 `be.network` 取/建。`content/kinetics/TorquePropagator.java:14, 26-40`。
`KineticBlockEntity.getOrCreateNetwork()` 直通到这里：`content/kinetics/base/KineticBlockEntity.java:355-357`。

### 1.3 传播的三个入口

| 入口 | 触发时机 | 代码 |
|---|---|---|
| `attachKinetics()` → `RotationPropagator.handleAdded` | BE 初始化 `tick()` 发现 `updateSpeed`、发电机新建网络 | `content/kinetics/base/KineticBlockEntity.java:98-100, 363-366` |
| `detachKinetics()` → `RotationPropagator.handleRemoved` | 方块被移除 / 主动断网重算 | `content/kinetics/base/KineticBlockEntity.java:200-207, 368-370` |
| 直接静态调用 `handleAdded` / `handleRemoved` | 换向器、控制器改倍率 | `content/kinetics/transmission/GearshiftBlock.java:67-84`；`content/kinetics/speedController/SpeedControllerBlockEntity.java:75-84` |

---

## 2. 传动元件契约表

### 2.1 `IRotate`（方块侧的强制契约）

`public interface IRotate extends IWrenchable`，`content/kinetics/base/IRotate.java:17`。

| 成员 | 声明 | 语义 | 必要性 |
|---|---|---|---|
| `hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face)` | `IRotate.java:144` | 本块在 `face` 这一面是否有**可对接的轴端**。`face` 是「从 `pos` 指向邻块」的方向。传播器只会用**相邻 6 向**调用它，斜对角需要靠 `addPropagationLocations` 铺路。默认实现 `return false`（`KineticBlock.java:51-53`）。 | **必须** |
| `getRotationAxis(BlockState)` | `IRotate.java:146` | 本块绕**世界坐标哪个轴**自转。渲染器直接用它算旋转轴（`KineticBlockEntityRenderer.java:135-138`），所以必须与模型视觉一致。 | **必须** |
| `getMinimumRequiredSpeedLevel()` | `IRotate.java:148-150` | 默认 `SpeedLevel.NONE`；被 `KineticBlockEntity.isSpeedRequirementFulfilled()` 用来门控机器能否工作（`KineticBlockEntity.java:372-379`）。 | 可选（机器类建议） |
| `hideStressImpact()` | `IRotate.java:152-154` |  goggles 里隐藏应力行。默认 false。 | 可选 |
| `showCapacityWithAnnotation()` | `IRotate.java:156-158` | goggles 容量显示带注记。默认 false。 | 可选 |

`SpeedLevel`（`IRotate.java:19-97`）：
- 枚举顺序 NONE(0) / SLOW(10) / MEDIUM(20) / FAST(30)，`getSpeedValue()`：SLOW=1、MEDIUM=`mediumSpeed`（默认 30）、FAST=`fastSpeed`（默认 100）、NONE=0。`IRotate.java:47-61`，配置见 `infrastructure/config/CKinetics.java:58-59`。
- `of(speed)` 取 `abs(speed)` 后按阈值分档。`IRotate.java:63-73`。

`StressImpact`（`IRotate.java:99-142`）：LOW/MEDIUM/HIGH/OVERSTRESSED 只是**显示分档**（按 `stressPercent` 0.5 / 0.75 / 1.0），真正的数值来自 `api/stress/BlockStressValues`：
- `calculateStressApplied()` = `BlockStressValues.getImpact(block)`，`calculateAddedStressCapacity()` = `BlockStressValues.getCapacity(block)`。`content/kinetics/base/KineticBlockEntity.java:179-189`。
- 附属注册应力：`BlockStressValues.IMPACTS.register(myBlock, () -> 8.0)`（`IMPACTS` 是 `SimpleRegistry<Block, DoubleSupplier>`，`api/stress/BlockStressValues.java` 字段 `IMPACTS`/`CAPACITIES`/`RPM` 与 `SimpleRegistry.register(K, V)`，`api/registry/SimpleRegistry.java:32`）。

### 2.2 `KineticBlockEntity` 的可覆盖钩子（BE 侧契约）

| 成员 | 声明 | 语义（含符号/倍率） | 必要性 |
|---|---|---|---|
| `float propagateRotationTo(target, stateFrom, stateTo, diff, connectedViaAxes, connectedViaCogs)` | `KineticBlockEntity.java:543-546` | 返回 **this → target 的有符号倍率**。返回 `0` = 「没有自定义规则，走内置的轴/齿轮规则」。返回非 0 则**完全接管**该对连接的倍率（优先级最高，排在轴判定之前）。必须**互反**：`r(a→b) * r(b→a) == 1`（例：大齿轮给小齿轮 ±2，小给大 ∓0.5）。 | 可选（高级） |
| `boolean isCustomConnection(other, state, otherState)` | `KineticBlockEntity.java:586-588` | `true` = 「虽然内置规则说不连，但我要求建立连接」。用于远程/异形对接。javadoc 明说**不必两端都实现，视为对称**（`KineticBlockEntity.java:578-585`）。 | 可选 |
| `List<BlockPos> addPropagationLocations(block, state, neighbours)` | `KineticBlockEntity.java:558-572` | 邻域枚举扩展。默认 6 邻已在列表里（`RotationPropagator.java:430-434`），默认实现只在 `canPropagateDiagonally` 为真时补「垂直于旋转轴、距离²=2」的对角位（4 个）。可加**任意远**的坐标（链式传送带的 `connections` 就是这么做的）。 | 可选（斜对角/远程必须） |
| `protected boolean canPropagateDiagonally(IRotate block, BlockState state)` | `KineticBlockEntity.java:590-592` | 默认 `ICogWheel.isSmallCog(state)`。大齿轮在 `SimpleKineticBlockEntity` 里被整体重写（见 4.2）。 | 可选 |
| `int getRotationAngleOffset(Axis axis)` | `KineticBlockEntity.java:620-622` | 渲染相位偏置（度），只影响视觉，**不影响物理**。默认 0。 | 可选 |
| `float getGeneratedSpeed()` | `KineticBlockEntity.java:281-283` | 非 0 即「我是源」。 | 发电机必须 |
| `void onSpeedChanged(float previousSpeed)` | `KineticBlockEntity.java:191-197` | 速度/符号变化回调；内部会累加 `flickerTally += 5`（换向或过零时）。 | 可选 |
| `boolean syncSequenceContext()` | `KineticBlockEntity.java:624-626` | 是否把 `sequenceContext` 同步到客户端（序列齿轮箱相关）。 | 可选 |
| `protected BlockPos getSourceFacing()`（在 `DirectionalShaftHalvesBlockEntity`） | `content/kinetics/base/DirectionalShaftHalvesBlockEntity.java:14-17` | 源方向 = `Direction.getNearest(source - pos)`。**注意：无 source 时会 NPE**（直接 `source.subtract`），调用方必须先判 `hasSource()`。 | 分体传动件必须 |

> ⚠ **文档过期点**：`propagateRotationTo` 的 javadoc 写 `connectedViaCogs = "connected via mutual IRotate.hasIntegratedCogwheel()"`（`KineticBlockEntity.java:537`），但 6.0.10 的 `IRotate` 里**没有** `hasIntegratedCogwheel()` 方法（`IRotate.java` 全文只有 5 个成员）。实际传入值是 `ICogWheel.isSmallCog(stateFrom) && ICogWheel.isSmallCog(stateTo)`（`RotationPropagator.java:68-69`）。

### 2.3 方块基类的「自动行为」矩阵

| 基类 | 状态属性 | 放置时自动对齐 | 覆写/新增内容 | 来源 |
|---|---|---|---|---|
| `KineticBlock` | 无 | 无 | `onPlace` 里做 `preventSpeedUpdate` 优化；`updateIndirectNeighbourShapes` 清空动能信息；`areStatesKineticallyEquivalent` 默认比较 block+轴 | `content/kinetics/base/KineticBlock.java:24-78` |
| `DirectionalKineticBlock` | `FACING` | `getPreferredFacing` 扫描 6 邻找有轴端的轴 | 放置朝向/rotate/mirror | `content/kinetics/base/DirectionalKineticBlock.java:16, 28-69` |
| `HorizontalKineticBlock` | `HORIZONTAL_FACING` | `getPreferredHorizontalFacing` | 同 `getRotationAxis`/`hasShaftTowards` 由子类给 | `content/kinetics/base/HorizontalKineticBlock.java:16, 29-53` |
| `HorizontalAxisKineticBlock` | `HORIZONTAL_AXIS` | `getPreferredHorizontalAxis` | **已实现** `getRotationAxis` = HORIZONTAL_AXIS、`hasShaftTowards` = 同轴面 | `content/kinetics/base/HorizontalAxisKineticBlock.java:20, 40-66` |
| `RotatedPillarKineticBlock` | `AXIS`（默认 Y） | `getPreferredAxis`（静态） | rotate 90° 时 X↔Z | `content/kinetics/base/RotatedPillarKineticBlock.java:16-40, 42-60` |
| `DirectionalAxisKineticBlock` | `FACING` + `AXIS_ALONG_FIRST_COORDINATE` | 复杂：优先接已有轴端的邻居 | **已实现** `getRotationAxis`（由 FACING + alongFirst 推出垂直轴）与 `hasShaftTowards` | `content/kinetics/base/DirectionalAxisKineticBlock.java:21, 47-100, 103-116, 143-145` |
| `AbstractEncasedShaftBlock` | `AXIS` | 优先接已有轴端（shaft 语义） | `hasShaftTowards` = `face.getAxis() == AXIS`；`getRotationAxis` = AXIS；不可弱充能、活塞 NORMAL | `content/kinetics/base/AbstractEncasedShaftBlock.java:27-56` |
| `AbstractShaftBlock` | `AXIS` + `WATERLOGGED` | 同 pillar | 同上，另加 `IBE<KineticBlockEntity>` 与水 | `content/kinetics/simpleRelays/AbstractShaftBlock.java:22-48` |
| `AbstractSimpleShaftBlock` | 继承 | — | 支架（bracket）拆除/掉落、BE 类型固定为 `BRACKETED_KINETIC` | `content/kinetics/simpleRelays/AbstractSimpleShaftBlock.java:22-62` |

### 2.4 传动件必须满足的三条不变量

1. **`hasShaftTowards` 双向互补**：`connectedByAxis = alignedAxes && from.hasShaftTowards(direction) && to.hasShaftTowards(direction.getOpposite())`，任一方向为 false 就整条轴断开。`content/kinetics/RotationPropagator.java:64-66`。
2. **`getRotationAxis` 与实际自转轴一致**：渲染器用它算角度（`KineticBlockEntityRenderer.java:94-100`），Flywheel 视觉也用它（`content/kinetics/base/KineticBlockEntityVisual.java:39-41`）。写错 → 方块转速对但模型绕错轴转。
3. **倍率互反且符号可解释**：因为传播器会**双向各算一次**（`getConveyedSpeed(from,to)` 与 `getConveyedSpeed(to,from)`，`content/kinetics/RotationPropagator.java:226-227`），不互反会让「谁压制谁」判定随机化，最终触发 `destroyBlock`。

---

## 3. `RotationPropagator.getRotationSpeedModifier` 完整判定顺序

### 3.1 前置：`diff`、`direction`、`alignedAxes`

```java
// content/kinetics/RotationPropagator.java:44-66
private static float getRotationSpeedModifier(KineticBlockEntity from, KineticBlockEntity to) {
    final BlockState stateFrom = from.getBlockState();
    final BlockState stateTo = to.getBlockState();

    Block fromBlock = stateFrom.getBlock();
    Block toBlock = stateTo.getBlock();
    if (!(fromBlock instanceof IRotate definitionFrom && toBlock instanceof IRotate definitionTo))
        return 0;

    final BlockPos diff = to.getBlockPos().subtract(from.getBlockPos());
    final Direction direction = Direction.getNearest(diff.getX(), diff.getY(), diff.getZ());
    final Level world = from.getLevel();

    boolean alignedAxes = true;
    for (Axis axis : Axis.values())
        if (axis != direction.getAxis())
            if (axis.choose(diff.getX(), diff.getY(), diff.getZ()) != 0)
                alignedAxes = false;

    boolean connectedByAxis =
        alignedAxes && definitionFrom.hasShaftTowards(world, from.getBlockPos(), stateFrom, direction)
            && definitionTo.hasShaftTowards(world, to.getBlockPos(), stateTo, direction.getOpposite());

    boolean connectedByGears = ICogWheel.isSmallCog(stateFrom) && ICogWheel.isSmallCog(stateTo);
```

要点：
- 两端都必须是 `IRotate` 方块，否则 0。
- `direction` 用 `Direction.getNearest`，对斜向偏移只会取「主轴方向」的那个最近方向；`alignedAxes` 保证 `diff` 只在 `direction` 的轴上非零，**斜对角偏移的 `alignedAxes` 为 false**，因此永远不会走同轴分支（幂等地保证了「斜对角必须靠齿轮分支或自定义」）。
- `connectedByAxis` 是第 2.4 节的双向互补判定。
- `connectedByGears` **只表示两端都是小齿轮**，不含距离/轴向校验，那些在下游 3.8 才判。

### 3.2 第一步：自定义连接（优先级最高）

```java
// content/kinetics/RotationPropagator.java:71-73
float custom = from.propagateRotationTo(to, stateFrom, stateTo, diff, connectedByAxis, connectedByGears);
if (custom != 0)
    return custom;
```
- 只有 `from` 一侧被调用；反向由上层的 `getConveyedSpeed(to, from)` 单独调 `to.propagateRotationTo(from, ...)`。
- 因此**自定义倍率的互反性由你自己负责**，传播器不会替你取倒数（与 `getAxisModifier` 分支不同）。
- 现存的 4 个实现：`content/kinetics/belt/BeltBlockEntity.java:559`、`content/kinetics/chainConveyor/ChainConveyorBlockEntity.java:665`、`content/kinetics/gantry/GantryShaftBlockEntity.java:53`、`content/contraptions/gantry/GantryCarriageBlockEntity.java:145`。

### 3.3 第二步：同轴连接 + `getAxisModifier`

```java
// content/kinetics/RotationPropagator.java:75-81
// Axis <-> Axis
if (connectedByAxis) {
    float axisModifier = getAxisModifier(to, direction.getOpposite());
    if (axisModifier != 0)
        axisModifier = 1 / axisModifier;
    return getAxisModifier(from, direction) * axisModifier;
}
```
- 纯轴/纯轴：两个 `getAxisModifier` 都返回 1 → 结果 `+1`（同向同速）。
- 一端是齿轮箱/分体轴时，倍率由第二套系统给出：
```java
// content/kinetics/RotationPropagator.java:157-170
private static float getAxisModifier(KineticBlockEntity be, Direction direction) {
    if (!(be.hasSource() || be.isSource()) || !(be instanceof DirectionalShaftHalvesBlockEntity))
        return 1;
    Direction source = ((DirectionalShaftHalvesBlockEntity) be).getSourceFacing();

    if (be instanceof GearboxBlockEntity)
        return direction.getAxis() == source.getAxis() ? direction == source ? 1 : -1
            : direction.getAxisDirection() == source.getAxisDirection() ? -1 : 1;

    if (be instanceof SplitShaftBlockEntity)
        return ((SplitShaftBlockEntity) be).getRotationSpeedModifier(direction);

    return 1;
}
```
- 三个关键点：
  1. **无 source 且自己不是 source 时一律返回 1**——所以齿轮箱刚放下、网络尚未定向的那一刻，它表现为「普通直轴」。
  2. `to` 侧取**倒数**，所以 `SplitShaftBlockEntity` 只需实现「相对源方向」的倍率：一个返回 `0` 的离合面，在对面看来是 `0`（`0` 不做倒数，`1/0` 被 `if (axisModifier != 0)` 保护），返回 `2` 的面在对面看来是 `0.5`。
  3. `direction` 参数永远是「**从该 BE 指向邻块**」的方向（`from` 侧传 `direction`，`to` 侧传 `direction.getOpposite()`）。

### 3.4 第三步：链传动（方块名是 Encased Chain Drive）

```java
// content/kinetics/RotationPropagator.java:83-87
// Attached Encased Belts
if (fromBlock instanceof ChainDriveBlock && toBlock instanceof ChainDriveBlock) {
    boolean connected = ChainDriveBlock.areBlocksConnected(stateFrom, stateTo, direction);
    return connected ? ChainDriveBlock.getRotationSpeedModifier(from, to) : 0;
}
```
- **两端都必须是 `ChainDriveBlock`**，否则跳过（所以链条方块和普通轴之间走同轴分支）。
- 连接性由 `PART` + `CONNECTED_ALONG_FIRST_COORDINATE` 推出的 `connectionAxis` 决定：`content/kinetics/chainDrive/ChainDriveBlock.java:173-196`。
- 倍率是**可调链轮箱的比值**：`fromMod / toMod`，`getModifierForSignal(power) = 1 + (power+1)/16`。`content/kinetics/chainDrive/ChainDriveBlock.java:198-206`；`content/kinetics/chainDrive/ChainGearshiftBlockEntity.java:70-74`。

### 3.5 第四步：大齿轮 ↔ 大齿轮（`±1`，45° 交叉啮合）

```java
// content/kinetics/RotationPropagator.java:89-97
if (isLargeToLargeGear(stateFrom, stateTo, diff)) {
    Axis sourceAxis = stateFrom.getValue(AXIS);
    Axis targetAxis = stateTo.getValue(AXIS);
    int sourceAxisDiff = sourceAxis.choose(diff.getX(), diff.getY(), diff.getZ());
    int targetAxisDiff = targetAxis.choose(diff.getX(), diff.getY(), diff.getZ());
    return sourceAxisDiff > 0 ^ targetAxisDiff > 0 ? -1 : 1;
}
```
几何条件（`content/kinetics/RotationPropagator.java:138-155`）：两端都是大齿轮、**旋转轴不同**，且对每个轴：属于 `fromAxis` 或 `toAxis` 的轴上 `diff` 分量非 0，第三轴分量为 0。即只允许 `(±1, ±1, 0)` 这类「两个轴各差 1」的错位——这就是大齿轮成 45° 斜接的几何。
符号：两侧 `diff` 分量**同号则 +1，异号则 -1**（XOR）。物理直觉：两个垂直轴大齿轮，一个在对方 `+` 侧、一个在 `-` 侧时，转向关系相反。

### 3.6 第五步：大齿轮 ↔ 小齿轮（`-2` / `-0.5`）

```java
// content/kinetics/RotationPropagator.java:99-105
// Gear <-> Large Gear
if (ICogWheel.isLargeCog(stateFrom) && ICogWheel.isSmallCog(stateTo))
    if (isLargeToSmallCog(stateFrom, stateTo, definitionTo, diff))
        return -2f;
if (ICogWheel.isLargeCog(stateTo) && ICogWheel.isSmallCog(stateFrom))
    if (isLargeToSmallCog(stateTo, stateFrom, definitionFrom, diff))
        return -.5f;
```
几何条件（`content/kinetics/RotationPropagator.java:172-185`）：
```java
Axis axisFrom = from.getValue(AXIS);                 // 大齿轮的轴
if (axisFrom != defTo.getRotationAxis(to)) return false;   // 小齿轮的旋转轴必须与它平行
if (axisFrom.choose(diff) != 0) return false;              // 沿大齿轮轴向不得偏移
for (Axis axis : Axis.values()) {
    if (axis == axisFrom) continue;
    if (Math.abs(axis.choose(diff)) != 1) return false;    // 另两轴必须各差 1 → 斜对角
}
return true;
```
- 「大带小」：**大齿轮 1 rpm → 小齿轮 -2 rpm**（反向、双倍）；「小带大」→ **-0.5 rpm**（反向、半速）。`-2 * -0.5 = 1`，互反成立。
- 这也是为什么不写 `getRotationSpeedModifier` 也能用：`isConnected` 只要求「任一方向非 0」。

### 3.7 第六步：小齿轮 ↔ 小齿轮（`-1`）

```java
// content/kinetics/RotationPropagator.java:107-117
// Gear <-> Gear
if (connectedByGears) {
    if (diff.distManhattan(BlockPos.ZERO) != 1) return 0;                 // 必须正相邻
    if (ICogWheel.isLargeCog(stateTo)) return 0;                          // 大齿轮已在上一步处理
    if (direction.getAxis() == definitionFrom.getRotationAxis(stateFrom)) return 0;  // 不得沿自身轴向错位
    if (definitionFrom.getRotationAxis(stateFrom) == definitionTo.getRotationAxis(stateTo))
        return -1;                                                        // 轴平行 → 反向同速
}
```

四个门坎逐个解释：
1. `distManhattan != 1` → 小齿轮只在**正相邻**时啮合（斜对角必须靠 `addPropagationLocations` 先枚举出该坐标，且 `distManhattan` 仍须为 1——见 4.2 的补充说明）。
2. `to` 是大齿轮 → 交给 3.6。
3. `direction` 的轴与 from 的旋转轴相同 → 说明两齿轮同轴排列，那是**轴对接**而不是啮合（应由 `connectedByAxis` 接管，此处避免重复计入）。
4. 两轴平行 → `-1`，一个齿距的反向传动。

### 3.8 第七步：都不匹配

```java
// content/kinetics/RotationPropagator.java:119
return 0;
```

### 3.9 `getConveyedSpeed`：唯一的速度合成点 + 转速控制器特判

```java
// content/kinetics/RotationPropagator.java:122-136
private static float getConveyedSpeed(KineticBlockEntity from, KineticBlockEntity to) {
    final BlockState stateFrom = from.getBlockState();
    final BlockState stateTo = to.getBlockState();

    // Rotation Speed Controller <-> Large Gear
    if (isLargeCogToSpeedController(stateFrom, stateTo, to.getBlockPos().subtract(from.getBlockPos())))
        return SpeedControllerBlockEntity.getConveyedSpeed(from, to, true);
    if (isLargeCogToSpeedController(stateTo, stateFrom, from.getBlockPos().subtract(to.getBlockPos())))
        return SpeedControllerBlockEntity.getConveyedSpeed(to, from, false);

    float rotationSpeedModifier = getRotationSpeedModifier(from, to);
    return from.getTheoreticalSpeed() * rotationSpeedModifier;
}
```
- 通用公式：`to.speed = from.theoreticalSpeed * ratio`。
- 特判的几何判定（`content/kinetics/RotationSpeedController` 专用）：
```java
// content/kinetics/RotationPropagator.java:187-198
if (!ICogWheel.isLargeCog(from) || !AllBlocks.ROTATION_SPEED_CONTROLLER.has(to)) return false;
if (!diff.equals(BlockPos.ZERO.below())) return false;          // 大齿轮必须正上方
Axis axis = from.getValue(CogWheelBlock.AXIS);
if (axis.isVertical()) return false;                            // 大齿轮轴必须水平
if (to.getValue(SpeedControllerBlock.HORIZONTAL_AXIS) == axis) return false;  // 且与控制器轴交叉
return true;
```
- `getConveyedSpeed(..., targetingController)` 的语义：`targetingController == true` 表示「问控制器 → 齿轮这一方向的速度」，false 表示「问齿轮 → 控制器」。`content/kinetics/speedController/SpeedControllerBlockEntity.java:86-102`。

### 3.10 `handleAdded` / `propagateNewSource`：网络定向与「压制」

```java
// content/kinetics/RotationPropagator.java:206-212
public static void handleAdded(Level worldIn, BlockPos pos, KineticBlockEntity addedTE) {
    if (worldIn.isClientSide) return;
    if (!worldIn.isLoaded(pos)) return;
    propagateNewSource(addedTE);
}
```

```java
// content/kinetics/RotationPropagator.java:219-300（节选核心分支）
for (KineticBlockEntity neighbourTE : getConnectedNeighbours(currentTE)) {
    float speedOfCurrent = currentTE.getTheoreticalSpeed();
    float speedOfNeighbour = neighbourTE.getTheoreticalSpeed();
    float newSpeed = getConveyedSpeed(currentTE, neighbourTE);      // current → neighbour
    float oppositeSpeed = getConveyedSpeed(neighbourTE, currentTE);  // neighbour → current

    if (newSpeed == 0 && oppositeSpeed == 0) continue;

    boolean incompatible =
        Math.signum(newSpeed) != Math.signum(speedOfNeighbour) && (newSpeed != 0 && speedOfNeighbour != 0);

    boolean tooFast = Math.abs(newSpeed) > AllConfigs.server().kinetics.maxRotationSpeed.get()
        || Math.abs(oppositeSpeed) > AllConfigs.server().kinetics.maxRotationSpeed.get();

    boolean speedChangedTooOften = currentTE.getFlickerScore() > MAX_FLICKER_SCORE;
    if (tooFast || speedChangedTooOften) {
        world.destroyBlock(pos, true);
        return;
    }

    // Opposite directions
    if (incompatible) {
        world.destroyBlock(pos, true);
        return;
    }
```
分支表：

| 条件 | 行为 |
|---|---|
| 双向都 0 | 跳过（不连） |
| 新速度符号 ≠ 邻块现有速度符号（且都非 0） | **摧毁本方块**（`destroyBlock(pos, true)`），掉落物品 |
| `|speed| > maxRotationSpeed`（255/默认 256 上限） | **摧毁本方块** |
| `flickerScore > 128` | **摧毁本方块** |
| 反向速度（邻块推我）比我现有速度绝对值大 | 我改认邻块为 source，`setSpeed(邻块推我的速度)`，继续递归 |
| 我推邻居的速度 ≥ 邻块现有速度 | 邻块改认我为 source（除非同网络 → 环路 → 见下） |
| 同网络且我要给的速度比邻块大（超过 epsilon） | **摧毁本方块**（环 + 加速） |
| 其它 | 邻块 `setSpeed(newSpeed)` + `setSource(this)` + 递归 |

环检测细节（`content/kinetics/RotationPropagator.java:266-274`）：
```java
if (Math.abs(newSpeed) >= Math.abs(speedOfNeighbour)) {
    // Do not overpower you own network -> cycle
    if (!currentTE.hasNetwork() || currentTE.network.equals(neighbourTE.network)) {
        float epsilon = Math.abs(speedOfNeighbour) / 256f / 256f;
        if (Math.abs(newSpeed) > Math.abs(speedOfNeighbour) + epsilon)
            world.destroyBlock(pos, true);
        continue;
    }
    ...
```
即：**同一 network 内部不允许存在「速度不相等」的推力**。这条正是不论自建自定义连接还是齿轮组的「闭环加速」防爆阀。

`handleRemoved`（`content/kinetics/RotationPropagator.java:309-331`）只处理「以我为 source 的邻居」，逐层 `propagateMissingSource` 清 source 并找新源（`content/kinetics/RotationPropagator.java:339-381`）。注意：
```java
if (removedBE.getTheoreticalSpeed() == 0) return;   // :314-315 零速块被移除时不做任何重算
```
→ **如果你的自定义元件断开时速度已经是 0，就不会触发重新定向**，需要自己 `handleAdded` 一侧补齐（见 6.4 第 5 条）。

### 3.11 `isConnected` 与邻域枚举

```java
// content/kinetics/RotationPropagator.java:402-408
public static boolean isConnected(KineticBlockEntity from, KineticBlockEntity to) {
    final BlockState stateFrom = from.getBlockState();
    final BlockState stateTo = to.getBlockState();
    return isLargeCogToSpeedController(stateFrom, stateTo, to.getBlockPos().subtract(from.getBlockPos()))
        || getRotationSpeedModifier(from, to) != 0
        || from.isCustomConnection(to, stateFrom, stateTo);
}
```
- **这是唯一对附属公开的连接查询**（`public static`）。`getRotationSpeedModifier` 与 `getConveyedSpeed` 都是 private。
- 不对称性处理：`findConnectedNeighbour` 做 `if (!isConnected(a,b) && !isConnected(b,a)) return null;`（`content/kinetics/RotationPropagator.java:397`），所以「只在一端实现 `isCustomConnection`」也能连上。

邻域枚举链：
```java
// content/kinetics/RotationPropagator.java:422-440
for (Direction facing : Iterate.directions) {                    // 先放 6 邻
    BlockPos relative = blockPos.relative(facing);
    if (level.isLoaded(relative)) neighbours.add(relative);
}
BlockState blockState = be.getBlockState();
if (!(blockState.getBlock() instanceof IRotate block)) return neighbours;
return be.addPropagationLocations(block, blockState, neighbours);  // 再让 BE 追加
```
→ **自定义连接的目标必须先出现在 `neighbours` 里**，否则传播器根本不会去查它。这是「远程连接」唯一可行的实现路径。

---

## 4. 逐个元件

### 4.1 轴（Shaft）/ 大轴基类 / 支架轴 / 包壳轴

**实现方式**
- `AbstractShaftBlock` = `RotatedPillarKineticBlock` + `IBE<KineticBlockEntity>` + `ProperWaterloggedBlock`；提供全部三个 IRotate 必需实现：
```java
// content/kinetics/simpleRelays/AbstractShaftBlock.java:41-48
@Override
public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
    return face.getAxis() == state.getValue(AXIS);
}

@Override
public Axis getRotationAxis(BlockState state) { return state.getValue(AXIS); }
```
- `ShaftBlock` 只做三件事：放置时可能替换成 `PoweredShaftBlock`（蒸汽引擎轴）、`EncasableBlock` 包壳、极杆式连续放置（`PoleHelper`）。`content/kinetics/simpleRelays/ShaftBlock.java:35-51, 103-135`。
- `AbstractSimpleShaftBlock` 绑死了 BE 类型：
```java
// content/kinetics/simpleRelays/AbstractSimpleShaftBlock.java:59-62
@Override
public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() {
    return AllBlockEntityTypes.BRACKETED_KINETIC.get();
}
```
- 支架由 BE 行为挂上，且只允许 shaft 类方块：
```java
// content/kinetics/simpleRelays/BracketedKineticBlockEntity.java:21-26
@Override
public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    behaviours.add(new BracketedBlockEntityBehaviour(this, state -> state.getBlock() instanceof AbstractSimpleShaftBlock));
    super.addBehaviours(behaviours);
}
```
- `AbstractEncasedShaftBlock`（包壳轴、链条传动轴的共同父类）只是把 pillar 逻辑重写干净，并显式禁掉弱充能与奇怪活塞行为：`content/kinetics/base/AbstractEncasedShaftBlock.java:27-56`。

**倍率/方向规则**：同轴 → `+1`（没有 `getAxisModifier` 贡献），这就是「整条轴同向同速」。

**关键代码**：`areStatesKineticallyEquivalent` 默认实现会检查轴是否变化，所以用扳手转轴（Y↔X）会触发完整重算：
```java
// content/kinetics/base/KineticBlock.java:55-59
protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
    if (oldState.getBlock() != newState.getBlock()) return false;
    return getRotationAxis(newState) == getRotationAxis(oldState);
}
```

### 4.2 小齿轮 / 大齿轮

**实现方式**
- 一个类两种形态：`CogWheelBlock.small(...)` / `.large(...)`，字段 `boolean isLarge`，并分别返回 `isSmallCog()/isLargeCog()`。`content/kinetics/simpleRelays/CogWheelBlock.java:42-63`。
- `ICogWheel` 的默认值很关键：`isLargeCog()` 默认 false，`isSmallCog()` 默认 `!isLargeCog()`（**即任何实现 ICogWheel 的方块默认都算小齿轮**）。`content/kinetics/simpleRelays/ICogWheel.java:54-64`。
- 依赖 `SimpleKineticBlockEntity` 做邻域扩展。

**倍率/方向规则**

| 组合 | 倍率 | 几何条件 | 代码 |
|---|---|---|---|
| 小-小 | `-1` | 正相邻（曼哈顿 1）、轴平行、不沿自身轴向错位 | `RotationPropagator.java:107-117` |
| 大-大 | `±1` | 两轴不同，`diff` 在两轴上各差 1、第三轴 0 | `RotationPropagator.java:89-97, 138-155` |
| 大→小 | `-2` | 小轮轴与大轮轴平行；沿大轮轴不偏移；另两轴各差 1 | `RotationPropagator.java:100-102, 172-185` |
| 小→大 | `-0.5` | 同上（角色互换） | `RotationPropagator.java:103-105` |

**斜对角连接的关键差异**（小轮 vs 大轮）：

```java
// content/kinetics/base/KineticBlockEntity.java:558-572（小齿轮默认）
public List<BlockPos> addPropagationLocations(IRotate block, BlockState state, List<BlockPos> neighbours) {
    if (!canPropagateDiagonally(block, state)) return neighbours;
    Axis axis = block.getRotationAxis(state);
    BlockPos.betweenClosedStream(new BlockPos(-1, -1, -1), new BlockPos(1, 1, 1))
        .forEach(offset -> {
            if (axis.choose(offset.getX(), offset.getY(), offset.getZ()) != 0) return;  // 必须在垂直平面内
            if (offset.distSqr(BlockPos.ZERO) != 2) return;                            // 只取对角
            neighbours.add(worldPosition.offset(offset));
        });
    return neighbours;
}
```
```java
// content/kinetics/simpleRelays/SimpleKineticBlockEntity.java:25-35（大齿轮）
@Override
public List<BlockPos> addPropagationLocations(IRotate block, BlockState state, List<BlockPos> neighbours) {
    if (!ICogWheel.isLargeCog(state))
        return super.addPropagationLocations(block, state, neighbours);
    BlockPos.betweenClosedStream(new BlockPos(-1, -1, -1), new BlockPos(1, 1, 1))
        .forEach(offset -> {
            if (offset.distSqr(BlockPos.ZERO) == 2)
                neighbours.add(worldPosition.offset(offset));   // 12 个对角位全给，不限制轴向
        });
    return neighbours;
}
```
- 小齿轮：只加**垂直于自身旋转轴**的 4 个对角位（大-小、小-小斜接用）。
- 大齿轮：加**全部 12 个**对角位（大-大 45° 斜接用，两轴必然不同）。
- 注意大齿轮分支**跳过了 `super`**，所以大齿轮不会重复添加小齿轮那 4 个位置（`SimpleKineticBlockEntity.java:26-27` 提前 return）。

**放置合法性**：`CogWheelBlock.isValidCogwheelPosition` 禁止齿轮「面对面重叠齿」：
```java
// content/kinetics/simpleRelays/CogWheelBlock.java:130-144
for (Direction facing : Iterate.directions) {
    if (facing.getAxis() == cogAxis) continue;
    BlockPos offsetPos = pos.relative(facing);
    BlockState blockState = worldIn.getBlockState(offsetPos);
    if (blockState.hasProperty(AXIS) && facing.getAxis() == blockState.getValue(AXIS)) continue;
    if (ICogWheel.isLargeCog(blockState) || large && ICogWheel.isSmallCog(blockState))
        return false;
}
```
放置轴还会特判「放在转速控制器上」→ 轴取交叉方向（`CogWheelBlock.java:156-157`），以及与控制器联动（见 4.7）。

### 4.3 齿轮箱（Gearbox）

**实现方式**
- 方块：`RotatedPillarKineticBlock`，但**语义反转**——`AXIS` 是「自转轴」，而轴端面是**垂直于 AXIS 的 4 个面**：
```java
// content/kinetics/gearbox/GearboxBlock.java:59-67
@Override
public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
    return face.getAxis() != state.getValue(AXIS);
}

@Override
public Axis getRotationAxis(BlockState state) { return state.getValue(AXIS); }
```
这是全库唯一 `hasShaftTowards` 用 `!=` 的传动件，也是「齿轮箱把一根轴翻成 4 根垂直轴」的实现基础。
- 放置时**永远先按 `AXIS=Y`**（`GearboxBlock.java:52-55`），再由 `VerticalGearboxItem` 在物品落地时按邻接轴端改写 AXIS：
```java
// content/kinetics/gearbox/VerticalGearboxItem.java:37-57
Axis prefferedAxis = null;
for (Direction side : Iterate.horizontalDirections) { ... }   // 扫水平 4 邻找已有轴端
Axis axis = prefferedAxis == null ? player.getDirection().getClockWise().getAxis()
        : prefferedAxis == Axis.X ? Axis.Z : Axis.X;         // 与邻轴垂直
world.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.AXIS, axis));
```
- 掉落/取物：水平形态掉 `vertical_gearbox` 物品。`GearboxBlock.java:36-50`。
- BE 是纯粹的 `DirectionalShaftHalvesBlockEntity`（只为了 `getSourceFacing`），自身**没有任何倍率逻辑**：
```java
// content/kinetics/gearbox/GearboxBlockEntity.java:9-20
public class GearboxBlockEntity extends DirectionalShaftHalvesBlockEntity {
    public GearboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    @Override protected boolean isNoisy() { return false; }
}
```

**倍率/方向规则**（`content/kinetics/RotationPropagator.java:162-164`）
```java
if (be instanceof GearboxBlockEntity)
    return direction.getAxis() == source.getAxis() ? direction == source ? 1 : -1
        : direction.getAxisDirection() == source.getAxisDirection() ? -1 : 1;
```
- 与源同轴：
  - 方向 = 源方向（即「源那一侧」）→ `1`
  - 方向 = 源的反向（AXIS 轴的另一侧）→ `-1`（同轴对面自然反转）
- 与源异轴（4 个侧面输出）：
  - 该面与源面 `AxisDirection` 相同 → `-1`
  - 不同 → `1`

**交叉验证**：客户端视觉用完全相同的条件算每面转速，证明这套规则就是「每面转向」的编码：
```java
// content/kinetics/gearbox/GearboxVisual.java:53-63
private float getSpeed(Direction direction) {
    float speed = blockEntity.getSpeed();
    if (speed != 0 && sourceFacing != null) {
        if (sourceFacing.getAxis() == direction.getAxis())
            speed *= sourceFacing == direction ? 1 : -1;
        else if (sourceFacing.getAxisDirection() == direction.getAxisDirection())
            speed *= -1;
    }
    return speed;
}
```
`GearboxRenderer`（非 Flywheel 回退路径）里是同一段逻辑的 `angle *=` 版本：`content/kinetics/gearbox/GearboxRenderer.java:44-51`。

**注意**：齿轮箱**不需要**多网络或多 BE，一个 BE 一个 speed 就能表达 4 面不同转向，因为「转向」是相对各自轴的，倍率只在 `getAxisModifier` 里现算。

### 4.4 离合器（Clutch）与 换向器（Gearshift）

**实现方式（共用一套骨架）**
- `SplitShaftBlockEntity` 只多一个抽象方法：
```java
// content/kinetics/transmission/SplitShaftBlockEntity.java:10-17
public abstract class SplitShaftBlockEntity extends DirectionalShaftHalvesBlockEntity {
    public abstract float getRotationSpeedModifier(Direction face);
}
```
- 两个 BE 的实现只差返回值：
```java
// content/kinetics/transmission/ClutchBlockEntity.java:15-22
@Override
public float getRotationSpeedModifier(Direction face) {
    if (hasSource()) {
        if (face != getSourceFacing() && getBlockState().getValue(BlockStateProperties.POWERED))
            return 0;                       // 通电时：非源侧 → 断开
    }
    return 1;
}
```
```java
// content/kinetics/transmission/GearshiftBlockEntity.java:15-22
@Override
public float getRotationSpeedModifier(Direction face) {
    if (hasSource()) {
        if (face != getSourceFacing() && getBlockState().getValue(BlockStateProperties.POWERED))
            return -1;                      // 通电时：非源侧 → 反转
    }
    return 1;
}
```
- **`face != getSourceFacing()` 是核心**：源那一侧永远返回 1，避免「自我反转」把源也翻掉（否则整网符号来回震荡 → flicker → 方块被摧毁）。
- 方块：`ClutchBlock extends GearshiftBlock`（只改 BE 类型 + 自己的邻居更新）：`content/kinetics/transmission/ClutchBlock.java:11-33`。

**状态变化时如何让网络重算**——这是全库最值得照抄的一段：
```java
// content/kinetics/transmission/GearshiftBlock.java:44-55
@Override
public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
                            boolean isMoving) {
    if (worldIn.isClientSide) return;

    boolean previouslyPowered = state.getValue(POWERED);
    if (previouslyPowered != worldIn.hasNeighborSignal(pos)) {
        detachKinetics(worldIn, pos, true);                       // 1) 立刻断网重定向
        worldIn.setBlock(pos, state.cycle(POWERED), Block.UPDATE_CLIENTS);
    }
}
```
```java
// content/kinetics/transmission/GearshiftBlock.java:67-84
public void detachKinetics(Level worldIn, BlockPos pos, boolean reAttachNextTick) {
    BlockEntity be = worldIn.getBlockEntity(pos);
    if (be == null || !(be instanceof KineticBlockEntity)) return;
    RotationPropagator.handleRemoved(worldIn, pos, (KineticBlockEntity) be);

    // Re-attach next tick
    if (reAttachNextTick)
        worldIn.scheduleTick(pos, this, 1, TickPriority.EXTREMELY_HIGH);
}

@Override
public void tick(BlockState state, ServerLevel worldIn, BlockPos pos, RandomSource random) {
    BlockEntity be = worldIn.getBlockEntity(pos);
    if (be == null || !(be instanceof KineticBlockEntity kte)) return;
    RotationPropagator.handleAdded(worldIn, pos, kte);
}
```
时序：**先 detach（让下游失去 source、速度归零）→ 再改 blockstate → 下一 tick attach（用新倍率重新定向）**。顺序颠倒会让下游带着旧符号参与判定，触发 `incompatible` → 摧毁方块。

`ClutchBlock` 的顺序相反（先改状态再 detach），因为它返回 0 是「断开」而不是「反转」，符号安全：
```java
// content/kinetics/transmission/ClutchBlock.java:17-28
boolean previouslyPowered = state.getValue(POWERED);
if (previouslyPowered != worldIn.hasNeighborSignal(pos)) {
    worldIn.setBlock(pos, state.cycle(POWERED), 2 | 16);
    detachKinetics(worldIn, pos, previouslyPowered);
}
```
（`detachKinetics` 复用父类方法，参数 `reAttachNextTick = previouslyPowered`：断电恢复时才需要 attach。）

### 4.5 序列齿轮箱（Sequenced Gearshift）

**方块**
- `SequencedGearshiftBlock extends HorizontalAxisKineticBlock`，额外 `VERTICAL`、`STATE(0..5)` 两个属性（`STATE` 同时是模拟输出值）。`content/kinetics/transmission/sequencer/SequencedGearshiftBlock.java:42-53, 163-167`。
- `hasShaftTowards` 在 `VERTICAL` 时只认竖直面，否则走 `HorizontalAxisKineticBlock` 的同轴规则：`SequencedGearshiftBlock.java:84-89, 142-146`。
- **主动放弃「状态等价」优化**，任何 state 变化都重算（因为 `STATE` 直接影响倍率）：
```java
// content/kinetics/transmission/sequencer/SequencedGearshiftBlock.java:79-81
@Override
protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
    return false;
}
```
- 红石：`neighborChanged` 只排一个 tick，真正的逻辑在 BE（`SequencedGearshiftBlock.java:61-76`）。

**BE 的倍率**
```java
// content/kinetics/transmission/sequencer/SequencedGearshiftBlockEntity.java:234-246
@Override
public float getRotationSpeedModifier(Direction face) {
    if (isVirtual()) return 1;
    return (!hasSource() || face == getSourceFacing()) ? 1 : getModifier();
}

public int getModifier() {
    if (currentInstruction >= instructions.size()) return 0;
    return isIdle() ? 0
        : instructions.get(currentInstruction).getSpeedModifier();
}
```
- 空闲（`currentInstruction == -1`）→ 倍率 0 → **等同于断开**（所以序列齿轮箱默认是「离合器断开」状态）。
- 运行中 → `Instruction.getSpeedModifier()` 返回 `speedModifier.value ∈ {2, 1, -1, -2}`，来自 `InstructionSpeedModifiers.FORWARD_FAST(2) / FORWARD(1) / BACK(-1) / BACK_FAST(-2)`。`content/kinetics/transmission/sequencer/InstructionSpeedModifiers.java:16-18`。
- `DELAY` / `AWAIT` / `END` 的 `getSpeedModifier()` 返回 0（只计时，不传动）。`Instruction.java:90-105`。

**指令模型**
```java
// content/kinetics/transmission/sequencer/SequencerInstructions.java:15-22
TURN_ANGLE("angle", ..., true, true, 360, 45, 90),
TURN_DISTANCE("distance", ..., true, true, 128, 5, 5),
DELAY("duration", ..., true, false, 600, 20, 10),
AWAIT("", AllGuiTextures.SEQUENCER_AWAIT),
END("", AllGuiTextures.SEQUENCER_END),
```
`needsPropagation()` 只对 TURN_ANGLE / TURN_DISTANCE 为真（`SequencerInstructions.java:53-55`），它们需要把「角度/距离」折算成「相对目标速度的 ticks」：
```java
// content/kinetics/transmission/sequencer/Instruction.java:41-67
int getDuration(float currentProgress, float speed) {
    speed *= speedModifier.value;
    speed = Math.abs(speed);
    double target = value - currentProgress;
    switch (instruction) {
    case TURN_ANGLE:
        double degreesPerTick = KineticBlockEntity.convertToAngular(speed);
        return (int) Math.ceil(target / degreesPerTick) + 2;   // +2 故意超调，由 target 提前截断
    case TURN_DISTANCE:
        double metersPerTick = KineticBlockEntity.convertToLinear(speed);
        return (int) Math.ceil(target / metersPerTick) + 2;
    case DELAY:  return (int) target;
    case AWAIT:  return -1;
    ...
```
- `convertToAngular(speed) = speed * 360 / 60 / 20`（rpm → 度/tick）；`convertToLinear(speed) = speed / 512`（rpm → 方块/tick）。`content/kinetics/base/KineticBlockEntity.java:512-520`。

**`OnIsPoweredResult` 的用法**——只服务 AWAIT：
```java
// content/kinetics/transmission/sequencer/OnIsPoweredResult.java:1-6
public enum OnIsPoweredResult { NOTHING, CONTINUE }
```
```java
// content/kinetics/transmission/sequencer/Instruction.java:107-109
OnIsPoweredResult onRedstonePulse() {
    return instruction == SequencerInstructions.AWAIT ? OnIsPoweredResult.CONTINUE : OnIsPoweredResult.NOTHING;
}
```
```java
// content/kinetics/transmission/sequencer/SequencedGearshiftBlockEntity.java:153-168
public void risingFlank() {
    Instruction instruction = getInstruction(currentInstruction);
    if (instruction == null) return;
    if (poweredPreviously) return;
    poweredPreviously = true;
    switch (instruction.onRedstonePulse()) {
    case CONTINUE: run(currentInstruction + 1); break;
    default: break;
    }
}
```
即：**AWAIT 是唯一由红石上升沿推进的指令**，其它指令只认 `STATE != 0` 的持续供电状态（`onRedstoneUpdate`，`SequencedGearshiftBlockEntity.java:134-151`）。

**运行状态机**（`SequencedGearshiftBlockEntity.java:170-195`）：
```java
detachKinetics();                                                    // :187 换指令前先断网
currentInstructionDuration = instruction.getDuration(0, getTheoreticalSpeed());
currentInstruction = instructionIndex;
sequenceContext = SequenceContext.fromGearshift(instruction.instruction,
    getTheoreticalSpeed() * getModifier(), instruction.value);       // :191-192
timer = 0;
level.setBlock(worldPosition, getBlockState().setValue(SequencedGearshiftBlock.STATE, instructionIndex + 1), Block.UPDATE_ALL);
```
注意末尾**没有再 `attachKinetics()`**：`setBlock` 会让 `updateIndirectNeighbourShapes` 把 `updateSpeed` 置真，从而下一 tick 由 `tick()` 自动 `attachKinetics()`（`KineticBlock.java:62-78`；`KineticBlockEntity.java:98-100`）。

`END` 时若倍率非 0 先 `detachKinetics()`，再清 `sequenceContext`（`SequencedGearshiftBlockEntity.java:172-185`）。`SequenceContext` 会随源一起下发，让下游机器知道「还要转多少度」，这由 `copySequenceContextFrom` 完成（基类 `KineticBlockEntity.java:322-324`），而序列箱自己**故意不复制**（`SequencedGearshiftBlockEntity.java:202-203`：`protected void copySequenceContextFrom(KineticBlockEntity sourceBE) {}`）。

**速度变化时的重算**：`onSpeedChanged` 用当前进度重新计算剩余时长（`SequencedGearshiftBlockEntity.java:111-128`），速度变 0 时直接终止序列（`run(-1)`）。

### 4.6 链传动（Encased Chain Drive / Adjustable Chain Gearshift）

**方块**
- `ChainDriveBlock extends RotatedPillarKineticBlock implements IBE<KineticBlockEntity>, TransformableBlock`。`content/kinetics/chainDrive/ChainDriveBlock.java:35-36`。
- 用 `PART`（START/MIDDLE/END/NONE）+ `CONNECTED_ALONG_FIRST_COORDINATE` 描述「链条走向」，`getConnectionAxis` 把两者合成一根世界轴：
```java
// content/kinetics/chainDrive/ChainDriveBlock.java:190-196
protected static Axis getConnectionAxis(BlockState state) {
    Axis axis = state.getValue(AXIS);
    boolean connectionAlongFirst = state.getValue(CONNECTED_ALONG_FIRST_COORDINATE);
    Axis connectionAxis =
        connectionAlongFirst ? (axis == Axis.X ? Axis.Y : Axis.X) : (axis == Axis.Z ? Axis.Y : Axis.Z);
    return connectionAxis;
}
```
（`AXIS` = 自身的自转轴；`connectionAxis` = 链条延伸方向，两轴互垂。）

**连接判定**
```java
// content/kinetics/chainDrive/ChainDriveBlock.java:173-188
public static boolean areBlocksConnected(BlockState state, BlockState other, Direction facing) {
    Part part = state.getValue(PART);
    Axis connectionAxis = getConnectionAxis(state);
    Axis otherConnectionAxis = getConnectionAxis(other);

    if (otherConnectionAxis != connectionAxis) return false;
    if (facing.getAxis() != connectionAxis) return false;
    if (facing.getAxisDirection() == AxisDirection.POSITIVE && (part == Part.MIDDLE || part == Part.START))
        return true;
    if (facing.getAxisDirection() == AxisDirection.NEGATIVE && (part == Part.MIDDLE || part == Part.END))
        return true;

    return false;
}
```
即「沿链条轴、且我这一端不是封口端」才算连。

**倍率（两端齿轮组 / 可调链轮箱）**
```java
// content/kinetics/chainDrive/ChainDriveBlock.java:198-206
public static float getRotationSpeedModifier(KineticBlockEntity from, KineticBlockEntity to) {
    float fromMod = 1;
    float toMod = 1;
    if (from instanceof ChainGearshiftBlockEntity)
        fromMod = ((ChainGearshiftBlockEntity) from).getModifier();
    if (to instanceof ChainGearshiftBlockEntity)
        toMod = ((ChainGearshiftBlockEntity) to).getModifier();
    return fromMod / toMod;
}
```
- 普通链条段：`1/1 = 1` → **同向同速、无反转**（链条是「同步带」而不是齿轮啮合）。
- 装了 `Adjustable Chain Gearshift` 后：`mod(from)/mod(to)`，其中
```java
// content/kinetics/chainDrive/ChainGearshiftBlockEntity.java:70-74
protected float getModifierForSignal(int newPower) {
    if (newPower == 0) return 1;
    return 1 + ((newPower + 1) / 16f);        // 1 → 1.0625 … 15 → 2.0
}
```
- 所以单端可调时倍率是 `≈1~2`，一端 1 一端 2 → `0.5` 或 `2`。这与齿轮的 `-2/-0.5` **符号相反**（链条不反转），是理解 Create 传动语义的关键差异。

**信号变化的处理**（可直接照抄的最小重算范式）：
```java
// content/kinetics/chainDrive/ChainGearshiftBlockEntity.java:63-68
protected void analogSignalChanged(int newSignal) {
    detachKinetics();
    removeSource();
    signal = newSignal;
    attachKinetics();
}
```
`detachKinetics() → removeSource() → 改值 → attachKinetics()` 四步，全部同一 tick 内完成——适用于「倍率变了但方块状态不变」的场景。

### 4.7 转速控制器（Rotation Speed Controller）

**方块**：`SpeedControllerBlock extends HorizontalAxisKineticBlock`，`HORIZONTAL_AXIS` 与上方大齿轮的轴**交叉**：
```java
// content/kinetics/speedController/SpeedControllerBlock.java:47-56
BlockState above = context.getLevel().getBlockState(context.getClickedPos().above());
if (ICogWheel.isLargeCog(above) && above.getValue(CogWheelBlock.AXIS).isHorizontal())
    return defaultBlockState().setValue(HORIZONTAL_AXIS,
        above.getValue(CogWheelBlock.AXIS) == Axis.X ? Axis.Z : Axis.X);
```
放置大齿轮时的对称特判在 `CogWheelBlock.getAxisForPlacement`：`content/kinetics/simpleRelays/CogWheelBlock.java:156-157`。

**BE：带 UI 的可调目标速度**
```java
// content/kinetics/speedController/SpeedControllerBlockEntity.java:60-73
targetSpeed = new KineticScrollValueBehaviour(CreateLang.translateDirect("kinetics.speed_controller.rotation_speed"),
    this, new ControllerValueBoxTransform());
targetSpeed.between(-max, max);
targetSpeed.value = DEFAULT_SPEED;                       // 16
targetSpeed.withCallback(i -> this.updateTargetRotation());
behaviours.add(targetSpeed);
```
- `DEFAULT_SPEED = 16`，范围 `[-maxRotationSpeed, +maxRotationSpeed]`。`SpeedControllerBlockEntity.java:32, 62-66`。

**如何用自身 BE 覆盖网络速度**（三重机制叠加，缺一不可）：

1. **它是源**：`getDesiredOutputSpeed` 让控制器在「无 source 且被问及自身输出」时输出 `targetSpeed`。
```java
// content/kinetics/speedController/SpeedControllerBlockEntity.java:104-132
public static float getDesiredOutputSpeed(KineticBlockEntity cogWheel, KineticBlockEntity speedControllerIn,
    boolean targetingController) {
    SpeedControllerBlockEntity speedController = (SpeedControllerBlockEntity) speedControllerIn;
    float targetSpeed = speedController.targetSpeed.getValue();
    float speed = speedControllerIn.getTheoreticalSpeed();
    float wheelSpeed = cogWheel.getTheoreticalSpeed();

    if (targetSpeed == 0) return 0;
    if (targetingController && wheelSpeed == 0) return 0;
    if (!speedController.hasSource()) {
        if (targetingController) return targetSpeed;   // 控制器自己就是源
        return 0;
    }

    boolean wheelPowersController = speedController.source.equals(cogWheel.getBlockPos());

    if (wheelPowersController) {
        if (targetingController) return targetSpeed;   // 大齿轮驱动控制器 → 控制器输出目标值
        return wheelSpeed;                             // 反向：齿轮给出它的实际速度
    }

    if (targetingController) return speed;
    return targetSpeed;
}
```
2. **合成规则是「取绝对值更大的那个」**（同号取 max，异号直接取目标值），保证控制器不会被外部网络「拉慢」：
```java
// content/kinetics/speedController/SpeedControllerBlockEntity.java:86-102
float compareSpeed = targetingController ? speed : wheelSpeed;
if (desiredOutputSpeed >= 0 && compareSpeed >= 0) return Math.max(desiredOutputSpeed, compareSpeed);
if (desiredOutputSpeed < 0 && compareSpeed < 0) return Math.min(desiredOutputSpeed, compareSpeed);
return desiredOutputSpeed;
```
3. **滑块变化时整网重算**：
```java
// content/kinetics/speedController/SpeedControllerBlockEntity.java:75-84
private void updateTargetRotation() {
    if (hasNetwork()) getOrCreateNetwork().remove(this);
    RotationPropagator.handleRemoved(level, worldPosition, this);
    removeSource();
    attachKinetics();
    if (isCogwheelPresent() && getSpeed() != 0) award(AllAdvancements.SPEED_CONTROLLER);
}
```
注意这里比 `ChainGearshift` 多了两步：`getOrCreateNetwork().remove(this)`（从应力域摘掉，避免旧容量残留）+ `removeSource()`（清自己的源身份）。

**控制器 → 齿轮这一方向的特殊路径**在 `RotationPropagator.getConveyedSpeed` 里被特判（见 3.9），所以**它既不是 `propagateRotationTo` 也不是 `getAxisModifier`**，而是第三条独立通道。它也因此在 `isConnected` 里被单独 OR 上：
```java
// content/kinetics/RotationPropagator.java:405-407
return isLargeCogToSpeedController(stateFrom, stateTo, to.getBlockPos().subtract(from.getBlockPos()))
    || getRotationSpeedModifier(from, to) != 0
    || from.isCustomConnection(to, stateFrom, stateTo);
```

### 4.8 传送带（Belt）

**它如何从两端轴拿到转速**——答案是「什么都不做」，全靠标准同轴规则 + 一个自定义段间绑定：

1. **滑轮（pulley）= START/END 段**才有轴端：
```java
// content/kinetics/belt/BeltBlock.java:118-133
@Override
public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
    if (face.getAxis() != getRotationAxis(state)) return false;
    return getBlockEntityOptional(world, pos).map(BeltBlockEntity::hasPulley).orElse(false);
}

@Override
public Axis getRotationAxis(BlockState state) {
    if (state.getValue(SLOPE) == BeltSlope.SIDEWAYS) return Axis.Y;
    return state.getValue(HORIZONTAL_FACING).getClockWise().getAxis();
}
```
```java
// content/kinetics/belt/BeltBlockEntity.java:326-330
public boolean hasPulley() {
    if (!AllBlocks.BELT.has(getBlockState())) return false;
    return getBlockState().getValue(BeltBlock.PART) != MIDDLE;
}
```
→ **旋转轴 = 皮带行进方向顺时针 90° 的水平轴**（皮带朝东 → 绕 Z 转），也就是滚筒轴。所以插在皮带端面侧面的普通轴用标准 `connectedByAxis` 就接上了，倍率 `±1`。**皮带没有 `getRotationSpeedModifier` 覆写，也没有 `BeltHelper` 参与倍率计算**。

2. **同一 controller 的各段之间用 `propagateRotationTo` 绑定**：
```java
// content/kinetics/belt/BeltBlockEntity.java:552-564
@Override
protected boolean canPropagateDiagonally(IRotate block, BlockState state) {
    return state.hasProperty(BeltBlock.SLOPE) && (state.getValue(BeltBlock.SLOPE) == BeltSlope.UPWARD
        || state.getValue(BeltBlock.SLOPE) == BeltSlope.DOWNWARD);
}

@Override
public float propagateRotationTo(KineticBlockEntity target, BlockState stateFrom, BlockState stateTo, BlockPos diff,
                                 boolean connectedViaAxes, boolean connectedViaCogs) {
    if (target instanceof BeltBlockEntity && !connectedViaAxes)
        return getController().equals(((BeltBlockEntity) target).getController()) ? 1 : 0;
    return 0;
}
```
- `canPropagateDiagonally` 被覆写为「上/下坡时为真」，让斜坡段的下一段坐标进入候选邻域（斜坡相邻段的 `diff` 是 `(±1, ±1, 0)`，`distSqr == 2`）。
- `getController().equals(...)` 保证**两条首尾相接但独立的皮带不会被传动绑定**（这是「同速但不同物品流」的正确语义）。
- `!connectedViaAxes` 条件避免与轴对接冲突（`BeltBlock.java:113-116` 里 `areStatesKineticallyEquivalent` 还额外比较 `PART`）。

3. **线性速度换算**：
```java
// content/kinetics/belt/BeltBlockEntity.java:314-324
public float getBeltMovementSpeed() { return getSpeed() / 480f; }

public float getDirectionAwareBeltMovementSpeed() {
    int offset = getBeltFacing().getAxisDirection().getStep();
    if (getBeltFacing().getAxis() == Axis.X) offset *= -1;
    return getBeltMovementSpeed() * offset;
}
```
- 480 rpm·tick/方块 → 与 `convertToLinear(speed) = speed/512`（`KineticBlockEntity.java:512-514`）不同，皮带用了自己的 480，属于物流侧常量。

**为什么既是传动件又是物流件**
- 传动侧：`KineticBlockEntity` 子类 + `hasShaftTowards`/`getRotationAxis` + `propagateRotationTo`，参与网络、应力、过载。
- 物流侧：`BeltInventory`（`getInventory()`，`BeltBlockEntity.java:392-399`）、`passengers` 实体运输（`BeltBlockEntity.java:59`）、`canInsertFrom`/`tryInsertingFromSide`（`BeltBlockEntity.java:449-542`）、控制器/段链（`isController()`、`getControllerBE()`、`BeltHelper.getSegmentBE/getControllerBE`，`content/kinetics/belt/BeltHelper.java:40-57`）。
- 段链遍历：
```java
// content/kinetics/belt/BeltBlock.java:556-591
public static List<BlockPos> getBeltChain(LevelAccessor world, BlockPos controllerPos) { ... }
public static BlockPos nextSegmentPosition(BlockState state, BlockPos pos, boolean forward) { ... }
```
- 因此「带是传动件也是物流件」= **同一个 BE 同时实现「转速来源」与「物品容量/位置插值」两套职责**，且皮带的速度方向语义（`getMovementFacing()`，`BeltBlockEntity.java:383-386`）由 `getSpeed()` 符号 + 方块朝向共同决定，不是简单的 `speed > 0`。

---

## 5. 渲染挂钩（简要）

### 5.1 两条并行路径

| 路径 | 触发条件 | 入口 | 代码 |
|---|---|---|---|
| Flywheel 视觉（主路径） | `VisualizationManager.supportsVisualization(level)` | `SimpleBlockEntityVisualizer` + `RotatingInstance` | `content/kinetics/base/KineticBlockEntityRenderer.java:50`；`foundation/data/CreateBlockEntityBuilder.java:105-113` |
| 原版 BER（回退） | 不支持视觉化时 | `KineticBlockEntityRenderer.renderSafe` | `content/kinetics/base/KineticBlockEntityRenderer.java:47-55` |

`KineticBlockEntityRenderer.renderSafe` 第一行就短路：
```java
// content/kinetics/base/KineticBlockEntityRenderer.java:50
if (VisualizationManager.supportsVisualization(be.getLevel())) return;
```

### 5.2 `RotatingInstance`：视觉侧的「速度 × 6」

```java
// content/kinetics/base/RotatingInstance.java:15, 63-69
public static final float SPEED_MULTIPLIER = 6;
...
public RotatingInstance setup(KineticBlockEntity blockEntity, Axis axis, float speed) {
    var blockState = blockEntity.getBlockState();
    var pos = blockEntity.getBlockPos();
    return setRotationAxis(axis)
        .setRotationalSpeed(speed * RotatingInstance.SPEED_MULTIPLIER)
        .setRotationOffset(KineticBlockEntityVisual.rotationOffset(blockState, axis, pos) + blockEntity.getRotationAngleOffset(axis));
}
```
- `rotationOffset` 会给「坐标和偶数」的方块加 22.5°，大齿轮再加 11.25° 基准，用于齿轮齿相位错开：
```java
// content/kinetics/base/KineticBlockEntityVisual.java:23-37
public static float rotationOffset(BlockState state, Axis axis, Vec3i pos) {
    if (shouldOffset(axis, pos)) return 22.5f;
    else return ICogWheel.isLargeCog(state) ? 11.25f : 0;
}
public static boolean shouldOffset(Axis axis, Vec3i pos) {
    int x = (axis == Axis.X) ? 0 : pos.getX();
    int y = (axis == Axis.Y) ? 0 : pos.getY();
    int z = (axis == Axis.Z) ? 0 : pos.getZ();
    return ((x + y + z) % 2) == 0;
}
```
- 过载染色（红/绿闪）：
```java
// content/kinetics/base/KineticBlockEntityVisual.java:43-59
Color color = overstressed ? Color.RED : Color.SPRING_GREEN;
instance.setColor(Color.WHITE.mixWith(color, weight));
```
- 网络色（调试用 `network` 哈希着色）：
```java
// content/kinetics/base/RotatingInstance.java:41-45
public static int colorFromBE(KineticBlockEntity be) {
    if (be.hasNetwork()) return Color.generateFromLong(be.network).getRGB();
    return 0xFFFFFF;
}
```

### 5.3 现成的 Visual 工厂（照抄即可）

| 工厂 | 用途 | 代码 |
|---|---|---|
| `SingleAxisRotatingVisual::shaft` | 绕 `getRotationAxis` 转的单个 partial model（轴/包壳轴/链轮箱） | `content/kinetics/base/SingleAxisRotatingVisual.java:57-59` |
| `SingleAxisRotatingVisual.of(partial)` | 自定义 partial model，源朝向 Y | 同上 `:42-46` |
| `SingleAxisRotatingVisual.ofZ(partial)` | partial model 源朝向 Z | 同上 `:51-55` |
| `OrientedRotatingVisual.of(partial)` / `.backHorizontal(partial)` | 绕 `FACING`/`HORIZONTAL_FACING` 朝向的轴 | `content/kinetics/base/OrientedRotatingVisual.java:43-58` |
| `SplitShaftVisual` | **两侧分别用 `getRotationSpeedModifier(dir)` 算速度**的两半轴 | `content/kinetics/transmission/SplitShaftVisual.java:25-60` |
| `GearboxVisual` | 4 侧面不同速度 | `content/kinetics/gearbox/GearboxVisual.java:27-63` |
| `ShaftVisual` | `SingleAxisRotatingVisual` + SHAFT partial model 的薄封装 | `content/kinetics/base/ShaftVisual.java:8-12` |

`SplitShaftVisual` 的更新逻辑是「客户端重算倍率」的范例（注意它直接调 BE 的方法，所以 BE 的状态必须同步到客户端）：
```java
// content/kinetics/transmission/SplitShaftVisual.java:49-60
@Override
public void update(float pt) {
    Block block = blockState.getBlock();
    final Direction.Axis boxAxis = ((IRotate) block).getRotationAxis(blockState);
    Direction[] directions = Iterate.directionsInAxis(boxAxis);
    for (int i : Iterate.zeroAndOne) {
        keys.get(i)
            .setup(blockEntity, blockEntity.getSpeed() * blockEntity.getRotationSpeedModifier(directions[i]))
            .setChanged();
    }
}
```

### 5.4 BER 侧的相位公式

```java
// content/kinetics/base/KineticBlockEntityRenderer.java:87-92
public static float getAngleForBe(KineticBlockEntity be, final BlockPos pos, Axis axis) {
    float time = AnimationTickHolder.getRenderTime(be.getLevel());
    float offset = getRotationOffsetForPosition(be, pos, axis);
    float angle = ((time * be.getSpeed() * 3f / 10 + offset) % 360) / 180 * (float) Math.PI;
    return angle;
}
```
- `getRotationOffsetForPosition = rotationOffset(...) + be.getRotationAngleOffset(axis)`。`KineticBlockEntityRenderer.java:126-128`。
- 轴渲染只换 blockstate 再套通用旋转：
```java
// content/kinetics/base/ShaftRenderer.java:12-15
@Override
protected BlockState getRenderedBlockState(KineticBlockEntity be) {
    return shaft(getRotationAxisOf(be));
}
```

### 5.5 注册位置（关键：Create 不用集中式 `AllVisuals`）

6.0.10 的视觉注册**内联在 BE 注册里**，通过 Registrate 扩展点 `.visual(...)`：
```java
// foundation/data/CreateBlockEntityBuilder.java:87-113
public CreateBlockEntityBuilder<T, P> visual(NonNullSupplier<SimpleBlockEntityVisualizer.Factory<T>> visualFactory,
                                             Predicate<@NotNull T> renderNormally) {
    if (this.visualFactory == null) {
        CatnipServices.PLATFORM.executeOnClientOnly(() -> this::registerVisualizer);
    }
    this.visualFactory = visualFactory;
    this.renderNormally = renderNormally;
    return this;
}

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
现成范例（传动件全部在这里）：
```java
// AllBlockEntityTypes.java:254-259
public static final BlockEntityEntry<BracketedKineticBlockEntity> BRACKETED_KINETIC = REGISTRATE
    .blockEntity("simple_kinetic", BracketedKineticBlockEntity::new)
    .visual(() -> BracketedKineticBlockEntityVisual::create, false)
    .validBlocks(AllBlocks.SHAFT, AllBlocks.COGWHEEL, AllBlocks.LARGE_COGWHEEL)
    .renderer(() -> BracketedKineticBlockEntityRenderer::new)
    .register();
```
```java
// AllBlockEntityTypes.java:268-281
public static final BlockEntityEntry<GearboxBlockEntity> GEARBOX = REGISTRATE
    .blockEntity("gearbox", GearboxBlockEntity::new)
    .visual(() -> GearboxVisual::new, false)
    .validBlocks(AllBlocks.GEARBOX)
    .renderer(() -> GearboxRenderer::new)
    .register();

public static final BlockEntityEntry<KineticBlockEntity> ENCASED_SHAFT = REGISTRATE
    .blockEntity("encased_shaft", KineticBlockEntity::new)
    .visual(() -> SingleAxisRotatingVisual::shaft, false)
    .validBlocks(AllBlocks.ANDESITE_ENCASED_SHAFT, AllBlocks.BRASS_ENCASED_SHAFT, AllBlocks.ENCASED_CHAIN_DRIVE,
        AllBlocks.METAL_GIRDER_ENCASED_SHAFT)
    .renderer(() -> ShaftRenderer::new)
    .register();
```
```java
// AllBlockEntityTypes.java:317-329
public static final BlockEntityEntry<ClutchBlockEntity> CLUTCH = REGISTRATE
    .blockEntity("clutch", ClutchBlockEntity::new)
    .visual(() -> SplitShaftVisual::new, false)
    .validBlocks(AllBlocks.CLUTCH)
    .renderer(() -> SplitShaftRenderer::new)
    .register();

public static final BlockEntityEntry<GearshiftBlockEntity> GEARSHIFT = REGISTRATE
    .blockEntity("gearshift", GearshiftBlockEntity::new)
    .visual(() -> SplitShaftVisual::new, false)
    .validBlocks(AllBlocks.GEARSHIFT)
    .renderer(() -> SplitShaftRenderer::new)
    .register();
```
- `.visual(factory, false)` 的第二参数 = `renderNormally`；`false` 意味着 `skipVanillaRender(true)`，即**方块自身的 baked model 不被常规渲染**（由视觉/partial model 全权负责）。`CreateBlockEntityBuilder.java:105-113`。
- 皮带是唯一传 predicate 的：`.visual(() -> BeltVisual::new, BeltBlockEntity::shouldRenderNormally)`（`AllBlockEntityTypes.java:449-452`；`BeltBlockEntity.java:571-576`）。

⚠ **待验证**：本仓库未包含 `BracketedKineticBlockEntityVisual`、`EncasedCogVisual` 等客户端类全文（它们在同一源码树的 `content/kinetics/simpleRelays/` 与 client 包下存在引用），若需要抄写「齿轮+支架」组合视觉，请另开文件精读。

---

## 6. 新增一种传动元件：可编译模板

以下模板只使用本文件已核实存在的 API，不引入任何未验证符号。

### 6.1 选基类决策

| 你的元件形态 | 基类 | 必做 |
|---|---|---|
| 一根轴/滚筒（单一自转轴，两面出轴） | `AbstractShaftBlock` 或 `AbstractEncasedShaftBlock` | 无需再写 IRotate（父类已实现） |
| 有明确朝向、仅一面出轴 | `DirectionalKineticBlock` | `hasShaftTowards` + `getRotationAxis` |
| 水平朝向的机器 | `HorizontalKineticBlock` | 同上 |
| 一根轴向 + 状态里带额外轴属性 | `HorizontalAxisKineticBlock` / `RotatedPillarKineticBlock` | 通常父类已够 |
| 一个面接输入、其余面按规则输出（换向/离合类） | `AbstractEncasedShaftBlock`（+ 自定义 BE 继承 `SplitShaftBlockEntity`） | `getRotationSpeedModifier(Direction)` |
| 需要按 FACING 自动推垂直轴 | `DirectionalAxisKineticBlock` | 父类已实现 `getRotationAxis`/`hasShaftTowards` |

### 6.2 方块类（例：带固定倍率的「行星传动箱」）

```java
package com.example.northstar.content.kinetics.planetary;

import com.example.northstar.AllBlockEntityTypes;
import com.simibubi.create.content.kinetics.base.AbstractEncasedShaftBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class PlanetaryGearboxBlock extends AbstractEncasedShaftBlock implements IBE<PlanetaryGearboxBlockEntity> {

    public PlanetaryGearboxBlock(Properties properties) { super(properties); }

    // 继承自 AbstractEncasedShaftBlock：hasShaftTowards = face.getAxis() == AXIS，getRotationAxis = AXIS
    // 若想只在一侧出轴（例如只允许 -AXIS 方向进、+AXIS 方向出），覆写：
    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(AXIS);   // 若需方向限制，改为 face == Direction.get(POSITIVE, AXIS) 等
    }

    // 状态里含倍率选择时必须放弃等价优化（否则改倍率不重算）
    @Override
    protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
        return super.areStatesKineticallyEquivalent(oldState, newState)
            && oldState.getValue(PlanetaryGearboxBlock.RATIO) == newState.getValue(PlanetaryGearboxBlock.RATIO);
    }

    @Override
    public Class<PlanetaryGearboxBlockEntity> getBlockEntityClass() { return PlanetaryGearboxBlockEntity.class; }

    @Override
    public BlockEntityType<? extends PlanetaryGearboxBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.PLANETARY_GEARBOX.get();
    }
}
```
- `areStatesKineticallyEquivalent` 的语义见 `content/kinetics/base/KineticBlock.java:55-59`；若返回 true，`onPlace` 会设 `preventSpeedUpdate = 2`（`:24-43`），即**跳过重算**——这是「改倍率后不生效」的最常见原因。

### 6.3 BE 类（三种倍率写法，按需选一）

**写法 A：对称倍率（推荐，最安全）——实现 `propagateRotationTo` 并保证互反**

```java
public class PlanetaryGearboxBlockEntity extends KineticBlockEntity {

    public PlanetaryGearboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private float ratio() {                       // 例：输出端 2 倍
        return 2f;
    }

    @Override
    public float propagateRotationTo(KineticBlockEntity target, BlockState stateFrom, BlockState stateTo,
                                     BlockPos diff, boolean connectedViaAxes, boolean connectedViaCogs) {
        if (!connectedViaAxes)
            return 0;                             // 只接管与我同轴对接的邻居，其余交回内置规则
        Direction dir = Direction.getNearest(diff.getX(), diff.getY(), diff.getZ());
        boolean myOutput = /* 依据 dir 判断这是输出侧 */ true;
        float r = ratio();
        // 我 → 对方：输出侧给 r，输入侧给 1/r（保证 r * (1/r) = 1）
        return myOutput ? r : 1f / r;
    }
}
```
要点：
- `connectedViaAxes` 已在 `RotationPropagator.java:64-66` 判定完毕，直接复用可以省掉全部几何代码。
- **互反必须自己保证**（3.2 节）。`r * (1/r) = 1` 是对称传动的最小要求；若 `r` 可正可负（含反转），符号也要跟着互反（`-r` 与 `-1/r`）。
- 返回 `0` 表示「这一对连接不归我管」，不是「断开」。要表达断开请返回一个极小值？**不行** —— 断开语义应通过 `isCustomConnection` 不成立 + 内置规则为 0 来实现；`propagateRotationTo` 返回 0 会退回内置规则。

**写法 B：按面给倍率（换向/离合类）——`SplitShaftBlockEntity`**

```java
public class PlanetaryShiftBlockEntity extends SplitShaftBlockEntity {

    public PlanetaryShiftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public float getRotationSpeedModifier(Direction face) {
        if (isVirtual()) return 1;                                   // 参考 SequencedGearshiftBlockEntity:235
        if (!hasSource() || face == getSourceFacing())
            return 1;                                                // 源侧永不反转/断开
        return getBlockState().getValue(Properties.POWERED) ? -1f : 1f;
    }
}
```
- `hasSource()` 必须先判，否则 `getSourceFacing()` 会 NPE（`DirectionalShaftHalvesBlockEntity.java:14-17` 直接 `source.subtract(...)`）。
- 返回 `0` = 该面断开（`1/0` 被保护，见 `RotationPropagator.java:77-79`）；返回 `-1` = 该面反转。

**写法 C：非对称倍率（真正的 2:1 减速箱）**

模仿大-小齿轮的做法：**在两侧各写死一个倒数倍率**，而不是靠自动取倒数。
```java
@Override
public float propagateRotationTo(KineticBlockEntity target, BlockState stateFrom, BlockState stateTo,
                                 BlockPos diff, boolean connectedViaAxes, boolean connectedViaCogs) {
    if (!connectedViaAxes) return 0;
    Direction dir = Direction.getNearest(diff.getX(), diff.getY(), diff.getZ());
    if (isInputSide(dir))  return -0.5f;   // 输入侧看出去：转速减半且反向
    if (isOutputSide(dir)) return -2f;     // 输出侧看出去：转速加倍且反向
    return 0;
}
```
与 `RotationPropagator.java:100-105` 的大-小齿轮 `-2f / -.5f` 完全一致的模式。

### 6.4 注册与渲染最小集

```java
// 1) BE 注册（Registrate 风格，照抄 AllBlockEntityTypes.java:268-273）
public static final BlockEntityEntry<PlanetaryGearboxBlockEntity> PLANETARY_GEARBOX = REGISTRATE
    .blockEntity("planetary_gearbox", PlanetaryGearboxBlockEntity::new)
    .visual(() -> SingleAxisRotatingVisual::shaft, false)      // 若模型就是轴；自定义 partial model 用 .of(...)
    .validBlocks(AllBlocks.MY_PLANETARY_GEARBOX)
    .renderer(() -> ShaftRenderer::new)                       // 回退 BER
    .register();
```
```java
// 2) 应力注册（必须，否则应力恒为 0，永远不过载）
BlockStressValues.IMPACTS.register(AllBlocks.MY_PLANETARY_GEARBOX.get(), () -> 4.0);   // 1 rpm 基准
// 3) 转速等级（可选，用于 goggles 与最低速度门控）
//    getMinimumRequiredSpeedLevel() 返回 SpeedLevel.MEDIUM 等
```
```java
// 4) 方块 state / 模型要点
//    - 若用 RotatedPillarKineticBlock 家族：blockstate 必须为 axis=x/y/z 三向，模型三向旋转版本
//    - 若视觉由 Flywheel 接管（.visual(factory, false)），方块模型可以只留「外壳」或完全省略旋转部件
//    - .visual(factory, true) 时 skipVanillaRender(false) → 方块模型照常渲染，视觉只叠加
```

**渲染选型速查**
| 你要的效果 | 用 |
|---|---|
| 整个方块绕自身轴转 | `SingleAxisRotatingVisual.of(partial)` |
| 每面转速不同 | 抄 `SplitShaftVisual`（`content/kinetics/transmission/SplitShaftVisual.java`） |
| 两个半轴 + 中间静止外壳 | 抄 `GearboxVisual`（`content/kinetics/gearbox/GearboxVisual.java`） |
| 只在某个朝向出轴并旋转 | `OrientedRotatingVisual.of(partial)` |

### 6.5 常见坑（全部有代码依据）

1. **连接判定必须双向一致。**
   `connectedByAxis` 是 `A.hasShaftTowards(dir) && B.hasShaftTowards(dir.getOpposite())`（`RotationPropagator.java:64-66`），单侧为 false 就断。而 `isCustomConnection` 只需单侧（`findConnectedNeighbour` 会反向再试一次，`RotationPropagator.java:397`）。**不要混用两种语义**：一边用 `hasShaftTowards` 一边用 `isCustomConnection` 会导致「A 认为连、B 认为不连」，在 `propagateNewSource` 里表现为速度单向压制，反复抖 → flicker。

2. **`hasShaftTowards` 必须与视觉轴一致。**
   渲染角速度按 `getRotationAxis` 算（`KineticBlockEntityRenderer.java:94-100`），视觉按 `RotatingInstance.setRotationAxis`（`RotatingInstance.java:63-69`）。若 `hasShaftTowards` 允许的面与 `getRotationAxis` 不对应（例如斜向出轴但轴还是 Y），会出现「物理连上了、视觉绕错轴」。

3. **倍率符号写反 → 整网反转 → 方块被摧毁。**
   `incompatible` 判定是纯符号比较（`RotationPropagator.java:232-233`），一旦同一条链上下游符号冲突就 `destroyBlock`（`:246-248`）。**先在没有源网络的环境里测符号**（手动放置曲柄），再串联。

4. **自定义连接不能成环（尤其不能成「加速环」）。**
   同 network（`currentTE.network.equals(neighbourTE.network)`）内的推力若比邻块现有速度大超过 `eps = |v_neighbour|/256/256`，直接摧毁（`RotationPropagator.java:269-273`）。因此「链条自己连回自己」在设计上只能保持严格等速。

5. **改变方块状态 / 倍率后必须重算网络。** 四种可用手段：
   | 场景 | 做法 | 参考 |
   |---|---|---|
   | blockstate 变了且影响连接 | 什么都不做，让 `updateIndirectNeighbourShapes` 清 `updateSpeed` + 下一 tick `attachKinetics` | `KineticBlock.java:62-78`；`KineticBlockEntity.java:98-100` |
   | blockstate 变了但要立刻生效 | `detachKinetics(world,pos,true)` + `scheduleTick(pos,this,1,EXTREMELY_HIGH)` + 在 `tick` 里 `handleAdded` | `GearshiftBlock.java:44-84` |
   | 状态不变只改倍率（模拟信号） | `detachKinetics(); removeSource(); 改值; attachKinetics();` | `ChainGearshiftBlockEntity.java:63-68` |
   | 自己是源且目标速度变了 | `getOrCreateNetwork().remove(this); handleRemoved(...); removeSource(); attachKinetics();` | `SpeedControllerBlockEntity.java:75-84` |
   | 发电机类 | 置 `reActivateSource = true`（下一 tick `updateGeneratedRotation`） | `GeneratingKineticBlockEntity.java:20, 47-54`；`KineticBlockEntity.java:406-408` |

6. **`maxRotationSpeed` 与 flicker 会直接摧毁方块。**
   - 超速：`|speed| > CKinetics.maxRotationSpeed`（默认 256，最小可配 64）→ `destroyBlock`。`RotationPropagator.java:235-243`；`infrastructure/config/CKinetics.java:14`。
   - flicker：`flickerTally > 128` → `destroyBlock`。阈值 `MAX_FLICKER_SCORE = 128`（`RotationPropagator.java:33`）；`onSpeedChanged` 在「过零或换向」时 `flickerTally += 5`（`KineticBlockEntity.java:191-197`）；每 tick `-1`（`KineticBlockEntity.java:117-118`）。
   - 实践含义：**任何会让下游每 tick 换向的实现都会在 ~26 tick 后炸方块**（5/tick 累积、1/tick 衰减，净 +4/tick）。`-2f` 这类高倍率元件在低速输入端会翻倍，务必检查 `input_rpm * 2 <= maxRotationSpeed`。

7. **`getSourceFacing()` 在无源时 NPE。** `DirectionalShaftHalvesBlockEntity.java:14-17` 没有判空。所有 `SplitShaftBlockEntity` 子类的 `getRotationSpeedModifier` 都先 `if (hasSource())`（`ClutchBlockEntity.java:17`、`GearshiftBlockEntity.java:17`、`SequencedGearshiftBlockEntity.java:237` 的 `!hasSource() ||`）。

8. **`preventSpeedUpdate` 每 tick 重置。** `KineticBlockEntity.tick()` 里 `preventSpeedUpdate = 0;`（`KineticBlockEntity.java:105`）；`KineticBlock.onPlace` 只在「同 block 且同轴」时设为 2（`KineticBlock.java:34-42`）。所以不要指望它长期抑制重算。

9. **`isConnected` 是唯一公开连接查询，别试图调 private 的 `getRotationSpeedModifier`。**
   若确实需要「我与邻居的倍率」用于 UI/调试，只能自己复制判定逻辑，或用 `isConnected` + 自己算。

10. **`propagateRotationTo` 的 `connectedViaCogs` 不含距离校验。**
    它只是「双方都是小齿轮」（`RotationPropagator.java:68-69`）。你覆写时必须自己判 `diff`，否则会把 3 格外的小齿轮也当成啮合——但注意传播器只会来查「邻域列表里存在的坐标」，所以第一道闸门其实是 `addPropagationLocations`。

11. **`handleRemoved` 对零速块直接 return。**
    `if (removedBE.getTheoreticalSpeed() == 0) return;`（`RotationPropagator.java:314-315`）。若你的元件在断电时已把自己速度清零再移除，下游不会重算，需要自己补 `handleAdded`。

12. **`getRotationAxis` 变化 = 完整重算；不变化 = 不重算。**
    默认 `areStatesKineticallyEquivalent` 只比 block + axis（`KineticBlock.java:55-59`）。所以「倍率属性」必须显式加入等价判定，或干脆返回 false（序列齿轮箱的做法，`SequencedGearshiftBlock.java:79-81`）。

---

## 7. Create 6.0.10 传动件一览表

| 方块名（注册名） | 主类 | 倍率规则 | 可否断开 | 备注 |
|---|---|---|---|---|
| `create:shaft` | `ShaftBlock`（`AbstractSimpleShaftBlock` → `AbstractShaftBlock`） | 同轴 `+1` | 否 | BE=`BRACKETED_KINETIC`；可被 `PoweredShaftBlock` 替换；可包壳 |
| `create:andesite_casing` 系 `*_encased_shaft` | `EncasedShaftBlock`（`AbstractEncasedShaftBlock`） | 同轴 `+1` | 否 | 不可弱充能，活塞 NORMAL（`AbstractEncasedShaftBlock.java:27-35`） |
| `create:metal_girder_encased_shaft` | `GirderEncasedShaftBlock` | 同轴 `+1` | 否 | 与 shaft 扳手互换（`ShaftBlock.java:83-93`） |
| `create:cogwheel` | `CogWheelBlock.small` | 小-小 `-1`；大→小 `-2`；小→大 `-0.5` | 否 | `isDedicatedCogWheel()==true`；斜对角仅垂直平面 4 位 |
| `create:large_cogwheel` | `CogWheelBlock.large` | 大-大 `±1`；大→小 `-2`；小→大 `-0.5` | 否 | BE 改由 `SimpleKineticBlockEntity` 加 12 个对角位 |
| `create:gearbox` | `GearboxBlock` + `GearboxBlockEntity` | 同轴面：同向 `1` / 对面 `-1`；异轴面：`AxisDirection` 相同 `-1`，不同 `1` | 否 | `hasShaftTowards` 用 `!=` AXIS；固定 AXIS=Y 放置，由 `VerticalGearboxItem` 落地改写 |
| `create:clutch` | `ClutchBlock` + `ClutchBlockEntity` | 未通电 `1`；通电且非源侧 `0` | **是** | `ClutchBlockEntity.java:16-22`；先改状态再 detach |
| `create:gearshift` | `GearshiftBlock` + `GearshiftBlockEntity` | 未通电 `1`；通电且非源侧 `-1` | 否（反转） | `GearshiftBlockEntity.java:16-22`；先 detach 再改状态 |
| `create:sequenced_gearshift` | `SequencedGearshiftBlock` + `...BlockEntity` | 空闲 `0`；运行中 `±1/±2` | **是**（空闲即断开） | 5 条指令；`STATE` 兼模拟输出；主动置 `areStatesKineticallyEquivalent=false` |
| `create:encased_chain_drive` | `ChainDriveBlock` | 段间 `1`；两端有可调链轮时 `mod(from)/mod(to)` | 否 | 无反转；`areBlocksConnected` 依 `PART`+`connectionAxis` |
| `create:adjustable_chain_gearshift` | `ChainGearshiftBlock` + `ChainGearshiftBlockEntity` | `getModifier() = 1 + (power+1)/16`（1→2.0） | 否（变速） | 与链条段倍率为**相除**关系 |
| `create:rotation_speed_controller` | `SpeedControllerBlock` + `...BlockEntity` | 不走倍率；`getConveyedSpeed` 特判取 `max/min` 合成（`-max..max`，默认 16） | 目标为 0 时输出 0 | 唯一在 `getConveyedSpeed` 层特判的元件；几何要求齿轮正上方且轴交叉 |
| `create:belt` | `BeltBlock` + `BeltBlockEntity` | 同 controller 段间 `1`；与轴对接 `±1`（标准同轴） | 否 | 同时是物流件；线性速度 `speed/480`；上/下坡允许斜连 |
| `create:creative_motor`（对照） | `CreativeMotorBlock` + BE | 自身 `getGeneratedSpeed()`，作为源 | 可设 0 | `OrientedRotatingVisual.of(SHAFT_HALF)` |
| `create:gantry_shaft`（对照） | `GantryShaftBlock` + `...BlockEntity` | 自定 `propagateRotationTo` 对接 gantry carriage；`isCustomConnection` 覆写 | 否 | 「自定义连接」唯一实际范例（`GantryShaftBlockEntity.java:52-80`） |
| `create:chain_conveyor`（对照） | `ChainConveyorBlock` + `...BlockEntity` | 自定 `connections` 集合内 `1` | 否 | 「远程连接」唯一实际范例（`ChainConveyorBlockEntity.java:658-674`） |

---

## 8. 对「北极星星球附属」的落地建议（选型映射）

| 你想要的新元件 | 建议继承 | 覆写点 | 风险 |
|---|---|---|---|
| 星球专属减速箱（2:1 / 3:1） | `AbstractEncasedShaftBlock` + `KineticBlockEntity` | `propagateRotationTo` 返回 `-r` / `-1/r`（互反） | 高倍率 × 高输入 → 超 `maxRotationSpeed` 被摧毁 |
| 可切换正反转的「应力换向阀」 | `AbstractEncasedShaftBlock` + `SplitShaftBlockEntity` | `getRotationSpeedModifier(face)` 返回 `0/1/-1` | 必须 `hasSource()` 保护；必须 detach→改→attach |
| 需要「读转速并按比例输出」的元件 | 仿 `SpeedControllerBlockEntity` | 在 `getConveyedSpeed` 层特判 **不可行**（private）→ 改为实现 `getGeneratedSpeed()` 的源 + `propagateRotationTo` | 若要做真正的 speed controller，必须挂钩 `RotationPropagator`（私有）→ **不建议** |
| 远距离（跨 3 格以上）传动 | `KineticBlockEntity` + `addPropagationLocations` 维护 `Set<BlockPos>`（仿 chain conveyor） | `addPropagationLocations` 追加坐标 + `propagateRotationTo` 判定集合内 `1`；`write/read` 持久化 + `sendData` | 集合变化后必须 `handleRemoved`+`handleAdded` 全量重算 |
| 只允许单向传动（棘轮/单向离合） | `SplitShaftBlockEntity` | `getRotationSpeedModifier(face)` 依据 `getSpeed()` 符号返回 `0` 或 `1` | ⚠ 会让下游每 tick 断/连 → flicker 累积；建议加滞回或仅在 blockstate 变化时重算（参考 `GearshiftBlock` 的 scheduleTick 模式） |
| 应力放大器 / 应力减半器 | 与传动无关，改 `BlockStressValues.IMPACTS/CAPACITIES` | 注册表 | 应力不在传播链里，是网络级结算（`KineticNetwork.java:133-175`） |

三条给附属的硬性建议：
1. **优先用 `propagateRotationTo`（写法 A/C），少用 `getAxisModifier` 通道**（后者依赖 `DirectionalShaftHalvesBlockEntity` + `hasSource()` 的隐式时序，且 `1/0` 语义容易误读）。
2. **任何「运行时可变倍率」都走 `detachKinetics → 改 → attachKinetics`**，并且保证改完后倍率互反。`ChainGearshiftBlockEntity.java:63-68` 是 4 行金标准。
3. **新元件先做「无源网络下的符号测试」**（手摇曲柄驱动），再串联到已有工厂；否则一次符号错误会在 `propagateNewSource` 里被解释成结构冲突而炸方块（`RotationPropagator.java:246-248`）。

---

## 9. 待验证 / 未覆盖清单

| 项 | 说明 |
|---|---|
| `BracketedKineticBlockEntityVisual` / `EncasedCogVisual` 全文 | 本仓库路径下类存在但未精读；若要做「齿轮+支架+包壳」组合视觉需补读 |
| `KineticEffectHandler` | 旋转指示粒子（`queueRotationIndicators`，`KineticBlock.java:90`）未展开 |
| `SmartBlockEntity.sendData()` / `notifyUpdate()` 的同步粒度 | 只确认了调用点（`RotationPropagator.java:259`、`BeltBlockEntity.java:582`），未读实现 |
| `ChainConveyorBlockEntity.connections` 的写入/维护流程 | 只读了追加与判定（`:658-674`），未读连接建立/拆除的完整生命周期 |
| `PoweredShaftBlock` / 蒸汽引擎输出 | 只读了 `getRotationAngleOffset`（`PoweredShaftBlockEntity.java:128-132`）与容量计算，未完整分析发电机侧 |
| Gantry 传动比 `getGantryPinionModifier` | 引用点已确认（`GantryShaftBlockEntity.java:68-69`），未读实现 |
| 配置项 `kineticValidationFrequency` 的副作用 | 默认 60（`CKinetics.java:17-18`），`validateKinetics` 会 `removeSource()+detachKinetics()` 已死源（`KineticBlockEntity.java:127-153`），新元件若 source 语义不规范会周期性被清 |
