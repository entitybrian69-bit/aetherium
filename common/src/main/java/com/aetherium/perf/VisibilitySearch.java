package com.aetherium.perf;

/**
 * Rate limit for vanilla 1.16.5's visible-section search ({@code LevelRenderer.setupRender}).
 *
 * <p>Vanilla repeats the whole search every frame in which any visible section is dirty or waiting
 * to be meshed, and every frame after chunk uploads, so while terrain streams in it runs every
 * frame although the result only changes when a mesh finishes. Those three triggers are held to
 * one search per {@link #INTERVAL_NANOS}; a held trigger is remembered and fires as soon as the
 * interval has passed. Camera movement and rotation are not routed through here and still update
 * at once. Render thread only.</p>
 */
public final class VisibilitySearch {

    /** 20 searches a second at most from the streaming triggers. */
    public static final long INTERVAL_NANOS = 50_000_000L;

    private static long lastSearch = Long.MIN_VALUE / 2;
    private static boolean pending;
    private static boolean window = true;
    private static long held;

    private VisibilitySearch() {
    }

    /** Start of {@code setupRender}: whether streaming triggers may fire this frame. */
    public static void beginFrame(final long now, final boolean throttle) {
        window = !throttle || now - lastSearch >= INTERVAL_NANOS;
    }

    /**
     * The search itself ran (vanilla cleared its update flag). Closes this frame's window too:
     * vanilla's dirty-section loop runs after the search and would otherwise request the next one
     * for the very next frame.
     */
    public static void searched(final long now) {
        lastSearch = now;
        pending = false;
        window = false;
    }

    /**
     * {@code !chunksToCompile.isEmpty()} in vanilla's update condition.
     *
     * @return the value vanilla should see ("empty" while the trigger is held)
     */
    public static boolean compileQueueEmpty(final boolean empty) {
        if (empty || window) {
            return empty;
        }
        pending = true;
        held++;
        return true;
    }

    /** A visible section is dirty or queued: whether vanilla may request a search now. */
    public static boolean dirtyTrigger() {
        if (window) {
            return true;
        }
        pending = true;
        held++;
        return false;
    }

    /** A held trigger whose interval has passed (once). */
    public static boolean takePending() {
        if (pending && window) {
            pending = false;
            return true;
        }
        return false;
    }

    /**
     * Chunk uploads finished this frame (end of the frame, after the search): whether vanilla
     * should request a search. Shares the interval with the other triggers.
     */
    public static boolean afterUploads(final boolean uploaded, final long now) {
        if (uploaded) {
            pending = true;
        }
        if (pending && now - lastSearch >= INTERVAL_NANOS) {
            pending = false;
            return true;
        }
        return false;
    }

    /** Triggers held back since start (diagnostics). */
    public static long held() {
        return held;
    }

    static void resetForTests() {
        lastSearch = Long.MIN_VALUE / 2;
        pending = false;
        window = true;
        held = 0;
    }
}
