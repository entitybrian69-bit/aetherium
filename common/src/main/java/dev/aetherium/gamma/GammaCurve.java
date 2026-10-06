package dev.aetherium.gamma;

/** Monotone cubic Hermite curve through 5 user control points; maps a 0..1 light level to 0..1 brightness. */
public final class GammaCurve {
    private final double[] y = new double[5];
    private final double[] m = new double[5];

    public GammaCurve(double[] points) { set(points); }

    public void set(double[] points) {
        for (int i = 0; i < 5; i++) y[i] = Math.max(0, Math.min(1, points[i]));
        // Fritsch–Carlson tangents keep the interpolant monotone (no brightness inversions).
        double[] d = new double[4];
        for (int i = 0; i < 4; i++) d[i] = (y[i + 1] - y[i]) * 4.0;
        m[0] = d[0]; m[4] = d[3];
        for (int i = 1; i < 4; i++) m[i] = (d[i - 1] * d[i] <= 0) ? 0 : (d[i - 1] + d[i]) * 0.5;
        for (int i = 0; i < 4; i++) {
            if (d[i] == 0) { m[i] = 0; m[i + 1] = 0; continue; }
            double a = m[i] / d[i], b = m[i + 1] / d[i];
            double s = a * a + b * b;
            if (s > 9) { double t = 3 / Math.sqrt(s); m[i] = t * a * d[i]; m[i + 1] = t * b * d[i]; }
        }
    }

    public double evaluate(double t) {
        t = Math.max(0, Math.min(1, t));
        double x = t * 4.0;
        int i = Math.min(3, (int) x);
        double u = x - i;
        double h = 0.25;
        double h00 = 2 * u * u * u - 3 * u * u + 1, h10 = u * u * u - 2 * u * u + u;
        double h01 = -2 * u * u * u + 3 * u * u, h11 = u * u * u - u * u;
        return Math.max(0, Math.min(1, h00 * y[i] + h10 * h * m[i] + h01 * y[i + 1] + h11 * h * m[i + 1]));
    }
}
