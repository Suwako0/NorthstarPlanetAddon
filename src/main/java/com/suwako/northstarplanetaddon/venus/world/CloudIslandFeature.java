package com.suwako.northstarplanetaddon.venus.world;

import com.suwako.northstarplanetaddon.venus.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 云海浮空岛（设计草案 9.2 节「浮空岛生成草案」的 M2 实现）。
 *
 * <p>草案原本计划用 3D 噪声密度函数；噪声路由 JSON 在 1.21.1 上字段极多且无法离线校验，
 * 所以这里先用「特征」实现同样观感的云岩岛：顶部平坦（可站立）、底部倒锥、边缘随机锯齿。
 * 空中岛屿由 {@code placed_feature} 的 height_range(180–300) 撒点，每区块 4 座。</p>
 */
public class CloudIslandFeature extends Feature<NoneFeatureConfiguration> {

    public CloudIslandFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        BlockState cloudStone = ModBlocks.CLOUD_STONE.get().defaultBlockState();

        int radius = 6 + random.nextInt(13);          // 直径 13–36 格
        int plateau = 2 + random.nextInt(3);          // 顶面厚度
        boolean placedAnything = false;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double distance = Math.sqrt((double) (dx * dx + dz * dz)) / radius;
                if (distance > 1.0D) {
                    continue;
                }
                // 顶面：中间厚、边缘薄，形成平顶圆盘
                int top = (int) Math.round(plateau * (1.0D - distance * distance));
                // 底部：倒锥，越靠边越薄（1.8 倍高度让岛底像钟乳石）
                int bottom = (int) Math.round(-plateau * 1.8D * Math.pow(1.0D - distance, 0.6D));
                for (int dy = bottom; dy <= top; dy++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) {
                        continue;
                    }
                    if (!level.isStateAtPosition(pos, state -> state.isAir())) {
                        continue;
                    }
                    level.setBlock(pos, cloudStone, 2);
                    placedAnything = true;
                }
            }
        }
        return placedAnything;
    }
}
