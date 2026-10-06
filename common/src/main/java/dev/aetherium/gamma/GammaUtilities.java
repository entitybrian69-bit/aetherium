package dev.aetherium.gamma;

import dev.aetherium.config.AetheriumConfig;

/**
 * Lightmap post-processing shared by every version. The client layer gives us the 16x16 lightmap (block x sky) right
 * before upload and we rewrite it in place, so brightness, night vision, cave vision, time-of-day gamma and the
 * custom curve all apply live without touching Options.gamma and without a resource reload.
 */
public final class GammaUtilities {
    private static final GammaUtilities INSTANCE = new GammaUtilities();

    private volatile boolean enabled = true;
    private volatile double brightness = 1.0;
    private volatile boolean nightVision, caveVision, timeBased, useCurve;
    private final GammaCurve curve = new GammaCurve(new double[]{0, .25, .5, .75, 1});
    private volatile float smoothedNightVision;

    private GammaUtilities() {}

    public static GammaUtilities get() { return INSTANCE; }

    public void applyConfig(AetheriumConfig cfg) {
        enabled = cfg.utilities.gammaUtilities;
        brightness = cfg.utilities.brightness;
        nightVision = cfg.utilities.nightVision;
        caveVision = cfg.utilities.caveVision;
        timeBased = cfg.utilities.timeBasedGamma;
        useCurve = cfg.utilities.customGammaCurve;
        curve.set(cfg.utilities.gammaCurve);
    }

    public boolean isEnabled() { return enabled; }
    public boolean isNightVision() { return nightVision; }
    public boolean isCaveVision() { return caveVision; }
    public double brightness() { return brightness; }

    /** True when the vanilla lightmap can stay untouched. */
    public boolean isIdentity() {
        return !enabled || (brightness == 1.0 && !nightVision && !caveVision && !timeBased && !useCurve);
    }

    /**
     * Transform one lightmap texel.
     *
     * @param blockLevel      0..15 block light index of the texel
     * @param skyLevel        0..15 sky light index of the texel
     * @param rgb             vanilla colour, floats 0..1 (modified in place)
     * @param dayTime         0..24000 world time, used by time-based gamma
     * @param cameraSkyLight  sky light at the camera (0..15); cave vision triggers below 4
     * @param tickDelta       for smoothing toggles
     */
    public void transformTexel(int blockLevel, int skyLevel, float[] rgb, long dayTime, int cameraSkyLight, float tickDelta) {
        if (isIdentity()) return;

        double gain = brightness; // 1.0 == vanilla max slider
        if (timeBased) {
            // 0 at noon, 1 at midnight; lift night gamma up to +1.5x without washing out day.
            double t = ((dayTime % 24000L) / 24000.0 + 0.25) % 1.0;
            double night = 0.5 - 0.5 * Math.cos(t * 2 * Math.PI);
            gain *= 1.0 + 1.5 * night;
        }
        if (caveVision && cameraSkyLight < 4) gain *= 2.5;

        float nvTarget = nightVision ? 1f : 0f;
        smoothedNightVision += (nvTarget - smoothedNightVision) * Math.min(1f, 0.15f + tickDelta * 0.1f);

        float maxLevel = Math.max(blockLevel, skyLevel) / 15f;
        for (int i = 0; i < 3; i++) {
            double v = rgb[i];
            if (useCurve) {
                double lvl = curve.evaluate(maxLevel);
                v = Math.max(v, lvl);
            }
            // Vanilla's gamma: lerp(v, 1 - (1 - v)^4, gamma). Beyond 1.0 we extend with a power lift.
            double g = Math.min(1.0, gain);
            double inv = 1.0 - v;
            double lifted = 1.0 - inv * inv * inv * inv;
            v = v + (lifted - v) * g;
            if (gain > 1.0) v = Math.pow(v, 1.0 / (1.0 + (gain - 1.0) * 0.5));
            if (smoothedNightVision > 0f) {
                double nv = 1.0 - (1.0 - v) * 0.15;
                v = v + (nv - v) * smoothedNightVision;
            }
            rgb[i] = (float) Math.max(0.0, Math.min(1.0, v));
        }
    }

    /** Convenience: transform a packed 0xAABBGGRR texel (NativeImage ABGR layout). */
    public int transformPacked(int abgr, int blockLevel, int skyLevel, long dayTime, int cameraSkyLight, float tickDelta) {
        float[] rgb = {(abgr & 0xFF) / 255f, ((abgr >> 8) & 0xFF) / 255f, ((abgr >> 16) & 0xFF) / 255f};
        transformTexel(blockLevel, skyLevel, rgb, dayTime, cameraSkyLight, tickDelta);
        int r = Math.round(rgb[0] * 255), g = Math.round(rgb[1] * 255), b = Math.round(rgb[2] * 255);
        return (abgr & 0xFF000000) | (b << 16) | (g << 8) | r;
    }
}
