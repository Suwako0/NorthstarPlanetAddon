package com.suwako.northstarplanetaddon.moon.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.suwako.northstarplanetaddon.moon.MoonModule;
import com.suwako.northstarplanetaddon.moon.client.model.MoonRabbitModel;
import com.suwako.northstarplanetaddon.moon.entity.MoonRabbitEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renders 月兔 with {@link MoonRabbitModel}. */
public class MoonRabbitRenderer extends MobRenderer<MoonRabbitEntity, MoonRabbitModel> {

    /** 64x64 sheet; the UV slots are described in {@link MoonRabbitModel}. */
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MoonModule.NAMESPACE, "textures/entity/moon_rabbit.png");

    public MoonRabbitRenderer(EntityRendererProvider.Context context) {
        super(context, new MoonRabbitModel(context.bakeLayer(MoonRabbitModel.LAYER_LOCATION)), 0.25F);
    }

    @Override
    public ResourceLocation getTextureLocation(MoonRabbitEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(MoonRabbitEntity entity, PoseStack poseStack, float partialTick) {
        if (entity.isBaby()) {
            poseStack.scale(0.55F, 0.55F, 0.55F);
        }
    }
}
