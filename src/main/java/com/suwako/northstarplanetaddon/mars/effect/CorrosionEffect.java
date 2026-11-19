package com.suwako.northstarplanetaddon.mars.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 腐蚀：火星恶魔蛾鳞粉造成的持续损伤，同时磨损护甲。
 *
 * <p>每秒（20 tick）结算一次：造成 (1 + 等级) 点魔法伤害，并让每个装备槽的护甲掉 1 点耐久。
 * 穿满火星钢护甲获得的「耐腐蚀」会直接免疫该效果（施加处拦截）。</p>
 */
public class CorrosionEffect extends MobEffect {

    public CorrosionEffect() {
        super(MobEffectCategory.HARMFUL, 0x6B0F0F);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.hurt(entity.damageSources().magic(), 1.0F + amplifier);

        if (entity instanceof Player player) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
                    continue;
                }
                ItemStack stack = player.getItemBySlot(slot);
                if (!stack.isEmpty() && stack.isDamageableItem()) {
                    stack.hurtAndBreak(1, player, slot);
                }
            }
        }
        return true;
    }
}
