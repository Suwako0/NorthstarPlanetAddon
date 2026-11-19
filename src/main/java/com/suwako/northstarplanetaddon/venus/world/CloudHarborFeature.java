package com.suwako.northstarplanetaddon.venus.world;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import com.suwako.northstarplanetaddon.venus.registry.ModBlocks;
import com.suwako.northstarplanetaddon.venus.registry.ModEntities;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * 云港遗迹（设计草案第 10 章 #1：48×40×48、云海间距 400、6–8 个箱子、黄铜执事 1–2 只）。
 *
 * <p>本实现是「特征式遗迹」：不依赖 jigsaw + NBT 模板，直接在空维度里生成一座
 * 21×21 的云岩平台 + 环形围墙 + 四角晶塔 + 中央塔，中央塔内放 2 个带战利品表的箱子。</p>
 *
 * <p>与草案的差异（已在 README 说明）：</p>
 * <ul>
 *   <li>尺寸缩到 21×~16×21，用 {@code rarity_filter} 控制间距（而不是 structure_set 的 400）；</li>
 *   <li>箱子 2 个（草案 6–8）：中央塔内保证 1 个含<b>弹道校准器</b>的箱子（设计草案 2.4 节的获取来源）；</li>
 *   <li>黄铜执事由特征在箱边随机生成 1 只（草案 1–2 只）。</li>
 * </ul>
 */
public class CloudHarborFeature extends Feature<NoneFeatureConfiguration> {

    /** 平台半径（正方形边长 2*R+1 = 21）。 */
    private static final int RADIUS = 10;
    private static final int PLATFORM_THICKNESS = 3;
    private static final int WALL_HEIGHT = 6;
    private static final int TOWER_HEIGHT = 12;
    private static final int TOWER_RADIUS = 2;

    private static final ResourceKey<LootTable> CHEST_LOOT =
            ResourceKey.create(Registries.LOOT_TABLE, VenusModule.id("chests/cloud_harbor"));
    private static final ResourceKey<LootTable> CALIBRATOR_LOOT =
            ResourceKey.create(Registries.LOOT_TABLE, VenusModule.id("chests/cloud_harbor_calibrator"));

    public CloudHarborFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int baseY = origin.getY();

        BlockState cloudStone = RuinPalette.FLOOR;
        BlockState metal = RuinPalette.CASE;
        BlockState crystal = RuinPalette.CRYSTAL;
        BlockState air = Blocks.AIR.defaultBlockState();

        // ---------------------------------------------- 平台（云岩，四角切斜）
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                boolean corner = Math.abs(dx) == RADIUS && Math.abs(dz) == RADIUS;
                if (corner) {
                    continue;
                }
                for (int dy = 0; dy > -PLATFORM_THICKNESS; dy--) {
                    level.setBlock(origin.offset(dx, dy, dz), cloudStone, 2);
                }
            }
        }

        // ---------------------------------------------- 环形围墙（每 5 格留窗）
        int wallY = baseY + 1;
        for (int i = -RADIUS + 1; i <= RADIUS - 1; i++) {
            boolean window = Math.floorMod(i, 5) == 0;
            for (int dy = 0; dy < WALL_HEIGHT; dy++) {
                BlockState state = (window && dy > 1 && dy < WALL_HEIGHT - 1) ? air : metal;
                level.setBlock(new BlockPos(origin.getX() + i, wallY + dy, origin.getZ() - RADIUS + 1), state, 2);
                level.setBlock(new BlockPos(origin.getX() + i, wallY + dy, origin.getZ() + RADIUS - 1), state, 2);
                level.setBlock(new BlockPos(origin.getX() + (RADIUS - 1), wallY + dy, origin.getZ() + i), state, 2);
                level.setBlock(new BlockPos(origin.getX() - (RADIUS - 1), wallY + dy, origin.getZ() + i), state, 2);
            }
        }

        // ---------------------------------------------- 四角晶塔
        int[][] corners = {{-8, -8}, {8, -8}, {-8, 8}, {8, 8}};
        for (int[] corner : corners) {
            for (int dy = 1; dy <= 9; dy++) {
                for (int dx = 0; dx <= 1; dx++) {
                    for (int dz = 0; dz <= 1; dz++) {
                        level.setBlock(origin.offset(corner[0] + dx, dy, corner[1] + dz), metal, 2);
                    }
                }
            }
            level.setBlock(origin.offset(corner[0], 10, corner[1]), crystal, 2);
            level.setBlock(origin.offset(corner[0] + 1, 10, corner[1] + 1), crystal, 2);
        }

        // ---------------------------------------------- 中央塔
        for (int dy = 1; dy <= TOWER_HEIGHT; dy++) {
            for (int dx = -TOWER_RADIUS; dx <= TOWER_RADIUS; dx++) {
                for (int dz = -TOWER_RADIUS; dz <= TOWER_RADIUS; dz++) {
                    boolean edge = Math.abs(dx) == TOWER_RADIUS || Math.abs(dz) == TOWER_RADIUS;
                    if (!edge) {
                        continue;
                    }
                    boolean doorway = dy <= 2 && (dz == TOWER_RADIUS && Math.abs(dx) <= 1);
                    level.setBlock(origin.offset(dx, dy, dz), doorway ? air : metal, 2);
                }
            }
        }
        // 塔顶
        for (int dx = -TOWER_RADIUS; dx <= TOWER_RADIUS; dx++) {
            for (int dz = -TOWER_RADIUS; dz <= TOWER_RADIUS; dz++) {
                level.setBlock(origin.offset(dx, TOWER_HEIGHT + 1, dz), cloudStone, 2);
            }
        }
        level.setBlock(origin.offset(0, TOWER_HEIGHT + 2, 0), crystal, 2);

        // ---------------------------------------------- 箱子
        placeChest(level, origin.offset(1, 1, -1), CHEST_LOOT, random.nextLong(), Direction.SOUTH);
        placeChest(level, origin.offset(0, 1, 2), CALIBRATOR_LOOT, random.nextLong(), Direction.NORTH);

        // ---------------------------------------------- 守卫（黄铜执事）
        if (random.nextFloat() < 0.75F) {
            var deacon = ModEntities.BRASS_DEACON.get().create(level.getLevel());
            if (deacon != null) {
                deacon.moveTo(origin.getX() + 0.5D, baseY + 1.0D, origin.getZ() + 0.5D,
                        random.nextFloat() * 360.0F, 0.0F);
                level.addFreshEntity(deacon);
            }
        }
        RuinPalette.detail(level, origin, 6, 4);
        return true;
    }

    private void placeChest(WorldGenLevel level, BlockPos pos, ResourceKey<LootTable> loot,
                            long seed, Direction facing) {
        level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 2);
        if (level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity container) {
            container.setLootTable(loot, seed);
        }
    }
}
