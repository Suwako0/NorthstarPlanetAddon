package com.suwako.northstarplanetaddon.venus.event;

import com.suwako.northstarplanetaddon.venus.combat.ArmorSetBonus;
import com.suwako.northstarplanetaddon.venus.combat.BossDamagePipeline;
import com.suwako.northstarplanetaddon.venus.combat.DamageClassifier;
import com.suwako.northstarplanetaddon.venus.combat.DamageKind;
import com.suwako.northstarplanetaddon.venus.entity.boss.AbstractBossEntity;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 战斗事件入口。
 *
 * <p>第 2 章最关键的一行：所有 Boss 伤害都必须经过 {@link BossDamagePipeline}。
 * 普通生物直接放行（性能：事件里只做一次 instanceof 判断）。</p>
 *
 * <p>玩家侧套装减伤也在这里（第 6.1 节）：</p>
 * <ul>
 *   <li>钨钢压力服 2 件：酸蚀 −30%；4 件：再 −20%（草案的「氧气 −25%」无氧气系统时用抗性代替）</li>
 *   <li>云行者 2 件：雷击 −50%</li>
 *   <li>熔管重甲 2 件：岩浆 / 酸蚀 −50%</li>
 * </ul>
 */
public final class CombatEvents {

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof AbstractBossEntity boss)) {
            return;
        }
        float result = BossDamagePipeline.process(boss, event.getSource(), event.getAmount());
        if (result <= 0.0F) {
            event.setAmount(0.0F);
            event.setCanceled(true);
            return;
        }
        event.setAmount(result);
    }

    @SubscribeEvent
    public static void onPlayerSetBonusDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        DamageKind kind = DamageClassifier.classify(event.getSource());
        float multiplier = 1.0F;

        int tungsten = ArmorSetBonus.tungsten(player);
        if (kind == DamageKind.ACID) {
            if (tungsten >= 2) {
                multiplier *= 0.7F;
            }
            if (tungsten >= 4) {
                multiplier *= 0.8F;
            }
            if (ArmorSetBonus.tubeHeavy(player) >= 2) {
                multiplier *= 0.5F;
            }
        }
        if (event.getSource().is(DamageTypeTags.IS_LIGHTNING) && ArmorSetBonus.cloudWalker(player) >= 2) {
            multiplier *= 0.5F;
        }
        if (event.getSource().is(DamageTypeTags.IS_FIRE) && ArmorSetBonus.tubeHeavy(player) >= 2) {
            multiplier *= 0.5F;
        }
        if (multiplier < 1.0F) {
            event.setAmount(event.getAmount() * multiplier);
        }
    }

    private CombatEvents() {
    }
}
