package com.suwako.northstarplanetaddon.mercury.world;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;

/**
 * Internal bridge implemented on Minecraft's {@code MultiNoiseBiomeSource} by the common mixin.
 * <p>
 * It exposes only the decoded climate entries needed to extend the Mercury dimension, so the injector
 * never reaches into Minecraft's fields or codecs directly.
 */
public interface ExtendableBiomeSource {

    List<Pair<Climate.ParameterPoint, Holder<Biome>>> mna$getClimateEntries();

    void mna$setClimateEntries(List<Pair<Climate.ParameterPoint, Holder<Biome>>> entries);
}
