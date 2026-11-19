package com.suwako.northstarplanetaddon.venus.blockentity;

import com.suwako.northstarplanetaddon.venus.config.PhosphorosConfig;
import com.suwako.northstarplanetaddon.venus.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 压缩机（设计草案第 8 章：8 SU/RPM / 64 RPM / 100 mB 空气每 tick）。
 *
 * <p>M3 阶段先做「手动曲柄」：右击一次 = 一个 64 RPM 转速下的 2 秒产出
 * （默认 2000 mB，即 2 bar），冷却 40 tick。
 * {@code compat/create} 的应力适配层接上之后，同一份 {@code receiveAir} 逻辑
 * 会被 Create 的 KineticBlockEntity 每 tick 调用，届时曲柄变为备用手段。</p>
 */
public class CompressorBlockEntity extends AbstractMachineBlockEntity {

    /** 机器自身缓存（够喂 1 tick 满产）。 */
    public static final int BUFFER = 4000;
    public static final int CRANK_COOLDOWN_TICKS = 40;

    private int crankTicks;

    public CompressorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPRESSOR.get(), pos, state, BUFFER);
    }

    @Override
    public boolean acceptsAir() {
        return false; // 压缩机是气源，不接受回灌
    }

    /** 由方块右击调用。 */
    public boolean crank() {
        if (this.crankTicks > 0) {
            return false;
        }
        int produced = PhosphorosConfig.COMPRESSOR_CRANK_AIR.get();
        int accepted = this.getAirStorage().receive(produced, false);
        if (accepted <= 0) {
            return false;
        }
        this.crankTicks = CRANK_COOLDOWN_TICKS;
        this.setChanged();
        return true;
    }

    public int getCrankCooldown() {
        return this.crankTicks;
    }

    @Override
    protected boolean tickMachine() {
        boolean changed = false;
        if (this.crankTicks > 0) {
            this.crankTicks--;
            changed = true;
        }
        int pushed = this.pushAirToNeighbors(PhosphorosConfig.COMPRESSOR_OUTPUT_PER_TICK.get());
        return changed || pushed > 0;
    }
}
