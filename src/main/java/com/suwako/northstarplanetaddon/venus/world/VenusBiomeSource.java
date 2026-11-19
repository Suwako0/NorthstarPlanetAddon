package com.suwako.northstarplanetaddon.venus.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.stream.Stream;

/**
 * Custom biome source for the cloud-sea dimension.
 *
 * <p>Why a Java BiomeSource instead of {@code minecraft:multi_noise}: the datapack file for
 * {@code multi_noise_biome_source_parameter_list} only accepts a preset reference
 * ({@code {"preset": "..."}}), not inline parameter lists, so custom biomes cannot be
 * described there. This source maps the climate temperature band to the biome list,
 * giving every biome a coherent latitude-like band.</p>
 */
public class VenusBiomeSource extends BiomeSource {

    public static final MapCodec<VenusBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes")
                            .forGetter(VenusBiomeSource::biomes))
            .apply(instance, VenusBiomeSource::new));

    private final HolderSet<Biome> biomes;

    public VenusBiomeSource(HolderSet<Biome> biomes) {
        this.biomes = biomes;
    }

    public HolderSet<Biome> biomes() {
        return this.biomes;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return this.biomes.stream();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        if (this.biomes.size() == 0) {
            throw new IllegalStateException("northstar_planet_addon:venus biome source has no biomes");
        }
        Climate.TargetPoint point = sampler.sample(x, y, z);
        float temperature = Climate.unquantizeCoord(point.temperature());
        int index = Mth.clamp((int) ((temperature + 1.0F) * 0.5F * this.biomes.size()),
                0, this.biomes.size() - 1);
        return this.biomes.get(index);
    }
}