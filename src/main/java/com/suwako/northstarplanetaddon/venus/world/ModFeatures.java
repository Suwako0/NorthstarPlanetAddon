package com.suwako.northstarplanetaddon.venus.world;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 世界生成特征注册。
 *
 * <p>JSON 侧：{@code data/phosphoros/worldgen/configured_feature/cloud_island.json}
 * + {@code data/phosphoros/worldgen/placed_feature/cloud_island.json}，
 * 由 {@code cloud_archipelago} 群系的 features 第 10 组（vegetal_decoration）引用。</p>
 */
public final class ModFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, VenusModule.NAMESPACE);

    /** 云海浮空岛。 */
    public static final DeferredHolder<Feature<?>, CloudIslandFeature> CLOUD_ISLAND =
            FEATURES.register("cloud_island", () -> new CloudIslandFeature(NoneFeatureConfiguration.CODEC));

    /** 云港遗迹（第 10 章 #1）：平台 + 围墙 + 四角晶塔 + 中央塔 + 战利品箱。 */
    public static final DeferredHolder<Feature<?>, CloudHarborFeature> CLOUD_HARBOR =
            FEATURES.register("cloud_harbor", () -> new CloudHarborFeature(NoneFeatureConfiguration.CODEC));

    /** 酸泵站（10 章 #2）。 */
    public static final DeferredHolder<Feature<?>, AcidPumpStationFeature> ACID_PUMP_STATION =
            FEATURES.register("acid_pump_station", () -> new AcidPumpStationFeature(NoneFeatureConfiguration.CODEC));

    /** 高压观测塔（10 章 #3）：赫菲斯托斯的剧情召唤点。 */
    public static final DeferredHolder<Feature<?>, ObservationTowerFeature> OBSERVATION_TOWER =
            FEATURES.register("observation_tower", () -> new ObservationTowerFeature(NoneFeatureConfiguration.CODEC));

    /** 真菌苗圃（10 章 #4）。 */
    public static final DeferredHolder<Feature<?>, FungalNurseryFeature> FUNGAL_NURSERY =
            FEATURES.register("fungal_nursery", () -> new FungalNurseryFeature(NoneFeatureConfiguration.CODEC));

    /** 坠毁飞艇（10 章 #5）。 */
    public static final DeferredHolder<Feature<?>, CrashedAirshipFeature> CRASHED_AIRSHIP =
            FEATURES.register("crashed_airship", () -> new CrashedAirshipFeature(NoneFeatureConfiguration.CODEC));

    /** 启明星祭坛（10 章 #7，结局多方块）。 */
    public static final DeferredHolder<Feature<?>, PhosphorosAltarFeature> PHOSPHOROS_ALTAR =
            FEATURES.register("phosphoros_altar", () -> new PhosphorosAltarFeature(NoneFeatureConfiguration.CODEC));
    /** 熔管城市（10 章 #6，Boss 3 场地）。 */
    public static final DeferredHolder<Feature<?>, LavaTubeCityFeature> LAVA_TUBE_CITY =
            FEATURES.register("lava_tube_city", () -> new LavaTubeCityFeature(NoneFeatureConfiguration.CODEC));
    private ModFeatures() {
    }
}
