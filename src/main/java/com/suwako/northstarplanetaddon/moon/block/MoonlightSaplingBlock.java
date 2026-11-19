package com.suwako.northstarplanetaddon.moon.block;

import com.suwako.northstarplanetaddon.moon.registry.NMABlocks;
import com.suwako.northstarplanetaddon.moon.world.NMATreeGrowers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

/**
 * 月华树苗 / Moonlight Sapling.
 * <p>
 * Growth is driven directly by the mod's own configured feature
 * ({@code northstar_planet_addon:moonlight_tree}) instead of a {@link TreeGrower} payload, so the
 * tree stays fully data driven and can be retuned in a datapack.
 */
public class MoonlightSaplingBlock extends SaplingBlock {

    public MoonlightSaplingBlock(BlockBehaviour.Properties properties) {
        // The superclass grower is only a placeholder; advanceTree is always handled below.
        super(TreeGrower.OAK, properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(NMABlocks.DREAMSLEEP_GRASS.get())
                || state.is(NMABlocks.MOON_SOIL.get())
                || super.mayPlaceOn(state, level, pos);
    }

    @Override
    public void advanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (state.getValue(STAGE) == 0) {
            level.setBlock(pos, state.cycle(STAGE), 4);
            return;
        }

        ConfiguredFeature<?, ?> feature = level.registryAccess()
                .registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getHolder(NMATreeGrowers.MOONLIGHT_TREE)
                .map(holder -> holder.value())
                .orElse(null);

        if (feature == null) {
            return;
        }

        level.removeBlock(pos, false);
        if (!feature.place(level, level.getChunkSource().getGenerator(), random, pos)) {
            // Put the sapling back so a failed placement is not silently swallowed.
            level.setBlock(pos, state, 3);
        }
    }
}
