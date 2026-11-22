# Create 6.0.10 应力体系 / 应力源 / 应力消耗装置 源码技术分析

> 目标版本：**Create 6.0.10 / Minecraft 1.21.1 / NeoForge**
> 源码根：`E:\mc\ModCreator\NorthstarPlanetAddon\.research\src\create\com\simibubi\create`
> 行号基准为上述根目录下的相对路径，行号以 `read` 工具显示为准。
> 用途：为「北极星星球附属」新增**应力源**与**应力消耗装置**提供可直接照抄的契约、模板与数值设计依据。

---

## 0. 结论速览（先看这 12 条）

1. **应力只有两个数**：`capacity`（网络可提供 SU）与 `stress`（网络已占用 SU）。两者都由「基值 × |转速|」逐项求和得到。`content/kinetics/KineticNetwork.java:133-175`。
2. **基值按方块查表**：`BlockStressValues.getImpact(block)` / `getCapacity(block)`，返回 `double`。**单位是每 1 RPM 的 SU**。`api/stress/BlockStressValues.java:24-32`。
3. **`getStressConfigKey()` 是可覆盖的查表键**，默认是 `getBlockState().getBlock()`。手摇曲柄用它让"手摇曲柄"与"铜阀门手轮"共用同一份应力配置。`content/kinetics/base/KineticBlockEntity.java:175-189`；`content/kinetics/crank/HandCrankBlockEntity.java:116-120`。
4. **过载不改数据，只改"可用转速"**：`getSpeed()` 在 `overStressed` 或 tick 冻结时返回 0，`getTheoreticalSpeed()` 保留真值。**所有逻辑判断必须用 `getSpeed()`，所有应力计算必须用 `getTheoreticalSpeed()`**。`content/kinetics/base/KineticBlockEntity.java:289-301`。
5. **应力源 = `GeneratingKineticBlockEntity` + 覆盖 `getGeneratedSpeed()`**；速度变化时必须走 `updateGeneratedRotation()`，它会重算容量、推网络、同步客户端。`content/kinetics/base/GeneratingKineticBlockEntity.java:89-116`。
6. **容量是"主动上报"的**：源在 `updateGeneratedRotation()` 里 `notifyStressCapacityChange(calculateAddedStressCapacity())`，网络才会更新 `sources` 表。**动态容量忘了上报 = 容量永远是旧值**。`content/kinetics/base/GeneratingKineticBlockEntity.java:107-112`。
7. **附属模组禁止用 `CStress.setImpact/setCapacity`**：内部 `assertFromCreate(builder)` 对非 `create` 的 modid 直接抛 `IllegalStateException`。正确姿势是直接注册 `BlockStressValues.IMPACTS/CAPACITIES`。`infrastructure/config/CStress.java:89-93`。
8. **注册 `DoubleSupplier` 而不是 `double`**，配置改了立刻生效（`getImpact` 每次调用 `supplier.getAsDouble()`）；而 `registerProvider` 的返回值**会被缓存**，配置热改后需要 `invalidate()`。`api/stress/BlockStressValues.java:24-27`；`api/registry/SimpleRegistry.java:65-71`。
9. **`BlockStressValues.RPM` 只影响 tooltip**，不参与计算；真实 RPM 来自你的 `getGeneratedSpeed()`。`api/stress/BlockStressValues.java:19-22`；`foundation/item/KineticStats.java:113-132`。
10. **蒸汽机不是动力 BE**：`SteamEngineBlockEntity extends SmartBlockEntity`，真正的"源"是它背后的 `PoweredShaftBlockEntity extends GeneratingKineticBlockEntity`，用 `capacityKey` 指回引擎方块取容量。这是"源必须占用轴位"的标准解法。`content/kinetics/steamEngine/SteamEngineBlockEntity.java:40`；`content/kinetics/steamEngine/PoweredShaftBlockEntity.java:23-29, 108-126`。
11. **容量与速度可解耦**：蒸汽机 `getGeneratedSpeed() = movementDirection * 16 * speedModifier`，而 `calculateAddedStressCapacity() = combinedCapacity / speedModifier`——即"慢而有力"或"快而无力"由同一个 modifier 分配。这是设计新源的黄金范式。`content/kinetics/steamEngine/PoweredShaftBlockEntity.java:109-126`。
12. **消耗装置的处理速度不是线性的**：搅拌器用 `Mth.log2(512 / speed)` 映射处理耗时，磨石用 `clamp(|speed|/16, 1, 512)` 作为每 tick 进度。**照抄这套映射，玩家才有"提速"的体感**。`content/kinetics/mixer/MechanicalMixerBlockEntity.java:141-165`；`content/kinetics/millstone/MillstoneBlockEntity.java:206-208`。

---

## 1. 应力模型：公式与生命周期

### 1.1 精确公式

```
stress   = Σ_{成员 be}  impactBase(be) × |be.getTheoreticalSpeed()|   + unloadedStress
capacity = Σ_{源   be}  capacityBase(be) × |be.getGeneratedSpeed()|   + unloadedCapacity

overStressed = capacity < stress  &&  StressImpact.isEnabled()
```

- `impactBase` / `capacityBase` 由 `BlockStressValues` 按方块查得。
- `getStressMultiplierForSpeed(speed) = Math.abs(speed)`，**线性**（不是旧文档说的"每级翻倍"）。`content/kinetics/KineticNetwork.java:165-175`。
- `unloaded*` 是区块卸载时的快照补偿量，`initFromTE` / `addSilently` 维护。

`KineticNetwork` 的两张表：

```java
public Map<KineticBlockEntity, Float> sources;   // 源   → 基值容量
public Map<KineticBlockEntity, Float> members;   // 成员 → 基值消耗
private float currentCapacity, currentStress;
private float unloadedCapacity, unloadedStress;
private int unloadedMembers;
```
`content/kinetics/KineticNetwork.java:13-20`

### 1.2 过载链路

```
网络算出 capacity < stress
  → KineticNetwork.sync() → 每个成员 be.updateFromNetwork(cap, stress, size)
      → overStressed = (cap < stress && StressImpact.isEnabled())
      → 若 overStressed 状态变化：onSpeedChanged(prevSpeed) + sendData()
  → 之后任何 getSpeed() 调用返回 0
      → 消耗装置 tick 里 `if (getSpeed() == 0) return;` 全部停摆
      → 客户端收到 overStressed，KineticEffectHandler 播过载粒子/音效
```

关键代码：

```java
public void updateFromNetwork(float maxStress, float currentStress, int networkSize) {
    this.capacity = maxStress;
    this.stress = currentStress;
    this.networkSize = networkSize;
    boolean overStressed = maxStress < currentStress && StressImpact.isEnabled();
    setChanged();
    if (overStressed != this.overStressed) {
        float prevSpeed = getSpeed();
        this.overStressed = overStressed;
        onSpeedChanged(prevSpeed);
        sendData();
    }
}
```
`content/kinetics/base/KineticBlockEntity.java:155-169`

```java
public float getSpeed() {
    if (overStressed || (level != null && level.tickRateManager().isFrozen()))
        return 0;
    return getTheoreticalSpeed();
}
```
`content/kinetics/base/KineticBlockEntity.java:289-301`

### 1.3 网络维护时机（做动态元件必须背下来）

| 事件 | 调用链 | 位置 |
|---|---|---|
| BE 加入网络 | `KineticNetwork.add(be)` → `sources/members.put` → `updateFromNetwork` → `be.networkDirty = true` | `KineticNetwork.java:58-66` |
| 区块加载、网络已存在 | `initialize()` → `network.initFromTE(cap, stress, size)` + `addSilently(be, lastCap, lastStress)` | `KineticBlockEntity.java:86-95`；`KineticNetwork.java:27-56` |
| 源容量变化 | `notifyStressCapacityChange(cap)` → `network.updateCapacityFor(this, cap)` → `updateCapacity()` → `sync()` | `GeneratingKineticBlockEntity.java:26-28, 107-112` |
| 成员消耗变化 | `network.updateStressFor(this, stress)` → `updateStress()` → `sync()` | `GeneratingKineticBlockEntity.java:110` |
| 脏标记兜底 | `if (networkDirty) getOrCreateNetwork().updateNetwork()` | `KineticBlockEntity.java:120-124` |
| 移出网络 | `KineticNetwork.remove(be)`；空网则从 `TorquePropagator.networks` 摘除 | `KineticNetwork.java:78-96` |

> **实践含义**：如果你做了一个"随环境变化的应力源"（例如日照强度影响输出），必须在**每次变化后**调用 `updateGeneratedRotation()`（它会带 `notifyStressCapacityChange` + `updateStressFor` + `notifyUpdate`）。只改一个字段而不上报，网络账本永远是旧值。

### 1.4 速度等级与"最低转速要求"

`IRotate.SpeedLevel` 是**声明式**的最低转速门槛：

```java
public interface IRotate extends IWrenchable {
    default SpeedLevel getMinimumRequiredSpeedLevel() { return SpeedLevel.NONE; }
}
```
`content/kinetics/base/IRotate.java:144-150`

- 阈值来自配置：`SLOW = 1`，`MEDIUM = kinetics.mediumSpeed`（默认 30），`FAST = kinetics.fastSpeed`（默认 100）。`IRotate.java:47-73`。
- 判定入口：`KineticBlockEntity.isSpeedRequirementFulfilled()`，用 `Math.abs(getSpeed()) >= level.getSpeedValue()`。`KineticBlockEntity.java:372-379`。
- 示例：机械搅拌器声明 `SpeedLevel.MEDIUM`，并在 tick 里 `if (getSpeed() == 0 || !isSpeedRequirementFulfilled())` 回退动画。`content/kinetics/mixer/MechanicalMixerBlock.java:63-66`；`content/kinetics/mixer/MechanicalMixerBlockEntity.java:141`。
- goggle 会显示"转速不够"提示：`addToTooltip` 里 `notFastEnough`。`KineticBlockEntity.java:418-449`。

> **新元件建议**：需要"最低转速才工作"的装置（如高精度机床）用 `getMinimumRequiredSpeedLevel()` 声明，别自己写魔法数比较——这样 goggle、Ponder、玩家认知都统一。

---

## 2. 应力数值的注册：唯一正确姿势

### 2.1 `BlockStressValues`

```java
public class BlockStressValues {
    /** Registry for suppliers of stress impacts. Determine the base impact at 1 RPM. */
    public static final SimpleRegistry<Block, DoubleSupplier> IMPACTS = SimpleRegistry.create();
    /** Registry for suppliers of stress capacities. Determine the base capacity at 1 RPM. */
    public static final SimpleRegistry<Block, DoubleSupplier> CAPACITIES = SimpleRegistry.create();
    /** Registry for generator RPM values. This is only used for tooltips. */
    public static final SimpleRegistry<Block, GeneratedRpm> RPM = SimpleRegistry.create();

    public static double getImpact(Block block) {
        DoubleSupplier supplier = IMPACTS.get(block);
        return supplier == null ? 0 : supplier.getAsDouble();
    }
    public static double getCapacity(Block block) { /* 同上 */ }

    public static NonNullConsumer<Block> setGeneratorSpeed(int value) { /* RPM 注册，tooltip 用 */ }
    public static NonNullConsumer<Block> setGeneratorSpeed(int value, boolean mayGenerateLess) { ... }

    public record GeneratedRpm(int value, boolean mayGenerateLess) { }
}
```
`api/stress/BlockStressValues.java:10-53`

三个注册入口的语义：

```java
void register(K object, V value);            // 直接注册，优先级最高
void registerProvider(Provider<K, V> provider); // 惰性提供者，结果被缓存
@Nullable V get(K object);
```
`api/registry/SimpleRegistry.java:28-63`

### 2.2 为什么不能用 `CStress`

```java
private static void assertFromCreate(BlockBuilder<?, ?> builder) {
    if (!builder.getOwner().getModid().equals(Create.ID))
        throw new IllegalStateException("Non-Create blocks cannot be added to Create's config.");
}
```
`infrastructure/config/CStress.java:89-93`

- `CStress` 是**Create 自己的配置文件**（`server().kinetics.stressValues`），键是 `ResourceLocation`，只允许 Create 的方块进入。
- 它通过 `registerProvider` 接入 `BlockStressValues`：
  ```java
  CStress stress = server().kinetics.stressValues;
  BlockStressValues.IMPACTS.registerProvider(stress::getImpact);
  BlockStressValues.CAPACITIES.registerProvider(stress::getCapacity);
  ```
  `infrastructure/config/AllConfigs.java:68-70`
- **附属模组应提供自己的 ModConfig + 自己的注册**，而不是往 Create 的配置里塞键。

### 2.3 三种可选落地方式

**方式 A（推荐）：自己的 ModConfig + DoubleSupplier 直接注册**

```java
public final class MyStressValues {
    public static void register() {
        BlockStressValues.IMPACTS.register(MyBlocks.MY_MACHINE.get(),
            () -> MyConfig.SERVER.myMachineImpact.get());
        BlockStressValues.CAPACITIES.register(MyBlocks.MY_GENERATOR.get(),
            () -> MyConfig.SERVER.myGeneratorCapacity.get());
        // tooltip 用的 RPM（不参与计算，但应保持与实际一致）
        BlockStressValues.RPM.register(MyBlocks.MY_GENERATOR.get(),
            new BlockStressValues.GeneratedRpm(32, false));
    }
}
```

优点：配置热改立刻生效（每次 `getImpact` 都重新 `getAsDouble()`），不缓存。

**方式 B：按标签批量给值**

```java
BlockStressValues.IMPACTS.registerProvider(
    SimpleRegistry.Provider.forBlockTag(MyTags.LOW_STRESS_KINETICS, 2.0));
```
`api/registry/SimpleRegistry.java:98-101`

适合"整类方块统一档位"。注意 provider 结果会缓存（`SimpleRegistry.java:65-71`），改配置后需 `BlockStressValues.IMPACTS.invalidate()`。

**方式 C：不注册，直接覆盖 BE 的计算方法**

```java
@Override
public float calculateStressApplied() {
    lastStressApplied = myDynamicImpact();
    return lastStressApplied;
}
```
只有当应力**不由方块决定**（例如"应力占用取决于内部库存量"）时才这么做；**记得同步 `lastStressApplied`**，否则区块卸载/重载后的 `addSilently` 补偿会算错。

### 2.4 参考：CEI（附魔工业）怎么做

CEI 没有直接用 Create 的 `CStress`，而是扩展了第三方库 **Create: Dragons Plus** 的 `StressConfig`：

```java
public class CEIStressConfig extends StressConfig {
    public CEIStressConfig() { super(CEICommon.ID); }
    @Override protected int getVersion() { return 1; }
}
```
`.research/src/cei/plus/dragons/createenchantmentindustry/config/CEIStressConfig.java:24-32`

然后挂在动力学配置组下（`config/CEIKineticsConfig.java:34`），并在方块注册链上使用：

```java
public static final BlockEntry<MechanicalGrindstoneBlock> MECHANICAL_GRINDSTONE = REGISTRATE
        .block("mechanical_grindstone", MechanicalGrindstoneBlock::new)
        .initialProperties(SharedProperties::stone)
        .transform(CEIConfig.stress().setImpact(4.0))
        ...
        .register();
```
`.../common/registry/CEIBlocks.java:64-72`

> **给我们的结论**：`transform(config.stress().setImpact(x))` 这种"注册期声明 + 配置驱动"的写法最贴合 Create 生态，但它**依赖额外库**（Dragons Plus）。我们已有 Create 的 `libs` jar，**更轻的做法是方式 A**：自建一个小 `StressConfigBase` 或直接注册 `DoubleSupplier`，API 行为完全一致。

---

## 3. 应力源清单（Create 6.0.10）

| 方块 | 类 | 容量基值 | 生成 RPM | 动态因素 | 关键代码 |
|---|---|---|---|---|---|
| 水车 | `WaterWheelBlockEntity extends GeneratingKineticBlockEntity` | 32 | 8 | 水流/气泡柱的流量与方向（`flowScore`），`getGeneratedSpeed() = clamp(flowScore,-1,1)*8/size` | `content/kinetics/waterwheel/WaterWheelBlockEntity.java:224-227`；`AllBlocks.java:580-581` |
| 大水车 | `LargeWaterWheelBlockEntity` | 128 | 4 | 同上，`getSize()=2` 使转速减半 | `AllBlocks.java:595-596` |
| 风车轴承 | `WindmillBearingBlockEntity extends MechanicalBearingBlockEntity` | 512 | 16（`mayGenerateLess`） | 帆数：`sails = sailBlocks / windmillSailsPerRPM`，`clamp(sails,1,16) * 方向` | `content/contraptions/bearing/WindmillBearingBlockEntity.java:65-74`；`AllBlocks.java:1184-1185` |
| 蒸汽机（动力轴） | `PoweredShaftBlockEntity extends GeneratingKineticBlockEntity` | 1024（挂在引擎方块上） | 16 × speedModifier | 锅炉效率 `engineEfficiency`：效率越高 → speedModifier 越大 → 转速高而单速容量低 | `content/kinetics/steamEngine/PoweredShaftBlockEntity.java:108-126`；`AllBlocks.java:1067-1068` |
| 手摇曲柄 | `HandCrankBlockEntity extends GeneratingKineticBlockEntity` | 8 | 32 | `inUse` 与 `backwards`（转向），松手即 0 | `content/kinetics/crank/HandCrankBlockEntity.java:58-69`；`AllBlocks.java:649-650` |
| 创造马达 | `CreativeMotorBlockEntity extends GeneratingKineticBlockEntity` | 16384 | 256（可调 `ScrollValueBehaviour`） | 玩家设定转速，符号 = 转向 | `content/kinetics/motor/CreativeMotorBlockEntity.java:66-78`；`AllBlocks.java:565-566` |
| 阀门手轮 | `ValveHandleBlockEntity extends HandCrankBlockEntity` | 8（复用曲柄配置） | 32 | 同曲柄 | `AllBlocks.java:967`；`BuilderTransformers.java:293-308` |

### 3.1 三种源实现范式

**范式 1：环境驱动型（水力/风力）** —— 由 `lazyTick` 周期性采样环境，变化时 `updateGeneratedRotation()`

```java
@Override
public void lazyTick() {
    super.lazyTick();
    determineAndApplyFlowScore();   // 采样水流方向
}

public void setFlowScoreAndUpdate(int score) {
    if (flowScore == score) return;
    flowScore = score;
    updateGeneratedRotation();      // ★ 关键：变化才上报
    setChanged();
}

@Override
public float getGeneratedSpeed() {
    return Mth.clamp(flowScore, -1, 1) * 8 / getSize();
}
```
`content/kinetics/waterwheel/WaterWheelBlockEntity.java:114-120, 163-169, 224-227`

**范式 2：结构驱动型（风车/锅炉）** —— 由外部实体（contraption / 锅炉）推送参数，收到就重算

```java
// PoweredShaft：被引擎 update() 推参数
public void update(BlockPos sourcePos, int direction, float efficiency) {
    BlockPos key = worldPosition.subtract(sourcePos);
    enginePos = key;
    float prev = engineEfficiency;
    engineEfficiency = efficiency;
    if (Mth.equal(efficiency, prev) && prevDirection == direction) return;  // 无变化不重算
    capacityKey = level.getBlockState(sourcePos).getBlock();
    movementDirection = direction;
    updateGeneratedRotation();
}

@Override
public float getGeneratedSpeed() {
    return getCombinedCapacity() > 0 ? movementDirection * 16 * getSpeedModifier() : 0;
}
private float getCombinedCapacity() {
    return capacityKey == null ? 0 : (float) (engineEfficiency * BlockStressValues.getCapacity(capacityKey));
}
private int getSpeedModifier() {
    return (int) (1 + (engineEfficiency >= 1 ? 3 : Math.min(2, Math.floor(engineEfficiency * 4))));
}
@Override
public float calculateAddedStressCapacity() {
    float capacity = getCombinedCapacity() / getSpeedModifier();
    this.lastCapacityProvided = capacity;
    return capacity;
}
```
`content/kinetics/steamEngine/PoweredShaftBlockEntity.java:44-57, 108-126`

> **这个 `speedModifier` 分配是创造引擎的核心技巧**：总"功率" `combinedCapacity` 固定，"转速 ↑" 与 "单位转速容量 ↓" 反向分配，于是 `容量 × 转速` 的乘积在效率=1 附近保持稳定，而玩家可以通过转速选择"高扭矩/高转速"。**做新源强烈建议照抄这个分配函数。**

**范式 3：交互驱动型（曲柄/马达）** —— 玩家操作直接改参数

```java
// 曲柄：inUse 倒计时控制
@Override
public float getGeneratedSpeed() {
    Block block = getBlockState().getBlock();
    if (!(block instanceof HandCrankBlock crank)) return 0;
    int speed = (inUse == 0 ? 0 : clockwise() ? -1 : 1) * crank.getRotationSpeed();
    return convertToDirection(speed, getBlockState().getValue(HandCrankBlock.FACING));
}
// tick() 里 inUse-- 到 0 时：updateGeneratedRotation()
```
`content/kinetics/crank/HandCrankBlockEntity.java:58-65, 85-101`

```java
// 马达：ScrollValueBehaviour 回调 → updateGeneratedRotation()
generatedSpeed.between(-max, max);
generatedSpeed.value = DEFAULT_SPEED;
generatedSpeed.withCallback(i -> this.updateGeneratedRotation());
```
`content/kinetics/motor/CreativeMotorBlockEntity.java:54-64`

### 3.2 `getGeneratedSpeed()` 的符号与朝向

源必须把"方块朝向"折算进符号，否则玩家会发现"换个朝向就反转"：

```java
return convertToDirection(generatedSpeed.getValue(), getBlockState().getValue(CreativeMotorBlock.FACING));
```
`content/kinetics/motor/CreativeMotorBlockEntity.java:74-78`

`convertToDirection(axisSpeed, d)` 定义在 `KineticBlockEntity.java:508-510`：`d.getAxisDirection() == POSITIVE ? axisSpeed : -axisSpeed`。

> 新源如果是**有朝向**的方块（`DirectionalKineticBlock`），`getGeneratedSpeed()` 必须用 `convertToDirection(...)`；如果是**轴对称**的（水车那种），直接用 `flowScore` 的符号即可。

---

## 4. 应力消耗装置清单（Create 6.0.10）

基值来自 `AllBlocks.java` 的 `CStress.setImpact(...)`；"速度依赖"来自各 BE 的实现。

| 方块 | 类 | 消耗基值 | 最低转速 | 速度如何影响行为 | 关键代码 |
|---|---|---|---|---|---|
| 磨石 | `MillstoneBlockEntity extends KineticBlockEntity` | 4.0 | 无 | 每 tick 进度 `clamp(|speed|/16, 1, 512)`；有速度就按比例加速 | `content/kinetics/millstone/MillstoneBlockEntity.java:86-129, 206-208` |
| 机械压床 | `MechanicalPressBlockEntity` | 8.0 | 无 | 冲压周期随转速缩短 | `AllBlocks.java:717` |
| 机械搅拌器 | `MechanicalMixerBlockEntity` | 4.0 | **MEDIUM** | 处理耗时 `log2(512/speed) * ceil(recipeSpeed*15) + 1`；低于 MEDIUM 直接回退 | `content/kinetics/mixer/MechanicalMixerBlockEntity.java:141-165`；`MechanicalMixerBlock.java:63-66` |
| 粉碎轮 | `CrushingWheelBlockEntity` | 8.0 | 无 | 转速影响粉碎效率与伤害；对向轮速配合 | `AllBlocks.java:693` |
| 动力锯 | `SawBlockEntity` | 4.0 | 无 | 切割速度 | `AllBlocks.java:1363` |
| 动力钻头 | `DrillBlockEntity` | 4.0 | 无 | 挖掘速度 | `AllBlocks.java:1350` |
| 鼓风机 | `EncasedFanBlockEntity` | 2.0 | 无 | 推送距离/处理速度随转速，上限由 `fanRotationArgmax` | `AllBlocks.java:619` |
| 部署器 | `DeployerBlockEntity` | 4.0 | 无 | 动作间隔随转速 | `AllBlocks.java:1376` |
| 机械臂 | `ArmBlockEntity` | 2.0 | 无 | 动作速度 | `AllBlocks.java:1551` |
| 转盘 | `TurntableBlockEntity` | 4.0 | 无 | 旋转速度 | `AllBlocks.java:640` |
| 机械轴承 | `MechanicalBearingBlockEntity` | 4.0 | 无 | 旋转速度 | `AllBlocks.java:1194` |
| 时钟轴承 | `ClockworkBearingBlockEntity` | 4.0 | 无 | 角度目标驱动 | `AllBlocks.java:1204` |
| 绳轮 | `PulleyBlockEntity` | 4.0 | 无 | 升降速度 | `AllBlocks.java:1245` |
| 升降机绳轮 | `ElevatorPulleyBlockEntity` | 4.0 | 无 | 同上 | `AllBlocks.java:1245` |
| 链式传送带 | `ChainConveyorBlockEntity` | 1.0 | 无 | 移动速度 | `AllBlocks.java:550-551` |
| 机械泵 | `PumpBlockEntity` | 4.0 | 无 | 流量速率 | `AllBlocks.java:935` |
| 机械合成器 | `MechanicalCrafterBlockEntity` | 2.0 | 无 | 每格独立计应力 | `AllBlocks.java:1551` |

> 注：表中 `AllBlocks.java` 行号是**该方块注册段内的 `setImpact` 行**，同一段可能被多个方块复用（例如链式传送带出现两次 `setImpact(1)`，见 `AllBlocks.java:550-551`）。

### 4.1 零应力 / 无影响的动力件

| 方块 | 处理 | 位置 |
|---|---|---|
| 传送带 | `CStress.setNoImpact()`（整条带只在 controller 计一次） | `AllBlocks.java:538`；`BeltBlockEntity.java:154-159` |
| 封闭轴 / 封闭齿轮 | `CStress.setNoImpact()` | `BuilderTransformers.java:226-232` |
| 龙门轴 | `CStress.setNoImpact()` | `AllBlocks.java:1174` |
| 离合器 / 换向器 / 序列齿轮箱 / 转速控制器 / 飞轮 | `setNoImpact()` | `AllBlocks.java:1565-1588` |

> **对我们的启示**：传动件默认应 `setNoImpact()`（不影响应力账本，只影响拓扑），只有"真正做功"的机器才收 SU。若你的传动件本身也耗功（例如带摩擦损耗的变速器），给一个小的固定 impact（1~2 SU）比 `0` 更有手感。

### 4.2 消耗装置的两个"速度映射"范式

**范式 A：进度累加型（磨石）** —— 保留"总工作量"，每 tick 扣进度

```java
public int getProcessingSpeed() {
    return Mth.clamp((int) Math.abs(getSpeed() / 16f), 1, 512);
}

// tick()
if (timer > 0) {
    timer -= getProcessingSpeed();
    if (timer <= 0) process();
    return;
}
```
`content/kinetics/millstone/MillstoneBlockEntity.java:96-107, 206-208`

**范式 B：对数耗时型（搅拌器）** —— 直接反算"要等多久"

```java
processingTicks = Math.max((Mth.log2((int) (512 / speed))) * Mth.ceil(recipeSpeed * 15) + 1, 1);
```
`content/kinetics/mixer/MechanicalMixerBlockEntity.java:162-163`

> 两者都保证"低速仍能工作，只是慢"，并且**低速时不会因为 `int` 截断变成 0 进度**（`clamp` 下限 1）。新机器建议用范式 A（更直观、更易配 Ponder 动画）。

### 4.3 消耗装置的判定顺序模板

从磨石/搅拌器归纳出的通用 tick 骨架：

```java
@Override
public void tick() {
    super.tick();                        // 必须：动力学更新 + 网络脏标记

    if (getSpeed() == 0) return;         // ★ 用 getSpeed()，过载/冻结自动停
    if (!isSpeedRequirementFulfilled()) return; // 声明了最低转速的机器

    // 输出满 → 停工（避免无意义算力与粒子）
    // 处理中 → 推进进度
    // 空闲   → 找配方 / 起始进度
}
```

---

## 5. 可编译级实操模板

### 5.1 新的应力源（最小可运行）

```java
package com.example.addon.content.kinetics;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 环境驱动的应力源示例：输出 = baseRpm × 环境系数，容量 = capacityBase × 环境系数。
 * 环境变化时务必调用 updateGeneratedRotation()。
 */
public class MyGeneratorBlockEntity extends GeneratingKineticBlockEntity {

    private int environmentLevel = 0;   // 例如：帆数 / 水温 / 日照等级

    public MyGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(40);            // 环境采样频率
    }

    /** 唯一必须覆盖的方法：真实 RPM（可带符号）。 */
    @Override
    public float getGeneratedSpeed() {
        if (environmentLevel <= 0) return 0;
        float rpm = Mth.clamp(environmentLevel, 1, 16) * 4f;   // 4 RPM / 单位环境
        return convertToDirection(rpm, getBlockState().getValue(MyGeneratorBlock.FACING));
    }

    /**
     * 可选覆盖 A：容量基值的来源。
     * 默认实现 = BlockStressValues.getCapacity(getStressConfigKey())，
     * 只想用注册值就别写这个方法。
     */
    @Override
    public float calculateAddedStressCapacity() {
        float capacity = super.calculateAddedStressCapacity();   // 已写入 lastCapacityProvided
        return capacity;
    }

    /**
     * 可选覆盖 B：查表键。
     * 默认 = getBlockState().getBlock()。若多个方块共用一份应力配置才需要覆盖。
     */
    @Override
    protected net.minecraft.world.level.block.Block getStressConfigKey() {
        return getBlockState().getBlock();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide) return;
        int sampled = sampleEnvironment();          // 你的环境探测逻辑
        if (sampled == environmentLevel) return;
        environmentLevel = sampled;
        updateGeneratedRotation();                  // ★ 重算速度 + 上报容量 + 同步
    }

    private int sampleEnvironment() {
        // 例：数周围可推动的方块 / 检查朝向是否有流体 / 读取天气
        return 4;
    }

    /** 让首次放置/加载就有输出（Create 的创造马达也是这么做的）。 */
    @Override
    public void initialize() {
        super.initialize();
        if (!hasSource() || getGeneratedSpeed() > getTheoreticalSpeed())
            updateGeneratedRotation();
    }
}
```
> `initialize()` 的写法参照 `content/kinetics/motor/CreativeMotorBlockEntity.java:66-71`。
> `convertToDirection` 来自 `KineticBlockEntity.java:508-510`。

配套方块（有朝向的用 `DirectionalKineticBlock`，轴对称的用 `RotatedPillarKineticBlock`）：

```java
public class MyGeneratorBlock extends DirectionalKineticBlock implements IBE<MyGeneratorBlockEntity> {

    public MyGeneratorBlock(Properties properties) { super(properties); }

    /** 只有背面接轴（示意）；两面都可接就两个方向都返回 true。 */
    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public Class<MyGeneratorBlockEntity> getBlockEntityClass() { return MyGeneratorBlockEntity.class; }

    @Override
    public BlockEntityType<? extends MyGeneratorBlockEntity> getBlockEntityType() {
        return MyBlocks.MY_GENERATOR_BE.get();
    }
}
```
> `DirectionalKineticBlock` 自带 `FACING` 与放置朝向推断：`content/kinetics/base/DirectionalKineticBlock.java:14-58`。
> `IBE` 契约：`foundation/block/IBE.java`。

应力值注册：

```java
public final class MyStressValues {
    public static void register() {
        // 源：容量基值（每 1 RPM 的 SU）
        BlockStressValues.CAPACITIES.register(MyBlocks.MY_GENERATOR.get(),
            () -> MyConfig.SERVER.generatorCapacity.get());
        // 仅用于 goggle / JEI 的标称 RPM
        BlockStressValues.RPM.register(MyBlocks.MY_GENERATOR.get(),
            new BlockStressValues.GeneratedRpm(64, true));
        // 该源自身也有微小消耗（可选）
        BlockStressValues.IMPACTS.register(MyBlocks.MY_GENERATOR.get(), () -> 0.0);
    }
}
```

### 5.2 新的应力消耗装置（最小可运行）

```java
package com.example.addon.content.kinetics;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 旋转驱动的加工机示例：进度累加型（照抄磨石的映射）。
 */
public class MyMachineBlockEntity extends KineticBlockEntity {

    private int timer;
    public int processingTicks;

    public MyMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** 每 tick 进度：|speed|/16，下限 1，避免低速卡死。 */
    public int getProcessingSpeed() {
        return Mth.clamp((int) Math.abs(getSpeed() / 16f), 1, 512);
    }

    @Override
    public void tick() {
        super.tick();                                  // ★ 必须

        if (level == null) return;
        if (level.isClientSide) return;                // 逻辑只在服务端；客户端靠 sendData

        if (getSpeed() == 0) return;                   // 过载/冻结自动停
        if (!isSpeedRequirementFulfilled()) return;    // 若在方块里声明了最低转速

        if (timer > 0) {
            timer -= getProcessingSpeed();
            if (timer <= 0) process();
            return;
        }
        if (!canStart()) return;
        timer = 100;                                   // 基准工作量
        sendData();
    }

    private boolean canStart() { return true; }

    private void process() {
        // 产物生成、消耗输入、award(...)
        setChanged();
        sendData();
    }
}
```

方块侧（同时声明最低转速要求）：

```java
public class MyMachineBlock extends HorizontalKineticBlock implements IBE<MyMachineBlockEntity> {

    public MyMachineBlock(Properties properties) { super(properties); }

    @Override
    public SpeedLevel getMinimumRequiredSpeedLevel() { return SpeedLevel.SLOW; }  // 1 RPM

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == getRotationAxis(state);
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();  // 或按你的模型定
    }

    @Override public Class<MyMachineBlockEntity> getBlockEntityClass() { return MyMachineBlockEntity.class; }
    @Override public BlockEntityType<? extends MyMachineBlockEntity> getBlockEntityType() { return MyBlocks.MY_MACHINE_BE.get(); }
}
```
> `HorizontalKineticBlock`：`content/kinetics/base/HorizontalKineticBlock.java`。
> `SpeedLevel` 枚举与阈值：`content/kinetics/base/IRotate.java:19-97`。

### 5.3 "轴位代理源"模板（源必须占用轴位置的场合）

如果源本质上是一个**轴**（像蒸汽机的动力轴），用 Create 的代理范式：

```java
public class MyPoweredShaftBlockEntity extends GeneratingKineticBlockEntity {
    public BlockPos hostPos;            // 谁在驱动我
    public float efficiency;            // 驱动效率 0~1
    public int direction = 1;           // ±1
    public Block capacityKey;           // ★ 容量从"宿主方块"查，而不是从我这个轴查

    public void update(BlockPos host, int dir, float eff) {
        if (Mth.equal(eff, efficiency) && direction == dir) return;
        hostPos = worldPosition.subtract(host);
        efficiency = eff;
        direction = dir;
        capacityKey = level.getBlockState(host).getBlock();
        updateGeneratedRotation();
    }

    @Override
    public float getGeneratedSpeed() {
        return getCombinedCapacity() > 0 ? direction * 16 * getSpeedModifier() : 0;
    }
    private float getCombinedCapacity() {
        return capacityKey == null ? 0 : (float) (efficiency * BlockStressValues.getCapacity(capacityKey));
    }
    private int getSpeedModifier() {
        return (int) (1 + (efficiency >= 1 ? 3 : Math.min(2, Math.floor(efficiency * 4))));
    }

    @Override
    public float calculateAddedStressCapacity() {
        float capacity = getCombinedCapacity() / getSpeedModifier();
        this.lastCapacityProvided = capacity;
        return capacity;
    }
}
```
> 完整原始实现见 `content/kinetics/steamEngine/PoweredShaftBlockEntity.java:44-126`（含 NBT 持久化 `enginePos`/`capacityKey` 的写法）。
> **关键点**：`calculateAddedStressCapacity()` 里必须同时写 `lastCapacityProvided`，否则 `initialize()` 的 `addSilently` 补偿会失真（`KineticBlockEntity.java:86-95`）。

---

## 6. 数值设计指南

### 6.1 Create 现有档位

| 阶段 | 典型源 | 满速 SU | 可带动 |
|---|---|---|---|
| 木质（手摇/水车） | 手摇曲柄 8×32=256；水车 32×8=256 | 256 | 磨石(4×64=256)、若干泵 |
| 木质+（大水车） | 128×4=512 | 512 | 多台磨石/压床 |
| 风（风车） | 512×16=8192 | 8192 | 中型工厂 |
| 铜（蒸汽机） | 1024×64=65536（效率分配后不同） | 65536 | 大型工厂 |
| 创造 | 16384×256 | ≈4.19M | 无限 |

单台机器的"满速消耗" = `impact × speed`：例如磨石 4 SU × 64 RPM = 256 SU。

### 6.2 新元件的选值原则

1. **先说"它替代谁"**：新源应落在现有档位之间并留出独特优势（例如"火星沙尘风车"= 水车的稳定性 + 风车的上限，容量 64×8=512）。不要一上来就 16384。
2. **消耗装置按"每秒工作量"定价**：`impact × 典型转速` 应接近同类 Create 机器。若你的机器快 2 倍，impact 也该 ≈2 倍。
3. **传动件默认 `setNoImpact()`**；除非它有实质摩擦损耗（给 1~2）。
4. **`RPM` 注册值必须与实际 `getGeneratedSpeed()` 一致**，否则 goggle 会显示错误的"容量@RPM"，玩家会以为坏了。
5. **动态容量要留"最小值"**：完全归零时 `getGeneratedSpeed()` 返回 0 → `isSource()` 变 false → 网络失去源 → 整线停摆（这是正确行为，但要让玩家看到提示，例如 Ponder + goggle）。

### 6.3 配置项建议

```java
public static final ModConfigSpec.DoubleValue generatorCapacity = BUILDER
    .comment("Stress capacity per RPM of the Mars Dust Turbine. [in SU/RPM]")
    .defineInRange("marsTurbineCapacity", 64.0, 0.0, 1_000_000.0);
```
要点：

- 名字带单位说明（`[in SU/RPM]`），与 Create 配置风格一致（`CStress.java:95-100` 的 `Comments`）。
- 区间上限给足（玩家爱调大）。
- **不要**把应力值做成"每次读配置都重算网络"——`DoubleSupplier` 已经是惰性读取，但**容量变更后网络不会自动刷新**；若要支持运行时改配置，监听 `ModConfigEvent.Reloading` 后对所有已知网络 `updateNetwork()`。

---

## 7. 常见坑（按踩中概率排序）

| # | 坑 | 症状 | 正确做法 | 依据 |
|---|---|---|---|---|
| 1 | 用 `getTheoreticalSpeed()` 判断"是否在转" | 过载后机器仍在跑、仍在吐产物 | 逻辑一律 `getSpeed()`；只有应力计算/对比用 `getTheoreticalSpeed()` | `KineticBlockEntity.java:289-301` |
| 2 | 动态容量不调 `updateGeneratedRotation()` | goggle 显示旧容量；实际能带动数不随环境变 | 每次参数变化后必须调用 | `GeneratingKineticBlockEntity.java:89-116` |
| 3 | 覆盖 `calculateAddedStressCapacity()` 忘写 `lastCapacityProvided` | 区块重载后网络容量偏移 | 与 Create 一样同时赋值 | `KineticBlockEntity.java:185-189`；`PoweredShaftBlockEntity.java:121-126` |
| 4 | 调用 `CStress.setImpact` | 启动即 `IllegalStateException` | 注册 `BlockStressValues.IMPACTS` | `CStress.java:89-93` |
| 5 | 用 `registerProvider` 提供配置值 | 改配置后仍是旧值（缓存） | 用 `register(block, () -> cfg.get())`，或改配置后 `invalidate()` | `SimpleRegistry.java:65-71` |
| 6 | 只在客户端算/显示应力 | 服务端行为与显示不一致 | 应力全在服务端；客户端靠 `sendData()` 同步 | `KineticBlockEntity.java:107-110, 155-169` |
| 7 | 源 RPM 超过 `maxRotationSpeed`(256) | **方块被直接摧毁** | 设计上限，或在 `getGeneratedSpeed()` 里 `Mth.clamp` | `RotationPropagator.java:235-243`；`CKinetics.java:14` |
| 8 | 源被活塞/轴承移动后状态错乱 | 拆装后不转/容量错 | KineticBlock 已 `warnOfMovement()`/`clearKineticInformation()`；BE 的 `read` 里尊重 `wasMoved` | `KineticBlock.java:61-78`；`KineticBlockEntity.java:244-252` |
| 9 | 消耗装置每 tick 无节制扫描 | 卡服 | 输入/输出满时提前 `return`；用 `lazyTick` 做慢扫描 | `MillstoneBlockEntity.java:90-95` |
| 10 | 应力基值给了 `double` 字面量而非 supplier | 玩家无法调平衡 | 一律 supplier + 配置 | 本文 2.3 |
| 11 | 传送带式多段结构每段都计应力 | 应力爆炸 | 只在 controller 计（`if (!isController()) return 0`） | `BeltBlockEntity.java:154-159` |
| 12 | 源与消耗用同一个 `getStressConfigKey` | 配置互相污染 | 覆盖 `getStressConfigKey()` 明确指定方块 | `HandCrankBlockEntity.java:116-120` |

---

## 8. 自检清单（提交前逐条打勾）

- [ ] `getGeneratedSpeed()` 的符号是否符合方块朝向（有朝向的用了 `convertToDirection`）？
- [ ] 所有参数变化路径（`lazyTick`/交互/外部推送/`read`）都会触发 `updateGeneratedRotation()`？
- [ ] `calculateAddedStressCapacity()` 是否同步了 `lastCapacityProvided`？
- [ ] 应力基值是否通过 `BlockStressValues.CAPACITIES/IMPACTS` 注册（而非 `CStress`）？
- [ ] `BlockStressValues.RPM` 是否与实际 RPM 一致？
- [ ] 消耗装置是否用 `getSpeed()`，并在 `getSpeed()==0` 时早退？
- [ ] 声明了 `getMinimumRequiredSpeedLevel()` 的机器是否在 tick 里检查 `isSpeedRequirementFulfilled()`？
- [ ] 输出/输入满时是否会提前退出以省算力？
- [ ] 过载时是否有可见反馈（goggle 会自动提示；机器自身是否停动画/停粒子）？
- [ ] 源上限是否低于 `maxRotationSpeed`（默认 256）？
- [ ] 配置项是否带单位注释与合理上限？
- [ ] `initialize()` 是否让摆放后立即输出（`if (!hasSource() || getGeneratedSpeed() > getTheoreticalSpeed()) updateGeneratedRotation();`）？

---

## 9. 待验证 / 未覆盖

- `LargeWaterWheelBlockEntity` 的 `getSize()` 具体返回值未逐行读（本文按"尺寸 2 → RPM 减半"表述，依据 `getGeneratedSpeed() = clamp(flowScore,-1,1)*8/getSize()` 与容量 128/4 RPM 的注册值，⚠ 建议落地前再确认一次）。
- `AllConfigs.server().kinetics` 中 `mediumCapacity`/`highCapacity` 只影响 goggle 颜色分档，不参与计算；本文按源码注释推断，⚠ 若要做 UI 一致性需再核。
- `CStress.java:98` 的注释 "this cost is doubled for every speed increase it receives" 与 6.0.10 实际线性实现矛盾，判定为**旧版遗留注释**（依据 `KineticNetwork.getStressMultiplierForSpeed` 直接返回 `Math.abs(speed)`）。写数值文档时以代码为准。
- 各消耗装置 `impact` 与 `AllBlocks` 行号的对应关系是按"注册段内最近一次 `setImpact`"人工比对，⚠ 个别方块（如链式传送带）存在重复调用，落地引用前建议用 `grep -n setImpact` 复核一次。
