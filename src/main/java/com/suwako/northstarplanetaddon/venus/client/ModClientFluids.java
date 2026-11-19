package com.suwako.northstarplanetaddon.venus.client;

import com.suwako.northstarplanetaddon.NorthstarPlanetAddon;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import com.suwako.northstarplanetaddon.venus.registry.ModFluids;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Client-side rendering for sulfuric acid (still / flowing textures + tint). */
@EventBusSubscriber(modid = NorthstarPlanetAddon.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModClientFluids {

    private static final ResourceLocation STILL = VenusModule.id("block/sulfuric_acid_still");
    private static final ResourceLocation FLOWING = VenusModule.id("block/sulfuric_acid_flow");

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return FLOWING;
            }

            @Override
            public int getTintColor() {
                return 0xFFB7E04A;
            }
        }, ModFluids.SULFURIC_ACID_TYPE.get());
    }

    private ModClientFluids() {
    }
}