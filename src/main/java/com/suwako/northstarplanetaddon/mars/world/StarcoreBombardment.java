package com.suwako.northstarplanetaddon.mars.world;

import com.suwako.northstarplanetaddon.mars.MarsModule;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 星核炮击：在目标点召唤星核，持续轰炸 {@value #DURATION_TICKS} tick（5 秒）。
 *
 * <p>数值来自《Boss属性重新设计》：<b>区域半径 6 格、每 {@value #INTERVAL_TICKS} tick 一次、
 * 每次 {@value #DAMAGE} 点伤害</b>（5 秒共 10 次）。</p>
 *
 * <p>为什么不用实体：轰炸只是「一个位置 + 一个计时器」，做成实体要额外注册实体类型、
 * 渲染器与贴图，收益为零。这里用一个服务端静态列表，由 {@code event.ServerEvents} 每 tick 推进。</p>
 */
public final class StarcoreBombardment {

    public static final int DURATION_TICKS = 100;
    public static final int INTERVAL_TICKS = 10;
    public static final float DAMAGE = 20.0F;
    public static final double RADIUS = 6.0D;

    private static final List<Instance> ACTIVE = new ArrayList<>();

    private static final class Instance {
        final ServerLevel level;
        final Vec3 center;
        final Entity owner;
        int ticksLeft = DURATION_TICKS;
        int nextBlast;

        Instance(ServerLevel level, Vec3 center, Entity owner) {
            this.level = level;
            this.center = center;
            this.owner = owner;
        }

        boolean owns(Entity entity) {
            return this.owner == entity;
        }

        boolean isSameLevel(ServerLevel other) {
            return this.level == other;
        }
    }

    private StarcoreBombardment() {
    }

    /** 是否有属于该玩家的轰炸正在进行（用于「一次只能放一个」）。 */
    public static boolean isActiveFor(Entity owner) {
        for (Instance instance : ACTIVE) {
            if (instance.owns(owner)) {
                return true;
            }
        }
        return false;
    }

    public static void start(ServerLevel level, Vec3 center, Entity owner) {
        ACTIVE.add(new Instance(level, center, owner));
        MarsModule.LOGGER.info("Starcore bombardment started at {} by {}",
                center, owner == null ? "?" : owner.getName().getString());
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.2F, 0.6F);
    }

    /** 由 {@code event.ServerEvents} 每服务端 tick 调用。 */
    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Instance> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            Instance instance = iterator.next();
            if (instance.ticksLeft <= 0) {
                iterator.remove();
                continue;
            }
            instance.ticksLeft--;
            if (--instance.nextBlast <= 0) {
                instance.nextBlast = INTERVAL_TICKS;
                blast(instance);
            } else {
                telegraph(instance);
            }
        }
    }

    public static void clear() {
        ACTIVE.clear();
    }

    private static void telegraph(Instance instance) {
        ServerLevel level = instance.level;
        double t = (DURATION_TICKS - instance.ticksLeft) * 0.35D;
        double radius = Math.min(RADIUS, 1.0D + t);
        for (int i = 0; i < 6; i++) {
            double angle = i * Math.PI / 3.0D + t * 0.2D;
            level.sendParticles(ParticleTypes.END_ROD,
                    instance.center.x + Math.cos(angle) * radius, instance.center.y + 0.4D,
                    instance.center.z + Math.sin(angle) * radius, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void blast(Instance instance) {
        ServerLevel level = instance.level;
        Vec3 center = instance.center;
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 3, 3.0D, 0.6D, 3.0D, 0.0D);
        level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.5D, center.z, 60, RADIUS * 0.6D, 1.0D, RADIUS * 0.6D, 0.03D);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.6F, 0.8F);

        AABB area = new AABB(center, center).inflate(RADIUS);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            if (entity == instance.owner) {
                continue;   // 不炸自己
            }
            if (instance.owner instanceof Player player && player.isAlliedTo(entity)) {
                continue;   // 不炸友军
            }
            entity.hurt(level.damageSources().explosion(null, instance.owner), DAMAGE);
            entity.setRemainingFireTicks(60);
        }
    }
}
