package com.suwako.northstarplanetaddon.venus.block;

import com.suwako.northstarplanetaddon.venus.blockentity.AbstractMachineBlockEntity;
import com.suwako.northstarplanetaddon.venus.machine.WorkConsumer;
import com.suwako.northstarplanetaddon.venus.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 气动马达（第 8 章：消耗 20 mB/t，输出 32 RPM）。
 *
 * <p>轴向与 Create 传动轴一致（axis 三态），装配方向决定轴孔朝向。</p>
 */
public class PneumaticMotorBlock extends AbstractMachineBlock {

    public static final int RPM = 32;
    public static final int AIR_PER_TICK = 20;

    /** 与 Create 传动轴一致的轴向。 */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    public PneumaticMotorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.Y));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        switch (rotation) {
            case COUNTERCLOCKWISE_90:
            case CLOCKWISE_90:
                return switch (state.getValue(AXIS)) {
                    case X -> state.setValue(AXIS, Direction.Axis.Z);
                    case Z -> state.setValue(AXIS, Direction.Axis.X);
                    default -> state;
                };
            default:
                return state;
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return com.suwako.northstarplanetaddon.venus.registry.ModBlockEntities.PNEUMATIC_MOTOR.get().create(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof Entity motor) {
            player.displayClientMessage(Component.translatable("message.northstar_planet_addon.motor_status",
                            String.format("%.1f", motor.getAirStorage().getBar()), motor.getRpm(),
                            motor.isLinked() ? "linked" : "idle")
                    .withStyle(ChatFormatting.AQUA), true);
        }
        return InteractionResult.SUCCESS;
    }

    /** 气动马达方块实体。 */
    public static class Entity extends AbstractMachineBlockEntity {

        public static final int BUFFER = 8000;
        private int rpm;
        private boolean linked;

        public Entity(BlockPos pos, BlockState state) {
            super(ModBlockEntities.PNEUMATIC_MOTOR.get(), pos, state, BUFFER);
        }

        @Override
        public boolean suppliesAir() {
            return false;
        }

        public int getRpm() {
            return this.rpm;
        }

        public boolean isLinked() {
            return this.linked;
        }

        @Override
        protected boolean isWorking() {
            return this.rpm > 0;
        }

        @Override
        protected boolean tickMachine() {
            this.rpm = 0;
            this.linked = false;
            if (this.level == null) {
                return false;
            }
            if (this.getAirStorage().getBar() < 0.1F && this.pullAirFromNeighbors(400) <= 0) {
                return false;
            }
            WorkConsumer consumer = WorkConsumer.findNeighbor(this.level, this.worldPosition);
            if (consumer == null) {
                return false;
            }
            this.getAirStorage().extract(AIR_PER_TICK, false);
            consumer.acceptWork(RPM);
            this.rpm = RPM;
            this.linked = true;
            return true;
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putInt("PhosphorosRpm", this.rpm);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            this.rpm = tag.getInt("PhosphorosRpm");
        }
    }
}