package com.suwako.northstarplanetaddon.mercury.world;

import com.suwako.northstarplanetaddon.mercury.MercuryModule;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The ore table a fresh meteorite crater is seeded with.
 * <p>
 * The ore blocks belong to Northstar, so they are looked up by id in the block registry instead of
 * being referenced as classes — the addon needs no compile-time dependency on Northstar, and a
 * renamed ore simply drops out of the table (with one warning in the log) instead of crashing.
 * <p>
 * Copper carries double weight, matching the ore list this feature was specified with.
 */
public final class MeteorOres {

    private static final List<Entry> TABLE = List.of(
            new Entry("mercury_copper_ore", 2),
            new Entry("mercury_iron_ore", 2),
            new Entry("mercury_zinc_ore", 2),
            new Entry("mercury_gold_ore", 1),
            new Entry("mercury_redstone_ore", 1),
            new Entry("mercury_lapis_ore", 1),
            new Entry("mercury_titanium_ore", 1),
            new Entry("mercury_tungsten_ore", 1),
            new Entry("mercury_diamond_ore", 1),
            new Entry("mercury_glowstone_ore", 1));

    /** Resolved once, on the first crater; {@code null} entries are ores the loaded Northstar lacks. */
    private static BlockState[] resolved;
    private static int totalWeight;

    private MeteorOres() {
    }

    /** Picks one ore block state, or air when Northstar provides none of them. */
    public static BlockState pick(RandomSource random) {
        BlockState[] states = resolved();
        if (totalWeight <= 0) {
            return Blocks.AIR.defaultBlockState();
        }

        int roll = random.nextInt(totalWeight);
        for (int index = 0; index < TABLE.size(); index++) {
            BlockState state = states[index];
            if (state == null) {
                continue;
            }
            roll -= TABLE.get(index).weight();
            if (roll < 0) {
                return state;
            }
        }

        // Unreachable for a well formed table; the first available ore is a safe fallback.
        for (BlockState state : states) {
            if (state != null) {
                return state;
            }
        }
        return Blocks.AIR.defaultBlockState();
    }

    /** Resolves the table once and reports it, so a missing id is visible instead of silently absent. */
    private static BlockState[] resolved() {
        if (resolved != null) {
            return resolved;
        }

        BlockState[] states = new BlockState[TABLE.size()];
        int weight = 0;
        int available = 0;
        for (int index = 0; index < TABLE.size(); index++) {
            Entry entry = TABLE.get(index);
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("northstar", entry.id());
            Block block = BuiltInRegistries.BLOCK.get(id);
            if (block == null || block == Blocks.AIR) {
                MercuryModule.LOGGER.warn(
                        "Meteorite ore {} is missing from the block registry; it will not appear in craters.", id);
                continue;
            }
            states[index] = block.defaultBlockState();
            weight += entry.weight();
            available++;
        }

        totalWeight = weight;
        resolved = states;
        MercuryModule.LOGGER.info("Meteorite crater ore table resolved: {}/{} ores available.",
                available, TABLE.size());
        return resolved;
    }

    private record Entry(String id, int weight) {
    }
}
