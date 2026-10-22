package com.aetherium.client;

import com.aetherium.gui.UiSound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

/** The settings screen's interface sounds: vanilla's button click at its own pitch and volume. */
final class UiSounds {

    private UiSounds() {
    }

    static void play(final Minecraft minecraft, final UiSound sound) {
        if (minecraft == null || sound == null) {
            return;
        }
        try {
            minecraft.getSoundHandler().playSound(new PositionedSoundRecord(
                    new ResourceLocation("gui.button.press"), sound.volume, sound.pitch, 0.0f, 0.0f, 0.0f) {
                {
                    this.attenuationType = AttenuationType.NONE;
                }
            });
        } catch (final RuntimeException error) {
            // Sound engine not ready: stay silent.
        }
    }
}
