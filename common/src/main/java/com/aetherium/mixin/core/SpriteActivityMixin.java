package com.aetherium.mixin.core;

import com.aetherium.perf.SpriteActivity;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.llamalad7.mixinextras.injector.ModifyReturnValue;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
// @era:qol16-end

/**
 * "Animate visible textures only" (1.16.5): tracking state per sprite, and every
 * {@code getU(double)} lookup counts as a use (fluids, block entities through
 * {@code SpriteCoordinateExpander}, breaking particles, the in-wall overlay). Meshed block faces,
 * items and block models drawn by entities are recorded from their quads instead. Applied only
 * where {@code Capabilities.STREAMING} is true.
 */
@Mixin(targets = "net.minecraft.client.renderer.texture.TextureAtlasSprite")
public abstract class SpriteActivityMixin implements SpriteActivity.Tracked {
    // @era:qol16-begin none
    @Override
    public boolean aetheriumTracked() {
        return false;
    }

    @Override
    public void aetheriumSetTracked(final boolean tracked) {
    }

    @Override
    public int aetheriumLastSeen() {
        return 0;
    }

    @Override
    public void aetheriumSetLastSeen(final int tick) {
    }
    // @era:qol16-else v16
    //~ @Unique
    //~ private boolean aetherium$tracked;

    //~ @Unique
    //~ private int aetherium$lastSeen;

    //~ @ModifyReturnValue(method = "getU(D)F", at = @At("RETURN"), require = 0)
    //~ private float aetherium$touch(final float u) {
        //~ SpriteActivity.touch(this);
        //~ return u;
    //~ }

    //~ @Override
    //~ public boolean aetheriumTracked() {
        //~ return this.aetherium$tracked;
    //~ }

    //~ @Override
    //~ public void aetheriumSetTracked(final boolean tracked) {
        //~ this.aetherium$tracked = tracked;
    //~ }

    //~ @Override
    //~ public int aetheriumLastSeen() {
        //~ return this.aetherium$lastSeen;
    //~ }

    //~ @Override
    //~ public void aetheriumSetLastSeen(final int tick) {
        //~ this.aetherium$lastSeen = tick;
    //~ }
    // @era:qol16-end
}
