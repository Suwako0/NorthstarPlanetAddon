package com.suwako.northstarplanetaddon.mars.block;

import com.suwako.northstarplanetaddon.mars.MarsModule;
import com.suwako.northstarplanetaddon.mars.entity.AresEntity;
import com.suwako.northstarplanetaddon.mars.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 阿瑞斯祭坛（v1.12 重新设计）。
 *
 * <h2>怎么召唤</h2>
 * <p><b>手持熔岩桶右击祭坛</b>即可唤醒阿瑞斯：祭坛上方生成 Boss，熔岩桶变成空桶，
 * 祭坛位置就是 Boss 的领域中心（48 格）。</p>
 *
 * <h2>和旧设计的区别</h2>
 * <p>旧祭坛要求「星核 + 3×3×3 的 26 块外壳 + 附近有岩浆 + 火山灰平原群系 + 手持暗红鳞粉右击星核」——
 * 四个前置条件里任何一个不满足都只给一句红字，玩家很难试出来（这也是本轮反馈的由来）。
 * 现在全部简化成一个方块 + 一桶岩浆：<b>岩浆本身就是旧设计里那堆条件要表达的东西</b>。</p>
 *
 * <p>唯一保留的约束：领域内（{@value #SUMMON_RADIUS} 格）已经有阿瑞斯时不再重复召唤，
 * 免得反复右键刷出一堆 Boss 把服务器卡死。</p>
 */
public class AresAltarBlock extends Block {

    /** 重复召唤检查半径（格）—— 与 Boss 领域半径一致。 */
    public static final int SUMMON_RADIUS = 48;

    public AresAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(Items.LAVA_BUCKET)) {
            // 空手 / 拿着别的东西右击：给一句提示，别让玩家猜（仅服务端发一次）
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("message.northstar_planet_addon.altar_hint")
                                .withStyle(ChatFormatting.GOLD), true);
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!serverLevel.getEntitiesOfClass(AresEntity.class,
                new AABB(pos).inflate(SUMMON_RADIUS)).isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.northstar_planet_addon.altar_already")
                            .withStyle(ChatFormatting.RED), true);
            return ItemInteractionResult.FAIL;
        }

        AresEntity ares = ModEntities.ARES.get().create(serverLevel);
        if (ares == null) {
            MarsModule.LOGGER.warn("Failed to create Ares at altar {}", pos);
            return ItemInteractionResult.FAIL;
        }
        ares.moveTo(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                player.getYRot() + 180.0F, 0.0F);
        ares.setDomainCenter(pos);
        serverLevel.addFreshEntity(ares);

        serverLevel.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D,
                120, 2.0D, 1.5D, 2.0D, 0.0D);
        serverLevel.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D,
                120, 2.0D, 1.5D, 2.0D, 0.05D);
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D,
                60, 1.5D, 1.0D, 1.5D, 0.02D);
        serverLevel.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.5F, 0.6F);

        // 熔岩桶 → 空桶（与倒岩浆的手感一致；创造模式不消耗）
        if (!player.getAbilities().instabuild) {
            player.setItemInHand(hand, new ItemStack(Items.BUCKET));
        }
        player.displayClientMessage(
                Component.translatable("message.northstar_planet_addon.altar_summoned")
                        .withStyle(ChatFormatting.DARK_RED), false);
        MarsModule.LOGGER.info("Ares summoned at altar {} by {}", pos, player.getName().getString());
        return ItemInteractionResult.SUCCESS;
    }
}
