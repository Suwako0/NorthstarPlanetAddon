# 北极星水星附属（NorthstarMercuryAddon）逻辑梳理

## 1. 定位与依赖

- 模组名 `NorthstarMercuryAddon`，modid `northstar_mercury_addon`，版本 `0.1.0`，作者 Suwako，许可 `All Rights Reserved`（`gradle.properties`）。
- 目标环境：Minecraft `1.21.1` + NeoForge `21.1.248`（`minecraft_version` / `neo_version`），Java 21 工具链。
- 硬依赖（`src/main/resources/META-INF/neoforge.mods.toml`）：
  - `neoforge [21.1,)`、`minecraft [1.21.1]`，均 `mandatory=true`；
  - `northstar [0.6.5,)`，`mandatory=true`、`ordering="AFTER"`、`side="BOTH"`。
- **代码层没有北极星的编译期依赖**：水星方块/矿石一律用 `ResourceLocation` 在注册表里查（`MeteorOres`），群系注入只用 id 与数据包 JSON；`northstar` 命名空间只出现在 5 个 `data/northstar/...` 标签追加文件里。
- Mixin 配置 `northstar_mercury_addon.mixins.json` 有两处登记：`neoforge.mods.toml` 的 `[[mixins]]`（开发运行从类目录加载）与 jar manifest 的 `MixinConfigs`（`build.gradle` 第 66 行，成品包）。
- 主类 `NorthstarMercuryAddon`（`@Mod(MODID)`）在构造器内完成接线：
  - 注册 6 个 `DeferredRegister`：方块、物品、实体、音效、特征、效果，外加创造栏；
  - mod bus 监听 `RegisterPayloadHandlersEvent`、`EntityAttributeCreationEvent`、`RegisterSpawnPlacementsEvent`；
  - `NeoForge.EVENT_BUS` 监听 `MercuryBiomeInjector::onServerAboutToStart`、`SolarStormManager::onServerTick`、`SolarStormCommand::onRegisterCommands`、`SunbathManager::onPlayerTick`；
  - 客户端接线包在 `FMLEnvironment.dist.isClient()` 内调 `MercuryClient.register`，专用服务器不会加载任何客户端类；
  - 启动日志打印 `ModList` 里的本模组版本号，便于确认 `mods/` 里装的是哪一份构建。

## 2. 注册内容清单

### 2.1 方块

| id | 类 | 属性 |
| --- | --- | --- |
| `northstar_mercury_addon:mercury_sand`（水星沙） | `block/MercurySandBlock`（继承 `FallingBlock`，自有 `CODEC = simpleCodec(MercurySandBlock::new)`） | `mapColor(MapColor.SAND)`、`strength(0.5F)`、`sound(SoundType.SAND)` |

- `registry/MNABlocks` 只注册这一个方块；重力行为完全复用原版 `FallingBlock`。
- 标签追加：`data/minecraft/tags/block/sand.json` → `#minecraft:sand`；`data/minecraft/tags/block/mineable/shovel.json` → `#minecraft:mineable/shovel`；`data/northstar/tags/block/mercury_stone_replaceable.json` → `#northstar:mercury_stone_replaceable`（让北极星矿脉可穿过水星沙生成）。

### 2.2 物品

| id | 类 | 关键数值 |
| --- | --- | --- |
| `mercury_sand` | `registerSimpleBlockItem` → `BlockItem` | 同时是创造栏图标 |
| `sunfish_slice`（太阳鱼片） | `Item` + `FoodProperties` | nutrition `2`、saturationModifier `0.1F`（与 `Foods.COD` 同级） |
| `sunfish_jam`（太阳鱼果烩） | `Item` + `FoodProperties` | nutrition `5`、saturationModifier `0.6F`、`alwaysEdible()`、`effect(() -> SunbathManager.foodEffect(), 1.0F)` |
| `sunfish_spawn_egg` | `DeferredSpawnEggItem(SUNFISH, ...)` | 主色 `0xFFC24A`、副色 `0xFFF3C4` |
| `blazing_sunfish_spawn_egg` | `DeferredSpawnEggItem(BLAZING_SUNFISH, ...)` | 主色 `0xB32D0C`、副色 `0xFFE066` |

- 果烩的效果实例走 supplier：每次食用都新建一个 `MobEffectInstance`（其时长是可变对象），同时避免在物品注册阶段就去解析尚未绑定的效果 Holder。

### 2.3 实体

| id | 类 | 分类 / 尺寸 / 追踪 |
| --- | --- | --- |
| `meteor`（陨石） | `entity/MeteorEntity`（`Entity` + `ItemSupplier`） | `MobCategory.MISC`，`sized(1.0F, 1.0F)`，`clientTrackingRange(64)`，`updateInterval(1)` |
| `sunfish`（太阳鱼） | `entity/SunfishEntity`（`PathfinderMob` + `SolarStormImmune` + `Enemy`） | `CREATURE`，`sized(1.3F, 1.2F)`，`eyeHeight(0.6F)`，追踪 12 / 2 |
| `blazing_sunfish`（炽烈太阳鱼） | `entity/BlazingSunfishEntity extends SunfishEntity` | `CREATURE`，`sized(1.55F, 1.4F)`，`eyeHeight(0.7F)`，`fireImmune()`，追踪 12 / 2 |

- 属性（`registerEntityAttributes`）：
  - 太阳鱼 `MAX_HEALTH 12`、`MOVEMENT_SPEED 0.25`、`FLYING_SPEED 0.5`、`FOLLOW_RANGE 48`、`ATTACK_DAMAGE 3`，`xpReward = 3`；
  - 炽烈太阳鱼在同一 builder 链上覆盖 `MAX_HEALTH 36`、`ARMOR 6`、`ATTACK_DAMAGE 8`、`FLYING_SPEED 0.62`，`xpReward = 8`（`MOVEMENT_SPEED 0.25` / `FOLLOW_RANGE 48` 继承）。
- 刷怪放置（`registerSpawnPlacements` + `checkOpenSkySpawnRules`）：两者都用 `SpawnPlacementTypes.NO_RESTRICTIONS` + `Heightmap.Types.MOTION_BLOCKING_NO_LEAVES` + `Operation.REPLACE`；自然刷怪要求 `canSeeSky(pos)` 且 `pos` 与 `pos.above()` 为空气。刷怪蛋/刷怪笼的 `spawnType != NATURAL` 一律放行。
- `MeteorEntity` 无属性；`isPickable()` / `isAttackable()` / `isPushable()` 全为 `false`，`isAlwaysTicking() = true`。

### 2.4 效果 / 伤害类型 / 声音 / 网络负载

- **效果** `northstar_mercury_addon:sunbath`（沐浴阳光），`effect/SunbathEffect`：
  - `MobEffectCategory.BENEFICIAL`，颜色 `0xFFD85A`；
  - 属性修饰符 `Attributes.MOVEMENT_SPEED`，id `northstar_mercury_addon:sunbath_speed`，`+0.30`，`Operation.ADD_MULTIPLIED_TOTAL`；
  - 常量 `EXHAUSTION_MULTIPLIER = 0.4F`（饥饿消耗 ×0.4，由 Mixin 施加）。
- **伤害类型**：`registry/MNADamageTypes` 只持有 `ResourceKey`，实体定义在 `data/northstar_mercury_addon/damage_type/*.json`：
  - `solar_storm`（message_id `solarStorm`）、`meteor`（`meteor`）、`sunfish_ram`（`sunfishRam`）、`sunfish_flame`（`sunfishFlame`）、`sunfish_beam`（`sunfishBeam`）；
  - 五个 JSON 参数一致：`effects: "burning"`、`exhaustion: 0.1`、`scaling: "when_caused_by_living_non_player"`；
  - `source(level, key)` / `source(level, key, attacker)` 用 `registryOrThrow(DAMAGE_TYPE).getHolderOrThrow(...)` 解析。
- **声音**（`registry/MNASounds`，全部 `SoundEvent.createVariableRangeEvent`）与 `sounds.json` 指向：
  - `solar_storm_warning` → `northstar:martian_duststorm_above`
  - `solar_storm_wind` → `northstar:martian_duststorm_above`
  - `meteor_falling` → `northstar:laser_burn1/2/3`
  - `meteor_impact` → `northstar:rocket_blast_1/3/5`
  - 本模组不打包任何 `.ogg`，全部复用北极星已有音频。
- **网络负载**：`network/SolarStormPayload(boolean active)`，`Type` = `northstar_mercury_addon:solar_storm`，`StreamCodec.composite(ByteBufCodecs.BOOL, SolarStormPayload::active, SolarStormPayload::new)`；`event.registrar("1").playToClient(TYPE, STREAM_CODEC, SolarStormPayload::handleOnClient)`；客户端 handler 内先判 `FMLEnvironment.dist.isClient()`，再 `context.enqueueWork(() -> MercuryClient.setStormActive(...))`。

### 2.5 创造模式标签页

- `CREATIVE_TABS.register("main", ...)` → `NorthstarMercuryAddon.MERCURY_TAB`；
- 标题 `itemGroup.northstar_mercury_addon`，图标 `MNAItems.MERCURY_SAND`；
- 内容顺序：水星沙 → 太阳鱼片 → 太阳鱼果烩 → 太阳鱼刷怪蛋 → 炽烈太阳鱼刷怪蛋。

## 3. 世界生成

**群系清单**：只有 1 个——`northstar_mercury_addon:dry_riverbed`（干涸海床），`data/northstar_mercury_addon/worldgen/biome/dry_riverbed.json`：

- 气候与效果：`temperature 0.8`、`downfall 0.4`、`has_precipitation false`、`sky_color 0`、`fog_color 0`、`water_color 4159204`、`water_fog_color 329011`；
- `carvers.air` = `minecraft:cave_extra_underground` + `minecraft:cave`；
- 刷怪沿用北极星水星地表：`northstar:mercury_raptor`（权重 30，1~3）、`mercury_tortoise`（30，1~3）、`mercury_roach`（30，2~5），其余刷怪类别为空；
- 已加入 `data/northstar/tags/worldgen/biome/mercury_biomes.json`（`replace: false`，只追加本群系）。

**注入方式**（`world/MercuryBiomeInjector.onServerAboutToStart`，挂 `ServerAboutToStartEvent`，即任何 level / 区块创建之前）：

1. 从 `Registries.LEVEL_STEM` 取 `northstar:mercury`，缺失则 `warn` 并放弃。
2. 要求其 `ChunkGenerator#getBiomeSource()` 同时是 `MultiNoiseBiomeSource`、`ExtendableBiomeSource`、`RefreshableBiomeSource`（后两个由 Mixin 挂上），否则 `error` 并放弃。
3. 从群系注册表取 `northstar_mercury_addon:dry_riverbed` 的 `Holder`，缺失则 `error` 并**保持维度原样**。
4. 若当前条目里已有该群系，`info` 后直接返回，不重复注入。
5. 定位 `northstar:mercury_hills` 且大陆度区间**恰好为 `[-1.0, 1.0]`**（用 `Climate.quantizeCoord` 量化后比较）的那一条；找不到则 `error` 并放弃（注释标注这是 Northstar 0.6.5 的形状）。
6. 把该条拆成三条子盒（`withContinentalness` 只替换 `continentalness`，temperature/humidity/erosion/depth/weirdness/offset 全部继承原条目）：
   - `[-1.0, -0.23]` → 仍是 `mercury_hills`
   - `[-0.23, -0.09]` → `dry_riverbed`
   - `[-0.09, 1.0]` → `mercury_hills`
   - 三个子盒互斥且恰好拼满原盒子，因此原盒内外的其它群系判定都保持不变；`[-0.19, -0.11]`（原版河流 / `northstar:mercury_basins` 的大陆度带）落在新带中心。
7. `mna$setClimateEntries(revised)` 写回 → `mna$refreshPossibleBiomes()` 重建候选集 → 再校验 `possibleBiomes()` 确实包含新群系 → 打印完整分区边界日志。

**feature / placed_feature**（群系 `features` 数组按原版 `GenerationStep.Decoration` 顺序）：

| 索引 | 内容 |
| --- | --- |
| 0 | `northstar:mercury_crater_sparse`、`northstar:mercury_crater_big_sparse` |
| 1 | `minecraft:lake_lava_underground` |
| 2（LOCAL_MODIFICATIONS） | `northstar_mercury_addon:dry_riverbed_channel`、`dry_riverbed_sand`、`dry_riverbed_stone` |
| 6（UNDERGROUND_ORES） | 北极星 17 条矿脉/岩团 placed_feature（zinc / titanium / tungsten ×3 / iron / diamond / copper / gold / lapis / redstone ×2 / 4 种 blob / glowstone） |
| 7（UNDERGROUND_DECORATION） | `northstar_mercury_addon:dry_riverbed_ancient_debris` |
| 3/4/5/8/9/10 | 空数组 |

- `dry_riverbed_channel`：`configured_feature` 类型 `northstar_mercury_addon:riverbed_channel`（`config: {}`，代码里注册为 `MNAFeatures.RIVERBED_CHANNEL`，`NoneFeatureConfiguration.CODEC`）；`placed_feature` = `count 1` + `in_square` + `heightmap OCEAN_FLOOR_WG`。
- `dry_riverbed_sand`：`minecraft:disk`，`simple_state_provider → northstar_mercury_addon:mercury_sand`，target 为 `northstar:mercury_stone` 或 `minecraft:andesite`，`radius uniform 3~8`，`half_height 2`；placed = `count 28` + `in_square` + `heightmap OCEAN_FLOOR_WG` + `biome`。
- `dry_riverbed_stone`：`minecraft:disk`，state 为 `northstar:mercury_stone`，target 为 `northstar_mercury_addon:mercury_sand`，`radius uniform 2~5`，`half_height 1`；placed = `count 14` + 同样的四段滤镜（与 sand 互为反向，形成沙洲与裸岩滩交错）。
- `dry_riverbed_ancient_debris`：`minecraft:scattered_ore`，`size 1`、`discard_chance_on_air_exposure 1.0`，target 为 `tag_match #northstar:mercury_stone_replaceable`，产物 `minecraft:ancient_debris`；placed = `rarity_filter chance 128` + `in_square` + `height_range trapezoid 8~24` + `biome`。

**biome_modifier**：`data/northstar_mercury_addon/neoforge/biome_modifier/mercury_sky_sunfish.json`，`neoforge:add_spawns`，biomes = `#northstar:mercury_biomes`：

- `northstar_mercury_addon:sunfish` 权重 `12`、`minCount 1` / `maxCount 3`；
- `northstar_mercury_addon:blazing_sunfish` 权重 `2`、`minCount 1` / `maxCount 1`。

群系特征不由 modifier 注入，而是直接写在 `dry_riverbed.json` 的 `features` 数组里；**陨石坑的矿石也不是 placed_feature**，而是运行时由 `SolarStormManager.carveCrater` 用 `MeteorOres` 权重表 `setBlock`（见 4.2）。

## 4. 核心机制

### 4.1 太阳风暴（`world/SolarStormManager` + `world/SolarStormData` + `world/SolarStormSettings`）

**状态存储**

- `SolarStormData extends SavedData`，存在主世界 `getDataStorage().computeIfAbsent(DATA_FACTORY, "northstar_mercury_addon_solar_storm")`（主世界常驻，服务器重启不丢）。
- NBT 字段：`Active`(boolean)、`EndTime`(long；`Long.MAX_VALUE` 表示无限期)。
- API：`start(gameTime, durationTicks)`（`durationTicks <= 0` → 无限期，否则 `endTime = gameTime + durationTicks`）、`stop()`（清 `active`、`endTime` 归 `MAX_VALUE`）、`remaining(gameTime)`（未激活或无限期返回 `-1`）。

**服务器刻状态机**（`onServerTick`，`ServerTickEvent.Post`）

1. 已激活且 `gameTime >= endTime` → `stop(server)` 并 return。
2. `gameTime % SYNC_INTERVAL(100) == 0` → `sync(server, data.isActive())` 心跳。
3. 取 `northstar:mercury` 的 `ServerLevel`；为 `null` 或 `players().isEmpty()` 则**直接 return**（不推进灼烧与陨石，但风暴计时照常走，所以水星无人时风暴照样会按时结束）。
4. 未激活 → `maybeStartNaturally`；已激活 → `tickStorm`。

**自然触发**（`maybeStartNaturally`，仅在水星有玩家时才会被调用）

- `gameTime % NATURAL_CHECK_INTERVAL(1200) != 0` 直接返回（每 60 秒掷一次）；
- `mercury.getRandom().nextFloat() >= NATURAL_CHANCE(0.02F)` 返回（约 2%，即平均约 50 分钟一场）；
- 成立则 `start`，时长 `NATURAL_MIN_DURATION(2400) + nextInt(7200-2400+1)` 刻 → 2~6 分钟。

**指令触发**（`command/SolarStormCommand`，`Commands.literal("solarstorm")`，`hasPermission(2)`）

| 子命令 | 行为 |
| --- | --- |
| `start` | 默认 `DEFAULT_COMMAND_DURATION = 6000` 刻（5 分钟） |
| `start infinite` | 传 `0` → 无限期 |
| `start <seconds>` | `IntegerArgumentType.integer(1, 3600)`，×20 转刻 |
| `stop` | 未在跑时 `sendFailure` 并返回 0 |
| `query` | 打印剩余秒数或「无时限」 |
| `impact` | 要求 `source.getLevel()` 维度 = `northstar:mercury`，否则失败；在源位置的 x/z 调 `dropMeteor` |

**灼烧**（`scorchExposedEntities`，每 `SCORCH_INTERVAL = 20` 刻一次，即每秒）

- 遍历 `mercury.getAllEntities()`，跳过：非 `LivingEntity`、`!isAlive()`、`isSpectator()`、实现 `SolarStormImmune` 的生物（两种太阳鱼）、`isCreative()` 的玩家、以及 `!mercury.canSeeSky(living.blockPosition())` 的实体。
- 命中者：`hurt(solar_storm, SCORCH_DAMAGE = 3.0F)` + `setRemainingFireTicks(SCORCH_FIRE_SECONDS(6) * 20)`；若是 `ServerPlayer` 再给 `MOVEMENT_SLOWDOWN`（`SCORCH_SLOW_DURATION = 40` 刻、`SCORCH_SLOW_AMPLIFIER = 0`）。
- 结论：屋顶、洞穴、走廊都是真掩体；创造模式免疫。

**陨石轰炸**

- 静态瞬态计数器 `meteorCountdown`，初值 `METEOR_FIRST_DELAY = 200`，**不写入存档**。
- 风暴期间每刻自减，归零则重置为 `METEOR_MIN_INTERVAL(260) + nextInt(700-260+1)`（13~35 秒间隔）并来一轮齐射。
- 齐射（`spawnMeteorVolley`）：随机挑一名水星玩家为目标，`count = 1 + random.nextInt(METEOR_MAX_PER_VOLLEY = 1)` = 1 颗；落点角度随机、水平距离 `20 + random*(60-20)`（`METEOR_MIN/MAX_DISTANCE`）。
- `dropMeteor(level, x, z)`（同时被 `/solarstorm impact` 复用，保证物理一致）：y = `getMaxBuildHeight() - METEOR_SPAWN_HEIGHT_BELOW_TOP(100)`；`deltaMovement = (±0.05 随机, -METEOR_INITIAL_SPEED(1.4), ±0.05 随机)`；生成时 `playSound(METEOR_FALLING, WEATHER, 4.0F, 0.7~1.0F)`。

**播报与同步**

- `announce`：给**水星在线玩家** `displayClientMessage("message.northstar_mercury_addon.solar_storm.begin|end")`，起始时另加 `playNotifySound(SOLAR_STORM_WARNING, WEATHER, 1.6F, 1.0F)`；再 `server.sendSystemMessage` 全局一行；同时 `LOGGER.info`。
- `sync(server, active)`：向**全部**在线玩家发 `SolarStormPayload`——每 100 刻的心跳覆盖迟到进服与维度切换，客户端收到后自行判断是否在水星。

### 4.2 陨石与陨石坑（`entity/MeteorEntity` + `SolarStormManager.meteorImpact` + `world/MeteorOres`）

- 物理参数：`getDefaultGravity() = 0.05`、每刻速度 ×`DRAG = 0.98`（终端速度 ≈ 2.5 格/刻）、`MAX_LIFETIME_TICKS = 600` 未落地则 `discard()`。
- `tick()` 顺序：`applyGravity()` → `move(MoverType.SELF, ...)` → 速度缩放到 `DRAG`；服务端每刻 `emitTrail`（FLAME×4、LARGE_SMOKE×2、LAVA×1、END_ROD×1）；`onGround() || horizontalCollision` → `meteorImpact(server, blockPosition())` 后 `discard()`。
- `isAlwaysTicking() = true`：避免跨 16 格区段时实体静默停止 tick 而悬在半空（与原版玩家实体走同一钩子）。
- `meteorImpact` = `carveCrater` → `damageAroundImpact` → `impactEffects`。
- **成坑**（`carveCrater`）：
  - 水平半径 `CRATER_MIN_RADIUS(2) + nextInt(2)` → 2~3；深度 `CRATER_MIN_DEPTH(2) + nextInt(2)` → 2~3；
  - 按 `(dx²+dz²)/radius² + dy²/depth² <= 1` 的**椭球**遍历（水平半径明显大于深度，是浅碟形不是深井），`dy ∈ [-depth, 1]`；
  - 跳过 `y <= level.getMinBuildHeight()+1`、空气、基岩；命中则 `setBlock(AIR, UPDATE_ALL)`，`dy <= 0` 的格子记入 `floor`；
  - 只装饰「真坑底」：`floor` 里位置的下方不在本次 `placed` 集合中、且下方非空气才处理；
  - 先试矿：`oresPlaced < ORE_MAX_PER_CRATER(4) && random < ORE_CHANCE(0.08F)` → `MeteorOres.pick`，成功则 `oresPlaced++` 并 continue；
  - 否则 `random < MAGMA_CHANCE(0.08F)` → 岩浆块；`random < 0.08F + SAND_CHANCE(0.30F)` → 水星沙；其余保留原方块（水星石）；
  - 结束 `LOGGER.debug` 打印 `radius / depth / ores seeded`。
- **撞击伤害**（`damageAroundImpact`）：AABB 以落点为中心 `inflate(IMPACT_RADIUS = 5.0)`，排除死亡实体与创造模式玩家；命中者 `hurt(meteor, IMPACT_DAMAGE = 10.0F)`、`setRemainingFireTicks(IMPACT_FIRE_SECONDS(5) * 20)`、`push(水平单位向量 × IMPACT_KNOCKBACK(0.55), 0.35, 水平单位向量 × 0.55)`（水平距离下限 `0.4` 防除零）。
- **表现**（`impactEffects`）：`EXPLOSION_EMITTER`×1、`EXPLOSION`×8、`LARGE_SMOKE`×40、`FLAME`×60、`LAVA`×20、`ASH`×60（`y+1`），并 `playSound(METEOR_IMPACT, WEATHER, 2.5F, 1.0F)`。
- 客户端用原版 `ThrownItemRenderer` 渲染：`getItem()` 返回 `Blocks.MAGMA_BLOCK`、`displayFireAnimation() = true`、`fireImmune() = true`。
- **矿石权重表**（`world/MeteorOres`，id 全在 `northstar` 命名空间、`BuiltInRegistries.BLOCK` 运行时解析并缓存）：`mercury_copper_ore` 2、`mercury_iron_ore` 2、`mercury_zinc_ore` 2、`mercury_gold_ore` 1、`mercury_redstone_ore` 1、`mercury_lapis_ore` 1、`mercury_titanium_ore` 1、`mercury_tungsten_ore` 1、`mercury_diamond_ore` 1、`mercury_glowstone_ore` 1（总权重 12）。缺失 id 会 `warn` 并跳过；`totalWeight == 0` 时返回空气。

### 4.3 沐浴阳光（`effect/SunbathEffect` + `world/SunbathManager`）

- **触发条件**（`meetsConditions`，需全部满足）：不是旁观者；`level.dimension() == northstar:mercury`；白天；`player.getY() ∈ [地表 - MAX_DEPTH_BELOW_SURFACE(2.0), 地表 + MAX_HEIGHT_ABOVE_SURFACE(6.0)]`（地表取 `Heightmap.MOTION_BLOCKING_NO_LEAVES`）；`level.canSeeSky(pos)` 且 `level.canSeeSky(pos.above())`。
- **白天判定**（`isDaytime`）：`level.dimensionType().fixedTime().orElse(level.getDayTime())`，`floorMod(time, DAY_LENGTH(24000)) < DAY_END(12000)`——因为原版 `Level#isDay()` 对固定时间的维度会返回 false。
- **维持逻辑**（`onPlayerTick`，`PlayerTickEvent.Post`，仅服务端，每 `CHECK_INTERVAL = 10` 刻）：
  - 满足条件、且当前效果不是食物来源、且（无效果或剩余 ≤ `REFRESH_BELOW = 30`）→ 续一个 `EFFECT_DURATION = 100` 刻、`ambient = true`、无粒子、显示图标的实例；
  - 不满足条件、且当前效果非食物来源 → 立即 `removeEffect`。
- **两条来源的区分**：日光给的效果 `ambient = true`；果烩给的 `foodEffect()` = `new MobEffectInstance(SUNBATH, FOOD_DURATION(20*120), 0, false, false, true)`（`ambient = false`）。`isFoodGranted` = `!instance.isAmbient()`，所以日光逻辑完全不碰果烩 buff，它在洞穴、夜晚、其它维度都能撑满 2 分钟。
- **效果本体**：`+30%` 移动速度（`ADD_MULTIPLIED_TOTAL`，随效果消失）+ 饥饿消耗 ×`0.4`（`PlayerExhaustionMixin` 在 `Player#causeFoodExhaustion(float)` 入参处缩放，是唯一覆盖冲刺/跳跃/攻击/受伤等全部消耗的收口点）。
- **与太阳鱼的休战**（`canSunfishAttack`，唯一门禁）：`null`/死亡/旁观/`isInvulnerable` → false；`SunfishEntity`（含子类）→ false（两种鱼不互相攻击）；玩家需「非创造模式且非沐浴阳光」；其余活体 → true。

### 4.4 水星沙（`block/MercurySandBlock`）

- 继承 `FallingBlock`，只实现 1.21 要求的 `codec()`；重力行为完全复用原版。
- 三条生成途径：`dry_riverbed_sand` 的 disk 特征（半径 3~8）、`RiverbedChannelFeature.layBed` 的坑底 2 格、陨石坑 `SAND_CHANCE = 0.30` 的坑底。
- 加工：熔炼 200 刻 / 高炉 100 刻 → `minecraft:glass`，经验 0.1；方块掉落表带 `minecraft:survives_explosion`。

### 4.5 干涸海床下陷（`world/RiverbedChannelFeature`）

- 常量：`MIN_DEPTH = 4`、`MAX_DEPTH = 5`、`RAMP_QUARTS = 2`（1 个 quart = 4 格，故岸坡 4~8 格）、`BED_SAND_DEPTH = 2`。
- `place()`：`new ChunkPos(context.origin())` 定区块，`depth = 4 + nextInt(2)`；`Band.sample` 在 quart 网格采样（外扩 `RAMP_QUARTS+1 = 3` 个 quart，逐格 `getHeight(OCEAN_FLOOR_WG)` 得 y，再 `getBiome(...).is(dry_riverbed)`），整区块无命中则返回 `false`。
- 逐列处理（`chunk.getMinBlockX()..getMaxBlockX()` × 同 Z 范围）：
  - `easeToEdge(x, z)`：在四个轴方向上数连续同群系 quart 格，取最小值并封顶 `RAMP_QUARTS`；`0` = 岸上不挖；
  - `dig = round(depth * ease / RAMP_QUARTS)`，≤0 跳过；
  - `surface = getHeight(OCEAN_FLOOR_WG, x, z) - 1`，`target = max(surface - dig, getMinBuildHeight() + 1)`；
  - `digColumn`：从 `surface` 向下挖到 `target`，遇任何流体（`!getFluidState().isEmpty()`）即停手（绝不挖开岩浆），空气跳过；
  - `layBed`：把 `target` 起 `BED_SAND_DEPTH(2)` 格改为水星沙，遇空气/流体中断，已是水星沙则跳过。
- 因为 placed_feature 只有 `count 1` 且原版按区块逐个应用特征，每列只执行一次，深度可预测；每列以**自身地表**为基准，河道跟随地形起伏。

### 4.6 太阳鱼 AI 与战斗（`entity/SunfishEntity` + `SunfishAttackGoal` + `SunfishCombatSettings` + `SunfishAttackMode`）

**悬浮与移动**

- `getDefaultGravity() = 0`；`aiStep()` 仅在服务端生效：攻击模式为 `NONE` 时调 `hoverAtAltitude()`，否则把 `attackVelocity` 直接写入 `deltaMovement`（写在 `super.aiStep()` 之后，所以移动控制在当刻无法覆盖它）。
- `hoverAtAltitude()`：目标高度 = `地表 + PREFERRED_ALTITUDE(24) + altitudeOffset()`，容忍带 `±ALTITUDE_TOLERANCE(6)`；带外每刻 `dy ± ALTITUDE_CORRECTION(0.03)`，上升/下降封顶 `MAX_VERTICAL_SPEED(0.12)`；若目标高出自身超过 `RECOVERY_GAP(10)`，上升封顶放宽到 `RECOVERY_MAX_VERTICAL_SPEED(0.28)`；带内 `dy = dy*0.6 + sin((tickCount + id*11)*0.06)*0.008`。
- `altitudeOffset()` = `(id*37 % (ALTITUDE_SPREAD(14)*2)) - 14`，让鱼群在垂直方向散开而不是挤在一个平面。
- 移动控制 `FlyingMoveControl(this, 12, true)`，导航 `FlyingPathNavigation`。

**目标选择与休战过滤**

- `goalSelector`：0 = `SunfishAttackGoal`，1 = `WaterAvoidingRandomFlyingGoal(1.0)`，2 = `LookAtPlayerGoal(Player, 12.0F)`，3 = `RandomLookAroundGoal`。
- `targetSelector`：1 = `HurtByTargetGoal`，2 = `NearestAttackableTargetGoal<Player>(this, Player.class, true, SunbathManager::canSunfishAttack)`。
- 四重过滤：覆写 `canAttack`（覆盖 `TargetingConditions` 与目标保留）、覆写 `setTarget`（拦 `HurtByTargetGoal` 的直接写入）、`customServerAiStep` 每刻复查已有目标（获得沐浴阳光后 1 刻内被丢弃）、`SunfishAttackGoal.target()` 每次取目标再过滤一次。
- `shouldDespawnInPeaceful() = true`；冷却在 `customServerAiStep` 里每刻递减（即使没有攻击目标）。

**免疫**

- 实现 `SolarStormImmune` → 风暴灼烧扫描直接跳过；
- `hurt()` 拒绝 `northstar:suffocation_no_oxygen`、`northstar:suffocation_no_spacesuit`、`solar_storm`；
- 数据包层还登记在 `#northstar:doesnt_require_oxygen`、`#northstar:can_survive_heat`、`#northstar:can_survive_cold`。

**攻击节奏**

- 初始冷却 `INITIAL_MIN_COOLDOWN(40) ~ INITIAL_MAX_COOLDOWN(80)` 刻（出生/读档后的缓冲）；每次攻击结束 `ATTACK_MIN_COOLDOWN(60) ~ ATTACK_MAX_COOLDOWN(110)` 刻（3~5.5 秒）。
- `canUse`：非客户端、冷却为 0、目标 3D 距离 ≤ `ATTACK_RANGE(44)`；`canContinueToUse`：存活、未到时长、目标距离 ≤ `LOSE_TARGET_RANGE(56)`。
- 攻击期间 `setFlags({MOVE, LOOK})`、`requiresUpdateEveryTick() = true`、`isInterruptable() = false`（漫游目标不能打断攻击）。
- `start()` 用 `nextBoolean()` 选「俯冲」或「第二种攻击」；第二种由 `usesRayAttack()` 决定是 `WING_FLAP`（太阳鱼）还是 `RAY`（炽烈太阳鱼）。模式写入 synched data `ATTACK_MODE`（`EntityDataSerializers.INT`），客户端模型据此播动画。

**旋转俯冲（两种鱼共有，`tickDive`）**

- 起手 `DIVE_WINDUP = 12` 刻原地以 0.05/刻上升并撒 `SMALL_FLAME`，第 1 刻播 `PHANTOM_SWOOP(1.1, 0.8)`；
- 之后以 `DIVE_SPEED = 0.85` 的速度向量直冲目标胸口（`position + bbHeight*0.5`），每刻 `getYRot() += DIVE_SPIN_DEGREES(34)`、`setYHeadRot` 跟随；
- 命中判定：`getBoundingBox().inflate(DIVE_HIT_RADIUS = 1.9)` 内的活体，用 `rammed` 集合（entityId）保证一次攻击内只结算一次，且需 3D 距离 ≤ 1.9；
- 伤害 = `ATTACK_DAMAGE`（3 / 8），伤害类型 `sunfish_ram`；额外 `setRemainingFireTicks(getFireSecondsOnHit()*20)`（太阳鱼 = 0，炽烈鱼 = 6 秒）；击退 `push(水平×DIVE_KNOCKBACK(0.75), 0.32, 水平×0.75)`，播 `PLAYER_ATTACK_KNOCKBACK(0.9, 0.7)`；
- 收尾：距目标 < 1.2 或持续 `DIVE_MAX_TICKS = 60` 刻。

**拍翅散射（太阳鱼独有，`tickWingFlap` + `flapBurst`）**

- `FLAP_APPROACH = 26` 刻飞到目标旁：目标点 = 目标位置 + `FLAP_HOVER_HEIGHT(2.0)` + 随机方位 × `FLAP_HOVER_DISTANCE(3.2)`，速度 `min(FLAP_APPROACH_SPEED(0.55), dist*0.25)`，第 1 刻播 `ENDER_DRAGON_FLAP(1.0, 1.3)`；
- `FLAP_WINDUP = 10` 刻蓄力（每 3 刻撒 SMALL_FLAME）后，每 `FLAP_BURST_INTERVAL = 9` 刻放一次火环，共 `FLAP_BURSTS = 3` 次；
- 每次爆发：24 方向 FLAME 环 + 10 个 LAVA + `ENDER_DRAGON_FLAP(1.3, 0.9)`；范围 `FLAP_RADIUS = 4.5`，伤害 = `ATTACK_DAMAGE × FLAP_DAMAGE_SCALE(0.6)`，点燃 `FLAP_FIRE_SECONDS = 3` 秒；伤害类型 `sunfish_flame`。

**高能射线（炽烈太阳鱼，`tickRay` + `fireRay`）**

- `RAY_WINDUP = 16` 刻悬停蓄能（第 1 刻 `BEACON_ACTIVATE(1.0, 1.6)`，每 2 刻撒 END_ROD），之后每 `RAY_INTERVAL = 10` 刻发一条，共 `RAY_SHOTS = 4` 条；
- 每条：从眼高到目标眼高的 hitscan，长度上限 `RAY_LENGTH = 32`，用 `server.clip(ClipContext(COLLIDER, NONE))` 截断在首个实心方块；`drawBeam` 分 `RAY_BEAM_SEGMENTS = 16` 段撒 END_ROD（每 4 段补 SMALL_FLAME）；播 `BLAZE_SHOOT(1.4, 0.7)`；
- 判定：`new AABB(start, end).inflate(1.0)` 内、且 `victim.getBoundingBox().inflate(0.3).clip(start, end)` 非空的活体；伤害 = `ATTACK_DAMAGE × RAY_DAMAGE_SCALE(0.8)`，点燃 `RAY_FIRE_SECONDS = 2` 秒；**穿透**——不在第一个受害者处停止；伤害类型 `sunfish_beam`。

**炽烈太阳鱼的差异**

- `usesRayAttack() = true`；`getFireSecondsOnHit() = 6`（俯冲命中额外点燃 6 秒，`BITE_FIRE_SECONDS`）；
- `doHurtTarget` 覆写：近战命中也让目标点燃 6 秒；
- `fireImmune() = true`（实体类型也标了 `fireImmune()`）；
- 音效换 `BLAZE_AMBIENT/HURT/DEATH`，音量 `0.5`、音调 `1.4`；
- 客户端每 3 刻撒 `SMALL_FLAME`、每 12 刻撒 `LAVA`。

## 5. Mixin

`src/main/resources/northstar_mercury_addon.mixins.json`：`required: true`、`minVersion "0.8.5"`、`compatibilityLevel "JAVA_17"`、`package com.suwako.northstarmercuryaddon.mixin`、`injectors.defaultRequire 1`；三个 Mixin 都在 `mixins` 数组里（无 `client` 分组）。

| Mixin | 目标类 | 手段 | 作用 |
| --- | --- | --- | --- |
| `MultiNoiseBiomeSourceMixin` | `MultiNoiseBiomeSource` | 实现 `ExtendableBiomeSource`；`@Shadow @Final @Mutable Either<Climate.ParameterList<Holder<Biome>>, Holder<MultiNoiseBiomeSourceParameterList>> parameters` | `mna$getClimateEntries()` 用 `Either.map` 取已解码条目；`mna$setClimateEntries()` 替换为 `Either.left(new Climate.ParameterList<>(List.copyOf(entries)))` |
| `BiomeSourceMixin` | `BiomeSource` | 实现 `RefreshableBiomeSource`；`@Shadow @Final @Mutable Supplier<Set<Holder<Biome>>> possibleBiomes` + `@Shadow abstract Stream<Holder<Biome>> collectPossibleBiomes()` | `mna$refreshPossibleBiomes()` 用 `Suppliers.memoize(...distinct().collect(toImmutableSet()))` 重建候选集缓存 |
| `PlayerExhaustionMixin` | `Player` | `@ModifyVariable(method = "causeFoodExhaustion(F)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)` | 有 `SUNBATH` 效果时把入参 ×`SunbathEffect.EXHAUSTION_MULTIPLIER (0.4F)` |

全部是 `@Shadow` 访问器与 `@ModifyVariable` 式的低侵入注入，未使用 `@Redirect`。

## 6. 客户端

**注册入口**（`client/MercuryClient.register`，仅客户端 dist 会调用）

- mod bus：`RegisterLayerDefinitions`、`RegisterRenderers`；
- `NeoForge.EVENT_BUS`：`ClientTickEvent.Post`、`ViewportEvent.ComputeFogColor`、`ViewportEvent.RenderFog`、`ViewportEvent.ComputeCameraAngles`。

**渲染器与模型**

- `meteor`：原版 `ThrownItemRenderer<>(ctx, 2.0F, true)`（放大 2 倍、满亮）。
- `SunfishRenderer`：`MobRenderer` + `SunfishModel`，影子 `0.5`，贴图 `textures/entity/sunfish.png`，附加 `EyesLayer` + `RenderType.eyes(sunfish_glow.png)`。
- `BlazingSunfishRenderer`：`MobRenderer` + `BlazingSunfishModel`，影子 `0.7`，贴图 `textures/entity/blazing_sunfish.png`，发光层 `blazing_sunfish_glow.png`，`scale(1.15F)` 统一放大。
- `SunfishModel`（`HierarchicalModel`，64×64）：部件树 `body → head(→snout, eye_left, eye_right)、dorsal_fin、pectoral_left(→tip)、pectoral_right(→tip)、anal_fin、tail(→base(→fan(→lobe_upper, lobe_lower)))`，共 17 个部件；类注释里带完整 UV 槽位表（body `(0,0) 8×7×12`、head `(40,0) 6×6×4`、dorsal `(0,19) 1×7×6`、wing `(14,19) 10×1×6`、tail fan `(0,32) 2×10×3` 等）。
- `BlazingSunfishModel`：复用同一套构建函数（`bodyCubes()` / `addHead(body, true)` / `addFins` / `addTail(body, true)`）与 UV 表，只加 8 个部件——`crest_1..4`（`(18,32) 1×5×1`）、`vent_left/right`（`(22,32) 1×5×3`）、`flame_upper/lower`（`(30,32) 1×6×4`）。
- 所有部件查找都走 `childOrEmpty()`：名字不匹配只 `warn` 并把该部件设为空，不会在资源重载时抛异常。

**动画**

- 滑翔而非走路：躯干蛇行 + 浮沉（`body.y = 8 + sin(t*0.09)*0.7`）、`xRot` 跟随真实速度矢量（`clamp(-motionY*7, ±0.4)`，上升抬头/下潜低头）。
- 尾巴是行进波：`tail → tailBase → tailFan → lobe_upper/lower` 的相位依次延后 `-0.35 / -0.7 / -1.1`。
- 胸鳍 `flap = sin(t*0.14)`，翼尖用 `sin(t*0.14 - 0.6)` 滞后；背鳍与臀鳍反相轻摆。
- 攻击姿态由 synched data `getAttackMode()` 改写已算好的滑翔姿态：
  - `DIVE`：`body.yRot = t*1.35`（连续自旋）+ `xRot = -0.45 + pitch*0.3` + 收翼；
  - `WING_FLAP`：`sin(t*0.95)` 高频振翅、翼尖再滞后 0.8；
  - `RAY`：后仰、双翼后掠、低头瞄准（`head.xRot = -0.25`）。
- `BlazingSunfishModel.animateExtras`：4 根骨冠依次摇动；`sin(t*0.06)`（约 7 秒）周期的「熔岩涌动」让 `vent` 的 `xScale/zScale` 鼓起、尾焰 `yScale` 拉长并左右甩动。

**风暴表现**（`isStormVisible` = `stormActive && level.dimension() == northstar:mercury`，全部本地生成、不占网络）

- 雾：`ComputeFogColor` 设 `(FOG_RED 0.82, FOG_GREEN 0.18, FOG_BLUE 0.05)`；`RenderFog` 在 `FOG_TERRAIN` / `FOG_SKY` 下 `scaleFarPlaneDistance(0.48F)`、`scaleNearPlaneDistance(0.6F)`。
- 每客户端刻粒子：`SOLAR_FOG (1.0, 0.18, 0.05) size 3.8` × `FOG_PARTICLES_PER_TICK(5)`、`SOLAR_DUST (1.0, 0.62, 0.18) size 2.2` × 8、从 `y+14 ~ y+14+SKY_HEIGHT(48)` 投下 `END_ROD` / `ELECTRIC_SPARK` / `SMALL_FLAME` 各 3、`SOLAR_EMBER (1.0, 0.32, 0.06) size 1.5` × 4；`tickCount % 6 == 0` 再加 `SMALL_GUST` + `ASH`；水平散布为 `AMBIENCE_RADIUS(56)` 的 ±半宽。
- 高空耀斑（`maybeFlare`）：`flareCountdown` 初值 60，归零后重置为 `FLARE_MIN_INTERVAL(80) ~ FLARE_MAX_INTERVAL(220)` 刻；放 `FLASH` + 14×(`END_ROD` + `GLOW`) 于 `y+26 ~ y+46`，并 `playLocalSound(LIGHTNING_BOLT_THUNDER, WEATHER, 0.55F, 0.9~1.15F)`。
- 镜头晃动：`exposure`（0~1）每 `EXPOSURE_CHECK_INTERVAL(4)` 刻按 `level.canSeeSky(player.blockPosition())` 向 0/1 移动，步长 `EXPOSURE_FADE_PER_TICK(0.08F)`（约 12 刻完成过渡）；幅度 `CAMERA_SWAY_DEGREES(0.7F) × exposure`，加到 roll（两项正弦加权）与 yaw（`cos(t*1.3)*sway*0.25`）。`updateExposure` 放在提前 return 之前，保证躲进掩体后强度能回落。
- 三层循环环境音（`SimpleSoundInstance` + `Attenuation.NONE` + `keepLooping` 在引擎丢弃后重播）：`MNASounds.SOLAR_STORM_WIND` 音量 `1.15`、`SoundEvents.AMBIENT_SOUL_SAND_VALLEY_LOOP` `1.0`、`SoundEvents.AMBIENT_BASALT_DELTAS_LOOP` `0.7`；风暴结束或离开水星时 `stopStormAmbience` 立即停。

## 7. 数据包与资源布局

```
data/northstar_mercury_addon/
├── damage_type/solar_storm.json
├── damage_type/meteor.json
├── damage_type/sunfish_ram.json
├── damage_type/sunfish_flame.json
├── damage_type/sunfish_beam.json
├── loot_table/blocks/mercury_sand.json
├── loot_table/entities/sunfish.json
├── loot_table/entities/blazing_sunfish.json
├── recipe/sunfish_jam.json
├── recipe/glass_from_smelting_mercury_sand.json
├── recipe/glass_from_blasting_mercury_sand.json
├── neoforge/biome_modifier/mercury_sky_sunfish.json
└── worldgen/
    ├── biome/dry_riverbed.json
    ├── configured_feature/dry_riverbed_channel.json
    ├── configured_feature/dry_riverbed_sand.json
    ├── configured_feature/dry_riverbed_stone.json
    ├── configured_feature/dry_riverbed_ancient_debris.json
    ├── placed_feature/dry_riverbed_channel.json
    ├── placed_feature/dry_riverbed_sand.json
    ├── placed_feature/dry_riverbed_stone.json
    └── placed_feature/dry_riverbed_ancient_debris.json
data/minecraft/tags/block/sand.json
data/minecraft/tags/block/mineable/shovel.json
data/northstar/tags/block/mercury_stone_replaceable.json
data/northstar/tags/worldgen/biome/mercury_biomes.json
data/northstar/tags/entity_type/doesnt_require_oxygen.json
data/northstar/tags/entity_type/can_survive_heat.json
data/northstar/tags/entity_type/can_survive_cold.json
assets/northstar_mercury_addon/
├── blockstates/mercury_sand.json
├── models/block/mercury_sand.json          (parent: minecraft:block/cube_all)
├── models/item/mercury_sand.json
├── models/item/sunfish_slice.json
├── models/item/sunfish_jam.json
├── models/item/sunfish_spawn_egg.json      (parent: minecraft:item/template_spawn_egg)
├── models/item/blazing_sunfish_spawn_egg.json
├── sounds.json
├── lang/en_us.json
├── lang/zh_cn.json
├── textures/block/mercury_sand.png
├── textures/item/sunfish_slice.png
├── textures/item/sunfish_jam.png
├── textures/entity/sunfish.png
├── textures/entity/sunfish_glow.png
├── textures/entity/blazing_sunfish.png
├── textures/entity/blazing_sunfish_glow.png
└── textures/mob_effect/sunbath.png
```

- 掉落表：太阳鱼 = 太阳鱼片 1~2 + `northstar:mercury_stone` 1~2 + 水星沙 1~3；炽烈太阳鱼 = 太阳鱼片 2~4 + 水星石 2~4 + 水星沙 2~5。两者的「抢夺加成」都用 `minecraft:enchanted_count_increase` 并显式带 `"enchantment": "minecraft:looting"`（太阳鱼 0~1，炽烈鱼 0~2）。
- 配方：`sunfish_jam` 为 `crafting_shapeless`（1 太阳鱼片 + 2 `minecraft:glow_berries` → 1 果烩，`group: sunfish_jam`）；水星沙 `smelting`(200t) / `blasting`(100t) → `minecraft:glass`。
- 语言文件（`en_us` 与 `zh_cn` 键集合一致）覆盖：`itemGroup`、方块名、群系名、三个实体名、四个物品名、`effect.northstar_mercury_addon.sunbath`、5 条伤害的 `death.attack.*`（各含 `.player` 变体）、5 条 `subtitles.*`、2 条 `message.*`、10 条 `commands.*`。

## 8. 已知限制与注意事项

- **README 与源码的若干不一致（以源码为准）**：
  - `README` 称「太阳鱼片目前是纯材料、不能吃」，但 `MNAItems` 中它带 `FoodProperties`（2 / 0.1），是可食用物品；
  - `README` 表格写炽烈太阳鱼 30 HP，源码是 `MAX_HEALTH = 36.0D`；
  - `README` 说「只注册了三个单次音效事件」，实际 `MNASounds` 注册了 4 个（多了 `solar_storm_wind`，用作客户端循环层）；
  - `README` 说「只用两个 Mixin」，实际 `northstar_mercury_addon.mixins.json` 有 3 个（多出 `PlayerExhaustionMixin`）；
  - `README` 未记录「沐浴阳光」效果、果烩的食用效果与 `PlayerExhaustionMixin`。
- `lang` 里的 `subtitles.northstar_mercury_addon.solar_storm` 没有对应的注册音效（`MNASounds` 只有 4 个 id），疑为遗留键（未确认）。
- 风暴**只在 `northstar:mercury`** 存在；且只在有水星玩家时才推进灼烧与陨石。水星无人时计时照常走，所以离开一阵再回来可能发现风暴已过期。
- `meteorCountdown` 是 `private static int` 瞬态字段，不进存档——服务器重启后按 `METEOR_FIRST_DELAY(200)` 重新计时，不续上半周期。`SolarStormData` 本身持久化在 `world/data/northstar_mercury_addon_solar_storm.dat`。
- 创造模式玩家对灼烧与撞击伤害均免疫，但风暴状态机与陨石生成照常发生（陨石仍会砸向创造玩家附近）。
- 群系注入对 Northstar 的数据形状有**硬性假设**：必须存在大陆度恰为 `[-1.0, 1.0]` 的 `northstar:mercury_hills` 条目。任何一环不满足都只打 `error` 日志并保持水星原样——功能静默缺失而不崩溃，但玩家会完全见不到干涸海床。
- 5 个伤害类型是**数据包条目**，`MNADamageTypes.source(...)` 用 `getHolderOrThrow` 解析；数据包被移除或改名会直接抛异常（代码里没有兜底）。
- `MeteorOres` 的解析结果缓存在静态字段 `resolved` / `totalWeight`，只在首次成坑时解析；此后注册表再变化也不会重算。
- `ORE_MAX_PER_CRATER = 4`，且只在「坑底下方是本次未挖空的原方块」时才装饰，因此坑壁与边缘的露出面不会被铺矿/沙。
- 两种太阳鱼实现了 `Enemy`，但 `MobCategory` 是 `CREATURE`——自然刷新占用生物上限而非怪物上限；`shouldDespawnInPeaceful() = true`，和平难度下会被移除。
- `SolarStormPayload` 用 `registrar("1")`，协议版本 `1`；只有 `playToClient`，没有客户端→服务端包。
- 客户端表现（雾效浓度、粒子密度、镜头晃动幅度、三层循环音的听感、四张实体贴图与模型动画的手感、陨石外观）是纯视觉逻辑，源码无法验证实际观感，需在游戏中确认。
- `blazing_sunfish` 的 `sized(1.55F, 1.4F)` 与渲染端 `scale(1.15F)` 需要保持一致；改碰撞箱时应同步渲染缩放。
