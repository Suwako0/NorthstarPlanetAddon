package com.suwako.northstarplanetaddon.moon.world;

/** Internal bridge that clears {@code BiomeSource#possibleBiomes()} after a late parameter update. */
public interface RefreshableBiomeSource {

    void nma$refreshPossibleBiomes();
}
