package com.suwako.northstarplanetaddon.mars.client;

import com.suwako.northstarplanetaddon.NorthstarPlanetAddon;

import com.mojang.blaze3d.platform.InputConstants;
import com.suwako.northstarplanetaddon.mars.MarsModule;
import com.suwako.northstarplanetaddon.mars.item.PortableCoolerItem;
import com.suwako.northstarplanetaddon.mars.network.ReloadCoolerPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端按键：<b>R = 换弹</b>（与常见射击游戏一致）。
 *
 * <p>注册在 mod bus（{@link RegisterKeyMappingsEvent#register(KeyMapping)}），
 * 每客户端 tick 轮询 {@code consumeClick()}，命中就发一个空负载给服务端 ——
 * 换弹逻辑只跑在服务端，客户端不碰背包状态。</p>
 */
@EventBusSubscriber(modid = NorthstarPlanetAddon.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModKeys {

    public static final String CATEGORY = "key.categories.northstar_planet_addon";

    public static final KeyMapping RELOAD_COOLER = new KeyMapping(
            "key.northstar_planet_addon.reload_cooler",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY);

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(RELOAD_COOLER);
    }

    /** 客户端 tick 轮询（game bus，仅客户端）。 */
    @EventBusSubscriber(modid = NorthstarPlanetAddon.MODID, value = Dist.CLIENT)
    public static final class TickHandler {

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.screen != null) {
                return;
            }
            while (RELOAD_COOLER.consumeClick()) {
                // 手里没拿冷却器就不用发包
                if (PortableCoolerItem.isCooler(minecraft.player.getMainHandItem())
                        || PortableCoolerItem.isCooler(minecraft.player.getOffhandItem())) {
                    PacketDistributor.sendToServer(ReloadCoolerPayload.INSTANCE);
                }
            }
        }

        private TickHandler() {
        }
    }

    private ModKeys() {
    }
}
