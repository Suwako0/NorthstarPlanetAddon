package com.suwako.northstarplanetaddon.mercury.mixin;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import com.suwako.northstarplanetaddon.mercury.world.RefreshableBiomeSource;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

/** Rebuilds the memoized biome candidate set after the Dry Seabed is added before level creation. */
@Mixin(BiomeSource.class)
public abstract class BiomeSourceMixin implements RefreshableBiomeSource {

    @Shadow @Final @Mutable
    private Supplier<Set<Holder<Biome>>> possibleBiomes;

    @Shadow
    protected abstract Stream<Holder<Biome>> collectPossibleBiomes();

    @Override
    public void mna$refreshPossibleBiomes() {
        this.possibleBiomes = Suppliers.memoize(
                () -> this.collectPossibleBiomes().distinct().collect(ImmutableSet.toImmutableSet()));
    }
}
