package com.suwako.northstarplanetaddon.venus.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.suwako.northstarplanetaddon.venus.VenusModule;
import com.suwako.northstarplanetaddon.venus.entity.boss.AphroditeEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * 阿佛洛狄忒 P2「结茧」的茧壳渲染层 —— 做法照火星 Boss 阿瑞斯的 {@code ShieldLayer}。
 *
 * <h2>为什么用独立骨骼 + 独立贴图</h2>
 * <p>{@code cocoon} 骨骼（59 个方块：壳身 + 顶盖 + 尖顶）是 geo 的一部分，主通道每次都会画它一遍。
 * 所以主贴图 {@code aphrodite.png} 在茧壳那块 UV 上是**全透明**的 —— {@code entityCutoutNoCull}
 * 会把这些像素直接丢弃，茧壳只在 {@link #renderForBone} 里用独立贴图重画一次；没结茧时它完全不可见。</p>
 *
 * <h2>颜色随护壁变化</h2>
 * <p>顶点色（ARGB）与贴图相乘，茧壳贴图是接近白的浅灰，所以染色完全由代码控制：
 * 满护壁 240 → <b>酸绿</b>；掉到一半 → <b>黄</b>；快破 → <b>橙红</b>。
 * 玩家不用看血条就知道还能打几下。alpha 走顶点色（0xB4 ≈ 70%），壳外仍能看到里面的女王。</p>
 */
public class AphroditeCocoonLayer extends GeoRenderLayer<AphroditeEntity> {

    /** 茧壳骨骼名，必须与 {@code geo/entity/aphrodite.geo.json} 一致。 */
    private static final String COCOON_BONE = "cocoon";

    private static final ResourceLocation TEXTURE = VenusModule.id("textures/entity/aphrodite_cocoon.png");

    /** 半透明双面渲染：从壳外能看到内部的 Boss。 */
    private static final RenderType COCOON_RENDER_TYPE = RenderType.entityTranslucent(TEXTURE);

    /** 顶点透明度（0xB4 ≈ 70%）。 */
    private static final int ALPHA = 0xB4;

    public AphroditeCocoonLayer(GeoRenderer<AphroditeEntity> renderer) {
        super(renderer);
    }

    @Override
    public void renderForBone(PoseStack poseStack, AphroditeEntity animatable, GeoBone bone, RenderType renderType,
                              MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                              int packedLight, int packedOverlay) {
        // 没结茧 → 一个顶点都不产生；不是茧壳骨骼 → 留给本体通道
        if (!animatable.isCocooned() || !COCOON_BONE.equals(bone.getName())) {
            return;
        }
        VertexConsumer cocoonBuffer = bufferSource.getBuffer(COCOON_RENDER_TYPE);
        this.renderer.renderCubesOfBone(poseStack, bone, cocoonBuffer, packedLight, packedOverlay,
                tintFor(animatable.getCocoonHealthRatio()));
    }

    /**
     * 护壁比例 → 顶点色。
     *
     * <p>1.0 酸绿 {@code #7BE82B} → 0.5 黄 {@code #E8D22B} → 0.0 橙红 {@code #E83B2B}，
     * 中间线性插值，alpha 固定 {@link #ALPHA}。</p>
     */
    private static int tintFor(float ratio) {
        float t = Math.max(0.0F, Math.min(1.0F, ratio));
        int r;
        int g;
        int b;
        if (t > 0.5F) {
            // 黄 → 绿
            float k = (t - 0.5F) / 0.5F;
            r = lerp(0xE8, 0x7B, k);
            g = lerp(0xD2, 0xE8, k);
            b = lerp(0x2B, 0x2B, k);
        } else {
            // 橙红 → 黄
            float k = t / 0.5F;
            r = lerp(0xE8, 0xE8, k);
            g = lerp(0x3B, 0xD2, k);
            b = lerp(0x2B, 0x2B, k);
        }
        return (ALPHA << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerp(int from, int to, float k) {
        return Math.round(from + (to - from) * k);
    }
}
