package com.suwako.northstarplanetaddon.mercury.world;

import com.suwako.northstarplanetaddon.mercury.MercuryModule;
import com.suwako.northstarplanetaddon.mercury.entity.MeteorEntity;
import com.suwako.northstarplanetaddon.mercury.entity.SolarStormImmune;
import com.suwako.northstarplanetaddon.mercury.network.SolarStormPayload;
import com.suwako.northstarplanetaddon.mercury.registry.MNABlocks;
import com.suwako.northstarplanetaddon.mercury.registry.MNADamageTypes;
import com.suwako.northstarplanetaddon.mercury.registry.MNAEntityTypes;
import com.suwako.northstarplanetaddon.mercury.registry.MNASounds;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Mercury's solar storm — the weather this addon exists for.
 * <p>
 * The storm lives in exactly one dimension. It is simulated only in {@code northstar:mercury}, only
 * while at least one player is there, and it is persisted in the overworld's saved data so a server
 * restart never loses a running storm. Everything a client needs to render it (the ambience sound
 * and the drifting sparks) is driven by the {@link SolarStormPayload} heartbeat; the server owns the
 * gameplay half: the scorching of anything standing in the open and the meteorite bombardment.
 */
public final class SolarStormManager {

    /** northstar:mercury — the only dimension the storm exists in. */
    public static final ResourceKey<Level> MERCURY = ResourceKey.create(
            Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("northstar", "mercury"));

    private static final String DATA_NAME = MercuryModule.NAMESPACE + "_solar_storm";
    private static final SavedData.Factory<SolarStormData> DATA_FACTORY =
            new SavedData.Factory<>(SolarStormData::new, SolarStormData::load);

    /**
     * Countdown to the next meteorite volley. Deliberately transient: it is not saved, so a restart
     * simply schedules the next volley again rather than resuming a half spent timer.
     */
    private static int meteorCountdown = SolarStormSettings.METEOR_FIRST_DELAY;

    private SolarStormManager() {
    }

    /** The persisted storm state, kept in the always loaded overworld storage. */
    public static SolarStormData state(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(DATA_FACTORY, DATA_NAME);
    }

    // ------------------------------------------------------------------ 指令接口

    /**
     * Starts the storm over Mercury.
     *
     * @param durationTicks length in ticks, or {@code <= 0} for a storm that only {@code /solarstorm stop} ends
     * @return whether a new storm was started (false if one was already running)
     */
    public static boolean start(MinecraftServer server, int durationTicks) {
        SolarStormData data = state(server);
        boolean wasActive = data.isActive();
        data.start(server.overworld().getGameTime(), durationTicks);
        meteorCountdown = SolarStormSettings.METEOR_FIRST_DELAY;
        announce(server, true, durationTicks);
        sync(server, true);
        return !wasActive;
    }

    /** Ends the storm over Mercury. Returns whether one had been running. */
    public static boolean stop(MinecraftServer server) {
        SolarStormData data = state(server);
        boolean wasActive = data.stop();
        if (wasActive) {
            announce(server, false, 0);
        }
        sync(server, false);
        return wasActive;
    }

    // ------------------------------------------------------------------ 服务器刻

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        SolarStormData data = state(server);
        long gameTime = server.overworld().getGameTime();

        if (data.isActive() && gameTime >= data.endTime()) {
            stop(server);
            return;
        }

        // 心跳：迟到进服的玩家、切维度回来的玩家都靠它拿到正确状态。
        if (gameTime % SolarStormSettings.SYNC_INTERVAL == 0L) {
            sync(server, data.isActive());
        }

        ServerLevel mercury = server.getLevel(MERCURY);
        if (mercury == null || mercury.players().isEmpty()) {
            // 水星上没有玩家：不推进伤害与陨石（区块也不会加载），但风暴计时照常走。
            return;
        }

        if (!data.isActive()) {
            maybeStartNaturally(server, mercury, gameTime);
            return;
        }

        tickStorm(mercury, gameTime);
    }

    /** Rolls for a natural storm. Only called while somebody is actually standing on Mercury. */
    private static void maybeStartNaturally(MinecraftServer server, ServerLevel mercury, long gameTime) {
        if (gameTime % SolarStormSettings.NATURAL_CHECK_INTERVAL != 0L) {
            return;
        }
        if (mercury.getRandom().nextFloat() >= SolarStormSettings.NATURAL_CHANCE) {
            return;
        }

        int span = SolarStormSettings.NATURAL_MAX_DURATION - SolarStormSettings.NATURAL_MIN_DURATION;
        int duration = SolarStormSettings.NATURAL_MIN_DURATION + mercury.getRandom().nextInt(span + 1);
        SolarStormManager.start(server, duration);
    }

    private static void tickStorm(ServerLevel mercury, long gameTime) {
        if (gameTime % SolarStormSettings.SCORCH_INTERVAL == 0L) {
            scorchExposedEntities(mercury);
        }

        if (--meteorCountdown > 0) {
            return;
        }

        RandomSource random = mercury.getRandom();
        meteorCountdown = nextMeteorDelay(random);
        spawnMeteorVolley(mercury, random);
    }

    /** Randomised gap between two volleys, so the bombardment never feels metronomic. */
    private static int nextMeteorDelay(RandomSource random) {
        int span = SolarStormSettings.METEOR_MAX_INTERVAL - SolarStormSettings.METEOR_MIN_INTERVAL;
        return SolarStormSettings.METEOR_MIN_INTERVAL + random.nextInt(span + 1);
    }

    // ------------------------------------------------------------------ 灼烧

    /**
     * Burns everything that is standing under Mercury's open sky. A roof is real protection, and so is
     * being a storm creature: anything implementing {@link SolarStormImmune} (the sky fish) rides the
     * solar wind instead of being cooked by it.
     */
    private static void scorchExposedEntities(ServerLevel mercury) {
        DamageSource source = MNADamageTypes.source(mercury, MNADamageTypes.SOLAR_STORM);
        for (Entity entity : mercury.getAllEntities()) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isSpectator()) {
                continue;
            }
            if (living instanceof SolarStormImmune) {
                continue;
            }
            if (living instanceof Player player && player.isCreative()) {
                continue;
            }
            if (!mercury.canSeeSky(living.blockPosition())) {
                continue;
            }

            living.hurt(source, SolarStormSettings.SCORCH_DAMAGE);
            living.setRemainingFireTicks(SolarStormSettings.SCORCH_FIRE_SECONDS * 20);
            if (living instanceof ServerPlayer player) {
                player.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN,
                        SolarStormSettings.SCORCH_SLOW_DURATION,
                        SolarStormSettings.SCORCH_SLOW_AMPLIFIER));
            }
        }
    }

    // ------------------------------------------------------------------ 陨石

    private static void spawnMeteorVolley(ServerLevel mercury, RandomSource random) {
        List<ServerPlayer> players = mercury.players();
        if (players.isEmpty()) {
            return;
        }

        ServerPlayer target = players.get(random.nextInt(players.size()));
        int count = 1 + random.nextInt(SolarStormSettings.METEOR_MAX_PER_VOLLEY);
        for (int index = 0; index < count; index++) {
            spawnMeteor(mercury, target, random);
        }
    }

    private static void spawnMeteor(ServerLevel mercury, ServerPlayer target, RandomSource random) {
        double angle = random.nextDouble() * Mth.TWO_PI;
        double distance = SolarStormSettings.METEOR_MIN_DISTANCE
                + random.nextDouble() * (SolarStormSettings.METEOR_MAX_DISTANCE - SolarStormSettings.METEOR_MIN_DISTANCE);
        dropMeteor(mercury, target.getX() + Math.cos(angle) * distance, target.getZ() + Math.sin(angle) * distance);
    }

    /**
     * Spawns one meteorite high above the given column. Shared by the storm's bombardment and by the
     * {@code /solarstorm impact} helper, so both always use identical physics.
     *
     * @return the spawned entity, already added to the level
     */
    public static MeteorEntity dropMeteor(ServerLevel level, double x, double z) {
        RandomSource random = level.getRandom();
        double y = level.getMaxBuildHeight() - SolarStormSettings.METEOR_SPAWN_HEIGHT_BELOW_TOP;

        MeteorEntity meteor = new MeteorEntity(MNAEntityTypes.METEOR.get(), level);
        meteor.setPos(x, y, z);
        meteor.setYRot(random.nextFloat() * 360.0F);
        meteor.setDeltaMovement(
                (random.nextDouble() - 0.5D) * 0.1D,
                -SolarStormSettings.METEOR_INITIAL_SPEED,
                (random.nextDouble() - 0.5D) * 0.1D);
        level.addFreshEntity(meteor);

        // 预警：在空中就能听见的呼啸声。
        level.playSound(null, x, y, z, MNASounds.METEOR_FALLING.get(), SoundSource.WEATHER,
                4.0F, 0.7F + random.nextFloat() * 0.3F);
        return meteor;
    }

    /**
     * Resolves a meteorite impact: carves the crater, seeds it with Northstar ores and scorches
     * everything standing close enough.
     */
    public static void meteorImpact(ServerLevel level, BlockPos center) {
        RandomSource random = level.getRandom();
        carveCrater(level, center, random);
        damageAroundImpact(level, center);
        impactEffects(level, center);
    }

    /**
     * Carves a shallow dish: the horizontal radius is deliberately larger than the depth, so a strike
     * leaves a crater to explore without gutting the terrain around it.
     */
    private static void carveCrater(ServerLevel level, BlockPos center, RandomSource random) {
        int radius = SolarStormSettings.CRATER_MIN_RADIUS
                + random.nextInt(SolarStormSettings.CRATER_MAX_RADIUS - SolarStormSettings.CRATER_MIN_RADIUS + 1);
        int depth = SolarStormSettings.CRATER_MIN_DEPTH
                + random.nextInt(SolarStormSettings.CRATER_MAX_DEPTH - SolarStormSettings.CRATER_MIN_DEPTH + 1);
        List<BlockPos> floor = new ArrayList<>();
        // 记录本次放置过的格子（long 编码的 BlockPos），用于判断"真正的坑底"。
        Set<Long> placed = new HashSet<>();
        int oresPlaced = 0;

        double horizontal = (double) (radius * radius);
        double vertical = (double) (depth * depth);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -depth; dy <= 1; dy++) {
                    // 椭球：水平半径 radius、垂直深度 depth
                    double shape = (dx * dx + dz * dz) / horizontal + (dy * dy) / vertical;
                    if (shape > 1.0D) {
                        continue;
                    }

                    BlockPos pos = center.offset(dx, dy, dz);
                    if (pos.getY() <= level.getMinBuildHeight() + 1) {
                        continue;
                    }
                    BlockState existing = level.getBlockState(pos);
                    if (existing.isAir() || existing.is(Blocks.BEDROCK)) {
                        continue;
                    }

                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    if (dy <= 0) {
                        floor.add(pos);
                    }
                }
            }
        }

        // 只装饰真正的坑底：下方是原本就存在、且没有被这一发陨石挖空的格子。
        // 已经放过矿石/沙的格子会被记进 placed，避免在其上方再叠一层。
        for (BlockPos pos : floor) {
            BlockPos below = pos.below();
            if (placed.contains(below.asLong())) {
                continue;
            }
            if (level.getBlockState(below).isAir()) {
                continue;
            }

            if (oresPlaced < SolarStormSettings.ORE_MAX_PER_CRATER
                    && random.nextFloat() < SolarStormSettings.ORE_CHANCE) {
                BlockState ore = MeteorOres.pick(random);
                if (!ore.isAir()) {
                    level.setBlock(pos, ore, Block.UPDATE_ALL);
                    placed.add(pos.asLong());
                    oresPlaced++;
                    continue;
                }
            }

            float roll = random.nextFloat();
            if (roll < SolarStormSettings.MAGMA_CHANCE) {
                level.setBlock(pos, Blocks.MAGMA_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                placed.add(pos.asLong());
            } else if (roll < SolarStormSettings.MAGMA_CHANCE + SolarStormSettings.SAND_CHANCE) {
                level.setBlock(pos, MNABlocks.MERCURY_SAND.get().defaultBlockState(), Block.UPDATE_ALL);
                placed.add(pos.asLong());
            }
        }

        MercuryModule.LOGGER.debug("Meteorite crater at {}: radius {}, depth {}, {} ores seeded.",
                center, radius, depth, oresPlaced);
    }

    private static void damageAroundImpact(ServerLevel level, BlockPos center) {
        DamageSource source = MNADamageTypes.source(level, MNADamageTypes.METEOR);
        Vec3 impactCenter = Vec3.atCenterOf(center);
        AABB area = new AABB(center).inflate(SolarStormSettings.IMPACT_RADIUS);

        for (Entity entity : level.getEntities((Entity) null, area,
                candidate -> candidate.isAlive() && !(candidate instanceof Player player && player.isCreative()))) {
            Vec3 away = entity.position().subtract(impactCenter);
            double horizontal = Math.max(away.horizontalDistance(), 0.4D);
            entity.hurt(source, SolarStormSettings.IMPACT_DAMAGE);
            entity.setRemainingFireTicks(SolarStormSettings.IMPACT_FIRE_SECONDS * 20);
            entity.push(away.x / horizontal * SolarStormSettings.IMPACT_KNOCKBACK, 0.35D,
                    away.z / horizontal * SolarStormSettings.IMPACT_KNOCKBACK);
        }
    }

    private static void impactEffects(ServerLevel level, BlockPos center) {
        double x = center.getX() + 0.5D;
        double y = center.getY() + 0.5D;
        double z = center.getZ() + 0.5D;

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 8, 1.5D, 1.0D, 1.5D, 0.0D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 40, 2.0D, 1.5D, 2.0D, 0.02D);
        level.sendParticles(ParticleTypes.FLAME, x, y, z, 60, 1.6D, 1.0D, 1.6D, 0.05D);
        level.sendParticles(ParticleTypes.LAVA, x, y, z, 20, 1.6D, 0.6D, 1.6D, 0.0D);
        level.sendParticles(ParticleTypes.ASH, x, y + 1.0D, z, 60, 3.0D, 2.0D, 3.0D, 0.01D);

        level.playSound(null, x, y, z, MNASounds.METEOR_IMPACT.get(), SoundSource.WEATHER, 2.5F, 1.0F);
    }

    // ------------------------------------------------------------------ 同步与播报

    private static void sync(MinecraftServer server, boolean active) {
        SolarStormPayload payload = new SolarStormPayload(active);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    private static void announce(MinecraftServer server, boolean started, int durationTicks) {
        ServerLevel mercury = server.getLevel(MERCURY);
        Component message = started
                ? Component.translatable("message." + MercuryModule.NAMESPACE + ".solar_storm.begin")
                : Component.translatable("message." + MercuryModule.NAMESPACE + ".solar_storm.end");

        if (mercury != null) {
            for (ServerPlayer player : mercury.players()) {
                player.displayClientMessage(message, false);
                if (started) {
                    player.playNotifySound(MNASounds.SOLAR_STORM_WARNING.get(), SoundSource.WEATHER, 1.6F, 1.0F);
                }
            }
        }

        MercuryModule.LOGGER.info("Solar storm over {}: {} ({}).",
                MERCURY.location(), started ? "began" : "ended",
                started ? (durationTicks <= 0 ? "no time limit" : durationTicks + " ticks") : "cleared");
        server.sendSystemMessage(message);
    }
}
