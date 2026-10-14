package com.aetherium.client;

import com.aetherium.Aetherium;
import com.aetherium.mixin.AetheriumMixinPlugin;
import com.aetherium.perf.RenderToggles;
import com.aetherium.render.CompactTerrain;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;

/**
 * The Aetherium terrain renderer on Minecraft 1.12.2 (VBO path).
 *
 * <p>Vanilla keeps building, uploading, ordering and drawing every chunk with its own state. Three
 * things change:</p>
 * <ol>
 *   <li>each solid / cutout-mipped / cutout mesh is reordered by face direction when it is uploaded
 *   (the same vanilla quads, only in another order: {@link CompactTerrain#sortQuadsByWinding} on
 *   1.12.2's 28-byte block vertex);</li>
 *   <li>its draw covers only the face groups that can point at the camera, in one
 *   {@code glDrawArrays} span (never more draw calls than vanilla), or nothing at all; and</li>
 *   <li>chunks that lie entirely beyond the end of the linear distance fog are not drawn.</li>
 * </ol>
 *
 * <p>A sorted mesh is still a valid vanilla mesh, so any fallback (switched off, an error, another
 * renderer) only means drawing every vertex again. Render thread only.</p>
 */
public final class TerrainRenderer {

    /** Distance the camera can sit in front of the position vanilla hands the chunk list. */
    private static final float FOG_MARGIN = 4.0f;
    private static final float HALF_CHUNK = 8.0f;

    private static double camX;
    private static double camY;
    private static double camZ;
    private static boolean fogLinear;
    private static float fogEnd;
    private static boolean fogLayer;
    private static float fwdX;
    private static float fwdY;
    private static float fwdZ;
    private static float fwdReach;
    private static boolean drewThisFrame;
    private static boolean drewLastFrame;
    private static long fogCulledFrame;
    private static long fogCulledShown;

    /** Set by the upload hook right before {@code VertexBuffer.bufferData}, taken by it. */
    private static CompactTerrain.Layout pendingLayout;

    private TerrainRenderer() {
    }

    /** Whether the hooks are installed and the renderer is wanted. */
    public static boolean enabled() {
        return RenderToggles.aetheriumRenderer && AetheriumMixinPlugin.rendererOwner() == null
                && AetheriumMixinPlugin.isApplied("ChunkDrawMixin") && AetheriumMixinPlugin.isApplied("VertexBufferMixin");
    }

    private static boolean sorting() {
        return enabled() && com.aetherium.client.SectionCull.sorting();
    }

    // ------------------------------------------------------------------ frame

    public static void onFrame() {
        drewLastFrame = drewThisFrame;
        drewThisFrame = false;
        fogCulledShown = fogCulledFrame;
        fogCulledFrame = 0L;
        fogLinear = false;
        if (SectionCull.frame() && enabled()) {
            // Just switched on: meshes already on the GPU are unsorted until rebuilt.
            VanillaOptions.reloadChunks();
        }
    }

    /** From the fog events: the terrain pass uses linear fog ending at {@code end} (or not). */
    public static void noteFog(final boolean linear, final float end) {
        fogLinear = linear;
        fogEnd = end;
    }

    /** {@code ChunkRenderContainer.initialize}: the position the chunk offsets are relative to. */
    public static void setCamera(final double x, final double y, final double z) {
        camX = x;
        camY = y;
        camZ = z;
    }

    // ------------------------------------------------------------------ upload

    /**
     * {@code ChunkRenderDispatcher.uploadVertexBuffer} (any thread that uploads, normally the
     * render thread): sorts a finished terrain mesh in place before vanilla hands it to the GPU.
     */
    public static void beforeUpload(final BufferBuilder buffer, final BlockRenderLayer layer) {
        pendingLayout = null;
        if (layer == null || layer == BlockRenderLayer.TRANSLUCENT || !sorting()) {
            return;
        }
        if (buffer.getDrawMode() != GL11.GL_QUADS || buffer.getVertexFormat() != DefaultVertexFormats.BLOCK
                || DefaultVertexFormats.BLOCK.getSize() != CompactTerrain.SOURCE_STRIDE_NO_NORMAL) {
            return;
        }
        final int vertices = buffer.getVertexCount();
        if (vertices <= 0 || (vertices & 3) != 0) {
            return;
        }
        final ByteBuffer data = buffer.getByteBuffer().duplicate();
        data.position(0);
        data.limit(vertices * CompactTerrain.SOURCE_STRIDE_NO_NORMAL);
        pendingLayout = SectionCull.sortVertices(data, false);
    }

    /** {@code VertexBuffer.bufferData}: the layout of the data being uploaded, or null. */
    public static CompactTerrain.Layout takeLayout() {
        final CompactTerrain.Layout layout = pendingLayout;
        pendingLayout = null;
        return layout;
    }

    // ------------------------------------------------------------------ draw

    /** {@code VboRenderList.renderChunkLayer} HEAD. */
    public static void beginLayer(final BlockRenderLayer layer) {
        final boolean on = enabled();
        SectionCull.beginLayer(on && layer != BlockRenderLayer.TRANSLUCENT, camX, camY, camZ);
        fogLayer = on && fogLinear && RenderToggles.smartChunkCulling && loadForward();
        drewThisFrame |= on;
    }

    public static void endLayer() {
        SectionCull.endLayer();
        fogLayer = false;
    }

    /** Replaces {@code vertexBuffer.drawArrays(GL_QUADS)} for one chunk at {@code origin}. */
    public static void draw(final VertexBuffer buffer, final int mode, final BlockPos origin) {
        if (origin != null && (fogLayer || SectionCull.layerOn())) {
            try {
                if (fogLayer && beyondFog(origin)) {
                    fogCulledFrame++;
                    return;
                }
                if (SectionCull.layerOn() && mode == GL11.GL_QUADS && buffer instanceof SortedBuffer) {
                    final SortedBuffer sorted = (SortedBuffer) buffer;
                    final CompactTerrain.Layout layout = sorted.aetherium$layout();
                    final int count = sorted.aetherium$count();
                    if (layout != null && (count & 3) == 0) {
                        final int plan = SectionCull.plan(layout, origin.getX(), origin.getY(), origin.getZ(),
                                count / 4 * 6);
                        if (plan == SectionCull.SKIP) {
                            return;
                        }
                        if (plan == SectionCull.RANGE) {
                            final int first = SectionCull.rangeFirstIndex() / 6 * 4;
                            final int vertices = SectionCull.rangeIndexCount() / 6 * 4;
                            GlStateManager.glDrawArrays(mode, first, vertices);
                            return;
                        }
                    }
                }
            } catch (final RuntimeException | LinkageError error) {
                SectionCull.fail(error);
                fogLayer = false;
            }
        }
        buffer.drawArrays(mode);
    }

    /** The whole chunk is further along the view axis than the fog end (fully fogged). */
    private static boolean beyondFog(final BlockPos origin) {
        final float cx = (float) (origin.getX() + HALF_CHUNK - camX);
        final float cy = (float) (origin.getY() + HALF_CHUNK - camY);
        final float cz = (float) (origin.getZ() + HALF_CHUNK - camZ);
        final float nearest = cx * fwdX + cy * fwdY + cz * fwdZ - fwdReach;
        return nearest > fogEnd + FOG_MARGIN;
    }

    /** View direction from the camera matrix vanilla captured for this frame. */
    private static boolean loadForward() {
        if (!AetheriumMixinPlugin.isApplied("CameraMatrixAccessor")) {
            return false;
        }
        final FloatBuffer m = com.aetherium.mixin.core.CameraMatrixAccessor.aetherium$modelview();
        if (m == null || m.capacity() < 16) {
            return false;
        }
        fwdX = -m.get(2);
        fwdY = -m.get(6);
        fwdZ = -m.get(10);
        final float len = (float) Math.sqrt(fwdX * fwdX + fwdY * fwdY + fwdZ * fwdZ);
        if (!(len > 0.5f && len < 2.0f)) {
            return false;
        }
        fwdX /= len;
        fwdY /= len;
        fwdZ /= len;
        fwdReach = HALF_CHUNK * (Math.abs(fwdX) + Math.abs(fwdY) + Math.abs(fwdZ));
        return true;
    }

    // ------------------------------------------------------------------ status

    public static boolean isDrawing() {
        return drewLastFrame && enabled() && !SectionCull.hasFailed();
    }

    public static String statusText() {
        final String owner = AetheriumMixinPlugin.rendererOwner();
        if (owner != null) {
            return "Vanilla (" + owner + " replaces the chunk renderer)";
        }
        if (!AetheriumMixinPlugin.isApplied("ChunkDrawMixin")) {
            return "Vanilla (hooks not installed)";
        }
        if (!Aetherium.isActive()) {
            return "Vanilla (Aetherium paused)";
        }
        final String base = SectionCull.statusText();
        return fogCulledShown > 0 ? base + ", " + fogCulledShown + " fogged chunk draws skipped" : base;
    }

    /** Implemented by {@code VertexBuffer} through {@code VertexBufferMixin}. */
    public interface SortedBuffer {
        CompactTerrain.Layout aetherium$layout();

        int aetherium$count();
    }
}
