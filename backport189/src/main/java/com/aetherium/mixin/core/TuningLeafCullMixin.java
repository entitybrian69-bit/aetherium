package com.aetherium.mixin.core;

import com.aetherium.client.TuningBackport;
import com.aetherium.perf.Tuning;
import net.minecraft.block.BlockLeavesBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Leaf culling: leaf faces buried inside a tree are not built into the chunk mesh. */
@Mixin(BlockLeavesBase.class)
public abstract class TuningLeafCullMixin {
    @Inject(method = "shouldSideBeRendered", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$cull(final IBlockAccess world, final BlockPos pos,
                                final EnumFacing side, final CallbackInfoReturnable<Boolean> cir) {
        if (Tuning.leafCulling && TuningBackport.leafFaceHidden(world, pos, side)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }
}
