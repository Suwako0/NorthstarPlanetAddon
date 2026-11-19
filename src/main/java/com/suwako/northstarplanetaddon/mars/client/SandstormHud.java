package com.suwako.northstarplanetaddon.mars.client;

import com.suwako.northstarplanetaddon.NorthstarPlanetAddon;

import com.suwako.northstarplanetaddon.mars.MarsModule;
import com.suwako.northstarplanetaddon.mars.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * 沙尘暴 HUD。
 *
 * <p>所有人都会看到「沙尘暴」标题（否则玩家根本不知道发生了什么）；</p>
 * <ul>
 *   <li>没戴护目镜：再给一行红字警告 —— 视野被压低、装备会被磨损；</li>
 *   <li>戴了护目镜：给出强化标记、风向与**真实**剩余时间
 *       （时间来自服务端每秒广播的 {@link ClientSandstormState}，本地平滑倒数）。</li>
 * </ul>
 */
@EventBusSubscriber(modid = NorthstarPlanetAddon.MODID, value = Dist.CLIENT)
public final class SandstormHud {

    private static final int MARGIN_X = 6;
    private static final int MARGIN_Y = 6;
    private static final int LINE_HEIGHT = 11;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null || minecraft.options.hideGui) {
            return;
        }
        if (!ClientSandstormState.active()) {
            return;
        }

        boolean goggles = player.getItemBySlot(EquipmentSlot.HEAD)
                .is(ModItems.SANDSTORM_GOGGLES.get());
        GuiGraphics graphics = event.getGuiGraphics();
        int x = MARGIN_X;
        int y = MARGIN_Y;

        graphics.drawString(minecraft.font,
                Component.translatable("hud.northstar_planet_addon.sandstorm.title").withStyle(ChatFormatting.GOLD),
                x, y, 0xFFFFFF);
        y += LINE_HEIGHT;

        if (!goggles) {
            graphics.drawString(minecraft.font,
                    Component.translatable("hud.northstar_planet_addon.sandstorm.warning")
                            .withStyle(ChatFormatting.RED),
                    x, y, 0xFFFFFF);
            return;
        }

        if (ClientSandstormState.intense()) {
            graphics.drawString(minecraft.font,
                    Component.translatable("hud.northstar_planet_addon.sandstorm.intense").withStyle(ChatFormatting.RED),
                    x, y, 0xFFFFFF);
            y += LINE_HEIGHT;
        }

        graphics.drawString(minecraft.font,
                Component.translatable("hud.northstar_planet_addon.sandstorm.wind",
                        Component.translatable("hud.northstar_planet_addon.direction." + ClientSandstormState.windKey())),
                x, y, 0xFFFFFF);
        y += LINE_HEIGHT;

        graphics.drawString(minecraft.font,
                Component.translatable("hud.northstar_planet_addon.sandstorm.remaining",
                        ClientSandstormState.remainingSeconds()),
                x, y, 0xFFFFFF);
    }

    private SandstormHud() {
    }
}
