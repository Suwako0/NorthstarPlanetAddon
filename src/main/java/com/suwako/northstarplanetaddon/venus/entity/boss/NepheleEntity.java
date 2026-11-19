package com.suwako.northstarplanetaddon.venus.entity.boss;

import com.suwako.northstarplanetaddon.venus.combat.WeakPoint;
import com.suwako.northstarplanetaddon.venus.registry.ModEntities;
import com.suwako.northstarplanetaddon.venus.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 涅斐勒，云鲸（设计草案 4.2 节）。
 *
 * <table>
 *   <tr><th>项</th><th>值</th></tr>
 *   <tr><td>本体 HP</td><td>520</td></tr>
 *   <tr><td>护甲板</td><td>4 × 120</td></tr>
 *   <tr><td>阶段</td><td>100–60% 巡航 / 60–25% 雷云 / 25–0% 力竭</td></tr>
 *   <tr><td>弱点</td><td>背部 2 个气囊（P3 暴露）+ 尾部散热口（常驻）</td></tr>
 * </table>
 *
 * <p>招式：云爆弹（8 伤害 / 4 格圆）、尾扫（12 / 正后方 6 格）、俯冲（16 / 直线 20 格）、
 * 召唤云帆鳐（3 只）。冷却与前摇照搬 4.2 节招式表，粒子与音效用于提示前摇
 * （前摇同样是弱点暴露条件）。</p>
 */
public class NepheleEntity extends AbstractBossEntity {

    /** 云爆弹：8 伤害、4 格圆、冷却 8s、前摇 1.0s。 */
    private static final int CLOUD_BOMB_CD = 160;
    private static final int CLOUD_BOMB_WINDUP = 20;
    private static final double CLOUD_BOMB_RADIUS = 4.0D;
    private static final float CLOUD_BOMB_DAMAGE = 8.0F;

    /** 尾扫：12 伤害、正后方 6 格、冷却 10s。 */
    private static final int TAIL_SWEEP_CD = 200;
    private static final float TAIL_SWEEP_DAMAGE = 12.0F;

    /** 召唤云帆鳐：3 只、冷却 20s。 */
    private static final int SUMMON_CD = 400;

    /** 俯冲：16 伤害、直线 20 格、冷却 30s、前摇 1.5s（仅 P2 之后）。 */
    private static final int DIVE_CD = 600;
    private static final int DIVE_WINDUP = 30;
    private static final float DIVE_DAMAGE = 16.0F;

    private int cloudBombTicks = CLOUD_BOMB_CD;
    private int tailSweepTicks = TAIL_SWEEP_CD;
    private int summonTicks = SUMMON_CD;
    private int diveTicks = DIVE_CD;
    private int windupTicks;
    private int windupMove;
    private Vec3 diveDirection = Vec3.ZERO;

    public NepheleEntity(EntityType<? extends NepheleEntity> type, Level level) {
        super(type, level, 4, 120.0F, BossEvent.BossBarColor.PURPLE);
        this.setNoGravity(true);
        this.moveControl = new net.minecraft.world.entity.ai.control.FlyingMoveControl(this, 20, true);
        // 尾部散热口常驻暴露；背部两个气囊只在 P3 暴露
        this.addWeakPoint("tail_vent", new Vec3(0.0D, 0.8D, 4.2D), 1.2D, WeakPoint.ALWAYS);
        this.addWeakPoint("air_sac_left", new Vec3(-1.6D, 2.2D, -0.4D), 1.2D, WeakPoint.PHASE_3);
        this.addWeakPoint("air_sac_right", new Vec3(1.6D, 2.2D, -0.4D), 1.2D, WeakPoint.PHASE_3);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBossAttributes()
                .add(Attributes.MAX_HEALTH, 520.0D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.FLYING_SPEED, 0.35D);
    }

    @Override
    protected float[] phaseThresholds() {
        // 60% / 25%（4.2 节阶段表）
        return new float[]{0.60F, 0.25F};
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 48.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void tickBossAi() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // -------------------------------------------------- 前摇中：站桩蓄力
        if (this.windupTicks > 0) {
            this.windupTicks--;
            this.setDeltaMovement(this.getDeltaMovement().scale(0.8D));
            if (this.windupTicks == 0) {
                this.executeWindup(target);
            }
            return;
        }

        // -------------------------------------------------- 巡航：悬停在目标上方 8 格
        Vec3 desired = target.position().add(0.0D, 8.0D, 0.0D);
        Vec3 delta = desired.subtract(this.position());
        if (delta.lengthSqr() > 9.0D) {
            this.setDeltaMovement(this.getDeltaMovement().add(delta.normalize().scale(0.06D)));
        }
        this.setDeltaMovement(this.getDeltaMovement().scale(0.94D));

        if (--this.cloudBombTicks <= 0) {
            this.cloudBombTicks = CLOUD_BOMB_CD;
            this.startWindup(target, CLOUD_BOMB_WINDUP, 0);
        }
        if (--this.tailSweepTicks <= 0 && this.distanceToSqr(target) < 36.0D) {
            this.tailSweepTicks = TAIL_SWEEP_CD;
            this.tailSweep(target);
        }
        if (--this.summonTicks <= 0) {
            this.summonTicks = SUMMON_CD;
            this.summonRays();
        }
        if (--this.diveTicks <= 0 && this.getPhase() >= 2) {
            this.diveTicks = DIVE_CD;
            this.diveDirection = target.position().subtract(this.position()).normalize();
            this.startWindup(target, DIVE_WINDUP, 1);
        }
    }

    private void startWindup(LivingEntity target, int ticks, int move) {
        this.windupTicks = ticks;
        this.windupMove = move;
        if (this.level() instanceof ServerLevel serverLevel) {
            // 前摇提示：白烟 / 云团（同时也是弱点暴露的视觉提示）
            serverLevel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 2.0D, this.getZ(),
                    12, 1.5D, 0.6D, 1.5D, 0.02D);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.PHANTOM_AMBIENT, SoundSource.HOSTILE,
                    2.0F, 0.7F);
        }
    }

    private void executeWindup(LivingEntity target) {
        if (this.windupMove == 0) {
            this.cloudBomb(target);
        } else {
            this.dive(target);
        }
    }

    /** 云爆弹：落点 4 格圆内 8 点伤害。 */
    private void cloudBomb(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 center = target.position();
        AABB area = new AABB(center.add(-CLOUD_BOMB_RADIUS, -2.0D, -CLOUD_BOMB_RADIUS),
                center.add(CLOUD_BOMB_RADIUS, 2.0D, CLOUD_BOMB_RADIUS));
        List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                entity -> !(entity instanceof AbstractBossEntity));
        for (LivingEntity victim : victims) {
            victim.hurt(serverLevel.damageSources().mobAttack(this), CLOUD_BOMB_DAMAGE);
        }
        serverLevel.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z,
                4, 1.0D, 0.5D, 1.0D, 0.0D);
        serverLevel.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z,
                30, 2.5D, 0.8D, 2.5D, 0.1D);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE,
                3.0F, 0.8F);
    }

    /** 尾扫：正后方 6 格内 12 点伤害 + 击退。 */
    private void tailSweep(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 behind = this.position().subtract(this.getLookAngle().scale(3.0D));
        if (target.position().distanceToSqr(behind) < 36.0D) {
            target.hurt(serverLevel.damageSources().mobAttack(this), TAIL_SWEEP_DAMAGE);
            Vec3 push = target.position().subtract(this.position()).normalize().scale(1.4D);
            target.push(push.x, 0.4D, push.z);
        }
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.RAVAGER_ATTACK, SoundSource.HOSTILE,
                2.0F, 0.6F);
    }

    /** 俯冲：沿直线 20 格，撞到的目标 16 点伤害。 */
    private void dive(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 end = this.position().add(this.diveDirection.scale(20.0D));
        AABB path = this.getBoundingBox().expandTowards(this.diveDirection.scale(20.0D)).inflate(2.0D);
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, path,
                entity -> entity != this && entity.isAlive() && !(entity instanceof AbstractBossEntity))) {
            victim.hurt(serverLevel.damageSources().mobAttack(this), DIVE_DAMAGE);
        }
        this.setPos(end.x, end.y, end.z);
        serverLevel.sendParticles(ParticleTypes.CLOUD, end.x, end.y, end.z, 40, 2.0D, 2.0D, 2.0D, 0.2D);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.PHANTOM_SWOOP, SoundSource.HOSTILE,
                3.0F, 0.6F);
    }

    /** 召唤 3 只云帆鳐（第 2.4 节「枪械清场」职责的靶子）。 */
    private void summonRays() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            var ray = ModEntities.CLOUD_RAY.get().create(serverLevel);
            if (ray == null) {
                continue;
            }
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            ray.moveTo(this.getX() + Math.cos(angle) * 3.0D, this.getY() - 1.0D,
                    this.getZ() + Math.sin(angle) * 3.0D, this.random.nextFloat() * 360.0F, 0.0F);
            serverLevel.addFreshEntity(ray);
        }
        serverLevel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY(), this.getZ(),
                24, 3.0D, 1.0D, 3.0D, 0.05D);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, source, hitByPlayer);
        // 第 4.2 节掉落表（首杀特殊掉落由 M4 的进度 / 纪事系统接上）
        this.spawnAtLocation(new ItemStack(ModItems.WHALE_BONE.get(), 2 + this.random.nextInt(3)));
        this.spawnAtLocation(new ItemStack(ModItems.CLOUD_SAIL_MEMBRANE.get(), 2 + this.random.nextInt(2)));
        this.spawnAtLocation(new ItemStack(ModItems.CLOUD_MAP_FRAGMENT.get(), 1));
        if (this.random.nextFloat() < 0.25F) {
            this.spawnAtLocation(new ItemStack(ModItems.AEROSTAT_CORE.get(), 1));
        }
    }

    @Override
    protected float getSoundVolume() {
        return 3.0F;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    /** 云海生物不该掉进虚空（第 5 章生成规则补充）。 */
    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide && this.getY() < this.level().getMinBuildHeight()) {
            this.setPos(this.getX(), 200.0D, this.getZ());
        }
    }
}
