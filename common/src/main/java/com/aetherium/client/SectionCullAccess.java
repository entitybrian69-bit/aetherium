package com.aetherium.client;

/** A section vertex buffer of the Aetherium renderer on 1.17..1.21.4 ({@code SectionCullBufferMixin}). */
public interface SectionCullAccess {

    /** This buffer holds a solid / cutout-mipped / cutout mesh of {@code section} (RenderChunk / RenderSection). */
    void aetheriumSetCullSection(Object section);
}
