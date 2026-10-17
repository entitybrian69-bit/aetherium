package com.aetherium.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Render scale decisions (1.9.0). */
final class RenderScaleTest {

    @Test
    void fullScaleAndBlockersDrawStraightIntoTheWindow() {
        assertEquals(100, RenderScale.percentFor(100, false, false, false));
        assertEquals(100, RenderScale.percentFor(70, true, false, false));
        assertEquals(100, RenderScale.percentFor(70, false, true, false));
        assertEquals(100, RenderScale.percentFor(70, false, false, true));
    }

    @Test
    void scaledFramesClampToTheFloor() {
        assertEquals(70, RenderScale.percentFor(70, false, false, false));
        assertEquals(RenderScale.MIN_PERCENT, RenderScale.percentFor(10, false, false, false));
    }

    @Test
    void sizesRoundAndNeverReachZero() {
        assertEquals(598, RenderScale.scaled(854, 70));
        assertEquals(336, RenderScale.scaled(480, 70));
        assertEquals(1, RenderScale.scaled(1, 50));
    }
}
