package com.suwako.northstarplanetaddon.venus.registry;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import com.suwako.northstarplanetaddon.venus.entity.AcidJellyEntity;
import com.suwako.northstarplanetaddon.venus.entity.SporeWalkerEntity;
import com.suwako.northstarplanetaddon.venus.entity.MushroomMothEntity;
import com.suwako.northstarplanetaddon.venus.entity.FrostStalkerEntity;
import com.suwako.northstarplanetaddon.venus.entity.ThunderChickEntity;
import com.suwako.northstarplanetaddon.venus.entity.LavaWormEntity;
import com.suwako.northstarplanetaddon.venus.entity.BrassDeaconEntity;
import com.suwako.northstarplanetaddon.venus.entity.CloudRayEntity;
import com.suwako.northstarplanetaddon.venus.entity.boss.AphroditeEntity;
import com.suwako.northstarplanetaddon.venus.entity.boss.HephaestusEntity;
import com.suwako.northstarplanetaddon.venus.entity.boss.NepheleEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 实体类型。 */
public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, VenusModule.NAMESPACE);

    // ------------------------------------------------------------------ Boss
    /** 阿佛洛狄忒（第 4.4 节）：终局 Boss，P3 巨大化。 */
    public static final DeferredHolder<EntityType<?>, EntityType<AphroditeEntity>> APHRODITE =
            ENTITY_TYPES.register("aphrodite", () -> EntityType.Builder
                    .of(AphroditeEntity::new, MobCategory.MONSTER)
                    .sized(4.0F, 6.0F)
                    .eyeHeight(5.2F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .build("aphrodite"));

    /**
     * 赫菲斯托斯巨像（第 4.3 节）：地表型，缓慢逼近。
     *
     * <p>判定箱 11.0×9.9 与重制后模型的真实外廓一致（模型 176×158 模型单位）：双拳/前臂
     * 在 ±4.1 格外，旧版 6×9 的盒子把它们排除在判定之外 —— 玩家照着拳头打会打空。
     * 巨像不走寻路（见 {@code HephaestusEntity#tickBossAi}），宽判定箱不会造成卡路。</p>
     */
    public static final DeferredHolder<EntityType<?>, EntityType<HephaestusEntity>> HEPHAESTUS =
            ENTITY_TYPES.register("hephaestus", () -> EntityType.Builder
                    .of(HephaestusEntity::new, MobCategory.MONSTER)
                    .sized(11.0F, 9.9F)
                    .eyeHeight(8.6F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .build("hephaestus"));

    /** 涅斐勒，云鲸（第 4.2 节）：7×4 碰撞箱，飞行。 */
    public static final DeferredHolder<EntityType<?>, EntityType<NepheleEntity>> NEPHELE =
            ENTITY_TYPES.register("nephele", () -> EntityType.Builder
                    .of(NepheleEntity::new, MobCategory.MONSTER)
                    .sized(7.0F, 4.0F)
                    .eyeHeight(2.6F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .build("nephele"));

    // ------------------------------------------------------------------ 生物
    /** 云帆鳐（第 5 章 #5）：飞行，25 HP，掉落云帆膜 / 云藻。 */
    public static final DeferredHolder<EntityType<?>, EntityType<CloudRayEntity>> CLOUD_RAY =
            ENTITY_TYPES.register("cloud_ray", () -> EntityType.Builder
                    .of(CloudRayEntity::new, MobCategory.CREATURE)
                    .sized(1.6F, 0.6F)
                    .eyeHeight(0.4F)
                    .clientTrackingRange(10)
                    .build("cloud_ray"));

    /** 酸潮水母（第 5 章 #1）：漂浮，12 HP，命中附加腐蚀。 */
    public static final DeferredHolder<EntityType<?>, EntityType<AcidJellyEntity>> ACID_JELLY =
            ENTITY_TYPES.register("acid_jelly", () -> EntityType.Builder
                    .of(AcidJellyEntity::new, MobCategory.MONSTER)
                    .sized(0.8F, 1.2F)
                    .eyeHeight(0.8F)
                    .clientTrackingRange(8)
                    .build("acid_jelly"));


    // ------------------------------------------------------------------ 生物（第 5 章）
    public static final DeferredHolder<EntityType<?>, EntityType<SporeWalkerEntity>> SPORE_WALKER =
            ENTITY_TYPES.register("spore_walker", () -> EntityType.Builder
                    .of(SporeWalkerEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .eyeHeight(1.7F)
                    .clientTrackingRange(8)
                    .build("spore_walker"));

    public static final DeferredHolder<EntityType<?>, EntityType<MushroomMothEntity>> MUSHROOM_MOTH =
            ENTITY_TYPES.register("mushroom_moth", () -> EntityType.Builder
                    .of(MushroomMothEntity::new, MobCategory.AMBIENT)
                    .sized(0.7F, 0.6F)
                    .eyeHeight(0.4F)
                    .clientTrackingRange(8)
                    .build("mushroom_moth"));

    public static final DeferredHolder<EntityType<?>, EntityType<FrostStalkerEntity>> FROST_STALKER =
            ENTITY_TYPES.register("frost_stalker", () -> EntityType.Builder
                    .of(FrostStalkerEntity::new, MobCategory.MONSTER)
                    .sized(1.4F, 0.9F)
                    .eyeHeight(0.65F)
                    .clientTrackingRange(8)
                    .build("frost_stalker"));

    public static final DeferredHolder<EntityType<?>, EntityType<ThunderChickEntity>> THUNDER_CHICK =
            ENTITY_TYPES.register("thunder_chick", () -> EntityType.Builder
                    .of(ThunderChickEntity::new, MobCategory.AMBIENT)
                    .sized(0.5F, 0.9F)
                    .eyeHeight(0.6F)
                    .clientTrackingRange(8)
                    .build("thunder_chick"));

    public static final DeferredHolder<EntityType<?>, EntityType<LavaWormEntity>> LAVA_WORM =
            ENTITY_TYPES.register("lava_worm", () -> EntityType.Builder
                    .of(LavaWormEntity::new, MobCategory.MONSTER)
                    .sized(0.4F, 0.3F)
                    .eyeHeight(0.2F)
                    .clientTrackingRange(8)
                    .build("lava_worm"));

    public static final DeferredHolder<EntityType<?>, EntityType<BrassDeaconEntity>> BRASS_DEACON =
            ENTITY_TYPES.register("brass_deacon", () -> EntityType.Builder
                    .of(BrassDeaconEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .eyeHeight(1.7F)
                    .clientTrackingRange(8)
                    .build("brass_deacon"));

    // ------------------------------------------------------------------ 投掷物
    /** 酸蚀矛投掷体：三叉戟式投掷的落点实体（命中留在地面可捡回）。 */
    public static final DeferredHolder<EntityType<?>, EntityType<com.suwako.northstarplanetaddon.venus.entity.ThrownAcidSpear>>
            THROWN_ACID_SPEAR = ENTITY_TYPES.register("acidic_spear", () -> EntityType.Builder
                    .<com.suwako.northstarplanetaddon.venus.entity.ThrownAcidSpear>of(
                            com.suwako.northstarplanetaddon.venus.entity.ThrownAcidSpear::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(6)
                    .updateInterval(10)
                    .build("acidic_spear"));

    /** mod bus：EntityAttributeCreationEvent。 */
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(NEPHELE.get(), NepheleEntity.createAttributes().build());
        event.put(HEPHAESTUS.get(), HephaestusEntity.createAttributes().build());
        event.put(APHRODITE.get(), AphroditeEntity.createAttributes().build());
        event.put(CLOUD_RAY.get(), CloudRayEntity.createAttributes().build());
        event.put(ACID_JELLY.get(), AcidJellyEntity.createAttributes().build());
        event.put(SPORE_WALKER.get(), com.suwako.northstarplanetaddon.venus.entity.SporeWalkerEntity.createAttributes().build());
        event.put(MUSHROOM_MOTH.get(), com.suwako.northstarplanetaddon.venus.entity.MushroomMothEntity.createAttributes().build());
        event.put(FROST_STALKER.get(), com.suwako.northstarplanetaddon.venus.entity.FrostStalkerEntity.createAttributes().build());
        event.put(THUNDER_CHICK.get(), com.suwako.northstarplanetaddon.venus.entity.ThunderChickEntity.createAttributes().build());
        event.put(LAVA_WORM.get(), com.suwako.northstarplanetaddon.venus.entity.LavaWormEntity.createAttributes().build());
        event.put(BRASS_DEACON.get(), com.suwako.northstarplanetaddon.venus.entity.BrassDeaconEntity.createAttributes().build());
    }

    private ModEntities() {
    }
}
