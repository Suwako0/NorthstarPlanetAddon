package com.suwako.northstarplanetaddon.mars.network;

import com.suwako.northstarplanetaddon.NorthstarPlanetAddon;

import com.suwako.northstarplanetaddon.mars.MarsModule;
import com.suwako.northstarplanetaddon.mars.client.ClientSandstormState;
import com.suwako.northstarplanetaddon.mars.item.PortableCoolerItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 网络注册（mod bus）。
 *
 * <p>签名核对自 {@code neoforge-21.1.249.jar}：
 * {@code RegisterPayloadHandlersEvent.registrar(String)} → {@code PayloadRegistrar}，
 * 再 {@code playToServer(Type, StreamCodec, IPayloadHandler)}。</p>
 */
@EventBusSubscriber(modid = NorthstarPlanetAddon.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(ReloadCoolerPayload.TYPE, ReloadCoolerPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        handleReload(player);
                    }
                }));
        // 沙尘暴状态：服务端 → 客户端。客户端处理只在真正收到包时才会链接到客户端类，
        // 专用服务端永远不会执行到那一行（playToClient 的包只下行）。
        registrar.playToClient(SandstormPayload.TYPE, SandstormPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientSandstormState.accept(payload)));
    }

    /**
     * R 键换弹：从背包里找第一个装满的冷却液罐换上去。
     *
     * <p>v1.13：按结果分开提示 —— **已满时不换弹**（不消耗罐子），只给一句灰字；
     * 只有真的「没有罐子」才报红字 + 失败音。</p>
     */
    private static void handleReload(ServerPlayer player) {
        if (PortableCoolerItem.findCooler(player) == null) {
            return;   // 身上根本没带冷却器：不打扰
        }
        switch (PortableCoolerItem.reloadFromInventory(player)) {
            case RELOADED -> player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 0.8F, 1.3F);
            case ALREADY_FULL -> player.displayClientMessage(
                    Component.translatable("tooltip.northstar_planet_addon.cooler.full")
                            .withStyle(ChatFormatting.GRAY), true);
            case NO_TANK -> {
                player.displayClientMessage(
                        Component.translatable("tooltip.northstar_planet_addon.cooler.no_tank")
                                .withStyle(ChatFormatting.RED), true);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6F, 1.6F);
            }
            case NO_COOLER -> {
            }
        }
    }

    private ModNetwork() {
    }
}
