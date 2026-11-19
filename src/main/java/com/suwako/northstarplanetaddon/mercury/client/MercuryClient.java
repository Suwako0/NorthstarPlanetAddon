package com.suwako.northstarplanetaddon.mercury.client;

import com.suwako.northstarplanetaddon.mercury.client.model.BlazingSunfishModel;
import com.suwako.northstarplanetaddon.mercury.client.model.SunfishModel;
import com.suwako.northstarplanetaddon.mercury.client.renderer.BlazingSunfishRenderer;
import com.suwako.northstarplanetaddon.mercury.client.renderer.SunfishRenderer;
import com.suwako.northstarplanetaddon.mercury.registry.MNAEntityTypes;
import com.suwako.northstarplanetaddon.mercury.registry.MNASounds;
import com.suwako.northstarplanetaddon.mercury.world.SolarStormSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3f;

/**
 * Client wiring.
 * <p>
 * Registration is driven from {@link com.suwako.northstarplanetaddon.mercury.MercuryModule} behind a
 * client dist check rather than an {@code @EventBusSubscriber} annotation, so this class is never
 * loaded on a dedicated server.
 * <p>
 * The whole presentation of the solar storm lives here and is generated locally, so its density costs
 * no network traffic: two layered ambience loops, a dense shower of solar dust and light streaks, a
 * warm fog that closes in, a subtle camera sway and the occasional high-altitude flare with its
 * thunder. The server only says whether the storm is running.
 */
public final class MercuryClient {

    private static final ResourceKey<Level> MERCURY = ResourceKey.create(
            Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("northstar", "mercury"));

    /** Golden-orange solar dust, orange-red fog and the hotter embers that drift near the ground. */
    private static final DustParticleOptions SOLAR_DUST =
            new DustParticleOptions(new Vector3f(1.0F, 0.62F, 0.18F), 2.2F);
    private static final DustParticleOptions SOLAR_FOG =
            new DustParticleOptions(new Vector3f(1.0F, 0.18F, 0.05F), 3.8F);
    private static final DustParticleOptions SOLAR_EMBER =
            new DustParticleOptions(new Vector3f(1.0F, 0.32F, 0.06F), 1.5F);

    private static final RandomSource RANDOM = RandomSource.create();
    private static final int AMBIENCE_RADIUS = 56;
    private static final int SKY_HEIGHT = 48;
    /** Volume of the three ambience layers: wind carries the storm, howl and rumble sit underneath. */
    private static final float WIND_VOLUME = 1.15F;
    private static final float HOWL_VOLUME = 1.0F;
    private static final float RUMBLE_VOLUME = 0.7F;

    private static boolean stormActive;
    private static SoundInstance stormWind;
    private static SoundInstance stormHowl;
    private static SoundInstance stormRumble;
    private static int flareCountdown = 60;
    /** 玩家被风暴直射的程度：0 = 身处掩体，1 = 完全暴露在天空下。镜头晃动按它缩放。 */
    private static float exposure;

    private MercuryClient() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(MercuryClient::onRegisterLayerDefinitions);
        modEventBus.addListener(MercuryClient::onRegisterRenderers);
        NeoForge.EVENT_BUS.addListener(MercuryClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(MercuryClient::onComputeFogColor);
        NeoForge.EVENT_BUS.addListener(MercuryClient::onRenderFog);
        NeoForge.EVENT_BUS.addListener(MercuryClient::onComputeCameraAngles);
    }

    /**
     * Called from the storm packet. Leaving {@code northstar:mercury} or the storm ending both stop
     * the ambience immediately, because the client tick below also verifies the dimension.
     */
    public static void setStormActive(boolean active) {
        stormActive = active;
        flareCountdown = 60;
        if (!active) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null) {
                stopStormAmbience(minecraft);
            }
        }
    }

    /** The meteorite is drawn by vanilla's thrown-item renderer: a big, glowing, tumbling rock. */
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MNAEntityTypes.METEOR.get(),
                context -> new ThrownItemRenderer<>(context, 2.0F, true));
        event.registerEntityRenderer(MNAEntityTypes.SUNFISH.get(), SunfishRenderer::new);
        event.registerEntityRenderer(MNAEntityTypes.BLAZING_SUNFISH.get(), BlazingSunfishRenderer::new);
    }

    /** The two sky fish skeletons, each rendered against its own 64x64 sheet. */
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SunfishModel.LAYER_LOCATION, SunfishModel::createBodyLayer);
        event.registerLayerDefinition(BlazingSunfishModel.LAYER_LOCATION, BlazingSunfishModel::createBodyLayer);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;

        // 必须放在提前 return 之前：躲进掩体后下面直接返回，不更新就会把晃动卡在旧强度上。
        updateExposure(minecraft);

        if (level == null || player == null || !isStormVisible(minecraft)) {
            stopStormAmbience(minecraft);
            return;
        }

        playStormAmbience(minecraft);
        emitStormParticles(level, player);
        maybeFlare(level, player);
    }

    // ------------------------------------------------------------------ 观感

    /** 风暴把天空染成暖橙色，并把可视距离压近——"扬尘遮蔽阳光"的体感。 */
    private static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!isStormVisible(Minecraft.getInstance())) {
            return;
        }
        event.setRed(SolarStormSettings.FOG_RED);
        event.setGreen(SolarStormSettings.FOG_GREEN);
        event.setBlue(SolarStormSettings.FOG_BLUE);
    }

    private static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!isStormVisible(Minecraft.getInstance())) {
            return;
        }
        if (event.getMode() == FogRenderer.FogMode.FOG_TERRAIN || event.getMode() == FogRenderer.FogMode.FOG_SKY) {
            event.scaleFarPlaneDistance(SolarStormSettings.FOG_FAR_PLANE_SCALE);
            event.scaleNearPlaneDistance(0.6F);
        }
    }

    /**
     * 极轻微的镜头晃动，让"站在风暴里"这件事有生理感受，又不至于让人晕。
     * 只有被太阳风暴直射时才晃：头顶有方块挡着（洞穴、屋顶、走廊……）强度为 0，画面完全静止。
     */
    private static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isStormVisible(minecraft) || minecraft.player == null || exposure <= 0.0F) {
            return;
        }

        float time = (minecraft.player.tickCount + (float) event.getPartialTick()) * 0.9F;
        float sway = SolarStormSettings.CAMERA_SWAY_DEGREES * exposure;
        event.setRoll(event.getRoll() + Mth.sin(time) * sway * 0.6F + Mth.sin(time * 1.7F) * sway * 0.4F);
        event.setYaw(event.getYaw() + Mth.cos(time * 1.3F) * sway * 0.25F);
    }

    // ------------------------------------------------------------------ 音效

    /**
     * Three looping ambience layers: the addon wind uses Northstar's dust-storm sound, while the two
     * vanilla ambient beds add a high howl and a low rumble without audible restart clicks.
     */
    private static void playStormAmbience(Minecraft minecraft) {
        stormWind = keepLooping(minecraft, stormWind, MNASounds.SOLAR_STORM_WIND.get(), WIND_VOLUME);
        stormHowl = keepLooping(minecraft, stormHowl, SoundEvents.AMBIENT_SOUL_SAND_VALLEY_LOOP.value(), HOWL_VOLUME);
        stormRumble = keepLooping(minecraft, stormRumble, SoundEvents.AMBIENT_BASALT_DELTAS_LOOP.value(), RUMBLE_VOLUME);
    }

    /** Restarts one layer only when the engine has actually dropped it. */
    private static SoundInstance keepLooping(Minecraft minecraft, SoundInstance current, SoundEvent sound, float volume) {
        if (current != null && minecraft.getSoundManager().isActive(current)) {
            return current;
        }
        SoundInstance instance = new SimpleSoundInstance(
                sound.getLocation(),
                SoundSource.AMBIENT,
                volume,
                1.0F,
                SoundInstance.createUnseededRandom(),
                true,
                0,
                SoundInstance.Attenuation.NONE,
                0.0D,
                0.0D,
                0.0D,
                true);
        minecraft.getSoundManager().play(instance);
        return instance;
    }

    private static void stopStormAmbience(Minecraft minecraft) {
        if (stormWind == null && stormHowl == null && stormRumble == null) {
            return;
        }
        if (minecraft != null) {
            if (stormWind != null) {
                minecraft.getSoundManager().stop(stormWind);
            }
            if (stormHowl != null) {
                minecraft.getSoundManager().stop(stormHowl);
            }
            if (stormRumble != null) {
                minecraft.getSoundManager().stop(stormRumble);
            }
        }
        stormWind = null;
        stormHowl = null;
        stormRumble = null;
    }

    // ------------------------------------------------------------------ 粒子

    /**
     * The storm's particle body, emitted every client tick: golden dust streaming sideways with the
     * solar wind, white light streaks and electric sparks raining out of the sky, and embers plus dust
     * puffs close to the player so the storm reads as something you are standing inside of.
     */
    private static void emitStormParticles(ClientLevel level, LocalPlayer player) {
        // 近处的大颗粒雾尘与远景雾色叠加，形成橙红色、可穿行的太阳风暴雾幕。
        for (int index = 0; index < SolarStormSettings.FOG_PARTICLES_PER_TICK; index++) {
            level.addParticle(SOLAR_FOG,
                    player.getX() + (RANDOM.nextDouble() - 0.5D) * 28.0D,
                    player.getY() - 1.0D + RANDOM.nextDouble() * 9.0D,
                    player.getZ() + (RANDOM.nextDouble() - 0.5D) * 28.0D,
                    0.11D + RANDOM.nextDouble() * 0.08D,
                    -0.01D,
                    0.0D);
        }

        for (int index = 0; index < 8; index++) {
            level.addParticle(SOLAR_DUST,
                    player.getX() + horizontalSpread(),
                    player.getY() - 4.0D + RANDOM.nextDouble() * 28.0D,
                    player.getZ() + horizontalSpread(),
                    0.14D + RANDOM.nextDouble() * 0.1D,
                    -0.02D - RANDOM.nextDouble() * 0.05D,
                    0.0D);
        }

        for (int index = 0; index < 3; index++) {
            double x = player.getX() + horizontalSpread();
            double z = player.getZ() + horizontalSpread();
            double y = player.getY() + 14.0D + RANDOM.nextDouble() * SKY_HEIGHT;
            level.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0D, -0.5D, 0.0D);
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0.0D, -0.42D, 0.0D);
            level.addParticle(ParticleTypes.SMALL_FLAME, x, y - 6.0D, z, 0.0D, -0.25D, 0.0D);
        }

        for (int index = 0; index < 4; index++) {
            level.addParticle(SOLAR_EMBER,
                    player.getX() + (RANDOM.nextDouble() - 0.5D) * 20.0D,
                    player.getY() + RANDOM.nextDouble() * 4.0D,
                    player.getZ() + (RANDOM.nextDouble() - 0.5D) * 20.0D,
                    0.05D, 0.02D, 0.05D);
        }

        if (player.tickCount % 6 == 0) {
            double x = player.getX() + horizontalSpread();
            double z = player.getZ() + horizontalSpread();
            level.addParticle(ParticleTypes.SMALL_GUST, x, player.getY() + 1.5D, z, 0.25D, 0.0D, 0.25D);
            level.addParticle(ParticleTypes.ASH, x, player.getY() + 5.0D, z, 0.05D, -0.02D, 0.05D);
        }
    }

    /** A distant flare: a bright flash high above, a spray of sparks, and rolling thunder. */
    private static void maybeFlare(ClientLevel level, LocalPlayer player) {
        if (--flareCountdown > 0) {
            return;
        }

        int span = SolarStormSettings.FLARE_MAX_INTERVAL - SolarStormSettings.FLARE_MIN_INTERVAL;
        flareCountdown = SolarStormSettings.FLARE_MIN_INTERVAL + RANDOM.nextInt(span + 1);

        double x = player.getX() + horizontalSpread();
        double z = player.getZ() + horizontalSpread();
        double y = player.getY() + 26.0D + RANDOM.nextDouble() * 20.0D;

        level.addParticle(ParticleTypes.FLASH, x, y, z, 0.0D, 0.0D, 0.0D);
        for (int index = 0; index < 14; index++) {
            level.addParticle(ParticleTypes.END_ROD,
                    x + (RANDOM.nextDouble() - 0.5D) * 8.0D,
                    y - RANDOM.nextDouble() * 6.0D,
                    z + (RANDOM.nextDouble() - 0.5D) * 8.0D,
                    0.0D, -0.4D, 0.0D);
            level.addParticle(ParticleTypes.GLOW,
                    x + (RANDOM.nextDouble() - 0.5D) * 10.0D,
                    y - RANDOM.nextDouble() * 8.0D,
                    z + (RANDOM.nextDouble() - 0.5D) * 10.0D,
                    0.0D, -0.1D, 0.0D);
        }

        level.playLocalSound(x, y, z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER,
                0.55F, 0.9F + RANDOM.nextFloat() * 0.25F, false);
    }

    /** Random horizontal offset inside the ambience radius, centred on the player. */
    private static double horizontalSpread() {
        return (RANDOM.nextDouble() - 0.5D) * AMBIENCE_RADIUS;
    }

    private static boolean isStormVisible(Minecraft minecraft) {
        return stormActive
                && minecraft.level != null
                && minecraft.player != null
                && minecraft.level.dimension().equals(MERCURY);
    }

    // ------------------------------------------------------------------ 遮蔽判定

    /**
     * 把"玩家此刻被风暴直射的程度"平滑地推向目标值 0 或 1。
     * <p>
     * 判定本身用 {@link Level#canSeeSky}，和服务端 {@code SolarStormManager} 决定"谁会被灼烧"用的是同一
     * 套标准：因此不会晃的玩家，也正好是不会掉血的玩家——屋顶、洞穴、走廊都是真的避难所。
     * 变化是渐进的，站在屋檐边缘反复进出时画面不会抽搐。
     */
    private static void updateExposure(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            exposure = 0.0F;
            return;
        }
        if (player.tickCount % SolarStormSettings.EXPOSURE_CHECK_INTERVAL != 0) {
            return;
        }

        float target = isStormVisible(minecraft) && minecraft.level.canSeeSky(player.blockPosition()) ? 1.0F : 0.0F;
        exposure += Mth.clamp(target - exposure,
                -SolarStormSettings.EXPOSURE_FADE_PER_TICK, SolarStormSettings.EXPOSURE_FADE_PER_TICK);
    }
}
