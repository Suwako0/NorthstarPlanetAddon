# 北极星月球附属（NorthstarMoonAddon）逻辑梳理

## 1. 定位与依赖

一句话定位：给「Create: Northstar - Redux（modid=`northstar`）」的月球维度 `northstar:moon` 追加一套月球生态 —— 月林（Moonforest）/露天温泉两个生物群系、月华树木家族、月兔 / 月壳蟹 / 梦眠月鹿 Boss、温泉流体与梦眠祭坛仪式。

| 项 | 值 | 出处 |
|---|---|---|
| modid / 命名空间 | `northstar_moon_addon`（主命名空间，与原版标签 `minecraft:`、北极星 `northstar:` 并用） | `gradle.properties`、`NorthstarMoonAddon.MODID` |
| Minecraft | 1.21.1（依赖区间 `[1.21.1]`） | `gradle.properties` |
| NeoForge | 21.1.248（依赖区间 `[21.1,)`） | `gradle.properties` |
| Java / 构建 | Java 21 toolchain；`net.neoforged.moddev` 2.0.144 | `build.gradle` |
| 版本 / 许可 | `0.2.0`；license `All Rights Reserved` | `gradle.properties` |
| 必需依赖 | `northstar` 区间 `[0.6.5,)`，`mandatory=true`、`ordering=AFTER`、`side=BOTH` | `META-INF/neoforge.mods.toml` |
| 编译期依赖 | **没有**北极星坐标。`build.gradle` 只声明 NeoForge；所有跨模组引用都是字符串 ID（`ResourceLocation("northstar", …)` 与 JSON 里的 `northstar:xxx`），未 import 任何北极星类 | `build.gradle`、全源码 grep |
| Mixin | `northstar_moon_addon.mixins.json`，`required=true`、`minVersion 0.8.5`、`compatibilityLevel JAVA_17`、`defaultRequire 1` | mixins.json、jar manifest |

## 2. 注册内容清单

注册全部通过 `DeferredRegister` 在 `NorthstarMoonAddon` 构造函数的 `modEventBus` 上按顺序完成：`NMAFluidTypes → NMAFluids → NMABlocks → NMAItems → NMAFeatures → NMASounds → NMAEntityTypes → CREATIVE_TABS`（流体类型必须最先，因为流体构造时取类型；流体先于方块的 `hot_spring` 液体块与桶）。

### 2.1 方块（`registry/NMABlocks`，共 12 个）

| 注册名 | 类 | 关键属性 / 行为 |
|---|---|---|
| `dreamsleep_grass` | `DreamsleepGrassBlock extends Block implements BonemealableBlock` | 梦眠之草。`MapColor.GRASS`、硬度 0.7、`SoundType.GRASS`；骨粉：32 次尝试，`random.nextInt(5)==0` 出月之花，否则 `minecraft:short_grass` |
| `moon_soil` | `Block` | 月壤。`MapColor.COLOR_PURPLE`、0.6、`SoundType.GRAVEL` |
| `moonlight_log` | `RotatedPillarBlock` | 月华原木。发光 4（`LOG_LIGHT`）、0.2/2.0、`SoundType.WOOD` |
| `stripped_moonlight_log` | `RotatedPillarBlock` | 去皮月华原木。不发光 |
| `moonlight_wood` | `Block` | 月华木（六面皮）。发光 4 |
| `stripped_moonlight_wood` | `Block` | 去皮月华木。不发光 |
| `moonlight_planks` | `Block` | 月华木板。`strength(2.0F, 3.0F)` |
| `moonlight_leaves` | `LeavesBlock` | 月华树叶。发光 **12**、0.2、`randomTicks`、`noOcclusion`、禁刷怪/窒息/遮视 |
| `moonlight_sapling` | `MoonlightSaplingBlock extends SaplingBlock` | 月华树苗。可立于 dreamsoils；`advanceTree` 覆写为取 `northstar_moon_addon:moonlight_tree` 直接 `place()` |
| `moon_bloom` | `MoonBloomBlock extends BushBlock` | 月之花。发光 6、`OffsetType.XZ`、`randomTicks`；仅可立于月壤/梦眠之草；悬空即掉落 |
| `hot_spring` | `LiquidBlock`（流体 `NMAFluids.HOT_SPRING`） | 温泉液体块。`replaceable`、`noCollission`、`strength(100)`、`.liquid()`、**`noLootTable()`** |
| `dreamsleep_altar` | `DreamsleepAltarBlock extends Block` | 梦眠祭坛。3.5/9.0、`SoundType.DEEPSLATE`、发光 7、`requiresCorrectToolForDrops`、`noOcclusion` |

### 2.2 物品（`registry/NMAItems`）

| 注册名 | 类 | 说明 |
|---|---|---|
| `moon_shell` | `Item` | 月壳，`Rarity.COMMON`；1 个 → 3 骨粉 |
| `mooncake` | `MooncakeItem` | `Foods.PUMPKIN_PIE`；食用后服务端给再生 I 200t、幸运 I 1200t |
| `moon_pearl` | `MoonPearlItem` | `UNCOMMON`；对本模组梦境草右键放置祭坛结构，变成破损的月明珠 |
| `broken_night_pearl` | `Item` | 消耗后的明珠；8 月壳围合成新明珠 |
| `moon_deer_antler` | `Item` | `RARE`，Boss 掉落；也是月斧修复材料 |
| `moon_rabbit_spawn_egg` | `DeferredSpawnEggItem` | 色 `0xC9C4D8`/`0xEBD8FF` |
| `moon_shell_crab_spawn_egg` | `DeferredSpawnEggItem` | 色 `0x6E7A9E`/`0xA9E4FF` |
| `lunar_deer_spawn_egg` | `DeferredSpawnEggItem` | 色 `0x18243E`/`0xD5F6FF`，`Rarity.EPIC` |
| `hot_spring_bucket` | `BucketItem` | 装流体 `HOT_SPRING`，`craftRemainder(BUCKET)`、`stacksTo(1)` |
| `moon_deer_axe` | `MoonDeerAxeItem extends AxeItem` | tier `NMATiers.DREAMSLEEP_MOON`，`createAttributes(tier, 5.0F, -2.9F)`、`RARE` |
| 11 个 BlockItem | `BlockItem` | `dreamsleep_grass`、`moon_soil`、`moonlight_log/wood`、`stripped_moonlight_log/wood`、`moonlight_planks/leaves/sapling`、`moon_bloom`、`dreamsleep_altar`（`hot_spring` 无 BlockItem，只有桶） |

### 2.3 实体（`registry/NMAEntityTypes`）

| 注册名 | 类 | 分类 / 尺寸 / 追踪 |
|---|---|---|
| `moon_rabbit` | `MoonRabbitEntity extends Animal` | `CREATURE`，0.45×0.5，眼高 0.4，追踪 10 |
| `moon_shell_crab` | `MoonShellCrabEntity extends Animal` | `CREATURE`，0.7×0.45，眼高 0.3，追踪 10 |
| `lunar_deer` | `MoonDeerEntity extends Animal` | `CREATURE`，1.5×1.8，眼高 1.55，追踪 64 |
| `moon_dust_projectile` | `MoonDustProjectileEntity extends ThrowableProjectile` | `MISC`，0.18³，追踪 32，`updateInterval(1)` |
| `moonlight_bolt` | `MoonlightBoltEntity extends ThrowableProjectile` | `MISC`，0.25³，追踪 64，`updateInterval(1)` |

属性注册在 `NorthstarMoonAddon.registerEntityAttributes`（`EntityAttributeCreationEvent`）。刷怪放置规则只用 `RegisterSpawnPlacementsEvent` 为兔与蟹注册：`ON_GROUND` + `MOTION_BLOCKING_NO_LEAVES` + 自定义 `checkMoonForestSpawnRules`（`Operation.REPLACE`）：`MobSpawnType.SPAWNER` 直接放行，否则要求脚下方块在 `northstar_moon_addon:moon_forest_spawnable` 标签内（梦眠之草、月壤、`northstar:moon_stone`、`northstar:moon_sand`）——绕过原版动物刷怪要求的「明亮方块」，因为月球无光照。

### 2.4 流体与流体类型

| 项 | 值 |
|---|---|
| 流体 `hot_spring` | `HotSpringFluid.Source`，`getAmount` 恒 8，`isSource=true` |
| 流体 `hot_spring_flowing` | `HotSpringFluid.Flowing`，带 `LEVEL` 属性，默认 7 |
| `BaseFlowingFluid.Properties` | `slopeFindDistance(4)`、`levelDecreasePerBlock(1)`、`tickRate(5)`、`explosionResistance(100.0F)`、bucket/block/provider 均指向本模组对象 |
| 流体类型 `hot_spring` | `density 1000`、`viscosity 1000`、`temperature 400`、`canConvertToSource(true)`（可无限取源）、`canExtinguish`、`canHydrate`、`supportsBoating`、`lightLevel 3`；桶音效为原版水桶音效，蒸发音效 `FIRE_EXTINGUISH` |

### 2.5 声音 / 效果 / 其他注册表

- `registry/NMASounds`（`SoundEvent`，全部 `createVariableRangeEvent`）：`lunar_deer_hit1/2/3`、`lunar_deer_die`、`moonduel`（Boss 战曲，`stream:true`）、`moonlight`（月林环境曲，`stream:true`）。
- `registry/NMAFeatures`（`Registries.FEATURE`）：`hot_spring_lake` → `HotSpringLakeFeature`，codec 复用 `LakeFeature.Configuration.CODEC`。
- 非注册表：`NMATiers.DREAMSLEEP_MOON`（enum 实现 `Tier`：2200 耐久、speed 9.5、攻击加成 4.5、附魔性 22、`INCORRECT_FOR_NETHERITE_TOOL`、修复材料月鹿角）；`NorthstarMoonAddon.MOON_FOREST_SPAWNABLE`（block TagKey）。
- 无自定义 MobEffect、无自定义 ParticleType、无自定义 Enchantment / VillagerProfession / 附魔。

### 2.6 创造模式标签页

`CREATIVE_TABS` 注册 `northstar_moon_addon:main`，标题 `itemGroup.northstar_moon_addon`（中「北极星月球拓展」/ 英「NorthStar Moon Addon」），图标 `moon_bloom`。`displayItems` 按顺序放入 21 项：梦眠之草、月壤、月之花、月华原木/木、去皮原木/木、木板、树叶、树苗、梦眠祭坛、月壳、月饼、月明珠、破损的月明珠、月鹿角、梦眠月斧、温泉水桶、月兔/月壳蟹/梦眠月鹿刷怪蛋。

## 3. 世界生成

### 3.1 生物群系（`data/northstar_moon_addon/worldgen/biome/`）

| 群系 | JSON 要点 |
|---|---|
| `moon_forest`（月林） | `temperature 0.7`、`downfall 0.4`、`has_precipitation:false`、`sky_color/fog_color = 0`、`water_color 4159204`、`water_fog_color 329011`；carver `cave`/`cave_extra_underground`；spawners：creature 月兔 weight 20 ×(2–4)、月壳蟹 weight 12 ×(1–3)；monster `northstar:moon_lunargrade` 8 ×(1–2) |
| `open_hot_spring`（露天温泉） | 同温湿度/天空色；spawners creature **全空**，monster：`northstar:moon_lunargrade` 40 ×(1–4)、`northstar:moon_snail` 20 ×(1–3)、`northstar:moon_eel` 20 ×(2–5) |

两者 features 按原版 step 下标排列：`[0]` `northstar:moon_crater`；`[1]` 月林放 `northstar:rare_lava_lake`、温泉放 `northstar_moon_addon:hot_spring_lake`；`[2]`（LOCAL_MODIFICATIONS，月林）`moon_forest_soil` → `moon_forest_grass` 两个 disk；`[6]` 复用 `northstar:` 月球矿石与四种 blob（andesite/asurine/basalt/blackstone）；`[7]` `northstar:moon_lunar_sapphire_geode` + `northstar:frost_sparse`；`[9]`（月林）`moon_forest_short_grass`、`moonlight_tree`、`moon_bloom_patch`。

### 3.2 注入方式与注入点（`world/MoonBiomeInjector`）

- 事件：`NeoForge.EVENT_BUS.addListener(MoonBiomeInjector::onServerAboutToStart)`，即 **`ServerAboutToStartEvent`** —— 在任何 Level / 区块创建、以及生成器 feature 列表求值之前。
- 目标：`ResourceKey<LevelStem>` = `northstar:moon`，只认这一维度 ID（不按内容猜 biome source）。
- 前置：`biomeSource` 必须同时是 `MultiNoiseBiomeSource` 且实现两个桥接接口（`MoonForestBiomeSource`、`RefreshableBiomeSource`，由两个 Mixin 实现），否则 `LOGGER.error` 并放弃。
- 算法：找到 `northstar:lunar_plains` 且 weirdness 恰为 `[-0.9333, -0.7666]`（`Climate.quantizeCoord` 精确比对）的条目，删除它，按 `BANDS` 从下往上重新切分：`open_hot_spring [-0.9333, -0.8666]`、`moon_forest [-0.85, -0.80]`，空隙与两端仍为 lunar_plains（`withWeirdness` 只改 weirdness，其余六个气候轴与 offset 原样保留）。
- 安全阀：群系注册表里缺任一目标群系直接整体放弃；已存在则不重复添加；找不到 lunar_plains 那个参数盒则不改动任何东西。写入后调用 `nma$refreshPossibleBiomes()` 重建候选集并逐条校验。
- 边界值来源注释写死为「Northstar 0.6.5 的 lunar-plains 单一表面气候盒」；两个区间刻意不重叠（`multi_noise` 取最近参数点，重叠会因距离并列 0 而被更早条目赢下）。

### 3.3 feature / placed_feature（`data/northstar_moon_addon/worldgen/`）

| configured_feature | 类型与要点 |
|---|---|
| `moon_forest_soil` | `minecraft:disk`，目标 `northstar:moon_stone` / `northstar:moon_sand`，铺 `moon_soil`，半径 3–8，`half_height 2` |
| `moon_forest_grass` | `minecraft:disk`，目标 `moon_soil`，铺 `dreamsleep_grass`，半径 3–8，`half_height 1` |
| `moonlight_tree` | `minecraft:tree`，`cherry_trunk_placer`（base_height 11、rand 4/6、2–3 枝、枝长 3–6、`branch_start_offset_from_top -7..-4`、end `-2..0`）+ `cherry_foliage_placer`（radius 3、height 5、25% 洞、15% 垂叶、30% 垂叶延伸）；decorators：`alter_ground`→`moon_soil`、`attached_to_leaves` 8% 概率向下的 `moon_bloom` |
| `moon_forest_short_grass` | `random_patch` tries 32、xz 5、y 2，放 `minecraft:short_grass` |
| `moon_bloom_patch` | `random_patch` tries 48、xz 6、y 3，放 `moon_bloom` |
| `hot_spring_lake` | 自定义 `northstar_moon_addon:hot_spring_lake`；fluid = `hot_spring`；barrier = `noise_threshold_provider`（seed 2025、threshold −0.25、`high_chance 0.07`）：默认 `northstar:moon_stone`，low `northstar:moon_sand`，high `northstar:moon_stone_lamp` |

对应 placed_feature：`moon_forest_soil` count 32 + `in_square` + `OCEAN_FLOOR_WG`；`moon_forest_grass` count 40 同上；`moonlight_tree` count uniform 5–9 + `surface_water_depth_filter(0)` + `OCEAN_FLOOR` + `would_survive` 月华树苗；`moon_forest_short_grass` count 3–6；`moon_bloom_patch` count 2–5；`hot_spring_lake` `rarity_filter(chance 6)` + `in_square` + `WORLD_SURFACE_WG` + `biome`。全部 placed_feature 都以 `minecraft:biome` 收尾。

结构：`data/northstar_moonaddon/structure/dreamsleep_alter.nbt` 一份 7×3×7 手工 NBT 模板，**没有** structure / structure_set / template_pool JSON，不参与世界生成，只被 `MoonPearlItem` 现场放置。

### 3.4 注入到 `northstar:` 标签的方式

| 注入文件 | 内容 |
|---|---|
| `data/northstar/tags/worldgen/biome/moon_biomes.json` | `moon_forest`、`open_hot_spring` |
| `data/northstar/tags/block/moon_blocks.json` | 10 个方块（除 `dreamsleep_altar`、`hot_spring`） |
| `data/northstar/tags/block/natural_moon_blocks.json` | `moon_soil`、`dreamsleep_grass`、`moonlight_log`、`moonlight_leaves` |
| `data/northstar/tags/block/moon_surface_replaceable.json` | `moon_soil`、`dreamsleep_grass` |
| `data/northstar/tags/entity_type/can_survive_cold.json` | 月兔、月壳蟹、月鹿 |
| `data/northstar/tags/entity_type/doesnt_require_oxygen.json` | 同上 |

全部 `replace:false`，纯追加。

## 4. 核心机制

### 4.1 生物群系运行时注入
触发：服务器启动（`ServerAboutToStartEvent`）。纯服务端/通用侧（Mixin 是 common）。数值与流程见 3.2；涉及的类：`MoonBiomeInjector`、`MoonForestBiomeSource`、`RefreshableBiomeSource`、`MultiNoiseBiomeSourceMixin`、`BiomeSourceMixin`。

### 4.2 月林地表生成
`moon_forest_soil`（disk，`LOCAL_MODIFICATIONS`）先把月球石/月沙转成月壤，紧接着 `moon_forest_grass` 把月壤盖成梦眠之草，保证矿石与植被步骤（`[6]`/`[9]`）之前地面已就位。

### 4.3 月华树与树苗
`MoonlightSaplingBlock` 忽略父类 `TreeGrower`（占位用 `TreeGrower.OAK`）：`STAGE 0` 时 `cycle(STAGE)`；`STAGE 1` 时从 `Registries.CONFIGURED_FEATURE` 取 `NMATreeGrowers.MOONLIGHT_TREE`（`northstar_moon_addon:moonlight_tree`）并 `feature.place(...)`，失败则把树苗放回去（不静默吞掉）。可立方块：梦眠之草、月壤（+ `BushBlock` 默认）。世界生成里靠 `would_survive(moonlight_sapling)` 过滤落点。

### 4.4 月之花（`MoonBloomBlock`）
- 可立：梦眠之草、月壤；`neighborChanged` 里若 `!canSurvive` 就 `destroyBlock(pos, true)`；`randomTick` 再检查一次（悬空的花拿不到邻居更新）。
- 自带发光 6；客户端 `animateTick` 每 3 次采样一次，沿切向撒 `ParticleTypes.GLOW`，纯客户端开销。
- 掉落：自身（`survives_explosion`）；也是月兔/月壳蟹的繁殖食物与 `TemptGoal` 目标，还是骨粉出花与「月壤+月之花 → 梦眠之草」配方的原料。

### 4.5 梦眠之草骨粉（`DreamsleepGrassBlock`）
`isValidBonemealTarget` 要求上方为空气；`performBonemeal` 32 次循环，落点范围内须是空气且下方仍是梦眠之草，`random.nextInt(5)==0` 生成月之花，否则 `minecraft:short_grass`，均通过 `canSurvive` 校验。

### 4.6 月兔（`MoonRabbitEntity`，8 血、速度 0.32、摔落 6、步高 0.6）
- 目标：0 `FloatGoal`、1 `PanicGoal(1.6)`、2 `MoonRabbitGoldenCarrotGoal(1.15)`、3 `BreedGoal(0.9)`、4 `TemptGoal(1.0, 月之花)`、5 `WaterAvoidingRandomStroll(0.9)`、6 `LookAtPlayer(8)`、7 `RandomLookAround`。`setPathfindingMalus(DANGER_FIRE, -1)`。
- 金胡萝卜「萌宠反应」（服务端判定、同步布尔 `GOLDEN_CARROT_CUTE`）：8 格外有玩家主/副手拿 `minecraft:golden_carrot` → 播 56t（2.8s，对应 `animations/moon_rabbit/golden_carrot_cute.json`）的可爱动作，停下导航并盯着玩家，结束后进 80t 冷却。
- 金胡萝卜「交易」（`MoonRabbitGoldenCarrotGoal`，搜索 12 格、拾取距离²≤2.25）：走向掉落的金胡萝卜，`consumeDroppedGoldenCarrot` 只取 1 个（`shrink(1)` + `onItemPickup` + `take` + `discard`），随后 48t 咀嚼动画（`golden_carrot_eat.json`），结束时 `eat(...)` 播原版进食音/粒子并 `spawnAtLocation`：**90% 月饼、10% 月明珠**，再进 80t 冷却。掉落物优先于玩家手持的可爱动作（`canStartGoldenCarrotTrade` 只看是否正在吃）。
- 繁殖：`isFood` = 月之花；`getBreedOffspring` 生成新 `MOON_RABBIT`。

### 4.7 月壳蟹（`MoonShellCrabEntity`，16 血、攻击 3、速度 0.18、护甲 6、击退抗性 0.4、跟随 10）
- 目标：只有 `HurtByTargetGoal`（不主动索敌），因此只对**真正造成伤害的活体攻击者**还手。和平行为 `FloatGoal`/`Breed(0.8)`/`Tempt(0.9, 月之花)`/`WaterAvoidingRandomStroll(0.6)`/`LookAtPlayer(8)`/`RandomLookAround`。`DANGER_FIRE`、`WATER` 寻路惩罚 −1。
- 战斗状态机（服务端 `tickCombat`，用同步字节 `COMBAT_ANIMATION` 表达 `IDLE/PINCH/THROW`）：
  - 距离 ≤4 且 `pinchCooldown==0` → `PINCH`，`combatTicks=14`，冷却 30t；第 `14−6=8` 个战斗刻（即发起后第 6 tick）`resolvePinch`：仍在 4 格内且有视线才 `doHurtTarget` + 推击 `(0.45, 0.12)`。
  - 否则距离 ≤10 且 `throwCooldown==0` → `THROW`，`combatTicks=18`，冷却 50t；第 `18−7` 刻 `throwMoonDust`：从眼前 0.55 格生成 `MoonDustProjectileEntity`，方向朝目标眼位并 +0.16 Y 补弧，`shoot(...,0.65F,4.0F)`（散布 4°），附 ENCHANT 粒子与投掷音。
  - 都不可用则 `moveTo(target, 0.85)`。目标消失即 `cancelCombat` 归零。
- 投射物 `MoonDustProjectileEntity`：重力 0.03、寿命 50t、命中活体（非自己）造成 `indirectMagic` **2.0** 伤害并 GLOW 粒子后 `discard`，命中方块即 `discard`；每 2t 撒 ENCHANT+GLOW。
- 掉落：`moon_shell` 1–2；25% 追加 `northstar:moon_sand`。繁殖同月之花。

### 4.8 梦眠月鹿 Boss（`MoonDeerEntity`，1000 血、攻击 12、护甲 10、击退抗性 1、速度 0.30、`xpReward 2000`、`setPersistenceRequired()`、永不自然消失）
- 只在 `DreamsleepAltarBlock` 祭坛仪式或刷怪蛋出现；`MobCategory.CREATURE` 但未进任何 biome spawners。
- 战斗目标池：`findEligibleCombatPlayers(range)` —— 球形半径内、`isAlive && !isSpectator && !isCreative`；主目标取最近者，多目标时轮流避开上一次的技能目标（`lastSkillTarget`）。
- Boss 条：`ServerBossEvent`，蓝色 PROGRESS，禁黑屏/禁自带 Boss 音乐/禁世界迷雾；标题为 `entity.northstar_moon_addon.lunar_deer` + 「 - 」+ 阶段译名（新月/上弦月/满月/月食）。
- 阶段：按血量比 `>0.75 / >0.50 / >0.25 / else` 分 `NEW_MOON / FIRST_QUARTER / FULL_MOON / ECLIPSE`；换阶段时给 20t 无敌、抗性提升 II + 吸收 II 各 200t、打断技能、播 `ENDERMAN_STARE`、撒 40 个 END_ROD。
- 倍率：`stunnedTicks>0` 受伤 ×1.5；`weaknessTicks>0` 受伤 ×2.0；无敌刻内 `hurt` 直接返回 false。
- 技能选择：`chooseSkill` 排除当前/上一个/冷却中/本阶段不可用的，`DASH` 在 ≤6 格时禁用；按距离加权偏好（<4 格偏好 RING；4–10 格偏好 DASH/SHADOW；>10 格偏好 BARRAGE/BEAM/MOON_RAIN/MOON_DUST）；ECLIPSE 且血量 <10% 且 `meteorUsed==false` 时强制 `METEOR`。技能结束 `finishSkill` 排下一次 `40+rand(41)`t，无技能时 20t 后再试。
- 技能表（冷却单位 tick）：

| 技能 | 冷却 | 可用阶段 | 行为与数值 |
|---|---|---|---|
| DASH 冲锋 | 160 | 全阶段 | 1–20t 蓄力，21–36t 沿锁定方向每刻 `move(1.0)`，4 格内玩家每次遭遇扣 10 伤害 + 推击 (1.8, 0.45)，撞墙则 `stunnedTicks=60` + `ANVIL_LAND` |
| BARRAGE 月华弹幕 | 100 | 全阶段 | 第 16t 发射：FULL_MOON/ECLIPSE 时 12 发整圈均分；否则以目标方向为轴 ±(0–2)×12° 共 5 发。`MoonlightBoltEntity` 伤害 4.0、速度 1.1、无重力、寿命 100t；28t 结束 |
| RING 月环 | 120 | 全阶段 | 第 10t 4 格、第 28t 8 格各判一次环带（带宽 ±1.4），`magic` 6 伤害 + 推击 (1.1, 0.32)；40t 结束 |
| BEAM 月华光束 | 200 | 非 NEW_MOON | 1–40t 预警（地面 1.5 格圈 + 头部 ENCHANT）；41t 起按 `-35°→+35°` 扫射 60t，射程 28 格、`ClipContext.Block.COLLIDER` 阻挡；每 10 tick 对线段 1.0 内玩家 `magic` 3 伤害 |
| MOON_RAIN 月雨 | 240 | FULL_MOON / ECLIPSE | 选 7 个落点（目标周围 1.5–7.5 格，Y 取 `MOTION_BLOCKING_NO_LEAVES`）；1–30t 画 1.3 格预警圈；第 31t 每点 30 个 END_ROD + 1.5×6×1.5 盒内 `magic` 7 伤害；42t 结束 |
| SHADOW 月影 | 200 | FIRST_QUARTER / ECLIPSE | 1–16t `setInvisible(true)` + 预警；第 17t 现身于目标背后 2.4 格，`mobAttack` 10 伤害 + 失明 60t；30t 结束 |
| MOON_DUST 月尘牵引 | 180 | 全阶段 | 起手时把 64 格内所有合法玩家记入 UUID 快照并画 GLOW 圈 + ENCHANT 预警；前 4t 持续重绘；第 4t 结算：仍在 64 格内者 `magic` **6.0** 伤害并被水平牵引 1.25、垂直 0.20；12t 结束 |
| METEOR 陨月 | 0（一次性） | ECLIPSE | 血量 <10% 触发一次；1–60t 每刻 `deltaMovement=(0,0.18,0)` 上浮 + 20 格预警圈；第 61t 对 20 格内玩家 `magic` **18** + 推击 (1.8, 1.0)，自身获得 200t 弱化（受伤 ×2）；75t 结束 |

- 近战：不施法时若 ≤3.5 格且 `meleeCooldown==0` 则 `doHurtTarget` 并设 20t 冷却，否则以 0.9 速度追击；`nextSkillTicks` 初值 45。
- 音乐/Boss 条：`startSeenByPlayer` 把玩家加入 Boss 条并用 `playNotifySound(MOON_DUEL, SoundSource.MUSIC)` 单独推流；`stopSeenByPlayer` / `die` 用 `ClientboundStopSoundPacket(location, MUSIC)` 只停这首战曲，不干扰环境曲。`die` 后隐藏并清空 Boss 条。
- 音效：环境 `HORSE_AMBIENT`、受击在 `lunar_deer_hit1/2/3` 随机、死亡 `lunar_deer_die`、脚步 `HORSE_STEP(0.25,0.8)`；施法音 `EVOKER_CAST_SPELL`、弹幕 `WITHER_SHOOT`、环/雨/牵引 `AMETHYST_BLOCK_CHIME`、陨石 `GENERIC_EXPLODE`。
- 掉落（`loot_table/entities/lunar_deer.json`）：月鹿角 ×1、钻石 3–5、青金石 6–10、`northstar:lunar_sapphire_shard` 10–15。
- 客户端模型表现（`MoonDeerModel`）：不走关键帧文件，而是在 `setupAnim` 里按 `getCombatAnimation()` 逐技能硬编码姿态（DASH/RING/BARRAGE/BEAM/MOON_RAIN/METEOR/SHADOW/MOON_DUST），另有呼吸/摆尾/耳朵抖动、受击退缩（`hurtTime`）与死亡塌陷姿态（`deathTime`）。

### 4.9 梦眠祭坛与月明珠（Boss 召唤链）
1. `MoonPearlItem.useOn`：只对**梦眠之草**生效（其它方块返回 `PASS`，把右键让给 `DreamsleepAltarBlock`）。必须在 `northstar:moon` 维度，否则提示 `moon_only` 并 `FAIL`。
2. 服务端从 `StructureManager` 取 `northstar_moonaddon:dreamsleep_alter`（注意命名空间少了中间下划线，与文件路径一致）；`Rotation` 随机四向，锚点 `(3,1,3)` 经 `StructureTemplate.calculateRelativePosition` 旋转补偿后整体上抬 1 格放置；`setIgnoreEntities(false)`、`setKnownShape(true)`。
3. 放置成功：非创造模式把**恰好 1 个**月明珠换成破损的月明珠（栈数 1 时直接替换手持，否则 `shrink(1)` 后 `addItem`／`drop`），播 `RESPAWN_ANCHOR_CHARGE(1,0.7)`，`swing`。
4. `DreamsleepAltarBlock.useItemOn`（手持月明珠右键祭坛）：
   - 非月明珠 → 交回父类；客户端 → `SUCCESS`。
   - 夜间检查 `isRitualHour`：`dimensionType().hasFixedTime()` 时用 `floorMod(dayTime,24000)` 落在 `[13000, 23000)` 判夜间，否则直接 `level.isNight()`；非夜间提示 `daylight` + `BEACON_DEACTIVATE(0.6,0.7)` + `FAIL`。
   - 防叠怪：`AABB(pos).inflate(128)` 内存在任意活着的 `MoonDeerEntity` → 提示 `occupied` + 撒粒子 + `RESPAWN_ANCHOR_DEPLETE(0.7,0.6)` + `FAIL`。
   - 召唤：创造模式不消耗明珠；在祭坛上方 1 格生成月鹿、朝向玩家 180° 反向、`setPersistenceRequired()`、`lookAt(EYES, player)`；播 `RESPAWN_ANCHOR_CHARGE(1,0.5)` + `moonlight(pos,45)`；`scheduleTick(pos, 40t)`。
   - `tick`：`moonlight(pos,60)` + `BEACON_DEACTIVATE(1,1.5)` + `destroyBlock(pos, false)` —— **祭坛自毁且不掉落**（掉落表 `pools: []`）。
   - `useWithoutItem`：空手右键只显示「需要献上一个月明珠」的 actionbar 提示。
   - `getStateForPlacement` 永远返回 `defaultBlockState()`。

### 4.10 温泉（流体 / 湖 / 浸泡）
- 流体行为：等同水（`slopeFindDistance 4`、`tickRate 5`、可助推、可游泳、可载船）；`canConvertToSource(true)` 意味着**两个相邻源 + 实心底面可再生新源**，只要没有取消 `CreateFluidSourceEvent`。
- 外观（`HotSpringFluid.animateTick`，纯客户端）：1/6 概率上升 BUBBLE；仅对「上方是空气的源方块」再以 1/24 概率 BUBBLE_POP、1/8 概率 CAMPFIRE_COSY_SMOKE 蒸汽。
- 客户端渲染（`NMAClient`）：`ItemBlockRenderTypes.setRenderLayer(..., RenderType.translucent())`；`IClientFluidTypeExtensions` 指定 `hot_spring_still`/`hot_spring_flow`（均带 `.png.mcmeta`，`frametime:2`），水下覆盖用原版 `textures/misc/underwater.png`，`getTintColor()` = `0xFFFFFFFF`（不染色）。
- 温泉湖 `HotSpringLakeFeature`：先跑原版 `LakeFeature.place`（含挖坑与灌流体），再在 `origin().below(4)` 起以 16×16×8 扫描，凡「当前格是 `hot_spring` 且下一格为实心」就单独 `floor.getState(random, pos)` 重掷一次底面 —— 让 `noise_threshold_provider` 的月石/月沙/月石灯混合真正逐块生效（原版只会整池铺一种）。
- 浸泡回复 `HotSpringSoakHandler.onEntityTick`（`EntityTickEvent.Post`，服务端）：每 5 tick 检查一次 `isInFluidType(HOT_SPRING)`，命中则续期 **再生 I** 与 **饱和 I** 各 20t（`ambient=true, visible=true, showIcon=true`），仅在剩余时长 ≤5t 时才重新发效果，离开水面约 1 秒自然失效。

### 4.11 梦眠月斧（`MoonDeerAxeItem`）
- 属性：tier 2200 耐久 / 9.5 速度 / +4.5 攻击 / 附魔性 22；`Item.Properties` 里 `DiggerItem.createAttributes(tier, 5.0F, -2.9F)`（注释综合为 10.5 攻击、1.1 攻速）；`Rarity.RARE`；修复材料月鹿角。
- `inventoryTick`：仅当斧在主手或副手时，服务端每 tick 刷 **抗性提升 I 40t**（`ambient=false, visible=false, showIcon=true`），收手后自然衰减。
- `hurtEnemy`：服务端在命中点撒 14 个 END_ROD 月华。
- 客户端 `NMAClient.emitMoonDeerAxeGlow`：每 10 tick 在持斧手侧撒 1 个 GLOW（依据视线方向推算左右手侧位），每 40 tick 加 1 个 END_ROD 亮闪；纯本地、不改物品渲染。

### 4.12 月饼（`MooncakeItem`）
`Foods.PUMPKIN_PIE` 营养；`finishUsingItem` 在服务端且食用者为 `ServerPlayer` 时给再生 I 200t + 幸运 I 1200t。主要来源是月兔交易。

## 5. Mixin

两个 Mixin 都是**接口桥接**，不使用 `@Inject`/`@Redirect`/`@ModifyVariable` —— 只 `@Shadow` 一个字段并实现 `nma$` 前缀的接口方法，因此不与任何模组争抢同一调用点。

| Mixin | 目标类 | 注入/改写的成员 | 目的 |
|---|---|---|---|
| `BiomeSourceMixin` | `net.minecraft.world.level.biome.BiomeSource` | `@Shadow @Final @Mutable Supplier<Set<Holder<Biome>>> possibleBiomes`；`@Shadow abstract collectPossibleBiomes()`；实现 `RefreshableBiomeSource.nma$refreshPossibleBiomes()` | 注入后把记忆化的候选群系集合重建为 `Collectors`/`ImmutableSet` 版本，让 `possibleBiomes()` 立刻包含新群系 |
| `MultiNoiseBiomeSourceMixin` | `net.minecraft.world.level.biome.MultiNoiseBiomeSource` | `@Shadow @Final @Mutable Either<Climate.ParameterList<Holder<Biome>>, Holder<MultiNoiseBiomeSourceParameterList>> parameters`；实现 `MoonForestBiomeSource.nma$getClimateEntries()` / `nma$setClimateEntries()` | 读取当前解码后的气候条目列表；写回时统一包装成 `Either.left(new Climate.ParameterList<>(...))` |

## 6. 客户端

- 装配入口 `client/NMAClient.register(modEventBus)`，由 `NorthstarMoonAddon` 在 `FMLEnvironment.dist.isClient()` 分支内调用（不用 `@EventBusSubscriber`，避免专用服务器加载客户端类）。监听：`RegisterLayerDefinitions`、`RegisterRenderers`、`RegisterClientReloadListeners`、`RegisterClientExtensions`、`FMLClientSetupEvent`，以及 `NeoForge.EVENT_BUS` 的 `ClientTickEvent.Post`。
- **无 GeckoLib**：三个生物都是原版 `HierarchicalModel` + `LayerDefinition.create(mesh, 64, 64)` 手写模型，64×64 皮肤，标准 box-UV。

| 模型 / 渲染器 | 层 ID | 纹理 | 影子半径 / 幼体缩放 |
|---|---|---|---|
| `MoonRabbitModel` / `MoonRabbitRenderer` | `northstar_moon_addon:moon_rabbit#main` | `textures/entity/moon_rabbit.png` | 0.25 / 0.55 |
| `MoonShellCrabModel` / `MoonShellCrabRenderer` | `northstar_moon_addon:moon_shell_crab#main` | `textures/entity/moon_shell_crab.png` | 0.4 / 0.6 |
| `MoonDeerModel` / `MoonDeerRenderer` | `northstar_moon_addon:moon_deer#main` | `textures/entity/moon_deer.png` | 0.9 / 0.6（Boss 实际不会被缩放的幼体调用） |
| `MoonDustProjectileRenderer` | — | `TextureAtlas.LOCATION_BLOCKS`（不使用贴图） | 纯粒子表现 |
| `MoonlightBoltRenderer` | — | `TextureAtlas.LOCATION_BLOCKS`（不使用贴图） | 纯粒子表现 |

- 骨骼与动画分工：兔/蟹的 `setupAnim` 走 `NMAAnimations` 里的 `AnimationDefinition`（`MOON_RABBIT_IDLE 3.5s looping`、`MOON_RABBIT_HOP 0.6s looping`（`animateWalk` 2.5/2.0）、`MOON_RABBIT_GAZE 3.5s looping`、`MOON_SHELL_CRAB_IDLE 4.0s`、`MOON_SHELL_CRAB_PINCH 0.70s`、`MOON_SHELL_CRAB_THROW 0.90s`、`MOON_SHELL_CRAB_WALK 1.0s looping`、`MOON_SHELL_CRAB_GAZE 3.5s`）；优先级为 战斗动画 > 拜月 > idle，行走层只在非战斗/非特殊动作时叠加。鹿完全靠程序化代码的姿态（无动画文件）。
- 可在资源包热替换的动画：`client/animation/NMAAnimationResources` 是 `SimpleJsonResourceReloadListener`（目录 `animations`），把 `assets/<ns>/animations/**/*.json` 解析成 `AnimationDefinition`（支持 `length`、`looping`、`bones.<bone>.rotation/position/scale`，每个关键帧 `time/x/y/z` 与可选 `interpolation: linear`，默认 CATMULLROM）。现有两份：`animations/moon_rabbit/golden_carrot_cute.json`（length 2.8）、`golden_carrot_eat.json`。
- 月林背景音乐（`NMAClient.onClientTick`）：仅在 `northstar:moon` 且玩家所在群系为 `moon_forest` 时计时，随机间隔 200–3600 tick（10 秒–3 分钟，`MOONLIGHT_MIN_DELAY`/`MOONLIGHT_MAX_DELAY`）；开播前 `getMusicManager().stopPlaying()`，播放期间若原版情境音乐插入则再停一次；64 格内存在活着的 `MoonDeerEntity` 或离开月维度时立刻停曲。
- 资源路径一览：`assets/northstar_moon_addon/{blockstates,models/block,models/item,textures/block,textures/item,textures/entity,sounds,animations,lang}`。

## 7. 数据包与资源布局

```
assets/northstar_moon_addon/
  blockstates/   12 个（dreamsleep_grass, moon_soil, moonlight_log/wood, stripped_moonlight_log/wood,
                 moonlight_planks/leaves/sapling, moon_bloom, dreamsleep_altar, hot_spring）
  models/block/  dreamsleep_grass, moon_soil, moonlight_log(+_horizontal), stripped_moonlight_log(+_horizontal),
                 moonlight_wood, stripped_moonlight_wood, moonlight_planks, moonlight_leaves,
                 moonlight_sapling, moon_bloom, dreamsleep_altar, hot_spring
  models/item/   11 个方块物品 + moon_shell, mooncake, moon_pearl, broken_night_pearl, moon_deer_antler,
                 moon_deer_axe, hot_spring_bucket, 3 个刷怪蛋
  textures/block/ 含 dreamsleep_grass_top/side, moon_soil, moonlight_*, moon_bloom, dreamsleep_altar_top/side/core,
                 hot_spring_still.png(+.mcmeta), hot_spring_flow.png(+.mcmeta)
  textures/entity/ moon_rabbit.png, moon_shell_crab.png, moon_deer.png
  sounds/        lunar_deer_hit1/2/3.ogg, lunar_deer_die.ogg, moonduel.ogg, moonlight.ogg + sounds.json
  animations/    moon_rabbit/golden_carrot_cute.json, moon_rabbit/golden_carrot_eat.json
  lang/          en_us.json, zh_cn.json
data/northstar_moon_addon/
  worldgen/biome/             moon_forest, open_hot_spring
  worldgen/configured_feature/ moon_forest_soil, moon_forest_grass, moon_forest_short_grass,
                              moon_bloom_patch, moonlight_tree, hot_spring_lake
  worldgen/placed_feature/    以上 6 个同名文件
  tags/block/    moon_forest_spawnable
  tags/item/     moonlight_logs
  loot_table/blocks/  dreamsleep_grass, moon_soil, moonlight_log, moonlight_wood,
                      stripped_moonlight_log, stripped_moonlight_wood, moonlight_planks,
                      moonlight_leaves, moonlight_sapling, moon_bloom, dreamsleep_altar(空池)
  loot_table/entities/ moon_rabbit, moon_shell_crab, lunar_deer
  recipe/        moonlight_planks(shapeless,4), moonlight_planks_from_stonecutting(1),
                 moonlight_wood(3), stripped_moonlight_wood, dreamsleep_grass(月壤+月之花),
                 bone_meal_from_moon_shell(3), moon_soil_from_smelting_moon_sand(200t/0.1xp),
                 moon_pearl_repair(8 月壳 + 破损明珠), moon_deer_axe(月鹿角+月壳+月华木板)
  advancement/   lunar_deer_defeated（challenge，player_killed_entity = lunar_deer）
data/northstar_moonaddon/structure/  dreamsleep_alter.nbt
data/minecraft/tags/block/  mineable/axe, mineable/shovel, mineable/pickaxe, logs, logs_that_burn,
                            planks, leaves, saplings, flowers, small_flowers, dirt
data/minecraft/tags/item/   logs, logs_that_burn, planks, leaves, saplings, flowers, small_flowers, axes
data/northstar/tags/block/  moon_blocks, natural_moon_blocks, moon_surface_replaceable
data/northstar/tags/entity_type/ can_survive_cold, doesnt_require_oxygen
data/northstar/tags/worldgen/biome/ moon_biomes
```

标签要点：`minecraft:mineable/axe` = 5 个木制品；`mineable/shovel` = 梦眠之草 + 月壤；`mineable/pickaxe` = 仅梦眠祭坛。方块掉落基本是「掉落自身（`survives_explosion`）」；`moonlight_leaves` 为剪刀/精准采集 → 树叶、否则 5% 树苗 + 2% 木棍；`dreamsleep_altar` 掉落表为空池（靠 `destroyBlock(pos,false)` 自毁）；`hot_spring` 无掉落表。全部 JSON 使用 1.21 的目录名 `loot_table`、`recipe`、`advancement`（单数）。

## 8. 已知限制与注意事项

1. **README 与源码不一致**：README 说「Moonforest 的位置由魔改北极星决定、运行时注入的 mixin 已删除」，但当前源码正是由 `MoonBiomeInjector` + 两个 Mixin 在 `ServerAboutToStartEvent` 运行时注入，模组不再需要魔改版北极星 jar。以源码为准。
2. **注入强绑定北极星 0.6.5 的气候盒数值**：`PLAINS_WEIRDNESS_MIN/MAX = -0.9333/-0.7666` 且要求 `northstar:lunar_plains` 条目存在并精确匹配（`Climate.quantizeCoord`）。北极星若改参数，注入只会打 `error` 日志后整体放弃，月亮保持原样 —— 不会崩，但也看不出新群系。
3. **群系区间必须不重叠**：`multi_noise` 取最近参数点，重叠会因距离并列而被更早条目赢下导致新群系永不生成。`open_hot_spring` 区间宽度 0.0667 > `moon_forest` 的 0.05。
4. **Boss 战斗状态不落盘**：`MoonDeerEntity` 没有覆写 `addAdditionalSaveData`/`readAdditionalSaveData`，阶段、技能冷却、`meteorUsed` 等都只在内存；区块卸载/重启后回到 `NEW_MOON`、冷却归零（`phase`、`activeSkill` 等字段均为普通字段）。
5. **温泉湖扫描范围硬编码**：`HotSpringLakeFeature` 用 `SPAN_XZ=16`、`SPAN_Y=8`、`FLUID_DEPTH=4` 复刻原版 `LakeFeature` 的包围盒；载入的原版实现若改变盒尺寸，这里需要同步。
6. **温泉可无限取源**：`canConvertToSource(true)` + `BaseFlowingFluid`，两个相邻源即可在实心地面上生第三源；若下游整合包需要限制，得取消 `CreateFluidSourceEvent` 或关掉该 flag。
7. **祭坛一次性且无掉落**：召唤后 40 tick 自毁且不掉落（掉落表 `pools: []`），Boss 存活期间 128 格内任何祭坛都拒绝响应；祭坛只在夜间（或固定时间维度的 `[13000,23000)` 段）可用。
8. **结构命名空间少一个下划线**：模板在 `data/northstar_moonaddon/structure/dreamsleep_alter.nbt`（`northstar_moonaddon`，`alter` 非 `altar`），与 `MoonPearlItem.DREAMSLEEP_ALTAR_TEMPLATE` 一致；改名需两处同步，否则只会在游戏内提示「无法感应模板」。
9. **`zh_cn.json` 有重复键**：`item.northstar_moon_addon.broken_night_pearl` 出现两次（「破损的月明珠」与「破损夜明珠」），后者生效。
10. **`hot_spring` 没有方块物品**，创造栏只能拿到温泉水桶；`LiquidBlock` 带 `noLootTable()`，破坏不掉落任何东西。
11. **Mixin 配置 `compatibilityLevel: JAVA_17`** 而工程 toolchain 是 Java 21；两个 Mixin 只用 `@Shadow` 字段，未使用较新注入器特性，实际是否需调整未确认。
12. **月鹿 `MobCategory.CREATURE` 但从未进 biome spawners**，只能通过祭坛或刷怪蛋出现；同时 `removeWhenFarAway` 返回 false、`setPersistenceRequired()` 常驻。
13. **月壳蟹的攻击目标只来自 `HurtByTargetGoal`**，不主动索敌；月兔/月壳蟹刷怪规则不看光照，仅看脚下是否属于 `moon_forest_spawnable`（含 `northstar:moon_stone`/`moon_sand`，所以生境略大于群系地面）。
14. **鹿的两个投射物渲染器不画几何体**：`MoonDustProjectileRenderer` / `MoonlightBoltRenderer` 的 `getTextureLocation` 返回方块图集占位，视觉完全由服务端同步的 GLOW/END_ROD/ENCHANT 粒子承担。
15. **未确认项**：`MoonlightSaplingBlock` 的 `NMABlocks` 注释提到「还会处理 2×2 巨树生长」，但类里没有对应实现（只有 `advanceTree`），实际是否支持 2×2 未确认。
