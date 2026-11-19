package com.suwako.northstarplanetaddon.mars.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * 陨石坑特征的配置（火山高地的「岩浆湖坑」与极地冰冠的「冰核坑」共用一套参数）。
 *
 * <p>剖面由 {@link MarsCraterFeature} 计算：中心下挖 {@code depth}、坑缘抬高 {@code rim_height}，
 * 坑底在 {@code core_radius} 内是冰核（{@code ice_block}），
 * {@code core_radius} 到 {@code radius * 0.9} 之间是熔岩环（{@code lava_block}），
 * 其余是普通坑底岩石（{@code fill_block}）。</p>
 *
 * <p><b>半径硬上限 8</b>：一个坑最多 (2×8+1)² = 289 列 × 每列 ≤40 方块，
 * 再大就会让单 tick 的区块生成超时（Watchdog 会直接杀进程，见 run/crash-reports 的历史记录）。
 * 上限写在 Codec 里而不是只写在注释里，改 JSON 也越不过去。</p>
 *
 * @param radius      外半径（1..8），超过它的列完全不动
 * @param depth       中心下挖深度（1..10）
 * @param rim_height  坑缘相对原地表的最大抬高（0..6）
 * @param lava_block  熔岩环方块（<code>minecraft:lava</code> / <code>minecraft:magma_block</code>）
 * @param ice_block   冰核方块（<code>northstar_planet_addon:mars_ice</code> / <code>dry_ice</code>）
 * @param fill_block  坑底与坑缘的岩石（<code>northstar:volcanic_rock</code> / <code>mars_stone</code>）
 * @param core_radius 冰核半径（0..8，代码里再夹到 radius 以内）
 */
public record MarsCraterConfig(int radius,
                               int depth,
                               int rimHeight,
                               BlockState lavaBlock,
                               BlockState iceBlock,
                               BlockState fillBlock,
                               int coreRadius) implements FeatureConfiguration {

    public static final Codec<MarsCraterConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, 16).fieldOf("radius").forGetter(MarsCraterConfig::radius),
            Codec.intRange(1, 16).fieldOf("depth").forGetter(MarsCraterConfig::depth),
            Codec.intRange(0, 6).fieldOf("rim_height").forGetter(MarsCraterConfig::rimHeight),
            BlockState.CODEC.fieldOf("lava_block").forGetter(MarsCraterConfig::lavaBlock),
            BlockState.CODEC.fieldOf("ice_block").forGetter(MarsCraterConfig::iceBlock),
            BlockState.CODEC.fieldOf("fill_block").forGetter(MarsCraterConfig::fillBlock),
            Codec.intRange(0, 16).fieldOf("core_radius").forGetter(MarsCraterConfig::coreRadius)
    ).apply(instance, MarsCraterConfig::new));
}
