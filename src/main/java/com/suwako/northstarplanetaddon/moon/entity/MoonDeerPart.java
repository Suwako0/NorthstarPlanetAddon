package com.suwako.northstarplanetaddon.moon.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

/**
 * One hittable piece of the 梦眠月鹿. The deer is a three-block-long quadruped, so a single
 * axis-aligned box can never hug it: the neck, the head and the two antler racks sit outside any
 * box small enough to still fit the torso. Each part is a hit proxy that the deer keeps glued to
 * the animated model every tick, and damage taken by a part is passed on to the herd's real body.
 * <p>
 * Offsets are given in blocks in the model's own frame: {@code forward} grows toward the direction
 * the deer faces, {@code lateral} toward the model's right, {@code height} up from its feet.
 */
public class MoonDeerPart extends PartEntity<MoonDeerEntity> {

    private final String name;
    private final double lateralOffset;
    private final double forwardOffset;
    private final double heightOffset;
    /**
     * Assigned after the super constructor, which may already ask for the dimensions — hence the
     * nullable field and the fallback in {@link #getDimensions(Pose)}.
     */
    @Nullable
    private EntityDimensions size;

    public MoonDeerPart(MoonDeerEntity parent, String name, float width, float height,
                        double lateralOffset, double forwardOffset, double heightOffset) {
        super(parent);
        this.name = name;
        this.size = EntityDimensions.scalable(width, height);
        this.refreshDimensions();
        this.lateralOffset = lateralOffset;
        this.forwardOffset = forwardOffset;
        this.heightOffset = heightOffset;
    }

    public String getPartName() {
        return this.name;
    }

    public double getLateralOffset() {
        return this.lateralOffset;
    }

    public double getForwardOffset() {
        return this.forwardOffset;
    }

    public double getHeightOffset() {
        return this.heightOffset;
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

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return this.size != null ? this.size : super.getDimensions(pose);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    /** Same slack as the deer itself: the parts ride a swinging neck, not a statue. */
    @Override
    public float getPickRadius() {
        return 0.5F;
    }

    /** Anything that hits a part hurts the deer itself, so shields, armour and phases all apply. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !this.isInvulnerableTo(source) && this.getParent().hurt(source, amount);
    }

    @Override
    public boolean is(Entity other) {
        return this == other || this.getParent() == other;
    }

    /** Parts are never part of the world on their own; the deer spawns and saves them. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public Component getName() {
        return Component.literal(this.name);
    }
}
