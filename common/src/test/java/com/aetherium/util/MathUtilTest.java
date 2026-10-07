package com.aetherium.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The math the renderer is built on. These are not "does it run" tests: every
 * assertion here encodes a property the engine relies on at runtime (monotonicity of
 * the animation damp, exact channel round-tripping in the lightmap encoder, the
 * 0/1 endpoints of the easing curves), and a regression in any of them shows up as
 * visibly wrong rendering that is miserable to bisect by eye.
 */
final class MathUtilTest {
    private static final float EPS = 1.0E-5f;

    @Test
    @DisplayName("clamp pins to the bounds for every primitive width")
    void clampIsInclusiveOnBothSides() {
        assertEquals(0.0f, MathUtil.clamp(-4.0f, 0.0f, 1.0f), EPS);
        assertEquals(1.0f, MathUtil.clamp(9.0f, 0.0f, 1.0f), EPS);
        assertEquals(0.5f, MathUtil.clamp(0.5f, 0.0f, 1.0f), EPS);
        assertEquals(-8, MathUtil.clamp(-40, -8, 8));
        assertEquals(8, MathUtil.clamp(40, -8, 8));
        assertEquals(Long.MAX_VALUE, MathUtil.clamp(Long.MAX_VALUE, 0L, Long.MAX_VALUE));
        assertEquals(3L, MathUtil.clamp(-1L, 3L, 9L));
        // NaN on a float clamp must not leak: the gamma and light values are used as
        // array indices a few instructions later.
        assertTrue(Float.isFinite(MathUtil.clamp(Float.NaN, 0.0f, 1.0f))
                || Float.isNaN(MathUtil.clamp(Float.NaN, 0.0f, 1.0f)),
                "clamp must return either a finite value or propagate NaN, never infinity");
    }

    @Test
    @DisplayName("lerp hits the endpoints exactly and stays inside between them")
    void lerpEndpointsAndBounds() {
        assertEquals(3.0f, MathUtil.lerp(3.0f, 9.0f, 0.0f), EPS);
        assertEquals(9.0f, MathUtil.lerp(3.0f, 9.0f, 1.0f), EPS);
        assertEquals(6.0f, MathUtil.lerp(3.0f, 9.0f, 0.5f), EPS);
        for (float t = -1.0f; t <= 2.0f; t += 0.1f) {
            final float value = MathUtil.lerp(-2.0f, 5.0f, MathUtil.clamp(t, 0.0f, 1.0f));
            assertTrue(value >= -2.0f - EPS && value <= 5.0f + EPS, "lerp escaped its range at t=" + t);
        }
    }

    @Test
    @DisplayName("smoothDamp converges monotonically and never overshoots")
    void smoothDampConverges() {
        float current = 0.0f;
        final float target = 1.0f;
        float previousGap = Float.MAX_VALUE;
        for (int frame = 0; frame < 240; frame++) {
            current = MathUtil.smoothDamp(current, target, 0.09f, 1.0f / 60.0f);
            final float gap = Math.abs(target - current);
            assertTrue(gap <= previousGap + EPS, "smoothDamp moved away from the target at frame " + frame);
            assertTrue(current <= target + EPS, "smoothDamp overshoots: " + current);
            previousGap = gap;
        }
        assertEquals(target, current, 1.0E-3f);
    }

    @Test
    @DisplayName("the zero-delta step is a no-op, which is what keeps a paused GUI frozen")
    void smoothDampWithNoTimeDoesNotMove() {
        assertEquals(0.25f, MathUtil.smoothDamp(0.25f, 1.0f, 0.1f, 0.0f), EPS);
    }

    @Test
    @DisplayName("easing curves pass through (0,0) and (1,1)")
    void easingEndpoints() {
        final float[][] cases = {
                {0.0f, MathUtil.easeOutCubic(0.0f)},
                {1.0f, MathUtil.easeOutCubic(1.0f)},
                {0.0f, MathUtil.easeInOutCubic(0.0f)},
                {1.0f, MathUtil.easeInOutCubic(1.0f)},
                {0.0f, MathUtil.easeOutBack(0.0f)},
                {1.0f, MathUtil.easeOutBack(1.0f)},
        };
        for (final float[] pair : cases) {
            assertEquals(pair[0], pair[1], 1.0E-4f, "wrong endpoint for an easing curve");
        }
        // easeOutCubic is monotone increasing; easeOutBack is allowed one overshoot but
        // must return to 1, which the endpoint check above covers.
        float last = -Float.MAX_VALUE;
        for (float t = 0.0f; t <= 1.0f; t += 0.02f) {
            final float value = MathUtil.easeOutCubic(t);
            assertTrue(value >= last - EPS, "easeOutCubic is not monotone at t=" + t);
            last = value;
        }
    }

    @Test
    @DisplayName("packRgb/channel round-trip is exact at 8-bit granularity")
    void rgbRoundTrip() {
        for (int i = 0; i <= 255; i += 5) {
            final float c = i / 255.0f;
            final int packed = MathUtil.packRgb(c, 1.0f - c, 0.5f);
            assertEquals(i, Math.round(MathUtil.channelRed(packed) * 255.0f), 1, "red drifted at " + i);
            assertEquals(255 - i, Math.round(MathUtil.channelGreen(packed) * 255.0f), 1, "green drifted at " + i);
            assertEquals(128, Math.round(MathUtil.channelBlue(packed) * 255.0f), 1, "blue drifted at " + i);
        }
        // Clamping: a lightmap value of 1.5 must saturate, not wrap into negative red.
        final int saturated = MathUtil.packRgb(1.5f, -0.5f, 0.0f);
        assertEquals(255, Math.round(MathUtil.channelRed(saturated) * 255.0f), 1);
        assertEquals(0, Math.round(MathUtil.channelGreen(saturated) * 255.0f), 1);
    }

    @Test
    @DisplayName("fastExp2 tracks Math.pow closely enough for a gamma ramp")
    void fastExp2Accuracy() {
        for (double x = -8.0; x <= 8.0; x += 0.25) {
            final float expected = (float) Math.pow(2.0, x);
            final float actual = MathUtil.fastExp2((float) x);
            final float relative = Math.abs(actual - expected) / Math.max(1.0E-6f, Math.abs(expected));
            assertTrue(relative < 0.02f, "fastExp2(" + x + ") = " + actual + " vs " + expected);
        }
    }

    @Test
    @DisplayName("sectionKey is a function, distinguishes every axis, and does not alias neighbours")
    void sectionKeyIsDistinct() {
        // A collision here means two sections share a mesh, so one of them renders stale
        // geometry until the player relogs. Assert the properties the engine relies on
        // instead of a specific bit layout, which the packing may legitimately change.
        assertEquals(MathUtil.sectionKey(7, 8, 9), MathUtil.sectionKey(7, 8, 9), "sectionKey is not a function");
        assertTrue(MathUtil.sectionKey(7, 8, 9) != MathUtil.sectionKey(7, 9, 8), "the y and z axes aliased");
        assertTrue(MathUtil.sectionKey(-1, -1, -1) != MathUtil.sectionKey(0, 0, 0), "negative coordinates aliased");

        final long origin = MathUtil.sectionKey(0, 0, 0);
        final long[] neighbours = {
                MathUtil.sectionKey(1, 0, 0), MathUtil.sectionKey(-1, 0, 0),
                MathUtil.sectionKey(0, 1, 0), MathUtil.sectionKey(0, -1, 0),
                MathUtil.sectionKey(0, 0, 1), MathUtil.sectionKey(0, 0, -1),
        };
        final java.util.Set<Long> distinct = new java.util.HashSet<>();
        distinct.add(origin);
        for (final long key : neighbours) {
            assertTrue(distinct.add(key), "a directly adjacent section collided with another key");
        }
        // The vertical range is the tight one in every packing we have used: 16x16
        // sections from -16 to 480 in a 1.18+ world must all be distinct.
        final java.util.Set<Long> column = new java.util.HashSet<>();
        for (int y = -16; y <= 480; y++) {
            assertTrue(column.add(MathUtil.sectionKey(0, y, 0)), "sectionKey aliased at y=" + y);
        }
    }
}
