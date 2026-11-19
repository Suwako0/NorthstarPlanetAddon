package com.suwako.northstarplanetaddon.mars.registry;

import com.suwako.northstarplanetaddon.mars.MarsModule;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

/** 本模组定义的标签（数据包文件在 data/northstar_planet_addon/tags/ 下）。 */
public final class ModTags {

    private ModTags() {
    }

    public static final class Blocks {

        /** 火星硬岩：赤铁岩 / 火星石 / 深火星石 / 火山岩 —— 火星钢镐在此双倍挖掘速度。 */
        public static final TagKey<Block> MARTIAN_HARD_ROCK = tag("martian_hard_rock");

        /** 可以种植火星草莓的耕地（含 northstar:mars_farmland）。 */
        public static final TagKey<Block> MARS_FARMLAND = tag("mars_farmland");

        private Blocks() {
        }

        private static TagKey<Block> tag(String path) {
            return TagKey.create(Registries.BLOCK, MarsModule.id(path));
        }
    }

    public static final class Items {

        /** 火星钢镐（含 northstar:martian_pickaxe）。 */
        public static final TagKey<Item> MARTIAN_STEEL_PICKAXES = tag("martian_steel_pickaxes");

        /** 火星钢护甲（含 Northstar 的火星钢四件套）—— 集齐后提供耐腐蚀。 */
        public static final TagKey<Item> CORROSION_RESISTANT_ARMOR = tag("corrosion_resistant_armor");

        /** 熔核护甲的修复材料（合成配方用火星钢锭 + 星核核心，修复用火星钢锭）。 */
        public static final TagKey<Item> MOLTEN_CORE_REPAIR = tag("molten_core_repair");

        private Items() {
        }

        private static TagKey<Item> tag(String path) {
            return TagKey.create(Registries.ITEM, MarsModule.id(path));
        }
    }

    public static final class Fluids {

        /** 低压岩浆（含 minecraft:lava，便于其它模组扩展）。 */
        public static final TagKey<Fluid> LOW_PRESSURE_LAVA = TagKey.create(Registries.FLUID,
                MarsModule.id("low_pressure_lava"));

        private Fluids() {
        }
    }
}
