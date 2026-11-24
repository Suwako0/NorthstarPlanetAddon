package com.suwako.northstarplanetaddon.venus.client.hud;

import com.suwako.northstarplanetaddon.NorthstarPlanetAddon;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import com.suwako.northstarplanetaddon.venus.entity.boss.AbstractBossEntity;
import com.suwako.northstarplanetaddon.venus.network.BarometerCache;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * 客户端 HUD：气压计读数（5 秒自动淡出）+ Boss 弱点提示。
 *
 * <p>只注册在 {@code Dist.CLIENT} + mod bus，服务端不会加载本类。</p>
 */
@EventBusSubscriber(modid = NorthstarPlanetAddon.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PhosphorosHud {

    private static final int TEXT_COLOR = 0xFFE8F4FF;
    private static final int ICON_COLOR = 0xFF5D9BD6;

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(VenusModule.id("hud"), PhosphorosHud::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        int width = graphics.guiWidth();

        // ---------------- 气压计读数（温度 / 腐蚀 / 季风） ----------------
        if (BarometerCache.isVisible()) {
            int x = width - 152;
            int y = 8;
            graphics.fill(x, y + 2, x + 120, y + 8, 0xAA000000);
            graphics.fill(x + 1, y + 3, x + 120, y + 7, ICON_COLOR);
            graphics.drawString(minecraft.font,
                    String.format("%.0fC  corr %d  monsoon %dd",
                            BarometerCache.temperature(),
                            BarometerCache.corrosion(), BarometerCache.daysToMonsoon()),
                    x, y + 12, TEXT_COLOR, false);
        }

        // ---------------- Boss 弱点提示 ----------------
        AbstractBossEntity boss = minecraft.level == null ? null : minecraft.level.getEntitiesOfClass(
                AbstractBossEntity.class, minecraft.player.getBoundingBox().inflate(40.0D),
                entity -> entity.isAlive()).stream().findFirst().orElse(null);
        if (boss == null) {
            return;
        }
        int exposed = 0;
        for (int i = 0; i < boss.getWeakPoints().size(); i++) {
            if (boss.isWeakPointExposed(i)) {
                exposed++;
            }
        }
        String line = Component.translatable("hud.northstar_planet_addon.weak_points", exposed,
                boss.getWeakPoints().size()).getString();
        int color = exposed > 0 ? 0xFFFFD37A : 0xFF8A9098;
        graphics.drawString(minecraft.font, line, 8, 8, color, true);
    }

    private PhosphorosHud() {
    }
}
