package com.suwako.northstarplanetaddon.moon.entity;

/** Server-synchronised combat body language for Moon Shell Crabs. */
public enum MoonShellCrabAnimation {
    IDLE,
    PINCH,
    THROW;

    private static final MoonShellCrabAnimation[] VALUES = values();

    public static MoonShellCrabAnimation byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : IDLE;
    }
}
