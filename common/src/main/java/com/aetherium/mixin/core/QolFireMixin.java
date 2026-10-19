package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:q-fire-begin none
// @era:q-fire-else v16
//~ import com.aetherium.qol.Qol;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import net.minecraft.client.Minecraft;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:q-fire-end

/**
 * Fire overlay height (Quality of life, Minecraft 1.16.5): while burning, the two flame sheets in
 * front of the camera are lowered so less of the screen is covered (100% = vanilla, 0% = not
 * drawn). The burning itself, its sound and damage are unchanged.
 */
@Mixin(targets = "net.minecraft.client.renderer.ScreenEffectRenderer")
public abstract class QolFireMixin {
    // @era:q-fire-begin none
    // @era:q-fire-else v16
    //~ /** How far the flames sink at 0% (their sheet is one unit tall, centred on the view). */
    //~ private static final double DROP = 0.75;
    //~ private static boolean aetherium$pushed;

    //~ @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private static void aetherium$fireStart(final Minecraft mc, final PoseStack pose, final CallbackInfo ci) {
        //~ final int height = Qol.fireOverlay;
        //~ if (height >= 100) {
            //~ return;
        //~ }
        //~ if (height <= 0) {
            //~ ci.cancel();
            //~ return;
        //~ }
        //~ pose.pushPose();
        //~ pose.translate(0.0, -DROP * (1.0 - height / 100.0), 0.0);
        //~ aetherium$pushed = true;
    //~ }

    //~ @Inject(method = "renderFire", at = @At("RETURN"), require = 0)
    //~ private static void aetherium$fireEnd(final Minecraft mc, final PoseStack pose, final CallbackInfo ci) {
        //~ if (aetherium$pushed) {
            //~ aetherium$pushed = false;
            //~ pose.popPose();
        //~ }
    //~ }
    // @era:q-fire-end
}
