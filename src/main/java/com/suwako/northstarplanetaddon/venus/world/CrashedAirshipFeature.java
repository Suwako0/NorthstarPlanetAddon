package com.suwako.northstarplanetaddon.venus.world;

import com.suwako.northstarplanetaddon.venus.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 坠毁飞艇（设计草案 10 章 #5：30×18×14，云海/雷暴带，气囊核心 75%）。
 *
 * <p>当前版本：15×5 的船体（云岩 + 钨块肋骨）+ 倾斜的断裂尾部 + 三个战利品箱
 * （其中一个保底气囊核心）。</p>
 */
public class CrashedAirshipFeature extends Feature<NoneFeatureConfiguration> {

    public CrashedAirshipFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        var hull = RuinPalette.COPPER_WEATHERED;
        var rib = RuinPalette.TUNGSTEN;
        var air = Blocks.AIR.defaultBlockState();

        RuinBuilder.clear(level, origin, 8, 6, 4);

        // 主船体：胶囊状
        for (int dx = -7; dx <= 7; dx++) {
            int halfWidth = (Math.abs(dx) > 5) ? 1 : 2;
            for (int dz = -halfWidth; dz <= halfWidth; dz++) {
                level.setBlock(origin.offset(dx, 0, dz), hull, 2);
                level.setBlock(origin.offset(dx, 3, dz), hull, 2);
                if (Math.abs(dz) == halfWidth) {
                    level.setBlock(origin.offset(dx, 1, dz), hull, 2);
                    level.setBlock(origin.offset(dx, 2, dz), hull, 2);
                }
            }
            if (Math.abs(dx) % 3 == 0) {
                level.setBlock(origin.offset(dx, 1, 0), rib, 2);
                level.setBlock(origin.offset(dx, 2, 0), rib, 2);
            }
        }

        // 断裂的尾部（倾斜下沉）
        for (int dx = 8; dx <= 11; dx++) {
            int drop = (dx - 7) / 2;
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(origin.offset(dx, -drop, dz), hull, 2);
                level.setBlock(origin.offset(dx, -drop + 1, dz), air, 2);
            }
        }

        // 破口
        level.setBlock(origin.offset(2, 1, 2), air, 2);
        level.setBlock(origin.offset(2, 2, 2), air, 2);

        RuinBuilder.chest(level, origin.offset(-3, 1, 0), "chests/crashed_airship", random.nextLong(), Direction.EAST);
        RuinBuilder.chest(level, origin.offset(2, 1, -1), "chests/crashed_airship", random.nextLong(), Direction.WEST);
        RuinBuilder.chest(level, origin.offset(5, 1, 1), "chests/crashed_airship_core", random.nextLong(),
                Direction.NORTH);
        RuinPalette.detail(level, origin, 4, 3);
        return true;
    }
}
