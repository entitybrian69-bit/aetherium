package com.aetherium.qol;

/**
 * Fog and sky settings and the fog arithmetic, kept free of Minecraft types so they can be
 * unit-tested. The mixins read the static fields (one volatile read each) and call
 * {@link #adjustFog}.
 */
public final class Scenery {
    public static final int AIR = 0;
    public static final int WATER = 1;
    public static final int LAVA = 2;

    /** Vanilla starts terrain fog at 75 % of the render distance. */
    public static final int VANILLA_FOG_START = 75;
    /** How far "clear lava" lets you see, in blocks. */
    public static final float CLEAR_LAVA_END = 10.0f;

    public static volatile boolean distanceFog = true;
    public static volatile int fogStartPercent = VANILLA_FOG_START;
    public static volatile int waterFogPercent = 100;
    public static volatile boolean clearLava;
    public static volatile boolean sky = true;
    public static volatile boolean sunMoon = true;
    public static volatile boolean stars = true;
    /** 0 = the dimension's own height. */
    public static volatile int cloudHeight;

    /** Index into the result of {@link #adjustFog}. */
    public static final int START = 0;
    public static final int END = 1;
    public static final int DENSITY = 2;

    private Scenery() {
    }

    /** True when every fog option is at its vanilla value (the hook then does nothing). */
    public static boolean fogVanilla() {
        return distanceFog && fogStartPercent == VANILLA_FOG_START && waterFogPercent == 100 && !clearLava;
    }

    /**
     * Terrain fog after the user's settings.
     *
     * @param medium         {@link #AIR}, {@link #WATER} or {@link #LAVA} at the camera
     * @param thick          the dimension uses thick fog (the Nether) or the camera is in it
     * @param renderDistance far plane distance in blocks
     * @param start          fog start vanilla set (linear fog)
     * @param end            fog end vanilla set (linear fog)
     * @param density        fog density vanilla set (exponential fog)
     * @param out            receives start, end, density
     * @return true when anything changed
     */
    public static boolean adjustFog(final int medium, final boolean thick, final float renderDistance, final float start,
                                    final float end, final float density, final float[] out) {
        out[START] = start;
        out[END] = end;
        out[DENSITY] = density;
        switch (medium) {
            case WATER: {
                final int pct = waterFogPercent;
                if (pct >= 100) {
                    return false;
                }
                out[DENSITY] = density * Math.max(0, pct) / 100.0f;
                return true;
            }
            case LAVA:
                if (!clearLava) {
                    return false;
                }
                out[START] = 0.0f;
                out[END] = Math.max(end, Math.min(CLEAR_LAVA_END, renderDistance));
                return true;
            default:
                if (!distanceFog) {
                    // Linear fog factor (end - d) / (end - start) stays above 1 for every visible
                    // distance, so nothing is fogged; end > start keeps the division defined.
                    out[START] = renderDistance * 4.0f + 64.0f;
                    out[END] = out[START] + 1.0f;
                    return true;
                }
                if (thick || fogStartPercent == VANILLA_FOG_START) {
                    return false;
                }
                out[START] = Math.min(end * 0.995f, renderDistance * fogStartPercent / 100.0f);
                return true;
        }
    }

    /** Index into the result of {@link #adjustFogData}. */
    public static final int ENV_START = 0;
    public static final int ENV_END = 1;
    public static final int RD_START = 2;
    public static final int RD_END = 3;

    /**
     * 1.21.6+: the fog uniform holds two linear ranges, the environmental one (water, lava, the
     * Nether, blindness, powder snow) and the render-distance one (terrain fade-out). The
     * distance settings only move the render-distance range, so they can never lift blindness or
     * Nether fog; the water and lava settings move the environmental range.
     *
     * @param medium {@link #AIR}, {@link #WATER} or {@link #LAVA} at the camera
     * @param out    receives envStart, envEnd, rdStart, rdEnd
     * @return true when anything changed
     */
    public static boolean adjustFogData(final int medium, final float envStart, final float envEnd,
                                        final float rdStart, final float rdEnd, final float[] out) {
        out[ENV_START] = envStart;
        out[ENV_END] = envEnd;
        out[RD_START] = rdStart;
        out[RD_END] = rdEnd;
        if (medium == WATER) {
            final int pct = waterFogPercent;
            if (pct >= 100) {
                return false;
            }
            if (pct <= 0) {
                out[ENV_START] = rdEnd * 4.0f + 64.0f;
                out[ENV_END] = out[ENV_START] + 1.0f;
            } else {
                out[ENV_END] = envStart + (envEnd - envStart) * 100.0f / pct;
            }
            return true;
        }
        if (medium == LAVA) {
            if (!clearLava) {
                return false;
            }
            out[ENV_START] = 0.0f;
            out[ENV_END] = Math.max(envEnd, Math.min(CLEAR_LAVA_END, rdEnd));
            return true;
        }
        if (!distanceFog) {
            out[RD_START] = rdEnd * 4.0f + 64.0f;
            out[RD_END] = out[RD_START] + 1.0f;
            return true;
        }
        final int pct = fogStartPercent;
        if (pct == VANILLA_FOG_START) {
            return false;
        }
        out[RD_START] = Math.min(rdEnd * 0.995f, rdEnd * pct / 100.0f);
        return true;
    }

    /** Cloud height after the user's setting; NaN (no clouds in this dimension) is kept. */
    /**
     * 1.17+ (shader fog, always linear): like {@link #adjustFog}, except that water fog is linear
     * as well, so the water setting moves its end instead of scaling a density. At p % the water
     * fog spans 100/p times the vanilla distance; at 0 % there is none.
     *
     * @return true when anything changed ({@code out[DENSITY]} is unused)
     */
    public static boolean adjustLinearFog(final int medium, final boolean thick, final float renderDistance,
                                          final float start, final float end, final float[] out) {
        if (medium != WATER) {
            return adjustFog(medium, thick, renderDistance, start, end, 0.0f, out);
        }
        out[START] = start;
        out[END] = end;
        out[DENSITY] = 0.0f;
        final int pct = waterFogPercent;
        if (pct >= 100) {
            return false;
        }
        if (pct <= 0) {
            out[START] = renderDistance * 4.0f + 64.0f;
            out[END] = out[START] + 1.0f;
            return true;
        }
        out[END] = start + (end - start) * 100.0f / pct;
        return true;
    }

    public static float cloudHeight(final float original) {
        final int h = cloudHeight;
        if (h <= 0 || Float.isNaN(original)) {
            return original;
        }
        return h;
    }
}
