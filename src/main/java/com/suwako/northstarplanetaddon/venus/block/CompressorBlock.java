package com.suwako.northstarplanetaddon.venus.block;

import com.suwako.northstarplanetaddon.venus.blockentity.CompressorBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 压缩机：右击摇曲柄（手中拿不拿东西都行），产出压缩空气并自动流进相邻压力罐。
 */
public class CompressorBlock extends AbstractMachineBlock {

    public CompressorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CompressorBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        return this.crank(level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        ItemInteractionResult result = this.crank(level, pos, player);
        return result == ItemInteractionResult.SUCCESS ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    private ItemInteractionResult crank(Level level, BlockPos pos, Player player) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof CompressorBlockEntity compressor)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!compressor.crank()) {
            player.displayClientMessage(Component.translatable("message.northstar_planet_addon.compressor_cooldown",
                            String.format("%.1f", compressor.getAirStorage().getBar()))
                    .withStyle(ChatFormatting.GRAY), true);
            return ItemInteractionResult.FAIL;
        }
        player.displayClientMessage(Component.translatable("message.northstar_planet_addon.compressor_crank",
                        String.format("%.1f", compressor.getAirStorage().getBar()))
                .withStyle(ChatFormatting.AQUA), true);
        return ItemInteractionResult.SUCCESS;
    }
}
