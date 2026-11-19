package com.suwako.northstarplanetaddon.venus.blockentity;

import com.suwako.northstarplanetaddon.venus.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 散热器（设计草案第 8 章：2 SU/RPM、16 RPM、9×5×9 内降温 40°C）。
 *
 * <p>草案里它是被 Create 驱动的机械；本阶段（气动先行）把「转动」换成
 * <b>消耗冷却剂</b>：雪球/冰/浮冰/蓝冰各自提供一段冷却时长，
 * 只要还有冷却剂，就给它周围 9×5×9 内的气动机器各提供 −40°C。</p>
 *
 * <p>它自己不参与过热累积（{@link #isWorking()} 恒为 false），
 * 否则金星地表 430°C 会把降温设备本身先烤停机。</p>
 */
public class RadiatorBlockEntity extends AbstractMachineBlockEntity implements ContentDropping {

    public static final int MAX_COOLANT_TICKS = 6400;
    private static final int SNOWBALL_TICKS = 100;
    private static final int ICE_TICKS = 400;
    private static final int PACKED_ICE_TICKS = 800;
    private static final int BLUE_ICE_TICKS = 1600;

    private int coolantTicks;

    public RadiatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADIATOR.get(), pos, state, 0);
    }

    @Override
    public boolean acceptsAir() {
        return false;
    }

    @Override
    public boolean suppliesAir() {
        return false;
    }

    @Override
    protected boolean isWorking() {
        return false;
    }

    public boolean isCooling() {
        return this.coolantTicks > 0;
    }

    public int getCoolantTicks() {
        return this.coolantTicks;
    }

    /** 每个冷却剂物品能换多少 tick 的冷却。 */
    public static int coolantValue(ItemStack stack) {
        if (stack.is(Items.SNOWBALL)) {
            return SNOWBALL_TICKS;
        }
        if (stack.is(Items.ICE)) {
            return ICE_TICKS;
        }
        if (stack.is(Items.PACKED_ICE)) {
            return PACKED_ICE_TICKS;
        }
        if (stack.is(Items.BLUE_ICE)) {
            return BLUE_ICE_TICKS;
        }
        return 0;
    }

    /** 右击投入冷却剂，返回是否成功。 */
    public boolean addCoolant(ItemStack stack) {
        int ticks = coolantValue(stack);
        if (ticks <= 0 || this.coolantTicks >= MAX_COOLANT_TICKS) {
            return false;
        }
        this.coolantTicks = Math.min(MAX_COOLANT_TICKS, this.coolantTicks + ticks);
        this.setChanged();
        return true;
    }

    @Override
    protected boolean tickMachine() {
        if (this.coolantTicks <= 0) {
            return false;
        }
        this.coolantTicks--;
        Level level = this.level;
        if (level instanceof ServerLevel serverLevel && level.getGameTime() % 20L == 0L) {
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 1.05D,
                    this.worldPosition.getZ() + 0.5D, 2, 0.25D, 0.05D, 0.25D, 0.01D);
        }
        return true;
    }

    @Override
    public void dropContents() {
        // 无物品栏：冷却剂按剩余时长折算成冰（每 400 tick = 1 个冰）
        Level level = this.level;
        if (level == null || level.isClientSide || this.coolantTicks <= 0) {
            return;
        }
        int iceCount = Math.max(1, this.coolantTicks / ICE_TICKS);
        net.minecraft.world.Containers.dropItemStack(level,
                this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(),
                new ItemStack(Items.ICE, Math.min(16, iceCount)));
        this.coolantTicks = 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("PhosphorosCoolant", this.coolantTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.coolantTicks = tag.getInt("PhosphorosCoolant");
    }
}
