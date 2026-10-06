package com.aetherium.render.gl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.system.MemoryUtil;

import com.aetherium.render.backend.BackendCapabilities;
import com.aetherium.render.hzb.HierarchicalDepthBuffer;
import com.aetherium.util.AetheriumLog;

/**
 * GPU-side indirect draw batching with compute-driven compaction.
 *
 * <p>Two GPU objects drive everything:</p>
 * <ul>
 *   <li><b>command buffer</b> — an array of {@code DrawCmd} (count, instanceCount,
 *       firstIndex, baseVertex, baseInstance = 5 x u32, stride 20), the exact
 *       layout {@code glMultiDrawElementsIndirect*} requires.</li>
 *   <li><b>parameter buffer</b> — one u32 survivor count written by the cull
 *       kernel with an atomic, consumed as the {@code drawcount} argument.</li>
 * </ul>
 *
 * <p>This is why the design beats a CPU loop: there is no readback anywhere. The
 * CPU never learns how many sections survived; it issues one call and the driver
 * reads its own count. The price is ordering — the command stream must exist
 * before the cull pass runs, so commands carry their section bounds in a parallel
 * SSBO and the kernel compacts both arrays together.</p>
 *
 * <p>Thread-safety: render-thread only. {@link #staging} is native memory
 * allocated in the constructor and reused every frame, so a frame never
 * allocates. If {@code GL_ARB_indirect_parameters} is absent the count buffer is
 * still maintained (useful in the stats) but the draw uses the CPU-side count,
 * which is the honest "batching without culling" fallback.</p>
 */
public final class GlIndirectBatch implements AutoCloseable {
    private static final AetheriumLog LOGGER = AetheriumLog.of(GlIndirectBatch.class);

    /** bytes per DrawCmd: indexCount, instanceCount, firstIndex, baseVertex, baseInstance. */
    public static final int COMMAND_BYTES = 20;
    public static final int COMMAND_INTS = COMMAND_BYTES / Integer.BYTES;
    /** vec4 per command in the bounds SSBO (ndc min.xy, max.z, pad). */
    public static final int BOUNDS_BYTES = 16;

    private final int capacityCommands;
    private final BackendCapabilities capabilities;
    private final GlPersistentArena arena;

    private final int commandBuffer;
    private final int countBuffer;
    private final int boundsBuffer;

    /** Native staging for the command stream: allocated once, never per frame. */
    private final ByteBuffer commandStage;
    private final ByteBuffer boundsStage;

    /** Frame-local cursors; render-thread only, no lock. */
    private int stagedCommands;
    private int framesWithDraws;
    private long totalDrawCommands;
    private long lastSubmittedBytes;
    private boolean cullPassUsable = true;
    private boolean closed;

    public GlIndirectBatch(final int capacityCommands, final GlPersistentArena arena, final BackendCapabilities capabilities) {
        this.capacityCommands = Math.max(256, capacityCommands);
        this.arena = arena;
        this.capabilities = capabilities;
        this.commandStage = MemoryUtil.memAlloc(this.capacityCommands * COMMAND_BYTES).order(ByteOrder.LITTLE_ENDIAN);
        this.boundsStage = MemoryUtil.memAlloc(this.capacityCommands * BOUNDS_BYTES).order(ByteOrder.LITTLE_ENDIAN);
        this.commandBuffer = GlProcs.createBuffer();
        this.countBuffer = GlProcs.createBuffer();
        this.boundsBuffer = GlProcs.createBuffer();
        GlProcs.namedBufferData(this.commandBuffer, (long) this.capacityCommands * COMMAND_BYTES, null, GlProcs.GL_DYNAMIC_DRAW);
        GlProcs.namedBufferData(this.boundsBuffer, (long) this.capacityCommands * BOUNDS_BYTES, null, GlProcs.GL_DYNAMIC_DRAW);
        GlProcs.namedBufferData(this.countBuffer, 4L, null, GlProcs.GL_DYNAMIC_DRAW);
        // An uninitialised survivor count is a draw of 4 billion instances, so the
        // buffer is zeroed here rather than relying on the first frame clearing it.
        GlProcs.clearNamedBufferU32(this.countBuffer);
        LOGGER.info("Indirect batch: {} command slots ({} KB commands, {} KB bounds), parameterBuffer={}",
                this.capacityCommands, this.capacityCommands * COMMAND_BYTES / 1024,
                this.capacityCommands * BOUNDS_BYTES / 1024, capabilities.supportsIndirectParameters());
    }

    /** Starts a frame's command stream. Must be called before any {@link #append}. */
    public void beginBatch() {
        this.stagedCommands = 0;
        this.commandStage.clear();
        this.boundsStage.clear();
    }

    /**
     * Appends one section's draw plus its conservative NDC bounds.
     *
     * @param indexCount   indices this command consumes from the shared index buffer
     * @param firstIndex   start offset in indices
     * @param baseVertex   vertex bias applied to every index
     * @param ndcMinX      screen-space box min x, normalised 0..1
     * @param ndcMinY      screen-space box min y, normalised 0..1
     * @param ndcNearDepth nearest depth of the box, 0..1 (1 = far plane)
     * @param ndcMaxX      screen-space box max x, normalised 0..1
     * @return false when the batch is full and the caller must defer the section
     */
    public boolean append(final int indexCount, final int firstIndex, final int baseVertex,
                          final float ndcMinX, final float ndcMinY, final float ndcNearDepth, final float ndcMaxX) {
        if (this.closed || this.stagedCommands >= this.capacityCommands) {
            return false;
        }
        final IntBuffer commands = this.commandStage.asIntBuffer();
        final int commandBase = this.stagedCommands * COMMAND_INTS;
        commands.put(commandBase, indexCount);
        commands.put(commandBase + 1, 1);
        commands.put(commandBase + 2, firstIndex);
        commands.put(commandBase + 3, baseVertex);
        commands.put(commandBase + 4, 0);

        final FloatBuffer bounds = this.boundsStage.asFloatBuffer();
        final int boundBase = this.stagedCommands * 4;
        bounds.put(boundBase, ndcMinX);
        bounds.put(boundBase + 1, ndcMinY);
        bounds.put(boundBase + 2, ndcNearDepth);
        bounds.put(boundBase + 3, ndcMaxX);

        this.stagedCommands++;
        return true;
    }

    /** Uploads the staged command + bounds arrays. Called by {@link #submit}. */
    private void upload() {
        final int commandBytes = this.stagedCommands * COMMAND_BYTES;
        final int boundsBytes = this.stagedCommands * BOUNDS_BYTES;
        if (commandBytes <= 0) {
            return;
        }
        this.commandStage.position(0).limit(commandBytes);
        this.boundsStage.position(0).limit(boundsBytes);
        // One sub-data per buffer per frame. With a persistent arena these could be
        // written directly into the mapped slot; that variant lives behind
        // GL_LEGACY/GL_CORE today because the section VAO's buffer needs stable
        // storage, and glNamedBufferData on a DSA buffer is a driver-cheap op.
        GlProcs.bindBuffer(GlProcs.GL_COPY_WRITE_BUFFER, this.commandBuffer);
        GlProcs.bufferSubData(GlProcs.GL_COPY_WRITE_BUFFER, 0L, this.commandStage);
        GlProcs.bindBuffer(GlProcs.GL_COPY_WRITE_BUFFER, this.boundsBuffer);
        GlProcs.bufferSubData(GlProcs.GL_COPY_WRITE_BUFFER, 0L, this.boundsStage);
        GlProcs.bindBuffer(GlProcs.GL_COPY_WRITE_BUFFER, 0);
        this.lastSubmittedBytes = commandBytes + boundsBytes;
        if (this.arena != null) {
            this.arena.reserve(0L); // keeps the arena's per-frame bookkeeping honest
        }
    }

    /**
     * Runs the HZB cull/compaction kernel over the staged commands.
     *
     * @return true when the survivor count is now GPU-valid
     */
    public boolean compact(final HierarchicalDepthBuffer hzb) {
        if (this.closed || this.stagedCommands == 0 || hzb == null || !this.cullPassUsable) {
            return false;
        }
        this.cullPassUsable = hzb.ensureCullProgram(this.commandBuffer, this.countBuffer);
        if (!this.cullPassUsable) {
            return false;
        }
        hzb.dispatchCompaction(this.stagedCommands);
        GlProcs.memoryBarrier(GlProcs.GL_COMMAND_BARRIER_BIT | GlProcs.GL_BUFFER_UPDATE_BARRIER_BIT);
        return true;
    }

    /** Uploads, optionally compacts, then issues the batched draw. */
    public String submit(final HierarchicalDepthBuffer hzb, final int indexType, final int vertexArrayIndexCount) {
        if (this.closed || this.stagedCommands == 0) {
            return "idle";
        }
        upload();
        final boolean compacted = compact(hzb);
        GlProcs.memoryBarrier(GlProcs.GL_COMMAND_BARRIER_BIT | GlProcs.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT
                | GlProcs.GL_ELEMENT_ARRAY_BARRIER_BIT | GlProcs.GL_BUFFER_UPDATE_BARRIER_BIT);
        this.framesWithDraws++;
        this.totalDrawCommands += this.stagedCommands;
        if (compacted && this.capabilities.supportsIndirectParameters()) {
            GlProcs.bindBuffer(GlProcs.GL_DRAW_INDIRECT_BUFFER, this.commandBuffer);
            GlProcs.bindParameterBuffer(this.countBuffer);
            GlProcs.multiDrawElementsIndirectCount(GlProcs.GL_TRIANGLES, indexType, 0L, 0L, this.stagedCommands, COMMAND_BYTES);
            GlProcs.bindParameterBuffer(0);
            GlProcs.bindBuffer(GlProcs.GL_DRAW_INDIRECT_BUFFER, 0);
            return this.stagedCommands + " cmds (gpu count)";
        }
        GlProcs.bindBuffer(GlProcs.GL_DRAW_INDIRECT_BUFFER, this.commandBuffer);
        GlProcs.multiDrawElementsIndirect(GlProcs.GL_TRIANGLES, indexType, 0L, this.stagedCommands, COMMAND_BYTES);
        GlProcs.bindBuffer(GlProcs.GL_DRAW_INDIRECT_BUFFER, 0);
        return this.stagedCommands + " cmds (cpu count)";
    }

    public int getStagedCommands() {
        return this.stagedCommands;
    }

    public int getCapacityCommands() {
        return this.capacityCommands;
    }

    public int getFramesWithDraws() {
        return this.framesWithDraws;
    }

    public long getTotalDrawCommands() {
        return this.totalDrawCommands;
    }

    public long getLastSubmittedBytes() {
        return this.lastSubmittedBytes;
    }

    public int getCommandBuffer() {
        return this.commandBuffer;
    }

    public int getCountBuffer() {
        return this.countBuffer;
    }

    public int getBoundsBuffer() {
        return this.boundsBuffer;
    }

    /** True when the batch is saturated and the scheduler should apply backpressure. */
    public boolean isFull() {
        return this.stagedCommands >= this.capacityCommands;
    }

    public String describe() {
        return String.format(java.util.Locale.ROOT, "MDI: %d/%d slots, %d draws issued over %d frames",
                this.stagedCommands, this.capacityCommands, this.totalDrawCommands, this.framesWithDraws);
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        MemoryUtil.memFree(this.commandStage);
        MemoryUtil.memFree(this.boundsStage);
        for (final int buffer : new int[]{this.commandBuffer, this.countBuffer, this.boundsBuffer}) {
            if (buffer != 0) {
                org.lwjgl.opengl.GL15.glDeleteBuffers(buffer);
            }
        }
        LOGGER.info("Indirect batch closed after {} frames with draws", this.framesWithDraws);
    }
}
