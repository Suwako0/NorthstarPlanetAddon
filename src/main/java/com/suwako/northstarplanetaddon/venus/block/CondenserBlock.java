package com.suwako.northstarplanetaddon.venus.block;

import com.suwako.northstarplanetaddon.venus.blockentity.AbstractMachineBlockEntity;
import com.suwako.northstarplanetaddon.venus.blockentity.ContentDropping;
import com.suwako.northstarplanetaddon.venus.machine.MachineEnvironment;
import com.suwako.northstarplanetaddon.venus.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * 冷凝塔（设计草案第 8 章：250 mB 水/t，需热端）。
 *
 * <p>气动近似：需要 ≥2 bar 气压 + 附近热源 ≥300°C，每 tick 耗 50 mB，
 * 1200 tick 冷凝出 1 个冰（把「250 mB 水/t」折算成冰，避免引入流体系统）。</p>
 */
public class CondenserBlock extends AbstractMachineBlock {

    public CondenserBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Entity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof Entity condenser)) {
            return InteractionResult.PASS;
        }
        ItemStack output = condenser.collect();
        if (output.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.northstar_planet_addon.condenser_status",
                            String.format("%.1f", condenser.getAirStorage().getBar()),
                            condenser.getProgressPercent())
                    .withStyle(ChatFormatting.AQUA), true);
            return InteractionResult.SUCCESS;
        }
        if (!player.addItem(output)) {
            player.drop(output, false);
        }
        player.displayClientMessage(Component.translatable("message.northstar_planet_addon.condenser_collected",
                output.getHoverName(), output.getCount()).withStyle(ChatFormatting.GOLD), true);
        return InteractionResult.SUCCESS;
    }

    /** 冷凝塔方块实体。 */
    public static class Entity extends AbstractMachineBlockEntity implements ContentDropping {

        public static final int BUFFER = 4000;
        public static final int CONDENSE_TICKS = 1200;
        public static final int AIR_PER_TICK = 50;
        public static final float REQUIRED_BAR = 2.0F;
        public static final float REQUIRED_HEAT = 300.0F;

        private final ItemStackHandler inventory = new ItemStackHandler(1) {
            @Override
            protected void onContentsChanged(int slot) {
                Entity.this.setChanged();
            }
        };
        private int progress;

        public Entity(BlockPos pos, BlockState state) {
            super(ModBlockEntities.CONDENSER.get(), pos, state, BUFFER);
        }

        @Override
        public boolean suppliesAir() {
            return false;
        }

        public ItemStackHandler getInventory() {
            return this.inventory;
        }

        public int getProgressPercent() {
            return Math.min(100, this.progress * 100 / CONDENSE_TICKS);
        }

        public ItemStack collect() {
            ItemStack output = this.inventory.getStackInSlot(0);
            if (output.isEmpty()) {
                return ItemStack.EMPTY;
            }
            this.inventory.setStackInSlot(0, ItemStack.EMPTY);
            return output;
        }

        @Override
        protected boolean isWorking() {
            return this.progress > 0;
        }

        @Override
        protected boolean tickMachine() {
            if (this.getAirStorage().getBar() < REQUIRED_BAR && this.pullAirFromNeighbors(400) <= 0) {
                return false;
            }
            float heat = MachineEnvironment.heatAt(this.level, this.worldPosition)
                    + MachineEnvironment.baseDimensionTemperature(this.level);
            if (heat < REQUIRED_HEAT) {
                return false;
            }
            this.getAirStorage().extract(AIR_PER_TICK, false);
            this.progress++;
            if (this.progress < CONDENSE_TICKS) {
                return true;
            }
            this.progress = 0;
            ItemStack output = this.inventory.getStackInSlot(0);
            if (output.isEmpty()) {
                this.inventory.setStackInSlot(0, new ItemStack(Items.ICE, 1));
            } else if (output.is(Items.ICE) && output.getCount() < output.getMaxStackSize()) {
                output.grow(1);
            }
            return true;
        }

        @Override
        public void dropContents() {
            ItemStack stack = this.inventory.getStackInSlot(0);
            if (!stack.isEmpty() && this.level != null) {
                net.minecraft.world.Containers.dropItemStack(this.level, this.worldPosition.getX(),
                        this.worldPosition.getY(), this.worldPosition.getZ(), stack);
                this.inventory.setStackInSlot(0, ItemStack.EMPTY);
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putInt("PhosphorosProgress", this.progress);
            tag.put("PhosphorosInventory", this.inventory.serializeNBT(registries));
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            this.progress = tag.getInt("PhosphorosProgress");
            if (tag.contains("PhosphorosInventory")) {
                this.inventory.deserializeNBT(registries, tag.getCompound("PhosphorosInventory"));
            }
        }
    }
}
