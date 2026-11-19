package com.suwako.northstarplanetaddon.mars.block;

import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 火星冰 —— 极地冰冠的地表构成。
 *
 * <p><b>刻意不继承原版 {@code IceBlock}</b>：原版冰有两个在火星上很糟的行为：</p>
 * <ul>
 *     <li>{@code randomTick} 只要**方块光 > 11**（火把、岩浆附近）就融化 ——
 *         而火山高地有地表岩浆湖，冰一放过去就没了；</li>
 *     <li>{@code playerDestroy} 在下方是空气/流体时把方块变成水，挖掘当场失去方块。</li>
 * </ul>
 *
 * <p>所以这里沿用 Northstar 自家 {@code CustomIceBlock} 的做法：继承
 * {@link HalfTransparentBlock}、**不随机融化、无破坏副作用**，冰在野外完全稳定，
 * 玩家有充足时间开采；需要「融化」时走配方 / 机器（见 {@code data/.../recipe/}）。</p>
 */
public class MarsIceBlock extends HalfTransparentBlock {

    public MarsIceBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
}
