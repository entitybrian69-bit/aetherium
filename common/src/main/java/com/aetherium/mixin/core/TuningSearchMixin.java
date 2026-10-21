package com.aetherium.mixin.core;

import com.aetherium.perf.LazySearch;
import org.spongepowered.asm.mixin.Mixin;
// @era:boost16-begin none
// @era:boost16-else v16
//~ import com.aetherium.perf.Tuning;
//~ import java.util.List;
//~ import net.minecraft.client.searchtree.ReloadableIdSearchTree;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
// @era:boost16-end

/**
 * Search index on demand (1.17.2, Minecraft 1.16.5): the creative inventory's and the recipe
 * book's search trees index every item (including its full tooltip) while the game starts, on
 * every resource reload and whenever recipes or tags arrive from a server, although most sessions
 * never search. With the option on, adding items only records them and a rebuild only marks the
 * tree; the index is built (with vanilla's own code) right before the first search that needs it.
 *
 * <p>The tree keeps its list of items exactly as vanilla does, so a rebuild finds the same items in
 * the same order. Items added after a rebuild become searchable at the next search, where vanilla
 * would only pick them up at its next full rebuild.</p>
 */
@Mixin(targets = "net.minecraft.client.searchtree.ReloadableIdSearchTree")
public abstract class TuningSearchMixin implements LazySearch {
    // @era:boost16-begin none
    // @era:boost16-else v16
    //~ /** Items were added or a rebuild was requested since the index was last built. */
    //~ @Unique
    //~ private boolean aetherium$dirty;

    //~ /** Inside {@link #aetherium$ensureBuilt}: let vanilla's rebuild run. */
    //~ @Unique
    //~ private boolean aetherium$building;

    //~ @Shadow
    //~ public abstract void refresh();

    //~ @Inject(method = "refresh", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$deferRefresh(final CallbackInfo ci) {
        //~ if (Tuning.lazySearch && !this.aetherium$building) {
            //~ this.aetherium$dirty = true;
            //~ ci.cancel();
            //~ return;
        //~ }
        //~ this.aetherium$dirty = false; // vanilla's full rebuild runs now
    //~ }

    //~ @Redirect(method = "add", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/searchtree/ReloadableIdSearchTree;index(Ljava/lang/Object;)V"), require = 0)
    //~ private void aetherium$deferIndex(final ReloadableIdSearchTree<?> self, final Object item) {
        //~ if (Tuning.lazySearch || this.aetherium$dirty) {
            //~ // A full rebuild is pending anyway (or will be): it indexes this item with the rest.
            //~ this.aetherium$dirty = true;
            //~ return;
        //~ }
        //~ this.index(item);
    //~ }

    //~ @Shadow
    //~ protected abstract void index(Object item);

    //~ @Inject(method = "search", at = @At("HEAD"), require = 0)
    //~ private void aetherium$buildBeforeSearch(final String query, final CallbackInfoReturnable<List<?>> cir) {
        //~ this.aetherium$ensureBuilt();
    //~ }

    //~ @Override
    //~ public void aetherium$ensureBuilt() {
        //~ if (!this.aetherium$dirty || this.aetherium$building) {
            //~ return;
        //~ }
        //~ this.aetherium$building = true;
        //~ try {
            //~ this.refresh(); // virtual: a subclass rebuilds its own tree too
            //~ this.aetherium$dirty = false;
        //~ } finally {
            //~ this.aetherium$building = false;
        //~ }
    //~ }
    // @era:boost16-end
}
