package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.mojang.blaze3d.vertex.VertexFormat;
//~ import java.nio.ByteBuffer;
//~ import java.util.List;
//~ import org.spongepowered.asm.mixin.gen.Accessor;
//~ import org.spongepowered.asm.mixin.gen.Invoker;
// @era:qol16-end

/**
 * {@code BufferBuilder} internals (1.16.5): the draw-state queue for the pipeline's mesh
 * conversion, and the write position, counters and growth used by the fast model path.
 */
@Mixin(targets = "com.mojang.blaze3d.vertex.BufferBuilder")
public interface BufferBuilderAccessor {
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ @Accessor("vertexCounts")
    //~ List<?> aetherium$drawStates();

    //~ @Accessor("lastRenderedCountIndex")
    //~ int aetherium$nextDrawState();

    //~ @Accessor("buffer")
    //~ ByteBuffer aetherium$buffer();

    //~ @Accessor("nextElementByte")
    //~ int aetherium$nextElementByte();

    //~ @Accessor("nextElementByte")
    //~ void aetherium$setNextElementByte(int value);

    //~ @Accessor("vertices")
    //~ int aetherium$vertices();

    //~ @Accessor("vertices")
    //~ void aetherium$setVertices(int value);

    //~ @Accessor("mode")
    //~ int aetherium$mode();

    //~ @Accessor("format")
    //~ VertexFormat aetherium$format();

    //~ @Accessor("building")
    //~ boolean aetherium$building();

    //~ @Accessor("elementIndex")
    //~ int aetherium$elementIndex();

    //~ @Invoker("ensureCapacity")
    //~ void aetherium$ensureCapacity(int bytes);

    //~ @Invoker("ensureVertexCapacity")
    //~ void aetherium$ensureVertexCapacity();
    // @era:qol16-end
}
