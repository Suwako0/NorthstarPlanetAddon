package com.suwako.northstarplanetaddon.moon.block;

import com.suwako.northstarplanetaddon.moon.entity.MoonDeerEntity;
import com.suwako.northstarplanetaddon.moon.registry.NMAEntityTypes;
import com.suwako.northstarplanetaddon.moon.registry.NMAItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 梦眠祭坛 / Dreamsleep Altar — offer one 月明珠 to call the Moonforest boss.
 * <p>
 * Offer a Moon Pearl and the altar wakes: it spends the pearl, calls 梦眠月鹿, plays its moonlight,
 * and once the light has run its course the altar itself is consumed, leaving nothing behind.
 * <p>
 * Only one deer may be called at a time. Before summoning, the altar sweeps a generous radius for a
 * living deer and refuses if it finds one, so a second altar (or a second right click) cannot stack
 * bosses on top of each other.
 */
public class DreamsleepAltarBlock extends Block {

    /** Radius searched for an existing boss before the altar will call another. */
    private static final double BOSS_GUARD_RADIUS = 128.0D;

    /** How long the moonlight burns before the altar spends itself. */
    private static final int RITUAL_TICKS = 40;

    public DreamsleepAltarBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            player.displayClientMessage(Component.translatable("block.northstar_planet_addon.dreamsleep_altar.needs_pearl")
                    .withStyle(ChatFormatting.GRAY), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(NMAItems.MOON_PEARL.get())) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) level;

        // 月华未至: the altar only answers once the sky has gone dark.
        if (!isRitualHour(level)) {
            player.displayClientMessage(Component.translatable("block.northstar_planet_addon.dreamsleep_altar.daylight")
                    .withStyle(ChatFormatting.GOLD), true);
            serverLevel.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.6F, 0.7F);
            return ItemInteractionResult.FAIL;
        }

        // Protection: never stack bosses. Any living deer within the guard radius blocks the ritual.
        AABB guard = new AABB(pos).inflate(BOSS_GUARD_RADIUS);
        if (!serverLevel.getEntitiesOfClass(MoonDeerEntity.class, guard, Entity::isAlive).isEmpty()) {
            player.displayClientMessage(Component.translatable("block.northstar_planet_addon.dreamsleep_altar.occupied")
                    .withStyle(ChatFormatting.RED), true);
            moonlight(serverLevel, pos, 10);
            serverLevel.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 0.7F, 0.6F);
            return ItemInteractionResult.FAIL;
        }

        MoonDeerEntity deer = NMAEntityTypes.MOON_DEER.get().create(serverLevel);
        if (deer == null) {
            return ItemInteractionResult.FAIL;
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        deer.moveTo(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, player.getYRot() + 180.0F, 0.0F);
        deer.setPersistenceRequired();
        serverLevel.addFreshEntity(deer);
        deer.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, player.position());

        serverLevel.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0F, 0.5F);
        moonlight(serverLevel, pos, 45);

        // The altar is spent once the moonlight finishes: burnt out, and it does not drop itself.
        serverLevel.scheduleTick(pos, this, RITUAL_TICKS);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        moonlight(level, pos, 60);
        level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 1.5F);
        level.destroyBlock(pos, false);
    }

    /** Moon-bright motes rising off the altar, plus a fainter wash of enchant light. */
    private static void moonlight(ServerLevel level, BlockPos pos, int count) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.6D;
        double z = pos.getZ() + 0.5D;
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, count, 0.45D, 0.75D, 0.45D, 0.015D);
        level.sendParticles(ParticleTypes.WITCH, x, y + 0.35D, z, Math.max(4, count / 4), 0.7D, 0.5D, 0.7D, 0.0D);
    }

    /**
     * True from dusk to dawn. The Moon and the Overworld both run a real day cycle, so this is simply
     * {@link Level#isNight()}; a dimension frozen at a single time is judged by that time instead, so
     * an altar in such a dimension is consistently open or closed rather than never working.
     */
    private static boolean isRitualHour(Level level) {
        if (level.dimensionType().hasFixedTime()) {
            long fixed = Math.floorMod(level.getDayTime(), 24000L);
            return fixed >= NIGHT_START || fixed < NIGHT_END;
        }
        return level.isNight();
    }

    /** First tick of the night, 13000, as vanilla defines it. */
    private static final long NIGHT_START = 13000L;

    /** First tick of the day again, 23000. */
    private static final long NIGHT_END = 23000L;

    /** Placing the altar never carries a block state over from whatever was there before. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState();
    }
}
