package com.aetherium.mixin.core;

// @era:shader-begin none
// @era:shader-else v16
//~ import com.aetherium.shader.ShaderVertex;
//~ import com.mojang.blaze3d.vertex.VertexFormat;
//~ import org.spongepowered.asm.mixin.Final;
//~ import org.spongepowered.asm.mixin.Mutable;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:shader-end
import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Shader pack engine (1.16.5): chunk buffers created while a pack extends the terrain format
 * count their vertices with the extended size. Once per buffer creation.
 */
// @era:shader-begin none
@Mixin(Minecraft.class)
// @era:shader-else v16
//~ @Mixin(targets = "com.mojang.blaze3d.vertex.VertexBuffer")
// @era:shader-end
public abstract class ShaderVertexBufferMixin {

    // @era:shader-begin none
    // @era:shader-else v16
    //~ @Shadow
    //~ @Final
    //~ @Mutable
    //~ private VertexFormat format;

    //~ @Inject(method = "<init>", at = @At("TAIL"), require = 0)
    //~ private void aetherium$shaderFormat(final VertexFormat requested, final CallbackInfo ci) {
        //~ this.format = ShaderVertex.swap(this.format);
    //~ }
    // @era:shader-end
}
