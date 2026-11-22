package com.suwako.northstarplanetaddon.moon.item;

import com.suwako.northstarplanetaddon.moon.registry.NMABlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Moon Pearl is an altar key, not a locator: right-click Dreamsleep Grass on Northstar's Moon to
 * consume one pearl and place the Dreamsleep Altar template directly at that location.
 */
public final class MoonPearlItem extends Item {

    private static final ResourceLocation MOON_DIMENSION = ResourceLocation.fromNamespaceAndPath("northstar", "moon");
    private static final ResourceLocation DREAMSLEEP_ALTAR_TEMPLATE =
            ResourceLocation.fromNamespaceAndPath("northstar_planet_addon", "dreamsleep_alter");
    /**
     * The NBT is 7×3×7; although the structure block was saved with offset (0, 1, 0), the actual
     * altar block is at template coordinate (3, 1, 3), the horizontal centre of that saved volume.
     */
    private static final BlockPos DREAMSLEEP_ALTAR_ANCHOR = new BlockPos(3, 1, 3);

    public MoonPearlItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();

        // Let DreamsleepAltarBlock handle a pearl used on an existing altar first; this method only
        // claims Dreamsleep Grass, so normal item use on every other block is unchanged.
        if (!level.getBlockState(pos).is(NMABlocks.DREAMSLEEP_GRASS.get())) {
            return InteractionResult.PASS;
        }
        if (!level.dimension().location().equals(MOON_DIMENSION)) {
            if (!level.isClientSide() && player != null) {
                player.displayClientMessage(Component.translatable("item.northstar_planet_addon.moon_pearl.moon_only"), true);
            }
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }

        StructureTemplate template = serverLevel.getStructureManager().get(DREAMSLEEP_ALTAR_TEMPLATE).orElse(null);
        if (template == null) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("item.northstar_planet_addon.moon_pearl.no_altar"), true);
            }
            return InteractionResult.FAIL;
        }

        Rotation rotation = Rotation.values()[serverLevel.getRandom().nextInt(Rotation.values().length)];
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(rotation)
                .setIgnoreEntities(false)
                .setKnownShape(true);
        // Rotation transforms the anchor as well as every other template coordinate. Compensate for
        // that transformed anchor (rather than the raw 3,1,3 coordinate) so every random rotation
        // still puts the altar exactly on the clicked centre block.
        BlockPos rotatedAnchor = StructureTemplate.calculateRelativePosition(settings, DREAMSLEEP_ALTAR_ANCHOR);
        // Raise the entire ritual structure one block above the clicked Dreamsleep Grass.
        BlockPos templateOrigin = pos.subtract(rotatedAnchor).above();
        boolean placed = template.placeInWorld(serverLevel, templateOrigin, templateOrigin, settings,
                serverLevel.getRandom(), Block.UPDATE_CLIENTS);
        if (!placed) {
            return InteractionResult.FAIL;
        }

        if (player == null || !player.getAbilities().instabuild) {
            replaceOneWithBrokenPearl(player, context.getHand(), stack);
        }
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player));
        level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0F, 0.7F);
        if (player != null) {
            player.swing(context.getHand(), true);
        }
        return InteractionResult.CONSUME;
    }

    /** Converts exactly the pearl used for altar placement, preserving any remaining pearl stack. */
    private static void replaceOneWithBrokenPearl(Player player, InteractionHand hand, ItemStack pearls) {
        ItemStack brokenPearl = new ItemStack(com.suwako.northstarplanetaddon.moon.registry.NMAItems.BROKEN_MOON_PEARL.get());
        if (pearls.getCount() == 1) {
            player.setItemInHand(hand, brokenPearl);
            return;
        }

        pearls.shrink(1);
        if (!player.addItem(brokenPearl)) {
            player.drop(brokenPearl, false);
        }
    }
}
