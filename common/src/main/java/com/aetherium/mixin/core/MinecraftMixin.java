package com.aetherium.mixin.core;

import com.aetherium.Aetherium;
import com.aetherium.client.ClientHooks;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client lifecycle hooks: boot, world enter/leave, tick.
 *
 * <p>Verified targets (mojmap names as used by CaffeineMC/sodium @ 1.21.1/stable,
 * whose mixin set for that version references {@code net.minecraft.client.Minecraft}):
 * {@code Minecraft#updateDisplayDistance()}, {@code Minecraft#tick()},
 * {@code Minecraft#setLevel(Level)}. The method <em>names</em> are stable from
 * 1.14 to 26.x; {@code setLevel} was {@code loadWorld} until 1.16.5 and
 * {@code setLevel} from 1.17, which is why both names are listed and
 * {@code require = 0}/{@code expect = 0} tolerates the one that is absent on the
 * target version (see {@code strictMixins} for flipping this to a hard failure).</p>
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void aetherium$onTick(final CallbackInfo ci) {
        if (!Aetherium.isActive()) {
            return;
        }
        ClientHooks.onClientTick((Minecraft) (Object) this);
    }

    // [UNVERIFIED: whether 1.21.1's method is named setLevel or loadWorld (it has flipped twice
    // in the porting range) and whether the null-level case is a separate overload. Both names
    // are listed; a version that declares neither reports "world hooks inactive" in the log.]
    @Inject(method = {"setLevel", "loadWorld"}, at = @At("TAIL"), require = 0, expect = 0)
    private void aetherium$onWorldEnter(final CallbackInfo ci) {
        Aetherium.onWorldEnter();
        if (Aetherium.subsystemsOrNull() != null) {
            // A load hitch must not sit in the p99 for the next five minutes.
            Aetherium.frameStats().resetWindow();
        }
    }

    /**
     * Boot hook. Mixin ordering across loaders makes an explicit entry point
     * unreliable — Fabric calls its initializer before any mixin runs, NeoForge's
     * mod constructor can run after the first render — so the engine bootstraps
     * lazily here as well, guarded inside {@link Aetherium#initialize()}.
     */
    @Inject(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;initServices()V", shift = At.Shift.AFTER, ordinal = 0), require = 0, expect = 0)
    private void aetherium$onServicesReady(final CallbackInfo ci) {
        Aetherium.initialize();
    }
}
