package com.aetherium.client;

/**
 * Added to {@code VertexBuffer} on 1.16.5 by {@code VertexBufferMixin}: the GL buffer name and
 * vertex count vanilla uploaded, so {@link ChunkRenderer} can draw the same buffer itself.
 */
public interface VertexBufferAccess {

    int aetheriumId();

    int aetheriumVertexCount();
}
