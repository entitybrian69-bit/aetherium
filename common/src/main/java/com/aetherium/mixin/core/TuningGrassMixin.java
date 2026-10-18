package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-grass-begin none
// @era:t-grass-else v16
//~ import com.aetherium.client.GrassQuads;
//~ import java.util.List;
//~ import java.util.Random;
//~ import net.minecraft.client.renderer.block.model.BakedQuad;
//~ import net.minecraft.client.resources.model.BakedModel;
//~ import net.minecraft.core.Direction;
//~ import net.minecraft.world.level.block.state.BlockState;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:t-grass-end

/** Fast grass (1.12.0, Minecraft 1.16.5): see {@link com.aetherium.client.GrassQuads}. */
@Mixin(targets = "net.minecraft.client.renderer.block.ModelBlockRenderer")
public abstract class TuningGrassMixin {
    // @era:t-grass-begin none
    // @era:t-grass-else v16
    //~ @Redirect(method = {"tesselateWithAO", "tesselateWithoutAO"}, at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/resources/model/BakedModel;getQuads(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Ljava/util/Random;)Ljava/util/List;"),
            //~ require = 0)
    //~ private List<BakedQuad> aetherium$quads(final BakedModel model, final BlockState state, final Direction side,
            //~ final Random random) {
        //~ return GrassQuads.filter(model.getQuads(state, side, random), state, side);
    //~ }
    // @era:t-grass-end
}
