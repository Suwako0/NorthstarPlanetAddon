package com.suwako.northstarplanetaddon.mercury.entity;

import com.suwako.northstarplanetaddon.mercury.world.SolarStormManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * 陨石 — a meteorite falling through Mercury's sky during a solar storm.
 * <p>
 * The entity is deliberately plain: it falls, it burns, and it resolves itself on contact. The client
 * draws it with vanilla's {@code ThrownItemRenderer} (a glowing, slowly turning magma block), so the
 * addon needs no custom renderer or model. All gameplay — the crater, the ores, the impact damage —
 * happens in {@link SolarStormManager#meteorImpact}.
 */
public final class MeteorEntity extends Entity implements ItemSupplier {

    /** Safety net: a meteorite that somehow never lands removes itself after half a minute. */
    private static final int MAX_LIFETIME_TICKS = 600;
    /** Vertical drag per tick, the same idea as a falling block's. */
    private static final double DRAG = 0.98D;

    public MeteorEntity(EntityType<? extends MeteorEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    /** Terminal velocity is gravity / (1 - DRAG) = 2.5 blocks per tick (scaled down by Northstar's
     *  per-planet gravity while it is in Mercury's sky). */
    @Override
    protected double getDefaultGravity() {
        return 0.05D;
    }

    /**
     * A meteorite must always finish its fall.
     * <p>
     * Minecraft decides whether an entity is ticked from the visibility of the 16-block section it
     * currently occupies, and an entity that crosses a section boundary into a section created while
     * its chunk was not registered as entity-ticking silently stops being ticked — the meteorite would
     * hang in mid-air until its lifetime ran out. Player entities opt out of that bookkeeping through
     * this exact hook, and so does a falling meteorite: it is short lived, it is the only entity that
     * resolves its own impact, and its chunk still has to be in entity-ticking range for
     * {@code ServerLevel} to tick it at all.
     */
    @Override
    public boolean isAlwaysTicking() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.tickCount > MAX_LIFETIME_TICKS) {
            this.discard();
            return;
        }

        this.applyGravity();
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(DRAG));

        if (this.level() instanceof ServerLevel server) {
            emitTrail(server);
            if (this.onGround() || this.horizontalCollision) {
                SolarStormManager.meteorImpact(server, this.blockPosition());
                this.discard();
            }
        }
    }

    /** A burning train of fire, lava and smoke, so the impact can be seen coming from far away. */
    private void emitTrail(ServerLevel server) {
        double x = this.getX();
        double y = this.getY() + 0.5D;
        double z = this.getZ();

        server.sendParticles(ParticleTypes.FLAME, x, y, z, 4, 0.3D, 0.3D, 0.3D, 0.02D);
        server.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 2, 0.25D, 0.25D, 0.25D, 0.01D);
        server.sendParticles(ParticleTypes.LAVA, x, y, z, 1, 0.2D, 0.2D, 0.2D, 0.0D);
        server.sendParticles(ParticleTypes.END_ROD, x, y + 0.4D, z, 1, 0.1D, 0.1D, 0.1D, 0.0D);
    }

    /** What the client draws: a glowing rock, scaled up by the renderer. */
    @Override
    public ItemStack getItem() {
        return new ItemStack(Blocks.MAGMA_BLOCK);
    }

    /** Renders the burning overlay without ever setting the entity itself on fire. */
    @Override
    public boolean displayFireAnimation() {
        return true;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
