package com.suwako.northstarplanetaddon.venus.entity.boss;

import com.suwako.northstarplanetaddon.venus.combat.WeakPoint;
import com.suwako.northstarplanetaddon.venus.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;


/**
 * 赫菲斯托斯巨像（设计草案 4.3 节）：地表层 · 阵地战。
 *
 * <table>
 *   <tr><th>项</th><th>值</th></tr>
 *   <tr><td>本体 HP</td><td>800</td></tr>
 *   <tr><td>护甲板</td><td>6 × 150</td></tr>
 *   <tr><td>阶段</td><td>100–66% 熔岩拳 / 66–33% 蒸汽 / 33–0% 炉心过载</td></tr>
 *   <tr><td>弱点</td><td>背部炉心（仅 P3 暴露）+ 双肩散热口（常驻）</td></tr>
 * </table>
 *
 * <h2>招式（4.3 节招式表）</h2>
 * <ul>
 *   <li>熔岩拳 16 / 前摇 0.9s / 冷却 6s / 前方 5×5</li>
 *   <li>熔渣投掷 8 + 火 4s / 前摇 1.2s / 冷却 10s / 落点 3 格</li>
 *   <li>蒸汽喷发 10 + 击退 / 前摇 2s / 冷却 5s / 4 个喷口 ——
 *       <b>喷口被实心方块压住 = 封堵成功，Boss 立刻开窗 4s</b>（设计草案 P2 机制）</li>
 *   <li>地震踏 14 / 前摇 1.4s / 冷却 18s / 半径 8（跳起可躲）</li>
 *   <li>过载爆发 20 / 前摇 3s / 冷却 40s / 半径 12（仅 P3）</li>
 * </ul>
 */
public class HephaestusEntity extends AbstractBossEntity {

    // ---------------------------------------------------------------- 招式参数
    private static final int FIST_CD = 120;
    private static final int FIST_WINDUP = 18;
    private static final float FIST_DAMAGE = 16.0F;
    private static final double FIST_REACH = 6.0D;

    private static final int SLAG_CD = 200;
    private static final int SLAG_WINDUP = 24;
    private static final float SLAG_DAMAGE = 8.0F;
    private static final double SLAG_RADIUS = 3.0D;
    private static final int SLAG_FIRE_TICKS = 80;

    private static final int STOMP_CD = 360;
    private static final int STOMP_WINDUP = 28;
    private static final float STOMP_DAMAGE = 14.0F;
    private static final double STOMP_RADIUS = 8.0D;

    private static final int STEAM_CD = 100;
    private static final int STEAM_WINDUP = 40;
    private static final float STEAM_DAMAGE = 10.0F;
    private static final int STEAM_COLUMN_HEIGHT = 8;
    private static final int STEAM_SEAL_WINDOW = 80;

    private static final int BURST_CD = 800;
    private static final int BURST_WINDUP = 60;
    private static final float BURST_DAMAGE = 20.0F;
    private static final double BURST_RADIUS = 12.0D;

    /** 4 个蒸汽喷口相对 Boss 的水平偏移。 */
    private static final int[][] VENTS = {{3, 0}, {-3, 0}, {0, 3}, {0, -3}};

    private int fistTicks = FIST_CD;
    private int slagTicks = SLAG_CD;
    private int stompTicks = STOMP_CD;
    private int steamTicks = STEAM_CD;
    private int burstTicks = BURST_CD;
    private int windupTicks;
    private int windupMove;
    private Vec3 slagTarget = Vec3.ZERO;
    private int heatAuraTicks;

    public HephaestusEntity(EntityType<? extends HephaestusEntity> type, Level level) {
        super(type, level, 6, 150.0F, BossEvent.BossBarColor.RED);
        // 双肩散热口常驻；背部炉心只在 P3 暴露
        this.addWeakPoint("vent_left", new Vec3(-2.6D, 3.4D, 0.0D), 1.2D, WeakPoint.ALWAYS);
        this.addWeakPoint("vent_right", new Vec3(2.6D, 3.4D, 0.0D), 1.2D, WeakPoint.ALWAYS);
        this.addWeakPoint("core", new Vec3(0.0D, 2.4D, 1.6D), 1.5D, WeakPoint.PHASE_3);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBossAttributes()
                .add(Attributes.MAX_HEALTH, 800.0D)
                .add(Attributes.ATTACK_DAMAGE, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.18D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 30.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void registerGoals() {
        // 移动由 tickBossAi 直接驱动：巨像只会缓慢逼近，不做绕圈寻路
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
        this.getLookControl().setLookAt(target, 20.0F, 20.0F);

        // -------------------------------------------------- 前摇中
        if (this.windupTicks > 0) {
            this.windupTicks--;
            this.getNavigation().stop();
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
            if (this.windupTicks == 0) {
                this.executeWindup(target);
            }
            return;
        }

        // -------------------------------------------------- 缓慢逼近
        double distance = this.distanceTo(target);
        if (distance > 2.5D) {
            this.getNavigation().moveTo(target, 1.0D);
        }
        this.tickPhaseEffects(target);

        // -------------------------------------------------- 招式选择
        if (--this.fistTicks <= 0 && distance <= FIST_REACH) {
            this.fistTicks = FIST_CD;
            this.startWindup(FIST_WINDUP, 0);
            return;
        }
        if (--this.slagTicks <= 0) {
            this.slagTicks = SLAG_CD;
            this.slagTarget = target.position();
            this.startWindup(SLAG_WINDUP, 1);
            return;
        }
        if (--this.stompTicks <= 0 && distance <= STOMP_RADIUS) {
            this.stompTicks = STOMP_CD;
            this.startWindup(STOMP_WINDUP, 2);
            return;
        }
        if (this.getPhase() >= 2 && --this.steamTicks <= 0) {
            this.steamTicks = STEAM_CD;
            this.startWindup(STEAM_WINDUP, 3);
            return;
        }
        if (this.getPhase() >= 3 && --this.burstTicks <= 0) {
            this.burstTicks = BURST_CD;
            this.startWindup(BURST_WINDUP, 4);
        }
    }

    private void startWindup(int ticks, int move) {
        this.windupTicks = ticks;
        this.windupMove = move;
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE,
                    2.5F, this.windupMove == 3 ? 1.4F : 0.6F);
            serverLevel.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY() + 3.0D, this.getZ(),
                    14, 1.2D, 1.0D, 1.2D, 0.0D);
        }
        // 蒸汽前摇：4 个喷口冒白烟（玩家必须在这 2s 内把喷口压住）
        if (this.windupMove == 3) {
            this.markVents(ParticleTypes.CLOUD, 8);
        }
    }

    private void executeWindup(LivingEntity target) {
        switch (this.windupMove) {
            case 0 -> this.lavaFist();
            case 1 -> this.slagThrow();
            case 2 -> this.earthquakeStomp();
            case 3 -> this.steamEruption();
            case 4 -> this.overloadBurst();
            default -> {
            }
        }
    }

    /** 熔岩拳：前方 5×5、16 点伤害。 */
    private void lavaFist() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 look = this.getLookAngle().scale(2.5D);
        AABB area = this.getBoundingBox().inflate(1.0D).move(look.x, 0.0D, look.z).inflate(1.5D);
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != this && entity.isAlive() && !(entity instanceof AbstractBossEntity))) {
            victim.hurt(serverLevel.damageSources().mobAttack(this), FIST_DAMAGE);
            victim.push(look.x * 0.6D, 0.4D, look.z * 0.6D);
        }
        serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX() + look.x, this.getY() + 2.0D,
                this.getZ() + look.z, 3, 1.0D, 0.6D, 1.0D, 0.0D);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 2.0F, 0.7F);
    }

    /** 熔渣投掷：落点 3 格内 8 点伤害 + 点燃 4s。 */
    private void slagThrow() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 center = this.slagTarget;
        AABB area = new AABB(center.add(-SLAG_RADIUS, -2.0D, -SLAG_RADIUS), center.add(SLAG_RADIUS, 2.0D, SLAG_RADIUS));
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != this && entity.isAlive() && !(entity instanceof AbstractBossEntity))) {
            victim.hurt(serverLevel.damageSources().mobAttack(this), SLAG_DAMAGE);
            victim.igniteForSeconds(SLAG_FIRE_TICKS / 20.0F);
        }
        serverLevel.sendParticles(ParticleTypes.LAVA, center.x, center.y + 0.4D, center.z,
                25, 1.5D, 0.4D, 1.5D, 0.0D);
        serverLevel.playSound(null, BlockPos.containing(center), SoundEvents.LAVA_POP, SoundSource.HOSTILE,
                1.6F, 0.8F);
    }

    /** 地震踏：半径 8、14 点伤害；离地（跳跃中）的目标免疫。 */
    private void earthquakeStomp() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        AABB area = this.getBoundingBox().inflate(STOMP_RADIUS);
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != this && entity.isAlive() && !(entity instanceof AbstractBossEntity))) {
            if (!victim.onGround()) {
                continue; // 跳出地面可躲
            }
            victim.hurt(serverLevel.damageSources().mobAttack(this), STOMP_DAMAGE);
            victim.push(0.0D, 0.6D, 0.0D);
        }
        serverLevel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.2D, this.getZ(),
                60, STOMP_RADIUS, 0.3D, STOMP_RADIUS, 0.05D);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 3.0F, 0.4F);
    }

    /**
     * 蒸汽喷发（P2 核心机制）：4 个喷口。
     *
     * <p>喷口位置若被实心方块压住（例如玩家在 2s 前摇里放下的冰 / 云岩），
     * 该喷口视为<b>封堵成功</b>：Boss 立刻开窗 4s，且不造成伤害。
     * 全部 4 个都被封堵时额外再开一次窗。</p>
     */
    private void steamEruption() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int sealed = 0;
        for (int[] vent : VENTS) {
            BlockPos ventPos = BlockPos.containing(this.getX() + vent[0], this.getY(), this.getZ() + vent[1]);
            BlockPos above = ventPos.above();
            if (!serverLevel.getBlockState(above).isAir()
                    && !serverLevel.getBlockState(above).getFluidState().is(Fluids.FLOWING_WATER)) {
                sealed++;
                continue;
            }
            AABB column = new AABB(ventPos.getX(), ventPos.getY(), ventPos.getZ(),
                    ventPos.getX() + 1.0D, ventPos.getY() + STEAM_COLUMN_HEIGHT, ventPos.getZ() + 1.0D);
            for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, column,
                    entity -> entity != this && entity.isAlive() && !(entity instanceof AbstractBossEntity))) {
                victim.hurt(serverLevel.damageSources().mobAttack(this), STEAM_DAMAGE);
                victim.push(0.0D, 0.8D, 0.0D);
            }
            serverLevel.sendParticles(ParticleTypes.CLOUD, ventPos.getX() + 0.5D, ventPos.getY() + 1.0D,
                    ventPos.getZ() + 0.5D, 20, 0.3D, 3.0D, 0.3D, 0.05D);
            serverLevel.playSound(null, ventPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1.2F, 1.2F);
        }
        if (sealed > 0) {
            this.openWindow(STEAM_SEAL_WINDOW);
            serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 4.0D, this.getZ(),
                    20, 1.5D, 1.0D, 1.5D, 0.1D);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.HOSTILE,
                    1.0F, 0.6F);
        }
    }

    /** 过载爆发（P3）：半径 12、20 点伤害。 */
    private void overloadBurst() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        AABB area = this.getBoundingBox().inflate(BURST_RADIUS);
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != this && entity.isAlive() && !(entity instanceof AbstractBossEntity))) {
            victim.hurt(serverLevel.damageSources().mobAttack(this), BURST_DAMAGE);
            victim.push(0.0D, 1.0D, 0.0D);
        }
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 4.0D,
                this.getZ(), 6, BURST_RADIUS * 0.5D, 2.0D, BURST_RADIUS * 0.5D, 0.0D);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 4.0F, 0.5F);
    }

    /** P3 热浪光环：全场 +80°C 的玩家侧表现（缓慢灼烧，无火焰抗性时生效）。 */
    private void tickPhaseEffects(LivingEntity target) {
        if (this.getPhase() < 3 || this.level().isClientSide) {
            return;
        }
        if (--this.heatAuraTicks > 0) {
            return;
        }
        this.heatAuraTicks = 20;
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (Player player : serverLevel.getPlayers(p -> p.isAlive() && !p.isSpectator()
                && p.distanceToSqr(this) < 16.0D * 16.0D)) {
            if (!player.hasEffect(MobEffects.FIRE_RESISTANCE)) {
                player.hurt(serverLevel.damageSources().onFire(), 1.0F);
                player.igniteForSeconds(2.0F);
            }
        }
        serverLevel.sendParticles(ParticleTypes.SMALL_FLAME, this.getX(), this.getY() + 2.0D, this.getZ(),
                20, 6.0D, 2.0D, 6.0D, 0.01D);
    }

    private void markVents(net.minecraft.core.particles.SimpleParticleType particle, int count) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int[] vent : VENTS) {
            serverLevel.sendParticles(particle, this.getX() + vent[0], this.getY() + 0.5D, this.getZ() + vent[1],
                    count, 0.2D, 0.8D, 0.2D, 0.02D);
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, source, hitByPlayer);
        // 第 4.3 节掉落表：炉心 → 巨像核心碎片；熔火齿轮暂用前文明合金替代（该物品未注册）
        this.spawnAtLocation(new ItemStack(ModItems.CORE_SHARD.get(), 2 + this.random.nextInt(2)));
        this.spawnAtLocation(new ItemStack(ModItems.ACID_PROOF_ALLOY_PLATE.get(), 3 + this.random.nextInt(3)));
        this.spawnAtLocation(new ItemStack(ModItems.PRE_CIVILIZATION_ALLOY.get(), 1 + this.random.nextInt(2)));
        if (this.random.nextFloat() < 0.25F) {
            this.spawnAtLocation(new ItemStack(ModItems.CORE_SHARD.get(), 1));
        }
    }

    @Override
    protected float getSoundVolume() {
        return 3.0F;
    }

    /** 巨像不会被击退、不会被船/矿车带走（基类已免疫击退与载具）。 */
    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide && this.getY() < this.level().getMinBuildHeight()) {
            this.setPos(this.getX(), 200.0D, this.getZ());
        }
    }

    /** 让「P3 炉心暴露」的读条状态可用于调试输出。 */
    @Override
    public String debugLines() {
        return super.debugLines() + " hephaestusPhase=" + this.getPhase()
                + " windup=" + this.windupTicks + "/" + this.windupMove
                + " steam=" + this.steamTicks;
    }
}
