package com.aetherium.mixin.core;

import java.util.List;

import com.aetherium.Aetherium;
import com.aetherium.hud.AetheriumHudRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Draws Aetherium's overlays (FPS corner, frame graph, backend tag) and appends a
 * line to the F3 text.
 *
 * <p>Target and handler shapes, read from real upstream mixins for each era:</p>
 * <ul>
 *   <li>1.21.1: {@code Gui#render(GuiGraphics, DeltaTracker)} —
 *       IrisShaders/Iris @ 1.21.1, {@code MixinGui#iris$handleHudHidingScreens(GuiGraphics, DeltaTracker, ...)}.</li>
 *   <li>1.20.6: {@code Gui#render(GuiGraphics, float)} —
 *       Iris @ 1.20.6, same mixin, {@code (GuiGraphics, float, CallbackInfo)}.</li>
 *   <li>1.19.4: {@code Gui#render(PoseStack, float, ...)} —
 *       Iris @ 1.19.4, same mixin, {@code (PoseStack, float, CallbackInfo)}.</li>
 * </ul>
 *
 * <p>Every era has exactly one method named {@code render} on {@code Gui}, so the
 * injection uses the bare name and captures only the <em>prefix</em> it needs (the
 * first parameter); Mixin binds a handler that declares a prefix of the target's
 * arguments, so one handler is legal across every overload shape above. Pre-1.20.5
 * rows rewrite {@code GuiGraphics} to {@code PoseStack}/{@code MatrixStack} in this
 * handler and in the HUD (see {@code tools/gen_deltas.py}); a wrong era fact is a
 * skipped injection ({@code require = 0}) and the overlay then only shows under F3.</p>
 *
 * <p>Overlay placement: TAIL of {@code Gui#render}, i.e. above the hotbar and boss
 * bars, below the debug screen. A performance readout must never intercept a click
 * and must never be hidden by a health-recently-taken flash.</p>
 */
@Mixin(Gui.class)
public abstract class GuiMixin {

    @Inject(method = "render", at = @At("TAIL"), require = 0, expect = 0)
    private void aetherium$renderOverlays(final GuiGraphics guiGraphics, final CallbackInfo ci) {
        aetherium$draw(guiGraphics);
    }

    // There is deliberately no second, legacy-descriptor injection here. An earlier
    // revision carried `render(Lnet/minecraft/client/renderer/LightTexture;IIF)V` "for
    // 1.16.5" - a descriptor no Minecraft version ever declared on Gui, invented from
    // the LightTexture class sitting near Gui's render path. The 1.16.5 port rewrites
    // the single injection above to the MatrixStack era (tools/gen_deltas.py, stack_class
    // fact); keeping a fake second injection only added a guaranteed no-op and a
    // misleading comment to every other row.

    private void aetherium$draw(final GuiGraphics guiGraphics) {
        final Aetherium.State state = Aetherium.getState();
        if (state != Aetherium.State.ACTIVE && state != Aetherium.State.SHADOW) {
            return;
        }
        final net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        if (isDebugScreenOpen(minecraft)) {
            AetheriumHudRenderer.drawBackendTag(guiGraphics);
            return;
        }
        AetheriumHudRenderer.render(guiGraphics);
    }

    /**
     * {@code DebugScreenOverlay#showDebugScreen()} is public on 1.19+; on earlier
     * versions the only signal is {@code Gui#debugOverlay}, so this small reflective
     * probe keeps one call site valid across the range instead of a version branch.
     */
    private static boolean isDebugScreenOpen(final net.minecraft.client.Minecraft minecraft) {
        try {
            final Object overlay = minecraft.getClass().getMethod("getDebugOverlay").invoke(minecraft);
            if (overlay == null) {
                return false;
            }
            final Object visible = overlay.getClass().getMethod("showDebugScreen").invoke(overlay);
            return Boolean.TRUE.equals(visible);
        } catch (final ReflectiveOperationException | RuntimeException | LinkageError error) {
            // Assume "not open": the worst case is the overlay drawing behind F3,
            // which is legible, whereas assuming "open" hides the FPS counter.
            return false;
        }
    }

    /**
     * Appends the Aetherium line to F3. {@code DebugScreenOverlay#getGameLines()} is
     * 1.19.2+ and {@code getDebugLines()} is 1.16.5-1.19.1; both return
     * {@code List<String>}, so one injection with two names covers the range.
     */
    // [UNVERIFIED: DebugScreenOverlay#getGameLines returning List<String> on 1.21.1 (the name is
    // from Sodium's core.debug overlay mixin); getDebugLines covers 1.16.5-1.19.1.]
    @Inject(method = {"getGameLines", "getDebugLines"}, at = @At("TAIL"), require = 0, expect = 0)
    private void aetherium$appendDebugLine(final CallbackInfoReturnable<List<String>> ci) {
        if (Aetherium.subsystemsOrNull() == null || !Aetherium.config().showBackendTag.get()) {
            return;
        }
        final List<String> lines = ci.getReturnValue();
        if (lines != null) {
            lines.add(Aetherium.describeRuntime());
            final com.aetherium.render.gl.GlDevice device = com.aetherium.client.ClientHooks.device();
            if (device != null) {
                lines.add(device.describe());
            }
        }
    }
}
