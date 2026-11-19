package com.suwako.northstarplanetaddon.venus.machine;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 配方类型与序列化器注册。
 *
 * <p>两者都走 {@link DeferredRegister}：{@code BuiltInRegistries.RECIPE_TYPE} 在模组构造期已经冻结，
 * 直接 {@code Registry.register} 会抛「Registry is already frozen」。因此类型对外暴露成
 * {@link DeferredHolder}，使用点（{@link HighPressureRecipe#getType()} 与高压熔炉的配方查找）
 * 都在注册表就绪之后才取值。</p>
 */
public final class ModRecipes {

    /** 高压熔炼：多材料 + 气压 + 温度门槛。 */
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, VenusModule.NAMESPACE);

    public static final DeferredHolder<RecipeType<?>, RecipeType<HighPressureRecipe>> HIGH_PRESSURE =
            RECIPE_TYPES.register("high_pressure", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return VenusModule.NAMESPACE + ":high_pressure";
                }
            });

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, VenusModule.NAMESPACE);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> HIGH_PRESSURE_SERIALIZER =
            RECIPE_SERIALIZERS.register("high_pressure", HighPressureRecipe.Serializer::new);

    private ModRecipes() {
    }

    /** 高压熔炼配方类型（注册表就绪后可用）。 */
    public static RecipeType<HighPressureRecipe> highPressureType() {
        return HIGH_PRESSURE.get();
    }
}
