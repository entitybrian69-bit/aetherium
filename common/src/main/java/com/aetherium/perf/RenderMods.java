package com.aetherium.perf;

/**
 * Other mods that drive OpenGL themselves (shader loaders, alternative renderers). Aetherium's GL
 * call trimming keeps its own copy of some driver state, which is only valid while every change
 * goes through Minecraft's {@code GlStateManager}; next to these mods that trimming is turned off.
 * Minecraft-free; the classes are looked up without being initialized.
 */
public final class RenderMods {
    /** Iris, OptiFine (OptiFabric), Canvas or Sodium is installed. */
    public static final boolean PRESENT = present("net.coderbot.iris.Iris") || present("net.irisshaders.iris.Iris")
            || present("net.optifine.Config") || present("grondag.canvas.CanvasMod")
            || present("me.jellysquid.mods.sodium.client.SodiumClientMod");

    private RenderMods() {
    }

    private static boolean present(final String name) {
        try {
            Class.forName(name, false, RenderMods.class.getClassLoader());
            return true;
        } catch (final ClassNotFoundException | LinkageError e) {
            return false;
        }
    }
}
