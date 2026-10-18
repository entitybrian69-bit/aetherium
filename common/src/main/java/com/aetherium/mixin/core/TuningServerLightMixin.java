package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.aetherium.perf.Tuning;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:qol16-end

/**
 * Faster singleplayer lighting (1.12.0, Minecraft 1.16.5): the server's light thread takes
 * {@code min(queued, taskPerBatch)} jobs per pass, and every pass ends with a full propagation run
 * and a copy of the light section maps. Vanilla's batch is 5 jobs, raised to 500 only while the
 * spawn area is prepared. With the option on a pass takes up to 64 jobs (or vanilla's own larger
 * value), so the fixed cost per pass is paid far less often and new chunks are lit sooner.
 */
@Mixin(targets = "net.minecraft.server.level.ThreadedLevelLightEngine")
public abstract class TuningServerLightMixin {
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ private static final int BATCH = 64;

    //~ @Redirect(method = "runUpdate", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"), require = 0)
    //~ private int aetherium$batch(final int queued, final int perBatch) {
        //~ return Math.min(queued, Tuning.serverLighting ? Math.max(perBatch, BATCH) : perBatch);
    //~ }
    // @era:qol16-end
}
