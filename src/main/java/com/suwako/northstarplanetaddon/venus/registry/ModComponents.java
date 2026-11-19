package com.suwako.northstarplanetaddon.venus.registry;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.Nullable;

/**
 * 数据组件（1.21.1 的 DataComponents 体系）。
 *
 * <ul>
 *   <li>{@link #FRESHNESS}：食物新鲜度 0–3，见第 7.1 节；</li>
 *   <li>{@link #CORROSION}：物品上的腐蚀值 0–100，见第 12.4 节；</li>
 *   <li>{@link #CHARGE}：雷羽弩的蓄能发数（雷暴带充能 +50% 伤害，10 发）。</li>
 * </ul>
 */
public final class ModComponents {

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, VenusModule.NAMESPACE);

    public static final DataComponentType<Integer> FRESHNESS =
            register("freshness", Codec.INT, ByteBufCodecs.VAR_INT);

    public static final DataComponentType<Integer> CORROSION =
            register("corrosion", Codec.INT, ByteBufCodecs.VAR_INT);

    public static final DataComponentType<Integer> CHARGE =
            register("charge", Codec.INT, ByteBufCodecs.VAR_INT);

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
