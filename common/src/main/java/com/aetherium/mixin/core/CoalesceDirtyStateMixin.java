package com.aetherium.mixin.core;

import com.aetherium.client.RenderChunkDirtyAccess;
import org.spongepowered.asm.mixin.Mixin;
// @era:level-extract-begin none
// @era:level-extract-else x262
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:level-extract-end

/**
 * Rebuild coalescing on 26.2+: the per-section dirty flag moved from the render section to
 * {@code SectionUpdateTracker$SectionDirtyState} (one per section slot around the camera, reused
 * as the camera moves). It records when its section became dirty and was last dirtied, as
 * {@code RenderChunkDirtyMixin} does for the render section on older versions. A slot handed to a
 * new section starts with no time, so that section is never held back. Applied only where
 * {@code Capabilities.COALESCE_EXTRACT} is true.
 */
@Mixin(targets = "net.minecraft.client.SectionUpdateTracker$SectionDirtyState")
public abstract class CoalesceDirtyStateMixin implements RenderChunkDirtyAccess {
    // @era:level-extract-begin none
    @Override
    public long aetheriumFirstDirty() {
        return 0L;
    }

    @Override
    public long aetheriumLastDirty() {
        return 0L;
    }
    // @era:level-extract-else x262
    //~ @Shadow
    //~ private boolean isDirty;

    //~ @Shadow
    //~ private long sectionNode;

    //~ @Unique
    //~ private long aetherium$firstDirty;

    //~ @Unique
    //~ private long aetherium$lastDirty;

    //~ @Inject(method = "setDirty", at = @At("HEAD"), require = 0)
    //~ private void aetherium$stampDirty(final boolean playerChanged, final CallbackInfo ci) {
        //~ final long now = System.nanoTime();
        //~ if (!this.isDirty || this.aetherium$firstDirty == 0L) {
            //~ this.aetherium$firstDirty = now;
        //~ }
        //~ this.aetherium$lastDirty = now;
    //~ }

    //~ @Inject(method = "setNotDirty", at = @At("HEAD"), require = 0)
    //~ private void aetherium$clearDirty(final CallbackInfo ci) {
        //~ this.aetherium$firstDirty = 0L;
    //~ }

    //~ @Inject(method = "setSectionNode", at = @At("HEAD"), require = 0)
    //~ private void aetherium$newSection(final long node, final CallbackInfo ci) {
        //~ if (node != this.sectionNode) {
            //~ this.aetherium$firstDirty = 0L;
            //~ this.aetherium$lastDirty = 0L;
        //~ }
    //~ }

    //~ @Override
    //~ public long aetheriumFirstDirty() {
        //~ return this.aetherium$firstDirty;
    //~ }

    //~ @Override
    //~ public long aetheriumLastDirty() {
        //~ return this.aetherium$lastDirty;
    //~ }
    // @era:level-extract-end
}
