package com.suwako.northstarplanetaddon.mars.world;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * 尖柱特征：从地表往上长柱子（火山岩柱 / 冰塔）。
 *
 * <p><b>为什么需要它</b>：火星维度的地形高度由 Northstar 的 {@code northstar:mars} 噪声设置决定，
 * 那是**全局**的、不区分群系；实测表明仅靠群系的气候参数（continentalness / erosion / weirdness）
 * 无法可靠地把某个群系固定在“高地”上（同一份配置换种子后平均高度在 48~79 之间跳）。
 * 所以「火山高地 / 极地冰冠不该像平原」这件事，用**垂直地形特征**来保证 —— 这是可控且可验证的部分。</p>
 *
 * <p>实现要点：只在空气处放置（不破坏已生成的地表与矿脉）；所有写入都夹在当前区块内，
 * 避免 {@code WorldGenRegion} 的越界写入；异常只跳过本次放置，绝不影响区块生成。</p>
 */
public class SpireFeature extends Feature<SpireConfig> {

    public SpireFeature(Codec<SpireConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<SpireConfig> context) {
        try {
            return doPlace(context);
        } catch (Throwable throwable) {
            com.suwako.northstarplanetaddon.mars.MarsModule.LOGGER.error(
                    "Spire feature failed at {}; skipping", context.origin(), throwable);
            return false;
        }
    }

    private boolean doPlace(FeaturePlaceContext<SpireConfig> context) {
        WorldGenLevel level = context.level();
        SpireConfig config = context.config();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        int height = Mth.nextInt(random, config.minHeight(), config.maxHeight());
        int radius = config.baseRadius();
        int maxY = level.getMaxBuildHeight() - 1;
        ChunkPos chunk = new ChunkPos(origin);
        BlockState state = config.state();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        int surface = firstAirY(level, origin.getX(), origin.getZ());
        boolean placed = false;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                // 十字形基座：细柱 + 四个方向各一格的矮基座
                boolean centre = dx == 0 && dz == 0;
                if (!centre && Math.abs(dx) + Math.abs(dz) > radius) {
                    continue;
                }
                int x = Mth.clamp(origin.getX() + dx, chunk.getMinBlockX(), chunk.getMaxBlockX());
                int z = Mth.clamp(origin.getZ() + dz, chunk.getMinBlockZ(), chunk.getMaxBlockZ());
                int thisHeight = centre ? height : Math.max(1, height / 3);
                int columnTop = centre ? surface : firstAirY(level, x, z);

                for (int y = 0; y < thisHeight; y++) {
                    int worldY = columnTop + y;
                    if (worldY > maxY) {
                        break;
                    }
                    cursor.set(x, worldY, z);
                    // 只往空气里长，不覆盖地表/矿脉/建筑
                    if (!level.getBlockState(cursor).isAir()) {
                        break;
                    }
                    level.setBlock(cursor, state, 2);
                    placed = true;
                }
            }
        }
        return placed;
    }

    /**
     * 该列**第一个空气方块**的 Y —— 柱子从这里开始长。
     *
     * <p>世界生成期间直接用 {@code level.getHeight(WORLD_SURFACE_WG, x, z)} 就是「第一个空气方块」
     * 的 Y：{@code WorldGenRegion#getHeight} 是 {@code chunk.getHeight(...) + 1}
     * （而 {@code Level.getHeight} 运行时走的是不 +1 的那条路）。
     * <b>不要在这里再 +1</b>，否则柱子会浮在地表上方一格。</p>
     */
    static int firstAirY(WorldGenLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
    }
}
