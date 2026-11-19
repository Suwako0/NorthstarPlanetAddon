package com.suwako.northstarplanetaddon.mercury.mixin;

import com.suwako.northstarplanetaddon.mercury.effect.SunbathEffect;
import com.suwako.northstarplanetaddon.mercury.registry.MNAMobEffects;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Every hunger cost — sprinting, jumping, attacking, taking damage — funnels through
 * {@code Player#causeFoodExhaustion(float)} before it reaches {@code FoodData}. Scaling the argument
 * there is the only way to slow the drain without fighting {@code FoodData#tick}, which consumes
 * exhaustion in 4-point chunks. Refunding exhaustion after the tick cannot see those chunks.
 */
@Mixin(Player.class)
public abstract class PlayerExhaustionMixin {

    @ModifyVariable(method = "causeFoodExhaustion(F)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float northstar_planet_addon$sunbathExhaustion(float exhaustion) {
        Player self = (Player) (Object) this;
        return self.hasEffect(MNAMobEffects.SUNBATH)
                ? exhaustion * SunbathEffect.EXHAUSTION_MULTIPLIER
                : exhaustion;
    }
}
