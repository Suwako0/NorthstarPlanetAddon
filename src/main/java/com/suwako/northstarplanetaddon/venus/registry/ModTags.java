package com.suwako.northstarplanetaddon.venus.registry;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * 本模组的标签。
 *
 * <p>弹道标签（{@link #BALLISTIC}）是反枪械宪章的根：TaCZ 的四种子弹伤害类型
 * 以及 {@code #minecraft:is_projectile} 都归入这一类，见
 * {@code data/phosphoros/tags/damage_type/ballistic.json}。</p>
 */
public final class ModTags {

    /** 伤害类型标签。 */
    public static final class DamageTypes {
        /** 弹道：tacz:bullet* + #minecraft:is_projectile。 */
        public static final TagKey<DamageType> BALLISTIC =
                TagKey.create(Registries.DAMAGE_TYPE, VenusModule.id("ballistic"));
        /** 酸蚀：本模组酸液、酸雨、酸蚀矛腐蚀伤害。 */
        public static final TagKey<DamageType> ACID =
                TagKey.create(Registries.DAMAGE_TYPE, VenusModule.id("acid"));
        /** Boss 的机制伤害（落石、硫火等），不计入 DPS 上限但计单次上限。 */
        public static final TagKey<DamageType> BOSS_MECHANIC =
                TagKey.create(Registries.DAMAGE_TYPE, VenusModule.id("boss_mechanic"));

        private DamageTypes() {
        }
    }

    /** 物品标签。 */
    public static final class Items {
        /** 能修复钨钢压力服的物品。 */
        public static final TagKey<Item> TUNGSTEN_REPAIR = ItemTags.create(VenusModule.id("tungsten_repair"));
        /** Boss 掉落物（JEI/EMI 与整合包过滤用）。 */
        public static final TagKey<Item> BOSS_DROPS = ItemTags.create(VenusModule.id("boss_drops"));
        /** 穿戴后视为「耐压装备」的物品（压力伤害判断用）。 */
        public static final TagKey<Item> PRESSURE_SUIT = ItemTags.create(VenusModule.id("pressure_suit"));

        private Items() {
        }
    }

    /** 方块标签。 */
    public static final class Blocks {
        /** 需要钨钢级工具挖掘。 */
        public static final TagKey<Block> NEEDS_TUNGSTEN_TOOL =
                BlockTags.create(VenusModule.id("needs_tungsten_tool"));
        /** 菌类方块（菌剪快速采集）。 */
        public static final TagKey<Block> FUNGUS = BlockTags.create(VenusModule.id("fungus"));

        private Blocks() {
        }
    }

    /** 实体标签。 */
    public static final class EntityTypes {
        /** Boss：伤害管线只处理这一类。 */
        public static final TagKey<EntityType<?>> BOSS =
                TagKey.create(Registries.ENTITY_TYPE, VenusModule.id("boss"));
        /** 云海生物：掉出岛屿会被传送回岛屿。 */
        public static final TagKey<EntityType<?>> CLOUDSEA_FAUNA =
                TagKey.create(Registries.ENTITY_TYPE, VenusModule.id("cloudsea_fauna"));

        private EntityTypes() {
        }
    }

    /** 免得 IDE 提示“未使用导入”时误删 damage_type 相关常量。 */
    public static TagKey<DamageType> projectileTag() {
        return DamageTypeTags.IS_PROJECTILE;
    }

    private ModTags() {
    }
}
