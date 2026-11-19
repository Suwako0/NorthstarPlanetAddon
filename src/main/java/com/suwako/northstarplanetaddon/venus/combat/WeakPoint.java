package com.suwako.northstarplanetaddon.venus.combat;

import net.minecraft.world.phys.Vec3;

/**
 * Boss 弱点（设计草案 2.4 节职责 B）。
 *
 * @param id       弱点标识，用于日志与调试命令
 * @param offset   相对 Boss 实体坐标的偏移
 * @param radius   判定半径（默认取配置 weakPointRadius = 1.2）
 * @param exposeIn 位掩码：哪些阶段暴露（1 = P1、2 = P2、4 = P3，任意位组合）
 */
public record WeakPoint(String id, Vec3 offset, double radius, int exposeIn) {

    public static final int PHASE_1 = 1;
    public static final int PHASE_2 = 2;
    public static final int PHASE_3 = 4;
    /** 任何阶段都暴露（例如过载时的强制暴露由 Boss 另行判断）。 */
    public static final int ALWAYS = PHASE_1 | PHASE_2 | PHASE_3;

    public boolean exposedIn(int phase) {
        return (this.exposeIn & (1 << (phase - 1))) != 0;
    }
}
