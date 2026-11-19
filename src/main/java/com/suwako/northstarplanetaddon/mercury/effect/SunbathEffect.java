package com.suwako.northstarplanetaddon.mercury.effect;

import com.suwako.northstarplanetaddon.mercury.MercuryModule;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 沐浴阳光 — Mercury's open daylight is a blessing instead of a hazard.
 * <p>
 * The speed bonus is a normal attribute modifier, so it stacks with everything else the player has
 * and disappears together with the effect. The two other halves of the effect live elsewhere:
 * {@link #EXHAUSTION_MULTIPLIER} is applied in {@code PlayerExhaustionMixin} at the single point
 * where every hunger cost enters {@code FoodData}, and the truce with the sky fish is checked by
 * {@code SunbathManager.canSunfishAttack}.
 */
public final class SunbathEffect extends MobEffect {

    /** A point of food exhaustion becomes this fraction of a point while sunbathing (60% less). */
    public static final float EXHAUSTION_MULTIPLIER = 0.4F;
    /** Warm sunlight gold, also used for the ambient effect particles. */
    private static final int COLOR = 0xFFD85A;
    /** +30% movement speed, applied to the total so it also scales sprinting. */
    private static final double SPEED_BONUS = 0.30D;

    public SunbathEffect() {
        super(MobEffectCategory.BENEFICIAL, COLOR);
        this.addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath(MercuryModule.NAMESPACE, "sunbath_speed"),
                SPEED_BONUS,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
