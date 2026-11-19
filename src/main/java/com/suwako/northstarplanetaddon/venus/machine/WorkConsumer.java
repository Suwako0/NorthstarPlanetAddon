package com.suwako.northstarplanetaddon.venus.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 「机械功」消费者（设计草案第 8 章，气动马达的转速等价物）。
 *
 * <p>本模组不引入 Create 的应力体系：气动马达消耗压缩空气产生 32 RPM 的机械功，
 * 通过本接口喂给相邻机器。机器按 {@link #acceptWork} 返回的倍率加速处理。
 * 将来接 Create 时，只需让 Create 的轴也调用本接口（或反过来把马达注册成 Create 动力源）。</p>
 */
public interface WorkConsumer {

    /** 本 tick 是否能吃下机械功（例如已经有配方在工作）。 */
    boolean canAcceptWork();

    /**
     * 接收机械功。
     *
     * @param rpm 转速（气动马达固定 32）
     * @return 获得的处理速度倍率（1.0 = 没有加成）
     */
    float acceptWork(int rpm);

    /** 在六个方向里找相邻的机械功能消费者。 */
    static WorkConsumer findNeighbor(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockEntity blockEntity = level.getBlockEntity(pos.relative(direction));
            if (blockEntity instanceof WorkConsumer consumer && consumer.canAcceptWork()) {
                return consumer;
            }
        }
        return null;
    }
}
