package com.suwako.northstarplanetaddon.venus.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 对应设计草案第 13 章 {@code config/phosphoros-common.toml}。
 *
 * <p>所有伤害管线数值都可热调：管线在每次结算时读取本类字段，
 * 因此改配置后不需要重进游戏（保存配置即可生效）。</p>
 */
public final class PhosphorosConfig {

    public static final ModConfigSpec SPEC;

    // ------------------------------------------------------------------ combat
    public static final ModConfigSpec.DoubleValue BALLISTIC_PLATE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue BALLISTIC_CORE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue MELEE_PLATE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue MELEE_CORE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue EXPLOSIVE_PLATE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue EXPLOSIVE_CORE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue ACID_PLATE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue ACID_CORE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue OTHER_PLATE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue OTHER_CORE_MULTIPLIER;

    public static final ModConfigSpec.DoubleValue HIT_CAP_BALLISTIC;
    public static final ModConfigSpec.DoubleValue HIT_CAP_MELEE;
    public static final ModConfigSpec.DoubleValue HIT_CAP_EXPLOSIVE;
    public static final ModConfigSpec.DoubleValue HIT_CAP_ACID;

    public static final ModConfigSpec.DoubleValue PER_PLAYER_DPS_CAP_BALLISTIC;
    public static final ModConfigSpec.DoubleValue PER_PLAYER_DPS_CAP_MELEE;
    public static final ModConfigSpec.DoubleValue PER_PLAYER_DPS_CAP_OTHER;
    public static final ModConfigSpec.IntValue DPS_WINDOW_TICKS;
    public static final ModConfigSpec.DoubleValue BOSS_HP_PER_EXTRA_PLAYER;
    public static final ModConfigSpec.DoubleValue BOSS_FIXED_DAMAGE_REDUCTION;

    public static final ModConfigSpec.DoubleValue WEAK_POINT_RADIUS;
    public static final ModConfigSpec.IntValue WEAK_POINT_COOLDOWN_TICKS;
    public static final ModConfigSpec.DoubleValue OVERLOAD_POINTS;
    public static final ModConfigSpec.IntValue OVERLOAD_DURATION_TICKS;
    public static final ModConfigSpec.IntValue OVERLOAD_INTERNAL_COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue OVERLOAD_WINDOW_TICKS;
    public static final ModConfigSpec.DoubleValue OVERLOAD_PLATE_BONUS;

    // ------------------------------------------------------------------ world
    public static final ModConfigSpec.BooleanValue ENABLE_ACID_RAIN;
    public static final ModConfigSpec.IntValue CLOUD_VOID_Y;
    public static final ModConfigSpec.DoubleValue CLOUD_VOID_DAMAGE_RATIO;
    public static final ModConfigSpec.IntValue MONSOON_INTERVAL_DAYS;

    // ------------------------------------------------------------------ compat
    public static final ModConfigSpec.BooleanValue TACZ_INTEGRATION;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("第 2 章 反枪械设计宪章：伤害乘数 / 单次上限 / 每人 DPS 上限").push("combat");

        BALLISTIC_PLATE_MULTIPLIER = b
                .comment("R2 弹道对护甲板乘数")
                .defineInRange("ballisticPlateMultiplier", 0.05D, 0.0D, 1.0D);
        BALLISTIC_CORE_MULTIPLIER = b
                .comment("R2 弹道对本体乘数")
                .defineInRange("ballisticCoreMultiplier", 0.10D, 0.0D, 1.0D);
        MELEE_PLATE_MULTIPLIER = b
                .comment("近战对护甲板乘数")
                .defineInRange("meleePlateMultiplier", 1.0D, 0.0D, 5.0D);
        MELEE_CORE_MULTIPLIER = b
                .comment("近战对本体乘数")
                .defineInRange("meleeCoreMultiplier", 1.0D, 0.0D, 5.0D);
        EXPLOSIVE_PLATE_MULTIPLIER = b
                .comment("爆炸对护甲板乘数")
                .defineInRange("explosivePlateMultiplier", 1.2D, 0.0D, 5.0D);
        EXPLOSIVE_CORE_MULTIPLIER = b
                .defineInRange("explosiveCoreMultiplier", 1.2D, 0.0D, 5.0D);
        ACID_PLATE_MULTIPLIER = b
                .comment("酸蚀对护甲板乘数")
                .defineInRange("acidPlateMultiplier", 0.8D, 0.0D, 5.0D);
        ACID_CORE_MULTIPLIER = b
                .defineInRange("acidCoreMultiplier", 0.8D, 0.0D, 5.0D);
        OTHER_PLATE_MULTIPLIER = b
                .comment("未分类伤害（普通箭矢、环境等）乘数")
                .defineInRange("otherPlateMultiplier", 0.5D, 0.0D, 5.0D);
        OTHER_CORE_MULTIPLIER = b
                .defineInRange("otherCoreMultiplier", 0.6D, 0.0D, 5.0D);

        HIT_CAP_BALLISTIC = b.comment("R3 单次伤害上限（弹道）").defineInRange("hitCapBallistic", 9.0D, 0.0D, 1000.0D);
        HIT_CAP_MELEE = b.comment("R3 单次伤害上限（近战）").defineInRange("hitCapMelee", 40.0D, 0.0D, 1000.0D);
        HIT_CAP_EXPLOSIVE = b.comment("R3 单次伤害上限（爆炸）").defineInRange("hitCapExplosive", 30.0D, 0.0D, 1000.0D);
        HIT_CAP_ACID = b.comment("R3 单次伤害上限（酸）").defineInRange("hitCapAcid", 20.0D, 0.0D, 1000.0D);

        PER_PLAYER_DPS_CAP_BALLISTIC = b.comment("R4 每人每秒伤害上限（弹道）")
                .defineInRange("perPlayerDpsCapBallistic", 13.0D, 0.0D, 1000.0D);
        PER_PLAYER_DPS_CAP_MELEE = b.comment("R4 每人每秒伤害上限（近战）")
                .defineInRange("perPlayerDpsCapMelee", 45.0D, 0.0D, 1000.0D);
        PER_PLAYER_DPS_CAP_OTHER = b.comment("R4 每人每秒伤害上限（其它）")
                .defineInRange("perPlayerDpsCapOther", 30.0D, 0.0D, 1000.0D);
        DPS_WINDOW_TICKS = b.comment("R4 滑动窗口长度（tick）").defineInRange("dpsWindowTicks", 20, 1, 200);
        BOSS_HP_PER_EXTRA_PLAYER = b.comment("多人缩放：每多一名玩家增加的 HP 比例")
                .defineInRange("bossHpPerExtraPlayer", 0.5D, 0.0D, 10.0D);
        BOSS_FIXED_DAMAGE_REDUCTION = b.comment("Boss 固定减伤（管线第 8 步，跳过原版护甲减免）")
                .defineInRange("bossFixedDamageReduction", 0.2D, 0.0D, 0.95D);

        WEAK_POINT_RADIUS = b.comment("弱点判定半径（格）").defineInRange("weakPointRadius", 1.2D, 0.1D, 16.0D);
        WEAK_POINT_COOLDOWN_TICKS = b.comment("同一点位内置冷却（tick）")
                .defineInRange("weakPointCooldownTicks", 5, 1, 100);
        OVERLOAD_POINTS = b.comment("打满弱点所需过载值").defineInRange("overloadPoints", 8.0D, 1.0D, 100.0D);
        OVERLOAD_DURATION_TICKS = b.comment("过载持续时间（tick）")
                .defineInRange("overloadDurationTicks", 120, 1, 2400);
        OVERLOAD_INTERNAL_COOLDOWN_TICKS = b.comment("两次过载的最小间隔（tick，默认 60s）")
                .defineInRange("overloadInternalCooldownTicks", 1200, 1, 24000);
        OVERLOAD_WINDOW_TICKS = b.comment("过载时强制开窗时长（tick）")
                .defineInRange("overloadWindowTicks", 80, 0, 2400);
        OVERLOAD_PLATE_BONUS = b.comment("过载期间护甲板受伤加成")
                .defineInRange("overloadPlateBonus", 1.5D, 1.0D, 10.0D);
        b.pop();

        b.comment("第 9 章 云海维度与硫酸季风").push("world");
        ENABLE_ACID_RAIN = b.comment("是否启用酸雨腐蚀累积").define("enableAcidRain", true);
        CLOUD_VOID_Y = b.comment("云海维度 y 低于该值视为掉出岛屿").defineInRange("cloudVoidY", 60, -64, 320);
        CLOUD_VOID_DAMAGE_RATIO = b.comment("掉出岛屿扣血比例（最大生命值百分比）")
                .defineInRange("cloudVoidDamageRatio", 0.2D, 0.0D, 1.0D);
        MONSOON_INTERVAL_DAYS = b.comment("硫酸季风周期（游戏日），气压计用它预报下一次季风")
                .defineInRange("monsoonIntervalDays", 5, 1, 60);
        b.pop();

        b.comment("联动").push("compat");
        TACZ_INTEGRATION = b.comment("启用 TaCZ 弹道伤害标签识别（反射，不硬依赖）")
                .define("taczIntegration", true);
        b.pop();

        SPEC = b.build();
    }

    private PhosphorosConfig() {
    }
}
