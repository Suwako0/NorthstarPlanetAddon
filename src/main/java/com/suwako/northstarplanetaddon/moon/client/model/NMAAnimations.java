package com.suwako.northstarplanetaddon.moon.client.model;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

/**
 * Keyframe animation data for the mod's mobs.
 * <p>
 * These are the same {@link AnimationDefinition} structures vanilla uses, consumed by
 * {@code HierarchicalModel#animate} / {@code #animateWalk} inside the models. Bone names must match
 * the {@code PartDefinition} names declared in {@link MoonRabbitModel} and {@link MoonShellCrabModel}.
 * <p>
 * Model space note: {@code posVec}'s Y axis points down, so a hop is a negative Y offset and a
 * downward bob is a positive one.
 */
public final class NMAAnimations {

    // region Moon Rabbit

    /** Idle: breathing, ear twitches and a slow look around. */
    public static final AnimationDefinition MOON_RABBIT_IDLE = AnimationDefinition.Builder.withLength(3.5F)
            .looping()
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.9F, KeyframeAnimations.degreeVec(-3.0F, 14.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.8F, KeyframeAnimations.degreeVec(2.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.7F, KeyframeAnimations.degreeVec(-3.0F, -14.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(3.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("left_ear", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.6F, KeyframeAnimations.degreeVec(-16.0F, 0.0F, -7.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.2F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.4F, KeyframeAnimations.degreeVec(9.0F, 0.0F, 5.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(3.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("right_ear", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F, KeyframeAnimations.degreeVec(-14.0F, 0.0F, 8.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.9F, KeyframeAnimations.degreeVec(8.0F, 0.0F, -6.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(3.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.SCALE,
                    new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0D, 1.0D, 1.0D), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.75F, KeyframeAnimations.scaleVec(1.02D, 0.97D, 1.02D), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(3.5F, KeyframeAnimations.scaleVec(1.0D, 1.0D, 1.0D), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("tail", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.8F, KeyframeAnimations.degreeVec(0.0F, 10.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.6F, KeyframeAnimations.degreeVec(0.0F, -10.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.4F, KeyframeAnimations.degreeVec(0.0F, 6.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(3.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
            .build();

    /** Hop cycle, driven by limb swing so the rhythm follows the rabbit's actual movement. */
    public static final AnimationDefinition MOON_RABBIT_HOP = AnimationDefinition.Builder.withLength(0.6F)
            .looping()
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.2F, KeyframeAnimations.posVec(0.0F, -1.4F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.45F, KeyframeAnimations.posVec(0.0F, 0.6F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.6F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.2F, KeyframeAnimations.degreeVec(9.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.45F, KeyframeAnimations.degreeVec(-6.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.2F, KeyframeAnimations.degreeVec(-7.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.45F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("left_front_leg", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.3F, KeyframeAnimations.degreeVec(34.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("right_front_leg", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(34.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.6F, KeyframeAnimations.degreeVec(34.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("left_haunch", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.3F, KeyframeAnimations.degreeVec(14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.6F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("right_haunch", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.3F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.6F, KeyframeAnimations.degreeVec(14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
            .build();

    // endregion

    // region Moon Shell Crab

    /** Idle: the shell rocks gently, the eye stalks rise and fall and the claws test the air. */
    public static final AnimationDefinition MOON_SHELL_CRAB_IDLE = AnimationDefinition.Builder.withLength(4.0F)
            .looping()
            .addAnimation("shell", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 2.5F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.9F, KeyframeAnimations.posVec(0.0F, -0.8F, -0.4F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.8F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(3.2F, KeyframeAnimations.posVec(0.0F, 0.4F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(4.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("left_claw", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.2F, KeyframeAnimations.degreeVec(0.0F, -22.0F, 16.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.2F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("right_claw", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(2.4F, KeyframeAnimations.degreeVec(0.0F, 22.0F, -16.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(3.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
            .build();

    /** A heavy double-claw wind-up and snap; server damage occurs on the snap frame. */
    public static final AnimationDefinition MOON_SHELL_CRAB_PINCH = AnimationDefinition.Builder.withLength(0.70F)
            .addAnimation("shell", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    pos(0.0F, 0.0F, 0.0F, 0.0F),
                    pos(0.20F, 0.0F, 0.7F, 0.0F),
                    pos(0.35F, 0.0F, -0.35F, 0.0F),
                    pos(0.70F, 0.0F, 0.0F, 0.0F)))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    deg(0.0F, 0.0F, 0.0F, 0.0F),
                    deg(0.20F, 18.0F, 0.0F, 0.0F),
                    deg(0.35F, -14.0F, 0.0F, 0.0F),
                    deg(0.70F, 0.0F, 0.0F, 0.0F)))
            .addAnimation("left_claw", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    deg(0.0F, 0.0F, 0.0F, 0.0F),
                    deg(0.20F, 25.0F, 0.0F, -38.0F),
                    deg(0.35F, -52.0F, 0.0F, 8.0F),
                    deg(0.70F, 0.0F, 0.0F, 0.0F)))
            .addAnimation("right_claw", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    deg(0.0F, 0.0F, 0.0F, 0.0F),
                    deg(0.20F, 25.0F, 0.0F, 38.0F),
                    deg(0.35F, -52.0F, 0.0F, -8.0F),
                    deg(0.70F, 0.0F, 0.0F, 0.0F)))
            .build();

    /** Right-claw scoop and release used when the crab throws an arcing moon-dust projectile. */
    public static final AnimationDefinition MOON_SHELL_CRAB_THROW = AnimationDefinition.Builder.withLength(0.90F)
            .addAnimation("shell", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    deg(0.0F, 0.0F, 0.0F, 0.0F),
                    deg(0.25F, 10.0F, 0.0F, 0.0F),
                    deg(0.60F, -8.0F, 0.0F, 0.0F),
                    deg(0.90F, 0.0F, 0.0F, 0.0F)))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    deg(0.0F, 0.0F, 0.0F, 0.0F),
                    deg(0.25F, 12.0F, 0.0F, 0.0F),
                    deg(0.60F, -16.0F, 0.0F, 0.0F),
                    deg(0.90F, 0.0F, 0.0F, 0.0F)))
            .addAnimation("right_claw", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    deg(0.0F, 0.0F, 0.0F, 0.0F),
                    deg(0.25F, 24.0F, 0.0F, 58.0F),
                    deg(0.35F, 32.0F, 0.0F, 68.0F),
                    deg(0.50F, -48.0F, 0.0F, -12.0F),
                    deg(0.90F, 0.0F, 0.0F, 0.0F)))
            .addAnimation("left_claw", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    deg(0.0F, 0.0F, 0.0F, 0.0F),
                    deg(0.25F, 12.0F, 0.0F, -20.0F),
                    deg(0.50F, -18.0F, 0.0F, 5.0F),
                    deg(0.90F, 0.0F, 0.0F, 0.0F)))
            .build();

    /** Sideways scuttle, driven by limb swing. */
    public static final AnimationDefinition MOON_SHELL_CRAB_WALK = AnimationDefinition.Builder.withLength(1.0F)
            .looping()
            .addAnimation("shell", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.posVec(0.0F, -0.7F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.75F, KeyframeAnimations.posVec(0.0F, -0.7F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("shell_top", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, 3.5F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, -3.5F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("left_leg_front", legStepping(false))
            .addAnimation("right_leg_front", legStepping(true))
            .addAnimation("left_leg_mid", legStepping(true))
            .addAnimation("right_leg_mid", legStepping(false))
            .addAnimation("left_leg_back", legStepping(false))
            .addAnimation("right_leg_back", legStepping(true))
            .addAnimation("left_claw", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, -8.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("right_claw", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 8.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
            .build();

    /**
     * One leg's step cycle over the 1 second walk loop. Crabs scuttle sideways, so the legs roll
     * about Z rather than pitching about X. {@code opposite} flips the phase so the six legs
     * alternate in the classic tripod gait.
     */
    private static AnimationChannel legStepping(boolean opposite) {
        float sign = opposite ? -1.0F : 1.0F;
        return new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 24.0F * sign), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -24.0F * sign), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR));
    }

    // endregion

    // region Mid-Autumn moon gazing

    /**
     * 月兔拜月 — the rabbit rears up on its haunches, lifts both front paws and turns its face to the
     * moon while its ears sweep back. Played occasionally, on top of the idle clip.
     */
    public static final AnimationDefinition MOON_RABBIT_GAZE = AnimationDefinition.Builder.withLength(3.5F)
            .looping()
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    pos(0.0F, 0.0F, 0.0F, 0.0F),
                    pos(0.8F, 0.0F, -1.8F, 0.0F),
                    pos(2.6F, 0.0F, -1.8F, 0.0F),
                    pos(3.4F, 0.0F, 0.0F, 0.0F)))
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    deg(0.0F, 0.0F, 0.0F, 0.0F),
                    deg(0.8F, -16.0F, 0.0F, 0.0F),
                    deg(2.6F, -16.0F, 0.0F, 0.0F),
                    deg(3.4F, 0.0F, 0.0F, 0.0F)))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    deg(0.0F, 0.0F, 0.0F, 0.0F),
                    deg(0.8F, -36.0F, 0.0F, 0.0F),
                    deg(1.8F, -42.0F, 7.0F, 0.0F),
                    deg(2.6F, -34.0F, -7.0F, 0.0F),
                    deg(3.4F, 0.0F, 0.0F, 0.0F)))
            .addAnimation("left_ear", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    deg(0.0F, 0.0F, 0.0F, 0.0F),
                    deg(0.8F, 26.0F, 0.0F, -6.0F),
                    deg(1.6F, 20.0F, 0.0F, 2.0F),
                    deg(2.6F, 27.0F, 0.0F, -5.0F),
                    deg(3.4F, 0.0F, 0.0F, 0.0F)))
            .addAnimation("right_ear", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    deg(0.0F, 0.0F, 0.0F, 0.0F),
                    deg(0.8F, 24.0F, 0.0F, 7.0F),
                    deg(1.9F, 19.0F, 0.0F, -2.0F),
                    deg(2.6F, 26.0F, 0.0F, 6.0F),
                    deg(3.4F, 0.0F, 0.0F, 0.0F)))
            .addAnimation("left_front_leg", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    deg(0.0F, 0.0F, 0.0F, 0.0F),
                    deg(0.8F, -80.0F, 0.0F, -10.0F),
                    deg(2.6F, -76.0F, 0.0F, -10.0F),
                    deg(3.4F, 0.0F, 0.0F, 0.0F)))
            .addAnimation("right_front_leg", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    deg(0.0F, 0.0F, 0.0F, 0.0F),
                    deg(0.8F, -80.0F, 0.0F, 10.0F),
                    deg(2.6F, -76.0F, 0.0F, 10.0F),
                    deg(3.4F, 0.0F, 0.0F, 0.0F)))
            .addAnimation("left_haunch", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    deg(0.0F, 0.0F, 0.0F, 0.0F),
                    deg(0.8F, 14.0F, 0.0F, 0.0F),
                    deg(2.6F, 14.0F, 0.0F, 0.0F),
                    deg(3.4F, 0.0F, 0.0F, 0.0F)))
            .addAnimation("right_haunch", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    deg(0.0F, 0.0F, 0.0F, 0.0F),
                    deg(0.8F, 14.0F, 0.0F, 0.0F),
                    deg(2.6F, 14.0F, 0.0F, 0.0F),
                    deg(3.4F, 0.0F, 0.0F, 0.0F)))
            .addAnimation("tail", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    deg(0.0F, 0.0F, 0.0F, 0.0F),
                    deg(0.8F, -12.0F, 0.0F, 0.0F),
                    deg(2.6F, -12.0F, 0.0F, 0.0F),
                    deg(3.4F, 0.0F, 0.0F, 0.0F)))
            .build();

    /**
     * 月壳蟹举螯望月 — the crab braces, lifts its mooncake shell and raises both claws towards the
     * moon. Played occasionally, on top of the idle clip.
     */
    public static final AnimationDefinition MOON_SHELL_CRAB_GAZE = AnimationDefinition.Builder.withLength(3.5F)
            .looping()
            .addAnimation("shell", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    pos(0.0F, 0.0F, 0.0F, 0.0F),
                    pos(0.9F, 0.0F, -1.0F, 0.0F),
                    pos(2.6F, 0.0F, -1.0F, 0.0F),
                    pos(3.4F, 0.0F, 0.0F, 0.0F)))
            .addAnimation("shell", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    deg(0.0F, 0.0F, 0.0F, 0.0F),
                    deg(0.9F, -14.0F, 0.0F, 0.0F),
                    deg(2.6F, -14.0F, 0.0F, 0.0F),
                    deg(3.4F, 0.0F, 0.0F, 0.0F)))
            .addAnimation("shell_top", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    deg(0.0F, 0.0F, 0.0F, 0.0F),
                    deg(0.9F, -8.0F, 0.0F, 0.0F),
                    deg(2.6F, -8.0F, 0.0F, 0.0F),
                    deg(3.4F, 0.0F, 0.0F, 0.0F)))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    deg(0.0F, 0.0F, 0.0F, 0.0F),
                    deg(0.9F, -26.0F, 0.0F, 0.0F),
                    deg(1.8F, -30.0F, 8.0F, 0.0F),
                    deg(2.6F, -24.0F, -8.0F, 0.0F),
                    deg(3.4F, 0.0F, 0.0F, 0.0F)))
            .addAnimation("left_claw", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    deg(0.0F, 0.0F, 0.0F, 0.0F),
                    deg(0.9F, -18.0F, 0.0F, 60.0F),
                    deg(1.8F, -22.0F, 0.0F, 66.0F),
                    deg(2.6F, -16.0F, 0.0F, 58.0F),
                    deg(3.4F, 0.0F, 0.0F, 0.0F)))
            .addAnimation("right_claw", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    deg(0.0F, 0.0F, 0.0F, 0.0F),
                    deg(0.9F, 18.0F, 0.0F, -60.0F),
                    deg(1.8F, 22.0F, 0.0F, -66.0F),
                    deg(2.6F, 16.0F, 0.0F, -58.0F),
                    deg(3.4F, 0.0F, 0.0F, 0.0F)))
            .build();

    // endregion

    private static Keyframe pos(float time, float x, float y, float z) {
        return new Keyframe(time, KeyframeAnimations.posVec(x, y, z), AnimationChannel.Interpolations.CATMULLROM);
    }

    private static Keyframe deg(float time, float x, float y, float z) {
        return new Keyframe(time, KeyframeAnimations.degreeVec(x, y, z), AnimationChannel.Interpolations.CATMULLROM);
    }

    private NMAAnimations() {
    }
}
