package com.suwako.northstarplanetaddon.moon.client;

import com.suwako.northstarplanetaddon.moon.MoonModule;
import com.suwako.northstarplanetaddon.moon.client.animation.NMAAnimationResources;
import com.suwako.northstarplanetaddon.moon.client.model.MoonDeerModel;
import com.suwako.northstarplanetaddon.moon.client.model.MoonRabbitModel;
import com.suwako.northstarplanetaddon.moon.client.model.MoonShellCrabModel;
import com.suwako.northstarplanetaddon.moon.client.renderer.MoonDeerRenderer;
import com.suwako.northstarplanetaddon.moon.client.renderer.MoonDustProjectileRenderer;
import com.suwako.northstarplanetaddon.moon.client.renderer.MoonlightBoltRenderer;
import com.suwako.northstarplanetaddon.moon.client.renderer.MoonRabbitRenderer;
import com.suwako.northstarplanetaddon.moon.client.renderer.MoonShellCrabRenderer;
import com.suwako.northstarplanetaddon.moon.entity.MoonDeerEntity;
import com.suwako.northstarplanetaddon.moon.registry.NMAEntityTypes;
import com.suwako.northstarplanetaddon.moon.registry.NMAFluids;
import com.suwako.northstarplanetaddon.moon.registry.NMAFluidTypes;
import com.suwako.northstarplanetaddon.moon.registry.NMAItems;
import com.suwako.northstarplanetaddon.moon.registry.NMASounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client wiring.
 * <p>
 * Registration is driven from {@link com.suwako.northstarplanetaddon.moon.MoonModule} behind a
 * client dist check rather than an {@code @EventBusSubscriber} annotation, so this class is never
 * loaded on a dedicated server where the model and renderer classes do not exist.
 */
public final class NMAClient {

    private static final ResourceKey<Biome> MOON_FOREST = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath("northstar_planet_addon", "moon_forest"));
    private static final ResourceKey<Level> MOON_DIMENSION = ResourceKey.create(
            Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("northstar", "moon"));
    /** Random quiet gap before a Moonforest track may begin: 10 seconds to 3 real-time minutes. */
    private static final int MOONLIGHT_MIN_DELAY = 200;
    private static final int MOONLIGHT_MAX_DELAY = 3_600;
    private static final RandomSource MUSIC_RANDOM = RandomSource.create();
    private static int moonlightDelay = nextMoonlightDelay();
    private static SoundInstance moonlightMusic;

    private NMAClient() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(NMAClient::onRegisterLayerDefinitions);
        modEventBus.addListener(NMAClient::onRegisterRenderers);
        modEventBus.addListener(NMAClient::onRegisterReloadListeners);
        modEventBus.addListener(NMAClient::onRegisterClientExtensions);
        modEventBus.addListener(NMAClient::onClientSetup);
        NeoForge.EVENT_BUS.addListener(NMAClient::onClientTick);
    }

    /** 温泉 renders through vanilla's translucent layer, exactly like water does. */
    public static void onClientSetup(FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(NMAFluids.HOT_SPRING.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(NMAFluids.HOT_SPRING_FLOWING.get(), RenderType.translucent());
    }

    /** Points the 温泉 fluid type at its own textures instead of making it look like water. */
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            private static final ResourceLocation STILL = ResourceLocation.fromNamespaceAndPath(
                    MoonModule.NAMESPACE, "block/hot_spring_still");
            private static final ResourceLocation FLOWING = ResourceLocation.fromNamespaceAndPath(
                    MoonModule.NAMESPACE, "block/hot_spring_flow");
            private static final ResourceLocation UNDER_FLUID = ResourceLocation.withDefaultNamespace(
                    "textures/misc/underwater.png");

            @Override
            public ResourceLocation getStillTexture() {
                return STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return FLOWING;
            }

            @Override
            public ResourceLocation getRenderOverlayTexture(Minecraft mc) {
                return UNDER_FLUID;
            }

            @Override
            public int getTintColor() {
                return 0xFFFFFFFF;
            }
        }, NMAFluidTypes.HOT_SPRING.get());
    }

    /**
     * Moonforest music follows vanilla's delayed ambient-music pattern. The timer only runs while
     * the player stands in Moonforest; once the chosen song starts it is allowed to finish after the
     * player walks into another Moon biome. Leaving {@code northstar:moon} stops it immediately.
     */
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        emitMoonDeerAxeGlow(minecraft);
        if (minecraft.player == null || minecraft.level == null || !minecraft.level.dimension().equals(MOON_DIMENSION)) {
            stopMoonlightMusic(minecraft);
            moonlightDelay = nextMoonlightDelay();
            return;
        }

        if (isMoonDeerBattleNearby(minecraft)) {
            // The Boss theme is also in the MUSIC category. Stop the biome track before the encounter
            // track starts so the two streamed songs never overlap.
            stopMoonlightMusic(minecraft);
            moonlightDelay = nextMoonlightDelay();
            return;
        }

        if (moonlightMusic != null) {
            // A vanilla situational track may have been scheduled after ours began. Cancel only that
            // MusicManager track, leaving the Moonforest instance running as the sole BGM.
            if (minecraft.getMusicManager().isPlayingMusic(minecraft.getSituationalMusic())) {
                minecraft.getMusicManager().stopPlaying();
            }
            if (!minecraft.getSoundManager().isActive(moonlightMusic)) {
                moonlightMusic = null;
                moonlightDelay = nextMoonlightDelay();
            }
            return;
        }

        if (!minecraft.level.getBiome(minecraft.player.blockPosition()).is(MOON_FOREST)) {
            return;
        }
        if (--moonlightDelay > 0) {
            return;
        }

        // Stop the current vanilla/situational BGM before starting ours. The ongoing active check
        // above also prevents a later MusicManager selection from overlapping this track.
        minecraft.getMusicManager().stopPlaying();
        moonlightMusic = SimpleSoundInstance.forMusic(NMASounds.MOONLIGHT.get());
        minecraft.getSoundManager().play(moonlightMusic);
    }

    /**
     * Emits a faint, local-only moonlight mote beside the hand holding the axe. Keeping this in the
     * client tick path means inventory, dropped-item, item-frame and GUI rendering never receive the
     * effect, while the player can still see the glow in either first- or third-person view.
     */
    private static void emitMoonDeerAxeGlow(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null || minecraft.player.isSpectator()
                || minecraft.player.tickCount % 10 != 0) {
            return;
        }

        boolean mainHand = minecraft.player.getMainHandItem().is(NMAItems.MOON_DEER_AXE.get());
        boolean offHand = minecraft.player.getOffhandItem().is(NMAItems.MOON_DEER_AXE.get());
        if (!mainHand && !offHand) {
            return;
        }

        // Approximate the active hand from the player's current view, with a little forward offset
        // so the subtle mote remains visible in first person as well as beside the arm in third person.
        Vec3 look = minecraft.player.getViewVector(1.0F);
        double handSide = mainHand ? 1.0D : -1.0D;
        Vec3 position = minecraft.player.position()
                .add(-look.z * handSide * 0.32D, 1.05D, look.x * handSide * 0.32D)
                .add(look.x * 0.28D, 0.0D, look.z * 0.28D);
        RandomSource random = minecraft.level.random;
        double drift = 0.003D + random.nextDouble() * 0.003D;
        minecraft.level.addParticle(ParticleTypes.GLOW,
                position.x + (random.nextDouble() - 0.5D) * 0.12D,
                position.y + random.nextDouble() * 0.16D,
                position.z + (random.nextDouble() - 0.5D) * 0.12D,
                0.0D, drift, 0.0D);

        // A single brighter glint every two seconds provides a restrained "enchanted" pulse.
        if (minecraft.player.tickCount % 40 == 0) {
            minecraft.level.addParticle(ParticleTypes.END_ROD,
                    position.x, position.y + 0.08D, position.z,
                    0.0D, 0.006D, 0.0D);
        }
    }

    private static boolean isMoonDeerBattleNearby(Minecraft minecraft) {
        return !minecraft.level.getEntitiesOfClass(MoonDeerEntity.class,
                        minecraft.player.getBoundingBox().inflate(64.0D), MoonDeerEntity::isAlive)
                .isEmpty();
    }

    private static void stopMoonlightMusic(Minecraft minecraft) {
        if (moonlightMusic != null) {
            minecraft.getSoundManager().stop(moonlightMusic);
            moonlightMusic = null;
        }
    }

    private static int nextMoonlightDelay() {
        return MOONLIGHT_MIN_DELAY + MUSIC_RANDOM.nextInt(MOONLIGHT_MAX_DELAY - MOONLIGHT_MIN_DELAY + 1);
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new NMAAnimationResources());
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MoonRabbitModel.LAYER_LOCATION, MoonRabbitModel::createBodyLayer);
        event.registerLayerDefinition(MoonShellCrabModel.LAYER_LOCATION, MoonShellCrabModel::createBodyLayer);
        event.registerLayerDefinition(MoonDeerModel.LAYER_LOCATION, MoonDeerModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(NMAEntityTypes.MOON_RABBIT.get(), MoonRabbitRenderer::new);
        event.registerEntityRenderer(NMAEntityTypes.MOON_SHELL_CRAB.get(), MoonShellCrabRenderer::new);
        event.registerEntityRenderer(NMAEntityTypes.MOON_DEER.get(), MoonDeerRenderer::new);
        event.registerEntityRenderer(NMAEntityTypes.MOON_DUST_PROJECTILE.get(), MoonDustProjectileRenderer::new);
        event.registerEntityRenderer(NMAEntityTypes.MOONLIGHT_BOLT.get(), MoonlightBoltRenderer::new);
    }
}
