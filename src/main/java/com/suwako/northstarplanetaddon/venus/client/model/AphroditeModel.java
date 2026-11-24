package com.suwako.northstarplanetaddon.venus.client.model;

import com.suwako.northstarplanetaddon.NorthstarPlanetAddon;
import com.suwako.northstarplanetaddon.venus.VenusModule;
import com.suwako.northstarplanetaddon.venus.entity.boss.AphroditeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.loading.FileLoader;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.model.GeoModel;

/**
 * 阿佛洛狄忒 · 硫之女王的 GeckoLib 模型（按三视图参考图重制）。
 *
 * <p>资源（Blockbench 工程只作制作期暂存，不入库）：
 * <ul>
 *   <li>{@code geo/entity/aphrodite.geo.json} —— 32 根骨骼 / 234 个方块 / 512×512 贴图</li>
 *   <li>{@code animations/entity/aphrodite.animation.json} ——
 *       idle / float / acid_wave / acid_needle / fire_ring / shockwave /
 *       cocoon_in / cocoon_break / phase_transition / death</li>
 *   <li>{@code textures/entity/aphrodite.png}</li>
 * </ul>
 * 模型为 1 单位 = 1/16 格的原生比例：裙底离地约 2 单位、总高 94.6 单位（≈5.9 格，判定箱 6.0），
 * P3 由实体尺寸翻倍到 12 格时模型按 GeoEntityRenderer 的 1:1 比例一起变大。
 * 弱点（硫晶核 r=1.5、双酸囊 r=1.2）在模型上都有对应实体：{@code sulfur_core} 骨与 {@code sac_left/right} 骨。</p>
 *
 * <h2>为什么重写 {@link #getAnimation}</h2>
 * <p>GeckoLib 的 {@code GeoModel#getAnimation} 只查静态缓存，查不到会直接抛异常（不是返回 null）。
 * 整合包 / Connector / 多资源包环境下这个文件可能取不到，Boss 就会一动不动。这里先照常查缓存，
 * 失败再用 {@link FileLoader} 自读一次并缓存，两条路都失败才真正没有动画（只记一次日志）。</p>
 */
public class AphroditeModel extends GeoModel<AphroditeEntity> {

    private static final ResourceLocation MODEL = VenusModule.id("geo/entity/aphrodite.geo.json");
    private static final ResourceLocation TEXTURE = VenusModule.id("textures/entity/aphrodite.png");
    private static final ResourceLocation ANIMATIONS = VenusModule.id("animations/entity/aphrodite.animation.json");

    private static BakedAnimations fallbackAnimations;
    private static boolean fallbackFailed;
    private static boolean cacheMissLogged;

    @Override
    public ResourceLocation getModelResource(AphroditeEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(AphroditeEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(AphroditeEntity animatable) {
        return ANIMATIONS;
    }

    @Override
    public Animation getAnimation(AphroditeEntity animatable, String name) {
        Animation animation = null;
        try {
            animation = super.getAnimation(animatable, name);
        } catch (RuntimeException exception) {
            if (!cacheMissLogged) {
                cacheMissLogged = true;
                NorthstarPlanetAddon.LOGGER.warn("[aphrodite] GeckoLib 动画缓存里没有 {}（{}），改由模型自读动画文件",
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
            NorthstarPlanetAddon.LOGGER.info("[aphrodite] 动画文件已由模型自行加载，共 {} 段",
                    fallbackAnimations.animations().size());
        } catch (Throwable throwable) {
            fallbackFailed = true;
            NorthstarPlanetAddon.LOGGER.error("[aphrodite] 自行加载动画文件失败，Boss 将没有骨骼动画：{} {}",
                    ANIMATIONS, throwable.toString());
        }
        return fallbackAnimations;
    }
}
