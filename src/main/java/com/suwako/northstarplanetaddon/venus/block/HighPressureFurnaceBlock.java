package com.suwako.northstarplanetaddon.venus.block;

import com.suwako.northstarplanetaddon.venus.blockentity.HighPressureFurnaceBlockEntity;
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
 * 高压熔炉：右击投料（2 格）、空手取成品并查看状态。
 *
 * <p>状态行会显示进度、气压、可用热量与环境温度，便于玩家判断是缺气、
 * 缺热还是「过热停机」（{@code message.northstar_planet_addon.overheated}）。</p>
 */
public class HighPressureFurnaceBlock extends AbstractMachineBlock {

    public HighPressureFurnaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HighPressureFurnaceBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof HighPressureFurnaceBlockEntity furnace)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!furnace.insertIngredient(stack)) {
            player.displayClientMessage(Component.translatable("message.northstar_planet_addon.furnace_reject")
                    .withStyle(ChatFormatting.RED), true);
            return ItemInteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.displayClientMessage(Component.translatable("message.northstar_planet_addon.furnace_inserted")
                .withStyle(ChatFormatting.GREEN), true);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof HighPressureFurnaceBlockEntity furnace)) {
            return InteractionResult.PASS;
        }

        if (!player.isShiftKeyDown()) {
            player.openMenu(furnace);
            return InteractionResult.SUCCESS;
        }
        if (furnace.hasOutput()) {
            ItemStack output = furnace.collectOutput();
            if (!player.addItem(output)) {
                player.drop(output, false);
            }
            player.displayClientMessage(Component.translatable("message.northstar_planet_addon.furnace_collected",
                            output.getHoverName(), output.getCount())
                    .withStyle(ChatFormatting.GOLD), true);
            return InteractionResult.SUCCESS;
        }

        if (furnace.isOverheated()) {
            player.displayClientMessage(Component.translatable("message.northstar_planet_addon.overheated",
                            String.format("%.0f", furnace.getAmbientTemperature()))
                    .withStyle(ChatFormatting.DARK_RED), true);
            return InteractionResult.SUCCESS;
        }

        int percent = furnace.getRequiredTime() <= 0 ? 0
                : furnace.getProgress() * 100 / furnace.getRequiredTime();
        player.displayClientMessage(Component.translatable("message.northstar_planet_addon.furnace_status",
                        percent,
                        String.format("%.1f", furnace.getAirStorage().getBar()),
                        String.format("%.0f", furnace.getHeatAvailable()),
                        String.format("%.0f", furnace.getAmbientTemperature()))
                .withStyle(ChatFormatting.AQUA), true);
        return InteractionResult.SUCCESS;
    }
}
