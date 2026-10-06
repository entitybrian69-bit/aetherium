package dev.aetherium.engine.gl;

import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL44;
import org.lwjgl.opengl.GL45;

import java.nio.ByteBuffer;

/**
 * Triple-buffered persistently mapped ring buffer (GL_ARB_buffer_storage + DSA).
 * Writes go straight into driver memory; a fence per region prevents overwriting data the GPU is still reading.
 */
public final class PersistentMappedBuffer implements AutoCloseable {
    private static final int REGIONS = 3;
    private static final int FLAGS = GL44.GL_MAP_WRITE_BIT | GL44.GL_MAP_PERSISTENT_BIT | GL44.GL_MAP_COHERENT_BIT;

    private final int id;
    private final long regionSize;
    private final ByteBuffer mapped;
    private final long[] fences = new long[REGIONS];
    private int region;
    private long writeOffset;

    public PersistentMappedBuffer(long regionSizeBytes) {
        this.regionSize = align(regionSizeBytes, 256);
        this.id = GL45.glCreateBuffers();
        GL45.glNamedBufferStorage(id, regionSize * REGIONS, FLAGS);
        this.mapped = GL45.glMapNamedBufferRange(id, 0, regionSize * REGIONS, FLAGS);
        if (mapped == null) throw new IllegalStateException("glMapNamedBufferRange returned null");
    }

    public int id() { return id; }
    public long regionSize() { return regionSize; }

    /** Advance to the next region, blocking only if the GPU is 3 frames behind. */
    public void nextFrame() {
        region = (region + 1) % REGIONS;
        long fence = fences[region];
        if (fence != 0L) {
            int r;
            do {
                r = GL32.glClientWaitSync(fence, GL32.GL_SYNC_FLUSH_COMMANDS_BIT, 1_000_000L);
            } while (r == GL32.GL_TIMEOUT_EXPIRED);
            GL32.glDeleteSync(fence);
            fences[region] = 0L;
        }
        writeOffset = 0;
    }

    /** Mark the current region as in-flight; call after the last draw that reads from it. */
    public void fenceCurrent() {
        if (fences[region] != 0L) GL32.glDeleteSync(fences[region]);
        fences[region] = GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
    }

    /** Reserve {@code bytes} in the current region; returns the absolute buffer offset or -1 if full. */
    public long allocate(long bytes, int alignment) {
        long off = align(writeOffset, alignment);
        if (off + bytes > regionSize) return -1L;
        writeOffset = off + bytes;
        return region * regionSize + off;
    }

    /** Slice of the mapping starting at an absolute offset returned by {@link #allocate}. */
    public ByteBuffer slice(long absoluteOffset, int bytes) {
        ByteBuffer dup = mapped.duplicate();
        dup.position((int) absoluteOffset);
        dup.limit((int) (absoluteOffset + bytes));
        return dup.slice();
    }

    public long bytesUsedThisFrame() { return writeOffset; }

    @Override public void close() {
        for (int i = 0; i < REGIONS; i++) if (fences[i] != 0L) GL32.glDeleteSync(fences[i]);
        GL45.glUnmapNamedBuffer(id);
        GL45.glDeleteBuffers(id);
    }

    private static long align(long v, int a) { return (v + a - 1) & -(long) a; }
}
