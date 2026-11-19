package com.suwako.northstarplanetaddon.mars.event;

import com.suwako.northstarplanetaddon.NorthstarPlanetAddon;

import com.suwako.northstarplanetaddon.mars.MarsModule;
import com.suwako.northstarplanetaddon.mars.registry.ModItems;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 熔核护甲四件套的套装效果（《Boss属性重新设计》）。
 *
 * <table>
 *   <tr><th>部位</th><th>效果</th><th>实现</th></tr>
 *   <tr><td>头盔</td><td>夜视（无限时长）</td><td>戴上即挂 {@code INFINITE_DURATION} 夜视、摘下即除（穿脱瞬间生效，见 {@link #onEquipmentChange}）</td></tr>
 *   <tr><td>胸甲</td><td>减伤 20%</td><td>{@code LivingIncomingDamageEvent} 里把伤害 ×0.8</td></tr>
 *   <tr><td>护腿</td><td>速度 +20%</td><td>移速属性 {@code ADD_MULTIPLIED_BASE 0.2} 修饰符</td></tr>
 *   <tr><td>靴子</td><td>摔落伤害免疫</td><td>取消 {@code #minecraft:is_fall} 伤害</td></tr>
 *   <tr><td>全套</td><td>火焰免疫 + 岩浆行走 + 免疫沙尘暴磨损</td>
 *       <td>取消 {@code #minecraft:is_fire}（该标签含 {@code minecraft:lava}）+ 每 tick 清火；
 *           沙尘暴磨损的豁免在 {@link PlayerEvents} 里用 {@link #wearsFullSet(Player)} 判断</td></tr>
 * </table>
 *
 * <p><b>「岩浆行走」的口径</b>：本模组实现为「全套时免疫岩浆伤害与着火，可以在岩浆里自由行动」
 * （{@code #minecraft:is_fire} 覆盖 {@code minecraft:lava} 与 {@code minecraft:hot_floor}）。
 * 没有做「把岩浆变成方块」那一类改地形实现 —— 那是破坏性的，且会与 Create 的流体系统打架。</p>
 */
@EventBusSubscriber(modid = NorthstarPlanetAddon.MODID)
public final class ArmorEvents {

    /** 护腿的移速加成：+20%（以基础值为基准）。 */
    private static final AttributeModifier LEGGINGS_SPEED = new AttributeModifier(
            MarsModule.id("molten_core_leggings_speed"), 0.2D,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

    /** 胸甲减伤：受到伤害 ×0.8。 */
    private static final float CHESTPLATE_DAMAGE_MULTIPLIER = 0.8F;

    /**
     * 头盔的夜视：**无限时长**（{@link MobEffectInstance#INFINITE_DURATION} = -1），戴上即有、摘下即除。
     *
     * <p>v1.13：以前是「每 200 tick 续一次 300 tick」—— 虽然事实上也常驻，但那是靠不断续期堆出来的，
     * HUD 上还会看到倒计时在跳。现在直接挂无限时长。</p>
     *
     * <p>摘下时只清**无限时长**的那一条：玩家自己喝的夜视药水是有限时长，不能被连带删掉
     * （唯一会被误删的情况是别人用指令直接给了无限夜视，README 已写明）。</p>
     */
    private static void updateHelmetNightVision(Player player) {
        MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
        if (wears(player, EquipmentSlot.HEAD)) {
            if (current == null || !current.isInfiniteDuration() || current.getAmplifier() != 0) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,
                        MobEffectInstance.INFINITE_DURATION, 0, true, false, false));
            }
        } else if (current != null && current.isInfiniteDuration()) {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    /** 穿脱头盔时立刻生效，不必等下一个 tick。 */
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide
                || event.getSlot() != EquipmentSlot.HEAD) {
            return;
        }
        updateHelmetNightVision(player);
    }

    public static boolean wears(Player player, EquipmentSlot slot) {
        return player.getItemBySlot(slot).is(moltenCoreItemFor(slot));
    }

    /** 是否穿齐熔核护甲四件套。 */
    public static boolean wearsFullSet(Player player) {
        return wears(player, EquipmentSlot.HEAD) && wears(player, EquipmentSlot.CHEST)
                && wears(player, EquipmentSlot.LEGS) && wears(player, EquipmentSlot.FEET);
    }

    private static net.minecraft.world.item.Item moltenCoreItemFor(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> ModItems.MOLTEN_CORE_HELMET.get();
            case CHEST -> ModItems.MOLTEN_CORE_CHESTPLATE.get();
            case LEGS -> ModItems.MOLTEN_CORE_LEGGINGS.get();
            case FEET -> ModItems.MOLTEN_CORE_BOOTS.get();
            default -> ItemStack.EMPTY.getItem();
        };
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }

        // 头盔：无限夜视（同时兜住「带着头盔进服 / 复活 / 被牛奶洗掉」这些情况）
        updateHelmetNightVision(player);

        // 护腿：移速 +20%
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            if (wears(player, EquipmentSlot.LEGS)) {
                speed.addOrUpdateTransientModifier(LEGGINGS_SPEED);
            } else {
                speed.removeModifier(LEGGINGS_SPEED.id());
            }
        }

        // 全套：不烧起来（fireImmune 对玩家不成立，只能自己清）
        if (wearsFullSet(player)) {
            player.clearFire();
            player.setRemainingFireTicks(0);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.isCreative()) {
            return;
        }
        // 全套：火焰 / 岩浆 / 热地面免疫（#minecraft:is_fire 含 lava 与 hot_floor）
        if (wearsFullSet(player) && event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true);
            return;
        }
        // 靴子：摔落伤害免疫（#minecraft:is_fall 含 fall 与 stalagmite）
        if (wears(player, EquipmentSlot.FEET) && event.getSource().is(DamageTypeTags.IS_FALL)) {
            event.setCanceled(true);
            return;
        }
        // 胸甲：减伤 20%
        if (wears(player, EquipmentSlot.CHEST)) {
            event.setAmount(event.getAmount() * CHESTPLATE_DAMAGE_MULTIPLIER);
        }
    }

    private ArmorEvents() {
    }
}
