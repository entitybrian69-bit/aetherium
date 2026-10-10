package com.aetherium.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One sidebar tab: icon, name, panel title/subtitle and its rows. */
public final class Page {
    public final String name;
    public final String title;
    public final String subtitle;
    public final PixelArt.Bitmap icon;
    private final List<Setting> rows = new ArrayList<Setting>();

    public Page(final String name, final String title, final String subtitle, final PixelArt.Bitmap icon) {
        this.name = name;
        this.title = title;
        this.subtitle = subtitle;
        this.icon = icon;
    }

    public Page add(final Setting setting) {
        if (setting != null) {
            this.rows.add(setting);
        }
        return this;
    }

    public List<Setting> rows() {
        return Collections.unmodifiableList(this.rows);
    }
}
