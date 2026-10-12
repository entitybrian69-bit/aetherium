package com.aetherium.mixin.core;

import com.aetherium.client.ClientHooks;

import net.minecraft.client.renderer.texture.TextureAtlas;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:atlas-tick-begin ticker
import com.aetherium.perf.SpriteActivity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
// @era:atlas-tick-else v16
//~ import com.aetherium.perf.SpriteActivity;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//~ import java.util.List;
//~ import net.minecraft.client.renderer.texture.TextureAtlasSprite;
//~ import net.minecraft.resources.ResourceLocation;
//~ import org.spongepowered.asm.mixin.Final;
//~ import org.spongepowered.asm.mixin.Shadow;
// @era:atlas-tick-else tickable
//~ import com.aetherium.perf.SpriteActivity;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//~ import java.util.IdentityHashMap;
//~ import java.util.Map;
//~ import net.minecraft.client.renderer.texture.Tickable;
//~ import net.minecraft.client.renderer.texture.TextureAtlasSprite;
//~ import net.minecraft.resources.ResourceLocation;
//~ import org.spongepowered.asm.mixin.Final;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.Unique;
// @era:atlas-tick-else none
// @era:atlas-tick-end

/**
 * "Animated textures" off: the atlas tick advances and re-uploads every animated sprite (water,
 * lava, fire, portals, sea lanterns, prismarine, command blocks...) twenty times a second with
 * glTexSubImage2D, which GL-over-GLES layers on phones handle badly. {@code tick()} is the single
 * entry point on every version (1.16.5 through 26.x); sprites simply keep their current frame.
 *
 * <p>1.16.5, "Animate visible textures only": after the block atlas loads, its animated sprites
 * (except fire and portals) are tracked, and the tick skips the step and upload of tracked sprites
 * nobody saw for a few ticks ({@code SpriteActivity}).</p>
 */
@Mixin(TextureAtlas.class)
public abstract class TextureAtlasMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$freezeAnimations(final CallbackInfo ci) {
        if (ClientHooks.freezeTextureAnimations()) {
            ci.cancel();
        }
    }
    // @era:atlas-tick-begin ticker
    @Shadow
    @Final
    private ResourceLocation location;

    /** Animation ticker -> its sprite, for this atlas (filled where vanilla creates the tickers). */
    @Unique
    private final Map<Object, TextureAtlasSprite> aetherium$tickerSprites = new IdentityHashMap<Object, TextureAtlasSprite>();

    @Unique
    private void aetherium$track(final TextureAtlasSprite sprite, final Object ticker) {
        if (ticker == null) {
            return;
        }
        this.aetherium$tickerSprites.put(ticker, sprite);
        final Object tracked = sprite;
        if (tracked instanceof SpriteActivity.Tracked) {
            final boolean blocks = TextureAtlas.LOCATION_BLOCKS.equals(this.location);
            // @era:sprite-name-begin contents
            final String name = String.valueOf(sprite.contents().name());
            // @era:sprite-name-else getname
            //~ final String name = String.valueOf(sprite.getName());
            // @era:sprite-name-end
            SpriteActivity.track((SpriteActivity.Tracked) tracked, blocks && !SpriteActivity.alwaysAnimated(name));
        }
    }

    @Inject(method = "cycleAnimationFrames", at = @At("TAIL"), require = 0)
    private void aetherium$advanceTick(final CallbackInfo ci) {
        if (TextureAtlas.LOCATION_BLOCKS.equals(this.location)) {
            SpriteActivity.advance();
        }
    }

    @Inject(method = "upload", at = @At("HEAD"), require = 0)
    private void aetherium$forgetTickers(final CallbackInfo ci) {
        this.aetherium$tickerSprites.clear();
    }

    @WrapOperation(method = "upload", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;createTicker()Lnet/minecraft/client/renderer/texture/TextureAtlasSprite$Ticker;"),
            require = 0)
    private TextureAtlasSprite.Ticker aetherium$trackTicker(final TextureAtlasSprite sprite,
                                                            final Operation<TextureAtlasSprite.Ticker> original) {
        final TextureAtlasSprite.Ticker ticker = original.call(sprite);
        this.aetherium$track(sprite, ticker);
        return ticker;
    }

    @WrapOperation(method = "cycleAnimationFrames", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite$Ticker;tickAndUpload()V"), require = 0)
    private void aetherium$animateIfSeen(final TextureAtlasSprite.Ticker ticker, final Operation<Void> original) {
        final TextureAtlasSprite sprite = this.aetherium$tickerSprites.get(ticker);
        if (sprite == null || SpriteActivity.shouldAnimate(sprite)) {
            original.call(ticker);
        }
    }
    // @era:atlas-tick-else v16

    //~ @Shadow
    //~ @Final
    //~ private List<TextureAtlasSprite> animatedTextures;

    //~ @Shadow
    //~ @Final
    //~ private ResourceLocation location;

    //~ @Inject(method = "reload", at = @At("TAIL"), require = 0)
    //~ private void aetherium$trackSprites(final CallbackInfo ci) {
        //~ final boolean blocks = TextureAtlas.LOCATION_BLOCKS.equals(this.location);
        //~ final List<TextureAtlasSprite> sprites = this.animatedTextures;
        //~ for (int i = 0, n = sprites.size(); i < n; i++) {
            //~ final Object sprite = sprites.get(i);
            //~ if (sprite instanceof SpriteActivity.Tracked) {
                //~ SpriteActivity.track((SpriteActivity.Tracked) sprite,
                        //~ blocks && !SpriteActivity.alwaysAnimated(String.valueOf(sprites.get(i).getName())));
            //~ }
        //~ }
    //~ }

    //~ @WrapOperation(method = "cycleAnimationFrames", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;cycleFrames()V"), require = 0)
    //~ private void aetherium$animateIfSeen(final TextureAtlasSprite sprite, final Operation<Void> original) {
        //~ if (SpriteActivity.shouldAnimate(sprite)) {
            //~ original.call(sprite);
        //~ }
    //~ }

    //~ @Inject(method = "cycleAnimationFrames", at = @At("TAIL"), require = 0)
    //~ private void aetherium$advanceTick(final CallbackInfo ci) {
        //~ if (TextureAtlas.LOCATION_BLOCKS.equals(this.location)) {
            //~ SpriteActivity.advance();
        //~ }
    //~ }
    // @era:atlas-tick-else tickable
    //~ @Shadow
    //~ @Final
    //~ private ResourceLocation location;

    //~ /** Animation ticker -> its sprite, for this atlas (filled where vanilla creates the tickers). */
    //~ @Unique
    //~ private final Map<Object, TextureAtlasSprite> aetherium$tickerSprites = new IdentityHashMap<Object, TextureAtlasSprite>();

    //~ @Unique
    //~ private void aetherium$track(final TextureAtlasSprite sprite, final Object ticker) {
        //~ if (ticker == null) {
            //~ return;
        //~ }
        //~ this.aetherium$tickerSprites.put(ticker, sprite);
        //~ final Object tracked = sprite;
        //~ if (tracked instanceof SpriteActivity.Tracked) {
            //~ final boolean blocks = TextureAtlas.LOCATION_BLOCKS.equals(this.location);
            //~ // @era:sprite-name-begin getname
            //~ final String name = String.valueOf(sprite.getName());
            //~ // @era:sprite-name-else contents
            //~ final String name = String.valueOf(sprite.contents().name());
            //~ // @era:sprite-name-end
            //~ SpriteActivity.track((SpriteActivity.Tracked) tracked, blocks && !SpriteActivity.alwaysAnimated(name));
        //~ }
    //~ }

    //~ @Inject(method = "cycleAnimationFrames", at = @At("TAIL"), require = 0)
    //~ private void aetherium$advanceTick(final CallbackInfo ci) {
        //~ if (TextureAtlas.LOCATION_BLOCKS.equals(this.location)) {
            //~ SpriteActivity.advance();
        //~ }
    //~ }

    //~ @Inject(method = "reload", at = @At("HEAD"), require = 0)
    //~ private void aetherium$forgetTickers(final CallbackInfo ci) {
        //~ this.aetherium$tickerSprites.clear();
    //~ }

    //~ @WrapOperation(method = "reload", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;getAnimationTicker()Lnet/minecraft/client/renderer/texture/Tickable;"),
            //~ require = 0)
    //~ private Tickable aetherium$trackTicker(final TextureAtlasSprite sprite, final Operation<Tickable> original) {
        //~ final Tickable ticker = original.call(sprite);
        //~ this.aetherium$track(sprite, ticker);
        //~ return ticker;
    //~ }

    //~ @WrapOperation(method = "cycleAnimationFrames", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/texture/Tickable;tick()V"), require = 0)
    //~ private void aetherium$animateIfSeen(final Tickable ticker, final Operation<Void> original) {
        //~ final TextureAtlasSprite sprite = this.aetherium$tickerSprites.get(ticker);
        //~ if (sprite == null || SpriteActivity.shouldAnimate(sprite)) {
            //~ original.call(ticker);
        //~ }
    //~ }
    // @era:atlas-tick-else none
    // @era:atlas-tick-end
}
