package com.suwako.northstarplanetaddon.moon.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;

/**
 * 梦眠月斧 / Dreamsleep Moon Axe — the axe forged from a defeated Moon Deer's antler.
 * <p>
 * Beyond being an axe that out-cuts netherite, it carries the deer's moonlight: holding it keeps a
 * refreshed Resistance I on the wielder. The client renders its subtle hand-held glow separately.
 * <p>
 * The buff is applied server side from {@link #inventoryTick} and only while the axe is actually in a
 * hand, so putting it away lets the effect lapse on its own a moment later rather than leaving a
 * permanent buff behind.
 */
public class MoonDeerAxeItem extends AxeItem {

    /** Re-applied every tick while held; short enough to fade quickly once it is put away. */
    private static final int RESISTANCE_TICKS = 40;

    public MoonDeerAxeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!(entity instanceof Player player) || !isHeld(player, stack)) {
            return;
        }
        if (!level.isClientSide()) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_RESISTANCE, RESISTANCE_TICKS, 0, false, false, true));
        }
    }

    private static boolean isHeld(Player player, ItemStack stack) {
        return player.getMainHandItem() == stack || player.getOffhandItem() == stack;
    }

    /**
     * Moonlight flashes when the axe actually bites, so a kill reads as a crescent-cut rather than a
     * plain hit. Runs server side and fans the motes out around the swing.
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, net.minecraft.world.entity.LivingEntity target,
                             net.minecraft.world.entity.LivingEntity attacker) {
        if (attacker.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    target.getX(), target.getY() + target.getBbHeight() * 0.6D, target.getZ(),
                    14, 0.35D, 0.35D, 0.35D, 0.05D);
        }
        return super.hurtEnemy(stack, target, attacker);
    }
}
