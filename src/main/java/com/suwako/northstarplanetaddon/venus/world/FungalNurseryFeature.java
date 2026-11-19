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
 * 真菌苗圃（设计草案 10 章 #4：28×12×28，真菌森林，菌床 ×8）。
 *
 * <p>当前版本：11×11 云岩台 + 四角真菌柱 + 8 个菌床（苔藓 + 巨型蘑菇方块，
 * 也就是 {@code #northstar_planet_addon:fungus} 标签里的方块）+ 2 个战利品箱（含农业材料）。</p>
 */
public class FungalNurseryFeature extends Feature<NoneFeatureConfiguration> {

    private static final int RADIUS = 5;

    public FungalNurseryFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        var stone = RuinPalette.FLOOR;
        var moss = Blocks.MOSS_BLOCK.defaultBlockState();
        var brown = Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState();
        var red = Blocks.RED_MUSHROOM_BLOCK.defaultBlockState();

        RuinBuilder.platform(level, origin, RADIUS, 2, stone);
        RuinBuilder.clear(level, origin, RADIUS - 1, 6, RADIUS - 1);

        // 8 个菌床：每个 2×2 苔藓 + 上方一株巨型蘑菇
        int[][] beds = {{-3, -3}, {0, -3}, {3, -3}, {-3, 0}, {3, 0}, {-3, 3}, {0, 3}, {3, 3}};
        for (int i = 0; i < beds.length; i++) {
            int x = beds[i][0];
            int z = beds[i][1];
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    level.setBlock(origin.offset(x + dx, 0, z + dz), moss, 2);
                }
            }
            var fungus = (i % 2 == 0) ? brown : red;
            level.setBlock(origin.offset(x, 1, z), fungus, 2);
            level.setBlock(origin.offset(x + 1, 1, z), fungus, 2);
            level.setBlock(origin.offset(x, 1, z + 1), fungus, 2);
            level.setBlock(origin.offset(x, 2, z), Blocks.MUSHROOM_STEM.defaultBlockState(), 2);
            level.setBlock(origin.offset(x, 3, z), fungus, 2);
        }

        // 中央小棚
        level.setBlock(origin.offset(0, 4, 0), ModBlocks.SULFUR_CRYSTAL_BLOCK.get().defaultBlockState(), 2);

        RuinBuilder.chest(level, origin.offset(1, 1, 1), "chests/fungal_nursery", random.nextLong(), Direction.SOUTH);
        RuinBuilder.chest(level, origin.offset(-1, 1, 1), "chests/fungal_nursery", random.nextLong(), Direction.WEST);
        RuinPalette.detail(level, origin, 5, 3);
        return true;
    }
}
