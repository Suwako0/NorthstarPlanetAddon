package com.suwako.northstarplanetaddon.mercury.registry;

import com.suwako.northstarplanetaddon.mercury.MercuryModule;
import com.suwako.northstarplanetaddon.mercury.world.RiverbedChannelFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** World generation features added by the mod. */
public final class MNAFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, MercuryModule.NAMESPACE);

    /**
     * 干涸海床的凹陷：为干涸海床群系挖出下沉的河道。
     * Referenced from the biome as {@code northstar_planet_addon:dry_riverbed_channel}.
     */
    public static final DeferredHolder<Feature<?>, RiverbedChannelFeature> RIVERBED_CHANNEL =
            FEATURES.register("riverbed_channel", () -> new RiverbedChannelFeature(NoneFeatureConfiguration.CODEC));

    private MNAFeatures() {
    }

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
    }
}
