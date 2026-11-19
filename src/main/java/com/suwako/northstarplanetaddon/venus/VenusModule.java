package com.suwako.northstarplanetaddon.venus;

import com.mojang.logging.LogUtils;
import com.suwako.northstarplanetaddon.venus.command.PhosphorosCommands;
import com.suwako.northstarplanetaddon.venus.compat.TaczCompat;
import com.suwako.northstarplanetaddon.venus.config.PhosphorosConfig;
import com.suwako.northstarplanetaddon.venus.event.CombatEvents;
import com.suwako.northstarplanetaddon.venus.event.PlayerEvents;
import com.suwako.northstarplanetaddon.venus.registry.ModArmorMaterials;
import com.suwako.northstarplanetaddon.venus.registry.ModBlockEntities;
import com.suwako.northstarplanetaddon.venus.registry.ModBlocks;
import com.suwako.northstarplanetaddon.venus.registry.ModComponents;
import com.suwako.northstarplanetaddon.venus.registry.ModEffects;
import com.suwako.northstarplanetaddon.venus.registry.ModEntities;
import com.suwako.northstarplanetaddon.venus.registry.ModItems;
import com.suwako.northstarplanetaddon.venus.registry.ModFluids;
import com.suwako.northstarplanetaddon.venus.registry.ModMenus;
import com.suwako.northstarplanetaddon.venus.registry.ModTabs;
import com.suwako.northstarplanetaddon.venus.world.ModFeatures;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * 北极星金星附属（合并后的金星模块，原「机械动力：启明星 / Create: Phosphoros」）——
 * 云海维度、气动压力机器、四套护甲、5 把武器、4 把工具、5 个群系、8 个结构与 3 个 Boss，
 * 以及「反枪械宪章」的弹道伤害管线。
 *
 * <p>与 Create / Northstar / TaCZ 的联动全部走注册名、标签与反射，<b>编译期不依赖它们</b>，
 * 因此本模块可以独立构建。</p>
 *
 * <p>合并进「北极星星球附属」后，本类不再是模组入口（入口是
 * {@link com.suwako.northstarplanetaddon.NorthstarPlanetAddon}），只由它调用
 * {@link #init(IEventBus, ModContainer)} 转交注册与配置。四个星球共享同一个注册命名空间
 * {@link com.suwako.northstarplanetaddon.NorthstarPlanetAddon#MODID}，模块之间用
 * {@link #MODULE} 区分创造模式标签页与语言键。「启明星」作为本模块的素材 / 装备品牌名保留
 * （启明星核、启明星战甲、启明星祭坛……）。</p>
 */
public final class VenusModule {

    /** 统一的注册命名空间（= 模组 ID）。 */
    public static final String NAMESPACE = com.suwako.northstarplanetaddon.NorthstarPlanetAddon.MODID;

    /** 本模块在统一命名空间里的前缀（创造模式标签页 ID、语言键用）。 */
    public static final String MODULE = "venus";

    public static final Logger LOGGER = LogUtils.getLogger();

    /** 云海维度 ID（数据包在 data/northstar_planet_addon/dimension/venus_clouds.json）。 */
    public static final ResourceLocation VENUS_CLOUDS = id("venus_clouds");

    private VenusModule() {
    }

    /** 由 {@link com.suwako.northstarplanetaddon.NorthstarPlanetAddon} 在模组构造期间调用一次。 */
    public static void init(IEventBus modBus, ModContainer container) {
        // ---------------------------------------------------------- 注册表
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modBus);
        ModArmorMaterials.ARMOR_MATERIALS.register(modBus);
        ModComponents.DATA_COMPONENTS.register(modBus);
        ModEffects.MOB_EFFECTS.register(modBus);
        ModEntities.ENTITY_TYPES.register(modBus);
        ModTabs.CREATIVE_MODE_TABS.register(modBus);
        ModFeatures.FEATURES.register(modBus);
        com.suwako.northstarplanetaddon.venus.machine.ModRecipes.RECIPE_TYPES.register(modBus);
        com.suwako.northstarplanetaddon.venus.machine.ModRecipes.RECIPE_SERIALIZERS.register(modBus);
        ModFluids.FLUID_TYPES.register(modBus);
        ModFluids.FLUIDS.register(modBus);
        com.suwako.northstarplanetaddon.venus.world.ModWorldgen.BIOME_SOURCES.register(modBus);
        ModMenus.MENU_TYPES.register(modBus);

        // 生物属性：EntityAttributeCreationEvent 属于 mod bus
        modBus.addListener(ModEntities::registerAttributes);
        // 机器槽位的 capability（漏斗 / 管道）
        modBus.addListener(com.suwako.northstarplanetaddon.venus.event.ModCapabilities::register);

        // ---------------------------------------------------------- 配置
        // 文件名带模块前缀，四个星球共用同一个 config/ 目录时不打架。
        container.registerConfig(ModConfig.Type.COMMON, PhosphorosConfig.SPEC,
                "northstar_planet_addon-venus-common.toml");

        // ---------------------------------------------------------- 游戏总线事件
        NeoForge.EVENT_BUS.register(CombatEvents.class);
        NeoForge.EVENT_BUS.register(PlayerEvents.class);
        NeoForge.EVENT_BUS.register(PhosphorosCommands.class);
        NeoForge.EVENT_BUS.addListener(TaczCompat::onServerStarted);

        LOGGER.info("[venus] 北极星金星附属：护甲板 / 弱点 / 弹道伤害管线已装载（反枪械宪章 R1–R5）");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(NAMESPACE, path);
    }
}
