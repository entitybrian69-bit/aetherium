package com.aetherium.mixin.core;

import com.aetherium.client.SectionCullLayoutAccess;
import com.aetherium.render.CompactTerrain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * The Aetherium renderer on 1.21.5+: a finished mesh remembers the face groups it was sorted into
 * on the chunk builder, until the section that owns it takes them over. Applied only where
 * {@code Capabilities.SECTION_CULL_MODERN} is true.
 */
@Mixin(targets = "com.mojang.blaze3d.vertex.MeshData")
public abstract class SectionCullMeshDataMixin implements SectionCullLayoutAccess {
    @Unique
    private volatile CompactTerrain.Layout aetherium$cullLayout;

    @Override
    public CompactTerrain.Layout aetheriumCullLayout() {
        return this.aetherium$cullLayout;
    }

    @Override
    public void aetheriumSetCullLayout(final CompactTerrain.Layout layout) {
        this.aetherium$cullLayout = layout;
    }
}
