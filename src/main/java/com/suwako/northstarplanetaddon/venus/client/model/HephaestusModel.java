package com.suwako.northstarplanetaddon.venus.client.model;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import com.suwako.northstarplanetaddon.venus.entity.boss.HephaestusEntity;
import com.suwako.northstarplanetaddon.client.GeckoSelfLoad;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.loading.FileLoader;
import software.bernie.geckolib.model.GeoModel;

/**
 * 赫菲斯托斯巨像的 GeckoLib 模型（2026-01 重制版）。
 *
 * <p>资源由 Blockbench 工程导出（编辑源只在制作期暂存，不入库）：
 * <ul>
 *   <li>{@code geo/entity/hephaestus.geo.json} —— 20 根骨骼、241 个方块、512×2048 box UV</li>
 *   <li>{@code animations/entity/hephaestus.animation.json} ——
 *       idle / idle_p2 / idle_p3 / walk / lava_punch / backhand / slag_throw / quake_stomp /
 *       steam_vent / overload_burst / plate_break / death</li>
 *   <li>{@code textures/entity/hephaestus.png} 主体贴图 +
 *       {@code hephaestus_glowmask.png} 自发光遮罩（由 {@code AutoGlowingGeoLayer} 读取）</li>
 * </ul>
 * 模型为 1 单位 = 1/16 格的原生比例（脚底 y=0，头顶约 158 单位 ≈ 9.9 格），渲染器不再额外缩放。</p>
 *
 * <h2>为什么要覆写 {@link #getAnimation}</h2>
 * <p>GeckoLib 的 {@code GeoModel#getAnimation} 只查静态烘焙缓存，查不到<b>先打印调用栈再抛异常</b>；
 * 整合包 / Connector 环境下这张缓存可能不含本模组条目，渲染线程每帧抛一次会刷出 GB 级日志。
 * 这里改用 {@link GeckoSelfLoad#resolve}：直接读缓存表，查不到就自读同一份动画文件，
 * 全程不触发那条会打日志的异常路径。</p>
 *
 * <p>这里做了两层兜底：先照常查缓存（正常环境零开销）；命中失败就用 {@link FileLoader} 自己把
 * {@code hephaestus.animation.json} 读一遍并缓存下来。两条路都失败才真正没有动画，且只记一次日志。</p>
 */
public class HephaestusModel extends GeoModel<HephaestusEntity> {

    private static final ResourceLocation MODEL =
            VenusModule.id("geo/entity/hephaestus.geo.json");
    private static final ResourceLocation TEXTURE =
            VenusModule.id("textures/entity/hephaestus.png");
    private static final ResourceLocation ANIMATIONS =
            VenusModule.id("animations/entity/hephaestus.animation.json");


    @Override
    public ResourceLocation getModelResource(HephaestusEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(HephaestusEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(HephaestusEntity animatable) {
        return ANIMATIONS;
    }

    @Override
    public Animation getAnimation(HephaestusEntity animatable, String name) {
        // 直接读缓存表 + 自读兜底：不调用会打印调用栈的 super 实现
        return GeckoSelfLoad.resolve(ANIMATIONS, name);
    }


}
