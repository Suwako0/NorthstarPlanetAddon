package com.suwako.northstarplanetaddon.moon.entity.ai;

import com.suwako.northstarplanetaddon.moon.entity.MoonRabbitEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.EnumSet;

/**
 * Piglin-style item-seeking behaviour for Moon Rabbits: find a dropped golden carrot, walk to it,
 * take exactly one, and let the rabbit perform its delayed eating/trade sequence.
 */
public final class MoonRabbitGoldenCarrotGoal extends Goal {

    private static final double SEARCH_RADIUS = 12.0D;
    private static final double PICKUP_DISTANCE_SQR = 2.25D;

    private final MoonRabbitEntity rabbit;
    private final double speedModifier;
    private ItemEntity target;

    public MoonRabbitGoldenCarrotGoal(MoonRabbitEntity rabbit, double speedModifier) {
        this.rabbit = rabbit;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.rabbit.canStartGoldenCarrotTrade()) {
            return false;
        }
        this.target = this.findNearestGoldenCarrot();
        return this.target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.rabbit.canStartGoldenCarrotTrade() && this.isValidTarget(this.target);
    }

    @Override
    public void start() {
        if (this.target != null) {
            this.rabbit.getNavigation().moveTo(this.target, this.speedModifier);
        }
    }

    @Override
    public void stop() {
        this.target = null;
        this.rabbit.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (!this.isValidTarget(this.target)) {
            return;
        }

        this.rabbit.getLookControl().setLookAt(this.target.getX(), this.target.getEyeY(), this.target.getZ(), 30.0F, 30.0F);
        if (this.rabbit.distanceToSqr(this.target) <= PICKUP_DISTANCE_SQR) {
            this.rabbit.consumeDroppedGoldenCarrot(this.target);
            return;
        }
        this.rabbit.getNavigation().moveTo(this.target, this.speedModifier);
    }

    private ItemEntity findNearestGoldenCarrot() {
        return this.rabbit.level().getEntitiesOfClass(
                        ItemEntity.class,
                        this.rabbit.getBoundingBox().inflate(SEARCH_RADIUS),
                        this::isValidTarget)
                .stream()
                .min(Comparator.comparingDouble(this.rabbit::distanceToSqr))
                .orElse(null);
    }

    private boolean isValidTarget(ItemEntity itemEntity) {
        return itemEntity != null
                && !itemEntity.isRemoved()
                && !itemEntity.hasPickUpDelay()
                && itemEntity.getItem().is(Items.GOLDEN_CARROT);
    }
}
