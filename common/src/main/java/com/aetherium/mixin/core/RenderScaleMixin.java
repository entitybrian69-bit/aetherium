package com.aetherium.mixin.core;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
// @era:render-scale-begin r17|r212|r215
import com.aetherium.client.ScaledRenderTarget;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:render-scale-else none
// @era:render-scale-else target
//~ import com.aetherium.client.ScaledRenderTarget;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import net.minecraft.client.Minecraft;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:render-scale-end

/**
 * Render scale (1.9.0; 1.17 to 26.1 since 1.14.0): the world pass of {@code GameRenderer.render} draws into a smaller
 * framebuffer (ScaledRenderTarget); everything after it (entity outlines, post effects, GUI)
 * stays at window resolution.
 */
@Mixin(GameRenderer.class)
public abstract class RenderScaleMixin {

    // @era:render-scale-begin r17|r212|r215
    // renderLevel's parameters differ per version (PoseStack, DeltaTracker, none on 26.3), so these
    // handlers take only the CallbackInfo.
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void aetherium$scaleBegin(final CallbackInfo ci) {
        ScaledRenderTarget.begin(Minecraft.getInstance());
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void aetherium$scaleEnd(final CallbackInfo ci) {
        ScaledRenderTarget.end(Minecraft.getInstance());
    }
    // @era:render-scale-else none
    // @era:render-scale-else target
    //~ @Inject(method = "renderLevel", at = @At("HEAD"))
    //~ private void aetherium$scaleBegin(final float partialTick, final long finishNanos, final PoseStack pose,
                                      //~ final CallbackInfo ci) {
        //~ ScaledRenderTarget.begin(Minecraft.getInstance());
    //~ }

    //~ @Inject(method = "renderLevel", at = @At("RETURN"))
    //~ private void aetherium$scaleEnd(final float partialTick, final long finishNanos, final PoseStack pose,
                                    //~ final CallbackInfo ci) {
        //~ ScaledRenderTarget.end(Minecraft.getInstance());
    //~ }
    // @era:render-scale-end
}
