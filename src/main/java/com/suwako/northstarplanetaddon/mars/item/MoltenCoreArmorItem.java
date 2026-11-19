package com.suwako.northstarplanetaddon.mars.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 熔核护甲的一件：tooltip 按部位给出各自的套装效果，并统一附一行「全套」说明。
 *
 * <p>实际效果在 {@code event.ArmorEvents} 里，这里只负责让玩家知道它有什么用。</p>
 */
public class MoltenCoreArmorItem extends ArmorItem {

    public MoltenCoreArmorItem(Holder<ArmorMaterial> material, ArmorItem.Type type,
                               Item.Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        String slot = switch (this.getType()) {
            case HELMET -> "helmet";
            case CHESTPLATE -> "chestplate";
            case LEGGINGS -> "leggings";
            case BOOTS -> "boots";
            default -> null;
        };
        if (slot != null) {
            tooltip.add(Component.translatable("tooltip.northstar_planet_addon.molten_core." + slot)
                    .withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable("tooltip.northstar_planet_addon.molten_core.set")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
