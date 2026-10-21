package com.aetherium.perf;

/**
 * HUD refresh rate (1.17.2): when the HUD is redrawn into its cached picture.
 *
 * <p>The HUD (hotbar, hearts, chat, F3 screen, boss bars, other mods' HUD) is drawn into an
 * off-screen picture {@code fps} times a second and that picture is laid over the world in every
 * other frame, which is one textured rectangle instead of hundreds of small draws. The crosshair,
 * the vignette and world-anchored labels stay live every frame.</p>
 *
 * <p>Caching only pays when the game draws clearly more frames than the HUD needs: below
 * {@link #MIN_GAIN} times the HUD rate every frame would refresh anyway and the extra copy would
 * cost more than it saves, so the HUD is then drawn directly, exactly as vanilla. Some changes
 * refresh at once: a new window or GUI size, another hotbar slot, the F3 screen or a menu opening
 * or closing ({@link #invalidate}).</p>
 *
 * <p>Minecraft-free (times are passed in); the 1.16.5 mixins do the drawing. Render thread only.</p>
 */
public final class HudRate {

    /** Slider value meaning "every frame" (cache off). */
    public static final int OFF = 0;
    public static final int MAX = 60;
    /** Cache only while the frame rate is at least this many times the HUD rate. */
    static final double MIN_GAIN = 1.5;
    /** Frame-time average weight per frame. */
    private static final double SMOOTH = 0.1;

    /** What the HUD hook does this frame. */
    public static final int DIRECT = 0;
    public static final int REFRESH = 1;
    public static final int REUSE = 2;

    private static volatile int fps;
    private static long lastRefresh;
    private static long lastFrame;
    private static double frameNanos;
    private static boolean valid;
    private static int width = -1;
    private static int height = -1;
    private static long stateKey = Long.MIN_VALUE;
    private static int refreshes;
    private static int reuses;
    /** The HUD is being drawn into the picture (render thread): live parts skip themselves. */
    private static boolean capturing;

    private HudRate() {
    }

    /** The HUD refresh rate in pictures per second, {@link #OFF} = vanilla. */
    public static void setFps(final int value) {
        fps = value <= 0 ? OFF : Math.min(MAX, value);
        if (fps == OFF) {
            valid = false;
        }
    }

    public static int fps() {
        return fps;
    }

    /** Forget the picture: the next frame redraws the HUD. */
    public static void invalidate() {
        valid = false;
    }

    /**
     * Once per frame, before the HUD is drawn.
     *
     * @param now      {@code System.nanoTime()}
     * @param w        framebuffer width
     * @param h        framebuffer height
     * @param key      anything that must refresh the picture when it changes (GUI scale, selected
     *                 slot, F3 shown, a menu open ...), packed into one number by the caller
     * @param live     true when something on the HUD must move every frame (then drawn directly)
     */
    public static int decide(final long now, final int w, final int h, final long key, final boolean live) {
        final long since = lastFrame == 0L ? 0L : now - lastFrame;
        lastFrame = now;
        if (since > 0L && since < 1_000_000_000L) {
            frameNanos = frameNanos <= 0.0 ? since : frameNanos + (since - frameNanos) * SMOOTH;
        }
        final int rate = fps;
        if (rate == OFF || live || frameNanos <= 0.0 || 1.0E9 / frameNanos < rate * MIN_GAIN) {
            valid = false;
            return DIRECT;
        }
        if (w != width || h != height || key != stateKey) {
            width = w;
            height = h;
            stateKey = key;
            valid = false;
        }
        final long interval = 1_000_000_000L / rate;
        if (valid && now - lastRefresh < interval && now >= lastRefresh) {
            reuses++;
            return REUSE;
        }
        lastRefresh = now;
        valid = true;
        refreshes++;
        return REFRESH;
    }

    /** True while the HUD is drawn into the picture: the crosshair, vignette and labels wait. */
    public static boolean capturing() {
        return capturing;
    }

    public static void setCapturing(final boolean value) {
        capturing = value;
    }

    /**
     * Alpha blend factors that keep the picture's alpha channel right while it is drawn over a
     * transparent background: a normal blend accumulates coverage (one, one minus source alpha),
     * an additive one ({@code dstRgb == GL_ONE}) leaves it as it is. Colour factors are untouched,
     * so colours land premultiplied, and the picture is laid over the world with (one, one minus
     * source alpha).
     *
     * @return {@code srcAlpha << 16 | dstAlpha}
     */
    public static int alphaFactorsFor(final int dstRgb) {
        return dstRgb == GL_ONE ? PACKED_ADDITIVE : PACKED_COVER;
    }

    public static final int GL_ZERO = 0;
    public static final int GL_ONE = 1;
    public static final int GL_ONE_MINUS_SRC_ALPHA = 0x0303;
    /** srcAlpha << 16 | dstAlpha. */
    static final int PACKED_COVER = GL_ONE << 16 | GL_ONE_MINUS_SRC_ALPHA;
    static final int PACKED_ADDITIVE = GL_ZERO << 16 | GL_ONE;

    /** The refresh failed (no picture): draw directly until the next decision. */
    public static void refreshFailed() {
        valid = false;
    }

    /** Counters since the last call, for the Backend page: refreshed and reused frames. */
    public static String takeStats() {
        final String s = refreshes + " redrawn / " + reuses + " reused";
        refreshes = 0;
        reuses = 0;
        return s;
    }

    /** Tests: forget everything. */
    static void reset() {
        fps = OFF;
        lastRefresh = 0L;
        lastFrame = 0L;
        frameNanos = 0.0;
        valid = false;
        width = -1;
        height = -1;
        stateKey = Long.MIN_VALUE;
        refreshes = 0;
        reuses = 0;
        capturing = false;
    }
}
