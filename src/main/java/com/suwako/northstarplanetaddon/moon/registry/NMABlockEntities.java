package com.suwako.northstarplanetaddon.moon.registry;

import com.suwako.northstarplanetaddon.moon.MoonModule;
import com.suwako.northstarplanetaddon.moon.block.MoonSpiritAltarBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 月球模块的方块实体类型。 */
public final class NMABlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MoonModule.NAMESPACE);

    /** 月灵石坛 —— 只用来播放赐福的粒子特效，没有需要持久化的数据。 */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MoonSpiritAltarBlockEntity>> MOON_SPIRIT_ALTAR =
            BLOCK_ENTITY_TYPES.register("moon_spirit_altar",
                    () -> BlockEntityType.Builder
                            .of(MoonSpiritAltarBlockEntity::new, NMABlocks.MOON_SPIRIT_ALTAR.get())
                            .build(null));

    private NMABlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
