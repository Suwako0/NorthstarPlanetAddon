package com.suwako.northstarplanetaddon.venus.world;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Worldgen registries that cannot live in a datapack (custom biome source). */
public final class ModWorldgen {

    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, VenusModule.NAMESPACE);

    static {
        BIOME_SOURCES.register("venus", () -> VenusBiomeSource.CODEC);
    }

    private ModWorldgen() {
    }
}