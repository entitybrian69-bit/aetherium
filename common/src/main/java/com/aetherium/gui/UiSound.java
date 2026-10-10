package com.aetherium.gui;

/**
 * Interface sounds of the settings screen. The host maps each one to a vanilla UI sound
 * with its own pitch and volume, so they share one audio asset and need no resource pack.
 */
public enum UiSound {
    /** Buttons, Apply, dropdown headers. */
    CLICK(1.0f, 0.25f),
    /** A toggle switched on: slightly brighter. */
    TOGGLE_ON(1.18f, 0.25f),
    /** A toggle switched off: slightly lower. */
    TOGGLE_OFF(0.86f, 0.25f),
    /** Another tab selected. */
    TAB(1.08f, 0.22f),
    /** One step of a slider or segmented choice; quiet, rate-limited by the view. */
    TICK(1.65f, 0.10f),
    /** A dropdown entry picked. */
    SELECT(1.3f, 0.22f),
    /** Apply/Done committed changes. */
    APPLY(1.45f, 0.28f),
    /** Light/dark theme switched. */
    THEME(0.75f, 0.28f);

    public final float pitch;
    public final float volume;

    UiSound(final float pitch, final float volume) {
        this.pitch = pitch;
        this.volume = volume;
    }
}
