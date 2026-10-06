package dev.aetherium.gui.anim;

import java.util.Random;

/**
 * Lightweight 2D glow particles for the menu "morph" transition. Pure math; the screen draws each particle as a
 * soft square. Call {@link #burst} on open / tab switch and {@link #update} once per frame.
 */
public final class ParticleField {
    public static final int MAX = 160;

    public final float[] x = new float[MAX], y = new float[MAX], vx = new float[MAX], vy = new float[MAX];
    public final float[] life = new float[MAX], maxLife = new float[MAX], size = new float[MAX];
    private final Random rng = new Random();
    private int count;
    private long lastMs;

    public void burst(float cx, float cy, float spreadX, float spreadY, int n) {
        for (int i = 0; i < n && count < MAX; i++, count++) {
            x[count] = cx + (rng.nextFloat() - 0.5f) * spreadX;
            y[count] = cy + (rng.nextFloat() - 0.5f) * spreadY;
            double a = rng.nextDouble() * Math.PI * 2;
            float sp = 8f + rng.nextFloat() * 40f;
            vx[count] = (float) Math.cos(a) * sp;
            vy[count] = (float) Math.sin(a) * sp - 12f;
            maxLife[count] = life[count] = 500f + rng.nextFloat() * 900f;
            size[count] = 1f + rng.nextFloat() * 2.5f;
        }
    }

    /** Ambient drift so the panel never looks static. */
    public void ambient(float w, float h) {
        if (count < 24 && rng.nextFloat() < 0.35f && count < MAX) {
            x[count] = rng.nextFloat() * w;
            y[count] = h + 4;
            vx[count] = (rng.nextFloat() - 0.5f) * 6f;
            vy[count] = -(6f + rng.nextFloat() * 14f);
            maxLife[count] = life[count] = 2500f + rng.nextFloat() * 2500f;
            size[count] = 0.8f + rng.nextFloat() * 1.6f;
            count++;
        }
    }

    public void update() {
        long now = System.nanoTime() / 1_000_000L;
        float dt = lastMs == 0 ? 16f : Math.min(50f, now - lastMs);
        lastMs = now;
        float s = dt / 1000f;
        for (int i = 0; i < count; ) {
            life[i] -= dt;
            if (life[i] <= 0) { swapRemove(i); continue; }
            x[i] += vx[i] * s;
            y[i] += vy[i] * s;
            vx[i] *= 0.985f;
            vy[i] *= 0.985f;
            i++;
        }
    }

    private void swapRemove(int i) {
        int l = --count;
        x[i] = x[l]; y[i] = y[l]; vx[i] = vx[l]; vy[i] = vy[l]; life[i] = life[l]; maxLife[i] = maxLife[l]; size[i] = size[l];
    }

    public int count() { return count; }
    public float alpha(int i) { float t = life[i] / maxLife[i]; return t < 0.2f ? t / 0.2f : t; }
}
