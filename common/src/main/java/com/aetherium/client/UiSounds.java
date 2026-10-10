package com.aetherium.client;

import com.aetherium.gui.UiSound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * Plays the settings screen's interface sounds through vanilla's UI channel (the "Master" slider
 * applies, the sound is not positional). Every sound is the vanilla button click at its own pitch
 * and volume, so nothing has to be shipped or registered and it works on every version.
 */
final class UiSounds {
    private UiSounds() {
    }

    static void play(final Minecraft minecraft, final UiSound sound) {
        if (minecraft == null || sound == null) {
            return;
        }
        try {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(click(), sound.pitch, sound.volume));
        } catch (final RuntimeException error) {
            // Sound engine not ready (or disabled by the launcher): stay silent.
        }
    }

    private static SoundEvent click() {
        // @era:sound-click-begin holder
        return SoundEvents.UI_BUTTON_CLICK.value();
        // @era:sound-click-else event
        //~ return SoundEvents.UI_BUTTON_CLICK;
        // @era:sound-click-end
    }
}
