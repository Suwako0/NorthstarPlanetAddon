package com.suwako.northstarplanetaddon.moon.world;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;

/**
 * Internal bridge implemented on Minecraft's {@code MultiNoiseBiomeSource} by the common mixin.
 * It deliberately exposes only the decoded climate entries needed to extend Northstar's Moon.
 */
public interface MoonForestBiomeSource {

    List<Pair<Climate.ParameterPoint, Holder<Biome>>> nma$getClimateEntries();

    void nma$setClimateEntries(List<Pair<Climate.ParameterPoint, Holder<Biome>>> entries);
}
