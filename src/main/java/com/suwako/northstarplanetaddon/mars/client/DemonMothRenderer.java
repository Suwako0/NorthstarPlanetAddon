package com.suwako.northstarplanetaddon.mars.client;

import com.suwako.northstarplanetaddon.mars.MarsModule;
import com.suwako.northstarplanetaddon.mars.entity.DemonMothEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import javax.annotation.Nullable;

/**
 * 火星恶魔蛾渲染：GeckoLib 自定义模型（翼展约 4.9 格）。
 *
 * <p>资源按 GeckoLib 约定自动解析：{@code geo/entity/demon_moth.geo.json}、
 * {@code textures/entity/demon_moth.png}、{@code animations/entity/demon_moth.animation.json}。</p>
 */
public class DemonMothRenderer extends GeoEntityRenderer<DemonMothEntity> {

    public DemonMothRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(MarsModule.id("demon_moth"), true));
        this.shadowRadius = 1.3F;
    }

    @Override
    public RenderType getRenderType(DemonMothEntity animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        // 翅膀是薄片，禁背面剔除避免从下方看穿
        return RenderType.entityCutoutNoCull(texture);
    }
}
