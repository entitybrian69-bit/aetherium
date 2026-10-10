package com.aetherium.client;

import org.lwjgl.opengl.GL11;

/**
 * Driver strings for the Backend page and the footer, read once on the render
 * thread (the only thread with a current GL context) and cached.
 *
 * <p>Read through LWJGL, which every supported Minecraft version ships, so no
 * per-version Blaze3D API is involved. Without a GL context (for example a
 * future non-GL backend), LWJGL throws instead of crashing, and the values fall
 * back to "Unknown".</p>
 */
public final class GlInfo {

    private static final int GL_VENDOR = 0x1F00;
    private static final int GL_RENDERER = 0x1F01;
    private static final int GL_VERSION = 0x1F02;

    private static volatile boolean loaded;
    private static String vendor = "Unknown";
    private static String renderer = "Unknown";
    private static String version = "Unknown";

    private GlInfo() {
    }

    /** Call on the render thread; cheap after the first call. */
    public static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        try {
            vendor = orUnknown(GL11.glGetString(GL_VENDOR));
            renderer = orUnknown(GL11.glGetString(GL_RENDERER));
            version = orUnknown(GL11.glGetString(GL_VERSION));
        } catch (final Throwable ignored) {
            // no current context or no GL backend: keep "Unknown"
        }
    }

    public static String vendor() {
        return vendor;
    }

    public static String renderer() {
        return renderer;
    }

    /** Full driver version string, e.g. "3.2.0 NVIDIA 551.23". */
    public static String version() {
        return version;
    }

    private static String orUnknown(final String s) {
        return s == null || s.trim().isEmpty() ? "Unknown" : s.trim();
    }
}
