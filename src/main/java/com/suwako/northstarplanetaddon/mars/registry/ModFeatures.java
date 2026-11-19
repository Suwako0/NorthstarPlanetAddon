package com.suwako.northstarplanetaddon.mars.registry;

import com.suwako.northstarplanetaddon.mars.MarsModule;
import com.suwako.northstarplanetaddon.mars.world.MarsCraterConfig;
import com.suwako.northstarplanetaddon.mars.world.MarsCraterFeature;
import com.suwako.northstarplanetaddon.mars.world.MarsSurfaceConfig;
import com.suwako.northstarplanetaddon.mars.world.MarsSurfaceLayerFeature;
import com.suwako.northstarplanetaddon.mars.world.SpireConfig;
import com.suwako.northstarplanetaddon.mars.world.SpireFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 本模组的世界生成特征类型。 */
public final class ModFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, MarsModule.NAMESPACE);

    /** 火星表层铺设（配置见 MarsSurfaceConfig）。 */
    public static final DeferredHolder<Feature<?>, MarsSurfaceLayerFeature> MARS_SURFACE_LAYER =
            FEATURES.register("mars_surface_layer", () -> new MarsSurfaceLayerFeature(MarsSurfaceConfig.CODEC));

    /** 尖柱（火山岩柱 / 冰塔），给群系制造垂直起伏。 */
    public static final DeferredHolder<Feature<?>, SpireFeature> SPIRE =
            FEATURES.register("spire", () -> new SpireFeature(SpireConfig.CODEC));

    /**
     * 陨石坑（火山高地的岩浆湖坑 / 极地冰冠的冰核坑）。
     *
     * <p>v1.8 的抬升台地特征已按用户要求推翻删除：
     * 现在两个群系不再抬地形，而是在地表挖出「周围是熔岩的岩浆湖 + 中间是火星冰」的陨石坑。</p>
     */
    public static final DeferredHolder<Feature<?>, MarsCraterFeature> CRATER =
            FEATURES.register("crater", () -> new MarsCraterFeature(MarsCraterConfig.CODEC));

    private ModFeatures() {
    }
}
