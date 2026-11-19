package com.suwako.northstarplanetaddon.venus.block;

import com.suwako.northstarplanetaddon.venus.blockentity.PressureTankBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 压力罐：空手右击查看本罐与整列的储量（1 bar = 1000 mB）。
 */
public class PressureTankBlock extends AbstractMachineBlock {

    public PressureTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PressureTankBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof PressureTankBlockEntity tank)) {
            return InteractionResult.PASS;
        }
        player.displayClientMessage(Component.translatable("message.northstar_planet_addon.tank_gauge",
                        String.format("%.1f", tank.getAirStorage().getBar()),
                        tank.getAirStorage().getAir(), tank.getAirStorage().getCapacity(),
                        tank.getColumnAir(), tank.getColumnCapacity())
                .withStyle(ChatFormatting.AQUA), true);
        return InteractionResult.SUCCESS;
    }
}
