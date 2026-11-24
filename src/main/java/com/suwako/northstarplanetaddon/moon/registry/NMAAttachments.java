package com.suwako.northstarplanetaddon.moon.registry;

import com.suwako.northstarplanetaddon.moon.MoonModule;
import com.suwako.northstarplanetaddon.moon.state.MoonSpiritState;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** 月球模块用到的 NeoForge 数据附件。 */
public final class NMAAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MoonModule.NAMESPACE);

    /**
     * 月灵丹药进度（服用次数 + 是否被允许再次服用）。
     *
     * <p>{@code serialize} 让它进存档；{@code sync} 让它到客户端（物品 {@code use()} 要在客户端判定）；
     * {@code copyOnDeath} 让死亡重生后进度不丢。</p>
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<MoonSpiritState>> MOON_SPIRIT_STATE =
            ATTACHMENT_TYPES.register("moon_spirit_state",
                    () -> AttachmentType.builder(() -> MoonSpiritState.INITIAL)
                            .serialize(MoonSpiritState.CODEC)
                            .sync(MoonSpiritState.STREAM_CODEC)
                            .copyOnDeath()
                            .build());

    private NMAAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
