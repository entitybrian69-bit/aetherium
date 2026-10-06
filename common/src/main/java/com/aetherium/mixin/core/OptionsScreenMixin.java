package com.aetherium.mixin.core;

import com.aetherium.Aetherium;
import com.aetherium.config.AetheriumConfig;
import com.aetherium.gui.AetheriumVideoOptionsScreen;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces vanilla's "Video Settings..." entry with Aetherium's screen.
 *
 * <p>Technique, verified against {@code CaffeineMC/sodium @ 1.21.1/stable}
 * ({@code features.gui.hooks.settings.OptionsScreenMixin}), which cancels the lambda
 * the vanilla options list builds for the video-settings button:</p>
 * <pre>
 *   &#64;Dynamic
 *   &#64;Inject(method = {"method_19828", "lambda$init$2"}, require = 1, at = &#64;At("HEAD"), cancellable = true)
 *   private void open(CallbackInfoReturnable&lt;Screen&gt; ci) { ci.setReturnValue(VideoSettingsScreen.createScreen(this)); }
 * </pre>
 *
 * <p>Two facts drive the design of this file:</p>
 * <ol>
 *   <li>{@code lambda$init$N} depends on how many lambdas {@code OptionsScreen#init}
 *       declares, so the index moves between versions. Every candidate index is
 *       listed with {@code require = 0, expect = 0}: one matches, the rest are
 *       skipped, and a version port needs no edit here (a delta that knows the exact
 *       index may narrow the list for a clearer log).</li>
 *   <li>If no candidate matches, the player must still reach our screen. The
 *       {@code init(T)V} TAIL hook below therefore appends an "Aetherium" row to the
 *       vanilla list as a permanent fallback. An unreachable settings screen is the
 *       most-reported failure mode of ported renderer mods, so this file never
 *       depends on a single name.</li>
 * </ol>
 */
@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {

    protected OptionsScreenMixin(final net.minecraft.network.chat.Component title) {
        super(title);
    }

    // [UNVERIFIED: "method_19828" is the intermediary name read out of Sodium 1.21.1's mixin for
    // this injection point, and the lambda$init$N index is the one Sodium needed on 1.21.1; both
    // move between versions. Every candidate is listed with require = 0 so a wrong index means
    // "the fallback button is used", never a crash.]
    @Inject(method = {
            "method_19828",
            "lambda$init$2",
            "lambda$init$3",
            "lambda$init$4",
            "lambda$init$5",
            "lambda$init$6"
    }, at = @At("HEAD"), cancellable = true, require = 0, expect = 0)
    private void aetherium$redirectVideoSettings(final CallbackInfoReturnable<Screen> ci) {
        if (!Aetherium.initialize()) {
            return;
        }
        ci.setReturnValue(AetheriumVideoOptionsScreen.create((Screen) (Object) this));
    }

    /**
     * Fallback entry: inserts a cycle-button-styled row that opens the Aetherium
     * screen, so a rename of the video-settings lambda degrades into "an extra
     * button" instead of "no access".
     */
    @Inject(method = "init(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("TAIL"), require = 0, expect = 0)
    private void aetherium$appendOwnButton(final Screen previous, final CallbackInfo ci) {
        final net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null) {
            return;
        }
        final net.minecraft.client.gui.components.Button button = net.minecraft.client.gui.components.Button
                .builder(net.minecraft.network.chat.Component.translatable("aetherium.screen.fallback_button"),
                        widget -> minecraft.setScreen(AetheriumVideoOptionsScreen.create((Screen) (Object) this)))
                .bounds(5, minecraft.getWindow().getGuiScaledHeight() - 24, 110, 20)
                .build();
        ((Screen) (Object) this).addRenderableWidget(button);
    }

    /**
     * Vanilla's own video-settings screen is built from a list of
     * {@link CycleButton}s; recording how many we saw is how the Advanced tab
     * reports "the hijack worked" versus "the fallback is in use".
     */
    @Inject(method = "init(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"), require = 0, expect = 0)
    private void aetherium$countVanillaControls(final Screen previous, final CallbackInfo ci) {
        int cycles = 0;
        for (final var child : ((Screen) (Object) this).children()) {
            if (child instanceof CycleButton<?>) {
                cycles++;
            }
        }
        AetheriumHijackStats.recordVanillaCycleButtons(cycles);
    }

    /** Small stat holder kept out of the screen class so both mixins can use it. */
    public static final class AetheriumHijackStats {
        /** Render-thread only (both injection sites are on that thread). */
        private static int vanillaCycleButtons = -1;

        static void recordVanillaCycleButtons(final int count) {
            vanillaCycleButtons = count;
        }

        public static int getVanillaCycleButtons() {
            return vanillaCycleButtons;
        }

        private AetheriumHijackStats() {
        }
    }

    /** Used by the GUI header so the user can tell which path opened the screen. */
    public static String describeHijack() {
        final int count = AetheriumHijackStats.getVanillaCycleButtons();
        if (count < 0) {
            return "Options screen not opened yet";
        }
        return count > 0
                ? "hijacked vanilla Video Settings (" + count + " vanilla cycle buttons beside us)"
                : "fallback button in use: no vanilla video controls were found";
    }
}
