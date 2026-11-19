package com.suwako.northstarplanetaddon.venus.entity;

import com.suwako.northstarplanetaddon.venus.registry.ModEffects;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 酸潮水母（设计草案第 5 章 #1）。
 *
 * <p>12 HP / 4 伤害 + 腐蚀 / 护甲 0 / 速度 0.15，漂浮在酸潮滩 y 60–90。
 * 接触目标时造成 4 点伤害并附加 {@code 腐蚀}（10s）。</p>
 */
public class AcidJellyEntity extends PathfinderMob {

    public static final float CONTACT_DAMAGE = 4.0F;
    /** 腐蚀 10s（200 tick）。 */
    public static final int CORROSION_TICKS = 200;

    public AcidJellyEntity(EntityType<? extends AcidJellyEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 40, true);
        this.setNoGravity(true);
    }

    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return net.minecraft.world.entity.Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.15D)
                .add(Attributes.FLYING_SPEED, 0.15D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 0.5D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        // 缓慢飘向目标，接触时造成伤害 + 腐蚀
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            if (this.distanceToSqr(target) < 3.0D) {
                if (this.tickCount % 20 == 0) {
                    // 用 acid 伤害类型：走 #northstar_planet_addon:acid 标签 → ACID 分类，
                    // 钨钢 2 件套的「酸蚀抗性 +30%」也才会生效。
                    target.hurt(com.suwako.northstarplanetaddon.venus.combat.ModDamageSources.acid(this.level(), this), CONTACT_DAMAGE);
                    target.addEffect(new MobEffectInstance(ModEffects.CORROSION, CORROSION_TICKS, 0));
                }
            } else {
                Vec3 delta = target.position().subtract(this.position()).normalize();
                this.setDeltaMovement(this.getDeltaMovement().add(delta.scale(0.01D)).scale(0.99D));
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SLIME_SQUISH_SMALL;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_HURT_SMALL;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH_SMALL;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }
}
