package com.aetherium.client;

import java.nio.FloatBuffer;

/**
 * The fog state vanilla 1.16.5 sets through {@code GlStateManager}, recorded by
 * {@code FogStateMixin} so {@link ChunkRenderer} never has to query fixed-function fog from GL
 * (GL translators on Android may not implement those queries). Render thread only.
 */
public final class FogTracker {

    static final int GL_FOG_COLOR = 0x0B66;
    static final int GL_EXP = 0x0800;

    private static boolean seen;
    private static boolean enabled;
    private static int mode = GL_EXP; // GL's initial fog mode
    private static float start;
    private static float end = 1.0f;
    private static float density = 1.0f;
    private static final float[] COLOR = new float[4];

    private FogTracker() {
    }

    public static void setEnabled(final boolean on) {
        seen = true;
        enabled = on;
    }

    public static void setMode(final int glMode) {
        seen = true;
        mode = glMode;
    }

    public static void setStart(final float value) {
        seen = true;
        start = value;
    }

    public static void setEnd(final float value) {
        seen = true;
        end = value;
    }

    public static void setDensity(final float value) {
        seen = true;
        density = value;
    }

    /** {@code glFogfv(pname, params)}: only the colour matters here. */
    public static void setParameter(final int pname, final float[] params) {
        if (pname == GL_FOG_COLOR && params != null && params.length >= 4) {
            seen = true;
            System.arraycopy(params, 0, COLOR, 0, 4);
        }
    }

    public static boolean seen() {
        return seen;
    }

    public static boolean enabled() {
        return enabled;
    }

    public static int mode() {
        return mode;
    }

    public static float start() {
        return start;
    }

    public static float end() {
        return end;
    }

    public static float density() {
        return density;
    }

    /** Writes r, g, b, a at indices 0..3 of {@code out}. */
    public static void copyColor(final FloatBuffer out) {
        for (int i = 0; i < 4; i++) {
            out.put(i, COLOR[i]);
        }
    }
}
