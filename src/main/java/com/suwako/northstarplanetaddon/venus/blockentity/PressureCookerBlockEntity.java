package com.suwako.northstarplanetaddon.venus.blockentity;

import com.suwako.northstarplanetaddon.venus.config.PhosphorosConfig;
import com.suwako.northstarplanetaddon.venus.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * 高压锅（设计草案第 8 章：6 SU/RPM / 32 RPM / 4 份每 200 tick / 需 8 bar）。
 *
 * <p>无 GUI 版交互：</p>
 * <ul>
 *   <li>手里拿可熔炼的东西右击 → 塞进 4 个原料槽（一次最多 4 份）；</li>
 *   <li>空手右击 → 查看进度 / 气压，并取出已完成的成品；</li>
 *   <li>原料凑满 4 份且气压 ≥ 8 bar 时自动开工，200 tick 内每 tick 消耗 20 mB 空气。</li>
 * </ul>
 */
public class PressureCookerBlockEntity extends AbstractMachineBlockEntity implements ContentDropping, net.minecraft.world.MenuProvider, com.suwako.northstarplanetaddon.venus.machine.WorkConsumer {

    public static final int BUFFER = 8000;
    public static final int INPUT_SLOTS = 4;
    public static final int OUTPUT_SLOT = INPUT_SLOTS;
    public static final int COOK_TICKS = 200;
    /** 烹饪时每 tick 消耗的空气。 */
    public static final int AIR_PER_TICK = 20;

    private final ItemStackHandler inventory = new ItemStackHandler(INPUT_SLOTS + 1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot < INPUT_SLOTS && PressureCookerBlockEntity.this.smeltResult(stack) != ItemStack.EMPTY;
        }

        @Override
        protected void onContentsChanged(int slot) {
            PressureCookerBlockEntity.this.setChanged();
        }
    };

    private int progress;
    private boolean cooking;

    public PressureCookerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRESSURE_COOKER.get(), pos, state, BUFFER);
    }

    private int workTicks;

    @Override
    public boolean canAcceptWork() {
        return true;
    }

    @Override
    public float acceptWork(int rpm) {
        this.workTicks = 20;
        return rpm >= 16 ? 2.0F : 1.0F;
    }

    /** 有机械功时每 tick 额外推进一次进度（等价 ×2 速度）。 */
    private void applyWorkBoost() {
        if (this.workTicks > 0) {
            this.workTicks--;
        }
    }
    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("block.northstar_planet_addon.pressure_cooker");
    }

    @Override
    public com.suwako.northstarplanetaddon.venus.menu.MachineMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory,
                                                      net.minecraft.world.entity.player.Player player) {
        com.suwako.northstarplanetaddon.venus.menu.MachineContainer container =
                new com.suwako.northstarplanetaddon.venus.menu.MachineContainer(this.inventory, INPUT_SLOTS);
        return new com.suwako.northstarplanetaddon.venus.menu.MachineMenu(id, inventory, container, new net.minecraft.world.inventory.ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case com.suwako.northstarplanetaddon.venus.menu.MachineMenu.DATA_PROGRESS -> PressureCookerBlockEntity.this.progress;
                    case com.suwako.northstarplanetaddon.venus.menu.MachineMenu.DATA_TOTAL -> COOK_TICKS;
                    default -> (int) (PressureCookerBlockEntity.this.getAirStorage().getBar() * 10.0F);
                };
            }
            @Override public void set(int index, int value) { }
            @Override public int getCount() { return 3; }
        });
    }
    @Override
    public boolean suppliesAir() {
        return false; // 纯消费者
    }

    @Override
    protected boolean isWorking() {
        return this.cooking;
    }

    public ItemStackHandler getInventory() {
        return this.inventory;
    }

    public int getProgress() {
        return this.progress;
    }

    public boolean isCooking() {
        return this.cooking;
    }

    public ItemStack smeltResult(ItemStack input) {
        Level level = this.level;
        if (level == null || input.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), level)
                .map(holder -> holder.value().getResultItem(level.registryAccess()).copy())
                .orElse(ItemStack.EMPTY);
    }

    /** 右击塞入原料，返回是否成功。 */
    public boolean insertIngredient(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ItemStack single = stack.copyWithCount(1);
        if (this.smeltResult(single).isEmpty()) {
            return false;
        }
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            ItemStack existing = this.inventory.getStackInSlot(slot);
            if (existing.isEmpty()) {
                this.inventory.setStackInSlot(slot, single);
                return true;
            }
        }
        return false;
    }

    /** 空手右击：取出成品。 */
    public ItemStack collectOutput() {
        ItemStack output = this.inventory.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return ItemStack.EMPTY;
        }
        this.inventory.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
        return output;
    }

    public boolean isInputFull() {
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            if (this.inventory.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean tickMachine() {
        boolean changed = false;

        // 气压不足时从相邻压力罐抽取
        float requiredBar = (float) (double) PhosphorosConfig.PRESSURE_COOKER_BAR.get();
        if (this.getAirStorage().getBar() < requiredBar) {
            if (this.pullAirFromNeighbors(400) > 0) {
                changed = true;
            }
        }

        if (!this.cooking) {
            if (this.isInputFull() && this.getAirStorage().hasBar(requiredBar) && this.canAcceptOutput()) {
                this.cooking = true;
                this.progress = 0;
                changed = true;
            }
            return changed;
        }

        // 烹饪中：气压断了就暂停（不丢进度）
        if (!this.getAirStorage().hasBar(1.0F)) {
            return changed;
        }
        this.getAirStorage().extract(AIR_PER_TICK, false);
        this.progress++;
        if (this.workTicks > 0) {
            this.progress++;
            this.workTicks--;
        }
        changed = true;

        if (this.progress >= COOK_TICKS) {
            this.finishCooking();
            this.progress = 0;
            this.cooking = false;
        }
        return changed;
    }

    private void finishCooking() {
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            ItemStack input = this.inventory.getStackInSlot(slot);
            if (input.isEmpty()) {
                continue;
            }
            ItemStack result = this.smeltResult(input);
            if (result.isEmpty()) {
                continue;
            }
            this.mergeIntoOutput(result);
            this.inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    private void mergeIntoOutput(ItemStack result) {
        ItemStack output = this.inventory.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) {
            this.inventory.setStackInSlot(OUTPUT_SLOT, result.copy());
            return;
        }
        if (ItemStack.isSameItemSameComponents(output, result)) {
            int merged = Math.min(output.getMaxStackSize(), output.getCount() + result.getCount());
            output.setCount(merged);
            this.inventory.setStackInSlot(OUTPUT_SLOT, output);
        }
    }

    private boolean canAcceptOutput() {
        ItemStack output = this.inventory.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return true;
        }
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            ItemStack result = this.smeltResult(this.inventory.getStackInSlot(slot));
            if (result.isEmpty()) {
                continue;
            }
            if (!ItemStack.isSameItemSameComponents(output, result)) {
                return false;
            }
            if (output.getCount() + result.getCount() > output.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    /** 方块被破坏时掉出内容物。 */
    @Override
    public void dropContents() {
        Level level = this.level;
        if (level == null || level.isClientSide) {
            return;
        }
        for (int slot = 0; slot < this.inventory.getSlots(); slot++) {
            ItemStack stack = this.inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(level,
                        this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(), stack);
                this.inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("PhosphorosProgress", this.progress);
        tag.putBoolean("PhosphorosCooking", this.cooking);
        tag.put("PhosphorosInventory", this.inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.progress = tag.getInt("PhosphorosProgress");
        this.cooking = tag.getBoolean("PhosphorosCooking");
        if (tag.contains("PhosphorosInventory")) {
            this.inventory.deserializeNBT(registries, tag.getCompound("PhosphorosInventory"));
        }
    }
}
