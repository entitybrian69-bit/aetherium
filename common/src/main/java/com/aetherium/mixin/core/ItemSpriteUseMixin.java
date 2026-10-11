package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.aetherium.perf.SpriteActivity;
//~ import java.util.List;
//~ import net.minecraft.client.renderer.block.model.BakedQuad;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.ModifyVariable;
// @era:qol16-end

/**
 * Items drawn anywhere (hand, GUI, item frames, dropped items) mark the sprites of their quads as
 * seen (1.16.5). Applied only where {@code Capabilities.STREAMING} is true.
 */
@Mixin(targets = "net.minecraft.client.renderer.entity.ItemRenderer")
public abstract class ItemSpriteUseMixin {
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ @ModifyVariable(method = "renderQuadList", at = @At("HEAD"), argsOnly = true, require = 0)
    //~ private List<BakedQuad> aetherium$itemSprites(final List<BakedQuad> quads) {
        //~ for (int i = 0, n = quads.size(); i < n; i++) {
            //~ final Object quad = quads.get(i);
            //~ if (!(quad instanceof BakedQuadAccessor)) {
                //~ SpriteActivity.markBroken();
                //~ return quads;
            //~ }
            //~ SpriteActivity.touch(((BakedQuadAccessor) quad).aetherium$sprite());
        //~ }
        //~ return quads;
    //~ }
    // @era:qol16-end
}
