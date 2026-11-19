package com.suwako.northstarplanetaddon.venus.world;

import com.suwako.northstarplanetaddon.venus.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 熔管城市（设计草案 10 章 #6：120×80×120，12 个箱子，Boss 3 场地，三层）。
 *
 * <p>当前版本按 41×30×41 的三层环形平台实现（体量压到可在特征里生成）：</p>
 * <ul>
 *   <li>第一层半径 20：外环钨钢墙 + 12 个战利品箱分布在四个方向；</li>
 *   <li>第二层半径 14：硫晶柱环 + 酸池（硫晶块铺底）；</li>
 *   <li>第三层半径 8：中央祭台，顶层硫晶信标；</li>
 *   <li>四角立柱贯通三层。</li>
 * </ul>
 *
 * <p>阿佛洛狄忒不在此生成：按草案 4.4 节，它由玩家放置「硫心」召唤，这里只提供战场。</p>
 */
public class LavaTubeCityFeature extends Feature<NoneFeatureConfiguration> {

    private static final int[][] TIERS = {{20, 0}, {14, 6}, {8, 12}};

    public LavaTubeCityFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        BlockState wall = RuinPalette.CASE_DARK;
        BlockState stone = RuinPalette.CASE;
        BlockState sulfur = RuinPalette.CRYSTAL;
        BlockState air = Blocks.AIR.defaultBlockState();

        // ---------------------------------------------- 三层平台
        for (int[] tier : TIERS) {
            int radius = tier[0];
            int y = tier[1];
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    if (distance > radius) {
                        continue;
                    }
                    level.setBlock(origin.offset(dx, y, dz), stone, 2);
                    if (y > 0) {
                        // 平台下方补一层，避免悬空
                        level.setBlock(origin.offset(dx, y - 1, dz), stone, 2);
                    }
                    // 外环墙体
                    if (distance > radius - 1.0D && Math.floorMod(dx + dz, 6) != 0) {
                        level.setBlock(origin.offset(dx, y + 1, dz), wall, 2);
                        level.setBlock(origin.offset(dx, y + 2, dz), wall, 2);
                    }
                    // 内部清空
                    if (distance < radius - 2.0D) {
                        for (int dy = 1; dy <= 4; dy++) {
                            level.setBlock(origin.offset(dx, y + dy, dz), air, 2);
                        }
                    }
                }
            }
        }

        // ---------------------------------------------- 四角立柱（贯通三层）
        int[][] corners = {{-18, -18}, {18, -18}, {-18, 18}, {18, 18}};
        for (int[] corner : corners) {
            for (int dy = 1; dy <= 14; dy++) {
                level.setBlock(origin.offset(corner[0], dy, corner[1]), wall, 2);
            }
            level.setBlock(origin.offset(corner[0], 15, corner[1]), sulfur, 2);
        }

        // ---------------------------------------------- 第二层硫晶柱环 + 酸池
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0D;
            int x = (int) Math.round(Math.cos(angle) * 10.0D);
            int z = (int) Math.round(Math.sin(angle) * 10.0D);
            for (int dy = 7; dy <= 9; dy++) {
                level.setBlock(origin.offset(x, dy, z), sulfur, 2);
            }
        }
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (Math.abs(dx) + Math.abs(dz) <= 4) {
                    level.setBlock(origin.offset(dx, 6, dz), sulfur, 2);
                }
            }
        }

        // ---------------------------------------------- 第三层中央祭台
        level.setBlock(origin.offset(0, 13, 0), wall, 2);
        level.setBlock(origin.offset(0, 14, 0), sulfur, 2);
        level.setBlock(origin.offset(0, 15, 0), sulfur, 2);

        // ---------------------------------------------- 12 个箱子（每层 4 个）
        int[][] firstTierChests = {{-6, 0}, {6, 0}, {0, -6}, {0, 6}};
        for (int[] c : firstTierChests) {
            RuinBuilder.chest(level, origin.offset(c[0], 1, c[1]), "chests/lava_tube_city",
                    random.nextLong(), Direction.SOUTH);
        }
        int[][] secondTierChests = {{-5, 0}, {5, 0}, {0, -5}, {0, 5}};
        for (int[] c : secondTierChests) {
            RuinBuilder.chest(level, origin.offset(c[0], 7, c[1]), "chests/lava_tube_city",
                    random.nextLong(), Direction.NORTH);
        }
        int[][] thirdTierChests = {{-3, 0}, {3, 0}, {0, -3}, {0, 3}};
        for (int[] c : thirdTierChests) {
            RuinBuilder.chest(level, origin.offset(c[0], 13, c[1]), "chests/lava_tube_city_boss",
                    random.nextLong(), Direction.EAST);
        }
        RuinPalette.detail(level, origin, 6, 4);
        return true;
    }
}
