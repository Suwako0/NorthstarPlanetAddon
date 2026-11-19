package com.suwako.northstarplanetaddon.mercury.world;

/** Internal bridge that clears {@code BiomeSource#possibleBiomes()} after a late parameter update. */
public interface RefreshableBiomeSource {

    void mna$refreshPossibleBiomes();
}
