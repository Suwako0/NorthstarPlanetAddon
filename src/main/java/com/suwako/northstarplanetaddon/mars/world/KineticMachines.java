package com.suwako.northstarplanetaddon.mars.world;

import com.suwako.northstarplanetaddon.mars.MarsModule;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Create 动力方块（齿轮 / 传动轴 / 动力源）的**软集成**工具。
 *
 * <p>通过类名反射识别 {@code com.simibubi.create.content.kinetics.base.KineticBlockEntity}，
 * 不引用 Create 的类，因此缺少 Create 时只会什么都不做。</p>
 *
 * <p>按需求，恶魔蛾的「机械腐蚀」设定已移除：本类只保留两件事——</p>
 * <ul>
 *     <li>{@link #findNearestKinetic}：让恶魔蛾优先扑向正在运转的机械方块；</li>
 *     <li>{@link #destroyNearby}：阿瑞斯自爆时摧毁战场内的机械方块。</li>
 * </ul>
 */
public final class KineticMachines {

    /** 判定为 Create 动力方块的类名。 */
    private static final String KINETIC_BLOCK_ENTITY = "com.simibubi.create.content.kinetics.base.KineticBlockEntity";

    private static Class<?> kineticClass;
    private static boolean kineticLookupDone;

    private KineticMachines() {
    }

    /** 半径内最近的 Create 动力方块（恶魔蛾的取敌目标）。 */
    public static BlockPos findNearestKinetic(ServerLevel level, BlockPos origin, double radius) {
        Class<?> kinetic = kineticClass();
        if (kinetic == null) {
            return null;
        }
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        BlockPos min = BlockPos.containing(origin.getX() - radius, origin.getY() - radius, origin.getZ() - radius);
        BlockPos max = BlockPos.containing(origin.getX() + radius, origin.getY() + radius, origin.getZ() + radius);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity == null || !kinetic.isInstance(blockEntity)) {
                continue;
            }
            double distance = pos.distSqr(origin);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos.immutable();
            }
        }
        return best;
    }

    /** 摧毁半径内所有 Create 动力方块（阿瑞斯自爆时把玩家的工厂一起熔铸）。返回摧毁数量。 */
    public static int destroyNearby(ServerLevel level, BlockPos origin, double radius) {
        Class<?> kinetic = kineticClass();
        if (kinetic == null) {
            return 0;
        }
        int destroyed = 0;
        BlockPos min = BlockPos.containing(origin.getX() - radius, origin.getY() - radius, origin.getZ() - radius);
        BlockPos max = BlockPos.containing(origin.getX() + radius, origin.getY() + radius, origin.getZ() + radius);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity == null || !kinetic.isInstance(blockEntity)) {
                continue;
            }
            level.destroyBlock(pos.immutable(), true);
            destroyed++;
        }
        return destroyed;
    }

    private static Class<?> kineticClass() {
        if (!kineticLookupDone) {
            kineticLookupDone = true;
            try {
                kineticClass = Class.forName(KINETIC_BLOCK_ENTITY);
            } catch (Throwable throwable) {
                MarsModule.LOGGER.info(
                        "Create not present ({}): kinetic machine lookup will be inert", KINETIC_BLOCK_ENTITY);
                kineticClass = null;
            }
        }
        return kineticClass;
    }
}
