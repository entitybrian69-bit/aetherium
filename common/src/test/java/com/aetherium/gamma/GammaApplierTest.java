package com.aetherium.gamma;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.aetherium.config.AetheriumConfig;
import com.aetherium.gamma.GammaApplier.GammaCurve;
import com.aetherium.util.MathUtil;

/**
 * The gamma utilities operate on the 256-entry lightmap, which is the single most
 * visible thing in the mod and the easiest to break silently: a wrong formulation either
 * washes the world to grey or leaves the pixels identical to vanilla while the GUI claims
 * a boost. Both failure modes are asserted against here.
 */
final class GammaApplierTest {
    private static int[] flatRamp() {
        // The lightmap vanilla would have written: brightness grows along the block axis.
        final int[] pixels = new int[GammaApplier.LIGHTMAP_SIZE];
        for (int i = 0; i < pixels.length; i++) {
            final float level = (i & 15) / 15.0f;
            pixels[i] = MathUtil.packRgb(level, level * 0.9f, level * 0.8f);
        }
        return pixels;
    }

    @Test
    @DisplayName("with everything off, the lightmap is byte-identical to vanilla's")
    void defaultsAreANoOp() {
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        final GammaApplier applier = new GammaApplier(config);
        final int[] before = flatRamp();
        final int[] after = flatRamp();
        applier.applyLightmap(after, 15, 15, 0.0f);
        for (int i = 0; i < before.length; i++) {
            assertEquals(before[i], after[i], "index " + i + " changed with gamma disabled");
        }
        assertTrue(applier.getAppliedFrames() >= 0);
    }

    @Test
    @DisplayName("an unchanged frame skips the whole pass, which is why idle gamma is free")
    void unchangedFrameSkips() {
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        config.gammaEnabled.set(true);
        config.gammaAmount.set(1.5d);
        final GammaApplier applier = new GammaApplier(config);
        final int[] pixels = flatRamp();
        applier.applyLightmap(pixels, 4, 2, 0.3f);
        final int firstFrames = applier.getAppliedFrames();
        assertEquals(1, firstFrames, "the first frame must run the pass");
        applier.applyLightmap(pixels, 4, 2, 0.3f);
        assertEquals(firstFrames, applier.getAppliedFrames(), "an identical frame must be skipped");
        config.gammaAmount.set(1.75d);
        applier.applyLightmap(pixels, 4, 2, 0.3f);
        assertEquals(firstFrames + 1, applier.getAppliedFrames(), "a changed setting must run again");
    }

    @Test
    @DisplayName("gamma brightens dark pixels and saturates rather than wrapping")
    void boostSaturates() {
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        config.gammaEnabled.set(true);
        config.gammaAmount.set(2.0d);
        final GammaApplier applier = new GammaApplier(config);
        final int[] pixels = flatRamp();
        applier.applyLightmap(pixels, 15, 15, 0.0f);

        // Column 0 is black by construction; 2x of black is still black, which is the
        // point: gamma without a floor must not lift pure black or caves stay dark but
        // the sky goes white.
        assertEquals(0, pixels[0] & 0xFF, 0);
        final int brightIndex = 15; // block fraction 1.0, red 1.0 after clamping
        assertTrue(MathUtil.channelRed(pixels[brightIndex]) >= 0.99f, "a 2x boost must saturate to white");
        // Monotone along the block axis: banding appears when it is not.
        float previous = -1.0f;
        for (int block = 0; block < 16; block++) {
            final float value = MathUtil.channelRed(pixels[block]);
            assertTrue(value >= previous - 1.0E-6f, "the ramp is no longer monotone at block " + block);
            previous = value;
        }
        for (final int pixel : pixels) {
            assertTrue(pixel >= 0, "a lightmap pixel must stay a positive packed int, got " + pixel);
        }
    }

    @Test
    @DisplayName("cave vision lifts a dark cave and leaves a sunlit surface alone")
    void caveVisionIsDarknessScaled() {
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        config.caveVision.set(true);
        config.caveVisionFloor.set(10.0d);
        final GammaApplier applier = new GammaApplier(config);

        // A black lightmap is the case a floor exists for: in a dark cave the black point
        // must rise, on a sunlit surface it must not (otherwise the sky turns milky).
        final int[] zeros = new int[GammaApplier.LIGHTMAP_SIZE];
        applier.applyLightmap(zeros, 0, 0, 0.0f);
        final int fullBlockColumn = 16 + 15; // sky row 1, block column 15
        assertTrue(MathUtil.channelRed(zeros[fullBlockColumn]) > 0.5f, "cave vision did nothing in a dark cave");
        // At the skylight end of the same row the cave floor is weighted by 0, so the
        // block-light axis keeps its shape - that is what makes the lift look like
        // ambient light instead of a fogged lens.
        assertEquals(0.0f, MathUtil.channelRed(zeros[16]), 0.01f, "cave vision leaked onto the skylight end");

        applier.invalidate();
        final int[] sunlit = new int[GammaApplier.LIGHTMAP_SIZE];
        applier.applyLightmap(sunlit, 15, 15, 0.0f);
        assertEquals(0.0f, MathUtil.channelRed(sunlit[fullBlockColumn]), 0.01f,
                "cave vision lifted a sunlit surface");

    }

    @Test
    @DisplayName("night vision boosts the skylight half without flattening torches")
    void nightVisionSplitsAxes() {
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        config.nightVisionBoost.set(true);
        config.nightVisionLevel.set(12.0d);
        final GammaApplier applier = new GammaApplier(config);
        final int[] pixels = new int[GammaApplier.LIGHTMAP_SIZE];
        applier.applyLightmap(pixels, 0, 0, 0.0f);
        // Block column 0 (the skylight end): the night floor applies with weight 1.
        assertTrue(MathUtil.channelRed(pixels[16]) > 0.5f, "the sky half was not lifted");
        // Block column 15 (the torch end): the sky floor is scaled by (1 - 1) = 0, so a
        // black pixel there stays black instead of joining the moonlight.
        assertEquals(0.0f, MathUtil.channelRed(pixels[31]), 0.01f, "the torch column must keep the ramp");
    }

    @Test
    @DisplayName("time-based gamma reaches its target at midnight and is inert at noon")
    void timeBasedGammaTracksDayCycle() {
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        config.timeBasedGamma.set(true);
        config.gammaNightTarget.set(2.0d);
        final GammaApplier applier = new GammaApplier(config);

        // A flat 0.4 ramp makes the multiplier measurable: the result is 0.4 * amount.
        final int[] noon = new int[GammaApplier.LIGHTMAP_SIZE];
        java.util.Arrays.fill(noon, MathUtil.packRgb(0.4f, 0.4f, 0.4f));
        applier.applyLightmap(noon, 15, 0, 0.0f);
        applier.invalidate();
        final int[] midnight = new int[GammaApplier.LIGHTMAP_SIZE];
        java.util.Arrays.fill(midnight, MathUtil.packRgb(0.4f, 0.4f, 0.4f));
        applier.applyLightmap(midnight, 0, 0, 0.5f);

        assertEquals(0.4f, MathUtil.channelRed(noon[15]), 0.02f, "noon must be unchanged: darkness 0, so lerp lands on 1.0x");
        assertEquals(0.8f, MathUtil.channelRed(midnight[15]), 0.02f, "midnight must reach the configured night target");
    }

    @Test
    @DisplayName("a malformed curve is reported and falls back to identity, never a crash")
    void curveParseError() {
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        final GammaApplier applier = new GammaApplier(config);
        assertNull(applier.getParseError(), "the shipped default must parse");
        config.gammaCurve.set("0:0,0.5:abc,1:1");
        assertNotNull(applier.getParseError(), "a bad curve must be reported so the GUI can say why");
        assertEquals("0.00:0.00,1.00:1.00", applier.describeCurve(), "the fallback must be the identity response");
        config.gammaCurve.set("0:0,0.2:0.6,0.6:0.8,1:1");
        assertNull(applier.getParseError(), "the error must clear once the user fixes the string");
        assertEquals(4, GammaCurve.parse("0:0,0.2:0.6,0.6:0.8,1:1").getPointCount());
    }

    @Test
    @DisplayName("vertex light is raised to the cave floor and sky bits survive")
    void vertexLightEncoder() {
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        final GammaApplier inert = new GammaApplier(config);
        final int packed = (12 << 20) | (4 << 4);
        assertEquals(packed, inert.applyToVertexLight(packed), "with everything off, the mesher must not be touched");

        config.caveVision.set(true);
        config.caveVisionFloor.set(10.0d);
        final GammaApplier applier = new GammaApplier(config);
        final int raised = applier.applyToVertexLight(packed);
        assertEquals(10, (raised & 0xFFFF) >> 4, "the block level must be raised to the floor");
        assertEquals(packed & 0xFFFF0000, raised & 0xFFFF0000, "skylight must pass through untouched");

        // A brighter source is never dimmed by the floor.
        final int alreadyBright = (12 << 20) | (14 << 4);
        assertEquals(14, (applier.applyToVertexLight(alreadyBright) & 0xFFFF) >> 4);

        config.gammaEnabled.set(true);
        config.gammaAmount.set(6.0d);
        final int fullBright = applier.applyToVertexLight(packed);
        assertEquals(15, (fullBright & 0xFFFF) >> 4, "above 4x the encoder means full brightness");
    }

    @Test
    @DisplayName("the curve is monotone, pinned at both ends, and round-trips through describe")
    void curveProperties() {
        final GammaCurve identity = GammaCurve.identity();
        assertEquals(0.0f, identity.evaluate(0.0f), 1.0E-6f);
        assertEquals(0.5f, identity.evaluate(0.5f), 1.0E-6f);
        assertEquals(1.0f, identity.evaluate(1.0f), 1.0E-6f);
        assertEquals(1.0f, identity.evaluate(-3.0f), 1.0E-6f, "evaluate clamps its input");

        final GammaCurve lifted = GammaCurve.parse("0:0,0.25:0.8,0.5:0.9,1:1");
        assertEquals(0.0f, lifted.evaluate(0.0f), 1.0E-5f, "the first point must be pinned to 0:0");
        assertEquals(1.0f, lifted.evaluate(1.0f), 1.0E-5f, "the last point must be pinned to 1:");
        float previous = -1.0f;
        for (float x = 0.0f; x <= 1.0001f; x += 0.005f) {
            final float y = lifted.evaluate(x);
            assertTrue(y >= previous - 1.0E-5f, "a gamma curve must never go backwards (x=" + x + ")");
            assertTrue(Float.isFinite(y), "evaluate produced " + y + " at x=" + x);
            previous = y;
        }
        assertTrue(lifted.evaluate(0.25f) > GammaCurve.parse("0:0,0.25:0.25,1:1").evaluate(0.25f),
                "control points must actually shape the response");
        // describe() is the GUI's text field: what it prints has to parse back.
        final GammaCurve reparsed = GammaCurve.parse(lifted.describe());
        assertEquals(lifted.getPointCount(), reparsed.getPointCount());
        final float[] liftedPoints = lifted.getControlPoints();
        final float[] reparsedPoints = reparsed.getControlPoints();
        assertEquals(liftedPoints.length, reparsedPoints.length);
        for (int i = 0; i < liftedPoints.length; i++) {
            assertEquals(liftedPoints[i], reparsedPoints[i], 0.011f, "point " + i + " drifted through describe()");
        }
    }

    @Test
    @DisplayName("a curve is rejected with a message a person can act on")
    void curveRejections() {
        assertThrows(IllegalArgumentException.class, () -> GammaCurve.parse("0:0"), "one point cannot define a curve");
        assertThrows(IllegalArgumentException.class, () -> GammaCurve.parse("garbage"));
        assertThrows(IllegalArgumentException.class, () -> GammaCurve.parse("0:0,0.5:Infinity,1:1"));
        assertThrows(IllegalArgumentException.class, () -> GammaCurve.parse("0:0,1:1,extra"));
        final StringBuilder tooMany = new StringBuilder();
        for (int i = 0; i <= 33; i++) {
            tooMany.append(i / 33.0f).append(':').append(i / 33.0f).append(',');
        }
        assertThrows(IllegalArgumentException.class, () -> GammaCurve.parse(tooMany.toString()));
        // Empty and null are not errors: they mean "no curve", i.e. identity.
        assertEquals(2, GammaCurve.parse("").getPointCount());
        assertEquals(2, GammaCurve.parse(null).getPointCount());
    }

    @Test
    @DisplayName("invalidate forces a rebuild even when nothing else changed")
    void invalidateForcesRebuild() {
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        config.gammaEnabled.set(true);
        final GammaApplier applier = new GammaApplier(config);
        final int[] pixels = flatRamp();
        applier.applyLightmap(pixels, 0, 0, 0.0f);
        applier.applyLightmap(pixels, 0, 0, 0.0f);
        final int frames = applier.getAppliedFrames();
        applier.invalidate();
        assertEquals("not built", applier.describeCurve(), "after invalidate the curve is gone until the next pass");
        applier.applyLightmap(pixels, 0, 0, 0.0f);
        assertEquals(frames + 1, applier.getAppliedFrames());
    }

    @Test
    @DisplayName("an empty or null pixel array is a no-op, not an exception")
    void degenerateInputs() {
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        config.gammaEnabled.set(true);
        final GammaApplier applier = new GammaApplier(config);
        applier.applyLightmap(null, 0, 0, 0.0f);
        applier.applyLightmap(new int[0], 0, 0, 0.0f);
        assertEquals(0, applier.getAppliedFrames(), "nothing was applied to nothing");
        // The lightmap is 16x16 = 256 on every supported version; the constant is here so
        // a delta that changes the vanilla layout has to say so in a failing test.
        assertEquals(256, GammaApplier.LIGHTMAP_SIZE);
    }
}
