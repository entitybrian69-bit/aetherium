package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.aetherium.client.QolHooks;
//~ import net.minecraft.client.Camera;
//~ import net.minecraft.client.renderer.FogRenderer;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:qol16-end

/**
 * Fog controls: after vanilla sets up terrain fog, applies distance fog off / fog start / water
 * fog density / clear lava. Sky fog and blindness are left alone. Applied only where
 * {@code Capabilities.SCENERY_CONTROLS} is true.
 */
@Mixin(targets = "net.minecraft.client.renderer.FogRenderer")
public abstract class FogControlMixin {
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ @Inject(method = "setupFog", at = @At("TAIL"), require = 0)
    //~ private static void aetherium$fogControls(final Camera camera, final FogRenderer.FogMode mode, final float farPlane,
                                              //~ final boolean thick, final CallbackInfo ci) {
        //~ if (mode == FogRenderer.FogMode.FOG_TERRAIN) {
            //~ QolHooks.adjustFog(camera, farPlane, thick);
        //~ }
    //~ }
    // @era:qol16-end
}
