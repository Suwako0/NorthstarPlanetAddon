package com.possible_triangle.create_jetpack

import com.mojang.serialization.MapCodec
import com.simibubi.create.content.equipment.armor.BacktankItem
import com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe
import kotlin.jvm.functions.Function1
import kotlin.jvm.internal.SourceDebugExtension
import net.minecraft.core.HolderLookup.Provider
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.ShapedRecipePattern

@SourceDebugExtension(["SMAP\nCopyComponentsMechanicalCraftingRecipe.kt\nKotlin\n*S Kotlin\n*F\n+ 1 CopyComponentsMechanicalCraftingRecipe.kt\ncom/possible_triangle/create_jetpack/CopyComponentsMechanicalCraftingRecipe\n+ 2 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,76:1\n1#2:77\n*E\n"])
public class CopyComponentsMechanicalCraftingRecipe(group: String,
   category: CraftingBookCategory,
   pattern: ShapedRecipePattern,
   recipeOutput: ItemStack,
   acceptMirrored: Boolean
) : MechanicalCraftingRecipe(group, category, pattern, recipeOutput, acceptMirrored) {
   public open fun getSerializer(): RecipeSerializer<*> {
      val var10000: Any = Content.INSTANCE.getCOPY_NBT_MECHANICAL_CRAFTING_SERIALIZER().get();
      return var10000 as RecipeSerializer<?>;
   }

   private fun findUpgradable(container: CraftingInput): ItemStack? {
      val width: Int = Math.min(container.width(), this.getWidth());

      for (int x = 0; x < width; x++) {
         var y: Int = 0;

         for (int var5 = Math.min(container.height(), this.getHeight()); y < var5; y++) {
            val stack: ItemStack = container.getItem(x + y * width);
            if (stack.getItem() is BacktankItem) {
               return stack;
            }
         }
      }

      return null;
   }

   public open fun assemble(container: CraftingInput, registries: Provider): ItemStack {
      val upgradeTarget: ItemStack = this.findUpgradable(container);
      val upgraded: ItemStack = super.assemble(container, registries);
      if (upgradeTarget != null) {
         upgraded.applyComponents(upgradeTarget.getComponents());
      }

      return upgraded;
   }

   public object Serializer : com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe.Serializer {
      private final val CODEC: MapCodec<MechanicalCraftingRecipe>
      private final val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, MechanicalCraftingRecipe>

      private fun from(recipe: MechanicalCraftingRecipe): MechanicalCraftingRecipe {
         val var10002: java.lang.String = recipe.getGroup();
         val var10003: CraftingBookCategory = recipe.category();
         val var10004: ShapedRecipePattern = recipe.pattern;
         val var10005: ItemStack = recipe.getResultItem(null);
         return new CopyComponentsMechanicalCraftingRecipe(var10002, var10003, var10004, var10005, recipe.acceptsMirrored());
      }

      private fun to(recipe: MechanicalCraftingRecipe): MechanicalCraftingRecipe {
         return new MechanicalCraftingRecipe(recipe.getGroup(), recipe.category(), recipe.pattern, recipe.getResultItem(null), recipe.acceptsMirrored());
      }

      public open fun codec(): MapCodec<MechanicalCraftingRecipe> {
         return CODEC;
      }

      @JvmStatic
      fun `CODEC$lambda$0`(`$tmp0`: Function1, p0: Any): MechanicalCraftingRecipe {
         return `$tmp0`.invoke(p0) as MechanicalCraftingRecipe;
      }

      @JvmStatic
      fun `CODEC$lambda$1`(`$tmp0`: Function1, p0: Any): MechanicalCraftingRecipe {
         return `$tmp0`.invoke(p0) as MechanicalCraftingRecipe;
      }

      @JvmStatic
      fun `STREAM_CODEC$lambda$0`(`$tmp0`: Function1, p0: Any): MechanicalCraftingRecipe {
         return `$tmp0`.invoke(p0) as MechanicalCraftingRecipe;
      }

      @JvmStatic
      fun `STREAM_CODEC$lambda$1`(`$tmp0`: Function1, p0: Any): MechanicalCraftingRecipe {
         return `$tmp0`.invoke(p0) as MechanicalCraftingRecipe;
      }

      @JvmStatic
      fun {
         val var10000: MapCodec = com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe.Serializer.CODEC
            .xmap(CopyComponentsMechanicalCraftingRecipe.Serializer::CODEC$lambda$0, CopyComponentsMechanicalCraftingRecipe.Serializer::CODEC$lambda$1);
         CODEC = var10000;
         val var0: StreamCodec = com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe.Serializer.STREAM_CODEC
            .map(
               CopyComponentsMechanicalCraftingRecipe.Serializer::STREAM_CODEC$lambda$0,
               CopyComponentsMechanicalCraftingRecipe.Serializer::STREAM_CODEC$lambda$1
            );
         STREAM_CODEC = var0;
      }
   }
}
