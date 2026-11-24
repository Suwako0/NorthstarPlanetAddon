package com.suwako.northstarplanetaddon.venus.network;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 气压计读数包（服务端 → 客户端）：环境温度 / 腐蚀值 / 距下次季风天数。
 *
 * <p>气动/压力机器体系移除后不再有"气压（bar）"读数。</p>
 *
 * <p>处理器只把数据写进 {@link BarometerCache}（纯数据类，不引用任何客户端类型），
 * HUD 再自己去读缓存 —— 这样服务端永远不会因为类加载而碰到客户端渲染类。</p>
 */
public record BarometerPayload(float temperature, int corrosion, int daysToMonsoon)
        implements CustomPacketPayload {

    public static final Type<BarometerPayload> TYPE = new Type<>(VenusModule.id("barometer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BarometerPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, BarometerPayload::temperature,
            ByteBufCodecs.VAR_INT, BarometerPayload::corrosion,
            ByteBufCodecs.VAR_INT, BarometerPayload::daysToMonsoon,
            BarometerPayload::new);

    @Override
    public Type<BarometerPayload> type() {
        return TYPE;
    }

    public static void handle(BarometerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> BarometerCache.update(payload));
    }
}
