package com.aetherium.client;

import com.aetherium.Aetherium;
import com.aetherium.mixin.AetheriumMixinPlugin;
import com.aetherium.perf.RenderToggles;
import com.aetherium.render.CompactTerrain;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.util.BlockPos;
import org.lwjgl.opengl.GL11;

/**
 * The Aetherium terrain renderer on Minecraft 1.8.9 (VBO path, which it switches on).
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

    /** Diagnostics (smoke test / bug reports): uploads seen and why meshes were not sorted. */
    private static long seenUploads;
    private static long rejectedFormat;
    private static long rejectedOther;
    private static long routedDraws;
    private static long plannedDraws;

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
        ensureVbo();
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

    /**
     * The renderer works on vanilla's VBO path, and 1.8.9 ships with "Use VBOs" off (display lists).
     * While the renderer is on and the GPU has VBOs, vanilla's own option is switched on once (as if
     * the player ticked it) and the chunks are rebuilt. Without VBO support nothing changes and the
     * status says so.
     */
    private static void ensureVbo() {
        if (!enabled() || !Aetherium.isActive()) {
            return;
        }
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings == null || mc.gameSettings.useVbo || !OpenGlHelper.vboSupported) {
            return;
        }
        mc.gameSettings.useVbo = true;
        if (mc.renderGlobal != null && mc.theWorld != null) {
            mc.renderGlobal.loadRenderers();
        }
        mc.gameSettings.saveOptions();
        com.aetherium.util.AetheriumLog.of(TerrainRenderer.class).info("Aetherium: turned on VBOs for the Aetherium terrain renderer");
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
    public static void beforeUpload(final WorldRenderer buffer, final EnumWorldBlockLayer layer) {
        pendingLayout = null;
        seenUploads++;
        if (layer == null || layer == EnumWorldBlockLayer.TRANSLUCENT || !sorting()) {
            return;
        }
        if (buffer.getDrawMode() != GL11.GL_QUADS || buffer.getVertexFormat() != DefaultVertexFormats.BLOCK
                || DefaultVertexFormats.BLOCK.getNextOffset() != CompactTerrain.SOURCE_STRIDE_NO_NORMAL) {
            rejectedFormat++;
            return;
        }
        final int vertices = buffer.getVertexCount();
        if (vertices <= 0 || (vertices & 3) != 0) {
            rejectedOther++;
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
    public static void beginLayer(final EnumWorldBlockLayer layer) {
        final boolean on = enabled();
        // The container origin is the player's feet; the eye (height, bobbing, third person,
        // freelook) lives in the camera matrix. Faces are culled against the true eye.
        final boolean eye = on && loadEye();
        SectionCull.beginLayer(on && eye && layer != EnumWorldBlockLayer.TRANSLUCENT, eyeX, eyeY, eyeZ);
        fogLayer = on && fogLinear && RenderToggles.smartChunkCulling && loadForward();
        drewThisFrame |= on;
    }

    public static void endLayer() {
        SectionCull.endLayer();
        fogLayer = false;
    }

    /** Replaces {@code vertexBuffer.drawArrays(GL_QUADS)} for one chunk at {@code origin}. */
    public static void draw(final VertexBuffer buffer, final int mode, final BlockPos origin) {
        routedDraws++;
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
                        plannedDraws++;
                        final int plan = SectionCull.plan(layout, origin.getX(), origin.getY(), origin.getZ(),
                                count / 4 * 6);
                        if (plan == SectionCull.SKIP) {
                            return;
                        }
                        if (plan == SectionCull.RANGE) {
                            final int first = SectionCull.rangeFirstIndex() / 6 * 4;
                            final int vertices = SectionCull.rangeIndexCount() / 6 * 4;
                            GL11.glDrawArrays(mode, first, vertices);
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
        final float cx = (float) (origin.getX() + HALF_CHUNK - eyeX);
        final float cy = (float) (origin.getY() + HALF_CHUNK - eyeY);
        final float cz = (float) (origin.getZ() + HALF_CHUNK - eyeZ);
        final float nearest = cx * fwdX + cy * fwdY + cz * fwdZ - fwdReach;
        return nearest > fogEnd + FOG_MARGIN;
    }

    private static double eyeX;
    private static double eyeY;
    private static double eyeZ;

    /**
     * Eye position in world space: container origin + (-M^-1 t) of the camera matrix captured by
     * {@code ActiveRenderInfo} (general 3x3 inverse: the nausea effect scales the modelview).
     */
    private static boolean loadEye() {
        final FloatBuffer m = CameraMatrix.modelview();
        if (m == null || m.capacity() < 16) {
            return false;
        }
        // column-major: a = M[row][col] = m[col*4 + row]
        final double a00 = m.get(0), a10 = m.get(1), a20 = m.get(2);
        final double a01 = m.get(4), a11 = m.get(5), a21 = m.get(6);
        final double a02 = m.get(8), a12 = m.get(9), a22 = m.get(10);
        final double tx = m.get(12), ty = m.get(13), tz = m.get(14);
        final double c00 = a11 * a22 - a12 * a21;
        final double c01 = a02 * a21 - a01 * a22;
        final double c02 = a01 * a12 - a02 * a11;
        final double c10 = a12 * a20 - a10 * a22;
        final double c11 = a00 * a22 - a02 * a20;
        final double c12 = a02 * a10 - a00 * a12;
        final double c20 = a10 * a21 - a11 * a20;
        final double c21 = a01 * a20 - a00 * a21;
        final double c22 = a00 * a11 - a01 * a10;
        final double det = a00 * c00 + a01 * c10 + a02 * c20;
        if (!(Math.abs(det) > 1.0e-6) || Double.isNaN(tx + ty + tz)) {
            return false;
        }
        final double ex = -(c00 * tx + c01 * ty + c02 * tz) / det;
        final double ey = -(c10 * tx + c11 * ty + c12 * tz) / det;
        final double ez = -(c20 * tx + c21 * ty + c22 * tz) / det;
        if (Math.abs(ex) > 64.0 || Math.abs(ey) > 64.0 || Math.abs(ez) > 64.0) {
            return false; // not the camera we expect (another mod's matrix): cull nothing
        }
        eyeX = camX + ex;
        eyeY = camY + ey;
        eyeZ = camZ + ez;
        return true;
    }

    /** View direction from the camera matrix vanilla captured for this frame. */
    private static boolean loadForward() {
        final FloatBuffer m = CameraMatrix.modelview();
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

    public static String diagnostics() {
        return "uploads seen " + seenUploads + ", wrong format " + rejectedFormat + ", empty/odd " + rejectedOther
                + ", chunk draws routed " + routedDraws + ", planned from a sorted layout " + plannedDraws;
    }

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
        if (!OpenGlHelper.vboSupported) {
            return "Vanilla (this GPU has no vertex buffers)";
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
