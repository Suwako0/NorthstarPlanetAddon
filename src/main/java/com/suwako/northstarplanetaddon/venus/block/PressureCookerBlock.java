package com.suwako.northstarplanetaddon.venus.block;

import com.suwako.northstarplanetaddon.venus.blockentity.PressureCookerBlockEntity;
import com.suwako.northstarplanetaddon.venus.config.PhosphorosConfig;
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
 * 高压锅（无 GUI 版交互）：
 * <ul>
 *   <li>拿可熔炼物品右击 → 塞入原料（最多 4 份）；</li>
 *   <li>空手右击 → 取成品，没有成品就报进度与气压。</li>
 * </ul>
 */
public class PressureCookerBlock extends AbstractMachineBlock {

    public PressureCookerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PressureCookerBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof PressureCookerBlockEntity cooker)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!cooker.insertIngredient(stack)) {
            player.displayClientMessage(Component.translatable("message.northstar_planet_addon.cooker_reject")
                    .withStyle(ChatFormatting.RED), true);
            return ItemInteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.displayClientMessage(Component.translatable("message.northstar_planet_addon.cooker_inserted")
                .withStyle(ChatFormatting.GREEN), true);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof PressureCookerBlockEntity cooker)) {
            return InteractionResult.PASS;
        }

        if (!player.isShiftKeyDown()) {
            player.openMenu(cooker);
            return InteractionResult.SUCCESS;
        }
        ItemStack output = cooker.collectOutput();
        if (!output.isEmpty()) {
            if (!player.addItem(output)) {
                player.drop(output, false);
            }
            player.displayClientMessage(Component.translatable("message.northstar_planet_addon.cooker_collected",
                            output.getHoverName(), output.getCount())
                    .withStyle(ChatFormatting.GOLD), true);
            return InteractionResult.SUCCESS;
        }

        float requiredBar = (float) (double) PhosphorosConfig.PRESSURE_COOKER_BAR.get();
        if (cooker.getAirStorage().getBar() < requiredBar) {
            player.displayClientMessage(Component.translatable("message.northstar_planet_addon.cooker_need_pressure",
                            String.format("%.1f", requiredBar),
                            String.format("%.1f", cooker.getAirStorage().getBar()))
                    .withStyle(ChatFormatting.RED), true);
        } else {
            int percent = cooker.getProgress() * 100 / PressureCookerBlockEntity.COOK_TICKS;
            player.displayClientMessage(Component.translatable("message.northstar_planet_addon.cooker_progress",
                            percent, String.format("%.1f", cooker.getAirStorage().getBar()))
                    .withStyle(ChatFormatting.AQUA), true);
        }
        return InteractionResult.SUCCESS;
    }
}
