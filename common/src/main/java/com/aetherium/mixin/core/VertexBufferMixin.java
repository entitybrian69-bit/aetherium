package com.aetherium.mixin.core;

import com.aetherium.client.VertexBufferAccess;
import com.aetherium.render.CompactTerrain;

// @era:chunk-renderer-begin none
// @era:chunk-renderer-else gl16
//~ import com.aetherium.client.ChunkRenderer;
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

    //~ @Unique
    //~ private boolean aetherium$compact;

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
        //~ return this.aetherium$compact ? this.aetherium$layout : null;
    //~ }

    //~ /**
     //~ * Replaces vanilla's {@code upload_} for tagged buffers while the pipeline is on. Same steps as
     //~ * vanilla (pop the builder's next buffer; if the GL buffer is alive, set the vertex count and
     //~ * store the data), with the data re-encoded when it fits.
     //~ */
    //~ @Inject(method = "upload_", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$upload(final BufferBuilder builder, final CallbackInfo ci) {
        //~ if (!this.aetherium$terrain) {
            //~ return;
        //~ }
        //~ if (!ChunkRenderer.compactUploadsWanted()) {
            //~ this.aetherium$compact = false;
            //~ return;
        //~ }
        //~ ci.cancel();
        //~ final ByteBuffer data = builder.popNextBuffer().getSecond();
        //~ ByteBuffer upload = data;
        //~ boolean compact = false;
        //~ try {
            //~ if (this.aetherium$layout == null) {
                //~ this.aetherium$layout = new CompactTerrain.Layout();
            //~ }
            //~ final ByteBuffer encoded = CompactTerrain.encode(data, CompactTerrain.scratch(), this.aetherium$layout);
            //~ if (encoded != null) {
                //~ upload = encoded;
                //~ compact = true;
            //~ }
        //~ } catch (final RuntimeException error) {
            //~ ChunkRenderer.fail("compact encode failed: " + error);
        //~ }
        //~ if (this.id != -1) {
            //~ this.vertexCount = compact ? this.aetherium$layout.quads() * 4 : data.remaining() / this.format.getVertexSize();
            //~ GL15.glBindBuffer(0x8892, this.id);       // GL_ARRAY_BUFFER
            //~ GL15.glBufferData(0x8892, upload, 0x88E4); // GL_STATIC_DRAW, as vanilla
            //~ GL15.glBindBuffer(0x8892, 0);
        //~ }
        //~ this.aetherium$compact = compact;
        //~ if (compact) {
            //~ ChunkRenderer.noteCompactUpload();
        //~ }
    //~ }
    // @era:chunk-renderer-end
}
