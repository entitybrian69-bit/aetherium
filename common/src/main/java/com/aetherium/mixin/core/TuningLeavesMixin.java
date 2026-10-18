package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-leaves-begin v16|l2111
import com.aetherium.perf.Tuning;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
// @era:t-leaves-else none
// @era:t-leaves-end

/**
 * Leaves mode (1.10.0, Minecraft 1.16.5): {@code ItemBlockRenderTypes.setFancy} decides whether
 * leaves use the cutout (see-through) or solid layer; vanilla passes the Graphics setting. The
 * leaves option overrides that one argument, whoever calls it.
 */
@Mixin(targets = "net.minecraft.client.renderer.ItemBlockRenderTypes")
public abstract class TuningLeavesMixin {
    // @era:t-leaves-begin v16
    @ModifyVariable(method = "setFancy", at = @At("HEAD"), argsOnly = true, require = 0)
    private static boolean aetherium$leaves(final boolean fancy) {
        final int mode = Tuning.leaves;
        final boolean result = mode == 1 ? false : mode == 2 ? true : fancy;
        Tuning.leavesFancy = result;
        return result;
    }
    // @era:t-leaves-else none
    // @era:t-leaves-else l2111
    //~ @ModifyVariable(method = "setCutoutLeaves", at = @At("HEAD"), argsOnly = true, require = 0)
    //~ private static boolean aetherium$leaves(final boolean fancy) {
        //~ final int mode = Tuning.leaves;
        //~ final boolean result = mode == 1 ? false : mode == 2 ? true : fancy;
        //~ Tuning.leavesFancy = result;
        //~ return result;
    //~ }
    // @era:t-leaves-end
}
