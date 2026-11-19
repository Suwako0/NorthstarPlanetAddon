package com.suwako.northstarplanetaddon.venus.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 云钩（设计草案 6.2 节）：2 伤害 / 400 耐久；右键钩住 24 格内的方块或生物并拉近，
 * 对空中的目标可以拉下来。
 *
 * <p>无客户端按键、无自建投射物实体：直接用 {@link Player#pick} 做射线判定，
 * 命中生物就把目标拉向玩家（空中目标额外给一个向下的分量），
 * 命中方块就把玩家拉向方块（抓钩位移）。</p>
 */
public class CloudHookItem extends Item {

    public static final double RANGE = 24.0D;

    public CloudHookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        HitResult hit = player.pick(RANGE, 0.0F, false);
        boolean used = false;

        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target) {
            Vec3 pull = player.position().subtract(target.position());
            Vec3 direction = pull.lengthSqr() > 0.01D ? pull.normalize() : Vec3.ZERO;
            target.setDeltaMovement(direction.x * 0.9D, 0.35D, direction.z * 0.9D);
            target.hurtMarked = true;
            // 空中目标拉下：给一个额外的向下分量
            if (!target.onGround()) {
                target.setDeltaMovement(target.getDeltaMovement().x, Math.min(0.0D, target.getDeltaMovement().y - 0.4D),
                        target.getDeltaMovement().z);
            }
            used = true;
        } else if (hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            Vec3 pull = Vec3.atCenterOf(blockHit.getBlockPos()).subtract(player.position());
            Vec3 direction = pull.normalize();
            player.setDeltaMovement(direction.x * 1.2D, Math.max(0.45D, direction.y * 1.2D + 0.2D),
                    direction.z * 1.2D);
            player.hasImpulse = true;
            player.fallDistance = 0.0F;
            used = true;
        }

        if (!used) {
            return InteractionResultHolder.pass(stack);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS,
                1.0F, 1.2F);
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        player.getCooldowns().addCooldown(this, 10);
        player.swing(hand, true);
        return InteractionResultHolder.success(stack);
    }
}
