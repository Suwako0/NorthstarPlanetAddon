package com.suwako.northstarplanetaddon.moon.registry;

import com.suwako.northstarplanetaddon.moon.MoonModule;
import com.suwako.northstarplanetaddon.moon.world.feature.HotSpringLakeFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The worldgen features added by the mod. */
public final class NMAFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, MoonModule.NAMESPACE);

    /** 温泉湖 — a vanilla lake whose floor is decided per block instead of once per pool. */
    public static final DeferredHolder<Feature<?>, HotSpringLakeFeature> HOT_SPRING_LAKE =
            FEATURES.register("hot_spring_lake", () -> new HotSpringLakeFeature(LakeFeature.Configuration.CODEC));

    private NMAFeatures() {
    }

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
    }
}
