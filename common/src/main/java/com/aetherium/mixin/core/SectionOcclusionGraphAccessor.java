package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:fog-cull-begin section
import java.util.concurrent.atomic.AtomicBoolean;
import org.spongepowered.asm.mixin.gen.Accessor;
// @era:fog-cull-else bfs|info|none
// @era:fog-cull-end

/** The frustum-pass request flag of the visibility graph (1.20.2..26.1; FogCullMixin). */
@Mixin(targets = "net.minecraft.client.renderer.SectionOcclusionGraph")
public interface SectionOcclusionGraphAccessor {

    // @era:fog-cull-begin section
    @Accessor("needsFrustumUpdate")
    AtomicBoolean aetheriumNeedsFrustumUpdate();
    // @era:fog-cull-else bfs|info|none
    // @era:fog-cull-end
}
