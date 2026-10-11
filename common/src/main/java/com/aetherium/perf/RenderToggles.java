package com.aetherium.perf;

import com.aetherium.Capabilities;
import com.aetherium.config.AetheriumConfig;

/**
 * Flags read by the render-thread hooks. Every field is a plain volatile so a
 * hook costs one memory read; the values are recomputed from the config
 * whenever an option changes ({@link #refresh}), never per frame.
 */
public final class RenderToggles {
    /** Master: true only while the OpenGL 3.0 backend (Aetherium) is active. */
    public static volatile boolean active;
    public static volatile boolean entityCulling;
    public static volatile double entityCullDistanceSq = 64.0 * 64.0;
    /** Skip entities whose sections vanilla's occlusion graph found hidden (see SectionVisibility). */
    public static volatile boolean occlusionCulling;
    /** Latched when the visible-list walker failed; occlusion culling stays off for the session. */
    private static volatile boolean occlusionBroken;
    /** The opt-in 1.16.5 terrain renderer (ChunkRenderer); false on every other version. */
    public static volatile boolean experimentalChunkRenderer;
    /** Percentage of particles kept, 0..100. */
    public static volatile int particleKeep = 100;
    public static volatile boolean hideWeather;
    public static volatile boolean hideVignette;
    /** Skip the per-tick animation of water, lava, fire, portal... textures. */
    public static volatile boolean freezeTextureAnimations;

    /** Client-thread only; spreads dropped particles evenly instead of randomly. */
    private static int particleAccumulator;

    private RenderToggles() {
    }

    public static void refresh(final AetheriumConfig config, final boolean isActive) {
        active = isActive;
        entityCulling = isActive && config.entityCulling.get().booleanValue();
        occlusionCulling = isActive && Capabilities.ENTITY_OCCLUSION && !occlusionBroken
                && config.occlusionCulling.get().booleanValue();
        experimentalChunkRenderer = isActive && Capabilities.EXPERIMENTAL_CHUNK_RENDERER
                && config.experimentalChunkRenderer.get().booleanValue();
        final double distance = config.entityCullDistance.get().intValue();
        entityCullDistanceSq = distance * distance;
        particleKeep = isActive ? config.particleDensity.get().intValue() : 100;
        hideWeather = isActive && !config.weather.get().booleanValue();
        hideVignette = isActive && !config.vignette.get().booleanValue();
        freezeTextureAnimations = isActive && !config.animatedTextures.get().booleanValue();
        UploadBudget.setEnabled(isActive && config.smoothChunkLoading.get().booleanValue());
        BlockEntityCull.setDistance(isActive ? config.blockEntityDistance.get().intValue() : BlockEntityCull.VANILLA);
    }

    /**
     * Deterministic decimation: at 30 % density exactly 3 of every 10 particles
     * survive, so effects keep their shape instead of flickering randomly.
     *
     * @return true when the particle being added should be discarded
     */
    public static boolean dropParticle() {
        final int keep = particleKeep;
        if (keep >= 100) {
            return false;
        }
        if (keep <= 0) {
            return true;
        }
        particleAccumulator += keep;
        if (particleAccumulator >= 100) {
            particleAccumulator -= 100;
            return false;
        }
        return true;
    }

    /** Turns entity occlusion culling off for the rest of the session (the hook threw). */
    public static void breakOcclusion() {
        occlusionBroken = true;
        occlusionCulling = false;
    }

    public static boolean isOcclusionBroken() {
        return occlusionBroken;
    }

    /** Distance test used by the entity-culling hook. */
    public static boolean beyondCullDistance(final double distanceSq) {
        return distanceSq > entityCullDistanceSq;
    }
}
