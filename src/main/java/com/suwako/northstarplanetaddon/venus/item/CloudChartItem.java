package com.suwako.northstarplanetaddon.venus.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * 云图仪（设计草案 6.3 节）：显示 64 格内结构方向；集齐 12 条云图残页后显示宝箱位置。
 *
 * <p>没有结构定位 API 时的实现：扫描 64 格内的容器（箱子）与遗迹方块
 * （钨块 / 硫晶块，云港遗迹的建材），报告最近目标的方向与距离。
 * 背包里 {@code cloud_map_fragment} ≥ 12 时额外报告精确坐标。</p>
 */
public class CloudChartItem extends Item {

    public static final int SCAN_RADIUS = 64;
    public static final int LOG_THRESHOLD = 12;

    public CloudChartItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        int fragments = 0;
        for (ItemStack inv : player.getInventory().items) {
            if (inv.is(com.suwako.northstarplanetaddon.venus.registry.ModItems.CLOUD_MAP_FRAGMENT.get())) {
                fragments += inv.getCount();
            }
        }

        BlockPos playerPos = player.blockPosition();
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        int containers = 0;
        for (BlockPos pos : BlockPos.betweenClosed(
                playerPos.offset(-SCAN_RADIUS, -32, -SCAN_RADIUS),
                playerPos.offset(SCAN_RADIUS, 32, SCAN_RADIUS))) {
            var state = level.getBlockState(pos);
            boolean interesting = state.is(Blocks.CHEST) || state.is(Blocks.BARREL)
                    || state.is(com.suwako.northstarplanetaddon.venus.registry.ModBlocks.TUNGSTEN_BLOCK.get())
                    || state.is(com.suwako.northstarplanetaddon.venus.registry.ModBlocks.SULFUR_CRYSTAL_BLOCK.get());
            if (!interesting) {
                continue;
            }
            if (state.is(Blocks.CHEST) || state.is(Blocks.BARREL)) {
                containers++;
            }
            double distance = pos.distSqr(playerPos);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = pos.immutable();
            }
        }

        if (nearest == null) {
            player.displayClientMessage(Component.translatable("message.northstar_planet_addon.chart_none", SCAN_RADIUS)
                    .withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.success(stack);
        }

        String direction = describeDirection(playerPos, nearest);
        int distance = (int) Math.sqrt(nearestDistance);
        if (fragments >= LOG_THRESHOLD) {
            player.displayClientMessage(Component.translatable("message.northstar_planet_addon.chart_exact",
                            direction, distance, nearest.getX(), nearest.getY(), nearest.getZ(), containers)
                    .withStyle(ChatFormatting.GOLD), true);
        } else {
            player.displayClientMessage(Component.translatable("message.northstar_planet_addon.chart_hint",
                            direction, distance, containers, LOG_THRESHOLD - fragments)
                    .withStyle(ChatFormatting.AQUA), true);
        }

        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        return InteractionResultHolder.success(stack);
    }

    private static String describeDirection(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        String ns = Math.abs(dz) < 4 ? "" : (dz > 0 ? "南" : "北");
        String ew = Math.abs(dx) < 4 ? "" : (dx > 0 ? "东" : "西");
        String horizontal = ns + ew;
        if (horizontal.isEmpty()) {
            return "脚下";
        }
        return horizontal;
    }
}
