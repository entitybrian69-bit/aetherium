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
}
