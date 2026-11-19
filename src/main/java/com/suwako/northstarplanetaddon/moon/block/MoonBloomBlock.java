package com.suwako.northstarplanetaddon.moon.block;

import net.minecraft.core.BlockPos;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.suwako.northstarplanetaddon.moon.registry.NMABlocks;

/**
 * 月之花 / Moon Bloom — a flower that only grows on Moonforest soil.
 * <p>
 * It is a plain {@link BushBlock} subclass (rather than {@code FlowerBlock}) so it ignores sky
 * light and needs no mob effect, which matters on an airless, lightless moon.
 */
public class MoonBloomBlock extends BushBlock {

    private static final VoxelShape SHAPE = box(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D);

    public MoonBloomBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return simpleCodec(MoonBloomBlock::new);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(NMABlocks.DREAMSLEEP_GRASS.get())
                || state.is(NMABlocks.MOON_SOIL.get())
                || super.mayPlaceOn(state, level, pos);
    }

    /**
     * 月之花 cannot float: the moment it has nothing to stand on — whether the soil was dug away or
     * worldgen left it hanging in mid air — it pops off and becomes a normal item drop.
     */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
            BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide() && !state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    /**
     * The catch-all: a bloom that is already floating gets no neighbour updates at all, so only a tick
     * can notice it. Blocks whose supporting block was removed are already handled by
     * {@code BushBlock#updateShape}, which turns the bloom into a drop through the same path.
     */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Client-only ambient fireflies that orbit the placed flower without creating server tick load. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) != 0) {
            return;
        }

        double angle = random.nextDouble() * Math.PI * 2.0D;
        double radius = 0.18D + random.nextDouble() * 0.28D;
        double x = pos.getX() + 0.5D + Math.cos(angle) * radius;
        double y = pos.getY() + 0.25D + random.nextDouble() * 0.55D;
        double z = pos.getZ() + 0.5D + Math.sin(angle) * radius;
        double tangent = 0.006D + random.nextDouble() * 0.007D;

        level.addParticle(ParticleTypes.GLOW, x, y, z,
                -Math.sin(angle) * tangent, 0.006D + random.nextDouble() * 0.008D,
                Math.cos(angle) * tangent);
    }
}
