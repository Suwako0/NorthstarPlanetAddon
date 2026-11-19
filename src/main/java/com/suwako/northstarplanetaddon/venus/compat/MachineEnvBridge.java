package com.suwako.northstarplanetaddon.venus.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

/**
 * Bridge to optional Northstar integration (main package, no Northstar references).
 * The Northstar classes are only touched when the mod is loaded.
 */
public final class MachineEnvBridge {

    private static final String NORTHSTAR_ID = "northstar";

    /** True when a Northstar temperature regulator covers this machine. */
    public static boolean isCooledByRegulator(Level level, BlockPos pos) {
        return level != null && !level.isClientSide
                && com.suwako.northstarplanetaddon.venus.compat.northstar.NorthstarReflect.hasRegulatorNear(level, pos);
    }

    /** Northstar ambient temperature in Celsius, or {@link Float#NaN} when unavailable. */
    public static float ambientTemperature(Level level, BlockPos pos) {
        if (level == null) {
            return Float.NaN;
        }
        return com.suwako.northstarplanetaddon.venus.compat.northstar.NorthstarReflect.ambientTemperature(level, pos);
    }
    private MachineEnvBridge() {
    }
}