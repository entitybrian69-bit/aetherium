package com.aetherium.mixin.core;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
// @era:render-scale-begin none
// @era:render-scale-else target
//~ import com.aetherium.client.RenderScale;
//~ import net.minecraft.world.entity.Entity;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
// @era:render-scale-end

/** Render scale holds full resolution while outlined (glowing) entities are drawn. */
@Mixin(Minecraft.class)
public abstract class GlowSeenMixin {

    // @era:render-scale-begin none
    // @era:render-scale-else target
    //~ @Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"))
    //~ private void aetherium$glowSeen(final Entity entity, final CallbackInfoReturnable<Boolean> cir) {
        //~ if (cir.getReturnValueZ()) {
            //~ RenderScale.glowing();
        //~ }
    //~ }
    // @era:render-scale-end
}
