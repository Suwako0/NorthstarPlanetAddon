package com.suwako.northstarplanetaddon.mercury.client.renderer;

import com.suwako.northstarplanetaddon.mercury.MercuryModule;
import com.suwako.northstarplanetaddon.mercury.client.model.SunfishModel;
import com.suwako.northstarplanetaddon.mercury.entity.SunfishEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders 太阳鱼 with {@link SunfishModel}, plus an emissive layer so the flank strips and fin edges
 * glow on their own — which is what makes a shoal of them visible in Mercury's night sky.
 */
public class SunfishRenderer extends MobRenderer<SunfishEntity, SunfishModel<SunfishEntity>> {

    /** 64x64 sheet; the UV slots are listed in {@link SunfishModel}. */
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MercuryModule.NAMESPACE, "textures/entity/sunfish.png");
    public static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(
            MercuryModule.NAMESPACE, "textures/entity/sunfish_glow.png");

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
}
