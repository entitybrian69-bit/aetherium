package com.aetherium.perf;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe pool of direct byte buffers in power-of-two sizes, for chunk meshes prepared on
 * worker threads and uploaded on the render thread. Reusing them avoids allocating (and later
 * garbage-collecting) a direct buffer per chunk upload, which on a phone heap shows up as GC
 * pauses while new terrain streams in.
 *
 * <p>A buffer that is never returned is simply garbage-collected; nothing leaks.</p>
 */
public final class DirectPool {
    /** Smallest bucket: 64 KiB. */
    static final int MIN_SHIFT = 16;
    /** Largest pooled bucket: 16 MiB; bigger requests are allocated and dropped. */
    static final int MAX_SHIFT = 24;
    /** Buffers kept per bucket. */
    static final int PER_BUCKET = 6;
    /** Total bytes kept idle across all buckets. */
    static final long MAX_IDLE_BYTES = 48L << 20;

    private static final int BUCKETS = MAX_SHIFT - MIN_SHIFT + 1;
    @SuppressWarnings("unchecked")
    private static final ConcurrentLinkedQueue<ByteBuffer>[] FREE = new ConcurrentLinkedQueue[BUCKETS];
    private static final AtomicInteger[] COUNT = new AtomicInteger[BUCKETS];
    private static final AtomicLong IDLE = new AtomicLong();
    private static final AtomicLong ALLOCATED = new AtomicLong();

    static {
        for (int i = 0; i < BUCKETS; i++) {
            FREE[i] = new ConcurrentLinkedQueue<ByteBuffer>();
            COUNT[i] = new AtomicInteger();
        }
    }

    private DirectPool() {
    }

    static int bucketFor(final int bytes) {
        int shift = MIN_SHIFT;
        while (shift < MAX_SHIFT && (1 << shift) < bytes) {
            shift++;
        }
        return (1 << shift) >= bytes ? shift - MIN_SHIFT : -1;
    }

    /** A native-order buffer with position 0 and limit {@code bytes}. */
    public static ByteBuffer acquire(final int bytes) {
        final int bucket = bucketFor(Math.max(1, bytes));
        ByteBuffer buffer = null;
        if (bucket >= 0) {
            buffer = FREE[bucket].poll();
            if (buffer != null) {
                COUNT[bucket].decrementAndGet();
                IDLE.addAndGet(-buffer.capacity());
            } else {
                buffer = ByteBuffer.allocateDirect(1 << (bucket + MIN_SHIFT)).order(ByteOrder.nativeOrder());
                ALLOCATED.incrementAndGet();
            }
        } else {
            buffer = ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder());
            ALLOCATED.incrementAndGet();
        }
        buffer.clear();
        buffer.limit(bytes);
        return buffer;
    }

    /** Copies {@code source} (position to limit) into a pooled buffer, flipped for reading. */
    public static ByteBuffer copyOf(final ByteBuffer source) {
        final ByteBuffer src = source.duplicate();
        final ByteBuffer out = acquire(src.remaining());
        out.put(src);
        out.flip();
        return out;
    }

    public static void release(final ByteBuffer buffer) {
        if (buffer == null || !buffer.isDirect()) {
            return;
        }
        final int capacity = buffer.capacity();
        if (Integer.bitCount(capacity) != 1) {
            return;
        }
        final int bucket = bucketFor(capacity);
        if (bucket < 0 || (1 << (bucket + MIN_SHIFT)) != capacity) {
            return;
        }
        if (COUNT[bucket].incrementAndGet() > PER_BUCKET || IDLE.get() + capacity > MAX_IDLE_BYTES) {
            COUNT[bucket].decrementAndGet();
            return;
        }
        IDLE.addAndGet(capacity);
        FREE[bucket].offer(buffer);
    }

    /** Drops every idle buffer (world unload). */
    public static void trim() {
        for (int i = 0; i < BUCKETS; i++) {
            ByteBuffer b;
            while ((b = FREE[i].poll()) != null) {
                COUNT[i].decrementAndGet();
                IDLE.addAndGet(-b.capacity());
            }
        }
    }

    public static long idleBytes() {
        return IDLE.get();
    }

    public static long allocations() {
        return ALLOCATED.get();
    }
}
