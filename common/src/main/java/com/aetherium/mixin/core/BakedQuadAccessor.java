package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import net.minecraft.client.renderer.texture.TextureAtlasSprite;
//~ import org.spongepowered.asm.mixin.gen.Accessor;
// @era:qol16-end

/** The sprite of a baked quad (1.16.5 has no getter). Applied only where {@code Capabilities.STREAMING} is true. */
@Mixin(targets = "net.minecraft.client.renderer.block.model.BakedQuad")
public interface BakedQuadAccessor {
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ @Accessor("sprite")
    //~ TextureAtlasSprite aetherium$sprite();
    // @era:qol16-end
}
