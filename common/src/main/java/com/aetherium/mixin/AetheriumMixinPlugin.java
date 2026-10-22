package com.aetherium.mixin;

import com.aetherium.Capabilities;
import com.aetherium.perf.WorkerThreads;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Decides which mixins apply and records which ones did.
 *
 * <p>The old plugin gated mixins on a Minecraft-version system property. That
 * could never work: the property was set by {@code Aetherium.initialize()},
 * which runs <em>after</em> the Minecraft classes have been transformed, so the
 * gate always saw "unknown". Every jar is now built for exactly one Minecraft
 * version and the per-version truth lives in {@link Capabilities}, a set of
 * compile-time constants chosen by the era engine. A mixin whose capability is
 * false targets a class that exists but has nothing to hook, so it is skipped
 * here rather than applied empty.</p>
 *
 * <p>This class sits outside the mixin package ({@code com.aetherium.mixin.core})
 * on purpose: {@code Aetherium} references it directly, and classes inside a
 * mixin package may not be loaded by normal code.</p>
 */
public final class AetheriumMixinPlugin implements IMixinConfigPlugin {

    private static final String PACKAGE = "com.aetherium.mixin.core.";

    private static final Set<String> FAST_MODEL_MIXINS = Collections.unmodifiableSet(new java.util.HashSet<String>(
            java.util.Arrays.asList("ModelCubeMixin", "ModelPolygonMixin", "ModelVertexMixin",
                    "DefaultedVertexAccessor", "FastModelMixin")));

    private static final Set<String> VISIBLE_ANIMATION_MIXINS = Collections.unmodifiableSet(new java.util.HashSet<String>(
            java.util.Arrays.asList("SpriteActivityMixin", "BakedQuadAccessor", "BlockSpriteUseMixin",
                    "ItemSpriteUseMixin", "SectionSpritesMixin", "CompiledChunkSpritesMixin")));

    /** Simple names of mixins that were actually applied (render thread reads, loader writes). */
    private static final Set<String> APPLIED = ConcurrentHashMap.newKeySet();

    private static volatile boolean mixinActive = true;
    private static volatile String minecraftVersion = "unknown";

    // ------------------------------------------------------------------ public API

    /** Global kill switch used when a conflicting mod asks us to stand down. Hooks check it each call. */
    public static void setMixinActive(final boolean active) {
        mixinActive = active;
    }

    public static boolean isMixinActive() {
        return mixinActive;
    }

    /** Informational only (shown on the Backend page); no gating depends on it. */
    public static void announceMinecraftVersion(final String version) {
        if (version != null && !version.isEmpty()) {
            minecraftVersion = version;
        }
    }

    public static String minecraftVersion() {
        return minecraftVersion;
    }

    /** @param simpleName mixin class simple name, e.g. {@code "WeatherMixin"} */
    public static boolean isApplied(final String simpleName) {
        return APPLIED.contains(simpleName);
    }

    /** Whether the dynamic-light hooks are in place this launch (always, where the version supports them). */
    public static boolean lightHooksInstalled() {
        return APPLIED.contains("LightLevelMixin");
    }

    public static int appliedCount() {
        return APPLIED.size();
    }

    public static Set<String> applied() {
        return Collections.unmodifiableSet(APPLIED);
    }

    /** Whether the mixin with this simple name should be applied on this build. */
    static boolean enabledOnThisVersion(final String simpleName) {
        if ("LightLevelMixin".equals(simpleName) || "EntityLightMixin".equals(simpleName)) {
            // These hook vanilla's per-vertex light lookup, which runs millions of times while
            // chunks are meshed. They are allocation-free return-value modifiers that cost one
            // volatile read while no light source exists, so they are always installed and
            // dynamic lights switch on and off live.
            return Capabilities.DYNAMIC_LIGHTS;
        }
        if (simpleName.startsWith("Tuning")) {
            return com.aetherium.perf.TuningSupport.mixin(simpleName);
        }
        if (simpleName.startsWith("Qol")) {
            return com.aetherium.qol.QolSupport.mixin(simpleName);
        }
        if ("LazyDataFixersMixin".equals(simpleName)) {
            // Lazy DataFixerUpper (1.10.0), unless a mod already does it.
            return com.aetherium.perf.TuningSupport.mixin(simpleName)
                    && !classPresent("me/steinborn/lazydfu/mixin/SchemasMixin.class");
        }
        if ("RenderScaleMixin".equals(simpleName) || "MainTargetAccessor".equals(simpleName)
                || "GlowSeenMixin".equals(simpleName)) {
            return Capabilities.RENDER_SCALE;
        }
        if ("OptionInstanceMixin".equals(simpleName)) {
            return Capabilities.GAMMA_INSTANCE;
        }
        if ("ChunkUploadMixin".equals(simpleName)) {
            return Capabilities.SMOOTH_CHUNK_UPLOADS;
        }
        if ("UtilThreadsMixin".equals(simpleName)) {
            return !Capabilities.THREADS_BY_PROPERTY;
        }
        if ("EntityOcclusionMixin".equals(simpleName) || "VisibleSectionsMixin".equals(simpleName)) {
            return Capabilities.ENTITY_OCCLUSION;
        }
        if ("RenderChunkInfoMixin".equals(simpleName)) {
            // Its target, LevelRenderer$RenderChunkInfo, only exists on 1.16.5-1.20.1; the
            // experimental 1.16.5 renderer reaches the sections through it as well.
            return Capabilities.CHUNK_INFO_LIST
                    && (Capabilities.ENTITY_OCCLUSION || Capabilities.AETHERIUM_RENDERER);
        }
        if ("ChunkRendererMixin".equals(simpleName) || "VertexBufferMixin".equals(simpleName)
                || "RenderChunkBuffersMixin".equals(simpleName) || "FogStateMixin".equals(simpleName)) {
            return Capabilities.AETHERIUM_RENDERER;
        }
        if ("KeyMappingAccessor".equals(simpleName) || "ZoomFovMixin".equals(simpleName)
                || "ZoomMouseMixin".equals(simpleName) || "ZoomCameraMixin".equals(simpleName)) {
            return Capabilities.ZOOM;
        }
        if ("NearBuildMixin".equals(simpleName)) {
            return Capabilities.THREADED_NEAR_BUILDS;
        }
        if ("PacketBudgetMixin".equals(simpleName) || "BlockableEventLoopInvoker".equals(simpleName)) {
            return Capabilities.PACKET_BUDGET;
        }
        if ("MeshAllocMixin".equals(simpleName) || "AoFaceAllocMixin".equals(simpleName)) {
            return Capabilities.MESH_ALLOC;
        }
        if ("RenderChunkDirtyMixin".equals(simpleName) || "CoalesceMixin".equals(simpleName)) {
            return Capabilities.COALESCE && !Capabilities.COALESCE_EXTRACT;
        }
        if ("CoalesceDirtyStateMixin".equals(simpleName) || "CoalesceExtractMixin".equals(simpleName)) {
            return Capabilities.COALESCE_EXTRACT;
        }
        if ("StreamingLevelMixin".equals(simpleName)) {
            return Capabilities.STREAMING;
        }
        if ("FramePacingMixin".equals(simpleName)) {
            return Capabilities.FRAME_PACING;
        }
        if (VISIBLE_ANIMATION_MIXINS.contains(simpleName)) {
            return Capabilities.VISIBLE_ANIMATIONS;
        }
        if ("VisibleSpritesMixin".equals(simpleName)) {
            return Capabilities.VISIBLE_ANIMATION_PASS;
        }
        if ("SectionCullBufferMixin".equals(simpleName) || "SectionCullOwnerMixin".equals(simpleName)
                || "SectionCullLayerMixin".equals(simpleName)) {
            return Capabilities.SECTION_CULL;
        }
        if ("SectionCullMeshDataMixin".equals(simpleName) || "SectionCullSortMixin".equals(simpleName)
                || "SectionCullDrawMixin".equals(simpleName)) {
            return Capabilities.SECTION_CULL_MODERN;
        }
        if ("SectionCullBuffersMixin".equals(simpleName) || "SectionCullUploadMixin".equals(simpleName)) {
            return Capabilities.SECTION_CULL_SRD;
        }
        if ("SectionCullCompiledMixin".equals(simpleName)) {
            return Capabilities.SECTION_CULL_MESH;
        }
        if (FAST_MODEL_MIXINS.contains(simpleName)) {
            return Capabilities.FAST_MODELS;
        }
        if ("Matrix3fAccessor".equals(simpleName)) {
            return Capabilities.FAST_MODELS_MOJANG;
        }
        if ("PoseNormalsAccessor".equals(simpleName)) {
            return Capabilities.FAST_MODELS_TRUSTED;
        }
        if ("ChunkPrepareMixin".equals(simpleName)) {
            // Converts meshes for the Aetherium pipeline, so it needs the pipeline's buffer hooks.
            return Capabilities.STREAMING && Capabilities.AETHERIUM_RENDERER;
        }
        if ("BufferBuilderAccessor".equals(simpleName)) {
            // The pipeline's mesh conversion (1.16.5) and the fast model path (..1.20.6).
            return (Capabilities.STREAMING && Capabilities.AETHERIUM_RENDERER) || Capabilities.FAST_MODELS;
        }
        if ("SearchThrottleMixin".equals(simpleName)) {
            return Capabilities.SEARCH_THROTTLE;
        }
        if ("FogCullMixin".equals(simpleName)) {
            return Capabilities.FOG_CULL;
        }
        if ("FogCullExtractMixin".equals(simpleName)) {
            return Capabilities.FOG_CULL_EXTRACT;
        }
        if ("RenderChunkInfoAccessor".equals(simpleName)) {
            return Capabilities.FOG_CULL_INFO;
        }
        if ("SectionOcclusionGraphAccessor".equals(simpleName)) {
            return Capabilities.FOG_CULL_SECTION;
        }
        if ("FastParticleMixin".equals(simpleName)) {
            return Capabilities.FAST_PARTICLES;
        }
        if ("FrustumCaptureMixin".equals(simpleName) || "FrustumArmMixin".equals(simpleName)) {
            return Capabilities.FRAME_FRUSTUM;
        }
        if ("MeshExecutorMixin".equals(simpleName)) {
            return Capabilities.MESH_THREADS;
        }
        if ("FogControlMixin".equals(simpleName) || "SkyMixin".equals(simpleName)
                || "ParticleTypeMixin".equals(simpleName) || "BlockParticleMixin".equals(simpleName)
                || "CloudHeightMixin".equals(simpleName)) {
            return Capabilities.SCENERY_CONTROLS;
        }
        if ("FreelookMouseMixin".equals(simpleName) || "FreelookCameraMixin".equals(simpleName)) {
            return Capabilities.FREELOOK;
        }
        if ("GuiMixin".equals(simpleName)) {
            return Capabilities.HUD_OVERLAY || Capabilities.VIGNETTE_TOGGLE;
        }
        return true;
    }

    // ------------------------------------------------------------------ IMixinConfigPlugin

    @Override
    public void onLoad(final String mixinPackage) {
        // Runs before Minecraft's Util class initializes, the only moment the pool size can be set.
        WorkerThreads.applyEarly(WorkerThreads.defaultConfigFile());
        try {
            // Fabric Loader 0.15+ and NeoForge initialise MixinExtras themselves; this covers older
            // loaders, where the copy bundled in the Aetherium jar has to be started by hand.
            // Idempotent when it is already running.
            com.llamalad7.mixinextras.MixinExtrasBootstrap.init();
        } catch (final Throwable error) {
            // Without MixinExtras the light hooks fail to apply (require = 0) and lights stay off.
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(final String targetClassName, final String mixinClassName) {
        return enabledOnThisVersion(simpleName(mixinClassName));
    }

    @Override
    public void acceptTargets(final Set<String> myTargets, final Set<String> otherTargets) {
        // Every mod's mixins are known now and nothing is applied yet: step aside where another
        // mod already changes the same method as one of the performance options.
        Overlap.resolve(myTargets);
        com.aetherium.util.AetheriumLog.of(Overlap.class).info("Aetherium overlap check: {}", Overlap.summary());
        for (final String note : Overlap.notes()) {
            com.aetherium.util.AetheriumLog.of(Overlap.class).info("Another mod already changes this, option {}", note);
        }
        SmokeTest.targets(myTargets);
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(final String targetClassName, final ClassNode targetClass,
                         final String mixinClassName, final IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(final String targetClassName, final ClassNode targetClass,
                          final String mixinClassName, final IMixinInfo mixinInfo) {
        APPLIED.add(simpleName(mixinClassName));
        SmokeTest.applied(targetClass, mixinClassName);
    }

    private static boolean classPresent(final String resource) {
        final ClassLoader loader = AetheriumMixinPlugin.class.getClassLoader();
        return loader != null && loader.getResource(resource) != null;
    }

    private static String simpleName(final String className) {
        if (className.startsWith(PACKAGE)) {
            return className.substring(PACKAGE.length());
        }
        final int dot = className.lastIndexOf('.');
        return dot < 0 ? className : className.substring(dot + 1);
    }
}
