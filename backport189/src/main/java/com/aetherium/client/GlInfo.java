package com.aetherium.client;

import org.lwjgl.opengl.GL11;

/** Driver strings, read once on the render thread. */
public final class GlInfo {

    private static String vendor = "?";
    private static String renderer = "?";
    private static String version = "?";
    private static boolean loaded;

    private GlInfo() {
    }

    public static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        try {
            vendor = String.valueOf(GL11.glGetString(GL11.GL_VENDOR));
            renderer = String.valueOf(GL11.glGetString(GL11.GL_RENDERER));
            version = String.valueOf(GL11.glGetString(GL11.GL_VERSION));
        } catch (final RuntimeException error) {
            loaded = false;
        }
    }

    public static String vendor() {
        return vendor;
    }

    public static String renderer() {
        return renderer;
    }

    public static String version() {
        return version;
    }
}
