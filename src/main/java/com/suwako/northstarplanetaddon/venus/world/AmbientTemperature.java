package com.suwako.northstarplanetaddon.venus.world;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 环境温度读数（只读，供气压计显示）。
 *
 * <p>原气动/压力机器体系里的 {@code MachineEnvironment} 已随机器一并移除；这里保留的是
 * 与机器无关的那部分——"这个地方有多热"——因为 {@code 气压计} 仍然需要对玩家显示温度。
 * 与机器版相比去掉了散热器与冷却源统计（那套只服务于过热机制）。</p>
 */
public final class AmbientTemperature {

    /** 其他维度的基准温度（°C）。 */
    public static final float BASE_TEMPERATURE = 20.0F;
    private static final ResourceLocation VENUS = ResourceLocation.parse("northstar:venus");
    /** 最多统计多少个热源，避免大范围遍历。 */
    private static final int MAX_SOURCES = 8;

    public static final float HEAT_LAVA = 600.0F;
    public static final float HEAT_MAGMA = 300.0F;
    public static final float HEAT_FIRE = 300.0F;
    public static final float HEAT_CAMPFIRE = 200.0F;

    /** 该位置的温度（°C）= 维度基准 + 附近热源求和。 */
    public static float temperatureAt(Level level, BlockPos pos) {
        return baseDimensionTemperature(level) + heatAt(level, pos);
    }

    public static float baseDimensionTemperature(Level level) {
        ResourceLocation dimension = level.dimension().location();
        if (dimension.equals(VenusModule.VENUS_CLOUDS)) {
            return 30.0F;
        }
        if (dimension.equals(VENUS)) {
            return 430.0F;
        }
        return BASE_TEMPERATURE;
    }

    /** 附近热源求和（半径 2、向下 2 向上 1，最多 8 个）。 */
    public static float heatAt(Level level, BlockPos pos) {
        float heat = 0.0F;
        int sources = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -2; dx <= 2 && sources < MAX_SOURCES; dx++) {
            for (int dy = -2; dy <= 1 && sources < MAX_SOURCES; dy++) {
                for (int dz = -2; dz <= 2 && sources < MAX_SOURCES; dz++) {
                    cursor.setWithOffset(pos, dx, dy, dz);
                    float value = heatValue(level.getBlockState(cursor));
                    if (value > 0.0F) {
                        heat += value;
                        sources++;
                    }
                }
            }
        }
        return heat;
    }

    public static float heatValue(BlockState state) {
        if (state.is(Blocks.LAVA)) {
            return HEAT_LAVA;
        }
        if (state.is(Blocks.MAGMA_BLOCK)) {
            return HEAT_MAGMA;
        }
        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
            return HEAT_FIRE;
        }
        if (state.is(BlockTags.CAMPFIRES)) {
            return HEAT_CAMPFIRE;
        }
        return 0.0F;
    }

    private AmbientTemperature() {
    }
}
