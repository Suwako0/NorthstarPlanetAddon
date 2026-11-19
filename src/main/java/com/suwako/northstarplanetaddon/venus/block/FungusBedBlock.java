package com.suwako.northstarplanetaddon.venus.block;

import com.suwako.northstarplanetaddon.venus.blockentity.AbstractMachineBlockEntity;
import com.suwako.northstarplanetaddon.venus.blockentity.ContentDropping;
import com.suwako.northstarplanetaddon.venus.registry.ModBlockEntities;
import com.suwako.northstarplanetaddon.venus.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * 菌床（设计草案第 8 章：1 份/8 分钟，机械手可收割）。
 *
 * <p>不需要气动也不需要应力：8 分钟产出一茬菌丝纤维，空手右击收取。
 * 物品槽同时暴露为 {@code IItemHandler}，漏斗/机械手可直接抽走。</p>
 */
public class FungusBedBlock extends AbstractMachineBlock {

    public FungusBedBlock(Properties properties) {
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
        if (!(level.getBlockEntity(pos) instanceof Entity bed)) {
            return InteractionResult.PASS;
        }
        ItemStack harvested = bed.harvest();
        if (harvested.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.northstar_planet_addon.fungus_bed_growing",
                    bed.getProgressPercent()).withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.SUCCESS;
        }
        if (!player.addItem(harvested)) {
            player.drop(harvested, false);
        }
        player.displayClientMessage(Component.translatable("message.northstar_planet_addon.fungus_bed_harvested",
                harvested.getHoverName(), harvested.getCount()).withStyle(ChatFormatting.GREEN), true);
        return InteractionResult.SUCCESS;
    }

    /** 菌床方块实体。 */
    public static class Entity extends AbstractMachineBlockEntity implements ContentDropping {

        public static final int GROWTH_TICKS = 9600;

        private final ItemStackHandler inventory = new ItemStackHandler(1) {
            @Override
            protected void onContentsChanged(int slot) {
                Entity.this.setChanged();
            }
        };
        private int growth;

        public Entity(BlockPos pos, BlockState state) {
            super(ModBlockEntities.FUNGUS_BED.get(), pos, state, 0);
        }

        @Override
        public boolean acceptsAir() {
            return false;
        }

        @Override
        public boolean suppliesAir() {
            return false;
        }

        public ItemStackHandler getInventory() {
            return this.inventory;
        }

        public int getProgressPercent() {
            return Math.min(100, this.growth * 100 / GROWTH_TICKS);
        }

        public ItemStack harvest() {
            ItemStack output = this.inventory.getStackInSlot(0);
            if (output.isEmpty()) {
                return ItemStack.EMPTY;
            }
            this.inventory.setStackInSlot(0, ItemStack.EMPTY);
            return output;
        }

        @Override
        protected boolean tickMachine() {
            if (!this.inventory.getStackInSlot(0).isEmpty()) {
                return false; // 上一茬没收，停止生长
            }
            if (++this.growth < GROWTH_TICKS) {
                return false;
            }
            this.growth = 0;
            this.inventory.setStackInSlot(0,
                    new ItemStack(ModItems.FUNGUS_FIBER.get(), 2 + this.level.random.nextInt(3)));
            if (this.level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                        this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 1.0D,
                        this.worldPosition.getZ() + 0.5D, 6, 0.3D, 0.1D, 0.3D, 0.0D);
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
            tag.putInt("PhosphorosGrowth", this.growth);
            tag.put("PhosphorosInventory", this.inventory.serializeNBT(registries));
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            this.growth = tag.getInt("PhosphorosGrowth");
            if (tag.contains("PhosphorosInventory")) {
                this.inventory.deserializeNBT(registries, tag.getCompound("PhosphorosInventory"));
            }
        }
    }
}
