package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:model-fast-begin none
// @era:model-fast-else v16
//~ import com.aetherium.perf.ModelCubeData;
//~ import com.aetherium.perf.ModelVertexWriter;
//~ import com.aetherium.perf.RenderToggles;
//~ import com.mojang.blaze3d.systems.RenderSystem;
//~ import com.mojang.blaze3d.vertex.BufferBuilder;
//~ import com.mojang.blaze3d.vertex.DefaultVertexFormat;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import com.mojang.blaze3d.vertex.VertexConsumer;
//~ import it.unimi.dsi.fastutil.objects.ObjectList;
//~ import java.nio.ByteBuffer;
//~ import java.nio.FloatBuffer;
//~ import net.minecraft.client.model.geom.ModelPart;
//~ import org.spongepowered.asm.mixin.Final;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:model-fast-else mojang|joml|trusted
//~ import com.aetherium.perf.ModelCubeData;
//~ import com.aetherium.perf.ModelVertexWriter;
//~ import com.aetherium.perf.RenderToggles;
//~ import com.mojang.blaze3d.systems.RenderSystem;
//~ import com.mojang.blaze3d.vertex.BufferBuilder;
//~ import com.mojang.blaze3d.vertex.DefaultVertexFormat;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import com.mojang.blaze3d.vertex.VertexConsumer;
//~ import com.mojang.blaze3d.vertex.VertexFormat;
//~ import java.util.List;
//~ import java.nio.ByteBuffer;
//~ // @era:math-begin mojang
//~ import java.nio.FloatBuffer;
//~ // @era:math-else joml
//~ // @era:math-end
//~ import net.minecraft.client.model.geom.ModelPart;
//~ import org.spongepowered.asm.mixin.Final;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:model-fast-end

/**
 * Fast mob models (1.16.5..1.20.6). {@code ModelPart.compile} copies the face normal and allocates a
 * {@code Vector4f} for every vertex, then pushes each vertex through fourteen arguments of
 * {@code BufferBuilder.vertex}. When the target is a plain {@code BufferBuilder} in the entity
 * format, at a vertex boundary and without a fixed colour (exactly when vanilla takes its own
 * fast path), the part's cubes are written straight into the buffer: same transform, same packing,
 * same bytes ({@code ModelVertexWriter}, checked against vanilla's arithmetic in the tests), then
 * the builder's write position and vertex count advance as {@code endVertex} would have. Anything
 * else (outline and glint wrappers, other formats, other threads) takes vanilla's path, and an
 * exception turns the feature off for the session before anything was committed. Applied only
 * where {@code Capabilities.FAST_MODELS} is true.
 */
@Mixin(targets = "net.minecraft.client.model.geom.ModelPart")
public abstract class FastModelMixin {
    // @era:model-fast-begin none
    // @era:model-fast-else v16
    //~ /** GL_QUADS, the only mode entity buffers use. */
    //~ @Unique
    //~ private static final int AETHERIUM$QUADS = 7;

    //~ /** Render thread only. */
    //~ @Unique
    //~ private static final float[] AETHERIUM$POSE = new float[16];

    //~ @Unique
    //~ private static final FloatBuffer AETHERIUM$POSE_BUFFER = FloatBuffer.wrap(AETHERIUM$POSE);

    //~ @Unique
    //~ private static final float[] AETHERIUM$NORMAL = new float[9];

    //~ @Shadow
    //~ @Final
    //~ private ObjectList<ModelPart.Cube> cubes;

    //~ @Inject(method = "compile", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$fastCompile(final PoseStack.Pose pose, final VertexConsumer consumer, final int light,
                                       //~ final int overlay, final float r, final float g, final float b, final float a,
                                       //~ final CallbackInfo ci) {
        //~ if (!RenderToggles.fastModels || consumer == null || consumer.getClass() != BufferBuilder.class
                //~ || !RenderSystem.isOnRenderThread()) {
            //~ return;
        //~ }
        //~ try {
            //~ if (this.aetherium$write(pose, consumer, light, overlay, r, g, b, a)) {
                //~ ci.cancel();
            //~ }
        //~ } catch (final RuntimeException | LinkageError e) {
            //~ RenderToggles.breakFastModels(e);
        //~ }
    //~ }

    //~ /** @return true when the part was written (vanilla must not run) */
    //~ @Unique
    //~ private boolean aetherium$write(final PoseStack.Pose pose, final VertexConsumer consumer, final int light,
                                    //~ final int overlay, final float r, final float g, final float b, final float a) {
        //~ final Object target = consumer;
        //~ if (!(target instanceof BufferBuilderAccessor) || !(target instanceof DefaultedVertexAccessor)) {
            //~ return false;
        //~ }
        //~ final BufferBuilderAccessor builder = (BufferBuilderAccessor) target;
        //~ if (!builder.aetherium$building() || builder.aetherium$mode() != AETHERIUM$QUADS
                //~ || builder.aetherium$format() != DefaultVertexFormat.NEW_ENTITY || builder.aetherium$elementIndex() != 0
                //~ || ((DefaultedVertexAccessor) target).aetherium$defaultColorSet()) {
            //~ return false;
        //~ }
        //~ final Object normalMatrix = pose.normal();
        //~ if (!(normalMatrix instanceof Matrix3fAccessor)) {
            //~ return false;
        //~ }
        //~ final ObjectList<ModelPart.Cube> list = this.cubes;
        //~ final int count = list.size();
        //~ int vertices = 0;
        //~ for (int i = 0; i < count; i++) {
            //~ final Object cube = list.get(i);
            //~ if (!(cube instanceof ModelCubeData)) {
                //~ return false;
            //~ }
            //~ final float[] faces = ((ModelCubeData) cube).aetheriumFaces();
            //~ if (faces == null) {
                //~ return false;
            //~ }
            //~ vertices += faces.length / ModelCubeData.FACE_FLOATS * 4;
        //~ }
        //~ if (vertices == 0) {
            //~ return false;
        //~ }
        //~ builder.aetherium$ensureCapacity(vertices * ModelVertexWriter.STRIDE);
        //~ final ByteBuffer buffer = builder.aetherium$buffer();
        //~ if (!ModelVertexWriter.usable(buffer)) {
            //~ return false;
        //~ }
        //~ pose.pose().store(AETHERIUM$POSE_BUFFER);
        //~ final Matrix3fAccessor n = (Matrix3fAccessor) normalMatrix;
        //~ final float[] nm = AETHERIUM$NORMAL;
        //~ nm[0] = n.aetherium$m00();
        //~ nm[1] = n.aetherium$m01();
        //~ nm[2] = n.aetherium$m02();
        //~ nm[3] = n.aetherium$m10();
        //~ nm[4] = n.aetherium$m11();
        //~ nm[5] = n.aetherium$m12();
        //~ nm[6] = n.aetherium$m20();
        //~ nm[7] = n.aetherium$m21();
        //~ nm[8] = n.aetherium$m22();
        //~ final int color = ModelVertexWriter.packColor(r, g, b, a);
        //~ final int start = builder.aetherium$nextElementByte();
        //~ int at = start;
        //~ for (int i = 0; i < count; i++) {
            //~ at += ModelVertexWriter.writeFaces(buffer, at, AETHERIUM$POSE, nm,
                    //~ ((ModelCubeData) list.get(i)).aetheriumFaces(), color, overlay, light);
        //~ }
        //~ // Committed only after every byte is written (an exception above leaves the builder as it was).
        //~ builder.aetherium$setNextElementByte(at);
        //~ builder.aetherium$setVertices(builder.aetherium$vertices() + vertices);
        //~ builder.aetherium$ensureVertexCapacity();
        //~ return true;
    //~ }
    // @era:model-fast-else mojang|joml|trusted
    //~ /** Render thread only. */
    //~ @Unique
    //~ private static final float[] AETHERIUM$POSE = new float[16];

    //~ // @era:math-begin mojang
    //~ @Unique
    //~ private static final FloatBuffer AETHERIUM$POSE_BUFFER = FloatBuffer.wrap(AETHERIUM$POSE);
    //~ // @era:math-else joml
    //~ // @era:math-end

    //~ @Unique
    //~ private static final float[] AETHERIUM$NORMAL = new float[9];

    //~ @Shadow
    //~ @Final
    //~ private List<ModelPart.Cube> cubes;

    //~ @Inject(method = "compile", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$fastCompile(final PoseStack.Pose pose, final VertexConsumer consumer, final int light,
                                       //~ final int overlay, final float r, final float g, final float b, final float a,
                                       //~ final CallbackInfo ci) {
        //~ if (!RenderToggles.fastModels || consumer == null || consumer.getClass() != BufferBuilder.class
                //~ || !RenderSystem.isOnRenderThread()) {
            //~ return;
        //~ }
        //~ try {
            //~ if (this.aetherium$write(pose, consumer, light, overlay, r, g, b, a)) {
                //~ ci.cancel();
            //~ }
        //~ } catch (final RuntimeException | LinkageError e) {
            //~ RenderToggles.breakFastModels(e);
        //~ }
    //~ }

    //~ /** @return true when the part was written (vanilla must not run) */
    //~ @Unique
    //~ private boolean aetherium$write(final PoseStack.Pose pose, final VertexConsumer consumer, final int light,
                                    //~ final int overlay, final float r, final float g, final float b, final float a) {
        //~ final Object target = consumer;
        //~ if (!(target instanceof BufferBuilderAccessor) || !(target instanceof DefaultedVertexAccessor)) {
            //~ return false;
        //~ }
        //~ final BufferBuilderAccessor builder = (BufferBuilderAccessor) target;
        //~ if (!builder.aetherium$building() || builder.aetherium$mode() != VertexFormat.Mode.QUADS
                //~ || builder.aetherium$format() != DefaultVertexFormat.NEW_ENTITY || builder.aetherium$elementIndex() != 0
                //~ || ((DefaultedVertexAccessor) target).aetherium$defaultColorSet()) {
            //~ return false;
        //~ }
        //~ // @era:math-begin mojang
        //~ final Object normalMatrix = pose.normal();
        //~ if (!(normalMatrix instanceof Matrix3fAccessor)) {
            //~ return false;
        //~ }
        //~ // @era:math-else joml
        //~ // @era:math-end
        //~ final List<ModelPart.Cube> list = this.cubes;
        //~ final int count = list.size();
        //~ int vertices = 0;
        //~ for (int i = 0; i < count; i++) {
            //~ final Object cube = list.get(i);
            //~ if (!(cube instanceof ModelCubeData)) {
                //~ return false;
            //~ }
            //~ final float[] faces = ((ModelCubeData) cube).aetheriumFaces();
            //~ if (faces == null) {
                //~ return false;
            //~ }
            //~ vertices += faces.length / ModelCubeData.FACE_FLOATS * 4;
        //~ }
        //~ if (vertices == 0) {
            //~ return false;
        //~ }
        //~ builder.aetherium$ensureCapacity(vertices * ModelVertexWriter.STRIDE);
        //~ final ByteBuffer buffer = builder.aetherium$buffer();
        //~ if (!ModelVertexWriter.usable(buffer)) {
            //~ return false;
        //~ }
        //~ // @era:math-begin mojang
        //~ pose.pose().store(AETHERIUM$POSE_BUFFER);
        //~ final Matrix3fAccessor n = (Matrix3fAccessor) normalMatrix;
        //~ final float[] nm = AETHERIUM$NORMAL;
        //~ nm[0] = n.aetherium$m00();
        //~ nm[1] = n.aetherium$m01();
        //~ nm[2] = n.aetherium$m02();
        //~ nm[3] = n.aetherium$m10();
        //~ nm[4] = n.aetherium$m11();
        //~ nm[5] = n.aetherium$m12();
        //~ nm[6] = n.aetherium$m20();
        //~ nm[7] = n.aetherium$m21();
        //~ nm[8] = n.aetherium$m22();
        //~ // @era:math-else joml
        //~ pose.pose().get(AETHERIUM$POSE);
        //~ // joml names entries column first (mCR); the writer takes row-major nm[r * 3 + c].
        //~ final org.joml.Matrix3f n = pose.normal();
        //~ final float[] nm = AETHERIUM$NORMAL;
        //~ nm[0] = n.m00();
        //~ nm[1] = n.m10();
        //~ nm[2] = n.m20();
        //~ nm[3] = n.m01();
        //~ nm[4] = n.m11();
        //~ nm[5] = n.m21();
        //~ nm[6] = n.m02();
        //~ nm[7] = n.m12();
        //~ nm[8] = n.m22();
        //~ // @era:math-end
        //~ // @era:pose-normals-begin trusted
        //~ // 1.20.5+: PoseStack.Pose.transformNormal normalises unless the pose's normals are trusted.
        //~ final Object poseObject = pose;
        //~ if (!(poseObject instanceof PoseNormalsAccessor)) {
            //~ return false;
        //~ }
        //~ final boolean normalize = !((PoseNormalsAccessor) poseObject).aetherium$trustedNormals();
        //~ // @era:pose-normals-else plain
        //~ // @era:pose-normals-end
        //~ final int color = ModelVertexWriter.packColor(r, g, b, a);
        //~ final int start = builder.aetherium$nextElementByte();
        //~ int at = start;
        //~ for (int i = 0; i < count; i++) {
            //~ // @era:pose-normals-begin plain
            //~ at += ModelVertexWriter.writeFaces(buffer, at, AETHERIUM$POSE, nm,
                    //~ ((ModelCubeData) list.get(i)).aetheriumFaces(), color, overlay, light);
            //~ // @era:pose-normals-else trusted
            //~ at += ModelVertexWriter.writeFaces(buffer, at, AETHERIUM$POSE, nm,
                    //~ ((ModelCubeData) list.get(i)).aetheriumFaces(), color, overlay, light, normalize);
            //~ // @era:pose-normals-end
        //~ }
        //~ // Committed only after every byte is written (an exception above leaves the builder as it was).
        //~ builder.aetherium$setNextElementByte(at);
        //~ builder.aetherium$setVertices(builder.aetherium$vertices() + vertices);
        //~ builder.aetherium$ensureVertexCapacity();
        //~ return true;
    //~ }
    // @era:model-fast-end
}
