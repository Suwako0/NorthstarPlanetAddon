package com.suwako.northstarplanetaddon;

import com.mojang.logging.LogUtils;
import com.suwako.northstarplanetaddon.mars.MarsModule;
import com.suwako.northstarplanetaddon.mercury.MercuryModule;
import com.suwako.northstarplanetaddon.moon.MoonModule;
import com.suwako.northstarplanetaddon.venus.VenusModule;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * 北极星星球附属 / Northstar Planet Addon —— Create: Northstar Redux 的月球、水星、火星、金星
 * 四个星球附属合而为一的模组入口。
 *
 * <p>四个星球附属共用同一个注册命名空间 {@link #MODID}，只用路径前缀与
 * {@code itemGroup.<namespace>.<module>} 这样的语言键区分模块：</p>
 *
 * <table border="1">
 *   <caption>四个星球附属</caption>
 *   <tr><th>模块</th><th>入口类</th><th>命名空间 / 标签页前缀</th><th>目标星球</th></tr>
 *   <tr><td>北极星月球附属</td><td>{@link MoonModule}</td><td>{@code northstar_planet_addon} / {@code moon}</td><td>{@code northstar:moon}</td></tr>
 *   <tr><td>北极星水星附属</td><td>{@link MercuryModule}</td><td>{@code northstar_planet_addon} / {@code mercury}</td><td>{@code northstar:mercury}</td></tr>
 *   <tr><td>北极星火星附属</td><td>{@link MarsModule}</td><td>{@code northstar_planet_addon} / {@code mars}</td><td>{@code northstar:mars}</td></tr>
 *   <tr><td>北极星金星附属</td><td>{@link VenusModule}</td><td>{@code northstar_planet_addon} / {@code venus}</td><td>{@code northstar_planet_addon:venus_clouds}（自定义维度）</td></tr>
 * </table>
 *
 * <p>四个模块各自在自己的 {@code init} 里完成注册表、事件、网络与客户端接线；本类只做顺序编排。
 * 注册顺序按「流体类型 → 流体 → 方块 → 物品 → 其它」的依赖方向，每个模块内部自洽，
 * 模块之间没有注册表依赖。</p>
 */
@Mod(NorthstarPlanetAddon.MODID)
public final class NorthstarPlanetAddon {

    /** 唯一的模组 ID，同时是四个星球附属共用的注册命名空间。 */
    public static final String MODID = "northstar_planet_addon";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NorthstarPlanetAddon(IEventBus modBus, ModContainer container) {
        // ------------------------------------------------------------ 北极星月球附属
        // 月兔 / 月壳蟹 / 月鹿、月林与露天温泉群系、温泉流体、梦眠草家族。
        MoonModule.init(modBus);

        // ------------------------------------------------------------ 北极星水星附属
        // 太阳风暴天气、水星沙、干涸海床群系、太阳鱼、沐浴阳光。
        MercuryModule.init(modBus);

        // ------------------------------------------------------------ 北极星火星附属
        // 三个火星群系、恶魔蛾、火星钢装备、沙尘暴、便携式冷却器、Boss 阿瑞斯与星核装备。
        MarsModule.init(modBus);

        // ------------------------------------------------------------ 北极星金星附属
        // 云海维度、气动压力机器、四套护甲 / 五把武器、三 Boss、弹道伤害管线（需要 ModContainer 注册配置）。
        VenusModule.init(modBus, container);

        LOGGER.info("北极星星球附属 / Northstar Planet Addon {} 已装载：月球 + 水星 + 火星 + 金星四个星球附属。",
                ModList.get().getModContainerById(MODID)
                        .map(modContainer -> modContainer.getModInfo().getVersion().toString())
                        .orElse("unknown"));
    }

    /** 本模组命名空间下的资源路径（四个星球附属共用）。 */
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
