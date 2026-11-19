package com.suwako.northstarplanetaddon.moon.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/**
 * 温泉 — NeoForge's water-like fluid plus the look of an actual hot spring: bubbles rise through the
 * water column, pop at the surface, and a thin campfire-style plume of steam drifts off it.
 * <p>
 * All of this lives in {@link #animateTick}, which the client calls for a handful of random blocks
 * per tick, so the effect costs nothing on the server and fades out with distance on its own.
 */
public abstract class HotSpringFluid extends BaseFlowingFluid {

    /** 1-in-N chances per animation sample; the numbers keep a pool lively without looking fizzy. */
    private static final int BUBBLE_CHANCE = 6;
    private static final int BUBBLE_POP_CHANCE = 24;
    private static final int STEAM_CHANCE = 8;

    protected HotSpringFluid(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(Level level, BlockPos pos, FluidState state, RandomSource random) {
        double x = pos.getX() + random.nextDouble();
        double z = pos.getZ() + random.nextDouble();

        if (random.nextInt(BUBBLE_CHANCE) == 0) {
            level.addParticle(ParticleTypes.BUBBLE,
                    x, pos.getY() + 0.05D + random.nextDouble() * 0.5D, z,
                    0.0D, 0.03D + random.nextDouble() * 0.05D, 0.0D);
        }

        // Steam and popping bubbles belong to the surface only, so they are tied to source blocks
        // whose block above is air — a submerged or covered block never plumes.
        if (!state.isSource() || !level.getBlockState(pos.above()).isAir()) {
            return;
        }

        if (random.nextInt(BUBBLE_POP_CHANCE) == 0) {
            level.addParticle(ParticleTypes.BUBBLE_POP, x, pos.getY() + 0.92D, z, 0.0D, 0.0D, 0.0D);
        }

        if (random.nextInt(STEAM_CHANCE) == 0) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    x, pos.getY() + 0.95D, z,
                    (random.nextDouble() - 0.5D) * 0.02D,
                    0.04D + random.nextDouble() * 0.03D,
                    (random.nextDouble() - 0.5D) * 0.02D);
        }
    }

    public static class Source extends HotSpringFluid {

        public Source(Properties properties) {
            super(properties);
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    public static class Flowing extends HotSpringFluid {

        public Flowing(Properties properties) {
            super(properties);
            registerDefaultState(getStateDefinition().any().setValue(LEVEL, 7));
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }
}
