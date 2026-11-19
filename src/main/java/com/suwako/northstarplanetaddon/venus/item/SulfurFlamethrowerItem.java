package com.suwako.northstarplanetaddon.venus.item;

import com.suwako.northstarplanetaddon.venus.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 硫火喷射器（设计草案 6.2 节）：持续喷射 6 伤害/秒，点燃目标 8s，
 * 消耗 100 mB 硫酸/秒（用 {@code northstar_planet_addon:charge} 组件存燃料，满 600）。
 *
 * <p>实现上没有流体系统：燃料直接记在物品的 {@code charge} 组件里，
 * 每 4 tick 消耗 5 mB（= 100 mB/s），同时对一个 4 格锥形范围结算 1.5 伤害
 * （4 tick 一次 ≈ 6/s）。燃料耗尽会自动打断使用。</p>
 */
public class SulfurFlamethrowerItem extends Item {

    public static final int FUEL_CAPACITY = 600;
    public static final int FUEL_PER_TICK = 5;
    public static final int BURN_INTERVAL = 4;
    public static final float DAMAGE_PER_BURN = 1.5F;
    public static final double RANGE = 4.5D;
    public static final int USE_DURATION = 100;

    public SulfurFlamethrowerItem(Properties properties) {
        super(properties.component(ModComponents.CHARGE, FUEL_CAPACITY));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getOrDefault(ModComponents.CHARGE, 0) <= 0) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.northstar_planet_addon.flamethrower_empty")
                        .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingUseDuration) {
        if (remainingUseDuration % BURN_INTERVAL != 0) {
            return;
        }
        int fuel = stack.getOrDefault(ModComponents.CHARGE, 0);
        if (fuel <= 0) {
            living.releaseUsingItem();
            return;
        }
        stack.set(ModComponents.CHARGE, Math.max(0, fuel - FUEL_PER_TICK * BURN_INTERVAL));
        if (level.isClientSide) {
            return;
        }

        Vec3 look = living.getLookAngle();
        Vec3 origin = living.getEyePosition();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                living.getBoundingBox().inflate(RANGE), candidate -> candidate != living && candidate.isAlive())) {
            Vec3 toTarget = target.position().subtract(origin).normalize();
            if (look.dot(toTarget) > 0.55D && target.distanceTo(living) <= RANGE) {
                target.hurt(level.damageSources().playerAttack((Player) living), DAMAGE_PER_BURN);
                target.igniteForSeconds(3.0F);
            }
        }
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    origin.x + look.x * 1.5D, origin.y + look.y * 1.5D, origin.z + look.z * 1.5D,
                    8, 0.3D, 0.3D, 0.3D, 0.02D);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    origin.x + look.x * 2.0D, origin.y + look.y * 2.0D, origin.z + look.z * 2.0D,
                    4, 0.3D, 0.3D, 0.3D, 0.01D);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int fuel = stack.getOrDefault(ModComponents.CHARGE, 0);
        tooltip.add(Component.translatable("tooltip.northstar_planet_addon.flamethrower.fuel", fuel, FUEL_CAPACITY)
                .withStyle(fuel > 0 ? ChatFormatting.GOLD : ChatFormatting.RED));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
