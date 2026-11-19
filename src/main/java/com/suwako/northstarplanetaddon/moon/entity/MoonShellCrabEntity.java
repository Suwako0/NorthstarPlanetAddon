package com.suwako.northstarplanetaddon.moon.entity;

import com.suwako.northstarplanetaddon.moon.registry.NMAEntityTypes;
import com.suwako.northstarplanetaddon.moon.registry.NMAItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 月壳蟹 / Moon Shell Crab — peaceful while left alone, but it retaliates only against a living
 * attacker. A provoked crab uses a heavy claw pinch up close and tosses a small arcing handful of
 * moon dust at mid-range targets.
 */
public class MoonShellCrabEntity extends Animal {

    private static final int GAZE_TICKS = 70;
    private static final int PINCH_TICKS = 14;
    private static final int PINCH_HIT_TICK = 6;
    private static final int PINCH_COOLDOWN = 30;
    private static final int THROW_TICKS = 18;
    private static final int THROW_RELEASE_TICK = 7;
    private static final int THROW_COOLDOWN = 50;
    private static final double PINCH_RANGE = 4.0D;
    private static final double THROW_RANGE = 10.0D;

    private static final EntityDataAccessor<Byte> COMBAT_ANIMATION = SynchedEntityData.defineId(
            MoonShellCrabEntity.class, EntityDataSerializers.BYTE);

    /** Drives the idle keyframe animation in {@code NMAAnimations}; walking uses animateWalk. */
    public final AnimationState idleAnimationState = new AnimationState();
    /** 月壳蟹举螯望月 — the occasional mid-Autumn moon gazing pose. */
    public final AnimationState gazeAnimationState = new AnimationState();
    /** Synced claw strike animation; its damage frame is decided only on the server. */
    public final AnimationState pinchAnimationState = new AnimationState();
    /** Synced moon-dust throw animation; the projectile is created only on the server. */
    public final AnimationState throwAnimationState = new AnimationState();

    private int idleAnimationTimeout;
    private int gazeCooldown;
    private int gazeUntil;
    private int combatTicks;
    private int pinchCooldown;
    private int throwCooldown;

    public MoonShellCrabEntity(EntityType<? extends MoonShellCrabEntity> entityType, Level level) {
        super(entityType, level);
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
        this.gazeCooldown = this.random.nextInt(240) + 100;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.18D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4D)
                .add(Attributes.FOLLOW_RANGE, THROW_RANGE)
                .add(Attributes.STEP_HEIGHT, 0.6D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COMBAT_ANIMATION, (byte) MoonShellCrabAnimation.IDLE.ordinal());
    }

    public MoonShellCrabAnimation getCombatAnimation() {
        return MoonShellCrabAnimation.byId(this.entityData.get(COMBAT_ANIMATION));
    }

    private void setCombatAnimation(MoonShellCrabAnimation animation) {
        this.entityData.set(COMBAT_ANIMATION, (byte) animation.ordinal());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Do not add a nearest-player target. This sole target goal makes aggression originate only
        // from the actual living source that damaged this crab.
        this.goalSelector.addGoal(2, new BreedGoal(this, 0.8D));
        this.goalSelector.addGoal(3, new TemptGoal(this, 0.9D, stack -> stack.is(NMAItems.MOON_BLOOM.get()), false));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.setupAnimationStates();
            this.updateCombatAnimationStates();
        } else {
            this.tickCombat();
        }
    }

    private void tickCombat() {
        if (this.pinchCooldown > 0) this.pinchCooldown--;
        if (this.throwCooldown > 0) this.throwCooldown--;

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            cancelCombat();
            return;
        }

        if (this.combatTicks > 0) {
            this.combatTicks--;
            this.getNavigation().stop();
            this.getLookControl().setLookAt(target, 50.0F, 30.0F);
            if (this.getCombatAnimation() == MoonShellCrabAnimation.PINCH && this.combatTicks == PINCH_TICKS - PINCH_HIT_TICK) {
                resolvePinch(target);
            }
            if (this.getCombatAnimation() == MoonShellCrabAnimation.THROW && this.combatTicks == THROW_TICKS - THROW_RELEASE_TICK) {
                throwMoonDust(target);
            }
            if (this.combatTicks == 0) {
                setCombatAnimation(MoonShellCrabAnimation.IDLE);
            }
            return;
        }

        double distance = this.distanceTo(target);
        if (distance <= PINCH_RANGE && this.pinchCooldown == 0) {
            this.combatTicks = PINCH_TICKS;
            this.pinchCooldown = PINCH_COOLDOWN;
            this.setCombatAnimation(MoonShellCrabAnimation.PINCH);
            this.playSound(SoundEvents.TURTLE_HURT, 0.55F, 1.45F);
            return;
        }
        if (distance <= THROW_RANGE && this.throwCooldown == 0) {
            this.combatTicks = THROW_TICKS;
            this.throwCooldown = THROW_COOLDOWN;
            this.setCombatAnimation(MoonShellCrabAnimation.THROW);
            this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6F, 1.55F);
            return;
        }
        this.getNavigation().moveTo(target, 0.85D);
    }

    private void resolvePinch(LivingEntity target) {
        if (this.distanceTo(target) <= PINCH_RANGE && this.hasLineOfSight(target)) {
            this.doHurtTarget(target);
            Vec3 knockback = target.position().subtract(this.position()).normalize();
            target.push(knockback.x * 0.45D, 0.12D, knockback.z * 0.45D);
            this.playSound(SoundEvents.TURTLE_HURT, 0.8F, 0.9F);
        }
    }

    private void throwMoonDust(LivingEntity target) {
        Vec3 origin = this.getEyePosition().add(this.getLookAngle().scale(0.55D));
        MoonDustProjectileEntity dust = new MoonDustProjectileEntity(this.level(), this, origin.x, origin.y, origin.z);
        Vec3 direction = target.getEyePosition().subtract(origin).normalize();
        dust.shoot(direction.x, direction.y + 0.16D, direction.z, 0.65F, 4.0F);
        this.level().addFreshEntity(dust);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT, origin.x, origin.y, origin.z,
                    10, 0.18D, 0.18D, 0.18D, 0.02D);
        }
        this.playSound(SoundEvents.SNOWBALL_THROW, 0.65F, 1.25F);
    }

    private void cancelCombat() {
        if (this.combatTicks != 0 || this.getCombatAnimation() != MoonShellCrabAnimation.IDLE) {
            this.combatTicks = 0;
            this.setCombatAnimation(MoonShellCrabAnimation.IDLE);
        }
    }

    private void updateCombatAnimationStates() {
        updateAnimationState(this.pinchAnimationState, this.getCombatAnimation() == MoonShellCrabAnimation.PINCH);
        updateAnimationState(this.throwAnimationState, this.getCombatAnimation() == MoonShellCrabAnimation.THROW);
    }

    private void updateAnimationState(AnimationState animationState, boolean active) {
        if (active) {
            if (!animationState.isStarted()) animationState.start(this.tickCount);
        } else {
            animationState.stop();
        }
    }

    private void setupAnimationStates() {
        if (this.idleAnimationTimeout <= 0) {
            this.idleAnimationTimeout = this.random.nextInt(80) + 120;
            this.idleAnimationState.start(this.tickCount);
        } else {
            --this.idleAnimationTimeout;
        }

        if (this.combatTicks > 0) {
            this.gazeAnimationState.stop();
            return;
        }
        if (this.gazeUntil > 0) {
            if (this.tickCount >= this.gazeUntil) {
                this.gazeUntil = 0;
                this.gazeAnimationState.stop();
            }
        } else if (--this.gazeCooldown <= 0) {
            this.gazeCooldown = this.random.nextInt(500) + 800;
            this.gazeUntil = this.tickCount + GAZE_TICKS;
            this.gazeAnimationState.start(this.tickCount);
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(NMAItems.MOON_BLOOM.get());
    }

    @Override
    public @Nullable MoonShellCrabEntity getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return NMAEntityTypes.MOON_SHELL_CRAB.get().create(level);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.isBaby() ? SoundEvents.TURTLE_AMBIENT_LAND : SoundEvents.SQUID_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.TURTLE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.TURTLE_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.SLIME_SQUISH_SMALL, 0.12F, 1.4F);
    }
}
