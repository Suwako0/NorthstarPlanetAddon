# 北极星金星附属（Create: Phosphoros）逻辑梳理

> 依据：`E:\mc\ModCreator\phosphoros` 的 Java 源码（`src/main/java/com/phosphoros`，116 个 `.java`）、`src/main/resources`（命名空间 `phosphoros`）以及 `build.gradle` / `gradle.properties` / `META-INF/neoforge.mods.toml`。README 与 `docs/` 只作交叉参考，冲突处以源码为准；源码无法确认的标「未确认」。
> 每个 `###`/`##` 小节末尾不再重复「详见」，凡提到数值均来自对应类里的常量或配置默认值。

## 1. 定位与依赖

| 项 | 值 |
|---|---|
| Minecraft / 加载器 | 1.21.1 / NeoForge 21.1.249（ModDevGradle 2.0.144，Java 21 toolchain，`options.release=21`） |
| Mod ID / 入口 | `phosphoros` / `com.phosphoros.Phosphoros`（`@Mod`，`MODID="phosphoros"`，`id(path)` 拼 `phosphoros:<path>`） |
| 显示名 / 版本 / 许可 | `Create: Phosphoros` / `0.2.0-skeleton` / MIT |
| 关键常量 | `Phosphoros.VENUS_CLOUDS = phosphoros:venus_clouds`；`Phosphoros.LOGGER` = LogUtils |

- `neoforge.mods.toml`：`neoforge`、`minecraft` 为 `required`；`northstar`(`[0.6.5,)`)、`create`(`[6.0,)`)、`geckolib`、`curios`、`tacz` 全为 `optional` + `ordering=AFTER`；TaCZ 与 Curios 的 `versionRange` 写死 `*`（TaCZ 自身版本形如 `1.1.8-hotfix-r6`）。
- 依赖策略是三层隔离：① 主包只碰本模组类型 —— `compat/MachineEnvBridge`、`compat/MachineTickerBridge` 无任何第三方 import；② 反射层 `compat/create/CreateReflect`、`compat/northstar/NorthstarReflect` 先用 `ModList.get().isLoaded(...)` 判定，再 `Class.forName` 取桥接类方法（`resolve()` 只跑一次，失败只打 warn 并永久降级为 null/false）；③ 真正引用第三方类的只有 `compat/create/CreateMachineBridge`、`compat/create/KineticPneumaticMotorEntity`、`compat/northstar/NorthstarMachineBridge`，仅反射命中后才被类加载。
- 编译期门禁在 `build.gradle`：`libs/` 下若齐备 `create-1.21.1-6.0.10.jar`+`flywheel`+`ponder`+`Registrate`+`northstar-0.6.5+1.21.1.jar` 则 `compileOnly files(...)` 并编译桥接类，否则用 `sourceSets.main.java.exclude` 排除它们退回纯反射（当前五个 jar 都在）；开发运行时用三条 `runtimeOnly maven.modrinth:...`，可用 `-PdevMods=false` 关闭；`-PnoWorldgen` 把 `dimension/**`、`dimension_type/**`、`worldgen/**` 从 jar 排除，用于崩溃二分。
- TaCZ 联动只有数据 + 一次自检：`compat/TaczCompat.onServerStarted` 在 `ServerStartedEvent` 打印 `#phosphoros:ballistic` 解出的伤害类型条数（`<=1` 时 warn），不 import 任何 TaCZ 类。

## 2. 注册内容清单

全部注册在 `Phosphoros` 构造器里显式 `register(modBus)`；`ModEntities::registerAttributes`（`EntityAttributeCreationEvent`）与 `ModCapabilities::register`（`RegisterCapabilitiesEvent`）走 mod bus listener；`ModConfig.Type.COMMON` 注册为 `phosphoros-common.toml`；游戏总线注册 `CombatEvents`、`PlayerEvents`、`PhosphorosCommands` + listener `TaczCompat::onServerStarted`。

### 2.1 方块与机器（`registry/ModBlocks` 16 个 + `ModFluids` 的硫酸液体方块 = 17，与 17 个 blockstate 一致）

| 注册名 | 类 | 关键属性 |
|---|---|---|
| `tungsten_block` / `sulfur_crystal_block` / `cloud_stone` | `Block` | 硬度 5.0-6.0 METAL / 1.5-3.0 光照 6 AMETHYST / 0.8-2.0 CALCITE |
| `pressure_tank` / `compressor` / `pressure_cooker` | `PressureTankBlock` / `CompressorBlock` / `PressureCookerBlock` | 8000 mB 罐 / 曲柄产气 / 需 8 bar、4 份每 200 tick |
| `high_pressure_furnace` / `radiator` | `HighPressureFurnaceBlock` / `RadiatorBlock` | 光照 4、自定义配方 / 9×5×9 内 −40 °C |
| `fungus_bed` / `condenser` / `cloud_lift` | `FungusBedBlock` / `CondenserBlock` / `CloudLiftBlock` | 9600 tick 产菌丝纤维 2–4 / ≥2 bar+≥300 °C 产冰 / 光照 6、气压传 32 格 |
| `pneumatic_motor` / `acid_pump` | `PneumaticMotorBlock` / `AcidPumpBlock` | 20 mB/t → 32 RPM / 浸酸 200 mB/t |
| `giant_fungus_crop` / `acid_vegetable_crop` / `cloud_algae_crop` | `PhosphorosCropBlock` | age 0–3；随机刻 1/12、1/18、1/9；产物 ×2–4、×2、×3 |
| `sulfuric_acid` | `LiquidBlock`（在 `ModFluids` 注册） | 无碰撞、`strength(100)`、`noLootTable()` |

`block/AbstractMachineBlock` 统一挂 ticker：服务端 tick 先调 `MachineTickerBridge.tickCreateMotor(...)`，再对 `AbstractMachineBlockEntity` 调 `serverTick()`；`onRemove` 里对 `ContentDropping` 实例调 `dropContents()`。

### 2.2 方块实体 / 菜单 / 配方序列化器

- `registry/ModBlockEntities`（10 个）：`pressure_tank`、`compressor`、`pressure_cooker`、`high_pressure_furnace`、`radiator`、`fungus_bed`、`condenser`、`cloud_lift`、`pneumatic_motor`、`acid_pump`；后五种（加上酸泵）的 BE 是所属 Block 的内部类 `Entity`。`pneumatic_motor` 是**条件注册**：先试 `CreateReflect.motorType()`（有 Create 则返回 `BlockEntityType<KineticPneumaticMotorEntity>`），失败才回落 `PneumaticMotorBlock.Entity`。
- 菜单：`registry/ModMenus` 注册唯一 `machine`（`IMenuTypeExtension.create((id,inv,data) -> MachineMenu.client(id,inv))`）；服务端由高压锅/高压熔炉实现 `MenuProvider#createMenu` 构造 `MachineMenu`（4 输入 + 1 输出 + 玩家 3×9 与快捷栏，`ContainerData` 三项 `DATA_PROGRESS/DATA_TOTAL/DATA_BAR`，气压按 `bar×10` 传整数）；`menu/MachineContainer` 把 `ItemStackHandler` 包成 `Container`。
- 配方：`machine/ModRecipes` 用 `Registry.register(BuiltInRegistries.RECIPE_TYPE, phosphoros:high_pressure, ...)` 直接注册类型 + `DeferredRegister` 注册序列化器；`machine/HighPressureRecipe` 是 record（`ingredients`/`result`/`bar` 默认 8/`temperature` 默认 1200/`time` 默认 400），`matches` 做「无序多材料、每件原料只能用一次」的贪心匹配，`isSpecial()=true`（不进配方书）。

### 2.3 物品（`registry/ModItems`，另加 `ModFluids` 的硫酸桶，合计 83 个）

- 材料 19：`tungsten_ingot`、`tungsten_plate`、`acid_proof_alloy_plate`、`pre_civilization_alloy`(UNCOMMON)、`sulfur_crystal`、`cloud_sail_membrane`、`aerostat_core`(UNCOMMON)、`whale_bone`、`thunder_feather`、`acid_sac`、`core_shard`(UNCOMMON)、`phosphoros_core`(EPIC)、`aphrodite_tear`(RARE)、`fungus_fiber`、`phosphor_dust`、`frost_scale`、`static_sac`、`lava_gland`、`cloud_map_fragment`。
- 武器 5 + 校准器 1：`acidic_spear`（`SwordItem.createAttributes(Tiers.NETHERITE, 6, -2.6F)`，攻速 +1.4，耐久 650）、`pressure_hammer`（`(Tiers.NETHERITE, 10, -3.25F)`，攻速 +0.75，耐久 800）、`thunder_crossbow`（耐久 500，初始 `charge=0`）、`cloud_hook`（`(Tiers.WOOD, 1, -3.0F)`，耐久 400）、`sulfur_flamethrower`（耐久 600，无攻击属性）、`ballistic_calibrator`（`stacksTo(1)`）。类注释写的面板值（9/13/9/2 伤害）需叠加层级加成才是实际值 —— **未确认**。
- 护甲 16（4 套 × 4 件，均 `Rarity.UNCOMMON`）：`tungsten_*`(385/560/525/455)、`cloud_walker_*`(275/400/375/325)、`tube_heavy_*`(550/800/750/650)、`phosphoros_*`(800/1100/1050/900)。
- 食物 8：`roasted_giant_fungus`(8/0.8+抗性 I 300 s)、`cloud_algae_jelly`(5/0.6+缓降 30 s)、`spore_syrup`(3/0.25+跳跃 I 90 s)、`acid_vegetable`(4/0.6+抗性 II 300 s)、`frostberry_smoothie`(3/0.333+抗火 300 s)、`thunder_honey_drink`(2/0.25+速度 II 90 s)、`pressure_stew`(12/0.8+力量 I 与抗性 I 180 s)、`phosphoros_feast`(14/0.8+力量/抗性/速度/再生/吸收 各 300 s)，效果概率均 1.0。
- 工具 4：`pressure_drill`(2400 耐久，`PressureDrillTier` 速度 10、攻伤 4、修复=`tungsten_plate`)、`fungus_shears`(250)、`cloud_chart`(200)、`barometer`(`stacksTo(1)`)。
- 召唤物 3（`BossSummonItem`，`stacksTo(1)`）：`whale_bone_horn`(RARE)、`pressure_valve`(RARE)、`sulfur_heart`(EPIC)。
- 模块 5（`ModuleItem`，`stacksTo(1)`+tooltip）：`acid_coating`、`pressure_compensator`、`heat_sink`、`air_bag`、`filter`；种子 3（`ItemNameBlockItem`）：`giant_fungus_spore`、`acid_vegetable_seeds`、`cloud_algae_spore`；刷怪蛋 5：`nephele`/`hephaestus`/`aphrodite`/`cloud_ray`/`acid_jelly`；方块物品 13 + `sulfuric_acid_bucket`（`craftRemainder=BUCKET`）。

### 2.4 实体与 Boss（`registry/ModEntities`，11 个）

| 实体 | 类（父类） | 碰撞箱 / 分类 | HP / 伤害 / 护甲 / 速度 |
|---|---|---|---|
| `aphrodite` | `AphroditeEntity`（`AbstractBossEntity`→`Monster`） | 4×6（P3 8×12）/ MONSTER / fireImmune | 1000 / 12 / 24 / 0.25，跟距 48 |
| `hephaestus` | `HephaestusEntity` | 6×9 / MONSTER / fireImmune | 800 / 16 / 30 / 0.18，跟距 48 |
| `nephele` | `NepheleEntity` | 7×4 / MONSTER / fireImmune、`isNoGravity` | 520 / 8 / 20（基类）/ 0.35 + flying 0.35 |
| `cloud_ray` | `CloudRayEntity`（`PathfinderMob`+飞行导航） | 1.6×0.6 / CREATURE | 25 / 5 / 2 / 0.4 |
| `acid_jelly` | `AcidJellyEntity`（`PathfinderMob`+飞行） | 0.8×1.2 / MONSTER | 12 / 4 / 0 / 0.15 |
| `spore_walker` | `SporeWalkerEntity`（`Zombie`） | 0.6×1.95 / MONSTER | 20 / 6 / 2 / 0.28，`isSunSensitive=false` |
| `mushroom_moth` | `MushroomMothEntity`（`Bee`） | 0.7×0.6 / AMBIENT | 8 / 0 / 0 / 0.3，`canAttackType=false` |
| `frost_stalker` | `FrostStalkerEntity`（`Spider`） | 1.4×0.9 / MONSTER | 30 / 9 / 8 / 0.32 |
| `thunder_chick` | `ThunderChickEntity`（`Bat`） | 0.5×0.9 / AMBIENT | 16 / 7 / 0 / 0.35 |
| `lava_worm` | `LavaWormEntity`（`Silverfish`） | 0.4×0.3 / MONSTER | 40 / 12 / 6 / 0.25 |
| `brass_deacon` | `BrassDeaconEntity`（`Vindicator`） | 0.6×1.95 / MONSTER | 60 / 14 / 10 / 0.2 |

### 2.5 效果 / 流体 / 数据组件 / 盔甲材质

- 效果 1：`corrosion`（`CorrosionMobEffect`，HARMFUL，`0x8BE04A`），`Attributes.ARMOR` 修饰符 `-1.0 ADD_VALUE`（id `phosphoros:effect.corrosion.armor`，随 amplifier 线性放大）；`applyEffectTick` 每次 `hurt(magic, 1.0)`，触发间隔 `max(1, 40 >> amplifier)`。
- 流体：`sulfuric_acid` FluidType（density 1400 / viscosity 1800 / temperature 330 / `canDrown` / `supportsBoating=false`）+ `sulfuric_acid` 与 `flowing_sulfuric_acid` 两态 `BaseFlowingFluid`（`slopeFindDistance=2`、`levelDecreasePerBlock=2`、`tickRate=15`）+ LiquidBlock + 桶。
- 数据组件 3（`registry/ModComponents`）：`freshness`、`corrosion`、`charge`，均 `Codec.INT` + `ByteBufCodecs.VAR_INT`；实际只有 `charge` 被代码使用（雷羽弩充能、硫火燃料）。
- 盔甲材质 4（`registry/ModArmorMaterials`）：`tungsten`（4/8/6/3，韧性 2.0，附魔 12，IRON，击退抗性 0，修复 `#phosphoros:tungsten_repair`）、`cloud_walker`（3/6/5/2，1.0，14，LEATHER，修复 `cloud_sail_membrane`）、`tube_heavy`（5/9/7/4，3.0，10，NETHERITE，击退抗性 0.2，修复 `acid_proof_alloy_plate`）、`phosphoros`（5/10/8/4，4.0，18，NETHERITE，击退抗性 0.1，修复 `pre_civilization_alloy`）；`defense()` 另给 `BODY` 填 11；贴图层 `phosphoros:<套装名>`。

### 2.6 创造模式标签页

`registry/ModTabs` 只有一个 `main`（标题 `itemGroup.phosphoros.main`，图标 `phosphoros_core`），`displayItems` 遍历 `ModItems.ITEMS.getEntries()`，即注册顺序就是展示顺序。

## 3. 世界生成

- 维度 `data/phosphoros/dimension/venus_clouds.json`：`type=phosphoros:venus_clouds`，生成器 `minecraft:noise` + `settings=phosphoros:venus_clouds`，`biome_source` 用自定义类型 `phosphoros:venus` 并列出 5 个候选群系。
- `dimension_type/venus_clouds.json`：`min_y=0`、`height=384`、`logical_height=384`、`ambient_light=0.35`、`natural=true`、`ultrawarm=false`、`has_ceiling=false`、`has_skylight=true`、`bed_works=false`、`respawn_anchor_works=false`、`has_raids=false`、`coordinate_scale=1`、`effects=minecraft:overworld`、`monster_spawn_light_level=uniform(0..7)`、`monster_spawn_block_light_limit=0`。
- `noise_settings/venus_clouds.json`：`default_block=phosphoros:cloud_stone`、`default_fluid=minecraft:water`、`sea_level=-64`、`aquifers_enabled=false`、`ore_veins_enabled=false`、`legacy_random_source=true`；`final_density = squeeze(0.64 × blend_density(...))` 复合表达式，`temperature`/`vegetation` 用 `shifted_noise`（`xz_scale=0.25`、`y_scale=0`）；`surface_rule` 仍保留原版 badlands/swamp 分支（判定里引用的 `minecraft:wooded_badlands`、`swamp` 等在本维度不会命中）。
- 自定义群系源：`world/ModWorldgen` 注册 `Registries.BIOME_SOURCE` 的 `venus`，实现见 `world/VenusBiomeSource` —— codec 只吃一个 `biomes`（`RegistryCodecs.homogeneousList`），`getNoiseBiome` 把 `Climate.TargetPoint` 的 temperature 反量化后线性映射到列表下标 `clamp(((T+1)*0.5)*size, 0, size-1)`。选 Java 源的理由写在类注释：数据包的 `multi_noise_biome_source_parameter_list` 只接受 preset 引用，装不下自定义群系参数。
- 群系 5 个：`cloud_archipelago`（temperature 1 / downfall 0）、`acid_flats`（1 / 0）、`metal_frost_highlands`（1 / 0）、`fungal_forest`（1.2 / 0.3）、`lava_tube_galleries`（1.9 / 0.5），`has_precipitation` 全为 `false`。前 4 个群系的 features 第 10 组引用 7 个结构特征（`cloud_archipelago` 不含 `lava_tube_city`，其余 3 个含），`lava_tube_galleries` features 为空。刷怪：`cloud_archipelago`/`acid_flats` 有 `acid_jelly`(monster weight 20, 1–2) 与 `cloud_ray`(creature weight 14, 1–3)；`fungal_forest` 有 `spore_walker`、`mushroom_moth`；`metal_frost_highlands` 有 `frost_stalker`；`lava_tube_galleries` 只有 `lava_worm`。
- 自定义 feature 8 个（`world/ModFeatures`，全部 `NoneFeatureConfiguration`）：`cloud_island`、`cloud_harbor`、`acid_pump_station`、`observation_tower`、`fungal_nursery`、`crashed_airship`、`phosphoros_altar`、`lava_tube_city`。
- placed_feature 均为 `rarity_filter`/`count` + `in_square` + `height_range` + `biome`：

| 特征 | 密度 | y 范围 | 实现要点 |
|---|---|---|---|
| `cloud_island` | 每区块 4 座 | 180–300 | `CloudIslandFeature`：半径 6–18、顶面厚 2–4 的平顶圆盘 + 倒锥底（`-plateau*1.8*pow(1-d,0.6)`），只填空气 |
| `cloud_harbor` | 1/120 | 200–280 | R=10 的 21×21 平台（四角切角）+ 6 高围墙（每 5 格开窗）+ 四角 2×2 晶塔（顶硫晶）+ 12 高中央塔 + 塔顶硫晶；2 箱（普通箱 + 保底校准器箱）；75% 生成 1 只 `brass_deacon` |
| `acid_pump_station` | 1/90 | 190–270 | R=6 平台 + 4 高围墙 + 中央 2 半径塔；3 个压力罐方块充当「泵体」；3 个 `chests/acid_pump_station` |
| `observation_tower` | 1/140 | 200–240 | 5×5 塔身 30 格 + 顶层 9×9 平台；塔底 4 箱（3 普通 + 1 `observation_tower_calibrator`） |
| `fungal_nursery` | 1/100 | 200–270 | R=5 平台 + 8 个 2×2 苔藓菌床 + 巨型蘑菇；2 箱 |
| `crashed_airship` | 1/110 | 210–290 | 15 长胶囊船体 + 每 3 格钨块肋骨 + 断裂下沉尾部；3 箱（含 `crashed_airship_core`） |
| `phosphoros_altar` | 1/400 | 270–300 | R=4 三级阶梯 + 四角 4 高硫晶柱 + 中央核心；1 箱 `chests/phosphoros_altar` |
| `lava_tube_city` | 1/500 | 190–240 | 三层圆台（R20/y0、R14/y6、R8/y12 ≈ 41×30×41）+ 四角贯通立柱 + 8 根硫晶柱；12 箱（每层 4，第三层用 `chests/lava_tube_city_boss`） |

- `world/RuinBuilder` 提供 `platform`/`wallRing`/`tower`/`clear`/`chest` 五个原语；`world/RuinPalette` 定 Create 风格配色（磨制安山岩、深板岩、切制铜、铜格栅、铁栏杆、锁链、灯笼、海晶灯）+ 本模组云岩/钨块/硫晶块，并提供 `detail()`（角落锁链灯笼 + 玻璃带 + 铜格栅 + 地面铜线）与 `truss()`。

## 4. 核心机制

### 4.1 气压 / 气动模型与 WorkConsumer

- 换算：`machine/PneumaticStorage.MILLIBUCKET_PER_BAR = 1000`，`getBar() = air/1000`；提供 `receive/extract(simulate)`、`hasBar`、`setAir`、`clampToCapacity`。
- 传输不引入新 capability：`blockentity/AbstractMachineBlockEntity` 的 `pushAirToNeighbors(maxPerNeighbor)` / `pullAirFromNeighbors(maxPerNeighbor)` 遍历六向且只认 `AbstractMachineBlockEntity` 邻居，方向由 `acceptsAir()`/`suppliesAir()` 控制（压缩机 `acceptsAir=false`；高压锅、高压熔炉、冷凝塔、云梯、马达、酸泵 `suppliesAir=false`；菌床、散热器、酸泵两者皆 false）。
- 各机器数值：

| 机器 | buffer | 产出 / 消耗 | 条件与其它 |
|---|---|---|---|
| `pressure_tank` | 8000 | 每 tick 每向推 500 mB | 空手右击显示本罐 + 向上连通整列的储量 |
| `compressor` | 4000 | 曲柄一次 `compressorCrankAir`(2000)，冷却 40 tick；每 tick 推 `compressorOutputPerTick`(2000) | 右击摇曲柄，手持物品亦可 |
| `pressure_cooker` | 8000 | 20 mB/t、200 tick 处理 4 份 | 需 `pressureCookerBar`(8) 开工；气压掉到 <1 bar 暂停且不丢进度；欠压时 400 mB/t 抽邻居 |
| `high_pressure_furnace` | 8000 | 20 mB/t，时长取配方（默认 400） | 需配方 `bar`(8) + `temperature`(1200，读 `heatAt + baseDimensionTemperature`)；欠压时 800 mB/t 抽邻居 |
| `pneumatic_motor` | 8000 | 20 mB/t → 32 RPM | `WorkConsumer.findNeighbor` 命中后 `acceptWork(32)`；气压 <0.1 bar 先抽 400 |
| `condenser` | 4000 | 50 mB/t，1200 tick 产 1 冰 | 需 ≥2 bar 且热源+基础温度 ≥300 |
| `cloud_lift` | 8000 | 每次 100 mB | 传送 32 格（潜行向下），60 tick 冷却，传送后 `fallDistance=0` |
| `acid_pump` | air 容量 0 | 浸酸时内部酸缓冲 +200 mB/t，上限 8000 | 扫描 ±3 X/Z、±1 Y 找 `SULFURIC_ACID`/`FLOWING_SULFURIC_ACID`；1000 mB 换硫酸桶 |
| `radiator` | air 容量 0 | 冷却剂上限 6400 tick | 雪球 100 / 冰 400 / 浮冰 800 / 蓝冰 1600 tick；`isWorking()` 恒 false，自身不累积过热 |
| `fungus_bed` | air 容量 0 | 9600 tick（8 分钟）产 `fungus_fiber` 2–4 | 槽内有货即停长；槽位暴露成 `IItemHandler` |

- 机械功接口 `machine/WorkConsumer`：`canAcceptWork()`、`acceptWork(int rpm)`（返回速度倍率）、`static findNeighbor(Level, BlockPos)`（扫六向取第一个可接受者）。实现者只有高压锅与高压熔炉：`acceptWork` 写 `workTicks = 20` 并返回 `rpm >= 16 ? 2.0F : 1.0F`，实际加速在 `tickMachine()` 里「`workTicks > 0` 则本 tick 额外 `progress++` 并 `workTicks--`」，等价 ×2 速度（两个类里另有私有 `applyWorkBoost()` 未被调用）。
- Create 侧：*反射命中*后 `phosphoros:pneumatic_motor` 的 BE 换成 `KineticPneumaticMotorEntity extends KineticBlockEntity`，`calculateStressApplied()` 返回 4.0（4 SU/RPM），在 `tick()` 与 `onSpeedChanged()` 把 `abs(getSpeed())` 当 rpm 推给相邻 `WorkConsumer`。`CreateCompat.isActive()` 恒 false —— 应力注册入口（`BlockStressValues` 一类）尚未实现，只有探测与日志。

### 4.2 过热机制（`AbstractMachineBlockEntity#tickOverheat`）

- 触发：`PhosphorosConfig.ENABLE_OVERHEAT`（`[balance] enableOverheat`，默认 true）、`isWorking()` 为真、且 `ambientTemperature > OVERHEAT_TEMPERATURE`（400 °C）三者同时成立。
- 累积：每工作 tick `overheatTimer++`，达 `OVERHEAT_TICKS`（1200 = 60 s）→ `overheated=true` 并调 `onOverheat()`（2 个 `EXPLOSION` 粒子 + `ANVIL_LAND` 0.8/0.5）；条件不满足时按 **2/tick** 回落。
- 停机：`serverTick()` 在 `overheated` 时直接 return（不再跑 `tickMachine()`）；每 40 tick 发 6 个 `LARGE_SMOKE` + `BELL_BLOCK`(0.6/0.5)。
- 恢复：温度回落期间 `cooldownTimer` 每 tick +1，满 `OVERHEAT_COOLDOWN_TICKS`（200 = 10 s）复位；温度仍高则 `cooldownTimer` 归零。
- 环境温度 `machine/MachineEnvironment.temperatureAt = 基础温度 + 热源 − 冷源`：基础 = `venus_clouds` 30 °C、`northstar:venus` 430 °C、其它 20 °C；热源扫 5×4×5（`dx,dz ∈ [-2,2]`、`dy ∈ [-2,1]`）——熔岩 600、岩浆块 300、火/灵魂火 300、`#minecraft:campfires` 200，最多 8 个；冷源扫 9×5×9（±4 × ±2 × ±4）——水/冰/蓝冰/浮冰/雪块每块 −40、外加「有冷却剂的散热器」每台 −40，同样上限 8。
- Northstar 联动：`compat/MachineEnvBridge.isCooledByRegulator` → `NorthstarReflect.hasRegulatorNear` → `NorthstarMachineBridge` 在 ±16 X/Z、±6 Y 内找 `TemperatureRegulatorBlock`；命中时 `overheatTimer=0`、`overheated=false` 并 return，等于完全免过热。

### 4.3 伤害管线与反枪械宪章 R1–R5（`combat/BossDamagePipeline#process`，8 步固定顺序）

入口唯一：`event/CombatEvents.onIncomingDamage` 在 `LivingIncomingDamageEvent` 里判 `getEntity() instanceof AbstractBossEntity`（普通生物直接放行），返回 0 时 `setAmount(0)`+`setCanceled(true)`。

1. **相位/无敌**：`boss.isDamageImmune()`（`transitionTicks>0` 或死亡中；阿佛洛狄忒结茧也 true）→ 先 `onImmuneHit(source, amount)` 再返回 0。
2. **分类**：`DamageClassifier.classify` 顺序 = 标签 `#phosphoros:ballistic` → `#minecraft:is_explosion` → `directEntity instanceof LivingEntity` → 标签 `#phosphoros:acid` → `isEnvironment`（`IS_FALL`/`IS_DROWNING`/`IS_FREEZING`/`IS_LIGHTNING`，或直接实体与造成实体均为 null）→ 否则 OTHER；`ENVIRONMENT` 一律返回 0（同一小节里的「环境伤害完全免疫」）。
3. **乘数（R2）+ R1 板/本体分离**：板乘数取 `DamageKind.plateMultiplier()`（弹道 0.05 / 近战 1.0 / 爆炸 1.2 / 酸 0.8 / 其它 0.5）；手持 `acidic_spear` 的近战再 ×1.2；携带 `ballistic_calibrator` 的弹道再 ×(1+0.15)；Boss 过载时再 ×`overloadPlateBonus`(1.5)。板在时 `plateDamage = amount×板乘数`、`coreDamage = amount×0.10×本体乘数`；板破后 `plateDamage = 0`、`coreDamage = amount×本体乘数×窗口系数`，窗口开 1.0、关 0.05（R5）。
4. **R3 单次上限**：板伤、本体伤各自 `min(cap)`；cap = 弹道 9 / 近战 40 / 爆炸 30 / 酸 20 / 其它 20。
5. **R4 每人滑动窗口**：有玩家来源时 `boss.consumeDpsBudget(uuid, kind, plate+core)`，按返回允许量等比缩放两段；无玩家来源且该类上限 ≤0 时整次归零。窗口是 `AbstractBossEntity.DpsWindow` 环形缓冲（容量 128、零分配），长度 `dpsWindowTicks`=20，上限 弹道 13 / 近战 45 / 其它 30（爆炸与酸共用 Other），非近战类随阶段 ×(1+0.15×(phase−1))。
6. **结算（先扣护甲板）**：`boss.damagePlates(plateDamage)` 按数组顺序逐块扣，溢出部分 ×0.30 追加进本体；最后一块板破碎瞬间 `openWindow(60)`。
7. **弱点/过载累积**：命中位置优先 `source.getDirectEntity().position()`，否则攻击者眼睛位置；`boss.tryHitWeakPoint(...)` 命中即累加过载值。
8. **固定减伤**：`coreDamage ×= 1 − bossFixedDamageReduction`（0.2），作为最终本体伤害返回给事件（跳过原版护甲）。

反逃课（`combat/AntiCheeseHandler`，由 `AbstractBossEntity#isInvulnerableTo` 调用）拦 `IS_FALL`、`IS_DROWNING`、`IS_FREEZING`、`fell_out_of_world`、`IS_LIGHTNING`，**不**拦 `BYPASSES_INVULNERABILITY`（`/kill` 仍有效）；Boss 另 `isPushable=false`、`knockback` 空实现、`canBeLeashed=false`、`startRiding` 返回 false。

### 4.4 弱点 / 护甲板 / 过载 / 阶段

- `combat/WeakPoint` 是 record（`id`、相对 `offset`、`radius`、`exposeIn` 位掩码 `PHASE_1/2/3` 与 `ALWAYS`）；暴露判定 `isWeakPointExposed(i)`：过载中或窗口期**无条件暴露**，否则看 `exposedIn(phase)`。
- 命中判定 `tryHitWeakPoint`：球心 `position()+offset`，半径 `max(weakPoint.radius, weakPointRadius=1.2)`；同一点位有 `weakPointCooldownTicks=5` 冷却（冷却内返回 false）；过载值增益 弹道 1.0 / 近战 0.5 / 其它 0.7，弹道 + 携带校准器再 +1.0。
- 过载 `addOverloadPoints`：`overloadTicks>0` 或距上次触发不足 `overloadInternalCooldownTicks`(1200 = 60 s) 时忽略；`overloadProgress >= overloadPoints`(8) → 清零、置 `overloadDurationTicks`(120)、`openWindow(overloadWindowTicks=80)`；机制入口 `addOverloadFromMechanic(2.0)` 只被压力锤震荡波调用。
- 护甲板：构造器收 `(plateCount, plateHpEach)`，`float[] plateHp` 存档为 `PhosphorosPlates`；涅斐勒 4×120、赫菲斯托斯 6×150、阿佛洛狄忒 4×200；`DATA_PLATE_HP`/`DATA_PHASE`/`DATA_WINDOW`/`DATA_OVERLOAD` 同步给客户端。
- 阶段：`getPhase()` 按血量比与 `phaseThresholds()`（默认 `{0.66, 0.33}`，涅斐勒覆盖为 `{0.60, 0.25}`）分三档；`onPhaseChanged` 一律 `transitionTicks=30`（1.5 s 无敌演出）+ `openWindow(to==3 ? 200 : 100)`。
- 多人缩放：每 100 tick 一次 `applyMultiplayerScaling()`，以首次的 `MAX_HEALTH` 为基准令 `target = base × (1 + bossHpPerExtraPlayer × (人数−1))`（默认 0.5），按当前血量比例重设，差值 <0.5 跳过；人数 = 64 格内非旁观存活玩家。血条 `ServerBossEvent`（`setDarkenScreen(true)`）每 20 tick 更新并按 64 格增删玩家。

### 4.5 Boss 召唤链（`item/BossSummonItem`）

- 三件召唤物与目标：`whale_bone_horn`→`NEPHELE`、`pressure_valve`→`HEPHAESTUS`、`sulfur_heart`→`APHRODITE`；均 `stacksTo(1)`，只在 `ServerLevel` + 有玩家时生效（`useOn`）。
- 拒绝：以落点（点击面相邻格）为球的 `AABB.inflate(64)` 内已有任何 `AbstractBossEntity` → 发 `message.phosphoros.summon_blocked` 并返回 `InteractionResult.FAIL`。
- 成功：`create` → `moveTo(x+0.5, y, z+0.5, 玩家 yRot+180, 0)` → `addFreshEntity`，播 `WITHER_SPAWN`(1.2/0.6)，发 `summon_success`；非创造模式 `shrink(1)`。对应配方 `whale_bone_horn`/`pressure_valve`/`sulfur_heart.json`。

### 4.6 护甲套装加成（件数判定统一在 `combat/ArmorSetBonus.countPieces`，比对 `ArmorItem#getMaterial().value()`）

| 套装 | 2 件 | 4 件 | 实现位置 |
|---|---|---|---|
| 钨钢压力服 | 酸蚀 ×0.7 | 再 ×0.8（合计 0.56） | `CombatEvents.onPlayerSetBonusDamage` |
| 云行者 | 雷击 ×0.5 | 下落中按潜行 → `delta.y=0.55` 二段跳，60 tick(3 s) 冷却 + `CLOUD` 粒子 | `PlayerEvents.tickArmorPassives` |
| 熔管重甲 | 酸蚀 ×0.5 且 `#minecraft:is_fire` ×0.5 | 身处 `minecraft:lava` 时给 3 s 抗火，160 tick(8 s) 冷却 | 同上 |
| 启明星战甲 | `canSeeSky` 时每 100 tick +1 吸收（上限 20） | 每 600 tick 腐蚀 −20，并持续移除 `blindness` | 同上 |

- 压力等级 `ArmorSetBonus.pressureRating`：钨钢 ≥2 件 = 100 atm、熔管 ≥2 件 = 120、启明星 ≥2 件 = 130，携带 `pressure_compensator` 再 +20；在 `northstar:venus` 维度若 `<100` 则每 400 tick `hurt(generic, 1.0)`。
- 模块的其余效果：`acid_coating` 每 tick 腐蚀 −1；`air_bag` 空中下坠(`vy < -0.3`)给 40 tick 缓降；`filter` 持续移除 `blindness`。

### 4.7 腐蚀与酸雨 / 掉出岛屿

- 玩家腐蚀值存 `player.getPersistentData()` 的 `phosphoros:corrosion`（`PlayerEvents.CORROSION_KEY`），范围 0–100；每 20 tick 结算一次：`enableAcidRain` 为真且 `isAcidExposed` 为真 → 腐蚀 `+2` 并 `hurt(acid, 1.0)`（`ModDamageSources.acid`，无直接实体所以被分类为 ACID）；否则腐蚀 `−1`。随后按 `amplifier = min(4, corrosion/20)` 给 60 tick 的 `corrosion` 效果。
- `isAcidExposed` = `level.isRainingAt(player.blockPosition())` 且维度是 `phosphoros:venus_clouds` 或 `northstar:venus`。（云海 5 群系 `has_precipitation=false`，按原版语义该维度不满足降雨条件；实际表现 **未确认**。）
- 云海掉出岛屿：`tickCloudVoid` 在 `venus_clouds` 且 `y < cloudVoidY`(60) 时 `teleportTo(x, 200, z)` 并 `hurt(generic, 最大生命 × cloudVoidDamageRatio)`(0.2)。

### 4.8 三个 Boss 的招式状态机（数值取自各实体类常量，统一「前摇 → 到点执行」，`transitionTicks>0` 时停导航并跳过 AI）

| Boss（板/HP/阈值） | 招式：伤害（前摇/冷却 tick，范围与附加） |
|---|---|
| 涅斐勒 4×120 / 520 / 60%–25% | 云爆弹 8（20/160，半径 4）；尾扫 12（0/200，需距离 <36，击退 1.4+上抛 0.4）；召唤云帆鳐 3 只（0/400）；俯冲 16（30/600，P2+，沿朝向 20 格扫 AABB，并把自身移到终点） |
| 赫菲斯托斯 6×150 / 800 / 66%–33% | 熔岩拳 16（18/120，需距离 ≤6）；熔渣投掷 8+点燃 4 s（24/200，半径 3，锁定施法时目标位置）；蒸汽喷发 10+上抛 0.8（40/100，4 个喷口 ±3、气柱高 8，**喷口上方非空气即封堵，≥1 个封堵就 `openWindow(80)`**）；地震踏 14（28/360，半径 8，`!onGround()` 免疫）；过载爆发 20（60/800，P3，半径 12）；P3 热浪每 20 tick 对 16 格内无抗火玩家 1 点 `onFire`+点燃 2 s |
| 阿佛洛狄忒 4×200 / 1000 / 66%–33% | 酸浪 12+腐蚀(amplifier 2, 400 tick)（16/140，需 `dot>0.3` 且距离 ≤8）；酸针 3×6（10/100）；召唤酸潮水母 2 只（0/300，仅 P1）；硫火环 8+点燃 3 s（30/240，P3，环带 2–10 格）；高压冲击波 20+上抛 1.2（50/500，P3，半径 15）；结茧（P2）护壁 240、近战全额其它 50%、最迟 400 tick 自动破、破茧 `openWindow(160)`、茧期酸位 0.5 格/秒且低于酸面者每 20 tick 吃 2 点 magic+腐蚀；P3 进入时 `refreshDimensions()` 到 8×12 并 `openWindow(120)`，4800 tick(4 min) 超时对 32 格内玩家 999 点 `magic` |

## 5. 事件、网络与指令

- 游戏总线：`CombatEvents`（Boss 管线入口 + 玩家套装减伤）、`PlayerEvents`（`PlayerTickEvent.Post`：护甲被动每 tick、腐蚀与掉出岛屿每 20 tick）、`PhosphorosCommands`、`TaczCompat::onServerStarted`。
- mod 总线：`ModEntities::registerAttributes`（11 个实体）、`ModCapabilities::register` —— 只给 `pressure_cooker` 与 `high_pressure_furnace` 注册 `Capabilities.ItemHandler.BLOCK`（气动刻意不占 capability，避免与 Create 冲突）。
- 网络（`network/ModNetwork`，`@EventBusSubscriber(bus=MOD)`，`registrar("1")`）：唯一包 `phosphoros:barometer`（`BarometerPayload(float bar, float temperature, int corrosion, int daysToMonsoon)`，手写 `StreamCodec.composite`），`playToClient`；处理器只 `context.enqueueWork(() -> BarometerCache.update(payload))`，`BarometerCache` 为纯数据（5 秒过期），服务端不会类加载到客户端类型。发送点只有 `BarometerItem#use`（扫描 ±16 X/Z、±8 Y 取最高 `getBar()`，温度优先取 Northstar 值，季风倒计时 `interval - floorMod(day, interval)`）。
- 指令（`hasPermission(2)`）：`/phosphoros debug boss|dps|classify`，对象是 64 格内最近的 `AbstractBossEntity`；`boss` 打印 `debugLines()`（phase/hp/plates/window/overload/transition/last trace，赫菲斯托斯与阿佛洛狄忒另有 windup、cocoon/acidLevel/enrage 字段），`dps` 打印弹道与近战本窗口已用/上限，`classify` 打印最近一次 `DamageTrace`（kind/raw/plate/core/note）。

## 6. 客户端

- `client/ClientSetup`（mod bus + `Dist.CLIENT`）：注册 5 个 `LayerDefinition`（Nephele/Hephaestus/Aphrodite/CloudRay/AcidJelly，自建 `EntityModel`）与 11 个渲染器 —— 3 Boss + `cloud_ray` + `acid_jelly` 用自建渲染器，`spore_walker`/`mushroom_moth`/`frost_stalker`/`thunder_chick`/`lava_worm`/`brass_deacon` 复用原版 `Zombie/Bee/Spider/Bat/Silverfish/Vindicator` 渲染器；另注册 `ModMenus.MACHINE` → `MachineScreen`。
- `client/hud/PhosphorosHud`：`RegisterGuiLayersEvent#registerAboveAll(phosphoros:hud, ...)`。右上角气压计条（宽 `min(120, bar*15)` px，文本 `%.1f bar / %.0fC / corr / monsoon`，随 5 秒缓存过期消失）；左上角常驻 `hud.phosphoros.weak_points`（暴露数/总数，有暴露时金色 `0xFFFFD37A`）。
- `client/screen/MachineScreen`：176×166，贴图 `textures/gui/machine.png`，进度箭头取贴图 `(176,0)`、宽 24、绘制于 `(79,34)`；另有气压条 `fill(8, 62, 8+width, 66)`，`width = min(80, bar*4)` px（80 px 即 20 bar）。
- `client/ModClientFluids`：`RegisterClientExtensionsEvent` 给硫酸注册 `block/sulfuric_acid_still`、`block/sulfuric_acid_flow` 与 tint `0xFFB7E04A`。

## 7. 数据包与资源布局

- `data/phosphoros/` 实有文件：`advancement` 14、`recipe` 57、`loot_table/blocks` 17、`loot_table/chests` 11、`damage_type` 1、`dimension` 1、`dimension_type` 1、`noise_settings` 1、`worldgen/biome` 5、`configured_feature` 8、`placed_feature` 8、`tags` 9。
- 伤害类型 `phosphoros:acid`（`message_id=acid`、`scaling=when_caused_by_living_non_player`、`exhaustion=0.1`）；标签 `#phosphoros:acid = [phosphoros:acid]`、`#phosphoros:ballistic = [tacz:bullet, tacz:bullet_ignore_armor, tacz:bullet_void, tacz:bullet_void_ignore_armor, #minecraft:is_projectile]`、`#phosphoros:boss_mechanic` **为空**。
- 其它标签：`#phosphoros:boss`(3 Boss)、`#phosphoros:cloudsea_fauna`(cloud_ray、acid_jelly)、`#phosphoros:pressure_suit`(4 件钨钢)、`#phosphoros:boss_drops`(7 件)、`#phosphoros:tungsten_repair`(tungsten_plate、acid_proof_alloy_plate)、`#phosphoros:needs_tungsten_tool`(钨块、硫晶块)、`#phosphoros:fungus` **为空**、`#minecraft:mineable/pickaxe`(16 个方块)。
- 箱子战利品表 11 个：`cloud_harbor`、`cloud_harbor_calibrator`、`acid_pump_station`、`observation_tower`、`observation_tower_calibrator`、`fungal_nursery`、`crashed_airship`、`crashed_airship_core`、`lava_tube_city`、`lava_tube_city_boss`、`phosphoros_altar`。
- 高压熔炼配方只有 `acid_proof_alloy_plate.json`（2 `tungsten_plate` + 1 `sulfur_crystal` + 1 金锭；`bar=8`、`temperature=1200`、`time=200`、产出 1）。
- `assets/phosphoros/`：`blockstates` 17、`models/block` 17、`models/item` 93、`textures/item` 65、`textures/block` 25、`textures/entity` 5、`textures/gui` 1、`textures/models/armor` 8；双语 `lang/en_us.json` 与 `zh_cn.json` 各约 198 行（item/block/entity/effect/tooltip/message/hud/advancement/death 各族）。
- 工具脚本：`tools/generate-resources.ps1`（生成 `data/phosphoros/**` 纯数据）、`tools/audit-resources.ps1`（注册项 ↔ 模型/贴图/语言审计）。

## 8. 配置项（`config/phosphoros-common.toml`，定义在 `config/PhosphorosConfig`，全部可热调）

| 段 | 键 = 默认值 |
|---|---|
| `[combat]` 乘数 | `ballisticPlateMultiplier`=0.05、`ballisticCoreMultiplier`=0.10、`meleePlateMultiplier`=1.0、`meleeCoreMultiplier`=1.0、`explosivePlateMultiplier`=1.2、`explosiveCoreMultiplier`=1.2、`acidPlateMultiplier`=0.8、`acidCoreMultiplier`=0.8、`otherPlateMultiplier`=0.5、`otherCoreMultiplier`=0.6 |
| `[combat]` 上限 | `hitCapBallistic`=9、`hitCapMelee`=40、`hitCapExplosive`=30、`hitCapAcid`=20；`perPlayerDpsCapBallistic`=13、`perPlayerDpsCapMelee`=45、`perPlayerDpsCapOther`=30、`dpsWindowTicks`=20 |
| `[combat]` Boss | `bossHpPerExtraPlayer`=0.5、`bossFixedDamageReduction`=0.2；`weakPointRadius`=1.2、`weakPointCooldownTicks`=5；`overloadPoints`=8、`overloadDurationTicks`=120、`overloadInternalCooldownTicks`=1200、`overloadWindowTicks`=80、`overloadPlateBonus`=1.5 |
| `[world]` | `enableAcidRain`=true、`cloudVoidY`=60、`cloudVoidDamageRatio`=0.2、`monsoonIntervalDays`=5 |
| `[machines]` | `compressorCrankAir`=2000、`compressorOutputPerTick`=2000、`pressureCookerBar`=8.0、`overheatTemperature`=400.0、`overheatTicks`=1200、`overheatCooldownTicks`=200 |
| `[compat]` / `[balance]` | `taczIntegration`=true / `enableOverheat`=true |

## 9. 已知限制与未落地部分（均可从源码或数据核对）

- **Create 应力注册未完成**：`CreateCompat.isActive()` 恒 false，`registerStressConsumer()` 只打日志；马达靠自建 `KineticBlockEntity` 子类 + `calculateStressApplied()` 挂在动力网上，未往 `BlockStressValues` 一类注册表登记；全模组只有高压锅与高压熔炉消费 `WorkConsumer`，其余机器不吃机械功。
- **两个标签文件为空**：`#phosphoros:fungus`（导致菌剪「采菌不扣耐久」的 `mineBlock` 分支默认不命中，`useOn` 移植路径另有原版蘑菇方块兜底）与 `#phosphoros:boss_mechanic`（代码中也无引用）。
- **已注册但无效果/无引用的对象**：模块 `heat_sink`（tooltip 写火焰伤害 −25%，`PlayerEvents` 无对应分支）；数据组件 `freshness`、`corrosion`；`DamageKind#wantsWindow`/`isFirearmLike`；高压锅与高压熔炉里私有的 `applyWorkBoost()`；`BrassDeaconEntity#equipPressureHammer()` 无调用方（该生物出生时不持压力锤）。
- **刷怪蛋缺 6 个**：`spore_walker`、`mushroom_moth`、`frost_stalker`、`thunder_chick`、`lava_worm`、`brass_deacon` 只有语言键，没有物品注册。
- **季风只有预报**：`monsoonIntervalDays` 仅用于气压计倒计时文本，没有对应的降雨/酸雨编排；`isAcidExposed` 又依赖 `isRainingAt`，而云海 5 群系 `has_precipitation=false`（实机表现 **未确认**）。
- **结构以特征替代 jigsaw**：8 个 feature 都是 `NoneFeatureConfiguration` + `RuinBuilder`/`RuinPalette` 手写摆放，没有 NBT 模板与 structure_set，尺寸普遍小于 README 里草案规格（云港 21×21 对 48×40×48、熔管城市 41×30×41 对 120×80×120）。
- **群系源是温度带映射**：`VenusBiomeSource` 只按 `Climate.TargetPoint.temperature` 线性取下标，`humidity`/`continentalness` 等不参与，因此分布是温度分带而非真正的 multi_noise 混合。
- **Boss 招式伤害不过管线**：三个 Boss 的招式都用 `damageSources().mobAttack(this)`/`onFire()`/`magic()`，不写 `#phosphoros:boss_mechanic`；管线只作用于 Boss **受到**的伤害，玩家侧招式伤害也不进 DPS 窗口。
- **机器交互多为 actionbar 文本**：只有高压锅与高压熔炉有 GUI；`MachineContainer#setChanged` 用 `setStackInSlot(0, getStackInSlot(0))` 触发，`MachineContainer#stillValid` 与 `MachineMenu#stillValid` 恒 true（无距离校验）。
- **`lava_tube_galleries` 群系没有结构**：`lava_tube_city` 挂在另外 3 个群系里，与该群系语义不一致，该群系目前只提供 `lava_worm` 刷怪。
