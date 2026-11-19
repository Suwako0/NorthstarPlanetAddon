package com.suwako.northstarplanetaddon.moon.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A pumpkin-pie-strength mooncake that grants a short lunar blessing when a player eats it. */
public class MooncakeItem extends Item {

    public MooncakeItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity consumer) {
        ItemStack result = super.finishUsingItem(stack, level, consumer);
        if (!level.isClientSide() && consumer instanceof ServerPlayer player) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 1200, 0));
        }
        return result;
    }
}
