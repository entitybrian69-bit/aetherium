package dev.aetherium.light;

import dev.aetherium.config.AetheriumConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Game-agnostic dynamic light core: source registry, spatial hashing into 16^3 cells, light-level and color
 * sampling, and dirty-section tracking. The per-version client layer feeds entity/item positions in and hooks
 * {@code LevelRenderer.getLightColor} / section rebuild scheduling via the callbacks exposed here.
 */
public final class DynamicLightEngine {
    private static final DynamicLightEngine INSTANCE = new DynamicLightEngine();

    private final ConcurrentHashMap<Integer, DynamicLightSource> sources = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, List<DynamicLightSource>> cells = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Boolean> dirtySections = new ConcurrentHashMap<>();
    private volatile boolean enabled = true;
    private volatile AetheriumConfig.DynamicLightQuality quality = AetheriumConfig.DynamicLightQuality.FANCY;
    private volatile boolean colored = true;
    private volatile boolean entities = true, items = true, self = true;
    private long lastUpdateNanos;

    private DynamicLightEngine() {}

    public static DynamicLightEngine get() { return INSTANCE; }

    public void applyConfig(AetheriumConfig cfg) {
        boolean wasEnabled = enabled;
        enabled = cfg.utilities.dynamicLights;
        quality = cfg.utilities.dynamicLightQuality;
        colored = cfg.utilities.coloredDynamicLights;
        entities = cfg.utilities.dynamicLightsEntities;
        items = cfg.utilities.dynamicLightsItems;
        self = cfg.utilities.dynamicLightsSelf;
        if (wasEnabled && !enabled) clearAll();
    }

    public boolean isEnabled() { return enabled; }
    public boolean isColored() { return colored; }
    public boolean lightsEntities() { return entities; }
    public boolean lightsItems() { return items; }
    public boolean lightsSelf() { return self; }

    /** Minimum interval between source re-evaluations, in nanoseconds, per quality tier. */
    public long updateIntervalNanos() {
        return switch (quality) {
            case FASTEST -> 500_000_000L;
            case FAST -> 200_000_000L;
            case FANCY -> 50_000_000L;
            case REALTIME -> 0L;
        };
    }

    public boolean shouldUpdate(long nowNanos) {
        if (nowNanos - lastUpdateNanos < updateIntervalNanos()) return false;
        lastUpdateNanos = nowNanos;
        return true;
    }

    // ---------------------------------------------------------------- source management

    public DynamicLightSource obtain(int id) { return sources.computeIfAbsent(id, DynamicLightSource::new); }

    /** Update (or insert) a source; marks the sections it touched before and after as dirty. */
    public void update(int id, double x, double y, double z, int luminance, int color) {
        if (!enabled) return;
        DynamicLightSource s = obtain(id);
        if (luminance <= 0) { remove(id); return; }
        s.x = x; s.y = y; s.z = z; s.luminance = Math.min(15, luminance); s.color = colored ? color : 0xFFFFFF;
        if (!s.dirty && !s.moved() && cellOf(s) != null) return;
        reindex(s);
        markDirtyAround(s.prevX, s.prevY, s.prevZ, s.prevLuminance);
        markDirtyAround(s.x, s.y, s.z, s.luminance);
        s.commit();
    }

    public void remove(int id) {
        DynamicLightSource s = sources.remove(id);
        if (s == null) return;
        unindex(s);
        markDirtyAround(s.x, s.y, s.z, Math.max(s.luminance, s.prevLuminance));
    }

    public void clearAll() {
        for (DynamicLightSource s : sources.values()) markDirtyAround(s.x, s.y, s.z, Math.max(s.luminance, s.prevLuminance));
        sources.clear();
        cells.clear();
    }

    public int sourceCount() { return sources.size(); }

    // ---------------------------------------------------------------- sampling

    /** Dynamic block-light contribution (0..15, may be fractional for smooth falloff) at a block position. */
    public double sampleLevel(int bx, int by, int bz) {
        if (!enabled || sources.isEmpty()) return 0;
        double best = 0;
        int cx = bx >> 4, cy = by >> 4, cz = bz >> 4;
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
            List<DynamicLightSource> list = cells.get(cellKey(cx + dx, cy + dy, cz + dz));
            if (list == null) continue;
            for (DynamicLightSource s : list) {
                double ddx = s.x - (bx + 0.5), ddy = s.y - (by + 0.5), ddz = s.z - (bz + 0.5);
                double dist = Math.sqrt(ddx * ddx + ddy * ddy + ddz * ddz);
                double v = s.luminance - dist;
                if (v > best) best = v;
            }
        }
        return Math.min(15, best);
    }

    /** Blended RGB (0xRRGGBB) of nearby sources weighted by their contribution; white when none. */
    public int sampleColor(int bx, int by, int bz) {
        if (!enabled || !colored || sources.isEmpty()) return 0xFFFFFF;
        double r = 0, g = 0, b = 0, w = 0;
        int cx = bx >> 4, cy = by >> 4, cz = bz >> 4;
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
            List<DynamicLightSource> list = cells.get(cellKey(cx + dx, cy + dy, cz + dz));
            if (list == null) continue;
            for (DynamicLightSource s : list) {
                double ddx = s.x - (bx + 0.5), ddy = s.y - (by + 0.5), ddz = s.z - (bz + 0.5);
                double v = s.luminance - Math.sqrt(ddx * ddx + ddy * ddy + ddz * ddz);
                if (v <= 0) continue;
                r += ((s.color >> 16) & 0xFF) * v; g += ((s.color >> 8) & 0xFF) * v; b += (s.color & 0xFF) * v; w += v;
            }
        }
        if (w <= 0) return 0xFFFFFF;
        return (clamp255(r / w) << 16) | (clamp255(g / w) << 8) | clamp255(b / w);
    }

    /** Pop dirty section keys (packed as {@link #sectionKey}) and hand them to the renderer. */
    public void flushDirtySections(Consumer<Long> rebuild) {
        if (dirtySections.isEmpty()) return;
        List<Long> keys = new ArrayList<>(dirtySections.keySet());
        dirtySections.clear();
        for (Long k : keys) rebuild.accept(k);
    }

    // ---------------------------------------------------------------- internals

    private void markDirtyAround(double x, double y, double z, int luminance) {
        if (luminance <= 0) return;
        int r = luminance;
        int minX = ((int) Math.floor(x - r)) >> 4, maxX = ((int) Math.floor(x + r)) >> 4;
        int minY = ((int) Math.floor(y - r)) >> 4, maxY = ((int) Math.floor(y + r)) >> 4;
        int minZ = ((int) Math.floor(z - r)) >> 4, maxZ = ((int) Math.floor(z + r)) >> 4;
        for (int sx = minX; sx <= maxX; sx++) for (int sy = minY; sy <= maxY; sy++) for (int sz = minZ; sz <= maxZ; sz++)
            dirtySections.put(sectionKey(sx, sy, sz), Boolean.TRUE);
    }

    private Long cellOf(DynamicLightSource s) {
        long key = cellKey((int) Math.floor(s.prevX) >> 4, (int) Math.floor(s.prevY) >> 4, (int) Math.floor(s.prevZ) >> 4);
        List<DynamicLightSource> l = cells.get(key);
        return l != null && l.contains(s) ? key : null;
    }

    private void reindex(DynamicLightSource s) {
        unindex(s);
        long key = cellKey((int) Math.floor(s.x) >> 4, (int) Math.floor(s.y) >> 4, (int) Math.floor(s.z) >> 4);
        cells.computeIfAbsent(key, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(s);
    }

    private void unindex(DynamicLightSource s) {
        long key = cellKey((int) Math.floor(s.prevX) >> 4, (int) Math.floor(s.prevY) >> 4, (int) Math.floor(s.prevZ) >> 4);
        List<DynamicLightSource> l = cells.get(key);
        if (l != null) { l.remove(s); if (l.isEmpty()) cells.remove(key, l); }
    }

    public static long sectionKey(int x, int y, int z) {
        return ((long) (x & 0x3FFFFF) << 42) | ((long) (y & 0xFFFFF) << 22) | (z & 0x3FFFFF);
    }
    public static int sectionX(long k) { return (int) (k >> 42); }
    public static int sectionY(long k) { return (int) (k << 22 >> 44); }
    public static int sectionZ(long k) { return (int) (k << 42 >> 42); }

    private static long cellKey(int x, int y, int z) { return sectionKey(x, y, z); }
    private static int clamp255(double v) { return (int) Math.max(0, Math.min(255, Math.round(v))); }
}
