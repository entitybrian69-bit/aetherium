package com.aetherium.mixin.core;

import com.aetherium.client.VertexBufferAccess;
import com.aetherium.render.CompactTerrain;

// @era:chunk-renderer-begin none
// @era:chunk-renderer-else gl16
//~ import com.aetherium.client.ChunkRenderer;
//~ import com.aetherium.perf.PreparedUploads;
//~ import com.aetherium.perf.StutterRecorder;
//~ import com.mojang.blaze3d.vertex.BufferBuilder;
//~ import com.mojang.blaze3d.vertex.VertexFormat;
//~ import java.nio.ByteBuffer;
//~ import org.lwjgl.opengl.GL15;
//~ import org.spongepowered.asm.mixin.Final;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:chunk-renderer-end
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Vertex buffer state for Aetherium's 1.16.5 terrain pipeline: the GL buffer name and vertex
 * count vanilla uploaded, and the compact upload path.
 *
 * <p>Buffers of the solid, cutout-mipped and cutout layers are tagged by
 * {@code RenderChunkBuffersMixin}. While the pipeline is on, their uploads are re-encoded into
 * {@link CompactTerrain}'s 16-byte, face-grouped format (half the memory and bandwidth); data that
 * does not fit is uploaded unchanged. Only {@code ChunkRenderer} draws these layers while compact
 * buffers exist, and switching the pipeline off rebuilds every section first.</p>
 *
 * <p>Applied only where {@code Capabilities.EXPERIMENTAL_CHUNK_RENDERER} is true; targeted by name
 * because the class changes shape (or disappears) on later versions. One CallbackInfo per buffer
 * upload (not per frame).</p>
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

    @Override
    public void aetheriumMarkTerrain() {
    }

    @Override
    public boolean aetheriumTerrain() {
        return false;
    }

    @Override
    public CompactTerrain.Layout aetheriumCompactLayout() {
        return null;
    }

    @Override
    public CompactTerrain.Layout aetheriumSortedLayout() {
        return null;
    }

    @Override
    public void aetheriumPrepare(final Object builder) {
    }
    // @era:chunk-renderer-else gl16
    //~ @Shadow
    //~ private int id;

    //~ @Shadow
    //~ private int vertexCount;

    //~ @Shadow
    //~ @Final
    //~ private VertexFormat format;

    //~ @Unique
    //~ private boolean aetherium$terrain;

    //~ /** What the last upload stored: 0 vanilla as built, 1 compact, 2 vanilla sorted into face groups. */
    //~ @Unique
    //~ private int aetherium$format;

    //~ @Unique
    //~ private CompactTerrain.Layout aetherium$layout;

    //~ @Override
    //~ public int aetheriumId() {
        //~ return this.id;
    //~ }

    //~ @Override
    //~ public int aetheriumVertexCount() {
        //~ return this.vertexCount;
    //~ }

    //~ @Override
    //~ public void aetheriumMarkTerrain() {
        //~ this.aetherium$terrain = true;
    //~ }

    //~ @Override
    //~ public boolean aetheriumTerrain() {
        //~ return this.aetherium$terrain;
    //~ }

    //~ @Override
    //~ public CompactTerrain.Layout aetheriumCompactLayout() {
        //~ return this.aetherium$format == 1 ? this.aetherium$layout : null;
    //~ }

    //~ @Override
    //~ public CompactTerrain.Layout aetheriumSortedLayout() {
        //~ return this.aetherium$format == 2 ? this.aetherium$layout : null;
    //~ }

    //~ /**
     //~ * Replaces vanilla's {@code upload_} for tagged buffers while the pipeline is on. Same steps as
     //~ * vanilla (pop the builder's next buffer; if the GL buffer is alive, set the vertex count and
     //~ * store the data), with the data re-encoded when it fits.
     //~ */
    //~ @Override
    //~ public void aetheriumPrepare(final Object source) {
        //~ if (!this.aetherium$terrain) {
            //~ return;
        //~ }
        //~ final int mode = ChunkRenderer.uploadMode();
        //~ if (mode == ChunkRenderer.MODE_OFF) {
            //~ return;
        //~ }
        //~ final BufferBuilder builder = (BufferBuilder) source;
        //~ if (!aetherium$hasMesh(builder)) {
            //~ return; // nothing to take out; the upload sees the same and leaves the section empty
        //~ }
        //~ final ByteBuffer data = builder.popNextBuffer().getSecond();
        //~ // From here on the mesh exists only in this record: it must reach the upload whatever
        //~ // happens (prepare never throws; a failed conversion parks an empty placeholder).
        //~ PreparedUploads.put(builder, PreparedUploads.prepare(data, mode == ChunkRenderer.MODE_SHADER));
    //~ }

    //~ /**
     //~ * Whether the builder still holds a finished mesh. {@code popNextBuffer} on an empty builder
     //~ * throws and, worse, leaves its read index past the end, which breaks the next renderer that
     //~ * draws with that builder (in 1.16.5 the render thread's chunk builders are shared with the
     //~ * entity and held-item renderers).
     //~ */
    //~ @Unique
    //~ private static boolean aetherium$hasMesh(final BufferBuilder builder) {
        //~ final Object raw = builder;
        //~ if (raw instanceof BufferBuilderAccessor) {
            //~ final BufferBuilderAccessor access = (BufferBuilderAccessor) raw;
            //~ return access.aetherium$nextDrawState() < access.aetherium$drawStates().size();
        //~ }
        //~ return true;
    //~ }

    //~ /** Uploads a mesh the worker already converted: only the GL calls remain for the render thread. */
    //~ @Unique
    //~ private void aetherium$uploadPrepared(final PreparedUploads.Prepared prepared, final int mode) {
        //~ if (prepared.error != null) {
            //~ ChunkRenderer.fail(prepared.error);
        //~ }
        //~ int format = prepared.format;
        //~ // The pipeline was switched off after this mesh was converted: vanilla cannot draw the
        //~ // converted bytes, so the buffer is left empty (switching rebuilds every section anyway).
        //~ final boolean unusable = prepared.data == null || (mode == ChunkRenderer.MODE_OFF && format != 0);
        //~ if (this.id != -1) {
            //~ if (unusable) {
                //~ this.vertexCount = 0;
            //~ } else {
                //~ this.vertexCount = format == 1 ? prepared.quads * CompactTerrain.VERTICES_PER_QUAD
                        //~ : prepared.data.remaining() / this.format.getVertexSize();
                //~ GL15.glBindBuffer(0x8892, this.id);       // GL_ARRAY_BUFFER
                //~ GL15.glBufferData(0x8892, prepared.data, 0x88E4); // GL_STATIC_DRAW, as vanilla
                //~ GL15.glBindBuffer(0x8892, 0);
            //~ }
        //~ }
        //~ if (unusable) {
            //~ format = 0;
        //~ }
        //~ if (format != 0) {
            //~ if (this.aetherium$layout == null) {
                //~ this.aetherium$layout = new CompactTerrain.Layout();
            //~ }
            //~ this.aetherium$layout.copyFrom(prepared.layout);
        //~ }
        //~ this.aetherium$format = format;
        //~ if (format == 1) {
            //~ ChunkRenderer.noteCompactUpload(prepared.quads);
        //~ }
        //~ StutterRecorder.preparedUploads++;
        //~ PreparedUploads.done(prepared);
    //~ }

    //~ @Inject(method = "upload_", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$upload(final BufferBuilder builder, final CallbackInfo ci) {
        //~ if (!this.aetherium$terrain) {
            //~ return;
        //~ }
        //~ final int mode = ChunkRenderer.uploadMode();
        //~ // A worker already took this mesh out of the builder: always consume its record,
        //~ // whatever the current mode, or the builder would be popped a second time.
        //~ final PreparedUploads.Prepared prepared = PreparedUploads.take(builder);
        //~ if (prepared != null) {
            //~ ci.cancel();
            //~ this.aetherium$uploadPrepared(prepared, mode);
            //~ return;
        //~ }
        //~ if (!aetherium$hasMesh(builder)) {
            //~ // Should never happen; if it does, an empty section (rebuilt later) beats a crash.
            //~ ci.cancel();
            //~ if (this.id != -1) {
                //~ this.vertexCount = 0;
            //~ }
            //~ this.aetherium$format = 0;
            //~ ChunkRenderer.noteEmptyUpload();
            //~ return;
        //~ }
        //~ if (mode == ChunkRenderer.MODE_OFF) {
            //~ this.aetherium$format = 0;
            //~ return;
        //~ }
        //~ ci.cancel();
        //~ final ByteBuffer data = builder.popNextBuffer().getSecond();
        //~ ByteBuffer upload = data;
        //~ int format = 0;
        //~ try {
            //~ if (this.aetherium$layout == null) {
                //~ this.aetherium$layout = new CompactTerrain.Layout();
            //~ }
            //~ if (mode == ChunkRenderer.MODE_SHADER) {
                //~ final ByteBuffer encoded = CompactTerrain.encode(data, CompactTerrain.scratch(), this.aetherium$layout);
                //~ if (encoded != null) {
                    //~ upload = encoded;
                    //~ format = 1;
                //~ }
            //~ } else {
                //~ // Compatible mode: the same vanilla bytes, only reordered by face direction.
                //~ final ByteBuffer sorted = CompactTerrain.sortQuads(data, CompactTerrain.scratch(), this.aetherium$layout);
                //~ if (sorted != null) {
                    //~ upload = sorted;
                    //~ format = 2;
                //~ }
            //~ }
        //~ } catch (final RuntimeException error) {
            //~ ChunkRenderer.fail("chunk encode failed: " + error);
        //~ }
        //~ if (this.id != -1) {
            //~ this.vertexCount = format == 1 ? this.aetherium$layout.quads() * CompactTerrain.VERTICES_PER_QUAD : data.remaining() / this.format.getVertexSize();
            //~ GL15.glBindBuffer(0x8892, this.id);       // GL_ARRAY_BUFFER
            //~ GL15.glBufferData(0x8892, upload, 0x88E4); // GL_STATIC_DRAW, as vanilla
            //~ GL15.glBindBuffer(0x8892, 0);
        //~ }
        //~ this.aetherium$format = format;
        //~ if (format == 1) {
            //~ ChunkRenderer.noteCompactUpload(this.aetherium$layout.quads());
        //~ }
    //~ }
    // @era:chunk-renderer-end
}
