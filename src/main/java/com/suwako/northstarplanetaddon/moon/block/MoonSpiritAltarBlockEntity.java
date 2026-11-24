package com.suwako.northstarplanetaddon.moon.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 月灵石坛的方块实体 —— 唯一职责是播放那段「灵力从石坛汇聚到玩家身上」的粒子。
 *
 * <p>不做持久化：赐福是一次性的即时表现，区块重载后本来就不该继续播放。
 * 服务端每 tick 沿「石坛 → 玩家胸口」的路径、用螺旋收窄的取样点发粒子：
 * 取样的前进位置随剩余 tick 推进，半径越小说明越靠近玩家，视觉上就是「汇聚到人身上」。</p>
 */
public class MoonSpiritAltarBlockEntity extends BlockEntity {

    /** 整段特效的时长（tick）。 */
    public static final int EFFECT_TICKS = 24;

    /** 每 tick 沿路径发出的取样点数。 */
    private static final int SAMPLES_PER_TICK = 3;

    private int effectTicks;
    private UUID target;

    public MoonSpiritAltarBlockEntity(BlockPos pos, BlockState state) {
        super(com.suwako.northstarplanetaddon.moon.registry.NMABlockEntities.MOON_SPIRIT_ALTAR.get(), pos, state);
    }

    /** 由方块在赐福成功时调用，开始播放汇聚特效。 */
    public void beginBlessing(Player player) {
        this.effectTicks = EFFECT_TICKS;
        this.target = player.getUUID();
        this.setChanged();
    }

    /** 服务端每 tick 调用一次。 */
    public void serverTick() {
        if (effectTicks <= 0 || !(level instanceof ServerLevel serverLevel) || target == null) {
            return;
        }
        Player player = serverLevel.getPlayerByUUID(target);
        if (player == null || !player.isAlive()) {
            effectTicks = 0;
            return;
        }

        Vec3 from = Vec3.atCenterOf(worldPosition).add(0.0D, 1.15D, 0.0D);
        Vec3 to = player.position().add(0.0D, 0.9D, 0.0D);
        Vec3 path = to.subtract(from);

        // progress: 0 → 1，特效越接近结束，粒子越靠近玩家。
        double progress = 1.0D - (double) effectTicks / EFFECT_TICKS;

        for (int i = 0; i < SAMPLES_PER_TICK; i++) {
            // 让取样点略微超前，粒子看起来像一条正在飞行的光带而不是一堆静止的点。
            double t = Mth.clamp(progress + i * 0.035D, 0.0D, 1.0D);
            Vec3 point = from.add(path.scale(t));

            // 螺旋收窄：t 越大半径越小，到玩家身上时半径归零 —— 这就是"汇聚"的观感来源。
            double radius = 0.42D * (1.0D - t);
            double angle = t * Math.PI * 6.0D + i * 2.0D;
            point = point.add(Math.cos(angle) * radius, Math.sin(angle) * radius * 0.65D, Math.sin(angle) * radius);

            serverLevel.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        // 每隔几 tick 混一点紫色魔尘，让特效更像"灵力"而不是纯光点。
        if (effectTicks % 4 == 0) {
            double t = Mth.clamp(progress, 0.0D, 1.0D);
            Vec3 point = from.add(path.scale(t));
            serverLevel.sendParticles(ParticleTypes.WITCH, point.x, point.y, point.z, 2, 0.12D, 0.12D, 0.12D, 0.0D);
        }

        effectTicks--;
        if (effectTicks == 0) {
            // 收尾：在玩家身上炸开一小圈光，表示药力已经融入。
            serverLevel.sendParticles(ParticleTypes.END_ROD, to.x, to.y, to.z, 14, 0.28D, 0.42D, 0.28D, 0.02D);
            serverLevel.sendParticles(ParticleTypes.GLOW, to.x, to.y, to.z, 8,
                    0.3D, 0.4D, 0.3D, 0.01D);
        }
    }
}
