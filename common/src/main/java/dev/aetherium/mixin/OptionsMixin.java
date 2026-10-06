package dev.aetherium.mixin;

import dev.aetherium.android.AndroidLauncherCompat;
import dev.aetherium.config.AetheriumConfig;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Battery saver: present a reduced render distance to the renderer without rewriting the user's setting. */
@Mixin(Options.class)
public abstract class OptionsMixin {
    @Inject(method = "getEffectiveRenderDistance", at = @At("RETURN"), cancellable = true)
    private void aetherium$batterySaverDistance(CallbackInfoReturnable<Integer> cir) {
        if (!AndroidLauncherCompat.isAndroid()) return;
        AetheriumConfig cfg = AetheriumConfig.get();
        int want = AndroidLauncherCompat.batterySaverRenderDistance(cfg);
        if (want < cir.getReturnValueI()) cir.setReturnValue(want);
    }
}
