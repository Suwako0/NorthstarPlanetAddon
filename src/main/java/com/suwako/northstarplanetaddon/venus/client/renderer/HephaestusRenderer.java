package com.suwako.northstarplanetaddon.venus.client.renderer;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import com.suwako.northstarplanetaddon.venus.client.model.HephaestusModel;
import com.suwako.northstarplanetaddon.venus.entity.boss.HephaestusEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 赫菲斯托斯渲染器：模型放大 3 倍以匹配 8×12×8 的体型。 */
public class HephaestusRenderer extends MobRenderer<HephaestusEntity, HephaestusModel> {

    private static final ResourceLocation TEXTURE = VenusModule.id("textures/entity/hephaestus.png");
    private static final float MODEL_SCALE = 3.0F;

    public HephaestusRenderer(EntityRendererProvider.Context context) {
        super(context, new HephaestusModel(context.bakeLayer(HephaestusModel.LAYER)), 3.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(HephaestusEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(HephaestusEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
    }
}
