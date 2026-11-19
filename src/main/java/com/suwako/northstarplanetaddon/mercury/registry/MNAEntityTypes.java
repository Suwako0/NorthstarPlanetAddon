package com.suwako.northstarplanetaddon.mercury.registry;

import com.suwako.northstarplanetaddon.mercury.MercuryModule;
import com.suwako.northstarplanetaddon.mercury.entity.BlazingSunfishEntity;
import com.suwako.northstarplanetaddon.mercury.entity.MeteorEntity;
import com.suwako.northstarplanetaddon.mercury.entity.SunfishEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Entity types added by the mod. */
public final class MNAEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MercuryModule.NAMESPACE);

    /**
     * 陨石 — the solar-storm meteorite. A tracked, non-collidable falling body: the server moves and
     * resolves it, the client draws it as a glowing rock with {@code ThrownItemRenderer}.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<MeteorEntity>> METEOR =
            ENTITY_TYPES.register("meteor", () -> EntityType.Builder
                    .<MeteorEntity>of(MeteorEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("meteor"));

    /** 太阳鱼 — a winged sky fish that glides through Mercury's sky (body ~0.9 block tall, wings 1.3 wide). */
    public static final DeferredHolder<EntityType<?>, EntityType<SunfishEntity>> SUNFISH =
            ENTITY_TYPES.register("sunfish", () -> EntityType.Builder
                    .<SunfishEntity>of(SunfishEntity::new, MobCategory.CREATURE)
                    .sized(1.3F, 1.2F)
                    .eyeHeight(0.6F)
                    .clientTrackingRange(12)
                    .updateInterval(2)
                    .build("sunfish"));

    /** 炽烈太阳鱼 — the armoured, fireproof solar-storm breed of 太阳鱼. */
    public static final DeferredHolder<EntityType<?>, EntityType<BlazingSunfishEntity>> BLAZING_SUNFISH =
            ENTITY_TYPES.register("blazing_sunfish", () -> EntityType.Builder
                    .<BlazingSunfishEntity>of(BlazingSunfishEntity::new, MobCategory.CREATURE)
                    .sized(1.55F, 1.4F)
                    .eyeHeight(0.7F)
                    .fireImmune()
                    .clientTrackingRange(12)
                    .updateInterval(2)
                    .build("blazing_sunfish"));

    private MNAEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
