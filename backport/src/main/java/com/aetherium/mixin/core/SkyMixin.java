package com.aetherium.mixin.core;

import com.aetherium.qol.Scenery;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sky toggles. In 1.12.2's {@code renderSky} the tessellator draws the sunrise glow (0), the sun
 * (1) and the moon (2); the stars are the second VBO draw / display list.
 */
@Mixin(RenderGlobal.class)
public abstract class SkyMixin {

    @Inject(method = "renderSky(FI)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$sky(final float partialTicks, final int pass, final CallbackInfo ci) {
        if (!Scenery.sky) {
            ci.cancel();
        }
    }

    @Redirect(method = "renderSky(FI)V", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Tessellator;draw()V", ordinal = 1))
    private void aetherium$sun(final Tessellator tessellator) {
        drawOrDiscard(tessellator);
    }

    @Redirect(method = "renderSky(FI)V", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Tessellator;draw()V", ordinal = 2))
    private void aetherium$moon(final Tessellator tessellator) {
        drawOrDiscard(tessellator);
    }

    private static void drawOrDiscard(final Tessellator tessellator) {
        if (Scenery.sunMoon) {
            tessellator.draw();
            return;
        }
        // The quad was built already: finish and drop it so the shared buffer is free again.
        final BufferBuilder buffer = tessellator.getBuffer();
        buffer.finishDrawing();
        buffer.reset();
    }

    @Redirect(method = "renderSky(FI)V", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/vertex/VertexBuffer;drawArrays(I)V", ordinal = 1))
    private void aetherium$starsVbo(final VertexBuffer stars, final int mode) {
        if (Scenery.stars) {
            stars.drawArrays(mode);
        }
    }

    @Redirect(method = "renderSky(FI)V", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;callList(I)V", ordinal = 1))
    private void aetherium$starsList(final int list) {
        if (Scenery.stars) {
            GlStateManager.callList(list);
        }
    }
}
