package com.aetherium.android;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Renderer names as launchers report them in {@code POJAV_RENDERER}. */
final class AndroidRendererTest {
    @Test
    @DisplayName("launcher renderer names map to the right GL translator")
    void parse() {
        assertEquals(AndroidRenderer.GL4ES, AndroidRenderer.parse("opengles2", null));
        assertEquals(AndroidRenderer.NGGL4ES, AndroidRenderer.parse("opengles3", null));
        assertEquals(AndroidRenderer.NGGL4ES, AndroidRenderer.parse("opengles_nggl4es", null), "Pojav forks / Krypton Wrapper");
        assertEquals(AndroidRenderer.NGGL4ES, AndroidRenderer.parse("krypton_wrapper", null));
        assertEquals(AndroidRenderer.LTW, AndroidRenderer.parse("opengles3_ltw", null));
        assertEquals(AndroidRenderer.MOBILEGLUES, AndroidRenderer.parse("opengles_mobileglues", null));
        assertEquals(AndroidRenderer.UNKNOWN, AndroidRenderer.parse("something_else", null));
    }
}
