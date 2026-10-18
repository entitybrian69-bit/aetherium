package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:fog-cull-begin section
import java.util.concurrent.atomic.AtomicBoolean;
import org.spongepowered.asm.mixin.gen.Accessor;
// @era:fog-cull-else bfs|info
// @era:fog-cull-else none
//~ // @era:fog-cull-x-begin none
//~ // @era:fog-cull-x-else extract
//~ //~ import java.util.concurrent.atomic.AtomicBoolean;
//~ //~ import org.spongepowered.asm.mixin.gen.Accessor;
//~ // @era:fog-cull-x-end
// @era:fog-cull-end

/** The frustum-pass request flag of the visibility graph (1.20.2+; FogCullMixin). */
@Mixin(targets = "net.minecraft.client.renderer.SectionOcclusionGraph")
public interface SectionOcclusionGraphAccessor {

    // @era:fog-cull-begin section
    @Accessor("needsFrustumUpdate")
    AtomicBoolean aetheriumNeedsFrustumUpdate();
    // @era:fog-cull-else bfs|info
    // @era:fog-cull-else none
    //~ // @era:fog-cull-x-begin none
    //~ // @era:fog-cull-x-else extract
    //~ //~ @Accessor("needsFrustumUpdate")
    //~ //~ AtomicBoolean aetheriumNeedsFrustumUpdate();
    //~ // @era:fog-cull-x-end
    // @era:fog-cull-end
}
