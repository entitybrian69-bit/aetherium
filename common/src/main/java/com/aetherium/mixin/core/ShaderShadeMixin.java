package com.aetherium.mixin.core;

// @era:shader-begin none
// @era:shader-else v16
//~ import com.aetherium.shader.ShaderRuntime;
//~ import net.minecraft.core.Direction;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
// @era:shader-end
import net.minecraft.client.multiplayer.ClientLevel;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Shader pack engine (1.16.5): OptiFine's {@code oldLighting=false} (the default for packs)
 * removes vanilla's per-face shading so packs light blocks themselves. Per meshed face while a
 * pack is loaded; a field read otherwise.
 */
@Mixin(ClientLevel.class)
public abstract class ShaderShadeMixin {

    // @era:shader-begin none
    // @era:shader-else v16
    //~ @Inject(method = "getShade", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$flatShade(final Direction direction, final boolean shade,
                                     //~ final CallbackInfoReturnable<Float> cir) {
        //~ if (ShaderRuntime.flatShading()) {
            //~ cir.setReturnValue(1.0F);
        //~ }
    //~ }
    // @era:shader-end
}
