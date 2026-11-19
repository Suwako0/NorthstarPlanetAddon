package com.suwako.northstarplanetaddon.moon.world;

import com.suwako.northstarplanetaddon.moon.MoonModule;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

/** Resource keys of the mod's data driven worldgen, shared between code and the generated JSON. */
public final class NMATreeGrowers {

    /** {@code data/northstar_planet_addon/worldgen/configured_feature/moonlight_tree.json} */
    public static final ResourceKey<ConfiguredFeature<?, ?>> MOONLIGHT_TREE = ResourceKey.create(
            Registries.CONFIGURED_FEATURE,
            ResourceLocation.fromNamespaceAndPath(MoonModule.NAMESPACE, "moonlight_tree"));

    private NMATreeGrowers() {
    }
}
