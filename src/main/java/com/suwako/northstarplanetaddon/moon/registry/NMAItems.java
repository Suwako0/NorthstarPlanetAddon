package com.suwako.northstarplanetaddon.moon.registry;

import com.suwako.northstarplanetaddon.moon.MoonModule;
import com.suwako.northstarplanetaddon.moon.item.MooncakeItem;
import com.suwako.northstarplanetaddon.moon.item.MoonPearlItem;
import com.suwako.northstarplanetaddon.moon.item.MoonSpiritElixirItem;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Every item added by the mod, including the block items of {@link NMABlocks}. */
public final class NMAItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MoonModule.NAMESPACE);

    /** 月壳 — dropped by the Moon Shell Crab, grinds down into bone meal. */
    public static final DeferredItem<Item> MOON_SHELL = ITEMS.registerSimpleItem("moon_shell",
            new Item.Properties().rarity(Rarity.COMMON));

    /** A pumpkin-pie-strength mooncake traded by a Moon Rabbit for a golden carrot. */
    public static final DeferredItem<MooncakeItem> MOONCAKE = ITEMS.registerItem("mooncake",
            MooncakeItem::new, new Item.Properties().food(Foods.PUMPKIN_PIE));

    /**
     * 月灵丹药 —— 右键进食、消耗 1 个，永久 +2 点生命上限，每个玩家只生效一次。
     * 暂无配方（由 {@code MoonSpiritElixirItem} 自己实现进食与属性逻辑）。
     */
    public static final DeferredItem<MoonSpiritElixirItem> MOON_SPIRIT_ELIXIR = ITEMS.registerItem(
            "moon_spirit_elixir",
            MoonSpiritElixirItem::new,
            new Item.Properties().rarity(Rarity.RARE).stacksTo(16));

    /** A Moon-only altar key that becomes a broken pearl after placing a Dreamsleep Altar. */
    public static final DeferredItem<MoonPearlItem> MOON_PEARL = ITEMS.registerItem("moon_pearl",
            MoonPearlItem::new, new Item.Properties().rarity(Rarity.UNCOMMON));

    /** Spent moon pearl; surround it with Moon Shells to restore a usable Moon Pearl. */
    public static final DeferredItem<Item> BROKEN_MOON_PEARL = ITEMS.registerSimpleItem("broken_moon_pearl",
            new Item.Properties().rarity(Rarity.COMMON));

    /** Trophy dropped by the Dreamsleep Moon Deer boss. */
    public static final DeferredItem<Item> MOON_DEER_ANTLER = ITEMS.registerSimpleItem("moon_deer_antler",
            new Item.Properties().rarity(Rarity.RARE));

    public static final DeferredItem<DeferredSpawnEggItem> MOON_RABBIT_SPAWN_EGG = ITEMS.registerItem(
            "moon_rabbit_spawn_egg",
            properties -> new DeferredSpawnEggItem(NMAEntityTypes.MOON_RABBIT, 0xC9C4D8, 0xEBD8FF, properties),
            new Item.Properties());

    public static final DeferredItem<DeferredSpawnEggItem> MOON_SHELL_CRAB_SPAWN_EGG = ITEMS.registerItem(
            "moon_shell_crab_spawn_egg",
            properties -> new DeferredSpawnEggItem(NMAEntityTypes.MOON_SHELL_CRAB, 0x6E7A9E, 0xA9E4FF, properties),
            new Item.Properties());

    /** Creative/admin summoning item for the Dreamsleep Moon Deer Boss. */
    public static final DeferredItem<DeferredSpawnEggItem> LUNAR_DEER_SPAWN_EGG = ITEMS.registerItem(
            "lunar_deer_spawn_egg",
            properties -> new DeferredSpawnEggItem(NMAEntityTypes.MOON_DEER, 0x18243E, 0xD5F6FF, properties),
            new Item.Properties().rarity(Rarity.EPIC));

    /** Carries 温泉 around; behaves exactly like a water bucket, including infinite-source tricks. */
    public static final DeferredItem<net.minecraft.world.item.BucketItem> HOT_SPRING_BUCKET = ITEMS.registerItem(
            "hot_spring_bucket",
            properties -> new net.minecraft.world.item.BucketItem(NMAFluids.HOT_SPRING.get(), properties),
            new Item.Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1));

    // Block items. Registered here (after the blocks) so the item registry always sees them.
    public static final DeferredItem<net.minecraft.world.item.BlockItem> DREAMSLEEP_GRASS =
            ITEMS.registerSimpleBlockItem("dreamsleep_grass", NMABlocks.DREAMSLEEP_GRASS);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> MOON_SOIL =
            ITEMS.registerSimpleBlockItem("moon_soil", NMABlocks.MOON_SOIL);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> MOONLIGHT_LOG =
            ITEMS.registerSimpleBlockItem("moonlight_log", NMABlocks.MOONLIGHT_LOG);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> MOONLIGHT_WOOD =
            ITEMS.registerSimpleBlockItem("moonlight_wood", NMABlocks.MOONLIGHT_WOOD);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> STRIPPED_MOONLIGHT_LOG =
            ITEMS.registerSimpleBlockItem("stripped_moonlight_log", NMABlocks.STRIPPED_MOONLIGHT_LOG);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> STRIPPED_MOONLIGHT_WOOD =
            ITEMS.registerSimpleBlockItem("stripped_moonlight_wood", NMABlocks.STRIPPED_MOONLIGHT_WOOD);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> MOONLIGHT_PLANKS =
            ITEMS.registerSimpleBlockItem("moonlight_planks", NMABlocks.MOONLIGHT_PLANKS);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> MOONLIGHT_LEAVES =
            ITEMS.registerSimpleBlockItem("moonlight_leaves", NMABlocks.MOONLIGHT_LEAVES);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> MOONLIGHT_SAPLING =
            ITEMS.registerSimpleBlockItem("moonlight_sapling", NMABlocks.MOONLIGHT_SAPLING);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> MOON_BLOOM =
            ITEMS.registerSimpleBlockItem("moon_bloom", NMABlocks.MOON_BLOOM);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> DREAMSLEEP_ALTAR =
            ITEMS.registerSimpleBlockItem("dreamsleep_altar", NMABlocks.DREAMSLEEP_ALTAR);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> MOON_SPIRIT_ALTAR =
            ITEMS.registerSimpleBlockItem("moon_spirit_altar", NMABlocks.MOON_SPIRIT_ALTAR);

    /**
     * 梦眠月斧 — forged from a defeated Moon Deer's antler. 10.5 attack damage (1 player base + 4.5
     * tier + 5.0 weapon) and 1.1 attack speed (4.0 base - 2.9), so a jump critical lands for about 16.
     * 2200 durability, 9.5 mining speed, and gold's enchantability of 22 come from {@link NMATiers}.
     */
    public static final DeferredItem<com.suwako.northstarplanetaddon.moon.item.MoonDeerAxeItem> MOON_DEER_AXE =
            ITEMS.registerItem("moon_deer_axe",
                    properties -> new com.suwako.northstarplanetaddon.moon.item.MoonDeerAxeItem(NMATiers.DREAMSLEEP_MOON, properties),
                    new Item.Properties()
                            .rarity(Rarity.RARE)
                            .attributes(net.minecraft.world.item.DiggerItem.createAttributes(
                                    NMATiers.DREAMSLEEP_MOON, 5.0F, -2.9F)));

    private NMAItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
