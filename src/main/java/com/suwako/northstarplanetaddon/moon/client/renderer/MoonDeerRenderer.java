package com.suwako.northstarplanetaddon.moon.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.suwako.northstarplanetaddon.moon.MoonModule;
import com.suwako.northstarplanetaddon.moon.client.model.MoonDeerModel;
import com.suwako.northstarplanetaddon.moon.entity.MoonDeerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renders 梦眠月鹿 with {@link MoonDeerModel}. */
public class MoonDeerRenderer extends MobRenderer<MoonDeerEntity, MoonDeerModel> {

    /** 64x64 sheet; the UV slots are described in {@link MoonDeerModel}. */
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MoonModule.NAMESPACE, "textures/entity/moon_deer.png");

    public MoonDeerRenderer(EntityRendererProvider.Context context) {
        super(context, new MoonDeerModel(context.bakeLayer(MoonDeerModel.LAYER_LOCATION)), 0.9F);
    }

    @Override
    public ResourceLocation getTextureLocation(MoonDeerEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(MoonDeerEntity entity, PoseStack poseStack, float partialTick) {
        if (entity.isBaby()) {
            poseStack.scale(0.6F, 0.6F, 0.6F);
        }
    }
}
