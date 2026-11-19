package com.suwako.northstarplanetaddon.venus.event;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import com.suwako.northstarplanetaddon.venus.combat.ArmorSetBonus;
import com.suwako.northstarplanetaddon.venus.combat.ModDamageSources;
import com.suwako.northstarplanetaddon.venus.config.PhosphorosConfig;
import com.suwako.northstarplanetaddon.venus.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 玩家侧规则：腐蚀累积（12.4 节）、云海掉出岛屿（9.2 节）、四套护甲被动（6.1 节）。
 *
 * <p>套装被动的落地方式（与草案的对应关系写在每个分支里）：</p>
 * <ul>
 *   <li>云行者 4 件：空中二段跳 —— 无自定义按键，改为「下落时按潜行获得一次上推」，3s 冷却；</li>
 *   <li>熔管重甲 4 件：站熔岩不受伤 —— 泡在熔岩里时每 8s 触发一次 3s 抗火；</li>
 *   <li>启明星战甲 2 件：露天/阳光下每 5s 回 1 点护盾（吸收伤害）；</li>
 *   <li>启明星战甲 4 件：每 30s 清 1 层腐蚀，并免疫「失明」（孢子云）。</li>
 * </ul>
 */
public final class PlayerEvents {

    public static final String CORROSION_KEY = "northstar_planet_addon:corrosion";
    private static final String JUMP_COOLDOWN_KEY = "phosphoros.armor.doubleJump";
    private static final String LAVA_COOLDOWN_KEY = "phosphoros.armor.lavaStep";
    private static final ResourceLocation VENUS = ResourceLocation.parse("northstar:venus");

    private static final int DOUBLE_JUMP_COOLDOWN = 60;
    private static final int LAVA_STEP_COOLDOWN = 160;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        tickArmorPassives(player);
        if (player.tickCount % 20 == 0) {
            tickCorrosion(player);
            tickCloudVoid(player);
        }
    }

    // ================================================================== 护甲被动
    private static void tickArmorPassives(Player player) {
        CompoundTag data = player.getPersistentData();

        // 云行者 4 件：下落中按潜行 → 二段跳（3s 冷却）
        if (ArmorSetBonus.cloudWalker(player) >= 4) {
            int cooldown = data.getInt(JUMP_COOLDOWN_KEY);
            if (cooldown > 0) {
                data.putInt(JUMP_COOLDOWN_KEY, cooldown - 1);
            } else if (player.isShiftKeyDown() && !player.onGround()
                    && player.getDeltaMovement().y < -0.1D) {
                player.setDeltaMovement(player.getDeltaMovement().x, 0.55D, player.getDeltaMovement().z);
                player.hasImpulse = true;
                data.putInt(JUMP_COOLDOWN_KEY, DOUBLE_JUMP_COOLDOWN);
                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(),
                            player.getZ(), 12, 0.3D, 0.1D, 0.3D, 0.02D);
                }
            }
        }

        // 熔管重甲 4 件：泡熔岩时 8s 触发一次 3s 抗火
        if (ArmorSetBonus.tubeHeavy(player) >= 4) {
            int cooldown = data.getInt(LAVA_COOLDOWN_KEY);
            if (cooldown > 0) {
                data.putInt(LAVA_COOLDOWN_KEY, cooldown - 1);
            } else if (player.getInBlockState().is(Blocks.LAVA)) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, false, false, true));
                data.putInt(LAVA_COOLDOWN_KEY, LAVA_STEP_COOLDOWN);
            }
        }

        // Curios modules (6.4): acid coating / air bag / filter
        if (com.suwako.northstarplanetaddon.venus.item.ModuleItem.isCarrying(player, com.suwako.northstarplanetaddon.venus.registry.ModItems.ACID_COATING.get())) {
            player.getPersistentData().putInt(CORROSION_KEY,
                    Math.max(0, player.getPersistentData().getInt(CORROSION_KEY) - 1));
        }
        if (com.suwako.northstarplanetaddon.venus.item.ModuleItem.isCarrying(player, com.suwako.northstarplanetaddon.venus.registry.ModItems.AIR_BAG.get())
                && !player.onGround() && player.getDeltaMovement().y < -0.3D) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false, false));
        }
        if (com.suwako.northstarplanetaddon.venus.item.ModuleItem.isCarrying(player, com.suwako.northstarplanetaddon.venus.registry.ModItems.FILTER.get())
                && player.hasEffect(MobEffects.BLINDNESS)) {
            player.removeEffect(MobEffects.BLINDNESS);
        }
        // Pressure rating (12.4): venus surface is 100 atm. Suits + module raise it.
        if (player.level().dimension().location().equals(VENUS)
                && com.suwako.northstarplanetaddon.venus.combat.ArmorSetBonus.pressureRating(player) < 100
                && player.tickCount % 400 == 0) {
            player.hurt(player.damageSources().generic(), 1.0F);
        }
        // 启明星 2 件：露天时每 5s 回 1 点护盾（上限 20）
        if (ArmorSetBonus.phosphoros(player) >= 2 && player.tickCount % 100 == 0
                && player.level().canSeeSky(player.blockPosition())
                && player.getAbsorptionAmount() < 20.0F) {
            player.setAbsorptionAmount(Math.min(20.0F, player.getAbsorptionAmount() + 1.0F));
        }

        // 启明星 4 件：每 30s 清 1 层腐蚀 + 免疫失明
        if (ArmorSetBonus.phosphoros(player) >= 4) {
            if (player.hasEffect(MobEffects.BLINDNESS)) {
                player.removeEffect(MobEffects.BLINDNESS);
            }
            if (player.tickCount % 600 == 0) {
                CompoundTag tag = player.getPersistentData();
                tag.putInt(CORROSION_KEY, Math.max(0, tag.getInt(CORROSION_KEY) - 20));
            }
        }
    }

    // ================================================================== 腐蚀
    /** 酸雨：腐蚀 +2/s 并造成 1 点酸蚀伤害（走 {@code #northstar_planet_addon:acid} 标签）。 */
    private static void tickCorrosion(Player player) {
        CompoundTag data = player.getPersistentData();
        int corrosion = data.getInt(CORROSION_KEY);
        boolean exposed = PhosphorosConfig.ENABLE_ACID_RAIN.get() && isAcidExposed(player);

        if (exposed) {
            corrosion = Math.min(100, corrosion + 2);
            player.hurt(ModDamageSources.acid(player.level(), null), 1.0F);
        } else {
            corrosion = Math.max(0, corrosion - 1);
        }

        data.putInt(CORROSION_KEY, corrosion);

        if (corrosion > 0) {
            int amplifier = Math.min(4, corrosion / 20);
            player.addEffect(new MobEffectInstance(ModEffects.CORROSION, 60, amplifier, false, false, true));
        }
    }

    // ================================================================== 云海
    private static void tickCloudVoid(Player player) {
        if (!player.level().dimension().location().equals(VenusModule.VENUS_CLOUDS)) {
            return;
        }
        if (player.getY() >= PhosphorosConfig.CLOUD_VOID_Y.get()) {
            return;
        }
        player.teleportTo(player.getX(), 200.0D, player.getZ());
        float damage = (float) (player.getMaxHealth() * PhosphorosConfig.CLOUD_VOID_DAMAGE_RATIO.get());
        player.hurt(player.damageSources().generic(), damage);
    }

    private static boolean isAcidExposed(Player player) {
        Level level = player.level();
        if (!level.isRainingAt(player.blockPosition())) {
            return false;
        }
        ResourceLocation dimension = level.dimension().location();
        return dimension.equals(VenusModule.VENUS_CLOUDS) || dimension.equals(VENUS);
    }

    private PlayerEvents() {
    }
}
