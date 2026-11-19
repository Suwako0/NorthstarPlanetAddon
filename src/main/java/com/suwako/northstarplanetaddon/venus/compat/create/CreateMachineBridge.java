package com.suwako.northstarplanetaddon.venus.compat.create;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Isolation layer for Create integration: this class references Create types directly,
 * so it is only loaded when Create is present. Main code reaches it through
 * {@code compat/MachineTickerBridge}, which has no Create references.
 */
public final class CreateMachineBridge {

    public static BlockEntityType<KineticPneumaticMotorEntity> motorType() {
        return BlockEntityType.Builder.of(
                (pos, state) -> new KineticPneumaticMotorEntity(
                        (BlockEntityType<?>) com.suwako.northstarplanetaddon.venus.registry.ModBlockEntities.PNEUMATIC_MOTOR.get(),
                        pos, state),
                com.suwako.northstarplanetaddon.venus.registry.ModBlocks.PNEUMATIC_MOTOR.get()).build(null);
    }

    public static boolean tickMotor(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        return blockEntity instanceof KineticPneumaticMotorEntity;
    }

    private CreateMachineBridge() {
    }
}