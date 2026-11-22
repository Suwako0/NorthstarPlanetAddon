package com.suwako.northstarplanetaddon.venus.client.renderer;

import com.suwako.northstarplanetaddon.venus.client.model.HephaestusModel;
import com.suwako.northstarplanetaddon.venus.entity.boss.HephaestusEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

import javax.annotation.Nullable;

/**
 * 赫菲斯托斯巨像渲染器（GeckoLib）。
 *
 * <p>模型本身已是原生比例（约 9.9 格高、10.8 格宽），这里不再做三倍缩放 ——
 * 旧版 {@code MobRenderer} 的 {@code MODEL_SCALE = 3.0F} 是给 54 单位高的方盒模型打的补丁。</p>
 *
 * <p>自发光：{@link AutoGlowingGeoLayer} 会自动查找
 * {@code textures/entity/hephaestus_glowmask.png}，用它的 alpha 决定哪些像素全亮 ——
 * 双肩散热口、胸口两条排气缝、拳缝/膝缝/炉心都是全亮区。</p>
 */
public class HephaestusRenderer extends GeoEntityRenderer<HephaestusEntity> {

    /** 巨像的碰撞盒是 6×9，投影半径取肩宽的一半略小，避免影子糊成一片。 */
    private static final float SHADOW_RADIUS = 2.6F;

    public HephaestusRenderer(EntityRendererProvider.Context context) {
        super(context, new HephaestusModel());
        this.shadowRadius = SHADOW_RADIUS;
        // 顺序要紧：先按护甲板掩码隐藏碎掉的甲片，自发光层再整体重画一遍才不会漏光
        this.addRenderLayer(new HephaestusPlateLayer(this));
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public RenderType getRenderType(HephaestusEntity animatable, ResourceLocation texture,
                                    @Nullable net.minecraft.client.renderer.MultiBufferSource bufferSource,
                                    float partialTick) {
        // 贴图里有透明像素（UV 空白区），必须 cutout，否则会渲染出黑边方块
        return RenderType.entityCutoutNoCull(texture);
    }
}
