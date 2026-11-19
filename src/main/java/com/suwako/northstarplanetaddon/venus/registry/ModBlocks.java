package com.suwako.northstarplanetaddon.venus.registry;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import com.suwako.northstarplanetaddon.venus.block.AcidPumpBlock;
import com.suwako.northstarplanetaddon.venus.block.CloudLiftBlock;
import com.suwako.northstarplanetaddon.venus.block.CompressorBlock;
import com.suwako.northstarplanetaddon.venus.block.PhosphorosCropBlock;
import com.suwako.northstarplanetaddon.venus.block.PneumaticMotorBlock;
import com.suwako.northstarplanetaddon.venus.block.CondenserBlock;
import com.suwako.northstarplanetaddon.venus.block.FungusBedBlock;
import com.suwako.northstarplanetaddon.venus.block.HighPressureFurnaceBlock;
import com.suwako.northstarplanetaddon.venus.block.PressureCookerBlock;
import com.suwako.northstarplanetaddon.venus.block.PressureTankBlock;
import com.suwako.northstarplanetaddon.venus.block.RadiatorBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 方块。
 *
 * <p>本骨架只落地「材料方块 + 结构方块」，第 8 章的压力机器（压缩机 / 压力罐 / 高压锅 /
 * 高压熔炉 / 冷凝塔 / 云梯）需要 Create 的应力体系与 BlockEntity，属于 M3 里程碑，
 * 扩展点见 {@code compat/create} 目录说明。</p>
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

    // ------------------------------------------------------------------ 气动机器（第 8 章）
    /** 压力罐：8000 mB / 罐，可堆叠串联。 */
    public static final DeferredBlock<PressureTankBlock> PRESSURE_TANK = BLOCKS.register("pressure_tank",
            () -> new PressureTankBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)));

    /** 压缩机：手动曲柄 / 后续接 Create 应力。 */
    public static final DeferredBlock<CompressorBlock> COMPRESSOR = BLOCKS.register("compressor",
            () -> new CompressorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.5F, 6.0F)
                    .sound(SoundType.METAL)));

    /** 高压锅：4 份 / 200 tick，需 8 bar。 */
    public static final DeferredBlock<PressureCookerBlock> PRESSURE_COOKER = BLOCKS.register("pressure_cooker",
            () -> new PressureCookerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.5F, 6.0F)
                    .sound(SoundType.METAL)));

    /** 高压熔炉：1 份 / 400 tick，需 8 bar + 1200°C。 */
    public static final DeferredBlock<HighPressureFurnaceBlock> HIGH_PRESSURE_FURNACE =
            BLOCKS.register("high_pressure_furnace", () -> new HighPressureFurnaceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(4.0F, 8.0F)
                    .lightLevel(state -> 4)
                    .sound(SoundType.METAL)));

    /** 散热器：9×5×9 内降温 40°C，消耗雪/冰类冷却剂。 */
    public static final DeferredBlock<RadiatorBlock> RADIATOR = BLOCKS.register("radiator",
            () -> new RadiatorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.COPPER)));

    /** Fungus bed: grows fungus fiber every 8 minutes. */
    public static final DeferredBlock<FungusBedBlock> FUNGUS_BED = BLOCKS.register("fungus_bed",
            () -> new FungusBedBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(0.6F, 1.0F)
                    .sound(SoundType.FUNGUS)));

    /** Condenser: air + heat -> ice. */
    public static final DeferredBlock<CondenserBlock> CONDENSER = BLOCKS.register("condenser",
            () -> new CondenserBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.COPPER)));

    /** Cloud lift: vertical teleport using compressed air. */
    public static final DeferredBlock<CloudLiftBlock> CLOUD_LIFT = BLOCKS.register("cloud_lift",
            () -> new CloudLiftBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F, 6.0F)
                    .lightLevel(state -> 6)
                    .sound(SoundType.METAL)));
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
    /** Pneumatic motor: 20 mB/t air -> 32 RPM mechanical work for adjacent machines. */
    public static final DeferredBlock<PneumaticMotorBlock> PNEUMATIC_MOTOR = BLOCKS.register("pneumatic_motor",
            () -> new PneumaticMotorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.COPPER)));
    /** Acid pump: extracts sulfuric acid from its own fluid. */
    public static final DeferredBlock<AcidPumpBlock> ACID_PUMP = BLOCKS.register("acid_pump",
            () -> new AcidPumpBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.COPPER)));
    private ModBlocks() {
    }
}
