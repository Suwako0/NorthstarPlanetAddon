package com.suwako.northstarplanetaddon.moon.block;

import com.mojang.serialization.MapCodec;
import com.suwako.northstarplanetaddon.moon.registry.NMABlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Moonforest turf. Bone meal spreads a small mix of ordinary short grass and glowing Moon Blooms
 * across adjacent Dreamsleep Grass, mirroring vanilla grass-block bonemeal without requiring rain or
 * sunlight on the Moon.
 */
public class DreamsleepGrassBlock extends Block implements BonemealableBlock {

    public DreamsleepGrassBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(DreamsleepGrassBlock::new);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return level.getBlockState(pos.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockPos origin = pos.above();
        for (int attempt = 0; attempt < 32; attempt++) {
            BlockPos target = origin.offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
            if (!level.getBlockState(target).isAir() || !level.getBlockState(target.below()).is(NMABlocks.DREAMSLEEP_GRASS.get())) {
                continue;
            }

            BlockState plant = random.nextInt(5) == 0
                    ? NMABlocks.MOON_BLOOM.get().defaultBlockState()
                    : Blocks.SHORT_GRASS.defaultBlockState();
            if (plant.canSurvive(level, target)) {
                level.setBlock(target, plant, Block.UPDATE_CLIENTS);
            }
        }
    }
}
