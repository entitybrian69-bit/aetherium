package com.aetherium.client;

import com.aetherium.perf.ArenaAllocator;
import com.aetherium.perf.DrawRanges;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.lwjgl.opengl.GL15;

/**
 * Batched terrain for the Aetherium pipeline (1.16.5, compatible mode): the solid, cutout-mipped
 * and cutout meshes of all sections in a region of 8 x 4 x 8 sections share a few GL buffers
 * ("pages") instead of one buffer per section.
 *
 * <p>Vanilla draws every section on its own: bind its buffer, point the vertex arrays at it, push,
 * load and multiply the matrix, draw, pop. Here each page is bound, pointed at and given its
 * region's matrix once per layer, and its visible ranges are drawn back to back ({@link DrawRanges}
 * also joins neighbouring ranges into one draw). Vertex positions are moved from section-local to
 * region-local when a mesh is stored (exact in float: at most 144 blocks), so every section of a
 * page shares one matrix. The vertex format, the GL calls and the draw mode are vanilla's own,
 * which is the path that runs on GL translators.</p>
 *
 * <p>GL4ES limits, from its source: {@code GL_QUADS} draws are turned into 16-bit indices counted
 * from the start of the buffer, so a buffer must hold at most 65536 vertices; draws over 32000 vertices
 * are split by GL4ES but restarted at vertex 0, so no draw is longer than {@link #DRAW_MAX_VERTICES};
 * {@code glMultiDrawArrays} builds wrong indices for {@code GL_QUADS} and is not used. GL4ES keeps a
 * CPU copy of every buffer, so pages start small (64 KiB) and double per region and layer up to
 * {@link #PAGE_MAX_VERTICES}.</p>
 *
 * <p>Render thread only, except {@link #release}, which may be called from anywhere.</p>
 */
public final class TerrainRegions {

    public static final int LAYERS = 3;
    /** Vanilla's BLOCK format: position, color, uv0, uv2, normal + padding. */
    public static final int STRIDE = 32;
    /**
     * 512 KiB. Tile-based mobile GPUs draw a frame or two behind the CPU; updating part of a buffer
     * a pending frame still reads can make the driver copy the whole buffer ("resource ghosting"),
     * so pages stay small enough for that copy to be cheap. Also far below GL4ES's 65536-vertex
     * limit for quad draws. Larger section meshes stay in their own buffer.
     */
    static final int PAGE_MAX_VERTICES = 16384;
    static final int PAGE_MIN_VERTICES = 2048;
    public static final int DRAW_MAX_VERTICES = 32000;
    /** Gap of live (other sections') vertices drawn through to save a draw call: 128 quads. */
    static final int MERGE_GAP_VERTICES = 512;

    /** GL buffer calls; replaced in unit tests. */
    public interface Buffers {
        int create(int bytes);

        void upload(int id, long offsetBytes, ByteBuffer data);

        void delete(int id);
    }

    private static final int GL_ARRAY_BUFFER = 0x8892;
    private static final int GL_STATIC_DRAW = 0x88E4;

    static final Buffers GL = new Buffers() {
        @Override
        public int create(final int bytes) {
            final int id = GL15.glGenBuffers();
            GL15.glBindBuffer(GL_ARRAY_BUFFER, id);
            GL15.glBufferData(GL_ARRAY_BUFFER, bytes, GL_STATIC_DRAW);
            GL15.glBindBuffer(GL_ARRAY_BUFFER, 0);
            return id;
        }

        @Override
        public void upload(final int id, final long offsetBytes, final ByteBuffer data) {
            GL15.glBindBuffer(GL_ARRAY_BUFFER, id);
            GL15.glBufferSubData(GL_ARRAY_BUFFER, offsetBytes, data);
            GL15.glBindBuffer(GL_ARRAY_BUFFER, 0);
        }

        @Override
        public void delete(final int id) {
            GL15.glDeleteBuffers(id);
        }
    };

    static Buffers buffers = GL;

    /** One section mesh stored in a page. */
    public static final class Slot {
        RegionPage page;
        int first;
        int count;
        /** Section origin (blocks) the mesh was stored for: where it is drawn. */
        int originX;
        int originY;
        int originZ;
        boolean released;

        public int originX() {
            return this.originX;
        }

        public int originY() {
            return this.originY;
        }

        public int originZ() {
            return this.originZ;
        }

        public int vertexCount() {
            return this.count;
        }
    }

    /** One shared GL buffer of a region and layer. */
    public static final class RegionPage {
        final Region region;
        final int layer;
        final int capacity;
        int id;
        final ArenaAllocator alloc;
        final DrawRanges ranges = new DrawRanges();
        boolean queued;
        int slots;

        RegionPage(final Region region, final int layer, final int capacity, final int id) {
            this.region = region;
            this.layer = layer;
            this.capacity = capacity;
            this.id = id;
            this.alloc = new ArenaAllocator(capacity);
        }

        public int id() {
            return this.id;
        }

        /** Region origin (blocks): the translation every vertex of this page is relative to. */
        public int originX() {
            return this.region.originX;
        }

        public int originY() {
            return this.region.originY;
        }

        public int originZ() {
            return this.region.originZ;
        }

        public int ranges() {
            return this.ranges.size();
        }

        public int rangeFirst(final int i) {
            return this.ranges.first(i);
        }

        public int rangeCount(final int i) {
            return this.ranges.count(i);
        }
    }

    static final class Region {
        final long key;
        final int originX;
        final int originY;
        final int originZ;
        @SuppressWarnings("unchecked")
        final ArrayList<RegionPage>[] layers = new ArrayList[LAYERS];
        int pages;

        Region(final long key, final int originX, final int originY, final int originZ) {
            this.key = key;
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
            for (int i = 0; i < LAYERS; i++) {
                this.layers[i] = new ArrayList<RegionPage>(2);
            }
        }
    }

    private static final Map<Long, Region> REGIONS = new HashMap<Long, Region>();
    private static final ArrayList<RegionPage> QUEUED = new ArrayList<RegionPage>();
    private static final ConcurrentLinkedQueue<Slot> RELEASES = new ConcurrentLinkedQueue<Slot>();
    private static volatile Thread renderThread;
    private static volatile boolean enabled;

    private static int pageCount;
    private static long pageBytes;
    private static long liveBytes;
    private static int drawsLastLayer;
    private static int pagesLastLayer;

    private TerrainRegions() {
    }

    // ------------------------------------------------------------------ switch

    /**
     * Once per frame: whether new uploads go into regions. Returns true when this changed while it
     * matters (pipeline on, or regions still hold meshes): every section is then rebuilt so all of
     * them move.
     */
    public static boolean configure(final boolean wanted, final boolean pipelineOn) {
        if (wanted == enabled) {
            return false;
        }
        enabled = wanted;
        return pipelineOn || !REGIONS.isEmpty();
    }

    public static boolean enabled() {
        return enabled;
    }

    public static boolean isEmpty() {
        return REGIONS.isEmpty();
    }

    // ------------------------------------------------------------------ storage

    /**
     * Stores one section mesh (vanilla BLOCK vertices, section-local positions) and returns its slot.
     * Releases {@code old} first. Returns null, with nothing changed in {@code data}, when the mesh
     * cannot go into a page (empty or larger than a page): the caller uploads it on its own.
     * Positions in {@code data} are rewritten to region-local in place only once the slot is secured.
     */
    public static Slot store(final Slot old, final int layer, final int originX, final int originY, final int originZ,
                             final ByteBuffer data, final int vertexCount) {
        claimRenderThread();
        drainReleases();
        if (old != null) {
            releaseNow(old, false); // its page may take the new mesh; dropped below if still empty
        }
        final Slot slot = place(layer, originX, originY, originZ, data, vertexCount);
        if (old != null && !old.page.queued && old.page.slots == 0) {
            dropPage(old.page);
        }
        return slot;
    }

    private static Slot place(final int layer, final int originX, final int originY, final int originZ,
                              final ByteBuffer data, final int vertexCount) {
        if (vertexCount <= 0 || vertexCount > PAGE_MAX_VERTICES || layer < 0 || layer >= LAYERS
                || data.remaining() < vertexCount * STRIDE) {
            return null;
        }
        final int sx = originX >> 4;
        final int sy = originY >> 4;
        final int sz = originZ >> 4;
        final int rx = sx >> 3;
        final int ry = sy >> 2;
        final int rz = sz >> 3;
        final long key = key(rx, ry, rz);
        Region region = REGIONS.get(key);
        if (region == null) {
            region = new Region(key, rx << 7, ry << 6, rz << 7);
            REGIONS.put(key, region);
        }
        final ArrayList<RegionPage> pages = region.layers[layer];
        RegionPage page = null;
        int first = -1;
        int largest = 0;
        for (int i = 0, n = pages.size(); i < n; i++) {
            final RegionPage p = pages.get(i);
            largest = Math.max(largest, p.capacity);
            first = p.alloc.allocate(vertexCount);
            if (first >= 0) {
                page = p;
                break;
            }
        }
        if (page == null) {
            final int capacity = pageCapacity(vertexCount, largest);
            final int id = buffers.create(capacity * STRIDE);
            page = new RegionPage(region, layer, capacity, id);
            pages.add(page);
            region.pages++;
            pageCount++;
            pageBytes += (long) capacity * STRIDE;
            first = page.alloc.allocate(vertexCount);
        }
        offsetPositions(data, vertexCount, (sx & 7) << 4, (sy & 3) << 4, (sz & 7) << 4);
        final ByteBuffer slice = data.duplicate();
        slice.limit(slice.position() + vertexCount * STRIDE);
        buffers.upload(page.id, (long) first * STRIDE, slice);
        final Slot slot = new Slot();
        slot.page = page;
        slot.first = first;
        slot.count = vertexCount;
        slot.originX = originX;
        slot.originY = originY;
        slot.originZ = originZ;
        page.slots++;
        liveBytes += (long) ArenaAllocator.align(vertexCount) * STRIDE;
        return slot;
    }

    /** Frees a slot. Safe from any thread: off the render thread it is freed at the next upload or layer. */
    public static void release(final Slot slot) {
        if (slot == null) {
            return;
        }
        if (Thread.currentThread() == renderThread) {
            releaseNow(slot, true);
        } else {
            RELEASES.add(slot);
        }
    }

    private static void releaseNow(final Slot slot, final boolean dropEmpty) {
        if (slot.released) {
            return;
        }
        slot.released = true;
        final RegionPage page = slot.page;
        page.alloc.free(slot.first, slot.count);
        liveBytes -= (long) ArenaAllocator.align(slot.count) * STRIDE;
        page.slots--;
        if (dropEmpty && page.slots == 0 && !page.queued) {
            dropPage(page);
        }
    }

    private static void dropPage(final RegionPage page) {
        final Region region = page.region;
        if (!region.layers[page.layer].remove(page)) {
            return;
        }
        buffers.delete(page.id);
        page.id = 0;
        pageCount--;
        pageBytes -= (long) page.capacity * STRIDE;
        if (--region.pages == 0) {
            REGIONS.remove(region.key);
        }
    }

    private static void drainReleases() {
        Slot slot;
        while ((slot = RELEASES.poll()) != null) {
            releaseNow(slot, true);
        }
    }

    private static void claimRenderThread() {
        if (renderThread == null) {
            renderThread = Thread.currentThread();
        }
    }

    static int pageCapacity(final int vertexCount, final int largestInLayer) {
        int capacity = Math.max(PAGE_MIN_VERTICES, largestInLayer * 2);
        while (capacity < vertexCount) {
            capacity <<= 1;
        }
        return Math.min(capacity, PAGE_MAX_VERTICES);
    }

    /** Adds the section's offset inside its region to every vertex position (x, y, z floats at 0, 4, 8). */
    static void offsetPositions(final ByteBuffer data, final int vertexCount, final int dx, final int dy, final int dz) {
        if (dx == 0 && dy == 0 && dz == 0) {
            return;
        }
        final ByteBuffer d = data.duplicate().order(ByteOrder.nativeOrder());
        final int base = data.position();
        for (int v = 0; v < vertexCount; v++) {
            final int at = base + v * STRIDE;
            d.putFloat(at, d.getFloat(at) + dx);
            d.putFloat(at + 4, d.getFloat(at + 4) + dy);
            d.putFloat(at + 8, d.getFloat(at + 8) + dz);
        }
    }

    static long key(final int rx, final int ry, final int rz) {
        return ((long) rx & 0x3FFFFFFL) << 38 | ((long) rz & 0x3FFFFFFL) << 12 | (long) ry & 0xFFFL;
    }

    // ------------------------------------------------------------------ drawing

    /** Start of a layer: frees what other threads released. */
    public static void beginLayer() {
        claimRenderThread();
        drainReleases();
    }

    /** Queues vertices {@code [first, first + count)} of a slot (relative to the slot) for this layer. */
    public static void queue(final Slot slot, final int first, final int count) {
        if (slot.released || count <= 0) {
            return;
        }
        final RegionPage page = slot.page;
        if (!page.queued) {
            page.queued = true;
            QUEUED.add(page);
        }
        page.ranges.add(slot.first + first, count);
    }

    /** Queues the whole slot. */
    public static void queueAll(final Slot slot) {
        queue(slot, 0, slot.count);
    }

    /** Merges the queued ranges of every page; returns the number of pages to draw ({@link #page}). */
    public static int preparePages() {
        int draws = 0;
        for (int i = 0, n = QUEUED.size(); i < n; i++) {
            final RegionPage page = QUEUED.get(i);
            draws += page.ranges.merge(MERGE_GAP_VERTICES, page.alloc, DRAW_MAX_VERTICES);
        }
        drawsLastLayer = draws;
        pagesLastLayer = QUEUED.size();
        return QUEUED.size();
    }

    public static RegionPage page(final int i) {
        return QUEUED.get(i);
    }

    /** End of a layer (also after a failure): clears the queue and drops pages emptied meanwhile. */
    public static void endLayer() {
        for (int i = 0, n = QUEUED.size(); i < n; i++) {
            final RegionPage page = QUEUED.get(i);
            page.ranges.clear();
            page.queued = false;
            if (page.slots == 0) {
                dropPage(page);
            }
        }
        QUEUED.clear();
    }

    // ------------------------------------------------------------------ status

    /** Backend page suffix, empty when nothing is batched. */
    public static String status() {
        if (pageCount == 0) {
            return "";
        }
        return "; batched: " + pageCount + " buffers, " + (pageBytes >> 20) + " MB (" + (liveBytes >> 20)
                + " MB used), last layer " + pagesLastLayer + " buffers / " + drawsLastLayer + " draws";
    }

    static int pageCount() {
        return pageCount;
    }

    static long pageBytes() {
        return pageBytes;
    }

    /** Unit tests: forget everything (buffers are not deleted). */
    static void resetForTests(final Buffers testBuffers) {
        REGIONS.clear();
        QUEUED.clear();
        RELEASES.clear();
        renderThread = null;
        enabled = false;
        pageCount = 0;
        pageBytes = 0;
        liveBytes = 0;
        drawsLastLayer = 0;
        pagesLastLayer = 0;
        buffers = testBuffers;
    }

    /** All pages of every region (tests). */
    static int regionCount() {
        return REGIONS.size();
    }

    static Iterator<Region> regions() {
        return REGIONS.values().iterator();
    }
}
