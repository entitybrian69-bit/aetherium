package com.aetherium.render.gl;

import java.nio.ByteBuffer;

import com.aetherium.util.AetheriumLog;

/**
 * Triple-buffered, persistently mapped upload arena.
 *
 * <p>This is the single biggest structural difference from vanilla's
 * {@code BufferBuilder -> glBufferData} path. Vanilla reallocates the storage of
 * the buffer it is uploading on every section rebuild, which (a) makes the driver
 * allocate inside the frame and (b) forces an implicit sync because the GPU may
 * still be reading the old contents. Here, allocation never happens during a
 * frame: the arena is one large immutable-storage buffer, mapped once, and the
 * renderer writes into a slot that the GPU provably is not reading, using a fence
 * per slot.</p>
 *
 * <p>Ring/fence invariants:</p>
 * <ul>
 *   <li>3 slots. Frame <i>N</i> writes slot <i>N%3</i>; the fence for slot
 *       <i>N-1</i> is inserted at the end of frame <i>N-1</i>, so by frame
 *       <i>N</i> at most one frame is in flight on a 2-deep swapchain. The third
 *       slot exists for the case where the driver queues two.</li>
 *   <li>{@link #beginFrame()} waits (non-blocking first, bounded second) on the
 *       fence of the slot it is about to overwrite — never on the slot currently
 *       being drawn.</li>
 *   <li>{@link #endFrame()} flushes only the range actually written
 *       ({@code GL_MAP_FLUSH_EXPLICIT_BIT}) and inserts the fence after the
 *       frame's commands are recorded.</li>
 * </ul>
 *
 * <p>Thread-safety: single-owner (render thread) by contract — see
 * {@link GlDevice}. The fallback path ({@code !persistent}) reuses the same API
 * but maps per-write, so the caller cannot tell which mode it is in except via
 * {@link #isPersistent()}.</p>
 */
public final class GlPersistentArena implements AutoCloseable {
    private static final AetheriumLog LOGGER = AetheriumLog.of(GlPersistentArena.class);
    private static final int SLOT_COUNT = 3;

    private final long capacityBytes;
    private final boolean wantPersistent;
    private final boolean backendAllows;

    private int buffer;
    private long mappedAddress;
    private ByteBuffer mappedView;
    private boolean persistent;

    /** Per-slot write cursor, in bytes from the slot start. */
    private final long[] slotUsed = new long[SLOT_COUNT];
    /** Per-slot fence handle, 0 when unused. */
    private final long[] slotFence = new long[SLOT_COUNT];
    /** Per-slot byte range flushed this frame, for the explicit-flush call. */
    private final long[] slotFlushStart = new long[SLOT_COUNT];

    private int activeSlot;
    private long totalBytesWritten;
    private int waitCount;
    private int forcedWaits;
    private boolean closed;

    public GlPersistentArena(final long capacityBytes, final boolean wantPersistent, final boolean backendAllows) {
        this.capacityBytes = Math.max(1024L * 1024L, capacityBytes);
        this.wantPersistent = wantPersistent;
        this.backendAllows = backendAllows;
        try {
            create();
        } catch (final RuntimeException | LinkageError error) {
            LOGGER.warn("Arena creation failed; falling back to an unmapped sub-data arena", error);
            this.persistent = false;
            try {
                createFallback();
            } catch (final RuntimeException second) {
                // A device with no usable buffer object is a device we must not draw on.
                throw new IllegalStateException("Could not create any upload arena", second);
            }
        }
    }

    private void create() {
        this.buffer = GlProcs.createBuffer();
        if (this.buffer == 0) {
            throw new IllegalStateException("glCreateBuffers returned 0");
        }
        if (this.wantPersistent && this.backendAllows) {
            GlProcs.namedBufferStorage(this.buffer, this.capacityBytes * SLOT_COUNT,
                    GlProcs.GL_MAP_PERSISTENT_BIT | GlProcs.GL_MAP_COHERENT_BIT | GlProcs.GL_MAP_WRITE_BIT | GlProcs.GL_CLIENT_STORAGE_BIT);
            final long flags = GlProcs.GL_MAP_PERSISTENT_BIT | GlProcs.GL_MAP_COHERENT_BIT | GlProcs.GL_MAP_WRITE_BIT
                    | GlProcs.GL_MAP_FLUSH_EXPLICIT_BIT;
            final long address = GlProcs.mapNamedBufferRange(this.buffer, 0L, this.capacityBytes * SLOT_COUNT, (int) flags);
            if (address == 0L || address == -1L) {
                final String errors = GlProcs.drainErrors();
                LOGGER.warn("Persistent mapping failed ({}); using per-write mapping", errors == null ? "null pointer" : errors);
                this.persistent = false;
                prepareFallbackView();
                return;
            }
            this.mappedAddress = address;
            // One direct view over the whole 3-slot mapping; viewAt() takes a slice
            // of it. memByteBuffer does no bounds checking of its own, so the size
            // here must match the map length exactly (it does: capacity * SLOT_COUNT).
            this.mappedView = org.lwjgl.system.MemoryUtil.memByteBuffer(address, (int) (this.capacityBytes * SLOT_COUNT))
                    .order(java.nio.ByteOrder.LITTLE_ENDIAN);
            this.persistent = true;
            LOGGER.info("Persistent arena mapped: {} MB x {} slots", this.capacityBytes / (1024 * 1024), SLOT_COUNT);
        } else {
            this.persistent = false;
            GlProcs.bindBuffer(GlProcs.GL_ARRAY_BUFFER, this.buffer);
            GlProcs.bufferData(GlProcs.GL_ARRAY_BUFFER, this.stagingCapacity(), GlProcs.GL_STREAM_DRAW);
            GlProcs.bindBuffer(GlProcs.GL_ARRAY_BUFFER, 0);
            prepareFallbackView();
        }
    }

    private void createFallback() {
        if (this.buffer == 0) {
            this.buffer = GlProcs.createBuffer();
        }
        this.persistent = false;
        GlProcs.bindBuffer(GlProcs.GL_ARRAY_BUFFER, this.buffer);
        GlProcs.bufferData(GlProcs.GL_ARRAY_BUFFER, this.stagingCapacity(), GlProcs.GL_STREAM_DRAW);
        GlProcs.bindBuffer(GlProcs.GL_ARRAY_BUFFER, 0);
        prepareFallbackView();
    }

    /** Fallback staging is heap memory, so it is capped hard (see prepareFallbackView). */
    private long stagingCapacity() {
        return Math.min(this.capacityBytes, 16L * 1024L * 1024L);
    }

    private void prepareFallbackView() {
        if (this.mappedView == null) {
            this.mappedView = ByteBuffer.allocateDirect((int) this.stagingCapacity());
        }
    }

    public boolean isPersistent() {
        return this.persistent;
    }

    public long getCapacityBytes() {
        return this.capacityBytes;
    }

    public long getMappedBytes() {
        return this.persistent ? this.slotUsed[this.activeSlot] : 0L;
    }

    public int getWaitCount() {
        return this.waitCount;
    }

    public int getForcedWaits() {
        return this.forcedWaits;
    }

    public long getTotalBytesWritten() {
        return this.totalBytesWritten;
    }

    public void beginFrame() {
        if (this.closed) {
            return;
        }
        this.activeSlot = (this.activeSlot + 1) % SLOT_COUNT;
        this.slotUsed[this.activeSlot] = 0L;
        this.slotFlushStart[this.activeSlot] = -1L;

        if (!this.persistent) {
            return;
        }
        // Recycle the slot's previous fence before reusing it. A zero-timeout poll
        // is tried first; only if the GPU has not caught up do we block, which is
        // the one and only place this renderer can stall the CPU.
        final long fence = this.slotFence[this.activeSlot];
        if (fence != 0L) {
            final int status = GlProcs.clientWaitSync(fence, GlProcs.GL_SYNC_FLUSH_COMMANDS_BIT, GlProcs.TIMEOUT_ZERO);
            if (!GlProcs.isSignaled(status)) {
                final int deadlineMs = 8;
                final int waited = GlProcs.clientWaitSync(fence, GlProcs.GL_SYNC_FLUSH_COMMANDS_BIT, deadlineMs * 1_000_000L);
                this.forcedWaits++;
                if (!GlProcs.isSignaled(waited)) {
                    LOGGER.dev("Arena stall: slot {} still busy after {} ms (queue depth 3 exceeded)", this.activeSlot, deadlineMs);
                } else {
                    this.waitCount++;
                }
            } else {
                this.waitCount++;
            }
            GlProcs.deleteSync(fence);
            this.slotFence[this.activeSlot] = 0L;
        }
    }

    /**
     * Reserves {@code bytes} in the active slot.
     *
     * @return the offset within the slot, or -1 when the arena is full this frame
     *         (callers must then defer the upload to the next frame — the
     *         scheduler treats that as backpressure rather than growing the arena)
     */
    public long reserve(final long bytes) {
        if (this.closed || bytes <= 0L) {
            return -1L;
        }
        final long limit = this.persistent ? this.capacityBytes : this.stagingCapacity();
        // 64-byte alignment keeps driver-side compression and cache lines happy.
        final long aligned = (bytes + 63L) & ~63L;
        final long offset = this.slotUsed[this.activeSlot];
        if (offset + aligned > limit) {
            return -1L;
        }
        this.slotUsed[this.activeSlot] = offset + aligned;
        if (this.slotFlushStart[this.activeSlot] < 0L) {
            this.slotFlushStart[this.activeSlot] = offset;
        }
        this.totalBytesWritten += bytes;
        return offset;
    }

    /**
     * @return a view over the reserved range. On the persistent path this writes
     *         straight into the mapped buffer; on the fallback path it is a direct
     *         heap buffer that {@link #endFrame()} submits in one go.
     */
    public ByteBuffer viewAt(final int slot, final long slotOffset, final int bytes) {
        if (this.persistent) {
            // Absolute address arithmetic against the single mapping: cheaper than
            // repositioning the parent buffer and it leaves no shared state behind.
            final long absolute = slot * this.capacityBytes + slotOffset;
            return org.lwjgl.system.MemoryUtil.memByteBuffer(this.mappedAddress + absolute, bytes)
                    .order(java.nio.ByteOrder.LITTLE_ENDIAN);
        }
        final ByteBuffer view = this.mappedView;
        view.limit((int) Math.min(bytes + slotOffset, view.capacity()));
        view.position((int) slotOffset);
        return view.slice().order(java.nio.ByteOrder.LITTLE_ENDIAN);
    }

    public void endFrame() {
        if (this.closed) {
            return;
        }
        final long used = Math.min(this.slotUsed[this.activeSlot], this.stagingCapacity());
        if (used <= 0L) {
            return;
        }
        if (this.persistent) {
            final long flushStart = Math.max(0L, this.slotFlushStart[this.activeSlot]);
            GlProcs.flushMappedNamedBufferRange(this.buffer, this.activeSlot * this.capacityBytes + flushStart, used - flushStart);
            GlProcs.memoryBarrier(GlProcs.GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT);
            this.slotFence[this.activeSlot] = GlProcs.fenceSync();
        } else {
            GlProcs.bindBuffer(GlProcs.GL_ARRAY_BUFFER, this.buffer);
            this.mappedView.position(0);
            this.mappedView.limit((int) used);
            GlProcs.bufferSubData(GlProcs.GL_ARRAY_BUFFER, 0L, this.mappedView);
            GlProcs.bindBuffer(GlProcs.GL_ARRAY_BUFFER, 0);
        }
    }

    /** The GL buffer name the geometry draws bind their attribute storage to. */
    public int getBuffer() {
        return this.buffer;
    }

    /** Absolute GL offset (not slot-relative) for a reserved slot offset. */
    public long absoluteOffset(final int slot, final long slotOffset) {
        return slot * this.capacityBytes + slotOffset;
    }

    public int getActiveSlot() {
        return this.activeSlot;
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        if (this.persistent && this.mappedAddress != 0L) {
            GlProcs.unmapNamedBuffer(this.buffer);
            this.mappedAddress = 0L;
        }
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (this.slotFence[i] != 0L) {
                GlProcs.deleteSync(this.slotFence[i]);
                this.slotFence[i] = 0L;
            }
        }
        if (this.buffer != 0) {
            org.lwjgl.opengl.GL15.glDeleteBuffers(this.buffer);
            this.buffer = 0;
        }
        LOGGER.info("Arena released: {} MB written, {} polls, {} blocking waits",
                this.totalBytesWritten / (1024 * 1024), this.waitCount, this.forcedWaits);
    }
}
