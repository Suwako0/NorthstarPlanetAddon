package com.suwako.northstarplanetaddon.moon.registry;

import com.suwako.northstarplanetaddon.moon.MoonModule;
import com.suwako.northstarplanetaddon.moon.fluid.HotSpringFluid;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The fluids added by the mod. */
public final class NMAFluids {

    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, MoonModule.NAMESPACE);

    /** The still source of 温泉, the one that gets picked up by a bucket and placed back down. */
    public static final DeferredHolder<Fluid, HotSpringFluid.Source> HOT_SPRING = FLUIDS.register("hot_spring",
            () -> new HotSpringFluid.Source(NMAFluids.hotSpringProperties()));

    /** Flowing 温泉 — spreads like water and never gets picked up by a bucket. */
    public static final DeferredHolder<Fluid, HotSpringFluid.Flowing> HOT_SPRING_FLOWING = FLUIDS.register(
            "hot_spring_flowing", () -> new HotSpringFluid.Flowing(NMAFluids.hotSpringProperties()));

    private NMAFluids() {
    }

    /** Water's own numbers, so 温泉 flows, spreads and reacts exactly like vanilla water. */
    private static BaseFlowingFluid.Properties hotSpringProperties() {
        return new BaseFlowingFluid.Properties(NMAFluidTypes.HOT_SPRING, NMAFluids.HOT_SPRING, NMAFluids.HOT_SPRING_FLOWING)
                .bucket(NMAItems.HOT_SPRING_BUCKET)
                .block(NMABlocks.HOT_SPRING)
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1)
                .tickRate(5)
                .explosionResistance(100.0F);
    }

    public static void register(IEventBus modEventBus) {
        FLUIDS.register(modEventBus);
    }
}
