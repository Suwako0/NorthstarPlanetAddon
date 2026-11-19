package com.suwako.northstarplanetaddon.mars.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 冷却液罐 —— 便携式冷却器的「弹夹」。
 *
 * <p>三种形态对应三个物品：空的 {@code coolant_tank}、{@code water_coolant_tank}、
 * {@code liquid_oxygen_coolant_tank}，全部堆叠上限 16。装满的那两种由机械动力的
 * <b>注液</b>（Spout / {@code create:filling}）灌装，见
 * {@code data/northstar_planet_addon/recipe/*_coolant_tank.json}。</p>
 */
public class CoolantTankItem extends Item {

    public static final String WATER = "water";
    public static final String LIQUID_OXYGEN = "liquid_oxygen";

    /** {@code null} = 空罐。 */
    private final String kind;

    public CoolantTankItem(String kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public String kind() {
        return this.kind;
    }

    public boolean isFilled() {
        return this.kind != null;
    }

    /** 从物品栈反查冷却液种类；空罐或不是罐则返回 {@code null}。 */
    public static String kindOf(ItemStack stack) {
        return stack.getItem() instanceof CoolantTankItem tank ? tank.kind() : null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (this.kind == null) {
            tooltip.add(Component.translatable("tooltip.northstar_planet_addon.tank.empty")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.northstar_planet_addon.tank.filled",
                            Component.translatable("tooltip.northstar_planet_addon.coolant." + this.kind),
                            PortableCoolerItem.TANK_CAPACITY)
                    .withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("tooltip.northstar_planet_addon.tank.cooling",
                            Math.round(PortableCoolerItem.coolantPerMb(this.kind) * PortableCoolerItem.TANK_CAPACITY))
                    .withStyle(ChatFormatting.BLUE));
        }
    }
}
