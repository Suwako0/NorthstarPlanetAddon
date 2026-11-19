package com.suwako.northstarplanetaddon.venus.machine;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import com.suwako.northstarplanetaddon.venus.blockentity.RadiatorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 环境温度与热源（设计草案第 8 章「过热机制」）。
 *
 * <p>没有 Northstar 的温度 API 时，用自己的读表实现同样的手感：</p>
 * <ul>
 *   <li>金星地表（{@code northstar:venus}）按 430°C 处理 —— 超过 400°C 阈值，
 *       所以「地表机器必须有降温手段才不会过热」这条设计在只有本模组时也成立；</li>
 *   <li>云海维度 30°C；主世界 20°C；</li>
 *   <li>附近熔岩 / 火 / 岩浆块升温，附近水 / 冰 / 蓝冰降温（9×5×9 范围内，
 *       即草案里散热器的作用体积；散热器方块本身留给后续机器批次）；</li>
 *   <li>高压熔炉额外读「热源求和」，需要 1200°C 才能熔炼。</li>
 * </ul>
 */
public final class MachineEnvironment {

    public static final float BASE_TEMPERATURE = 20.0F;
    private static final ResourceLocation VENUS = ResourceLocation.parse("northstar:venus");
    /** 散热器 / 降温水源的作用体积：±4 × ±2 × ±4 = 9×5×9。 */
    public static final int COOLING_RADIUS = 4;
    public static final int COOLING_HEIGHT = 2;
    /** 每个降温方块的降温量。 */
    public static final float COOLING_PER_BLOCK = 40.0F;
    /** 每个熔岩源的升温量。 */
    public static final float HEAT_LAVA = 600.0F;
    public static final float HEAT_MAGMA = 300.0F;
    public static final float HEAT_FIRE = 300.0F;
    public static final float HEAT_CAMPFIRE = 200.0F;
    /** 最多统计多少个热源/冷源，避免超大型装置做无谓遍历。 */
    private static final int MAX_SOURCES = 8;

    /** 机器所在位置的环境温度（°C）。 */
    public static float temperatureAt(Level level, BlockPos pos) {
        return baseDimensionTemperature(level) + heatAt(level, pos) - coolingAt(level, pos);
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

    /** 附近热源求和（熔炉用 1200°C 门槛）。 */
    public static float heatAt(Level level, BlockPos pos) {
        float heat = 0.0F;
        int sources = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -2; dx <= 2 && sources < MAX_SOURCES; dx++) {
            for (int dy = -2; dy <= 1 && sources < MAX_SOURCES; dy++) {
                for (int dz = -2; dz <= 2 && sources < MAX_SOURCES; dz++) {
                    cursor.setWithOffset(pos, dx, dy, dz);
                    BlockState state = level.getBlockState(cursor);
                    float value = heatValue(state);
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

    /** 附近水 / 冰 / 蓝冰 / 雪的降温求和（散热器未覆盖时的临时手段）。 */
    public static float coolingAt(Level level, BlockPos pos) {
        float cooling = 0.0F;
        int sources = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -COOLING_RADIUS; dx <= COOLING_RADIUS && sources < MAX_SOURCES; dx++) {
            for (int dy = -COOLING_HEIGHT; dy <= COOLING_HEIGHT && sources < MAX_SOURCES; dy++) {
                for (int dz = -COOLING_RADIUS; dz <= COOLING_RADIUS && sources < MAX_SOURCES; dz++) {
                    cursor.setWithOffset(pos, dx, dy, dz);
                    BlockState state = level.getBlockState(cursor);
                    if (state.is(Blocks.WATER) || state.is(Blocks.ICE) || state.is(Blocks.BLUE_ICE)
                            || state.is(Blocks.PACKED_ICE) || state.is(Blocks.SNOW_BLOCK)) {
                        cooling += COOLING_PER_BLOCK;
                        sources++;
                    }
                }
            }
        }
        // 散热器（第 8 章）：有冷却剂时每台再降 COOLING_PER_BLOCK
        for (BlockPos radiatorPos : BlockPos.betweenClosed(pos.offset(-COOLING_RADIUS, -COOLING_HEIGHT, -COOLING_RADIUS),
                pos.offset(COOLING_RADIUS, COOLING_HEIGHT, COOLING_RADIUS))) {
            if (sources >= MAX_SOURCES) {
                break;
            }
            if (level.getBlockEntity(radiatorPos) instanceof RadiatorBlockEntity radiator
                    && radiator.isCooling()) {
                cooling += COOLING_PER_BLOCK;
                sources++;
            }
        }
        return cooling;
    }

    private MachineEnvironment() {
    }
}
