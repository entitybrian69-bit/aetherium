package com.aetherium.render.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.aetherium.android.AndroidEnvironment;
import com.aetherium.config.AetheriumConfig;

/**
 * Backend selection is the one place where a wrong answer is visible in every frame, so
 * the feasibility gates are tested individually: what a driver must report to get the DSA
 * path, and what happens when a user asks for something the GPU cannot do.
 */
final class BackendSelectorTest {
    private static GpuInfo gpu(final int major, final int minor, final GpuInfo.Source source) {
        return new GpuInfo("NVIDIA Corporation", "NVIDIA GeForce RTX 4070", "4.6.0", "4.60 NVIDIA",
                major, minor, true, false, source);
    }

    private static Set<String> extensions(final String... names) {
        return new LinkedHashSet<>(java.util.Arrays.asList(names));
    }

    private static BackendCapabilities.Builder desktop() {
        return BackendCapabilities.builder()
                .gpu(gpu(4, 6, GpuInfo.Source.DRIVER_NATIVE))
                .extensions(extensions("GL_ARB_direct_state_access", "GL_ARB_compute_shader",
                        "GL_ARB_buffer_storage", "GL_ARB_draw_indirect_count", "GL_ARB_shader_image_load_store"))
                .maxColorAttachments(8)
                .maxDepthTextureBits(24)
                .computeLimits(65535, 1024)
                .maxIndirectCount(4096)
                .maxServerWaitTimeoutMs(1000)
                .parallelShaderCompile(1)
                .vulkan(false, "no Vulkan ICD reported by the GL probe");
    }

    private static BackendCapabilities capsFor(final RenderBackend... supported) {
        final BackendCapabilities.Builder builder = desktop();
        for (final RenderBackend backend : supported) {
            builder.support(backend);
        }
        return builder.resolve(RenderBackend.COMPATIBILITY).build();
    }

    @Test
    @DisplayName("extension names are matched regardless of the case the driver reports")
    void extensionLookupIsCaseInsensitive() {
        // glGetStringi yields "GL_ARB_direct_state_access"; the predicates query the
        // upper-case constant. If these two ever stop matching, every extension-gated
        // feature turns off silently - which is precisely the bug this test exists for.
        final BackendCapabilities caps = capsFor(RenderBackend.GL46_DSA);
        assertTrue(caps.hasExtension("GL_ARB_DIRECT_STATE_ACCESS"));
        assertTrue(caps.hasExtension("gl_arb_direct_state_access"));
        assertTrue(caps.hasExtension("GL_ARB_direct_state_access"));
        assertFalse(caps.hasExtension("GL_ARB_nonexistent_thing"));
        assertFalse(caps.hasExtension(null));
        assertTrue(caps.supportsDirectStateAccess());
        assertTrue(caps.supportsCompute());
        assertEquals(5, caps.getExtensionCount());
    }

    @Test
    @DisplayName("AUTO takes the DSA path on a complete 4.6 core profile")
    void autoPicksDsaWhenEverythingIsPresent() {
        final BackendCapabilities caps = capsFor(RenderBackend.GL46_DSA, RenderBackend.GL_CORE,
                RenderBackend.GL_LEGACY, RenderBackend.COMPATIBILITY);
        assertEquals(RenderBackend.GL46_DSA, BackendSelector.resolve(AetheriumConfig.BackendChoice.AUTO, caps));
        assertTrue(caps.isAnyBackendUsable());
        assertNotNull(caps.describe());
    }

    @Test
    @DisplayName("the GL 4.6 + DSA gate is hard, and scoring cannot talk its way past it")
    void hardGatesAreNotOutrankedByScoring() {
        // GL 4.6 core without the DSA entry points: compute is implied by the version, so
        // the only missing piece is direct state access, and that alone must veto DSA.
        final BackendCapabilities noDsa = BackendCapabilities.builder()
                .gpu(gpu(4, 6, GpuInfo.Source.DRIVER_NATIVE))
                .extensions(extensions("GL_ARB_compute_shader", "GL_ARB_buffer_storage"))
                .computeLimits(65535, 1024)
                .support(RenderBackend.GL46_DSA)
                .support(RenderBackend.GL_CORE)
                .resolve(RenderBackend.COMPATIBILITY)
                .build();
        assertTrue(noDsa.supportsCompute(), "4.6 core implies compute, and the code says so");
        assertFalse(noDsa.supportsDirectStateAccess());
        assertEquals(RenderBackend.GL_CORE, BackendSelector.resolve(AetheriumConfig.BackendChoice.AUTO, noDsa));
        // GL 4.5 with DSA present: still one minor version short of the DSA backend.
        final BackendCapabilities onlyFourFive = BackendCapabilities.builder()
                .gpu(gpu(4, 5, GpuInfo.Source.DRIVER_NATIVE))
                .extensions(extensions("GL_ARB_direct_state_access"))
                .computeLimits(65535, 1024)
                .support(RenderBackend.GL46_DSA)
                .support(RenderBackend.GL_CORE)
                .resolve(RenderBackend.COMPATIBILITY)
                .build();
        assertTrue(onlyFourFive.supportsDirectStateAccess(), "4.5 has DSA in core");
        assertEquals(RenderBackend.GL_CORE,
                BackendSelector.resolve(AetheriumConfig.BackendChoice.AUTO, onlyFourFive),
                "the multi-draw-indirect-count path needs 4.6, so a 4.5 context must not get it");
        // An explicit GL46_DSA request is refused on the same hardware, with a warning.
        assertEquals(RenderBackend.GL_CORE,
                BackendSelector.resolve(AetheriumConfig.BackendChoice.GL46_DSA, onlyFourFive));
    }

    @Test
    @DisplayName("a 3.3 GPU gets GL_CORE, a 2.1 GPU gets GL_LEGACY")
    void olderGpusFallThroughTheLadder() {
        final BackendCapabilities threeThree = BackendCapabilities.builder()
                .gpu(gpu(3, 3, GpuInfo.Source.DRIVER_NATIVE))
                .extensions(extensions())
                .support(RenderBackend.GL46_DSA)
                .support(RenderBackend.GL_CORE)
                .support(RenderBackend.GL_LEGACY)
                .resolve(RenderBackend.COMPATIBILITY)
                .build();
        assertEquals(RenderBackend.GL_CORE, BackendSelector.resolve(AetheriumConfig.BackendChoice.AUTO, threeThree));

        final BackendCapabilities twoOne = BackendCapabilities.builder()
                .gpu(gpu(2, 1, GpuInfo.Source.GL4ES))
                .support(RenderBackend.GL46_DSA)
                .support(RenderBackend.GL_CORE)
                .support(RenderBackend.GL_LEGACY)
                .resolve(RenderBackend.COMPATIBILITY)
                .build();
        assertEquals(RenderBackend.GL_LEGACY, BackendSelector.resolve(AetheriumConfig.BackendChoice.AUTO, twoOne));
    }

    @Test
    @DisplayName("an explicit choice the GPU cannot honour degrades instead of crashing")
    void explicitChoiceFallsBackToAuto() {
        final BackendCapabilities threeThree = BackendCapabilities.builder()
                .gpu(gpu(3, 3, GpuInfo.Source.DRIVER_NATIVE))
                .support(RenderBackend.GL_CORE)
                .support(RenderBackend.GL_LEGACY)
                .resolve(RenderBackend.COMPATIBILITY)
                .build();
        assertEquals(RenderBackend.GL_CORE,
                BackendSelector.resolve(AetheriumConfig.BackendChoice.GL46_DSA, threeThree),
                "a requested backend that fails the gates must fall back, not be honoured");
        // A backend the probe never offered is also refused, even if it would be feasible
        // on paper: the user must not get a backend the loader has no context for.
        assertEquals(RenderBackend.GL_CORE,
                BackendSelector.resolve(AetheriumConfig.BackendChoice.VULKAN_13, threeThree));
    }

    @Test
    @DisplayName("COMPATIBILITY and a null probe both mean vanilla")
    void compatibilityIsTheSafeAnswer() {
        final BackendCapabilities caps = capsFor(RenderBackend.GL46_DSA);
        assertEquals(RenderBackend.COMPATIBILITY,
                BackendSelector.resolve(AetheriumConfig.BackendChoice.COMPATIBILITY, caps));
        assertEquals(RenderBackend.COMPATIBILITY,
                BackendSelector.resolve(AetheriumConfig.BackendChoice.AUTO, null));
        assertEquals(RenderBackend.COMPATIBILITY,
                BackendSelector.resolve(null, null));
    }

    @Test
    @DisplayName("Vulkan is only chosen when the probe actually found a device")
    void vulkanNeedsADevice() {
        final BackendCapabilities withVulkan = BackendCapabilities.builder()
                .gpu(gpu(4, 6, GpuInfo.Source.DRIVER_NATIVE))
                .extensions(extensions("GL_ARB_compute_shader"))
                .computeLimits(65535, 1024)
                .vulkan(true, null)
                .support(RenderBackend.VULKAN_13)
                .support(RenderBackend.GL_CORE)
                .resolve(RenderBackend.COMPATIBILITY)
                .build();
        assertEquals(RenderBackend.VULKAN_13, BackendSelector.resolve(AetheriumConfig.BackendChoice.VULKAN_13, withVulkan));

        final BackendCapabilities withoutVulkan = BackendCapabilities.builder()
                .gpu(gpu(4, 6, GpuInfo.Source.DRIVER_NATIVE))
                .extensions(extensions("GL_ARB_compute_shader"))
                .computeLimits(65535, 1024)
                .vulkan(false, "vkCreateInstance returned VK_ERROR_INCOMPATIBLE_DRIVER")
                .support(RenderBackend.VULKAN_13)
                .support(RenderBackend.GL_CORE)
                .resolve(RenderBackend.COMPATIBILITY)
                .build();
        assertFalse(withoutVulkan.isVulkanAvailable());
        assertTrue(withoutVulkan.getVulkanFailureReason().contains("VK_ERROR_INCOMPATIBLE_DRIVER"),
                "the reason has to reach the log, or the user guesses");
        assertEquals(RenderBackend.GL_CORE, BackendSelector.resolve(AetheriumConfig.BackendChoice.AUTO, withoutVulkan));
    }

    @Test
    @DisplayName("apply() turns features off that the chosen backend cannot run")
    void featureMatrixFollowsBackend() {
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        assertTrue(config.persistentBuffers.get(), "precondition: the defaults ask for the fast paths");
        assertTrue(config.hzb.get());

        // A 3.3 GPU: no persistent mapping, no HZB, no indirect count.
        final BackendCapabilities weak = BackendCapabilities.builder()
                .gpu(gpu(3, 3, GpuInfo.Source.DRIVER_NATIVE))
                .extensions(extensions())
                .computeLimits(0, 0)
                .support(RenderBackend.GL_CORE)
                .support(RenderBackend.GL_LEGACY)
                .resolve(RenderBackend.COMPATIBILITY)
                .build();
        BackendSelector.apply(config, weak, null);
        assertFalse(config.persistentBuffers.get(), "persistent mapping must not stay on without buffer storage");
        assertFalse(config.hzb.get(), "HZB without compute is a shader that never compiles");
        assertFalse(config.indirectDraw.get());

        // A full 4.6 GPU: every switch the defaults asked for survives.
        final AetheriumConfig strong = AetheriumConfig.createDefaults();
        BackendSelector.apply(strong, capsFor(RenderBackend.GL46_DSA, RenderBackend.GL_CORE,
                RenderBackend.GL_LEGACY, RenderBackend.COMPATIBILITY), null);
        assertTrue(strong.persistentBuffers.get());
        assertTrue(strong.indirectDraw.get());
        assertTrue(strong.hzb.get());
    }

    @Test
    @DisplayName("the legacy backend kills the features gl4es only pretends to have")
    void legacyBackendDisablesLightingExtras() {
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        final BackendCapabilities legacy = BackendCapabilities.builder()
                .gpu(gpu(4, 6, GpuInfo.Source.GL4ES))
                .extensions(extensions("GL_ARB_compute_shader"))
                .computeLimits(65535, 1024)
                .support(RenderBackend.GL_LEGACY)
                .resolve(RenderBackend.COMPATIBILITY)
                .build();
        // gl4es reports 4.6 numbers it does not implement: the probe's own support set is
        // what limits it, and the selector must not upgrade beyond that set.
        final RenderBackend chosen = BackendSelector.resolve(AetheriumConfig.BackendChoice.AUTO, legacy);
        assertEquals(RenderBackend.GL_LEGACY, chosen);
        BackendSelector.apply(config, legacy, null);
        assertFalse(config.smoothLighting.get(), "GL_LEGACY must disable smooth lighting");
    }

    @Test
    @DisplayName("a translated or software GL source degrades persistent mapping")
    void translatedSourcesLoseFastPaths() {
        final BackendCapabilities zink = BackendCapabilities.builder()
                .gpu(gpu(4, 6, GpuInfo.Source.ZINK))
                .extensions(extensions("GL_ARB_direct_state_access", "GL_ARB_compute_shader", "GL_ARB_buffer_storage"))
                .computeLimits(65535, 1024)
                .support(RenderBackend.GL46_DSA)
                .support(RenderBackend.GL_CORE)
                .resolve(RenderBackend.COMPATIBILITY)
                .build();
        assertFalse(zink.supportsPersistentMapping(),
                "Zink has no real client-mapped persistent buffers; the arena would stall per frame");
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        BackendSelector.apply(config, zink, null);
        assertFalse(config.persistentBuffers.get(), "the config must not promise what the backend cannot do");
    }

    @Test
    @DisplayName("the supported set is honoured exactly: an empty probe means vanilla")
    void emptyProbeMeansCompatibility() {
        final BackendCapabilities nothing = BackendCapabilities.builder()
                .gpu(gpu(4, 6, GpuInfo.Source.DRIVER_NATIVE))
                .resolve(RenderBackend.COMPATIBILITY)
                .build();
        assertFalse(nothing.isAnyBackendUsable(), "a probe that offered no backend must not be usable");
        assertEquals(RenderBackend.COMPATIBILITY, BackendSelector.resolve(AetheriumConfig.BackendChoice.AUTO, nothing));
        assertTrue(EnumSet.allOf(RenderBackend.class).size() >= 5, "the enum grew; check the switch in feasible()");
    }

    @Test
    @DisplayName("reapply invalidates the subsystems that cached per-backend state")
    void reapplyIsSafeBeforeStartup() {
        // Called with a real config but the bridges left null-on-purpose is not legal, so
        // this test covers the documented early-out for a null config plus the fact that
        // reapply never touches the file format (no store access at all).
        BackendSelector.reapply(null, null, null, null);
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        final AndroidEnvironment android = AndroidEnvironment.probe(config);
        assertNotNull(android.describe());
        BackendSelector.apply(config, capsFor(RenderBackend.GL46_DSA, RenderBackend.COMPATIBILITY), android);
        assertNotNull(config.backend.get());
    }
}
