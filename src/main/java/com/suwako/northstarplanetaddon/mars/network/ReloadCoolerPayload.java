package com.suwako.northstarplanetaddon.mars.network;

import com.suwako.northstarplanetaddon.mars.MarsModule;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 → 服务端：请求「换弹」（按 R 键）。
 *
 * <p>空负载（没有字段）：换弹所需的全部信息（哪个物品、背包里的罐子）服务端自己就能查到，
 * 客户端只需要告诉服务端「我按了键」。{@link StreamCodec#unit} 就是为这种包准备的。</p>
 */
public record ReloadCoolerPayload() implements CustomPacketPayload {

    public static final ReloadCoolerPayload INSTANCE = new ReloadCoolerPayload();

    public static final CustomPacketPayload.Type<ReloadCoolerPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(MarsModule.NAMESPACE, "reload_cooler"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReloadCoolerPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
