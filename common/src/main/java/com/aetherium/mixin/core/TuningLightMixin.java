package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-light-begin v16
import com.aetherium.client.FarTerrain;
import com.aetherium.client.OceanFloor;
import com.aetherium.client.SkullLookup;
import com.aetherium.client.TuningClient;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
// @era:t-light-else none
// @era:t-light-end

/**
 * Merged light rebuilds (1.10.0, Minecraft 1.16.5): every light change marks its section for a
 * rebuild immediately, once per light layer and per update, so a torch placed / a flickering
 * redstone lamp / sunrise rebuilds the same sections several times in a row. The sections are
 * collected instead and rebuilt once every 2 ticks ({@link TuningClient#tick}).
 */
@Mixin(targets = "net.minecraft.client.multiplayer.ClientChunkCache")
public abstract class TuningLightMixin {
    // @era:t-light-begin v16
    @Inject(method = "onLightUpdate", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$merge(final LightLayer layer, final SectionPos pos, final CallbackInfo ci) {
        if (SkullLookup.onClientThread() && TuningClient.deferLight(pos)) {
            ci.cancel();
        }
    }

    /** Far terrain (1.11.0): every chunk the server sends is sampled for the far outline. */
    @Inject(method = "replaceWithPacketData", at = @At("RETURN"), require = 0)
    private void aetherium$farTerrain(final CallbackInfoReturnable<LevelChunk> cir) {
        FarTerrain.capture(cir.getReturnValue());
        OceanFloor.capture(cir.getReturnValue());
    }
    // @era:t-light-else none
    // @era:t-light-end
}
