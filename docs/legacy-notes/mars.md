# 北极星火星附属（NorthstarMarsAddon）逻辑梳理

## 1. 定位与依赖

- 工程 `E:\mc\ModCreator\NorthstarMarsAddon`，`modid = northstarmarsaddon`，主类 `com.vitran.northstarmarsaddon.NorthstarMarsAddon`；`@Mod` 构造器只把 8 个 `DeferredRegister` 挂到 mod bus（方块 / 物品 / 盔甲材质 / 实体 / 效果 / 数据组件 / feature / 创造标签）。
- 目标环境：Minecraft 1.21.1 / NeoForge 21.1.249，Java 21，Gradle 插件 `net.neoforged.moddev 2.0.144`。
- 定位：`Create: Northstar Redux（northstar）` 的火星维度扩展。**编译期不引用 Northstar 的任何类**，集成全部走注册名 / 标签 / 数据包 ID。
- `neoforge.mods.toml` 依赖：`neoforge`、`minecraft`、`northstar`、`create`、`geckolib` 全为 `required`。
- 版本区间（`gradle.properties`）：`northstar=[0.6.5,)`、`create=[6.0,)`、`geckolib=[4.7,5.0)`、`neoforge=[21.1,)`。
- 开发期依赖（`build.gradle`，仅 run* 生效）：`northstar-redux:0.6.5+1.21.1`、`create:6.0.10+mc1.21.1`、`geckolib:Grwn5rUB`（用 Modrinth version id 才拿得到 NeoForge 构建）；`compileOnly` 只有 GeckoLib。
- 软取 Northstar 方块的入口：`world/NorthstarContent`（按注册名 `BuiltInRegistries.BLOCK.get(...)`，`ConcurrentHashMap` 缓存，缺失按 `Blocks.AIR` 处理）。

## 2. 注册内容清单

### 2.1 方块

`registry/ModBlocks`（`DeferredRegister.Blocks`）共 6 个：

| 注册名 | 类 | 关键属性 |
|---|---|---|
| `red_ironstone` | `Block` | `COLOR_RED`、`strength(1.5F, 6.0F)`、需正确工具 |
| `mars_ice` | `MarsIceBlock`（继承 `HalfTransparentBlock`） | `friction 0.98`、`strength 0.5`、`GLASS` 音、`noOcclusion`、禁止生成生物、非红石导体 |
| `dry_ice` | `DryIceBlock` | `strength 0.6`、`GLASS` 音、`noOcclusion` |
| `mars_strawberry_crop` | `MarsStrawberryCropBlock`（继承 `CropBlock`） | 8 个 age（0–7）、`noCollission`、`randomTicks`、`instabreak` |
| `star_core` | `Block` | `strength(4.0F, 12.0F)`、`lightLevel 12`（v1.12 起召唤不再依赖它） |
| `ares_altar_casing` | `AresAltarBlock` | `strength(5.0F, 20.0F)`、`lightLevel 7`、`NETHER_BRICKS` 音 |

- `mars_ice` 刻意不继承原版 `IceBlock`（不随机融化、挖掘不变成水）；`dry_ice` 的 `randomTick` 是空实现，两者「融化 / 升华」都走配方。
- `mars_strawberry_crop` 的 `mayPlaceOn` 只接受 `#northstarmarsaddon:mars_farmland`，掉落写在 `getDrops` 里，**没有战利品表**。
- 方块物品 5 个（`red_ironstone` / `mars_ice` / `dry_ice` / `star_core` / `ares_altar_casing`）；作物用种子物品。

### 2.2 物品

`registry/ModItems`（`DeferredRegister.Items`）共 25 个（含 5 个方块物品）：

- 材料：`demon_moth_wing_membrane`、`dark_red_scale_dust`、`anti_corrosion_coating`（普通 `Item`；涂层只作为 `portable_cooler` 的合成材料，源码里没有运行时效果）。
- `sandstorm_goggles`：`ArmorItem(ArmorMaterials.IRON, HELMET)`，耐久 220；物品贴图自绘，头部渲染沿用原版铁护甲材质层。
- `portable_cooler`：`PortableCoolerItem`，`stacksTo(1)`；弹夹三态 `coolant_tank`（空）/ `water_coolant_tank` / `liquid_oxygen_coolant_tank` 均为 `CoolantTankItem`，`stacksTo(16)`。
- `mars_strawberry`：nutrition 3、saturationModifier 0.3，100% 概率给抗性提升 200 tick 等级 0；`mars_strawberry_seeds` 为 `ItemNameBlockItem`，指向 `mars_strawberry_crop`。
- `star_core_shard`：`LoreItem` + `fireResistant()`；`star_core_heart`：`LoreItem` + `fireResistant()` + `RARE`。
- `molten_core_sword`：`MoltenCoreSwordItem`，耐久 2400、`EPIC`、`SwordItem.createAttributes(NETHERITE, 8, -2.4F)`（显示攻击力 12）。
- `starcore_cannon`：`StarcoreCannonItem`，耐久 1800、`EPIC`、`createAttributes(NETHERITE, 16, -2.6F)`（显示攻击力 20）。
- 熔核护甲四件：`MoltenCoreArmorItem` + `ModArmorMaterials.MOLTEN_CORE`，耐久 头盔 610 / 胸甲 888 / 护腿 832 / 靴子 721，均 `fireResistant` + `EPIC`。
- 刷怪蛋：`demon_moth_spawn_egg`（0x7A1414 / 0x2B0A0A）、`ares_spawn_egg`（0x5A1A0A / 0xFF6A00）。

### 2.3 实体与 Boss

`registry/ModEntities`（`Registries.ENTITY_TYPE`）共 2 个：

- `demon_moth` → `entity/DemonMothEntity`，**继承原版 `Phantom`** + GeckoLib `GeoEntity`；`MONSTER`、`sized(1.4F, 0.8F)`、`eyeHeight 0.5`、`clientTrackingRange 10`；属性 生命 30 / 攻击 5 / 移速 0.28 / 跟随 32，`xpReward = 10`，`finalizeSpawn` 里 `setPhantomSize(3)`。
- `ares` → `entity/AresEntity`，**继承原版 `IronGolem`** + `GeoEntity`；`sized(1.6F, 3.2F)`、`eyeHeight 2.9`、`fireImmune()`、`clientTrackingRange 12`；属性 生命 800 / 护甲 20 / 击退抗性 1.0 / 攻击 12 / 移速 0.28 / 跟随 64 / `Attributes.SCALE 2.0`，`xpReward = 500`，构造器 `setPersistenceRequired()`（不脱战消失）。

### 2.4 效果 / 数据组件 / 盔甲材质 / 声音

- 效果 `registry/ModEffects`：`corrosion`（`CorrosionEffect`，HARMFUL，色 0x6B0F0F）与 `corrosion_resistance`（`CorrosionResistanceEffect`，BENEFICIAL，色 0x9BB0C9）；`MobEffect` 构造器是 protected，故后者写成公开子类。
- 数据组件 `registry/ModComponents`（`DeferredRegister.createDataComponents`）：`coolant`（`Codec.STRING` + `STRING_UTF8`）、`coolant_charges`（`Codec.INT` + `VAR_INT`）。
- 盔甲材质 `registry/ModArmorMaterials`：`molten_core` = `defense(5, 12, 9, 5, 17)`、附魔值 15、`ARMOR_EQUIP_NETHERITE`、韧性 4.5、击退抗性 0.15（数值为下界合金 ×1.5）；修复材料 `Ingredient.of(#northstarmarsaddon:molten_core_repair)`（火星钢锭）；贴图层 `NorthstarMarsAddon.id("molten_core")` ⇒ `textures/models/armor/molten_core_layer_1/2.png`（均 64×32）。
- 声音：**未注册任何自定义 `SoundEvent`**，全部复用原版 `SoundEvents`（`ARMOR_EQUIP_NETHERITE`、`WITHER_SPAWN`、`GENERIC_EXPLODE`、`PHANTOM_FLAP`、`ELYTRA_FLYING` 等）。

### 2.5 创造模式标签页

- `registry/ModTabs` 只有 1 个标签：`main`，标题键 `itemGroup.northstarmarsaddon.main`，图标 `dark_red_scale_dust`；`displayItems` 依次放入上述 25 个物品。
- `mars_strawberry_crop` 自身不在标签里（只给种子）。

## 3. 世界生成

### 3.1 三个群系（`data/northstarmarsaddon/worldgen/biome/`）

| 群系 ID | 中文名 | temperature / downfall | fog_color | monster 生成 |
|---|---|---|---|---|
| `mars_rusty_wasteland` | 赤色荒原 | 0.8 / 0.4，`temperature_modifier: frozen` | 16767935 | `demon_moth` weight 12，1–2 只 |
| `mars_volcanic_highlands` | 火山灰平原 | 1.2 / 0.1 | 15259336 | `demon_moth` weight 20，1–2 只 |
| `mars_polar_ice_cap` | 火星冰原 | -0.5 / 0.5，`frozen`，`has_precipitation: true` | 14674421 | `demon_moth` weight 8，1 只 |

- 三者共用 `carvers: [minecraft:cave, minecraft:cave_extra_underground]` 与相同 sky/water 色（14331548 / 4159204 / 329011），`spawn_costs` 为空。
- `features` 按 MC 的 11 个装饰步骤逐格排列，同时按注册名引用 Northstar 的项：`northstar:mars_worm_nest`、`northstar:big_dumb_rock`、`northstar:mars_rock`、全部 `northstar:mars_ore_*` 与 `mars_blob_*`；火山群系另有 `mars_lava_lake`、`magma_cluster`、`volcanic_rock_cluster`、`volcanic_ash_cluster`。
- 极地冰冠额外挂 `northstarmarsaddon:mars_ice_pile` 与 `dry_ice_pile`，荒原额外挂 `northstar:mars_roots`。

### 3.2 自定义 feature 类型（`registry/ModFeatures` + `world/`）

- `northstarmarsaddon:mars_surface_layer` → `MarsSurfaceLayerFeature`，配置记录 `MarsSurfaceConfig(top, filler, accent, filler_depth 0–8, accent_chance 0–1)`；逐列把 `northstar:mars_stone` / `mars_deep_stone` 换成顶层（按 `accent_chance` 换 `accent`）+ 至多 `filler_depth` 层次表层，逐列比对起点群系以免跨群系铺错，异常只跳过本区块。三份配置：`mars_surface_layer_rusty`（`mars_sand` / `mars_gravel` / `red_ironstone`，depth 3、chance 0.3）、`_volcanic`（`volcanic_ash` / `volcanic_rock` / `red_ironstone`，0.25）、`_polar`（`mars_ice` / `mars_soil` / `dry_ice`，0.45）。
- `northstarmarsaddon:spire` → `SpireFeature`，配置 `SpireConfig(state, min_height 1–32, max_height 1–32, base_radius 0–3)`；中心柱高 `Mth.nextInt(min,max)`、非中心列高 `max(1, height/3)`，只往空气里长且坐标夹在本区块内。配置：`volcanic_spire`（`volcanic_rock`，6–18，半径 1）、`ice_spire`（`dry_ice`，5–14，半径 0）。
- `northstarmarsaddon:crater` → `MarsCraterFeature`，配置 `MarsCraterConfig(radius 1–16, depth 1–16, rim_height 0–6, lava_block, ice_block, fill_block, core_radius 0–16)`。剖面 `dig = round(depth × (1 - ratio²))`、`rim = round(rim_height × (ratio-0.75)/0.25)`（仅 `ratio > 0.75`），`airY = surface - dig + rim`；`d ≤ core_radius` 铺冰核（厚度 `min(max(3, 6-d), min(6, dig-rim))`），`core_radius < d ≤ radius×0.9` 铺 1–2 格熔岩，其余铺岩石，坑底另垫 3 格 `FLOOR_MATERIAL_DEPTH` 岩石。预算常量 `MAX_COLUMNS_PER_PLACEMENT = (2×16+1)²`、`MAX_WRITES_PER_COLUMN = 64`、`MAX_TOTAL_WRITES = 24000`，`ALLOW_NEIGHBOUR_CHUNKS = true`（Δchunk ≤ 1）。两份配置：`lava_crater`（半径 16、深 12、缘 5、`core_radius 0`、`minecraft:lava` + `magma_block` + `northstar:volcanic_rock`）、`ice_crater`（同尺寸、`core_radius 16`，`lava_block` 与 `ice_block` 都是 `mars_ice`）。

### 3.3 placed_feature

共 11 个：

- 三个 `mars_surface_layer_*`：`count 1` + `in_square` + `heightmap WORLD_SURFACE_WG` + `biome`；**必须带 heightmap**，否则末尾的 `minecraft:biome` 过滤器会拿世界最低点 Y 查群系（命中洞穴群系），特征一个区块都不跑。
- `lava_crater` / `ice_crater`：`rarity_filter chance 320` + `in_square` + heightmap + biome。
- `raw_martian_iron_vein`：`count 6`，`minecraft:ore`，size 9，target `northstar:mars_stone`；`raw_martian_iron_boulder`：`count 4`，`block_pile`（`northstar:mars_iron_ore`）。
- `mars_ice_pile` / `dry_ice_pile`：各 `count 6`，`block_pile`，仅极地冰冠引用。
- `volcanic_spire`（count 10）与 `ice_spire`（count 12）：数据完整，但**没有任何群系的 `features` 列表引用它们**，当前版本不会实际生成。

### 3.4 注入方式与 northstar 标签

- `world/MarsBiomeInjector` 监听 `LevelEvent.Load`，只处理 `ServerLevel` 且只处理 `northstar:mars`；维度身份靠标记群系 `northstar:martian_dunes` 判定。
- 注入手法：反射替换 `MultiNoiseBiomeSource` 私有 `parameters` 字段与 `BiomeSource` 的 `possibleBiomes` 缓存（顺序是先缓存、后参数表，避免半注入），随后 `refreshFeaturesPerStep()` + `validate()` 自检；任一步失败整体回滚为「只有 Northstar 群系」。应急开关 `-Dnorthstarmarsaddon.disableBiomeInjection=true`；静态 `injectionAttempted` / `injectionFailed` 保证每 JVM 只尝试一次。
- 气候轴全部放宽：`T[-1.5,1.5]`、`H[-1,2]`、`C[-1.3,1.3]`、`E[-1,1]`、`W[-1.3,1.3]`，`depth[-0.5,1.49]`（上界卡 1.49，让 Northstar 洞穴群系的 depth `[1.5,2]` 在地下仍胜出）。
- HIGH 层 = `C[-0.10,1.3]` 且 `E[-1,-0.05]`（约 23–24%、平均 Y≈82），按 weirdness 三带：火山高地 `W[-1.3,-0.16]`、`northstar:martian_peaks` `W[-0.16,0.15]`、极地冰冠 `W[0.15,1.3]`。
- LOW 层 = 2 段 C/E（`C[-1.3,-0.10]` 全域 + `C[-0.10,1.3]` 且 `E[-0.05,1]`）× 6 条 W 带：`martian_dunes [-1.30,-0.62]`、火山高地 `[-0.62,-0.46]`（露头窄带）、赤色荒原 `[-0.46,0.20]`、极地冰冠 `[0.20,0.36]`（露头窄带）、`martian_highlands [0.36,0.68]`、赤色荒原 `[0.68,1.30]`。
- 合计 15 个互不重叠的地表盒子：本模组群系 10 块（荒原 4、火山 3、冰原 3）+ Northstar 三个地表群系 5 块（「一条换多条」重排）；洞穴群系完全不动。
- 注入的 northstar 标签（`data/northstar/tags/`）：`entity_type/{can_survive_cold,can_survive_heat,doesnt_require_oxygen}`（均含 `demon_moth`、`ares`）、`worldgen/biome/{has_mars_base,mars_biomes}`（均含三个新群系）。

## 4. 核心机制

### 4.1 沙尘暴（`world/Sandstorm`，服务端权威静态状态机）

- 状态：`active`、`intense`、`startGameTime`、`endGameTime`、`windDegrees`、`nextRollGameTime`、`origin`（`NATURAL` / `BOSS` / `COMMAND`）。
- 常量：`DEFAULT_DURATION = 12000`；自然时长 `3600–12000`；`ROLL_INTERVAL = 200`；`ROLL_ODDS = 90`（每 200 tick 掷 1/90，期望约 15 分钟一场）；`INTENSE_CHANCE = 4`（1/4 强化）；`COOLDOWN_TICKS = 1200`。
- 触发三条路径：① 自然 —— 火星有玩家、`now ≥ nextRollGameTime`、`now % 200 == 0` 时掷骰命中；② Boss —— `AresEntity` 阶段二与鳞粉风暴调 `trigger(level, 12000, true, Origin.BOSS)`；③ 指令。
- `trigger` 拒绝非 `northstar:mars` 维度，随机风向 0–359°，`endGameTime = now + max(20, ticks)`，随后广播并发全员 actionbar 提示。
- 结束：到期 `stop`；`stopIfFromBoss` 只收 `Origin.BOSS` 那场（`AresEntity` 的 `die`、`remove(KILLED)`、进入阶段三、被冷却打落四条路径都调用，幂等）。
- 同步：开始 / 结束 / 每 20 tick 广播 `SandstormPayload`；登录与换维度用 `syncTo` 补发（不在火星时明确发「无沙尘暴」）。
- 风向方位键 `windKey(deg)` = `{"n","ne","e","se","s","sw","w","nw"}[(deg+22)/45 mod 8]`，服务端命令与 HUD 共用。

### 4.2 沙尘暴的影响：装备磨损与腐蚀

- 装备磨损（`event/PlayerEvents.wearEquipmentBySandstorm`）：每 `SANDSTORM_WEAR_INTERVAL = 200` tick，对每个 `HUMANOID_ARMOR` 与 `HAND` 槽位的可损坏物品各 `hurtAndBreak(1)`；豁免 = 创造 / 旁观、`!canSeeSky`、穿齐熔核四件套（`ArmorEvents.wearsFullSet`）、头盔槽的 `sandstorm_goggles`。
- 腐蚀（`effect/CorrosionEffect`）：`duration % 20 == 0` 时结算 —— `hurt(magic, 1 + amplifier)`，并让玩家每个 `HUMANOID_ARMOR` 槽再掉 1 点耐久。
- 耐腐蚀（`CorrosionResistanceEffect`）：`PlayerEvents.onPlayerTick` 每 40 tick 数一次命中 `#northstarmarsaddon:corrosion_resistant_armor` 的件数，达到 `REQUIRED_ARMOR_PIECES = 3` 就挂 100 tick 的 `corrosion_resistance`（头盔位可留给护目镜）。
- 恶魔蛾的鳞粉喷洒、咬击与阿瑞斯的追踪鳞粉都会先检查该效果，再决定是否施加腐蚀。

### 4.3 阿瑞斯（`entity/AresEntity`）

- 召唤链（`block/AresAltarBlock`）：手持 `minecraft:lava_bucket` 右击 `ares_altar_casing` → 祭坛上方 `+1` 生成 Boss（朝向 `player.getYRot()+180`）、`setDomainCenter(pos)`、熔岩桶变空桶（创造不消耗）、`WITHER_SPAWN` + LAVA/FLAME/LARGE_SMOKE 粒子；`SUMMON_RADIUS = 48` 内已有阿瑞斯则拒绝（`altar_already`），手持其它物品右击只给 `altar_hint`。
- 属性：生命 800 / 护甲 20 / 击退抗性 1.0 / 攻击 12 / 移速 0.28 / 跟随 64 / 体型 ×2；`fireImmune`、免疫 `IS_FIRE` / `IS_DROWNING` / `IS_FALL`、`causeFallDamage` 恒 false；`push(...)` 与 `knockback(...)` 覆写为空实现（属性挡不住直接改速度的推力）。
- 核心温度：唯一写入口 `setCoreTemperature`（夹到 `[0, 2200]` 并同步 `DATA_SHIELD`）；初始 1200，每 20 tick `+15 + phase×5`，每次**实际结算**的伤害 `+HEAT_PER_HIT = 20`；阈值 `SHIELD/OVERHEAT = 1500`、`BRITTLE = 500`。
  - `> 1500`：伤害 ×0.4（减伤 60%）、攻击 +50%（`doHurtTarget` 追打）、移速 `+0.3 ADD_MULTIPLIED_BASE`、**展开护盾**；护盾期 `hurt` 直接返回 false ⇒ 完全无敌、受击不再升温，破盾只能靠冷却器。
  - `< 500`：受伤 ×3、每 60 tick 挂 `MOVEMENT_SLOWDOWN` 等级 1。
- 阶段一（血量 `>0.66`，熔火形态）：岩浆拳 `AresMeleeGoal` 冷却 `80` tick（4 秒）、前摇 `16` tick（0.8 秒），`getAttackBoundingBox().inflate(2.0)` 提供 4 格触及，命中后 3 格内其它生物再吃 6 点 + 60 tick 着火。
- 阶段一另两个技能：地热喷发（CD `160`、前摇 `30`、6 个落点、半径 2、8 点、着火 `100` tick、选点搜索 32 格）、召唤恶魔蛾（CD `900`、前摇 `20`、2–4 只，`markSummoned()` 后代不掉落不给经验）。
- 阶段二（`>0.33`，风暴形态）：`hovering = true` + `setNoGravity(true)`，`tickHovering` 把 Boss 拉到地表 + `HOVER_HEIGHT = 8`；`summonSandstorm` 召唤强化沙尘暴（只在该维度 `isMars` 时调用，每秒若已停会重开）。
- 阶段二技能：鳞粉风暴 CD `120`、前摇 `20`、8 枚追踪鳞粉（速度 0.55 格/tick、最长 80 tick、命中 6 点魔法伤害 + `corrosion` 100 tick 等级 1、射线被方块挡住即消散）。
- 阶段二冷却窗口：未压到 500 ℃ 以下时每 10 tick `heal(5)`（≈10 HP/s）；压到 500 ℃ 以下则坠落、`vulnerabilityTicks = 200`（10 秒脆弱期，受伤 ×3、停止移动）并收掉沙尘暴。
- 阶段三（其余，过载形态）：停止移动、`breakArmor()` 用 `-20 ADD_VALUE` 把护甲打到 0、`selfDestructTimer = 1200`（60 秒）；每 `40` tick 起一次前摇 `20` 的全图地热喷发（每个玩家脚下各一点、10 点、半径 2、着火 100 tick），倒计时归零调用 `selfDestruct`。
- 前摇调度（`startWindup` / `windupTelegraph` / `executePendingSkill`）：前摇期间站定，每 5 tick 画落点（SMOKE + LAVA）或 Boss 身边鳞粉，结束时才结算，玩家可看着预警跑开。
- 领域：`DOMAIN_RADIUS = 48`；`setTarget` 直接挡掉领域外目标，`enforceDomain` 每 tick 解除领域外目标的锁定，无目标且离领域中心 >8 格时走回中心。
- 自爆（`selfDestruct`）：`level.explode(power 6, ExplosionInteraction.MOB)`；另对 32 格内非蛾非 Boss 实体按距离线性结算最多 100 点爆炸伤害 + 着火 120 tick；`KineticMachines.destroyNearby(level, pos, 16)` 摧毁 Create 动力方块；最后 `setHealth(0)` + `die(...)`（不能用 `kill()`：它等价 `hurt(genericKill)`，会被自己的护盾挡掉）。
- 掉落（`dropCustomDeathLoot`，`selfDestructed` 时整段跳过且不给经验）：星核碎片 2–4、`northstar:martian_steel_ingot` 4–8（取不到退化为星核碎片）、`star_core_heart` 15%、恶魔蛾翼膜 2、暗红鳞粉 4–8；经验 `xpReward = 500`。
- 血条与存档：`ServerBossEvent(RED, NOTCHED_10)`，名称键 `boss.northstarmarsaddon.ares.bar`（含阶段名与核心温度）；存档字段 `CoreTemperature` / `AresPhase` / `SelfDestructTimer` / `VulnerabilityTicks` / `Hovering` / `SelfDestructed`。

### 4.4 便携式冷却器与冷却液（`item/PortableCoolerItem`、`item/CoolantTankItem`）

- 装填：潜行 + 右击，副手的罐经 `CoolantTankItem.kindOf` 读出种类，写入 `coolant` 与 `coolant_charges = TANK_CAPACITY = 1000`（mB），罐 `shrink(1)`；已满时只提示 `cooler.full` 并 `fail`，不吃罐子。
- R 键换弹：`client/ModKeys` 发 `ReloadCoolerPayload`，服务端 `reloadFromInventory` 返回 `NO_COOLER` / `ALREADY_FULL` / `NO_TANK` / `RELOADED` 四态；成功播 `BUCKET_EMPTY`，无罐播 `DISPENSER_FAIL` 并给红字。
- 开火：按住右键 `startUsingItem`（`getUseDuration` 72000、`UseAnim.NONE`）；每 tick 消耗 `DRAIN_PER_TICK = 5` mB，`remainingUseDuration % FIRE_INTERVAL(2) == 0` 时发射；一罐 1000 mB ⇒ 200 tick ≈ 10 秒，打空自动停手 + `DISPENSER_FAIL`。
- 射流：射程 `RANGE = 12`，先 `level.clip` 求命中点，再在 `expandTowards` 框里找最近活体；粒子 `SNOWFLAKE`（液氧）/ `SPLASH`（水）从第 2 段开始，音效 `GENERIC_SPLASH` 音量 0.20。
- 命中效果：实体只 `clearFire()`；命中 `AresEntity` 时 `applyCoolant(kind, round(perMb × 5 × 2))`；**无伤害、无击退**（刻意不用 `push`）。
- 降温效率：`WATER_COOLING_PER_MB = 0.30`、`OXYGEN_COOLING_PER_MB = 0.80` ⇒ 单次射流 3 ℃ / 8 ℃，一整罐 300 ℃ / 800 ℃。
- UI：耐久条显示剩余量（`getBarWidth = 13 × amount / 1000`，水 `0x3399FF`、液氧 `0x66CCFF`）；tooltip 键 `cooler.empty` / `cooler.filled` / `cooler.usage` / `cooler.reload`。
- 罐来源：`create:filling` 配方，1000 mB 的 `minecraft:water` 或 `northstar:liquid_oxygen` 灌 `coolant_tank`。

### 4.5 火星草莓作物（`block/MarsStrawberryCropBlock`）

- 只能种在 `#northstarmarsaddon:mars_farmland`（标签值 `northstar:mars_farmland`），走 `CropBlock` 默认 8 个年龄阶段。
- 掉落（`getDrops`，无战利品表）：成熟（age 7）→ 1–2 个 `mars_strawberry`，另有 50% 概率额外 1 个种子；未成熟 → 只掉种子。
- 种子来源：`mars_strawberry` 或 `northstar:martian_strawberry` 无序合成各 1 个种子。

### 4.6 星核炮（`item/StarcoreCannonItem` + `world/StarcoreBombardment`）

- 右击（仅服务端）：冷却中或该玩家已有轰炸进行中（`isActiveFor`）则 `fail`；否则用 `AIM_RANGE = 48` 的射线取点，`StarcoreBombardment.start`，加 `COOLDOWN_TICKS = 600`（30 秒）冷却、扣 1 点耐久、`swing`、播 `FIRECHARGE_USE`。
- 轰炸参数：`DURATION_TICKS = 100`（5 秒）、`INTERVAL_TICKS = 10`（共 10 次）、`DAMAGE = 20`、`RADIUS = 6`；每次 `blast` 用 `explosion(null, owner)` 结算伤害 + `setRemainingFireTicks(60)`，跳过 owner 与其 `isAlliedTo` 的实体。
- 表现与实现：非 blast tick 画 `END_ROD` 环形预警（半径从 1 递增到 6）；本体是服务端静态 `List<Instance>`，由 `ServerEvents` 每 tick 推进，`ServerStoppedEvent` 时 `clear()`（不持久化）。

### 4.7 融核巨剑（`item/MoltenCoreSwordItem`）

- 右击「地热斩」：冷却 `SLASH_COOLDOWN_TICKS = 60`（3 秒）、`SLASH_RADIUS = 8`、`SLASH_DAMAGE = 20`、`SLASH_FIRE_TICKS = 100`。
- 扇形由视线点积 `ARC_COS = 0.2`（≈±78°）界定，排除自己、旁观者与 `isAlliedTo` 的实体。
- 表现：沿视线 9 段 `FLAME` + `LAVA` 粒子，播 `BLAZE_SHOOT` 与 `FIRECHARGE_USE`；扣 1 点耐久。

### 4.8 熔核护甲套装（`event/ArmorEvents`）

| 部位 | 效果 | 实现 |
|---|---|---|
| 头盔 | 无限时长夜视 | `LivingEquipmentChangeEvent` + 每 tick `updateHelmetNightVision` 挂 `INFINITE_DURATION`；摘下只清「无限时长」的那条 |
| 胸甲 | 受到伤害 ×0.8 | `LivingIncomingDamageEvent` 里 `setAmount(amount × 0.8)` |
| 护腿 | 移速 +20% | 瞬时修饰符 `molten_core_leggings_speed`（`ADD_MULTIPLIED_BASE 0.2`） |
| 靴子 | 摔落伤害免疫 | 取消 `DamageTypeTags.IS_FALL` |
| 全套 | 火焰 / 岩浆 / 热地面免疫 + 每 tick `clearFire` + 免疫沙尘暴磨损 | 取消 `IS_FIRE`；`PlayerEvents` 用 `wearsFullSet` 判断是否磨损 |

## 5. 事件与网络

`@EventBusSubscriber` 分布（14 个类各一处注解，其中 `ModKeys` 与其内部类 `TickHandler` 分别一处）：

- **game bus（默认）**：`ServerEvents`（`ServerTickEvent.Post` → `Sandstorm.tick` + `StarcoreBombardment.tick`；`PlayerLoggedInEvent` / `PlayerChangedDimensionEvent` → `Sandstorm.syncTo`；`ServerStoppedEvent` → `reset` + `clear`）。
- game bus：`PlayerEvents`（`PlayerTickEvent.Post` 耐腐蚀判定 + 磨损、`PlayerEvent.BreakSpeed` 火星钢镐 ×2）、`ArmorEvents`（`LivingEquipmentChangeEvent`、`PlayerTickEvent.Post`、`LivingIncomingDamageEvent`）、`ModCommands`（`RegisterCommandsEvent`）、`MarsBiomeInjector`（`LevelEvent.Load`）。
- **mod bus**：`CommonEvents`（`EntityAttributeCreationEvent`；`RegisterSpawnPlacementsEvent` 给恶魔蛾注册 `NO_RESTRICTIONS` + `MOTION_BLOCKING_NO_LEAVES` + 「非和平且够暗」的 REPLACE 规则）、`ModNetwork`（`RegisterPayloadHandlersEvent`）。
- **仅客户端（`value = Dist.CLIENT`）**：`ClientSetup`（mod bus，注册 2 个渲染器）、`ModKeys`（mod bus `RegisterKeyMappingsEvent` + 内部 game bus `ClientTickEvent.Post`）、`ClientSandstormState`、`SandstormClientEffects`、`SandstormFogHandler`、`SandstormHud`。

网络载荷（`network/ModNetwork`，协议版本 `"1"`）：

- `northstarmarsaddon:reload_cooler` → `ReloadCoolerPayload`（`StreamCodec.unit` 空负载，`playToServer`）→ `handleReload`。
- `northstarmarsaddon:sandstorm` → `SandstormPayload(active, intense, windDegrees, remainingTicks)`（`BOOL/BOOL/VAR_INT/VAR_INT`，`playToClient`）→ `ClientSandstormState.accept`。

指令（`command/ModCommands`，权限等级 2）：

- `/northstarmars sandstorm …` 与独立别名 `/sandstorm …`（两处注册同一个 builder）。
- 子命令 `start [seconds]`（`intRange 10..3600`，默认 12000 tick）、`intense [seconds]`、`stop`、`status`。
- `northstar:mars` 未加载时统一报 `...no_mars` 失败；执行者不在火星时用 `started_elsewhere` 变体提示。

## 6. 客户端

- 状态源：`client/ClientSandstormState` —— 唯一的客户端数据源，`accept(payload)` 写四个字段，`ClientTickEvent.Post` 本地递减 `remainingTicks`，`remainingSeconds()` 向上取整。
- 雾（`client/SandstormFogHandler`）：无护目镜时远端 `DUST_FAR_CALM = 72` / `DUST_FAR_INTENSE = 32`、近端压到 `far × 0.25`；戴护目镜时远端推到 `512`、近端至少 `64`。
- 雾的关键点：**必须 `setCanceled(true)`**，否则 NeoForge 的 `ClientHooks#onFogRender` 不会把这两个值写进渲染状态（`ComputeFogColor` 则是无条件应用的）。
- 雾色：向 (0.72, 0.55, 0.36) 用 `Mth.lerp` 混合，普通强度 0.55、强化 0.85。
- 粒子与音（`client/SandstormClientEffects`）：每 `PARTICLE_INTERVAL = 2` tick 撒 6（普通）/ 14（强化）个 `WHITE_ASH`，戴护目镜降到 1/4（下限 1），每第 3 个补 `SMOKE`；生成盒半径 14、高 5。
- 风向与风速：`windX = sin(rad)`、`windZ = -cos(rad)`，速度 0.35–0.75 格/tick；每 70 tick 播 `ELYTRA_FLYING`（音量 0.4、音高 0.55）。
- 屏幕沙尘罩：`RenderGuiEvent.Pre` 用 `graphics.fill` 铺 `0x2E7A5228`（普通）/ `0x557A4A20`（强化）；戴护目镜不铺。
- HUD（`client/SandstormHud`，`RenderGuiEvent.Post`，左上 6/6、行高 11）：所有人看到标题；无护目镜只多一行红色警告（视野受损 + 装备被磨损）；戴护目镜时显示「强化沙尘暴」（若强化）、风向（八方位键）与剩余秒数。
- 按键：`client/ModKeys` 的 `RELOAD_COOLER`（默认 `GLFW_KEY_R`，分类 `key.categories.northstarmarsaddon`），仅手上是冷却器时发包。
- 渲染器：`client/DemonMothRenderer`（`GeoEntityRenderer` + `DefaultedEntityGeoModel(demon_moth)`，`shadowRadius 1.3`、`entityCutoutNoCull`）；`client/AresRenderer`（`shadowRadius 1.8`）。
- 阿瑞斯贴图分档：`PhaseModel` 覆写 `GeoModel#getTextureResource`，按 `getPhase()` 取 `ares_phase1/2/3.png`（128×128）；`ShieldLayer` 在 `hasShield()` 时用 `entityTranslucent(ares_shield.png)` 只重画 `shield` 骨骼，主体仍走 `entityCutoutNoCull`。
- 动画名与 `RawAnimation` 一致：恶魔蛾 `idle/fly/attack`；阿瑞斯 `idle/walk/attack/eruption/storm/overload/self_destruct/slam/summon/core_purge`。

## 7. 数据包与资源布局

- `data/northstarmarsaddon/worldgen/`：`biome`（3）、`configured_feature`（11）、`placed_feature`（11）。
- `data/northstarmarsaddon/loot_table/blocks/`：`ares_altar_casing`、`dry_ice`、`mars_ice`、`red_ironstone`、`star_core`（作物没有战利品表）。
- `data/northstarmarsaddon/recipe/`（18 个）：7 个 `create:mechanical_crafting`（祭坛、4 件熔核护甲、巨剑、星核炮）、2 个 `create:filling`（`minecraft:water` / `northstar:liquid_oxygen` 各 1000 mB 灌空罐）、9 个原版工作台配方（涂层、空罐、干冰→液氧桶、火星冰→水桶、两种种子、冷却器、护目镜、星核方块）。
- `data/northstarmarsaddon/tags/`：`block/mars_farmland`、`block/martian_hard_rock`、`item/corrosion_resistant_armor`、`item/martian_steel_pickaxes`、`item/molten_core_repair`、`fluid/low_pressure_lava`（值仅 `minecraft:lava`，源码中暂无使用点）。
- `data/northstar/tags/`：`entity_type/{can_survive_cold,can_survive_heat,doesnt_require_oxygen}`、`worldgen/biome/{has_mars_base,mars_biomes}`。
- `assets/northstarmarsaddon/`：`lang/en_us.json` 与 `zh_cn.json`（同键集）、`blockstates`（6）、`models/block`（13，含作物 8 个阶段）、`models/item`（25）。
- 贴图：`textures/block`（13）、`textures/item`（18）、`textures/entity`（6；`ares.png`、`ares_phase1-3.png`、`ares_shield.png`、`ares_wing.png` 均 128×128，`demon_moth.png` 64×64）、`textures/models/armor`（`molten_core_layer_1/2.png`，64×32）。
- 模型与动画：`geo/entity`（`ares.geo.json` 含 `body` / `shield` / `core` 等骨骼、`demon_moth.geo.json`）、`animations/entity`（2 份）。
- 数据侧约定：三个群系的 `features` 直接按注册名引用 Northstar 的 placed_feature（注意 `mars_` 前缀命名）；同一装饰步骤内的相对顺序需与 Northstar 火星群系一致，否则会触发 `Feature order cycle found`。

## 8. 已知限制与注意事项

1. **群系注入是反射 + 一次性**：只在 `ServerLevel` 加载 `northstar:mars` 时执行（静态 `injectionAttempted`，不支持热重载）；Northstar 若改用 preset 参数表或非 `multi_noise` 群系源，会整体放弃并保持原样。
2. **气候盒子是实测标定值**：注释里的区间来自对 `randomState().sampler()` 的采样统计，Northstar 若改 `noise_settings/mars.json`，份额与分布会漂移。
3. **`MarsCraterFeature` 常量与注释不一致**：代码 `MAX_RADIUS = 16`，类注释与 JSON 说明写「上限 8」；`lava_crater` / `ice_crater` 实际配置 `radius 16`（约 800 列），可能触达 `MAX_TOTAL_WRITES = 24000` 而被截断。
4. **`volcanic_spire` / `ice_spire` 是未被引用的数据**：configured + placed 都在，但没有群系 `features` 列表引用，当前不会生成。
5. **对 Northstar 注册名硬依赖**：`northstar:mars_stone` / `mars_deep_stone` / `mars_farmland` / `martian_steel_ingot` / `martian_pickaxe` / `martian_steel_*` 护甲 / `liquid_oxygen` / `titanium_sheet` 等；改名后标签静默变空、配方与挖掘加速失效。
6. **`NorthstarContent.block()` 的告警分支不会触发**：`BuiltInRegistries.BLOCK.get` 返回注册表默认值而非 null，缺失时靠调用方 `stone == Blocks.AIR` 判断后静默跳过。
7. **恶魔蛾对一切爆炸伤害免疫**：`DemonMothEntity#isInvulnerableTo` 含 `IS_EXPLOSION`（目的是让阿瑞斯自爆的 `level.explode` 不伤召唤物），副作用是 TNT 与苦力怕也伤不到它；它同时免疫火焰、溺水、冻结、仙人掌、甜浆果、窒息、摔落，且 `isSunBurnTick()` 恒 false。
8. **阿瑞斯护盾期 `hurt()` 返回 false**：因此 `LivingEntity#kill()` 会被护盾挡掉；`selfDestruct` 特意改用 `setHealth(0)` + `die(...)`，后续若在别处调 `kill()` 需注意。
9. **沙尘暴与星核炮击都是进程内静态状态**：不写存档，`ServerStoppedEvent` 时清空；沙尘暴是服务端唯一实例且只对 `northstar:mars` 生效，与主世界天气无关。
10. **冷却器降温是细水长流**：单次射流 3 ℃ / 8 ℃，一整罐 300 ℃ / 800 ℃；护盾期（>1500 ℃）想破盾通常要多罐，因为每秒还会自然升温 `15 + phase×5`。
11. **`anti_corrosion_coating` 目前只是合成材料**：仅出现在 `portable_cooler` 配方，没有运行时效果；耐腐蚀来自 `#northstarmarsaddon:corrosion_resistant_armor` 的 3 件套判定。
12. **两处判定偏严的玩法**：草莓只能种在 `northstar:mars_farmland`，且掉落集中在代码里（附魔 / 精准采集不参与）；沙尘暴磨损要求 `canSeeSky`，室内与密封基地不受影响。
13. **`world/KineticMachines` 靠类名反射** `com.simibubi.create.content.kinetics.base.KineticBlockEntity`：恶魔蛾优先扑向最近的一台，阿瑞斯自爆时 16 格内全部 `destroyBlock`；缺 Create 时该类静默失效。
14. **换弹的物品栏扫描范围**：`PortableCoolerItem.inventoryStacks` 遍历 `player.getInventory().getContainerSize()`（36 主格 + 4 护甲 + 1 副手），只有找到冷却器时 R 键换弹才真正消耗罐子。
15. **本文只做静态核对**：所有数值取自源码常量与 JSON（行内已标注类名 / 文件名），未启动客户端实测；`README.md` 中部分描述（祭坛 3×3×3 结构、冷却器「各 8 次」、旧掉落表）早于当前实现，应以本文与源码为准。
