package com.suwako.northstarplanetaddon.moon.registry;

import com.suwako.northstarplanetaddon.moon.MoonModule;
import com.suwako.northstarplanetaddon.moon.block.DreamsleepAltarBlock;
import com.suwako.northstarplanetaddon.moon.block.DreamsleepGrassBlock;
import com.suwako.northstarplanetaddon.moon.block.MoonBloomBlock;
import com.suwako.northstarplanetaddon.moon.block.MoonlightSaplingBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Every block added by the mod. */
public final class NMABlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MoonModule.NAMESPACE);

    /** Brightness of the glowing Moonlight wood family. */
    private static final int LOG_LIGHT = 4;
    private static final int LEAVES_LIGHT = 12;

    /** 梦眠之草 — the soil that covers the floor of the Moonforest. */
    public static final DeferredBlock<DreamsleepGrassBlock> DREAMSLEEP_GRASS = BLOCKS.register("dreamsleep_grass",
            () -> new DreamsleepGrassBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GRASS)
                    .strength(0.7F)
                    .sound(SoundType.GRASS)));

    /** 月壤 — the block Dreamsleep Grass grows on top of. */
    public static final DeferredBlock<Block> MOON_SOIL = BLOCKS.registerSimpleBlock("moon_soil",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.6F)
                    .sound(SoundType.GRAVEL));

    /** 月华原木 — faintly glowing trunk of the Moonforest trees. */
    public static final DeferredBlock<RotatedPillarBlock> MOONLIGHT_LOG = BLOCKS.registerBlock("moonlight_log",
            RotatedPillarBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)
                    .lightLevel(state -> LOG_LIGHT));

    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_MOONLIGHT_LOG = BLOCKS.registerBlock("stripped_moonlight_log",
            RotatedPillarBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F)
                    .sound(SoundType.WOOD));

    public static final DeferredBlock<Block> MOONLIGHT_WOOD = BLOCKS.registerSimpleBlock("moonlight_wood",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)
                    .lightLevel(state -> LOG_LIGHT));

    public static final DeferredBlock<Block> STRIPPED_MOONLIGHT_WOOD = BLOCKS.registerSimpleBlock("stripped_moonlight_wood",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F)
                    .sound(SoundType.WOOD));

    public static final DeferredBlock<Block> MOONLIGHT_PLANKS = BLOCKS.registerSimpleBlock("moonlight_planks",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD));

    /** 月华树叶 — the glowing canopy of the Moonforest. */
    public static final DeferredBlock<LeavesBlock> MOONLIGHT_LEAVES = BLOCKS.register("moonlight_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(0.2F)
                    .randomTicks()
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, type) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
                    .lightLevel(state -> LEAVES_LIGHT)));

    /** 月华树苗 — grows into a Moonlight tree, and also handles 2x2 mega growth. */
    public static final DeferredBlock<MoonlightSaplingBlock> MOONLIGHT_SAPLING = BLOCKS.register("moonlight_sapling",
            () -> new MoonlightSaplingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .pushReaction(PushReaction.DESTROY)));

    /** 月之花 — the glowing flower of the Moonforest floor. */
    public static final DeferredBlock<MoonBloomBlock> MOON_BLOOM = BLOCKS.register("moon_bloom",
            () -> new MoonBloomBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .noCollission()
                    .instabreak()
                    .randomTicks()
                    .sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .pushReaction(PushReaction.DESTROY)
                    .lightLevel(state -> 6)));

    /** 温泉 — the hot spring fluid block that fills the pools of the 露天温泉 biome. */
    public static final DeferredBlock<LiquidBlock> HOT_SPRING = BLOCKS.register("hot_spring",
            () -> new LiquidBlock(NMAFluids.HOT_SPRING.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WATER)
                    .replaceable()
                    .noCollission()
                    .strength(100.0F)
                    .pushReaction(PushReaction.DESTROY)
                    .liquid()
                    .noLootTable()));

    /** 梦眠祭坛 — offer one 月明珠 to call the 梦眠月鹿 boss, at the cost of the altar itself. */
    public static final DeferredBlock<DreamsleepAltarBlock> DREAMSLEEP_ALTAR = BLOCKS.register("dreamsleep_altar",
            () -> new DreamsleepAltarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.5F, 9.0F)
                    .sound(SoundType.DEEPSLATE)
                    .lightLevel(state -> 7)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    private NMABlocks() {
    }
    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
