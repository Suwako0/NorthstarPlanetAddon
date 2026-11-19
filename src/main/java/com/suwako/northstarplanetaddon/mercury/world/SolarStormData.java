package com.suwako.northstarplanetaddon.mercury.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Persisted state of Mercury's solar storm.
 * <p>
 * It lives in the overworld's data storage (the one dimension that is always loaded), so the storm
 * survives a server restart and is the single source of truth for both the tick handler and the
 * command.
 */
public class SolarStormData extends SavedData {

    private static final String ACTIVE = "Active";
    private static final String END_TIME = "EndTime";

    /** {@link Long#MAX_VALUE} marks a storm that was started without an end. */
    private long endTime = Long.MAX_VALUE;

    private boolean active;

    public static SolarStormData load(CompoundTag tag, HolderLookup.Provider registries) {
        SolarStormData data = new SolarStormData();
        data.active = tag.getBoolean(ACTIVE);
        data.endTime = tag.contains(END_TIME) ? tag.getLong(END_TIME) : Long.MAX_VALUE;
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean(ACTIVE, this.active);
        tag.putLong(END_TIME, this.endTime);
        return tag;
    }

    public boolean isActive() {
        return this.active;
    }

    public long endTime() {
        return this.endTime;
    }

    /** Ticks left, or {@code -1} for a storm without an end. */
    public long remaining(long gameTime) {
        if (!this.active || this.endTime == Long.MAX_VALUE) {
            return -1L;
        }
        return Math.max(0L, this.endTime - gameTime);
    }

    /**
     * Starts (or restarts) the storm.
     *
     * @param gameTime     current overworld game time
     * @param durationTicks length in ticks, or anything {@code <= 0} for a storm without an end
     */
    public void start(long gameTime, int durationTicks) {
        this.active = true;
        this.endTime = durationTicks <= 0 ? Long.MAX_VALUE : gameTime + durationTicks;
        this.setDirty();
    }

    /** Stops the storm. Returns whether it had been running. */
    public boolean stop() {
        boolean wasActive = this.active;
        this.active = false;
        this.endTime = Long.MAX_VALUE;
        this.setDirty();
        return wasActive;
    }
}
