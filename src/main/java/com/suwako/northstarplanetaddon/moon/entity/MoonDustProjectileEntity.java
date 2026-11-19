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

/** A light, arcing handful of moon dust thrown by a provoked Moon Shell Crab. */
public final class MoonDustProjectileEntity extends ThrowableProjectile {

    private static final int MAX_LIFETIME = 50;
    private static final float DAMAGE = 2.0F;

    public MoonDustProjectileEntity(EntityType<? extends MoonDustProjectileEntity> type, Level level) {
        super(type, level);
    }

    public MoonDustProjectileEntity(Level level, MoonShellCrabEntity owner, double x, double y, double z) {
        this(NMAEntityTypes.MOON_DUST_PROJECTILE.get(), level);
        this.setOwner(owner);
        this.setPos(x, y, z);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.03D;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 2 == 0) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT, this.getX(), this.getY(), this.getZ(), 2,
                    0.08D, 0.08D, 0.08D, 0.01D);
            serverLevel.sendParticles(ParticleTypes.GLOW, this.getX(), this.getY(), this.getZ(), 1,
                    0.03D, 0.03D, 0.03D, 0.0D);
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
            target.hurt(this.damageSources().indirectMagic(this, this.getOwner()), DAMAGE);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.GLOW, target.getX(), target.getEyeY(), target.getZ(), 12,
                        0.35D, 0.35D, 0.35D, 0.03D);
            }
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
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
    }
}
