package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:cull-new-begin old|csm6|uber26|uber262|pearl
// @era:cull-new-else srd5
//~ import com.aetherium.client.SectionCullLayoutAccess;
//~ import com.aetherium.render.CompactTerrain;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//~ import com.mojang.blaze3d.vertex.MeshData;
//~ import java.util.concurrent.CompletableFuture;
//~ import java.util.concurrent.Executor;
//~ import net.minecraft.client.renderer.RenderType;
//~ import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
// @era:cull-new-end

/**
 * The Aetherium renderer on 1.21.5: a section layer's upload is queued for the render thread; the
 * face groups of its mesh are handed to the layer's GPU buffers in the same task, right after the
 * vertices are written, so a draw never pairs new vertices with old groups. Applied only where
 * {@code Capabilities.SECTION_CULL_SRD} is true.
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection")
public abstract class SectionCullUploadMixin {
    // @era:cull-new-begin old|csm6|uber26|uber262|pearl
    // @era:cull-new-else srd5
    //~ /** The layer and layout of the upload being queued on this (builder) thread. */
    //~ @Unique
    //~ private static final ThreadLocal<Object[]> AETHERIUM_PENDING = new ThreadLocal<>();

    //~ @Inject(method = "uploadSectionLayer", at = @At("HEAD"), require = 0)
    //~ private void aetherium$noteUpload(final RenderType type, final MeshData mesh,
                                      //~ final CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        //~ final Object raw = mesh;
        //~ final CompactTerrain.Layout layout = raw instanceof SectionCullLayoutAccess
                //~ ? ((SectionCullLayoutAccess) raw).aetheriumCullLayout() : null;
        //~ AETHERIUM_PENDING.set(new Object[] {type, layout});
    //~ }

    //~ @WrapOperation(method = "uploadSectionLayer", at = @At(value = "INVOKE",
            //~ target = "Ljava/util/concurrent/CompletableFuture;runAsync(Ljava/lang/Runnable;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"),
            //~ require = 0)
    //~ private CompletableFuture<Void> aetherium$attachLayout(final Runnable task, final Executor executor,
                                                           //~ final Operation<CompletableFuture<Void>> original) {
        //~ final Object[] pending = AETHERIUM_PENDING.get();
        //~ AETHERIUM_PENDING.remove();
        //~ if (pending == null || !(pending[0] instanceof RenderType)) {
            //~ return original.call(task, executor);
        //~ }
        //~ final RenderType type = (RenderType) pending[0];
        //~ final CompactTerrain.Layout layout = (CompactTerrain.Layout) pending[1];
        //~ final SectionRenderDispatcher.RenderSection self = (SectionRenderDispatcher.RenderSection) (Object) this;
        //~ final Runnable withLayout = () -> {
            //~ task.run();
            //~ final Object buffers = self.getBuffers(type);
            //~ if (buffers instanceof SectionCullLayoutAccess) {
                //~ ((SectionCullLayoutAccess) buffers).aetheriumSetCullLayout(layout);
            //~ }
        //~ };
        //~ return original.call(withLayout, executor);
    //~ }
    // @era:cull-new-end
}
