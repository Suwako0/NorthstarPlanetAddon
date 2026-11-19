package com.suwako.northstarplanetaddon.mercury.world;

import com.mojang.serialization.Codec;
import com.suwako.northstarplanetaddon.mercury.MercuryModule;
import com.suwako.northstarplanetaddon.mercury.registry.MNABlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Digs the Dry Seabed down into Mercury's surface.
 * <p>
 * A biome cannot lower terrain on its own — the shape of the ground comes from the dimension's noise
 * settings, and Northstar's Mercury does not carve river valleys. So the depression is produced the
 * supported way: a feature that runs inside the biome and cuts the band itself into a channel.
 * <p>
 * The feature is placed exactly once per chunk (a single {@code minecraft:count} entry) and then walks
 * that chunk column by column, so every column is carved exactly once and the depth is fully
 * predictable:
 * <ul>
 * <li>only columns whose biome is {@code northstar_planet_addon:dry_riverbed} are touched, so the
 * sunken part of the ground lines up exactly with the biome a player sees named in the debug screen;</li>
 * <li>the bank is eased in over {@link #RAMP_QUARTS} biome cells, so the channel never ends in a
 * vertical wall;</li>
 * <li>each column is lowered relative to its own surface, which keeps the channel following the
 * landscape instead of slicing a flat trench through it;</li>
 * <li>digging stops at lava, and the new floor is dressed with 水星沙.</li>
 * </ul>
 * The biome is sampled on Minecraft's own quart grid (the same resolution the vanilla
 * {@code minecraft:biome} placement modifier uses), so a whole chunk costs about eighty biome lookups
 * no matter how wide the riverbed is.
 */
public class RiverbedChannelFeature extends Feature<NoneFeatureConfiguration> {

    private static final ResourceKey<Biome> DRY_RIVERBED = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(MercuryModule.NAMESPACE, "dry_riverbed"));

    /** How far the flat floor sits below the surrounding surface. */
    private static final int MIN_DEPTH = 4;
    private static final int MAX_DEPTH = 5;
    /** Width of the bank ramp, in biome cells (one cell = four blocks), so the ramp spans 4-8 blocks. */
    private static final int RAMP_QUARTS = 2;
    /** How many blocks of the new floor are turned into 水星沙 (deeper blocks stay water-star stone). */
    private static final int BED_SAND_DEPTH = 2;

    public RiverbedChannelFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        ChunkPos chunk = new ChunkPos(context.origin());
        int depth = MIN_DEPTH + random.nextInt(MAX_DEPTH - MIN_DEPTH + 1);

        Band band = Band.sample(level, chunk);
        if (band.isEmpty()) {
            return false;
        }

        boolean dug = false;
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                int ease = band.easeToEdge(x, z);
                if (ease <= 0) {
                    continue;
                }
                int dig = (int) Math.round(depth * ease / (double) RAMP_QUARTS);
                if (dig <= 0) {
                    continue;
                }

                int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                int target = Math.max(surface - dig, level.getMinBuildHeight() + 1);
                if (surface <= target) {
                    continue;
                }

                dug |= digColumn(level, x, z, surface, target);
                layBed(level, x, z, target);
            }
        }

        return dug;
    }

    /** Clears one column from {@code surface} down to {@code target}, stopping at lava. */
    private boolean digColumn(WorldGenLevel level, int x, int z, int surface, int target) {
        boolean dug = false;
        for (int y = surface; y > target; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (isFluid(state)) {
                // Never open a hole into Mercury's lava: keep what is above it.
                break;
            }
            if (state.isAir()) {
                continue;
            }
            setBlock(level, pos, Blocks.AIR.defaultBlockState());
            dug = true;
        }
        return dug;
    }

    /** Turns the freshly exposed floor into 水星沙, leaving deeper blocks as water-star stone. */
    private void layBed(WorldGenLevel level, int x, int z, int target) {
        BlockState sand = MNABlocks.MERCURY_SAND.get().defaultBlockState();
        for (int index = 0; index < BED_SAND_DEPTH; index++) {
            BlockPos pos = new BlockPos(x, target + index, z);
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || isFluid(state)) {
                break;
            }
            if (!state.is(MNABlocks.MERCURY_SAND.get())) {
                setBlock(level, pos, sand);
            }
        }
    }

    /** {@code BlockState#liquid()} is deprecated in this NeoForge line; the fluid state is the way in. */
    private static boolean isFluid(BlockState state) {
        return !state.getFluidState().isEmpty();
    }

    /**
     * The riverbed band around one chunk, sampled on Minecraft's quart grid: one biome lookup per
     * four-by-four blocks, exactly like vanilla's own biome placement modifier sees the world.
     */
    private static final class Band {

        private final boolean[] riverbed;
        private final int minQuartX;
        private final int minQuartZ;
        private final int sizeX;
        private final int sizeZ;

        private Band(boolean[] riverbed, int minQuartX, int minQuartZ, int sizeX, int sizeZ) {
            this.riverbed = riverbed;
            this.minQuartX = minQuartX;
            this.minQuartZ = minQuartZ;
            this.sizeX = sizeX;
            this.sizeZ = sizeZ;
        }

        static Band sample(WorldGenLevel level, ChunkPos chunk) {
            int margin = RAMP_QUARTS + 1;
            int minQuartX = QuartPos.fromBlock(chunk.getMinBlockX()) - margin;
            int maxQuartX = QuartPos.fromBlock(chunk.getMaxBlockX()) + margin;
            int minQuartZ = QuartPos.fromBlock(chunk.getMinBlockZ()) - margin;
            int maxQuartZ = QuartPos.fromBlock(chunk.getMaxBlockZ()) + margin;
            int sizeX = maxQuartX - minQuartX + 1;
            int sizeZ = maxQuartZ - minQuartZ + 1;

            boolean[] riverbed = new boolean[sizeX * sizeZ];
            boolean any = false;
            for (int qx = minQuartX; qx <= maxQuartX; qx++) {
                for (int qz = minQuartZ; qz <= maxQuartZ; qz++) {
                    int x = QuartPos.toBlock(qx);
                    int z = QuartPos.toBlock(qz);
                    int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                    boolean inside = level.getBiome(new BlockPos(x, y, z)).is(DRY_RIVERBED);
                    riverbed[(qx - minQuartX) * sizeZ + (qz - minQuartZ)] = inside;
                    any |= inside;
                }
            }
            return new Band(riverbed, minQuartX, minQuartZ, sizeX, sizeZ);
        }

        boolean isEmpty() {
            for (boolean inside : this.riverbed) {
                if (inside) {
                    return false;
                }
            }
            return true;
        }

        /**
         * How deep into the band a column sits, in biome cells, capped at the ramp width.
         * {@code 0} means "on the bank, do not dig"; {@code RAMP_QUARTS} means "flat floor".
         */
        int easeToEdge(int x, int z) {
            int qx = QuartPos.fromBlock(x);
            int qz = QuartPos.fromBlock(z);
            if (!isRiverbed(qx, qz)) {
                return 0;
            }

            int smallest = RAMP_QUARTS;
            smallest = Math.min(smallest, run(qx, qz, 1, 0));
            smallest = Math.min(smallest, run(qx, qz, -1, 0));
            smallest = Math.min(smallest, run(qx, qz, 0, 1));
            smallest = Math.min(smallest, run(qx, qz, 0, -1));
            return smallest;
        }

        /** Consecutive riverbed cells from (qx, qz) in one axis direction, capped at the ramp width. */
        private int run(int qx, int qz, int stepX, int stepZ) {
            for (int step = 1; step <= RAMP_QUARTS; step++) {
                if (!isRiverbed(qx + stepX * step, qz + stepZ * step)) {
                    return step - 1;
                }
            }
            return RAMP_QUARTS;
        }

        private boolean isRiverbed(int qx, int qz) {
            int index = (qx - this.minQuartX) * this.sizeZ + (qz - this.minQuartZ);
            if (index < 0 || index >= this.riverbed.length) {
                return false;
            }
            return this.riverbed[index];
        }
    }
}
