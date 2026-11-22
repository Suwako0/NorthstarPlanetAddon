package com.suwako.northstarplanetaddon.moon;

import com.suwako.northstarplanetaddon.NorthstarPlanetAddon;
import com.suwako.northstarplanetaddon.moon.entity.MoonRabbitEntity;
import com.suwako.northstarplanetaddon.moon.entity.MoonShellCrabEntity;
import com.suwako.northstarplanetaddon.moon.registry.NMABlocks;
import com.suwako.northstarplanetaddon.moon.registry.NMAEntityTypes;
import com.suwako.northstarplanetaddon.moon.registry.NMAFeatures;
import com.suwako.northstarplanetaddon.moon.registry.NMAFluids;
import com.suwako.northstarplanetaddon.moon.registry.NMAFluidTypes;
import com.suwako.northstarplanetaddon.moon.registry.NMAItems;
import com.suwako.northstarplanetaddon.moon.registry.NMASounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * 北极星月球附属（合并后的月球模块）—— 给 Create: Northstar - Redux 的月球加月兔、月壳蟹、月鹿、
 * 月林 / 露天温泉两个群系、梦眠草家族与温泉流体。
 *
 * <p>合并进「北极星星球附属」后，本类不再是模组入口（入口是
 * {@link com.suwako.northstarplanetaddon.NorthstarPlanetAddon}），只由它调用
 * {@link #init(IEventBus)}。四个星球共享同一个注册命名空间 {@link NorthstarPlanetAddon#MODID}，
 * 模块之间用 {@link #MODULE} 区分创造模式标签页与语言键。</p>
 */
public final class MoonModule {

    /** 统一的注册命名空间（= 模组 ID）。 */
    public static final String NAMESPACE = NorthstarPlanetAddon.MODID;

    /** 本模块在统一命名空间里的前缀（创造模式标签页 ID、语言键用）。 */
    public static final String MODULE = "moon";

    public static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE + "." + MODULE);

    /**
     * Ground blocks the two new mobs may spawn on. The Moon has no light and the vanilla animal
     * spawn rule requires a bright block, so the Moonforest mobs use this tag instead.
     */
    public static final TagKey<Block> MOON_FOREST_SPAWNABLE = TagKey.create(
            net.minecraft.core.registries.Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(NAMESPACE, "moon_forest_spawnable"));

    /** 本模块的创造模式标签页 —— 月球附属的全部方块与物品都在这里。 */
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NAMESPACE);

    public static final Supplier<CreativeModeTab> MOON_TAB = CREATIVE_TABS.register(MODULE, () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + NAMESPACE + "." + MODULE))
            .icon(() -> new ItemStack(NMAItems.MOON_BLOOM.get()))
            .displayItems((parameters, output) -> {
                // Soil and plants first, then the wood family, then items and the spawn eggs.
                output.accept(NMAItems.DREAMSLEEP_GRASS.get());
                output.accept(NMAItems.MOON_SOIL.get());
                output.accept(NMAItems.MOON_BLOOM.get());
                output.accept(NMAItems.MOONLIGHT_LOG.get());
                output.accept(NMAItems.MOONLIGHT_WOOD.get());
                output.accept(NMAItems.STRIPPED_MOONLIGHT_LOG.get());
                output.accept(NMAItems.STRIPPED_MOONLIGHT_WOOD.get());
                output.accept(NMAItems.MOONLIGHT_PLANKS.get());
                output.accept(NMAItems.MOONLIGHT_LEAVES.get());
                output.accept(NMAItems.MOONLIGHT_SAPLING.get());
                output.accept(NMAItems.DREAMSLEEP_ALTAR.get());
                output.accept(NMAItems.MOON_SHELL.get());
                output.accept(NMAItems.MOONCAKE.get());
                output.accept(NMAItems.MOON_PEARL.get());
                output.accept(NMAItems.BROKEN_MOON_PEARL.get());
                output.accept(NMAItems.MOON_DEER_ANTLER.get());
                output.accept(NMAItems.MOON_DEER_AXE.get());
                output.accept(NMAItems.HOT_SPRING_BUCKET.get());
                output.accept(NMAItems.MOON_RABBIT_SPAWN_EGG.get());
                output.accept(NMAItems.MOON_SHELL_CRAB_SPAWN_EGG.get());
                output.accept(NMAItems.LUNAR_DEER_SPAWN_EGG.get());
            })
            .build());

    private MoonModule() {
    }

    /** 由 {@link com.suwako.northstarplanetaddon.NorthstarPlanetAddon} 在模组构造期间调用一次。 */
    public static void init(IEventBus modEventBus) {
        // Fluid types first: every fluid below reaches for its type when it is constructed, and the
        // fluids in turn are needed by the 温泉 liquid block and by the hot spring bucket.
        NMAFluidTypes.register(modEventBus);
        NMAFluids.register(modEventBus);
        NMABlocks.register(modEventBus);
        NMAItems.register(modEventBus);
        NMAFeatures.register(modEventBus);
        NMASounds.register(modEventBus);
        NMAEntityTypes.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);

        modEventBus.addListener(MoonModule::registerEntityAttributes);
        modEventBus.addListener(MoonModule::registerSpawnPlacements);

        // The Moonforest and the Open Hot Spring are attached to Northstar's decoded Moon biome
        // source before any level or chunk is created; the handler targets only the northstar:moon
        // dimension id.
        NeoForge.EVENT_BUS.addListener(com.suwako.northstarplanetaddon.moon.world.MoonBiomeInjector::onServerAboutToStart);

        // Soaking in 温泉 regenerates health and keeps entities fed.
        NeoForge.EVENT_BUS.addListener(com.suwako.northstarplanetaddon.moon.world.HotSpringSoakHandler::onEntityTick);

        // One grep-able line that proves which build is actually loaded, so "the jar in mods/ is an old
        // one" is never a silent possibility again.
        LOGGER.info("[moon] 北极星月球附属 {}：已装载，向 northstar:moon 注入月林与露天温泉群系"
                        + "（不需要改过的北极星 jar）。",
                net.neoforged.fml.ModList.get().getModContainerById(NorthstarPlanetAddon.MODID)
                        .map(container -> container.getModInfo().getVersion().toString())
                        .orElse("unknown"));

        // Client-only wiring. The reference stays inside this guarded branch so the client classes
        // are never loaded on a dedicated server.
        if (FMLEnvironment.dist.isClient()) {
            com.suwako.northstarplanetaddon.moon.client.NMAClient.register(modEventBus);
        }
    }

    private static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(NMAEntityTypes.MOON_RABBIT.get(), MoonRabbitEntity.createAttributes().build());
        event.put(NMAEntityTypes.MOON_SHELL_CRAB.get(), MoonShellCrabEntity.createAttributes().build());
        event.put(
                NMAEntityTypes.MOON_DEER.get(),
                com.suwako.northstarplanetaddon.moon.entity.MoonDeerEntity.createAttributes().build());
    }

    private static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
                NMAEntityTypes.MOON_RABBIT.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                MoonModule::checkMoonForestSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);

        event.register(
                NMAEntityTypes.MOON_SHELL_CRAB.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                MoonModule::checkMoonForestSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /** Spawns anywhere on Moonforest soil, regardless of sky light (the Moon is airless and dark). */
    private static <T extends net.minecraft.world.entity.Entity> boolean checkMoonForestSpawnRules(
            EntityType<T> type, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (spawnType == MobSpawnType.SPAWNER) {
            return true;
        }
        BlockState below = level.getBlockState(pos.below());
        return below.is(MOON_FOREST_SPAWNABLE);
    }
}
