package com.aetherium.mixin.core;

// @era:chunk-renderer-begin none
// @era:chunk-renderer-else gl16
//~ import com.aetherium.client.ChunkInfoOrigin;
//~ import com.aetherium.client.ChunkRenderer;
//~ import com.aetherium.client.VertexBufferAccess;
//~ import com.aetherium.perf.RenderToggles;
//~ import com.aetherium.render.CompactTerrain;
//~ import com.mojang.blaze3d.systems.RenderSystem;
//~ import com.mojang.blaze3d.vertex.DefaultVertexFormat;
//~ import com.mojang.blaze3d.vertex.VertexBuffer;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import com.mojang.math.Matrix4f;
//~ import net.minecraft.client.Camera;
//~ import net.minecraft.client.renderer.GameRenderer;
//~ import net.minecraft.client.renderer.LightTexture;
//~ import it.unimi.dsi.fastutil.objects.ObjectList;
//~ import java.nio.FloatBuffer;
//~ import net.minecraft.client.renderer.RenderType;
//~ import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
//~ import net.minecraft.core.BlockPos;
//~ import org.spongepowered.asm.mixin.Final;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.Unique;
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
 * <p>Sections uploaded in the Aetherium pipeline's compact format are drawn face-group by
 * face-group (only the directions that can face the camera); see {@code VertexBufferMixin}.</p>
 *
 * <p>Compatible mode (1.3.3) replays vanilla's own layer loop with the same calls (bind,
 * {@code BLOCK.setupBufferState}, the push/load/mult/pop of {@code VertexBuffer.draw},
 * {@code RenderSystem.drawArrays(GL_QUADS)}, unbind, clearBufferState); the only difference is that
 * a section sorted by face direction is drawn as the ranges that can face the camera.</p>
 *
 * <p>Off unless the user enables it; applied only on versions with
 * {@code Capabilities.EXPERIMENTAL_CHUNK_RENDERER}. Any failure falls back to vanilla for the
 * rest of the session. One CallbackInfo per layer per frame (five calls) plus one per frame for
 * the pipeline switch; not a hot path.</p>
 */
@Mixin(LevelRenderer.class)
public abstract class ChunkRendererMixin {

    // @era:chunk-renderer-begin none
    // @era:chunk-renderer-else gl16
    //~ @Shadow
    //~ @Final
    //~ private ObjectList<?> renderChunks;

    //~ /**
     //~ * Once per frame, before anything is drawn: rebuilds every section when the pipeline turns on
     //~ * (so the whole view converts at once) or turns off / fails while compact buffers exist.
     //~ */
    //~ @Inject(method = "renderLevel", at = @At("HEAD"), require = 0)
    //~ private void aetherium$pipelineSwitch(final PoseStack poseStack, final float partialTick, final long finishNanos,
                                          //~ final boolean renderOutline, final Camera camera, final GameRenderer gameRenderer,
                                          //~ final LightTexture lightTexture, final Matrix4f projection, final CallbackInfo ci) {
        //~ // The projection vanilla loads into GL for this frame: read it here instead of querying
        //~ // fixed-function state, which GL translators may not track.
        //~ projection.store(ChunkRenderer.projectionBuffer());
        //~ ChunkRenderer.projectionStored();
        //~ if (ChunkRenderer.updatePipeline(RenderToggles.experimentalChunkRenderer, RenderToggles.pipelineShader)) {
            //~ ((LevelRenderer) (Object) this).allChanged();
        //~ }
    //~ }

    //~ @Inject(method = "renderChunkLayer", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$renderLayer(final RenderType type, final PoseStack poseStack, final double camX,
                                       //~ final double camY, final double camZ, final CallbackInfo ci) {
        //~ final boolean cutout;
        //~ if (type == RenderType.solid()) {
            //~ cutout = false;
        //~ } else if (type == RenderType.cutoutMipped() || type == RenderType.cutout()) {
            //~ cutout = true;
        //~ } else {
            //~ return;
        //~ }
        //~ final int mode = ChunkRenderer.drawMode(RenderToggles.experimentalChunkRenderer);
        //~ if (mode == ChunkRenderer.MODE_OFF) {
            //~ if (ChunkRenderer.hasCompactBuffers()) {
                //~ ci.cancel(); // vanilla cannot read compact buffers; the rebuild is queued for the next frame
            //~ }
            //~ return;
        //~ }
        //~ if (mode == ChunkRenderer.MODE_COMPATIBLE) {
            //~ this.aetherium$drawCompatible(type, poseStack, camX, camY, camZ);
            //~ ci.cancel();
            //~ return;
        //~ }
        //~ type.setupRenderState();
        //~ boolean begun = false;
        //~ try {
            //~ final FloatBuffer pose = ChunkRenderer.poseBuffer();
            //~ poseStack.last().pose().store(pose);
            //~ if (!ChunkRenderer.begin(pose, cutout)) {
                //~ if (ChunkRenderer.hasCompactBuffers()) {
                    //~ ci.cancel(); // first frame: buffers were already uploaded compact; the rebuild follows
                //~ }
                //~ return;
            //~ }
            //~ begun = true;
            //~ int notCompact = 0;
            //~ final ObjectList<?> list = this.renderChunks;
            //~ for (int i = 0, n = list.size(); i < n; i++) {
                //~ final ChunkRenderDispatcher.RenderChunk chunk =
                        //~ (ChunkRenderDispatcher.RenderChunk) ((ChunkInfoOrigin) list.get(i)).aetheriumChunk();
                //~ if (chunk.getCompiledChunk().isEmpty(type)) {
                    //~ continue;
                //~ }
                //~ final VertexBufferAccess buffer = (VertexBufferAccess) chunk.getBuffer(type);
                //~ final BlockPos origin = chunk.getOrigin();
                //~ final float dx = (float) (origin.getX() - camX);
                //~ final float dy = (float) (origin.getY() - camY);
                //~ final float dz = (float) (origin.getZ() - camZ);
                //~ final CompactTerrain.Layout layout = buffer.aetheriumCompactLayout();
                //~ if (layout != null) {
                    //~ ChunkRenderer.drawCompact(buffer.aetheriumId(), layout, dx, dy, dz);
                //~ } else {
                    //~ // Still in vanilla's format (built before the switch to shader mode, or not
                    //~ // encodable): never fed to the shader. Vanilla's 32-byte vertices drawn as
                    //~ // GL_QUADS from generic attributes make GL4ES convert them on the CPU from the
                    //~ // buffer offset as if it were an address, a native crash.
                    //~ notCompact++;
                //~ }
            //~ }
            //~ begun = false;
            //~ ChunkRenderer.end();
            //~ if (notCompact > 0) {
                //~ // The same vanilla GL path compatible mode uses (it skips the compact sections).
                //~ this.aetherium$drawCompatible(type, poseStack, camX, camY, camZ);
            //~ }
            //~ ci.cancel();
        //~ } catch (final RuntimeException | LinkageError error) {
            //~ // The renderer is off now. Vanilla draws this layer unless compact buffers exist (then the
            //~ // layer is skipped until next frame's rebuild has replaced them).
            //~ ChunkRenderer.fail(error.toString());
            //~ if (begun) {
                //~ ChunkRenderer.end();
            //~ }
            //~ if (ChunkRenderer.hasCompactBuffers()) {
                //~ ci.cancel();
            //~ }
        //~ } finally {
            //~ type.clearRenderState();
        //~ }
    //~ }

    //~ /**
     //~ * Compatible mode: vanilla's renderChunkLayer, call for call (same GL path vanilla takes, so it
     //~ * runs wherever vanilla runs), drawing only the face groups of sorted sections that can face the
     //~ * camera. Compact buffers left from shader mode are skipped until their rebuild replaces them.
     //~ */
    //~ @Unique
    //~ private void aetherium$drawCompatible(final RenderType type, final PoseStack poseStack, final double camX,
                                          //~ final double camY, final double camZ) {
        //~ type.setupRenderState();
        //~ ChunkRenderer.beginCompatible();
        //~ try {
            //~ final ObjectList<?> list = this.renderChunks;
            //~ for (int i = 0, n = list.size(); i < n; i++) {
                //~ final ChunkRenderDispatcher.RenderChunk chunk =
                        //~ (ChunkRenderDispatcher.RenderChunk) ((ChunkInfoOrigin) list.get(i)).aetheriumChunk();
                //~ if (chunk.getCompiledChunk().isEmpty(type)) {
                    //~ continue;
                //~ }
                //~ final VertexBuffer buffer = chunk.getBuffer(type);
                //~ final VertexBufferAccess access = (VertexBufferAccess) buffer;
                //~ if (access.aetheriumCompactLayout() != null) {
                    //~ continue;
                //~ }
                //~ final BlockPos origin = chunk.getOrigin();
                //~ final double ox = origin.getX() - camX;
                //~ final double oy = origin.getY() - camY;
                //~ final double oz = origin.getZ() - camZ;
                //~ final int runs = ChunkRenderer.compatibleRuns(access.aetheriumSortedLayout(), (float) ox, (float) oy,
                        //~ (float) oz);
                //~ if (runs == 0) {
                    //~ continue;
                //~ }
                //~ poseStack.pushPose();
                //~ poseStack.translate(ox, oy, oz);
                //~ buffer.bind();
                //~ DefaultVertexFormat.BLOCK.setupBufferState(0L);
                //~ RenderSystem.pushMatrix();
                //~ RenderSystem.loadIdentity();
                //~ RenderSystem.multMatrix(poseStack.last().pose());
                //~ if (runs < 0) {
                    //~ RenderSystem.drawArrays(7, 0, access.aetheriumVertexCount()); // GL_QUADS, as vanilla
                //~ } else {
                    //~ for (int r = 0; r < runs; r++) {
                        //~ RenderSystem.drawArrays(7, ChunkRenderer.runFirstVertex(r), ChunkRenderer.runVertexCount(r));
                    //~ }
                //~ }
                //~ RenderSystem.popMatrix();
                //~ poseStack.popPose();
                //~ ChunkRenderer.compatibleSectionDrawn();
            //~ }
            //~ VertexBuffer.unbind();
            //~ RenderSystem.clearCurrentColor();
            //~ DefaultVertexFormat.BLOCK.clearBufferState();
            //~ ChunkRenderer.endCompatible();
        //~ } catch (final RuntimeException | LinkageError error) {
            //~ ChunkRenderer.fail("compatible mode: " + error);
        //~ } finally {
            //~ type.clearRenderState();
        //~ }
    //~ }
    // @era:chunk-renderer-end
}
