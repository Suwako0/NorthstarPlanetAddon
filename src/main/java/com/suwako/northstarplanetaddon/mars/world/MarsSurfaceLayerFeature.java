package com.suwako.northstarplanetaddon.mars.world;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * 本模组火星群系的表层特征 —— 让新群系也有「火星地表」。
 *
 * <p><b>为什么需要它</b>：Northstar 的 {@code northstar:mars} 噪声设置用
 * {@code minecraft:biome} + {@code biome_is: [...]} 按群系决定表层（沙/土/砾石）。
 * 本模组新增的三个群系不在名单里 → 表层规则不匹配 → 地表停在噪声默认的
 * {@code northstar:mars_stone}，整片维度看起来空空荡荡。</p>
 *
 * <p><b>做什么</b>：遍历当前区块的全部 16×16 列，从地表往下，只要是
 * {@code northstar:mars_stone} / {@code mars_deep_stone} 就逐层替换为配置里的
 * 顶层 / 次表层方块，最多 1 + {@code fillerDepth} 层。</p>
 *
 * <h2>两个必须记住的坑（都踩过，改动前先读）</h2>
 * <ol>
 *     <li><b>对应的 {@code placed_feature} 必须带 {@code minecraft:heightmap} 修饰符。</b>
 *         {@code ChunkGenerator.applyBiomeDecoration} 传给特征链的初始坐标是
 *         {@code SectionPos.origin()}，<b>Y = 世界最低点</b>。末尾的 {@code minecraft:biome}
 *         过滤器会拿这个 Y 去查群系 —— 那里 {@code overworld/depth ≈ 1.5}，命中的是
 *         Northstar 的洞穴群系（它的 depth 盒子是 {@code [1.5, 2]}），于是
 *         {@code hasFeature} 恒为 false，特征<b>一个区块都不会跑</b>。
 *         加上 {@code heightmap: WORLD_SURFACE_WG} 把起点抬到地表，过滤器才看得到地表群系。</li>
 *     <li><b>世界生成期间 {@code getHeight()} 的语义和运行时不同。</b>
 *         {@code Level.getHeight()} 是「最高非空气方块自身的 Y」，
 *         但 {@code WorldGenRegion.getHeight()} 是<b>那个 Y 再 +1</b>，即「第一个空气方块」。
 *         特征跑在 {@code WorldGenRegion} 里，所以表层方块是 {@code surface - 1}。</li>
 * </ol>
 *
 * <p>放在 {@code TOP_LAYER_MODIFICATION}（最后一个装饰步骤，原版
 * {@code freeze_top_layer} 也在这一步）：更早放的话，
 * {@code big_dumb_rock} / {@code mars_rock} / {@code mars_dunes_rock} /
 * {@code mars_blob_mars_soil} / {@code mars_worm_nest} 都会把地表重新盖成
 * {@code mars_stone}。</p>
 *
 * <p>Northstar 缺失时（{@code mars_stone} 查不到）直接返回 false，什么都不做。</p>
 */
public class MarsSurfaceLayerFeature extends Feature<MarsSurfaceConfig> {

    public MarsSurfaceLayerFeature(Codec<MarsSurfaceConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<MarsSurfaceConfig> context) {
        try {
            return doPlace(context);
        } catch (Throwable throwable) {
            // 这是纯装饰性特征：任何异常都只跳过一次，绝不能让区块生成失败（否则会留下虚空区块）
            com.suwako.northstarplanetaddon.mars.MarsModule.LOGGER.error(
                    "Mars surface layer failed at {}; skipping this chunk", context.origin(), throwable);
            return false;
        }
    }

    private boolean doPlace(FeaturePlaceContext<MarsSurfaceConfig> context) {
        WorldGenLevel level = context.level();
        MarsSurfaceConfig config = context.config();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        ChunkPos chunkPos = new ChunkPos(origin);

        Block stone = NorthstarContent.marsStone();
        Block deepStone = NorthstarContent.marsDeepStone();
        if (stone == Blocks.AIR) {
            // 没有 Northstar：不动任何方块
            return false;
        }

        BlockState top = config.top();
        BlockState filler = config.filler();
        BlockState accent = config.accent();
        int maxDepth = config.fillerDepth();
        float accentChance = config.accentChance();

        // 这个特征只在自己的群系里生效（placed_feature 末尾的 minecraft:biome 过滤器）。
        // 但 applyBiomeDecoration 会把「3x3 区块范围内出现过的群系」的特征全都放一遍，
        // 所以一个区块里可能混着两种群系 —— 逐列比对起点群系，避免把沙铺到隔壁冰盖上。
        ResourceKey<Biome> originBiome = level.getBiome(origin).unwrapKey().orElse(null);

        boolean placed = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int minY = level.getMinBuildHeight();

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = chunkPos.getMinBlockX() + dx;
                int z = chunkPos.getMinBlockZ() + dz;
                int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                if (originBiome != null
                        && !originBiome.equals(level.getBiome(cursor.set(x, surface - 1, z)).unwrapKey().orElse(null))) {
                    continue;
                }

                // ★ 世界生成期间的 getHeight() 语义和运行时**不一样**，别搞错：
                //   - Level.getHeight()          → 最高非空气方块自身的 Y（ChunkAccess.getHeight 内部 -1）
                //   - WorldGenRegion.getHeight() → 该 Y + 1，也就是「第一个空气方块」的 Y
                //     （见 WorldGenRegion#getHeight，它就是 +1）
                // 特征跑在 WorldGenRegion 里，所以这里得到的是**地表上方那一格**，
                // 表层方块是 surface - 1。
                for (int depth = 0, y = surface - 1; y > minY && depth <= maxDepth; depth++, y--) {
                    cursor.set(x, y, z);
                    BlockState current = level.getBlockState(cursor);
                    Block currentBlock = current.getBlock();
                    if (currentBlock != stone && currentBlock != deepStone) {
                        // 已经是别的东西（岩石/矿脉/水冰等）：不要覆盖
                        break;
                    }
                    BlockState target = depth == 0
                            ? (accentChance > 0.0F && random.nextFloat() < accentChance ? accent : top)
                            : filler;
                    level.setBlock(cursor, target, 2);
                    placed = true;
                }
            }
        }
        return placed;
    }
}
