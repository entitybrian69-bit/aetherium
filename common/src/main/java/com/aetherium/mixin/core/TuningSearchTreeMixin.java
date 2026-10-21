package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:boost16-begin none
// @era:boost16-else v16
//~ import com.aetherium.perf.LazySearch;
//~ import java.util.List;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
// @era:boost16-end

/**
 * Search index on demand (1.17.2, Minecraft 1.16.5): the name-and-tooltip tree overrides
 * {@code search}, so it asks the base class (see {@link TuningSearchMixin}) to build the index
 * first as well.
 */
@Mixin(targets = "net.minecraft.client.searchtree.ReloadableSearchTree")
public abstract class TuningSearchTreeMixin {
    // @era:boost16-begin none
    // @era:boost16-else v16
    //~ @Inject(method = "search", at = @At("HEAD"), require = 0)
    //~ private void aetherium$buildBeforeSearch(final String query, final CallbackInfoReturnable<List<?>> cir) {
        //~ final Object self = this;
        //~ if (self instanceof LazySearch) {
            //~ ((LazySearch) self).aetherium$ensureBuilt();
        //~ }
    //~ }
    // @era:boost16-end
}
