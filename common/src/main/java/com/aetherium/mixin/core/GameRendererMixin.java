package com.aetherium.mixin.core;

import com.aetherium.Aetherium;
import com.aetherium.client.ClientHooks;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Frame boundaries on the render thread: the only place GL work may start or end.
 *
 * <p>Target {@code GameRenderer#renderLevel(float)} exists on every version in the
 * porting range (1.16.5 -&gt; 26.x); the sibling overloads
 * {@code renderLevel(float, boolean, ...)} and {@code renderFinishingRainPass} were
 * verified present in 1.21.1-era mixin sets
 * ({@code CaffeineMC/sodium @ 1.21.1/stable}, {@code core.render.GameRendererMixin}
 * injects into {@code renderLevel}). Both names are listed with
 * {@code require = 0, expect = 0} so one version's extra overload is a no-op rather
 * than a launch failure.</p>
 *
 * <p>Why these two points and not {@code GameRenderer#render}: {@code render()} also
 * covers the screen and the panoroma, where there is no level, no camera and no
 * chunk geometry to cull. Wrapping only the level pass means the HZB pyramid, the
 * indirect batch and the light collection all see exactly one valid camera per
 * frame — which is also what makes the shadow-mode measurement comparable to
 * vanilla.</p>
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = {"renderLevel"}, at = @At("HEAD"))
    private void aetherium$beginFrame(final float tickDelta, final CallbackInfo ci) {
        if (Aetherium.isVanillaPath()) {
            return;
        }
        ClientHooks.beginFrame();
    }

    @Inject(method = {"renderLevel"}, at = @At("TAIL"))
    private void aetherium$endFrame(final float tickDelta, final CallbackInfo ci) {
        if (Aetherium.isVanillaPath()) {
            return;
        }
        ClientHooks.endFrame(framebufferWidth(), framebufferHeight());
    }

    /**
     * 1.21.1 declares {@code renderLevel(float, boolean)} for the fancy/fast cloud
     * split path. Injecting the same pair keeps frame accounting correct if the
     * renderer takes that route instead.
     */
    @Inject(method = {"renderLevel(float, boolean)"}, at = @At("HEAD"), require = 0, expect = 0)
    private void aetherium$beginFrameAlt(final float tickDelta, final boolean renderParticles, final CallbackInfo ci) {
        if (Aetherium.isVanillaPath()) {
            return;
        }
        ClientHooks.beginFrame();
    }

    @Inject(method = {"renderLevel(float, boolean)"}, at = @At("TAIL"), require = 0, expect = 0)
    private void aetherium$endFrameAlt(final float tickDelta, final boolean renderParticles, final CallbackInfo ci) {
        if (Aetherium.isVanillaPath()) {
            return;
        }
        ClientHooks.endFrame(framebufferWidth(), framebufferHeight());
    }

    /**
     * Framebuffer size at the end of the level pass. Read from the window rather
     * than passed in, because {@code GameRenderer#renderLevel} takes no size on any
     * version in the porting range, and the HZB pyramid must be resized against the
     * real drawable dimensions (not the GUI-scaled ones) to avoid a one-frame streak
     * when the player drags the window.
     */
    private static int framebufferWidth() {
        final net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        return minecraft != null && minecraft.getWindow() != null ? minecraft.getWindow().getFramebufferWidth() : 1;
    }

    private static int framebufferHeight() {
        final net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        return minecraft != null && minecraft.getWindow() != null ? minecraft.getWindow().getFramebufferHeight() : 1;
    }
}
