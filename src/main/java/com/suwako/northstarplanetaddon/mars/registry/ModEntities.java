package com.suwako.northstarplanetaddon.mars.registry;

import com.suwako.northstarplanetaddon.mars.MarsModule;
import com.suwako.northstarplanetaddon.mars.entity.AresEntity;
import com.suwako.northstarplanetaddon.mars.entity.DemonMothEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 本模组实体。
 *
 * <p>恶魔蛾继承原版幻翼（复用模型/贴图与飞行手感），阿瑞斯继承原版铁傀儡
 * （复用模型/贴图，符合「熔核巨像」设定）。</p>
 */
public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MarsModule.NAMESPACE);

    public static final DeferredHolder<EntityType<?>, EntityType<DemonMothEntity>> DEMON_MOTH =
            ENTITY_TYPES.register("demon_moth", () -> EntityType.Builder
                    .of(DemonMothEntity::new, MobCategory.MONSTER)
                    .sized(1.4F, 0.8F)
                    .eyeHeight(0.5F)
                    .clientTrackingRange(10)
                    .build("demon_moth"));

    public static final DeferredHolder<EntityType<?>, EntityType<AresEntity>> ARES =
            ENTITY_TYPES.register("ares", () -> EntityType.Builder
                    .of(AresEntity::new, MobCategory.MONSTER)
                    .sized(1.6F, 3.2F)
                    .eyeHeight(2.9F)
                    .fireImmune()
                    .clientTrackingRange(12)
                    .build("ares"));

    private ModEntities() {
    }
}
