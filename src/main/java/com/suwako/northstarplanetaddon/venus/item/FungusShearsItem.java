package com.suwako.northstarplanetaddon.venus.item;

import com.suwako.northstarplanetaddon.venus.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 菌剪（设计草案 6.3 节）：挖掘速度 2 / 耐久 250；
 * 采菌伞、藤蔓不会损坏掉落；右键把菌类方块「移植」到相邻空位。
 *
 * <p>菌类判定走 {@code #northstar_planet_addon:fungus} 标签（数据包里可扩展），
 * 另外兼容原版的巨型蘑菇方块。</p>
 */
public class FungusShearsItem extends ShearsItem {

    public FungusShearsItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        boolean isFungus = state.is(ModTags.Blocks.FUNGUS)
                || state.is(Blocks.BROWN_MUSHROOM_BLOCK)
                || state.is(Blocks.RED_MUSHROOM_BLOCK)
                || state.is(Blocks.MUSHROOM_STEM);
        if (!isFungus) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockPos target = pos.relative(context.getClickedFace());
        if (!level.isEmptyBlock(target)) {
            return InteractionResult.PASS;
        }
        level.setBlock(target, state, 2);
        if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().hurtAndBreak(1, context.getPlayer(),
                    LivingEntity.getSlotForHand(context.getHand()));
        }
        return InteractionResult.SUCCESS;
    }

    /** 采集菌类方块不额外损耗（单点挖掘仍按原版剪刀的耐久规则）。 */
    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        if (!level.isClientSide && state.is(ModTags.Blocks.FUNGUS)) {
            return true; // 不扣耐久
        }
        return super.mineBlock(stack, level, state, pos, entity);
    }

    /** 让方块 ID 便于外部复用（保留给后续扩展）。 */
    public static boolean isFungusBlock(Block block) {
        return block == Blocks.BROWN_MUSHROOM_BLOCK || block == Blocks.RED_MUSHROOM_BLOCK;
    }
}
