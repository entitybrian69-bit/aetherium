package com.aetherium.mixin.core;

// @era:shader-begin none
// @era:shader-else v16
//~ import com.aetherium.shader.ShaderVertex;
//~ import com.mojang.blaze3d.vertex.DefaultVertexFormat;
//~ import com.mojang.blaze3d.vertex.VertexFormat;
//~ import com.mojang.blaze3d.vertex.VertexFormatElement;
//~ import java.nio.ByteBuffer;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.ModifyVariable;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:shader-end
import com.mojang.blaze3d.vertex.BufferBuilder;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Shader pack engine (1.16.5): builders that would write vanilla's BLOCK format write the
 * extended one while a pack needs mc_Entity / mc_midTexCoord / at_tangent, and endVertex fills
 * those elements. Without a pack: one volatile read per begin() and one field compare per vertex.
 */
@Mixin(BufferBuilder.class)
public abstract class ShaderBufferBuilderMixin {

    // @era:shader-begin none
    // @era:shader-else v16
    //~ @Shadow
    //~ private ByteBuffer buffer;
    //~ @Shadow
    //~ private int nextElementByte;
    //~ @Shadow
    //~ private int vertices;
    //~ @Shadow
    //~ private int elementIndex;
    //~ @Shadow
    //~ private int mode;
    //~ @Shadow
    //~ private VertexFormat format;

    //~ @Shadow
    //~ public abstract void nextElement();

    //~ @ModifyVariable(method = "begin", at = @At("HEAD"), argsOnly = true, require = 0)
    //~ private VertexFormat aetherium$shaderFormat(final VertexFormat format) {
        //~ return ShaderVertex.swap(format);
    //~ }

    //~ @Inject(method = "endVertex", at = @At("HEAD"), require = 0)
    //~ private void aetherium$shaderVertex(final CallbackInfo ci) {
        //~ if (!ShaderVertex.isExtended(this.format) || this.elementIndex != 6) {
            //~ if (this.elementIndex == 0 && this.format == DefaultVertexFormat.BLOCK && ShaderVertex.packed()) {
                //~ ShaderVertex.pack(this.buffer, this.nextElementByte - 32, this.vertices, this.mode == 7);
            //~ }
            //~ return;
        //~ }
        //~ ShaderVertex.fill(this.buffer, this.nextElementByte, this.vertices, this.mode == 7);
        //~ for (int i = ShaderVertex.extraElements(); i > 0; i--) {
            //~ this.nextElement();
        //~ }
    //~ }
    // @era:shader-end
}
