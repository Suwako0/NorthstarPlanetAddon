package com.suwako.northstarplanetaddon.mars.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
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
import net.minecraft.server.level.ServerLevel;

import java.util.List;

/**
 * 融核巨剑（{@code molten_core_sword}）。
 *
 * <p>数值来自《Boss属性重新设计》：<b>攻击力 12</b>（{@code createAttributes(NETHERITE, 8, -2.4)} ⇒
 * 属性修饰符 8 + 下界合金层级加成 3 = 11，游戏内显示 12）、<b>右键释放「地热斩」</b>：</p>
 * <ul>
 *   <li>以视线为中心约 ±78° 的扇形，半径 {@value #SLASH_RADIUS} 格；</li>
 *   <li>每个目标 {@value #SLASH_DAMAGE} 点火焰伤害并点燃 {@value #SLASH_FIRE_TICKS} tick（5 秒）；</li>
 *   <li>消耗 1 点耐久，冷却 {@value #SLASH_COOLDOWN_TICKS} tick（3 秒）；</li>
 *   <li>不伤自己、不伤友军（{@code isAlliedTo}，含驯服宠物与同队玩家）。</li>
 * </ul>
 *
 * <p>v1.13 按用户要求上调：<b>半径 {@value #SLASH_RADIUS} 格、每次 {@value #SLASH_DAMAGE} 点伤害</b>
 * （原来是 3 格 / 8 点）。冷却与点燃时长未变。要改直接改这里的常量。</p>
 */
public class MoltenCoreSwordItem extends SwordItem {

    public static final int SLASH_COOLDOWN_TICKS = 60;
    public static final float SLASH_DAMAGE = 20.0F;
    public static final double SLASH_RADIUS = 8.0D;
    public static final int SLASH_FIRE_TICKS = 100;
    /** 扇形张角：视线方向点积下限，0.2 ≈ ±78°。 */
    private static final double ARC_COS = 0.2D;

    public MoltenCoreSwordItem(Item.Properties properties) {
        super(Tiers.NETHERITE, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.northstar_planet_addon.molten_core_sword")
                .withStyle(ChatFormatting.GOLD));
    }

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

        // 表现：沿视线方向铺一条火焰弧 + 地面熔渣
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
}
