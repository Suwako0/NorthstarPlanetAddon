package com.suwako.northstarplanetaddon.mars.registry;

import com.suwako.northstarplanetaddon.mars.MarsModule;
import com.suwako.northstarplanetaddon.mars.item.CoolantTankItem;
import com.suwako.northstarplanetaddon.mars.item.LoreItem;
import com.suwako.northstarplanetaddon.mars.item.MoltenCoreArmorItem;
import com.suwako.northstarplanetaddon.mars.item.MoltenCoreSwordItem;
import com.suwako.northstarplanetaddon.mars.item.PortableCoolerItem;
import com.suwako.northstarplanetaddon.mars.item.StarcoreCannonItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 本模组物品。
 *
 * <p>火星钢镐/护甲/草莓等已由 Northstar 提供，本模组通过标签把它们纳入自己的玩法
 * （见 {@link ModTags}），不重复注册同功能物品。</p>
 */
public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(MarsModule.NAMESPACE);

    // ---------------------------------------------------------------- 材料

    /** 恶魔蛾翼膜：防腐蚀涂层的原料。 */
    public static final DeferredItem<Item> DEMON_MOTH_WING_MEMBRANE =
            simple("demon_moth_wing_membrane");

    /** 暗红鳞粉：附魔材料，也是召唤阿瑞斯的触媒。 */
    public static final DeferredItem<Item> DARK_RED_SCALE_DUST =
            simple("dark_red_scale_dust");

    /** 防腐蚀涂层。 */
    public static final DeferredItem<Item> ANTI_CORROSION_COATING =
            simple("anti_corrosion_coating");

    // ---------------------------------------------------------------- 装备

    /**
     * 沙尘暴护目镜：占头盔位。使用原版 iron 护甲材质，因此复用原版铁头盔贴图。
     */
    public static final DeferredItem<ArmorItem> SANDSTORM_GOGGLES = ITEMS.register("sandstorm_goggles",
            () -> new ArmorItem(ArmorMaterials.IRON, ArmorItem.Type.HELMET,
                    new Item.Properties().durability(220)));

    /** 便携式冷却器：装填水或液氧，用于给阿瑞斯降温。 */
    public static final DeferredItem<PortableCoolerItem> PORTABLE_COOLER = ITEMS.register("portable_cooler",
            () -> new PortableCoolerItem(new Item.Properties().stacksTo(1)));

    // 冷却液罐（弹夹）：空罐 → 机械动力注液 → 水罐 / 液氧罐。堆叠上限 16。
    public static final DeferredItem<CoolantTankItem> COOLANT_TANK = ITEMS.register("coolant_tank",
            () -> new CoolantTankItem(null, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<CoolantTankItem> WATER_COOLANT_TANK = ITEMS.register("water_coolant_tank",
            () -> new CoolantTankItem(CoolantTankItem.WATER, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<CoolantTankItem> LIQUID_OXYGEN_COOLANT_TANK =
            ITEMS.register("liquid_oxygen_coolant_tank",
                    () -> new CoolantTankItem(CoolantTankItem.LIQUID_OXYGEN, new Item.Properties().stacksTo(16)));

    // ---------------------------------------------------------------- 食物与农业

    /** 火星草莓：恢复 3 点饱食度，并给予 10 秒抗性提升。 */
    public static final DeferredItem<Item> MARS_STRAWBERRY = ITEMS.register("mars_strawberry",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(3)
                    .saturationModifier(0.3F)
                    .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0), 1.0F)
                    .build())));

    /** 火星草莓种子。 */
    public static final DeferredItem<ItemNameBlockItem> MARS_STRAWBERRY_SEEDS =
            ITEMS.register("mars_strawberry_seeds", () -> new ItemNameBlockItem(
                    ModBlocks.MARS_STRAWBERRY_CROP.get(), new Item.Properties()));

    // ---------------------------------------------------------------- v1.11 星核系列（《Boss属性重新设计》）

    /** 星核碎片：阿瑞斯的常规掉落（2–4），熔核巨剑的刃材。 */
    public static final DeferredItem<Item> STAR_CORE_SHARD = ITEMS.register("star_core_shard",
            () -> new LoreItem(new Item.Properties().fireResistant(),
                    "tooltip.northstar_planet_addon.star_core_shard"));

    /** 星核核心：阿瑞斯的稀有掉落（15%），星核炮与熔核护甲四件套的核心材料。 */
    public static final DeferredItem<Item> STAR_CORE_HEART = ITEMS.register("star_core_heart",
            () -> new LoreItem(new Item.Properties().fireResistant().rarity(Rarity.RARE),
                    "tooltip.northstar_planet_addon.star_core_heart"));

    /** 融核巨剑：攻击力 12，右键「地热斩」（扇形火焰伤害，消耗耐久）。 */
    public static final DeferredItem<MoltenCoreSwordItem> MOLTEN_CORE_SWORD =
            ITEMS.register("molten_core_sword", () -> new MoltenCoreSwordItem(new Item.Properties()
                    .durability(2400)
                    .fireResistant()
                    .rarity(Rarity.EPIC)
                    // 攻击力 12 = 修饰符 8 + 下界合金层级 3 + 空手 1
                    .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 8, -2.4F))));

    /** 星核炮：攻击力 20，右键「星核炮击」（5 秒区域轰炸）。 */
    public static final DeferredItem<StarcoreCannonItem> STARCORE_CANNON =
            ITEMS.register("starcore_cannon", () -> new StarcoreCannonItem(new Item.Properties()
                    .durability(1800)
                    .fireResistant()
                    .rarity(Rarity.EPIC)
                    // 攻击力 20 = 修饰符 16 + 下界合金层级 3 + 空手 1
                    .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 16, -2.6F))));

    // 熔核护甲四件套：数值为下界合金 ×1.5，耐久同样 ×1.5（610 / 888 / 832 / 721）
    public static final DeferredItem<ArmorItem> MOLTEN_CORE_HELMET =
            moltenCore("molten_core_helmet", ArmorItem.Type.HELMET, 610);
    public static final DeferredItem<ArmorItem> MOLTEN_CORE_CHESTPLATE =
            moltenCore("molten_core_chestplate", ArmorItem.Type.CHESTPLATE, 888);
    public static final DeferredItem<ArmorItem> MOLTEN_CORE_LEGGINGS =
            moltenCore("molten_core_leggings", ArmorItem.Type.LEGGINGS, 832);
    public static final DeferredItem<ArmorItem> MOLTEN_CORE_BOOTS =
            moltenCore("molten_core_boots", ArmorItem.Type.BOOTS, 721);

    private static DeferredItem<ArmorItem> moltenCore(String name, ArmorItem.Type type, int durability) {
        return ITEMS.register(name, () -> new MoltenCoreArmorItem(ModArmorMaterials.MOLTEN_CORE, type,
                new Item.Properties().durability(durability).fireResistant().rarity(Rarity.EPIC)));
    }

    // ---------------------------------------------------------------- 刷怪蛋
    public static final DeferredItem<SpawnEggItem> DEMON_MOTH_SPAWN_EGG = ITEMS.register("demon_moth_spawn_egg",
            () -> new SpawnEggItem(ModEntities.DEMON_MOTH.get(), 0x7A1414, 0x2B0A0A, new Item.Properties()));

    public static final DeferredItem<SpawnEggItem> ARES_SPAWN_EGG = ITEMS.register("ares_spawn_egg",
            () -> new SpawnEggItem(ModEntities.ARES.get(), 0x5A1A0A, 0xFF6A00, new Item.Properties()));

    // ---------------------------------------------------------------- 方块物品

    public static final DeferredItem<BlockItem> RED_IRONSTONE = blockItem("red_ironstone",
            ModBlocks.RED_IRONSTONE);
    public static final DeferredItem<BlockItem> MARS_ICE = blockItem("mars_ice", ModBlocks.MARS_ICE);
    public static final DeferredItem<BlockItem> DRY_ICE = blockItem("dry_ice", ModBlocks.DRY_ICE);
    public static final DeferredItem<BlockItem> STAR_CORE = blockItem("star_core", ModBlocks.STAR_CORE);
    public static final DeferredItem<BlockItem> ARES_ALTAR_CASING = blockItem("ares_altar_casing",
            ModBlocks.ARES_ALTAR_CASING);

    private static DeferredItem<Item> simple(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static DeferredItem<BlockItem> blockItem(String name,
                                                     net.neoforged.neoforge.registries.DeferredBlock<? extends net.minecraft.world.level.block.Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private ModItems() {
    }
}
