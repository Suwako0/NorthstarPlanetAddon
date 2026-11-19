package com.suwako.northstarplanetaddon.moon.registry;

import com.suwako.northstarplanetaddon.moon.MoonModule;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Sound events used by the Dreamsleep Moon Deer boss. */
public final class NMASounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, MoonModule.NAMESPACE);

    public static final DeferredHolder<SoundEvent, SoundEvent> LUNAR_DEER_HIT_1 = registerVariableRange("lunar_deer_hit1");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUNAR_DEER_HIT_2 = registerVariableRange("lunar_deer_hit2");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUNAR_DEER_HIT_3 = registerVariableRange("lunar_deer_hit3");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUNAR_DEER_DIE = registerVariableRange("lunar_deer_die");
    /** Streamed battle theme that starts for each player when they begin tracking the Boss. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MOON_DUEL = registerVariableRange("moonduel");
    /** Ambient Moonforest background music, scheduled client-side with a vanilla-like random delay. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MOONLIGHT = registerVariableRange("moonlight");

    private NMASounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> registerVariableRange(String id) {
        return SOUND_EVENTS.register(id, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(MoonModule.NAMESPACE, id)));
    }

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
