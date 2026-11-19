package com.suwako.northstarplanetaddon.mars.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * 尖柱特征的配置：在地表往上长一根（或一小丛）柱子，用来给群系制造垂直起伏。
 *
 * @param state     柱子方块
 * @param minHeight 高度下限
 * @param maxHeight 高度上限
 * @param baseRadius 基座半径（0 = 1×1 细柱；1 = 十字形基座，看起来更像岩柱/冰塔）
 */
public record SpireConfig(BlockState state, int minHeight, int maxHeight, int baseRadius)
        implements FeatureConfiguration {

    public static final Codec<SpireConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("state").forGetter(SpireConfig::state),
            Codec.intRange(1, 32).fieldOf("min_height").forGetter(SpireConfig::minHeight),
            Codec.intRange(1, 32).fieldOf("max_height").forGetter(SpireConfig::maxHeight),
            Codec.intRange(0, 3).fieldOf("base_radius").forGetter(SpireConfig::baseRadius)
    ).apply(instance, SpireConfig::new));
}
