package com.suwako.northstarplanetaddon.venus.combat;

import com.suwako.northstarplanetaddon.venus.registry.ModTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 伤害分类器（设计草案 12.2 节）。
 *
 * <p>刻意不 import 任何 TaCZ 类：TaCZ 的伤害类型通过 {@code data/phosphoros/tags/damage_type/ballistic.json}
 * 归入标签，枪包（含三角洲包）只改数值不改类型，所以一个标签覆盖所有枪包。</p>
 */
public final class DamageClassifier {

    /** 分类顺序与文档 2.3 节一致，不要随意调整：弹道优先于「直接实体是生物」的判断。 */
    public static DamageKind classify(DamageSource source) {
        if (source.is(ModTags.DamageTypes.BALLISTIC)) {
            return DamageKind.BALLISTIC;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            return DamageKind.EXPLOSIVE;
        }
        Entity direct = source.getDirectEntity();
        if (direct instanceof LivingEntity) {
            return DamageKind.MELEE;
        }
        if (source.is(ModTags.DamageTypes.ACID)) {
            return DamageKind.ACID;
        }
        if (isEnvironment(source)) {
            return DamageKind.ENVIRONMENT;
        }
        return DamageKind.OTHER;
    }

    /**
     * 环境伤害：没有任何实体来源（摔落 / 虚空 / 液体 / 窒息），
     * 或者命中 IS_FALL / IS_DROWNING / IS_FREEZING 标签。
     */
    public static boolean isEnvironment(DamageSource source) {
        if (source.is(DamageTypeTags.IS_FALL)
                || source.is(DamageTypeTags.IS_DROWNING)
                || source.is(DamageTypeTags.IS_FREEZING)
                || source.is(DamageTypeTags.IS_LIGHTNING)) {
            return true;
        }
        return source.getDirectEntity() == null && source.getEntity() == null;
    }

    private DamageClassifier() {
    }
}
