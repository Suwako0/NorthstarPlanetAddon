package com.suwako.northstarplanetaddon.venus.compat;

/**
 * Tick dispatch bridge (main package, no Create references).
 * CreateMachineBridge is only touched when Create is loaded.
 */
public final class MachineTickerBridge {

    public static boolean tickCreateMotor(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
                                          net.minecraft.world.level.block.state.BlockState state,
                                          net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
        return com.suwako.northstarplanetaddon.venus.compat.create.CreateReflect.tickMotor(level, pos, state, blockEntity);
    }

    private MachineTickerBridge() {
    }
}