package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin v16
import com.aetherium.client.SkullLookup;
import com.aetherium.client.TuningClient;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:qol16-else none
// @era:qol16-end

/**
 * Merged light rebuilds (1.10.0, Minecraft 1.16.5): every light change marks its section for a
 * rebuild immediately, once per light layer and per update, so a torch placed / a flickering
 * redstone lamp / sunrise rebuilds the same sections several times in a row. The sections are
 * collected instead and rebuilt once every 2 ticks ({@link TuningClient#tick}).
 */
@Mixin(targets = "net.minecraft.client.multiplayer.ClientChunkCache")
public abstract class TuningLightMixin {
    // @era:qol16-begin v16
    @Inject(method = "onLightUpdate", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$merge(final LightLayer layer, final SectionPos pos, final CallbackInfo ci) {
        if (SkullLookup.onClientThread() && TuningClient.deferLight(pos)) {
            ci.cancel();
        }
    }
    // @era:qol16-else none
    // @era:qol16-end
}
