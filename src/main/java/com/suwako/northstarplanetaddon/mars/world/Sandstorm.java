package com.suwako.northstarplanetaddon.mars.world;

import com.suwako.northstarplanetaddon.mars.MarsModule;
import com.suwako.northstarplanetaddon.mars.network.SandstormPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;

/**
 * 火星沙尘暴（v1.10 重写：不再借用原版天气）。
 *
 * <h2>为什么必须自己实现一套状态</h2>
 * <p>旧实现把沙尘暴等同于「{@code northstar:mars} 维度里原版天气非 clear」，靠
 * {@code level.setWeatherParameters(0, duration, true, intense)} 触发。这条路线在本模组的
 * 目标版本上**根本不成立**，两条都已在 1.21.1 反编译源码里核对过：</p>
 * <ol>
 *   <li>{@code ServerLevel.setWeatherParameters} 只是转调 {@code serverLevelData} 上的
 *       {@code setRainTime/setThunderTime/setRaining/setThundering}；而**非主世界维度**的
 *       {@code serverLevelData} 是 {@code DerivedLevelData}，这四个方法的方法体在 1.21.1 里是
 *       <b>空的</b>（{@code DerivedLevelData.java} 第 94~108 行）。所以在火星调用它等于什么都没做
 *       —— Boss 阶段二的「强化沙尘暴」从来不会真的出现。</li>
 *   <li>{@code DerivedLevelData.isRaining()} 直接代理 {@code wrapped}（**主世界**）。
 *       于是 {@code Sandstorm.isActive(mars)} 实际含义是「主世界在下雨」：
 *       火星上永远等不到，反而在主世界下雨时会让火星玩家平白遭受磨损与雾效。</li>
 * </ol>
 *
 * <h2>现在怎么做</h2>
 * <p>沙尘暴由本模组自己掌管：一个只对 {@code northstar:mars} 有效的状态机
 * （{@link #active} / {@link #intense} / {@link #endGameTime} / {@link #windDegrees}），</p>
 * <ul>
 *   <li><b>自然发生</b>：火星维度里有玩家时，每 {@link #ROLL_INTERVAL} tick 掷一次骰子；
 *       命中则随机时长、随机风向，{@value #INTENSE_CHANCE} 分之一的概率是「强化沙尘暴」；</li>
 *   <li><b>Boss 召唤</b>：{@code AresEntity} 阶段二调用 {@link #trigger}；</li>
 *   <li><b>手动</b>：{@code /northstarmars sandstorm start|intense|stop|status}（方便验收）；</li>
 *   <li><b>同步</b>：开始 / 结束 / 每 20 tick 各广播一次 {@link SandstormPayload}，
 *       玩家登录与跨维度时单独补发，客户端雾效与 HUD 都读这份数据
 *       （客户端侧见 {@code client.ClientSandstormState}）。</li>
 * </ul>
 *
 * <p>本类是**服务端权威**的：{@link #isActive(Level)} 只反映服务端状态，客户端代码不要调用它，
 * 请用 {@code ClientSandstormState}（否则专用服务端会因加载客户端类而崩）。</p>
 */
public final class Sandstorm {

    public static final ResourceLocation MARS = ResourceLocation.fromNamespaceAndPath("northstar", "mars");
    public static final ResourceKey<Level> MARS_KEY = ResourceKey.create(Registries.DIMENSION, MARS);

    /** Boss 召唤时使用的时长（tick）。 */
    public static final int DEFAULT_DURATION = 12000;

    /** 自然沙尘暴的时长范围（tick）：3~10 分钟。 */
    public static final int MIN_NATURAL_DURATION = 3600;
    public static final int MAX_NATURAL_DURATION = 12000;

    /** 掷骰间隔（tick）。 */
    private static final int ROLL_INTERVAL = 200;
    /** 每次掷骰命中概率的倒数 —— 200 tick 一次、1/90 ⇒ 期望约 15 分钟一场。 */
    private static final int ROLL_ODDS = 90;
    /** 强化沙尘暴的比例：1/4。 */
    private static final int INTENSE_CHANCE = 4;
    /** 一场结束后至少间隔多久才可能自然再来一场（tick，1 分钟）。 */
    private static final int COOLDOWN_TICKS = 1200;

    // ---- 状态（只对 northstar:mars 有效，同一服务器只有一个） ----
    private static boolean active;
    private static boolean intense;
    private static long startGameTime;
    private static long endGameTime;
    private static int windDegrees;
    private static long nextRollGameTime;
    private static Origin origin = Origin.NATURAL;

    /**
     * 沙尘暴的来源。
     *
     * <p>为什么要记：<b>只有 Boss 召唤的那一场才应该随 Boss 一起结束</b>。自然发生与指令触发的
     * 沙尘暴跟 Boss 毫无关系，Boss 死了也不该把它们一起收掉（v1.13 的 bug 是反过来的：
     * Boss 召唤的那场在 Boss 死后还挂着 10 分钟）。</p>
     */
    public enum Origin {
        /** 自然发生。 */
        NATURAL,
        /** 阿瑞斯阶段二召唤。 */
        BOSS,
        /** {@code /northstarmars sandstorm} 指令。 */
        COMMAND
    }

    private Sandstorm() {
    }

    // ------------------------------------------------------------------ 查询

    public static boolean isMars(Level level) {
        return level.dimension().location().equals(MARS);
    }

    /** 火星维度的 {@code ServerLevel}，维度未加载时为 {@code null}。 */
    @Nullable
    public static ServerLevel marsLevel(MinecraftServer server) {
        return server.getLevel(MARS_KEY);
    }

    /** 服务端权威状态：当前是否处于沙尘暴。客户端请读 {@code ClientSandstormState}。 */
    public static boolean isActive(Level level) {
        return active && isMars(level);
    }

    /** 是否是强化沙尘暴（Boss 阶段二召唤 / 自然生成里更强的那些）。 */
    public static boolean isIntense(Level level) {
        return isActive(level) && intense;
    }

    public static boolean isActiveAnywhere() {
        return active;
    }

    public static int windDegrees() {
        return windDegrees;
    }

    /**
     * 八方位键名（{@code n/ne/e/...}）。
     *
     * <p>放在这里而不是客户端类里：服务端命令也要用，而服务端不能引用客户端类。</p>
     */
    public static String windKey(int degrees) {
        String[] keys = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};
        return keys[Math.floorMod((degrees + 22) / 45, 8)];
    }

    public static long endGameTime() {
        return endGameTime;
    }

    public static int remainingTicks(ServerLevel level) {
        return active ? (int) Math.max(0, endGameTime - level.getGameTime()) : 0;
    }

    // ------------------------------------------------------------------ 控制

    /** 在火星召唤一场沙尘暴。 {@code source} 决定它会不会随 Boss 一起结束（见 {@link Origin}）。 */
    public static void trigger(ServerLevel level, int durationTicks, boolean strong, Origin source) {
        if (!isMars(level)) {
            MarsModule.LOGGER.warn("Refused to trigger a sandstorm outside northstar:mars ({})",
                    level.dimension().location());
            return;
        }
        long now = level.getGameTime();
        active = true;
        intense = strong;
        origin = source;
        startGameTime = now;
        endGameTime = now + Math.max(20, durationTicks);
        windDegrees = level.random.nextInt(360);
        nextRollGameTime = endGameTime + COOLDOWN_TICKS;

        MarsModule.LOGGER.info("Sandstorm started on Mars: origin={}, intense={}, {} ticks (wind {}°), players={}",
                source, strong, durationTicks, windDegrees, level.players().size());
        broadcast(level);
        announce(level, strong, remainingTicks(level));
    }

    /** 立刻结束沙尘暴。 */
    public static void stop(ServerLevel level) {
        if (!active) {
            return;
        }
        Origin ended = origin;
        active = false;
        intense = false;
        origin = Origin.NATURAL;
        endGameTime = 0L;
        nextRollGameTime = level.getGameTime() + COOLDOWN_TICKS;
        MarsModule.LOGGER.info("Sandstorm ended on Mars (origin={}, next natural roll no earlier than {} ticks)",
                ended, COOLDOWN_TICKS);
        broadcast(level);
        for (ServerPlayer player : level.players()) {
            player.displayClientMessage(
                    Component.translatable("message.northstar_planet_addon.sandstorm.end")
                            .withStyle(ChatFormatting.GRAY), true);
        }
    }

    /**
     * Boss 死了 / 风暴形态结束：把它召唤的那场沙尘暴收掉。
     *
     * <p><b>只收 {@link Origin#BOSS} 的</b>：自然发生的与指令触发的沙尘暴跟 Boss 无关，
     * 不该因为 Boss 死了就被掐掉。不是 Boss 那场时本方法是空操作。</p>
     *
     * <p>幂等：重复调用（死亡 → 移除 → 阶段切换）只会响应第一次。</p>
     */
    public static void stopIfFromBoss(ServerLevel level) {
        if (active && origin == Origin.BOSS) {
            MarsModule.LOGGER.info("Boss-summoned sandstorm ended early (the summoner is gone)");
            stop(level);
        }
    }

    public static Origin origin() {
        return origin;
    }

    /** 服务器停机 / 维度重载时清状态。 */
    public static void reset() {
        active = false;
        intense = false;
        origin = Origin.NATURAL;
        startGameTime = 0L;
        endGameTime = 0L;
        windDegrees = 0;
        nextRollGameTime = 0L;
    }

    // ------------------------------------------------------------------ tick

    /** 每个服务端 tick 调用一次（见 {@code event.ServerEvents}）。 */
    public static void tick(MinecraftServer server) {
        ServerLevel mars = marsLevel(server);
        if (mars == null) {
            return;
        }
        long now = mars.getGameTime();

        if (active) {
            if (now >= endGameTime) {
                stop(mars);
            } else if (now % 20L == 0L) {
                broadcast(mars);   // 每秒对一次表：覆盖中途登录、也顺便刷新剩余时间
            }
            return;
        }

        if (mars.players().isEmpty() || now < nextRollGameTime || now % ROLL_INTERVAL != 0L) {
            return;
        }
        if (mars.random.nextInt(ROLL_ODDS) != 0) {
            return;
        }
        boolean strong = mars.random.nextInt(INTENSE_CHANCE) == 0;
        int span = MAX_NATURAL_DURATION - MIN_NATURAL_DURATION;
        MarsModule.LOGGER.info("Natural sandstorm roll hit on Mars (intense={})", strong);
        trigger(mars, MIN_NATURAL_DURATION + mars.random.nextInt(span + 1), strong, Origin.NATURAL);
    }

    // ------------------------------------------------------------------ 同步

    /** 把当前状态发给该玩家（登录 / 换维度时用；不在火星就明确告诉他「没沙尘暴」）。 */
    public static void syncTo(ServerPlayer player) {
        ServerLevel level = player.level() instanceof ServerLevel serverLevel ? serverLevel : null;
        boolean here = level != null && isMars(level);
        PacketDistributor.sendToPlayer(player,
                new SandstormPayload(here && active, here && intense, windDegrees,
                        here ? remainingTicks(level) : 0));
    }

    private static void broadcast(ServerLevel level) {
        PacketDistributor.sendToPlayersInDimension(level,
                new SandstormPayload(active, intense, windDegrees, remainingTicks(level)));
    }

    private static void announce(ServerLevel level, boolean strong, int remaining) {
        int seconds = remaining / 20;
        Component message = Component.translatable(strong
                        ? "message.northstar_planet_addon.sandstorm.intense"
                        : "message.northstar_planet_addon.sandstorm.start", seconds)
                .withStyle(strong ? ChatFormatting.RED : ChatFormatting.GOLD);
        for (ServerPlayer player : level.players()) {
            player.displayClientMessage(message, true);
        }
    }
}
