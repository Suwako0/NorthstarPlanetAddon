package com.suwako.northstarplanetaddon.venus.item;

import com.suwako.northstarplanetaddon.venus.blockentity.AbstractMachineBlockEntity;
import com.suwako.northstarplanetaddon.venus.config.PhosphorosConfig;
import com.suwako.northstarplanetaddon.venus.event.PlayerEvents;
import com.suwako.northstarplanetaddon.venus.machine.MachineEnvironment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 气压计（设计草案 6.3 节）：显示气压 / 温度 / 腐蚀值，并预报硫酸季风。
 *
 * <p>草案要求 HUD；本轮先做「右键读数」版本（避免引入客户端 HUD 渲染管线），
 * 读数包含：附近机器最高气压、所在位置环境温度、玩家腐蚀值、以及按
 * {@code monsoonIntervalDays} 推算的季风日期。</p>
 */
public class BarometerItem extends Item {

    public static final int SCAN_RADIUS = 16;

    public BarometerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        BlockPos pos = player.blockPosition();
        float temperature = MachineEnvironment.temperatureAt(level, pos);
        float pressure = 0.0F;
        for (BlockPos candidate : BlockPos.betweenClosed(
                pos.offset(-SCAN_RADIUS, -8, -SCAN_RADIUS), pos.offset(SCAN_RADIUS, 8, SCAN_RADIUS))) {
            BlockEntity blockEntity = level.getBlockEntity(candidate);
            if (blockEntity instanceof AbstractMachineBlockEntity machine) {
                pressure = Math.max(pressure, machine.getAirStorage().getBar());
            }
        }

        float northstarTemp = com.suwako.northstarplanetaddon.venus.compat.MachineEnvBridge.ambientTemperature(level, pos);
        if (!Float.isNaN(northstarTemp)) {
            temperature = northstarTemp;
        }
        int corrosion = player.getPersistentData().getInt(PlayerEvents.CORROSION_KEY);
        long day = level.getDayTime() / 24000L;
        int interval = Math.max(1, PhosphorosConfig.MONSOON_INTERVAL_DAYS.get());
        long daysUntilMonsoon = interval - Math.floorMod(day, interval);

        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer,
                    new com.suwako.northstarplanetaddon.venus.network.BarometerPayload(pressure, temperature,
                            corrosion, (int) daysUntilMonsoon));
        }
        return InteractionResultHolder.success(stack);
    }
}
