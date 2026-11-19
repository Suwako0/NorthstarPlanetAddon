package com.suwako.northstarplanetaddon.venus.client.renderer;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import com.suwako.northstarplanetaddon.venus.client.model.NepheleModel;
import com.suwako.northstarplanetaddon.venus.entity.boss.NepheleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 涅斐勒渲染器：模型放大约 2.5 倍以匹配 14×6×14 的体型。 */
public class NepheleRenderer extends MobRenderer<NepheleEntity, NepheleModel> {

    private static final ResourceLocation TEXTURE = VenusModule.id("textures/entity/nephele.png");
    private static final float MODEL_SCALE = 2.5F;

    public NepheleRenderer(EntityRendererProvider.Context context) {
        super(context, new NepheleModel(context.bakeLayer(NepheleModel.LAYER)), 3.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(NepheleEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(NepheleEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
    }
}
