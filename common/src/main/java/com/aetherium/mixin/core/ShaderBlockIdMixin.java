package com.aetherium.mixin.core;

// @era:shader-begin none
// @era:shader-else v16
//~ import com.aetherium.shader.ShaderVertex;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import com.mojang.blaze3d.vertex.VertexConsumer;
//~ import java.util.Random;
//~ import net.minecraft.core.BlockPos;
//~ import net.minecraft.world.level.BlockAndTintGetter;
//~ import net.minecraft.world.level.block.state.BlockState;
//~ import net.minecraft.world.level.material.FluidState;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//~ import net.minecraft.client.renderer.block.BlockRenderDispatcher;
// @era:shader-end
import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Shader pack engine (1.16.5): the block.properties id of the block being meshed, for mc_Entity.
 * One volatile read per block while no pack extends the format.
 */
// @era:shader-begin none
@Mixin(Minecraft.class)
// @era:shader-else v16
//~ @Mixin(BlockRenderDispatcher.class)
// @era:shader-end
public abstract class ShaderBlockIdMixin {

    // @era:shader-begin none
    // @era:shader-else v16
    //~ @Inject(method = "renderBatched", at = @At("HEAD"), require = 0)
    //~ private void aetherium$blockId(final BlockState state, final BlockPos pos, final BlockAndTintGetter level,
                                   //~ final PoseStack poseStack, final VertexConsumer consumer, final boolean cull,
                                   //~ final Random random, final CallbackInfoReturnable<Boolean> cir) {
        //~ ShaderVertex.setBlock(state);
    //~ }

    //~ @Inject(method = "renderLiquid", at = @At("HEAD"), require = 0)
    //~ private void aetherium$fluidId(final BlockPos pos, final BlockAndTintGetter level, final VertexConsumer consumer,
                                   //~ final FluidState fluid, final CallbackInfoReturnable<Boolean> cir) {
        //~ if (ShaderVertex.extended()) {
            //~ ShaderVertex.setBlock(fluid.createLegacyBlock());
        //~ }
    //~ }
    // @era:shader-end
}
