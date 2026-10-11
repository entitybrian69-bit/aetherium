package com.aetherium.perf;

import com.aetherium.render.CompactTerrain;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Chunk meshes already converted on a worker thread, waiting for their GL upload.
 *
 * <p>Vanilla 1.16.5 finishes a chunk mesh on a worker thread and then uploads it on the render
 * thread, inside the frame. The Aetherium pipeline used to re-encode each mesh at that point, on
 * the render thread, which made every arriving chunk cost frame time twice. Now the worker that
 * built the mesh also encodes it, right before it queues the upload ({@code uploadChunkLayer}), and
 * parks the result here keyed by the vertex builder it came from. The render thread then only
 * calls {@code glBufferData}.</p>
 *
 * <p>Keyed by builder identity: every layer of every in-flight build has its own builder, so two
 * builds of the same section can never pick up each other's data, whatever order their uploads run
 * in.</p>
 */
public final class PreparedUploads {

    /** One converted layer mesh. */
    public static final class Prepared {
        public final ByteBuffer data;
        /** 0 vanilla bytes as built, 1 compact, 2 vanilla reordered into face groups. */
        public final int format;
        /** Group starts and planes (formats 1 and 2), owned by this record. */
        public final CompactTerrain.Layout layout;
        public final int quads;
        /** A conversion error to report on the render thread, or null. */
        public final String error;

        public Prepared(final ByteBuffer data, final int format, final CompactTerrain.Layout layout, final int quads, final String error) {
            this.data = data;
            this.format = format;
            this.layout = layout;
            this.quads = quads;
            this.error = error;
        }
    }

    // Oldest first per builder: a builder's meshes are uploaded in the order they were popped.
    private static final Map<Object, ArrayDeque<Prepared>> BY_BUILDER = new IdentityHashMap<Object, ArrayDeque<Prepared>>();
    private static int pendingCount;
    private static long preparedCount;

    private PreparedUploads() {
    }

    /**
     * Converts one built layer (worker thread). Never throws: data that does not fit, or a
     * conversion error, is kept as the vanilla bytes (format 0).
     *
     * @param shader true for the compact shader format, false for compatible mode (vanilla
     *               vertices reordered into face groups)
     */
    public static Prepared prepare(final ByteBuffer data, final boolean shader) {
        final CompactTerrain.Layout layout = new CompactTerrain.Layout();
        try {
            final CompactTerrain.Scratch scratch = CompactTerrain.scratch();
            final ByteBuffer out = shader ? CompactTerrain.encode(data, scratch, layout)
                    : CompactTerrain.sortQuads(data, scratch, layout);
            if (out != null) {
                return new Prepared(DirectPool.copyOf(out), shader ? 1 : 2, layout, layout.quads(), null);
            }
            return new Prepared(DirectPool.copyOf(data), 0, null, 0, null);
        } catch (final RuntimeException error) {
            return new Prepared(DirectPool.copyOf(data), 0, null, 0, "chunk encode failed: " + error);
        }
    }

    /**
     * Parks a mesh popped from {@code builder}. Several meshes of one builder may wait at once;
     * they are handed out oldest first, in the order of their uploads. None is ever dropped: every
     * popped mesh must be consumed by exactly one upload, or that upload would pop the builder a
     * second time (and a failed pop leaves the builder's read index past its end).
     */
    public static void put(final Object builder, final Prepared prepared) {
        synchronized (BY_BUILDER) {
            ArrayDeque<Prepared> queue = BY_BUILDER.get(builder);
            if (queue == null) {
                queue = new ArrayDeque<Prepared>(2);
                BY_BUILDER.put(builder, queue);
            }
            queue.addLast(prepared);
            pendingCount++;
            preparedCount++;
        }
    }

    /** The oldest mesh parked for this builder, removed; null when its next upload was not prepared. */
    public static Prepared take(final Object builder) {
        synchronized (BY_BUILDER) {
            if (pendingCount == 0) {
                return null;
            }
            final ArrayDeque<Prepared> queue = BY_BUILDER.get(builder);
            if (queue == null) {
                return null;
            }
            final Prepared prepared = queue.pollFirst();
            if (queue.isEmpty()) {
                BY_BUILDER.remove(builder);
            }
            if (prepared != null) {
                pendingCount--;
            }
            return prepared;
        }
    }

    /** Hand the buffer back after the upload. */
    public static void done(final Prepared prepared) {
        if (prepared != null) {
            DirectPool.release(prepared.data);
        }
    }

    /** World unload / renderer reload: drop anything that will never be uploaded. */
    public static void clear() {
        synchronized (BY_BUILDER) {
            for (final ArrayDeque<Prepared> queue : BY_BUILDER.values()) {
                for (final Prepared p : queue) {
                    DirectPool.release(p.data);
                }
            }
            BY_BUILDER.clear();
            pendingCount = 0;
        }
    }

    public static int pending() {
        synchronized (BY_BUILDER) {
            return pendingCount;
        }
    }

    public static long preparedTotal() {
        synchronized (BY_BUILDER) {
            return preparedCount;
        }
    }
}
