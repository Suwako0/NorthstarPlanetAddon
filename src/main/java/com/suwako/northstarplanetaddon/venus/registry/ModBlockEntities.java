package com.suwako.northstarplanetaddon.venus.registry;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import com.suwako.northstarplanetaddon.venus.blockentity.CompressorBlockEntity;
import com.suwako.northstarplanetaddon.venus.blockentity.HighPressureFurnaceBlockEntity;
import com.suwako.northstarplanetaddon.venus.blockentity.PressureCookerBlockEntity;
import com.suwako.northstarplanetaddon.venus.blockentity.RadiatorBlockEntity;
import com.suwako.northstarplanetaddon.venus.blockentity.PressureTankBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 方块实体类型。 */
public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, VenusModule.NAMESPACE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PressureTankBlockEntity>> PRESSURE_TANK =
            BLOCK_ENTITY_TYPES.register("pressure_tank", () -> BlockEntityType.Builder
                    .of(PressureTankBlockEntity::new, ModBlocks.PRESSURE_TANK.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CompressorBlockEntity>> COMPRESSOR =
            BLOCK_ENTITY_TYPES.register("compressor", () -> BlockEntityType.Builder
                    .of(CompressorBlockEntity::new, ModBlocks.COMPRESSOR.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PressureCookerBlockEntity>> PRESSURE_COOKER =
            BLOCK_ENTITY_TYPES.register("pressure_cooker", () -> BlockEntityType.Builder
                    .of(PressureCookerBlockEntity::new, ModBlocks.PRESSURE_COOKER.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HighPressureFurnaceBlockEntity>>
            HIGH_PRESSURE_FURNACE = BLOCK_ENTITY_TYPES.register("high_pressure_furnace", () -> BlockEntityType.Builder
                    .of(HighPressureFurnaceBlockEntity::new, ModBlocks.HIGH_PRESSURE_FURNACE.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadiatorBlockEntity>> RADIATOR =
            BLOCK_ENTITY_TYPES.register("radiator", () -> BlockEntityType.Builder
                    .of(RadiatorBlockEntity::new, ModBlocks.RADIATOR.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.suwako.northstarplanetaddon.venus.block.FungusBedBlock.Entity>>
            FUNGUS_BED = BLOCK_ENTITY_TYPES.register("fungus_bed", () -> BlockEntityType.Builder
                    .of(com.suwako.northstarplanetaddon.venus.block.FungusBedBlock.Entity::new, ModBlocks.FUNGUS_BED.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.suwako.northstarplanetaddon.venus.block.CondenserBlock.Entity>>
            CONDENSER = BLOCK_ENTITY_TYPES.register("condenser", () -> BlockEntityType.Builder
                    .of(com.suwako.northstarplanetaddon.venus.block.CondenserBlock.Entity::new, ModBlocks.CONDENSER.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.suwako.northstarplanetaddon.venus.block.CloudLiftBlock.Entity>>
            CLOUD_LIFT = BLOCK_ENTITY_TYPES.register("cloud_lift", () -> BlockEntityType.Builder
                    .of(com.suwako.northstarplanetaddon.venus.block.CloudLiftBlock.Entity::new, ModBlocks.CLOUD_LIFT.get())
                    .build(null));
    /**
     * Pneumatic motor: a Create kinetic machine when the modpack has Create,
     * air-driven otherwise. Runtime detection is per-mod and independent.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> PNEUMATIC_MOTOR =
            BLOCK_ENTITY_TYPES.register("pneumatic_motor", () -> {
                BlockEntityType<?> kinetic = com.suwako.northstarplanetaddon.venus.compat.create.CreateReflect.motorType();
                if (kinetic != null) {
                    return kinetic;
                }
                return BlockEntityType.Builder.of(
                        com.suwako.northstarplanetaddon.venus.block.PneumaticMotorBlock.Entity::new,
                        ModBlocks.PNEUMATIC_MOTOR.get()).build(null);
            });

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.suwako.northstarplanetaddon.venus.block.AcidPumpBlock.Entity>>
            ACID_PUMP = BLOCK_ENTITY_TYPES.register("acid_pump", () -> BlockEntityType.Builder
                    .of(com.suwako.northstarplanetaddon.venus.block.AcidPumpBlock.Entity::new, ModBlocks.ACID_PUMP.get())
                    .build(null));

    private ModBlockEntities() {
    }
}
