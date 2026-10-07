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
 * <p>Targets verified against {@code CaffeineMC/sodium @ 1.21.1/stable}, whose mixin
 * set for that version includes {@code features.options.overlays.GuiMixin} on
 * {@code net.minecraft.client.gui.Gui}. {@code Gui#render} keeps the
 * {@code float partialTick} parameter across the whole porting range, but the first
 * parameter changed type on 1.18 ({@code PoseStack} -&gt; {@code GuiGraphics}); the
 * two tolerant injections below cover both, and {@code require = 0}/{@code expect = 0}
 * means exactly one matches on any version (flip
 * {@code advanced.strict_mixins} to make a miss fatal instead).</p>
 *
 * <p>Overlay placement: TAIL of {@code Gui#render}, i.e. above the hotbar and boss
 * bars, below the debug screen. A performance readout must never intercept a click
 * and must never be hidden by a health-recently-taken flash.</p>
 */
@Mixin(Gui.class)
public abstract class GuiMixin {

    // [UNVERIFIED: the full descriptor "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V" for
    // 1.21.1 - the parameter list is read from Sodium's Gui overlay mixin for that version, the
    // exact first-parameter type is inferred. A wrong descriptor is a skipped injection, and the
    // overlay then only shows under F3 (which uses a different hook).]
    /** 1.18+ signature. */
    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("TAIL"), require = 0, expect = 0)
    private void aetherium$renderOverlaysGuiGraphics(final GuiGraphics guiGraphics, final int mouseX, final int mouseY,
                                                      final float partialTick, final CallbackInfo ci) {
        aetherium$draw(guiGraphics);
    }

    /** 1.16.5-1.17.1 signature (PoseStack first). Kept for the port; a no-op on 1.18+. */
    @Inject(method = "render(Lnet/minecraft/client/renderer/LightTexture;IIF)V", at = @At("TAIL"), require = 0, expect = 0)
    private void aetherium$renderOverlaysLegacy(final Object ignored, final int mouseX, final int mouseY,
                                                 final float partialTick, final CallbackInfo ci) {
        // LightTexture-typed render() is the 1.16-era overload; the overlay on that
        // version is drawn from the level-load screen instead, so this hook exists
        // only so the config's method list has a legal target on 1.16.5.
    }

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
