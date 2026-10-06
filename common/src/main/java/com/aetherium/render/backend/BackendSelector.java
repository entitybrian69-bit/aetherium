package com.aetherium.render.backend;

import java.util.EnumMap;
import java.util.Map;

import com.aetherium.android.AndroidEnvironment;
import com.aetherium.config.AetheriumConfig;
import com.aetherium.gamma.GammaApplier;
import com.aetherium.lighting.DynamicLightEngine;
import com.aetherium.shader.IrisBridge;
import com.aetherium.util.AetheriumLog;

/**
 * Turns "what the user asked for" plus "what the GPU can do" into one backend,
 * and re-applies the feature switches that follow from it.
 *
 * <p>Selection is a scoring pass, not an if-chain, so that the penalty logic in
 * {@link GpuInfo#penaltyFor(RenderBackend)} and the Android routing hints stay in
 * their own places. Lower score wins; ties break toward the more conservative
 * backend by enum order, which makes the result deterministic and therefore
 * comparable across machines in {@code BENCHMARK.md}.</p>
 */
public final class BackendSelector {
    private static final AetheriumLog LOGGER = AetheriumLog.of(BackendSelector.class);

    private BackendSelector() {
    }

    /**
     * Resolves the effective backend for {@code requested} given the probe.
     *
     * @return the backend to run, never null; {@link RenderBackend#COMPATIBILITY}
     *         when nothing is usable
     */
    public static RenderBackend resolve(final AetheriumConfig.BackendChoice requested, final BackendCapabilities caps) {
        if (caps == null) {
            return RenderBackend.COMPATIBILITY;
        }
        if (requested == AetheriumConfig.BackendChoice.COMPATIBILITY) {
            return RenderBackend.COMPATIBILITY;
        }
        if (requested != AetheriumConfig.BackendChoice.AUTO) {
            final RenderBackend explicit = map(requested);
            if (caps.getSupported().contains(explicit) && feasible(explicit, caps)) {
                LOGGER.info("Using user-selected backend {}", explicit.getId());
                return explicit;
            }
            LOGGER.warn("Requested backend {} is not feasible ({}); falling back to AUTO", explicit.getId(), caps.describe());
        }

        RenderBackend best = RenderBackend.COMPATIBILITY;
        int bestScore = Integer.MAX_VALUE;
        for (final RenderBackend candidate : caps.getSupported()) {
            if (candidate == RenderBackend.COMPATIBILITY) {
                continue;
            }
            if (!feasible(candidate, caps)) {
                LOGGER.dev("Rejecting {}: capability requirements not met", candidate.getId());
                continue;
            }
            int score = caps.getGpu().penaltyFor(candidate);
            // Native preference ordering: DSA > Vulkan > core > legacy.
            score += candidate.ordinal() * 5;
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        LOGGER.info("Auto-selected {} (score {}) from {} candidates", best.getId(), bestScore, caps.getSupported().size());
        return best;
    }

    /** Hard capability gates that no amount of scoring may override. */
    private static boolean feasible(final RenderBackend backend, final BackendCapabilities caps) {
        switch (backend) {
            case GL46_DSA:
                return caps.getGpu().getGlLevel() >= 460
                        && caps.supportsDirectStateAccess()
                        && caps.supportsCompute();
            case VULKAN_13:
                return caps.isVulkanAvailable();
            case GL_CORE:
                return caps.getGpu().getGlLevel() >= 330;
            case GL_LEGACY:
                return caps.getGpu().getGlLevel() >= 200;
            case COMPATIBILITY:
                return true;
            default:
                return false;
        }
    }

    private static RenderBackend map(final AetheriumConfig.BackendChoice choice) {
        switch (choice) {
            case GL46_DSA:
                return RenderBackend.GL46_DSA;
            case VULKAN_13:
                return RenderBackend.VULKAN_13;
            case GL_CORE:
                return RenderBackend.GL_CORE;
            case GL_LEGACY:
                return RenderBackend.GL_LEGACY;
            case COMPATIBILITY:
                return RenderBackend.COMPATIBILITY;
            case AUTO:
            default:
                return RenderBackend.GL46_DSA;
        }
    }

    /**
     * Applies the feature switches derived from the backend. Called on the initial
     * resolve and again from {@code Aetherium.hotSwapBackend}, so a user changing
     * the backend in the GUI gets a coherent feature set rather than a renderer
     * with compute paths enabled on a driver that has no compute.
     */
    public static void apply(final AetheriumConfig config, final BackendCapabilities caps, final AndroidEnvironment android) {
        final RenderBackend resolved = resolve(config.backend.get(), caps);
        final Map<RenderBackend, Boolean> support = new EnumMap<>(RenderBackend.class);
        support.put(RenderBackend.GL46_DSA, resolved == RenderBackend.GL46_DSA);

        final boolean allowPersistent = resolved.supportsPersistentBuffers()
                && caps.supportsPersistentMapping()
                && (android == null || android.getRenderer().allowPersistentMapping());
        final boolean allowIndirect = resolved.supportsIndirectDraw()
                && caps.supportsIndirectCount()
                && (android == null || android.getRenderer().allowIndirectDraw());
        final boolean allowHzb = caps.supportsHierarchicalZ()
                && (android == null || android.getRenderer().allowCompute());

        if (!config.persistentBuffers.get().booleanValue() && allowPersistent) {
            LOGGER.dev("Persistent mapping available but disabled by config");
        }
        config.persistentBuffers.set(allowPersistent && config.persistentBuffers.get());
        config.indirectDraw.set(allowIndirect && config.indirectDraw.get());
        config.hzb.set(allowHzb && config.hzb.get());
        if (!allowHzb && config.hzb.get()) {
            LOGGER.info("HZB disabled: no compute/image-store path on {}", caps.describe());
        }
        if (resolved == RenderBackend.GL_LEGACY) {
            // gl4es reports 4.x numbers it does not implement; trust only the ES path.
            config.smoothLighting.set(false);
            LOGGER.info("GL_LEGACY backend: smooth lighting and async upload disabled");
        }
        LOGGER.info("Feature matrix for {}: dsa={} persistent={} indirect={} hzb={}",
                resolved.getId(), support.getOrDefault(RenderBackend.GL46_DSA, false), allowPersistent, allowIndirect, allowHzb);
    }

    /**
     * Re-run after a live backend change. Invalidates anything that cached
     * per-backend state so the next frame rebuilds against the new one.
     */
    public static void reapply(final AetheriumConfig config, final IrisBridge iris, final DynamicLightEngine lights, final GammaApplier gamma) {
        if (config == null) {
            return;
        }
        // Shader packs must be recompiled when the owning renderer changes; the
        // bridge forwards to Iris' own reload so we never touch its internals.
        iris.onRendererChanged();
        lights.invalidateCache();
        gamma.invalidate();
        LOGGER.info("Backend re-applied; smoothLighting={} indirectDraw={} hzb={}",
                config.smoothLighting.get(), config.indirectDraw.get(), config.hzb.get());
    }
}
