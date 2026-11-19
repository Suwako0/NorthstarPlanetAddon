package com.suwako.northstarplanetaddon.mars.network;

import com.suwako.northstarplanetaddon.mars.MarsModule;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 → 客户端：火星沙尘暴状态。
 *
 * <p>为什么需要自己发：沙尘暴不再借用原版天气（原因见 {@code world.Sandstorm} 的类注释），
 * 客户端也就没法从 {@code ClientLevel.isRaining()} 推出任何东西 —— 雾效与 HUD 只能读这份数据。</p>
 *
 * <p>服务端在**开始 / 结束 / 每 20 tick** 各广播一次（覆盖中途登录、并顺带刷新剩余时间），
 * 玩家登录与跨维度时用 {@code Sandstorm.syncTo} 单独补发。</p>
 *
 * @param active         火星当前是否有沙尘暴
 * @param intense        是否强化沙尘暴
 * @param windDegrees    风向（0~359，正北为 0，顺时针）
 * @param remainingTicks 剩余 tick（客户端每秒本地递减，两次包之间也能平滑倒数）
 */
public record SandstormPayload(boolean active, boolean intense, int windDegrees, int remainingTicks)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SandstormPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(MarsModule.NAMESPACE, "sandstorm"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SandstormPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, SandstormPayload::active,
                    ByteBufCodecs.BOOL, SandstormPayload::intense,
                    ByteBufCodecs.VAR_INT, SandstormPayload::windDegrees,
                    ByteBufCodecs.VAR_INT, SandstormPayload::remainingTicks,
                    SandstormPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
