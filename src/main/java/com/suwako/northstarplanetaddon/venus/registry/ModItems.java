package com.suwako.northstarplanetaddon.venus.registry;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import com.suwako.northstarplanetaddon.venus.item.AcidicSpearItem;
import com.suwako.northstarplanetaddon.venus.item.BallisticCalibratorItem;
import com.suwako.northstarplanetaddon.venus.item.BossSummonItem;
import com.suwako.northstarplanetaddon.venus.item.BarometerItem;
import com.suwako.northstarplanetaddon.venus.item.CloudChartItem;
import com.suwako.northstarplanetaddon.venus.item.FungusShearsItem;
import com.suwako.northstarplanetaddon.venus.item.ModuleItem;
import com.suwako.northstarplanetaddon.venus.item.PressureDrillItem;
import com.suwako.northstarplanetaddon.venus.item.PressureDrillTier;
import com.suwako.northstarplanetaddon.venus.item.CloudHookItem;
import com.suwako.northstarplanetaddon.venus.item.SulfurFlamethrowerItem;
import com.suwako.northstarplanetaddon.venus.item.ThunderCrossbowItem;
import com.suwako.northstarplanetaddon.venus.item.PressureHammerItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 物品注册。
 *
 * <p>数值严格对应设计草案第 6、7 章；武器属性用
 * {@code 1 + 层级攻击加成 + 修饰符} 的方式对齐 DPS 表：</p>
 * <ul>
 *   <li>酸蚀矛：9 伤害 / 1.4 攻速（下界合金层级 +3 → 修饰符 6，攻速修饰符 -2.6）</li>
 *   <li>压力锤：13 伤害 / 0.75 攻速（修饰符 10，攻速修饰符 -3.25）</li>
 * </ul>
 */
public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(VenusModule.NAMESPACE);

    // ================================================================== 材料
    /** 钨锭：地表层基础金属。 */
    public static final DeferredItem<Item> TUNGSTEN_INGOT = simple("tungsten_ingot");
    /** 钨板：压力服与机器外壳。 */
    public static final DeferredItem<Item> TUNGSTEN_PLATE = simple("tungsten_plate");
    /** 耐酸合金板：高压熔炉 8 bar 产物（第 6.5 节）。 */
    public static final DeferredItem<Item> ACID_PROOF_ALLOY_PLATE = simple("acid_proof_alloy_plate");
    /** 前文明合金：云港遗迹 / 熔管城市的稀有材料。 */
    public static final DeferredItem<Item> PRE_CIVILIZATION_ALLOY =
            rare("pre_civilization_alloy", Rarity.UNCOMMON);
    /** 硫晶：硫之女王的领域材料。 */
    public static final DeferredItem<Item> SULFUR_CRYSTAL = simple("sulfur_crystal");
    /** 云帆膜：云行者护甲与云钩的原料。 */
    public static final DeferredItem<Item> CLOUD_SAIL_MEMBRANE = simple("cloud_sail_membrane");
    /** 气囊核心：云城科技的浮空单元。 */
    public static final DeferredItem<Item> AEROSTAT_CORE = rare("aerostat_core", Rarity.UNCOMMON);
    /** 鲸骨：涅斐勒掉落。 */
    public static final DeferredItem<Item> WHALE_BONE = simple("whale_bone");
    /** 雷羽：雷羽雏鸟掉落，可给雷羽弩充能。 */
    public static final DeferredItem<Item> THUNDER_FEATHER = simple("thunder_feather");
    /** 酸囊：酸潮水母 / 酸蚀矛的材料。 */
    public static final DeferredItem<Item> ACID_SAC = simple("acid_sac");
    /** 巨像核心碎片：赫菲斯托斯掉落。 */
    public static final DeferredItem<Item> CORE_SHARD = rare("core_shard", Rarity.UNCOMMON);
    /** 启明星核：终局材料（首杀阿佛洛狄忒 100%）。 */
    public static final DeferredItem<Item> PHOSPHOROS_CORE = rare("phosphoros_core", Rarity.EPIC);
    /** 阿佛洛狄忒之泪。 */
    public static final DeferredItem<Item> APHRODITE_TEAR = rare("aphrodite_tear", Rarity.RARE);
    /** 菌丝纤维：孢子行者掉落，菌剪/菌床体系的基础材料。 */
    public static final DeferredItem<Item> FUNGUS_FIBER = simple("fungus_fiber");
    /** 磷粉：菌伞蛾掉落的发光粉末。 */
    public static final DeferredItem<Item> PHOSPHOR_DUST = simple("phosphor_dust");
    /** 霜甲片：霜甲潜行者的甲壳碎片。 */
    public static final DeferredItem<Item> FROST_SCALE = simple("frost_scale");
    /** 静电囊：霜甲潜行者 / 雷暴带的储能器官。 */
    public static final DeferredItem<Item> STATIC_SAC = simple("static_sac");
    /** 熔管腺：熔管蠕虫的熔岩腺体。 */
    public static final DeferredItem<Item> LAVA_GLAND = simple("lava_gland");

    /** 云图残页：集齐 24 条解锁云城日志进度。 */
    public static final DeferredItem<Item> CLOUD_MAP_FRAGMENT = simple("cloud_map_fragment");

    // ================================================================== 武器
    /**
     * 酸蚀矛：9 伤害 / 1.4 攻速，命中叠腐蚀（-1 护甲/层，5 层，8s），对护甲板 ×1.2。
     */
    public static final DeferredItem<AcidicSpearItem> ACIDIC_SPEAR = ITEMS.register("acidic_spear",
            () -> new AcidicSpearItem(new Item.Properties()
                    .durability(650)
                    .rarity(Rarity.UNCOMMON)
                    .component(DataComponents.TOOL, SwordItem.createToolProperties())
                    .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 6, -2.6F))));

    /**
     * 压力锤：13 伤害 / 0.75 攻速，右键蓄力释放 3 格震荡波（+6 伤害，记入弱点过载）。
     */
    public static final DeferredItem<PressureHammerItem> PRESSURE_HAMMER = ITEMS.register("pressure_hammer",
            () -> new PressureHammerItem(new Item.Properties()
                    .durability(800)
                    .rarity(Rarity.RARE)
                    .component(DataComponents.TOOL, SwordItem.createToolProperties())
                    .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 10, -3.25F))));

    /** 雷羽弩：9 伤害 / 1.2s 蓄力 / 连锁 3 目标；静电囊可充能 10 发（×1.5）。 */
    public static final DeferredItem<ThunderCrossbowItem> THUNDER_CROSSBOW = ITEMS.register("thunder_crossbow",
            () -> new ThunderCrossbowItem(new Item.Properties()
                    .durability(500)
                    .rarity(Rarity.RARE)
                    .component(ModComponents.CHARGE, 0)));

    /** 云钩：2 伤害 / 400 耐久；右键钩住 24 格内的方块或生物拉近。 */
    public static final DeferredItem<CloudHookItem> CLOUD_HOOK = ITEMS.register("cloud_hook",
            () -> new CloudHookItem(new Item.Properties()
                    .durability(400)
                    .rarity(Rarity.UNCOMMON)
                    .attributes(SwordItem.createAttributes(Tiers.WOOD, 1, -3.0F))));

    /** 硫火喷射器：6 伤害/秒、点燃 8s；燃料 600 mB（= 6 秒喷射）。 */
    public static final DeferredItem<SulfurFlamethrowerItem> SULFUR_FLAMETHROWER =
            ITEMS.register("sulfur_flamethrower", () -> new SulfurFlamethrowerItem(new Item.Properties()
                    .durability(600)
                    .rarity(Rarity.RARE)));
    /** 弹道校准器（Curios 模块，缺失 Curios 时背包内生效）：弱点 +1 过载 / 护甲板 +15%。 */
    public static final DeferredItem<BallisticCalibratorItem> BALLISTIC_CALIBRATOR =
            ITEMS.register("ballistic_calibrator",
                    () -> new BallisticCalibratorItem(new Item.Properties()
                            .stacksTo(1)
                            .rarity(Rarity.RARE)));

    // ================================================================== 钨钢压力服
    public static final DeferredItem<ArmorItem> TUNGSTEN_HELMET = armor("tungsten_helmet",
            ArmorItem.Type.HELMET, 385);
    public static final DeferredItem<ArmorItem> TUNGSTEN_CHESTPLATE = armor("tungsten_chestplate",
            ArmorItem.Type.CHESTPLATE, 560);
    public static final DeferredItem<ArmorItem> TUNGSTEN_LEGGINGS = armor("tungsten_leggings",
            ArmorItem.Type.LEGGINGS, 525);
    public static final DeferredItem<ArmorItem> TUNGSTEN_BOOTS = armor("tungsten_boots",
            ArmorItem.Type.BOOTS, 455);

    // ================================================================== 云行者（T2 云海轻甲）
    public static final DeferredItem<ArmorItem> CLOUD_WALKER_HELMET = armorOf("cloud_walker_helmet",
            ModArmorMaterials.CLOUD_WALKER, ArmorItem.Type.HELMET, 275);
    public static final DeferredItem<ArmorItem> CLOUD_WALKER_CHESTPLATE = armorOf("cloud_walker_chestplate",
            ModArmorMaterials.CLOUD_WALKER, ArmorItem.Type.CHESTPLATE, 400);
    public static final DeferredItem<ArmorItem> CLOUD_WALKER_LEGGINGS = armorOf("cloud_walker_leggings",
            ModArmorMaterials.CLOUD_WALKER, ArmorItem.Type.LEGGINGS, 375);
    public static final DeferredItem<ArmorItem> CLOUD_WALKER_BOOTS = armorOf("cloud_walker_boots",
            ModArmorMaterials.CLOUD_WALKER, ArmorItem.Type.BOOTS, 325);

    // ================================================================== 熔管重甲（T4 地下）
    public static final DeferredItem<ArmorItem> TUBE_HEAVY_HELMET = armorOf("tube_heavy_helmet",
            ModArmorMaterials.TUBE_HEAVY, ArmorItem.Type.HELMET, 550);
    public static final DeferredItem<ArmorItem> TUBE_HEAVY_CHESTPLATE = armorOf("tube_heavy_chestplate",
            ModArmorMaterials.TUBE_HEAVY, ArmorItem.Type.CHESTPLATE, 800);
    public static final DeferredItem<ArmorItem> TUBE_HEAVY_LEGGINGS = armorOf("tube_heavy_leggings",
            ModArmorMaterials.TUBE_HEAVY, ArmorItem.Type.LEGGINGS, 750);
    public static final DeferredItem<ArmorItem> TUBE_HEAVY_BOOTS = armorOf("tube_heavy_boots",
            ModArmorMaterials.TUBE_HEAVY, ArmorItem.Type.BOOTS, 650);

    // ================================================================== 启明星战甲（T5 终局）
    public static final DeferredItem<ArmorItem> PHOSPHOROS_HELMET = armorOf("phosphoros_helmet",
            ModArmorMaterials.PHOSPHOROS, ArmorItem.Type.HELMET, 800);
    public static final DeferredItem<ArmorItem> PHOSPHOROS_CHESTPLATE = armorOf("phosphoros_chestplate",
            ModArmorMaterials.PHOSPHOROS, ArmorItem.Type.CHESTPLATE, 1100);
    public static final DeferredItem<ArmorItem> PHOSPHOROS_LEGGINGS = armorOf("phosphoros_leggings",
            ModArmorMaterials.PHOSPHOROS, ArmorItem.Type.LEGGINGS, 1050);
    public static final DeferredItem<ArmorItem> PHOSPHOROS_BOOTS = armorOf("phosphoros_boots",
            ModArmorMaterials.PHOSPHOROS, ArmorItem.Type.BOOTS, 900);
    // ================================================================== 食物
    /** 烤巨菌排：8 营养 / 12.8 饱和，抗酸（抗性提升 I）5 分钟。 */
    public static final DeferredItem<Item> ROASTED_GIANT_FUNGUS = ITEMS.register("roasted_giant_fungus",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(8)
                    .saturationModifier(0.8F)
                    .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6000, 0), 1.0F)
                    .build())));

    /** 云藻冻：5 营养 / 6.0 饱和，缓降 30s。 */
    public static final DeferredItem<Item> CLOUD_ALGAE_JELLY = ITEMS.register("cloud_algae_jelly",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(5)
                    .saturationModifier(0.6F)
                    .effect(() -> new MobEffectInstance(MobEffects.SLOW_FALLING, 600, 0), 1.0F)
                    .build())));

    /** 孢子糖浆：3 营养 / 1.5 饱和，跳跃提升 I 90s。 */
    public static final DeferredItem<Item> SPORE_SYRUP = ITEMS.register("spore_syrup",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(3)
                    .saturationModifier(0.25F)
                    .effect(() -> new MobEffectInstance(MobEffects.JUMP, 1800, 0), 1.0F)
                    .build())));


    // ================================================================== 工具（第 6.3 节）
    /** 高压钻：挖掘速度 10 / 耐久 2400；右键 3×3 挖掘（耐久 ×9）。 */
    public static final DeferredItem<PressureDrillItem> PRESSURE_DRILL = ITEMS.register("pressure_drill",
            () -> new PressureDrillItem(new Item.Properties()
                    .durability(2400)
                    .rarity(Rarity.RARE)
                    .component(DataComponents.TOOL, PressureDrillTier.INSTANCE.createToolProperties(
                            net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE))
                    .attributes(PickaxeItem.createAttributes(PressureDrillTier.INSTANCE, 3.0F, -2.8F))));

    /** 菌剪：采集菌类不损耐久；右键移植菌类方块。 */
    public static final DeferredItem<FungusShearsItem> FUNGUS_SHEARS = ITEMS.register("fungus_shears",
            () -> new FungusShearsItem(new Item.Properties().durability(250)));

    /** 云图仪：扫描 64 格内遗迹与箱子；集齐 12 条云图残页后显示精确坐标。 */
    public static final DeferredItem<CloudChartItem> CLOUD_CHART = ITEMS.register("cloud_chart",
            () -> new CloudChartItem(new Item.Properties().durability(200).rarity(Rarity.UNCOMMON)));

    /** 气压计：读数式 —— 附近最高气压 / 环境温度 / 腐蚀值 / 季风倒计时。 */
    public static final DeferredItem<BarometerItem> BAROMETER = ITEMS.register("barometer",
            () -> new BarometerItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    // ================================================================== Boss 召唤物（4.2–4.4 节）
    /** 鲸骨号角：召唤涅斐勒（云港遗迹顶层）。 */
    public static final DeferredItem<BossSummonItem> WHALE_BONE_HORN = ITEMS.register("whale_bone_horn",
            () -> new BossSummonItem(new Item.Properties().rarity(Rarity.RARE), ModEntities.NEPHELE::get));

    /** 压力阀：召唤赫菲斯托斯（高压观测塔底层，需注入 8 bar）。 */
    public static final DeferredItem<BossSummonItem> PRESSURE_VALVE = ITEMS.register("pressure_valve",
            () -> new BossSummonItem(new Item.Properties().rarity(Rarity.RARE), ModEntities.HEPHAESTUS::get));

    /** 硫心：放置后召唤阿佛洛狄忒（熔管城市核心层）。 */
    public static final DeferredItem<BossSummonItem> SULFUR_HEART = ITEMS.register("sulfur_heart",
            () -> new BossSummonItem(new Item.Properties().rarity(Rarity.EPIC), ModEntities.APHRODITE::get));

    // ================================================================== Curios modules (6.4)
    /** Acid coating: corrosion accumulation -60%. */
    public static final DeferredItem<ModuleItem> ACID_COATING = ITEMS.register("acid_coating",
            () -> new ModuleItem(new Item.Properties().rarity(Rarity.UNCOMMON),
                    "tooltip.northstar_planet_addon.acid_coating"));
    /** Pressure compensator: +20 atm pressure rating. */
    public static final DeferredItem<ModuleItem> PRESSURE_COMPENSATOR = ITEMS.register("pressure_compensator",
            () -> new ModuleItem(new Item.Properties().rarity(Rarity.UNCOMMON),
                    "tooltip.northstar_planet_addon.pressure_compensator"));
    /** Heat sink: +1 heat tier (fire damage -25%). */
    public static final DeferredItem<ModuleItem> HEAT_SINK = ITEMS.register("heat_sink",
            () -> new ModuleItem(new Item.Properties().rarity(Rarity.UNCOMMON),
                    "tooltip.northstar_planet_addon.heat_sink"));
    /** Air bag: slow falling while airborne. */
    public static final DeferredItem<ModuleItem> AIR_BAG = ITEMS.register("air_bag",
            () -> new ModuleItem(new Item.Properties().rarity(Rarity.UNCOMMON),
                    "tooltip.northstar_planet_addon.air_bag"));
    /** Filter: immune to spore blindness. */
    public static final DeferredItem<ModuleItem> FILTER = ITEMS.register("filter",
            () -> new ModuleItem(new Item.Properties().rarity(Rarity.UNCOMMON),
                    "tooltip.northstar_planet_addon.filter"));
    // ================================================================== Agriculture (chapter 7.3)
    /** Giant fungus spore: plants the giant fungus crop. */
    public static final DeferredItem<ItemNameBlockItem> GIANT_FUNGUS_SPORE = ITEMS.register("giant_fungus_spore",
            () -> new ItemNameBlockItem(ModBlocks.GIANT_FUNGUS_CROP.get(), new Item.Properties()));
    /** Acid vegetable seeds. */
    public static final DeferredItem<ItemNameBlockItem> ACID_VEGETABLE_SEEDS = ITEMS.register("acid_vegetable_seeds",
            () -> new ItemNameBlockItem(ModBlocks.ACID_VEGETABLE_CROP.get(), new Item.Properties()));
    /** Cloud algae spore. */
    public static final DeferredItem<ItemNameBlockItem> CLOUD_ALGAE_SPORE = ITEMS.register("cloud_algae_spore",
            () -> new ItemNameBlockItem(ModBlocks.CLOUD_ALGAE_CROP.get(), new Item.Properties()));
    // ================================================================== Food (chapter 7.2)
    /** Acid-pickled vegetable: 4 nutrition / 4.8 saturation / resistance II 5 min. */
    public static final DeferredItem<Item> ACID_VEGETABLE = ITEMS.register("acid_vegetable",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(4)
                    .saturationModifier(0.6F)
                    .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6000, 1), 1.0F)
                    .build())));

    /** Frostberry smoothie: 3 nutrition / 2.0 saturation / fire resistance 5 min. */
    public static final DeferredItem<Item> FROSTBERRY_SMOOTHIE = ITEMS.register("frostberry_smoothie",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(3)
                    .saturationModifier(0.333F)
                    .effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0), 1.0F)
                    .build())));

    /** Thunder honey drink: 2 nutrition / 1.0 saturation / speed II 90s. */
    public static final DeferredItem<Item> THUNDER_HONEY_DRINK = ITEMS.register("thunder_honey_drink",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(2)
                    .saturationModifier(0.25F)
                    .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1800, 1), 1.0F)
                    .build())));

    /** Pressure stew: 12 nutrition / 19.2 saturation / strength I + resistance I 3 min. */
    public static final DeferredItem<Item> PRESSURE_STEW = ITEMS.register("pressure_stew",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(12)
                    .saturationModifier(0.8F)
                    .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3600, 0), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 3600, 0), 1.0F)
                    .build())));

    /** VenusModule feast: 14 nutrition / 22.4 saturation / all-round +1 for 5 min. */
    public static final DeferredItem<Item> PHOSPHOROS_FEAST = ITEMS.register("phosphoros_feast",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(14)
                    .saturationModifier(0.8F)
                    .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 6000, 0), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6000, 0), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 6000, 0), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 6000, 0), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.ABSORPTION, 6000, 0), 1.0F)
                    .build())));
    // ================================================================== 刷怪蛋
    public static final DeferredItem<SpawnEggItem> NEPHELE_SPAWN_EGG = ITEMS.register("nephele_spawn_egg",
            () -> new SpawnEggItem(ModEntities.NEPHELE.get(), 0xE8D9A0, 0x2F4F6B, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> HEPHAESTUS_SPAWN_EGG = ITEMS.register("hephaestus_spawn_egg",
            () -> new SpawnEggItem(ModEntities.HEPHAESTUS.get(), 0x5A2A1E, 0xE8A33B, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> APHRODITE_SPAWN_EGG = ITEMS.register("aphrodite_spawn_egg",
            () -> new SpawnEggItem(ModEntities.APHRODITE.get(), 0x4A2A4E, 0xC8D93B, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> CLOUD_RAY_SPAWN_EGG = ITEMS.register("cloud_ray_spawn_egg",
            () -> new SpawnEggItem(ModEntities.CLOUD_RAY.get(), 0xC9E8F2, 0x5A8CAA, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> ACID_JELLY_SPAWN_EGG = ITEMS.register("acid_jelly_spawn_egg",
            () -> new SpawnEggItem(ModEntities.ACID_JELLY.get(), 0x8BE04A, 0x2E5B1E, new Item.Properties()));

    // ================================================================== 方块物品
    public static final DeferredItem<BlockItem> TUNGSTEN_BLOCK = blockItem("tungsten_block", ModBlocks.TUNGSTEN_BLOCK);
    public static final DeferredItem<BlockItem> SULFUR_CRYSTAL_BLOCK =
            blockItem("sulfur_crystal_block", ModBlocks.SULFUR_CRYSTAL_BLOCK);
    public static final DeferredItem<BlockItem> CLOUD_STONE = blockItem("cloud_stone", ModBlocks.CLOUD_STONE);


    // ================================================================== 辅助
    private static DeferredItem<Item> simple(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static DeferredItem<Item> rare(String name, Rarity rarity) {
        return ITEMS.register(name, () -> new Item(new Item.Properties().rarity(rarity)));
    }

    private static DeferredItem<ArmorItem> armor(String name, ArmorItem.Type type, int durability) {
        return ITEMS.register(name, () -> new ArmorItem(ModArmorMaterials.TUNGSTEN, type,
                new Item.Properties().durability(durability).rarity(Rarity.UNCOMMON)));
    }

    private static DeferredItem<BlockItem> blockItem(String name, DeferredBlock<? extends net.minecraft.world.level.block.Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static DeferredItem<ArmorItem> armorOf(String name,
                                                   net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.item.ArmorMaterial, net.minecraft.world.item.ArmorMaterial> material,
                                                   ArmorItem.Type type, int durability) {
        return ITEMS.register(name, () -> new ArmorItem(material, type,
                new Item.Properties().durability(durability).rarity(Rarity.UNCOMMON)));
    }

    private ModItems() {
    }
}
