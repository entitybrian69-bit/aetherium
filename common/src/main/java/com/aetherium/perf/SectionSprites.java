package com.aetherium.perf;

/** The animated sprites a compiled chunk section uses (mixed into {@code CompiledChunk}). */
public interface SectionSprites {

    /** Null until the section was meshed with the recording hooks. */
    Object[] aetheriumSprites();

    void aetheriumSetSprites(Object[] sprites);
}
