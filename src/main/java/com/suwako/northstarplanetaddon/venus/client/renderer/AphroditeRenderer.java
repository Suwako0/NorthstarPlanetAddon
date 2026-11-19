package com.suwako.northstarplanetaddon.venus.client.renderer;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import com.suwako.northstarplanetaddon.venus.client.model.AphroditeModel;
import com.suwako.northstarplanetaddon.venus.entity.boss.AphroditeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 阿佛洛狄忒渲染器：P3 女王形态放大到约 2 倍体型。 */
public class AphroditeRenderer extends MobRenderer<AphroditeEntity, AphroditeModel> {

    private static final ResourceLocation TEXTURE = VenusModule.id("textures/entity/aphrodite.png");
    private static final float BASE_SCALE = 2.0F;
    private static final float GIANT_SCALE = 3.6F;

    public AphroditeRenderer(EntityRendererProvider.Context context) {
        super(context, new AphroditeModel(context.bakeLayer(AphroditeModel.LAYER)), 1.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(AphroditeEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(AphroditeEntity entity, PoseStack poseStack, float partialTick) {
        float scale = entity.getPhase() >= 3 ? GIANT_SCALE : BASE_SCALE;
        poseStack.scale(scale, scale, scale);
    }
}
