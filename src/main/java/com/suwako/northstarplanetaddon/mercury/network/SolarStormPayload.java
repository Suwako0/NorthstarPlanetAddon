package com.suwako.northstarplanetaddon.mercury.network;

import com.suwako.northstarplanetaddon.mercury.MercuryModule;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * One boolean, sent server to client: is the solar storm currently raging over Mercury.
 * <p>
 * The client owns nothing but the presentation, so the whole weather only needs this single flag
 * (re-sent as a heartbeat, which is what gets late joiners and dimension changes right). The client
 * handler class is referenced only inside a dist check, so it is never loaded on a dedicated server.
 */
public record SolarStormPayload(boolean active) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SolarStormPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MercuryModule.NAMESPACE, "solar_storm"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SolarStormPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SolarStormPayload::active, SolarStormPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnClient(SolarStormPayload payload, IPayloadContext context) {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }
        context.enqueueWork(() -> com.suwako.northstarplanetaddon.mercury.client.MercuryClient.setStormActive(payload.active()));
    }
}
