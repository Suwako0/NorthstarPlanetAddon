package com.suwako.northstarplanetaddon.mercury.world;

/**
 * Every tunable number of the solar storm, gathered in one place.
 * <p>
 * The storm is a weather, not a scripted event: it has a natural trigger roll, a duration, a
 * scorching pass over anything standing in the open and a meteorite schedule. Tuning the storm means
 * editing this class and rebuilding.
 */
public final class SolarStormSettings {

    // ---------------------------------------------------------------- 自然触发

    /** 自然触发的判定间隔（游戏刻）。每 60 秒掷一次骰子。 */
    public static final int NATURAL_CHECK_INTERVAL = 1200;
    /** 每次判定的概率：0.02 ≈ 玩家在水星上时的每分钟 2%，即平均约 50 分钟来一场。 */
    public static final float NATURAL_CHANCE = 0.02F;
    /** 自然触发的风暴时长下限／上限（游戏刻）：2 ~ 6 分钟。 */
    public static final int NATURAL_MIN_DURATION = 2400;
    public static final int NATURAL_MAX_DURATION = 7200;

    /** 指令不填秒数时的默认时长：5 分钟。 */
    public static final int DEFAULT_COMMAND_DURATION = 6000;

    // ---------------------------------------------------------------- 辐射灼烧

    /** 灼烧判定间隔（游戏刻）：每秒一次。 */
    public static final int SCORCH_INTERVAL = 20;
    /** 每次判定的伤害。露天且无遮蔽的实体每秒吃这么多。 */
    public static final float SCORCH_DAMAGE = 3.0F;
    /** 每次判定附加的燃烧秒数。 */
    public static final int SCORCH_FIRE_SECONDS = 6;
    /** 露天玩家每次灼烧判定获得的迟缓持续时间（游戏刻）。 */
    public static final int SCORCH_SLOW_DURATION = 40;
    /** 迟缓效果等级；0 对应迟缓 I。 */
    public static final int SCORCH_SLOW_AMPLIFIER = 0;

    // ---------------------------------------------------------------- 陨石

    /** 风暴开始后到第一颗陨石的延迟（游戏刻），给玩家反应时间。 */
    public static final int METEOR_FIRST_DELAY = 200;
    /** 两次陨石之间的间隔范围（游戏刻）：13 ~ 35 秒。陨石是重事件，刻意给得稀疏。 */
    public static final int METEOR_MIN_INTERVAL = 260;
    public static final int METEOR_MAX_INTERVAL = 700;
    /** 一次齐射最多几颗陨石（实际为 1 ~ 该值）。1 = 每次只来一颗。 */
    public static final int METEOR_MAX_PER_VOLLEY = 1;
    /** 陨石落点与玩家的水平距离范围。太近会砸到脚，太远看不见。 */
    public static final double METEOR_MIN_DISTANCE = 20.0D;
    public static final double METEOR_MAX_DISTANCE = 60.0D;
    /** 生成高度 = 世界最高可建造高度 - 该值；保证玩家有足够的预警时间。 */
    public static final int METEOR_SPAWN_HEIGHT_BELOW_TOP = 100;
    /** 生成时的初速度（格/刻）。重力会把它加速到终端速度。 */
    public static final double METEOR_INITIAL_SPEED = 1.4D;

    // ---------------------------------------------------------------- 撞击

    /** 撞击伤害的作用半径。 */
    public static final double IMPACT_RADIUS = 5.0D;
    /** 撞击伤害。 */
    public static final float IMPACT_DAMAGE = 10.0F;
    /** 撞击附加的燃烧秒数。 */
    public static final int IMPACT_FIRE_SECONDS = 5;
    /** 撞击的水平击退力度。 */
    public static final double IMPACT_KNOCKBACK = 0.55D;

    /** 陨石坑的水平半径范围：小坑，主要靠粒子与音效撑场面。 */
    public static final int CRATER_MIN_RADIUS = 2;
    public static final int CRATER_MAX_RADIUS = 3;
    /** 陨石坑的深度范围（比水平半径小，坑是浅碟形而不是深井）。 */
    public static final int CRATER_MIN_DEPTH = 2;
    public static final int CRATER_MAX_DEPTH = 3;
    /** 坑底每个格子生成矿石的概率。 */
    public static final float ORE_CHANCE = 0.08F;
    /** 一发陨石最多铺多少块矿石，避免陨石坑变成矿洞。 */
    public static final int ORE_MAX_PER_CRATER = 4;
    /** 坑底改成岩浆块的概率（陨石坑自带余温与光源）。 */
    public static final float MAGMA_CHANCE = 0.08F;
    /** 坑底改成水星沙的概率；其余保持水星石。 */
    public static final float SAND_CHANCE = 0.30F;

    // ---------------------------------------------------------------- 表现与同步

    /** 向客户端重发风暴状态的心跳间隔（游戏刻）。5 秒足够覆盖迟到进服与维度切换。 */
    public static final int SYNC_INTERVAL = 100;
    /** 风暴雾效：橙红色雾幕与压缩后的可视距离。 */
    public static final float FOG_RED = 0.82F;
    public static final float FOG_GREEN = 0.18F;
    public static final float FOG_BLUE = 0.05F;
    public static final float FOG_FAR_PLANE_SCALE = 0.48F;
    /** 每客户端刻生成的橙红色雾尘粒子数。 */
    public static final int FOG_PARTICLES_PER_TICK = 5;
    /** 镜头在风暴中的晃动幅度（度）。只有被风暴直射（头顶无遮蔽）时才生效。 */
    public static final float CAMERA_SWAY_DEGREES = 0.7F;
    /** 重新判定"是否被直射"的间隔（游戏刻）。0.2 秒一次，够跟脚又不至于每刻翻高度图。 */
    public static final int EXPOSURE_CHECK_INTERVAL = 4;
    /**
     * 暴露程度每刻的变化上限。走进/走出掩体时镜头晃动是淡入淡出的，
     * 0.08 ≈ 12 刻（0.6 秒）完成过渡，避免站在屋檐边缘时画面抽搐。
     */
    public static final float EXPOSURE_FADE_PER_TICK = 0.08F;
    /** 两次高空耀斑（闪光 + 雷声）之间的间隔范围（游戏刻）。 */
    public static final int FLARE_MIN_INTERVAL = 80;
    public static final int FLARE_MAX_INTERVAL = 220;

    private SolarStormSettings() {
    }
}
