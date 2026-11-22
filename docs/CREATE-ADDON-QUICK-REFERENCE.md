# Create 6.0.x 附属开发 · 总索引与速查表

> 本目录是「北极星星球附属」对接 **Create 6.0.x（MC 1.21.1 / NeoForge）** 的源码研究资料。
> 目标：为我们后续新增 **应力源 / 应力消耗装置 / 传动系统元件** 提供可直接动手的契约、模板与数值依据。
>
> 研究材料（全部为真实源码，非二手教程）：
> - **Create 6.0.10**：`create-1.21.1-6.0.10-281-sources.jar`（maven.createmod.net），与仓库 `libs/create-1.21.1-6.0.10.jar` 同版本线。
> - **Create: Enchantment Industry 2.5.4b**：Modrinth 官方 `-sources.jar`。
> - **Create Jetpack 5.2.1 + FlightLib 4.0.36**：GitHub tag 源码（交叉核对发行 jar 反编译产物）。
>
> 核对时间：2026-11。行号均以对应 sources 包中的真实路径为准。

---

## 1. 文档地图

| 文档 | 内容 | 什么时候读 |
|---|---|---|
| [CREATE-6.0-CORE.md](CREATE-6.0-CORE.md) | **总纲**：版本与依赖、代码地图、模组骨架、动力学核心对象、传播算法、应力模型、元件三分类、渲染/Ponder 概览、附属接入红线、落地路线 | 第一次接触本主题时通读 |
| [CREATE-KINETIC-TRANSMISSION.md](CREATE-KINETIC-TRANSMISSION.md) | **传动系统**：`IRotate` 契约表、`getRotationSpeedModifier` 完整判定顺序、轴/齿轮/齿轮箱/离合/换向/序列/链传动/转速控制器/传送带逐个剖析、新增传动元件的实操模板、传动件一览表 | 要做「新的传动元件」 |
| [CREATE-STRESS-SYSTEM.md](CREATE-STRESS-SYSTEM.md) | **应力体系**：精确公式、网络维护时机、过载链路、`BlockStressValues` 唯一正确注册方式、全部应力源清单、全部消耗装置清单、源/消耗两套可编译模板、数值设计指南、12 个坑 | 要做「新的应力源 / 新机器」 |
| [CREATE-REGISTRATION-RENDER-PONDER.md](CREATE-REGISTRATION-RENDER-PONDER.md) | **工程层**：`CreateRegistrate` 封锁与自建、builder 链、`SmartBlockEntity` 生命周期、Flywheel 1.0 Visual 链路、Ponder 注册、ValueBox 交互、Goggle、6.0.6 后的 datagen、CreativeTab、addon 起步清单 | 开始写第一个方块时 |
| [CREATE-CASE-STUDY-ENCHANTMENT-INDUSTRY.md](CREATE-CASE-STUDY-ENCHANTMENT-INDUSTRY.md) | **案例 A（附魔工业）**：模组骨架、机械磨石/磨石排液/烈焰附魔器/烈焰锻造器逐个剖析、用到的全部 Create 扩展点、11 个 mixin 的动机、可选集成四层隔离、可复用结论 | 想知道"成熟附属长什么样" |
| [CREATE-CASE-STUDY-JETPACK.md](CREATE-CASE-STUDY-JETPACK.md) | **案例 B（喷气背包）**：单 jar 双入口、Kotlin/jarjar 取舍、复用 Create 的 BE/Renderer/模型、背罐空气系统扩展点、配置同步三件套、扩展机械合成配方的范式 | 想看"轻量附属如何最大化复用" |

阅读顺序建议：**CORE → 按任务选 TRANSMISSION 或 STRESS → REGISTRATION → 两个 CASE STUDY（对照取舍）**。

---

## 2. 术语表

| 术语 | 含义 | 出处（Create 源码路径） |
|---|---|---|
| RPM | 转速，`float`，**符号表示转向** | `content/kinetics/base/KineticBlockEntity.java` |
| SU | 应力单位（Stress Unit） | `api/stress/BlockStressValues.java` |
| 网络 / network | 共享同一源速度与应力账本的一组动力方块；按 `Long id` 存在 `TorquePropagator.networks` | `content/kinetics/KineticNetwork.java`、`TorquePropagator.java` |
| 源 / source | `getGeneratedSpeed() != 0` 的 BE，即"发电机" | `KineticBlockEntity.isSource()` |
| 成员 / member | 网络里除源以外的动力 BE（消耗方 + 传动件） | `KineticNetwork.members` |
| 容量 / capacity | 网络可提供的 SU 总量 | `KineticNetwork.calculateCapacity()` |
| 消耗 / stress | 网络已占用的 SU 总量 | `KineticNetwork.calculateStress()` |
| 过载 / overstressed | `capacity < stress`；此后 `getSpeed()` 返回 0 | `KineticBlockEntity.updateFromNetwork()` |
| 倍率 / modifier | 传播时两个 BE 之间的速度比与方向，`float`；`0` = 不连 | `RotationPropagator.getRotationSpeedModifier()` |
| 轴连接 | 两个 BE 面对面、都在对方那面 `hasShaftTowards` 为真 | 同上 `connectedByAxis` |
| 自定义连接 | BE 通过 `propagateRotationTo` / `isCustomConnection` 声明非标准连接 | `KineticBlockEntity` 的三个钩子 |
| 应力基值 | `impactBase`（每 1 RPM 的 SU 消耗）/ `capacityBase`（每 1 RPM 的 SU 容量） | `BlockStressValues.IMPACTS/CAPACITIES` |

---

## 3. 一分钟速查

### 3.1 我要做……

| 目标 | 基类 | 必须覆盖 | 还要做 |
|---|---|---|---|
| 新的应力源 | `GeneratingKineticBlockEntity` | `getGeneratedSpeed()` | 注册 `BlockStressValues.CAPACITIES` + `RPM`；变化后调 `updateGeneratedRotation()` |
| 新的应力消耗机器 | `KineticBlockEntity` | `tick()` 里的速度驱动逻辑 | 注册 `BlockStressValues.IMPACTS`；`getSpeed()==0` 早退 |
| 新的传动件（同轴直传） | `RotatedPillarKineticBlock` 或 `AbstractShaftBlock` | `hasShaftTowards()` + `getRotationAxis()` | `setNoImpact()`；渲染用 `SingleAxisRotatingVisual` |
| 新的换向/变速件 | `DirectionalKineticBlock` + `SplitShaftBlockEntity` | `getRotationSpeedModifier(Direction face)` | 倍率符号必须与放置朝向自洽 |
| 新的"轴位代理源" | `GeneratingKineticBlockEntity`（挂在轴上） | `getGeneratedSpeed()` + `calculateAddedStressCapacity()` | 用 `capacityKey` 指回宿主方块取容量（蒸汽机范式） |

### 3.2 连接倍率表

| 连接 | 倍率 | 说明 |
|---|---|---|
| 同轴对接 | `1` | 同向同速 |
| 小齿轮 ↔ 小齿轮 | `-1` | 外啮合反向 |
| 大齿轮带小齿轮 | `-2` | 小轮 2 倍速、反向 |
| 小齿轮带大齿轮 | `-0.5` | 大轮半速、反向 |
| 大齿轮 ↔ 大齿轮（轴垂直） | `±1` | 取决于轴差符号 |
| 齿轮箱（同轴对面） | `-1` | 换向 |
| 齿轮箱（垂直面） | `±1` | 按 axis direction |
| 离合器断电 | `0` | 断开 |
| 换向器通电 | `-1` | 反转 |
| 自定义 | `propagateRotationTo` 返回值 | 任意 |

### 3.3 应力数值档（Create 6.0.10 出厂值）

| 源 | 容量基值 | 生成 RPM | 满速 SU |
|---|---|---|---|
| 水车 | 32 | 8 | 256 |
| 大水车 | 128 | 4 | 512 |
| 风车轴承 | 512 | 16 | 8192 |
| 蒸汽机 | 1024 | 64 | 65536 |
| 手摇曲柄 | 8 | 32 | 256 |
| 创造马达 | 16384 | 256 | ≈4.19M |

| 常见消耗 | impact 基值 |
|---|---|
| 磨石 / 搅拌器 / 转盘 / 轴承 / 绳轮 / 泵 / 锯 / 钻 / 部署器 | 4.0 |
| 压床 / 粉碎轮 | 8.0 |
| 鼓风机 | 2.0 |
| 机械臂 / 机械合成器 | 2.0 |
| 链式传送带 | 1.0 |
| 传送带 / 封闭轴 / 传动件 | 0（`setNoImpact()`） |

> 详细表（含动态因素与行号）见 [CREATE-STRESS-SYSTEM.md](CREATE-STRESS-SYSTEM.md) 第 3、4 章。

### 3.4 三条铁律

1. **逻辑判断用 `getSpeed()`，应力计算用 `getTheoreticalSpeed()`** —— 前者在过载/冻结时归零。
2. **禁止调用 `CStress.setImpact/setCapacity`**（非 `create` modid 会抛异常），一律注册 `BlockStressValues`。
3. **禁止调用 `Create.registrate()`**（有调用方包名检查），一律自建 `CreateRegistrate.create(MOD_ID)`。

---

## 4. 本仓库现状与差距（截至本次分析）

| 项 | 现状 | 结论 |
|---|---|---|
| MC / NeoForge | 1.21.1 / `21.1.249`（`gradle.properties`） | ✅ 与技能钉选线一致 |
| NeoForge 插件 | `net.neoforged.moddev` 2.0.144 | ✅ |
| `neoforge.mods.toml` | 已含 `modLoader="javafml"` + `loaderVersion="[1,)"` | ✅ 1.21.1 必需，已正确 |
| Create 依赖 | `libs/create-1.21.1-6.0.10.jar`，`compileOnly` + `runtimeOnly` | ✅ 编译期可见，不进发布 jar |
| `create_version_range` | `[6.0,)` | ⚠ **建议收紧到 `[6.0.10,)`**：6.0.0~6.0.5 的 `ProcessingRecipe`/datagen API 与 6.0.10 不兼容（6.0.6 重构），放宽会导致装了旧 Create 的整合包在运行期炸 |
| 现有 Create 对接代码 | `venus/compat/create/CreateCompat.java` 仍是「探测 + 日志」，反射判类 | ⚠ 已有真实 jar 后应改为**直接编译期引用 + 真实注册**；反射路径可作为"无 Create 降级"保留 |
| 现有动力学探测 | `mars/world/KineticMachines.java` 用 `Class.forName` 判 `KineticBlockEntity` | ⚠ 同上；但作为"恶魔蛾攻击动力方块"的软探测，可保留 |
| 新元件包位置（建议） | `com.suwako.northstarplanetaddon.create.kinetics`（新建） | 与现有 `mars`/`mercury`/`venus` 星球包并列，避免混进"软兼容"代码 |

> 详细落地路线见 [CREATE-6.0-CORE.md](CREATE-6.0-CORE.md) 第 10 章。

### 4.1 佐证：成熟附属的元数据怎么写

**Create: Enchantment Industry 2.5.4b** 的 `META-INF/neoforge.mods.toml`（真实内容，可直接对照）：

```toml
modLoader="javafml"
loaderVersion="[4,)"
license="LGPL-3.0-or-later"

[[mods]]
    modId="create_enchantment_industry"
    version="2.5.4b"
    displayName="Create: Enchantment Industry"

[[mixins]]
    config="create_enchantment_industry.mixins.json"
[[mixins]]
    config="create_enchantment_industry.apothic_enchanting.mixins.json"
[[mixins]]
    config="create_enchantment_industry.apotheosis.mixins.json"
[[mixins]]
    config="create_enchantment_industry.sable.mixins.json"
[[mixins]]
    config="create_enchantment_industry.touhou_little_maid.mixins.json"

[[dependencies.create_enchantment_industry]]
    modId="neoforge"
    type="required"
    versionRange="[21.1.228,)"
    side="BOTH"

[[dependencies.create_enchantment_industry]]
    modId="minecraft"
    type="required"
    versionRange="[1.21.1, 1.22)"      # ★ MC 版本用左闭右开区间
    side="BOTH"

[[dependencies.create_enchantment_industry]]
    modId="create"
    type="required"
    versionRange="[6.0.10,)"           # ★ 精确下限 = 编译时的 Create 版本
    side="BOTH"

# 可选集成：一个 mod 一段，全部 optional + AFTER
[[dependencies.create_enchantment_industry]]
    modId="apotheosis"
    type="optional"
    versionRange="[8.7.0,)"
    ordering="AFTER"
    side="BOTH"
```

三条可抄的写法：

1. **MC 版本用左闭右开**（`[1.21.1, 1.22)`），防止误装到 1.22 上。
2. **`create` 的下限写死成编译版本**（CEI 写 `[6.0.10,)`，正好等于它编译时的 Create 版本）。这直接支持我们在 §4 里的建议：把本仓库的 `[6.0,)` 收紧到 `[6.0.10,)`。
3. **每个可选集成一个独立 mixin config**，并在 toml 里逐个 `[[mixins]]` 声明。理由见 [CREATE-CASE-STUDY-ENCHANTMENT-INDUSTRY.md](CREATE-CASE-STUDY-ENCHANTMENT-INDUSTRY.md)：mixin config 里任何一条注入失败（`defaultRequire: 1`）都会硬崩，把可选集成的注入隔离出去，没装该 mod 时就不会被加载。

> ⚠ 注意：CEI 的 sources jar **只含主 config**（`create_enchantment_industry.mixins.json`，11 条 mixin + 1 个 `accessor.CreateRecipeCategoryAccessor`）；4 个集成 config 在 toml 里声明但不在 sources 包内（构建期生成/被排除）。研究集成 mixin 的注入点时应以反编译后的发行 jar 为准。

---

## 5. 检查清单：提交新元件前

- [ ] 方块实现了 `IRotate`（或继承 KineticBlock 家族），`hasShaftTowards` 与 `getRotationAxis` 自洽
- [ ] 双向连接判定互补（我方 `face`，对方 `face.getOpposite()`）
- [ ] 倍率符号与方块朝向一致（有朝向的用 `convertToDirection`）
- [ ] 应力值通过 `BlockStressValues` 注册（supplier + 配置，不写字面量）
- [ ] 源：参数变化后必调 `updateGeneratedRotation()`；`calculateAddedStressCapacity()` 时同步 `lastCapacityProvided`
- [ ] 消耗：`getSpeed()==0` 早退；输入/输出满时早退
- [ ] 过载有可见反馈（goggle 自动；自身停动画/停粒子）
- [ ] `initialize()` 里让摆放后立即生效
- [ ] BE 注册了 Visual（Flywheel）+ Renderer（回退）
- [ ] 物品 tooltip 走 `ItemDescription` + `KineticStats.create(item)`
- [ ] 有 Ponder 场景（新玩家的唯一学习路径）
- [ ] 资源走 `runData`（`build.gradle` 已有 data run），不手写 JSON
- [ ] `create_version_range` ≥ 实际用到的 API 版本

---

## 6. 研究过程与可复现性

源码获取方式（便于后续复核或升级到 Create 6.0.11+）：

```powershell
# Create sources（maven，版本号带 build 后缀）
https://maven.createmod.net/com/simibubi/create/create-1.21.1/<ver>-<build>/create-1.21.1-<ver>-<build>-sources.jar
# 元数据：https://maven.createmod.net/com/simibubi/create/create-1.21.1/maven-metadata.xml

# CEI sources（Modrinth 版本 API 里的 -sources.jar）
https://api.modrinth.com/v2/project/JWGBpFUP/version?loaders=%5B%22neoforge%22%5D&game_versions=%5B%221.21.1%22%5D

# Jetpack / FlightLib（GitHub tag）
https://github.com/PssbleTrngle/CreateJetpack/archive/refs/tags/5.2.1.tar.gz
https://github.com/PssbleTrngle/FlightLib/archive/refs/tags/4.0.36%2B1.21.1.tar.gz

# 反编译（核对 jar 与 GitHub 源码差异）
java -jar vineflower-1.11.1.jar -dgs=1 -hdc=0 -rsy=1 <mod>.jar <outDir>
```

工作副本位于 `.research/`（`src/create`、`src/cei`、`src/jetpack*`、`src/flightlib-github`、`sections/`），该目录**不参与构建**，可随时删除。

升级到 Create 6.0.11+ 时需重点复核的位置：

| 关注点 | 为什么 |
|---|---|
| `api/stress/BlockStressValues.java` | 应力注册入口若变，全部新机器受影响 |
| `content/kinetics/base/KineticBlockEntity.java`、`GeneratingKineticBlockEntity.java` | 契约基类 |
| `content/kinetics/RotationPropagator.java` | 倍率判定顺序（私有方法，签名变动会影响自定义连接语义） |
| `content/kinetics/KineticNetwork.java` | 应力账本与 `unloaded*` 补偿 |
| `foundation/data/CreateRegistrate.java`、`CreateBlockEntityBuilder.java` | 注册期 API（`.visual()` / `.renderer()`） |
| `infrastructure/config/CStress.java` | `assertFromCreate` 是否仍然存在（决定 addon 能否用配置） |
| Ponder / Flywheel 版本 | 独立库，可能单独升级 |
| 6.0.6 的 `ProcessingRecipe` 重构 | 任何走 Create 配方的机器都要跟 |
