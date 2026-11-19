package com.suwako.northstarplanetaddon.venus.registry;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Sulfuric acid fluid (design chapter 8 / 12.4).
 *
 * <p>Self-contained: no third-party fluid dependency. The acid damages entities that
 * are not acid-proof through the {@code #northstar_planet_addon:acid} damage tag chain, and the
 * acid pump (see {@code AcidPumpBlock}) extracts it into buckets.</p>
 */
public final class ModFluids {

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, VenusModule.NAMESPACE);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, VenusModule.NAMESPACE);

    public static final DeferredHolder<FluidType, FluidType> SULFURIC_ACID_TYPE = FLUID_TYPES.register("sulfuric_acid",
            () -> new FluidType(FluidType.Properties.create()
                    .density(1400)
                    .viscosity(1800)
                    .temperature(330)
                    .canConvertToSource(false)
                    .canDrown(true)
                    .supportsBoating(false)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SULFURIC_ACID =
            FLUIDS.register("sulfuric_acid", () -> new BaseFlowingFluid.Source(acidProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_SULFURIC_ACID =
            FLUIDS.register("flowing_sulfuric_acid", () -> new BaseFlowingFluid.Flowing(acidProperties()));

    public static final DeferredBlock<LiquidBlock> SULFURIC_ACID_BLOCK = ModBlocks.BLOCKS.register("sulfuric_acid",
            () -> new LiquidBlock(SULFURIC_ACID.get(), Block.Properties.of()
                    .noCollission()
                    .strength(100.0F)
                    .noLootTable()
                    .liquid()));

    public static final DeferredItem<BucketItem> SULFURIC_ACID_BUCKET = ModItems.ITEMS.register("sulfuric_acid_bucket",
            () -> new BucketItem(SULFURIC_ACID.get(), new Item.Properties()
                    .craftRemainder(Items.BUCKET)
                    .stacksTo(1)));

    private static BaseFlowingFluid.Properties acidProperties() {
        return new BaseFlowingFluid.Properties(SULFURIC_ACID_TYPE, SULFURIC_ACID, FLOWING_SULFURIC_ACID)
                .block(SULFURIC_ACID_BLOCK)
                .bucket(SULFURIC_ACID_BUCKET)
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2)
                .tickRate(15);
    }

    private ModFluids() {
    }
}