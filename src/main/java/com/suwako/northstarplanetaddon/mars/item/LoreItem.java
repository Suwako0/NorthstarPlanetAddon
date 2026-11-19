package com.suwako.northstarplanetaddon.mars.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 纯说明物品：只有名字与若干行 tooltip（星核碎片、星核核心这类材料）。
 *
 * <p>tooltip 的键在构造时传进来，文案全在语言文件里 —— 中文与英文各一份，
 * 不需要为每种材料写一个类。</p>
 */
public class LoreItem extends Item {

    private final String[] tooltipKeys;

    public LoreItem(Item.Properties properties, String... tooltipKeys) {
        super(properties);
        this.tooltipKeys = tooltipKeys;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        for (String key : this.tooltipKeys) {
            tooltip.add(Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
