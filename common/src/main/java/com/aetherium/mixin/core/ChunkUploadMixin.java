package com.aetherium.mixin.core;

import com.aetherium.perf.UploadBudget;

import java.util.Queue;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
// @era:chunk-upload-begin srd
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:chunk-upload-else crd
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:chunk-upload-else crdbool
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
// @era:chunk-upload-else none
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:chunk-upload-end

/**
 * Smooth chunk loading: replaces vanilla's "upload every finished mesh this frame" loop with
 * {@link UploadBudget#drain}, which keeps a guaranteed minimum per frame and spreads the rest of a
 * burst over the following frames. The queue is vanilla's own {@code toUpload}; nothing is dropped
 * or reordered. 26.x moved uploads into the GPU device layer and has no queue to pace.
 */
// @era:chunk-upload-begin srd
@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher")
// @era:chunk-upload-else crd
//~ @Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher")
// @era:chunk-upload-else crdbool
//~ @Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher")
// @era:chunk-upload-else none
//~ @Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher")
// @era:chunk-upload-end
public abstract class ChunkUploadMixin {

    @Shadow
    @Final
    private Queue<Runnable> toUpload;

    // @era:chunk-upload-begin srd
    @Inject(method = "uploadAllPendingUploads", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$pacedUploads(final CallbackInfo ci) {
        if (UploadBudget.enabled()) {
            UploadBudget.drain(this.toUpload);
            ci.cancel();
        }
    }
    // @era:chunk-upload-else crd
    //~ @Inject(method = "uploadAllPendingUploads", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$pacedUploads(final CallbackInfo ci) {
        //~ if (UploadBudget.enabled()) {
            //~ UploadBudget.drain(this.toUpload);
            //~ ci.cancel();
        //~ }
    //~ }
    // @era:chunk-upload-else crdbool
    //~ @Inject(method = "uploadAllPendingUploads", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$pacedUploads(final CallbackInfoReturnable<Boolean> cir) {
        //~ if (UploadBudget.enabled()) {
            //~ cir.setReturnValue(Boolean.valueOf(UploadBudget.drain(this.toUpload) > 0));
        //~ }
    //~ }
    // @era:chunk-upload-else none
    //~ @Inject(method = "uploadAllPendingUploads", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$pacedUploads(final CallbackInfo ci) {
    //~ }
    // @era:chunk-upload-end
}
