package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:anim-begin v16|on
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.gen.Accessor;
// @era:anim-else none
// @era:anim-end

/**
 * The sprite of a baked quad (a protected field through 1.21.4). Applied only where
 * {@code Capabilities.VISIBLE_ANIMATIONS} is true.
 */
@Mixin(targets = "net.minecraft.client.renderer.block.model.BakedQuad")
public interface BakedQuadAccessor {
    // @era:anim-begin v16|on
    @Accessor("sprite")
    TextureAtlasSprite aetherium$sprite();
    // @era:anim-else none
    // @era:anim-end
}
