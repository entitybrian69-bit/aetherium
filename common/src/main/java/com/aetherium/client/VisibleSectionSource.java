package com.aetherium.client;

import com.aetherium.perf.SectionVisibility;

/**
 * Added to {@code LevelRenderer} by {@code VisibleSectionsMixin}: copies the sections vanilla
 * decided to draw this frame into a {@link SectionVisibility} grid. Lives outside the mixin
 * package because normal code may not reference mixin classes.
 */
public interface VisibleSectionSource {

    /** Marks every section in vanilla's visible list; the caller has already called {@code begin}. */
    void aetheriumCollectVisible(SectionVisibility out);
}
