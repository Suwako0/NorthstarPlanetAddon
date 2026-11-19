package com.suwako.northstarplanetaddon.mars.world;

import com.suwako.northstarplanetaddon.NorthstarPlanetAddon;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.suwako.northstarplanetaddon.mars.MarsModule;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 把本模组的 3 个火星群系注入 {@code northstar:mars} 维度的 multi-noise 群系源。
 *
 * <p>Northstar 0.6.5 的 {@code data/northstar/dimension/mars.json} 使用
 * {@code minecraft:multi_noise} 并**内联** biomes 列表，没有官方的追加接口，
 * 因此这里在火星维度加载时反射替换 {@link MultiNoiseBiomeSource} 的内部状态。</p>
 *
 * <h2>顺序很重要（这里踩过坑，改动前先读）</h2>
 * <ol>
 *     <li><b>先刷新 {@link BiomeSource#possibleBiomes} 的缓存，再替换参数表。</b>
 *         {@code possibleBiomes} 是 Guava memoize 的 final 字段，必须一起替换；
 *         反过来做（先参数后缓存）一旦缓存更新失败，就会出现「群系源里有新群系、
 *         但 {@code ChunkGenerator} 的特征索引/装饰逻辑里没有」的半注入状态。</li>
 *     <li>注入后**自检**：{@code possibleBiomes()} 必须真的包含新群系，否则整体回滚。</li>
 *     <li>调用 NeoForge 的 {@link ChunkGenerator#refreshFeaturesPerStep()} 让特征索引重建，
 *         再用 {@link ChunkGenerator#validate()} 立刻触发一次构建 —— 有问题在加载期就暴露，
 *         并且回滚到「只有 Northstar 群系」的干净状态，而不是把世界生成留在一个坏状态里。</li>
 * </ol>
 *
 * <p>任何一步失败都只记日志并放弃注入（火星保持 Northstar 原样），不会崩服、不会产生半注入。</p>
 */
@EventBusSubscriber(modid = NorthstarPlanetAddon.MODID)
public final class MarsBiomeInjector {

    private static final ResourceLocation MARS = ResourceLocation.fromNamespaceAndPath("northstar", "mars");

    /** 用于确认「这个群系源就是火星」的 Northstar 标志性群系。 */
    private static final ResourceLocation MARKER_BIOME =
            ResourceLocation.fromNamespaceAndPath("northstar", "martian_dunes");

    /** 应急开关：加 JVM 参数 {@code -Dnorthstar_planet_addon.disableBiomeInjection=true} 可完全关闭注入。 */
    private static final String DISABLE_PROPERTY = "northstar_planet_addon.disableBiomeInjection";

    // ------------------------------------------------------------------ 气候轴区间常量

    /**
     * 各轴的**实际**取值范围（用 {@code ServerChunkCache.randomState().sampler()} 在
     * ±4096 格 / 32 格步长 / 66049 个点上实测，见 README §5.5）：
     * <pre>
     * temperature    [-0.65, 0.98]  湿度恒为 0
     * continentalness[-0.97, 1.25]
     * erosion        [-0.97, 0.90]
     * depth          [ 0.21, 1.38]   ← 地表处不是 0！
     * weirdness      [-1.21, 1.30]  钟形，p25=-0.23 p50=0.00 p75=0.25
     * </pre>
     * 区间取得比实测略宽，保证「盒子内距离 = 0」。
     */
    private static final Climate.Parameter T_FULL = span(-1.5F, 1.5F);
    private static final Climate.Parameter H_FULL = span(-1.0F, 2.0F);
    private static final Climate.Parameter C_FULL = span(-1.3F, 1.3F);
    private static final Climate.Parameter E_FULL = span(-1.0F, 1.0F);
    private static final Climate.Parameter W_FULL = span(-1.3F, 1.3F);

    /**
     * 地表 depth 区间。实测地表处 depth 为 {@code 0.21~1.38}，所以**不能写 {@code point(0)}**
     * （那会凭空加一个 0.04~1.9 的距离）。
     * 上界卡在 {@code 1.49} 而不是 2.0，是为了让 Northstar 的**洞穴**群系
     * （depth 盒子 {@code [1.5, 2]}）在地下仍然严格胜出，不会让地表群系长到地下。
     */
    private static final Climate.Parameter SURFACE_DEPTH = span(-0.5F, 1.49F);

    /**
     * 高度分层 —— 依据是实测的 {@code (continentalness, erosion) -> 平均地表 Y} 表，
     * 而不是原版分带表（本维度虽然用的是原版 noise_router，但数值范围完全不同）：
     * <pre>
     *                      E≤-0.80  -0.80~-0.60  -0.60~-0.40  -0.40~-0.20  -0.20~0.00  0.00~0.20
     * C -0.38~-0.10           54        54           53           52           55          53
     * C -0.10~+0.17          103        93           79           75           70          68
     * C +0.17~+0.45            -       131          108           96           79          69
     * C +0.45~+0.73            -         -          109           93           85          68
     * </pre>
     * 结论：<b>高度同时需要 C 和 E</b>——{@code C <= -0.10} 时不管 E 多负都只有 5x（这是之前按
     * {@code E <= -0.375} 分带失效的原因：单看 E 的高/低两层平均高度只差 5 格）。
     * 取 {@code HIGH = (C >= -0.10 且 E <= -0.05)}：约 24% 面积、平均 Y≈82；
     * 其余为 LOW，约 76%、平均 Y≈60 —— 两层差 22 格，而 HIGH 层内部还能到 130+。
     */
    private static final Climate.Parameter HIGH_C = span(-0.10F, 1.3F);
    private static final Climate.Parameter HIGH_E = span(-1.0F, -0.05F);

    /**
     * weirdness 分带 —— <b>解决「附近只有一种群系」的关键</b>。
     *
     * <p>{@code continentalness}/{@code erosion} 的波长约 2048 格，按它们划分出来的群系是几千格
     * 一片：实测出生点 192×192 格内 DISTINCT=1（100% 赤色荒原），这正是用户看到的
     * 「其他群系都不生成」。{@code weirdness} 是 {@code minecraft:ridge} 噪声，波长约 512 格，
     * 实测分布是横跨 {@code [-1.21, 1.30]} 的钟形（p25=-0.23、p50=0.00、p75=0.25、p90=0.47）。</p>
     *
     * <p><b>带宽必须大致均衡。</b>第一版让赤色荒原占了 {@code W ∈ [-1.3, 0.28]}（宽 1.58），
     * 局部 W 的波动范围（约 1.5）整个落在这一条带里，走多远都是赤色荒原 ——
     * 实测 192 格内 DISTINCT 仍然只有 1。</p>
     */
    /** HIGH 层三带：火山高地 | martian_peaks | 极地冰冠。边界取实测直方图 p33/p67（-0.16 / +0.15）。 */
    private static final Climate.Parameter W_HIGH_VOLCANIC = span(-1.3F, -0.16F);
    private static final Climate.Parameter W_HIGH_PEAKS = span(-0.16F, 0.15F);
    private static final Climate.Parameter W_HIGH_POLAR = span(0.15F, 1.3F);

    /**
     * LOW 层的 weirdness 分带表（6 条，铺满 {@code [-1.3, 1.3]}）。
     *
     * <p><b>为什么要在这里塞「露头」</b>：用户反馈「火星上根本没发现高地形，所以这两种群系生成不了」——
     * 实测数据支持这个判断：火星地表高度分位是
     * {@code p50=63 p75=71 p90=88 p95=106 p99=128}，也就是 <b>Y&gt;88 只占 10%</b>，
     * 而 HIGH 层（{@code C>=-0.10 且 E<=-0.05}）只占约 23%、平均 Y 仅 82。
     * 把火山高地/极地冰冠锁死在那一层里，玩家在自家基地周围就永远遇不到。</p>
     *
     * <p>所以 LOW 层里给这两个群系各加了<b>一条窄带</b>（宽 0.13 / 0.14，空间上约 85~150 格宽），
     * 它们会像零散露头一样出现在全星球任何地方 —— 保证走几百格一定碰得到；
     * 而它们的**主要分布区**仍然留在 HIGH 层的高地形上（round 5 的「不能像平原」不受影响）。</p>
     */
    private record WBand(ResourceLocation id, float wMin, float wMax) {
    }

    private static final List<WBand> LOW_BANDS = List.of(
            new WBand(northstar("martian_dunes"), -1.30F, -0.62F),
            new WBand(MarsModule.id("mars_volcanic_highlands"), -0.62F, -0.46F),   // 火山露头
            new WBand(MarsModule.id("mars_rusty_wasteland"), -0.46F, 0.20F),
            new WBand(MarsModule.id("mars_polar_ice_cap"), 0.20F, 0.36F),         // 冰冠露头
            new WBand(northstar("martian_highlands"), 0.36F, 0.68F),
            new WBand(MarsModule.id("mars_rusty_wasteland"), 0.68F, 1.30F)
    );

    /** LOW 层由两段 C/E 组成：{@code C < -0.10} 的全部地形，以及 {@code C >= -0.10} 但侵蚀高的低地。 */
    private static final List<Climate.Parameter[]> LOW_TIER_REGIONS = List.of(
            new Climate.Parameter[]{span(-1.3F, -0.10F), E_FULL},
            new Climate.Parameter[]{HIGH_C, span(-0.05F, 1.0F)}
    );

    private static Climate.ParameterPoint surface(Climate.Parameter c, Climate.Parameter e, Climate.Parameter w) {
        return new Climate.ParameterPoint(T_FULL, H_FULL, c, e, SURFACE_DEPTH, w, 0L);
    }

    /**
     * <b>全部</b>地表盒子（本模组 3 个群系 + Northstar 3 个地表群系共用这一套分区）：
     * HIGH 层 3 块 + LOW 层 6 条带 × 2 段 = 15 块，两两互不重叠且恰好铺满整个气候空间 ——
     * 没有重叠就没有 RTree 平局，归属完全由盒子决定。
     */
    private static final List<BiomeEntry> ALL_SURFACE_BOXES = buildSurfaceBoxes();

    private static List<BiomeEntry> buildSurfaceBoxes() {
        List<BiomeEntry> boxes = new ArrayList<>();
        // HIGH 层：三种高地群系各占约 1/3
        boxes.add(new BiomeEntry(MarsModule.id("mars_volcanic_highlands"),
                surface(HIGH_C, HIGH_E, W_HIGH_VOLCANIC)));
        boxes.add(new BiomeEntry(northstar("martian_peaks"),
                surface(HIGH_C, HIGH_E, W_HIGH_PEAKS)));
        boxes.add(new BiomeEntry(MarsModule.id("mars_polar_ice_cap"),
                surface(HIGH_C, HIGH_E, W_HIGH_POLAR)));
        // LOW 层：6 条 W 带 × 2 段 C/E
        for (Climate.Parameter[] region : LOW_TIER_REGIONS) {
            for (WBand band : LOW_BANDS) {
                boxes.add(new BiomeEntry(band.id(),
                        surface(region[0], region[1], span(band.wMin(), band.wMax()))));
            }
        }
        return List.copyOf(boxes);
    }

    /** Northstar 的三个**地表**群系（它们的原条目要被替换成多块盒子；洞穴群系不动）。 */
    private static final Set<ResourceLocation> NORTHSTAR_SURFACE_BIOMES = Set.of(
            northstar("martian_dunes"), northstar("martian_highlands"), northstar("martian_peaks"));


    private static boolean injectionAttempted;
    private static boolean injectionFailed;

    /**
     * 给 Northstar 自己的三个**地表**群系重新分配气候盒子 —— 让它们在本模组共存时仍有概率生成。
     *
     * <h2>为什么必须动它们</h2>
     * <p>直接沿用原始盒子它们几乎永远选不中，三条原因（读源码 + 实测确认）：</p>
     * <ol>
     *     <li><b>humidity 在本维度恒为 0。</b>{@code noise_settings/mars.json} 里
     *         {@code "vegetation": 0}，而 {@code Climate.Sampler} 把 humidity 映射到
     *         {@code router.vegetation()}（实测 p0=p100=0.000）。dunes / highlands 的盒子
     *         {@code [-1, -0.35]} 于是永远背着 {@code 0.35² = 0.1225} 的距离。</li>
     *     <li><b>weirdness 盒子是原版分带表的切片</b>（{@code [-0.9333, -0.7666]}，约 0.17 宽），
     *         而本维度的 weirdness 是 {@code minecraft:ridge} 噪声、不驱动地形，卡这么窄等于自闭。</li>
     *     <li><b>depth 写着 {@code point(0)}</b>，但本维度地表 depth 是 {@code 0.21~1.38}。</li>
     * </ol>
     * <p>而且 {@code Climate.ParameterList.findValue} 走的是 <b>RTree</b>
     * （{@code Climate.RTree.SubTree.search} 用严格 {@code >} 比较），平局归属取决于树的内部顺序，
     * <b>不是「列表里靠前就赢」</b>，所以不能靠「Northstar 排在前面」碰运气 ——
     * 只能靠「互不重叠的盒子」把归属变成确定的。</p>
     *
     * <h2>怎么分配</h2>
     * <p>六个地表群系共用 {@link #ALL_SURFACE_BOXES} 一套分区：<b>先按 C/E 分 HIGH / LOW 两层</b>，
     * <b>再按 weirdness 切分</b>（波长约 512 格 → 走几百格就能换群系）。</p>
     * <ul>
     *     <li>HIGH 层（约 23%，平均 Y≈82）：火山高地 / {@code martian_peaks} / 极地冰冠 各约 1/3。</li>
     *     <li>LOW 层（约 77%）：{@link #LOW_BANDS} 的 6 条带 × 2 段 C/E；
     *         其中火山高地与极地冰冠各占<b>一条窄带（露头）</b>，保证在全星球任何地方走几百格都能碰到。</li>
     * </ul>
     * <p>洞穴群系（depth {@code [1.5, 2]}）<b>完全不动</b>。份额与高度实测见 README §5.6。</p>
     */
    private static ResourceLocation northstar(String path) {
        return ResourceLocation.fromNamespaceAndPath("northstar", path);
    }

    /** 只有服务端需要注入：客户端没有 ChunkGenerator，它的群系来自服务端同步的区块数据。 */
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!MARS.equals(level.dimension().location())) {
            return;
        }
        inject(level);
    }

    private static void inject(ServerLevel level) {
        if (injectionAttempted) {
            return;
        }
        injectionAttempted = true;

        if (Boolean.getBoolean(DISABLE_PROPERTY)) {
            MarsModule.LOGGER.warn("Mars biome injection disabled via -D{}=true", DISABLE_PROPERTY);
            return;
        }

        ChunkGenerator generator = level.getChunkSource().getGenerator();
        BiomeSource source = generator.getBiomeSource();
        if (!(source instanceof MultiNoiseBiomeSource multiNoise)) {
            failOnce("northstar:mars no longer uses minecraft:multi_noise; Mars biomes were not injected");
            return;
        }

        Field parametersField = null;
        Either<Climate.ParameterList<Holder<Biome>>, Holder<MultiNoiseBiomeSourceParameterList>> original = null;
        Collection<Holder<Biome>> originalPossible = null;
        boolean possibleBiomesPatched = false;

        try {
            originalPossible = source.possibleBiomes();
            boolean isMars = originalPossible.stream().anyMatch(holder -> holder.unwrapKey()
                    .map(key -> key.location().equals(MARKER_BIOME))
                    .orElse(false));
            if (!isMars) {
                return;
            }

            parametersField = MultiNoiseBiomeSource.class.getDeclaredField("parameters");
            parametersField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Either<Climate.ParameterList<Holder<Biome>>, Holder<MultiNoiseBiomeSourceParameterList>> current =
                    (Either<Climate.ParameterList<Holder<Biome>>, Holder<MultiNoiseBiomeSourceParameterList>>)
                            parametersField.get(multiNoise);
            original = current;

            Climate.ParameterList<Holder<Biome>> list = current.left().orElse(null);
            if (list == null) {
                failOnce("northstar:mars uses a biome preset instead of an inline list; Mars biomes were not injected");
                return;
            }

            Registry<Biome> biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);
            List<Pair<Climate.ParameterPoint, Holder<Biome>>> points =
                    new ArrayList<>(list.values().size() + ALL_SURFACE_BOXES.size());
            int reboxed = 0;

            // 统一分区：把 ALL_SURFACE_BOXES 按群系分组（一个群系会有多块盒子）
            Map<ResourceLocation, List<Climate.ParameterPoint>> boxesByBiome = new LinkedHashMap<>();
            for (BiomeEntry box : ALL_SURFACE_BOXES) {
                boxesByBiome.computeIfAbsent(box.id(), key -> new ArrayList<>()).add(box.point());
            }

            // 1) Northstar 的三个地表群系：原条目换成它们的新盒子（「一条换多条」），洞穴群系原样保留
            for (Pair<Climate.ParameterPoint, Holder<Biome>> pair : list.values()) {
                ResourceLocation id = pair.getSecond().unwrapKey().map(ResourceKey::location).orElse(null);
                List<Climate.ParameterPoint> boxes =
                        id != null && NORTHSTAR_SURFACE_BIOMES.contains(id) ? boxesByBiome.get(id) : null;
                if (boxes == null) {
                    points.add(pair);
                } else {
                    reboxed++;
                    for (Climate.ParameterPoint box : boxes) {
                        points.add(Pair.of(box, pair.getSecond()));
                    }
                }
            }

            // 2) 本模组的群系：同一群系的多块盒子共用同一个 holder
            Map<ResourceLocation, Holder<Biome>> ownHolders = new LinkedHashMap<>();
            List<Holder<Biome>> added = new ArrayList<>();
            for (BiomeEntry box : ALL_SURFACE_BOXES) {
                if (!MarsModule.NAMESPACE.equals(box.id().getNamespace())) {
                    continue;
                }
                Holder<Biome> holder = ownHolders.computeIfAbsent(box.id(), id -> {
                    var found = biomeRegistry.getHolder(ResourceKey.create(Registries.BIOME, id));
                    if (found.isEmpty()) {
                        MarsModule.LOGGER.warn("Biome {} not found in the registry; skipped", id);
                    }
                    return found.orElse(null);
                });
                if (holder == null) {
                    continue;
                }
                if (!added.contains(holder)) {
                    added.add(holder);
                }
                points.add(Pair.of(box.point(), holder));
            }
            if (added.isEmpty()) {
                return;
            }

            // 1) 先换 possibleBiomes 缓存（失败就直接抛出，parameters 还没动 → 不会半注入）
            Set<Holder<Biome>> union = new LinkedHashSet<>(originalPossible);
            union.addAll(added);
            Field cacheField = BiomeSource.class.getDeclaredField("possibleBiomes");
            cacheField.setAccessible(true);
            cacheField.set(multiNoise, Suppliers.ofInstance(ImmutableSet.copyOf(union)));
            possibleBiomesPatched = true;

            // 2) 再换参数表
            parametersField.set(multiNoise, Either.left(new Climate.ParameterList<>(points)));

            // 3) 自检：新群系必须真的出现在 possibleBiomes 里
            if (!source.possibleBiomes().containsAll(added)) {
                rollback(parametersField, multiNoise, original, cacheField, originalPossible, generator);
                failOnce("possibleBiomes cache did not take effect; biome injection rolled back");
                return;
            }

            // 4) 让特征索引重建，并立刻验证一次（把问题留在加载期，而不是留到区块生成时）
            generator.refreshFeaturesPerStep();
            generator.validate();

            MarsModule.LOGGER.info(
                    "Injected {} Mars biomes into northstar:mars (re-boxed {} Northstar surface biomes)",
                    added.size(), reboxed);
        } catch (Throwable throwable) {
            if (parametersField != null && original != null) {
                try {
                    Field cacheField = BiomeSource.class.getDeclaredField("possibleBiomes");
                    cacheField.setAccessible(true);
                    rollback(parametersField, multiNoise, original, cacheField, originalPossible, generator);
                } catch (Throwable rollbackFailure) {
                    MarsModule.LOGGER.error("Failed to roll back Mars biome injection", rollbackFailure);
                }
            }
            failOnce("Failed to inject Mars biomes: " + throwable);
        }
    }

    /** 回滚到「只有 Northstar 群系」的干净状态，并重建特征索引。 */
    private static void rollback(Field parametersField,
                                 MultiNoiseBiomeSource multiNoise,
                                 Either<Climate.ParameterList<Holder<Biome>>, Holder<MultiNoiseBiomeSourceParameterList>> original,
                                 Field cacheField,
                                 Collection<Holder<Biome>> originalPossible,
                                 ChunkGenerator generator) throws Exception {
        parametersField.set(multiNoise, original);
        cacheField.set(multiNoise, Suppliers.ofInstance(ImmutableSet.copyOf(originalPossible)));
        generator.refreshFeaturesPerStep();
        generator.validate();
    }

    private static void failOnce(String message) {
        if (!injectionFailed) {
            injectionFailed = true;
            MarsModule.LOGGER.error("{} (Mars keeps Northstar's original biomes)", message);
        }
    }

    private static Climate.Parameter span(float min, float max) {
        return Climate.Parameter.span(min, max);
    }

    /** 一条「群系 → 气候盒子」记录。同一个群系可以出现多条（补集/多块）。 */
    private record BiomeEntry(ResourceLocation id, Climate.ParameterPoint point) {
    }

    private MarsBiomeInjector() {
    }
}
