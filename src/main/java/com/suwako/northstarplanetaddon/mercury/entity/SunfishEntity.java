package com.suwako.northstarplanetaddon.mercury.entity;

import com.suwako.northstarplanetaddon.mercury.registry.MNADamageTypes;
import com.suwako.northstarplanetaddon.mercury.world.SunbathManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * 太阳鱼 — a fish that drifts through Mercury's sky instead of its seas.
 * <p>
 * It has no gravity at all: instead {@link #hoverAtAltitude()} keeps it inside a personal altitude
 * band a couple of dozen blocks above the ground, which is what makes it read as a sky creature
 * rather than a fish flopping over the surface. Each fish gets its own offset inside that band, so a
 * shoal spreads out vertically instead of stacking on one plane.
 * <p>
 * A storm creature lives in the weather, so the weather must not kill it: it implements
 * {@link SolarStormImmune} (the storm's scorching pass skips it) and it also refuses Northstar's two
 * oxygen-suffocation damage types outright, on top of being listed in
 * {@code #northstar:doesnt_require_oxygen}.
 * <p>
 * Both breeds are hostile: they pick the nearest player themselves and run a random attack every few
 * seconds (see {@link SunfishAttackGoal}). A player with 沐浴阳光 is filtered out at every step —
 * see {@link #canAttack} and {@link #setTarget}. They keep {@code MobCategory.CREATURE} deliberately:
 * that is what keeps a natural shoal to the creature spawn cap instead of blanketing the sky with
 * the much larger monster cap.
 */
public class SunfishEntity extends PathfinderMob implements SolarStormImmune, Enemy {

    /** Northstar's "no oxygen" and "no space suit" suffocation damage. */
    private static final ResourceKey<DamageType> NORTHSTAR_SUFFOCATION_NO_OXYGEN = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("northstar", "suffocation_no_oxygen"));
    private static final ResourceKey<DamageType> NORTHSTAR_SUFFOCATION_NO_SPACESUIT = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("northstar", "suffocation_no_spacesuit"));

    /** Preferred height above the local surface, in blocks. */
    private static final double PREFERRED_ALTITUDE = 24.0D;
    /** Half-height of the band it hovers inside. */
    private static final double ALTITUDE_TOLERANCE = 6.0D;
    /** Vertical correction applied per tick while outside the band. */
    private static final double ALTITUDE_CORRECTION = 0.03D;
    private static final double MAX_VERTICAL_SPEED = 0.12D;
    /** Vertical spread between individual fish. */
    private static final int ALTITUDE_SPREAD = 14;
    /** A dive can leave the fish far below its band; climb back faster than the idle drift would. */
    private static final double RECOVERY_GAP = 10.0D;
    private static final double RECOVERY_MAX_VERTICAL_SPEED = 0.28D;

    /** Synched so the client model can spin / flap / rear back during the matching server attack. */
    private static final EntityDataAccessor<Integer> ATTACK_MODE =
            SynchedEntityData.defineId(SunfishEntity.class, EntityDataSerializers.INT);

    /** Server-only: ticks until the next attack may begin. */
    private int attackCooldown;
    /** Server-only: velocity requested by the running attack for the next movement step. */
    private Vec3 attackVelocity = Vec3.ZERO;

    public SunfishEntity(EntityType<? extends SunfishEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 12, true);
        this.xpReward = 3;
        int span = SunfishCombatSettings.INITIAL_MAX_COOLDOWN - SunfishCombatSettings.INITIAL_MIN_COOLDOWN;
        this.attackCooldown = SunfishCombatSettings.INITIAL_MIN_COOLDOWN + this.random.nextInt(span + 1);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FLYING_SPEED, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ATTACK_MODE, SunfishAttackMode.NONE.getId());
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        // The attack owns MOVE and LOOK, so wandering and looking around cannot interrupt it.
        this.goalSelector.addGoal(0, new SunfishAttackGoal(this));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));

        // 主动锁定玩家；沐浴阳光 / 创造模式 / 旁观者一律被过滤掉。
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true,
                SunbathManager::canSunfishAttack));
    }

    /** 太阳鱼没有重力：高度完全由 {@link #hoverAtAltitude()} 控制。 */
    @Override
    protected double getDefaultGravity() {
        return 0.0D;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }
        if (this.getAttackMode() != SunfishAttackMode.NONE) {
            // Written after super.aiStep(), exactly like hoverAtAltitude(): the vanilla movement
            // step has already run, so this velocity drives the next tick and no move control
            // can overwrite it mid-tick.
            this.setDeltaMovement(this.attackVelocity);
        } else {
            this.hoverAtAltitude();
        }
    }

    /**
     * The cooldown must keep ticking even while no attack goal is running, and a target that gains
     * 沐浴阳光 mid-fight has to be dropped within one tick.
     */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
        LivingEntity current = this.getTarget();
        if (current != null && !SunbathManager.canSunfishAttack(current)) {
            this.setTarget(null);
        }
    }

    /** Drifts toward the fish's own slice of sky, and idles with a slow bob once it is there. */
    private void hoverAtAltitude() {
        BlockPos pos = this.blockPosition();
        int surface = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        double preferred = surface + PREFERRED_ALTITUDE + this.altitudeOffset();

        Vec3 motion = this.getDeltaMovement();
        double dy = motion.y;
        if (this.getY() < preferred - ALTITUDE_TOLERANCE) {
            double ceiling = preferred - this.getY() > RECOVERY_GAP
                    ? RECOVERY_MAX_VERTICAL_SPEED : MAX_VERTICAL_SPEED;
            dy = Math.min(dy + ALTITUDE_CORRECTION, ceiling);
        } else if (this.getY() > preferred + ALTITUDE_TOLERANCE) {
            dy = Math.max(dy - ALTITUDE_CORRECTION, -MAX_VERTICAL_SPEED);
        } else {
            // 在目标高度带内：缓慢平复，并叠一点随波起伏的呼吸感。
            dy = dy * 0.6D + Mth.sin((this.tickCount + this.getId() * 11) * 0.06F) * 0.008D;
        }
        this.setDeltaMovement(motion.x, dy, motion.z);
    }

    /** Stable per-fish offset, so a school spreads out vertically instead of forming one layer. */
    private double altitudeOffset() {
        return (this.getId() * 37 % (ALTITUDE_SPREAD * 2)) - ALTITUDE_SPREAD;
    }

    // ------------------------------------------------------------------ 敌对判定

    /**
     * 沐浴阳光期间不会被太阳鱼 / 炽烈太阳鱼主动攻击。
     * <p>
     * {@code canAttack} covers target acquisition ({@code TargetingConditions}) and target
     * retention ({@code TargetGoal}); overriding {@code setTarget} additionally blocks the direct
     * write {@code HurtByTargetGoal} does when something hits the fish.
     */
    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof Player player && SunbathManager.isSunbathing(player)) {
            return false;
        }
        return super.canAttack(target);
    }

    @Override
    public void setTarget(LivingEntity target) {
        if (target instanceof Player player && SunbathManager.isSunbathing(player)) {
            target = null;
        }
        super.setTarget(target);
    }

    /** Standard hostile-mob behaviour: no aggressive sky fish on Peaceful. */
    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    // ------------------------------------------------------------------ 攻击状态（AI 与客户端模型共用）

    public SunfishAttackMode getAttackMode() {
        return SunfishAttackMode.fromId(this.entityData.get(ATTACK_MODE));
    }

    public void setAttackMode(SunfishAttackMode mode) {
        this.entityData.set(ATTACK_MODE, mode.getId());
    }

    public int getAttackCooldown() {
        return this.attackCooldown;
    }

    public void setAttackCooldown(int ticks) {
        this.attackCooldown = ticks;
    }

    public void setAttackVelocity(Vec3 velocity) {
        this.attackVelocity = velocity;
    }

    /** Base damage for every attack; the individual modes scale it. */
    public float getAttackDamage() {
        return (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    /** 炽烈太阳鱼把第二种攻击换成高能射线，并让命中的目标烧得更久。 */
    protected boolean usesRayAttack() {
        return false;
    }

    protected int getFireSecondsOnHit() {
        return 0;
    }

    /**
     * A sky fish needs no air and fears no solar wind.
     * <p>
     * Northstar's suffocation damage and this addon's own storm damage are refused outright, so a
     * missing datapack tag or a change in Northstar's oxygen logic cannot turn the shoals over Mercury
     * into falling corpses.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(NORTHSTAR_SUFFOCATION_NO_OXYGEN)
                || source.is(NORTHSTAR_SUFFOCATION_NO_SPACESUIT)
                || source.is(MNADamageTypes.SOLAR_STORM)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SALMON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SALMON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SALMON_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6F;
    }
}
