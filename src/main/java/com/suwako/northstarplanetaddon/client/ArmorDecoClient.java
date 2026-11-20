package com.suwako.northstarplanetaddon.client;

import com.suwako.northstarplanetaddon.NorthstarPlanetAddon;
import com.suwako.northstarplanetaddon.client.render.ArmorDecoLayer;
import com.suwako.northstarplanetaddon.mars.client.model.MoltenCoreArmorModel;
import com.suwako.northstarplanetaddon.venus.client.model.CloudWalkerArmorModel;
import com.suwako.northstarplanetaddon.venus.client.model.PhosphorosArmorModel;
import com.suwako.northstarplanetaddon.venus.client.model.TubeHeavyArmorModel;
import com.suwako.northstarplanetaddon.venus.client.model.TungstenArmorModel;
import com.suwako.northstarplanetaddon.venus.registry.ModItems;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 3D 盔甲装饰件的客户端接线：注册模型层 → 烤模型 → 给玩家渲染器挂上 {@link ArmorDecoLayer}。
 *
 * <p>模型层名固定为 {@code northstar_planet_addon:<set>_armor}，贴图统一走
 * {@code textures/models/armor/<set>_armor.png}（128x128）。</p>
 */
@EventBusSubscriber(modid = NorthstarPlanetAddon.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ArmorDecoClient {

    private static final ModelLayerLocation TUNGSTEN_LAYER = layer("tungsten_armor");
    private static final ModelLayerLocation CLOUD_WALKER_LAYER = layer("cloud_walker_armor");
    private static final ModelLayerLocation TUBE_HEAVY_LAYER = layer("tube_heavy_armor");
    private static final ModelLayerLocation PHOSPHOROS_LAYER = layer("phosphoros_armor");
    private static final ModelLayerLocation MOLTEN_CORE_LAYER = layer("molten_core_armor");

    private static boolean layersAttached;

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        CubeDeformation growth = new CubeDeformation(0.5F);
        event.registerLayerDefinition(TUNGSTEN_LAYER, () -> TungstenArmorModel.createArmorLayer(growth));
        event.registerLayerDefinition(CLOUD_WALKER_LAYER, () -> CloudWalkerArmorModel.createArmorLayer(growth));
        event.registerLayerDefinition(TUBE_HEAVY_LAYER, () -> TubeHeavyArmorModel.createArmorLayer(growth));
        event.registerLayerDefinition(PHOSPHOROS_LAYER, () -> PhosphorosArmorModel.createArmorLayer(growth));
        event.registerLayerDefinition(MOLTEN_CORE_LAYER, () -> MoltenCoreArmorModel.createArmorLayer(growth));
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        EntityModelSet models = event.getEntityModels();

        TungstenArmorModel tungsten = new TungstenArmorModel(models.bakeLayer(TUNGSTEN_LAYER));
        for (var piece : new net.minecraft.world.item.Item[] {
                ModItems.TUNGSTEN_HELMET.get(), ModItems.TUNGSTEN_CHESTPLATE.get(),
                ModItems.TUNGSTEN_LEGGINGS.get(), ModItems.TUNGSTEN_BOOTS.get() }) {
            ArmorDecoLayer.register(piece, tungsten, "tungsten");
        }

        CloudWalkerArmorModel cloudWalker = new CloudWalkerArmorModel(models.bakeLayer(CLOUD_WALKER_LAYER));
        for (var piece : new net.minecraft.world.item.Item[] {
                ModItems.CLOUD_WALKER_HELMET.get(), ModItems.CLOUD_WALKER_CHESTPLATE.get(),
                ModItems.CLOUD_WALKER_LEGGINGS.get(), ModItems.CLOUD_WALKER_BOOTS.get() }) {
            ArmorDecoLayer.register(piece, cloudWalker, "cloud_walker");
        }

        TubeHeavyArmorModel tubeHeavy = new TubeHeavyArmorModel(models.bakeLayer(TUBE_HEAVY_LAYER));
        for (var piece : new net.minecraft.world.item.Item[] {
                ModItems.TUBE_HEAVY_HELMET.get(), ModItems.TUBE_HEAVY_CHESTPLATE.get(),
                ModItems.TUBE_HEAVY_LEGGINGS.get(), ModItems.TUBE_HEAVY_BOOTS.get() }) {
            ArmorDecoLayer.register(piece, tubeHeavy, "tube_heavy");
        }

        PhosphorosArmorModel phosphoros = new PhosphorosArmorModel(models.bakeLayer(PHOSPHOROS_LAYER));
        for (var piece : new net.minecraft.world.item.Item[] {
                ModItems.PHOSPHOROS_HELMET.get(), ModItems.PHOSPHOROS_CHESTPLATE.get(),
                ModItems.PHOSPHOROS_LEGGINGS.get(), ModItems.PHOSPHOROS_BOOTS.get() }) {
            ArmorDecoLayer.register(piece, phosphoros, "phosphoros");
        }

        MoltenCoreArmorModel moltenCore = new MoltenCoreArmorModel(models.bakeLayer(MOLTEN_CORE_LAYER));
        for (var piece : new net.minecraft.world.item.Item[] {
                com.suwako.northstarplanetaddon.mars.registry.ModItems.MOLTEN_CORE_HELMET.get(),
                com.suwako.northstarplanetaddon.mars.registry.ModItems.MOLTEN_CORE_CHESTPLATE.get(),
                com.suwako.northstarplanetaddon.mars.registry.ModItems.MOLTEN_CORE_LEGGINGS.get(),
                com.suwako.northstarplanetaddon.mars.registry.ModItems.MOLTEN_CORE_BOOTS.get() }) {
            ArmorDecoLayer.register(piece, moltenCore, "molten_core");
        }

        if (layersAttached) {
            return;
        }
        layersAttached = true;
        for (var skin : event.getSkins()) {
            var renderer = event.getSkin(skin);
            if (renderer instanceof LivingEntityRenderer<?, ?> living) {
                attachTo(living);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void attachTo(LivingEntityRenderer<?, ?> renderer) {
        LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> playerRenderer =
                (LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>) renderer;
        playerRenderer.addLayer(new ArmorDecoLayer(playerRenderer));
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(
                ResourceLocation.fromNamespaceAndPath(NorthstarPlanetAddon.MODID, name), "main");
    }

    private ArmorDecoClient() {
    }
}
