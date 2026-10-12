package com.aetherium.mixin.core;

import com.aetherium.client.SectionCullAccess;

import org.spongepowered.asm.mixin.Mixin;
// @era:section-cull-begin pop17|rendered19|mesh21
import com.aetherium.client.SectionCull;
import com.aetherium.render.CompactTerrain;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.GlStateManager;
import java.nio.ByteBuffer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
// @era:section-cull-else v16|none
// @era:section-cull-end
// @era:section-cull-begin mesh21
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:section-cull-else pop17
//~ import com.mojang.blaze3d.vertex.BufferBuilder;
//~ import com.mojang.blaze3d.vertex.VertexFormat;
//~ import com.mojang.datafixers.util.Pair;
// @era:section-cull-else rendered19
//~ import com.mojang.blaze3d.vertex.BufferBuilder;
//~ import com.mojang.blaze3d.vertex.VertexFormat;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.ModifyVariable;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:section-cull-else v16|none
// @era:section-cull-end
// @era:section-api-begin srd
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
// @era:section-api-else crd
//~ import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
// @era:section-api-else v16|srd-ro|mesh|none
// @era:section-api-end

/**
 * The Aetherium renderer on 1.17..1.21.4 ({@link com.aetherium.client.SectionCull}), per section
 * vertex buffer: the solid / cutout-mipped / cutout mesh is reordered by face direction as it is
 * uploaded, and while vanilla draws that layer the buffer's {@code glDrawElements} covers only
 * the faces that can point at the camera. Vanilla's binds, shader, uniforms and index buffer are
 * untouched. Applied only where {@code Capabilities.SECTION_CULL} is true.
 */
@Mixin(targets = "com.mojang.blaze3d.vertex.VertexBuffer")
public abstract class SectionCullBufferMixin implements SectionCullAccess {

    // @era:section-cull-begin pop17|rendered19|mesh21
    /** The section owning this buffer (solid / cutout layers only), else null. */
    @Unique
    private Object aetherium$cullSection;

    /** Face groups of the mesh in this buffer; valid only while {@link #aetherium$sorted}. */
    @Unique
    private final CompactTerrain.Layout aetherium$layout = new CompactTerrain.Layout();

    @Unique
    private boolean aetherium$sorted;

    /** Vanilla's block vertex (position, colour, uv, light, normal); anything else is never reordered. */
    @Unique
    private static final int BLOCK_VERTEX_BYTES = 32;

    @Override
    public void aetheriumSetCullSection(final Object section) {
        this.aetherium$cullSection = section;
    }

    /** Reorders {@code data} (the vertices about to be uploaded) in place; records the layout. */
    @Unique
    private void aetherium$sortUpload(final ByteBuffer data, final boolean eligible) {
        this.aetherium$sorted = false;
        if (!eligible || data == null || this.aetherium$cullSection == null || !SectionCull.sorting()) {
            return;
        }
        try {
            final ByteBuffer sorted = CompactTerrain.sortQuads(data, CompactTerrain.scratch(), this.aetherium$layout);
            if (sorted == null || sorted.remaining() != data.remaining()) {
                return;
            }
            final ByteBuffer target = data.duplicate();
            target.put(sorted);
            this.aetherium$sorted = true;
            SectionCull.noteSortedUpload();
        } catch (final RuntimeException | LinkageError error) {
            SectionCull.fail(error);
        }
    }
    // @era:section-cull-else v16|none
    //~ @Override
    //~ public void aetheriumSetCullSection(final Object section) {
    //~ }
    // @era:section-cull-end

    // @era:section-cull-begin mesh21
    @Unique
    private boolean aetherium$sortNext;

    /** 1.21..1.21.4: a mesh without its own index buffer uses the shared sequential quad indices. */
    @Inject(method = "upload", at = @At("HEAD"), require = 0)
    private void aetherium$checkUpload(final MeshData mesh, final CallbackInfo ci) {
        boolean eligible = false;
        if (this.aetherium$cullSection != null && mesh != null && mesh.indexBuffer() == null && SectionCull.sorting()) {
            try {
                eligible = mesh.drawState().mode() == VertexFormat.Mode.QUADS
                        && mesh.drawState().format().getVertexSize() == BLOCK_VERTEX_BYTES;
            } catch (final RuntimeException | LinkageError error) {
                SectionCull.fail(error);
            }
        }
        this.aetherium$sortNext = eligible;
    }

    @ModifyVariable(method = "uploadVertexBuffer", at = @At("HEAD"), argsOnly = true, require = 0)
    private ByteBuffer aetherium$sortVertices(final ByteBuffer data) {
        final boolean eligible = this.aetherium$sortNext;
        this.aetherium$sortNext = false;
        this.aetherium$sortUpload(data, eligible);
        return data;
    }
    // @era:section-cull-else pop17
    //~ /** 1.17..1.18.2: the mesh leaves the builder here; a sequential-index mesh is vertices only. */
    //~ @WrapOperation(method = "upload_", at = @At(value = "INVOKE",
            //~ target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;popNextBuffer()Lcom/mojang/datafixers/util/Pair;"),
            //~ require = 0)
    //~ private Pair<BufferBuilder.DrawState, ByteBuffer> aetherium$sortPopped(final BufferBuilder builder,
            //~ final Operation<Pair<BufferBuilder.DrawState, ByteBuffer>> original) {
        //~ final Pair<BufferBuilder.DrawState, ByteBuffer> popped = original.call(builder);
        //~ boolean eligible = false;
        //~ ByteBuffer data = null;
        //~ if (popped != null && this.aetherium$cullSection != null) {
            //~ final BufferBuilder.DrawState state = popped.getFirst();
            //~ data = popped.getSecond();
            //~ eligible = state != null && data != null && state.sequentialIndex() && !state.indexOnly()
                    //~ && state.mode() == VertexFormat.Mode.QUADS && data.remaining() == state.vertexBufferSize()
                    //~ && state.format().getVertexSize() == BLOCK_VERTEX_BYTES;
        //~ }
        //~ this.aetherium$sortUpload(data, eligible);
        //~ return popped;
    //~ }
    // @era:section-cull-else rendered19
    //~ @Unique
    //~ private boolean aetherium$sortNext;

    //~ /** 1.19..1.20.6: only meshes drawn with the shared sequential quad indices are reordered. */
    //~ @Inject(method = "upload", at = @At("HEAD"), require = 0)
    //~ private void aetherium$checkUpload(final BufferBuilder.RenderedBuffer mesh, final CallbackInfo ci) {
        //~ this.aetherium$sortNext = this.aetherium$cullSection != null && mesh != null
                //~ && mesh.drawState().sequentialIndex() && mesh.drawState().mode() == VertexFormat.Mode.QUADS
                //~ && mesh.drawState().format().getVertexSize() == BLOCK_VERTEX_BYTES;
    //~ }

    //~ @ModifyVariable(method = "uploadVertexBuffer", at = @At("HEAD"), argsOnly = true, require = 0)
    //~ private ByteBuffer aetherium$sortVertices(final ByteBuffer data) {
        //~ final boolean eligible = this.aetherium$sortNext;
        //~ this.aetherium$sortNext = false;
        //~ this.aetherium$sortUpload(data, eligible);
        //~ return data;
    //~ }
    // @era:section-cull-else v16|none
    // @era:section-cull-end

    // @era:section-cull-begin pop17|rendered19|mesh21
    /**
     * The draw itself ({@code drawChunkLayer} to 1.18.2, {@code draw} from 1.19): while vanilla draws
     * a solid / cutout layer, a sorted mesh is drawn as the one index range that can face the camera.
     */
    @WrapOperation(method = {"drawChunkLayer", "draw"}, at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;drawElements(III)V"), require = 0)
    private void aetherium$rangedDraw(final int mode, final int count, final int type, final Operation<Void> original) {
        if (SectionCull.layerOn() && this.aetherium$sorted && this.aetherium$cullSection != null
                && mode == SectionCull.GL_TRIANGLES) {
            final int bytes = SectionCull.indexBytes(type);
            if (bytes > 0) {
                try {
                    // @era:section-api-begin srd
                    final BlockPos origin = ((SectionRenderDispatcher.RenderSection) this.aetherium$cullSection).getOrigin();
                    // @era:section-api-else crd
                    //~ final BlockPos origin = ((ChunkRenderDispatcher.RenderChunk) this.aetherium$cullSection).getOrigin();
                    // @era:section-api-else v16|srd-ro|mesh|none
                    //~ final BlockPos origin = null;
                    // @era:section-api-end
                    final int plan = SectionCull.plan(this.aetherium$layout, origin.getX(), origin.getY(), origin.getZ(),
                            count);
                    if (plan == SectionCull.SKIP) {
                        return;
                    }
                    if (plan == SectionCull.RANGE) {
                        GlStateManager._drawElements(mode, SectionCull.rangeIndexCount(), type,
                                (long) SectionCull.rangeFirstIndex() * bytes);
                        return;
                    }
                } catch (final RuntimeException | LinkageError error) {
                    SectionCull.fail(error);
                }
            }
        }
        original.call(mode, count, type);
    }
    // @era:section-cull-else v16|none
    // @era:section-cull-end
}
