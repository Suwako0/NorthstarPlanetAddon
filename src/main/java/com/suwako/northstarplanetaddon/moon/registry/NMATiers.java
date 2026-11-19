package com.suwako.northstarplanetaddon.moon.registry;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/**
 * Tool material of 梦眠月斧 — a tier that sits just above netherite, and is repaired with 月鹿角.
 * <p>
 * Netherite is {@code 2031 uses / speed 9.0 / damage +4.0 / enchantability 15}; this is
 * {@code 2200 uses / speed 9.5 / damage +4.5}, and it keeps a gold axe's enchantability of 22 so the
 * reward for beating the boss enchants like gold while cutting like netherite.
 */
public enum NMATiers implements Tier {
    DREAMSLEEP_MOON(2200, 9.5F, 4.5F, 22);

    private final int uses;
    private final float speed;
    private final float attackDamageBonus;
    private final int enchantmentValue;

    NMATiers(int uses, float speed, float attackDamageBonus, int enchantmentValue) {
        this.uses = uses;
        this.speed = speed;
        this.attackDamageBonus = attackDamageBonus;
        this.enchantmentValue = enchantmentValue;
    }

    @Override
    public int getUses() {
        return this.uses;
    }

    @Override
    public float getSpeed() {
        return this.speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return this.attackDamageBonus;
    }

    /** Obsidian-class blocks are fair game, exactly like a netherite tool. */
    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
    }

    @Override
    public int getEnchantmentValue() {
        return this.enchantmentValue;
    }

    /** A fallen deer's antler is what the axe is made of, so it is also what mends it. */
    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(NMAItems.MOON_DEER_ANTLER.get());
    }
}
