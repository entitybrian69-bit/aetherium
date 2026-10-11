package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.aetherium.client.ChunkInfoOrigin;
//~ import com.aetherium.client.FogTracker;
//~ import com.aetherium.client.QolHooks;
//~ import com.aetherium.mixin.AetheriumMixinPlugin;
//~ import com.aetherium.perf.FogCull;
//~ import com.aetherium.perf.RenderToggles;
//~ import com.aetherium.perf.SectionSprites;
//~ import com.aetherium.perf.SpriteActivity;
//~ import com.aetherium.perf.ViewFrustum;
//~ import com.aetherium.perf.VisibilitySearch;
//~ import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import com.mojang.math.Matrix4f;
//~ import it.unimi.dsi.fastutil.objects.ObjectList;
//~ import java.nio.FloatBuffer;
//~ import net.minecraft.client.Camera;
//~ import net.minecraft.client.Minecraft;
//~ import net.minecraft.client.renderer.GameRenderer;
//~ import net.minecraft.client.renderer.LevelRenderer;
//~ import net.minecraft.client.renderer.LightTexture;
//~ import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
//~ import net.minecraft.client.renderer.culling.Frustum;
//~ import net.minecraft.world.phys.AABB;
//~ import net.minecraft.world.phys.Vec3;
//~ import org.spongepowered.asm.mixin.Final;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:qol16-end

/**
 * Terrain streaming and culling in {@code LevelRenderer} (1.16.5; the mesh threads are
 * {@code MeshExecutorMixin}):
 * <ul>
 *   <li>sections dirtied repeatedly while chunks arrive are rebuilt once
 *       ({@code RebuildCoalescer}), by answering "not dirty yet" at the one {@code isDirty()} call
 *       of {@code setupRender} while a section is still settling;</li>
 *   <li>the visible-section search that vanilla repeats every frame while chunks upload, while any
 *       visible section is dirty and while sections wait to be meshed runs at most 20 times a
 *       second ({@code VisibilitySearch}); camera moves still update it at once;</li>
 *   <li>sections wholly inside the distance fog are treated as outside the view
 *       ({@code FogCull}), so they are not drawn and the search does not walk past them;</li>
 *   <li>once per frame: this frame's view frustum ({@code ViewFrustum}, for particles and fog
 *       culling) and, once per tick, the animated sprites of the visible sections are marked
 *       ({@code SpriteActivity}).</li>
 * </ul>
 * Applied only where {@code Capabilities.STREAMING} is true.
 */
@Mixin(targets = "net.minecraft.client.renderer.LevelRenderer")
public abstract class StreamingLevelMixin {
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ /** PUTFIELD, for field-write injection points. */
    //~ @Unique
    //~ private static final int AETHERIUM$PUTFIELD = 181;

    //~ @Unique
    //~ private static final FloatBuffer AETHERIUM$PROJECTION = FloatBuffer.allocate(16);

    //~ @Unique
    //~ private static final FloatBuffer AETHERIUM$VIEW = FloatBuffer.allocate(16);

    //~ /** Fog-end writes counted when the frame started (render thread). */
    //~ @Unique
    //~ private static int aetherium$fogWritesAtStart;

    //~ @Unique
    //~ private static double aetherium$camX;

    //~ @Unique
    //~ private static double aetherium$camY;

    //~ @Unique
    //~ private static double aetherium$camZ;

    //~ @Unique
    //~ private static boolean aetherium$spriteHooksChecked;

    //~ @Shadow
    //~ private boolean needsUpdate;

    //~ @Shadow
    //~ @Final
    //~ private ObjectList<?> renderChunks;

    //~ @Inject(method = "renderLevel", at = @At("HEAD"), require = 0)
    //~ private void aetherium$frameStart(final PoseStack poseStack, final float partialTick, final long finishNanos,
                                      //~ final boolean renderOutline, final Camera camera, final GameRenderer gameRenderer,
                                      //~ final LightTexture lightTexture, final Matrix4f projection, final CallbackInfo ci) {
        //~ // The same two matrices vanilla builds its own frustum from this frame.
        //~ projection.store(AETHERIUM$PROJECTION);
        //~ poseStack.last().pose().store(AETHERIUM$VIEW);
        //~ ViewFrustum.FRAME.update(AETHERIUM$PROJECTION, AETHERIUM$VIEW);
        //~ aetherium$fogWritesAtStart = FogTracker.endWrites();
        //~ SpriteActivity.frame();
        //~ if (SpriteActivity.enabled() && SpriteActivity.seenPassDue()) {
            //~ this.aetherium$markVisibleSprites();
        //~ }
    //~ }

    //~ /** Once per atlas tick: the animated sprites of every section in the visible list are in use. */
    //~ @Unique
    //~ private void aetherium$markVisibleSprites() {
        //~ final ObjectList<?> chunks = this.renderChunks;
        //~ final int count = chunks.size();
        //~ if (count == 0) {
            //~ return;
        //~ }
        //~ if (!aetherium$spriteHooksChecked) {
            //~ // Sections exist, so their mesh hooks had their chance to apply: without them visible
            //~ // textures would pause, so everything keeps animating instead.
            //~ aetherium$spriteHooksChecked = true;
            //~ if (!AetheriumMixinPlugin.isApplied("SectionSpritesMixin")
                    //~ || !AetheriumMixinPlugin.isApplied("CompiledChunkSpritesMixin")
                    //~ || !AetheriumMixinPlugin.isApplied("BlockSpriteUseMixin")) {
                //~ SpriteActivity.markBroken();
                //~ return;
            //~ }
        //~ }
        //~ for (int i = 0; i < count; i++) {
            //~ final Object info = chunks.get(i);
            //~ if (!(info instanceof ChunkInfoOrigin)) {
                //~ SpriteActivity.markBroken();
                //~ return;
            //~ }
            //~ final Object chunk = ((ChunkInfoOrigin) info).aetheriumChunk();
            //~ if (chunk instanceof ChunkRenderDispatcher.RenderChunk) {
                //~ final Object compiled = ((ChunkRenderDispatcher.RenderChunk) chunk).getCompiledChunk();
                //~ if (!(compiled instanceof SectionSprites)) {
                    //~ SpriteActivity.markBroken();
                    //~ return;
                //~ }
                //~ SpriteActivity.seen(((SectionSprites) compiled).aetheriumSprites());
            //~ }
        //~ }
    //~ }

    //~ @Inject(method = "setupRender", at = @At("HEAD"), require = 0)
    //~ private void aetherium$camera(final Camera camera, final Frustum frustum, final boolean hasCapturedFrustum,
                                  //~ final int frame, final boolean spectator, final CallbackInfo ci) {
        //~ QolHooks.setRebuildCamera(camera);
        //~ final boolean smart = RenderToggles.smartChunkCulling;
        //~ VisibilitySearch.beginFrame(System.nanoTime(), smart);
        //~ if (VisibilitySearch.takePending()) {
            //~ this.needsUpdate = true;
        //~ }
        //~ final Vec3 pos = camera.getPosition();
        //~ aetherium$camX = pos.x;
        //~ aetherium$camY = pos.y;
        //~ aetherium$camZ = pos.z;
        //~ FogCull.begin(smart, FogTracker.endWrites() != aetherium$fogWritesAtStart, FogTracker.mode(),
                //~ FogTracker.start(), FogTracker.end(), Minecraft.getInstance().gameRenderer.getRenderDistance(),
                //~ ViewFrustum.FRAME);
    //~ }

    //~ @WrapOperation(method = "setupRender", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk;isDirty()Z"), require = 0)
    //~ private boolean aetherium$coalesce(final ChunkRenderDispatcher.RenderChunk chunk, final Operation<Boolean> original) {
        //~ // Direct call instead of original.call: no argument array per section per frame.
        //~ return chunk.isDirty() && !QolHooks.deferRebuild(chunk);
    //~ }

    //~ /** Both frustum tests of the visible-section search: a section wholly inside the fog counts as outside. */
    //~ @WrapOperation(method = "setupRender", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/culling/Frustum;isVisible(Lnet/minecraft/world/phys/AABB;)Z"),
            //~ require = 0)
    //~ private boolean aetherium$fogCull(final Frustum frustum, final AABB box, final Operation<Boolean> original) {
        //~ if (!frustum.isVisible(box)) {
            //~ return false;
        //~ }
        //~ if (!FogCull.active()) {
            //~ return true;
        //~ }
        //~ return !FogCull.hidden(ViewFrustum.FRAME,
                //~ (float) ((box.minX + box.maxX) * 0.5 - aetherium$camX),
                //~ (float) ((box.minY + box.maxY) * 0.5 - aetherium$camY),
                //~ (float) ((box.minZ + box.maxZ) * 0.5 - aetherium$camZ),
                //~ (float) ((box.maxX - box.minX) * 0.5), (float) ((box.maxY - box.minY) * 0.5),
                //~ (float) ((box.maxZ - box.minZ) * 0.5));
    //~ }

    //~ /** {@code !chunksToCompile.isEmpty()} in the update condition (camera moves are separate terms). */
    //~ @ModifyExpressionValue(method = "setupRender", at = @At(value = "INVOKE", target = "Ljava/util/Set;isEmpty()Z"),
            //~ require = 0)
    //~ private boolean aetherium$holdQueueTrigger(final boolean empty) {
        //~ return VisibilitySearch.compileQueueEmpty(empty);
    //~ }

    //~ /** {@code needsUpdate = false} where the search starts: it ran this frame. */
    //~ @Inject(method = "setupRender", at = @At(value = "FIELD",
            //~ target = "Lnet/minecraft/client/renderer/LevelRenderer;needsUpdate:Z", opcode = AETHERIUM$PUTFIELD,
            //~ ordinal = 1), require = 0)
    //~ private void aetherium$searched(final Camera camera, final Frustum frustum, final boolean hasCapturedFrustum,
                                    //~ final int frame, final boolean spectator, final CallbackInfo ci) {
        //~ VisibilitySearch.searched(System.nanoTime());
    //~ }

    //~ /** {@code needsUpdate = true} for a visible section that is dirty or queued. */
    //~ @WrapOperation(method = "setupRender", at = @At(value = "FIELD",
            //~ target = "Lnet/minecraft/client/renderer/LevelRenderer;needsUpdate:Z", opcode = AETHERIUM$PUTFIELD,
            //~ ordinal = 2), require = 0)
    //~ private void aetherium$holdDirtyTrigger(final LevelRenderer self, final boolean value,
                                            //~ final Operation<Void> original) {
        //~ // Written directly (no argument array per dirty section per frame).
        //~ if (!value || VisibilitySearch.dirtyTrigger()) {
            //~ this.needsUpdate = value;
        //~ }
    //~ }

    //~ @ModifyExpressionValue(method = "compileChunksUntil", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher;uploadAllPendingUploads()Z"), require = 0)
    //~ private boolean aetherium$throttleVisibility(final boolean uploaded) {
        //~ return QolHooks.visibilityUpdateAfterUploads(uploaded);
    //~ }
    // @era:qol16-end
}
