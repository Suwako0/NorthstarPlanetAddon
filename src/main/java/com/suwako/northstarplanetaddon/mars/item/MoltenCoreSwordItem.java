package com.suwako.northstarplanetaddon.mars.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 熔核巨剑（{@code molten_core_sword}）—— 三层攻击模组 + 原有「地热斩」。
 *
 * <p>三维模型由 {@code docs/models/molten_core_sword.bbmodel} 导出为普通 Java 3D 物品模型
 * （{@code models/item/molten_core_sword.json} + {@code textures/item/molten_core_sword.png}）。
 * 物品本身<b>不做骨骼动画</b>：手持时的挥砍由玩家的手臂动作驱动，剑保持刚体。</p>
 *
 * <p>攻击模组由 {@code attack.mjs} 生成（flame 主题），三层结构：</p>
 * <ol>
 *   <li><b>命中层</b> {@link #hurtEnemy}：35% 概率点燃目标 6 秒 + 一串 FLAME 粒子；</li>
 *   <li><b>主动层</b> {@link #use}：保留原版设计的「地热斩」—— 前方 ±78° 扇形、半径
 *       {@value #SLASH_RADIUS} 格、每目标 {@value #SLASH_DAMAGE} 点火焰伤害并点燃 5 秒，
 *       冷却 {@value #SLASH_COOLDOWN_TICKS} tick、消耗 1 耐久（不伤友军）；</li>
 *   <li><b>被动层</b> {@link #inventoryTick}：手持时每 20 tick 冒一串熔岩粒子。</li>
 * </ol>
 */
public class MoltenCoreSwordItem extends SwordItem {

    /** 地热斩：冷却 3 秒。 */
    public static final int SLASH_COOLDOWN_TICKS = 60;
    /** 地热斩：每目标伤害。 */
    public static final float SLASH_DAMAGE = 20.0F;
    /** 地热斩：半径（格）。 */
    public static final double SLASH_RADIUS = 8.0D;
    /** 地热斩：点燃 tick。 */
    public static final int SLASH_FIRE_TICKS = 100;
    /** 扇形张角：视线方向点积下限，0.2 ≈ ±78°。 */
    private static final double ARC_COS = 0.2D;

    /* ---- 攻击模组参数（attack.mjs · flame）---- */
    private static final int HIT_CHANCE = 35;
    private static final int HIT_DURATION = 120;
    private static final int PASSIVE_EVERY = 15;

    public MoltenCoreSwordItem(Item.Properties properties) {
        super(Tiers.NETHERITE, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.northstar_planet_addon.molten_core_sword")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.northstar_planet_addon.molten_core_sword.ability",
                Math.round(SLASH_DAMAGE), SLASH_COOLDOWN_TICKS / 20).withStyle(ChatFormatting.YELLOW));
    }

    /** 命中层：35% 概率点燃。 */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        Level level = attacker.level();
        if (!level.isClientSide && attacker.getRandom().nextInt(100) < HIT_CHANCE) {
            target.setRemainingFireTicks(HIT_DURATION); // 1.21.1 没有 setSecondsOnFire
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.FLAME,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                        8, 0.35, 0.35, 0.35, 0.02);
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    /** 主动层：地热斩（原设计，扇形 + 点燃）。 */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return InteractionResultHolder.fail(stack);
        }
        player.getCooldowns().addCooldown(stack.getItem(), SLASH_COOLDOWN_TICKS);
        player.swing(hand, true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.9F, 0.7F);

        if (level instanceof ServerLevel serverLevel) {
            slash(serverLevel, player);
            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private void slash(ServerLevel level, Player player) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 origin = player.position().add(0.0D, 1.0D, 0.0D);

        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(SLASH_RADIUS, 2.0D, SLASH_RADIUS),
                e -> e != player && e.isAlive() && !e.isSpectator() && !player.isAlliedTo(e));
        for (LivingEntity entity : candidates) {
            Vec3 direction = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D)
                    .subtract(origin);
            if (direction.lengthSqr() < 1.0E-6D) {
                continue;
            }
            if (look.dot(direction.normalize()) < ARC_COS) {
                continue;
            }
            entity.hurt(level.damageSources().playerAttack(player), SLASH_DAMAGE);
            entity.setRemainingFireTicks(SLASH_FIRE_TICKS);
        }

        // 表现：沿视线方向镀一条火焰弧 + 地面熔渣
        for (int i = 1; i <= 9; i++) {
            Vec3 point = origin.add(look.scale(i * (SLASH_RADIUS / 9.0D)));
            level.sendParticles(ParticleTypes.FLAME, point.x, point.y - 0.7D, point.z,
                    4, 0.18D, 0.08D, 0.18D, 0.02D);
            level.sendParticles(ParticleTypes.LAVA, point.x, point.y - 0.8D, point.z,
                    1, 0.12D, 0.05D, 0.12D, 0.0D);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    /** 被动层：手持时每 20 tick 一簇熔岩。 */
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
            server.sendParticles(ParticleTypes.LAVA,
                    player.getX(), player.getY() + 0.9, player.getZ(), 2, 0.25, 0.3, 0.25, 0.0);
        }
    }
}
