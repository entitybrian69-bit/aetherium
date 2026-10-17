package com.aetherium.client;

import com.aetherium.perf.RenderToggles;
import com.aetherium.util.AetheriumLog;

/**
 * Render scale (1.9.0): the decisions behind drawing the 3D world into a smaller framebuffer
 * and scaling it up to the window, kept free of Minecraft types so they are testable and
 * version-neutral. The framebuffer swap itself lives in {@link ScaledRenderTarget} (versions
 * with {@code Capabilities.RENDER_SCALE}).
 *
 * <p>Render thread only.</p>
 */
public final class RenderScale {
    /** Lowest accepted percentage; the config slider has the same floor. */
    public static final int MIN_PERCENT = 50;
    /**
     * Frames to stay at full resolution after a glowing entity was drawn. Vanilla's outline
     * pass draws into a window-sized target with the current viewport, so outlines would land
     * in the wrong place in a scaled frame.
     */
    static final int GLOW_HOLD_FRAMES = 60;

    private static final AetheriumLog LOGGER = AetheriumLog.of(RenderScale.class);

    private static boolean glowThisFrame;
    private static int glowHold;
    private static boolean broken;
    private static int lastPercent = 100;

    private RenderScale() {
    }

    /** A glowing (outlined) entity is being drawn this frame. */
    public static void glowing() {
        glowThisFrame = true;
    }

    /**
     * Called once per frame before the world is drawn: the percentage to draw this frame at,
     * 100 meaning "draw straight into the window".
     */
    public static int beginFrame(final boolean fabulous) {
        if (glowThisFrame) {
            glowThisFrame = false;
            glowHold = GLOW_HOLD_FRAMES;
        } else if (glowHold > 0) {
            glowHold--;
        }
        lastPercent = percentFor(RenderToggles.renderScale, fabulous, glowHold > 0, broken);
        return lastPercent;
    }

    /** Pure decision, exposed for tests. */
    static int percentFor(final int configured, final boolean fabulous, final boolean glowHeld,
                          final boolean failed) {
        if (configured >= 100 || fabulous || glowHeld || failed) {
            return 100;
        }
        return Math.max(MIN_PERCENT, configured);
    }

    /** Size of one framebuffer side at {@code percent}, rounded, never below one pixel. */
    public static int scaled(final int size, final int percent) {
        return Math.max(1, (size * percent + 50) / 100);
    }

    /** Percentage the last frame was drawn at (100 = full resolution). */
    public static int lastPercent() {
        return lastPercent;
    }

    /** The swap failed: full resolution for the rest of the session. */
    public static void fail(final Throwable error) {
        if (!broken) {
            broken = true;
            LOGGER.warn("Render scale disabled for this session", error);
        }
    }

    public static boolean broken() {
        return broken;
    }
}
