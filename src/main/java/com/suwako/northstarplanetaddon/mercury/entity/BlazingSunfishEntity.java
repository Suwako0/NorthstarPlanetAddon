package com.suwako.northstarplanetaddon.mercury.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * 炽烈太阳鱼 — the solar storm's own breed of 太阳鱼.
 * <p>
 * Same silhouette and same lazy glide, but armoured, fireproof and burning: it hits back, its bite
 * sets the target alight, and it trails flame particles wherever it swims. Its model adds a crest,
 * flank plates and tail flames on top of the base skeleton, and its texture is drawn from the same
 * atlas with a molten palette.
 * <p>
 * It shares the hostile targeting and the spinning dive with 太阳鱼, but replaces the wing-flap
 * burst with a volley of piercing high-energy rays (see {@link SunfishAttackGoal}).
 */
public class BlazingSunfishEntity extends SunfishEntity {

    /** How long the bite keeps a target burning, in seconds. */
    private static final int BITE_FIRE_SECONDS = 6;

    public BlazingSunfishEntity(EntityType<? extends BlazingSunfishEntity> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    /**
     * 比普通太阳鱼更耐打、更疼：后者是 12 HP / 3 攻击。同名属性重复 add 会覆盖基础值，
     * 所以这里覆写的是 {@link SunfishEntity#createAttributes()} 里对应的两项。
     */
    public static AttributeSupplier.Builder createAttributes() {
        return SunfishEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 36.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.FLYING_SPEED, 0.62D);
    }

    /** 第二种攻击是连续高能射线，而不是拍翅散射。 */
    @Override
    protected boolean usesRayAttack() {
        return true;
    }

    /** 俯冲与射线命中后额外点燃目标。 */
    @Override
    protected int getFireSecondsOnHit() {
        return BITE_FIRE_SECONDS;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    /** A bite that leaves the victim burning. */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.setRemainingFireTicks(BITE_FIRE_SECONDS * 20);
        }
        return hit;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            if (this.tickCount % 3 == 0) {
                this.level().addParticle(ParticleTypes.SMALL_FLAME,
                        this.getRandomX(0.9D), this.getRandomY(), this.getRandomZ(0.9D),
                        0.0D, 0.01D, 0.0D);
            }
            if (this.tickCount % 12 == 0) {
                this.level().addParticle(ParticleTypes.LAVA,
                        this.getRandomX(0.7D), this.getRandomY(), this.getRandomZ(0.7D),
                        0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BLAZE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BLAZE_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.5F;
    }

    @Override
    public float getVoicePitch() {
        return 1.4F;
    }
}
