package com.suwako.northstarplanetaddon.venus.item;

import com.suwako.northstarplanetaddon.venus.registry.ModItems;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * 高压钻的挖掘层级（设计草案 6.3 节：挖掘速度 10、耐久 2400）。
 */
public class PressureDrillTier implements Tier {

    public static final PressureDrillTier INSTANCE = new PressureDrillTier();

    @Override
    public int getUses() {
        return 2400;
    }

    @Override
    public float getSpeed() {
        return 10.0F;
    }

    @Override
    public float getAttackDamageBonus() {
        return 4.0F;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
    }

    @Override
    public int getEnchantmentValue() {
        return 12;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ModItems.TUNGSTEN_PLATE.get());
    }
}
