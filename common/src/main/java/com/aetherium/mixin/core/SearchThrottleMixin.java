package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:search-throttle-begin v16|none
// @era:search-throttle-else bfs
//~ import com.aetherium.client.QolHooks;
//~ import com.aetherium.perf.RenderToggles;
//~ import com.aetherium.perf.VisibilitySearch;
//~ import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//~ import net.minecraft.client.renderer.LevelRenderer;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:search-throttle-end

/**
 * Visible-section search throttle on 1.17 (part of "Smarter chunk culling"; 1.16.5 has the same
 * hooks in StreamingLevelMixin). The search runs on the render thread whenever {@code needsUpdate}
 * is set; while terrain streams in, the triggers that only discover sections behind the visible
 * ones (finished uploads, a non-empty compile queue, dirty visible sections) are limited to 20
 * searches a second ({@code VisibilitySearch}). Camera moves still search at once.
 */
@Mixin(targets = "net.minecraft.client.renderer.LevelRenderer")
public abstract class SearchThrottleMixin {

    // @era:search-throttle-begin v16|none
    // @era:search-throttle-else bfs
    //~ /** {@code Opcodes.PUTFIELD}. */
    //~ @Unique
    //~ private static final int AETHERIUM$PUTFIELD = 181;

    //~ @Shadow
    //~ private boolean needsUpdate;

    //~ @Inject(method = "setupRender", at = @At("HEAD"), require = 0)
    //~ private void aetherium$throttleFrame(final CallbackInfo ci) {
        //~ VisibilitySearch.beginFrame(System.nanoTime(), RenderToggles.smartChunkCulling);
        //~ if (VisibilitySearch.takePending()) {
            //~ this.needsUpdate = true;
        //~ }
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
    //~ private void aetherium$searched(final CallbackInfo ci) {
        //~ VisibilitySearch.searched(System.nanoTime());
    //~ }

    //~ /** {@code needsUpdate = true} for a visible section that is dirty. */
    //~ @WrapOperation(method = "setupRender", at = @At(value = "FIELD",
            //~ target = "Lnet/minecraft/client/renderer/LevelRenderer;needsUpdate:Z", opcode = AETHERIUM$PUTFIELD,
            //~ ordinal = 2), require = 0)
    //~ private void aetherium$holdDirtyTrigger(final LevelRenderer self, final boolean value,
                                            //~ final Operation<Void> original) {
        //~ if (!value || VisibilitySearch.dirtyTrigger()) {
            //~ this.needsUpdate = value;
        //~ }
    //~ }

    //~ @ModifyExpressionValue(method = "compileChunksUntil", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher;uploadAllPendingUploads()Z"), require = 0)
    //~ private boolean aetherium$throttleVisibility(final boolean uploaded) {
        //~ return QolHooks.visibilityUpdateAfterUploads(uploaded);
    //~ }
    // @era:search-throttle-end
}
