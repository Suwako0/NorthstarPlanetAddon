package com.suwako.northstarplanetaddon.venus.compat.northstar;

import com.lightning.northstar.block.tech.temperature_regulator.TemperatureRegulatorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Isolation layer for Northstar Redux: only loaded when Northstar is present.
 *
 * <p>Design chapter 8: a Northstar temperature regulator keeps this mod's machines
 * from overheating inside its working area. We detect the regulator block by type
 * within a small radius, which matches "in its range" closely enough.</p>
 */
public final class NorthstarMachineBridge {

    private static final int HORIZONTAL = 16;
    private static final int VERTICAL = 6;

    public static boolean hasRegulatorNear(Level level, BlockPos pos) {
        for (BlockPos candidate : BlockPos.betweenClosed(
                pos.offset(-HORIZONTAL, -VERTICAL, -HORIZONTAL),
                pos.offset(HORIZONTAL, VERTICAL, HORIZONTAL))) {
            if (level.getBlockState(candidate).getBlock() instanceof TemperatureRegulatorBlock) {
                return true;
            }
        }
        return false;
    }

    /** Ambient temperature (Celsius) reported by Northstar for this level/position. */
    public static float ambientTemperature(Level level, BlockPos pos) {
        try {
            return new com.lightning.northstar.world.temperature.NorthstarTemperature(level)
                    .getTemperature(pos);
        } catch (RuntimeException exception) {
            return Float.NaN;
        }
    }
    private NorthstarMachineBridge() {
    }
}