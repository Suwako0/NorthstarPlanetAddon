package com.suwako.northstarplanetaddon.venus.world;

import com.suwako.northstarplanetaddon.venus.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 酸泵站（设计草案 10 章 #2：24×16×24，酸潮滩，可修复为酸泵）。
 *
 * <p>当前版本：9×9 云岩地基 + 环形围墙 + 中央「泵体」+ 3 个战利品箱。
 * 泵体原先是 {@code pressure_tank} 方块，**压力机器体系移除后改用磨制深板岩**，
 * 只作为"废弃泵站"的视觉占位。修复玩法（投入耐酸合金板换酸泵）随机器一起作废，
 * 若将来重做工业线，需要重新设计抽取装置。</p>
 */
public class AcidPumpStationFeature extends Feature<NoneFeatureConfiguration> {

    private static final int RADIUS = 6;

    public AcidPumpStationFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        var stone = RuinPalette.FLOOR;
        var metal = RuinPalette.CASE;
        // 泵体：气动压力机器体系已移除，这里用磨制深板岩表现"被遗弃的立式泵"占位。
        var pump = RuinPalette.CASE_DARK;

        RuinBuilder.platform(level, origin, RADIUS, 3, stone);
        RuinBuilder.wallRing(level, origin, RADIUS, 4, metal, Blocks.AIR.defaultBlockState());
        RuinBuilder.tower(level, origin, 2, 5, metal, ModBlocks.SULFUR_CRYSTAL_BLOCK.get().defaultBlockState());

        // 泵体：两格高的压力罐
        level.setBlock(origin.offset(3, 1, 0), pump, 2);
        level.setBlock(origin.offset(3, 2, 0), pump, 2);
        level.setBlock(origin.offset(-3, 1, 0), pump, 2);

        RuinBuilder.chest(level, origin.offset(1, 1, 1), "chests/acid_pump_station", random.nextLong(),
                net.minecraft.core.Direction.SOUTH);
        RuinBuilder.chest(level, origin.offset(-1, 1, 2), "chests/acid_pump_station", random.nextLong(),
                net.minecraft.core.Direction.NORTH);
        RuinBuilder.chest(level, origin.offset(2, 1, -2), "chests/acid_pump_station", random.nextLong(),
                net.minecraft.core.Direction.EAST);
        RuinPalette.detail(level, origin, RADIUS, 3);
        return true;
    }
}
