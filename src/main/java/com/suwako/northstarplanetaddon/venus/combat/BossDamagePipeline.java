package com.suwako.northstarplanetaddon.venus.combat;

import com.suwako.northstarplanetaddon.venus.config.PhosphorosConfig;
import com.suwako.northstarplanetaddon.venus.entity.boss.AbstractBossEntity;
import com.suwako.northstarplanetaddon.venus.item.BallisticCalibratorItem;
import com.suwako.northstarplanetaddon.venus.registry.ModItems;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * 护甲板 / 本体伤害管线（设计草案 2.3 节，务必按此顺序执行）。
 *
 * <pre>
 * 1. 相位切换 / 无敌 → 取消
 * 2. 分类
 * 3. 乘数（护甲板 / 本体）
 * 4. 单次上限 R3
 * 5. 每人 20 tick 滑动窗口 R4
 * 6. 结算：先扣护甲板，溢出 30% 进本体
 * 7. 弱点 / 过载累积
 * 8. 固定减伤后返回给事件的最终本体伤害
 * </pre>
 *
 * <p>关键点：TaCZ 的 {@code tacz:bullet_ignore_armor} 会绕过原版护甲，
 * 所以限制只能放在 {@code LivingIncomingDamageEvent} 这一层（见 {@code CombatEvents}）。</p>
 */
public final class BossDamagePipeline {

    /** 弱点命中额外过载值：近战 0.5、其它 0.7、弹道 1.0（第 2.4 节）。 */
    private static final double OVERLOAD_MELEE = 0.5D;
    private static final double OVERLOAD_RANGED = 1.0D;
    private static final double OVERLOAD_OTHER = 0.7D;

    /** 酸蚀矛对护甲板 ×1.2（第 6.2 节）。 */
    private static final float ACID_SPEAR_PLATE_BONUS = 1.2F;

    /**
     * @return 应当作用到本体生命值上的最终伤害；0 表示整次伤害被吸收
     */
    public static float process(AbstractBossEntity boss, DamageSource source, float amount) {
        DamageKind kind = DamageClassifier.classify(source);
        LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
        Player player = attacker instanceof Player p ? p : null;

        // ------------------------------------------------ 1. 相位切换 / 无敌
        if (boss.isDamageImmune()) {
            boss.onImmuneHit(source, amount);
            boss.setLastTrace(DamageTrace.blocked(kind, amount, "immune/transition"));
            return 0.0F;
        }

        // ------------------------------------------------ 2. 环境伤害完全免疫
        if (kind == DamageKind.ENVIRONMENT) {
            boss.setLastTrace(DamageTrace.blocked(kind, amount, "environment"));
            return 0.0F;
        }

        // ------------------------------------------------ 3. 乘数
        boolean hasPlate = boss.hasLivingPlate();
        float plateMultiplier = kind.plateMultiplier();
        if (kind == DamageKind.MELEE && attacker != null
                && attacker.getMainHandItem().is(ModItems.ACIDIC_SPEAR.get())) {
            plateMultiplier *= ACID_SPEAR_PLATE_BONUS;
        }
        if (kind == DamageKind.BALLISTIC && player != null
                && BallisticCalibratorItem.isCarriedBy(player)) {
            plateMultiplier *= (float) (1.0D + BallisticCalibratorItem.PLATE_DAMAGE_BONUS);
        }
        if (hasPlate && boss.isOverloaded()) {
            plateMultiplier *= (float) (double) PhosphorosConfig.OVERLOAD_PLATE_BONUS.get();
        }

        float plateDamage;
        float coreDamage;
        if (hasPlate) {
            // R1：护甲板未破时，本体只吃 10% × 本体乘数
            plateDamage = amount * plateMultiplier;
            coreDamage = amount * 0.10F * kind.coreMultiplier();
        } else {
            // R5：本体只在窗口期全额，窗口外 5% 刮痧
            plateDamage = 0.0F;
            float windowFactor = boss.isWindowOpen() ? 1.0F : 0.05F;
            coreDamage = amount * kind.coreMultiplier() * windowFactor;
        }

        // ------------------------------------------------ 4. 单次上限 R3
        float cap = kind.hitCap();
        plateDamage = Math.min(plateDamage, cap);
        coreDamage = Math.min(coreDamage, cap);

        // ------------------------------------------------ 5. 每人滑动窗口 R4
        String note = hasPlate ? "plate" : (boss.isWindowOpen() ? "window" : "closed");
        if (player != null) {
            float requested = plateDamage + coreDamage;
            float allowed = boss.consumeDpsBudget(player.getUUID(), kind, requested);
            if (allowed < requested && requested > 1.0E-4F) {
                float scale = allowed / requested;
                plateDamage *= scale;
                coreDamage *= scale;
                note += "+dpsCapped";
            }
        } else if (kind.perPlayerDpsCap() <= 0.0F) {
            boss.setLastTrace(DamageTrace.blocked(kind, amount, "noSource"));
            return 0.0F;
        }

        // ------------------------------------------------ 6. 结算：先扣护甲板
        boolean hadPlate = hasPlate;
        float absorbed = plateDamage > 0.0F ? boss.damagePlates(plateDamage) : 0.0F;
        if (plateDamage > absorbed) {
            // 溢出伤害的 30% 进入本体
            coreDamage += (plateDamage - absorbed) * 0.30F;
        }
        if (hadPlate && !boss.hasLivingPlate()) {
            // 最后一块板被打碎：立刻开窗（第 2.4 节职责 C 的收益）
            boss.openWindow(60);
            note += "+plateBreak";
        }

        // ------------------------------------------------ 7. 弱点 / 过载
        Vec3 hitPosition = hitPosition(source, attacker);
        if (hitPosition != null && boss.tryHitWeakPoint(hitPosition, attacker, kind)) {
            note += "+weakPoint";
        }

        // ------------------------------------------------ 8. 固定减伤（跳过原版护甲）
        float reduction = (float) (double) PhosphorosConfig.BOSS_FIXED_DAMAGE_REDUCTION.get();
        coreDamage *= (1.0F - reduction);

        boss.setLastTrace(new DamageTrace(kind, amount, absorbed, coreDamage, note));
        return Math.max(0.0F, coreDamage);
    }

    /**
     * 命中位置：优先用弹丸 / 直接实体的位置（散弹多弹丸各自判定），
     * 其次用攻击者眼睛位置（近战）。
     */
    @Nullable
    private static Vec3 hitPosition(DamageSource source, @Nullable LivingEntity attacker) {
        Entity direct = source.getDirectEntity();
        if (direct != null) {
            return direct.position();
        }
        return attacker != null ? attacker.getEyePosition() : null;
    }

    private BossDamagePipeline() {
    }
}
