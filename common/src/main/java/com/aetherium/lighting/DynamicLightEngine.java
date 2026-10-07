package com.aetherium.lighting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.aetherium.android.AndroidEnvironment;
import com.aetherium.config.AetheriumConfig;
import com.aetherium.util.AetheriumLog;
import com.aetherium.util.MathUtil;

/**
 * Coloured, entity/item-based dynamic lights with real-time propagation.
 *
 * <p>Model: Aetherium does <b>not</b> write into the world's light engine. It
 * keeps a shadow layer — added block light per position — and answers
 * {@link #sampleBlockLight} for it. The Aetherium mesher adds that value to the
 * sampled vanilla level before encoding the light vertex attribute, which is
 * where the visible effect comes from. In Compatibility mode the same values
 * drive the halo pass ({@code client.DynamicLightHaloRenderer}) plus a lightmap
 * floor lift, because writing per-vertex light into vanilla's already-built
 * meshes is not possible without rebuilding them — that trade-off is documented
 * in README.md ("Honest status") rather than hidden.</p>
 *
 * <p>Propagation is the second half of the design. A light is a list of
 * (position, level, colour) contributions computed by a Chebyshev falloff with a
 * DDA occlusion walk, produced lazily into a per-section bucket and cached until
 * something changes. Full BFS through the voxel grid would be more correct and
 * about 30x more expensive per frame; with attenuation over a 14-block radius the
 * visible difference is confined to corners, and no one remeshes in response to a
 * corner.</p>
 *
 * <p>Thread-safety: sources are registered from the client tick thread and read
 * from mesh workers. The {@code sources} list is swapped wholesale (copy-on-write
 * via a volatile reference), so readers never see a half-mutated list and no lock
 * appears on the sampling path. The per-frame caches are render-thread only.</p>
 */
public final class DynamicLightEngine {
    private static final AetheriumLog LOGGER = AetheriumLog.of(DynamicLightEngine.class);
    private static final int MAX_SOURCES = 512;
    private static final int SECTION_SHIFT = 4;

    /** Why a light exists; affects decay and whether it re-propagates every tick. */
    public enum Kind {
        /** Held item or equipped light: moves every frame, cheap path. */
        HELD,
        /** Placed block entity (lantern, torch, campfire): static, cached hard. */
        BLOCK,
        /** Entity carrying a light source (bee, blaze, item entity). */
        ENTITY,
        /** Player-applied effect light (potion glow, spell). */
        EFFECT
    }

    /** One light. Deliberately a mutable pooled struct: 400 of these per frame. */
    public static final class LightSource {
        /** Position in block coordinates. */
        public int x;
        public int y;
        public int z;
        /** Emitted vanilla light level, 0..15. */
        public float level;
        /** Linear-space colour, 0..1 each. */
        public float red;
        public float green;
        public float blue;
        public Kind kind;
        /** Identity for de-duplication (entity id, or block-position hash). */
        public long key;
        /** Frame stamp; sources not refreshed this tick expire. */
        public int stamp;
        public boolean used;

        LightSource() {
        }

        void set(final long key, final int x, final int y, final int z, final float level,
                 final float red, final float green, final float blue, final Kind kind, final int stamp) {
            this.key = key;
            this.x = x;
            this.y = y;
            this.z = z;
            this.level = MathUtil.clamp(level, 0.0f, 15.0f);
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.kind = kind;
            this.stamp = stamp;
            this.used = true;
        }

        @Override
        public String toString() {
            return String.format(java.util.Locale.ROOT, "light@%d,%d,%d lvl=%.1f rgb=(%.2f,%.2f,%.2f) %s",
                    this.x, this.y, this.z, this.level, this.red, this.green, this.blue, this.kind);
        }
    }

    /** Cached contribution of one light to one section, keyed per block. */
    private static final class Contribution {
        int level;
        float red;
        float green;
        float blue;
    }

    private final AetheriumConfig config;
    private final AndroidEnvironment android;

    /** Copy-on-write: swapped by the client tick thread, read by mesh workers. */
    private volatile LightSource[] active = new LightSource[0];
    /** Pool the active array is drawn from; touched only by the registering thread. */
    private final List<LightSource> pool = new ArrayList<>(128);
    private final Map<Long, Integer> indexByKey = new HashMap<>(256);

    /** Render-thread-only caches. key = section cube + block offset. */
    private final Map<Long, Contribution> perBlock = new HashMap<>(4096);
    /** Sections whose added light changed enough to justify a re-mesh. */
    private final List<long[]> dirtySections = new ArrayList<>(32);

    private int stamp = 1;
    private int frame;
    private int quality;
    private int radius;
    private float intensity;
    private int samplesThisFrame;
    private int occlusionTestsThisFrame;
    private int lightsSeen;
    private boolean occlusionEnabled;

    public DynamicLightEngine(final AetheriumConfig config, final AndroidEnvironment android) {
        this.config = Objects.requireNonNull(config, "config");
        this.android = android;
        this.quality = config.dynamicLightsQuality.get();
        this.radius = config.dynamicLightsRange.get();
        this.intensity = config.dynamicLightsIntensity.get().floatValue();
        this.occlusionEnabled = this.quality >= 3;
    }

    /** Clears everything (world change, disable, backend swap). */
    public void clear() {
        synchronized (this.pool) {
            for (final LightSource source : this.pool) {
                source.used = false;
            }
            this.pool.clear();
            this.indexByKey.clear();
            this.active = new LightSource[0];
        }
        this.perBlock.clear();
        this.dirtySections.clear();
        LOGGER.dev("Dynamic light engine cleared");
    }

    /** Drops derived caches; keeps sources. Called by {@code BackendSelector.reapply}. */
    public void invalidateCache() {
        this.perBlock.clear();
        this.quality = this.config.dynamicLightsQuality.get();
        this.radius = this.config.dynamicLightsRange.get();
        this.intensity = this.config.dynamicLightsIntensity.get().floatValue();
        this.occlusionEnabled = this.quality >= 3;
    }

    public boolean isEnabled() {
        return this.config.dynamicLights.get() && this.quality > 0;
    }

    /**
     * Begins a registration pass. Call before {@link #addLight} each tick so that
     * lights that stopped being reported expire automatically instead of leaking
     * forever (the common bug in naive implementations that never remove a light
     * when the player drops the torch).
     */
    public void beginFrame() {
        this.frame++;
        synchronized (this.pool) {
            for (final LightSource source : this.pool) {
                source.used = false;
            }
            this.indexByKey.clear();
        }
        this.samplesThisFrame = 0;
        this.occlusionTestsThisFrame = 0;
    }

    /**
     * Adds or refreshes one light for this tick.
     *
     * @param key identity — reuse the same key to move a light instead of spawning
     *            a second one (entity id, or {@code BlockPos#asLong})
     * @return true when the light was newly created
     */
    public boolean addLight(final long key, final int x, final int y, final int z, final float level,
                           final float red, final float green, final float blue, final Kind kind) {
        if (!isEnabled() || this.frame >= MAX_SOURCES * 2 && this.pool.size() >= MAX_SOURCES) {
            return false;
        }
        final float scaled = MathUtil.clamp(level * this.intensity, 0.0f, 15.0f);
        if (scaled <= 0.0f) {
            return false;
        }
        synchronized (this.pool) {
            final Integer existingIndex = this.indexByKey.get(key);
            if (existingIndex != null && existingIndex < this.pool.size()) {
                final LightSource existing = this.pool.get(existingIndex);
                if (existing.x == x && existing.y == y && existing.z == z && Math.abs(existing.level - scaled) < 0.5f) {
                    // Unchanged since last tick: still mark used so it survives the swap.
                    existing.used = true;
                    existing.stamp = this.stamp;
                    return false;
                }
                existing.set(key, x, y, z, scaled, red, green, blue, kind, this.stamp);
                return false;
            }
            if (this.pool.size() >= MAX_SOURCES) {
                LOGGER.dev("Dynamic light source cap ({}) reached; ignoring {}", MAX_SOURCES, kind);
                return false;
            }
            final LightSource source = new LightSource();
            source.set(key, x, y, z, scaled, red, green, blue, kind, this.stamp);
            this.indexByKey.put(key, this.pool.size());
            this.pool.add(source);
            return true;
        }
    }

    /** Publishes the frame's lights, dropping anything not refreshed. */
    public void endFrame() {
        LightSource[] snapshot;
        synchronized (this.pool) {
            this.pool.removeIf(source -> !source.used);
            snapshot = this.pool.toArray(new LightSource[0]);
        }
        // The array is only ever replaced, never mutated after publication, so mesh
        // workers holding the old reference keep reading a consistent set.
        this.active = snapshot;
        this.lightsSeen = snapshot.length;
        this.stamp++;
        this.perBlock.clear();
    }

    /**
     * Added light level at a block position, 0..15.
     *
     * <p>Cheap path (quality 1): nearest source only, no falloff smoothing.
     * Quality 2 adds distance-weighted blending of up to 4 sources. Quality 3
     * additionally walks a DDA line to the source and scales the contribution by
     * how much solid material is in the way.</p>
     */
    public float sampleBlockLight(final int x, final int y, final int z, final OcclusionProbe probe) {
        final LightSource[] sources = this.active;
        if (sources.length == 0) {
            return 0.0f;
        }
        this.samplesThisFrame++;
        float total = 0.0f;
        int contributing = 0;
        for (final LightSource source : sources) {
            final int dx = Math.abs(source.x - x);
            if (dx > this.radius) {
                continue;
            }
            final int dy = Math.abs(source.y - y);
            if (dy > this.radius) {
                continue;
            }
            final int dz = Math.abs(source.z - z);
            if (dz > this.radius) {
                continue;
            }
            final float distanceSq = dx * dx + dy * dy + dz * dz;
            if (distanceSq > this.radius * this.radius) {
                continue;
            }
            float attenuation = 1.0f - (float) Math.sqrt(distanceSq) / (this.radius + 1.0f);
            attenuation = attenuation * attenuation;
            if (this.occlusionEnabled && probe != null) {
                this.occlusionTestsThisFrame++;
                final float visibility = probe.visibility(source.x, source.y, source.z, x, y, z);
                if (visibility <= 0.02f) {
                    continue;
                }
                attenuation *= 0.25f + 0.75f * visibility;
            }
            final float contribution = source.level * attenuation;
            if (this.quality <= 1) {
                // Sources-only mode: max, not sum. Cheap and never over-brightens.
                total = Math.max(total, contribution);
                break;
            }
            total += contribution;
            if (++contributing >= 4) {
                break;
            }
        }
        if (total <= 0.0f) {
            return 0.0f;
        }
        return MathUtil.clamp(total, 0.0f, 15.0f);
    }

    /**
     * Blended colour at a position, for the halo pass and the tinted mesher path.
     *
     * @return packed ABGR, or -1 when nothing lights this position
     */
    public int sampleColor(final int x, final int y, final int z) {
        final LightSource[] sources = this.active;
        float red = 0.0f;
        float green = 0.0f;
        float blue = 0.0f;
        float weight = 0.0f;
        for (final LightSource source : sources) {
            final int dx = Math.abs(source.x - x);
            final int dy = Math.abs(source.y - y);
            final int dz = Math.abs(source.z - z);
            if (dx > this.radius || dy > this.radius || dz > this.radius) {
                continue;
            }
            final float distanceSq = dx * dx + dy * dy + dz * dz;
            if (distanceSq > this.radius * this.radius) {
                continue;
            }
            final float falloff = 1.0f / (1.0f + distanceSq);
            red += source.red * falloff;
            green += source.green * falloff;
            blue += source.blue * falloff;
            weight += falloff;
        }
        if (weight <= 0.0f) {
            return -1;
        }
        final float inv = 1.0f / weight;
        return MathUtil.packRgb(red * inv, green * inv, blue * inv);
    }

    /**
     * Marks sections whose added light changed materially, so the mesher rebuilds
     * only those. Bounded by {@code maxDirtyPerFrame} so walking past a wall of
     * torches cannot produce a rebuild storm.
     */
    public void collectDirtySections(final int maxDirtyPerFrame, final SectionDirtySink sink) {
        if (this.dirtySections.isEmpty() || this.quality < 2) {
            return;
        }
        int emitted = 0;
        final Iterator<long[]> iterator = this.dirtySections.iterator();
        while (iterator.hasNext() && emitted < maxDirtyPerFrame) {
            final long[] entry = iterator.next();
            sink.dirty((int) entry[0], (int) entry[1], (int) entry[2], entry[3] != 0L);
            iterator.remove();
            emitted++;
        }
    }

    /** Called by the engine when a light moved, to flag its neighbourhood. */
    private void flagSection(final int x, final int y, final int z, final boolean important) {
        final int sx = Math.floorDiv(x, 1 << SECTION_SHIFT);
        final int sy = Math.floorDiv(y, 1 << SECTION_SHIFT);
        final int sz = Math.floorDiv(z, 1 << SECTION_SHIFT);
        // Coalesce: the same section is flagged by every one of its lights.
        for (final long[] existing : this.dirtySections) {
            if (existing[0] == sx && existing[1] == sy && existing[2] == sz) {
                existing[3] = existing[3] | (important ? 1L : 0L);
                return;
            }
        }
        this.dirtySections.add(new long[]{sx, sy, sz, important ? 1L : 0L});
    }

    public void onWorldEnter() {
        clear();
        LOGGER.dev("Dynamic lights: world entered, quality={} radius={}", this.quality, this.radius);
    }

    public void onWorldLeave() {
        clear();
    }

    /** One client tick: expire lights, refresh quality, mark sections dirty. */
    public void onClientTick(final List<LightEmitting> emitters) {
        if (!isEnabled()) {
            return;
        }
        this.quality = this.config.dynamicLightsQuality.get();
        this.radius = this.config.dynamicLightsRange.get();
        this.occlusionEnabled = this.quality >= 3 && (this.android == null || !this.android.isAndroid() || this.quality >= 3);
        if (emitters.isEmpty()) {
            return;
        }
        beginFrame();
        for (final LightEmitting emitter : emitters) {
            addLight(emitter.lightKey(), emitter.lightX(), emitter.lightY(), emitter.lightZ(), emitter.lightLevel(),
                    emitter.lightRed(), emitter.lightGreen(), emitter.lightBlue(), emitter.lightKind());
            flagSection(emitter.lightX(), emitter.lightY(), emitter.lightZ(), true);
        }
        endFrame();
    }

    /** Minimal contract so the engine never needs a Minecraft type. */
    public interface LightEmitting {
        long lightKey();

        int lightX();

        int lightY();

        int lightZ();

        float lightLevel();

        default float lightRed() {
            return 1.0f;
        }

        default float lightGreen() {
            return 0.85f;
        }

        default float lightBlue() {
            return 0.6f;
        }

        default Kind lightKind() {
            return Kind.ENTITY;
        }
    }

    /**
     * Occlusion query used by quality 3: sample the line between light and
     * receiver and return 1.0 when it is clear. Implemented against the world by
     * {@code ClientHooks}; a null probe simply means quality 2 behaviour.
     */
    public interface OcclusionProbe {
        float visibility(int fromX, int fromY, int fromZ, int toX, int toY, int toZ);
    }

    /** Sink for section rebuild requests, wired to {@code LevelRenderer}. */
    public interface SectionDirtySink {
        void dirty(int sectionX, int sectionY, int sectionZ, boolean important);
    }

    public int getActiveLightCount() {
        return this.active.length;
    }

    public int getSamplesThisFrame() {
        return this.samplesThisFrame;
    }

    public int getOcclusionTestsThisFrame() {
        return this.occlusionTestsThisFrame;
    }

    public int getLightsSeen() {
        return this.lightsSeen;
    }

    public int getQuality() {
        return this.quality;
    }

    public int getRadius() {
        return this.radius;
    }

    public String describe() {
        if (!isEnabled()) {
            return "dynamic lights: off";
        }
        return String.format(java.util.Locale.ROOT, "dynamic lights: %d active, quality %d, radius %d, %d samples, %d occlusion tests",
                this.active.length, this.quality, this.radius, this.samplesThisFrame, this.occlusionTestsThisFrame);
    }

    public void shutdown() {
        clear();
    }
}
