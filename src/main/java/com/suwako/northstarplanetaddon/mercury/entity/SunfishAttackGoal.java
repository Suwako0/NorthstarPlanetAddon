package com.suwako.northstarplanetaddon.mercury.entity;

import com.suwako.northstarplanetaddon.mercury.registry.MNADamageTypes;
import com.suwako.northstarplanetaddon.mercury.world.SunbathManager;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * One goal, three attacks, one random choice per attack.
 * <p>
 * 太阳鱼 picks between {@link #tickDive} (旋转俯冲) and {@link #tickWingFlap} (拍翅散射火焰);
 * 炽烈太阳鱼 picks between the same dive and {@link #tickRay} (连续高能射线). The choice is made
 * once in {@link #start()} with a fresh random roll, so the same fish alternates unpredictably.
 * <p>
 * The goal owns MOVE and LOOK, which locks out the idle wander / look goals while it runs, and is
 * deliberately non-interruptible so an attack always plays out. It never uses pathfinding: the
 * fish is weightless, so the goal writes a desired velocity onto the entity every tick and
 * {@link SunfishEntity#aiStep()} applies it after the vanilla movement step. Damage, particles and
 * sounds are all server-side; only the {@link SunfishAttackMode} is synced, for the client model.
 */
public final class SunfishAttackGoal extends Goal {

    private final SunfishEntity fish;
    private final Set<Integer> rammed = new HashSet<>();

    private SunfishAttackMode mode = SunfishAttackMode.NONE;
    private int tick;
    private int bursts;
    private int shots;
    private Vec3 flapOffset = Vec3.ZERO;

    public SunfishAttackGoal(SunfishEntity fish) {
        this.fish = fish;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    /** The attack is a timed state machine; it must be advanced on every single server tick. */
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /** A wander goal must never cut an attack short. */
    @Override
    public boolean isInterruptable() {
        return false;
    }

    @Override
    public boolean canUse() {
        if (this.fish.level().isClientSide || this.fish.getAttackCooldown() > 0) {
            return false;
        }
        LivingEntity target = this.target();
        return target != null
                && this.fish.distanceToSqr(target) <= SunfishCombatSettings.ATTACK_RANGE * SunfishCombatSettings.ATTACK_RANGE;
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.fish.isAlive() || this.fish.isRemoved() || this.tick >= this.duration()) {
            return false;
        }
        LivingEntity target = this.target();
        return target != null
                && this.fish.distanceToSqr(target)
                <= SunfishCombatSettings.LOSE_TARGET_RANGE * SunfishCombatSettings.LOSE_TARGET_RANGE;
    }

    /** The live target, filtered through the 沐浴阳光 truce. */
    private LivingEntity target() {
        LivingEntity target = this.fish.getTarget();
        return SunbathManager.canSunfishAttack(target) ? target : null;
    }

    @Override
    public void start() {
        this.tick = 0;
        this.bursts = 0;
        this.shots = 0;
        this.rammed.clear();

        boolean dive = this.fish.getRandom().nextBoolean();
        this.mode = dive ? SunfishAttackMode.DIVE
                : (this.fish.usesRayAttack() ? SunfishAttackMode.RAY : SunfishAttackMode.WING_FLAP);

        double angle = this.fish.getRandom().nextDouble() * Mth.TWO_PI;
        this.flapOffset = new Vec3(
                Math.cos(angle) * SunfishCombatSettings.FLAP_HOVER_DISTANCE,
                0.0D,
                Math.sin(angle) * SunfishCombatSettings.FLAP_HOVER_DISTANCE);

        this.fish.setAttackMode(this.mode);
        this.fish.setAttackVelocity(Vec3.ZERO);
        this.fish.getNavigation().stop();
    }

    @Override
    public void stop() {
        this.fish.setAttackMode(SunfishAttackMode.NONE);
        this.fish.setAttackVelocity(Vec3.ZERO);
        int span = SunfishCombatSettings.ATTACK_MAX_COOLDOWN - SunfishCombatSettings.ATTACK_MIN_COOLDOWN;
        this.fish.setAttackCooldown(
                SunfishCombatSettings.ATTACK_MIN_COOLDOWN + this.fish.getRandom().nextInt(span + 1));
        this.mode = SunfishAttackMode.NONE;
        this.fish.getNavigation().stop();
    }

    private int duration() {
        return switch (this.mode) {
            case DIVE -> SunfishCombatSettings.DIVE_WINDUP + SunfishCombatSettings.DIVE_MAX_TICKS;
            case WING_FLAP -> SunfishCombatSettings.FLAP_APPROACH + SunfishCombatSettings.FLAP_WINDUP
                    + SunfishCombatSettings.FLAP_BURST_INTERVAL * SunfishCombatSettings.FLAP_BURSTS + 6;
            case RAY -> SunfishCombatSettings.RAY_WINDUP
                    + SunfishCombatSettings.RAY_INTERVAL * SunfishCombatSettings.RAY_SHOTS + 6;
            default -> 0;
        };
    }

    @Override
    public void tick() {
        if (!(this.fish.level() instanceof ServerLevel server)) {
            return;
        }
        this.tick++;
        LivingEntity target = this.target();
        if (target == null) {
            // The goal selector stops us on its next cleanup pass.
            return;
        }

        switch (this.mode) {
            case DIVE -> this.tickDive(server, target);
            case WING_FLAP -> this.tickWingFlap(server, target);
            case RAY -> this.tickRay(server, target);
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ 旋转俯冲

    /**
     * Winds up in place while the model spins the body, then streaks down at the target's chest.
     * Every living thing it sweeps through is hit once, and the fish pulls out as soon as it reaches
     * the target (or the dive times out) instead of grinding into the ground.
     */
    private void tickDive(ServerLevel server, LivingEntity target) {
        this.fish.getLookControl().setLookAt(target, 60.0F, 60.0F);

        if (this.tick == 1) {
            this.playSound(server, SoundEvents.PHANTOM_SWOOP, 1.1F, 0.8F);
        }
        if (this.tick <= SunfishCombatSettings.DIVE_WINDUP) {
            this.fish.setAttackVelocity(new Vec3(0.0D, 0.05D, 0.0D));
            server.sendParticles(ParticleTypes.SMALL_FLAME,
                    this.fish.getX(), this.fish.getY(), this.fish.getZ(), 4, 0.7D, 0.4D, 0.7D, 0.02D);
            return;
        }

        Vec3 aim = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        Vec3 towards = aim.subtract(this.fish.position());
        double distance = towards.length();
        if (distance > 0.001D) {
            this.fish.setAttackVelocity(towards.scale(SunfishCombatSettings.DIVE_SPEED / distance));
        } else {
            this.fish.setAttackVelocity(Vec3.ZERO);
        }

        // 实体朝向追着目标，模型自身再绕纵轴旋转：看起来是一条打着旋扎下来的鱼。
        this.fish.setYRot(this.fish.getYRot() + SunfishCombatSettings.DIVE_SPIN_DEGREES);
        this.fish.setYHeadRot(this.fish.getYRot());
        server.sendParticles(ParticleTypes.FLAME,
                this.fish.getX(), this.fish.getY(), this.fish.getZ(), 3, 0.3D, 0.3D, 0.3D, 0.03D);

        DamageSource source = MNADamageTypes.source(server, MNADamageTypes.SUNFISH_RAM, this.fish);
        AABB hitbox = this.fish.getBoundingBox().inflate(SunfishCombatSettings.DIVE_HIT_RADIUS);
        for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, hitbox)) {
            if (!SunbathManager.canSunfishAttack(victim) || !this.rammed.add(victim.getId())) {
                continue;
            }
            if (victim.distanceToSqr(this.fish)
                    > SunfishCombatSettings.DIVE_HIT_RADIUS * SunfishCombatSettings.DIVE_HIT_RADIUS) {
                continue;
            }
            victim.hurt(source, this.fish.getAttackDamage());
            victim.setRemainingFireTicks(this.fish.getFireSecondsOnHit() * 20);
            Vec3 away = victim.position().subtract(this.fish.position()).normalize();
            victim.push(away.x * SunfishCombatSettings.DIVE_KNOCKBACK, 0.32D,
                    away.z * SunfishCombatSettings.DIVE_KNOCKBACK);
            this.playSound(server, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.9F, 0.7F);
        }

        if (distance < 1.2D || this.tick - SunfishCombatSettings.DIVE_WINDUP >= SunfishCombatSettings.DIVE_MAX_TICKS) {
            this.tick = this.duration();
        }
    }

    // ------------------------------------------------------------------ 拍翅散射

    /**
     * Flutters to a point beside the player, hovers there beating its wings fast, then throws three
     * expanding rings of flame. Each burst damages everything inside {@code FLAP_RADIUS}.
     */
    private void tickWingFlap(ServerLevel server, LivingEntity target) {
        this.fish.getLookControl().setLookAt(target, 60.0F, 60.0F);

        if (this.tick <= SunfishCombatSettings.FLAP_APPROACH) {
            Vec3 desired = target.position()
                    .add(0.0D, SunfishCombatSettings.FLAP_HOVER_HEIGHT, 0.0D)
                    .add(this.flapOffset);
            Vec3 towards = desired.subtract(this.fish.position());
            double distance = towards.length();
            if (distance > 0.4D) {
                double speed = Math.min(SunfishCombatSettings.FLAP_APPROACH_SPEED, distance * 0.25D);
                this.fish.setAttackVelocity(towards.scale(speed / distance));
            } else {
                this.fish.setAttackVelocity(Vec3.ZERO);
            }
            if (this.tick == 1) {
                this.playSound(server, SoundEvents.ENDER_DRAGON_FLAP, 1.0F, 1.3F);
            }
            if (this.tick % 4 == 0) {
                server.sendParticles(ParticleTypes.SMALL_FLAME,
                        this.fish.getX(), this.fish.getY(), this.fish.getZ(), 2, 0.7D, 0.3D, 0.7D, 0.02D);
            }
            return;
        }

        this.fish.setAttackVelocity(Vec3.ZERO);
        int since = this.tick - SunfishCombatSettings.FLAP_APPROACH;
        if (since <= SunfishCombatSettings.FLAP_WINDUP) {
            if (since % 3 == 0) {
                server.sendParticles(ParticleTypes.SMALL_FLAME,
                        this.fish.getX(), this.fish.getY(), this.fish.getZ(), 3, 0.9D, 0.3D, 0.9D, 0.02D);
            }
            return;
        }

        int burstTick = since - SunfishCombatSettings.FLAP_WINDUP;
        if (burstTick % SunfishCombatSettings.FLAP_BURST_INTERVAL == 0
                && this.bursts < SunfishCombatSettings.FLAP_BURSTS) {
            this.bursts++;
            this.flapBurst(server);
        }
    }

    private void flapBurst(ServerLevel server) {
        double x = this.fish.getX();
        double y = this.fish.getY();
        double z = this.fish.getZ();

        for (int i = 0; i < 24; i++) {
            double angle = (double) i / 24.0D * Mth.TWO_PI;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            server.sendParticles(ParticleTypes.FLAME, x + dx * 0.8D, y, z + dz * 0.8D,
                    1, dx * 0.35D, 0.04D, dz * 0.35D, 0.16D);
        }
        server.sendParticles(ParticleTypes.LAVA, x, y, z, 10, 0.6D, 0.2D, 0.6D, 0.02D);
        this.playSound(server, SoundEvents.ENDER_DRAGON_FLAP, 1.3F, 0.9F);

        DamageSource source = MNADamageTypes.source(server, MNADamageTypes.SUNFISH_FLAME, this.fish);
        double radius = SunfishCombatSettings.FLAP_RADIUS;
        AABB area = this.fish.getBoundingBox().inflate(radius);
        for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, area)) {
            if (!SunbathManager.canSunfishAttack(victim) || victim.distanceToSqr(this.fish) > radius * radius) {
                continue;
            }
            victim.hurt(source, this.fish.getAttackDamage() * SunfishCombatSettings.FLAP_DAMAGE_SCALE);
            victim.setRemainingFireTicks(SunfishCombatSettings.FLAP_FIRE_SECONDS * 20);
        }
    }

    // ------------------------------------------------------------------ 高能射线

    /**
     * Hovers and charges, then fires {@code RAY_SHOTS} beams in a row. A beam is a hitscan from the
     * fish's eye to the target's eye, clipped at the first solid block; it does not stop at the first
     * victim, so it pierces everyone standing in the line and draws a particle trail along the way.
     */
    private void tickRay(ServerLevel server, LivingEntity target) {
        this.fish.getLookControl().setLookAt(target, 60.0F, 60.0F);
        this.fish.setAttackVelocity(Vec3.ZERO);

        if (this.tick <= SunfishCombatSettings.RAY_WINDUP) {
            if (this.tick == 1) {
                this.playSound(server, SoundEvents.BEACON_ACTIVATE, 1.0F, 1.6F);
            }
            if (this.tick % 2 == 0) {
                server.sendParticles(ParticleTypes.END_ROD,
                        this.fish.getX(), this.fish.getEyeY(), this.fish.getZ(),
                        3, 0.35D, 0.35D, 0.35D, 0.04D);
            }
            return;
        }

        int since = this.tick - SunfishCombatSettings.RAY_WINDUP;
        if (since % SunfishCombatSettings.RAY_INTERVAL == 0 && this.shots < SunfishCombatSettings.RAY_SHOTS) {
            this.shots++;
            this.fireRay(server, target);
        }
    }

    private void fireRay(ServerLevel server, LivingEntity target) {
        Vec3 start = new Vec3(this.fish.getX(), this.fish.getEyeY(), this.fish.getZ());
        Vec3 direction = target.getEyePosition().subtract(start).normalize();
        if (direction.lengthSqr() < 1.0E-4D) {
            direction = this.fish.getLookAngle();
        }
        Vec3 end = start.add(direction.scale(SunfishCombatSettings.RAY_LENGTH));

        BlockHitResult wall = server.clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.fish));
        if (wall.getType() != HitResult.Type.MISS) {
            end = wall.getLocation();
        }

        this.drawBeam(server, start, end);
        this.playSound(server, SoundEvents.BLAZE_SHOOT, 1.4F, 0.7F);

        DamageSource source = MNADamageTypes.source(server, MNADamageTypes.SUNFISH_BEAM, this.fish);
        AABB area = new AABB(start, end).inflate(1.0D);
        for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, area)) {
            if (!SunbathManager.canSunfishAttack(victim)) {
                continue;
            }
            if (victim.getBoundingBox().inflate(0.3D).clip(start, end).isEmpty()) {
                continue;
            }
            victim.hurt(source, this.fish.getAttackDamage() * SunfishCombatSettings.RAY_DAMAGE_SCALE);
            victim.setRemainingFireTicks(SunfishCombatSettings.RAY_FIRE_SECONDS * 20);
        }
    }

    /** Draws one beam as a handful of short particles packets instead of one packet per block. */
    private void drawBeam(ServerLevel server, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        int segments = SunfishCombatSettings.RAY_BEAM_SEGMENTS;
        for (int i = 0; i < segments; i++) {
            Vec3 a = start.add(delta.scale((double) i / segments));
            Vec3 b = start.add(delta.scale((double) (i + 1) / segments));
            Vec3 middle = a.add(b).scale(0.5D);
            Vec3 half = b.subtract(a).scale(0.5D);
            double sx = Math.abs(half.x) + 0.02D;
            double sy = Math.abs(half.y) + 0.02D;
            double sz = Math.abs(half.z) + 0.02D;
            server.sendParticles(ParticleTypes.END_ROD, middle.x, middle.y, middle.z, 3, sx, sy, sz, 0.0D);
            if (i % 4 == 0) {
                server.sendParticles(ParticleTypes.SMALL_FLAME, middle.x, middle.y, middle.z, 2, sx, sy, sz, 0.0D);
            }
        }
    }

    private void playSound(ServerLevel server, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        server.playSound(null, this.fish.getX(), this.fish.getY(), this.fish.getZ(),
                sound, SoundSource.HOSTILE, volume, pitch);
    }
}
