package com.suwako.northstarplanetaddon.mars.event;

import com.suwako.northstarplanetaddon.NorthstarPlanetAddon;

import com.suwako.northstarplanetaddon.mars.MarsModule;
import com.suwako.northstarplanetaddon.mars.entity.AresEntity;
import com.suwako.northstarplanetaddon.mars.entity.DemonMothEntity;
import com.suwako.northstarplanetaddon.mars.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/** mod bus 上的注册类事件：属性与生成规则。 */
@EventBusSubscriber(modid = NorthstarPlanetAddon.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class CommonEvents {

    @SubscribeEvent
    public static void onEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.DEMON_MOTH.get(), DemonMothEntity.createAttributes().build());
        event.put(ModEntities.ARES.get(), AresEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        // 飞行生物：无地形限制，夜晚生成
        event.register(ModEntities.DEMON_MOTH.get(),
                SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                CommonEvents::checkDemonMothSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static boolean checkDemonMothSpawnRules(EntityType<DemonMothEntity> type, ServerLevelAccessor level,
                                                    MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && Monster.isDarkEnoughToSpawn(level, pos, random);
    }

    private CommonEvents() {
    }
}
