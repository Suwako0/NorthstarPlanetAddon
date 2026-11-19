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
 * 启明星祭坛（设计草案 10 章 #7：16×16×16，云海最高岛，结局多方块）。
 *
 * <p>当前版本：9×9 三级阶梯平台 + 四角硫晶柱 + 中央启明星核心（用启明星核的
 * 方块形态占位：硫晶块 + 钨块结构）+ 1 个结局战利品箱。结局触发逻辑（放置
 * 启明星核完成多方块）等结局流程设计确定后再接。</p>
 */
public class PhosphorosAltarFeature extends Feature<NoneFeatureConfiguration> {

    private static final int RADIUS = 4;

    public PhosphorosAltarFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        var stone = RuinPalette.FLOOR;
        var metal = RuinPalette.COPPER;
        var crystal = RuinPalette.CRYSTAL;

        // 三级阶梯
        RuinBuilder.platform(level, origin, RADIUS, 2, stone);
        for (int tier = 1; tier <= 2; tier++) {
            int r = RADIUS - tier;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    level.setBlock(origin.offset(dx, tier, dz), metal, 2);
                }
            }
        }

        // 四角晶柱
        int[][] corners = {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}};
        for (int[] c : corners) {
            for (int dy = 1; dy <= 4; dy++) {
                level.setBlock(origin.offset(c[0], dy, c[1]), crystal, 2);
            }
        }

        // 中央核心
        level.setBlock(origin.offset(0, 3, 0), crystal, 2);
        level.setBlock(origin.offset(0, 4, 0), metal, 2);
        level.setBlock(origin.offset(0, 5, 0), crystal, 2);

        RuinBuilder.chest(level, origin.offset(1, 3, 1), "chests/phosphoros_altar", random.nextLong(), Direction.SOUTH);
        RuinPalette.detail(level, origin, RADIUS, 2);
        return true;
    }
}
