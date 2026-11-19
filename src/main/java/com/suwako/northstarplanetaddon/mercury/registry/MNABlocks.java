package com.suwako.northstarplanetaddon.mercury.registry;

import com.suwako.northstarplanetaddon.mercury.MercuryModule;
import com.suwako.northstarplanetaddon.mercury.block.MercurySandBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Every block added by the mod. */
public final class MNABlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MercuryModule.NAMESPACE);

    /**
     * 水星沙 — the pale, bone-dry sand of Mercury. It forms the bed of the 干涸海床 biome and the
     * scorched floor of a fresh meteorite crater.
     * <p>
     * It is a {@link MercurySandBlock} (a vanilla-style gravity block), so sand poured into a crater
     * or stacked by a player behaves like overworld sand does.
     */
    public static final DeferredBlock<MercurySandBlock> MERCURY_SAND = BLOCKS.register("mercury_sand",
            () -> new MercurySandBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(0.5F)
                    .sound(SoundType.SAND)));

    private MNABlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
