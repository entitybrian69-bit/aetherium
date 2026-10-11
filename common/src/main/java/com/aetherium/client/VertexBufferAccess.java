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
}
