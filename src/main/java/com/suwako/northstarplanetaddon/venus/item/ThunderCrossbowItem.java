package com.suwako.northstarplanetaddon.venus.item;

import com.suwako.northstarplanetaddon.venus.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/**
 * 雷羽弩（设计草案 6.2 节）：9 伤害 / 1.2s 蓄力 / 500 耐久；
 * 命中连锁 3 个目标（后续目标 4 伤害）；雷暴带可充能（+50% 伤害，10 发）。
 *
 * <p>充能方式：手持 1 个 {@code static_sac}（静电囊）潜行右击 → +10 发充能（上限 10）。
 * 充能射击伤害 ×1.5，并消耗 1 发——对应草案的「雷暴带充能 10 发」。</p>
 */
public class ThunderCrossbowItem extends Item {

    public static final int CHARGE_TICKS = 24;
    public static final float BASE_DAMAGE = 9.0F;
    public static final float CHAIN_DAMAGE = 4.0F;
    public static final int MAX_CHARGES = 10;
    public static final float RANGE = 24.0F;

    public ThunderCrossbowItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return CHARGE_TICKS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 潜行 + 静电囊 = 充能
        if (player.isShiftKeyDown()) {
            ItemStack sac = player.getInventory().items.stream()
                    .filter(s -> s.is(com.suwako.northstarplanetaddon.venus.registry.ModItems.STATIC_SAC.get()))
                    .findFirst().orElse(ItemStack.EMPTY);
            if (!sac.isEmpty() && stack.getOrDefault(ModComponents.CHARGE, 0) < MAX_CHARGES) {
                if (!level.isClientSide) {
                    sac.shrink(1);
                    stack.set(ModComponents.CHARGE, MAX_CHARGES);
                    player.displayClientMessage(Component.translatable("message.northstar_planet_addon.crossbow_charged")
                            .withStyle(ChatFormatting.AQUA), true);
                }
                return InteractionResultHolder.success(stack);
            }
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        int used = this.getUseDuration(stack, entity) - timeLeft;
        if (used < CHARGE_TICKS) {
            return;
        }
        if (!(entity instanceof Player player) || level.isClientSide) {
            return;
        }

        int charges = stack.getOrDefault(ModComponents.CHARGE, 0);
        boolean empowered = charges > 0;
        float multiplier = empowered ? 1.5F : 1.0F;
        if (empowered) {
            stack.set(ModComponents.CHARGE, charges - 1);
        }

        HitResult hit = player.pick(RANGE, 0.0F, false);
        if (hit.getType() != HitResult.Type.ENTITY || !(hit instanceof EntityHitResult entityHit)
                || !(entityHit.getEntity() instanceof LivingEntity target)) {
            // 没打中：只消耗耐久
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
            return;
        }

        target.hurt(level.damageSources().playerAttack(player), BASE_DAMAGE * multiplier);
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));

        // 连锁 3 目标：链到最近的 2 个其它目标
        List<LivingEntity> chain = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(target.blockPosition()).inflate(6.0D),
                candidate -> candidate != target && candidate != player && candidate.isAlive()
                        && !(candidate instanceof com.suwako.northstarplanetaddon.venus.entity.boss.AbstractBossEntity));
        int chained = 0;
        for (LivingEntity candidate : chain) {
            if (chained >= 2) {
                break;
            }
            candidate.hurt(level.damageSources().playerAttack(player), CHAIN_DAMAGE * multiplier);
            chained++;
        }
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                    target.getX(), target.getY() + 1.0D, target.getZ(), 18, 0.4D, 0.6D, 0.4D, 0.05D);
        }
        player.getCooldowns().addCooldown(this, 10);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int charges = stack.getOrDefault(ModComponents.CHARGE, 0);
        tooltip.add(Component.translatable("tooltip.northstar_planet_addon.thunder_crossbow.charges", charges, MAX_CHARGES)
                .withStyle(charges > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.northstar_planet_addon.thunder_crossbow.hint")
                .withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
