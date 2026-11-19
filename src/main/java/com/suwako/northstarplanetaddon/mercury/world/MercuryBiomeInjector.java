package com.suwako.northstarplanetaddon.mercury.world;

import com.mojang.datafixers.util.Pair;
import com.suwako.northstarplanetaddon.mercury.MercuryModule;
import java.util.ArrayList;
import java.util.List;
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
 * Attaches the mod's 干涸海床 (Dry Seabed) biome to Mercury before any level or chunk exists.
 * <p>
 * Northstar builds Mercury's biome source from an inline {@code minecraft:multi_noise} parameter list.
 * One of its surface entries, {@code northstar:mercury_hills}, spans the entire continentalness range
 * {@code [-1, 1]}, which is what makes a non-invasive injection possible: the entry is split into three
 * continentalness sub-boxes — the original biome below and above, the Dry Seabed over
 * {@code [-0.19, -0.11]}.
 * <ul>
 * <li>{@code [-0.23, -0.09]} is centred on the continentalness band vanilla Minecraft uses for its
 * river biomes (and Northstar gives to {@code northstar:mercury_basins}), only wider, so the new
 * biome lands where a river bed belongs and is a common sight rather than a rarity.</li>
 * <li>The three sub-boxes are an exact partition of the original box: disjoint and covering it. Every
 * climate point therefore keeps the fitness it had before, and any point inside the original box still
 * resolves to exactly one sub-box. In other words the only places where generation changes are the
 * ones inside the new band — no other biome can be displaced by this injection.</li>
 * <li>The sub-boxes keep {@code mercury_hills}' temperature, humidity, erosion, depth and weirdness, so
 * the new biome inherits the same "where Mercury's highlands are" envelope.</li>
 * </ul>
 * If Northstar ever changes that entry's shape the injector logs what it expected and leaves Mercury
 * completely untouched rather than guessing.
 */
public final class MercuryBiomeInjector {

    private static final ResourceKey<LevelStem> MERCURY_STEM = ResourceKey.create(
            Registries.LEVEL_STEM, ResourceLocation.fromNamespaceAndPath("northstar", "mercury"));

    private static final ResourceKey<Biome> DRY_RIVERBED = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath(MercuryModule.NAMESPACE, "dry_riverbed"));
    private static final ResourceKey<Biome> MERCURY_HILLS = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath("northstar", "mercury_hills"));

    /**
     * The continentalness band the Dry Seabed takes.
     * <p>
     * It is centred on the band vanilla Minecraft gives its river biomes — and that Northstar gives
     * {@code northstar:mercury_basins}, {@code [-0.19, -0.11]} — but deliberately wider on both sides,
     * so dry riverbeds are a common sight on Mercury instead of a rarity.
     */
    private static final float RIVERBED_CONTINENTALNESS_MIN = -0.23F;
    private static final float RIVERBED_CONTINENTALNESS_MAX = -0.09F;

    /** Exact boundaries of Northstar 0.6.5's {@code northstar:mercury_hills} climate box. */
    private static final float HILLS_CONTINENTALNESS_MIN = -1.0F;
    private static final float HILLS_CONTINENTALNESS_MAX = 1.0F;

    private MercuryBiomeInjector() {
    }

    /** Runs before MinecraftServer creates any level or evaluates the generator's feature list. */
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        Registry<LevelStem> stems = event.getServer().registryAccess().registryOrThrow(Registries.LEVEL_STEM);
        LevelStem mercuryStem = stems.get(MERCURY_STEM);
        if (mercuryStem == null) {
            MercuryModule.LOGGER.warn("Dry Seabed injection skipped: dimension {} is unavailable.",
                    MERCURY_STEM.location());
            return;
        }

        ChunkGenerator generator = mercuryStem.generator();
        BiomeSource biomeSource = generator.getBiomeSource();
        if (!(biomeSource instanceof MultiNoiseBiomeSource)
                || !(biomeSource instanceof ExtendableBiomeSource bridge)
                || !(biomeSource instanceof RefreshableBiomeSource refreshable)) {
            MercuryModule.LOGGER.error(
                    "Dry Seabed injection failed: {} uses biome source {}, which cannot be extended safely.",
                    MERCURY_STEM.location(), biomeSource.getClass().getName());
            return;
        }

        Registry<Biome> biomes = event.getServer().registryAccess().registryOrThrow(Registries.BIOME);
        Holder<Biome> riverbed = biomes.getHolder(DRY_RIVERBED).orElse(null);
        if (riverbed == null) {
            MercuryModule.LOGGER.error(
                    "Dry Seabed injection failed: biome {} is absent from the loaded biome registry; "
                            + "the dimension was left completely unchanged on purpose.",
                    DRY_RIVERBED.location());
            return;
        }

        List<Pair<Climate.ParameterPoint, Holder<Biome>>> current = bridge.mna$getClimateEntries();
        ResourceKey<Biome> alreadyPresent = current.stream()
                .map(entry -> entry.getSecond().unwrapKey().orElse(null))
                .filter(key -> DRY_RIVERBED.equals(key))
                .findFirst()
                .orElse(null);
        if (alreadyPresent != null) {
            MercuryModule.LOGGER.info("{} is already present in {}'s biome source; no duplicate was added.",
                    alreadyPresent.location(), MERCURY_STEM.location());
            return;
        }

        int hillsIndex = findSurfaceEntry(current, MERCURY_HILLS,
                HILLS_CONTINENTALNESS_MIN, HILLS_CONTINENTALNESS_MAX);
        if (hillsIndex < 0) {
            MercuryModule.LOGGER.error(
                    "Dry Seabed injection failed: Northstar's {} continentalness box [{}, {}] was not found; "
                            + "the dimension was left unchanged to avoid displacing another biome.",
                    MERCURY_HILLS.location(), HILLS_CONTINENTALNESS_MIN, HILLS_CONTINENTALNESS_MAX);
            return;
        }

        Pair<Climate.ParameterPoint, Holder<Biome>> original = current.get(hillsIndex);
        Climate.ParameterPoint hills = original.getFirst();
        Holder<Biome> hillsBiome = original.getSecond();

        List<Pair<Climate.ParameterPoint, Holder<Biome>>> revised = new ArrayList<>(current.size() + 2);
        for (int index = 0; index < current.size(); index++) {
            if (index != hillsIndex) {
                revised.add(current.get(index));
                continue;
            }

            // The exact partition: hills, riverbed, hills — disjoint and covering the original box.
            revised.add(Pair.of(withContinentalness(hills, HILLS_CONTINENTALNESS_MIN, RIVERBED_CONTINENTALNESS_MIN),
                    hillsBiome));
            revised.add(Pair.of(withContinentalness(hills, RIVERBED_CONTINENTALNESS_MIN, RIVERBED_CONTINENTALNESS_MAX),
                    riverbed));
            revised.add(Pair.of(withContinentalness(hills, RIVERBED_CONTINENTALNESS_MAX, HILLS_CONTINENTALNESS_MAX),
                    hillsBiome));
        }

        bridge.mna$setClimateEntries(revised);
        refreshable.mna$refreshPossibleBiomes();

        if (!biomeSource.possibleBiomes().contains(riverbed)) {
            MercuryModule.LOGGER.error(
                    "Dry Seabed injection failed: {} did not retain {} in its refreshed candidate set.",
                    MERCURY_STEM.location(), DRY_RIVERBED.location());
            return;
        }

        MercuryModule.LOGGER.info(
                "Injected {} into {} by partitioning {}'s continentalness [{}, {}]: hills [{}, {}], "
                        + "dry riverbed [{}, {}], hills [{}, {}].",
                DRY_RIVERBED.location(), MERCURY_STEM.location(), MERCURY_HILLS.location(),
                HILLS_CONTINENTALNESS_MIN, HILLS_CONTINENTALNESS_MAX,
                HILLS_CONTINENTALNESS_MIN, RIVERBED_CONTINENTALNESS_MIN,
                RIVERBED_CONTINENTALNESS_MIN, RIVERBED_CONTINENTALNESS_MAX,
                RIVERBED_CONTINENTALNESS_MAX, HILLS_CONTINENTALNESS_MAX);
    }

    /** Finds the surface entry of the given biome whose continentalness box is exactly the expected one. */
    private static int findSurfaceEntry(List<Pair<Climate.ParameterPoint, Holder<Biome>>> entries,
            ResourceKey<Biome> biome, float minimum, float maximum) {
        long expectedMin = Climate.quantizeCoord(minimum);
        long expectedMax = Climate.quantizeCoord(maximum);

        for (int index = 0; index < entries.size(); index++) {
            Pair<Climate.ParameterPoint, Holder<Biome>> entry = entries.get(index);
            if (!biome.equals(entry.getSecond().unwrapKey().orElse(null))) {
                continue;
            }
            Climate.Parameter continentalness = entry.getFirst().continentalness();
            if (continentalness.min() == expectedMin && continentalness.max() == expectedMax) {
                return index;
            }
        }
        return -1;
    }

    /** Same climate point, with the continentalness interval replaced. */
    private static Climate.ParameterPoint withContinentalness(Climate.ParameterPoint source,
            float minimum, float maximum) {
        return new Climate.ParameterPoint(
                source.temperature(), source.humidity(), Climate.Parameter.span(minimum, maximum),
                source.erosion(), source.depth(), source.weirdness(), source.offset());
    }
}
