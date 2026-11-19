package com.suwako.northstarplanetaddon.venus.client.screen;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import com.suwako.northstarplanetaddon.venus.menu.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 高压锅 / 高压熔炉共用界面（176×166，箭头区域贴在贴图 (176,0)）。
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {

    private static final ResourceLocation TEXTURE = VenusModule.id("textures/gui/machine.png");
    private static final int ARROW_X = 79;
    private static final int ARROW_Y = 34;
    private static final int ARROW_WIDTH = 24;

    public MachineScreen(MachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        int progress = this.menu.getProgress() * ARROW_WIDTH / this.menu.getTotalTime();
        if (progress > 0) {
            graphics.blit(TEXTURE, this.leftPos + ARROW_X, this.topPos + ARROW_Y,
                    176, 0, progress, 16);
        }
        // 气压条：0–80 bar 映射到 80px 宽度
        int barWidth = (int) Math.min(80.0F, this.menu.getBar() * 4.0F);
        graphics.fill(this.leftPos + 8, this.topPos + 62, this.leftPos + 8 + barWidth,
                this.topPos + 66, 0xFF5D9BD6);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, 0x404040, false);
    }
}
