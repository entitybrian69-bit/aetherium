package com.aetherium.mixin.core;

import com.aetherium.client.VertexBufferAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Buffer name and vertex count of a vanilla vertex buffer, for the experimental 1.16.5 chunk
 * renderer. Applied only where {@code Capabilities.EXPERIMENTAL_CHUNK_RENDERER} is true; targeted by
 * name because the class changes shape (or disappears) on later versions.
 */
@Mixin(targets = "com.mojang.blaze3d.vertex.VertexBuffer")
public abstract class VertexBufferMixin implements VertexBufferAccess {

    // @era:chunk-renderer-begin none
    @Override
    public int aetheriumId() {
        return 0;
    }

    @Override
    public int aetheriumVertexCount() {
        return 0;
    }
    // @era:chunk-renderer-else gl16
    //~ @Shadow
    //~ private int id;

    //~ @Shadow
    //~ private int vertexCount;

    //~ @Override
    //~ public int aetheriumId() {
        //~ return this.id;
    //~ }

    //~ @Override
    //~ public int aetheriumVertexCount() {
        //~ return this.vertexCount;
    //~ }
    // @era:chunk-renderer-end
}
