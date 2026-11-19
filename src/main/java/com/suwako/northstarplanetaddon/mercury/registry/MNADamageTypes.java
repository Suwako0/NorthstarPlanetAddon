package com.suwako.northstarplanetaddon.mercury.registry;

import com.suwako.northstarplanetaddon.mercury.MercuryModule;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * The mod's damage types. Like every 1.21 damage type these are datapack entries
 * ({@code data/northstar_planet_addon/damage_type/*.json}), so only the keys live in code.
 */
public final class MNADamageTypes {

    /** Scorching by the storm's radiation: the damage dealt to anything standing under open sky. */
    public static final ResourceKey<DamageType> SOLAR_STORM = key("solar_storm");
    /** The impact of a meteorite. */
    public static final ResourceKey<DamageType> METEOR = key("meteor");
    /** Spinning dive charge of either sky fish. */
    public static final ResourceKey<DamageType> SUNFISH_RAM = key("sunfish_ram");
    /** Fire scattered by 太阳鱼's wing beats. */
    public static final ResourceKey<DamageType> SUNFISH_FLAME = key("sunfish_flame");
    /** High-energy ray fired by 炽烈太阳鱼. */
    public static final ResourceKey<DamageType> SUNFISH_BEAM = key("sunfish_beam");

    private MNADamageTypes() {
    }

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE,
                ResourceLocation.fromNamespaceAndPath(MercuryModule.NAMESPACE, name));
    }

    /** Resolves a damage type of this mod against the loaded datapack registry. */
    public static DamageSource source(Level level, ResourceKey<DamageType> type) {
        return new DamageSource(holder(level, type));
    }

    /** Same, but attributed to the attacking fish so death messages and difficulty scaling work. */
    public static DamageSource source(Level level, ResourceKey<DamageType> type, Entity attacker) {
        return new DamageSource(holder(level, type), attacker);
    }

    private static Holder<DamageType> holder(Level level, ResourceKey<DamageType> type) {
        return level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type);
    }
}
