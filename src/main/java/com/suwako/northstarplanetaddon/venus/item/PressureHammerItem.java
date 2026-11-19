package com.suwako.northstarplanetaddon.venus.item;

import com.suwako.northstarplanetaddon.venus.entity.boss.AbstractBossEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 压力锤。
 *
 * <p>第 6.2 节：13 伤害 / 0.75 攻速 / 800 耐久；右键蓄力释放 3 格震荡波（+6 伤害），
 * 对 Boss 追加 2 点过载值（模拟「硬直 +12」），可击碎硫晶柱（M3 硫晶柱方块落地后接上）。</p>
 */
public class PressureHammerItem extends SwordItem {

    public static final float SHOCKWAVE_RADIUS = 3.0F;
    public static final float SHOCKWAVE_DAMAGE = 6.0F;
    public static final int SHOCKWAVE_COOLDOWN_TICKS = 40;

    public PressureHammerItem(Properties properties) {
        super(net.minecraft.world.item.Tiers.NETHERITE, properties.rarity(Rarity.RARE));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // 只在攻击蓄力完成后触发：等于「1.2s 蓄力」
        if (player.getAttackStrengthScale(0.5F) < 0.95F) {
            return InteractionResultHolder.pass(stack);
        }

        player.getCooldowns().addCooldown(this, SHOCKWAVE_COOLDOWN_TICKS);
        stack.hurtAndBreak(12, player, LivingEntity.getSlotForHand(hand));

        if (!level.isClientSide) {
            AABB area = player.getBoundingBox().inflate(SHOCKWAVE_RADIUS, 1.5D, SHOCKWAVE_RADIUS);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                    entity -> entity != player && entity.isAlive() && !player.isAlliedTo(entity));
            for (LivingEntity target : targets) {
                target.hurt(level.damageSources().playerAttack(player), SHOCKWAVE_DAMAGE);
                if (target instanceof AbstractBossEntity boss) {
                    // 硬直：直接给 Boss 累积过载值（重力锤是唯一能「砸」出过载的近战武器）
                    boss.addOverloadFromMechanic(2.0D);
                }
            }
        }
        player.swing(hand, true);
        return InteractionResultHolder.success(stack);
    }
}
