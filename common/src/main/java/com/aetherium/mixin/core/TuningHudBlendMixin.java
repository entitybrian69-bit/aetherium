package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:boost16-begin none
// @era:boost16-else v16
//~ import com.aetherium.perf.HudRate;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:boost16-end

/**
 * HUD refresh rate (1.17.2, Minecraft 1.16.5): while the HUD is drawn into its cached picture
 * (a transparent off-screen image), every blend mode keeps its colour factors but gets alpha
 * factors that build the picture's coverage correctly ({@link HudRate#alphaFactorsFor}). Nothing
 * changes outside that capture.
 */
@Mixin(targets = "com.mojang.blaze3d.platform.GlStateManager")
public abstract class TuningHudBlendMixin {
    // @era:boost16-begin none
    // @era:boost16-else v16
    //~ @Shadow
    //~ public static void _blendFuncSeparate(final int srcRgb, final int dstRgb, final int srcAlpha, final int dstAlpha) {
        //~ throw new AssertionError();
    //~ }

    //~ @Inject(method = "_blendFunc", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private static void aetherium$captureBlend(final int srcRgb, final int dstRgb, final CallbackInfo ci) {
        //~ if (HudRate.capturing()) {
            //~ ci.cancel();
            //~ final int alpha = HudRate.alphaFactorsFor(dstRgb);
            //~ _blendFuncSeparate(srcRgb, dstRgb, alpha >>> 16, alpha & 0xFFFF);
        //~ }
    //~ }

    //~ @Inject(method = "_blendFuncSeparate", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private static void aetherium$captureBlendSeparate(final int srcRgb, final int dstRgb, final int srcAlpha,
                                                       //~ final int dstAlpha, final CallbackInfo ci) {
        //~ if (HudRate.capturing()) {
            //~ final int alpha = HudRate.alphaFactorsFor(dstRgb);
            //~ final int wantSrc = alpha >>> 16;
            //~ final int wantDst = alpha & 0xFFFF;
            //~ if (srcAlpha != wantSrc || dstAlpha != wantDst) {
                //~ ci.cancel();
                //~ _blendFuncSeparate(srcRgb, dstRgb, wantSrc, wantDst);
            //~ }
        //~ }
    //~ }
    // @era:boost16-end
}
