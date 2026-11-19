package com.suwako.northstarplanetaddon.venus.world;

import com.suwako.northstarplanetaddon.venus.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Create-style structure palette and trim pass.
 *
 * <p>Look and feel follows Create's industrial language: andesite casings, cut copper,
 * copper grating as vents, iron bars as girders, chains, lanterns and tinted glass.
 * Our own tungsten / sulfur crystal blocks stay as the "VenusModule" identity accents.</p>
 */
public final class RuinPalette {

    /** Structural casing (andesite family). */
    public static final BlockState CASE = Blocks.POLISHED_ANDESITE.defaultBlockState();
    public static final BlockState CASE_DARK = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
    public static final BlockState TRIM = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
    public static final BlockState FLOOR = Blocks.SMOOTH_STONE.defaultBlockState();

    /** Copper detailing (Create's signature warm metal). */
    public static final BlockState COPPER = Blocks.CUT_COPPER.defaultBlockState();
    public static final BlockState COPPER_WEATHERED = Blocks.WEATHERED_CUT_COPPER.defaultBlockState();
    public static final BlockState GRATE = Blocks.COPPER_GRATE.defaultBlockState();
    public static final BlockState COPPER_ROOF = Blocks.CUT_COPPER_STAIRS.defaultBlockState();

    /** Openings and light. */
    public static final BlockState GLASS = Blocks.TINTED_GLASS.defaultBlockState();
    public static final BlockState GIRDER = Blocks.IRON_BARS.defaultBlockState();
    public static final BlockState CHAIN = Blocks.CHAIN.defaultBlockState();
    public static final BlockState LANTERN = Blocks.LANTERN.defaultBlockState()
            .setValue(LanternBlock.HANGING, Boolean.TRUE);
    public static final BlockState LAMP = Blocks.SEA_LANTERN.defaultBlockState();

    /** VenusModule identity accents. */
    public static final BlockState CLOUD = ModBlocks.CLOUD_STONE.get().defaultBlockState();
    public static final BlockState TUNGSTEN = ModBlocks.TUNGSTEN_BLOCK.get().defaultBlockState();
    public static final BlockState CRYSTAL = ModBlocks.SULFUR_CRYSTAL_BLOCK.get().defaultBlockState();

    /**
     * Generic trim pass: corner lanterns on chains, a glass band and grated vents.
     * Applied after the main layout so every ruin gets the same finish.
     */
    public static void detail(WorldGenLevel level, BlockPos origin, int radius, int wallHeight) {
        int[][] corners = {
                {-radius + 1, -radius + 1}, {radius - 1, -radius + 1},
                {-radius + 1, radius - 1}, {radius - 1, radius - 1}
        };
        for (int[] corner : corners) {
            BlockPos top = origin.offset(corner[0], wallHeight + 2, corner[1]);
            level.setBlock(top, CHAIN, 2);
            level.setBlock(top.below(), LANTERN, 2);
        }
        // 玻璃带 + 铜格栅通风口
        for (int i = -radius + 2; i <= radius - 2; i++) {
            if (Math.floorMod(i, 3) != 0) {
                continue;
            }
            level.setBlock(origin.offset(i, 2, -radius + 1), GLASS, 2);
            level.setBlock(origin.offset(i, 2, radius - 1), GLASS, 2);
            level.setBlock(origin.offset(-radius + 1, 2, i), GRATE, 2);
            level.setBlock(origin.offset(radius - 1, 2, i), GRATE, 2);
        }
        // 地面铜线
        for (int i = -radius + 2; i <= radius - 2; i++) {
            level.setBlock(origin.offset(i, 0, 0), COPPER, 2);
        }
    }

    /** Copper truss lines along the outside of a tower wall. */
    public static void truss(WorldGenLevel level, BlockPos origin, int radius, int height) {
        for (int y = 1; y <= height; y += 4) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos pos = origin.offset(direction.getStepX() * radius, y, direction.getStepZ() * radius);
                level.setBlock(pos, COPPER, 2);
            }
        }
    }

    private RuinPalette() {
    }
}