package com.aetherium.gamma;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.aetherium.config.AetheriumConfig;
import com.aetherium.util.AetheriumLog;
import com.aetherium.util.MathUtil;

/**
 * Gamma utilities: full brightness, cave vision, night vision, time-based gamma,
 * and a custom monotone gamma curve.
 *
 * <p>All five features reduce to one function: a mapping from the lightmap's
 * input level (0..1 per channel, block and sky combined) to an output value.
 * Vanilla computes {@code (level/15)^2}-ish responses per dimension; Aetherium
 * replaces that response with {@link GammaCurve} and applies it at the tail of
 * {@code LightTexture}'s update, after vanilla has written its own pixels. That
 * placement is deliberate: it means the feature works with any shader pack (Iris
 * writes its own lightmap and we post-process the result), with no Iris API, and
 * it costs one pass over 256 integers — under 200 ns.</p>
 *
 * <p>Thread-safety: {@code curve} is rebuilt only on the render thread (config
 * listener), and read by the mixin on the same thread. {@link #invalidate()} is
 * called from {@code BackendSelector} which may run during a swap, hence the
 * volatile reference to an immutable curve.</p>
 */
public final class GammaApplier {
    private static final AetheriumLog LOGGER = AetheriumLog.of(GammaApplier.class);

    /** Lightmap texture size in vanilla (16 block x 16 sky, packed 16x16 per row). */
    public static final int LIGHTMAP_SIZE = 256;

    private final AetheriumConfig config;

    /** Volatile: swapped by the render thread, read from the lightmap hook. */
    private volatile GammaCurve curve;
    private volatile float lastAmount = Float.NaN;
    private volatile float lastCaveFloor = Float.NaN;
    private volatile float lastNightFloor = Float.NaN;

    private int appliedFrames;
    private boolean active;
    private String parseError;

    public GammaApplier(final AetheriumConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.config.gammaCurve.addListener(text -> rebuildCurve(text));
        this.config.gammaEnabled.addListener(enabled -> {
            this.active = enabled;
            LOGGER.info("Gamma override {}", enabled ? "enabled" : "disabled");
        });
        rebuildCurve(this.config.gammaCurve.get());
        this.active = this.config.gammaEnabled.get();
    }

    /** Drops any cached curve so the next frame rebuilds it (used by hot-swap). */
    public void invalidate() {
        this.curve = null;
        this.lastAmount = Float.NaN;
    }

    public boolean isActive() {
        return this.active;
    }

    /**
     * Applies the response to a lightmap pixel array in place.
     *
     * @param pixels      the ABGR ints vanilla just wrote
     * @param skyLevel    0..15 skylight at the camera, for the cave-vision falloff
     * @param blockLevel  0..15 block light at the camera, likewise (a torch in hand
     *                    halves the cave lift instead of killing it)
     * @param dayPercent  0..1 world time fraction, for time-based gamma
     */
    public void applyLightmap(final int[] pixels, final int skyLevel, final int blockLevel, final float dayPercent) {
        if (pixels == null || pixels.length == 0) {
            return;
        }
        GammaCurve curve = this.curve;
        if (curve == null) {
            curve = rebuildCurve(this.config.gammaCurve.get());
        }
        if (curve == null) {
            return;
        }

        float amount = 1.0f;
        if (this.config.gammaEnabled.get()) {
            amount = this.config.gammaAmount.get().floatValue();
        } else if (this.config.caveVision.get() || this.config.nightVisionBoost.get() || this.config.timeBasedGamma.get()) {
            // Utilities on but the master gamma switch off: only the floors apply.
            amount = 1.0f;
        }

        float caveFloor = 0.0f;
        if (this.config.caveVision.get()) {
            // Cave vision must not wash out the surface: the floor is scaled by how
            // dark the camera's own surroundings are, so standing in daylight does
            // nothing and y=-58 does a lot.
            // Both light sources count: standing on a surface at noon gives 0 lift, and a
            // deep cave with a torch in hand gets a partial one rather than nothing, which
            // is the difference between "helpful" and "broken in mineshafts".
            final float darkness = MathUtil.clamp(1.0f - (skyLevel + blockLevel) / 30.0f, 0.0f, 1.0f);
            caveFloor = this.config.caveVisionFloor.get().floatValue() / 15.0f * darkness;
        }

        float nightFloor = 0.0f;
        if (this.config.nightVisionBoost.get()) {
            // Sky-light-only boost: the vanilla night sky sits near 0.05, and raising
            // only the sky channel keeps torches contrasty instead of washing the
            // whole world to flat grey.
            nightFloor = this.config.nightVisionLevel.get().floatValue() / 15.0f;
        }

        if (this.config.timeBasedGamma.get()) {
            final float nightTarget = this.config.gammaNightTarget.get().floatValue();
            // dayPercent 0.0 is sunrise-ish in vanilla (0 = day start); peak darkness
            // is 0.5. Triangular blend gives a smooth dawn/dusk ramp with no seam.
            final float darkness = 1.0f - Math.abs(dayPercent * 2.0f - 1.0f);
            amount = MathUtil.lerp(amount, nightTarget, darkness);
        }

        if (amount == this.lastAmount && caveFloor == this.lastCaveFloor && nightFloor == this.lastNightFloor) {
            // Unchanged since the last frame: vanilla's texture upload already holds
            // the right pixels, so skip the whole pass. This is why the feature costs
            // nothing when it is idle.
            return;
        }
        this.lastAmount = amount;
        this.lastCaveFloor = caveFloor;
        this.lastNightFloor = nightFloor;

        for (int i = 0; i < pixels.length; i++) {
            final int pixel = pixels[i];
            float red = MathUtil.channelRed(pixel);
            float green = MathUtil.channelGreen(pixel);
            float blue = MathUtil.channelBlue(pixel);

            // The lightmap's horizontal axis is block light and the vertical axis is
            // sky; index layout is (sky << 4) | block for the 256-entry array.
            final int blockLevelIndex = i & 15;
            final float blockFraction = blockLevelIndex / 15.0f;
            // The floor *raises the black point* rather than adding light. An additive
            // floor clips bright pixels to white and the whole world goes flat, which is
            // the standard complaint about "full brightness" mods. Block light gets the
            // cave floor, skylight gets the night floor, so torches and moonlight stay
            // distinguishable.
            final float floor = Math.max(caveFloor * blockFraction, nightFloor * (1.0f - blockFraction));

            red = Math.max(MathUtil.clamp(curve.evaluate(red) * amount, 0.0f, 1.0f), floor);
            green = Math.max(MathUtil.clamp(curve.evaluate(green) * amount, 0.0f, 1.0f), floor);
            blue = Math.max(MathUtil.clamp(curve.evaluate(blue) * amount, 0.0f, 1.0f), floor);

            pixels[i] = MathUtil.packRgb(red, green, blue);
        }
        this.appliedFrames++;
    }

    /**
     * Per-vertex light encoder for the Aetherium mesher path: raises the block
     * level to a floor before it is packed, which is how "full brightness" makes
     * freshly built geometry bright without a lightmap write.
     */
    public int applyToVertexLight(final int packedLight) {
        if (!this.active && !this.config.caveVision.get()) {
            return packedLight;
        }
        final int block = packedLight & 0xFFFF;
        int blockLevel = block >> 4;
        if (this.config.caveVision.get()) {
            final int floor = (int) Math.round(this.config.caveVisionFloor.get().doubleValue());
            blockLevel = Math.max(blockLevel, MathUtil.clamp(floor, 0, 15));
        }
        if (this.config.gammaEnabled.get() && this.config.gammaAmount.get() >= 4.0) {
            // "Full brightness" above 4x is a de-facto no-shadows mode; clamp to full
            // rather than producing a nonsensical 60-level light.
            blockLevel = 15;
        }
        return (packedLight & 0xFFFF0000) | (blockLevel << 4);
    }

    private GammaCurve rebuildCurve(final String spec) {
        try {
            final GammaCurve parsed = GammaCurve.parse(spec);
            this.curve = parsed;
            this.parseError = null;
            return parsed;
        } catch (final IllegalArgumentException error) {
            this.parseError = error.getMessage();
            this.curve = GammaCurve.identity();
            LOGGER.warn("Ignoring gamma curve '{}' ({}); using the identity response", spec, error.getMessage());
            return this.curve;
        }
    }

    public String getParseError() {
        return this.parseError;
    }

    public int getAppliedFrames() {
        return this.appliedFrames;
    }

    public String describeCurve() {
        final GammaCurve local = this.curve;
        return local == null ? "not built" : local.describe();
    }

    /**
     * Monotone piecewise-cubic response curve (Fritsch-Carlson).
     *
     * <p>Why monotone interpolation and not a plain cubic spline: a gamma curve
     * that overshoots between control points produces banding and, worse,
     * non-monotonic light, so two different block levels can render with the same
     * brightness and the terrain gets flat patches. Fritsch-Carlson guarantees
     * monotonicity for any increasing input by clamping the tangent at each
     * knot — this is the standard algorithm in scientific plotting and it is
     * four lines longer than Catmull-Rom.</p>
     */
    public static final class GammaCurve {
        private final float[] xs;
        private final float[] ys;
        private final float[] tangents;

        private GammaCurve(final float[] xs, final float[] ys, final float[] tangents) {
            this.xs = xs;
            this.ys = ys;
            this.tangents = tangents;
        }

        public static GammaCurve identity() {
            return new GammaCurve(new float[]{0.0f, 1.0f}, new float[]{0.0f, 1.0f}, new float[]{1.0f, 1.0f});
        }

        /**
         * Parses {@code "x:y,x:y,..."} with 2..32 points. Input need not be sorted;
         * x is clamped to [0,1] and duplicate x values collapse to the later entry,
         * which is what the GUI editor expects while a point is being dragged.
         */
        public static GammaCurve parse(final String spec) {
            if (spec == null || spec.trim().isEmpty()) {
                return identity();
            }
            final List<float[]> points = new ArrayList<>(8);
            for (final String token : spec.split("[,;\\s]+")) {
                if (token.trim().isEmpty()) {
                    continue;
                }
                final String[] pair = token.split("[:=]");
                if (pair.length != 2) {
                    throw new IllegalArgumentException("expected x:y but found '" + token + "'");
                }
                try {
                    final float x = Float.parseFloat(pair[0].trim());
                    final float y = Float.parseFloat(pair[1].trim());
                    if (!Float.isFinite(x) || !Float.isFinite(y)) {
                        throw new IllegalArgumentException("non-finite value in '" + token + "'");
                    }
                    points.add(new float[]{MathUtil.clamp(x, 0.0f, 1.0f), MathUtil.clamp(y, 0.0f, 4.0f)});
                } catch (final NumberFormatException error) {
                    throw new IllegalArgumentException("bad number in '" + token + "'", error);
                }
            }
            if (points.size() < 2) {
                throw new IllegalArgumentException("a gamma curve needs at least 2 points, got " + points.size());
            }
            if (points.size() > 32) {
                throw new IllegalArgumentException("a gamma curve is capped at 32 points, got " + points.size());
            }
            points.sort((a, b) -> Float.compare(a[0], b[0]));

            final float[] xs = new float[points.size()];
            final float[] ys = new float[points.size()];
            for (int i = 0; i < points.size(); i++) {
                xs[i] = points.get(i)[0];
                ys[i] = points.get(i)[1];
            }
            // Pin the endpoints: a curve that does not start at 0 lifts black to grey,
            // and one that does not end at 1 clips the brightest light.
            xs[0] = 0.0f;
            ys[0] = 0.0f;
            xs[xs.length - 1] = 1.0f;
            ys[ys.length - 1] = Math.max(ys[ys.length - 1], 1.0f);
            return new GammaCurve(xs, ys, fritschCarlsonTangents(xs, ys));
        }

        /** @return tangent at each knot, monotone-safe by construction */
        private static float[] fritschCarlsonTangents(final float[] xs, final float[] ys) {
            final int n = xs.length;
            final float[] delta = new float[n - 1];
            for (int i = 0; i < n - 1; i++) {
                final float dx = xs[i + 1] - xs[i];
                delta[i] = dx <= 1.0E-6f ? 0.0f : (ys[i + 1] - ys[i]) / dx;
            }
            final float[] tangents = new float[n];
            tangents[0] = delta[0];
            tangents[n - 1] = delta[n - 2];
            for (int i = 1; i < n - 1; i++) {
                if (delta[i - 1] * delta[i] <= 0.0f) {
                    tangents[i] = 0.0f;
                } else {
                    // The initial estimate is the mean of the two adjacent secants;
                    // the |alpha|,|beta| limiter below is what makes it Fritsch-Carlson
                    // rather than a Catmull-Rom spline that can overshoot.
                    tangents[i] = (delta[i - 1] + delta[i]) * 0.5f;
                }
            }
            for (int i = 0; i < n - 1; i++) {
                if (delta[i] == 0.0f) {
                    tangents[i] = 0.0f;
                    tangents[i + 1] = 0.0f;
                    continue;
                }
                final float alpha = tangents[i] / delta[i];
                final float beta = tangents[i + 1] / delta[i];
                final float squared = alpha * alpha + beta * beta;
                if (squared > 9.0f) {
                    final float scale = 3.0f / (float) Math.sqrt(squared);
                    tangents[i] = scale * alpha * delta[i];
                    tangents[i + 1] = scale * beta * delta[i];
                }
            }
            return tangents;
        }

        /** Hermite interpolation between the two knots bracketing {@code x}. */
        public float evaluate(final float x) {
            final float t = MathUtil.clamp(x, 0.0f, 1.0f);
            int low = 0;
            int high = this.xs.length - 1;
            while (high - low > 1) {
                final int mid = (low + high) >>> 1;
                if (this.xs[mid] <= t) {
                    low = mid;
                } else {
                    high = mid;
                }
            }
            final float x0 = this.xs[low];
            final float x1 = this.xs[high];
            final float span = x1 - x0;
            if (span <= 1.0E-6f) {
                return this.ys[low];
            }
            final float u = (t - x0) / span;
            final float u2 = u * u;
            final float u3 = u2 * u;
            final float h00 = 2.0f * u3 - 3.0f * u2 + 1.0f;
            final float h10 = u3 - 2.0f * u2 + u;
            final float h01 = -2.0f * u3 + 3.0f * u2;
            final float h11 = u3 - u2;
            final float value = h00 * this.ys[low] + h10 * span * this.tangents[low]
                    + h01 * this.ys[high] + h11 * span * this.tangents[high];
            return MathUtil.clamp(value, 0.0f, 4.0f);
        }

        public int getPointCount() {
            return this.xs.length;
        }

        public float[] getControlPoints() {
            final float[] out = new float[this.xs.length * 2];
            for (int i = 0; i < this.xs.length; i++) {
                out[i * 2] = this.xs[i];
                out[i * 2 + 1] = this.ys[i];
            }
            return out;
        }

        public String describe() {
            final StringBuilder builder = new StringBuilder(64);
            for (int i = 0; i < this.xs.length; i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(String.format(Locale.ROOT, "%.2f:%.2f", this.xs[i], this.ys[i]));
            }
            return builder.toString();
        }
    }
}
