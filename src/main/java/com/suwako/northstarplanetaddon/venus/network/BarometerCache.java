package com.suwako.northstarplanetaddon.venus.network;

/**
 * 客户端 HUD 的数据缓存（纯数据，服务端也可安全加载）。
 *
 * <p>HUD 每帧读取这里；包处理器只负责写，不碰任何渲染类。</p>
 */
public final class BarometerCache {

    private static float bar;
    private static float temperature;
    private static int corrosion;
    private static int daysToMonsoon;
    private static long expiresAtMillis;

    public static void update(BarometerPayload payload) {
        bar = payload.bar();
        temperature = payload.temperature();
        corrosion = payload.corrosion();
        daysToMonsoon = payload.daysToMonsoon();
        expiresAtMillis = System.currentTimeMillis() + 5000L;
    }

    public static boolean isVisible() {
        return System.currentTimeMillis() < expiresAtMillis;
    }

    public static float bar() {
        return bar;
    }

    public static float temperature() {
        return temperature;
    }

    public static int corrosion() {
        return corrosion;
    }

    public static int daysToMonsoon() {
        return daysToMonsoon;
    }

    private BarometerCache() {
    }
}
