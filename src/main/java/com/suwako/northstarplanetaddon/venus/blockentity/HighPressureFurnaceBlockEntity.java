package com.suwako.northstarplanetaddon.venus.blockentity;

import com.suwako.northstarplanetaddon.venus.machine.HighPressureRecipe;
import com.suwako.northstarplanetaddon.venus.machine.MachineEnvironment;
import com.suwako.northstarplanetaddon.venus.machine.ModRecipes;
import com.suwako.northstarplanetaddon.venus.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * 高压熔炉（设计草案第 8 章：12 SU/RPM、64 RPM、1 份/400t、容量 2 格、需 8 bar + 1200°C）。
 *
 * <p>气压来自压力罐，1200°C 来自附近热源 + 维度基础温度：
 * 熔岩 600°C、火 300°C、岩浆块 300°C、营火 200°C，金星地表额外 430°C
 * （见 {@link MachineEnvironment}）。所以「金星上 1 桶岩浆 + 1 处火」就能开炉，
 * 主世界则需要更多热源 —— 这正是草案里「地表层 · 阵地战」的资源压力。</p>
 *
 * <p>关于「容量 2 格」：草案的耐酸合金板配方是 2 钨板 + 1 硫晶 + 1 金锭（4 种材料），
 * 2 个槽放不下，所以输入缓存给到 {@value #INPUT_SLOTS} 格，把「2 格」理解为
 * <b>产物吞吐上限</b>（1 份/400t，输出缓存 2 份）。</p>
 */
public class HighPressureFurnaceBlockEntity extends AbstractMachineBlockEntity implements ContentDropping, net.minecraft.world.MenuProvider, com.suwako.northstarplanetaddon.venus.machine.WorkConsumer {

    public static final int BUFFER = 8000;
    public static final int INPUT_SLOTS = 4;
    public static final int OUTPUT_SLOT = 4;
    public static final int DEFAULT_TIME = 400;
    /** 熔炼时每 tick 消耗的空气。 */
    public static final int AIR_PER_TICK = 20;

    private final ItemStackHandler inventory = new ItemStackHandler(INPUT_SLOTS + 1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot < INPUT_SLOTS;
        }

        @Override
        protected void onContentsChanged(int slot) {
            HighPressureFurnaceBlockEntity.this.setChanged();
        }
    };

    private int progress;
    private int requiredTime = DEFAULT_TIME;
    private boolean working;

    public HighPressureFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HIGH_PRESSURE_FURNACE.get(), pos, state, BUFFER);
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
        return net.minecraft.network.chat.Component.translatable("block.northstar_planet_addon.high_pressure_furnace");
    }

    @Override
    public com.suwako.northstarplanetaddon.venus.menu.MachineMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory,
                                                      net.minecraft.world.entity.player.Player player) {
        com.suwako.northstarplanetaddon.venus.menu.MachineContainer container =
                new com.suwako.northstarplanetaddon.venus.menu.MachineContainer(this.inventory, INPUT_SLOTS);
        return new com.suwako.northstarplanetaddon.venus.menu.MachineMenu(id, inventory, container, new net.minecraft.world.inventory.ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case com.suwako.northstarplanetaddon.venus.menu.MachineMenu.DATA_PROGRESS -> HighPressureFurnaceBlockEntity.this.progress;
                    case com.suwako.northstarplanetaddon.venus.menu.MachineMenu.DATA_TOTAL -> HighPressureFurnaceBlockEntity.this.requiredTime;
                    default -> (int) (HighPressureFurnaceBlockEntity.this.getAirStorage().getBar() * 10.0F);
                };
            }
            @Override public void set(int index, int value) { }
            @Override public int getCount() { return 3; }
        });
    }
    @Override
    public boolean suppliesAir() {
        return false;
    }

    public ItemStackHandler getInventory() {
        return this.inventory;
    }

    public int getProgress() {
        return this.progress;
    }

    public int getRequiredTime() {
        return this.requiredTime;
    }

    /** 当前可用的热量（热源求和 + 维度基础温度）。 */
    public float getHeatAvailable() {
        Level level = this.level;
        if (level == null) {
            return 0.0F;
        }
        return MachineEnvironment.heatAt(level, this.worldPosition)
                + MachineEnvironment.baseDimensionTemperature(level);
    }

    private CraftingInput asCraftingInput() {
        List<ItemStack> stacks = new ArrayList<>(INPUT_SLOTS);
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            stacks.add(this.inventory.getStackInSlot(slot));
        }
        return CraftingInput.of(INPUT_SLOTS, 1, stacks);
    }

    private HighPressureRecipe findRecipe() {
        Level level = this.level;
        if (level == null) {
            return null;
        }
        return level.getRecipeManager()
                .getRecipeFor(ModRecipes.highPressureType(), this.asCraftingInput(), level)
                .map(holder -> holder.value())
                .orElse(null);
    }

    public boolean insertIngredient(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            ItemStack existing = this.inventory.getStackInSlot(slot);
            if (existing.isEmpty()) {
                this.inventory.setStackInSlot(slot, stack.copyWithCount(1));
                return true;
            }
            if (ItemStack.isSameItemSameComponents(existing, stack)
                    && existing.getCount() < existing.getMaxStackSize()) {
                existing.grow(1);
                this.inventory.setStackInSlot(slot, existing);
                return true;
            }
        }
        return false;
    }

    public ItemStack collectOutput() {
        ItemStack output = this.inventory.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return ItemStack.EMPTY;
        }
        this.inventory.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
        return output;
    }

    public boolean hasOutput() {
        return !this.inventory.getStackInSlot(OUTPUT_SLOT).isEmpty();
    }

    @Override
    protected boolean isWorking() {
        return this.working;
    }

    @Override
    protected boolean tickMachine() {
        boolean changed = false;

        if (this.getAirStorage().getBar() < 8.0F) {
            if (this.pullAirFromNeighbors(800) > 0) {
                changed = true;
            }
        }

        HighPressureRecipe recipe = this.findRecipe();
        if (recipe == null) {
            if (this.working) {
                this.working = false;
                this.progress = 0;
                return true;
            }
            return changed;
        }

        if (!this.working) {
            if (!this.canAfford(recipe)) {
                return changed;
            }
            this.working = true;
            this.progress = 0;
            this.requiredTime = Math.max(20, recipe.time());
            return true;
        }

        // 运行中：气压或温度掉下来就暂停（不丢进度）
        if (!this.canAfford(recipe)) {
            return changed;
        }
        this.getAirStorage().extract(AIR_PER_TICK, false);
        this.progress++;
        if (this.workTicks > 0) {
            this.progress++;
            this.workTicks--;
        }
        changed = true;

        if (this.progress >= this.requiredTime) {
            this.finish(recipe);
            this.progress = 0;
            this.working = false;
        }
        return changed;
    }

    private boolean canAfford(HighPressureRecipe recipe) {
        if (!this.getAirStorage().hasBar(recipe.bar())) {
            return false;
        }
        if (this.getHeatAvailable() < recipe.temperature()) {
            return false;
        }
        ItemStack output = this.inventory.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(output, recipe.result())
                && output.getCount() + recipe.result().getCount() <= output.getMaxStackSize();
    }

    private void finish(HighPressureRecipe recipe) {
        // 配方要求「原料件数 == 非空输入槽件数」，所以每个非空槽扣 1 个即可
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            ItemStack stack = this.inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                stack.shrink(1);
                this.inventory.setStackInSlot(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
            }
        }
        ItemStack output = this.inventory.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) {
            this.inventory.setStackInSlot(OUTPUT_SLOT, recipe.result().copy());
        } else {
            output.grow(recipe.result().getCount());
            this.inventory.setStackInSlot(OUTPUT_SLOT, output);
        }
    }

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
        tag.putInt("PhosphorosRequiredTime", this.requiredTime);
        tag.putBoolean("PhosphorosWorking", this.working);
        tag.put("PhosphorosInventory", this.inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.progress = tag.getInt("PhosphorosProgress");
        this.requiredTime = Math.max(20, tag.getInt("PhosphorosRequiredTime"));
        this.working = tag.getBoolean("PhosphorosWorking");
        if (tag.contains("PhosphorosInventory")) {
            this.inventory.deserializeNBT(registries, tag.getCompound("PhosphorosInventory"));
        }
    }
}
