package com.aetherium.client;

import com.aetherium.perf.PipelineCrashGuard;
import com.aetherium.perf.RenderToggles;
import com.aetherium.render.CompactTerrain;
import com.aetherium.util.AetheriumLog;

/**
 * The Aetherium renderer on 1.17..1.21.4 (no Minecraft types; the mixins pass plain values).
 *
 * <p>Vanilla keeps building, uploading, culling, ordering and drawing every section with its own
 * shaders, uniforms, vertex arrays and quad index buffer. Two things change for the solid,
 * cutout-mipped and cutout layers:</p>
 * <ol>
 *   <li>each mesh is reordered by face direction when it is uploaded ({@link CompactTerrain#sortQuads};
 *   the same vanilla vertices, only in a different order), and</li>
 *   <li>its draw covers one index range that leaves out the face groups that point away from the
 *   camera (or nothing at all when no face can be seen), instead of the whole mesh. Never more draw
 *   calls than vanilla.</li>
 * </ol>
 *
 * <p>A sorted mesh is still a valid vanilla mesh, so falling back (switched off, an error) only
 * means drawing every index again: no rebuild, nothing that can be left in a broken state. Render
 * thread only.</p>
 */
public final class SectionCull {

    private static final AetheriumLog LOG = AetheriumLog.of(SectionCull.class);

    /** {@link #plan}: draw the whole mesh as vanilla does. */
    public static final int FULL = -1;
    /** {@link #plan}: nothing faces the camera; skip the draw. */
    public static final int SKIP = 0;
    /** {@link #plan}: draw {@link #rangeFirstIndex()} / {@link #rangeIndexCount()}. */
    public static final int RANGE = 1;

    /** GL_TRIANGLES: what {@code VertexFormat.Mode.QUADS} draws with from 1.17. */
    public static final int GL_TRIANGLES = 4;

    private static boolean failed;
    private static String failure = "";
    private static boolean layerOn;
    private static double camX;
    private static double camY;
    private static double camZ;
    private static boolean lastEnabled;
    private static boolean firstFrame = true;

    private static final int[] RUN_FIRST = new int[4];
    private static final int[] RUN_COUNT = new int[4];
    private static int rangeFirst;
    private static int rangeCount;

    private static int frameSections;
    private static int frameSkipped;
    private static long frameQuads;
    private static long frameQuadsDrawn;
    private static int shownSections;
    private static int shownSkipped;
    private static long shownQuads;
    private static long shownQuadsDrawn;
    private static boolean drewLastFrame;
    private static boolean drewThisFrame;
    private static long sortedUploads;

    private SectionCull() {
    }

    /** Whether meshes are sorted on upload right now. */
    public static boolean sorting() {
        return RenderToggles.aetheriumRenderer && !failed;
    }

    /**
     * Start of a frame ({@code LevelRenderer.renderLevel} HEAD).
     *
     * @return true when the loaded sections should be rebuilt (the renderer was just turned on, so
     *         the meshes already on the GPU get sorted)
     */
    public static boolean frame() {
        layerOn = false;
        shownSections = frameSections;
        shownSkipped = frameSkipped;
        shownQuads = frameQuads;
        shownQuadsDrawn = frameQuadsDrawn;
        drewLastFrame = drewThisFrame;
        frameSections = 0;
        frameSkipped = 0;
        frameQuads = 0L;
        frameQuadsDrawn = 0L;
        drewThisFrame = false;
        final boolean enabled = sorting();
        if (!enabled && PipelineCrashGuard.isArmed()) {
            PipelineCrashGuard.disarm();
        }
        final boolean rebuild = enabled && !lastEnabled && !firstFrame;
        lastEnabled = enabled;
        firstFrame = false;
        return rebuild;
    }

    /** A vanilla terrain layer starts; {@code terrain} for solid, cutout-mipped and cutout. */
    public static void beginLayer(final boolean terrain, final double x, final double y, final double z) {
        layerOn = terrain && sorting();
        if (layerOn) {
            camX = x;
            camY = y;
            camZ = z;
            drewThisFrame = true;
            PipelineCrashGuard.arm(PipelineCrashGuard.MODE_COMPATIBLE);
        }
    }

    public static void endLayer() {
        layerOn = false;
    }

    /** Whether draws of sorted section buffers are ranged right now. */
    public static boolean layerOn() {
        return layerOn;
    }

    public static void noteSortedUpload() {
        sortedUploads++;
    }

    /**
     * How to draw one section whose mesh starts at {@code origin} and whose draw would cover
     * {@code indexCount} indices. A layout that does not describe exactly that mesh is never used.
     */
    public static int plan(final CompactTerrain.Layout layout, final int originX, final int originY, final int originZ,
                           final int indexCount) {
        return planRelative(layout, (float) (camX - originX), (float) (camY - originY), (float) (camZ - originZ),
                indexCount);
    }

    /** {@link #plan} with the camera position relative to the section origin. */
    static int planRelative(final CompactTerrain.Layout layout, final float rx, final float ry, final float rz,
                            final int indexCount) {
        if (layout == null) {
            return FULL;
        }
        final int quads = layout.quads();
        if ((long) quads * 6L != indexCount) {
            return FULL; // not the mesh this layout was made for
        }
        frameSections++;
        frameQuads += quads;
        if (quads == 0) {
            return FULL;
        }
        final int mask = CompactTerrain.visibleGroups(layout.plane, rx, ry, rz);
        int runs = CompactTerrain.runs(layout, mask, RUN_FIRST, RUN_COUNT);
        if (runs == 0) {
            frameSkipped++;
            return SKIP;
        }
        // One span per section: never more draws than vanilla.
        runs = CompactTerrain.mergeRuns(RUN_FIRST, RUN_COUNT, runs, Integer.MAX_VALUE);
        if (runs != 1) {
            frameQuadsDrawn += quads;
            return FULL;
        }
        rangeFirst = RUN_FIRST[0] * 6;
        rangeCount = RUN_COUNT[0] * 6;
        if (rangeFirst < 0 || rangeCount <= 0 || rangeFirst + rangeCount > indexCount) {
            frameQuadsDrawn += quads;
            return FULL;
        }
        frameQuadsDrawn += RUN_COUNT[0];
        return RANGE;
    }

    /** First index of the range {@link #plan} chose (6 indices per quad). */
    public static int rangeFirstIndex() {
        return rangeFirst;
    }

    public static int rangeIndexCount() {
        return rangeCount;
    }

    /** Bytes per index for a GL index type (GL_UNSIGNED_BYTE / SHORT / INT); 0 when unknown. */
    public static int indexBytes(final int glType) {
        switch (glType) {
            case 0x1401:
                return 1;
            case 0x1403:
                return 2;
            case 0x1405:
                return 4;
            default:
                return 0;
        }
    }

    /** Anything unexpected: vanilla draws for the rest of the session (sorted meshes stay valid). */
    public static void fail(final Throwable error) {
        if (failed) {
            return;
        }
        failed = true;
        failure = String.valueOf(error);
        layerOn = false;
        PipelineCrashGuard.disarm();
        LOG.warn("Aetherium renderer turned off for this session, vanilla draws terrain: " + failure, error);
    }

    public static boolean hasFailed() {
        return failed;
    }

    public static String statusText() {
        if (failed) {
            return "Vanilla (fell back: " + failure + ")";
        }
        if (!RenderToggles.aetheriumRenderer && PipelineCrashGuard.wasTripped()) {
            return "Vanilla (turned off: the game closed while the renderer was drawing last time)";
        }
        if (!sorting()) {
            return "Vanilla";
        }
        if (!drewLastFrame) {
            return "Aetherium renderer (waiting for terrain)";
        }
        final long percent = shownQuads == 0L ? 100L : Math.round(shownQuadsDrawn * 100.0 / shownQuads);
        return "Aetherium renderer (last frame " + shownSections + " sorted section layers, " + percent
                + "% of their faces drawn, " + shownSkipped + " skipped entirely; " + sortedUploads
                + " meshes sorted)";
    }
}
