package com.aetherium.client;

import com.aetherium.gui.UiSound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;

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
                    SoundEvents.UI_BUTTON_CLICK.getSoundName(), SoundCategory.MASTER, sound.volume, sound.pitch,
                    false, 0, ISound.AttenuationType.NONE, 0.0f, 0.0f, 0.0f));
        } catch (final RuntimeException error) {
            // Sound engine not ready: stay silent.
        }
    }
}
