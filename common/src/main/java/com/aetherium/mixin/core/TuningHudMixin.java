package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:boost16-begin none
// @era:boost16-else v16
//~ import com.aetherium.perf.HudRate;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:boost16-end

/**
 * HUD refresh rate (1.17.2, Minecraft 1.16.5): while the HUD is drawn into its cached picture, the
 * vignette (a darkening multiplied into the world) and the crosshair (colour-inverting) are left
 * out; {@link TuningHudFrameMixin} draws both live every frame.
 */
@Mixin(targets = "net.minecraft.client.gui.Gui")
public abstract class TuningHudMixin {
    // @era:boost16-begin none
    // @era:boost16-else v16
    //~ @Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$vignetteLive(final CallbackInfo ci) {
        //~ if (HudRate.capturing()) {
            //~ ci.cancel();
        //~ }
    //~ }

    //~ @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$crosshairLive(final CallbackInfo ci) {
        //~ if (HudRate.capturing()) {
            //~ ci.cancel();
        //~ }
    //~ }
    // @era:boost16-end
}
