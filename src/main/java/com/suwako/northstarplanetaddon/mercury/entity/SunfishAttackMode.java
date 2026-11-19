package com.suwako.northstarplanetaddon.mercury.entity;

/**
 * Which attack a sky fish is currently performing. Stored in {@link SunfishEntity}'s synched data
 * so the client model can spin, flap or rear back at the same time as the server-side attack.
 */
public enum SunfishAttackMode {
    NONE,
    /** 旋转俯冲 — spin up, then dive into the target. Shared by both species. */
    DIVE,
    /** 拍翅散射 — 太阳鱼 only: flutter close and throw rings of fire. */
    WING_FLAP,
    /** 高能射线 — 炽烈太阳鱼 only: several piercing beams in a row. */
    RAY;

    private static final SunfishAttackMode[] BY_ID = values();

    public int getId() {
        return this.ordinal();
    }

    public static SunfishAttackMode fromId(int id) {
        return id >= 0 && id < BY_ID.length ? BY_ID[id] : NONE;
    }
}
