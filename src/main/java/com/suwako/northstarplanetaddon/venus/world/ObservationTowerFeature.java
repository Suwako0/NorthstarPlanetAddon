package com.suwako.northstarplanetaddon.venus.world;

import com.suwako.northstarplanetaddon.venus.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 高压观测塔（设计草案 10 章 #3：20×60×20，Boss 2 场地）。
 *
 * <p>当前版本：5×5 塔身 30 格高、四角支柱、顶层平台 + 硫晶信标，
 * 塔底 4 个战利品箱（含气压计）。赫菲斯托斯的剧情召唤点留给「压力阀」道具，
 * 玩家在塔底用压力阀即可开战。</p>
 */
public class ObservationTowerFeature extends Feature<NoneFeatureConfiguration> {

    private static final int HEIGHT = 30;

    public ObservationTowerFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        var metal = RuinPalette.TRIM;
        var stone = RuinPalette.CASE;
        var crystal = RuinPalette.CRYSTAL;

        RuinBuilder.platform(level, origin, 5, 3, stone);
        RuinBuilder.tower(level, origin, 3, HEIGHT, metal, crystal);

        // 顶层平台
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                level.setBlock(origin.offset(dx, HEIGHT + 1, dz), stone, 2);
            }
        }
        level.setBlock(origin.offset(0, HEIGHT + 2, 0), crystal, 2);

        // 塔底箱子（含气压计）
        RuinBuilder.chest(level, origin.offset(1, 1, 1), "chests/observation_tower", random.nextLong(),
                Direction.SOUTH);
        RuinBuilder.chest(level, origin.offset(-1, 1, 1), "chests/observation_tower", random.nextLong(),
                Direction.SOUTH);
        RuinBuilder.chest(level, origin.offset(1, 1, -1), "chests/observation_tower_calibrator", random.nextLong(),
                Direction.NORTH);
        RuinBuilder.chest(level, origin.offset(-1, 1, -1), "chests/observation_tower", random.nextLong(),
                Direction.NORTH);
        RuinPalette.truss(level, origin, 3, HEIGHT);
        RuinPalette.detail(level, origin, 5, 4);
        return true;
    }
}
