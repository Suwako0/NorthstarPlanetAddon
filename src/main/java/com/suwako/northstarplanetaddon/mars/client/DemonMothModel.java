package com.suwako.northstarplanetaddon.mars.client;

import com.suwako.northstarplanetaddon.client.GeckoSelfLoad;
import com.suwako.northstarplanetaddon.mars.MarsModule;
import com.suwako.northstarplanetaddon.mars.entity.DemonMothEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 恶魔蛾的 GeckoLib 模型（idle / fly / attack）。
 *
 * <p>资源：{@code geo/entity/demon_moth.geo.json}、{@code textures/entity/demon_moth.png}、
 * {@code animations/entity/demon_moth.animation.json}。</p>
 *
 * <h2>为什么不直接用 {@code DefaultedEntityGeoModel}</h2>
 * <p>默认实现走 {@code GeoModel#getAnimation}，它只查 GeckoLib 的静态烘焙缓存，查不到<b>先打印调用栈再抛</b>。
 * 本模组在整合包里实测这张缓存不含自己的条目，于是渲染线程每帧抛一次 —— 一晚 1.3 GB 日志、恶魔蛾全程无动画。
 * 这里改用 {@link GeckoSelfLoad#resolve}：直接读缓存表，查不到就自读同一份动画文件，
 * 全程不触发那条会打日志的异常路径。</p>
 */
public class DemonMothModel extends GeoModel<DemonMothEntity> {

    private static final ResourceLocation MODEL = MarsModule.id("geo/entity/demon_moth.geo.json");
    private static final ResourceLocation TEXTURE = MarsModule.id("textures/entity/demon_moth.png");
    private static final ResourceLocation ANIMATIONS =
            MarsModule.id("animations/entity/demon_moth.animation.json");

    @Override
    public ResourceLocation getModelResource(DemonMothEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(DemonMothEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(DemonMothEntity animatable) {
        return ANIMATIONS;
    }

    @Override
    public Animation getAnimation(DemonMothEntity animatable, String name) {
        return GeckoSelfLoad.resolve(ANIMATIONS, name);
    }
}
