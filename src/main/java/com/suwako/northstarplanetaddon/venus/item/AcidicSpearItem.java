package com.suwako.northstarplanetaddon.venus.item;

import com.suwako.northstarplanetaddon.venus.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/**
 * 酸蚀矛。
 *
 * <p>第 6.2 节：9 伤害 / 1.4 攻速 / 650 耐久；命中叠 {@code 腐蚀}（-1 护甲/层，最多 5 层，8s）；
 * 对 Boss 护甲板 ×1.2（乘数在 {@code BossDamagePipeline} 里按手持物品判定）。</p>
 */
public class AcidicSpearItem extends SwordItem {

    /** 腐蚀持续 8s。 */
    public static final int CORROSION_DURATION = 160;
    /** 最多 5 层（amplifier 0–4）。 */
    public static final int CORROSION_MAX_LEVEL = 5;

    public AcidicSpearItem(Properties properties, Tier tier) {
        super(tier, properties);
    }

    public AcidicSpearItem(Properties properties) {
        this(properties, net.minecraft.world.item.Tiers.NETHERITE);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean hit = super.hurtEnemy(stack, target, attacker);
        if (hit) {
            MobEffectInstance current = target.getEffect(ModEffects.CORROSION);
            int amplifier = current == null ? 0 : Math.min(current.getAmplifier() + 1, CORROSION_MAX_LEVEL - 1);
            target.addEffect(new MobEffectInstance(ModEffects.CORROSION, CORROSION_DURATION, amplifier));
        }
        return hit;
    }
}
