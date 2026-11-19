package com.suwako.northstarplanetaddon.venus.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * 把 {@link ItemStackHandler} 包成原版 {@link Container}，供 {@link MachineMenu} 使用。
 *
 * <p>这样方块实体不必改成 {@code BaseContainerBlockEntity}，nor 影响已有的
 * {@code IItemHandler} capability（漏斗 / 管道仍然工作）。</p>
 */
public class MachineContainer implements Container {

    private final ItemStackHandler handler;
    private final int inputSlots;

    public MachineContainer(ItemStackHandler handler, int inputSlots) {
        this.handler = handler;
        this.inputSlots = inputSlots;
    }

    public ItemStackHandler getHandler() {
        return this.handler;
    }

    public int getInputSlots() {
        return this.inputSlots;
    }

    @Override
    public int getContainerSize() {
        return this.handler.getSlots();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < this.handler.getSlots(); i++) {
            if (!this.handler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.handler.getStackInSlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack extracted = this.handler.extractItem(slot, amount, false);
        return extracted;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack current = this.handler.getStackInSlot(slot);
        this.handler.setStackInSlot(slot, ItemStack.EMPTY);
        return current;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.handler.setStackInSlot(slot, stack);
    }

    @Override
    public void setChanged() {
        this.handler.setStackInSlot(0, this.handler.getStackInSlot(0));
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < this.inputSlots;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.handler.getSlots(); i++) {
            this.handler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }
}
