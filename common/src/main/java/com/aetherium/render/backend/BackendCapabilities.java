package com.aetherium.render.backend;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable result of the runtime capability probe. Computed once after context
 * creation (and again on a backend hot-swap); every consumer reads the same
 * object, so "the config says Vulkan but the GPU says no" is answered from one
 * place.
 */
public final class BackendCapabilities {
    private final GpuInfo gpu;
    private final Set<String> extensions;
    private final int maxColorAttachments;
    private final int maxDepthTextureBits;
    private final int maxComputeWorkGroupCount;
    private final int maxComputeWorkGroupSize;
    private final int maxElementsIndirectCount;
    private final int maxServerWaitTimeoutMs;
    private final int parallelShaderCompileSupport;
    private final boolean vulkanAvailable;
    private final String vulkanFailureReason;
    private final Set<RenderBackend> supported;
    private final RenderBackend resolved;

    private BackendCapabilities(final Builder builder) {
        this.gpu = builder.gpu;
        // Case-insensitive by construction: glGetStringi reports "GL_ARB_direct_state_access"
        // while the predicates below query the upper-case constant. A plain HashSet let the
        // two differ, which made every extension-gated capability read false on real drivers
        // - a silent, total failure of the feature matrix.
        final Set<String> frozenExtensions = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        frozenExtensions.addAll(builder.extensions);
        this.extensions = Collections.unmodifiableSet(frozenExtensions);
        this.maxColorAttachments = builder.maxColorAttachments;
        this.maxDepthTextureBits = builder.maxDepthTextureBits;
        this.maxComputeWorkGroupCount = builder.maxComputeWorkGroupCount;
        this.maxComputeWorkGroupSize = builder.maxComputeWorkGroupSize;
        this.maxElementsIndirectCount = builder.maxElementsIndirectCount;
        this.maxServerWaitTimeoutMs = builder.maxServerWaitTimeoutMs;
        this.parallelShaderCompileSupport = builder.parallelShaderCompileSupport;
        this.vulkanAvailable = builder.vulkanAvailable;
        this.vulkanFailureReason = builder.vulkanFailureReason;
        this.supported = Collections.unmodifiableSet(new LinkedHashSet<>(builder.supported));
        this.resolved = builder.resolved;
    }

    public static Builder builder() {
        return new Builder();
    }

    public GpuInfo getGpu() {
        return this.gpu;
    }

    public Set<String> getExtensions() {
        return this.extensions;
    }

    public boolean hasExtension(final String name) {
        // The set's comparator already ignores case, so no normalisation is needed here.
        return name != null && this.extensions.contains(name);
    }

    public int getExtensionCount() {
        return this.extensions.size();
    }

    public int getMaxColorAttachments() {
        return this.maxColorAttachments;
    }

    public int getMaxDepthTextureBits() {
        return this.maxDepthTextureBits;
    }

    public int getMaxComputeWorkGroupCount() {
        return this.maxComputeWorkGroupCount;
    }

    public int getMaxComputeWorkGroupSize() {
        return this.maxComputeWorkGroupSize;
    }

    public int getMaxElementsIndirectCount() {
        return this.maxElementsIndirectCount;
    }

    public int getMaxServerWaitTimeoutMs() {
        return this.maxServerWaitTimeoutMs;
    }

    /** 0 = unsupported, 1 = ARB_parallel_shader_compile, 2 = KHR_ + driver thread budget. */
    public int getParallelShaderCompileSupport() {
        return this.parallelShaderCompileSupport;
    }

    public boolean isAsyncShaderCompileUsable() {
        return this.parallelShaderCompileSupport > 0;
    }

    public boolean isVulkanAvailable() {
        return this.vulkanAvailable;
    }

    public String getVulkanFailureReason() {
        return this.vulkanFailureReason;
    }

    public Set<RenderBackend> getSupported() {
        return this.supported;
    }

    public RenderBackend getResolved() {
        return this.resolved;
    }

    public boolean isAnyBackendUsable() {
        return this.resolved != RenderBackend.COMPATIBILITY || !this.supported.isEmpty();
    }

    public boolean supportsDirectStateAccess() {
        return hasExtension("GL_ARB_DIRECT_STATE_ACCESS") && this.gpu.getGlLevel() >= 450;
    }

    public boolean supportsBufferStorage() {
        return hasExtension("GL_ARB_BUFFER_STORAGE") || this.gpu.getGlLevel() >= 440;
    }

    /** Persistent mapping needs storage + coherent bits; without it the arena degrades. */
    public boolean supportsPersistentMapping() {
        return supportsBufferStorage()
                && (hasExtension("GL_ARB_buffer_storage") || this.gpu.getGlLevel() >= 440)
                && !this.gpu.isTranslated();
    }

    /**
     * Offsets-instead-of-pointers form of MDI. Without it the GPU-side count is
     * unusable and the batcher degrades to a fixed CPU count.
     */
    public boolean supportsIndirectParameters() {
        return this.gpu.getGlLevel() >= 460 || hasExtension("GL_ARB_INDIRECT_PARAMETERS");
    }

    public boolean supportsIndirectCount() {
        return this.gpu.getGlLevel() >= 460 || hasExtension("GL_ARB_DRAW_INDIRECT_COUNT");
    }

    public boolean supportsCompute() {
        return this.gpu.getGlLevel() >= 430 || hasExtension("GL_ARB_COMPUTE_SHADER");
    }

    public boolean supportsShaderImageLoad() {
        return supportsCompute() && (this.gpu.getGlLevel() >= 420 || hasExtension("GL_ARB_SHADER_IMAGE_LOAD_STORE_MULTI_SAMPLE"));
    }

    /** HZB requires compute + image stores + a depth texture we can sample as an image. */
    public boolean supportsHierarchicalZ() {
        return supportsShaderImageLoad() && this.maxComputeWorkGroupCount > 1 && !this.gpu.isSoftware();
    }

    public String describe() {
        return "GL" + this.gpu.getGlMajor() + '.' + this.gpu.getGlMinor()
                + (this.gpu.isCoreProfile() ? "core" : "compat")
                + " dsa=" + supportsDirectStateAccess()
                + " pmap=" + supportsPersistentMapping()
                + " mdi=" + supportsIndirectCount()
                + " hzb=" + supportsHierarchicalZ()
                + " vulkan=" + this.vulkanAvailable
                + " (" + this.gpu.describe() + ')';
    }

    /** Builder keeps the probe code linear; the result is frozen on {@link #build()}. */
    public static final class Builder {
        private GpuInfo gpu;
        private final Set<String> extensions = new LinkedHashSet<>();
        private int maxColorAttachments = 4;
        private int maxDepthTextureBits = 24;
        private int maxComputeWorkGroupCount = 1;
        private int maxComputeWorkGroupSize = 16;
        private int maxElementsIndirectCount = 1;
        private int maxServerWaitTimeoutMs;
        private int parallelShaderCompileSupport;
        private boolean vulkanAvailable;
        private String vulkanFailureReason = "not probed";
        private final Set<RenderBackend> supported = new LinkedHashSet<>();
        private RenderBackend resolved = RenderBackend.COMPATIBILITY;

        public Builder gpu(final GpuInfo value) {
            this.gpu = Objects.requireNonNull(value, "gpu");
            return this;
        }

        public Builder extensions(final Set<String> values) {
            if (values != null) {
                this.extensions.addAll(values);
            }
            return this;
        }

        public Builder maxColorAttachments(final int value) {
            this.maxColorAttachments = value;
            return this;
        }

        public Builder maxDepthTextureBits(final int value) {
            this.maxDepthTextureBits = value;
            return this;
        }

        public Builder computeLimits(final int groupCount, final int groupSize) {
            this.maxComputeWorkGroupCount = groupCount;
            this.maxComputeWorkGroupSize = groupSize;
            return this;
        }

        public Builder maxIndirectCount(final int value) {
            this.maxElementsIndirectCount = value;
            return this;
        }

        public Builder maxServerWaitTimeoutMs(final int value) {
            this.maxServerWaitTimeoutMs = value;
            return this;
        }

        public Builder parallelShaderCompile(final int level) {
            this.parallelShaderCompileSupport = level;
            return this;
        }

        public Builder vulkan(final boolean available, final String reason) {
            this.vulkanAvailable = available;
            this.vulkanFailureReason = Objects.requireNonNullElse(reason, available ? "available" : "unavailable");
            return this;
        }

        public Builder support(final RenderBackend backend) {
            this.supported.add(Objects.requireNonNull(backend, "backend"));
            return this;
        }

        public Builder resolve(final RenderBackend backend) {
            this.resolved = Objects.requireNonNull(backend, "backend");
            return this;
        }

        public BackendCapabilities build() {
            if (this.gpu == null) {
                throw new IllegalStateException("GpuInfo must be probed before building capabilities");
            }
            return new BackendCapabilities(this);
        }
    }
}
