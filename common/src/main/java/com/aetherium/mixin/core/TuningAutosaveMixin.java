package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-autosave-begin none
// @era:t-autosave-else v16
//~ import com.aetherium.client.FlightPreload;
//~ import com.aetherium.perf.Tuning;
//~ import java.util.function.BooleanSupplier;
//~ import net.minecraft.server.MinecraftServer;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:t-autosave-end

/**
 * Integrated server hooks (1.10.0, Minecraft 1.16.5):
 * <ul>
 *   <li>marks the periodic 5-minute autosave, so {@code TuningChunkSaveMixin} can spread its chunk
 *       writes over the following ticks instead of freezing the game;</li>
 *   <li>elytra look-ahead chunk loading ({@link FlightPreload}).</li>
 * </ul>
 * The options are client options, so on a dedicated server both stay off.
 */
@Mixin(targets = "net.minecraft.server.MinecraftServer")
public abstract class TuningAutosaveMixin {
    // @era:t-autosave-begin none
    // @era:t-autosave-else v16
    //~ @Redirect(method = "tickServer", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/server/MinecraftServer;saveAllChunks(ZZZ)Z"), require = 0)
    //~ private boolean aetherium$autosave(final MinecraftServer server, final boolean suppressLog,
                                       //~ final boolean flush, final boolean force) {
        //~ Tuning.autosaving = true;
        //~ try {
            //~ return server.saveAllChunks(suppressLog, flush, force);
        //~ } finally {
            //~ Tuning.autosaving = false;
        //~ }
    //~ }

    //~ @Inject(method = "tickServer", at = @At("TAIL"), require = 0)
    //~ private void aetherium$flight(final BooleanSupplier hasTime, final CallbackInfo ci) {
        //~ if (Tuning.flightPreload) {
            //~ FlightPreload.tick((MinecraftServer) (Object) this);
        //~ }
    //~ }
    // @era:t-autosave-end
}
