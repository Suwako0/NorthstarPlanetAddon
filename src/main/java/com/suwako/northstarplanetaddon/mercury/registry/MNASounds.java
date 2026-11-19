package com.suwako.northstarplanetaddon.mercury.registry;

import com.suwako.northstarplanetaddon.mercury.MercuryModule;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Sound events used by the solar storm.
 * <p>
 * The looping storm wind and one-shot cues point at existing files inside Northstar's own resource
 * pack, so the addon ships no binary assets at all.
 */
public final class MNASounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, MercuryModule.NAMESPACE);

    /** The single call that opens a storm. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SOLAR_STORM_WARNING =
            registerVariableRange("solar_storm_warning");
    /** The continuous wind layer played locally for the storm's full duration. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SOLAR_STORM_WIND =
            registerVariableRange("solar_storm_wind");
    /** The burning whistle of a meteorite on its way down. */
    public static final DeferredHolder<SoundEvent, SoundEvent> METEOR_FALLING = registerVariableRange("meteor_falling");
    /** The blast of a meteorite hitting the ground. */
    public static final DeferredHolder<SoundEvent, SoundEvent> METEOR_IMPACT = registerVariableRange("meteor_impact");

    private MNASounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> registerVariableRange(String id) {
        return SOUND_EVENTS.register(id, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(MercuryModule.NAMESPACE, id)));
    }

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
