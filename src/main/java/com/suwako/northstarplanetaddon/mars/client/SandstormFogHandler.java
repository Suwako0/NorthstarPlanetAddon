package com.suwako.northstarplanetaddon.mars.client;

import com.suwako.northstarplanetaddon.NorthstarPlanetAddon;

import com.suwako.northstarplanetaddon.mars.MarsModule;
import com.suwako.northstarplanetaddon.mars.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * 沙尘暴的客户端雾效：遮天黄沙 + 护目镜的清晰视野。
 *
 * <p>v1.10 起状态来自服务端广播（{@link ClientSandstormState}）。效果分两档：</p>
 * <ul>
 *   <li><b>没戴护目镜</b>：远端雾面拉到 32~72 格并把雾染成沙尘色 —— 这就是「视野降低」；</li>
 *   <li><b>戴了护目镜</b>：远端雾面推到 {@value #CLEAR_FAR} 格，视野恢复清晰。</li>
 * </ul>
 *
 * <h2>v1.12 修的关键 bug：改了雾还必须 <code>setCanceled(true)</code></h2>
 * <p>NeoForge 21.1.249 的 {@code ClientHooks#onFogRender} 是这么写的：</p>
 * <pre>
 * ViewportEvent.RenderFog event = new ViewportEvent.RenderFog(...);
 * if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
 *     RenderSystem.setShaderFogStart(event.getNearPlaneDistance());
 *     RenderSystem.setShaderFogEnd(event.getFarPlaneDistance());
 *     ...
 * }
 * </pre>
 * <p>也就是说 <b>{@code RenderFog} 的 setter 只有在事件被 cancel 之后才会写进渲染状态</b>
 * （渲染器自己已经按原版值设过一次了）。v1.11 只调了 setter 没 cancel，
 * 所以「视野缩小」一点效果都没有 —— 这正是用户反馈的现象。</p>
 * <p>对比：{@code ViewportEvent.ComputeFogColor} 是**无条件**应用的（同一个类的
 * {@code getFogColor} 里 post 完直接取 event 的 RGB），所以雾色那一半当时其实是生效的。</p>
 */
@EventBusSubscriber(modid = NorthstarPlanetAddon.MODID, value = Dist.CLIENT)
public final class SandstormFogHandler {

    /** 戴护目镜时的清晰视野。 */
    private static final float CLEAR_NEAR = 64.0F;
    private static final float CLEAR_FAR = 512.0F;

    /** 没戴护目镜时的雾面（普通 / 强化）。 */
    private static final float DUST_FAR_CALM = 72.0F;
    private static final float DUST_FAR_INTENSE = 32.0F;

    /** 沙尘雾色（偏黄褐）。 */
    private static final float DUST_RED = 0.72F;
    private static final float DUST_GREEN = 0.55F;
    private static final float DUST_BLUE = 0.36F;

    private static boolean gogglesWorn() {
        Player player = Minecraft.getInstance().player;
        return player != null && player.getItemBySlot(EquipmentSlot.HEAD)
                .is(ModItems.SANDSTORM_GOGGLES.get());
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!ClientSandstormState.active() || Minecraft.getInstance().level == null) {
            return;
        }
        if (gogglesWorn()) {
            if (event.getFarPlaneDistance() < CLEAR_FAR) {
                event.setNearPlaneDistance(Math.max(event.getNearPlaneDistance(), CLEAR_NEAR));
                event.setFarPlaneDistance(CLEAR_FAR);
                event.setCanceled(true);
            }
            return;
        }
        float far = ClientSandstormState.intense() ? DUST_FAR_INTENSE : DUST_FAR_CALM;
        if (event.getFarPlaneDistance() > far) {
            event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), far * 0.25F));
            event.setFarPlaneDistance(far);
            // 必须 cancel：见类注释，否则这两个值不会被 ClientHooks 应用
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!ClientSandstormState.active() || Minecraft.getInstance().level == null || gogglesWorn()) {
            return;
        }
        float strength = ClientSandstormState.intense() ? 0.85F : 0.55F;
        event.setRed(Mth.lerp(strength, event.getRed(), DUST_RED));
        event.setGreen(Mth.lerp(strength, event.getGreen(), DUST_GREEN));
        event.setBlue(Mth.lerp(strength, event.getBlue(), DUST_BLUE));
    }

    private SandstormFogHandler() {
    }
}
