package com.suwako.northstarplanetaddon.venus.compat.create;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

/**
 * Reflection bridge to the Create kinetic layer.
 *
 * <p>Why reflection: the kinetic classes can only be compiled when the Create jar
 * (plus Flywheel / Registrate) is on the build classpath. Using reflection here keeps
 * the main sources compilable even when that group is absent, and it still enables the
 * integration at runtime whenever the modpack has Create 6 installed.</p>
 */
public final class CreateReflect {

    private static final String BRIDGE = "com.suwako.northstarplanetaddon.venus.compat.create.CreateMachineBridge";
    private static boolean resolved;
    private static Method motorTypeMethod;
    private static Method tickMotorMethod;

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        if (!ModList.get().isLoaded("create")) {
            return;
        }
        try {
            Class<?> bridge = Class.forName(BRIDGE);
            motorTypeMethod = bridge.getMethod("motorType");
            tickMotorMethod = bridge.getMethod("tickMotor", Level.class, BlockPos.class, BlockState.class,
                    BlockEntity.class);
        } catch (Throwable throwable) {
            com.suwako.northstarplanetaddon.venus.VenusModule.LOGGER.warn("[VenusModule] Create bridge unavailable: {}", throwable.toString());
            motorTypeMethod = null;
            tickMotorMethod = null;
        }
    }

    /** Create kinetic motor block entity type, or null when Create is absent. */
    public static BlockEntityType<?> motorType() {
        resolve();
        if (motorTypeMethod == null) {
            return null;
        }
        try {
            return (BlockEntityType<?>) motorTypeMethod.invoke(null);
        } catch (Throwable throwable) {
            com.suwako.northstarplanetaddon.venus.VenusModule.LOGGER.warn("[VenusModule] Create motor type failed: {}", throwable.toString());
            return null;
        }
    }

    /** True when this block entity is a Create kinetic motor (Create ticks it itself). */
    public static boolean tickMotor(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        resolve();
        if (tickMotorMethod == null) {
            return false;
        }
        try {
            return (Boolean) tickMotorMethod.invoke(null, level, pos, state, blockEntity);
        } catch (Throwable throwable) {
            return false;
        }
    }

    private CreateReflect() {
    }
}