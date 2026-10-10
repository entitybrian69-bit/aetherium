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

    public static int appliedCount() {
        return APPLIED.size();
    }

    public static Set<String> applied() {
        return Collections.unmodifiableSet(APPLIED);
    }

    /** Whether the mixin with this simple name should be applied on this build. */
    static boolean enabledOnThisVersion(final String simpleName) {
        if ("LightLevelMixin".equals(simpleName) || "EntityLightMixin".equals(simpleName)) {
            return Capabilities.DYNAMIC_LIGHTS;
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
    }

    private static String simpleName(final String className) {
        if (className.startsWith(PACKAGE)) {
            return className.substring(PACKAGE.length());
        }
        final int dot = className.lastIndexOf('.');
        return dot < 0 ? className : className.substring(dot + 1);
    }
}
