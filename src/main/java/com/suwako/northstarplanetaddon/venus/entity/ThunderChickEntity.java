package com.suwako.northstarplanetaddon.venus.entity;

import com.suwako.northstarplanetaddon.venus.registry.ModItems;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 雷羽雏鸟（设计草案第 5 章 #6）：16 HP / 7 伤害（连锁 3 目标）/ 速度 0.35 / 飞行。
 *
 * <p>雷暴带的群居飞行怪（4–6 只）。每次攻击对最近的 3 个目标各造成 7 点伤害并附加短暂麻痹
 * （原版「缓慢」）——这就是草案里的「连锁 3 目标」。沿用原版蝙蝠骨架。</p>
 */
public class ThunderChickEntity extends Bat {

    private static final int ZAP_COOLDOWN = 60;
    private static final float ZAP_DAMAGE = 7.0F;
    private static final int CHAIN_TARGETS = 3;

    private int zapCooldown = ZAP_COOLDOWN;

    public ThunderChickEntity(EntityType<? extends ThunderChickEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Bat.createAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.ATTACK_DAMAGE, ZAP_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.FLYING_SPEED, 0.35D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }
        if (--this.zapCooldown > 0) {
            return;
        }
        this.zapCooldown = ZAP_COOLDOWN;
        AABB area = this.getBoundingBox().inflate(6.0D);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != this && entity.isAlive()
                        && !(entity instanceof ThunderChickEntity)
                        && entity instanceof net.minecraft.world.entity.player.Player);
        int hit = 0;
        for (LivingEntity target : targets) {
            if (hit >= CHAIN_TARGETS) {
                break;
            }
            target.hurt(this.damageSources().mobAttack(this), ZAP_DAMAGE);
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            hit++;
        }
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level, DamageSource source,
                                       boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, source, hitByPlayer);
        this.spawnAtLocation(new ItemStack(ModItems.THUNDER_FEATHER.get(), 1 + this.random.nextInt(3)));
        if (this.random.nextFloat() < 0.3F) {
            this.spawnAtLocation(new ItemStack(ModItems.THUNDER_FEATHER.get(), 1));
        }
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    /** 蝙蝠默认会怕光照并躲在洞里，这里让它白天也活动（雷暴带常驻）。 */
    @Override
    public boolean isResting() {
        return false;
    }

    @Override
    public void setResting(boolean resting) {
        // 不进入倒挂状态
    }

    private static boolean isPlayer(Entity entity) {
        return entity instanceof net.minecraft.world.entity.player.Player;
    }
}
