package com.suwako.northstarplanetaddon.moon.entity;

import com.suwako.northstarplanetaddon.moon.registry.NMAEntityTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/** A non-gravitating, non-terrain-damaging moonlight projectile fired by the Dreamsleep Moon Deer. */
public final class MoonlightBoltEntity extends ThrowableProjectile {

    private static final int MAX_LIFETIME = 100;
    private float damage = 4.0F;

    public MoonlightBoltEntity(EntityType<? extends MoonlightBoltEntity> type, Level level) {
        super(type, level);
    }

    public MoonlightBoltEntity(Level level, MoonDeerEntity owner, double x, double y, double z, float damage) {
        this(NMAEntityTypes.MOONLIGHT_BOLT.get(), level);
        this.setOwner(owner);
        this.setPos(x, y, z);
        this.damage = damage;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0D;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.GLOW, this.getX(), this.getY(), this.getZ(), 3,
                    0.08D, 0.08D, 0.08D, 0.01D);
            serverLevel.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 1,
                    0.02D, 0.02D, 0.02D, 0.0D);
        }
        if (this.tickCount > MAX_LIFETIME) {
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return entity.isAlive() && entity != this.getOwner() && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide() && result.getEntity() instanceof LivingEntity target) {
            target.hurt(this.damageSources().indirectMagic(this, this.getOwner()), this.damage);
        }
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        this.discard();
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        tag.putFloat("Damage", this.damage);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        if (tag.contains("Damage")) {
            this.damage = tag.getFloat("Damage");
        }
    }
}
