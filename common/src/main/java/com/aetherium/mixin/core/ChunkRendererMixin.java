package com.aetherium.mixin.core;

// @era:chunk-renderer-begin none
// @era:chunk-renderer-else gl16
//~ import com.aetherium.client.ChunkInfoOrigin;
//~ import com.aetherium.client.ChestBaking;
//~ import com.aetherium.client.ChunkRenderer;
//~ import com.aetherium.client.MultiDraw;
//~ import com.aetherium.client.OcclusionCulling;
//~ import com.aetherium.client.TerrainRegions;
//~ import com.aetherium.client.VertexBufferAccess;
//~ import com.aetherium.perf.RenderToggles;
//~ import com.aetherium.perf.Tuning;
//~ import com.aetherium.render.PackedTerrain;
//~ import com.aetherium.render.CompactTerrain;
//~ import com.mojang.blaze3d.platform.GlStateManager;
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
//~ import org.lwjgl.opengl.GL15;
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
 * {@code Capabilities.AETHERIUM_RENDERER}. Any failure falls back to vanilla for the
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
        //~ final boolean switched = ChunkRenderer.updatePipeline(RenderToggles.aetheriumRenderer, false);
        //~ final boolean pipelineOn = ChunkRenderer.uploadMode() == ChunkRenderer.MODE_COMPATIBLE;
        //~ final boolean regions = TerrainRegions.configure(pipelineOn && RenderToggles.regionBatching, pipelineOn);
        //~ // 1.17.2: packed vertices for batched terrain; chests baked into their chunk. Either change
        //~ // rebuilds every section so all of them use the new form.
        //~ final boolean packing = TerrainRegions.configurePacking(TerrainRegions.enabled() && Tuning.packedTerrain);
        //~ final boolean chests = ChestBaking.configure(Tuning.batchedChests);
        //~ OcclusionCulling.beginFrame(pipelineOn && Tuning.occlusionQueries && !TerrainRegions.translated());
        //~ if (switched || regions || packing || chests) {
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
        //~ final int mode = ChunkRenderer.drawMode(RenderToggles.aetheriumRenderer);
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
        //~ ChunkRenderer.beginCompatible(TerrainRegions.enabled());
        //~ TerrainRegions.beginLayer();
        //~ final boolean occlusion = OcclusionCulling.active();
        //~ try {
            //~ final ObjectList<?> list = this.renderChunks;
            //~ for (int i = 0, n = list.size(); i < n; i++) {
                //~ final ChunkRenderDispatcher.RenderChunk chunk =
                        //~ (ChunkRenderDispatcher.RenderChunk) ((ChunkInfoOrigin) list.get(i)).aetheriumChunk();
                //~ if (chunk.getCompiledChunk().isEmpty(type)) {
                    //~ continue;
                //~ }
                //~ if (occlusion) {
                    //~ final BlockPos at = chunk.getOrigin();
                    //~ if (OcclusionCulling.hidden(chunk, (float) (at.getX() - camX), (float) (at.getY() - camY),
                            //~ (float) (at.getZ() - camZ))) {
                        //~ continue;
                    //~ }
                //~ }
                //~ final VertexBuffer buffer = chunk.getBuffer(type);
                //~ final VertexBufferAccess access = (VertexBufferAccess) buffer;
                //~ if (access.aetheriumCompactLayout() != null) {
                    //~ continue;
                //~ }
                //~ final TerrainRegions.Slot slot = access.aetheriumSlot();
                //~ if (slot != null) {
                    //~ // Batched: desktop queues the exact runs that can face the camera (1.9.0); GL
                    //~ // translators queue one span per section (1.10.0: the exact runs multiplied the
                    //~ // draw calls, which cost phones far more than the hidden faces they skipped). The
                    //~ // page draws them below and joins small gaps (TerrainRegions.mergeGap).
                    //~ final float sx = (float) (slot.originX() - camX);
                    //~ final float sy = (float) (slot.originY() - camY);
                    //~ final float sz = (float) (slot.originZ() - camZ);
                    //~ final int runs = TerrainRegions.translated()
                            //~ ? ChunkRenderer.compatibleRuns(access.aetheriumSortedLayout(), sx, sy, sz)
                            //~ : ChunkRenderer.compatibleRunsExact(access.aetheriumSortedLayout(), sx, sy, sz);
                    //~ if (runs < 0) {
                        //~ TerrainRegions.queueAll(slot);
                    //~ } else {
                        //~ for (int r = 0; r < runs; r++) {
                            //~ TerrainRegions.queue(slot, ChunkRenderer.runFirstVertex(r), ChunkRenderer.runVertexCount(r));
                        //~ }
                    //~ }
                    //~ if (runs != 0) {
                        //~ ChunkRenderer.compatibleSectionDrawn();
                    //~ }
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
                    //~ ChunkRenderer.compatibleDraws(1);
                //~ } else {
                    //~ // One span per section since 1.7.1 (never more draws than vanilla).
                    //~ for (int r = 0; r < runs; r++) {
                        //~ ChunkRenderer.compatibleDraws(
                                //~ aetherium$drawQuads(ChunkRenderer.runFirstVertex(r), ChunkRenderer.runVertexCount(r)));
                    //~ }
                //~ }
                //~ RenderSystem.popMatrix();
                //~ poseStack.popPose();
                //~ ChunkRenderer.compatibleSectionDrawn();
            //~ }
            //~ // Batched pages: one bind, one set of vertex pointers and one matrix per page.
            //~ final boolean multiDraw = MultiDraw.usable(Tuning.multiDraw);
            //~ boolean packedPointers = false;
            //~ for (int p = 0, pages = TerrainRegions.preparePages(); p < pages; p++) {
                //~ final TerrainRegions.RegionPage page = TerrainRegions.page(p);
                //~ poseStack.pushPose();
                //~ poseStack.translate(page.originX() - camX, page.originY() - camY, page.originZ() - camZ);
                //~ GL15.glBindBuffer(0x8892, page.id()); // GL_ARRAY_BUFFER, as VertexBuffer.bind
                //~ if (page.packed()) {
                    //~ // 1.17.2 packed vertices: shorts scaled back by the model-view and texture matrices.
                    //~ aetherium$packedPointers();
                    //~ poseStack.translate(PackedTerrain.POSITION_BIAS, PackedTerrain.POSITION_BIAS, PackedTerrain.POSITION_BIAS);
                    //~ final float unit = 1.0f / PackedTerrain.POSITION_SCALE;
                    //~ poseStack.scale(unit, unit, unit);
                    //~ if (!packedPointers) {
                        //~ aetherium$uvMatrix(true);
                        //~ packedPointers = true;
                    //~ }
                //~ } else {
                    //~ if (packedPointers) {
                        //~ aetherium$uvMatrix(false);
                        //~ packedPointers = false;
                    //~ }
                    //~ DefaultVertexFormat.BLOCK.setupBufferState(0L);
                //~ }
                //~ RenderSystem.pushMatrix();
                //~ RenderSystem.loadIdentity();
                //~ RenderSystem.multMatrix(poseStack.last().pose());
                //~ if (!multiDraw || page.ranges() < 2 || !MultiDraw.draw(page)) {
                    //~ for (int r = 0, n = page.ranges(); r < n; r++) {
                        //~ RenderSystem.drawArrays(7, page.rangeFirst(r), page.rangeCount(r)); // GL_QUADS
                    //~ }
                //~ }
                //~ RenderSystem.popMatrix();
                //~ poseStack.popPose();
            //~ }
            //~ if (packedPointers) {
                //~ aetherium$uvMatrix(false);
            //~ }
            //~ VertexBuffer.unbind();
            //~ RenderSystem.clearCurrentColor();
            //~ DefaultVertexFormat.BLOCK.clearBufferState();
            //~ if (occlusion && type == RenderType.cutout()) {
                //~ // The last opaque layer: ask the GPU which sections this depth buffer hides.
                //~ this.aetherium$occlusionQueries(poseStack, camX, camY, camZ);
            //~ }
            //~ ChunkRenderer.endCompatible();
        //~ } catch (final RuntimeException | LinkageError error) {
            //~ ChunkRenderer.fail("compatible mode: " + error);
        //~ } finally {
            //~ TerrainRegions.endLayer();
            //~ type.clearRenderState();
        //~ }
    //~ }

    //~ /**
     //~ * Packed terrain (1.17.2): vertex pointers for {@link PackedTerrain} 20-byte vertices, the same
     //~ * arrays {@code BLOCK.setupBufferState} enables minus the normals (terrain is unlit).
     //~ */
    //~ @Unique
    //~ private static void aetherium$packedPointers() {
        //~ final int stride = PackedTerrain.STRIDE;
        //~ GlStateManager._vertexPointer(3, 0x1402, stride, 0L); // GL_SHORT
        //~ GlStateManager._enableClientState(0x8074); // GL_VERTEX_ARRAY
        //~ GlStateManager._colorPointer(4, 0x1401, stride, PackedTerrain.OFFSET_COLOR); // GL_UNSIGNED_BYTE
        //~ GlStateManager._enableClientState(0x8076); // GL_COLOR_ARRAY
        //~ GlStateManager._glClientActiveTexture(0x84C0); // GL_TEXTURE0
        //~ GlStateManager._texCoordPointer(2, 0x1402, stride, PackedTerrain.OFFSET_UV0);
        //~ GlStateManager._enableClientState(0x8078); // GL_TEXTURE_COORD_ARRAY
        //~ GlStateManager._glClientActiveTexture(0x84C2); // GL_TEXTURE2: lightmap, as vanilla's UV2
        //~ GlStateManager._texCoordPointer(2, 0x1402, stride, PackedTerrain.OFFSET_UV2);
        //~ GlStateManager._enableClientState(0x8078);
        //~ GlStateManager._glClientActiveTexture(0x84C0);
        //~ GlStateManager._disableClientState(0x8075); // GL_NORMAL_ARRAY
    //~ }

    //~ /** Packed terrain: texture unit 0 scales the packed texture coordinates back to 0..1 (or undoes that). */
    //~ @Unique
    //~ private static void aetherium$uvMatrix(final boolean packed) {
        //~ RenderSystem.activeTexture(0x84C0);
        //~ RenderSystem.matrixMode(0x1702); // GL_TEXTURE
        //~ if (packed) {
            //~ RenderSystem.pushMatrix();
            //~ final float unit = 1.0f / PackedTerrain.UV_SCALE;
            //~ RenderSystem.scalef(unit, unit, 1.0f);
        //~ } else {
            //~ RenderSystem.popMatrix();
        //~ }
        //~ RenderSystem.matrixMode(0x1700); // GL_MODELVIEW
    //~ }

    //~ /**
     //~ * GPU occlusion culling (1.17.2): one occlusion query per section in view (box grown by
     //~ * {@link OcclusionCulling#GROW}), drawn invisibly against the opaque terrain just drawn.
     //~ */
    //~ @Unique
    //~ private void aetherium$occlusionQueries(final PoseStack poseStack, final double camX, final double camY,
                                            //~ final double camZ) {
        //~ final ObjectList<?> list = this.renderChunks;
        //~ boolean begun = false;
        //~ try {
            //~ for (int i = 0, n = list.size(); i < n; i++) {
                //~ final ChunkRenderDispatcher.RenderChunk chunk =
                        //~ (ChunkRenderDispatcher.RenderChunk) ((ChunkInfoOrigin) list.get(i)).aetheriumChunk();
                //~ if (chunk.getCompiledChunk().hasNoRenderableLayers()) {
                    //~ continue;
                //~ }
                //~ final BlockPos at = chunk.getOrigin();
                //~ final double ox = at.getX() - camX;
                //~ final double oy = at.getY() - camY;
                //~ final double oz = at.getZ() - camZ;
                //~ if (!OcclusionCulling.wantQuery(chunk, (float) ox, (float) oy, (float) oz)) {
                    //~ continue;
                //~ }
                //~ if (!begun) {
                    //~ RenderSystem.colorMask(false, false, false, false);
                    //~ RenderSystem.depthMask(false);
                    //~ RenderSystem.disableAlphaTest();
                    //~ RenderSystem.disableTexture();
                    //~ RenderSystem.disableCull();
                    //~ OcclusionCulling.bindBox();
                    //~ begun = true;
                //~ }
                //~ final float grow = OcclusionCulling.GROW;
                //~ poseStack.pushPose();
                //~ poseStack.translate(ox - grow, oy - grow, oz - grow);
                //~ poseStack.scale(16f + 2f * grow, 16f + 2f * grow, 16f + 2f * grow);
                //~ RenderSystem.pushMatrix();
                //~ RenderSystem.loadIdentity();
                //~ RenderSystem.multMatrix(poseStack.last().pose());
                //~ OcclusionCulling.beginQuery(chunk);
                //~ OcclusionCulling.drawBox();
                //~ OcclusionCulling.endQuery();
                //~ RenderSystem.popMatrix();
                //~ poseStack.popPose();
            //~ }
        //~ } finally {
            //~ if (begun) {
                //~ OcclusionCulling.unbindBox();
                //~ RenderSystem.enableCull();
                //~ RenderSystem.enableTexture();
                //~ RenderSystem.enableAlphaTest();
                //~ RenderSystem.depthMask(true);
                //~ RenderSystem.colorMask(true, true, true, true);
            //~ }
        //~ }
    //~ }

    //~ /**
     //~ * {@code GL_QUADS} draw of at most {@link TerrainRegions#DRAW_MAX_VERTICES} vertices at a time:
     //~ * GL4ES splits longer quad draws itself but restarts each piece at vertex 0.
     //~ */
    //~ @Unique
    //~ private static int aetherium$drawQuads(final int first, final int count) {
        //~ int draws = 0;
        //~ for (int done = 0; done < count; done += TerrainRegions.DRAW_MAX_VERTICES) {
            //~ RenderSystem.drawArrays(7, first + done, Math.min(TerrainRegions.DRAW_MAX_VERTICES, count - done));
            //~ draws++;
        //~ }
        //~ return draws;
    //~ }
    // @era:chunk-renderer-end
}
