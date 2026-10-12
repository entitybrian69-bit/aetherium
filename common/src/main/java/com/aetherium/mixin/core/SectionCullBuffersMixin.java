package com.aetherium.mixin.core;

import com.aetherium.client.SectionCullLayoutAccess;
import com.aetherium.render.CompactTerrain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * The Aetherium renderer on 1.21.5: a section layer's GPU buffers remember the face groups of the
 * vertices last uploaded into them (set on the render thread right after the upload). Applied only
 * where {@code Capabilities.SECTION_CULL_SRD} is true.
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$SectionBuffers")
public abstract class SectionCullBuffersMixin implements SectionCullLayoutAccess {
    @Unique
    private CompactTerrain.Layout aetherium$cullLayout;

    @Override
    public CompactTerrain.Layout aetheriumCullLayout() {
        return this.aetherium$cullLayout;
    }

    @Override
    public void aetheriumSetCullLayout(final CompactTerrain.Layout layout) {
        this.aetherium$cullLayout = layout;
    }
}
