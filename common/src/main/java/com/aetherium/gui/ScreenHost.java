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
}
