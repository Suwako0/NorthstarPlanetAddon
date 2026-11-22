package com.suwako.northstarplanetaddon.client;

import com.suwako.northstarplanetaddon.NorthstarPlanetAddon;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.loading.FileLoader;
import software.bernie.geckolib.loading.object.BakedAnimations;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GeckoLib 动画「自读兜底」。
 *
 * <p>实测（用户的整合包：Sinytra Connector + 200+ 模组）GeckoLib 的资源重载监听器<b>扫不到本模组的
 * 动画文件</b>：{@code GeckoLibCache.getBakedAnimations()} 里有 28 条别的模组的条目，唯独没有
 * {@code northstar_planet_addon:animations/**}。而 {@code GeoModel#getAnimation} 只查这张静态缓存，
 * 查不到直接抛 {@code Unable to find animation file} —— 结果就是模型渲染正常、但一个动作都不动。</p>
 *
 * <p>这里用 {@link FileLoader} 自己把同一个文件从资源管理器读出来（同一个加载路径，只是绕过了那张缓存），
 * 并按 {@link ResourceLocation} 缓存结果；失败只记一次日志，不会每帧抛。</p>
 */
public final class GeckoSelfLoad {

    private static final Map<ResourceLocation, BakedAnimations> LOADED = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> FAILED = ConcurrentHashMap.newKeySet();
    private static final Set<ResourceLocation> LOGGED = ConcurrentHashMap.newKeySet();

    private GeckoSelfLoad() {
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
