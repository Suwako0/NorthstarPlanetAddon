package com.suwako.northstarplanetaddon.venus.blockentity;

import com.suwako.northstarplanetaddon.venus.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 压力罐（设计草案第 8 章：容量 8000 mB，可堆叠串联，每罐 +8000）。
 *
 * <p>每块压力罐都自带 8000 mB 储量，垂直/水平相邻的罐子之间会自动均压，
 * 所以「叠 3 个罐」= 24000 mB 总容量，与草案的堆叠设计一致。</p>
 */
public class PressureTankBlockEntity extends AbstractMachineBlockEntity {

    public static final int CAPACITY = 8000;
    /** 每 tick 向相邻机器输送的上限。 */
    private static final int PUSH_PER_NEIGHBOR = 500;

    public PressureTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRESSURE_TANK.get(), pos, state, CAPACITY);
    }

    @Override
    protected boolean tickMachine() {
        int pushed = this.pushAirToNeighbors(PUSH_PER_NEIGHBOR);
        return pushed > 0;
    }

    public int getColumnAir() {
        if (this.level == null) {
            return this.getAirStorage().getAir();
        }
        int total = 0;
        BlockPos.MutableBlockPos cursor = this.worldPosition.mutable();
        while (true) {
            if (this.level.getBlockEntity(cursor) instanceof PressureTankBlockEntity tank) {
                total += tank.getAirStorage().getAir();
                cursor.move(0, 1, 0);
            } else {
                break;
            }
        }
        return total;
    }

    public int getColumnCapacity() {
        if (this.level == null) {
            return this.getAirStorage().getCapacity();
        }
        int count = 0;
        BlockPos.MutableBlockPos cursor = this.worldPosition.mutable();
        while (true) {
            if (this.level.getBlockEntity(cursor) instanceof PressureTankBlockEntity) {
                count++;
                cursor.move(0, 1, 0);
            } else {
                break;
            }
        }
        return Math.max(1, count) * CAPACITY;
    }
}
