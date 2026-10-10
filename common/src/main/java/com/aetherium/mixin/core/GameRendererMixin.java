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
 * <p>Target {@code GameRenderer#renderLevel} exists on every version in the porting
 * range (1.16.5 -&gt; 26.x), but its parameter list moved three times, and each move
 * was read from a real upstream mixin of that era:</p>
 * <ul>
 *   <li>1.21.1: {@code renderLevel(DeltaTracker, boolean, Camera, ...)} —
 *       IrisShaders/Iris @ 1.21.1, {@code MixinGameRenderer#iris$runColorSpace(DeltaTracker, CallbackInfo)}.</li>
 *   <li>1.20.6: {@code renderLevel(float, long, ...)} —
 *       Iris @ 1.20.6, same mixin, {@code (float f, long l, CallbackInfo)}.</li>
 *   <li>1.19.4: {@code renderLevel(float, long, PoseStack, ...)} —
 *       Iris @ 1.19.4, same mixin, {@code (float, long, PoseStack, CallbackInfo)}.</li>
 * </ul>
 *
 * <p>The handlers therefore capture <b>no</b> target arguments: Mixin binds an
 * argument-less handler to any overload, which is the only shape that is legal in
 * all three eras at once (a handler declaring {@code float tickDelta} applied to the
 * 1.21.1 {@code DeltaTracker} form is an apply-time failure — exactly the bug the
 * first draft of this file shipped). The bare method name matches every overload.</p>
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
    private void aetherium$beginFrame(final CallbackInfo ci) {
        if (Aetherium.isVanillaPath()) {
            return;
        }
        ClientHooks.beginFrame();
    }

    @Inject(method = {"renderLevel"}, at = @At("TAIL"))
    private void aetherium$endFrame(final CallbackInfo ci) {
        if (Aetherium.isVanillaPath()) {
            return;
        }
        ClientHooks.endFrame(framebufferWidth(), framebufferHeight());
    }

    // There is deliberately no second pair of injections for an "alternate renderLevel overload".
    // One was written here with `method = {"renderLevel(float, boolean)"}` and the Mixin annotation
    // processor rejected it on 2026-10-06: a target descriptor must be JVM form (`name(DF)J`), not
    // prose. The claim it rested on - that 1.21.1 has a `renderLevel(float, boolean)` for a
    // fancy/fast cloud split - was invented from the name of a cloud setting, and even with a legal
    // descriptor the handler would have failed at apply time, because a handler's arguments must
    // match the target it chose. `method = {"renderLevel"}` above already matches every overload.

    /**
     * Framebuffer size at the end of the level pass. Read from the window rather
     * than passed in, because {@code GameRenderer#renderLevel} takes no size on any
     * version in the porting range, and the HZB pyramid must be resized against the
     * real drawable dimensions (not the GUI-scaled ones) to avoid a one-frame streak
     * when the player drags the window.
     *
     * <p>{@code Window}'s framebuffer accessors on 1.21.1 are {@code getWidth()}/{@code getHeight()};
     * there is no {@code getFramebufferWidth()} (javac: cannot find symbol). The GUI-scaled pair next
     * to them is what screen code uses, and it is the one this mod's other mixin already calls.</p>
     */
    private static int framebufferWidth() {
        final net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        return minecraft != null && minecraft.getWindow() != null ? minecraft.getWindow().getWidth() : 1;
    }

    private static int framebufferHeight() {
        final net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        return minecraft != null && minecraft.getWindow() != null ? minecraft.getWindow().getHeight() : 1;
    }
}
