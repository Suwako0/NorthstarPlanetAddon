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

/**
 * Renders 炽热太阳鱼: same serpentine skeleton as 太阳鱼, but the molten breed — hooked beak, whiskers,
 * glowing chest core, molten sail spikes, layered flame-blade fins and a solid flame tail blade.
 * <p>
 * The emissive layer carries the core, the sail spikes, the fin membranes, the horn tips and the tail
 * blade; those are the strongest glow islands of the 128x128 glow sheet.
 */
public class BlazingSunfishRenderer extends MobRenderer<BlazingSunfishEntity, BlazingSunfishModel> {

    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MercuryModule.NAMESPACE, "textures/entity/blazing_sunfish.png");
    public static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(
            MercuryModule.NAMESPACE, "textures/entity/blazing_sunfish_glow.png");

    /** Bigger breed, matching its 1.55 x 1.4 hitbox. */
    private static final float SCALE = 1.15F;
    private static final float MODEL_Y_OFFSET = -0.34F;

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

    @Override
    protected void scale(BlazingSunfishEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.translate(0.0F, MODEL_Y_OFFSET, 0.0F);
        poseStack.scale(SCALE, SCALE, SCALE);
    }
}
