package com.suwako.northstarplanetaddon.venus.combat;

import com.suwako.northstarplanetaddon.venus.config.PhosphorosConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 伤害分类（设计草案 2.3 节第 2 步）。
 *
 * <p>每一类各自带一套「护甲板乘数 / 本体乘数 / 单次上限 / 每人 DPS 上限」，
 * 全部从配置读取，因此可以热调（第 13 章）。</p>
 */
public enum DamageKind {

    /** 弹道：TaCZ 的 tacz:bullet* 与 #minecraft:is_projectile。 */
    BALLISTIC,
    /** 近战：任何直接由生物造成的伤害（含枪械刺刀）。 */
    MELEE,
    /** 爆炸：TNT / 火箭 / 榴弹。 */
    EXPLOSIVE,
    /** 酸蚀：本模组的酸液、酸雨、腐蚀伤害。 */
    ACID,
    /** 环境：摔落、窒息、虚空、液体等，Boss 完全免疫。 */
    ENVIRONMENT,
    /** 其它：普通箭矢、魔法等。 */
    OTHER;

    public float plateMultiplier() {
        return switch (this) {
            case BALLISTIC -> value(PhosphorosConfig.BALLISTIC_PLATE_MULTIPLIER);
            case MELEE -> value(PhosphorosConfig.MELEE_PLATE_MULTIPLIER);
            case EXPLOSIVE -> value(PhosphorosConfig.EXPLOSIVE_PLATE_MULTIPLIER);
            case ACID -> value(PhosphorosConfig.ACID_PLATE_MULTIPLIER);
            case OTHER -> value(PhosphorosConfig.OTHER_PLATE_MULTIPLIER);
            case ENVIRONMENT -> 0.0F;
        };
    }

    public float coreMultiplier() {
        return switch (this) {
            case BALLISTIC -> value(PhosphorosConfig.BALLISTIC_CORE_MULTIPLIER);
            case MELEE -> value(PhosphorosConfig.MELEE_CORE_MULTIPLIER);
            case EXPLOSIVE -> value(PhosphorosConfig.EXPLOSIVE_CORE_MULTIPLIER);
            case ACID -> value(PhosphorosConfig.ACID_CORE_MULTIPLIER);
            case OTHER -> value(PhosphorosConfig.OTHER_CORE_MULTIPLIER);
            case ENVIRONMENT -> 0.0F;
        };
    }

    /** R3 单次伤害上限。 */
    public float hitCap() {
        return switch (this) {
            case BALLISTIC -> value(PhosphorosConfig.HIT_CAP_BALLISTIC);
            case MELEE -> value(PhosphorosConfig.HIT_CAP_MELEE);
            case EXPLOSIVE -> value(PhosphorosConfig.HIT_CAP_EXPLOSIVE);
            case ACID, OTHER -> value(PhosphorosConfig.HIT_CAP_ACID);
            case ENVIRONMENT -> 0.0F;
        };
    }

    /** R4 每人每秒伤害上限。 */
    public float perPlayerDpsCap() {
        return switch (this) {
            case BALLISTIC -> value(PhosphorosConfig.PER_PLAYER_DPS_CAP_BALLISTIC);
            case MELEE -> value(PhosphorosConfig.PER_PLAYER_DPS_CAP_MELEE);
            case EXPLOSIVE, ACID, OTHER -> value(PhosphorosConfig.PER_PLAYER_DPS_CAP_OTHER);
            case ENVIRONMENT -> 0.0F;
        };
    }

    /** 是否吃「本体窗口期」惩罚：只有直接打本体的近战 / 爆炸 / 酸需要窗口，弹道本体固定 ×0.10。 */
    public boolean wantsWindow() {
        return this != BALLISTIC && this != ENVIRONMENT;
    }

    public boolean isFirearmLike() {
        return this == BALLISTIC;
    }

    private static float value(ModConfigSpec.DoubleValue config) {
        Double v = config.get();
        return v == null ? 0.0F : v.floatValue();
    }
}
