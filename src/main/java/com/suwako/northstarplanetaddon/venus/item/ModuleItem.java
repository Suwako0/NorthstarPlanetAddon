package com.suwako.northstarplanetaddon.venus.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Curios 模块（设计草案 6.4 节）。
 *
 * <p>没有 Curios 时按「背包内生效」处理：只要在物品栏里（含副手）就触发被动，
 * 与 {@link BallisticCalibratorItem} 一致。有 Curios 后只需把这些物品注册成 curio 即可。</p>
 */
public class ModuleItem extends Item {

    private final String tooltipKey;

    public ModuleItem(Properties properties, String tooltipKey) {
        super(properties.stacksTo(1));
        this.tooltipKey = tooltipKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(this.tooltipKey).withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    /** 背包（含副手）里是否携带指定物品。 */
    public static boolean isCarrying(LivingEntity entity, Item item) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                return true;
            }
        }
        return player.getOffhandItem().is(item);
    }
}
