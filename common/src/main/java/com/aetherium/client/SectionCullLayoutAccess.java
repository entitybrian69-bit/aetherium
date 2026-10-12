package com.aetherium.client;

import com.aetherium.render.CompactTerrain;

/**
 * The Aetherium renderer on 1.21.5+: a mesh ({@code MeshData}) or a section's GPU buffers (1.21.5
 * {@code SectionBuffers}) carrying the face groups of the vertices in it; null when unsorted.
 */
public interface SectionCullLayoutAccess {
    CompactTerrain.Layout aetheriumCullLayout();

    void aetheriumSetCullLayout(CompactTerrain.Layout layout);
}
