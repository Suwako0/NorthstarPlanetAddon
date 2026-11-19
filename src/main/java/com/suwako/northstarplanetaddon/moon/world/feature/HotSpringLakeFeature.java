package com.suwako.northstarplanetaddon.moon.world.feature;

import com.mojang.serialization.Codec;
import com.suwako.northstarplanetaddon.moon.registry.NMABlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * 温泉湖 — vanilla's {@code minecraft:lake} with a floor that is actually mixed.
 * <p>
 * {@code LakeFeature} asks its barrier provider for one state and then lines the entire pool with it,
 * so the weighted / noise mix configured in JSON only ever shows up as one uniform lining — a pool is
 * either all 月石 or all 月石灯, never "mostly 月石 and 月沙 with a few 月石灯". This subclass keeps the
 * carve and the water exactly as vanilla does it and then re-rolls the provider for every floor block
 * the water sits on, so the mix defined in {@code hot_spring_lake.json} finally applies per block.
 */
public class HotSpringLakeFeature extends LakeFeature {

    /** The lake's own bounding box, taken straight from {@code LakeFeature#place}. */
    private static final int SPAN_XZ = 16;
    private static final int SPAN_Y = 8;
    private static final int FLUID_DEPTH = 4;

    public HotSpringLakeFeature(Codec<LakeFeature.Configuration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<LakeFeature.Configuration> context) {
        if (!super.place(context)) {
            return false;
        }

        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockStateProvider floor = context.config().barrier();
        BlockPos base = context.origin().below(FLUID_DEPTH);

        for (int x = 0; x < SPAN_XZ; x++) {
            for (int z = 0; z < SPAN_XZ; z++) {
                for (int y = 0; y < SPAN_Y; y++) {
                    BlockPos water = base.offset(x, y, z);
                    if (level.getBlockState(water).getBlock() != NMABlocks.HOT_SPRING.get()) {
                        continue;
                    }

                    // Only the cell directly under the lowest water block of a column is the floor the
                    // player sees; every other water cell has water above and below it.
                    BlockPos below = water.below();
                    BlockState state = level.getBlockState(below);
                    if (!state.isSolid()) {
                        continue;
                    }

                    level.setBlock(below, floor.getState(random, below), 2);
                }
            }
        }

        return true;
    }
}
