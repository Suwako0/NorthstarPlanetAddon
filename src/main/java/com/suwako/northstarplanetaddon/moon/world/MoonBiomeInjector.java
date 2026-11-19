package com.suwako.northstarplanetaddon.moon.world;

import com.mojang.datafixers.util.Pair;
import com.suwako.northstarplanetaddon.moon.MoonModule;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

/**
 * Adds the mod's own Moon biomes to Northstar's already-decoded Moon biome source before levels or
 * chunks exist.
 * <p>
 * This intentionally targets the {@code northstar:moon} level stem, rather than trying to recognize a
 * biome source by its contents. Northstar's single lunar-plains surface climate box is partitioned
 * into non-overlapping weirdness intervals: the intervals listed in {@link #BANDS} become the mod's
 * biomes, and every remaining slice stays lunar plains. No cave parameter entry is touched.
 * <p>
 * Each of the mod's biomes is registered exactly like Moonforest is — a data driven biome plus its
 * own band carved out of lunar plains — so there is only ever one injection point to reason about,
 * and every biome stays independent of the others.
 */
public final class MoonBiomeInjector {

    private static final ResourceKey<LevelStem> MOON_STEM = ResourceKey.create(
            Registries.LEVEL_STEM, ResourceLocation.fromNamespaceAndPath("northstar", "moon"));
    private static final ResourceLocation LUNAR_PLAINS = ResourceLocation.fromNamespaceAndPath("northstar", "lunar_plains");

    private static final ResourceKey<Biome> MOON_FOREST = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(MoonModule.NAMESPACE, "moon_forest"));
    private static final ResourceKey<Biome> OPEN_HOT_SPRING = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(MoonModule.NAMESPACE, "open_hot_spring"));

    /** Exact boundaries of Northstar 0.6.5's lone lunar-plains surface climate box. */
    private static final float PLAINS_WEIRDNESS_MIN = -0.9333F;
    private static final float PLAINS_WEIRDNESS_MAX = -0.7666F;

    /** The interval Moonforest has always taken. */
    private static final float FOREST_WEIRDNESS_MIN = -0.85F;
    private static final float FOREST_WEIRDNESS_MAX = -0.80F;

    /**
     * The weirdness intervals carved out of that box, ordered from the lowest upward. The gaps
     * between them, and the slices outside the outermost ones, stay lunar plains.
     * <p>
     * 露天温泉 owns its own interval instead of a share of Moonforest's, so the two biomes are
     * independent: each is scattered over the Moon by its own noise band and neither one needs the
     * other to exist. The hot spring interval is deliberately the wider of the two (0.0667 against
     * 0.05) so hot springs turn up a little more often than Moonforest patches; widen or narrow
     * {@link #HOT_SPRING_WEIRDNESS_MAX} to tune that, but never let the two intervals overlap.
     * <p>
     * Keeping every interval non-overlapping is not optional: {@code minecraft:multi_noise} selects
     * the closest parameter point, so a band nested inside a surviving entry would tie at distance 0
     * and lose the tie to the earlier entry — the biome would simply never generate.
     */
    private static final float HOT_SPRING_WEIRDNESS_MIN = PLAINS_WEIRDNESS_MIN;
    private static final float HOT_SPRING_WEIRDNESS_MAX = -0.8666F;

    private static final List<Band> BANDS = List.of(
            new Band(OPEN_HOT_SPRING, HOT_SPRING_WEIRDNESS_MIN, HOT_SPRING_WEIRDNESS_MAX),
            new Band(MOON_FOREST, FOREST_WEIRDNESS_MIN, FOREST_WEIRDNESS_MAX));

    private MoonBiomeInjector() {
    }

    /** Runs before MinecraftServer creates any level or evaluates the generator's feature list. */
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        Registry<LevelStem> stems = event.getServer().registryAccess().registryOrThrow(Registries.LEVEL_STEM);
        LevelStem moonStem = stems.get(MOON_STEM);
        if (moonStem == null) {
            MoonModule.LOGGER.warn("Moon biome injection skipped: dimension {} is unavailable.", MOON_STEM.location());
            return;
        }

        ChunkGenerator generator = moonStem.generator();
        BiomeSource biomeSource = generator.getBiomeSource();
        if (!(biomeSource instanceof MultiNoiseBiomeSource) || !(biomeSource instanceof MoonForestBiomeSource bridge)
                || !(biomeSource instanceof RefreshableBiomeSource refreshable)) {
            MoonModule.LOGGER.error(
                    "Moon biome injection failed: {} uses biome source {}, which cannot be extended safely.",
                    MOON_STEM.location(), biomeSource.getClass().getName());
            return;
        }

        Registry<Biome> biomes = event.getServer().registryAccess().registryOrThrow(Registries.BIOME);
        Map<ResourceKey<Biome>, Holder<Biome>> injected = new LinkedHashMap<>();
        for (Band band : BANDS) {
            Holder<Biome> holder = biomes.getHolder(band.biome()).orElse(null);
            if (holder == null) {
                MoonModule.LOGGER.error(
                        "Moon biome injection failed: biome {} is absent from the loaded biome registry; "
                                + "the Moon was left completely unchanged on purpose.",
                        band.biome().location());
                return;
            }
            injected.put(band.biome(), holder);
        }

        List<Pair<Climate.ParameterPoint, Holder<Biome>>> current = bridge.nma$getClimateEntries();
        ResourceKey<Biome> alreadyPresent = current.stream()
                .map(entry -> entry.getSecond().unwrapKey().orElse(null))
                .filter(key -> key != null && injected.containsKey(key))
                .findFirst()
                .orElse(null);
        if (alreadyPresent != null) {
            MoonModule.LOGGER.info("{} is already present in {}'s biome source; no duplicate was added.",
                    alreadyPresent.location(), MOON_STEM.location());
            return;
        }

        int plainsIndex = findOriginalPlainsEntry(current);
        if (plainsIndex < 0) {
            MoonModule.LOGGER.error(
                    "Moon biome injection failed: Northstar's expected lunar-plains surface climate box was not found; "
                            + "the Moon was left unchanged to avoid replacing another biome.");
            return;
        }

        Pair<Climate.ParameterPoint, Holder<Biome>> original = current.get(plainsIndex);
        Climate.ParameterPoint plains = original.getFirst();
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> revised = new ArrayList<>(current.size() + 2 * BANDS.size());
        for (int index = 0; index < current.size(); index++) {
            if (index != plainsIndex) {
                revised.add(current.get(index));
                continue;
            }

            // Walk the former lunar-plains band from its low edge up, emitting one entry per slice.
            float cursor = PLAINS_WEIRDNESS_MIN;
            for (Band band : BANDS) {
                if (cursor < band.min()) {
                    revised.add(Pair.of(withWeirdness(plains, cursor, band.min()), original.getSecond()));
                }
                revised.add(Pair.of(withWeirdness(plains, band.min(), band.max()), injected.get(band.biome())));
                cursor = band.max();
            }
            if (cursor < PLAINS_WEIRDNESS_MAX) {
                revised.add(Pair.of(withWeirdness(plains, cursor, PLAINS_WEIRDNESS_MAX), original.getSecond()));
            }
        }

        bridge.nma$setClimateEntries(revised);
        refreshable.nma$refreshPossibleBiomes();
        for (Map.Entry<ResourceKey<Biome>, Holder<Biome>> entry : injected.entrySet()) {
            if (!biomeSource.possibleBiomes().contains(entry.getValue())) {
                MoonModule.LOGGER.error(
                        "Moon biome injection failed: {} did not retain {} in its refreshed candidate set.",
                        MOON_STEM.location(), entry.getKey().location());
                return;
            }
        }

        MoonModule.LOGGER.info(
                "Injected {} entr{} into {} by partitioning northstar:lunar_plains weirdness [{}, {}]: {}.",
                BANDS.size(), BANDS.size() == 1 ? "y" : "ies", MOON_STEM.location(),
                PLAINS_WEIRDNESS_MIN, PLAINS_WEIRDNESS_MAX, describeBands());
    }

    /** Human readable form of the partition, e.g. {@code hot spring [-0.9, -0.85], forest [-0.85, -0.8]}. */
    private static String describeBands() {
        StringBuilder builder = new StringBuilder();
        float cursor = PLAINS_WEIRDNESS_MIN;
        for (Band band : BANDS) {
            if (cursor < band.min()) {
                builder.append("plains [").append(cursor).append(", ").append(band.min()).append("], ");
            }
            builder.append(band.biome().location()).append(" [").append(band.min()).append(", ").append(band.max())
                    .append("], ");
            cursor = band.max();
        }
        if (cursor < PLAINS_WEIRDNESS_MAX) {
            builder.append("plains [").append(cursor).append(", ").append(PLAINS_WEIRDNESS_MAX).append("]");
        } else {
            builder.setLength(builder.length() - 2);
        }
        return builder.toString();
    }

    private static int findOriginalPlainsEntry(List<Pair<Climate.ParameterPoint, Holder<Biome>>> entries) {
        for (int index = 0; index < entries.size(); index++) {
            Pair<Climate.ParameterPoint, Holder<Biome>> entry = entries.get(index);
            if (!LUNAR_PLAINS.equals(entry.getSecond().unwrapKey().map(ResourceKey::location).orElse(null))) {
                continue;
            }
            Climate.Parameter weirdness = entry.getFirst().weirdness();
            if (weirdness.min() == Climate.quantizeCoord(PLAINS_WEIRDNESS_MIN)
                    && weirdness.max() == Climate.quantizeCoord(PLAINS_WEIRDNESS_MAX)) {
                return index;
            }
        }
        return -1;
    }

    private static Climate.ParameterPoint withWeirdness(Climate.ParameterPoint source, float minimum, float maximum) {
        return new Climate.ParameterPoint(
                source.temperature(), source.humidity(), source.continentalness(), source.erosion(), source.depth(),
                Climate.Parameter.span(minimum, maximum), source.offset());
    }

    private record Band(ResourceKey<Biome> biome, float min, float max) {
    }
}
