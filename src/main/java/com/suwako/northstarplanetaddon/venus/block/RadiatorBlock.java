package com.suwako.northstarplanetaddon.venus.block;

import com.suwako.northstarplanetaddon.venus.blockentity.RadiatorBlockEntity;
import com.suwako.northstarplanetaddon.venus.machine.MachineEnvironment;
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
 * 散热器：右击投入雪球 / 冰 / 浮冰 / 蓝冰续冷却，空手右击查看状态。
 */
public class RadiatorBlock extends AbstractMachineBlock {

    public RadiatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadiatorBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof RadiatorBlockEntity radiator)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!radiator.addCoolant(stack)) {
            player.displayClientMessage(Component.translatable("message.northstar_planet_addon.radiator_reject")
                    .withStyle(ChatFormatting.RED), true);
            return ItemInteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.displayClientMessage(Component.translatable("message.northstar_planet_addon.radiator_added",
                        radiator.getCoolantTicks() / 20)
                .withStyle(ChatFormatting.GREEN), true);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof RadiatorBlockEntity radiator)) {
            return InteractionResult.PASS;
        }
        float ambient = MachineEnvironment.temperatureAt(level, pos);
        String ambientText = String.format("%.0f", ambient);
        String coolingText = String.format("%.0f", MachineEnvironment.COOLING_PER_BLOCK);
        net.minecraft.network.chat.MutableComponent message = radiator.isCooling()
                ? net.minecraft.network.chat.Component.translatable("message.northstar_planet_addon.radiator_active",
                        radiator.getCoolantTicks() / 20, ambientText, coolingText)
                : net.minecraft.network.chat.Component.translatable("message.northstar_planet_addon.radiator_idle",
                        ambientText, coolingText);
        player.displayClientMessage(message.withStyle(radiator.isCooling()
                ? ChatFormatting.AQUA : ChatFormatting.GRAY), true);
        return InteractionResult.SUCCESS;
    }
}
