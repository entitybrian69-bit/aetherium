package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.aetherium.perf.SpriteActivity;
//~ import java.util.List;
//~ import net.minecraft.client.renderer.block.model.BakedQuad;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.ModifyVariable;
// @era:qol16-end

/**
 * Records the sprites of the block faces {@code ModelBlockRenderer} draws (1.16.5): per face list
 * while a chunk section is meshed (kept with that section), and for block models drawn directly
 * (falling blocks, minecart and enderman blocks). A field read and a type check per quad; only
 * animated block-atlas sprites go further. Applied only where {@code Capabilities.STREAMING} is true.
 */
@Mixin(targets = "net.minecraft.client.renderer.block.ModelBlockRenderer")
public abstract class BlockSpriteUseMixin {
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ @ModifyVariable(method = {"renderModelFaceAO", "renderModelFaceFlat"}, at = @At("HEAD"), argsOnly = true,
            //~ require = 0)
    //~ private List<BakedQuad> aetherium$faceSprites(final List<BakedQuad> quads) {
        //~ aetherium$touchAll(quads);
        //~ return quads;
    //~ }

    //~ @ModifyVariable(method = "renderQuadList", at = @At("HEAD"), argsOnly = true, require = 0)
    //~ private static List<BakedQuad> aetherium$listSprites(final List<BakedQuad> quads) {
        //~ aetherium$touchAll(quads);
        //~ return quads;
    //~ }

    //~ @Unique
    //~ private static void aetherium$touchAll(final List<BakedQuad> quads) {
        //~ for (int i = 0, n = quads.size(); i < n; i++) {
            //~ final Object quad = quads.get(i);
            //~ if (!(quad instanceof BakedQuadAccessor)) {
                //~ // The accessor did not apply: nothing could be recorded, so animate everything.
                //~ SpriteActivity.markBroken();
                //~ return;
            //~ }
            //~ SpriteActivity.touch(((BakedQuadAccessor) quad).aetherium$sprite());
        //~ }
    //~ }
    // @era:qol16-end
}
