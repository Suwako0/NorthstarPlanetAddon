package com.suwako.northstarplanetaddon.venus.registry;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

/**
 * 护甲材质（设计草案 6.1 节，4 套）。
 *
 * <table>
 *   <tr><th>套装</th><th>护甲</th><th>韧性/件</th><th>耐久（头/胸/腿/靴）</th><th>贴图层</th></tr>
 *   <tr><td>钨钢压力服</td><td>4/8/6/3</td><td>2.0</td><td>385/560/525/455</td><td>原版下界合金</td></tr>
 *   <tr><td>云行者</td><td>3/6/5/2</td><td>1.0</td><td>275/400/375/325</td><td>原版钻石</td></tr>
 *   <tr><td>熔管重甲</td><td>5/9/7/4</td><td>3.0</td><td>550/800/750/650</td><td>原版金</td></tr>
 *   <tr><td>启明星战甲</td><td>5/10/8/4</td><td>4.0</td><td>800/1100/1050/900</td><td>原版下界合金</td></tr>
 * </table>
 *
 * <p>贴图沿用原版护甲层（美术资源到位后改成 {@code northstar_planet_addon:<套装名>} 即可。</p>
 */
public final class ModArmorMaterials {

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, VenusModule.NAMESPACE);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> TUNGSTEN =
            ARMOR_MATERIALS.register("tungsten", () -> new ArmorMaterial(
                    defense(4, 8, 6, 3),
                    12,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(ModTags.Items.TUNGSTEN_REPAIR),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.parse("northstar_planet_addon:tungsten"))),
                    2.0F,
                    0.0F));

    /** 云行者：轻甲，韧性低、无移速惩罚。 */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> CLOUD_WALKER =
            ARMOR_MATERIALS.register("cloud_walker", () -> new ArmorMaterial(
                    defense(3, 6, 5, 2),
                    14,
                    SoundEvents.ARMOR_EQUIP_LEATHER,
                    () -> Ingredient.of(ModItems.CLOUD_SAIL_MEMBRANE.get()),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.parse("northstar_planet_addon:cloud_walker"))),
                    1.0F,
                    0.0F));

    /** 熔管重甲：重甲，击退抗性 0.2、韧性 3/件。 */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> TUBE_HEAVY =
            ARMOR_MATERIALS.register("tube_heavy", () -> new ArmorMaterial(
                    defense(5, 9, 7, 4),
                    10,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(ModItems.ACID_PROOF_ALLOY_PLATE.get()),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.parse("northstar_planet_addon:tube_heavy"))),
                    3.0F,
                    0.2F));

    /** 启明星战甲：终局，韧性 4/件、全套 16。 */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> PHOSPHOROS =
            ARMOR_MATERIALS.register("northstar_planet_addon", () -> new ArmorMaterial(
                    defense(5, 10, 8, 4),
                    18,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(ModItems.PRE_CIVILIZATION_ALLOY.get()),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.parse("northstar_planet_addon:phosphoros"))),
                    4.0F,
                    0.1F));

    private static EnumMap<ArmorItem.Type, Integer> defense(int helmet, int chestplate, int leggings, int boots) {
        EnumMap<ArmorItem.Type, Integer> map = new EnumMap<>(ArmorItem.Type.class);
        map.put(ArmorItem.Type.HELMET, helmet);
        map.put(ArmorItem.Type.CHESTPLATE, chestplate);
        map.put(ArmorItem.Type.LEGGINGS, leggings);
        map.put(ArmorItem.Type.BOOTS, boots);
        map.put(ArmorItem.Type.BODY, 11);
        return map;
    }

    private ModArmorMaterials() {
    }
}
