package com.suwako.northstarplanetaddon.mars.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * 自定义火星表层特征的配置。
 *
 * <p>Northstar 的 {@code northstar:mars} 噪声设置里，表层规则是**按群系分派**的
 * （{@code minecraft:biome} + {@code biome_is: [martian_dunes/highlands/peaks]}）。
 * 本模组新增的群系不在名单里，于是**不会铺任何表层**，地表直接是噪声默认的
 * {@code northstar:mars_stone} —— 这正是「维度里啥也没有」的根因。</p>
 *
 * <p>这个特征在 LOCAL_MODIFICATIONS 阶段（表层规则之后、其它特征之前）为整块区块的每一列
 * 铺一层地表：顶层 {@code top}（按 {@code accentChance} 概率换成 {@code accent}），
 * 其下 {@code fillerDepth} 层 {@code filler}，再往下保持火星石不变。</p>
 *
 * @param top          顶层方块
 * @param filler       次表层方块
 * @param accent       顶层点缀方块（矿脉/岩石露头的感觉）
 * @param fillerDepth  次表层厚度（0-8）
 * @param accentChance 顶层被 accent 替换的概率（0-1）
 */
public record MarsSurfaceConfig(BlockState top,
                                BlockState filler,
                                BlockState accent,
                                int fillerDepth,
                                float accentChance) implements FeatureConfiguration {

    public static final Codec<MarsSurfaceConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("top").forGetter(MarsSurfaceConfig::top),
            BlockState.CODEC.fieldOf("filler").forGetter(MarsSurfaceConfig::filler),
            BlockState.CODEC.fieldOf("accent").forGetter(MarsSurfaceConfig::accent),
            Codec.intRange(0, 8).fieldOf("filler_depth").forGetter(MarsSurfaceConfig::fillerDepth),
            Codec.floatRange(0.0F, 1.0F).fieldOf("accent_chance").forGetter(MarsSurfaceConfig::accentChance)
    ).apply(instance, MarsSurfaceConfig::new));
}
