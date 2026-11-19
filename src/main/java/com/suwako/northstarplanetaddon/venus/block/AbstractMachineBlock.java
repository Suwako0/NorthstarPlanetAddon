package com.suwako.northstarplanetaddon.venus.block;

import com.suwako.northstarplanetaddon.venus.blockentity.AbstractMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * 气动机器方块基类：负责挂 ticker 与「被破坏时掉出内容物」的统一实现。
 *
 * <p>不需要 GUI：交互全部走方块右击（见各子类），
 * 这样 M3 阶段不必先做 Menu/Screen，也能完整验证第 8 章的数值。</p>
 */
public abstract class AbstractMachineBlock extends Block implements EntityBlock {

    protected AbstractMachineBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public abstract BlockEntity newBlockEntity(BlockPos pos, BlockState state);

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                 BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) -> {
            com.suwako.northstarplanetaddon.venus.compat.MachineTickerBridge.tickCreateMotor(level, pos, state, blockEntity);
            if (blockEntity instanceof AbstractMachineBlockEntity machine) {
                machine.serverTick();
            }
        };
    }

    /** 机器掉落后保留内部物品（高压锅 / 高压熔炉的原料与产品）。 */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof com.suwako.northstarplanetaddon.venus.blockentity.ContentDropping dropping) {
                dropping.dropContents();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
