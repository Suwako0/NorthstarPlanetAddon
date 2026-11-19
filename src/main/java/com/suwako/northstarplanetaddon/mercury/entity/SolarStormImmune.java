package com.suwako.northstarplanetaddon.mercury.entity;

/**
 * Marks a mob that lives inside Mercury's solar storm and therefore ignores its effects.
 * <p>
 * Anything implementing this is skipped by the storm's scorching pass in
 * {@link com.suwako.northstarplanetaddon.mercury.world.SolarStormManager}, so it is neither hurt nor set on
 * fire by the weather. The 太阳鱼 and 炽烈太阳鱼 are storm creatures themselves — they ride the solar
 * wind instead of being cooked by it.
 */
public interface SolarStormImmune {
}
