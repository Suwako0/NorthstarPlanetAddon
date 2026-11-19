package com.suwako.northstarplanetaddon.mars.client;

import com.suwako.northstarplanetaddon.NorthstarPlanetAddon;

import com.suwako.northstarplanetaddon.mars.MarsModule;
import com.suwako.northstarplanetaddon.mars.network.SandstormPayload;
import com.suwako.northstarplanetaddon.mars.world.Sandstorm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 客户端沙尘暴状态：服务端广播的 {@link SandstormPayload} 的本地镜像。
 *
 * <p>v1.10 起这是**唯一**的客户端数据源 —— 雾效（{@link SandstormFogHandler}）与 HUD
 * （{@link SandstormHud}）都读它，不再猜 {@code ClientLevel.isRaining()}。</p>
 *
 * <p>两次广播之间（服务端每秒一次）剩余时间由 {@link #onClientTick} 本地递减，
 * 所以 HUD 上的倒计时是平滑的，而不是一秒一跳。</p>
 */
@EventBusSubscriber(modid = NorthstarPlanetAddon.MODID, value = Dist.CLIENT)
public final class ClientSandstormState {

    private static boolean active;
    private static boolean intense;
    private static int windDegrees;
    private static int remainingTicks;

    /** 收到服务端广播（由 {@code ModNetwork} 的 playToClient 处理器调用）。 */
    public static void accept(SandstormPayload payload) {
        active = payload.active();
        intense = payload.intense();
        windDegrees = payload.windDegrees();
        remainingTicks = Math.max(0, payload.remainingTicks());
    }

    /** 客户端每 tick 递减剩余时间；归零也继续靠服务端的下一个包纠偏。 */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (active && remainingTicks > 0) {
            remainingTicks--;
        }
    }

    public static boolean active() {
        return active;
    }

    public static boolean intense() {
        return active && intense;
    }

    public static int windDegrees() {
        return windDegrees;
    }

    public static int remainingTicks() {
        return active ? remainingTicks : 0;
    }

    public static int remainingSeconds() {
        return (remainingTicks + 19) / 20;
    }

    /** 八方位键名，用于语言文件（与命令共用 {@code Sandstorm.windKey}）。 */
    public static String windKey() {
        return Sandstorm.windKey(windDegrees);
    }

    private ClientSandstormState() {
    }
}
