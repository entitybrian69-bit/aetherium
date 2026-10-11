package com.aetherium.mixin.core;

// @era:chunk-renderer-begin none
// @era:chunk-renderer-else gl16
//~ import com.aetherium.client.VertexBufferAccess;
//~ import com.mojang.blaze3d.vertex.VertexBuffer;
//~ import net.minecraft.client.renderer.RenderType;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:chunk-renderer-end
import org.spongepowered.asm.mixin.Mixin;

/**
 * Tags a 1.16.5 chunk section's solid, cutout-mipped and cutout vertex buffers when the section
 * is created, so their uploads may use the Aetherium pipeline's compact format. Translucent and
 * tripwire buffers are left alone: vanilla sorts and draws them.
 *
 * <p>Applied only where {@code Capabilities.EXPERIMENTAL_CHUNK_RENDERER} is true. Runs once per
 * section object (they are pooled by the view area), not per frame.</p>
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk")
public abstract class RenderChunkBuffersMixin {

    // @era:chunk-renderer-begin none
    // @era:chunk-renderer-else gl16
    //~ @Shadow
    //~ public abstract VertexBuffer getBuffer(RenderType type);

    //~ @Inject(method = "<init>", at = @At("TAIL"), require = 0)
    //~ private void aetherium$tagTerrainBuffers(final CallbackInfo ci) {
        //~ ((VertexBufferAccess) this.getBuffer(RenderType.solid())).aetheriumMarkTerrain();
        //~ ((VertexBufferAccess) this.getBuffer(RenderType.cutoutMipped())).aetheriumMarkTerrain();
        //~ ((VertexBufferAccess) this.getBuffer(RenderType.cutout())).aetheriumMarkTerrain();
    //~ }
    // @era:chunk-renderer-end
}
