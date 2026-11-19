package com.suwako.northstarplanetaddon.venus.block;

import com.suwako.northstarplanetaddon.venus.blockentity.AbstractMachineBlockEntity;
import com.suwako.northstarplanetaddon.venus.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 云梯（设计草案第 8 章：10 SU/RPM / 32 RPM / 1 人每 3 秒 / 64 格范围 / 跨维度）。
 *
 * <p>当前版本：消耗 100 mB 气压，把人向上送 32 格（潜行则向下 32 格），
 * 每次传送有 60 tick 冷却。跨维度传送等云梯与维度锚点系统一起做。</p>
 */
public class CloudLiftBlock extends AbstractMachineBlock {

    public static final int LIFT_DISTANCE = 32;
    public static final int AIR_PER_LIFT = 100;
    public static final int COOLDOWN_TICKS = 60;

    public CloudLiftBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Entity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        return this.lift(level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        ItemInteractionResult result = this.lift(level, pos, player);
        return result == ItemInteractionResult.SUCCESS ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    private ItemInteractionResult lift(Level level, BlockPos pos, Player player) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof Entity lift)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (lift.getCooldown() > 0) {
            player.displayClientMessage(Component.translatable("message.northstar_planet_addon.cloud_lift_cooldown",
                    lift.getCooldown() / 20).withStyle(ChatFormatting.GRAY), true);
            return ItemInteractionResult.FAIL;
        }
        if (lift.getAirStorage().hasBar(1.0F) || lift.pullFromNeighbors() > 0) {
            if (lift.getAirStorage().extract(AIR_PER_LIFT, true) < AIR_PER_LIFT) {
                player.displayClientMessage(Component.translatable("message.northstar_planet_addon.cloud_lift_no_air")
                        .withStyle(ChatFormatting.RED), true);
                return ItemInteractionResult.FAIL;
            }
            lift.getAirStorage().extract(AIR_PER_LIFT, false);
            int direction = player.isShiftKeyDown() ? -1 : 1;
            double targetY = Math.max(level.getMinBuildHeight() + 2,
                    Math.min(level.getMaxBuildHeight() - 2, player.getY() + direction * LIFT_DISTANCE));
            player.teleportTo(player.getX(), targetY, player.getZ());
            player.fallDistance = 0.0F;
            level.playSound(null, pos, SoundEvents.PHANTOM_FLAP, SoundSource.BLOCKS, 1.0F, 1.2F);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                        player.getX(), player.getY(), player.getZ(), 20, 0.6D, 0.6D, 0.6D, 0.02D);
            }
            lift.setCooldown(COOLDOWN_TICKS);
            return ItemInteractionResult.SUCCESS;
        }
        player.displayClientMessage(Component.translatable("message.northstar_planet_addon.cloud_lift_no_air")
                .withStyle(ChatFormatting.RED), true);
        return ItemInteractionResult.FAIL;
    }

    /** 云梯方块实体：只用空气与冷却，无物品栏。 */
    public static class Entity extends AbstractMachineBlockEntity {

        public static final int BUFFER = 8000;
        private int cooldown;

        public Entity(BlockPos pos, BlockState state) {
            super(ModBlockEntities.CLOUD_LIFT.get(), pos, state, BUFFER);
        }

        @Override
        public boolean suppliesAir() {
            return false;
        }

        public int getCooldown() {
            return this.cooldown;
        }

        public void setCooldown(int ticks) {
            this.cooldown = ticks;
            this.setChanged();
        }

        public int pullFromNeighbors() {
            return this.pullAirFromNeighbors(800);
        }

        @Override
        protected boolean tickMachine() {
            if (this.cooldown > 0) {
                this.cooldown--;
                return true;
            }
            return false;
        }

        @Override
        protected void saveAdditional(net.minecraft.nbt.CompoundTag tag,
                                      net.minecraft.core.HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putInt("PhosphorosCooldown", this.cooldown);
        }

        @Override
        protected void loadAdditional(net.minecraft.nbt.CompoundTag tag,
                                      net.minecraft.core.HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            this.cooldown = tag.getInt("PhosphorosCooldown");
        }
    }
}
