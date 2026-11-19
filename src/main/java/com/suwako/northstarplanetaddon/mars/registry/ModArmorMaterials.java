package com.suwako.northstarplanetaddon.mars.registry;

import com.suwako.northstarplanetaddon.mars.MarsModule;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;

/**
 * 本模组的护甲材质。
 *
 * <p>1.21.1 的 {@code ArmorMaterial} 是**记录**（不再有枚举），必须注册进
 * {@code Registries.ARMOR_MATERIAL}；{@code ArmorItem} 收的是 {@code Holder<ArmorMaterial>}，
 * {@link DeferredHolder} 就是 {@code Holder}，直接传进去即可。</p>
 *
 * <p>签名核对自 1.21.1 反编译源码 {@code ArmorMaterial.java}：</p>
 * <pre>
 * record ArmorMaterial(Map&lt;ArmorItem.Type, Integer&gt; defense, int enchantmentValue,
 *                      Holder&lt;SoundEvent&gt; equipSound, Supplier&lt;Ingredient&gt; repairIngredient,
 *                      List&lt;ArmorMaterial.Layer&gt; layers, float toughness, float knockbackResistance)
 * </pre>
 */
public final class ModArmorMaterials {

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, MarsModule.NAMESPACE);

    /**
     * 熔核护甲：全部数值为**下界合金套 ×1.5**（护甲值向上取整，因为字段是 int）。
     *
     * <table>
     *   <tr><th>部位</th><th>下界合金</th><th>熔核（×1.5）</th></tr>
     *   <tr><td>头盔</td><td>3</td><td>5</td></tr>
     *   <tr><td>胸甲</td><td>8</td><td>12</td></tr>
     *   <tr><td>护腿</td><td>6</td><td>9</td></tr>
     *   <tr><td>靴子</td><td>3</td><td>5</td></tr>
     *   <tr><td>全套 BODY</td><td>11</td><td>17</td></tr>
     *   <tr><td>韧性 / 击退抗性 / 附魔值</td><td>3.0 / 0.1 / 15</td><td>4.5 / 0.15 / 15</td></tr>
     * </table>
     *
     * <p>贴图层是 {@code textures/models/armor/molten_core_layer_1.png}（外层）与
     * {@code _layer_2.png}（内层 / 护腿），由 {@code ArmorMaterial.Layer} 按资源名前缀推导。</p>
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> MOLTEN_CORE =
            ARMOR_MATERIALS.register("molten_core", () -> new ArmorMaterial(
                    defense(5, 12, 9, 5, 17),
                    15,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(ModTags.Items.MOLTEN_CORE_REPAIR),
                    java.util.List.of(new ArmorMaterial.Layer(MarsModule.id("molten_core"))),
                    4.5F,
                    0.15F));

    private static EnumMap<ArmorItem.Type, Integer> defense(int helmet, int chestplate, int leggings,
                                                            int boots, int body) {
        EnumMap<ArmorItem.Type, Integer> map = new EnumMap<>(ArmorItem.Type.class);
        map.put(ArmorItem.Type.HELMET, helmet);
        map.put(ArmorItem.Type.CHESTPLATE, chestplate);
        map.put(ArmorItem.Type.LEGGINGS, leggings);
        map.put(ArmorItem.Type.BOOTS, boots);
        map.put(ArmorItem.Type.BODY, body);
        return map;
    }

    private ModArmorMaterials() {
    }
}
