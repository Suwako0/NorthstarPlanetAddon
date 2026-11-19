package com.suwako.northstarplanetaddon.mercury.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.suwako.northstarplanetaddon.mercury.MercuryModule;
import com.suwako.northstarplanetaddon.mercury.client.model.BlazingSunfishModel;
import com.suwako.northstarplanetaddon.mercury.entity.BlazingSunfishEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Renders 炽烈太阳鱼: same skeleton with the crest, plates and tail flames, and a hot emissive skin. */
public class BlazingSunfishRenderer extends MobRenderer<BlazingSunfishEntity, BlazingSunfishModel> {

    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MercuryModule.NAMESPACE, "textures/entity/blazing_sunfish.png");
    public static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(
            MercuryModule.NAMESPACE, "textures/entity/blazing_sunfish_glow.png");

    public BlazingSunfishRenderer(EntityRendererProvider.Context context) {
        super(context, new BlazingSunfishModel(context.bakeLayer(BlazingSunfishModel.LAYER_LOCATION)), 0.7F);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return RenderType.eyes(GLOW);
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(BlazingSunfishEntity entity) {
        return TEXTURE;
    }

    /** A little larger than the base breed, matching its bigger hitbox. */
    @Override
    protected void scale(BlazingSunfishEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.15F, 1.15F, 1.15F);
    }
}
