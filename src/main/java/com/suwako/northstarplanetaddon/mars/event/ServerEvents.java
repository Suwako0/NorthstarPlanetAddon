package com.suwako.northstarplanetaddon.mars.event;

import com.suwako.northstarplanetaddon.NorthstarPlanetAddon;

import com.suwako.northstarplanetaddon.mars.MarsModule;
import com.suwako.northstarplanetaddon.mars.world.Sandstorm;
import com.suwako.northstarplanetaddon.mars.world.StarcoreBombardment;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 服务端生命周期与沙尘暴驱动。
 *
 * <p>沙尘暴状态机（{@link Sandstorm}）需要三件事才能真的跑起来：
 * 每个 tick 被推一次、玩家进出火星时补发状态、服务器停机时清状态。都在这里。</p>
 */
@EventBusSubscriber(modid = NorthstarPlanetAddon.MODID)
public final class ServerEvents {

    /** 驱动沙尘暴：自然发生、到期结束、每秒对表；顺带推进星核炮击。 */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Sandstorm.tick(event.getServer());
        StarcoreBombardment.tick(event.getServer());
    }

    /** 登录时把当前状态（或「没沙尘暴」）发给玩家。 */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Sandstorm.syncTo(player);
        }
    }

    /**
     * 换维度时补发状态。
     *
     * <p>进火星 → 收到当前状态；离开火星 → 收到「无沙尘暴」，否则客户端会带着火星的雾效回到主世界。</p>
     */
    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Sandstorm.syncTo(player);
        }
    }

    /** 停机清状态，避免下次开服继承上一场的剩余时间 / 未打完的炮击。 */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        Sandstorm.reset();
        StarcoreBombardment.clear();
    }

    private ServerEvents() {
    }
}
