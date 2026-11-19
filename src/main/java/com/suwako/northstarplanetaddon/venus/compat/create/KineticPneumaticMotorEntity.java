package com.suwako.northstarplanetaddon.venus.compat.create;

import com.suwako.northstarplanetaddon.venus.machine.WorkConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Create-integrated pneumatic motor (loaded only when Create is present).
 *
 * <p>It is a stress CONSUMER on Create's kinetic network: shaft speed is converted
 * into this mod's mechanical work ({@link WorkConsumer}) for adjacent machines.
 * Without Create the block entity is {@code PneumaticMotorBlock.Entity} (air driven, 32 RPM).</p>
 */
public class KineticPneumaticMotorEntity extends KineticBlockEntity {

    /** Stress impact: 4 SU per RPM. */
    public static final float STRESS_IMPACT = 4.0F;

    public KineticPneumaticMotorEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void initialize() {
        super.initialize();
        this.attachKinetics();
    }

    @Override
    public float calculateStressApplied() {
        return STRESS_IMPACT;
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        this.pushWork();
    }

    @Override
    public void tick() {
        super.tick();
        this.pushWork();
    }

    private void pushWork() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        int rpm = (int) Math.abs(this.getSpeed());
        if (rpm <= 0) {
            return;
        }
        WorkConsumer consumer = WorkConsumer.findNeighbor(this.level, this.worldPosition);
        if (consumer != null) {
            consumer.acceptWork(rpm);
        }
    }
}