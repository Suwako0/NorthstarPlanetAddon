package com.suwako.northstarplanetaddon.mars.registry;

import com.mojang.serialization.Codec;
import com.suwako.northstarplanetaddon.mars.MarsModule;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.Nullable;

/**
 * 便携式冷却器的数据组件。
 *
 * <p>写法与 Northstar Redux 0.6.5 的 {@code NorthstarDataComponents} 一致
 * （1.21.1 上已验证可用）。</p>
 */
public final class ModComponents {

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MarsModule.NAMESPACE);

    /** 冷却液种类："water" / "liquid_oxygen"（空 = 未装填）。 */
    public static final DataComponentType<String> COOLANT =
            register("coolant", Codec.STRING, ByteBufCodecs.STRING_UTF8);

    /** 剩余可用次数 0..MAX_CHARGES。 */
    public static final DataComponentType<Integer> COOLANT_CHARGES =
            register("coolant_charges", Codec.INT, ByteBufCodecs.VAR_INT);

    private static <T> DataComponentType<T> register(String name,
                                                     @Nullable Codec<T> persistentCodec,
                                                     @Nullable StreamCodec<? super RegistryFriendlyByteBuf, T> networkCodec) {
        DataComponentType.Builder<T> builder = DataComponentType.builder();
        if (persistentCodec != null) {
            builder.persistent(persistentCodec);
        }
        if (networkCodec != null) {
            builder.networkSynchronized(networkCodec);
        }
        DataComponentType<T> type = builder.build();
        DATA_COMPONENTS.register(name, () -> type);
        return type;
    }

    private ModComponents() {
    }
}
