package com.suwako.northstarplanetaddon.mars.registry;

import com.suwako.northstarplanetaddon.mars.MarsModule;
import com.suwako.northstarplanetaddon.mars.block.AresAltarBlock;
import com.suwako.northstarplanetaddon.mars.block.DryIceBlock;
import com.suwako.northstarplanetaddon.mars.block.MarsIceBlock;
import com.suwako.northstarplanetaddon.mars.block.MarsStrawberryCropBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 本模组方块。火星砂砾 / 火星石 / 火山岩等已由 Northstar 提供，此处不重复注册。 */
public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(MarsModule.NAMESPACE);

    /** 赤铁岩 —— 火星基础岩石，地表覆盖层之一。 */
    public static final DeferredBlock<Block> RED_IRONSTONE = BLOCKS.register("red_ironstone",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .requiresCorrectToolForDrops()
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.STONE)));

    /**
     * 火星冰 —— 极地冰冠的主要构成。
     *
     * <p>刻意用 {@link MarsIceBlock} 而不是原版 {@code IceBlock}：原版冰会因方块光（火把/岩浆）
     * 随机融化、并在挖掘时变成水。现在冰在野外完全稳定，融化走配方 / 机器。</p>
     */
    public static final DeferredBlock<MarsIceBlock> MARS_ICE = BLOCKS.register("mars_ice",
            () -> new MarsIceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .friction(0.98F)
                    .strength(0.5F)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, entityType) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)));

    /** 干冰 —— 极地冰冠的另一半。野外稳定，升华走配方（不再随机刻消失）。 */
    public static final DeferredBlock<DryIceBlock> DRY_ICE = BLOCKS.register("dry_ice",
            () -> new DryIceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(0.6F)
                    .sound(SoundType.GLASS)
                    .noOcclusion()));

    /** 火星草莓作物。 */
    public static final DeferredBlock<MarsStrawberryCropBlock> MARS_STRAWBERRY_CROP = BLOCKS.register(
            "mars_strawberry_crop",
            () -> new MarsStrawberryCropBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .pushReaction(PushReaction.DESTROY)));

    /** 星核：装饰方块（v1.12 起召唤不再依赖它，祭坛自己就能召唤）。 */
    public static final DeferredBlock<Block> STAR_CORE = BLOCKS.register("star_core",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .requiresCorrectToolForDrops()
                    .strength(4.0F, 12.0F)
                    .lightLevel(state -> 12)
                    .sound(SoundType.METAL)));

    /** 阿瑞斯祭坛：手持熔岩桶右击召唤 Boss（v1.12 重新设计，单方块、不再需要 3×3×3 外壳）。 */
    public static final DeferredBlock<Block> ARES_ALTAR_CASING = BLOCKS.register("ares_altar_casing",
            () -> new AresAltarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NETHER)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F, 20.0F)
                    .lightLevel(state -> 7)
                    .sound(SoundType.NETHER_BRICKS)));

    private ModBlocks() {
    }
}
