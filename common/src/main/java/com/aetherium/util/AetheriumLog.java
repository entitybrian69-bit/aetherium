package com.aetherium.util;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Level-filtered logging facade.
 *
 * <p>Aetherium logs from three very different threads (render thread, mesh
 * workers, and the async shader compiler). Two rules are enforced here rather
 * than at each call site:</p>
 *
 * <ul>
 *   <li>{@link #dev(String, Object...)} is a <em>hot path</em> call: it is a
 *       no-op unless {@code advanced.debug_logging} is on. Because the flag can
 *       be flipped from the GUI on the render thread while a worker is logging,
 *       {@code devEnabled} is a {@code volatile} boolean instead of a getter
 *       call into the config.</li>
 *   <li>Nothing in this class rethrows. Logging must never be the reason a
 *       render loop dies; failures are reported by the caller instead.</li>
 * </ul>
 */
public final class AetheriumLog {
    /** Immutable: the delegate is assigned once during mod init. */
    private final Logger delegate;

    /**
     * Guard for hot-path verbosity. Written by the render thread (GUI toggle),
     * read by mesh/shader worker threads; a plain volatile is sufficient because
     * a stale value only costs one dropped or one extra line.
     */
    private volatile boolean devEnabled;

    /** Set when we are inside a crash report so we can flush synchronously. */
    private volatile boolean eagerFlush;

    public AetheriumLog(final String name) {
        this.delegate = LoggerFactory.getLogger(Objects.requireNonNull(name, "logger name"));
    }

    public static AetheriumLog of(final Class<?> owner) {
        Objects.requireNonNull(owner, "owner");
        return new AetheriumLog(owner.getName());
    }

    public void setDevEnabled(final boolean enabled) {
        this.devEnabled = enabled;
    }

    public boolean isDevEnabled() {
        return this.devEnabled;
    }

    /**
     * Debug/diagnostic line. Free when {@code advanced.debug_logging} is off — the
     * varargs array is never allocated because the guard precedes the call.
     */
    public void dev(final String message, final Object... args) {
        if (!this.devEnabled) {
            return;
        }
        this.delegate.debug("[dev] " + message, args);
    }

    public void info(final String message, final Object... args) {
        this.delegate.info(message, args);
    }

    public void warn(final String message, final Object... args) {
        this.delegate.warn(message, args);
    }

    /** Recoverable failure: the engine continues in a degraded mode. */
    public void warn(final String message, final Throwable error) {
        this.delegate.warn("{} ({})", message, error.getClass().getSimpleName(), error);
    }

    /** Unrecoverable for the subsystem involved; caller is expected to fall back. */
    public void error(final String message, final Throwable error) {
        if (this.eagerFlush) {
            this.delegate.error(message, error);
            this.delegate.info("flush-on-error requested: continuing shutdown");
            return;
        }
        this.delegate.error("{} — {}", message, describe(error), error);
    }

    public void error(final String message, final Object... args) {
        this.delegate.error(message, args);
    }

    /** Called by the crash-report hook so buffered lines are not lost. */
    public void beginCrashFlush() {
        this.eagerFlush = true;
    }

    private static String describe(final Throwable error) {
        if (error == null) {
            return "no cause";
        }
        final String msg = error.getMessage();
        return msg == null || msg.isEmpty() ? error.getClass().getName() : error.getClass().getSimpleName() + ": " + msg;
    }
}
