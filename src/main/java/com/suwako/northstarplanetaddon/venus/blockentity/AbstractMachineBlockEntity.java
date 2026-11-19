package com.suwako.northstarplanetaddon.venus.blockentity;

import com.suwako.northstarplanetaddon.venus.config.PhosphorosConfig;
import com.suwako.northstarplanetaddon.venus.machine.MachineEnvironment;
import com.suwako.northstarplanetaddon.venus.machine.PneumaticStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 气动机器基类（含第 8 章的过热机制）。
 *
 * <h2>气动约定</h2>
 * <ul>
 *   <li>空气按 <b>1 bar = 1000 mB</b> 换算，机器之间通过相邻方块直接推/拉
 *       （不引入新 capability，避免和 Create 的流体/应力 capability 打架）；</li>
 *   <li>每 tick 由 {@link com.suwako.northstarplanetaddon.venus.block.AbstractMachineBlock} 的 ticker 调 {@link #serverTick()}，
 *       子类只实现 {@link #tickMachine()} / {@link #isWorking()}；</li>
 *   <li>所有状态变化都 {@code setChanged()} ，掉方块时由方块负责掉出内容物。</li>
 * </ul>
 *
 * <h2>过热</h2>
 * <p>工作状态下若环境温度持续高于 {@code overheatTemperature}（默认 400°C），
 * 累计 {@code overheatTicks}（默认 1200 tick = 60s）后停机：冒黑烟 + 报警音，
 * 直到温度回落到阈值以下再冷却 {@code overheatCooldownTicks}（默认 200 tick）才恢复。</p>
 */
public abstract class AbstractMachineBlockEntity extends BlockEntity {

    private static final int ENVIRONMENT_CHECK_INTERVAL = 20;
    private static final int SMOKE_INTERVAL = 40;

    private final PneumaticStorage air;

    private int envTimer;
    private float ambientTemperature = MachineEnvironment.BASE_TEMPERATURE;
    private int overheatTimer;
    private boolean overheated;
    private int cooldownTimer;

    protected AbstractMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int airCapacity) {
        super(type, pos, state);
        this.air = new PneumaticStorage(airCapacity);
    }

    public PneumaticStorage getAirStorage() {
        return this.air;
    }

    /** 是否允许邻居把空气推进来（压缩机不收空气）。 */
    public boolean acceptsAir() {
        return true;
    }

    /** 是否愿意把空气推给邻居（高压锅是纯消费者）。 */
    public boolean suppliesAir() {
        return true;
    }

    /** 是否处于「工作」状态：只有工作时才会累积过热。 */
    protected boolean isWorking() {
        return false;
    }

    public final void serverTick() {
        Level level = this.level;
        if (level == null || level.isClientSide) {
            return;
        }
        this.tickOverheat(level);
        if (this.overheated) {
            return;
        }
        if (this.tickMachine()) {
            this.setChanged();
        }
    }

    private void tickOverheat(Level level) {
        if (com.suwako.northstarplanetaddon.venus.compat.MachineEnvBridge.isCooledByRegulator(level, this.worldPosition)) {
            this.overheatTimer = 0;
            this.overheated = false;
            return;
        }

        if (--this.envTimer <= 0) {
            this.envTimer = ENVIRONMENT_CHECK_INTERVAL;
            this.ambientTemperature = MachineEnvironment.temperatureAt(level, this.worldPosition);
        }
        if (!PhosphorosConfig.ENABLE_OVERHEAT.get()) {
            this.overheatTimer = 0;
            this.overheated = false;
            return;
        }

        float threshold = (float) (double) PhosphorosConfig.OVERHEAT_TEMPERATURE.get();
        boolean hot = this.ambientTemperature > threshold;

        if (!this.overheated) {
            if (hot && this.isWorking()) {
                // 逐 tick 累加：overheatTicks 的语义就是 tick 数（默认 1200 = 60s）
                this.overheatTimer++;
                if (this.overheatTimer >= PhosphorosConfig.OVERHEAT_TICKS.get()) {
                    this.overheated = true;
                    this.cooldownTimer = 0;
                    this.overheatTimer = PhosphorosConfig.OVERHEAT_TICKS.get();
                    this.onOverheat();
                }
            } else if (this.overheatTimer > 0) {
                // 停机或降温后按 2 倍速回落
                this.overheatTimer = Math.max(0, this.overheatTimer - 2);
            }
            return;
        }

        // 已停机：温度回落后开始冷却（同样按 tick 计）
        if (!hot) {
            this.cooldownTimer++;
            if (this.cooldownTimer >= PhosphorosConfig.OVERHEAT_COOLDOWN_TICKS.get()) {
                this.overheated = false;
                this.overheatTimer = 0;
                this.cooldownTimer = 0;
            }
        } else {
            this.cooldownTimer = 0;
        }
        if (level instanceof ServerLevel serverLevel && level.getGameTime() % SMOKE_INTERVAL == 0L) {
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 1.1D,
                    this.worldPosition.getZ() + 0.5D, 6, 0.3D, 0.2D, 0.3D, 0.01D);
            serverLevel.playSound(null, this.worldPosition, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS,
                    0.6F, 0.5F);
        }
    }

    protected void onOverheat() {
        if (this.level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.6D,
                    this.worldPosition.getZ() + 0.5D, 2, 0.2D, 0.2D, 0.2D, 0.0D);
            serverLevel.playSound(null, this.worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS,
                    0.8F, 0.5F);
        }
    }

    public boolean isOverheated() {
        return this.overheated;
    }

    public float getAmbientTemperature() {
        return this.ambientTemperature;
    }

    /** 0.0–1.0 的过热进度，供交互提示使用。 */
    public float getOverheatProgress() {
        int limit = PhosphorosConfig.OVERHEAT_TICKS.get();
        return limit <= 0 ? 0.0F : Math.min(1.0F, this.overheatTimer / (float) limit);
    }

    /**
     * @return 是否发生了状态变化（决定要不要 setChanged）
     */
    protected abstract boolean tickMachine();

    /**
     * 把自身空气推给相邻的、愿意接收的机器。
     *
     * @return 实际推出的总量
     */
    protected int pushAirToNeighbors(int maxPerNeighbor) {
        Level level = this.level;
        if (level == null || !this.suppliesAir() || this.air.isEmpty()) {
            return 0;
        }
        int totalPushed = 0;
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
            if (this.air.isEmpty()) {
                break;
            }
            BlockEntity neighbor = level.getBlockEntity(this.worldPosition.relative(direction));
            if (!(neighbor instanceof AbstractMachineBlockEntity other)
                    || other == this
                    || !other.acceptsAir()) {
                continue;
            }
            int taken = this.air.extract(maxPerNeighbor, true);
            if (taken <= 0) {
                continue;
            }
            int accepted = other.getAirStorage().receive(taken, true);
            if (accepted <= 0) {
                continue;
            }
            int moved = Math.min(accepted, this.air.extract(accepted, false));
            if (moved > 0) {
                other.getAirStorage().receive(moved, false);
                other.setChanged();
                totalPushed += moved;
            }
        }
        return totalPushed;
    }

    /**
     * 从相邻机器抽空气（高压锅 / 高压熔炉用）。
     *
     * @return 实际抽到的量
     */
    protected int pullAirFromNeighbors(int maxPerNeighbor) {
        Level level = this.level;
        if (level == null || !this.acceptsAir()) {
            return 0;
        }
        int totalPulled = 0;
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
            int missing = this.air.getCapacity() - this.air.getAir();
            if (missing <= 0) {
                break;
            }
            BlockEntity neighbor = level.getBlockEntity(this.worldPosition.relative(direction));
            if (!(neighbor instanceof AbstractMachineBlockEntity other)
                    || other == this
                    || !other.suppliesAir()) {
                continue;
            }
            int taken = other.getAirStorage().extract(Math.min(maxPerNeighbor, missing), false);
            if (taken > 0) {
                this.air.receive(taken, false);
                other.setChanged();
                totalPulled += taken;
            }
        }
        return totalPulled;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("PhosphorosAir", this.air.getAir());
        tag.putInt("PhosphorosOverheatTimer", this.overheatTimer);
        tag.putBoolean("PhosphorosOverheated", this.overheated);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.air.setAir(tag.getInt("PhosphorosAir"));
        this.overheatTimer = tag.getInt("PhosphorosOverheatTimer");
        this.overheated = tag.getBoolean("PhosphorosOverheated");
    }
}
