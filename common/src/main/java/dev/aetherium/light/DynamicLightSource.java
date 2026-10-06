package dev.aetherium.light;

/** A moving point light. Coordinates are world-space doubles; {@code luminance} is 0..15 like block light. */
public final class DynamicLightSource {
    public final int id;
    public double x, y, z;
    public int luminance;
    /** 0xRRGGBB tint; white = 0xFFFFFF. */
    public int color = 0xFFFFFF;
    public double prevX, prevY, prevZ;
    public int prevLuminance;
    public boolean dirty;

    public DynamicLightSource(int id) { this.id = id; }

    public boolean moved() {
        return Math.abs(x - prevX) > 0.1 || Math.abs(y - prevY) > 0.1 || Math.abs(z - prevZ) > 0.1 || luminance != prevLuminance;
    }

    public void commit() { prevX = x; prevY = y; prevZ = z; prevLuminance = luminance; dirty = false; }
}
