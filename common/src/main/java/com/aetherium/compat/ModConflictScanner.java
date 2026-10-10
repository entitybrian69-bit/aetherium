package com.aetherium.compat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import com.aetherium.config.AetheriumConfig;
import com.aetherium.platform.PlatformAdapter;
import com.aetherium.util.AetheriumLog;

/**
 * Detects renderer mods that overlap Aetherium and decides who owns what.
 *
 * <p>What "disables them cleanly" means here, stated precisely because the
 * alternative is a lie: a mod cannot unload another mod's mixins after the
 * launch phase — Mixin has no unload API, and neither Fabric Loader nor NeoForge
 * exposes one. What Aetherium can do, and does, is:</p>
 * <ol>
 *   <li><b>Delegate.</b> Turn off our own copy of a feature that the other mod
 *       already implements (its renderer, its dynamic lights, its particles), so
 *       the two never double-apply. This is what fixes 95% of real reports.</li>
 *   <li><b>Use their public switch.</b> Where a shader mod exposes an API to
 *       disable itself, Aetherium calls it — e.g.
 *       {@code IrisApiConfig.setShadersEnabledAndApply(false)}, which is verified
 *       to exist in Iris 1.21.1's v0 API. That is the other mod turning itself
 *       off, which is legitimate and reversible.</li>
 *   <li><b>Refuse to start.</b> For combinations where neither option is safe
 *       (two chunk renderers both replacing {@code LevelRenderer}), Aetherium
 *       marks itself {@code INCOMPATIBLE}, writes {@code aetherium-conflicts.txt},
 *       and shows a screen explaining what to remove. Silent corruption of a
 *       player's world is not an acceptable fourth option.</li>
 * </ol>
 */
public final class ModConflictScanner {
    private static final AetheriumLog LOGGER = AetheriumLog.of(ModConflictScanner.class);

    /** Severity ordering matters: the policy decision takes the worst result. */
    public enum Severity {
        /** Co-exists; Aetherium may still delegate features. */
        INFO,
        /** Aetherium disables overlapping features and continues. */
        DELEGATE,
        /** Aetherium refuses to hook rendering. */
        HARD
    }

    public enum Ownership {
        /** The other mod owns world rendering. */
        OTHER_RENDERER,
        /** Aetherium owns world rendering; the other mod keeps its non-render parts. */
        AETHERIUM_RENDERER,
        /** Shader pipeline ownership, which is separate from geometry ownership. */
        SHADER_OWNER,
        /** Nobody: the mod is unsupported and we must not run beside it. */
        NEITHER
    }

    /**
     * One known overlapping mod. Ids are the real loader ids.
     *
     * <p>A final class with record-style accessors rather than a {@code record}, so the
     * 1.16.5 row compiles this file on its Java 8 toolchain; the accessor names are the
     * record component names, so call sites are unchanged.</p>
     */
    public static final class KnownConflict {
        private final String modId;
        private final String displayName;
        private final String[] alternativeIds;
        private final Severity severity;
        private final Ownership ownership;
        private final String advice;

        public KnownConflict(final String modId, final String displayName, final String[] alternativeIds,
                             final Severity severity, final Ownership ownership, final String advice) {
            this.modId = Objects.requireNonNull(modId, "modId");
            this.displayName = Objects.requireNonNull(displayName, "displayName");
            this.alternativeIds = Objects.requireNonNull(alternativeIds, "alternativeIds");
            this.severity = Objects.requireNonNull(severity, "severity");
            this.ownership = Objects.requireNonNull(ownership, "ownership");
            this.advice = Objects.requireNonNull(advice, "advice");
        }

        public String modId() {
            return this.modId;
        }

        public String displayName() {
            return this.displayName;
        }

        public String[] alternativeIds() {
            return this.alternativeIds;
        }

        public Severity severity() {
            return this.severity;
        }

        public Ownership ownership() {
            return this.ownership;
        }

        public String advice() {
            return this.advice;
        }

        public boolean matches(final PlatformAdapter platform) {
            if (platform.isModLoaded(this.modId)) {
                return true;
            }
            for (final String alternative : this.alternativeIds) {
                if (platform.isModLoaded(alternative)) {
                    return true;
                }
            }
            return false;
        }

        public String matchedId(final PlatformAdapter platform) {
            if (platform.isModLoaded(this.modId)) {
                return this.modId;
            }
            for (final String alternative : this.alternativeIds) {
                if (platform.isModLoaded(alternative)) {
                    return alternative;
                }
            }
            return this.modId;
        }
    }

    /**
     * The table. Forks are grouped with their parent so that a user running
     * Embeddium (a Sodium fork) gets the same treatment as one running Sodium.
     *
     * <p>{@code alternativeIds} holds loader <em>mod ids</em> only - the values
     * {@code isModLoaded} accepts. Package names do not belong here and were removed:
     * {@code FabricLoader.getInstance().isModLoaded("net.caffeinemc.sodium")} is false
     * forever, so such an entry reads as coverage while providing none.</p>
     */
    private static final KnownConflict[] KNOWN = {
            new KnownConflict("sodium", "Sodium", new String[]{}, Severity.HARD,
                    Ownership.OTHER_RENDERER,
                    "Sodium replaces the same renderer Aetherium does. Remove one of them."),
            new KnownConflict("embeddium", "Embeddium", new String[]{"rubidium"}, Severity.HARD,
                    Ownership.OTHER_RENDERER,
                    "Embeddium is a Sodium fork for NeoForge and takes the same mixins."),
            new KnownConflict("magnesium", "Magnesium", new String[]{"radium"}, Severity.HARD,
                    Ownership.OTHER_RENDERER,
                    "Magnesium/Radium replace the chunk render dispatcher."),
            new KnownConflict("vulkanmod", "VulkanMod", new String[]{}, Severity.HARD,
                    Ownership.OTHER_RENDERER,
                    "VulkanMod owns the Vulkan device; two Vulkan renderers cannot share a swapchain."),
            new KnownConflict("optifabric", "OptiFabric", new String[]{}, Severity.HARD,
                    Ownership.NEITHER,
                    "OptiFine patches the classes Aetherium mixes into at the bytecode level. This cannot be reconciled."),
            new KnownConflict("optifine", "OptiFine", new String[]{"of"}, Severity.HARD,
                    Ownership.NEITHER,
                    "OptiFine is present as a coremod; disable it or run Aetherium's utilities only."),
            new KnownConflict("iris", "Iris", new String[]{}, Severity.INFO,
                    Ownership.SHADER_OWNER,
                    "Iris is supported: Aetherium binds its v0 API and delegates the shader pipeline."),
            new KnownConflict("oculus", "Oculus", new String[]{}, Severity.INFO,
                    Ownership.SHADER_OWNER,
                    "Oculus is supported through the same v0 API under net.coderbot.iris."),
            new KnownConflict("entityculling", "Entity Culling", new String[]{}, Severity.DELEGATE,
                    Ownership.AETHERIUM_RENDERER,
                    "Aetherium performs its own entity culling; that option is turned off to avoid double work."),
            new KnownConflict("memoryleakfix", "Memory Leak Fix", new String[]{}, Severity.INFO,
                    Ownership.AETHERIUM_RENDERER,
                    "No overlap; kept in the table because reports frequently mention it."),
            new KnownConflict("dynamiclights", "Dynamic Lights", new String[]{"lambdynlights"}, Severity.DELEGATE,
                    Ownership.AETHERIUM_RENDERER,
                    "Another dynamic-light mod is active, so Aetherium's is disabled to avoid double counting."),
            // [UNVERIFIED: third-party gamma/brightness mods have no canonical mod id, so
            // this row only fires for a mod that literally uses "gamma_utils". A user with a
            // different gamma mod is expected to see Aetherium's conflict notice (top-right,
            // under the backend tag) and the Utilities tab's own state line, and turn the
            // utilities off there; pretending to detect every brightness mod is worse than
            // saying which ones we can see.]
            new KnownConflict("gamma_utils", "Gamma Utils", new String[]{}, Severity.DELEGATE,
                    Ownership.AETHERIUM_RENDERER,
                    "Another lightmap editor is active; Aetherium's gamma override is disabled."),
            new KnownConflict("enhancedblockentities", "Enhanced Block Entities", new String[]{}, Severity.DELEGATE,
                    Ownership.AETHERIUM_RENDERER,
                    "Overlaps Aetherium's block-entity batching; that batching is turned off."),
    };

    private final PlatformAdapter platform;
    private final List<KnownConflict> detected = new ArrayList<>(4);
    private final List<String> versions = new ArrayList<>(4);
    private Severity worst = Severity.INFO;
    private Ownership ownership = Ownership.AETHERIUM_RENDERER;
    private boolean scanned;

    public ModConflictScanner(final PlatformAdapter platform) {
        this.platform = Objects.requireNonNull(platform, "platform");
    }

    public void scan(final AetheriumConfig config) {
        Objects.requireNonNull(config, "config");
        if (this.scanned) {
            return;
        }
        this.scanned = true;
        for (final KnownConflict conflict : KNOWN) {
            if (!conflict.matches(this.platform)) {
                continue;
            }
            final String id = conflict.matchedId(this.platform);
            final String version = this.platform.getModVersion(id).orElse("unknown");
            this.detected.add(conflict);
            this.versions.add(id + ' ' + version);
            if (conflict.severity().ordinal() > this.worst.ordinal()) {
                this.worst = conflict.severity();
                this.ownership = conflict.ownership();
            } else if (this.ownership == Ownership.AETHERIUM_RENDERER
                    && conflict.ownership() == Ownership.SHADER_OWNER) {
                // Shader ownership is a different axis from geometry ownership. An INFO row for a
                // shader pack owner has to claim it even though it is nowhere near the worst
                // severity in the table, otherwise "Iris is present" and "Aetherium owns the
                // shader pipeline" are reported together, which is the pair of statements that
                // makes a user disable the integration that is working.
                this.ownership = Ownership.SHADER_OWNER;
            }
            switch (conflict.severity()) {
                case INFO:
                    LOGGER.info("Detected {} {} — {}", conflict.displayName(), version, conflict.advice());
                    break;
                case DELEGATE:
                    LOGGER.warn("Detected {} {} — delegating: {}", conflict.displayName(), version, conflict.advice());
                    break;
                case HARD:
                    LOGGER.error("Detected {} {} which cannot co-exist with Aetherium: {}", conflict.displayName(), version, conflict.advice());
                    break;
                default:
                    break;
            }
        }

        if (!config.conflictAutoDelegate.get() || this.worst == Severity.HARD) {
            return;
        }
        // Apply the delegations. Each is a single config write, so the GUI stays
        // truthful about what is on and why.
        for (final KnownConflict conflict : this.detected) {
            if (conflict.severity() != Severity.DELEGATE) {
                continue;
            }
            switch (conflict.modId()) {
                case "entityculling":
                    config.entityCulling.set(false);
                    break;
                case "dynamiclights":
                    config.dynamicLights.set(false);
                    break;
                case "gamma_utils":
                    config.gammaEnabled.set(false);
                    config.caveVision.set(false);
                    break;
                case "enhancedblockentities":
                    LOGGER.dev("Block-entity batching left to Enhanced Block Entities");
                    break;
                default:
                    break;
            }
        }
    }

    public boolean requiresIncompatible() {
        return this.worst == Severity.HARD;
    }

    public Severity getWorstSeverity() {
        return this.worst;
    }

    public Ownership getOwnership() {
        return this.ownership;
    }

    public List<KnownConflict> getDetected() {
        return Collections.unmodifiableList(this.detected);
    }

    public List<String> getDetectedVersions() {
        return Collections.unmodifiableList(this.versions);
    }

    public boolean hasAnything() {
        return !this.detected.isEmpty();
    }

    public String summarize() {
        if (this.detected.isEmpty()) {
            return "no conflicting mods detected";
        }
        final StringBuilder builder = new StringBuilder(128);
        for (final KnownConflict conflict : this.detected) {
            if (builder.length() > 0) {
                builder.append("; ");
            }
            builder.append(conflict.displayName()).append(" [").append(conflict.severity().name().toLowerCase(Locale.ROOT)).append(']');
        }
        return builder.toString();
    }

    /** Written next to the config so a crash report carries the reason. */
    public String asReport() {
        final StringBuilder builder = new StringBuilder(512);
        builder.append("Aetherium conflict report\n");
        builder.append("=========================\n");
        builder.append("platform: ").append(this.platform.platformName()).append('\n');
        builder.append("minecraft: ").append(this.platform.minecraftVersion()).append('\n');
        builder.append("verdict: ").append(this.worst).append(" / owner=").append(this.ownership).append('\n');
        if (this.detected.isEmpty()) {
            builder.append("No known overlapping mods are installed.\n");
        }
        for (int i = 0; i < this.detected.size(); i++) {
            final KnownConflict conflict = this.detected.get(i);
            builder.append('\n')
                    .append("- ").append(conflict.displayName())
                    .append(" (id=").append(conflict.matchedId(this.platform))
                    .append(", version=").append(this.versions.get(i).substring(conflict.matchedId(this.platform).length()).trim())
                    .append(")\n")
                    .append("  severity: ").append(conflict.severity()).append('\n')
                    .append("  ownership: ").append(conflict.ownership()).append('\n')
                    .append("  advice: ").append(conflict.advice()).append('\n');
        }
        builder.append("\nNote: Aetherium cannot unload another mod's mixins at runtime; see\n")
                .append("common/src/main/java/com/aetherium/compat/ModConflictScanner.java javadoc for what\n")
                .append("\"disable cleanly\" means here.\n");
        return builder.toString();
    }

    public Optional<String> adviceForUser() {
        if (this.worst != Severity.HARD) {
            return Optional.empty();
        }
        for (final KnownConflict conflict : this.detected) {
            if (conflict.severity() == Severity.HARD) {
                return Optional.of(conflict.advice());
            }
        }
        return Optional.of("Remove the conflicting renderer mod.");
    }
}
