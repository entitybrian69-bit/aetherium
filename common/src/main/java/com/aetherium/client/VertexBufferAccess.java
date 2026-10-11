package com.aetherium.client;

import com.aetherium.render.CompactTerrain;

/**
 * Added to {@code VertexBuffer} on 1.16.5 by {@code VertexBufferMixin}: the GL buffer name and
 * vertex count vanilla uploaded, so {@link ChunkRenderer} can draw the same buffer itself, plus the
 * Aetherium pipeline's per-buffer state.
 */
public interface VertexBufferAccess {

    int aetheriumId();

    int aetheriumVertexCount();

    /** Marks a solid / cutout-mipped / cutout section buffer: its uploads may use the compact format. */
    void aetheriumMarkTerrain();

    boolean aetheriumTerrain();

    /** Group layout when the last upload stored {@link CompactTerrain} vertices, otherwise null (vanilla layout). */
    CompactTerrain.Layout aetheriumCompactLayout();

    /**
     * Group layout when the last upload stored vanilla vertices sorted into face groups (compatible
     * mode, {@link CompactTerrain#sortQuads}), otherwise null.
     */
    CompactTerrain.Layout aetheriumSortedLayout();

    /**
     * Worker thread, right before vanilla queues this buffer's upload: converts the builder's
     * finished mesh for the Aetherium pipeline and parks it for the render thread
     * ({@code PreparedUploads}). Does nothing for untagged buffers or while the pipeline is off.
     *
     * @param builder the {@code BufferBuilder} holding the finished mesh
     */
    void aetheriumPrepare(Object builder);
}
