package com.suwako.northstarplanetaddon.venus.client.renderer;

import com.suwako.northstarplanetaddon.venus.client.model.NepheleModel;
import com.suwako.northstarplanetaddon.venus.entity.boss.NepheleEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

import javax.annotation.Nullable;

/**
 * 涅斐勒 · 云鲸渲染器（GeckoLib）。
 *
 * <p>模型已是原生比例（体长 9.6 格、主体高 4.7 格、光环顶 5.9 格），不再像旧版 {@code MobRenderer}
 * 那样打 2.5 倍缩放补丁。</p>
 *
 * <p>自发光：{@link AutoGlowingGeoLayer} 自动查找 {@code textures/entity/nephele_glowmask.png}，
 * 用它的亮度决定哪些像素全亮 —— 头顶光环 3 段弧、两道眼缝、尾端散热环与环芯、头冠尖端。</p>
 */
public class NepheleRenderer extends GeoEntityRenderer<NepheleEntity> {

    /** 判定箱 7×4.8，投影半径取躯干半宽略小，避免贴地时影子糊成一片。 */
    private static final float SHADOW_RADIUS = 2.2F;

    public NepheleRenderer(EntityRendererProvider.Context context) {
        super(context, new NepheleModel());
        this.shadowRadius = SHADOW_RADIUS;
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public RenderType getRenderType(NepheleEntity animatable, ResourceLocation texture,
                                    @Nullable net.minecraft.client.renderer.MultiBufferSource bufferSource,
                                    float partialTick) {
        // 贴图有透明像素（UV 空白区），必须 cutout，否则会渲染出黑边方块
        return RenderType.entityCutoutNoCull(texture);
    }
}
