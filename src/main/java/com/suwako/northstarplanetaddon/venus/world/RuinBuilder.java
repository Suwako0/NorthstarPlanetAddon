package com.suwako.northstarplanetaddon.venus.world;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * 遗迹建造工具：平台 / 环形围墙 / 塔 / 箱子。
 *
 * <p>M2 的 5 个结构都复用这一套原语，单个特征类只描述布局，
 * 不用重复写「遍历坐标 + setBlock」的样板代码。</p>
 */
public final class RuinBuilder {

    public static void platform(WorldGenLevel level, BlockPos origin, int radius, int thickness, BlockState state) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (Math.abs(dx) == radius && Math.abs(dz) == radius) {
                    continue;
                }
                for (int dy = 0; dy > -thickness; dy--) {
                    level.setBlock(origin.offset(dx, dy, dz), state, 2);
                }
            }
        }
    }

    public static void wallRing(WorldGenLevel level, BlockPos origin, int radius, int height,
                               BlockState wall, BlockState window) {
        for (int i = -radius + 1; i <= radius - 1; i++) {
            boolean isWindow = Math.floorMod(i, 5) == 0;
            for (int dy = 0; dy < height; dy++) {
                BlockState state = (isWindow && dy > 1 && dy < height - 1) ? window : wall;
                level.setBlock(origin.offset(i, 1 + dy, -radius + 1), state, 2);
                level.setBlock(origin.offset(i, 1 + dy, radius - 1), state, 2);
                level.setBlock(origin.offset(radius - 1, 1 + dy, i), state, 2);
                level.setBlock(origin.offset(-radius + 1, 1 + dy, i), state, 2);
            }
        }
    }

    public static void tower(WorldGenLevel level, BlockPos origin, int radius, int height,
                             BlockState shell, BlockState cap) {
        for (int dy = 1; dy <= height; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    boolean edge = Math.abs(dx) == radius || Math.abs(dz) == radius;
                    if (edge) {
                        level.setBlock(origin.offset(dx, dy, dz), shell, 2);
                    }
                }
            }
        }
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                level.setBlock(origin.offset(dx, height + 1, dz), shell, 2);
            }
        }
        level.setBlock(origin.offset(0, height + 2, 0), cap, 2);
    }

    /** 用空气清出一个立方体空间（用于飞艇内部、苗圃棚下）。 */
    public static void clear(WorldGenLevel level, BlockPos origin, int radiusX, int height, int radiusZ) {
        for (int dx = -radiusX; dx <= radiusX; dx++) {
            for (int dz = -radiusZ; dz <= radiusZ; dz++) {
                for (int dy = 1; dy <= height; dy++) {
                    level.setBlock(origin.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    public static void chest(WorldGenLevel level, BlockPos pos, String lootPath, long seed, Direction facing) {
        level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 2);
        if (level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity container) {
            container.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, VenusModule.id(lootPath)), seed);
        }
    }

    private RuinBuilder() {
    }
}
