package com.aetherium.mixin.core;

import com.aetherium.perf.SpriteActivity;

import org.spongepowered.asm.mixin.Mixin;
// @era:anim-begin v16|on
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
// @era:anim-else none
// @era:anim-end

/**
 * Visible-only animations: a sprite remembers whether it is tracked and when it was last drawn;
 * reading its U coordinate (every quad that uses it) counts as a use. Applied only where
 * {@code Capabilities.VISIBLE_ANIMATIONS} is true.
 */
@Mixin(targets = "net.minecraft.client.renderer.texture.TextureAtlasSprite")
public abstract class SpriteActivityMixin implements SpriteActivity.Tracked {
    // @era:anim-begin v16|on
    @Unique
    private boolean aetherium$tracked;

    @Unique
    private int aetherium$lastSeen;

    // @era:sprite-u-begin f
    @ModifyReturnValue(method = "getU(F)F", at = @At("RETURN"), require = 0)
    // @era:sprite-u-else d
    //~ @ModifyReturnValue(method = "getU(D)F", at = @At("RETURN"), require = 0)
    // @era:sprite-u-end
    private float aetherium$touch(final float u) {
        SpriteActivity.touch(this);
        return u;
    }

    @Override
    public boolean aetheriumTracked() {
        return this.aetherium$tracked;
    }

    @Override
    public void aetheriumSetTracked(final boolean tracked) {
        this.aetherium$tracked = tracked;
    }

    @Override
    public int aetheriumLastSeen() {
        return this.aetherium$lastSeen;
    }

    @Override
    public void aetheriumSetLastSeen(final int tick) {
        this.aetherium$lastSeen = tick;
    }
    // @era:anim-else none
    //~ @Override
    //~ public boolean aetheriumTracked() {
        //~ return false;
    //~ }

    //~ @Override
    //~ public void aetheriumSetTracked(final boolean tracked) {
    //~ }

    //~ @Override
    //~ public int aetheriumLastSeen() {
        //~ return 0;
    //~ }

    //~ @Override
    //~ public void aetheriumSetLastSeen(final int tick) {
    //~ }
    // @era:anim-end
}
