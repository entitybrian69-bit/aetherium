package com.aetherium.gui;

import java.util.List;

/** What the version-independent view needs from the screen around it. */
public interface ScreenHost {
    /** Writes the changed rows and runs the follow-up work (save, chunk reload). */
    void applyChanges(List<Setting> changed);

    /** Closes the screen and returns to the parent. */
    void requestClose();

    int currentFps();

    String footerLeft();

    String footerRight();

    boolean touchMode();

    /** The saved light/dark preference. */
    boolean darkMode();

    /** Saves the light/dark preference immediately (it is a display preference, not a staged option). */
    void setDarkMode(boolean dark);

    /** Plays an interface sound when UI sounds are enabled; never throws. */
    void playSound(UiSound sound);

    /**
     * True when the world is drawn behind the screen, so rows marked
     * {@link Setting#livePreview()} can fade the screen away while they are edited.
     */
    default boolean livePreviewAvailable() {
        return false;
    }

    /** True when the header shows the External Shaders button (the shader engine exists here). */
    default boolean shaderPageAvailable() {
        return false;
    }

    /** Opens the Iris-style shader pack page. */
    default void openShaderPage() {
    }

    /** Called at the start of every frame, before the view draws. */
    default void beforeFrame() {
    }
}
