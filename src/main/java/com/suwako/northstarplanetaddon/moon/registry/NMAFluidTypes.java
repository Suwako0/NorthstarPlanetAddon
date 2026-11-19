package com.suwako.northstarplanetaddon.moon.registry;

import com.suwako.northstarplanetaddon.moon.MoonModule;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/** The fluid types added by the mod. */
public final class NMAFluidTypes {

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, MoonModule.NAMESPACE);

    /**
     * 温泉 / Hot Spring — otherwise water-like: it pushes entities, can be swum in and supports boats.
     * {@code canConvertToSource(true)} is what gives it water's infinite-source rule: two adjacent
     * source blocks above a solid floor grow a new source, as long as nothing cancels
     * {@code CreateFluidSourceEvent}.
     */
    public static final Supplier<FluidType> HOT_SPRING = FLUID_TYPES.register("hot_spring", () -> new FluidType(
            FluidType.Properties.create()
                    .descriptionId("fluid." + MoonModule.NAMESPACE + ".hot_spring")
                    .density(1000)
                    .viscosity(1000)
                    .temperature(400)
                    .canConvertToSource(true)
                    .canExtinguish(true)
                    .canHydrate(true)
                    .supportsBoating(true)
                    .lightLevel(3)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                    .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)));

    private NMAFluidTypes() {
    }

    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
    }
}
