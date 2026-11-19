package com.suwako.northstarplanetaddon.mercury;

import com.suwako.northstarplanetaddon.NorthstarPlanetAddon;
import com.suwako.northstarplanetaddon.mercury.client.MercuryClient;
import com.suwako.northstarplanetaddon.mercury.command.SolarStormCommand;
import com.suwako.northstarplanetaddon.mercury.entity.BlazingSunfishEntity;
import com.suwako.northstarplanetaddon.mercury.entity.SunfishEntity;
import com.suwako.northstarplanetaddon.mercury.network.SolarStormPayload;
import com.suwako.northstarplanetaddon.mercury.registry.MNABlocks;
import com.suwako.northstarplanetaddon.mercury.registry.MNAEntityTypes;
import com.suwako.northstarplanetaddon.mercury.registry.MNAFeatures;
import com.suwako.northstarplanetaddon.mercury.registry.MNAItems;
import com.suwako.northstarplanetaddon.mercury.registry.MNAMobEffects;
import com.suwako.northstarplanetaddon.mercury.registry.MNASounds;
import com.suwako.northstarplanetaddon.mercury.world.MercuryBiomeInjector;
import com.suwako.northstarplanetaddon.mercury.world.SolarStormManager;
import com.suwako.northstarplanetaddon.mercury.world.SunbathManager;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 北极星水星附属（合并后的水星模块）—— 给 Create: Northstar — Redux 的水星加太阳风暴天气、
 * 水星沙与干涸海床群系，以及太阳鱼 / 炽烈太阳鱼与「沐浴阳光」效果。
 *
 * <p>合并进「北极星星球附属」后，本类不再是模组入口（入口是
 * {@link NorthstarPlanetAddon}），只由它调用 {@link #init(IEventBus)}。这里仍负责三件事：
 * <ul>
 * <li>本模块的注册表与它自己的创造模式标签页，</li>
 * <li>在任何维度被创建之前运行的群系注入（{@code ServerAboutToStartEvent}），</li>
 * <li>风暴的服务端刻、{@code /solarstorm} 指令与那一个客户端方向的网络包。</li>
 * </ul>
 * 四个星球共享同一个注册命名空间 {@link NorthstarPlanetAddon#MODID}，模块之间用
 * {@link #MODULE} 区分创造模式标签页与语言键。</p>
 */
public final class MercuryModule {

    /** 统一的注册命名空间（= 模组 ID）。 */
    public static final String NAMESPACE = NorthstarPlanetAddon.MODID;

    /** 本模块在统一命名空间里的前缀（创造模式标签页 ID、语言键用）。 */
    public static final String MODULE = "mercury";

    public static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE + "." + MODULE);

    /** 本模块的创造模式标签页 —— 水星附属的全部方块与物品都在这里。 */
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NAMESPACE);

    public static final Supplier<CreativeModeTab> MERCURY_TAB = CREATIVE_TABS.register(MODULE, () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + NAMESPACE + "." + MODULE))
            .icon(() -> new ItemStack(MNAItems.MERCURY_SAND.get()))
            .displayItems((parameters, output) -> {
                output.accept(MNAItems.MERCURY_SAND.get());
                output.accept(MNAItems.SUNFISH_SLICE.get());
                output.accept(MNAItems.SUNFISH_JAM.get());
                output.accept(MNAItems.SUNFISH_SPAWN_EGG.get());
                output.accept(MNAItems.BLAZING_SUNFISH_SPAWN_EGG.get());
            })
            .build());

    private MercuryModule() {
    }

    /** 由 {@link NorthstarPlanetAddon} 在模组构造期间调用一次。 */
    public static void init(IEventBus modEventBus) {
        MNABlocks.register(modEventBus);
        MNAItems.register(modEventBus);
        MNAEntityTypes.register(modEventBus);
        MNASounds.register(modEventBus);
        MNAFeatures.register(modEventBus);
        MNAMobEffects.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);

        modEventBus.addListener(MercuryModule::registerPayloads);
        modEventBus.addListener(MercuryModule::registerEntityAttributes);
        modEventBus.addListener(MercuryModule::registerSpawnPlacements);

        // 干涸海床：在任何维度/区块被创建之前，把群系挂进水星的多噪声群系源。
        NeoForge.EVENT_BUS.addListener(MercuryBiomeInjector::onServerAboutToStart);

        // 太阳风暴：每服务器刻推进天气；指令手动触发；水星以外的维度完全不受影响。
        NeoForge.EVENT_BUS.addListener(SolarStormManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(SolarStormCommand::onRegisterCommands);

        // 沐浴阳光：按玩家刻检查「水星地表 + 露天 + 白天」，满足即给予、不满足立即移除。
        NeoForge.EVENT_BUS.addListener(SunbathManager::onPlayerTick);

        // 一眼可查的一行日志：确认 mods/ 里加载的确实是这一份构建。
        LOGGER.info("[mercury] 北极星水星附属 {} 已装载：太阳风暴、水星沙与干涸海床（northstar:mercury）。",
                ModList.get().getModContainerById(NorthstarPlanetAddon.MODID)
                        .map(container -> container.getModInfo().getVersion().toString())
                        .orElse("unknown"));

        // 客户端接线。引用留在 dist 判断内部，专用服务器永远不会加载客户端类。
        if (FMLEnvironment.dist.isClient()) {
            MercuryClient.register(modEventBus);
        }
    }

    /** The storm's whole network presence: one boolean, server to client. */
    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(
                SolarStormPayload.TYPE, SolarStormPayload.STREAM_CODEC, SolarStormPayload::handleOnClient);
    }

    /** 太阳鱼与炽烈太阳鱼的属性。 */
    private static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(MNAEntityTypes.SUNFISH.get(), SunfishEntity.createAttributes().build());
        event.put(MNAEntityTypes.BLAZING_SUNFISH.get(), BlazingSunfishEntity.createAttributes().build());
    }

    /**
     * Both fish spawn in open sky rather than on the ground, so they use an unrestricted placement
     * type with a rule of their own: two blocks of air to appear in. A spawn egg or a spawner bypasses
     * the rule, exactly like vanilla.
     */
    private static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
                MNAEntityTypes.SUNFISH.get(),
                SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                MercuryModule::checkOpenSkySpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                MNAEntityTypes.BLAZING_SUNFISH.get(),
                SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                MercuryModule::checkOpenSkySpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static <T extends Entity> boolean checkOpenSkySpawnRules(
            EntityType<T> type, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (spawnType != MobSpawnType.NATURAL) {
            return true;
        }
        return level.canSeeSky(pos)
                && level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir();
    }
}
