package dev.aetherium.engine.gl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL45;

import java.nio.ByteBuffer;

/**
 * Collects DrawElementsIndirectCommand records for one render pass into a persistent command buffer and issues a
 * single glMultiDrawElementsIndirect. One batch == one VAO/material; chunk sections sharing a layer collapse into
 * one draw call, which is where the ~90% draw-call reduction comes from.
 */
public final class IndirectDrawBatcher implements AutoCloseable {
    /** sizeof(DrawElementsIndirectCommand) == 5 * uint32. */
    public static final int COMMAND_BYTES = 20;

    private final PersistentMappedBuffer commands;
    private final int maxCommands;
    private ByteBuffer cursor;
    private long baseOffset;
    private int count;

    public IndirectDrawBatcher(int maxCommandsPerFrame) {
        this.maxCommands = maxCommandsPerFrame;
        this.commands = new PersistentMappedBuffer((long) maxCommandsPerFrame * COMMAND_BYTES);
    }

    public void beginFrame() {
        commands.nextFrame();
        baseOffset = commands.allocate((long) maxCommands * COMMAND_BYTES, 4);
        cursor = commands.slice(baseOffset, maxCommands * COMMAND_BYTES);
        count = 0;
    }

    /** @return false when the per-frame command budget is exhausted. */
    public boolean push(int indexCount, int instanceCount, int firstIndex, int baseVertex, int baseInstance) {
        if (count >= maxCommands) return false;
        cursor.putInt(indexCount).putInt(instanceCount).putInt(firstIndex).putInt(baseVertex).putInt(baseInstance);
        count++;
        return true;
    }

    public int pendingCount() { return count; }

    /** Issue every pushed command. The caller binds the VAO, program, textures and index buffer first. */
    public void flush() {
        if (count == 0) return;
        GL45.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, commands.id());
        GL43.glMultiDrawElementsIndirect(GL11.GL_TRIANGLES, GL11.GL_UNSIGNED_INT, baseOffset, count, 0);
        commands.fenceCurrent();
        count = 0;
    }

    /** Buffer object + byte offset for GPU-side compaction (the HZB pass writes visible commands here). */
    public int commandBufferId() { return commands.id(); }
    public long commandBufferOffset() { return baseOffset; }

    @Override public void close() { commands.close(); }
}
