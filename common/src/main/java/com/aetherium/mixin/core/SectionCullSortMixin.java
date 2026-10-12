package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:cull-new-begin old
// @era:cull-new-else srd5|csm6|uber26|uber262|pearl
//~ import com.aetherium.client.SectionCull;
//~ import com.aetherium.client.SectionCullLayoutAccess;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//~ import com.mojang.blaze3d.vertex.DefaultVertexFormat;
//~ import com.mojang.blaze3d.vertex.MeshData;
//~ import java.util.Map;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
// @era:cull-new-end
// @era:cull-new-begin old
// @era:cull-new-else srd5|csm6|uber26|uber262
//~ import com.mojang.blaze3d.vertex.VertexFormat;
// @era:cull-new-else pearl
//~ import com.mojang.renderpearl.api.vertex.VertexFormat;
// @era:cull-new-end
// @era:cull-new-begin old|csm6|uber26|uber262|pearl
// @era:cull-new-else srd5
//~ import net.minecraft.client.renderer.RenderType;
// @era:cull-new-end

/**
 * The Aetherium renderer on 1.21.5+: as the chunk builder stores each finished layer mesh, a solid
 * / cutout mesh of vanilla block-vertex quads with sequential indices is reordered by face direction
 * (on the builder thread, off the render thread) and remembers its face groups. Translucent meshes
 * keep vanilla's own sorted indices. Applied only where {@code Capabilities.SECTION_CULL_MODERN} is
 * true.
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionCompiler")
public abstract class SectionCullSortMixin {
    // @era:cull-new-begin old
    // @era:cull-new-else srd5|csm6|uber26|uber262|pearl
    //~ /** Vanilla's block vertex up to 1.21.11: position, colour, uv, light, normal, padding. */
    //~ @Unique
    //~ private static final int WITH_NORMAL = 32;
    //~ /** Vanilla's block vertex from 26.1: position, colour, uv, light. */
    //~ @Unique
    //~ private static final int WITHOUT_NORMAL = 28;

    //~ @WrapOperation(method = "compile", at = @At(value = "INVOKE",
            //~ target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), require = 0)
    //~ private Object aetherium$sortLayer(final Map<Object, Object> map, final Object layer, final Object mesh,
                                       //~ final Operation<Object> original) {
        //~ if (mesh instanceof MeshData && SectionCull.sorting() && aetherium$terrain(layer)) {
            //~ try {
                //~ final MeshData data = (MeshData) mesh;
                //~ final int stride = aetherium$blockStride(data);
                //~ if (stride != 0) {
                    //~ ((SectionCullLayoutAccess) mesh).aetheriumSetCullLayout(
                            //~ SectionCull.sortVertices(data.vertexBuffer(), stride == WITH_NORMAL));
                //~ }
            //~ } catch (final RuntimeException | LinkageError error) {
                //~ SectionCull.fail(error);
            //~ }
        //~ }
        //~ return original.call(map, layer, mesh);
    //~ }
    // @era:cull-new-end

    // @era:cull-new-begin old
    // @era:cull-new-else srd5|csm6
    //~ /** Vertex size of a mesh this version's sorter takes, or 0 to leave it alone. */
    //~ @Unique
    //~ private static int aetherium$blockStride(final MeshData data) {
        //~ final MeshData.DrawState state = data.drawState();
        //~ final VertexFormat format = state.format();
        //~ return state.mode() == VertexFormat.Mode.QUADS && data.indexBuffer() == null
                //~ && format == DefaultVertexFormat.BLOCK && format.getVertexSize() == WITH_NORMAL ? WITH_NORMAL : 0;
    //~ }
    // @era:cull-new-else uber26
    //~ /** Vertex size of a mesh this version's sorter takes, or 0 to leave it alone. */
    //~ @Unique
    //~ private static int aetherium$blockStride(final MeshData data) {
        //~ final MeshData.DrawState state = data.drawState();
        //~ final VertexFormat format = state.format();
        //~ return state.mode() == VertexFormat.Mode.QUADS && data.indexBuffer() == null
                //~ && format == DefaultVertexFormat.BLOCK && format.getVertexSize() == WITHOUT_NORMAL ? WITHOUT_NORMAL : 0;
    //~ }
    // @era:cull-new-else uber262|pearl
    //~ /** Vertex size of a mesh this version's sorter takes, or 0 to leave it alone. */
    //~ @Unique
    //~ private static int aetherium$blockStride(final MeshData data) {
        //~ final MeshData.DrawState state = data.drawState();
        //~ final VertexFormat format = state.format();
        //~ final Object topology = state.primitiveTopology();
        //~ final boolean quads = topology instanceof Enum && "QUADS".equals(((Enum<?>) topology).name());
        //~ return quads && data.indexBuffer() == null && format == DefaultVertexFormat.BLOCK
                //~ && format.getVertexSize() == WITHOUT_NORMAL ? WITHOUT_NORMAL : 0;
    //~ }
    // @era:cull-new-end

    // @era:cull-new-begin old
    // @era:cull-new-else srd5
    //~ @Unique
    //~ private static boolean aetherium$terrain(final Object layer) {
        //~ return layer == RenderType.solid() || layer == RenderType.cutoutMipped() || layer == RenderType.cutout();
    //~ }
    // @era:cull-new-else csm6|uber26|uber262|pearl
    //~ @Unique
    //~ private static boolean aetherium$terrain(final Object layer) {
        //~ return layer instanceof Enum && SectionCull.terrainLayerName(((Enum<?>) layer).name());
    //~ }
    // @era:cull-new-end
}
