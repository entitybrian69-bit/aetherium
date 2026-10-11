package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import java.util.List;
//~ import org.spongepowered.asm.mixin.gen.Accessor;
// @era:qol16-end

/**
 * Read access to a vertex builder's queue of finished meshes, so a chunk upload can check that
 * the builder still holds a mesh before taking it out. {@code popNextBuffer} increments its read
 * index before the list lookup, so popping an empty builder both throws and leaves the builder
 * unusable for the next renderer that draws with it. Applied only where
 * {@code Capabilities.STREAMING} and {@code Capabilities.EXPERIMENTAL_CHUNK_RENDERER} are true.
 */
@Mixin(targets = "com.mojang.blaze3d.vertex.BufferBuilder")
public interface BufferBuilderAccessor {
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ /** Finished meshes ({@code BufferBuilder.DrawState}), oldest first. */
    //~ @Accessor("vertexCounts")
    //~ List<?> aetherium$drawStates();

    //~ /** Index of the next mesh {@code popNextBuffer} hands out. */
    //~ @Accessor("lastRenderedCountIndex")
    //~ int aetherium$nextDrawState();
    // @era:qol16-end
}
