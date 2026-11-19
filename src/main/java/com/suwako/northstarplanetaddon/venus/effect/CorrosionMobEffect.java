package com.suwako.northstarplanetaddon.venus.effect;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 腐蚀：每层 -1 护甲，并在叠满期间周期性造成 1 点无视护甲伤害。
 *
 * <p>1.21.1 的 {@link MobEffect} 用 {@code addAttributeModifier} 注册属性修饰符，
 * 数值会按效果等级（amplifier + 1）线性放大，因此 -1 就是「每层 -1 护甲」。</p>
 */
public class CorrosionMobEffect extends MobEffect {

    public CorrosionMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x8BE04A);
        this.addAttributeModifier(Attributes.ARMOR, VenusModule.id("effect.corrosion.armor"),
                -1.0D, AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // 第 7.2 节：腐蚀每 2s 造成 1 点伤害（无视护甲）
        entity.hurt(entity.damageSources().magic(), 1.0F);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = Math.max(1, 40 >> amplifier);
        return duration % interval == 0;
    }
}
