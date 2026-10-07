package com.aetherium.platform;

import java.util.Iterator;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import com.aetherium.util.AetheriumLog;

/**
 * Resolves the single {@link PlatformAdapter} for the current loader.
 *
 * <p>{@code ServiceLoader} is used instead of compile-time dispatch so that the
 * common module stays loader-free (this is what makes the per-version deltas in
 * {@code deltas/<version>/} textual rather than structural). The lookup happens
 * once and the result is cached in a {@code volatile} field: the platform module
 * registers its adapter from its own entry point, which on NeoForge can run
 * before the first {@link #current()} call from a worker thread, so the field is
 * not final.</p>
 */
public final class PlatformServices {
    private static final AetheriumLog LOGGER = AetheriumLog.of(PlatformServices.class);

    /**
     * Guarded by nothing: publication of an immutable, fully-constructed
     * adapter through a volatile write is sufficient (double-checked read, no
     * reassignment after first success).
     */
    private static volatile PlatformAdapter cached;
    private static volatile boolean searched;

    private PlatformServices() {
    }

    /** Explicit registration from the loader entry point (preferred path). */
    public static void register(final PlatformAdapter adapter) {
        Objects.requireNonNull(adapter, "adapter");
        if (cached != null && cached != adapter) {
            LOGGER.warn("PlatformAdapter registered twice: keeping {}, ignoring {}", cached.platformName(), adapter.platformName());
            return;
        }
        cached = adapter;
        searched = true;
        LOGGER.info("Platform adapter active: {}", adapter.platformName());
    }

    public static PlatformAdapter current() {
        PlatformAdapter local = cached;
        if (local != null) {
            return local;
        }
        local = discover();
        if (local == null) {
            throw new IllegalStateException(
                    "No Aetherium PlatformAdapter available. The mod jar is missing its loader entry point — "
                            + "check that fabric/src/main/java/com/aetherium/fabric/AetheriumFabric.java (or the "
                            + "NeoForge equivalent) is packaged and that META-INF/services is present.");
        }
        return local;
    }

    /** @return the discovered adapter, or null when running headless (unit tests) */
    public static PlatformAdapter currentOrNull() {
        PlatformAdapter local = cached;
        if (local != null) {
            return local;
        }
        return discover();
    }

    private static synchronized PlatformAdapter discover() {
        if (cached != null) {
            return cached;
        }
        if (searched) {
            return null;
        }
        searched = true;
        try {
            final ServiceLoader<PlatformAdapter> loader = ServiceLoader.load(PlatformAdapter.class, PlatformServices.class.getClassLoader());
            final Iterator<PlatformAdapter> iterator = loader.iterator();
            while (iterator.hasNext()) {
                try {
                    final PlatformAdapter candidate = iterator.next();
                    if (candidate != null && candidate.isClient()) {
                        cached = candidate;
                        LOGGER.dev("PlatformAdapter discovered via ServiceLoader: {}", candidate.platformName());
                        return candidate;
                    }
                } catch (final ServiceConfigurationError error) {
                    // One malformed provider must not hide a good one.
                    LOGGER.warn("Skipping a broken PlatformAdapter provider", error);
                }
            }
        } catch (final ServiceConfigurationError error) {
            LOGGER.warn("ServiceLoader for PlatformAdapter failed; falling back to explicit registration", error);
        }
        return null;
    }

    /** Test seam: clears the cached adapter so unit tests stay independent. */
    public static void resetForTests() {
        cached = null;
        searched = false;
    }
}
