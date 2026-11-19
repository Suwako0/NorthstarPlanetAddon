package com.suwako.northstarplanetaddon.venus.menu;

import com.suwako.northstarplanetaddon.venus.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 高压锅 / 高压熔炉共用的容器菜单：4 个输入槽 + 1 个输出槽 + 玩家背包。
 *
 * <p>{@link ContainerData} 暴露 3 个整数给客户端：进度、总时长、气压（bar × 10）。</p>
 */
public class MachineMenu extends AbstractContainerMenu {

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_TOTAL = 1;
    public static final int DATA_BAR = 2;
    public static final int INPUT_SLOTS = 4;
    public static final int OUTPUT_SLOT = 4;
    public static final int SLOT_COUNT = INPUT_SLOTS + 1;

    private final Container container;
    private final ContainerData data;

    /** 服务端构造（来自 MenuProvider）。 */
    public MachineMenu(int id, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenus.MACHINE.get(), id);
        this.container = container;
        this.data = data;

        // 输入 4 格（2×2）
        for (int i = 0; i < INPUT_SLOTS; i++) {
            int x = 53 + (i % 2) * 18;
            int y = 17 + (i / 2) * 18;
            this.addSlot(new Slot(container, i, x, y));
        }
        // 输出 1 格
        this.addSlot(new Slot(container, OUTPUT_SLOT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        // 玩家背包 3×9 + 快捷栏
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
        this.addDataSlots(data);
    }

    /** 客户端构造（没有方块实体时的空容器）。 */
    public static MachineMenu client(int id, Inventory playerInventory) {
        return new MachineMenu(id, playerInventory, new SimpleContainer(SLOT_COUNT), new SimpleContainerData(3));
    }

    public int getProgress() {
        return this.data.get(DATA_PROGRESS);
    }

    public int getTotalTime() {
        return Math.max(1, this.data.get(DATA_TOTAL));
    }

    /** 气压（bar），数据里存的是 bar × 10。 */
    public float getBar() {
        return this.data.get(DATA_BAR) / 10.0F;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < SLOT_COUNT) {
                if (!this.moveItemStackTo(stack, SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, INPUT_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
}
