package com.suwako.northstarplanetaddon.moon.registry;

import com.suwako.northstarplanetaddon.moon.MoonModule;
import com.suwako.northstarplanetaddon.moon.entity.MoonDeerEntity;
import com.suwako.northstarplanetaddon.moon.entity.MoonDustProjectileEntity;
import com.suwako.northstarplanetaddon.moon.entity.MoonRabbitEntity;
import com.suwako.northstarplanetaddon.moon.entity.MoonlightBoltEntity;
import com.suwako.northstarplanetaddon.moon.entity.MoonShellCrabEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Entity types added by the mod. */
public final class NMAEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MoonModule.NAMESPACE);

    /** 月兔 — a small, peaceful lunar rabbit. */
    public static final DeferredHolder<EntityType<?>, EntityType<MoonRabbitEntity>> MOON_RABBIT =
            ENTITY_TYPES.register("moon_rabbit", () -> EntityType.Builder
                    .of(MoonRabbitEntity::new, MobCategory.CREATURE)
                    .sized(0.45F, 0.5F)
                    .eyeHeight(0.4F)
                    .clientTrackingRange(10)
                    .build("moon_rabbit"));

    /** 月壳蟹 — a heavily armoured, peaceful lunar crab. */
    public static final DeferredHolder<EntityType<?>, EntityType<MoonShellCrabEntity>> MOON_SHELL_CRAB =
            ENTITY_TYPES.register("moon_shell_crab", () -> EntityType.Builder
                    .of(MoonShellCrabEntity::new, MobCategory.CREATURE)
                    .sized(0.7F, 0.45F)
                    .eyeHeight(0.3F)
                    .clientTrackingRange(10)
                    .build("moon_shell_crab"));

    /**
     * 梦眠月鹿 Boss. The box spans the whole creature's height and antler spread (1.8 x 2.9 blocks, eye
     * height 2.6 = the deer's own eyes, measured off the model), so the neck and head sit inside it;
     * the chest front, neck, head, both antler racks, the haunch and the tail additionally carry their
     * own hit parts, which is what makes the long body and the antlers clickable from the side.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<MoonDeerEntity>> MOON_DEER =
            ENTITY_TYPES.register("lunar_deer", () -> EntityType.Builder
                    .of(MoonDeerEntity::new, MobCategory.CREATURE)
                    .sized(1.8F, 2.9F)
                    .eyeHeight(2.6F)
                    .clientTrackingRange(64)
                    .build("lunar_deer"));

    /** Particle-rendered lunar dust projectile thrown by a Moon Shell Crab. */
    public static final DeferredHolder<EntityType<?>, EntityType<MoonDustProjectileEntity>> MOON_DUST_PROJECTILE =
            ENTITY_TYPES.register("moon_dust_projectile", () -> EntityType.Builder
                    .<MoonDustProjectileEntity>of((type, level) -> new MoonDustProjectileEntity(type, level), MobCategory.MISC)
                    .sized(0.18F, 0.18F)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build("moon_dust_projectile"));

    /** Invisible entity shell for the Moon Deer's particle-rendered moonlight bolts. */
    public static final DeferredHolder<EntityType<?>, EntityType<MoonlightBoltEntity>> MOONLIGHT_BOLT =
            ENTITY_TYPES.register("moonlight_bolt", () -> EntityType.Builder
                    .<MoonlightBoltEntity>of((type, level) -> new MoonlightBoltEntity(type, level), MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("moonlight_bolt"));

    private NMAEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
