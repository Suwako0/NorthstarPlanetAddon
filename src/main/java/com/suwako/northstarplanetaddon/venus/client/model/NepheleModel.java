package com.suwako.northstarplanetaddon.venus.client.model;

import com.suwako.northstarplanetaddon.NorthstarPlanetAddon;
import com.suwako.northstarplanetaddon.venus.VenusModule;
import com.suwako.northstarplanetaddon.venus.entity.boss.NepheleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.loading.FileLoader;
import software.bernie.geckolib.loading.object.BakedAnimations;
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
 * <h2>为什么重写 {@link #getAnimation}</h2>
 * <p>GeckoLib 的 {@code GeoModel#getAnimation} 只查静态缓存，查不到会直接抛异常（不是返回 null）。
 * 整合包 / Connector / 多资源包环境下这个文件可能取不到，Boss 就会一动不动。这里先照常查缓存，
 * 失败再用 {@link FileLoader} 自读一次并缓存，两条路都失败才真正没有动画（只记一次日志）。</p>
 */
public class NepheleModel extends GeoModel<NepheleEntity> {

    private static final ResourceLocation MODEL = VenusModule.id("geo/entity/nephele.geo.json");
    private static final ResourceLocation TEXTURE = VenusModule.id("textures/entity/nephele.png");
    private static final ResourceLocation ANIMATIONS = VenusModule.id("animations/entity/nephele.animation.json");

    private static BakedAnimations fallbackAnimations;
    private static boolean fallbackFailed;
    private static boolean cacheMissLogged;

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
        Animation animation = null;
        try {
            animation = super.getAnimation(animatable, name);
        } catch (RuntimeException exception) {
            if (!cacheMissLogged) {
                cacheMissLogged = true;
                NorthstarPlanetAddon.LOGGER.warn("[nephele] GeckoLib 动画缓存里没有 {}（{}），改由模型自读动画文件",
                        ANIMATIONS, exception.getMessage());
            }
        }
        if (animation == null) {
            BakedAnimations baked = selfLoadedAnimations();
            animation = baked == null ? null : baked.getAnimation(name);
        }
        return animation;
    }

    private static synchronized BakedAnimations selfLoadedAnimations() {
        if (fallbackAnimations != null || fallbackFailed) {
            return fallbackAnimations;
        }
        try {
            fallbackAnimations = FileLoader.loadAnimationsFile(ANIMATIONS,
                    Minecraft.getInstance().getResourceManager());
            NorthstarPlanetAddon.LOGGER.info("[nephele] 动画文件已由模型自行加载，共 {} 段",
                    fallbackAnimations.animations().size());
        } catch (Throwable throwable) {
            fallbackFailed = true;
            NorthstarPlanetAddon.LOGGER.error("[nephele] 自行加载动画文件失败，Boss 将没有骨骼动画：{} {}",
                    ANIMATIONS, throwable.toString());
        }
        return fallbackAnimations;
    }
}
