package com.suwako.northstarplanetaddon.mercury.registry;

import com.suwako.northstarplanetaddon.mercury.MercuryModule;
import com.suwako.northstarplanetaddon.mercury.world.SunbathManager;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Every item added by the mod, including the block items of {@link MNABlocks}. */
public final class MNAItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MercuryModule.NAMESPACE);

    /**
     * 太阳鱼片与原版生鱼 / 生鲑鱼（{@code Foods.COD} / {@code Foods.SALMON}）完全一致：
     * 回复 2 点饥饿值，饱和度系数 0.1。
     */
    private static final FoodProperties SUNFISH_SLICE_FOOD = new FoodProperties.Builder()
            .nutrition(2)
            .saturationModifier(0.1F)
            .build();

    /**
     * 太阳鱼果烩：回复 5 点饥饿值（2.5 个鸡腿），饱和度加成 0.6，食用后获得 2 分钟沐浴阳光。
     * <p>
     * 设为 {@code alwaysEdible}，让饱食时也能吃下它来取得效果。效果实例走 supplier，
     * 每次食用都新建一个（{@code MobEffectInstance} 的时长是会递减的可变对象），
     * 同时也避免物品注册时就已经去解析尚未绑定的效果 Holder。
     */
    private static final FoodProperties SUNFISH_JAM_FOOD = new FoodProperties.Builder()
            .nutrition(5)
            .saturationModifier(0.6F)
            .alwaysEdible()
            .effect(() -> SunbathManager.foodEffect(), 1.0F)
            .build();

    /** 水星沙的物品形态；创造模式物品栏里的图标也是它。 */
    public static final DeferredItem<BlockItem> MERCURY_SAND =
            ITEMS.registerSimpleBlockItem("mercury_sand", MNABlocks.MERCURY_SAND);

    /** 太阳鱼片：可食用，击杀太阳鱼 / 炽烈太阳鱼的掉落物，也是太阳鱼果烩的原料。 */
    public static final DeferredItem<Item> SUNFISH_SLICE = ITEMS.registerItem(
            "sunfish_slice",
            properties -> new Item(properties.food(SUNFISH_SLICE_FOOD)),
            new Item.Properties());

    /** 太阳鱼果烩：1 个太阳鱼片 + 2 个发光浆果合成的食物，食用后获得 2 分钟沐浴阳光。 */
    public static final DeferredItem<Item> SUNFISH_JAM = ITEMS.registerItem(
            "sunfish_jam",
            properties -> new Item(properties.food(SUNFISH_JAM_FOOD)),
            new Item.Properties());

    /** 太阳鱼刷怪蛋。 */
    public static final DeferredItem<DeferredSpawnEggItem> SUNFISH_SPAWN_EGG = ITEMS.registerItem(
            "sunfish_spawn_egg",
            properties -> new DeferredSpawnEggItem(MNAEntityTypes.SUNFISH, 0xFFC24A, 0xFFF3C4, properties),
            new Item.Properties());

    /** 炽烈太阳鱼刷怪蛋。 */
    public static final DeferredItem<DeferredSpawnEggItem> BLAZING_SUNFISH_SPAWN_EGG = ITEMS.registerItem(
            "blazing_sunfish_spawn_egg",
            properties -> new DeferredSpawnEggItem(MNAEntityTypes.BLAZING_SUNFISH, 0xB32D0C, 0xFFE066, properties),
            new Item.Properties());

    private MNAItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
