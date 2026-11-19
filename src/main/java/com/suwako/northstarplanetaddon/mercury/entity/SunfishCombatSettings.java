package com.suwako.northstarplanetaddon.mercury.entity;

/**
 * Every tunable number of the sky fish attacks, gathered in one place.
 * <p>
 * Both species use one attack goal that picks a random mode each time its cooldown expires; the
 * species only decides whether the second mode is {@code WING_FLAP} or {@code RAY}.
 */
public final class SunfishCombatSettings {

    // ---------------------------------------------------------------- 攻击节奏

    /** Cooldown between two attacks (ticks): 3 ~ 5.5 s after the previous attack ends. */
    public static final int ATTACK_MIN_COOLDOWN = 60;
    public static final int ATTACK_MAX_COOLDOWN = 110;
    /** Grace period after spawning / loading, so a fresh shoal does not dive instantly. */
    public static final int INITIAL_MIN_COOLDOWN = 40;
    public static final int INITIAL_MAX_COOLDOWN = 80;
    /** The fish must be this close (3D) to commit to an attack. */
    public static final double ATTACK_RANGE = 44.0D;
    /** Give up an attack if the target gets further away than this. */
    public static final double LOSE_TARGET_RANGE = 56.0D;

    // ---------------------------------------------------------------- 旋转俯冲（两种鱼共有）

    public static final int DIVE_WINDUP = 12;
    public static final int DIVE_MAX_TICKS = 60;
    public static final double DIVE_SPEED = 0.85D;
    public static final double DIVE_HIT_RADIUS = 1.9D;
    public static final double DIVE_KNOCKBACK = 0.75D;
    /** Degrees of visible body spin per tick while diving. */
    public static final float DIVE_SPIN_DEGREES = 34.0F;

    // ---------------------------------------------------------------- 拍翅散射（太阳鱼）

    public static final int FLAP_APPROACH = 26;
    public static final int FLAP_WINDUP = 10;
    public static final int FLAP_BURSTS = 3;
    public static final int FLAP_BURST_INTERVAL = 9;
    public static final double FLAP_APPROACH_SPEED = 0.55D;
    /** How close the fish tries to hover beside its target. */
    public static final double FLAP_HOVER_DISTANCE = 3.2D;
    public static final double FLAP_HOVER_HEIGHT = 2.0D;
    /** Radius of each fire burst. */
    public static final double FLAP_RADIUS = 4.5D;
    public static final float FLAP_DAMAGE_SCALE = 0.6F;
    public static final int FLAP_FIRE_SECONDS = 3;

    // ---------------------------------------------------------------- 高能射线（炽烈太阳鱼）

    public static final int RAY_WINDUP = 16;
    public static final int RAY_SHOTS = 4;
    public static final int RAY_INTERVAL = 10;
    public static final double RAY_LENGTH = 32.0D;
    public static final float RAY_DAMAGE_SCALE = 0.8F;
    public static final int RAY_FIRE_SECONDS = 2;
    /** Particle packets per beam; the ray is drawn as short segments rather than per-block packets. */
    public static final int RAY_BEAM_SEGMENTS = 16;

    private SunfishCombatSettings() {
    }
}
