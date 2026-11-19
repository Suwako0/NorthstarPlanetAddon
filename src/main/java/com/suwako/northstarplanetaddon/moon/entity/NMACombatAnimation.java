package com.suwako.northstarplanetaddon.moon.entity;

/**
 * Body language a client model plays for the Moon Deer, kept separate from the entity's private
 * combat {@code Skill} enum on purpose: the model only needs "what is the deer doing", so renaming,
 * adding or reordering skills cannot silently repoint an animation.
 */
public enum NMACombatAnimation {

    /** Standing, walking or running — the model's idle and stride layers carry it. */
    IDLE,
    /** Lowered charge with the antlers forward. */
    DASH,
    /** Rearing back to send out a ring. */
    RING,
    /** Rapid head thrusts. */
    BARRAGE,
    /** Braced, antlers levelled for a beam. */
    BEAM,
    /** Muzzle to the sky while moon rain falls. */
    MOON_RAIN,
    /** Winds up tall, then slams down for a meteor. */
    METEOR,
    /** Crouched low, flattened into the dark. */
    SHADOW,
    /** Antlers lowered while lunar dust gathers around every marked player. */
    MOON_DUST;

    private static final NMACombatAnimation[] VALUES = values();

    /** Safe lookup for a synced byte, so a stale client value cannot throw. */
    public static NMACombatAnimation byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : IDLE;
    }
}
