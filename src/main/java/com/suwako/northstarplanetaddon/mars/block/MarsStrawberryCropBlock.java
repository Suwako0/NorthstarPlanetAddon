package com.suwako.northstarplanetaddon.mars.block;

import com.suwako.northstarplanetaddon.mars.registry.ModItems;
import com.suwako.northstarplanetaddon.mars.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.ArrayList;
import java.util.List;

/**
 * 火星草莓作物：只能种在火星耕地（{@code #northstar_planet_addon:mars_farmland}）上。
 *
 * <p>成熟后产出 1-2 个火星草莓，并有一定概率额外掉种子；未成熟只掉种子。
 * 掉落由 {@link #getDrops} 直接给出（没有战利品表文件）。</p>
 */
public class MarsStrawberryCropBlock extends CropBlock {

    public MarsStrawberryCropBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModTags.Blocks.MARS_FARMLAND);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.MARS_STRAWBERRY_SEEDS.get();
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>(2);
        if (isMaxAge(state)) {
            ServerLevel level = params.getLevel();
            RandomSource random = level.getRandom();
            drops.add(new ItemStack(ModItems.MARS_STRAWBERRY.get(), 1 + random.nextInt(2)));
            if (random.nextFloat() < 0.5F) {
                drops.add(new ItemStack(ModItems.MARS_STRAWBERRY_SEEDS.get()));
            }
        } else {
            drops.add(new ItemStack(ModItems.MARS_STRAWBERRY_SEEDS.get()));
        }
        return drops;
    }
}
