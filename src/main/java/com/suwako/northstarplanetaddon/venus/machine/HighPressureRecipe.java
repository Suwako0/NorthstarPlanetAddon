package com.suwako.northstarplanetaddon.venus.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * 高压熔炼配方（设计草案第 8 章「高压熔炉：1 份/400t，需 8 bar + 1200°C」）。
 *
 * <p>无序多材料配方：机器把 2 个输入槽塞进 {@link CraftingInput}，
 * 配方只要「每个 ingredient 都能找到一件不重复的匹配原料」即可开工。
 * JSON 示例见 {@code data/phosphoros/recipe/acid_proof_alloy_plate.json}。</p>
 */
public record HighPressureRecipe(List<Ingredient> ingredients, ItemStack result,
                                 int bar, int temperature, int time) implements Recipe<CraftingInput> {

    public static final MapCodec<HighPressureRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").forGetter(HighPressureRecipe::ingredients),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(HighPressureRecipe::result),
            Codec.INT.optionalFieldOf("bar", 8).forGetter(HighPressureRecipe::bar),
            Codec.INT.optionalFieldOf("temperature", 1200).forGetter(HighPressureRecipe::temperature),
            Codec.INT.optionalFieldOf("time", 400).forGetter(HighPressureRecipe::time)
    ).apply(instance, HighPressureRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, HighPressureRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), HighPressureRecipe::ingredients,
            ItemStack.STREAM_CODEC, HighPressureRecipe::result,
            ByteBufCodecs.VAR_INT, HighPressureRecipe::bar,
            ByteBufCodecs.VAR_INT, HighPressureRecipe::temperature,
            ByteBufCodecs.VAR_INT, HighPressureRecipe::time,
            HighPressureRecipe::new);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        if (stacks.size() != this.ingredients.size()) {
            return false;
        }
        boolean[] used = new boolean[stacks.size()];
        for (Ingredient ingredient : this.ingredients) {
            boolean matched = false;
            for (int i = 0; i < stacks.size(); i++) {
                if (!used[i] && ingredient.test(stacks.get(i))) {
                    used[i] = true;
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= this.ingredients.size();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.copyOf(this.ingredients);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.HIGH_PRESSURE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.highPressureType();
    }

    @Override
    public boolean isSpecial() {
        return true; // 不在配方书里展示
    }

    /** 序列化器：1.20.5+ 需要同时提供 MapCodec（数据包）与 StreamCodec（网络同步）。 */
    public static final class Serializer implements RecipeSerializer<HighPressureRecipe> {

        @Override
        public MapCodec<HighPressureRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HighPressureRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
