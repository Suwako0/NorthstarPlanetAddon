package com.suwako.northstarplanetaddon.venus.client;

import com.suwako.northstarplanetaddon.NorthstarPlanetAddon;
import com.suwako.northstarplanetaddon.venus.VenusModule;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import software.bernie.geckolib.cache.GeckoLibCache;

import java.util.Map;

/**
 * 客户端一次性自检：GeckoLib 到底有没有把本模组的动画文件塞进
 * {@link GeckoLibCache#getBakedAnimations()}。
 *
 * <p>这是「模型有、但一动不动」这类问题的唯一分水岭：{@code GeoModel#getAnimation}
 * 只从这张静态缓存里取数据，取不到就直接抛 {@code Unable to find animation file}。</p>
 *
 * <p><b>触发时机很重要</b>：必须等真正进了世界再看（资源重载早已完成）。之前按固定 tick 数
 * 在加载界面上就开查，缓存还是 0 条，于是调用 {@code getAnimation} 直接把客户端炸了 —— 现在改成
 * 「进世界 20 tick 后」或「标题界面挂满 1200 tick 后」才查，且全程 try/catch。</p>
 */
@EventBusSubscriber(modid = NorthstarPlanetAddon.MODID, value = Dist.CLIENT)
public final class HephaestusAnimDiagnostics {

    private static final ResourceLocation ANIMATIONS =
            VenusModule.id("animations/entity/hephaestus.animation.json");
    /** 进世界后等这么多 tick 再查。 */
    private static final int TICKS_IN_WORLD = 20;
    /** 一直停在标题界面时的兜底时间点（60 秒）。 */
    private static final int TICKS_TITLE_FALLBACK = 1200;

    private static boolean reported;
    private static int ticks;
    /** 骨骼采样上限（每 10 tick 一次，够看清一次出招）。 */
    private static final int MAX_SAMPLES = 40;
    private static int probeTicks;
    private static int samples;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!reported) {
            checkOnce();
        }
        probeBones();
    }

    private static void checkOnce() {
        Minecraft minecraft = Minecraft.getInstance();
        ticks++;
        boolean inWorld = minecraft.level != null;
        if (inWorld ? ticks < TICKS_IN_WORLD : ticks < TICKS_TITLE_FALLBACK) {
            return;
        }
        reported = true;
        try {
            Map<ResourceLocation, ?> cache = GeckoLibCache.getBakedAnimations();
            boolean stored = cache.containsKey(ANIMATIONS);
            NorthstarPlanetAddon.LOGGER.info("[hephaestus] GeckoLib 动画缓存 {} 条（inWorld={}），本模组动画【{}】：{}",
                    cache.size(), inWorld, stored ? "已加载" : "缺失", ANIMATIONS);
            if (!stored && !cache.isEmpty()) {
                for (ResourceLocation key : cache.keySet()) {
                    NorthstarPlanetAddon.LOGGER.warn("[hephaestus]   缓存条目：{}", key);
                }
            }
            // HephaestusModel#getAnimation 内部有 try/catch + 自读回退，这里不会炸
            var model = new com.suwako.northstarplanetaddon.venus.client.model.HephaestusModel();
            var idle = model.getAnimation(null, "idle");
            NorthstarPlanetAddon.LOGGER.info("[hephaestus] 动画解析：{}",
                    idle == null ? "失败（控制器拿不到任何动画）"
                            : "成功，" + idle.length() + " tick / " + idle.boneAnimations().length + " 条骨骼通道");
        } catch (Throwable throwable) {
            NorthstarPlanetAddon.LOGGER.warn("[hephaestus] 动画自检异常（不影响游戏）：{}", throwable.toString());
        }
    }

    /**
     * 骨骼采样：每 10 tick 打印一次最近一只巨像的「动作状态 + 几个关键骨骼的实际旋转」。
     *
     * <p>这是判断「动画没被播放」和「播了但骨骼没动」的唯一办法：控制器状态是服务端同步过来的，
     * 骨骼旋转是 GeckoLib 渲染层真正应用后的值 —— 两者对不上就说明问题在渲染/模型层。</p>
     */
    private static void probeBones() {
        if (samples >= MAX_SAMPLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        if (++probeTicks < 10) {
            return;
        }
        probeTicks = 0;
        var boss = minecraft.level.getEntitiesOfClass(
                        com.suwako.northstarplanetaddon.venus.entity.boss.HephaestusEntity.class,
                        minecraft.player.getBoundingBox().inflate(64.0D))
                .stream().findFirst().orElse(null);
        if (boss == null) {
            return;
        }
        samples++;
        String bones;
        try {
            var renderer = minecraft.getEntityRenderDispatcher().getRenderer(boss);
            if (renderer instanceof software.bernie.geckolib.renderer.GeoEntityRenderer<?> geo) {
                var model = geo.getGeoModel();
                bones = String.format(java.util.Locale.ROOT,
                        "legR.x=%.1f armR.x=%.1f fistR.x=%.1f chest.x=%.1f cover.x=%.1f",
                        deg(model, "leg_right"), deg(model, "arm_right"), deg(model, "fist_right"),
                        deg(model, "chest"), deg(model, "furnace_cover"));
            } else {
                bones = "渲染器不是 GeoEntityRenderer: " + (renderer == null ? "null" : renderer.getClass().getName());
            }
        } catch (Throwable throwable) {
            bones = "采样异常: " + throwable;
        }
        NorthstarPlanetAddon.LOGGER.info("[hephaestus] 采样#{} action={} phase={} 移动中={} 骨骼[{}]",
                samples, boss.getAction(), boss.getPhase(),
                boss.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D, bones);
    }

    /** 读一根骨骼的 X 旋转（度）；模型还没烘焙时返回 NaN。 */
    private static double deg(software.bernie.geckolib.model.GeoModel<?> model, String bone) {
        return model.getBone(bone).map(b -> (double) b.getRotX()).orElse(Double.NaN);
    }

    private HephaestusAnimDiagnostics() {
    }
}
