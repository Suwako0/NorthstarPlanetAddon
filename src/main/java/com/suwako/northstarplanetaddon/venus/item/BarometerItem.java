package com.suwako.northstarplanetaddon.venus.item;

import com.suwako.northstarplanetaddon.venus.config.PhosphorosConfig;
import com.suwako.northstarplanetaddon.venus.event.PlayerEvents;
import com.suwako.northstarplanetaddon.venus.network.BarometerPayload;
import com.suwako.northstarplanetaddon.venus.world.AmbientTemperature;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 气压计：读数式仪器，右击把当前环境读数发到客户端 HUD。
 *
 * <p>显示三项：环境温度、玩家腐蚀值、距离下一次硫酸季风还有几天。
 * <b>气压读数已随气动/压力机器体系一并移除</b>（机器、压力罐都不在了，bar 没有来源）。</p>
 */
public class BarometerItem extends Item {

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
        float temperature = AmbientTemperature.temperatureAt(level, pos);
        int corrosion = player.getPersistentData().getInt(PlayerEvents.CORROSION_KEY);
        long day = level.getDayTime() / 24000L;
        int interval = Math.max(1, PhosphorosConfig.MONSOON_INTERVAL_DAYS.get());
        long daysUntilMonsoon = interval - Math.floorMod(day, interval);

        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer,
                    new BarometerPayload(temperature, corrosion, (int) daysUntilMonsoon));
        }
        return InteractionResultHolder.success(stack);
    }
}
