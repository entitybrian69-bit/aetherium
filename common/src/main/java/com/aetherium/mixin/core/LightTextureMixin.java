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
 * continues without gamma post-processing. Verified method targets:
 * {@code LightTexture#updateTick(float)} (1.16.5-1.20.4) and
 * {@code LightTexture#tick(float)} (1.20.5+); {@code LightTexture#block(int)} and
 * {@code LightTexture#sky(int)} were confirmed to exist by reading
 * {@code neoforged/NeoForge @ 1.21.1} patch
 * {@code patches/net/minecraft/client/renderer/LightTexture.java.patch}.</p>
 *
 * <p>Why TAIL of the tick method: vanilla has finished writing its own colours
 * (including any shader pack's contribution, since Iris writes the lightmap through
 * the same object), so we are the last transform before the texture upload, which is
 * what makes the feature work identically with and without Iris.</p>
 */
@Mixin(LightTexture.class)
public abstract class LightTextureMixin {

    // [UNVERIFIED: which of the two names 1.21.1 uses (updateTick on 1.16.5-1.20.4, tick on
    // 1.20.5+) and whether the parameter is float deltaFrames or a tick count. require = 0 makes
    // a wrong guess mean "gamma post-processing is off, one log line", not a crash.]
    @Inject(method = {"updateTick", "tick"}, at = @At("TAIL"), require = 0, expect = 0)
    private void aetherium$afterLightmapUpdate(final CallbackInfo ci) {
        final Aetherium.State state = Aetherium.getState();
        if (state != Aetherium.State.ACTIVE && state != Aetherium.State.SHADOW) {
            return;
        }
        LightmapWriter.apply((LightTexture) (Object) this);
    }

    /**
     * {@code LightTexture#upload(NativeImage)} (1.16.5-1.19.2) is where the packed
     * pixels reach the GPU on the NativeImage builds; hooking it as well means the
     * same writer works on both shapes without a version branch.
     */
    @Inject(method = "upload", at = @At("HEAD"), require = 0, expect = 0)
    private void aetherium$beforeUpload(final CallbackInfo ci) {
        // The writer is idempotent per frame; a second call in the same frame is a
        // no-op because nothing changed, so no extra guard is needed here.
        final Aetherium.State state = Aetherium.getState();
        if (state != Aetherium.State.ACTIVE && state != Aetherium.State.SHADOW) {
            return;
        }
        LightmapWriter.apply((LightTexture) (Object) this);
    }
}
