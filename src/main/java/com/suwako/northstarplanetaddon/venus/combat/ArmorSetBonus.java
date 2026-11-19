package com.suwako.northstarplanetaddon.venus.combat;

import com.suwako.northstarplanetaddon.venus.registry.ModArmorMaterials;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * 套装判定（设计草案 6.1 节）。
 *
 * <p>四个套装共用一套「数件数」逻辑，套装效果分别在 {@code CombatEvents}
 * （抗性类）与 {@code PlayerEvents}（被动类）里实现。</p>
 */
public final class ArmorSetBonus {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    /** 某个护甲材质的套装件数（0–4）。 */
    public static int countPieces(LivingEntity entity, DeferredHolder<ArmorMaterial, ArmorMaterial> material) {
        int count = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.getItem() instanceof ArmorItem armor
                    && armor.getMaterial().value() == material.get()) {
                count++;
            }
        }
        return count;
    }

    /** 钨钢压力服（T2）。 */
    public static int tungsten(LivingEntity entity) {
        return countPieces(entity, ModArmorMaterials.TUNGSTEN);
    }

    /** 云行者（T2 云海轻甲）。 */
    public static int cloudWalker(LivingEntity entity) {
        return countPieces(entity, ModArmorMaterials.CLOUD_WALKER);
    }

    /** 熔管重甲（T4 地下）。 */
    public static int tubeHeavy(LivingEntity entity) {
        return countPieces(entity, ModArmorMaterials.TUBE_HEAVY);
    }

    /** 启明星战甲（T5 终局）。 */
    public static int phosphoros(LivingEntity entity) {
        return countPieces(entity, ModArmorMaterials.PHOSPHOROS);
    }

    /** Pressure rating in atm (design 6.1 / 12.4): suit + pressure compensator module. */
    public static int pressureRating(net.minecraft.world.entity.LivingEntity entity) {
        int rating = 0;
        int tungsten = tungsten(entity);
        if (tungsten >= 2) {
            rating = 100;
        }
        if (tubeHeavy(entity) >= 2) {
            rating = Math.max(rating, 120);
        }
        if (phosphoros(entity) >= 2) {
            rating = Math.max(rating, 130);
        }
        if (com.suwako.northstarplanetaddon.venus.item.ModuleItem.isCarrying(entity,
                com.suwako.northstarplanetaddon.venus.registry.ModItems.PRESSURE_COMPENSATOR.get())) {
            rating += 20;
        }
        return rating;
    }
    private ArmorSetBonus() {
    }
}
