package com.suwako.northstarplanetaddon.mars.world;

import com.mojang.serialization.Codec;
import com.suwako.northstarplanetaddon.mars.MarsModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * 陨石坑特征 —— 火山高地 / 极地冰冠的**新地貌**，取代 v1.8 的抬升台地特征
 * （用户要求「火山和冰冠全部推翻，改成陨石坑」，原抬升台地的特征类与 JSON 已全部删除）。
 *
 * <p>每个坑的形状：中心往下挖一个碗（{@code depth} 格），坑缘略微抬高（{@code rim_height} 格），
 * 坑底中心铺冰（{@code ice_block}，火星冰 / 干冰），冰核外面一圈是熔岩或岩浆块
 * （{@code lava_block}），坑底其余部分与坑缘是岩石（{@code fill_block}）。</p>
 *
 * <h2>三条必须记住的坑（改动前先读）</h2>
 * <ol>
 *     <li><b>世界生成期间 {@code getHeight(WORLD_SURFACE_WG, x, z)} 是「第一个空气方块」的 Y</b>
 *         （{@code WorldGenRegion#getHeight} = {@code chunk.getHeight(...) + 1}），
 *         所以最高的实心方块是 {@code surface - 1}，<b>不要再 ±1</b>。
 *         详见 {@link SpireFeature#firstAirY} 与 {@link MarsSurfaceLayerFeature} 的类注释。</li>
 *     <li><b>{@code placed_feature} 必须有 {@code minecraft:heightmap} 修饰符</b>：
 *         {@code ChunkGenerator.applyBiomeDecoration} 给特征链的初始坐标 Y = 世界最低点，
 *         末尾的 {@code minecraft:biome} 过滤器会拿那个 Y 查群系（命中 Northstar 的洞穴群系）
 *         → 特征一个区块都不会跑。详见 {@link MarsSurfaceLayerFeature} 的类注释。</li>
 *     <li><b>跨区块写只允许 1 格</b>：装饰步骤跑在 {@code FEATURES} chunk status 里，
 *         它的 {@code blockStateWriteRadius()} = <b>1</b> —— {@code WorldGenRegion.ensureCanWrite}
 *         只放行 {@code |Δchunk| ≤ 1}，再远就记 {@code Detected setBlock in a far chunk}
 *         并且<b>拒绝写入</b>（Northstar 自己的 {@code mars_crater} 半径 9~18 就是撞在这上面）。
 *         本特征半径上限 8 &lt; 16，整个圆盘必然落在本区块 ±1 之内 ——
 *         所以默认写出**完整**的坑，开关见 {@link #ALLOW_NEIGHBOUR_CHUNKS}。</li>
 * </ol>
 *
 * <h2>性能</h2>
 * 单 tick 写入量必须远低于 Watchdog 的容忍度（这个坑踩过：区块生成超时会被直接杀进程）。
 * 因此：(a) {@link MarsCraterConfig} 的半径上限写死在 Codec 里（≤ {@value #MAX_RADIUS}）；
 * (b) {@link #MAX_COLUMNS_PER_PLACEMENT} 裁剪每区块处理列数；
 * (c) {@link #MAX_WRITES_PER_COLUMN} 裁剪每列写入方块数；
 * (d) {@link #MAX_TOTAL_WRITES} 是单次放置的总预算，超了就地收手。
 * 全部写入都用 {@code level.setBlock(pos, state, 2)}，且绝不碰 {@code y < minBuildHeight + 1}。
 */
public class MarsCraterFeature extends Feature<MarsCraterConfig> {

    /** 半径硬上限：与 {@link MarsCraterConfig} 的 Codec 一致。 */
    private static final int MAX_RADIUS = 16;

    /** 每区块（每次放置）最多处理的列数 = (2 × 8 + 1)²，半径 8 的整圆也只有 197 列。 */
    private static final int MAX_COLUMNS_PER_PLACEMENT = (2 * MAX_RADIUS + 1) * (2 * MAX_RADIUS + 1);

    /** 单列最多写入的方块数（挖 + 填 + 冰/熔岩）。 */
    private static final int MAX_WRITES_PER_COLUMN = 64;

    /** 单次放置的总写入预算（默认参数估算约 1300 次写入，这里留数倍余量，防 Watchdog 超时）。 */
    private static final int MAX_TOTAL_WRITES = 24000;

    /** 坑底岩石的厚度（在坑底实心面往下铺，让坑底有统一的材质观感）。 */
    private static final int FLOOR_MATERIAL_DEPTH = 3;

    /** 冰核厚度的上下限：{@code max(3, 6 - d)}，再夹到「不会顶穿原地表」的范围内。 */
    private static final int MIN_ICE_THICKNESS = 3;
    private static final int MAX_ICE_THICKNESS = 6;

    /**
     * 是否允许把坑写到相邻区块（<b>默认 true：坑是完整的</b>）。
     *
     * <p>依据（读的是 1.21.1 反编译源码，不是猜的）：</p>
     * <ul>
     *     <li>{@code WorldGenRegion.ensureCanWrite}：
     *         {@code |Δchunk| ≤ generatingStep.blockStateWriteRadius()} 才允许写入，
     *         否则 {@code Util.logAndPauseIfInIde("Detected setBlock in a far chunk ...")} 并
     *         {@code return false}（写入被拒绝，不抛异常）。</li>
     *     <li>装饰步骤所在的 {@code FEATURES} chunk status 的 {@code blockStateWriteRadius()} = 1
     *         （{@code ChunkPyramid}：FEATURES → 1，NOISE/SURFACE/CARVERS → 0）。</li>
     *     <li>{@link MarsCraterConfig} 的半径上限 {@value #MAX_RADIUS} &lt; 16，
     *         所以 {@code origin ± radius} 最多只越界到相邻区块，永远不会触发 far chunk 拒绝。</li>
     * </ul>
     *
     * <p>设为 {@code false} 时只写当前区块（v1.8 尖柱 / 表层特征的做法）。
     * 代价很大：坑心随机落在 16×16 内，只有 ((16-2×7)/16)² ≈ <b>1.6%</b> 的坑完全落在区块内，
     * 其余 ~98% 会被区块边界切掉一半（连带切掉一半熔岩环与冰核），
     * 所以默认开启跨区块写。</p>
     */
    private static final boolean ALLOW_NEIGHBOUR_CHUNKS = true;

    /** 允许写入的邻居区块数（= FEATURES 步骤的 {@code blockStateWriteRadius}）。 */
    private static final int MAX_CHUNK_DELTA = 1;

    /** 坑缘从半径的这个比例处开始抬升（0.75 → 外圈 25% 是坑缘）。 */
    private static final float RIM_INNER_RATIO = 0.75F;

    /** 熔岩环的外边界比例（再往外到坑缘是普通岩石）。 */
    private static final float LAVA_RING_OUTER_RATIO = 0.9F;

    public MarsCraterFeature(Codec<MarsCraterConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<MarsCraterConfig> context) {
        try {
            return doPlace(context);
        } catch (Throwable throwable) {
            // 纯地形装饰：异常只跳过这一次，绝不能让区块生成失败（否则会留下虚空区块）
            MarsModule.LOGGER.error(
                    "Mars crater failed at {}; skipping this chunk", context.origin(), throwable);
            return false;
        }
    }

    private boolean doPlace(FeaturePlaceContext<MarsCraterConfig> context) {
        WorldGenLevel level = context.level();
        MarsCraterConfig config = context.config();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        ChunkPos chunk = new ChunkPos(origin);

        int radius = Mth.clamp(config.radius(), 1, MAX_RADIUS);
        int depth = Math.max(1, config.depth());
        int rimHeight = Math.max(0, config.rimHeight());
        int coreRadius = Mth.clamp(config.coreRadius(), 0, radius);
        if (config.fillBlock() == null || config.fillBlock().isAir()) {
            return false;
        }

        Shaper shaper = new Shaper(level, random, config, radius, depth, rimHeight, coreRadius);

        // 坑心 = context.origin()，对每列算水平距离，逐列整形。
        // 列坐标不再夹回区块：半径 ≤ 8 时整个圆盘都在本区块 ±1 之内，而 FEATURES 步骤的
        // blockStateWriteRadius = 1，所以写邻区块是合法的、不会刷 far chunk
        // （详见 ALLOW_NEIGHBOUR_CHUNKS 的注释）。把该开关关掉就退回「只写当前区块」。
        int columns = 0;
        int totalWrites = 0;
        boolean placed = false;

        candidateLoop:
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double d = Math.sqrt((double) (dx * dx + dz * dz));
                if (d > radius) {
                    continue;
                }
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                if (!canWrite(x, z, chunk)) {
                    continue;
                }
                if (columns >= MAX_COLUMNS_PER_PLACEMENT || totalWrites >= MAX_TOTAL_WRITES) {
                    break candidateLoop;
                }
                columns++;
                int written = shaper.shapeColumn(x, z, d);
                totalWrites += written;
                placed |= written > 0;
            }
        }
        return placed;
    }

    /** 这一列能不能写（见 {@link #ALLOW_NEIGHBOUR_CHUNKS}）。半径 ≤ 8 时恒为 true。 */
    private static boolean canWrite(int x, int z, ChunkPos chunk) {
        if (ALLOW_NEIGHBOUR_CHUNKS) {
            int chunkX = SectionPos.blockToSectionCoord(x);
            int chunkZ = SectionPos.blockToSectionCoord(z);
            return Math.abs(chunkX - chunk.x) <= MAX_CHUNK_DELTA
                    && Math.abs(chunkZ - chunk.z) <= MAX_CHUNK_DELTA;
        }
        return x >= chunk.getMinBlockX() && x <= chunk.getMaxBlockX()
                && z >= chunk.getMinBlockZ() && z <= chunk.getMaxBlockZ();
    }

    /**
     * 单次放置的形状计算器：持有一份不可变的参数和一块可复用的游标，逐列整形。
     *
     * <p>剖面（{@code d} = 该列到坑心的水平距离，{@code ratio = d / radius}）：</p>
     * <ul>
     *     <li>下挖 {@code dig = round(depth × (1 - ratio²))}，中心最深（{@code = depth}）、半径处为 0；</li>
     *     <li>坑缘 {@code rim = round(rim_height × (ratio - 0.75) / 0.25)}，仅在 {@code ratio > 0.75} 时非零，
     *         半径处恰好等于 {@code rim_height}；</li>
     *     <li>整形后该列的第一个空气方块 {@code airY = surface - dig + rim}：
     *         {@code airY < surface} 时把 {@code [airY, surface - 1]} 清成空气（下挖），
     *         {@code airY > surface} 时把 {@code [surface, airY - 1]} 填成岩石（坑缘）；</li>
     *     <li>坑底实心面 {@code base = airY - 1}：
     *         {@code d <= core_radius} 铺冰核（从 {@code base + 1} 起向上 {@code max(3, 6 - d)} 格，上面留空），
     *         {@code core_radius < d <= radius × 0.9} 铺熔岩环（{@code base} 往下 1~2 格熔岩），
     *         其余（{@code radius × 0.9 < d <= radius}）只铺岩石。</li>
     * </ul>
     */
    private static final class Shaper {

        private final WorldGenLevel level;
        private final RandomSource random;
        private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        private final int radius;
        private final int depth;
        private final int rimHeight;
        private final int coreRadius;
        private final int minY;
        private final int maxY;
        private final BlockState lava;
        private final BlockState ice;
        private final BlockState fill;

        Shaper(WorldGenLevel level, RandomSource random, MarsCraterConfig config,
               int radius, int depth, int rimHeight, int coreRadius) {
            this.level = level;
            this.random = random;
            this.radius = radius;
            this.depth = depth;
            this.rimHeight = rimHeight;
            this.coreRadius = coreRadius;
            // 绝不写 y < minBuildHeight + 1 的方块
            this.minY = level.getMinBuildHeight() + 1;
            this.maxY = level.getMaxBuildHeight() - 1;
            this.lava = config.lavaBlock();
            this.ice = config.iceBlock();
            this.fill = config.fillBlock();
        }

        /** 整形一列，返回真正写入的方块数。 */
        int shapeColumn(int x, int z, double d) {
            int writes = 0;

            // ★ 世界生成期间 WorldGenRegion#getHeight = chunk.getHeight(...) + 1
            //   = **第一个空气方块**的 Y；最高的实心方块是 surface - 1。不要再 ±1。
            int surface = this.level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            if (surface <= this.minY + 1 || surface > this.maxY + 1) {
                // 地形已经贴到建筑高度上下限：不动它
                return 0;
            }

            double ratio = d / (double) this.radius;
            int dig = (int) Math.round(this.depth * (1.0D - ratio * ratio));
            if (dig < 0) {
                dig = 0;
            }
            int rim = 0;
            if (ratio > RIM_INNER_RATIO) {
                rim = (int) Math.round(this.rimHeight * (ratio - RIM_INNER_RATIO) / (1.0D - RIM_INNER_RATIO));
                if (rim < 0) {
                    rim = 0;
                }
            }
            // 整形后该列「第一个空气方块」的 Y
            int airY = surface - dig + rim;

            // 1) 下挖：把 [airY, surface - 1] 清成空气（原地表的实心方块都在这个区间里）
            for (int y = airY; y < surface && writes < MAX_WRITES_PER_COLUMN; y++) {
                if (y < this.minY || y > this.maxY) {
                    continue;
                }
                this.cursor.set(x, y, z);
                if (this.level.getBlockState(this.cursor).isAir()) {
                    continue;
                }
                this.level.setBlock(this.cursor, Blocks.AIR.defaultBlockState(), 2);
                writes++;
            }

            // 2) 抬坑缘：整形后的坑底高于原地表时向上填（坑缘土脊）
            for (int y = surface; y < airY && writes < MAX_WRITES_PER_COLUMN; y++) {
                if (y < this.minY || y > this.maxY) {
                    continue;
                }
                writes += this.place(x, y, z, this.fill);
            }

            int base = airY - 1;   // 整形后最上面那块实心方块的 Y
            if (base < this.minY || base > this.maxY) {
                return writes;
            }

            if (d <= this.coreRadius && dig - rim >= 1) {
                // 3a) 冰核：坑底垫岩石，冰从坑底往上铺 max(3, 6 - d) 格，上面留空。
                //     厚度再夹一次，保证冰不会顶穿原地表（可用空间 = dig - rim）。
                int thickness = Math.min(Math.max(MIN_ICE_THICKNESS, MAX_ICE_THICKNESS - (int) d),
                        Math.min(MAX_ICE_THICKNESS, dig - rim));
                writes += this.run(x, z, base, -1, FLOOR_MATERIAL_DEPTH, this.fill, writes);
                writes += this.run(x, z, base + 1, 1, Math.max(1, thickness), this.ice, writes);
            } else if (d <= this.radius * LAVA_RING_OUTER_RATIO) {
                // 3b) 熔岩环：凹地底部 1~2 格熔岩，下面垫坑底岩石（熔岩顶面与坑底齐平）
                int lavaThickness = 1 + this.random.nextInt(2);
                writes += this.run(x, z, base, -1, lavaThickness, this.lava, writes);
                writes += this.run(x, z, base - lavaThickness, -1, FLOOR_MATERIAL_DEPTH, this.fill, writes);
            } else {
                // 3c) 坑缘内侧到坑缘：只有岩石
                writes += this.run(x, z, base, -1, FLOOR_MATERIAL_DEPTH, this.fill, writes);
            }
            return writes;
        }

        /**
         * 从 {@code (x, startY, z)} 起沿 {@code step} 方向连续放 {@code count} 个 {@code state}。
         * 越界的 Y 跳过；已经是该方块的格不重复写（省写入量）。
         *
         * @param budgetUsed 本列已经用掉的写入预算
         */
        private int run(int x, int z, int startY, int step, int count, BlockState state, int budgetUsed) {
            int writes = 0;
            for (int i = 0; i < count && budgetUsed + writes < MAX_WRITES_PER_COLUMN; i++) {
                int y = startY + i * step;
                if (y < this.minY || y > this.maxY) {
                    continue;
                }
                writes += this.place(x, y, z, state);
            }
            return writes;
        }

        private int place(int x, int y, int z, BlockState state) {
            this.cursor.set(x, y, z);
            if (this.level.getBlockState(this.cursor) == state) {
                return 0;
            }
            this.level.setBlock(this.cursor, state, 2);
            return 1;
        }
    }
}
