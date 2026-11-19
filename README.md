# 北极星星球附属 / Northstar Planet Addon

给 **Create: Northstar Redux（北极星，modid `northstar`）** 的**月球 / 水星 / 火星 / 金星**四个星球扩展。
本工程把原先各自独立的四个附属模组（月球、水星、火星、金星「Create: Phosphoros」）**合并成一个模组**：
一个 jar、一个模组条目、一份依赖声明，四个星球的内容与玩法全部保留。

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
| 许可 | All Rights Reserved（金星模块的原始工程为 MIT，见 §12） |

---

## 目录

0. [合并了什么](#0-合并了什么)
1. [合并后的架构](#1-合并后的架构)
2. [北极星月球附属（`moon`）](#2-北极星月球附属前缀-moon)
3. [北极星水星附属（`mercury`）](#3-北极星水星附属前缀-mercury)
4. [北极星火星附属（`mars`）](#4-北极星火星附属前缀-mars)
5. [北极星金星附属（`venus`）](#5-北极星金星附属前缀-venus原-create-phosphoros)
6. [四个星球会不会互相打架](#6-四个星球会不会互相打架)
7. [目录结构](#7-目录结构)
8. [构建与运行](#8-构建与运行)
9. [从四个 jar 迁移到本模组](#9-从四个-jar-迁移到本模组)
10. [已知问题与待办（按模块汇总）](#10-已知问题与待办按模块汇总)
11. [合并过程中改了什么 / 没改什么](#11-合并过程中改了什么--没改什么)
12. [来源与许可](#12-来源与许可)

---

## 0. 合并了什么

| 模块 | 对外名称（中 / 英） | 原工程目录 | 原命名空间 | 单一命名空间下的前缀 | 目标星球 / 维度 | 内容规模（源码实测） |
|---|---|---|---|---|---|---|
| 月球 | 北极星月球附属 / Northstar Moon Addon | `NorthstarMoonAddon` | `northstar_moon_addon` | `moon` | `northstar:moon` | 12 方块 / 21 物品 / 5 实体 / 2 流体 / 2 群系 |
| 水星 | 北极星水星附属 / Northstar Mercury Addon | `NorthstarMercuryAddon` | `northstar_mercury_addon` | `mercury` | `northstar:mercury` | 1 方块 / 5 物品 / 3 实体 / 1 效果 / 1 群系 |
| 火星 | 北极星火星附属 / Northstar Mars Addon | `NorthstarMarsAddon` | `northstarmarsaddon` | `mars` | `northstar:mars` | 6 方块 / 25 物品 / 2 实体 / 2 效果 / 3 群系 |
| 金星 | 北极星金星附属 / Northstar Venus Addon | `phosphoros` | `phosphoros` | `venus` | `northstar_planet_addon:venus_clouds`（自定义维度） | 17 方块 / 10 方块实体 / 83 物品 / 11 实体 / 5 群系 / 8 结构 |

原来的四个工程**保持不动**，本工程是它们的合并产物，可以单独构建、单独发布。

**关键设计：一个模组、一个命名空间、四个前缀。**

- 模组 ID 与注册命名空间都是 `northstar_planet_addon`，四个星球附属用路径前缀区分解锁：
  `northstar_planet_addon:moon_rabbit`、`northstar_planet_addon:mercury_sand`、
  `northstar_planet_addon:ares`、`northstar_planet_addon:pressure_tank`；
- 创造模式标签页各用一个 ID：`northstar_planet_addon:moon` / `mercury` / `mars` / `venus`；
- 语言键统一成 `itemGroup.<命名空间>.<前缀>` 这类形式，四套命名空间合进同一个
  `assets/northstar_planet_addon/` 与 `data/northstar_planet_addon/`；
- 中文名统一为「北极星 ×× 附属」，英文统一为「Northstar ×× Addon」；**「启明星」保留为金星模块的
  素材 / 装备品牌名**（启明星核、启明星战甲、启明星祭坛、启明星套餐……）。

> ⚠️ 命名空间统一是**破坏性变更**：旧存档里 `northstarmarsaddon:molten_core_helmet` 这类 ID 不会被识别。
> 升级路径与映射表见 [§9](#9-从四个-jar-迁移到本模组)。

---

## 1. 合并后的架构

### 1.1 一个入口，四个模块类

```java
@Mod(NorthstarPlanetAddon.MODID)
public final class NorthstarPlanetAddon {
    public static final String MODID = "northstar_planet_addon";

    public NorthstarPlanetAddon(IEventBus modBus, ModContainer container) {
        MoonModule.init(modBus);          // 月球：注册表 + 事件 + 客户端接线
        MercuryModule.init(modBus);       // 水星：注册表 + 事件 + 网络 + 客户端接线
        MarsModule.init(modBus);          // 火星：8 个 DeferredRegister（其余走 @EventBusSubscriber）
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

### 1.2 包名映射与命名约定

| 原包 | 现包 |
|---|---|
| `com.suwako.northstarmoonaddon` | `com.suwako.northstarplanetaddon.moon` |
| `com.suwako.northstarmercuryaddon` | `com.suwako.northstarplanetaddon.mercury` |
| `com.vitran.northstarmarsaddon` | `com.suwako.northstarplanetaddon.mars` |
| `com.phosphoros` | `com.suwako.northstarplanetaddon.venus` |

原入口类 `NorthstarMoonAddon` / `NorthstarMercuryAddon` / `NorthstarMarsAddon` / `Phosphoros` 分别改名为
`MoonModule` / `MercuryModule` / `MarsModule` / `VenusModule`，并去掉 `@Mod`；
每个模块类只暴露两个常量：

```java
public static final String NAMESPACE = NorthstarPlanetAddon.MODID;  // 统一命名空间
public static final String MODULE    = "moon";                      // 本模块前缀
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

资源与数据的目录也随之统一：四套 `assets/<旧命名空间>/` 合并成一份 `assets/northstar_planet_addon/`，
四套 `data/<旧命名空间>/` 合并成一份 `data/northstar_planet_addon/`（`data/minecraft/` 与
`data/northstar/` 是向原版与北极星的追加，保持原名）。

### 1.3 Mixin：两个配置，五个 Mixin

月球与水星原本各有一套「群系源注入」Mixin，合并后按模块拆成两个配置（保持与原来完全一致的应用方式，
两者 `@Shadow` 的字段相同但接口方法前缀不同：月球 `nma$`、水星 `mna$`，互不冲突）：

| 配置 | package | Mixin | 目标类 | 手段 |
|---|---|---|---|---|
| `northstar_planet_addon.moon.mixins.json` | `…moon.mixin` | `BiomeSourceMixin` | `BiomeSource` | `@Shadow @Final @Mutable possibleBiomes`，实现 `nma$refreshPossibleBiomes()` |
| | | `MultiNoiseBiomeSourceMixin` | `MultiNoiseBiomeSource` | `@Shadow @Final @Mutable parameters`，实现 `nma$get/setClimateEntries()` |
| `northstar_planet_addon.mercury.mixins.json` | `…mercury.mixin` | `BiomeSourceMixin` | `BiomeSource` | 同结构，方法前缀 `mna$` |
| | | `MultiNoiseBiomeSourceMixin` | `MultiNoiseBiomeSource` | 同结构，方法前缀 `mna$` |
| | | `PlayerExhaustionMixin` | `Player` | `@ModifyVariable(method = "causeFoodExhaustion(F)V", at = HEAD)`，沐浴阳光时饥饿消耗 ×0.4 |

全部 Mixin 都是 `@Shadow` 字段桥接或 `@ModifyVariable`，**没有一个 `@Redirect`**。
两个配置同时登记在 `neoforge.mods.toml` 的 `[[mixins]]`（开发运行）与 jar 清单的 `MixinConfigs`（成品包，逗号分隔）。

火星与金星**没有 Mixin**：火星的群系注入走反射，金星的伤害管线走事件。

### 1.4 共享数据文件的合并（并集）

四个模块里有 5 个数据文件落在同一路径（`data/minecraft/…` 与 `data/northstar/…`），合并时按**并集**写入，
而不是后复制者覆盖：

| 文件 | 合并后内容 |
|---|---|
| `data/minecraft/tags/block/mineable/shovel.json` | `northstar_planet_addon:dreamsleep_grass`、`moon_soil`、`mercury_sand` |
| `data/northstar/tags/entity_type/can_survive_cold.json` | 月兔、月壳蟹、月鹿、太阳鱼、炽烈太阳鱼、恶魔蛾、阿瑞斯 |
| `data/northstar/tags/entity_type/can_survive_heat.json` | 太阳鱼、炽烈太阳鱼、恶魔蛾、阿瑞斯 |
| `data/northstar/tags/entity_type/doesnt_require_oxygen.json` | 同上七个 |

同理，合并进同一命名空间后有两处**注册表重名**必须改名，否则加载即崩：

| 冲突 | 处理 |
|---|---|
| 四个模块的创造模式标签页都叫 `main` | 改成 `moon` / `mercury` / `mars` / `venus` |
| 火星与金星都注册了效果 `corrosion` | 火星保留 `northstar_planet_addon:corrosion`（腐蚀）；金星改为 `northstar_planet_addon:acid_corrosion`（酸蚀，语言键同名调整） |

其余 `data/minecraft/**`、`data/northstar/**` 是各模块独占的文件（`northstar:tags/worldgen/biome/moon_biomes`、
`mercury_biomes`、`mars_biomes`、`has_mars_base`、`moon_blocks`、`mercury_stone_replaceable` 等），原样保留。

### 1.5 依赖与开发期联调 jar

`neoforge.mods.toml` 声明的依赖：

| mod | 类型 | 区间 | 理由 |
|---|---|---|---|
| `neoforge` / `minecraft` | required | `[21.1,)` / `[1.21.1]` | — |
| `northstar` | required | `[0.6.5,)` | 四个模块的目标维度、方块与群系都来自它 |
| `create` | required | `[6.0,)` | 火星机械腐蚀 / 星核炮摧毁动力方块；金星气动马达与 WorkConsumer |
| `geckolib` | required | `[4.7,5.0)` | 火星恶魔蛾与阿瑞斯的 geo/animation |
| `curios` | optional | `*` | 金星弹道校准器槽位，缺失时改为背包内生效 |
| `tacz` | optional | `*` | 金星只按伤害类型标签识别弹道（TaCZ 版本形如 `1.1.8-hotfix-r6`，区间只能写 `*`） |

`libs/` 里的 6 个 jar（northstar、create、flywheel、ponder、Registrate、geckolib）只作为
`compileOnly` + `runtimeOnly` 使用：**不会打进发布 jar，也不会变成玩家的硬依赖声明**。
代码层对它们的引用全部是注册名 / 标签 / 反射（少数桥接类只在反射命中后才被类加载，见 §5.1）。

### 1.6 配置文件

| 文件 | 来源模块 |
|---|---|
| `config/northstar_planet_addon-venus-common.toml` | 金星（战斗乘数 / 上限 / Boss / 世界 / 机器 / 兼容），键名与原 `phosphoros-common.toml` 一致 |

其余三个模块没有配置项，参数写死在常量里（详见各模块章节）。

---

## 2. 北极星月球附属（前缀 `moon`）

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

三个生物都是**原版 `HierarchicalModel` + 64×64 皮肤**（不用 GeckoLib）。兔 / 蟹的骨骼动画定义在 `NMAAnimations`
（idle / hop / pinch / throw / walk / gaze），鹿在 `setupAnim` 里按技能硬编码姿态。
`NMAAnimationResources` 是资源重载监听器，可热替换 `assets/<ns>/animations/**/*.json` 的两份兔动画。
月林背景音乐仅在 `northstar:moon` + `moon_forest` 群系播放（随机 200–3600 tick），月鹿存活或离开维度立即停曲。

---

## 3. 北极星水星附属（前缀 `mercury`）

**定位**：给 `northstar:mercury` 加太阳风暴天气、水星沙、干涸海床群系、太阳鱼 / 炽烈太阳鱼与「沐浴阳光」效果。

### 3.1 注册内容

| 类别 | 内容 |
|---|---|
| 方块 1 | `mercury_sand`（`FallingBlock`，可以熔炼 / 高炉成玻璃） |
| 物品 5 | 水星沙、太阳鱼片（2 / 0.1）、太阳鱼果烩（5 / 0.6，食用给 2 分钟沐浴阳光）、2 个刷怪蛋 |
| 实体 3 | `meteor`（MISC，物理下落）、`sunfish`（12 HP / 3 攻击）、`blazing_sunfish`（36 HP / 护甲 6 / 8 攻击，火免疫） |
| 效果 1 | `sunbath`：移速 +30%（`ADD_MULTIPLIED_TOTAL`）、饥饿消耗 ×0.4 |
| 伤害类型 5 | `solar_storm`、`meteor`、`sunfish_ram`、`sunfish_flame`、`sunfish_beam`（全部 `burning` 特效） |
| 音效 4 | 全部复用北极星已有 ogg（`martian_duststorm_above`、`laser_burn*`、`rocket_blast*`） |
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

## 4. 北极星火星附属（前缀 `mars`）

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

## 5. 北极星金星附属（前缀 `venus`，原 Create: Phosphoros）

**定位**：独立的云海维度 `northstar_planet_addon:venus_clouds` + 气动 / 压力机器体系 + 四套护甲 / 五把武器 / 四把工具 +
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
| 效果 / 流体 / 组件 / 材质 | 1 / 2 / 3 / 4 | 腐蚀（护甲 −1 每级，周期 40>>amp tick 掉血）、硫酸（density 1400、`canDrown`）、`freshness`/`corrosion`/`charge`、钨钢 / 云行者 / 熔管重甲 / 启明星 |
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

## 6. 四个星球会不会互相打架

合并后四个模块共用一个 jar、一条 mod bus、一条 game bus、一套配置目录。逐项检查过：

| 可能的冲突点 | 现状 | 为什么没问题 |
|---|---|---|
| 注册表重名 | **已处理两处** | 四个模块现在共用一个命名空间，所以逐类比对过注册路径：创造标签页 `main` ×4 → 改成 `moon`/`mercury`/`mars`/`venus`；效果 `corrosion` 火星与金星重名 → 金星改为 `acid_corrosion`。其余 10 个注册表（block / item / entity_type / fluid / feature / armor_material / menu / recipe_type / recipe_serializer / biome_source / damage_type / sound_event）路径两两不重复 |
| 资源 / 数据文件重名 | **已处理** | 四套 `assets/*` 与 `data/*` 合并成一份目录，逐文件比对过：只有 `lang/en_us.json`、`lang/zh_cn.json`、`sounds.json` 三处同名，按并集合并（详见 §1.4） |
| 群系注入互相覆盖 | 无 | 月球 / 水星打的是 `northstar:moon` 与 `northstar:mercury`，火星打 `northstar:mars`，金星是自己维度的群系源；目标维度两两不同 |
| Mixin 抢同一调用点 | 无 | 5 个 Mixin 全是 `@Shadow` 字段桥接 + 1 个 `@ModifyVariable`；月球与水星的接口方法前缀不同（`nma$` / `mna$`），且分在两个配置里 |
| 网络包 / 协议 | 无 | 三个载荷（水星风暴、火星沙尘暴、火星换弹、金星气压计）共用命名空间但路径不同，互不覆盖 |
| 天气类变量 | 无 | 太阳风暴（水星）与沙尘暴（火星）都是自建服务端静态 / `SavedData` 状态，维度内生效，不碰原版 `Level` 天气 |
| 客户端雾 / HUD | 无 | 各处理器都先判维度：水星雾只在水星、火星雾只在火星；火星 HUD 只在沙尘暴时出现，金星 HUD 只在有 Boss / 有气压计数据时出现 |
| 按键 | 只有一个 | 火星的 `R`（换冷却器）是唯一 `KeyMapping`，分类键统一为 `key.categories.northstar_planet_addon` |
| 常量 / 工具类重名 | 已隔离 | 模块之间同名类（如两个 `RefreshableBiomeSource`、两个 `ModBlocks`、两个 `ModTabs`、两个 `ModEffects`）都在各自包内，互不引用 |
| 事件订阅 | 已归一 | 火星与金星的 `@EventBusSubscriber` 全部指向 `NorthstarPlanetAddon.MODID`，否则 NeoForge 根本不会把这些监听器挂到本模组上 |

唯一需要留意的相邻关系：**北极星金星附属的「反枪械宪章」只管 `AbstractBossEntity`**，与火星阿瑞斯、月球梦眠月鹿无关；
三个 Boss 体系各自独立（火星靠核心温度与护盾，月球靠阶段与技能池，金星靠护甲板 + 弱点 + 过载）。

---

## 7. 目录结构

```
NorthstarPlanetAddon/
├── build.gradle              NeoForge moddev 工程；libs/*.jar 只 compileOnly + runtimeOnly
├── gradle.properties         MC / NeoForge / 依赖区间 / mod 元数据
├── settings.gradle
├── gradlew(.bat) + gradle/   Gradle 9.2.1 wrapper
├── libs/                     开发期联调 jar（northstar / create / flywheel / ponder / Registrate / geckolib）
├── docs/
│   ├── legacy-notes/         合并前逐模组的源码级梳理（旧命名空间下的事实，含全部数值与类名出处）
│   └── runserver-smoke-test.log
├── tools/
│   ├── regen-module-packages.ps1   从四个旧工程重新生成「模块内容包」（除入口类）
│   └── regen-resources.ps1         重建 resources：并集合并 + 命名空间统一 + 对外名称统一
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
        ├── assets/northstar_planet_addon/   四套资源合并：animations / blockstates / geo / lang /
        │                                    models / sounds(+sounds.json) / textures
        ├── data/northstar_planet_addon/     四套数据合并：advancement / damage_type / dimension /
        │                                    dimension_type / loot_table / neoforge / recipe /
        │                                    structure / tags / worldgen
        ├── data/northstar/                  向北极星标签追加的 17 个文件（含 3 个并集合并过的）
        └── data/minecraft/                  向原版标签追加的 25 个文件（含 1 个并集合并过的）
```

`assets/` 与 `data/` 下只剩一个属于本模组的命名空间目录：资源与数据包按命名空间寻址，
统一之后「一个星球附属占一个命名空间」变成「四个星球附属共用一个命名空间、用路径区分」，
后续要加内容时直接往 `northstar_planet_addon` 里放即可，不必再新建目录。

---

## 8. 构建与运行

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
- 本机验证结果见下一节。

### 8.1 本机验证记录（可复现）

`.\gradlew.bat build` **BUILD SUCCESSFUL**，产物 `build/libs/northstar_planet_addon-1.0.0.jar` 约 5.94 MB /
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

这次冒烟测试同时抓出并修掉了一个**原工程里就存在的启动崩溃**：北极星金星附属的
`machine/ModRecipes` 在静态初始化里直接 `Registry.register(BuiltInRegistries.RECIPE_TYPE, …)` 注册配方类型，
而 `BuiltInRegistries.RECIPE_TYPE` 在模组构造期早已冻结，会抛
`IllegalStateException: Registry is already frozen`，导致整个模组加载失败。
现在配方**类型**也走 `DeferredRegister`（`ModRecipes.RECIPE_TYPES`），使用点通过
`ModRecipes.highPressureType()` 在注册表就绪后取值。

日志里仍会出现三类**不影响启动**的提示（都来自原有数据，不是合并引入的）：

| 日志 | 含义 |
|---|---|
| `Couldn't load tag northstar_planet_addon:ballistic as it is missing following references` | 没装 TaCZ 时该标签引用的 `tacz:*` 伤害类型不存在，标签整条加载失败 → `#northstar_planet_addon:ballistic` 为空，弹道判定退化为 `#minecraft:is_projectile` 兜底（`TaczCompat` 会打印解出的条数） |
| `Detected northstar_planet_addon:mushroom_moth ... AMBIENT mob category but was added under CREATURE` | 金星 `fungal_forest` 群系把 `mushroom_moth` 挂在 creature 分类下，与实体注册的 `AMBIENT` 不一致（只影响刷怪上限统计） |
| `The following entities have not registered to the RegisterSpawnPlacementsEvent` | 金星的 6 种生物（`cloud_ray` / `mushroom_moth` / `lava_worm` / `frost_stalker` / `spore_walker` / `acid_jelly`）没有注册刷怪放置规则，等于「落点不受限」；要收紧需在北极星金星附属加 `RegisterSpawnPlacementsEvent` |

**没有验证到的部分**：`runClient` 未做可视化核对——模型 / 贴图 / 雾效 / HUD / 按键手感的实际观感必须进游戏确认。
公共侧（注册表、Mixin、群系注入、数据包、网络包注册）已由上面这次 `runServer` 覆盖。


---

## 9. 从四个 jar 迁移到本模组

统一命名空间是**破坏性变更**：旧存档里带旧命名空间的 ID 在新版本里不存在。分两种情况。

### 9.1 新存档 / 只要一个 jar

1. **删掉原来的四个 jar**：`northstar_moon_addon-*.jar`、`northstar_mercury_addon-*.jar`、
   `NorthstarMarsAddon-*.jar`、`phosphoros-*.jar`。新旧**不能同时装**——两者注册同一批 ID，会直接抛重复注册错误。
2. 放进 `northstar_planet_addon-1.0.0.jar`，确认 `northstar` / `create` / `geckolib` 在 `mods/` 里。
3. 起服后看日志确认四个星球附属都装上了（见 9.3）。

### 9.2 沿用旧存档：ID 映射表

| 旧 ID | 新 ID |
|---|---|
| `northstar_moon_addon:<path>` | `northstar_planet_addon:<path>` |
| `northstar_mercury_addon:<path>` | `northstar_planet_addon:<path>` |
| `northstarmarsaddon:<path>` | `northstar_planet_addon:<path>` |
| `phosphoros:<path>` | `northstar_planet_addon:<path>` |
| 效果 `phosphoros:corrosion` | `northstar_planet_addon:acid_corrosion`（火星的 `corrosion` 保持不变） |
| 维度 `phosphoros:venus_clouds` | `northstar_planet_addon:venus_clouds` |

规则很简单：**只换命名空间，路径部分不变**；只有金星的腐蚀效果因为与火星重名被改成了 `acid_corrosion`。

- 存档里的方块 / 物品 / 生物 / 群系都是按 ID 存盘的，所以旧存档里的这些内容会变成「未知 ID」。
  想继续用需要自己做一次 ID 重映射（数据修复工具或手工改 level.dat / 区块 NBT），**本模组不自带迁移器**；
- 维度改名后，旧的金星云海世界数据不会再被加载（会按新 ID 重新生成）；
- 旧 `config/phosphoros-common.toml` 不再被读取，新键在 `config/northstar_planet_addon-venus-common.toml`，
  键名一一对应，需要的话手工搬过去。

结论：**旧存档先备份**；接受「旧内容变成未知」，或先做 ID 重映射再升级。

### 9.3 装好后确认

```
[moon] 北极星月球附属 1.0.0：已装载，向 northstar:moon 注入月林与露天温泉群系（不需要改过的北极星 jar）。
[mercury] 北极星水星附属 1.0.0 已装载：太阳风暴、水星沙与干涸海床（northstar:mercury）。
[mars] 北极星火星附属：注册表已初始化（沙尘暴 / 阿瑞斯 / 火星钢 / 星核装备）
[venus] 北极星金星附属：护甲板 / 弱点 / 弹道伤害管线已装载（反枪械宪章 R1–R5）
北极星星球附属 / Northstar Planet Addon 1.0.0 已装载：月球 + 水星 + 火星 + 金星四个星球附属。
```

群系注入是否成功另有各模块自己的日志（成功时会打印切分后的气候带边界）。

---
## 10. 已知问题与待办（按模块汇总）

这些都来自源码核对，不是猜的；合并过程**没有**修改玩法数值，所以它们原样保留
（唯一的例外是 §8.1 里那个启动崩溃的配方类型注册）。

**月球**

1. 群系注入强绑定北极星 0.6.5 的气候盒（要求存在 weirdness 恰为 `[-0.9333,-0.7666]` 的 `northstar:lunar_plains`）。
   北极星改参数后注入会整体放弃并打 error，月亮保持原样。
2. 梦眠月鹿的战斗状态（阶段 / 冷却 / `meteorUsed`）不写存档，区块卸载后回到第一阶段。
3. 祭坛一次性、召唤后 40 tick 自毁且不掉落；只在夜间（或固定时间维度的 `[13000,23000)`）可用。
4. 结构模板在 `data/northstar_planet_addon/structure/dreamsleep_alter.nbt`（路径是小写 `alter`，不是 `altar`），`MoonPearlItem` 里的模板键与之对应，改名需两处同步。
5. 原月球 `zh_cn.json` 里 `broken_night_pearl` 键重复；合并时按 JSON 语义取后者，现在只剩一个键。
6. 温泉 `canConvertToSource(true)`：两个相邻源 + 实心底面即可再生，需要限制时得取消 `CreateFluidSourceEvent`。

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
8. `northstarmarsaddon` 工程里 `resources/assets/northstarmarsaddon/modelem/FungusShearsItem.java` 是误放的源码残留，
   合并时已排除（真正的 `FungusShearsItem` 在金星模块里）。

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

**合并层面**

1. 新旧四个 jar **不能同时装**（同 ID 重复注册）。
2. 命名空间统一是破坏性变更：旧存档里的 `northstarmarsaddon:*`、`phosphoros:*` 等 ID 不再存在，
   升级路径与映射表见 §9.2。
3. 四个模块的既有 `README.md`（各自工程内）部分描述早于当前源码（例如月球 README 说运行时注入已删除、
   太阳鱼片不能吃、阿瑞斯祭坛是 3×3×3 结构等），**以本文件与 `docs/legacy-notes/` 为准**。
   那四份笔记是合并前逐类逐 JSON 核对过的源码级梳理，本文件是它们的汇总版。
4. 原工程 `neo_version` 有 21.1.248 / 21.1.249 两种，合并统一到 **21.1.248**（同一分支，API 无差异）。
5. 金星配置文件名从 `phosphoros-common.toml` 改为 `northstar_planet_addon-venus-common.toml`，键名不变。
6. `docs/legacy-notes/` 的四份梳理是在**统一命名空间之前**做的，里面写的仍是旧命名空间的 ID；
   读的时候把 `northstar_moon_addon:` / `northstar_mercury_addon:` / `northstarmarsaddon:` / `phosphoros:`
   一律换成 `northstar_planet_addon:` 即可（金星腐蚀效果除外，现为 `acid_corrosion`）。

---

## 11. 合并过程中改了什么 / 没改什么

**改了什么**

| 项 | 变化 |
|---|---|
| mod id | 四个 → 一个：`northstar_planet_addon` |
| **注册命名空间** | 四个旧命名空间 → 统一为 `northstar_planet_addon`；资源与数据目录合并成 `assets/northstar_planet_addon` 与 `data/northstar_planet_addon` |
| **对外名称** | 统一为「北极星月球 / 水星 / 火星 / 金星附属」（英文 Northstar Moon / Mercury / Mars / Venus Addon）；「启明星」保留为金星素材名 |
| 创造标签页 ID | 四个 `main` → `moon` / `mercury` / `mars` / `venus`；标题键 `itemGroup.northstar_planet_addon.<前缀>` |
| 效果重名 | 金星的 `corrosion` → `acid_corrosion`（酸蚀），火星的 `corrosion`（腐蚀）不变 |
| 语言文件 | 四份 `lang/en_us.json` + `zh_cn.json` 合并成一份（各 355 条），`sounds.json` 合并成一份 |
| Java 包 | 四个根包 → `com.suwako.northstarplanetaddon.{moon,mercury,mars,venus}` |
| 入口类 | 4 个 `@Mod` 类 → 1 个 `@Mod` 入口 + 4 个 `XxxModule.init(...)` 静态方法 |
| 常量名 | 各模块 `MODID` → `NAMESPACE`（语义就是那个保留的注册命名空间） |
| 事件订阅 | 火星 / 金星所有 `@EventBusSubscriber(modid = …)` 指向新 mod id（否则监听器不会被挂上） |
| Mixin 配置 | 2 个独立配置，文件名带模块前缀，`compatibilityLevel` 从 `JAVA_17` 提到 `JAVA_21` |
| 启动日志 | 统一前缀（`[moon]` / `[mercury]` / `[mars]` / `[venus]`），版本号改读新 mod 容器 |
| 配置文件名 | `phosphoros-common.toml` → `northstar_planet_addon-venus-common.toml` |
| 共享标签文件 | 4 个 `data/northstar/tags/entity_type/*` 与 1 个 `data/minecraft/tags/block/mineable/shovel.json` 按并集合并 |
| 金星配方类型注册 | 从「静态初始化直接写 `BuiltInRegistries.RECIPE_TYPE`」改成走 `DeferredRegister`（原写法在模组构造期必然抛「Registry is already frozen」，见 §8.1） |

**没改什么**

- 所有内容 ID 的**路径部分**（`moon_rabbit`、`mercury_sand`、`ares`、`pressure_tank` ……），只换了命名空间。
- 所有玩法数值与状态机逻辑：Boss 血量、技能冷却、伤害倍率、风暴时长、温度阈值、伤害管线 8 步顺序……
- 所有资源的相对路径（`textures/…`、`models/…`、`worldgen/…`、`recipe/…`）。
- 所有跨模组集成方式（注册名 / 标签 / 反射 + 反射桥接类）。

---

## 12. 来源与许可

- 月球 / 水星 / 火星三个模块来自工程 `NorthstarMoonAddon`、`NorthstarMercuryAddon`、`NorthstarMarsAddon`，
  原作者分别标注为 Suwako / Vitran，许可 `All Rights Reserved`。
- 金星模块来自工程 `phosphoros`（Create: Phosphoros，原作者标注 Phosphoros Team，许可 **MIT**）。
  按 MIT 要求，该部分的原始版权与许可声明应随分发保留；本工程整体 `license` 字段暂按多数模块标为
  `All Rights Reserved`，如需改为 MIT 或双许可，请自行调整 `gradle.properties` 的 `mod_license` 并补充许可文件。
- 四个星球的目标模组是 [Create: Northstar Redux](https://modrinth.com/mod/create-northstar)（modid `northstar`），
  本模组不含它的任何代码，运行时依赖它的维度、方块、群系与标签。
