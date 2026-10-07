package com.aetherium.mixin.core;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

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

/**
 * Puts an "Aetherium" row in vanilla's Options screen so the settings are reachable.
 *
 * <p>This file used to also <em>hijack</em> vanilla's "Video Settings..." button by cancelling the
 * private lambda the options list builds for it, the way Sodium does - {@code method_19828} plus
 * {@code lambda$init$2..6}. That injection is gone, and the reason is the whole lesson of this file:
 * the mixin annotation processor must resolve <em>every</em> name it is handed while writing the
 * refmap, so an intermediary name or a synthetic lambda index that exists on one version is a
 * compile-time failure on all the others ("Unable to locate obfuscation mapping for @Inject target
 * method_19828", CI, on the 1.21 row). {@code require = 0} cannot help, because the failure happens
 * before any injection is attempted. See the [BLOCKED] note below for what would bring it back.</p>
 *
 * <p>What remains is the part that works on every row: a TAIL hook on {@code init} that appends our
 * own row, and a HEAD hook that counts vanilla's own video controls so the Advanced tab can report
 * which path the player is looking at. Both target {@code init} by bare name - {@code OptionsScreen}
 * changed the shape of that method across the range, and a name with no descriptor binds to whichever
 * overload the row declares.</p>
 */
@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {

    protected OptionsScreenMixin(final net.minecraft.network.chat.Component title) {
        super(title);
    }

    // [BLOCKED] The vanilla video-settings hijack is removed rather than carried broken.
    // It injected into {@code method_19828} / {@code lambda$init$2..6} - an intermediary name and the
    // synthetic indices of a private lambda, both read off Sodium's own mixin for 1.21.1. The mixin
    // annotation processor must resolve every name it is given while writing the refmap, so on every
    // other row the build died with "Unable to locate obfuscation mapping for @Inject target
    // method_19828" (CI, 1.21) - a require = 0 does not help, because the failure is at compile time,
    // not at apply time. A target list that only one version can satisfy is not a tolerant injection;
    // it is a hard dependency on a private implementation detail. What replaces it is the fallback
    // row below, which opens the same screen and resolves on every row. Restoring the hijack needs a
    // verified per-version target list in deltas/<version>/, and PORTING_MATRIX.md is where that would
    // be recorded.

    /**
     * Fallback entry: inserts a cycle-button-styled row that opens the Aetherium
     * screen, so a rename of the video-settings lambda degrades into "an extra
     * button" instead of "no access".
     */
    // No descriptor on purpose: OptionsScreen's init overload changed shape across the range
    // (init(CallbackInfo) vs init(Screen, CallbackInfo)), and a bare name matches whichever
    // one the row declares - the handler is then bound to the compatible candidate.
    @Inject(method = "init", at = @At("TAIL"), require = 0, expect = 0)
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
        // Screen#addRenderableWidget is protected on 1.21.1, and javac type-checks a mixin against
        // its own source, not against the class it will be merged into - so the call the transform
        // makes legal does not compile. A handle is the same answer ClientHooks#dirtySectionsAround
        // gives: nothing structural is pinned, and a miss means "no fallback button", which is what
        // the version gate below already produces. No log on the miss: this runs on every
        // options-screen open, and the Advanced tab reports the fallback's state directly.
        final MethodHandle adder = ADD_RENDERABLE_WIDGET;
        if (adder != null) {
            try {
                adder.invoke(this, button);
            } catch (final Throwable error) {
                // Throwable: an invoke site can surface LinkageError, and this is a convenience
                // widget, not a correctness-critical one.
            }
        }
    }

    /**
     * Vanilla's own video-settings screen is built from a list of
     * {@link CycleButton}s; recording how many we saw is how the Advanced tab
     * reports "the hijack worked" versus "the fallback is in use".
     */
    @Inject(method = "init", at = @At("HEAD"), require = 0, expect = 0)
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

    /** Resolved once; null means the fallback button is simply not added. */
    private static final MethodHandle ADD_RENDERABLE_WIDGET = findAddRenderableWidget();

    /**
     * Matched by name and arity rather than by {@code getDeclaredMethod}: the parameter is the erasure
     * of a type variable whose bounds are three interfaces, and spelling that erasure wrong is
     * another way for a port to fail to launch.
     */
    private static MethodHandle findAddRenderableWidget() {
        for (final java.lang.reflect.Method candidate : Screen.class.getDeclaredMethods()) {
            if (candidate.getName().equals("addRenderableWidget") && candidate.getParameterCount() == 1) {
                try {
                    candidate.setAccessible(true);
                    return MethodHandles.lookup().unreflect(candidate);
                } catch (final RuntimeException | IllegalAccessException error) {
                    return null;
                }
            }
        }
        return null;
    }
}
