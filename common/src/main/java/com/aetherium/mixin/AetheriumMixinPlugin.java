package com.aetherium.mixin;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;

import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Decides, before any bytecode is written, whether an Aetherium mixin may touch a
 * class on <em>this</em> Minecraft version.
 *
 * <h2>Why this exists at all</h2>
 * <p>A mixin whose injection target does not exist is a launch failure by default.
 * Two mechanisms fix that: tolerant annotations ({@code require = 0, expect = 0},
 * used on every injection in {@code com.aetherium.mixin.core}), or refusing the mixin
 * up front. Aetherium uses both, because they catch different mistakes: the
 * annotations absorb a renamed <em>method</em>, while this plugin absorbs a version
 * where a whole hook is meaningless (no {@code GuiGraphics} before 1.20.2, no
 * {@code LevelRenderer#onSectionCompleted} before 1.17) and, more importantly, turns
 * "the mod is inert because a HARD conflict was detected" into one boolean instead of
 * a half-transformed game.</p>
 *
 * <h2>Only the parts of {@code IMixinConfigPlugin} that exist in Mixin 0.8.x</h2>
 * <p>This class implements exactly seven methods - {@code onLoad}, {@code getMixins},
 * {@code acceptTargets}, {@code getRefMapperConfig}, {@code shouldApplyMixin}, {@code preApply}
 * and {@code postApply} - in the parameter order the interface declares. There is no {@code acceptTarget} on
 * Mixin 0.8.5 (CI: "does not override or implement a method from a supertype"), which is
 * why the per-target sanity check below lives in {@code postApply}. It is tempting to reach for hooks like {@code mixinAccepted} that appear
 * in newer Mixin forks; doing so turns a port into a compile error with no obvious
 * cause, so the plugin stays on the boring subset. {@code required} is false in
 * {@code aetherium-common.mixins.json}, which means a plugin that throws at load time
 * must not be able to kill the game - hence every method here catches its own
 * exceptions and defaults to "allow".</p>
 *
 * <p>No version <em>strings</em> are compared for feature decisions where a shape can
 * be inspected instead: the lightmap storage question ({@code int[] pixels} versus
 * {@code NativeImage}) is answered at runtime by
 * {@link com.aetherium.gamma.LightmapWriter}, which probes the real class and caches
 * a {@code MethodHandle}. A plugin that hard-codes "1.20.2 uses int[]" is wrong on a
 * re-mapped launcher and right on the day it is written, which is the worst possible
 * combination.</p>
 */
public final class AetheriumMixinPlugin implements IMixinConfigPlugin {
    private static final org.apache.logging.log4j.Logger LOGGER =
            org.apache.logging.log4j.LogManager.getLogger("Aetherium/Mixin");

    /**
     * Mixin simple-name suffix -&gt; inclusive version range where its target exists.
     * {@code "*"} means "every row of PORTING_MATRIX.md that a delta has been verified
     * against"; a range means "do not even try outside this". Entries are added here by
     * {@code tools/gen_deltas.py} when a port needs one - that is the whole point of the
     * table being in source rather than in a config file: it is generated, checked and
     * diffed like the rest of the engine.
     */
    private static final Map<String, String> TARGETED_RANGES = Map.of(
            "core.MinecraftMixin", "*",
            "core.GameRendererMixin", "*",
            "core.LevelRendererMixin", "*",
            "core.LightTextureMixin", "*",
            // Gui#render takes GuiGraphics from 1.20.2; before that the overlay is
            // drawn by the legacy hook in the same class, so the range only gates the
            // descriptor-typed injections.
            "core.GuiMixin", "*",
            // The options-screen hijack needs the video-settings lambda, which exists
            // from 1.17.4 onward (older versions get the fallback button only).
            "core.OptionsScreenMixin", "[1.17.4,)"
    );

    /** AETHERIUM-PORT-GEN: global kill switch, flipped by the conflict scanner. */
    private static volatile boolean disabled;

    /**
     * Called by {@link com.aetherium.compat.ModConflictScanner} when a HARD conflict is
     * detected (a second full renderer, not merely a overlapping feature). Making our
     * mixins inert is the only safe response that does not require unloading someone
     * else's - which cannot be done after transform, whatever a mod claims.
     */
    public static void setMixinActive(final boolean active) {
        disabled = !active;
        LOGGER.info("Aetherium mixins {}", active ? "re-enabled" : "disabled by conflict delegation");
    }

    public static boolean isMixinActive() {
        return !disabled;
    }

    /** Written by {@code Aetherium} before the mixins are applied, read here. */
    private static volatile String announcedVersion = "";

    /**
     * Mixin runs transform-time code before a mod's own entry point on some loaders, so
     * the version is passed in by system property as well: {@code Aetherium} sets
     * {@code aetherium.minecraft.version} during {@code initialize()}, and a loader
     * that transforms earlier still gets a correct answer from the fallback path.
     */
    public static void announceMinecraftVersion(final String version) {
        announcedVersion = version == null ? "" : version;
    }

    private String minecraftVersion = "";

    @Override
    public void onLoad(final String mixinPackage) {
        this.minecraftVersion = firstNonEmpty(System.getProperty("aetherium.minecraft.version", ""), announcedVersion);
        // An empty version is not an error: in a unit-test or datagen JVM there is no
        // game at all. Range checks then simply pass and the tolerant annotations decide.
        LOGGER.info("Aetherium mixin plugin loaded for package {} (MC '{}')", mixinPackage,
                this.minecraftVersion.isEmpty() ? "unknown" : this.minecraftVersion);
    }

    /**
     * null: this plugin does not contribute a list of its own, so the {@code client} array in
     * {@code aetherium-common.mixins.json} stays the single source of which mixins exist. A plugin
     * that returned a list would silently duplicate or shadow that file.
     */
    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public String getRefMapperConfig() {
        // null = the loader's default refmap. Choosing a per-version refmap here would
        // duplicate what the delta's build script already configures, and the two would
        // drift.
        return null;
    }

    /**
     * Abstract on Mixin 0.8.5 (it was the method javac named when the class refused to compile):
     * the place where a plugin can veto mixins that collide with another mod's targets. Aetherium
     * makes exactly one such decision, per mixin, in {@link #shouldApplyMixin}, so both sets stay
     * untouched.
     */
    @Override
    public void acceptTargets(final Set<String> myTargets, final Set<String> otherTargets) {
        // Intentionally empty: no target is exclusive to Aetherium.
    }

    @Override
    public boolean shouldApplyMixin(final String targetClassName, final String mixinClassName) {
        try {
            if (disabled) {
                LOGGER.debug("Skipping {} : Aetherium is standing down for a hard conflict", mixinClassName);
                return false;
            }
            final String suffix = suffixOf(mixinClassName);
            final String range = TARGETED_RANGES.get(suffix);
            if (range == null) {
                // Unlisted means allowed: the map is a list of exceptions, not a
                // whitelist, so adding a mixin is one file instead of two.
                return true;
            }
            if ("*".equals(range) || this.minecraftVersion.isEmpty()) {
                return true;
            }
            if (inRange(this.minecraftVersion, range)) {
                return true;
            }
            LOGGER.warn("Refusing {} : Minecraft {} is outside the supported range {}. The feature is "
                    + "disabled on this version; everything else keeps working.", mixinClassName,
                    this.minecraftVersion, range);
            return false;
        } catch (final RuntimeException | LinkageError error) {
            // Fail open on purpose. A plugin that throws during transform is a crash
            // the user cannot read, and every decision here is an optimisation over
            // require = 0 in the mixin itself.
            LOGGER.warn("Aetherium mixin plugin failed to decide on {}; allowing it and relying on "
                    + "the tolerant injection annotations", mixinClassName, error);
            return true;
        }
    }

    /**
     * Invoked for each accepted mixin with the real target {@link ClassNode}, after the transform.
     * One use: a sanity check that the class the mixin patched looks like the version we think it
     * is, so a wrong range in {@link #TARGETED_RANGES} shows up as one log line at startup instead of
     * as a half-rendered world. There is no {@code acceptTarget} on Mixin 0.8.5 (javac: "method does
     * not override or implement a method from a supertype") - {@code postApply} is the hook that gets
     * the target node, so the check lives in it.
     */
    @Override
    public void postApply(final String targetClassName, final ClassNode targetClassNode,
                          final String mixinClassName, final IMixinInfo mixinInfo) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Applied {} -> {}", mixinClassName, targetClassName);
        }
        if (targetClassNode == null || targetClassNode.methods == null) {
            return;
        }
        final String suffix = suffixOf(mixinClassName);
        if (!"core.GameRendererMixin".equals(suffix) && !"core.LevelRendererMixin".equals(suffix)) {
            return;
        }
        boolean sawLevelPass = false;
        for (final var method : targetClassNode.methods) {
            if (method != null && (method.name.startsWith("renderLevel") || method.name.startsWith("render(")
                    || "renderLevel".equals(method.name))) {
                sawLevelPass = true;
                break;
            }
        }
        if (!sawLevelPass) {
            LOGGER.info("Note: {} was applied to {} but no renderLevel-shaped method was found; if frame "
                    + "stats stay at zero on this version, the level-pass target name needs adding to the "
                    + "candidate list", mixinClassName, targetClassName);
        }
    }

    @Override
    public void preApply(final String targetClassName, final ClassNode targetClassNode,
                         final String mixinClassName, final IMixinInfo mixinInfo) {
        // Nothing to rewrite before transform; every hook lives in its mixin class.
    }

    // ------------------------------------------------------------------- helpers

    private static String suffixOf(final String mixinClassName) {
        final int marker = mixinClassName.indexOf(".mixin.");
        if (marker < 0) {
            return mixinClassName;
        }
        return mixinClassName.substring(marker + ".mixin.".length());
    }

    private static String firstNonEmpty(final String a, final String b) {
        return a == null || a.isEmpty() ? (b == null ? "" : b) : a;
    }

    /**
     * Delegates to {@link VersionRange}, which owns the syntax and the unit tests. The
     * only thing kept here is the log message, because "why is this mixin off" is a
     * mixin question.
     */
    static boolean inRange(final String version, final String range) {
        return com.aetherium.util.VersionRange.contains(version, range);
    }

}
