package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:boost16-begin none
// @era:boost16-else v16
//~ import com.aetherium.perf.CompactNeighbours;
//~ import com.aetherium.perf.EarlyFlags;
//~ import com.aetherium.perf.StateIndex;
//~ import com.google.common.collect.ImmutableMap;
//~ import com.google.common.collect.Table;
//~ import java.util.Map;
//~ import net.minecraft.world.level.block.state.properties.Property;
//~ import org.spongepowered.asm.mixin.Final;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:boost16-end

/**
 * Compact block states (1.17.2, Minecraft 1.16.5): instead of building a hash table of "this
 * state with one property changed" inside every block and fluid state, each state gets a tiny
 * {@link CompactNeighbours} view over one {@link StateIndex} shared by all states of its block.
 * Vanilla's {@code setValue} keeps running unchanged on top of it (same answers, same errors), so
 * nothing else needs hooking.
 *
 * <p>Start-up only ({@link EarlyFlags#compactBlockStates}). When a block's states are not the plain
 * combination of its property values (a mod doing something unusual), that block keeps vanilla's
 * tables.</p>
 */
@Mixin(targets = "net.minecraft.world.level.block.state.StateHolder")
public abstract class TuningStateMixin {
    // @era:boost16-begin none
    // @era:boost16-else v16
    //~ @Shadow
    //~ @Final
    //~ private ImmutableMap<Property<?>, Comparable<?>> values;

    //~ @Shadow
    //~ private Table<Property<?>, Comparable<?>, Object> neighbours;

    //~ @Inject(method = "populateNeighbours", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$compact(final Map<Map<Property<?>, Comparable<?>>, Object> byValues, final CallbackInfo ci) {
        //~ if (this.neighbours != null || byValues == null || !EarlyFlags.compactBlockStates()) {
            //~ return; // vanilla (and its "populated twice" error)
        //~ }
        //~ final StateIndex index = StateIndex.forBlock(byValues, this.values,
                //~ property -> ((Property<?>) property).getPossibleValues());
        //~ if (index == null) {
            //~ return;
        //~ }
        //~ final int at = index.indexOf(this.values);
        //~ if (at < 0 || index.state(at) != (Object) this) {
            //~ return;
        //~ }
        //~ this.neighbours = new CompactNeighbours(index, at, this);
        //~ ci.cancel();
    //~ }
    // @era:boost16-end
}
