package com.suwako.northstarplanetaddon.mercury.registry;

import com.suwako.northstarplanetaddon.mercury.MercuryModule;
import com.suwako.northstarplanetaddon.mercury.effect.SunbathEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Mob effects added by the mod. */
public final class MNAMobEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, MercuryModule.NAMESPACE);

    /**
     * 沐浴阳光 — granted to players standing in Mercury's open daylight. Faster movement, slower
     * hunger drain, and the sky fish simply ignore whoever is wearing it.
     */
    public static final DeferredHolder<MobEffect, SunbathEffect> SUNBATH =
            MOB_EFFECTS.register("sunbath", SunbathEffect::new);

    private MNAMobEffects() {
    }

    public static void register(IEventBus modEventBus) {
        MOB_EFFECTS.register(modEventBus);
    }
}
