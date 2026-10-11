package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.aetherium.client.QolHooks;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//~ import net.minecraft.client.player.LocalPlayer;
//~ import org.spongepowered.asm.mixin.injection.At;
// @era:qol16-end

/**
 * Freelook: while active, mouse movement turns the free camera instead of the player. Applied
 * only where {@code Capabilities.FREELOOK} is true.
 */
@Mixin(targets = "net.minecraft.client.MouseHandler")
public abstract class FreelookMouseMixin {
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ @WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"), require = 0)
    //~ private void aetherium$freelookTurn(final LocalPlayer player, final double dx, final double dy,
                                        //~ final Operation<Void> original) {
        //~ if (!QolHooks.freelookTurn(dx, dy)) {
            //~ original.call(player, dx, dy);
        //~ }
    //~ }
    // @era:qol16-end
}
