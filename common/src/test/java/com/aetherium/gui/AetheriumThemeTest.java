package com.aetherium.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class AetheriumThemeTest {

    @AfterEach
    void backToLight() {
        AetheriumTheme.apply(0f);
    }

    @Test
    @DisplayName("apply(0) and apply(1) publish exactly the light and dark palettes")
    void endpoints() {
        AetheriumTheme.apply(0f);
        assertEquals(AetheriumTheme.LIGHT[0], AetheriumTheme.BACKGROUND);
        assertEquals(0xFFFFFFFF, AetheriumTheme.SURFACE);
        assertEquals(0xFF111827, AetheriumTheme.INK);
        AetheriumTheme.apply(1f);
        assertEquals(AetheriumTheme.DARK[0], AetheriumTheme.BACKGROUND);
        assertEquals(0xFF171A21, AetheriumTheme.SURFACE);
        assertEquals(0xFFF3F4F6, AetheriumTheme.INK);
        AetheriumTheme.apply(7f);
        assertEquals(1f, AetheriumTheme.darkness(), 0f);
    }

    @Test
    @DisplayName("every palette entry keeps readable contrast between ink and surface in both themes")
    void contrast() {
        for (final float t : new float[]{0f, 1f}) {
            AetheriumTheme.apply(t);
            final double ink = luminance(AetheriumTheme.INK);
            final double surface = luminance(AetheriumTheme.SURFACE);
            final double ratio = (Math.max(ink, surface) + 0.05) / (Math.min(ink, surface) + 0.05);
            assertTrue(ratio >= 7.0, "ink on surface must reach WCAG AAA (t=" + t + ", ratio=" + ratio + ")");
            final double muted = luminance(AetheriumTheme.MUTED);
            final double mutedRatio = (Math.max(muted, surface) + 0.05) / (Math.min(muted, surface) + 0.05);
            assertTrue(mutedRatio >= 4.5, "descriptions must stay readable (t=" + t + ", ratio=" + mutedRatio + ")");
        }
    }

    @Test
    @DisplayName("mix is exact at the ends, monotonic between, and fade only touches alpha")
    void mixAndFade() {
        assertEquals(0xFF000000, AetheriumTheme.mix(0xFF000000, 0xFFFFFFFF, 0f));
        assertEquals(0xFFFFFFFF, AetheriumTheme.mix(0xFF000000, 0xFFFFFFFF, 1f));
        int last = -1;
        for (int i = 0; i <= 20; i++) {
            final int red = (AetheriumTheme.mix(0xFF000000, 0xFFFF0000, i / 20f) >>> 16) & 0xFF;
            assertTrue(red >= last);
            last = red;
        }
        assertEquals(0x80123456, AetheriumTheme.fade(0xFF123456, 0.503f) & 0xFFFFFFFF);
        assertEquals(0x00123456, AetheriumTheme.fade(0xFF123456, -3f));
    }

    private static double luminance(final int argb) {
        return 0.2126 * channel((argb >> 16) & 0xFF) + 0.7152 * channel((argb >> 8) & 0xFF) + 0.0722 * channel(argb & 0xFF);
    }

    private static double channel(final int value) {
        final double c = value / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}
