package com.aetherium.client;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL14;

/**
 * Multi-draw terrain (1.17.2, Minecraft 1.16.5, Aetherium pipeline with batched terrain): all
 * queued ranges of one terrain buffer go to the driver in a single {@code glMultiDrawArrays} call
 * instead of one {@code glDrawArrays} each. Desktop GL only: GL translators on phones either lack
 * the call or split it back into single draws (and cannot multi-draw quads), so they keep the
 * separate draws, as does any GL older than 1.4.
 *
 * <p>Render thread only.</p>
 */
public final class MultiDraw {
    private static IntBuffer firsts;
    private static IntBuffer counts;
    private static Boolean supported;
    private static boolean failed;

    private MultiDraw() {
    }

    /** Whether this frame's pages may be multi-drawn. */
    public static boolean usable(final boolean wanted) {
        if (!wanted || failed || TerrainRegions.translated()) {
            return false;
        }
        if (supported == null) {
            boolean ok;
            try {
                ok = GL.getCapabilities() != null && GL.getCapabilities().OpenGL14;
            } catch (final RuntimeException | LinkageError error) {
                ok = false;
            }
            supported = Boolean.valueOf(ok);
        }
        return supported.booleanValue();
    }

    /**
     * Draws the ranges of {@code page} as {@code GL_QUADS} with one call.
     *
     * @return false when the call is unavailable (then nothing was drawn and multi-draw stays off)
     */
    public static boolean draw(final TerrainRegions.RegionPage page) {
        final int n = page.ranges();
        if (n == 0) {
            return true;
        }
        if (firsts == null || firsts.capacity() < n) {
            final int capacity = Math.max(n, 256);
            firsts = ByteBuffer.allocateDirect(capacity * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
            counts = ByteBuffer.allocateDirect(capacity * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
        }
        firsts.clear();
        counts.clear();
        for (int r = 0; r < n; r++) {
            firsts.put(page.rangeFirst(r));
            counts.put(page.rangeCount(r));
        }
        firsts.flip();
        counts.flip();
        try {
            GL14.glMultiDrawArrays(7, firsts, counts); // GL_QUADS
            return true;
        } catch (final RuntimeException | LinkageError error) {
            failed = true;
            return false;
        }
    }
}
