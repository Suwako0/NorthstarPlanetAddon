# 北极星星球附属 / Northstar Planet Addon

给 **Create: Northstar Redux（北极星，modid `northstar`）** 提供 **月球 / 水星 / 火星 / 金星** 四条星球内容线的附属模组：
一个 jar、一个模组条目、一份依赖声明，四条内容线各自补齐群系、生物、Boss、天气、装备与机械。

| 项 | 值 |
|---|---|
| Minecraft | 1.21.1 |
| 加载器 | NeoForge 21.1.248（依赖区间 `[21.1,)`） |
| Java | 21（toolchain，`options.release = 21`） |
| 构建插件 | `net.neoforged.moddev` 2.0.144，Gradle 9.2.1 wrapper |
| mod id | `northstar_planet_addon` |
| 版本 | 1.0.0 |
| 主类 | `com.suwako.northstarplanetaddon.NorthstarPlanetAddon` |
| 必需依赖 | `northstar [0.6.5,)`、`create [6.0,)`、`geckolib [4.7,5.0)` |
| 软依赖 | `curios`、`tacz`（缺失时金星模块自动降级） |
| 许可 | All Rights Reserved |

---

## 目录

0. [内容总览](#0-内容总览)
1. [架构与实现](#1-架构与实现)
2. [月球附属（前缀 `moon`）](#2-月球附属前缀-moon)
3. [水星附属（前缀 `mercury`）](#3-水星附属前缀-mercury)
4. [火星附属（前缀 `mars`）](#4-火星附属前缀-mars)
5. [金星附属（前缀 `venus`）](#5-金星附属前缀-venus)
6. [四条内容线之间的约定与隔离](#6-四条内容线之间的约定与隔离)
7. [美术资源约定](#7-美术资源约定)
8. [目录结构](#8-目录结构)
9. [构建与运行](#9-构建与运行)
10. [已知问题与待办](#10-已知问题与待办)
11. [作者与许可](#11-作者与许可)

---

## 0. 内容总览

**一个模组、一个命名空间、四个内容前缀。** 注册命名空间统一为 `northstar_planet_addon`，四条内容线用路径前缀区分：
`northstar_planet_addon:moon_rabbit`、`northstar_planet_addon:mercury_sand`、`northstar_planet_addon:ares`、
`northstar_planet_addon:pressure_tank`。创造模式标签页各用一个 ID：`…:moon` / `…:mercury` / `…:mars` / `…:venus`。

| 内容线 | 对外名称（中 / 英） | 前缀 | 目标星球 / 维度 | 内容规模 | 一句话玩法 |
|---|---|---|---|---|---|
| 月球 | 北极星月球附属 / Northstar Moon Addon | `moon` | `northstar:moon` | 12 方块 / 21 物品 / 5 实体 / 2 流体 / 2 群系 | 月林与露天温泉、月华树、月兔与月壳蟹、梦眠月鹿 Boss、温泉浸泡 |
| 水星 | 北极星水星附属 / Northstar Mercury Addon | `mercury` | `northstar:mercury` | 1 方块 / 5 物品 / 3 实体 / 1 效果 / 1 群系 | 太阳风暴天气、陨石坠落、水星沙与干涸海床、太阳鱼 |
| 火星 | 北极星火星附属 / Northstar Mars Addon | `mars` | `northstar:mars` | 6 方块 / 25 物品 / 2 实体 / 2 效果 / 3 群系 | 沙尘暴与护目镜、火星钢装备、星核炮、熔核巨像·阿瑞斯 |
| 金星 | 北极星金星附属 / Northstar Venus Addon | `venus` | `northstar_planet_addon:venus_clouds`（本模组自带维度） | 17 方块 / 10 方块实体 / 83 物品 / 11 实体 / 5 群系 / 8 结构 | 云海维度、气压机器、四套护甲、三个 Boss 与「反枪械宪章」伤害管线 |

中文名统一为「北极星 ×× 附属」，英文统一为「Northstar ×× Addon」；**「启明星」是金星内容线的装备与素材品牌名**
（启明星核、启明星战甲、启明星祭坛、启明星套餐……）。

---

## 1. 架构与实现

### 1.1 一个入口，四个模块类

```java
@Mod(NorthstarPlanetAddon.MODID)
public final class NorthstarPlanetAddon {
    public static final String MODID = "northstar_planet_addon";

    public NorthstarPlanetAddon(IEventBus modBus, ModContainer container) {
        MoonModule.init(modBus);             // 月球：注册表 + 事件 + 客户端接线
        MercuryModule.init(modBus);          // 水星：注册表 + 事件 + 网络 + 客户端接线
        MarsModule.init(modBus);             // 火星：8 个 DeferredRegister（其余走 @EventBusSubscriber）
        VenusModule.init(modBus, container); // 金星：13 个注册表 + 配置 + 游戏总线事件
    }
}
```

| 模块类 | 包 | 职责 |
|---|---|---|
| `moon.MoonModule` | `com.suwako.northstarplanetaddon.moon` | 流体类型 → 流体 → 方块 → 物品 → feature → 音效 → 实体 → 创造标签；群系注入与温泉浸泡监听；客户端 `NMAClient` |
| `mercury.MercuryModule` | `…mercury` | 方块 / 物品 / 实体 / 音效 / feature / 效果 / 创造标签；网络包注册；群系注入、太阳风暴、指令、沐浴阳光监听；客户端 `MercuryClient` |
| `mars.MarsModule` | `…mars` | 方块 / 物品 / 盔甲材质 / 实体 / 效果 / 数据组件 / feature / 创造标签；其余事件全部由 `@EventBusSubscriber` 自注册 |
| `venus.VenusModule` | `…venus` | 方块 / 物品 / 方块实体 / 盔甲材质 / 数据组件 / 效果 / 实体 / 创造标签 / feature / 配方序列化器 / 流体 / 群系源 / 菜单；注册 `northstar_planet_addon-venus-common.toml` 配置；游戏总线注册 `CombatEvents` / `PlayerEvents` / `PhosphorosCommands` |

注册顺序在模块内部自洽（流体类型必须先于流体），模块之间**没有注册表依赖**，因此编排顺序不影响结果。

### 1.2 命名空间与前缀

每个模块类只暴露两个常量，其余代码一律按它们拼注册名与资源路径：

```java
public static final String NAMESPACE = NorthstarPlanetAddon.MODID;  // 统一命名空间
public static final String MODULE    = "moon";                      // 本内容线前缀
```

| 对外项 | 月球 | 水星 | 火星 | 金星 |
|---|---|---|---|---|
| 中文名 | 北极星月球附属 | 北极星水星附属 | 北极星火星附属 | 北极星金星附属 |
| 英文名 | Northstar Moon Addon | Northstar Mercury Addon | Northstar Mars Addon | Northstar Venus Addon |
| 创造标签 ID | `northstar_planet_addon:moon` | `…:mercury` | `…:mars` | `…:venus` |
| 标签标题键 | `itemGroup.northstar_planet_addon.moon` | `…mercury` | `…mars` | `…venus` |
| 模块类 | `MoonModule` | `MercuryModule` | `MarsModule` | `VenusModule` |
| 包 | `…northstarplanetaddon.moon` | `….mercury` | `….mars` | `….venus` |
| 日志前缀 | `[moon]` | `[mercury]` | `[mars]` | `[venus]` |

资源与数据同样只有一份：`assets/northstar_planet_addon/` 与 `data/northstar_planet_addon/`；
`data/minecraft/` 与 `data/northstar/` 是对原版与北极星的标签追加，保持原来的命名空间。

### 1.3 注册表与事件的组织

| 内容线 | 注册方式 | 事件接线 |
|---|---|---|
| 月球 | 集中在 `MoonModule.init` 内按依赖顺序注册 | `modBus` 上注册客户端接线；游戏总线监听群系注入与温泉 tick |
| 水星 | 集中在 `MercuryModule.init`，另有网络包注册 | `@EventBusSubscriber` + `modBus` 网络注册 |
| 火星 | 8 个 `DeferredRegister`（方块 / 物品 / 盔甲材质 / 实体 / 效果 / 数据组件 / feature / 创造标签） | 全部 `@EventBusSubscriber(modid = NorthstarPlanetAddon.MODID)` |
| 金星 | 13 个 `DeferredRegister`（含方块实体、菜单、配方序列化器、流体、群系源） | 游戏总线注册 `CombatEvents` / `PlayerEvents` / `PhosphorosCommands` |

`@EventBusSubscriber` 的 `modid` 必须指向 `NorthstarPlanetAddon.MODID`，否则 NeoForge 不会把监听器挂到本模组上。

网络载荷共三类：太阳风暴状态（`northstar_planet_addon:solar_storm`，`bool`，`playToClient`）、
火星沙尘暴 / 冷却器换弹、金星气压计数据。它们共用命名空间但路径互不相同。

指令：`/solarstorm start|stop|query|impact`（水星）、`/northstarmars sandstorm …`（别名 `/sandstorm`，火星）、
`/phosphoros debug dps|boss|classify`（金星，仅 OP）。
按键：全模组只有一个 `KeyMapping`——火星冷却器的 `R`（分类键 `key.categories.northstar_planet_addon`）。

### 1.4 Mixin：两个配置，五个 Mixin

| 配置 | package | Mixin | 目标类 | 手段 |
|---|---|---|---|---|
| `northstar_planet_addon.moon.mixins.json` | `…moon.mixin` | `BiomeSourceMixin` | `BiomeSource` | `@Shadow @Final @Mutable possibleBiomes`，实现 `nma$refreshPossibleBiomes()` |
| | | `MultiNoiseBiomeSourceMixin` | `MultiNoiseBiomeSource` | `@Shadow @Final @Mutable parameters`，实现 `nma$get/setClimateEntries()` |
| `northstar_planet_addon.mercury.mixins.json` | `…mercury.mixin` | `BiomeSourceMixin` | `BiomeSource` | 同结构，方法前缀 `mna$` |
| | | `MultiNoiseBiomeSourceMixin` | `MultiNoiseBiomeSource` | 同结构，方法前缀 `mna$` |
| | | `PlayerExhaustionMixin` | `Player` | `@ModifyVariable(method = "causeFoodExhaustion(F)V", at = HEAD)`，沐浴阳光时饥饿消耗 ×0.4 |

全部 Mixin 都是 `@Shadow` 字段桥接或 `@ModifyVariable`，**没有一个 `@Redirect`**——避免与其它模组抢同一调用点。
两个配置同时登记在 `neoforge.mods.toml` 的 `[[mixins]]`（开发运行）与 jar 清单的 `MixinConfigs`（成品包，逗号分隔）。

火星与金星**没有 Mixin**：火星的群系注入走反射，金星的伤害管线走事件。

### 1.5 依赖与开发期联调 jar

`neoforge.mods.toml` 声明的依赖：

| mod | 类型 | 区间 | 理由 |
|---|---|---|---|
| `neoforge` / `minecraft` | required | `[21.1,)` / `[1.21.1]` | — |
| `northstar` | required | `[0.6.5,)` | 四条内容线光顾的维度、方块与群系都来自它 |
| `create` | required | `[6.0,)` | 火星机械腐蚀 / 星核炮摧毁动力方块；金星气动马达与 WorkConsumer |
| `geckolib` | required | `[4.7,5.0)` | 火星恶魔蛾与阿瑞斯的 geo/animation |
| `curios` | optional | `*` | 金星弹道校准器槽位，缺失时改为背包内生效 |
| `tacz` | optional | `*` | 金星只按伤害类型标签识别弹道（TaCZ 版本形如 `1.1.8-hotfix-r6`，区间只能写 `*`） |

`libs/` 里的 6 个 jar（northstar、create、flywheel、ponder、Registrate、geckolib）只作为
`compileOnly` + `runtimeOnly` 使用：**不会打进发布 jar，也不会变成玩家的硬依赖声明**。
代码层对它们的引用全部是注册名 / 标签 / 反射（少数桥接类只在反射命中后才被类加载，见 §5.1）。

### 1.6 配置文件

| 文件 | 归属 |
|---|---|
| `config/northstar_planet_addon-venus-common.toml` | 金星（战斗乘数 / 上限 / Boss / 世界 / 机器 / 兼容） |

其余三条内容线没有配置项，参数写死在常量里（详见各章节）。

---

## 2. 月球附属（前缀 `moon`）

**定位**：给 `northstar:moon` 追加月林 / 露天温泉两个群系、月华树木家族、月兔 / 月壳蟹 / 梦眠月鹿 Boss、
温泉流体与梦眠祭坛召唤仪式。

### 2.1 注册内容

| 类别 | 内容 |
|---|---|
| 方块 12 | 梦眠之草（可骨粉）、月壤、月华原木 / 木 / 去皮两件 / 木板 / 树叶（发光 12）/ 树苗、月之花（发光 6）、`hot_spring` 液体块（无 BlockItem、无掉落表）、梦眠祭坛 |
| 物品 21 | 月壳、月饼（再生 I 200t + 幸运 I 1200t）、月明珠、破损的月明珠、月鹿角、梦眠月斧（2200 耐久 / 速度 9.5 / +4.5 / 附魔性 22）、温泉水桶、3 个刷怪蛋、11 个方块物品 |
| 实体 5 | `moon_rabbit`、`moon_shell_crab`、`lunar_deer`（Boss）、`moon_dust_projectile`、`moonlight_bolt` |
| 流体 | `hot_spring` / `hot_spring_flowing` + 流体类型（temp 400、`canConvertToSource`、lightLevel 3） |
| 音效 6 | `lunar_deer_hit1/2/3`、`lunar_deer_die`、`moonduel`（Boss 战曲）、`moonlight`（月林环境曲） |
| 其它 | feature `hot_spring_lake`、标签 `northstar_planet_addon:moon_forest_spawnable`、创造标签 `northstar_planet_addon:moon`（21 项） |

### 2.2 世界生成

- **群系**：`moon_forest`（月林，creature 刷月兔 / 月壳蟹）与 `open_hot_spring`（露天温泉，只刷北极星月球怪物）。
  两者都挂 `northstar:` 的陨石坑、矿石、晶洞与 blob，feature 按原版装饰步骤下标排列。
- **注入**（`MoonBiomeInjector`，`ServerAboutToStartEvent`）：只认 `northstar:moon` 维度；找到
  `northstar:lunar_plains` 且 weirdness 恰为 `[-0.9333, -0.7666]` 的那条气候参数盒，删掉后从下往上切成
  `open_hot_spring [-0.9333, -0.8666]` 与 `moon_forest [-0.85, -0.80]`（两段刻意不重叠——`multi_noise` 取最近参数点，
  重叠会让先出现的条目赢下），其余六个气候轴与 offset 原样继承。写回后调 `nma$refreshPossibleBiomes()` 重建候选集。
- **安全阀**：群系缺失 / 维度不存在 / 找不到那条参数盒 / 已注入过 → 只打日志并整体放弃，月球保持原样，不会崩。
- 结构模板 `data/northstar_planet_addon/structure/dreamsleep_alter.nbt` 只被月明珠现场放置，不参与世界生成。

### 2.3 核心机制

| 机制 | 要点 |
|---|---|
| 梦眠之草骨粉 | 32 次尝试，`rand(5)==0` 出月之花，否则短草 |
| 月华树苗 | 覆写 `advanceTree`，直接取 `northstar_planet_addon:moonlight_tree` 放置，失败把树苗放回去 |
| 月之花 | 悬空即毁（`neighborChanged` + `randomTick` 双保险），发光 6，客户端撒 GLOW |
| 月兔（8 HP） | 金胡萝卜「萌宠反应」56t 动画 + 80t 冷却；金胡萝卜「交易」（`MoonRabbitGoldenCarrotGoal`）48t 咀嚼后 **90% 月饼 / 10% 月明珠**，再 80t 冷却 |
| 月壳蟹（16 HP / 护甲 6） | 只被 `HurtByTargetGoal` 引战；≤4 格钳击（14t 动画、30t 冷却、推击），≤10 格月尘（18t 动画、50t 冷却、弹体 2.0 伤害） |
| 梦眠月鹿 Boss（1000 HP / 护甲 10 / xp 2000） | 4 阶段（0.75 / 0.50 / 0.25）、8 技能（DASH 160t / BARRAGE 100t / RING 120t / BEAM 200t / MOON_RAIN 240t / SHADOW 200t / MOON_DUST 180t / METEOR 一次性），换阶段 20t 无敌 + 抗性 / 吸收，Boss 条含阶段名，专属战曲 `moonduel` 单独推流、只停这一首 |
| 召唤链 | 月明珠右键**梦眠之草**（必须 `northstar:moon`）→ 放置祭坛结构并消耗 1 颗明珠 → 手持月明珠右键祭坛、**夜间**、128 格内无存活月鹿 → 生成 Boss；祭坛 40 tick 后自毁且不掉落 |
| 温泉浸泡 | `EntityTickEvent.Post` 每 5 tick 检查 `isInFluidType(HOT_SPRING)`，续期再生 I + 饱和 I 各 20t |
| 温泉湖 | 自定义 `HotSpringLakeFeature`：先跑原版 `LakeFeature`，再对湖底逐格重掷 `noise_threshold_provider`，让月石 / 月沙 / 月石灯混合真正逐块生效 |

### 2.4 客户端

三个生物都是**原版 `HierarchicalModel`**（不用 GeckoLib）：兔 / 蟹的骨骼动画定义在 `NMAAnimations`
（idle / hop / pinch / throw / walk / gaze），鹿的姿态全部在 `MoonDeerModel.setupAnim` 里分层叠加。

梦眠月鹿的模型用 **Blockbench 制作、脚本生成**：项目文件 `build/art/moon_deer.bbmodel`（**53 个骨骼 /
213 个立方体** / 256×256 贴图），导出 `build/art/moon_deer.geo.json` 后由
`build/art/gen-moon-deer-java.ps1` 生成 `MoonDeerModel` 的 `createBodyLayer()`（含每块的 `texOffs`、
`PartPose` 与 `CubeDeformation`，并负责 `° → rad` 与 +Y/-Y 坐标翻转），改动一律走
「Blockbench → 重新导出 → 重新生成」，不手改生成段。躯干上几块大方量（胸 / 腹 / 臀 / 臀板 / 肩峰 / 颈 /
头骨 / 大腿）都拆成「上盖 + 中段 + 下盖」三层，且上下两层的 X/Z 各内收 0.5–1.3 单位，所以轮廓是阶梯状收边
而不是一块方板。骨架是完整的四足结构：四段躯干（胸 / 肩峰 / 收腹 / 后臀）+ 臀板 + 胸口月纹板 + 脊背纵脊、
两节脖子（含颈部鬃毛）+ 颈鬃、头骨 / 眉脊 / 面板 / 颊甲 / 颊毛 / 上下颌 / 鼻镜 / 鼻梁、额心神眼 + 两侧眼 +
双耳、**每侧六枝鹿角**（角基 / 中段 / 角尖 + 前枝 / 后枝 / 外枝 / 上下副枝 + 两簇垂挂花）、胸前三层鬃毛、
脊背 5 片冰刃 + 颈部 2 片、两节尾骨 + 尾扇、四条**三节腿**（大腿 / 小腿 / 蹄，另有球节、系部）。
整体 29.3 × 56.9 × 48.0 模型单位（约 1.8 × 3.6 × 3.0 格），蹄子正好落在 `y = 24` 的地面平面上。

判定区不是一个大方块，而是「主干碰撞箱 + 7 个判定部件」：`EntityType` 的盒子是 1.8 × 2.9（整只鹿的高度与
鹿角跨度，视高 2.6 = 鹿自己眼睛的高度，所以脖子和头本身就落在盒子里），胸前 / 颈部 / 头部 / 左右鹿角 /
臀后 / 尾巴各有自己的 `MoonDeerPart`（`PartEntity` 子类，与末影龙同一套多部件机制），每 tick 按 `yBodyRot`
贴在模型上；打中任一部件都会转成对本体的 `hurt`，所以护甲、阶段无敌、抗性与伤害管线全部照常生效。
另外所有判定盒（含主干）都带 0.5 格的 `getPickRadius`：颈部与头部在待机扫视和技能蓄力时摆动超过半格，
贴着静止姿态描的盒子会让头从准星下滑出去。各部件的尺寸和偏移不是估的，而是由
`build/art/measure-deer-parts.ps1` 算出每根骨骼在「以蹄为原点、面朝 -z」的世界空间中的实际包围盒后取的，
再由 `build/art/verify-deer-hitbox.ps1` 逐块取角点回代覆盖率（含 0.5 格容差）：
**1704 个立方体角点 100% 落在判定区内**。

动画分三层叠加，全部用相对量，静止姿态由模型本身决定：**步态层**（对角四拍、三节腿折叠 / 蹄回正、
躯干两次起伏 + 侧倾、颈头反向稳定、鬃毛与尾巴滞后）→ **待机层**（双频呼吸、重心偏移、慢速左右张望、
耳朵各自抽动、鹿角与花簇滞后、脊刃行波、尾巴甩动）→ **技能层**（8 个技能各自按「蓄力 → 出手 → 收势」
分段缓动，用模型自带的「每实体技能时钟」推进，另有受击缩身抖动与倒地过程）。渲染时再叠一层
`RenderType.eyes` 的自发光贴图 `moon_deer_glow.png`，让双眼、额心神眼、胸口月纹与花簇在暗处自己发亮。

`NMAAnimationResources` 是资源重载监听器，可热替换 `assets/<ns>/animations/**/*.json` 的两份兔动画。
月林背景音乐仅在 `northstar:moon` + `moon_forest` 群系播放（随机 200–3600 tick），月鹿存活或离开维度立即停曲。

---

## 3. 水星附属（前缀 `mercury`）

**定位**：给 `northstar:mercury` 加太阳风暴天气、水星沙、干涸海床群系、太阳鱼 / 炽烈太阳鱼与「沐浴阳光」效果。

### 3.1 注册内容

| 类别 | 内容 |
|---|---|
| 方块 1 | `mercury_sand`（`FallingBlock`，可以熔炼 / 高炉成玻璃） |
| 物品 5 | 水星沙、太阳鱼片（2 / 0.1）、太阳鱼果烩（5 / 0.6，食用给 2 分钟沐浴阳光）、2 个刷怪蛋 |
| 实体 3 | `meteor`（MISC，物理下落）、`sunfish`（12 HP / 3 攻击）、`blazing_sunfish`（36 HP / 护甲 6 / 8 攻击，火免疫） |
| 效果 1 | `sunbath`：移速 +30%（`ADD_MULTIPLIED_TOTAL`）、饥饿消耗 ×0.4 |
| 伤害类型 5 | `solar_storm`、`meteor`、`sunfish_ram`、`sunfish_flame`、`sunfish_beam`（全部 `burning` 特效） |
| 音效 4 | 复用北极星已有 ogg（`martian_duststorm_above`、`laser_burn*`、`rocket_blast*`） |
| 网络 1 | `northstar_planet_addon:solar_storm`（`bool`，`playToClient`） |
| 其它 | 创造标签 `northstar_planet_addon:mercury`（5 项） |

### 3.2 世界生成

- **群系 1**：`dry_riverbed`（干涸海床），刷北极星水星的猛禽 / 陆龟 / 蟑螂；feature 分组里挂
  `dry_riverbed_channel`（自定义）、`dry_riverbed_sand` / `_stone`（互为反向的 disk）、`dry_riverbed_ancient_debris`，
  另有 17 条北极星矿脉与 2 个陨石坑特征。
- **注入**（`MercuryBiomeInjector`，`ServerAboutToStartEvent`）：只认 `northstar:mercury`；把
  `northstar:mercury_hills` 那条大陆度恰好 `[-1.0, 1.0]` 的参数盒拆成三段
  `[-1,-0.23]` 丘陵 / `[-0.23,-0.09]` **干涸海床** / `[-0.09,1]` 丘陵——三段互斥且恰好拼满原盒，
  所以原盒内外的其它群系判定完全不变。写回后刷新候选集并逐条校验。
- **biome_modifier**：`neoforge:add_spawns` 给 `#northstar:mercury_biomes` 加太阳鱼（权重 12，1–3）与炽烈太阳鱼（权重 2，1）。
- **下陷**：`RiverbedChannelFeature` 逐列以自身地表为基准挖 4–5 格（岸坡 4–8 格），坑底铺 2 格水星沙；
  遇任何流体立即停手，绝不挖开岩浆。因为 `count 1` 且按区块应用，每列只执行一次，深度可预期。

### 3.3 核心机制

| 机制 | 要点 |
|---|---|
| 太阳风暴状态 | `SolarStormData extends SavedData`（主世界存储，重启不丢）：`Active` + `EndTime`（`MAX_VALUE` = 无限期） |
| 自然触发 | 每 1200 tick（60 s）掷一次，2% 命中，时长 2400–7200 tick（2–6 分钟） |
| 指令触发 | `/solarstorm start [infinite\|<秒>]` / `stop` / `query` / `impact`（权限 2，默认 6000 tick） |
| 灼烧 | 每 20 tick 扫水星全部实体：跳过 `SolarStormImmune`（两种太阳鱼）、旁观、创造、`!canSeeSky`；命中 3.0 伤害 + 6 秒火，玩家再加 40 tick 迟缓 |
| 陨石 | 首颗延迟 200 tick，之后 260–700 tick 一轮；从 y = 顶界 −100、初速 1.4、阻力 0.98 下落；600 tick 未落地即丢弃 |
| 陨石坑 | 椭球浅碟（半径 2–3、深 2–3），坑底 8% 概率嵌矿石（权重表 10 种北极星水星矿，总权重 12）、8% 岩浆块、30% 水星沙；撞击 5 格内 10 点伤害 + 5 秒火 + 推击 |
| 沐浴阳光 | 水星 + 白天（按维度 `fixed_time` 计算）+ 地表 ±(2/6) 格 + 头顶两次 `canSeeSky` → 续 100 tick 的 `ambient` 实例；条件失效立即移除。果烩给的实例 `ambient=false`，两条来源互不干扰 |
| 太阳鱼 AI | 悬浮在「地表 +24±6」格（`altitudeOffset` 让鱼群垂直散开），攻击三选一：旋转俯冲（起手 12t、0.85 速、命中半径 1.9、每刻自旋 34°）、拍翅散射（3 轮火环，半径 4.5，×0.6 伤害）、高能射线（4 发、32 格穿透、×0.8 伤害，仅炽烈鱼） |
| 休战过滤 | 玩家获得沐浴阳光后，太阳鱼在 1 tick 内丢弃目标；两种鱼互不攻击；四重过滤（`canAttack` / `setTarget` / 每刻复查 / Goal 取目标） |

### 3.4 客户端

雾色 `(0.82, 0.18, 0.05)` + 远平面 ×0.48；每 tick 五类粒子（灰雾 / 尘 / 天降火花 / 余烬）与 6 tick 一次的阵风；
80–220 tick 一次高空耀斑 + 雷声；镜头按「是否露天」在 12 tick 内过渡出 0.7° 抖动；三层循环环境音（风暴风 + 两种原版氛围）。
太阳鱼父类模型 17 个部件，炽烈鱼在此基础上加 8 个部件；攻击姿态由同步数据 `ATTACK_MODE` 改写滑翔姿态。

---

## 4. 火星附属（前缀 `mars`）

**定位**：给 `northstar:mars` 加赤色荒原 / 火山灰平原 / 火星冰原三个群系、恶魔蛾、火星钢装备、
沙尘暴天气与护目镜、便携式冷却器、火星草莓、星核炮，以及 Boss「熔核巨像 · 阿瑞斯」。

### 4.1 注册内容

| 类别 | 内容 |
|---|---|
| 方块 6 | `red_ironstone`、`mars_ice`（不随机融化、挖了不化水）、`dry_ice`、`mars_strawberry_crop`（8 阶段，只种在 `#northstar_planet_addon:mars_farmland`）、`star_core`（发光 12）、`ares_altar_casing` |
| 物品 25 | 材料 3（翼膜 / 鳞粉 / 防腐蚀涂层）、沙尘暴护目镜（铁头盔 220 耐久）、便携式冷却器 + 3 种冷却罐、火星草莓 + 种子、星核碎片 / 星核核心（抗火）、融核巨剑（2400 耐久、显示攻击 12）、星核炮（1800 耐久、显示攻击 20）、熔核护甲四件（610/888/832/721，抗火）、2 个刷怪蛋 |
| 实体 2 | `demon_moth`（30 HP，**继承原版 `Phantom`** + GeckoLib）、`ares`（800 HP / 护甲 20 / 击退抗性 1 / 体型 ×2，**继承原版 `IronGolem`** + GeckoLib） |
| 效果 2 | `corrosion`（每 20 tick 1+amplifier 魔法伤害 + 护甲掉耐久）、`corrosion_resistance` |
| 数据组件 2 | `coolant`（种类）、`coolant_charges`（mB） |
| 盔甲材质 1 | `molten_core`：5/12/9/5、韧性 4.5、附魔 15、击退抗性 0.15 |
| 其它 | 3 个自定义 feature 类型、创造标签 `northstar_planet_addon:mars`（25 项）、2 个网络包、`/sandstorm` 指令 |

### 4.2 世界生成

- **三个群系**：`mars_rusty_wasteland`（赤色荒原，`frozen`）、`mars_volcanic_highlands`（火山灰平原）、
  `mars_polar_ice_cap`（火星冰原，`has_precipitation`）。三者按装饰步骤逐格引用北极星的
  `mars_ore_*` / `mars_blob_*` / `mars_rock` / `mars_worm_nest` 等，火山群系另有熔岩湖与火山岩簇，极地挂冰堆与干冰堆。
- **自定义 feature**：`mars_surface_layer`（逐列把 `mars_stone` / `mars_deep_stone` 换成顶层 + 次表层，逐列比对起点群系以免跨群系铺错）、
  `spire`（中心柱 + 低矮外圈）、`crater`（`dig = depth×(1-ratio²)` 剖面，内核铺冰、环带铺熔岩，含 24000 次写入预算）。
- **注入**（`MarsBiomeInjector`，`LevelEvent.Load`）：反射替换 `MultiNoiseBiomeSource.parameters` 与
  `BiomeSource.possibleBiomes` 缓存（先缓存后参数表，避免半注入），随后 `refreshFeaturesPerStep()` + `validate()` 自检，
  任一步失败整体回滚为「只有北极星群系」。气候轴全部放宽（`T[-1.5,1.5]`、`depth[-0.5,1.49]` 让洞穴群系在地下仍胜出），
  HIGH 层约 23% 按 weirdness 分三带、LOW 层 6 条 W 带，合计 **15 个互不重叠的地表盒子**（本模组占 10 块）。
  应急开关：`-Dnorthstar_planet_addon.disableBiomeInjection=true`；每 JVM 只尝试一次。

### 4.3 核心机制

| 机制 | 要点 |
|---|---|
| 沙尘暴 | 服务端静态状态机（`active` / `intense` / 风向 / 起止时刻 / 来源 `NATURAL`/`BOSS`/`COMMAND`）；自然每 200 tick 掷 1/90（期望约 15 分钟一场），时长 3600–12000 tick，1/4 概率强化；指令 `/northstarmars sandstorm start\|intense\|stop\|status`（别名 `/sandstorm`） |
| 沙尘暴影响 | 每 200 tick 磨损护甲与手持物品 1 点（熔核四件套 / 护目镜 / 创造 / 旁观 / 不露天 免疫）；`corrosion` 每 20 tick 扣血并掉护甲耐久；耐腐蚀 = 穿 3 件 `#northstar_planet_addon:corrosion_resistant_armor` |
| 阿瑞斯核心温度 | 初值 1200，每 20 tick +15+phase×5，**每次实际结算的伤害 +20**；上限 2200。>1500 展开护盾（`hurt` 返回 false，完全无敌）且伤害 ×0.4、攻击 +50%；<500 脆弱，受伤 ×3 且迟缓 |
| 阿瑞斯阶段 | P1 熔火：岩浆拳（CD 80 / 前摇 16）、地热喷发（CD 160 / 6 落点）、召唤恶魔蛾（CD 900 / 2–4 只）；P2 风暴：悬停地表 +8 格、召唤强化沙尘暴、鳞粉风暴（8 枚追踪）、未压到 500 ℃ 以下每 10 tick 自愈 5；压下去则坠落 + 10 秒 ×3 脆弱期；P3 过载：护甲清零、每 40 tick 全图地热喷发、60 秒自爆（6 级爆炸 + 32 格最多 100 点 + 摧毁 16 格内 Create 动力方块） |
| 阿瑞斯其它 | 领域半径 48（域外目标直接解绑、无目标回中心）；免疫火 / 溺水 / 摔落，`push` 与 `knockback` 空实现；掉落星核碎片 2–4、火星钢锭 4–8、星核核心 15%、翼膜 2、鳞粉 4–8（自爆则全不掉） |
| 便携式冷却器 | 潜行右击装罐（1000 mB），R 键从背包换弹；每 tick 耗 5 mB、每 2 tick 发射一次（一罐≈10 秒）；射流 12 格，只对阿瑞斯降温：水 0.30 ℃/mB、液氧 0.80 ℃/mB（整罐 300 / 800 ℃），无伤害无击退 |
| 星核炮 | 右击 48 格瞄准 → 5 秒 10 轮轰炸（每轮 20 点、半径 6），冷却 600 tick，跳过自己与盟友 |
| 融核巨剑 | 右击地热斩：±78° 扇形、8 格、20 点 + 5 秒火、冷却 60 tick |
| 熔核护甲套装 | 头盔无限夜视、胸甲受伤 ×0.8、护腿移速 +20%、靴子免摔落，全套免火 / 岩浆 / 热地面 + 免疫沙尘暴磨损 |
| 火星草莓 | 成熟（age 7）掉 1–2 果 + 50% 概率额外 1 种子；吃下 100% 概率抗性 I 200 tick |

### 4.4 客户端

`ClientSandstormState` 是唯一客户端状态源（含 `remainingTicks` 本地递减）；雾在**无护目镜**时压到 72/32 格、
戴上推到 512 格（`RenderFog` 必须 `setCanceled(true)` 才会生效）；雾色向 `(0.72, 0.55, 0.36)` 混合；
每 2 tick 撒 `WHITE_ASH`（戴护目镜降到 1/4）+ `RenderGuiEvent.Pre` 铺全屏沙尘罩；左上角 HUD 显示标题、
警告或「强化 + 风向 + 剩余秒数」。
阿瑞斯贴图按阶段切 `ares_phase1/2/3.png`，护盾期额外用半透明层重画 `shield` 骨骼；恶魔蛾与阿瑞斯都走 GeckoLib 渲染。

---

## 5. 金星附属（前缀 `venus`）

**定位**：本模组自带维度 `northstar_planet_addon:venus_clouds` + 气动 / 压力机器体系 + 四套护甲 / 五把武器 / 四把工具 +
三个 Boss + 「反枪械宪章」的弹道伤害管线。

### 5.1 依赖策略：三层隔离

| 层 | 内容 |
|---|---|
| ① 主命名空间 | 只碰本模组类型；`compat/MachineEnvBridge`、`compat/MachineTickerBridge` 没有任何第三方 import |
| ② 反射层 | `compat/create/CreateReflect`、`compat/northstar/NorthstarReflect` 先 `ModList.isLoaded` 再 `Class.forName`，`resolve()` 只跑一次，失败只打 warn 并永久降级 |
| ③ 桥接类 | 真正引用第三方类的只有 `CreateMachineBridge`、`KineticPneumaticMotorEntity`、`NorthstarMachineBridge`，仅反射命中后才被类加载；`libs/` 缺 jar 时 `build.gradle` 用 `sourceSets.java.exclude` 排除它们，退回纯反射 |

TaCZ 只有数据侧联动：`TaczCompat` 在 `ServerStartedEvent` 打印 `#northstar_planet_addon:ballistic` 解出的伤害类型条数，不 import 任何 TaCZ 类。

### 5.2 注册内容

| 类别 | 数量 | 摘要 |
|---|---|---|
| 方块 | 17 | 钨块 / 硫晶块 / 云岩、压力罐、压缩机、高压锅、高压熔炉、散热器、菌床、冷凝塔、云梯、气动马达、酸泵、3 种作物、硫酸液体块 |
| 方块实体 | 10 | 上述机器；气动马达是**条件注册**（有 Create 时换成 `KineticPneumaticMotorEntity`） |
| 物品 | 83 | 19 材料、5 武器 + 弹道校准器、16 护甲（4 套 × 4 件）、8 食物、4 工具、3 召唤物、5 模块、3 种子、5 刷怪蛋、13 方块物品、硫酸桶 |
| 实体 | 11 | 3 Boss（`nephele` 520 / `hephaestus` 800 / `aphrodite` 1000）+ 8 群系生物 |
| 效果 / 流体 / 组件 / 材质 | 1 / 2 / 3 / 4 | 酸蚀 `acid_corrosion`（护甲 −1 每级，周期 40>>amp tick 掉血）、硫酸（density 1400、`canDrown`）、`freshness`/`corrosion`/`charge`、钨钢 / 云行者 / 熔管重甲 / 启明星 |
| 菜单 | 1 | `northstar_planet_addon:machine`（高压锅与高压熔炉共用，4 输入 + 1 输出 + 进度 + 气压条） |
| 配方序列化器 | 1 | `northstar_planet_addon:high_pressure`（无序多材料贪心匹配，默认 8 bar / 1200 ℃ / 400 tick） |
| 创造标签 | 1 | `northstar_planet_addon:venus`（按注册顺序铺满） |

### 5.3 世界生成

- **维度** `northstar_planet_addon:venus_clouds`：`min_y=0` / `height=384` / `ambient_light=0.35` / 无床无重生锚、
  `noise_settings` 默认方块为 `northstar_planet_addon:cloud_stone`、`sea_level=-64`、无含水层与矿脉。
- **群系源** `world/VenusBiomeSource`（注册到 `BIOME_SOURCE`）：只按 `Climate.TargetPoint.temperature` 线性映射到 5 个候选群系下标
  （数据包的 `multi_noise_biome_source_parameter_list` 装不下自定义群系参数）。
- **群系 5**：`cloud_archipelago`、`acid_flats`、`metal_frost_highlands`、`fungal_forest`、`lava_tube_galleries`
  （前 4 个各挂 7 个结构里的若干项；`lava_tube_galleries` 只刷熔岩蠕虫）。
- **结构 8**（全部是自定义 feature + `RuinBuilder`/`RuinPalette` 手写摆放，非 NBT 模板）：
  浮空岛（每区块 4 座，180–300 层）、云港遗迹（1/120）、酸泵站（1/90）、观测塔（1/140）、
  菌类苗圃（1/100）、坠毁飞艇（1/110）、启明星祭坛（1/400）、熔管城市（1/500，三层圆台 + 12 个箱子）。
  箱子战利品表 11 张。

### 5.4 核心机制

| 机制 | 要点 |
|---|---|
| 气压模型 | `MILLIBUCKET_PER_BAR = 1000`；机器之间只认相邻的 `AbstractMachineBlockEntity`（不占 capability，避免与 Create 冲突）；压缩机摇曲柄一次产 2000 mB、每 tick 推 2000；压力罐 8000 mB 容量、每 tick 每向推 500 |
| 机械功 | `WorkConsumer`：高压锅 / 高压熔炉在收到 ≥16 RPM 后获得 ×2 速度；气动马达 20 mB/t → 32 RPM，Create 在场时换成真正的 `KineticBlockEntity`（4 SU/RPM） |
| 过热 | 环境温度 = 维度基础（云海 30 ℃ / `northstar:venus` 430 ℃ / 其它 20 ℃）+ 热源（熔岩 600、岩浆块 300、火 300、营火 200，最多 8 个）− 冷源（水 / 冰 / 蓝冰 / 雪块 / 有冷却剂的散热器各 −40，最多 8 个）；>400 ℃ 连续 1200 tick 停机，回落后 200 tick 恢复。北极星温度调节器在 ±16/±6 内 = 完全免过热 |
| 伤害管线（R1–R5） | 唯一入口 `LivingIncomingDamageEvent` → 8 步：无敌判定 → 分类（弹道 / 爆炸 / 近战 / 酸 / 环境 / 其它，环境直接 0）→ 乘数（板 0.05/1.0/1.2/0.8/0.5，本体 0.10 起，窗口开 1.0 关 0.05）→ 单次上限（9/40/30/20）→ 每人 20 tick 滑动窗口（13/45/30）→ 先扣护甲板（溢出 ×0.30 进本体）→ 弱点命中累计过载 → 固定减伤 0.2 |
| 板 / 弱点 / 过载 | 护甲板：涅斐勒 4×120、赫菲斯托斯 6×150、阿佛洛狄忒 4×200；弱点按阶段暴露（过载与窗口期无条件暴露），命中 +1.0（弹道）/ 0.5（近战）/ 0.7（其它）；过载到 8 点触发 120 tick 过载 + 80 tick 窗口，内部冷却 1200 tick |
| 阶段与多人缩放 | 血量比 0.66 / 0.33 分三档（涅斐勒 0.60 / 0.25），换阶段 30 tick 无敌演出 + 开窗口；每 100 tick 按 64 格内玩家人数上调血量（每人 +50%） |
| Boss 招式 | 涅斐勒：云爆弹 / 尾扫 / 召唤云帆鳐 / 俯冲；赫菲斯托斯：熔岩拳 / 熔渣投掷 / 蒸汽喷发（喷口被堵 ≥1 个就开窗口）/ 地震踏 / 过载爆发 + P3 热浪；阿佛洛狄忒：酸浪 / 酸针 / 召唤酸潮水母 / 硫火环 / 高压冲击波 / 结茧（护壁 240、破茧开 160 tick 窗口）/ P3 变形 + 4 分钟超时 999 点 |
| 召唤链 | `whale_bone_horn`→涅斐勒、`pressure_valve`→赫菲斯托斯、`sulfur_heart`→阿佛洛狄忒；64 格内已有 Boss 则拒绝 |
| 护甲套装 | 钨钢：酸蚀 ×0.7 / ×0.8；云行者：雷击 ×0.5、潜行二段跳（3 秒冷却）；熔管重甲：酸蚀与火 ×0.5、岩浆内给抗火；启明星：露天每 100 tick +1 吸收（上限 20）、每 600 tick 腐蚀 −20 并移除失明 |
| 压力等级 | 钨钢 ≥2 件 100 atm、熔管 120、启明星 130，带 `pressure_compensator` +20；`northstar:venus` 下 <100 每 400 tick 扣 1 点 |
| 腐蚀与云海坠落 | 玩家腐蚀值 0–100（存 `PersistentData`）：酸雨暴露每 20 tick +2 并 1 点酸伤害，否则 −1；随后按 `min(4, corrosion/20)` 给腐蚀效果。云海 y<60 传送回 200 并扣 20% 最大生命 |

### 5.5 客户端

5 个自建 `EntityModel` + 11 个渲染器（6 种生物复用原版渲染器）、`MachineScreen`（176×166）、
右上角气压计条（5 秒缓存过期即消失）与左上角弱点计数、硫酸流体贴图与 tint `0xFFB7E04A`。

---

## 6. 四条内容线之间的约定与隔离

四条内容线共用一个 jar、一条 mod bus、一条 game bus、一套配置目录，因此按下列约定分工：

| 关注点 | 约定 | 结果 |
|---|---|---|
| 注册名 | 一律 `<前缀>_<名字>`（月球的少数基础方块沿用北极星风格命名，如 `dreamsleep_grass`） | 12 个注册表（block / item / entity_type / fluid / feature / armor_material / menu / recipe_type / recipe_serializer / biome_source / damage_type / sound_event）路径两两不重复 |
| 创造标签页 | 各线一个：`moon` / `mercury` / `mars` / `venus` | 标签 ID 与标题键 `itemGroup.northstar_planet_addon.<前缀>` 一一对应 |
| 效果命名 | 火星 `corrosion`（腐蚀）、金星 `acid_corrosion`（酸蚀） | 两个不同的酸类效果各自独立，语言键同名对应 |
| 资源与数据 | 共用 `assets/northstar_planet_addon/` 与 `data/northstar_planet_addon/`；向原版与北极星的标签追加放 `data/minecraft/`、`data/northstar/` | `lang/en_us.json`、`lang/zh_cn.json`、`sounds.json` 是三个共用文件，条目按 ID 排序维护，四条内容线各写各的前缀 |
| 群系注入 | 月球 / 水星打 `northstar:moon` 与 `northstar:mercury`，火星打 `northstar:mars`，金星是自己维度的群系源 | 目标维度两两不同，互不覆盖 |
| Mixin | 5 个全是 `@Shadow` 字段桥接 + 1 个 `@ModifyVariable`，月球与水星的接口方法前缀不同（`nma$` / `mna$`）且分在两个配置里 | 不存在抢同一调用点的风险 |
| 网络包 | 太阳风暴（水星）、沙尘暴 / 换弹（火星）、气压计（金星） | 共用命名空间但路径不同，互不覆盖 |
| 天气 | 太阳风暴与沙尘暴都是自建服务端状态（`SavedData` / 静态字段），维度内生效 | 不碰原版 `Level` 天气，两套天气可以同时存在 |
| 客户端雾 / HUD | 每个处理器先判维度：水星雾只在水星、火星雾只在火星；火星 HUD 只在沙尘暴时出现，金星 HUD 只在有 Boss / 气压计数据时出现 | 同一屏最多只有一条线的覆盖层 |
| 按键 | 全模组只有一个 `KeyMapping`（火星 `R` 换冷却器） | 分类键统一 `key.categories.northstar_planet_addon` |
| 事件订阅 | 火星与金星的 `@EventBusSubscriber` 全部指向 `NorthstarPlanetAddon.MODID` | 监听器能挂到本模组上 |
| 同名工具类 | 各线自己的 `ModBlocks` / `ModTabs` / `ModEffects` / `RefreshableBiomeSource` 留在各自包内 | 类名相同但互不引用 |

三个 Boss 体系各自独立：火星靠核心温度与护盾，月球靠阶段与技能池，金星靠护甲板 + 弱点 + 过载。
金星的「反枪械宪章」只对 `AbstractBossEntity` 生效，与阿瑞斯、梦眠月鹿无关。

---

## 7. 美术资源约定

| 资源 | 路径 | 规格 |
|---|---|---|
| 物品图标 | `assets/northstar_planet_addon/textures/item/<名字>.png` | 16×16 或 32×32；模型为 `minecraft:item/generated`，`layer0` 指向同名贴图 |
| 盔甲穿戴层 | `assets/northstar_planet_addon/textures/models/armor/<材质>_layer_1.png` / `_layer_2.png` | 64×32，按原版人形 UV 布局（layer_1 = 头盔 + 胸甲 + 靴子，layer_2 = 护腿），UV 与 `ArmorMaterial.Layer("northstar_planet_addon:<材质>")` 对应 |
| 生物皮肤 / 模型 | `textures/entity/`、`geo/`、`animations/` | 原版 `HierarchicalModel` 皮肤 64×64；GeckoLib 的 geo + animation 各自成对 |

**启明星战甲（`phosphoros`）** 的当前美术：物品图标 32×32（珍珠白甲板 + 金色描边 + 磷光青点缀，头盔带冠饰与护目槽，
胸甲带领甲 / 肩甲 / 核心晶石，护腿带腰封与膝甲，战靴带翻口与鞋尖护片）；
穿戴层 `phosphoros_layer_1/2.png` 为 64×32 双层贴图，按「护面板 + 金色包边 + 磷光缝线」绘制，
layer_1 覆盖头盔（冠顶星纹 / 面罩护目槽 / 颊甲通气口 / 颈甲）、胸甲（肩甲 / 胸板核心 / 腰带）与靴子（膝甲 / 胫甲 / 鞋底），
layer_2 覆盖护腿（腰封 + 腿甲 + 膝甲）与胯甲。

**梦眠月鹿（`moon_deer`）** 走的是「Blockbench 建模 → 脚本生成」的流水线，工具全部在 `build/art/`：

| 文件 | 作用 |
|---|---|
| `moon_deer.bbmodel` | Blockbench 源工程（Modded Entity 格式，53 骨骼 / 213 立方体 / 256×256 贴图） |
| `moon_deer.geo.json` | 从 Blockbench 导出的 Bedrock 几何（骨骼 pivot / 旋转 / 每块 box UV） |
| `gen-moon-deer-java.ps1` | 几何 → Java：坐标映射（+Y 朝上 → 原版 +Y 朝下、地面 y=24、X 镜像）、父级相对偏移、`° → rad`、`CubeDeformation` |
| `moon-deer-anim.java.txt` | 手写动画体（三层叠加 + 8 技能分段缓动 + 受击 / 倒地），生成时拼到几何后面 |
| `painter3.ps1` | 按导出的 UV 表逐面绘制 `moon_deer.png` 与 `moon_deer_glow.png`：每个面沿纵向做「亮 → 暗」色阶渐变，两档之间用 4×4 Bayer 有序抖动过渡（不是散点）；只有眼睛 / 神眼 / 月纹 / 花簇 / 冰刃尖保留刻意的发光小块 |
| `audit-deer-java.ps1` | 用 Java 的 `ModelPart` 变换顺序复算包围盒与各部位位置，核对导出 → Java 的坐标映射 |
| `measure-deer-parts.ps1` | 按骨骼分组输出世界空间包围盒（单位：格，原点在蹄、面朝 -z），`MoonDeerPart` 的尺寸与偏移就是照它定的 |
| `verify-deer-hitbox.ps1` | 把「主干盒子 + 7 个部件」回代，逐块取立方体角点算判定区覆盖率，改完判定区跑一遍确认没有漏掉部位 |

因为 `MoonDeerModel` 是生成物，改动顺序固定为：Blockbench 改模型 → 导出 `moon_deer.geo.json` →
跑 `gen-moon-deer-java.ps1` → 跑 `painter3.ps1`（UV 变了必须重画）→ 跑 `audit-deer-java.ps1` 核对 → `gradlew build`。

---

## 8. 目录结构

```
NorthstarPlanetAddon/
├── build.gradle              NeoForge moddev 工程；libs/*.jar 只 compileOnly + runtimeOnly
├── gradle.properties         MC / NeoForge / 依赖区间 / mod 元数据
├── settings.gradle
├── gradlew(.bat) + gradle/   Gradle 9.2.1 wrapper
├── libs/                     开发期联调 jar（northstar / create / flywheel / ponder / Registrate / geckolib）
├── docs/                     逐内容线的源码级笔记（含全部数值与类名出处）与 runserver 冒烟测试日志
├── tools/
│   ├── regen-module-packages.ps1   按内容线重新生成模块内容包（除入口类）
│   └── regen-resources.ps1         重建 resources：并集写入 + 命名空间与前缀归一
└── src/main/
    ├── java/com/suwako/northstarplanetaddon/
    │   ├── NorthstarPlanetAddon.java        唯一 @Mod 入口
    │   ├── moon/       MoonModule + block/client/entity/fluid/item/mixin/registry/world
    │   ├── mercury/    MercuryModule + block/client/command/effect/entity/mixin/network/registry/world
    │   ├── mars/       MarsModule + block/client/command/effect/entity/event/item/network/registry/world
    │   └── venus/       VenusModule + block/blockentity/client/combat/command/compat/config/effect/
    │                    entity/event/item/machine/menu/network/registry/world
    └── resources/
        ├── META-INF/neoforge.mods.toml      依赖声明 + 两个 [[mixins]]
        ├── northstar_planet_addon.moon.mixins.json
        ├── northstar_planet_addon.mercury.mixins.json
        ├── pack.mcmeta
        ├── assets/northstar_planet_addon/   animations / blockstates / geo / lang /
        │                                    models / sounds(+sounds.json) / textures
        ├── data/northstar_planet_addon/     advancement / damage_type / dimension /
        │                                    dimension_type / loot_table / neoforge / recipe /
        │                                    structure / tags / worldgen
        ├── data/northstar/                  向北极星标签追加的 17 个文件
        └── data/minecraft/                  向原版标签追加的 25 个文件
```

`assets/` 与 `data/` 下只有一个属于本模组的命名空间目录；新增内容直接往 `northstar_planet_addon` 里放，用前缀区分归属。

---

## 9. 构建与运行

```powershell
# Windows：需要 JDK 21（Gradle 用 17 起也能跑，但 toolchain 要能解析到 21）
$env:JAVA_HOME = "C:\Users\<你>\.jdks\jdk-21.0.12.1+1"

cd E:\mc\ModCreator\NorthstarPlanetAddon
.\gradlew.bat build          # 产物：build/libs/northstar_planet_addon-1.0.0.jar
.\gradlew.bat compileJava    # 只编译，快速验错
.\gradlew.bat runClient      # 开发客户端（libs/ 里的联调模组会自动进运行时类路径）
.\gradlew.bat runServer      # 开发服务端（--nogui）
.\gradlew.bat runData        # 数据生成（输出到 src/generated/resources）
```

要点：

- 首次构建会下载 NeoForge 21.1.248 与 NeoForm 产物，需要联网；之后可离线。
- `libs/*.jar` 是 `compileOnly` + `runtimeOnly`，**不会被打进 jar**（`build/libs` 里只有本模组）。
- jar 内清单登记了 `MixinConfigs: northstar_planet_addon.moon.mixins.json,northstar_planet_addon.mercury.mixins.json`，
  `neoforge.mods.toml` 另有两条 `[[mixins]]`（供 ModDev 从类目录加载时使用）。

### 9.1 本机验证记录（可复现）

`.\gradlew.bat build` **BUILD SUCCESSFUL**，产物 `build/libs/northstar_planet_addon-1.0.0.jar` 约 5.95 MB /
1132 个条目：`assets/` 下只有 `northstar_planet_addon` 一个命名空间，`data/` 下是
`northstar_planet_addon`、`northstar`、`minecraft` 三个，外加 5 个 Mixin 类。

`.\gradlew.bat runServer`（`run/eula.txt` 置 `eula=true`，联动模组由 `libs/` 提供）**启动成功**：

```
Mod List: Create 6.0.10 (create) / Flywheel 1.0.6 / GeckoLib 4 4.9.3 (geckolib) /
          Northstar Planet Addon 1.0.0 (northstar_planet_addon) / Northstar Redux 0.6.5+1.21.1 (northstar) / Ponder 1.0.82
[mixin]: Compatibility level set to JAVA_21
[moon] / [mercury] / [mars] / [venus] 四行模块装载日志
Injected 2 entries into northstar:moon by partitioning northstar:lunar_plains weirdness [-0.9333, -0.7666]:
    northstar_planet_addon:open_hot_spring [-0.9333, -0.8666], plains [-0.8666, -0.85],
    northstar_planet_addon:moon_forest [-0.85, -0.8], plains [-0.8, -0.7666].
Injected northstar_planet_addon:dry_riverbed into northstar:mercury by partitioning
    northstar:mercury_hills's continentalness [-1.0, 1.0]: hills [-1.0, -0.23], dry riverbed [-0.23, -0.09], hills [-0.09, 1.0].
Injected 3 Mars biomes into northstar:mars (re-boxed 3 Northstar surface biomes)
Done (8.099s)! For help, type "help"
```

即：注册表全部就位、两个 Mixin 配置都按 `JAVA_21` 生效、三个群系注入（月球 2 条 / 水星 1 条 / 火星 3 群系）
全部成功、数据包（含 `northstar_planet_addon:venus_clouds` 维度）全部解析通过。完整日志见 `docs/runserver-smoke-test.log`。

这次冒烟测试同时抓出并修掉了一个启动崩溃：`machine/ModRecipes` 原先在静态初始化里直接
`Registry.register(BuiltInRegistries.RECIPE_TYPE, …)`，而 `BuiltInRegistries.RECIPE_TYPE` 在模组构造期早已冻结，
会抛 `IllegalStateException: Registry is already frozen` 导致整个模组加载失败。现在配方**类型**也走
`DeferredRegister`（`ModRecipes.RECIPE_TYPES`），使用点通过 `ModRecipes.highPressureType()` 在注册表就绪后取值。

日志里仍会出现三类**不影响启动**的提示：

| 日志 | 含义 |
|---|---|
| `Couldn't load tag northstar_planet_addon:ballistic as it is missing following references` | 没装 TaCZ 时该标签引用的 `tacz:*` 伤害类型不存在，标签整条加载失败 → `#northstar_planet_addon:ballistic` 为空，弹道判定退化为 `#minecraft:is_projectile` 兜底（`TaczCompat` 会打印解出的条数） |
| `Detected northstar_planet_addon:mushroom_moth ... AMBIENT mob category but was added under CREATURE` | 金星 `fungal_forest` 群系把 `mushroom_moth` 挂在 creature 分类下，与实体注册的 `AMBIENT` 不一致（只影响刷怪上限统计） |
| `The following entities have not registered to the RegisterSpawnPlacementsEvent` | 金星的 6 种生物（`cloud_ray` / `mushroom_moth` / `lava_worm` / `frost_stalker` / `spore_walker` / `acid_jelly`）没有注册刷怪放置规则，等于「落点不受限」 |

**没有验证到的部分**：`runClient` 未做可视化核对——模型 / 贴图 / 雾效 / HUD / 按键手感的实际观感必须进游戏确认。
公共侧（注册表、Mixin、群系注入、数据包、网络包注册）已由上面这次 `runServer` 覆盖。

---

## 10. 已知问题与待办

以下均来自源码核对，数值与实现保持现状。

**月球**

1. 群系注入强绑定北极星 0.6.5 的气候盒（要求存在 weirdness 恰为 `[-0.9333,-0.7666]` 的 `northstar:lunar_plains`）。
   北极星改参数后注入会整体放弃并打 error，月亮保持原样。
2. 梦眠月鹿的战斗状态（阶段 / 冷却 / `meteorUsed`）不写存档，区块卸载后回到第一阶段。
3. 祭坛一次性、召唤后 40 tick 自毁且不掉落；只在夜间（或固定时间维度的 `[13000,23000)`）可用。
4. 结构模板在 `data/northstar_planet_addon/structure/dreamsleep_alter.nbt`（路径是小写 `alter`，不是 `altar`），
   `MoonPearlItem` 里的模板键与之对应，改名需两处同步。
5. 温泉 `canConvertToSource(true)`：两个相邻源 + 实心底面即可再生，需要限制时得取消 `CreateFluidSourceEvent`。

**水星**

1. 群系注入要求存在大陆度恰为 `[-1.0,1.0]` 的 `northstar:mercury_hills`；不满足时干涸海床静默不生成。
2. 陨石倒计时 `meteorCountdown` 是瞬态静态字段，服务器重启后重新从 200 tick 计。
3. 5 个伤害类型是数据包条目，`getHolderOrThrow` 解析，包被移除会抛异常（无兜底）。
4. 两种太阳鱼是 `CREATURE` 分类但实现了 `Enemy`：占生物上限而非怪物上限，和平难度会被移除。

**火星**

1. 群系注入是反射 + 一次性（`LevelEvent.Load` 时执行，不支持热重载）；北极星若改群系源结构会整体放弃。
2. `MarsCraterFeature.MAX_RADIUS` 实为 16（注释写 8），`lava_crater` / `ice_crater` 用半径 16 时接近 24000 次写入预算。
3. `volcanic_spire` / `ice_spire` 的 configured + placed 数据完整，但**没有任何群系引用**，当前不会生成。
4. 恶魔蛾免疫一切爆炸伤害（为了不让阿瑞斯自爆伤到召唤物），副作用是 TNT 与苦力怕也伤不到它。
5. 阿瑞斯护盾期 `hurt()` 返回 false，因此不能用 `kill()` 收尾（自爆走 `setHealth(0)` + `die`）。
6. 沙尘暴与星核炮击都是进程内静态状态，不写存档。
7. `anti_corrosion_coating` 目前只是冷却器的合成材料，没有运行时效果；耐腐蚀来自 3 件套判定。

**金星**

1. Create 应力注册入口未实现（`CreateCompat.isActive()` 恒 false），气动马达靠自建 `KineticBlockEntity` 挂在动力网上；
   全模组只有高压锅与高压熔炉消费机械功。
2. `#northstar_planet_addon:fungus` 与 `#northstar_planet_addon:boss_mechanic` 两个标签文件为空（后者代码里也无引用）。
3. 已注册但无效果：模块 `heat_sink`（tooltip 写火伤 −25%，代码无分支）、数据组件 `freshness` / `corrosion`。
4. 6 种生物（`spore_walker`、`mushroom_moth`、`frost_stalker`、`thunder_chick`、`lava_worm`、`brass_deacon`）只有语言键、没有刷怪蛋。
5. 季风只有气压计倒计时文本；酸雨依赖 `isRainingAt`，而云海群系 `has_precipitation=false`，实际是否触发**未在游戏内确认**。
6. 三个 Boss 的招式伤害走 `mobAttack` / `onFire` / `magic`，不过 `#northstar_planet_addon:boss_mechanic` 管线（管线只作用于 Boss **受到**的伤害）。
7. 8 个结构是 feature 手写摆放，没有 NBT 模板与 `structure_set`，尺寸小于原设计草案。
8. `lava_tube_galleries` 群系本身没有结构（`lava_tube_city` 挂在另外 3 个群系里）。

**跨内容线**

1. 新增注册项请遵守 `<前缀>_<名字>` 命名，并在对应 `DeferredRegister` 里注册；重名会在加载期直接报错。
2. 群系注入的三处实现（月球 `nma$`、水星 `mna$`、火星反射）都以北极星当前的气候参数盒为锚点，
   升级北极星后请同时复核注入日志。

---

## 11. 作者与许可

- 作者：Suwako、Vitran、Phosphoros Team。
- 许可：`All Rights Reserved`（如需改为 MIT 或双许可，请调整 `gradle.properties` 的 `mod_license` 并补充许可文件）。
- 目标模组是 [Create: Northstar Redux](https://modrinth.com/mod/create-northstar)（modid `northstar`），
  本模组不含它的任何代码，运行时依赖它的维度、方块、群系与标签。
