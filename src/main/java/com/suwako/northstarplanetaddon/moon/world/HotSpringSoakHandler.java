package com.suwako.northstarplanetaddon.moon.world;

import com.suwako.northstarplanetaddon.moon.registry.NMAFluidTypes;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Soaking in 温泉 grants 饱和 and 生命恢复 I as real, visible effects.
 * <p>
 * 饱和 is the vanilla Saturation effect rather than a silent refill of {@code FoodData}: it feeds
 * {@code amplifier + 1} hunger and twice that in saturation on every tick it is active, so the buff
 * icon stays on the screen and the food bar fills even from empty — which is the point of the pool.
 * 生命恢复 I is refreshed in the same way, and both lapse about a second after leaving the water.
 */
public final class HotSpringSoakHandler {

    /** Both effects are renewed on this cadence; the short durations are what make them drop off. */
    private static final int CHECK_INTERVAL = 5;
    private static final int EFFECT_DURATION = 20;
    private static final int REGENERATION_LEVEL = 0;
    private static final int SATURATION_LEVEL = 0;

    private HotSpringSoakHandler() {
    }

    /** Runs for every entity on the logical server; the fluid test itself is throttled. */
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || entity.level().isClientSide()
                || entity.tickCount % CHECK_INTERVAL != 0) {
            return;
        }
        if (!entity.isInFluidType(NMAFluidTypes.HOT_SPRING.get())) {
            return;
        }

        refresh(entity, MobEffects.REGENERATION, REGENERATION_LEVEL);
        refresh(entity, MobEffects.SATURATION, SATURATION_LEVEL);
    }

    /**
     * Renews the effect only when it is about to lapse, so standing in a pool is not a steady stream
     * of effect updates on the wire.
     */
    private static void refresh(LivingEntity entity, Holder<MobEffect> effect, int amplifier) {
        MobEffectInstance active = entity.getEffect(effect);
        if (active != null && active.getDuration() > CHECK_INTERVAL) {
            return;
        }
        entity.addEffect(new MobEffectInstance(effect, EFFECT_DURATION, amplifier, true, true, true));
    }
}
