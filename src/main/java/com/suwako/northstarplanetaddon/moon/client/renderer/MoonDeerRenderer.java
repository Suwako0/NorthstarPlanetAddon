package com.suwako.northstarplanetaddon.moon.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.suwako.northstarplanetaddon.moon.MoonModule;
import com.suwako.northstarplanetaddon.moon.client.model.MoonDeerModel;
import com.suwako.northstarplanetaddon.moon.entity.MoonDeerEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders 梦眠月鹿 with {@link MoonDeerModel}, plus an emissive layer so the eyes, the brow gem, the
 * chest rune and the antler blossoms keep their own light — the crown reads as a constellation even
 * in the moon's dim light.
 */
public class MoonDeerRenderer extends MobRenderer<MoonDeerEntity, MoonDeerModel> {

    /** 128x64 sheet; the UV slots are described in {@link MoonDeerModel}. */
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MoonModule.NAMESPACE, "textures/entity/moon_deer.png");
    /** Same UV layout, only the emissive pixels painted. */
    public static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(
            MoonModule.NAMESPACE, "textures/entity/moon_deer_glow.png");

    public MoonDeerRenderer(EntityRendererProvider.Context context) {
        super(context, new MoonDeerModel(context.bakeLayer(MoonDeerModel.LAYER_LOCATION)), 1.1F);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return RenderType.eyes(GLOW);
            }
        });
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
