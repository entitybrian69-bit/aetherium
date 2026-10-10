package com.aetherium.platform;

import java.nio.file.Path;
import java.util.Optional;

/**
 * The only loader-facing surface in the codebase. Fabric and NeoForge each ship
 * one implementation, resolved through {@link PlatformServices} at boot, so the
 * engine and the GUI never reference {@code FabricLoader} or {@code ModList}.
 *
 * <p>Contract for implementors: every method must be safe to call from the
 * render thread and from worker threads, must be allocation-free after warm-up,
 * and must not throw for "not found" — those return {@link Optional#empty()} or
 * {@code false}.</p>
 */
public interface PlatformAdapter {

    /** {@code "fabric"} or {@code "neoforge"}; used in crash reports. */
    String platformName();

    /** Game directory that {@link #configDirectory()} is resolved against. */
    Path gameDirectory();

    /** Directory Aetherium writes {@code aetherium.json}, shader caches, logs. */
    Path configDirectory();

    /**
     * Whether a mod with the given id is present. Implementations must consult
     * the loader's own registry rather than the jar list, so that a mod whose
     * dependencies failed to load is reported as absent.
     */
    boolean isModLoaded(String modId);

    /** Human-readable version of a loaded mod, for the conflict report. */
    Optional<String> getModVersion(String modId);

    /** Current Minecraft version string, e.g. {@code "1.21.1"} or {@code "26.2"}. */
    String minecraftVersion();

    /**
     * True when the run has a usable GL/Vulkan context (i.e. not a data
     * generation or dedicated-server JVM). Aetherium registers no client hooks
     * when this is false.
     */
    boolean isClient();

    /**
     * Registers a frame-time observer. Fabric forwards to its client tick event,
     * NeoForge to {@code RenderLevelStageEvent}; the mixin path in common is the
     * fallback. Default no-op keeps common-side boot working on a bare JVM in
     * tests.
     */
    default void installFrameHook(final Runnable onFrameEnd) {
        // no-op by default
    }
}
