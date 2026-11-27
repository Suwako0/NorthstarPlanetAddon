package com.suwako.northstarplanetaddon.mars.client;

import com.suwako.northstarplanetaddon.mars.entity.DemonMothEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import javax.annotation.Nullable;

/**
 * 火星恶魔蛾渲染：GeckoLib 自定义模型（翼展约 4.9 格）。
 *
 * <p>模型与资源见 {@link DemonMothModel}；这里只负责渲染类型与阴影。</p>
 */
public class DemonMothRenderer extends GeoEntityRenderer<DemonMothEntity> {

    public DemonMothRenderer(EntityRendererProvider.Context context) {
        super(context, new DemonMothModel());
        this.shadowRadius = 1.3F;
    }

    @Override
    public RenderType getRenderType(DemonMothEntity animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        // 翅膀是薄片，禁背面剔除避免从下方看穿
        return RenderType.entityCutoutNoCull(texture);
    }
}
