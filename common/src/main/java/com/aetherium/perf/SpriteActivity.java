package com.aetherium.perf;

import java.util.Arrays;

/**
 * "Animate only visible textures": which animated block-atlas sprites were used recently.
 *
 * <p>A sprite counts as seen when a visible chunk section's mesh uses it (recorded per section
 * while meshing, marked every frame for the sections being drawn), or when it is drawn directly on
 * the render thread (items, block entities, falling blocks, fluids, breaking particles). The atlas
 * tick skips the animation step, and with it the texture upload, of sprites not seen for
 * {@link #WINDOW} ticks; they resume where they paused when seen again. Fire and portal sprites
 * are never tracked: screen overlays and burning entities draw them in ways not recorded here.</p>
 */
public final class SpriteActivity {

    /** A sprite with tracking state (mixed into {@code TextureAtlasSprite}). */
    public interface Tracked {
        /** Animated, in the block atlas, and not one of the always-animated sprites. */
        boolean aetheriumTracked();

        void aetheriumSetTracked(boolean tracked);

        int aetheriumLastSeen();

        void aetheriumSetLastSeen(int tick);
    }

    /** Ticks (1/20 s) a sprite keeps animating after it was last seen; covers frame rates down to 2.5 FPS. */
    public static final int WINDOW = 8;

    static final Object[] NONE = new Object[0];

    private static final String[] ALWAYS = {
        "minecraft:block/fire_0", "minecraft:block/fire_1", "minecraft:block/soul_fire_0",
        "minecraft:block/soul_fire_1", "minecraft:block/nether_portal",
    };

    private static volatile boolean enabled;
    /** A recording hook is missing: everything animates for the rest of the session. */
    private static volatile boolean broken;
    private static volatile int tick;
    private static volatile Thread renderThread;
    /** A section is being built synchronously on the render thread (vanilla's rebuildChunkSync). */
    private static boolean renderThreadSection;
    private static final ThreadLocal<Collector> COLLECTOR = new ThreadLocal<Collector>();

    private static long skipped;
    private static long animated;

    /** Sprites used by one chunk section mesh. */
    static final class Collector {
        Object[] items = new Object[4];
        int size;
        boolean active;

        void add(final Object sprite) {
            for (int i = 0; i < this.size; i++) {
                if (this.items[i] == sprite) {
                    return;
                }
            }
            if (this.size == this.items.length) {
                this.items = Arrays.copyOf(this.items, this.size * 2);
            }
            this.items[this.size++] = sprite;
        }
    }

    private SpriteActivity() {
    }

    public static void setEnabled(final boolean on) {
        enabled = on && !broken;
    }

    /** A recording hook did not apply: animate everything from now on. */
    public static void markBroken() {
        broken = true;
        enabled = false;
    }

    public static boolean enabled() {
        return enabled;
    }

    /** Whether a sprite name is always animated (never tracked). */
    public static boolean alwaysAnimated(final String name) {
        for (int i = 0; i < ALWAYS.length; i++) {
            if (ALWAYS[i].equals(name)) {
                return true;
            }
        }
        return false;
    }

    /** After an atlas (re)load: a tracked sprite starts as just seen. */
    public static void track(final Tracked sprite, final boolean tracked) {
        sprite.aetheriumSetTracked(tracked);
        sprite.aetheriumSetLastSeen(tick);
    }

    /** Start of a frame on the render thread. */
    public static void frame() {
        if (renderThread == null) {
            renderThread = Thread.currentThread();
        }
        // A synchronous rebuild never spans frames; one that threw must not keep recording.
        renderThreadSection = false;
    }

    /** A sprite is used now. On a meshing thread inside a section build it is recorded for that section. */
    public static void touch(final Object sprite) {
        if (!(sprite instanceof Tracked)) {
            return;
        }
        final Tracked t = (Tracked) sprite;
        if (!t.aetheriumTracked()) {
            return;
        }
        if (renderThreadSection || Thread.currentThread() != renderThread) {
            final Collector c = COLLECTOR.get();
            if (c != null && c.active) {
                c.add(sprite);
                return;
            }
        }
        t.aetheriumSetLastSeen(tick);
    }

    /** A section build starts on this thread. */
    public static void beginSection() {
        Collector c = COLLECTOR.get();
        if (c == null) {
            c = new Collector();
            COLLECTOR.set(c);
        }
        Arrays.fill(c.items, 0, c.size, null);
        c.size = 0;
        c.active = true;
        if (Thread.currentThread() == renderThread) {
            renderThreadSection = true;
        }
    }

    /** The section build on this thread ended: the sprites it used (a shared empty array when none). */
    public static Object[] endSection() {
        final Collector c = COLLECTOR.get();
        if (Thread.currentThread() == renderThread) {
            renderThreadSection = false;
        }
        if (c == null || !c.active) {
            return NONE;
        }
        c.active = false;
        if (c.size == 0) {
            return NONE;
        }
        final Object[] out = Arrays.copyOf(c.items, c.size);
        Arrays.fill(c.items, 0, c.size, null);
        c.size = 0;
        return out;
    }

    /** A visible section's sprites, every frame. */
    public static void seen(final Object[] sprites) {
        if (sprites == null) {
            return;
        }
        final int now = tick;
        for (int i = 0; i < sprites.length; i++) {
            ((Tracked) sprites[i]).aetheriumSetLastSeen(now);
        }
    }

    /** Atlas tick: whether this sprite's animation step runs. */
    public static boolean shouldAnimate(final Object sprite) {
        if (!enabled || !(sprite instanceof Tracked)) {
            return true;
        }
        final Tracked t = (Tracked) sprite;
        if (!t.aetheriumTracked() || tick - t.aetheriumLastSeen() <= WINDOW) {
            animated++;
            return true;
        }
        skipped++;
        return false;
    }

    private static int lastSeenPass = -1;

    /** Once per atlas tick on the render thread: whether the visible sections should be marked now. */
    public static boolean seenPassDue() {
        final int now = tick;
        if (now == lastSeenPass) {
            return false;
        }
        lastSeenPass = now;
        return true;
    }

    /** End of the block atlas tick. */
    public static void advance() {
        tick++;
    }

    /** Animation steps skipped / run since start (diagnostics). */
    public static String status() {
        return "animations run " + animated + ", skipped " + skipped;
    }

    static int tick() {
        return tick;
    }

    static void resetForTests() {
        enabled = false;
        broken = false;
        tick = 0;
        renderThread = null;
        renderThreadSection = false;
        lastSeenPass = -1;
        COLLECTOR.remove();
        skipped = 0;
        animated = 0;
    }
}
