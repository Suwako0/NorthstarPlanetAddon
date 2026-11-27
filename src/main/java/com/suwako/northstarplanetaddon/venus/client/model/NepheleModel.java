package com.suwako.northstarplanetaddon.venus.client.model;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import com.suwako.northstarplanetaddon.venus.entity.boss.NepheleEntity;
import com.suwako.northstarplanetaddon.client.GeckoSelfLoad;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 涅斐勒 · 云鲸的 GeckoLib 模型（按三视图参考图重制）。
 *
 * <p>资源（Blockbench 工程只作制作期暂存，不入库）：
 * <ul>
 *   <li>{@code geo/entity/nephele.geo.json} —— 25 根骨骼 / 82 个方块 / 512×512 box UV</li>
 *   <li>{@code animations/entity/nephele.animation.json} ——
 *       idle / fly / cloud_burst / dive / tail_sweep / summon / phase_transition / plate_break / death</li>
 *   <li>{@code textures/entity/nephele.png} + {@code nephele_glowmask.png}（由
 *       {@link software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer} 读取：光环、眼缝、尾端散热环）</li>
 * </ul>
 * 模型为 1 单位 = 1/16 格的原生比例：腹底离地 1.2 格、主体顶 4.7 格、光环顶约 5.9 格，
 * 视觉体长 9.6 格，尾端散热环中心正好落在弱点坐标 (0, 0.8, +4.2)。渲染器不再额外缩放。</p>
 *
 * <h2>为什么要覆写 {@link #getAnimation}</h2>
 * <p>GeckoLib 的 {@code GeoModel#getAnimation} 只查静态烘焙缓存，查不到<b>先打印调用栈再抛异常</b>；
 * 整合包 / Connector 环境下这张缓存可能不含本模组条目，渲染线程每帧抛一次会刷出 GB 级日志。
 * 这里改用 {@link GeckoSelfLoad#resolve}：直接读缓存表，查不到就自读同一份动画文件，
 * 全程不触发那条会打日志的异常路径。</p>
 */
public class NepheleModel extends GeoModel<NepheleEntity> {

    private static final ResourceLocation MODEL = VenusModule.id("geo/entity/nephele.geo.json");
    private static final ResourceLocation TEXTURE = VenusModule.id("textures/entity/nephele.png");
    private static final ResourceLocation ANIMATIONS = VenusModule.id("animations/entity/nephele.animation.json");


    @Override
    public ResourceLocation getModelResource(NepheleEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(NepheleEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(NepheleEntity animatable) {
        return ANIMATIONS;
    }

    @Override
    public Animation getAnimation(NepheleEntity animatable, String name) {
        // 直接读缓存表 + 自读兜底：不调用会打印调用栈的 super 实现
        return GeckoSelfLoad.resolve(ANIMATIONS, name);
    }

}
