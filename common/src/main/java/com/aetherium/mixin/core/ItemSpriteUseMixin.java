package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:anim-begin v16|on
import com.aetherium.perf.SpriteActivity;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
// @era:anim-else none
// @era:anim-end

/** Visible-only animations: item quads drawn this frame use their sprites (renderQuadList is static from 1.21.4). */
@Mixin(targets = "net.minecraft.client.renderer.entity.ItemRenderer")
public abstract class ItemSpriteUseMixin {
    // @era:anim-begin v16|on
    @ModifyVariable(method = "renderQuadList", at = @At("HEAD"), argsOnly = true, require = 0)
    // @era:item-quads-begin instance
    private List<BakedQuad> aetherium$itemSprites(final List<BakedQuad> quads) {
    // @era:item-quads-else static
    //~ private static List<BakedQuad> aetherium$itemSprites(final List<BakedQuad> quads) {
    // @era:item-quads-end
        aetherium$touchAll(quads);
        return quads;
    }

    @Unique
    private static void aetherium$touchAll(final List<BakedQuad> quads) {
        for (int i = 0, n = quads.size(); i < n; i++) {
            final Object quad = quads.get(i);
            if (!(quad instanceof BakedQuadAccessor)) {
                SpriteActivity.markBroken();
                return;
            }
            SpriteActivity.touch(((BakedQuadAccessor) quad).aetherium$sprite());
        }
    }
    // @era:anim-else none
    // @era:anim-end
}
