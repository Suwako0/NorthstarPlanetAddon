package com.suwako.northstarplanetaddon.mercury.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 水星沙 — 与主世界沙子行为完全一致的重力方块。
 * <p>
 * It is kept as its own class rather than reusing vanilla sand so the block has its own id, texture
 * and tags, while {@link FallingBlock}'s gravity handling (a protected constructor plus the abstract
 * codec every 1.21 block needs) is reused instead of reimplemented.
 */
public class MercurySandBlock extends FallingBlock {

    /** Required by 1.21's block codec refactor; the plain properties-only codec is the right one here. */
    public static final MapCodec<MercurySandBlock> CODEC = simpleCodec(MercurySandBlock::new);

    public MercurySandBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends FallingBlock> codec() {
        return CODEC;
    }
}
