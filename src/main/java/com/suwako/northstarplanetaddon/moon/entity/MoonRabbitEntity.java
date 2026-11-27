package com.suwako.northstarplanetaddon.moon.entity;

import com.suwako.northstarplanetaddon.moon.entity.ai.MoonRabbitGoldenCarrotGoal;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import org.jetbrains.annotations.Nullable;

/**
 * 月兔 / Moon Rabbit — a small, peaceful lunar rabbit that hops across the Moonforest floor and
 * shelters under Moonlight trees. It is colour-matched to the pale, moonlit palette of the biome.
 */
public class MoonRabbitEntity extends Animal {

    /** Drives the idle keyframe animation in {@code NMAAnimations}; walking uses animateWalk. */
    public final AnimationState idleAnimationState = new AnimationState();

    /** 月兔拜月 — the occasional mid-Autumn moon gazing pose. */
    public final AnimationState gazeAnimationState = new AnimationState();

    /** Ticks the moon gazing clip lasts; matches {@code MOON_RABBIT_GAZE}'s length. */
    private static final int GAZE_TICKS = 70;

    /** A player holding a golden carrot within this radius makes the rabbit perform its cute pose. */
    private static final double GOLDEN_CARROT_RADIUS = 8.0D;
    /** 2.8 seconds, exactly matching {@code entity_animations/moon_rabbit/golden_carrot_cute.json}. */
    private static final int GOLDEN_CARROT_ACTION_TICKS = 56;
    /** Prevents a nearby player from restarting the action every tick. */
    private static final int GOLDEN_CARROT_COOLDOWN_TICKS = 80;
    private static final EntityDataAccessor<Boolean> GOLDEN_CARROT_CUTE = SynchedEntityData.defineId(
            MoonRabbitEntity.class, EntityDataSerializers.BOOLEAN);
    /** 2.4 seconds, exactly matching {@code entity_animations/moon_rabbit/golden_carrot_eat.json}. */
    private static final int GOLDEN_CARROT_EAT_TICKS = 48;
    private static final EntityDataAccessor<Boolean> EATING_GOLDEN_CARROT = SynchedEntityData.defineId(
            MoonRabbitEntity.class, EntityDataSerializers.BOOLEAN);

    /** The synced cute pose played while a nearby player holds a golden carrot. */
    public final AnimationState goldenCarrotCuteAnimationState = new AnimationState();
    /** The synced eating pose played after the rabbit picks up a dropped golden carrot. */
    public final AnimationState goldenCarrotEatAnimationState = new AnimationState();

    private int idleAnimationTimeout;
    private int gazeCooldown;
    private int gazeUntil;
    private int goldenCarrotActionTicks;
    private int goldenCarrotCooldown;
    private int goldenCarrotEatTicks;

    public MoonRabbitEntity(EntityType<? extends MoonRabbitEntity> entityType, Level level) {
        super(entityType, level);
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
        this.gazeCooldown = this.random.nextInt(200) + 80;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.SAFE_FALL_DISTANCE, 6.0D)
                .add(Attributes.STEP_HEIGHT, 0.6D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GOLDEN_CARROT_CUTE, false);
        builder.define(EATING_GOLDEN_CARROT, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.6D));
        this.goalSelector.addGoal(2, new MoonRabbitGoldenCarrotGoal(this, 1.15D));
        this.goalSelector.addGoal(3, new BreedGoal(this, 0.9D));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.0D, stack -> stack.is(NMAItems.MOON_BLOOM.get()), false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            this.setupAnimationStates();
            this.updateGoldenCarrotAnimationStates();
        } else {
            this.updateGoldenCarrotEating();
            if (!this.isEatingGoldenCarrot()) {
                this.updateGoldenCarrotReaction();
            }
        }
    }

    /** True while the server-synchronised golden-carrot pose is playing. */
    public boolean isGoldenCarrotCuteActionActive() {
        return this.entityData.get(GOLDEN_CARROT_CUTE);
    }

    /** True while the rabbit is consuming a golden carrot it picked up from the ground. */
    public boolean isEatingGoldenCarrot() {
        return this.entityData.get(EATING_GOLDEN_CARROT);
    }

    private void updateGoldenCarrotReaction() {
        if (this.goldenCarrotActionTicks > 0) {
            this.goldenCarrotActionTicks--;
            Player player = this.findGoldenCarrotPlayer();
            if (player != null) {
                this.getNavigation().stop();
                this.getLookControl().setLookAt(player, 30.0F, 30.0F);
            }
            if (this.goldenCarrotActionTicks == 0) {
                this.entityData.set(GOLDEN_CARROT_CUTE, false);
                this.goldenCarrotCooldown = GOLDEN_CARROT_COOLDOWN_TICKS;
            }
            return;
        }

        if (this.goldenCarrotCooldown > 0) {
            this.goldenCarrotCooldown--;
            return;
        }

        Player player = this.findGoldenCarrotPlayer();
        if (player != null) {
            this.goldenCarrotActionTicks = GOLDEN_CARROT_ACTION_TICKS;
            this.entityData.set(GOLDEN_CARROT_CUTE, true);
            this.getNavigation().stop();
            this.getLookControl().setLookAt(player, 30.0F, 30.0F);
        }
    }

    private @Nullable Player findGoldenCarrotPlayer() {
        return this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(GOLDEN_CARROT_RADIUS),
                        player -> player.isAlive() && player.distanceToSqr(this) <= GOLDEN_CARROT_RADIUS * GOLDEN_CARROT_RADIUS
                                && (player.getMainHandItem().is(Items.GOLDEN_CARROT)
                                || player.getOffhandItem().is(Items.GOLDEN_CARROT)))
                .stream()
                .findFirst()
                .orElse(null);
    }

    /** Whether the dropped-item goal may begin a new golden-carrot trade. */
    public boolean canStartGoldenCarrotTrade() {
        // A dropped carrot always takes precedence over the nearby-player cute-pose cooldown: the
        // usual interaction is to hold it briefly and then throw it down for the rabbit.
        return !this.isEatingGoldenCarrot();
    }

    /**
     * Takes exactly one dropped golden carrot, then holds still while its eating animation plays.
     * This is called by {@link MoonRabbitGoldenCarrotGoal}, rather than Mob's generic loot pickup:
     * generic pickup only tries items already in reach and never gives the rabbit a reason to walk to
     * a carrot on the ground.
     */
    public void consumeDroppedGoldenCarrot(ItemEntity itemEntity) {
        if (this.level().isClientSide() || !this.canStartGoldenCarrotTrade()
                || itemEntity.isRemoved() || itemEntity.hasPickUpDelay()
                || !itemEntity.getItem().is(Items.GOLDEN_CARROT)) {
            return;
        }

        ItemStack remaining = itemEntity.getItem();
        remaining.shrink(1);
        this.onItemPickup(itemEntity);
        this.take(itemEntity, 1);
        if (remaining.isEmpty()) {
            itemEntity.discard();
        }

        this.goldenCarrotActionTicks = 0;
        this.entityData.set(GOLDEN_CARROT_CUTE, false);
        this.goldenCarrotEatTicks = GOLDEN_CARROT_EAT_TICKS;
        this.entityData.set(EATING_GOLDEN_CARROT, true);
        this.getNavigation().stop();
    }

    private void updateGoldenCarrotEating() {
        if (this.goldenCarrotEatTicks <= 0) {
            return;
        }

        this.goldenCarrotEatTicks--;
        this.getNavigation().stop();
        if (this.goldenCarrotEatTicks == 0) {
            // The temporary stack gives the rabbit the standard golden-carrot eating sound/particles
            // without consuming a player's inventory stack.
            this.eat(this.level(), new ItemStack(Items.GOLDEN_CARROT));
            this.spawnAtLocation(this.random.nextFloat() < 0.10F
                    ? new ItemStack(NMAItems.MOON_PEARL.get())
                    : new ItemStack(NMAItems.MOONCAKE.get()), 0.45F);
            this.entityData.set(EATING_GOLDEN_CARROT, false);
            this.goldenCarrotCooldown = GOLDEN_CARROT_COOLDOWN_TICKS;
        }
    }

    private void updateGoldenCarrotAnimationStates() {
        updateAnimationState(this.goldenCarrotCuteAnimationState, this.isGoldenCarrotCuteActionActive());
        updateAnimationState(this.goldenCarrotEatAnimationState, this.isEatingGoldenCarrot());
    }

    private void updateAnimationState(AnimationState animationState, boolean active) {
        if (active) {
            if (!animationState.isStarted()) {
                animationState.start(this.tickCount);
            }
        } else {
            animationState.stop();
        }
    }

    private void setupAnimationStates() {
        if (this.idleAnimationTimeout <= 0) {
            this.idleAnimationTimeout = this.random.nextInt(60) + 100;
            this.idleAnimationState.start(this.tickCount);
        } else {
            --this.idleAnimationTimeout;
        }

        // 月兔拜月: every so often the rabbit stops and turns its face to the moon.
        if (this.gazeUntil > 0) {
            if (this.tickCount >= this.gazeUntil) {
                this.gazeUntil = 0;
                this.gazeAnimationState.stop();
            }
        } else if (--this.gazeCooldown <= 0) {
            this.gazeCooldown = this.random.nextInt(500) + 700;
            this.gazeUntil = this.tickCount + GAZE_TICKS;
            this.gazeAnimationState.start(this.tickCount);
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(NMAItems.MOON_BLOOM.get());
    }

    @Override
    public @Nullable MoonRabbitEntity getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return NMAEntityTypes.MOON_RABBIT.get().create(level);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.RABBIT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.RABBIT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RABBIT_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.RABBIT_JUMP, 0.15F, 1.0F);
    }
}
