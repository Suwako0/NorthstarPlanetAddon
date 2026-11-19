package com.suwako.northstarplanetaddon.venus.item;

import com.suwako.northstarplanetaddon.venus.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 弹道校准器（第 2.4 节「配套物品」）。
 *
 * <p>效果在 {@code BossDamagePipeline} 里读取：</p>
 * <ul>
 *   <li>弱点命中额外 +1 过载值（每 0.25s 一次）</li>
 *   <li>对护甲板伤害 +15%</li>
 * </ul>
 *
 * <p>Curios 缺失时按「背包内生效」处理（不注册 Curios 槽位，也不硬依赖 Curios）。</p>
 */
public class BallisticCalibratorItem extends Item {

    /** 护甲板伤害加成。 */
    public static final double PLATE_DAMAGE_BONUS = 0.15D;
    /** 弱点额外过载值。 */
    public static final double EXTRA_OVERLOAD = 1.0D;

    public BallisticCalibratorItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.northstar_planet_addon.ballistic_calibrator.1")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.northstar_planet_addon.ballistic_calibrator.2")
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    /** 玩家背包（含副手）内是否携带校准器。 */
    public static boolean isCarriedBy(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.BALLISTIC_CALIBRATOR.get())) {
                return true;
            }
        }
        return player.getOffhandItem().is(ModItems.BALLISTIC_CALIBRATOR.get());
    }
}
