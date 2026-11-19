package com.suwako.northstarplanetaddon.mars;

import com.mojang.logging.LogUtils;
import com.suwako.northstarplanetaddon.NorthstarPlanetAddon;
import com.suwako.northstarplanetaddon.mars.registry.ModArmorMaterials;
import com.suwako.northstarplanetaddon.mars.registry.ModBlocks;
import com.suwako.northstarplanetaddon.mars.registry.ModComponents;
import com.suwako.northstarplanetaddon.mars.registry.ModEffects;
import com.suwako.northstarplanetaddon.mars.registry.ModEntities;
import com.suwako.northstarplanetaddon.mars.registry.ModFeatures;
import com.suwako.northstarplanetaddon.mars.registry.ModItems;
import com.suwako.northstarplanetaddon.mars.registry.ModTabs;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import org.slf4j.Logger;

/**
 * 北极星火星附属（合并后的火星模块）—— Create: Northstar Redux 的火星扩展：三个火星群系、
 * 恶魔蛾、火星钢装备、沙尘暴与护目镜、便携式冷却器、火星草莓，以及 Boss 熔核巨像·阿瑞斯与星核装备。
 *
 * <p>本模块不直接引用 Northstar / Create 的类，全部集成走注册名 / 标签 / 数据包 ID，
 * 因此编译期不需要它们，运行时由 {@code neoforge.mods.toml} 声明为必需依赖。</p>
 *
 * <p>合并进「北极星星球附属」后，本类不再是模组入口（入口是 {@link NorthstarPlanetAddon}），
 * 只由它调用 {@link #init(IEventBus)}。本模块的绝大多数事件处理走 {@code @EventBusSubscriber}，
 * 见 {@code mars.event} / {@code mars.world} 包。四个星球共享同一个注册命名空间
 * {@link NorthstarPlanetAddon#MODID}，模块之间用 {@link #MODULE} 区分创造模式标签页与语言键。</p>
 */
public final class MarsModule {

    /** 统一的注册命名空间（= 模组 ID）。 */
    public static final String NAMESPACE = NorthstarPlanetAddon.MODID;

    /** 本模块在统一命名空间里的前缀（创造模式标签页 ID、语言键用）。 */
    public static final String MODULE = "mars";

    public static final Logger LOGGER = LogUtils.getLogger();

    private MarsModule() {
    }

    /** 由 {@link NorthstarPlanetAddon} 在模组构造期间调用一次。 */
    public static void init(IEventBus modBus) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModArmorMaterials.ARMOR_MATERIALS.register(modBus);
        ModEntities.ENTITY_TYPES.register(modBus);
        ModEffects.MOB_EFFECTS.register(modBus);
        ModComponents.DATA_COMPONENTS.register(modBus);
        ModFeatures.FEATURES.register(modBus);
        ModTabs.CREATIVE_TABS.register(modBus);

        LOGGER.info("[mars] 北极星火星附属：注册表已初始化（沙尘暴 / 阿瑞斯 / 火星钢 / 星核装备）");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(NAMESPACE, path);
    }
}