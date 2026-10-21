package com.aetherium.client;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

/**
 * GPU occlusion culling (1.17.2, Minecraft 1.16.5, Aetherium pipeline, computers only).
 *
 * <p>After the solid and cutout terrain of a frame is drawn, the graphics card is asked, for
 * sections in view, how many pixels of the section's box would still be visible against that
 * depth buffer (an occlusion query; colour and depth writes off, so nothing appears). Results are
 * read in later frames without waiting for the card. A section whose box was completely hidden
 * {@link #HIDE_AFTER} times in a row skips its solid and cutout terrain until a query sees it
 * again; hidden sections are queried every frame, visible ones every {@link #VISIBLE_PERIOD}
 * frames (spread over the frames).</p>
 *
 * <p>Never culled: sections within {@link #NEAR} blocks of the camera (or around it), sections
 * whose last result is older than {@link #STALE_FRAMES} frames, and everything while the option
 * is off or the GL has no queries. Translucent terrain, entities and block entities are not
 * affected. A section can appear one frame late after a fast turn, which is why this is off by
 * default.</p>
 *
 * <p>The bookkeeping ({@link #decideQuery}, {@link #shouldSkip}, {@link #record}) is plain Java and
 * unit-tested; the GL calls are thin wrappers. Render thread only.</p>
 */
public final class OcclusionCulling {

    static final int HIDE_AFTER = 2;
    static final int VISIBLE_PERIOD = 4;
    static final int STALE_FRAMES = 6;
    static final float NEAR = 24f;
    /** The box is grown by this much (blocks) on every side so its own surface never hides it. */
    static final float GROW = 0.5f;
    private static final int PRUNE_PERIOD = 128;

    /** Per-section state. */
    static final class Entry {
        int query;
        boolean pending;
        long queryFrame = Long.MIN_VALUE / 2;
        long resultFrame = Long.MIN_VALUE / 2;
        long seenFrame;
        int zeros;
        boolean hidden;
        final int phase;

        Entry(final int phase) {
            this.phase = phase;
        }
    }

    private static final Map<Object, Entry> ENTRIES = new IdentityHashMap<Object, Entry>();
    private static long frame;
    private static boolean active;
    private static Boolean supported;
    private static int cube;
    private static int culledLastFrame;
    private static int culledThisFrame;
    private static int queriesLastFrame;
    private static int queriesThisFrame;

    private OcclusionCulling() {
    }

    /**
     * Once per frame before terrain is drawn. {@code wanted}: option on, pipeline drawing, desktop
     * GL. Returns whether culling runs this frame.
     */
    public static boolean beginFrame(final boolean wanted) {
        frame++;
        culledLastFrame = culledThisFrame;
        queriesLastFrame = queriesThisFrame;
        culledThisFrame = 0;
        queriesThisFrame = 0;
        if (!wanted || !supported()) {
            if (!ENTRIES.isEmpty()) {
                clear();
            }
            active = false;
            return false;
        }
        active = true;
        if (frame % PRUNE_PERIOD == 0) {
            prune();
        }
        return true;
    }

    public static boolean active() {
        return active;
    }

    private static boolean supported() {
        if (supported == null) {
            boolean ok;
            try {
                ok = GL.getCapabilities() != null && GL.getCapabilities().OpenGL15;
            } catch (final RuntimeException | LinkageError error) {
                ok = false;
            }
            supported = Boolean.valueOf(ok);
        }
        return supported.booleanValue();
    }

    /**
     * Draw loop: whether the section with box minimum {@code (dx, dy, dz)} (camera-relative) skips
     * its solid and cutout terrain this frame.
     */
    public static boolean hidden(final Object section, final float dx, final float dy, final float dz) {
        if (!active) {
            return false;
        }
        final Entry entry = ENTRIES.get(section);
        if (entry == null) {
            return false;
        }
        entry.seenFrame = frame;
        if (shouldSkip(entry.hidden, entry.resultFrame, frame, dx, dy, dz)) {
            culledThisFrame++;
            return true;
        }
        return false;
    }

    static boolean shouldSkip(final boolean hidden, final long resultFrame, final long now, final float dx,
                              final float dy, final float dz) {
        return hidden && now - resultFrame <= STALE_FRAMES && !near(dx, dy, dz);
    }

    /** Whether the camera (origin) is within {@link #NEAR} blocks of the 16-block box at (dx, dy, dz). */
    static boolean near(final float dx, final float dy, final float dz) {
        final float ex = Math.max(0f, Math.max(dx, -(dx + 16f)));
        final float ey = Math.max(0f, Math.max(dy, -(dy + 16f)));
        final float ez = Math.max(0f, Math.max(dz, -(dz + 16f)));
        return ex * ex + ey * ey + ez * ez <= NEAR * NEAR;
    }

    /**
     * After the opaque layers: collects the section's finished result, then says whether to issue a
     * new query for it now (the caller then calls {@link #beginQuery}, draws {@link #drawBox} and
     * calls {@link #endQuery}).
     */
    public static boolean wantQuery(final Object section, final float dx, final float dy, final float dz) {
        if (!active) {
            return false;
        }
        Entry entry = ENTRIES.get(section);
        if (entry == null) {
            entry = new Entry(System.identityHashCode(section) & (VISIBLE_PERIOD - 1));
            ENTRIES.put(section, entry);
        }
        entry.seenFrame = frame;
        if (entry.pending && entry.query != 0) {
            if (GL15.glGetQueryObjecti(entry.query, GL15.GL_QUERY_RESULT_AVAILABLE) != 0) {
                record(entry, GL15.glGetQueryObjecti(entry.query, GL15.GL_QUERY_RESULT), frame);
            }
        }
        if (near(dx, dy, dz)) {
            entry.hidden = false;
            entry.zeros = 0;
            return false;
        }
        return decideQuery(entry.pending, entry.hidden, entry.phase, frame);
    }

    static boolean decideQuery(final boolean pending, final boolean hidden, final int phase, final long now) {
        if (pending) {
            return false;
        }
        return hidden || (now & (VISIBLE_PERIOD - 1)) == phase;
    }

    static void record(final Entry entry, final int samples, final long now) {
        entry.pending = false;
        entry.resultFrame = now;
        if (samples == 0) {
            entry.zeros++;
            entry.hidden = entry.zeros >= HIDE_AFTER;
        } else {
            entry.zeros = 0;
            entry.hidden = false;
        }
    }

    public static void beginQuery(final Object section) {
        final Entry entry = ENTRIES.get(section);
        if (entry == null) {
            return;
        }
        if (entry.query == 0) {
            entry.query = GL15.glGenQueries();
        }
        GL15.glBeginQuery(GL15.GL_SAMPLES_PASSED, entry.query);
        entry.pending = true;
        entry.queryFrame = frame;
        queriesThisFrame++;
    }

    public static void endQuery() {
        GL15.glEndQuery(GL15.GL_SAMPLES_PASSED);
    }

    /** Binds the unit box (0..1 cube, as quads) and points the vertex array at it. */
    public static void bindBox() {
        if (cube == 0) {
            final float[] v = {
                0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, // bottom
                0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, // top
                0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, // north
                0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1, // south
                0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0, // west
                1, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, // east
            };
            final ByteBuffer data = ByteBuffer.allocateDirect(v.length * 4).order(ByteOrder.nativeOrder());
            for (final float f : v) {
                data.putFloat(f);
            }
            data.flip();
            cube = GL15.glGenBuffers();
            GL15.glBindBuffer(0x8892, cube);
            GL15.glBufferData(0x8892, data, 0x88E4); // GL_STATIC_DRAW
        } else {
            GL15.glBindBuffer(0x8892, cube);
        }
        GL11.glVertexPointer(3, GL11.GL_FLOAT, 12, 0L);
        GL11.glEnableClientState(0x8074); // GL_VERTEX_ARRAY
    }

    /** Draws the bound unit box with the current model-view matrix. */
    public static void drawBox() {
        GL11.glDrawArrays(GL11.GL_QUADS, 0, 24);
    }

    public static void unbindBox() {
        GL11.glDisableClientState(0x8074);
        GL15.glBindBuffer(0x8892, 0);
    }

    /** Status for the Backend page. */
    public static String status() {
        if (!active) {
            return "";
        }
        return "; occlusion: " + culledLastFrame + " hidden, " + queriesLastFrame + " queries";
    }

    private static void prune() {
        final ArrayList<Integer> dead = new ArrayList<Integer>();
        final Iterator<Entry> it = ENTRIES.values().iterator();
        while (it.hasNext()) {
            final Entry entry = it.next();
            if (frame - entry.seenFrame > PRUNE_PERIOD) {
                if (entry.query != 0) {
                    dead.add(Integer.valueOf(entry.query));
                }
                it.remove();
            }
        }
        for (final Integer id : dead) {
            GL15.glDeleteQueries(id.intValue());
        }
    }

    /** Forgets every section (option off, world change); deletes the queries. */
    public static void clear() {
        for (final Entry entry : ENTRIES.values()) {
            if (entry.query != 0) {
                try {
                    GL15.glDeleteQueries(entry.query);
                } catch (final RuntimeException | LinkageError ignored) {
                    // no context: nothing to free
                }
            }
        }
        ENTRIES.clear();
        active = false;
    }

    /** Tests. */
    static void resetForTests() {
        ENTRIES.clear();
        frame = 0;
        active = false;
        supported = null;
        culledLastFrame = 0;
        culledThisFrame = 0;
        queriesLastFrame = 0;
        queriesThisFrame = 0;
    }
}
