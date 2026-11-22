package com.suwako.northstarplanetaddon.mercury.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.suwako.northstarplanetaddon.mercury.MercuryModule;
import com.suwako.northstarplanetaddon.mercury.client.model.SunfishModel;
import com.suwako.northstarplanetaddon.mercury.entity.SunfishEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders 太阳鱼 with {@link SunfishModel}, plus an emissive layer so the eye glints, fin membranes,
 * horn tips and the four flame streamers glow on their own — which is what makes a shoal of them
 * visible in Mercury's night sky.
 * <p>
 * The 128x128 sheet is exported from the Blockbench project (build-time scratch, not committed); the glow sheet is a
 * mask over the same UV layout (only the emissive cubes' islands), built by
 * {@code tools/extract-hd-textures.mjs} + the glow pass in that workflow.
 */
public class SunfishRenderer extends MobRenderer<SunfishEntity, SunfishModel<SunfishEntity>> {

    /** 128x128 sheet; the UV slots are packed by the Blockbench project. */
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MercuryModule.NAMESPACE, "textures/entity/sunfish.png");
    public static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(
            MercuryModule.NAMESPACE, "textures/entity/sunfish_glow.png");

    /** The serpent's head reaches y=24 px while the hitbox is only 1.2 blocks tall, so the model is
     *  dropped to sit centred in the box instead of floating half a block above it. */
    private static final float MODEL_Y_OFFSET = -0.42F;

    public SunfishRenderer(EntityRendererProvider.Context context) {
        super(context, new SunfishModel<>(context.bakeLayer(SunfishModel.LAYER_LOCATION)), 0.5F);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return RenderType.eyes(GLOW);
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(SunfishEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(SunfishEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.translate(0.0F, MODEL_Y_OFFSET, 0.0F);
    }
}
