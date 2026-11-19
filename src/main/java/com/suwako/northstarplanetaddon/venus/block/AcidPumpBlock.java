package com.suwako.northstarplanetaddon.venus.block;

import com.suwako.northstarplanetaddon.venus.blockentity.AbstractMachineBlockEntity;
import com.suwako.northstarplanetaddon.venus.registry.ModBlockEntities;
import com.suwako.northstarplanetaddon.venus.registry.ModFluids;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Acid pump (design chapter 8): 200 mB sulfuric acid per tick, requires immersion.
 *
 * <p>It scans a small radius for its own acid fluid; when immersed it fills an internal
 * buffer (200 mB/t) and turns empty buckets into acid buckets on right-click.
 * It needs no energy because the acid itself is the resource being moved.</p>
 */
public class AcidPumpBlock extends AbstractMachineBlock {

    public static final int BUFFER = 8000;
    public static final int MB_PER_TICK = 200;
    public static final int SCAN_RADIUS = 3;
    public static final int BUCKET_MB = 1000;

    public AcidPumpBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Entity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof Entity pump)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.is(Items.BUCKET) && pump.getAcid() >= BUCKET_MB) {
            pump.drain(BUCKET_MB);
            ItemStack filled = new ItemStack(ModFluids.SULFURIC_ACID_BUCKET.get());
            if (!player.addItem(filled)) {
                player.drop(filled, false);
            }
            stack.shrink(1);
            return ItemInteractionResult.SUCCESS;
        }
        player.displayClientMessage(Component.translatable("message.northstar_planet_addon.acid_pump",
                        pump.getAcid(), BUFFER, pump.isImmersed() ? "ok" : "dry")
                .withStyle(pump.isImmersed() ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        return InteractionResult.PASS;
    }

    /** Acid pump block entity. */
    public static class Entity extends AbstractMachineBlockEntity {

        private int acid;
        private boolean immersed;

        public Entity(BlockPos pos, BlockState state) {
            super(ModBlockEntities.ACID_PUMP.get(), pos, state, 0);
        }

        @Override
        public boolean acceptsAir() {
            return false;
        }

        @Override
        public boolean suppliesAir() {
            return false;
        }

        public int getAcid() {
            return this.acid;
        }

        public boolean isImmersed() {
            return this.immersed;
        }

        public void drain(int amount) {
            this.acid = Math.max(0, this.acid - amount);
            this.setChanged();
        }

        @Override
        protected boolean isWorking() {
            return this.immersed;
        }

        @Override
        protected boolean tickMachine() {
            if (this.level == null) {
                return false;
            }
            this.immersed = this.scanForAcid();
            if (!this.immersed || this.acid >= BUFFER) {
                return false;
            }
            this.acid = Math.min(BUFFER, this.acid + MB_PER_TICK);
            return true;
        }

        private boolean scanForAcid() {
            for (BlockPos candidate : BlockPos.betweenClosed(
                    this.worldPosition.offset(-SCAN_RADIUS, -1, -SCAN_RADIUS),
                    this.worldPosition.offset(SCAN_RADIUS, 1, SCAN_RADIUS))) {
                if (this.level.getFluidState(candidate).getType() == ModFluids.SULFURIC_ACID.get()) {
                    return true;
                }
                if (this.level.getFluidState(candidate).getType() == ModFluids.FLOWING_SULFURIC_ACID.get()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putInt("PhosphorosAcid", this.acid);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            this.acid = tag.getInt("PhosphorosAcid");
        }
    }
}