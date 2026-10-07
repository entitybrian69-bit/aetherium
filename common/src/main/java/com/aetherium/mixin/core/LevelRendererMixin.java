package com.aetherium.mixin.core;

import com.aetherium.Aetherium;
import com.aetherium.client.ClientHooks;
import com.aetherium.render.mesh.MeshCounters;
import com.aetherium.util.AetheriumLog;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Section-dirty coordination and rebuild accounting on the vanilla renderer.
 *
 * <p>Targets, verified as present in the 1.21.1 era by
 * {@code CaffeineMC/sodium @ 1.21.1/stable} (its mixin set injects into
 * {@code core.render.world.LevelRendererMixin} and
 * {@code features.render.world.clouds.LevelRendererMixin}): {@code setSectionDirty},
 * {@code allChanged}, {@code onSectionCompleted}. All three are named
 * {@code method = "..."} with no descriptor, so each matches whichever overload the
 * target version declares — which is the property the porting system depends on.</p>
 *
 * <p>Behaviour contract: this mixin never cancels or replaces vanilla work. It counts,
 * and it clears our own queued uploads when the game rebuilds everything - because the
 * moment a renderer mod cancels a section rebuild it has to reproduce the entire mesher,
 * and that is the point where "outperforms Sodium" turns into "renders nothing on
 * 1.20.6". The Aetherium mesher path is opted into separately (see README,
 * "Honest status").</p>
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    /**
     * Static because the figures it reports live in {@link MeshCounters}; Mixin copies
     * this field onto {@code LevelRenderer} itself, which is exactly one static per
     * class and costs nothing after class loading.
     */
    private static final AetheriumLog AETHERIUM$LOGGER = AetheriumLog.of(LevelRenderer.class);

    // [UNVERIFIED: setSectionDirty's parameter list on 1.21.1 (int x, int y, int z, boolean
    // important) - the name is confirmed by Sodium 1.21.1's renderer mixins, the shape is read
    // from the same version's call sites in other mods. This injection has require from the
    // config default (0), so a mismatch degrades to "dirty counters stay zero", never a crash.]
    @Inject(method = "setSectionDirty", at = @At("HEAD"))
    private void aetherium$countDirtyRequest(final int x, final int y, final int z, final boolean important, final CallbackInfo ci) {
        if (Aetherium.isVanillaPath()) {
            return;
        }
        if (MeshCounters.noteDirtyRequest(important)) {
            // A burst of important rebuilds in one frame is the signature of a
            // piston/quartz chain. The scheduler's backpressure handles it; all that is
            // left to do is say so, and MeshCounters has already throttled us.
            AETHERIUM$LOGGER.warn("{} important section rebuilds in one frame, latest near {}, {}, {} - a redstone clock?",
                    MeshCounters.getDirtyThisFrame(), x, y, z);
        }
    }

    @Inject(method = "allChanged", at = @At("HEAD"))
    private void aetherium$onFullRebuild(final CallbackInfo ci) {
        if (Aetherium.isVanillaPath()) {
            return;
        }
        MeshCounters.noteFullRebuild();
        final var scheduler = ClientHooks.scheduler();
        if (scheduler != null) {
            // A full rebuild invalidates every mesh we hold; keeping stale tasks in
            // the queue uploads geometry against block data that no longer exists.
            scheduler.clearAll();
        }
        Aetherium.lights().invalidateCache();
    }

    @Inject(method = "allChanged", at = @At("TAIL"))
    private void aetherium$afterFullRebuild(final CallbackInfo ci) {
        if (Aetherium.isVanillaPath()) {
            return;
        }
        final var scheduler = ClientHooks.scheduler();
        if (scheduler != null) {
            scheduler.pump();
        }
    }

    /**
     * Re-sets the per-frame dirty counter at the start of the render tick. Unconditional
     * (no vanilla-path guard): a counter that is only reset on one path would report a
     * frame of any length as a burst.
     */
    @Inject(method = "tick", at = @At("HEAD"), require = 0, expect = 0)
    private void aetherium$onRenderTick(final CallbackInfo ci) {
        MeshCounters.beginFrame();
    }

    /**
     * 1.21.1 signature: {@code onSectionCompleted(ChunkRenderDispatcher.RenderChunk)}.
     * Tolerant because the method was {@code finishMeshBuilding} on 1.20.x and does
     * not exist at all on 1.16.5.
     */
    @Inject(method = {"onSectionCompleted", "finishMeshBuilding"}, at = @At("HEAD"), require = 0, expect = 0)
    private void aetherium$onSectionCompleted(final CallbackInfo ci) {
        if (Aetherium.isVanillaPath()) {
            return;
        }
        // Counted, not acted on: AetheriumHudRenderer prints "built" against "dirty" so
        // an overlap between two renderers (both meshing the same section every frame) is
        // visible in the HUD instead of only in someone's bug report.
        MeshCounters.noteSectionCompleted();
    }

    // No aetherium$get* accessors here: MeshCounters is the single readable home for
    // these figures, so a per-instance getter would be a second source of truth that
    // nothing consumes. The dirty/burst/full-rebuild counts all live there.
}
