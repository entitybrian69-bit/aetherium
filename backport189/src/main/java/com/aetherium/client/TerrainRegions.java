package com.aetherium.client;

/**
 * Backport stand-in for the main mod's region renderer bookkeeping. The shared {@code Aetherium}
 * bootstrap tells it whether the GPU needs per-region translation (Android); the 1.12.2 terrain
 * renderer ({@link TerrainRenderer}) draws chunk by chunk and has no regions, so there is nothing
 * to configure. Providing this class keeps the main mod's LWJGL 3 renderer out of the backport.
 */
public final class TerrainRegions {
    private TerrainRegions() {
    }

    public static void setTranslated(final boolean translated) {
    }
}
