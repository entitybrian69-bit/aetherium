package com.aetherium.client;

import com.aetherium.render.CompactTerrain;

/**
 * The Aetherium renderer on 1.21.6+: a compiled section mesh ({@code CompiledSectionMesh}) knows the
 * face groups of each layer it was built with. One mesh object per build, so the layout always
 * describes exactly the vertices drawn for it.
 */
public interface SectionCullLayersAccess {
    /** Face groups of the layer with this ordinal, or null when that layer was not sorted. */
    CompactTerrain.Layout aetheriumCullLayout(int layerOrdinal);
}
