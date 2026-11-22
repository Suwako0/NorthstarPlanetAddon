package com.suwako.northstarplanetaddon.venus.client.model;

import com.suwako.northstarplanetaddon.NorthstarPlanetAddon;
import com.suwako.northstarplanetaddon.venus.VenusModule;
import com.suwako.northstarplanetaddon.venus.entity.boss.HephaestusEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.loading.FileLoader;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.model.GeoModel;

/**
 * 赫菲斯托斯巨像的 GeckoLib 模型（2026-01 重制版）。
 *
 * <p>资源由 {@code docs/models/hephaestus.bbmodel} 导出：
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
 * <h2>为什么重写 {@link #getAnimation}</h2>
 * <p>GeckoLib 的 {@code GeoModel#getAnimation} 只从静态缓存 {@code GeckoLibCache.getBakedAnimations()}
 * 里查，查不到<b>直接抛</b> {@code Unable to find animation file}（不是返回 null）。这张缓存由客户端资源
 * 重载监听器填充，在整合包（Connector / 多资源包 / 自定义加载顺序）里有可能取不到我们这一个文件 ——
 * 一旦取不到，Boss 就会一动不动。</p>
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

    /** 自读回退结果；null 表示还没试过。 */
    private static BakedAnimations fallbackAnimations;
    /** 自读也失败了（只记一次日志，不刷屏）。 */
    private static boolean fallbackFailed;
    private static boolean cacheMissLogged;

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
        Animation animation = null;
        try {
            animation = super.getAnimation(animatable, name);
        } catch (RuntimeException exception) {
            if (!cacheMissLogged) {
                cacheMissLogged = true;
                NorthstarPlanetAddon.LOGGER.warn(
                        "[hephaestus] GeckoLib 动画缓存里没有 {}（{}），改由模型自读动画文件", ANIMATIONS, exception.getMessage());
            }
        }
        if (animation == null) {
            BakedAnimations baked = selfLoadedAnimations();
            animation = baked == null ? null : baked.getAnimation(name);
        }
        // 控制器每请求一个动画名就打一次，用来判断「动作没播」到底是没切换，还是切了但没解析出来
        if (REQUESTED.add(name)) {
            NorthstarPlanetAddon.LOGGER.info("[hephaestus] 控制器请求动画 {} → {}", name,
                    animation == null ? "未解析" : ("OK " + animation.length() + " tick / "
                            + animation.boneAnimations().length + " 骨骼"));
        }
        return animation;
    }

    /** 已经打过日志的动画名，避免每帧刷屏。 */
    private static final java.util.Set<String> REQUESTED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static synchronized BakedAnimations selfLoadedAnimations() {
        if (fallbackAnimations != null || fallbackFailed) {
            return fallbackAnimations;
        }
        try {
            fallbackAnimations = FileLoader.loadAnimationsFile(ANIMATIONS,
                    Minecraft.getInstance().getResourceManager());
            NorthstarPlanetAddon.LOGGER.info("[hephaestus] 动画文件已由模型自行加载，共 {} 段",
                    fallbackAnimations.animations().size());
        } catch (Throwable throwable) {
            fallbackFailed = true;
            NorthstarPlanetAddon.LOGGER.error("[hephaestus] 自行加载动画文件失败，Boss 将没有骨骼动画：{} {}",
                    ANIMATIONS, throwable.toString());
        }
        return fallbackAnimations;
    }
}
