package com.aetherium.mixin.core;

import com.aetherium.Aetherium;
import com.aetherium.gamma.LightmapWriter;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Post-processes the lightmap for gamma utilities and coloured dynamic lights.
 *
 * <p>Deliberately contains <b>no {@code @Shadow} field</b>. The vanilla lightmap's
 * storage has changed shape across the porting range ({@code NativeImage lightmap}
 * on 1.16.5-1.19.2, {@code int[] pixels} on 1.20.2+, with 1.20.1 in between), and a
 * {@code @Shadow} field whose name or type drifted is a launch-time crash rather a
 * degraded feature — which is precisely the wrong trade for a mod that ships 33
 * versions. Instead the field is resolved once by {@link LightmapWriter} through a
 * reflective probe of the real class and cached as a {@code MethodHandle}; if no
 * supported shape exists the writer disables itself with one log line and the game
 * continues without gamma post-processing. Verified method target:
 * {@code LightTexture#updateLightTexture(float)} exists on both 1.19.4 and 1.21.1 -
 * read from IrisShaders/Iris's own {@code MixinLightTexture} on both branches, which
 * injects into that exact method (the comment at the injection site records the
 * source); the pre-1.17 rows list {@code tickLightTexture} as a tolerant second
 * candidate whose exact name has not yet been read from a jar.</p>
 *
 * <p>Why TAIL of the tick method: vanilla has finished writing its own colours
 * (including any shader pack's contribution, since Iris writes the lightmap through
 * the same object), so we are the last transform before the texture upload, which is
 * what makes the feature work identically with and without Iris.</p>
 */
@Mixin(LightTexture.class)
public abstract class LightTextureMixin {

    // Verified name on 1.21.1 AND 1.19.4: IrisShaders/Iris's own MixinLightTexture injects into
    // `updateLightTexture` on both branches (at INVOKE of ClientLevel#getSkyDarken(F)F), so the
    // name has been stable across the whole GuiGraphics transition at minimum. The earlier
    // candidate pair {"updateTick", "tick"} matched no known version - those names were invented.
    // `tickLightTexture` stays as a tolerant second candidate for the pre-1.17 rows whose exact
    // name has not been read from a jar yet; require = 0 keeps a miss at "feature off, one log
    // line", never a crash. The handler captures no arguments, so the float-vs-tick parameter
    // drift between versions cannot break binding.
    @Inject(method = {"updateLightTexture", "tickLightTexture"}, at = @At("TAIL"), require = 0, expect = 0)
    private void aetherium$afterLightmapUpdate(final CallbackInfo ci) {
        final Aetherium.State state = Aetherium.getState();
        if (state != Aetherium.State.ACTIVE && state != Aetherium.State.SHADOW) {
            return;
        }
        LightmapWriter.apply((LightTexture) (Object) this);
    }

    // No `upload` injection. An earlier revision injected into `LightTexture#upload` "for the
    // 1.16.5-1.19.2 NativeImage builds" - no such method could be found on any version whose
    // real source was read (the upload lives on DynamicTexture, a different class), so the
    // injection was a permanent no-op dressed as a compatibility hook. Removed rather than kept
    // with require = 0: dead injections are how a porting table starts lying.
}
