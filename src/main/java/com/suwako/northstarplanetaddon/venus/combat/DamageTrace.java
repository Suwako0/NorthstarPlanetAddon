package com.suwako.northstarplanetaddon.venus.combat;

/**
 * 一次伤害结算的追踪记录，供 {@code /phosphoros debug classify|boss|dps} 使用。
 *
 * @param kind  分类结果
 * @param raw   事件原始伤害
 * @param plate 实际打给护甲板的伤害
 * @param core  实际打给本体的伤害
 * @param note  说明（被拦截原因 / 是否命中弱点等）
 */
public record DamageTrace(DamageKind kind, float raw, float plate, float core, String note) {

    private static final DamageTrace NONE = new DamageTrace(DamageKind.OTHER, 0.0F, 0.0F, 0.0F, "none");

    public static DamageTrace none() {
        return NONE;
    }

    public static DamageTrace blocked(DamageKind kind, float raw, String reason) {
        return new DamageTrace(kind, raw, 0.0F, 0.0F, reason);
    }
}
