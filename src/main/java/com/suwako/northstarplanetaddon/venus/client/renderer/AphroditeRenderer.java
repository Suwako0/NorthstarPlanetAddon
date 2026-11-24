package com.suwako.northstarplanetaddon.venus.client.renderer;

import com.suwako.northstarplanetaddon.venus.client.model.AphroditeModel;
import com.suwako.northstarplanetaddon.venus.entity.boss.AphroditeEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

import javax.annotation.Nullable;

/**
 * 阿佛洛狄忒 · 硫之女王渲染器（GeckoLib）。
 *
 * <p>模型本身已是原生比例（总高约 5.9 格、裙底直径约 4.1 格），这里不再做缩放 ——
 * 旧版 {@code MobRenderer} 的 {@code BASE_SCALE = 2.0 / GIANT_SCALE = 3.6} 是给 36 单位高的方盒模型打的补丁，
 * 叠在新模型上会得到 19 格高的怪物。</p>
 *
 * <p>P3 女王形态的「体型 ×2」由实体尺寸（8×12）驱动：{@code GeoEntityRenderer} 按实体尺寸缩放，
 * 所以模型不需要第二套骨骼，动画也共用。</p>
 */
public class AphroditeRenderer extends GeoEntityRenderer<AphroditeEntity> {

    /** 判定箱 4×6，投影半径取裙底半径略小。 */
    private static final float SHADOW_RADIUS = 1.7F;

    public AphroditeRenderer(EntityRendererProvider.Context context) {
        super(context, new AphroditeModel());
        this.shadowRadius = SHADOW_RADIUS;
        // 自发光：AutoGlowingGeoLayer 自动查找 textures/entity/aphrodite_glowmask.png
        // （亮部=面甲、硫晶核与外环、裙摆内胆、酸滴、腰侧酸囊；其余全黑）
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
        // 结茧（P2）：独立半透明壳层，颜色随护壁从酸绿 → 黄 → 橙红
        this.addRenderLayer(new AphroditeCocoonLayer(this));
    }

    @Override
    public RenderType getRenderType(AphroditeEntity animatable, ResourceLocation texture,
                                    @Nullable net.minecraft.client.renderer.MultiBufferSource bufferSource,
                                    float partialTick) {
        // 贴图里有透明像素（UV 空白区），必须 cutout，否则会渲染出黑边方块
        return RenderType.entityCutoutNoCull(texture);
    }

    /** P3「女王降临」体型×2：判定箱翻倍，视觉必须跟着翻倍，否则 12 格高的箱子里站着一只 5.9 格的女王。 */
    @Override
    public void preRender(com.mojang.blaze3d.vertex.PoseStack poseStack, AphroditeEntity animatable,
                          software.bernie.geckolib.cache.object.BakedGeoModel model,
                          net.minecraft.client.renderer.MultiBufferSource bufferSource,
                          com.mojang.blaze3d.vertex.VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, colour);
        if (!isReRender && animatable.getPhase() >= 3) {
            poseStack.scale(2.0F, 2.0F, 2.0F);
        }
    }
}
