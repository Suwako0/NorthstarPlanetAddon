package com.suwako.northstarplanetaddon.venus.registry;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import com.suwako.northstarplanetaddon.venus.block.PhosphorosCropBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 方块：材料方块 + 作物。
 *
 * <p><b>气动压力机器（压缩机 / 压力罐 / 高压锅 / 高压熔炉 / 散热器 / 冷凝塔 / 云梯 /
 * 气动马达 / 酸泵 / 菌床）已整体移除</b>，连同它们的方块实体、配方、战利品表与贴图。
 * 移除前的实现与清单见项目根目录 {@code .backup/industry-2026*}/MANIFEST.txt；
 * 移除原因与当时的状况记录在 {@code docs/VENUS-INDUSTRY-STATUS.md}。</p>
 */
public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(VenusModule.NAMESPACE);

    /** 钨块：可作信标底座，需钨钢级工具。 */
    public static final DeferredBlock<Block> TUNGSTEN_BLOCK = BLOCKS.registerSimpleBlock("tungsten_block",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL));

    /** 硫晶块：微光（亮度 6），酸蚀工业的基础材料。 */
    public static final DeferredBlock<Block> SULFUR_CRYSTAL_BLOCK = BLOCKS.registerSimpleBlock("sulfur_crystal_block",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .requiresCorrectToolForDrops()
                    .strength(1.5F, 3.0F)
                    .lightLevel(state -> 6)
                    .sound(SoundType.AMETHYST));

    /** 云岩：云海浮空岛的岩体，很轻（硬度 0.8）。 */
    public static final DeferredBlock<Block> CLOUD_STONE = BLOCKS.registerSimpleBlock("cloud_stone",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOL)
                    .strength(0.8F, 2.0F)
                    .sound(SoundType.CALCITE)
                    .pushReaction(PushReaction.NORMAL));

    /** Giant fungus crop: 8 min, harvests fungus fiber. */
    public static final DeferredBlock<PhosphorosCropBlock> GIANT_FUNGUS_CROP = BLOCKS.register("giant_fungus_crop",
            () -> new PhosphorosCropBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN).noCollission().randomTicks()
                    .instabreak().sound(SoundType.CROP), () -> ModItems.GIANT_FUNGUS_SPORE.get(),
                    () -> ModItems.FUNGUS_FIBER.get(), 2, 4, 12));

    /** Acid vegetable crop: 12 min, harvests acid-pickled vegetable. */
    public static final DeferredBlock<PhosphorosCropBlock> ACID_VEGETABLE_CROP =
            BLOCKS.register("acid_vegetable_crop", () -> new PhosphorosCropBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN).noCollission().randomTicks()
                    .instabreak().sound(SoundType.CROP), () -> ModItems.ACID_VEGETABLE_SEEDS.get(),
                    () -> ModItems.ACID_VEGETABLE.get(), 2, 2, 18));

    /** Cloud algae crop: 6 min, harvests cloud algae jelly. */
    public static final DeferredBlock<PhosphorosCropBlock> CLOUD_ALGAE_CROP =
            BLOCKS.register("cloud_algae_crop", () -> new PhosphorosCropBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN).noCollission().randomTicks()
                    .instabreak().sound(SoundType.CROP), () -> ModItems.CLOUD_ALGAE_SPORE.get(),
                    () -> ModItems.CLOUD_ALGAE_JELLY.get(), 3, 3, 9));

    private ModBlocks() {
    }
}
