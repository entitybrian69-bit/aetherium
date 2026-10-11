package com.aetherium.client;

/** When a 1.16.5 chunk section became dirty (added to the section by {@code RenderChunkDirtyMixin}). */
public interface RenderChunkDirtyAccess {

    /** {@code System.nanoTime()} when it first became dirty since its last build, or 0. */
    long aetheriumFirstDirty();

    /** {@code System.nanoTime()} of the latest {@code setDirty}. */
    long aetheriumLastDirty();
}
