package com.suwako.northstarplanetaddon.venus.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 高压钻（设计草案 6.3 节）：挖掘速度 10 / 耐久 2400；
 * 对金星石、熔管玄武岩之类额外 +50%；右键 3×3 挖掘（耐久 ×9）。
 *
 * <p>因为编译期不依赖 Northstar，这里用「注册名包含 venus / basalt」判断金星岩与玄武岩，
 * 不用硬引用对方的方块类。</p>
 */
public class PressureDrillItem extends PickaxeItem {

    private static final float BONUS_MULTIPLIER = 1.5F;

    public PressureDrillItem(Properties properties) {
        super(PressureDrillTier.INSTANCE, properties);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float speed = super.getDestroySpeed(stack, state);
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        if (path.contains("venus") || path.contains("basalt") || path.contains("sulfur")) {
            return speed * BONUS_MULTIPLIER;
        }
        return speed;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        BlockPos origin = context.getClickedPos();
        Direction face = context.getClickedFace();
        ItemStack stack = context.getItemInHand();

        List<BlockPos> targets = new ArrayList<>(9);
        Direction.Axis axis = face.getAxis();
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                BlockPos pos = switch (axis) {
                    case Y -> origin.offset(a, 0, b);
                    case X -> origin.offset(0, a, b);
                    case Z -> origin.offset(a, b, 0);
                };
                targets.add(pos);
            }
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        int broken = 0;
        for (BlockPos pos : targets) {
            if (pos.equals(origin) || level.isEmptyBlock(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.getDestroySpeed(level, pos) < 0.0F) {
                continue; // 基岩之类的不可破坏方块
            }
            if (level.destroyBlock(pos, !player.isCreative(), player)) {
                broken++;
            }
        }
        if (broken == 0) {
            return InteractionResult.PASS;
        }
        if (!player.getAbilities().instabuild) {
            stack.hurtAndBreak(9, player, LivingEntity.getSlotForHand(context.getHand()));
        }
        player.swing(context.getHand(), true);
        return InteractionResult.SUCCESS;
    }
}
