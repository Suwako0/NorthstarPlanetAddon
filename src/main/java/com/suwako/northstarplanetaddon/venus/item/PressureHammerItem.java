package com.suwako.northstarplanetaddon.venus.item;

import com.suwako.northstarplanetaddon.venus.entity.boss.AbstractBossEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 压力锤（{@code pressure_hammer}）—— 三层攻击模组 + 原有「震荡波」。
 *
 * <p>攻击模组由 {@code attack.mjs} 生成（iron 主题），三层结构：</p>
 * <ol>
 *   <li><b>命中层</b> {@link #hurtEnemy}：25% 概率给目标 4 秒虚弱（模拟被砸得握不住家伙）+ CRIT 粒子；</li>
 *   <li><b>主动层</b> {@link #use}：保留原设计的震荡波——蓄力满后右键，半径
 *       {@value #SHOCKWAVE_RADIUS} 格、{@value #SHOCKWAVE_DAMAGE} 点伤害，
 *       对 Boss 追加 2 点过载值（唯一能「砸」出过载的近战武器），消耗 12 耐久、冷却 40 tick；</li>
 *   <li><b>被动层</b> {@link #inventoryTick}：手持时每 30 tick 冒附魔粒子。</li>
 * </ol>
 */
public class PressureHammerItem extends SwordItem {

    public static final float SHOCKWAVE_RADIUS = 3.0F;
    public static final float SHOCKWAVE_DAMAGE = 6.0F;
    public static final int SHOCKWAVE_COOLDOWN_TICKS = 40;

    /* ---- 攻击模组参数（attack.mjs · iron） ---- */
    private static final int HIT_CHANCE = 30;
    private static final int HIT_DURATION = 100;
    private static final int HIT_AMPLIFIER = 1;
    private static final int PASSIVE_EVERY = 25;

    public PressureHammerItem(Properties properties) {
        super(Tiers.NETHERITE, properties.rarity(Rarity.RARE));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.northstar_planet_addon.pressure_hammer.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.northstar_planet_addon.pressure_hammer.ability",
                Math.round(SHOCKWAVE_DAMAGE), SHOCKWAVE_COOLDOWN_TICKS / 20).withStyle(ChatFormatting.GOLD));
    }

    /** 命中层：25% 概率虚弱。 */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        Level level = attacker.level();
        if (!level.isClientSide && attacker.getRandom().nextInt(100) < HIT_CHANCE) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, HIT_DURATION, HIT_AMPLIFIER), attacker);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.CRIT,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                        8, 0.35, 0.35, 0.35, 0.02);
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    /** 主动层：震荡波（原设计，含 Boss 过载）。 */
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
                    boss.addOverloadFromMechanic(2.0D);
                }
            }
        }
        player.swing(hand, true);
        return InteractionResultHolder.success(stack);
    }

    /** 被动层：手持时每 30 tick 冒附魔粒子。 */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!selected || level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        if (player.tickCount % PASSIVE_EVERY != 0) {
            return;
        }
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    player.getX(), player.getY() + 0.9, player.getZ(), 2, 0.25, 0.3, 0.25, 0.0);
        }
    }
}
