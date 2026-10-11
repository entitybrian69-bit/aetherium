package com.aetherium.mixin.core;

// @era:chunk-renderer-begin none
// @era:chunk-renderer-else gl16
//~ import com.aetherium.client.ChunkInfoOrigin;
//~ import com.aetherium.client.ChunkRenderer;
//~ import com.aetherium.client.VertexBufferAccess;
//~ import com.aetherium.perf.RenderToggles;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import it.unimi.dsi.fastutil.objects.ObjectList;
//~ import java.nio.FloatBuffer;
//~ import net.minecraft.client.renderer.RenderType;
//~ import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
//~ import net.minecraft.core.BlockPos;
//~ import org.spongepowered.asm.mixin.Final;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:chunk-renderer-end
import net.minecraft.client.renderer.LevelRenderer;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Experimental 1.16.5 terrain path: the solid, cutout-mipped and cutout layers are drawn by
 * {@code ChunkRenderer} (one GLSL program, shared index buffer) instead of vanilla's per-section
 * fixed-function draws. Vanilla still builds, uploads, culls and orders the sections and sets up
 * each layer's render state; translucent and tripwire stay entirely vanilla (they need sorting).
 *
 * <p>Off unless the user enables it; applied only on versions with
 * {@code Capabilities.EXPERIMENTAL_CHUNK_RENDERER}. Any failure falls back to vanilla for the
 * rest of the session. One CallbackInfo per layer per frame (five calls), not a hot path.</p>
 */
@Mixin(LevelRenderer.class)
public abstract class ChunkRendererMixin {

    // @era:chunk-renderer-begin none
    // @era:chunk-renderer-else gl16
    //~ @Shadow
    //~ @Final
    //~ private ObjectList<?> renderChunks;

    //~ @Inject(method = "renderChunkLayer", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$renderLayer(final RenderType type, final PoseStack poseStack, final double camX,
                                       //~ final double camY, final double camZ, final CallbackInfo ci) {
        //~ if (!RenderToggles.experimentalChunkRenderer || ChunkRenderer.hasFailed()) {
            //~ return;
        //~ }
        //~ final boolean cutout;
        //~ if (type == RenderType.solid()) {
            //~ cutout = false;
        //~ } else if (type == RenderType.cutoutMipped() || type == RenderType.cutout()) {
            //~ cutout = true;
        //~ } else {
            //~ return;
        //~ }
        //~ type.setupRenderState();
        //~ boolean begun = false;
        //~ try {
            //~ final FloatBuffer pose = ChunkRenderer.poseBuffer();
            //~ poseStack.last().pose().store(pose);
            //~ if (!ChunkRenderer.begin(pose, cutout)) {
                //~ return;
            //~ }
            //~ begun = true;
            //~ final ObjectList<?> list = this.renderChunks;
            //~ for (int i = 0, n = list.size(); i < n; i++) {
                //~ final ChunkRenderDispatcher.RenderChunk chunk =
                        //~ (ChunkRenderDispatcher.RenderChunk) ((ChunkInfoOrigin) list.get(i)).aetheriumChunk();
                //~ if (chunk.getCompiledChunk().isEmpty(type)) {
                    //~ continue;
                //~ }
                //~ final VertexBufferAccess buffer = (VertexBufferAccess) chunk.getBuffer(type);
                //~ final BlockPos origin = chunk.getOrigin();
                //~ ChunkRenderer.draw(buffer.aetheriumId(), buffer.aetheriumVertexCount(), (float) (origin.getX() - camX),
                        //~ (float) (origin.getY() - camY), (float) (origin.getZ() - camZ));
            //~ }
            //~ begun = false;
            //~ ChunkRenderer.end();
            //~ ci.cancel();
        //~ } catch (final RuntimeException | LinkageError error) {
            //~ // Not cancelled: vanilla draws this layer, and every later frame (the renderer is off now).
            //~ ChunkRenderer.fail(error.toString());
            //~ if (begun) {
                //~ ChunkRenderer.end();
            //~ }
        //~ } finally {
            //~ type.clearRenderState();
        //~ }
    //~ }
    // @era:chunk-renderer-end
}
