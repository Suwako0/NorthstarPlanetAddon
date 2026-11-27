package com.suwako.northstarplanetaddon.client;

import com.suwako.northstarplanetaddon.NorthstarPlanetAddon;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.loading.FileLoader;
import software.bernie.geckolib.loading.object.BakedAnimations;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GeckoLib 动画「自读兜底」。
 *
 * <p>实测（用户的整合包：Sinytra Connector + 200+ 模组）GeckoLib 的资源重载监听器<b>扫不到本模组的
 * 动画文件</b>：{@code GeckoLibCache.getBakedAnimations()} 里有别的模组的条目，唯独没有
 * {@code northstar_planet_addon:animations/**}。而 {@code GeoModel#getAnimation} 只查这张静态缓存，
 * 查不到<b>直接抛异常并且先打印整个调用栈</b> —— 渲染线程每帧调一次，一晚能刷出 1.3 GB 日志。</p>
 *
 * <h2>为什么不直接 try/catch {@code super.getAnimation}</h2>
 * <p>因为 GeckoLib 在<b>抛出之前</b>就已经 {@code printStackTrace} 了：catch 只能阻止本帧崩，
 * 阻止不了日志。所以这里完全不调用那个会抛的方法，改成<b>直接读缓存表</b>
 * （{@link GeckoLibCache#getBakedAnimations()}），查不到再用 {@link FileLoader} 自读同一份文件。</p>
 *
 * <p>正常情况下第一次就命中缓存，开销只是一次 map 查询；整合包里走自读，且每个文件只读一次。</p>
 */
public final class GeckoSelfLoad {

    private static final Map<ResourceLocation, BakedAnimations> LOADED = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> FAILED = ConcurrentHashMap.newKeySet();
    private static final Set<ResourceLocation> LOGGED = ConcurrentHashMap.newKeySet();

    private GeckoSelfLoad() {
    }

    /**
     * 取一段动画：先查 GeckoLib 的烘焙缓存（不调用会抛异常的 API），再退回自读。
     *
     * @return 找不到时返回 {@code null}（调用方的控制器会保持上一个动作，不会崩）
     */
    public static Animation resolve(ResourceLocation animations, String name) {
        BakedAnimations baked = baked(animations);
        if (baked != null) {
            Animation animation = baked.getAnimation(name);
            if (animation != null) {
                return animation;
            }
        }
        BakedAnimations self = load(animations);
        return self == null ? null : self.getAnimation(name);
    }

    /** 只读缓存表，不触发任何异常。 */
    private static BakedAnimations baked(ResourceLocation animations) {
        try {
            return GeckoLibCache.getBakedAnimations().get(animations);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** 读取并缓存一份动画文件；失败返回 {@code null}（只记一次错误）。 */
    public static BakedAnimations load(ResourceLocation animations) {
        if (FAILED.contains(animations)) {
            return null;
        }
        BakedAnimations cached = LOADED.get(animations);
        if (cached != null) {
            return cached;
        }
        try {
            BakedAnimations baked = FileLoader.loadAnimationsFile(animations,
                    Minecraft.getInstance().getResourceManager());
            LOADED.put(animations, baked);
            if (LOGGED.add(animations)) {
                NorthstarPlanetAddon.LOGGER.info("[anim] GeckoLib 缓存里没有 {}，已由模型自读（{} 段）",
                        animations, baked.animations().size());
            }
            return baked;
        } catch (Throwable throwable) {
            FAILED.add(animations);
            NorthstarPlanetAddon.LOGGER.error("[anim] 自读动画文件失败 {}：{}", animations, throwable.toString());
            return null;
        }
    }
}
