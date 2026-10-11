package com.aetherium.mixin.core;

// @era:chunk-renderer-begin none
// @era:chunk-renderer-else gl16
//~ import com.aetherium.client.VertexBufferAccess;
//~ import com.mojang.blaze3d.vertex.VertexBuffer;
//~ import net.minecraft.client.renderer.RenderType;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.Unique;
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
 * <p>Also frees a section's batched meshes when the section is reset (see {@code TerrainRegions}).</p>
 *
 * <p>Applied only where {@code Capabilities.AETHERIUM_RENDERER} is true. Runs once per
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
        //~ ((VertexBufferAccess) this.getBuffer(RenderType.solid())).aetheriumMarkTerrain(this, 0);
        //~ ((VertexBufferAccess) this.getBuffer(RenderType.cutoutMipped())).aetheriumMarkTerrain(this, 1);
        //~ ((VertexBufferAccess) this.getBuffer(RenderType.cutout())).aetheriumMarkTerrain(this, 2);
    //~ }

    //~ /**
     //~ * The section was moved to a new position or emptied ({@code setOrigin} resets it): vanilla
     //~ * stops drawing its buffers until the next rebuild, so their batched copies are freed now. A
     //~ * merged draw of a shared page could otherwise still show the old mesh at the old position.
     //~ */
    //~ @Inject(method = "reset", at = @At("HEAD"), require = 0)
    //~ private void aetherium$leaveRegions(final CallbackInfo ci) {
        //~ aetherium$leaveRegion(this.getBuffer(RenderType.solid()));
        //~ aetherium$leaveRegion(this.getBuffer(RenderType.cutoutMipped()));
        //~ aetherium$leaveRegion(this.getBuffer(RenderType.cutout()));
    //~ }

    //~ @Unique
    //~ private static void aetherium$leaveRegion(final VertexBuffer buffer) {
        //~ if (buffer != null) { // reset before the buffers exist
            //~ ((VertexBufferAccess) buffer).aetheriumLeaveRegion();
        //~ }
    //~ }
    // @era:chunk-renderer-end
}
