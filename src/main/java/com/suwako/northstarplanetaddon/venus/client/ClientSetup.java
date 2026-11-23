package com.suwako.northstarplanetaddon.venus.client;

import com.suwako.northstarplanetaddon.NorthstarPlanetAddon;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import com.suwako.northstarplanetaddon.venus.client.model.AcidJellyModel;
import com.suwako.northstarplanetaddon.venus.client.model.AphroditeModel;
import com.suwako.northstarplanetaddon.venus.client.model.CloudRayModel;
import com.suwako.northstarplanetaddon.venus.client.model.NepheleModel;
import com.suwako.northstarplanetaddon.venus.client.renderer.AcidJellyRenderer;
import com.suwako.northstarplanetaddon.venus.client.renderer.AphroditeRenderer;
import com.suwako.northstarplanetaddon.venus.client.renderer.CloudRayRenderer;
import com.suwako.northstarplanetaddon.venus.client.renderer.HephaestusRenderer;
import com.suwako.northstarplanetaddon.venus.client.renderer.NepheleRenderer;
import com.suwako.northstarplanetaddon.venus.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** 客户端渲染器与模型层注册（mod bus，仅客户端）。 */
@EventBusSubscriber(modid = NorthstarPlanetAddon.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // 涅斐勒已改为 GeckoLib 模型（nephele.geo.json），不再注册 LayerDefinition
        event.registerLayerDefinition(AphroditeModel.LAYER, AphroditeModel::createBodyLayer);
        event.registerLayerDefinition(CloudRayModel.LAYER, CloudRayModel::createBodyLayer);
        event.registerLayerDefinition(AcidJellyModel.LAYER, AcidJellyModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.NEPHELE.get(), NepheleRenderer::new);
        event.registerEntityRenderer(ModEntities.HEPHAESTUS.get(), HephaestusRenderer::new);
        event.registerEntityRenderer(ModEntities.APHRODITE.get(), AphroditeRenderer::new);
        event.registerEntityRenderer(ModEntities.CLOUD_RAY.get(), CloudRayRenderer::new);
        event.registerEntityRenderer(ModEntities.ACID_JELLY.get(), AcidJellyRenderer::new);
        event.registerEntityRenderer(ModEntities.SPORE_WALKER.get(), net.minecraft.client.renderer.entity.ZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.MUSHROOM_MOTH.get(), net.minecraft.client.renderer.entity.BeeRenderer::new);
        event.registerEntityRenderer(ModEntities.FROST_STALKER.get(), net.minecraft.client.renderer.entity.SpiderRenderer::new);
        event.registerEntityRenderer(ModEntities.THUNDER_CHICK.get(), net.minecraft.client.renderer.entity.BatRenderer::new);
        event.registerEntityRenderer(ModEntities.LAVA_WORM.get(), net.minecraft.client.renderer.entity.SilverfishRenderer::new);
        event.registerEntityRenderer(ModEntities.BRASS_DEACON.get(), net.minecraft.client.renderer.entity.VindicatorRenderer::new);
        // 投掷的酸蚀矛：直接用物品模型渲染（缩小到约 1.2 格，免得飞出去像根电线杆）
        event.registerEntityRenderer(ModEntities.THROWN_ACID_SPEAR.get(),
                context -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(context, 0.5F, false));
    }

    @SubscribeEvent
    public static void onRegisterScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(com.suwako.northstarplanetaddon.venus.registry.ModMenus.MACHINE.get(),
                com.suwako.northstarplanetaddon.venus.client.screen.MachineScreen::new);
    }
    private ClientSetup() {
    }
}
