package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:q-fire-begin v16|f17|f214|f219|f262
import com.aetherium.qol.Qol;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:q-fire-else none
// @era:q-fire-end

/**
 * Fire overlay height (Quality of life, every version): while burning, the two flame sheets in
 * front of the camera are lowered so less of the screen is covered (100% = vanilla, 0% = not
 * drawn). The burning itself, its sound and damage are unchanged. The method's parameters change
 * in 1.21.4 and 1.21.9 and it became submitFire in 26.2 (whose geometry is submitted with the pose
 * as it is at that moment, so the same translation applies).
 */
@Mixin(targets = "net.minecraft.client.renderer.ScreenEffectRenderer")
public abstract class QolFireMixin {
    // @era:q-fire-begin v16|f17|f214|f219|f262
    /** How far the flames sink at 0% (their sheet is one unit tall, centred on the view). */
    private static final double DROP = 0.75;
    private static boolean aetherium$pushed;

    private static void aetherium$start(final PoseStack pose, final CallbackInfo ci) {
        final int height = Qol.fireOverlay;
        if (height >= 100) {
            return;
        }
        if (height <= 0) {
            ci.cancel();
            return;
        }
        pose.pushPose();
        pose.translate(0.0, -DROP * (1.0 - height / 100.0), 0.0);
        aetherium$pushed = true;
    }

    private static void aetherium$end(final PoseStack pose) {
        if (aetherium$pushed) {
            aetherium$pushed = false;
            pose.popPose();
        }
    }
    // @era:q-fire-else none
    // @era:q-fire-end

    // @era:q-fire-begin v16|f17
    @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true, require = 0)
    private static void aetherium$fireStart(final net.minecraft.client.Minecraft mc, final PoseStack pose, final CallbackInfo ci) {
        aetherium$start(pose, ci);
    }

    @Inject(method = "renderFire", at = @At("RETURN"), require = 0)
    private static void aetherium$fireEnd(final net.minecraft.client.Minecraft mc, final PoseStack pose, final CallbackInfo ci) {
        aetherium$end(pose);
    }
    // @era:q-fire-else f214
    //~ @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private static void aetherium$fireStart(final PoseStack pose, final net.minecraft.client.renderer.MultiBufferSource buffers, final CallbackInfo ci) {
        //~ aetherium$start(pose, ci);
    //~ }

    //~ @Inject(method = "renderFire", at = @At("RETURN"), require = 0)
    //~ private static void aetherium$fireEnd(final PoseStack pose, final net.minecraft.client.renderer.MultiBufferSource buffers, final CallbackInfo ci) {
        //~ aetherium$end(pose);
    //~ }
    // @era:q-fire-else f219
    //~ @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private static void aetherium$fireStart(final PoseStack pose, final net.minecraft.client.renderer.MultiBufferSource buffers, final net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, final CallbackInfo ci) {
        //~ aetherium$start(pose, ci);
    //~ }

    //~ @Inject(method = "renderFire", at = @At("RETURN"), require = 0)
    //~ private static void aetherium$fireEnd(final PoseStack pose, final net.minecraft.client.renderer.MultiBufferSource buffers, final net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, final CallbackInfo ci) {
        //~ aetherium$end(pose);
    //~ }
    // @era:q-fire-else f262
    //~ @Inject(method = "submitFire", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private static void aetherium$fireStart(final PoseStack pose, final net.minecraft.client.renderer.SubmitNodeCollector collector, final net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, final CallbackInfo ci) {
        //~ aetherium$start(pose, ci);
    //~ }

    //~ @Inject(method = "submitFire", at = @At("RETURN"), require = 0)
    //~ private static void aetherium$fireEnd(final PoseStack pose, final net.minecraft.client.renderer.SubmitNodeCollector collector, final net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, final CallbackInfo ci) {
        //~ aetherium$end(pose);
    //~ }
    // @era:q-fire-else none
    // @era:q-fire-end
}
