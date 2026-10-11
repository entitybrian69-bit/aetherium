package com.aetherium.mixin.core;

// @era:chunk-renderer-begin none
// @era:chunk-renderer-else gl16
//~ import com.aetherium.client.FogTracker;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:chunk-renderer-end
import org.spongepowered.asm.mixin.Mixin;

/**
 * Records the fog state vanilla 1.16.5 sets through {@code GlStateManager} for the Aetherium
 * pipeline's shader, so it never queries fixed-function fog from GL (Android GL translators may
 * not implement those queries). A handful of calls per frame, each a field store.
 *
 * <p>Applied only where {@code Capabilities.EXPERIMENTAL_CHUNK_RENDERER} is true; targeted by name
 * because the fog methods are gone from 1.17 on.</p>
 */
@Mixin(targets = "com.mojang.blaze3d.platform.GlStateManager")
public abstract class FogStateMixin {

    // @era:chunk-renderer-begin none
    // @era:chunk-renderer-else gl16
    //~ @Inject(method = "_enableFog", at = @At("HEAD"), require = 0)
    //~ private static void aetherium$enableFog(final CallbackInfo ci) {
        //~ FogTracker.setEnabled(true);
    //~ }

    //~ @Inject(method = "_disableFog", at = @At("HEAD"), require = 0)
    //~ private static void aetherium$disableFog(final CallbackInfo ci) {
        //~ FogTracker.setEnabled(false);
    //~ }

    //~ @Inject(method = "_fogMode", at = @At("HEAD"), require = 0)
    //~ private static void aetherium$fogMode(final int mode, final CallbackInfo ci) {
        //~ FogTracker.setMode(mode);
    //~ }

    //~ @Inject(method = "_fogStart", at = @At("HEAD"), require = 0)
    //~ private static void aetherium$fogStart(final float value, final CallbackInfo ci) {
        //~ FogTracker.setStart(value);
    //~ }

    //~ @Inject(method = "_fogEnd", at = @At("HEAD"), require = 0)
    //~ private static void aetherium$fogEnd(final float value, final CallbackInfo ci) {
        //~ FogTracker.setEnd(value);
    //~ }

    //~ @Inject(method = "_fogDensity", at = @At("HEAD"), require = 0)
    //~ private static void aetherium$fogDensity(final float value, final CallbackInfo ci) {
        //~ FogTracker.setDensity(value);
    //~ }

    //~ @Inject(method = "_fog", at = @At("HEAD"), require = 0)
    //~ private static void aetherium$fog(final int pname, final float[] params, final CallbackInfo ci) {
        //~ FogTracker.setParameter(pname, params);
    //~ }
    // @era:chunk-renderer-end
}
