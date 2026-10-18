package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-ao-cache-begin v16|a215
import com.aetherium.perf.Tuning;
import it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
// @era:t-ao-cache-else none
// @era:t-ao-cache-end

/**
 * Bigger smooth-lighting cache (1.12.0, Minecraft 1.16.5): while a chunk is built, each builder
 * thread caches the light and shade of the last 100 block positions it looked up
 * ({@code ModelBlockRenderer.Cache}), evicting the oldest when the map holds exactly 100. Blocks
 * are visited in rows of 16, so the neighbouring row's entries are already gone when smooth
 * lighting needs them again. The cap becomes 320.
 *
 * <p>Safety: the maps are created with 100 expected entries at load factor 0.25 (512 slots) and
 * vanilla disables rehashing, so they must never fill up. The size check is answered as "full"
 * from 320 entries on with the option on, and from 100 on with it off (so a map left larger by a
 * switch never grows further). The maps are cleared after every chunk anyway.</p>
 */
@Mixin(targets = "net.minecraft.client.renderer.block.ModelBlockRenderer$Cache")
public abstract class TuningAoCacheMixin {
    // @era:t-ao-cache-begin v16
    private static final int VANILLA = 100;
    private static final int LIMIT = 320;

    private static int aetherium$full(final int size) {
        if (Tuning.aoCache) {
            return size >= LIMIT ? VANILLA : -1;
        }
        return size >= VANILLA ? VANILLA : size;
    }

    @Redirect(method = "getLightColor", at = @At(value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/longs/Long2IntLinkedOpenHashMap;size()I"), require = 0)
    private int aetherium$colorSize(final Long2IntLinkedOpenHashMap map) {
        return aetherium$full(map.size());
    }

    @Redirect(method = "getShadeBrightness", at = @At(value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/longs/Long2FloatLinkedOpenHashMap;size()I"), require = 0)
    private int aetherium$brightnessSize(final Long2FloatLinkedOpenHashMap map) {
        return aetherium$full(map.size());
    }
    // @era:t-ao-cache-else none
    // @era:t-ao-cache-else a215
    //~ private static final int VANILLA = 100;
    //~ private static final int LIMIT = 320;

    //~ private static int aetherium$full(final int size) {
        //~ if (Tuning.aoCache) {
            //~ return size >= LIMIT ? VANILLA : -1;
        //~ }
        //~ return size >= VANILLA ? VANILLA : size;
    //~ }

    //~ // 1.21.5+: the colour cache is filled inside a lambda (the cached BrightnessGetter), so every
    //~ // method of the class is targeted; both handlers are static, valid for a lambda of either kind.
    //~ @Redirect(method = "*", at = @At(value = "INVOKE",
            //~ target = "Lit/unimi/dsi/fastutil/longs/Long2IntLinkedOpenHashMap;size()I"), require = 0)
    //~ private static int aetherium$colorSize(final Long2IntLinkedOpenHashMap map) {
        //~ return aetherium$full(map.size());
    //~ }

    //~ @Redirect(method = "*", at = @At(value = "INVOKE",
            //~ target = "Lit/unimi/dsi/fastutil/longs/Long2FloatLinkedOpenHashMap;size()I"), require = 0)
    //~ private static int aetherium$brightnessSize(final Long2FloatLinkedOpenHashMap map) {
        //~ return aetherium$full(map.size());
    //~ }
    // @era:t-ao-cache-end
}
