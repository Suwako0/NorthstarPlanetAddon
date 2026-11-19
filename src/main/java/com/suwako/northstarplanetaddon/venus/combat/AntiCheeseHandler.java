package com.suwako.northstarplanetaddon.venus.combat;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;

/**
 * 反逃课（设计草案 2.6 节）。
 *
 * <p>这里只处理「伤害层面」的免疫；推挤 / 载具 / 拴绳 / 掉出场地由
 * {@link com.suwako.northstarplanetaddon.venus.entity.boss.AbstractBossEntity} 的实体方法覆盖与
 * {@code PlayerEvents} 的云海传送兜底。</p>
 *
 * <p>注意不拦截 {@link DamageTypeTags#BYPASSES_INVULNERABILITY}（{@code /kill}）：
 * 管理员权限本就该能杀。</p>
 */
public final class AntiCheeseHandler {

    public static boolean isBlocked(DamageSource source) {
        return source.is(DamageTypeTags.IS_FALL)
                || source.is(DamageTypeTags.IS_DROWNING)
                || source.is(DamageTypeTags.IS_FREEZING)
                || source.is(DamageTypes.FELL_OUT_OF_WORLD)
                || source.is(DamageTypeTags.IS_LIGHTNING);
    }

    private AntiCheeseHandler() {
    }
}
