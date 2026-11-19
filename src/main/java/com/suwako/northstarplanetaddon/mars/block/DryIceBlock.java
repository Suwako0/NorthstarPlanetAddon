package com.suwako.northstarplanetaddon.mars.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 干冰 —— 极地冰冠的另一半构成。
 *
 * <p><b>不再随机升华。</b>之前的实现里每个随机刻有 25% 概率把方块换成液态氧或直接消失，
 * 结果地表上的干冰几分钟就自己没了，玩家根本没时间开采。现在干冰在野外完全稳定，
 * 「升华成液态氧」改由配方完成（见 {@code data/.../recipe/dry_ice_liquid_oxygen.json}）。</p>
 *
 * <p>如果确实想要「只有靠近高温才升华」的风味：给 {@code ModBlocks.DRY_ICE} 的属性加上
 * {@code randomTicks()}，然后在 {@link #randomTick} 里按方块光判断即可（默认不启用）。</p>
 */
public class DryIceBlock extends Block {

    public DryIceBlock(Properties properties) {
        super(properties);
    }

    /** 默认空实现：干冰在野外稳定，保证玩家有充足时间开采。 */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 故意留空
    }
}
