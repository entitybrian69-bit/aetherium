package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:cull-new-begin old
// @era:cull-new-else srd5
//~ import com.aetherium.client.SectionCull;
//~ import com.aetherium.client.SectionCullLayoutAccess;
//~ import com.aetherium.render.CompactTerrain;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//~ import com.mojang.blaze3d.buffers.GpuBuffer;
//~ import com.mojang.blaze3d.systems.RenderPass;
//~ import com.mojang.blaze3d.vertex.VertexFormat;
//~ import java.util.ArrayList;
//~ import java.util.function.Consumer;
//~ import net.minecraft.client.renderer.LevelRenderer;
//~ import net.minecraft.client.renderer.RenderType;
//~ import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
//~ import net.minecraft.core.BlockPos;
//~ import org.joml.Matrix4f;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:cull-new-else csm6
//~ import com.aetherium.client.SectionCull;
//~ import com.aetherium.client.SectionCullLayersAccess;
//~ import com.aetherium.render.CompactTerrain;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//~ import com.mojang.blaze3d.buffers.GpuBuffer;
//~ import com.mojang.blaze3d.systems.RenderPass;
//~ import com.mojang.blaze3d.vertex.VertexFormat;
//~ import java.util.List;
//~ import java.util.function.BiConsumer;
//~ import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
//~ import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
//~ import net.minecraft.client.renderer.chunk.SectionBuffers;
//~ import net.minecraft.client.renderer.chunk.SectionMesh;
//~ import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
//~ import net.minecraft.core.BlockPos;
//~ import org.joml.Matrix4fc;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
// @era:cull-new-else uber26
//~ import com.aetherium.client.ClientHooks;
//~ import com.aetherium.client.SectionCull;
//~ import com.aetherium.client.SectionCullLayersAccess;
//~ import com.aetherium.render.CompactTerrain;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//~ import com.mojang.blaze3d.buffers.GpuBuffer;
//~ import com.mojang.blaze3d.systems.RenderPass;
//~ import com.mojang.blaze3d.vertex.VertexFormat;
//~ import java.util.List;
//~ import java.util.function.BiConsumer;
//~ import net.minecraft.client.Camera;
//~ import net.minecraft.client.Minecraft;
//~ import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
//~ import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
//~ import net.minecraft.client.renderer.chunk.SectionMesh;
//~ import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
//~ import net.minecraft.core.BlockPos;
//~ import net.minecraft.world.phys.Vec3;
//~ import org.joml.Matrix4fc;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
// @era:cull-new-else uber262
//~ import com.aetherium.client.ClientHooks;
//~ import com.aetherium.client.SectionCull;
//~ import com.aetherium.client.SectionCullLayersAccess;
//~ import com.aetherium.render.CompactTerrain;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//~ import com.mojang.blaze3d.IndexType;
//~ import com.mojang.blaze3d.buffers.GpuBuffer;
//~ import com.mojang.blaze3d.systems.RenderPass;
//~ import java.util.List;
//~ import java.util.function.BiConsumer;
//~ import net.minecraft.client.Camera;
//~ import net.minecraft.client.Minecraft;
//~ import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
//~ import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
//~ import net.minecraft.client.renderer.chunk.SectionMesh;
//~ import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
//~ import net.minecraft.core.BlockPos;
//~ import net.minecraft.world.phys.Vec3;
//~ import org.joml.Matrix4fc;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
// @era:cull-new-else pearl
//~ import com.aetherium.client.ClientHooks;
//~ import com.aetherium.client.SectionCull;
//~ import com.aetherium.client.SectionCullLayersAccess;
//~ import com.aetherium.render.CompactTerrain;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//~ import java.util.List;
//~ import java.util.Map;
//~ import net.minecraft.client.Camera;
//~ import net.minecraft.client.Minecraft;
//~ import net.minecraft.client.renderer.DynamicGpuData;
//~ import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
//~ import net.minecraft.client.renderer.chunk.SectionMesh;
//~ import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
//~ import net.minecraft.core.BlockPos;
//~ import net.minecraft.world.phys.Vec3;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
// @era:cull-new-end

/**
 * The Aetherium renderer on 1.21.5+: vanilla builds one draw record ({@code RenderPass$Draw}) per
 * visible section layer and submits them together. For a solid / cutout layer whose vertices were
 * sorted by face direction, the record is narrowed to the one index range that can face the camera
 * (or left out when nothing can): never more draws than vanilla, same buffers, same uniforms.
 * Applied only where {@code Capabilities.SECTION_CULL_MODERN} is true.
 */
@Mixin(targets = "net.minecraft.client.renderer.LevelRenderer")
public abstract class SectionCullDrawMixin {
    // @era:cull-new-begin old
    // @era:cull-new-else srd5|csm6|uber26|uber262|pearl
    //~ /** Face groups behind the draw record being built next, or null. */
    //~ @Unique
    //~ private static CompactTerrain.Layout aetherium$recordLayout;
    //~ /** A record built but left out (no face can point at the camera); dropped from the list. */
    //~ @Unique
    //~ private static Object aetherium$skipped;

    //~ @Unique
    //~ private static boolean aetherium$drop(final Object record) {
        //~ if (record != null && record == aetherium$skipped) {
            //~ aetherium$skipped = null;
            //~ return true;
        //~ }
        //~ return false;
    //~ }

    //~ /** First index and count for a record ({@code plan} RANGE), or the original ones. */
    //~ @Unique
    //~ private static int aetherium$plan(final boolean sequential, final int firstIndex, final int indexCount) {
        //~ final CompactTerrain.Layout layout = aetherium$recordLayout;
        //~ aetherium$recordLayout = null;
        //~ aetherium$skipped = null;
        //~ try {
            //~ return SectionCull.planRecord(layout, sequential, firstIndex, indexCount);
        //~ } catch (final RuntimeException error) {
            //~ SectionCull.fail(error);
            //~ return SectionCull.FULL;
        //~ }
    //~ }
    // @era:cull-new-end

    // @era:cull-new-begin old
    // @era:cull-new-else srd5
    //~ @Inject(method = "renderLevel", at = @At("HEAD"), require = 0)
    //~ private void aetherium$cullFrame(final CallbackInfo ci) {
        //~ if (SectionCull.frame()) {
            //~ // Just turned on: rebuild so the meshes already on the GPU get sorted too.
            //~ ((LevelRenderer) (Object) this).allChanged();
        //~ }
    //~ }

    //~ @Inject(method = "renderSectionLayer", at = @At("HEAD"), require = 0)
    //~ private void aetherium$beginLayer(final RenderType type, final double camX, final double camY, final double camZ,
                                      //~ final Matrix4f modelView, final Matrix4f projection, final CallbackInfo ci) {
        //~ SectionCull.beginLayer(type == RenderType.solid() || type == RenderType.cutoutMipped() || type == RenderType.cutout(),
                //~ camX, camY, camZ);
    //~ }

    //~ @Inject(method = "renderSectionLayer", at = @At("RETURN"), require = 0)
    //~ private void aetherium$endLayer(final RenderType type, final double camX, final double camY, final double camZ,
                                    //~ final Matrix4f modelView, final Matrix4f projection, final CallbackInfo ci) {
        //~ SectionCull.endLayer();
    //~ }

    //~ @WrapOperation(method = "renderSectionLayer", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;getRenderOrigin()Lnet/minecraft/core/BlockPos;"),
            //~ require = 0)
    //~ private BlockPos aetherium$origin(final SectionRenderDispatcher.RenderSection section,
                                      //~ final Operation<BlockPos> original) {
        //~ final BlockPos origin = original.call(section);
        //~ SectionCull.setOrigin(origin.getX(), origin.getY(), origin.getZ());
        //~ return origin;
    //~ }

    //~ @WrapOperation(method = "renderSectionLayer", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$SectionBuffers;getVertexBuffer()Lcom/mojang/blaze3d/buffers/GpuBuffer;"),
            //~ require = 0)
    //~ private GpuBuffer aetherium$recordBuffers(final SectionRenderDispatcher.SectionBuffers buffers,
                                              //~ final Operation<GpuBuffer> original) {
        //~ final Object raw = buffers;
        //~ aetherium$recordLayout = raw instanceof SectionCullLayoutAccess ? ((SectionCullLayoutAccess) raw).aetheriumCullLayout() : null;
        //~ return original.call(buffers);
    //~ }

    //~ @WrapOperation(method = "renderSectionLayer", at = @At(value = "NEW",
            //~ target = "com/mojang/blaze3d/systems/RenderPass$Draw"),
            //~ require = 0)
    //~ private RenderPass.Draw aetherium$narrow(final int slot, final GpuBuffer vertices, final GpuBuffer indices,
                                             //~ final VertexFormat.IndexType indexType, final int firstIndex,
                                             //~ final int indexCount, final Consumer uniforms,
                                             //~ final Operation<RenderPass.Draw> original) {
        //~ final int plan = aetherium$plan(indices == null, firstIndex, indexCount);
        //~ if (plan == SectionCull.RANGE) {
            //~ return original.call(slot, vertices, indices, indexType, SectionCull.rangeFirstIndex(),
                    //~ SectionCull.rangeIndexCount(), uniforms);
        //~ }
        //~ final RenderPass.Draw record = original.call(slot, vertices, indices, indexType, firstIndex, indexCount, uniforms);
        //~ if (plan == SectionCull.SKIP) {
            //~ aetherium$skipped = record;
        //~ }
        //~ return record;
    //~ }

    //~ @WrapOperation(method = "renderSectionLayer", at = @At(value = "INVOKE",
            //~ target = "Ljava/util/ArrayList;add(Ljava/lang/Object;)Z"), require = 0)
    //~ private boolean aetherium$dropSkipped(final ArrayList<Object> list, final Object record,
                                          //~ final Operation<Boolean> original) {
        //~ return aetherium$drop(record) || original.call(list, record);
    //~ }
    // @era:cull-new-else csm6
    //~ @Inject(method = "prepareChunkRenders", at = @At("HEAD"), require = 0)
    //~ private void aetherium$beginRecords(final Matrix4fc modelView, final double camX, final double camY,
                                        //~ final double camZ, final CallbackInfoReturnable<ChunkSectionsToRender> cir) {
        //~ SectionCull.beginRecords(camX, camY, camZ);
    //~ }

    //~ @Inject(method = "prepareChunkRenders", at = @At("RETURN"), require = 0)
    //~ private void aetherium$endRecords(final Matrix4fc modelView, final double camX, final double camY,
                                      //~ final double camZ, final CallbackInfoReturnable<ChunkSectionsToRender> cir) {
        //~ SectionCull.endRecords();
    //~ }

    //~ @WrapOperation(method = "prepareChunkRenders", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;getRenderOrigin()Lnet/minecraft/core/BlockPos;"),
            //~ require = 0)
    //~ private BlockPos aetherium$origin(final SectionRenderDispatcher.RenderSection section,
                                      //~ final Operation<BlockPos> original) {
        //~ final BlockPos origin = original.call(section);
        //~ SectionCull.setOrigin(origin.getX(), origin.getY(), origin.getZ());
        //~ return origin;
    //~ }

    //~ @WrapOperation(method = "prepareChunkRenders", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/chunk/SectionMesh;getBuffers(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;)Lnet/minecraft/client/renderer/chunk/SectionBuffers;"),
            //~ require = 0)
    //~ private SectionBuffers aetherium$recordLayer(final SectionMesh mesh, final ChunkSectionLayer layer,
                                                 //~ final Operation<SectionBuffers> original) {
        //~ final Object raw = mesh;
        //~ aetherium$recordLayout = raw instanceof SectionCullLayersAccess && layer != null
                //~ && SectionCull.terrainLayerName(layer.name())
                //~ ? ((SectionCullLayersAccess) raw).aetheriumCullLayout(layer.ordinal()) : null;
        //~ return original.call(mesh, layer);
    //~ }

    //~ @WrapOperation(method = "prepareChunkRenders", at = @At(value = "NEW",
            //~ target = "com/mojang/blaze3d/systems/RenderPass$Draw"),
            //~ require = 0)
    //~ private RenderPass.Draw aetherium$narrow(final int slot, final GpuBuffer vertices, final GpuBuffer indices,
                                             //~ final VertexFormat.IndexType indexType, final int firstIndex,
                                             //~ final int indexCount, final BiConsumer uniforms,
                                             //~ final Operation<RenderPass.Draw> original) {
        //~ final int plan = aetherium$plan(indices == null, firstIndex, indexCount);
        //~ if (plan == SectionCull.RANGE) {
            //~ return original.call(slot, vertices, indices, indexType, SectionCull.rangeFirstIndex(),
                    //~ SectionCull.rangeIndexCount(), uniforms);
        //~ }
        //~ final RenderPass.Draw record = original.call(slot, vertices, indices, indexType, firstIndex, indexCount, uniforms);
        //~ if (plan == SectionCull.SKIP) {
            //~ aetherium$skipped = record;
        //~ }
        //~ return record;
    //~ }

    //~ @WrapOperation(method = "prepareChunkRenders", at = @At(value = "INVOKE",
            //~ target = "Ljava/util/List;add(Ljava/lang/Object;)Z"), require = 0)
    //~ private boolean aetherium$dropSkipped(final List<Object> list, final Object record,
                                          //~ final Operation<Boolean> original) {
        //~ return aetherium$drop(record) || original.call(list, record);
    //~ }
    // @era:cull-new-else uber26
    //~ @Inject(method = "prepareChunkRenders", at = @At("HEAD"), require = 0)
    //~ private void aetherium$beginRecords(final Matrix4fc modelView,
                                        //~ final CallbackInfoReturnable<ChunkSectionsToRender> cir) {
        //~ final Camera camera = ClientHooks.mainCamera(Minecraft.getInstance());
        //~ if (camera == null) {
            //~ SectionCull.endRecords();
            //~ return;
        //~ }
        //~ // @era:cam-pos-begin getter
        //~ final Vec3 pos = camera.getPosition();
        //~ // @era:cam-pos-else position
        //~ final Vec3 pos = camera.position();
        //~ // @era:cam-pos-end
        //~ SectionCull.beginRecords(pos.x, pos.y, pos.z);
    //~ }

    //~ @Inject(method = "prepareChunkRenders", at = @At("RETURN"), require = 0)
    //~ private void aetherium$endRecords(final Matrix4fc modelView,
                                      //~ final CallbackInfoReturnable<ChunkSectionsToRender> cir) {
        //~ SectionCull.endRecords();
    //~ }

    //~ @WrapOperation(method = "prepareChunkRenders", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;getRenderOrigin()Lnet/minecraft/core/BlockPos;"),
            //~ require = 0)
    //~ private BlockPos aetherium$origin(final SectionRenderDispatcher.RenderSection section,
                                      //~ final Operation<BlockPos> original) {
        //~ final BlockPos origin = original.call(section);
        //~ SectionCull.setOrigin(origin.getX(), origin.getY(), origin.getZ());
        //~ return origin;
    //~ }

    //~ @WrapOperation(method = "prepareChunkRenders", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/chunk/SectionMesh;getSectionDraw(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;)Lnet/minecraft/client/renderer/chunk/SectionMesh$SectionDraw;"),
            //~ require = 0)
    //~ private SectionMesh.SectionDraw aetherium$recordLayer(final SectionMesh mesh, final ChunkSectionLayer layer,
                                                          //~ final Operation<SectionMesh.SectionDraw> original) {
        //~ final Object raw = mesh;
        //~ aetherium$recordLayout = raw instanceof SectionCullLayersAccess && layer != null
                //~ && SectionCull.terrainLayerName(layer.name())
                //~ ? ((SectionCullLayersAccess) raw).aetheriumCullLayout(layer.ordinal()) : null;
        //~ return original.call(mesh, layer);
    //~ }

    //~ @WrapOperation(method = "prepareChunkRenders", at = @At(value = "NEW",
            //~ target = "com/mojang/blaze3d/systems/RenderPass$Draw"),
            //~ require = 0)
    //~ private RenderPass.Draw aetherium$narrow(final int slot, final GpuBuffer vertices, final GpuBuffer indices,
                                             //~ final VertexFormat.IndexType indexType, final int firstIndex,
                                             //~ final int indexCount, final int baseVertex, final BiConsumer uniforms,
                                             //~ final Operation<RenderPass.Draw> original) {
        //~ final int plan = aetherium$plan(indices == null, firstIndex, indexCount);
        //~ if (plan == SectionCull.RANGE) {
            //~ return original.call(slot, vertices, indices, indexType, SectionCull.rangeFirstIndex(),
                    //~ SectionCull.rangeIndexCount(), baseVertex, uniforms);
        //~ }
        //~ final RenderPass.Draw record = original.call(slot, vertices, indices, indexType, firstIndex, indexCount,
                //~ baseVertex, uniforms);
        //~ if (plan == SectionCull.SKIP) {
            //~ aetherium$skipped = record;
        //~ }
        //~ return record;
    //~ }

    //~ @WrapOperation(method = "prepareChunkRenders", at = @At(value = "INVOKE",
            //~ target = "Ljava/util/List;add(Ljava/lang/Object;)Z"), require = 0)
    //~ private boolean aetherium$dropSkipped(final List<Object> list, final Object record,
                                          //~ final Operation<Boolean> original) {
        //~ return aetherium$drop(record) || original.call(list, record);
    //~ }
    // @era:cull-new-else uber262
    //~ @Inject(method = "prepareChunkRenders", at = @At("HEAD"), require = 0)
    //~ private void aetherium$beginRecords(final Matrix4fc modelView,
                                        //~ final CallbackInfoReturnable<ChunkSectionsToRender> cir) {
        //~ final Camera camera = ClientHooks.mainCamera(Minecraft.getInstance());
        //~ if (camera == null) {
            //~ SectionCull.endRecords();
            //~ return;
        //~ }
        //~ // @era:cam-pos-begin getter
        //~ final Vec3 pos = camera.getPosition();
        //~ // @era:cam-pos-else position
        //~ final Vec3 pos = camera.position();
        //~ // @era:cam-pos-end
        //~ SectionCull.beginRecords(pos.x, pos.y, pos.z);
    //~ }

    //~ @Inject(method = "prepareChunkRenders", at = @At("RETURN"), require = 0)
    //~ private void aetherium$endRecords(final Matrix4fc modelView,
                                      //~ final CallbackInfoReturnable<ChunkSectionsToRender> cir) {
        //~ SectionCull.endRecords();
    //~ }

    //~ @WrapOperation(method = "prepareChunkRenders", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;getRenderOrigin()Lnet/minecraft/core/BlockPos;"),
            //~ require = 0)
    //~ private BlockPos aetherium$origin(final SectionRenderDispatcher.RenderSection section,
                                      //~ final Operation<BlockPos> original) {
        //~ final BlockPos origin = original.call(section);
        //~ SectionCull.setOrigin(origin.getX(), origin.getY(), origin.getZ());
        //~ return origin;
    //~ }

    //~ @WrapOperation(method = "prepareChunkRenders", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/chunk/SectionMesh;getSectionDraw(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;)Lnet/minecraft/client/renderer/chunk/SectionMesh$SectionDraw;"),
            //~ require = 0)
    //~ private SectionMesh.SectionDraw aetherium$recordLayer(final SectionMesh mesh, final ChunkSectionLayer layer,
                                                          //~ final Operation<SectionMesh.SectionDraw> original) {
        //~ final Object raw = mesh;
        //~ aetherium$recordLayout = raw instanceof SectionCullLayersAccess && layer != null
                //~ && SectionCull.terrainLayerName(layer.name())
                //~ ? ((SectionCullLayersAccess) raw).aetheriumCullLayout(layer.ordinal()) : null;
        //~ return original.call(mesh, layer);
    //~ }

    //~ @WrapOperation(method = "prepareChunkRenders", at = @At(value = "NEW",
            //~ target = "com/mojang/blaze3d/systems/RenderPass$Draw"),
            //~ require = 0)
    //~ private RenderPass.Draw aetherium$narrow(final int slot, final GpuBuffer vertices, final GpuBuffer indices,
                                             //~ final IndexType indexType, final int firstIndex, final int indexCount,
                                             //~ final int baseVertex, final BiConsumer uniforms,
                                             //~ final Operation<RenderPass.Draw> original) {
        //~ final int plan = aetherium$plan(indices == null, firstIndex, indexCount);
        //~ if (plan == SectionCull.RANGE) {
            //~ return original.call(slot, vertices, indices, indexType, SectionCull.rangeFirstIndex(),
                    //~ SectionCull.rangeIndexCount(), baseVertex, uniforms);
        //~ }
        //~ final RenderPass.Draw record = original.call(slot, vertices, indices, indexType, firstIndex, indexCount,
                //~ baseVertex, uniforms);
        //~ if (plan == SectionCull.SKIP) {
            //~ aetherium$skipped = record;
        //~ }
        //~ return record;
    //~ }

    //~ @WrapOperation(method = "prepareChunkRenders", at = @At(value = "INVOKE",
            //~ target = "Ljava/util/List;add(Ljava/lang/Object;)Z"), require = 0)
    //~ private boolean aetherium$dropSkipped(final List<Object> list, final Object record,
                                          //~ final Operation<Boolean> original) {
        //~ return aetherium$drop(record) || original.call(list, record);
    //~ }
    // @era:cull-new-else pearl
    //~ // 26.3: per-section records (DynamicGpuData$IndexedDraw) collected into draw groups, drawn either
    //~ // one by one or by a GPU indirect command; the record's index range is narrowed. A section with
    //~ // no face toward the camera keeps its full record (no zero-length draws on this backend).
    //~ @Inject(method = "extractSectionDrawGroups", at = @At("HEAD"), require = 0)
    //~ private void aetherium$beginRecords(final boolean flag, final List<Object> infos, final Map<Object, Object> groups,
                                        //~ final CallbackInfoReturnable<Integer> cir) {
        //~ final Camera camera = ClientHooks.mainCamera(Minecraft.getInstance());
        //~ if (camera == null) {
            //~ SectionCull.endRecords();
            //~ return;
        //~ }
        //~ // @era:cam-pos-begin getter
        //~ final Vec3 pos = camera.getPosition();
        //~ // @era:cam-pos-else position
        //~ final Vec3 pos = camera.position();
        //~ // @era:cam-pos-end
        //~ SectionCull.beginRecords(pos.x, pos.y, pos.z);
    //~ }

    //~ @Inject(method = "extractSectionDrawGroups", at = @At("RETURN"), require = 0)
    //~ private void aetherium$endRecords(final boolean flag, final List<Object> infos, final Map<Object, Object> groups,
                                      //~ final CallbackInfoReturnable<Integer> cir) {
        //~ SectionCull.endRecords();
    //~ }

    //~ @WrapOperation(method = "extractSectionDrawGroups", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;getRenderOrigin()Lnet/minecraft/core/BlockPos;"),
            //~ require = 0)
    //~ private BlockPos aetherium$origin(final SectionRenderDispatcher.RenderSection section,
                                      //~ final Operation<BlockPos> original) {
        //~ final BlockPos origin = original.call(section);
        //~ SectionCull.setOrigin(origin.getX(), origin.getY(), origin.getZ());
        //~ return origin;
    //~ }

    //~ @WrapOperation(method = "extractSectionDrawGroups", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/chunk/SectionMesh;getSectionDraw(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;)Lnet/minecraft/client/renderer/chunk/SectionMesh$SectionDraw;"),
            //~ require = 0)
    //~ private SectionMesh.SectionDraw aetherium$recordLayer(final SectionMesh mesh, final ChunkSectionLayer layer,
                                                          //~ final Operation<SectionMesh.SectionDraw> original) {
        //~ final SectionMesh.SectionDraw draw = original.call(mesh, layer);
        //~ final Object raw = mesh;
        //~ aetherium$recordLayout = draw != null && !draw.hasCustomIndexBuffer() && raw instanceof SectionCullLayersAccess
                //~ && layer != null && SectionCull.terrainLayerName(layer.name())
                //~ ? ((SectionCullLayersAccess) raw).aetheriumCullLayout(layer.ordinal()) : null;
        //~ return draw;
    //~ }

    //~ @WrapOperation(method = "extractSectionDrawGroups", at = @At(value = "NEW",
            //~ target = "net/minecraft/client/renderer/DynamicGpuData$IndexedDraw"), require = 0)
    //~ private DynamicGpuData.IndexedDraw aetherium$narrow(final int indexCount, final int instances, final int firstIndex,
                                                        //~ final int baseVertex, final int baseInstance,
                                                        //~ final Operation<DynamicGpuData.IndexedDraw> original) {
        //~ if (aetherium$plan(true, firstIndex, indexCount) == SectionCull.RANGE) {
            //~ return original.call(SectionCull.rangeIndexCount(), instances, SectionCull.rangeFirstIndex(), baseVertex,
                    //~ baseInstance);
        //~ }
        //~ aetherium$skipped = null;
        //~ return original.call(indexCount, instances, firstIndex, baseVertex, baseInstance);
    //~ }
    // @era:cull-new-end
}
