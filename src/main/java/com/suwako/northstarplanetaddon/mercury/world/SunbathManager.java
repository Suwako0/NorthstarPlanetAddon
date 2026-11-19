package com.suwako.northstarplanetaddon.mercury.world;

import com.suwako.northstarplanetaddon.mercury.entity.SunfishEntity;
import com.suwako.northstarplanetaddon.mercury.registry.MNAMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 沐浴阳光 — the reward for standing on Mercury's surface, in the open, while the sun is up.
 * <p>
 * The effect is maintained by a server-side player tick: a short instance is refreshed while the
 * conditions hold and removed the moment they stop, so walking under a roof, digging down, leaving
 * the dimension or nightfall all end it immediately. Nothing is persisted — a relog simply rechecks
 * on the next tick — and no client-side state is needed because {@link MobEffectInstance} already
 * syncs itself to the owning player.
 * <p>
 * 太阳鱼果烩 grants the very same effect for a fixed two minutes, sun or no sun. Those instances are
 * marked with {@code ambient = false} (see {@link #foodEffect()}) while the sun's own are ambient,
 * so the tick below only refreshes or removes the ambient ones and a jam buff survives caves, night
 * and other dimensions until its own timer expires — no extra player state or NBT required.
 * <p>
 * This class also owns the "truce": the sky fish ask {@link #canSunfishAttack} before acquiring a
 * target, before keeping one and before every hit, so a sunbathing player is never actively attacked.
 */
public final class SunbathManager {

    /** Duration of the 太阳鱼果烩 buff, in ticks: 2 minutes. */
    public static final int FOOD_DURATION = 20 * 120;
    /** Recheck cadence (ticks). 0.5 s is quick enough for a roof or a portal, cheap enough for a server. */
    private static final int CHECK_INTERVAL = 10;
    /** Granted duration (ticks); refreshed before it can lapse. */
    private static final int EFFECT_DURATION = 100;
    /** Refresh once the instance has burned down to this many ticks. */
    private static final int REFRESH_BELOW = 30;
    /** How far above the local surface still counts as "on the surface". */
    private static final double MAX_HEIGHT_ABOVE_SURFACE = 6.0D;
    /** A shallow dip in the ground is still the surface; a cave is not. */
    private static final double MAX_DEPTH_BELOW_SURFACE = 2.0D;
    private static final long DAY_LENGTH = 24000L;
    /** 0 = sunrise, 6000 = noon, 12000 = sunset. */
    private static final long DAY_END = 12000L;

    private SunbathManager() {
    }

    /** Returns whether the player is currently protected by 沐浴阳光. */
    public static boolean isSunbathing(Player player) {
        return player.hasEffect(MNAMobEffects.SUNBATH);
    }

    /**
     * A fresh, marked instance for 太阳鱼果烩. {@code ambient = false} is the marker: sunlight's own
     * effect is ambient, so {@link #onPlayerTick} can tell the two apart without any extra state.
     * Particles stay off, matching the subtle look of the natural effect.
     * <p>
     * The longer duration also lets {@code LivingEntity#addEffect} cleanly upgrade an existing
     * ambient instance in place when the player eats the jam while already sunbathing.
     */
    public static MobEffectInstance foodEffect() {
        return new MobEffectInstance(MNAMobEffects.SUNBATH, FOOD_DURATION, 0, false, false, true);
    }

    private static boolean isFoodGranted(MobEffectInstance instance) {
        return instance != null && !instance.isAmbient();
    }

    /**
     * The single gate used by the sky fish. Sunbathing, creative, invulnerable, dead and spectator
     * players are not valid targets, and the fish never hurt each other with their area attacks.
     */
    public static boolean canSunfishAttack(LivingEntity entity) {
        if (entity == null || !entity.isAlive() || entity.isSpectator() || entity.isInvulnerable()) {
            return false;
        }
        if (entity instanceof SunfishEntity) {
            return false;
        }
        if (entity instanceof Player player) {
            return !player.isCreative() && !isSunbathing(player);
        }
        return true;
    }

    /** Rechecks the conditions for one player; fired on both sides, so the client half is skipped. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.tickCount % CHECK_INTERVAL != 0) {
            return;
        }

        MobEffectInstance current = player.getEffect(MNAMobEffects.SUNBATH);
        // 果烩赋予的效果由它自己的 2 分钟计时管理，日光的刷新/清除逻辑一概不碰它。
        boolean fromFood = isFoodGranted(current);
        if (meetsConditions(player)) {
            if (!fromFood && (current == null || current.getDuration() <= REFRESH_BELOW)) {
                // ambient = subtle HUD icon, no particle fountain, icon visible.
                player.addEffect(new MobEffectInstance(
                        MNAMobEffects.SUNBATH, EFFECT_DURATION, 0, true, false, true));
            }
        } else if (current != null && !fromFood) {
            player.removeEffect(MNAMobEffects.SUNBATH);
        }
    }

    /**
     * 水星维度表面 + 头顶无遮挡 + 白天.
     * <p>
     * {@code canSeeSky} already rules out caves (the heightmap above the player's feet is higher)
     * and roofs. The height comparison then rules out flying high above the terrain with an elytra:
     * the effect is about standing in the sunlight on the ground, not about being anywhere in the sky.
     */
    public static boolean meetsConditions(Player player) {
        if (player.isSpectator()) {
            return false;
        }
        Level level = player.level();
        if (!level.dimension().equals(SolarStormManager.MERCURY) || !isDaytime(level)) {
            return false;
        }

        BlockPos pos = player.blockPosition();
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        if (player.getY() < surface - MAX_DEPTH_BELOW_SURFACE
                || player.getY() > surface + MAX_HEIGHT_ABOVE_SURFACE) {
            return false;
        }
        return level.canSeeSky(pos) && level.canSeeSky(pos.above());
    }

    /**
     * Vanilla {@link Level#isDay()} reports false for dimensions with a fixed time, so read the
     * clock ourselves: use the dimension's fixed time when there is one, otherwise the live day time.
     */
    private static boolean isDaytime(Level level) {
        long time = level.dimensionType().fixedTime().orElse(level.getDayTime());
        return Math.floorMod(time, DAY_LENGTH) < DAY_END;
    }
}
