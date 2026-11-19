package com.suwako.northstarplanetaddon.moon.mixin;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.suwako.northstarplanetaddon.moon.world.MoonForestBiomeSource;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.Holder.Reference;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

/** Gives the dimension-keyed injector controlled access to a decoded multi-noise parameter list. */
@Mixin(MultiNoiseBiomeSource.class)
public abstract class MultiNoiseBiomeSourceMixin implements MoonForestBiomeSource {

    @Shadow @Final @Mutable
    private Either<Climate.ParameterList<Holder<Biome>>, Holder<MultiNoiseBiomeSourceParameterList>> parameters;

    @Override
    public List<Pair<Climate.ParameterPoint, Holder<Biome>>> nma$getClimateEntries() {
        return this.parameters.map(Climate.ParameterList::values, preset -> preset.value().parameters().values());
    }

    @Override
    public void nma$setClimateEntries(List<Pair<Climate.ParameterPoint, Holder<Biome>>> entries) {
        this.parameters = Either.left(new Climate.ParameterList<>(List.copyOf(entries)));
    }
}
