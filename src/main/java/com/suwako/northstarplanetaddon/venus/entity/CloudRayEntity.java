package com.suwako.northstarplanetaddon.venus.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 云帆鳐（设计草案第 5 章 #5）。
 *
 * <p>25 HP / 5 伤害 / 护甲 2 / 速度 0.4，飞行。云海群岛的被动生物，
 * 是枪械玩家「职责 A 清杂兵」的主要靶子。</p>
 *
 * <p>实现说明：用 {@link PathfinderMob} + {@link FlyingPathNavigation} + {@link FlyingMoveControl}
 * 组合（与蜜蜂 / 鹦鹉一致），这样 {@code WaterAvoidingRandomFlyingGoal} 这类
 * 寻路型目标才能正常工作；纯 {@code FlyingMob} 只能写类似恶魂的自定义 Goal。</p>
 */
public class CloudRayEntity extends PathfinderMob {

    public CloudRayEntity(EntityType<? extends CloudRayEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return net.minecraft.world.entity.Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 25.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.4D)
                .add(Attributes.FLYING_SPEED, 0.4D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.4D));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 0.9D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PHANTOM_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PHANTOM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PHANTOM_DEATH;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }
}
