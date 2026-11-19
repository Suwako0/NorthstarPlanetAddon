package com.suwako.northstarplanetaddon.venus.compat.northstar;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

/**
 * Reflection bridge to Northstar's temperature regulator.
 *
 * <p>Only enabled at runtime when the modpack has Northstar installed; the mod compiles
 * and runs fine without it.</p>
 */
public final class NorthstarReflect {

    private static final String BRIDGE = "com.suwako.northstarplanetaddon.venus.compat.northstar.NorthstarMachineBridge";
    private static boolean resolved;
    private static Method regulatorMethod;
    private static Method temperatureMethod;

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        if (!ModList.get().isLoaded("northstar")) {
            return;
        }
        try {
            Class<?> bridge = Class.forName(BRIDGE);
            regulatorMethod = bridge.getMethod("hasRegulatorNear", Level.class, BlockPos.class);
            temperatureMethod = bridge.getMethod("ambientTemperature", Level.class, BlockPos.class);
        } catch (Throwable throwable) {
            com.suwako.northstarplanetaddon.venus.VenusModule.LOGGER.warn("[VenusModule] Northstar bridge unavailable: {}", throwable.toString());
            regulatorMethod = null;
            temperatureMethod = null;
        }
    }

    public static boolean hasRegulatorNear(Level level, BlockPos pos) {
        resolve();
        if (regulatorMethod == null) {
            return false;
        }
        try {
            return (Boolean) regulatorMethod.invoke(null, level, pos);
        } catch (Throwable throwable) {
            return false;
        }
    }

    /** Northstar ambient temperature, or NaN when unavailable. */
    public static float ambientTemperature(Level level, BlockPos pos) {
        resolve();
        if (temperatureMethod == null) {
            return Float.NaN;
        }
        try {
            return (Float) temperatureMethod.invoke(null, level, pos);
        } catch (Throwable throwable) {
            return Float.NaN;
        }
    }

    private NorthstarReflect() {
    }
}