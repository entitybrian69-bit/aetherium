package com.aetherium.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The GPU-free parts of the experimental 1.16.5 chunk renderer. */
final class ChunkRendererTest {

    @Test
    @DisplayName("chunk renderer: fog comes from what vanilla told GlStateManager, not from GL queries")
    void fogTracker() {
        FogTracker.setEnabled(true);
        FogTracker.setMode(ChunkRenderer.GL_LINEAR);
        FogTracker.setStart(12.0f);
        FogTracker.setEnd(96.0f);
        FogTracker.setDensity(0.5f);
        FogTracker.setParameter(0x0B65, new float[] {1, 2, 3, 4}); // not the colour: ignored
        FogTracker.setParameter(FogTracker.GL_FOG_COLOR, new float[] {0.1f, 0.2f, 0.3f, 1.0f});
        assertTrue(FogTracker.seen());
        assertEquals(ChunkRenderer.FOG_LINEAR, ChunkRenderer.fogModeFor(FogTracker.enabled(), FogTracker.mode()));
        assertEquals(12.0f, FogTracker.start(), 0.0f);
        assertEquals(96.0f, FogTracker.end(), 0.0f);
        final java.nio.FloatBuffer color = java.nio.FloatBuffer.allocate(4);
        FogTracker.copyColor(color);
        assertEquals(0.2f, color.get(1), 0.0f);
        assertEquals(1.0f, color.get(3), 0.0f);
        FogTracker.setParameter(FogTracker.GL_FOG_COLOR, new float[] {1});  // too short: ignored
        FogTracker.copyColor(color);
        assertEquals(0.1f, color.get(0), 0.0f);
        FogTracker.setEnabled(false);
        assertEquals(ChunkRenderer.FOG_NONE, ChunkRenderer.fogModeFor(FogTracker.enabled(), FogTracker.mode()));
    }

    @Test
    @DisplayName("chunk renderer: GL fog state maps to the shader's modes")
    void fogModes() {
        assertEquals(ChunkRenderer.FOG_NONE, ChunkRenderer.fogModeFor(false, ChunkRenderer.GL_LINEAR));
        assertEquals(ChunkRenderer.FOG_LINEAR, ChunkRenderer.fogModeFor(true, ChunkRenderer.GL_LINEAR));
        assertEquals(ChunkRenderer.FOG_EXP, ChunkRenderer.fogModeFor(true, ChunkRenderer.GL_EXP));
        assertEquals(ChunkRenderer.FOG_EXP2, ChunkRenderer.fogModeFor(true, ChunkRenderer.GL_EXP2));
        assertEquals(ChunkRenderer.FOG_LINEAR, ChunkRenderer.fogModeFor(true, 0), "unknown mode falls back to linear");
    }

    @Test
    @DisplayName("chunk renderer: shaders are GLSL 1.20 (GL4ES-safe) and match the bound attributes and uniforms")
    void shaderSources() {
        final String vs = ChunkRenderer.VERTEX_SHADER;
        final String fs = ChunkRenderer.FRAGMENT_SHADER;
        assertTrue(vs.startsWith("#version 120\n"));
        assertTrue(fs.startsWith("#version 120\n"));
        for (final String attribute : new String[] {"a_pos", "a_color", "a_uv0", "a_uv2"}) {
            assertTrue(vs.contains("attribute vec") && vs.contains(" " + attribute + ";"), attribute);
        }
        for (final String uniform : new String[] {"u_projection", "u_modelView", "u_offset"}) {
            assertTrue(vs.contains("uniform") && vs.contains(uniform + ";"), uniform);
        }
        for (final String uniform : new String[] {"u_blocks", "u_lightmap", "u_alphaCutoff", "u_fogMode", "u_fog", "u_fogColor"}) {
            assertTrue(fs.contains(uniform + ";"), uniform);
        }
        for (final String banned : new String[] {"in ", "out ", "texture(", "layout", "#version 1" + "50", "gl_ModelView", "gl_Fog"}) {
            assertFalse(vs.contains(banned) || fs.contains(banned), "GLSL 1.20 / no fixed-function built-ins: " + banned);
        }
        assertEquals(32, ChunkRenderer.STRIDE);
        assertEquals(7, ChunkRenderer.GL_QUADS, "legacy buffers use vanilla's own draw call");
        assertTrue(ChunkRenderer.OFFSET_POS < ChunkRenderer.OFFSET_COLOR && ChunkRenderer.OFFSET_COLOR < ChunkRenderer.OFFSET_UV0
                && ChunkRenderer.OFFSET_UV0 < ChunkRenderer.OFFSET_UV2 && ChunkRenderer.OFFSET_UV2 + 4 <= ChunkRenderer.STRIDE);
    }

    @Test
    @DisplayName("aetherium pipeline: the compact shader is GLSL 1.20 and decodes exactly what CompactTerrain encodes")
    void compactShader() {
        final String vs = ChunkRenderer.COMPACT_VERTEX_SHADER;
        assertTrue(vs.startsWith("#version 120\n"));
        assertTrue(vs.contains("attribute vec4 a_pos;") && vs.contains("attribute vec4 a_color;") && vs.contains("attribute vec2 a_uv0;"));
        assertFalse(vs.contains("a_uv2"), "light rides in a_pos.w");
        assertTrue(vs.contains("a_pos.xyz / 1024.0 - vec3(8.0)"), vs);
        for (final String banned : new String[] {"in ", "out ", "texture(", "layout", "E-", "gl_ModelView"}) {
            assertFalse(vs.contains(banned), banned);
        }
        // CPU mirror of the shader's light decode for every packed value.
        for (int block = 0; block < 256; block += 15) {
            for (int sky = 0; sky < 256; sky += 15) {
                final float w = (float) (block | sky << 8);
                final float decodedSky = (float) Math.floor(w / 256.0f);
                assertEquals(sky, decodedSky, 0.0f);
                assertEquals(block, w - decodedSky * 256.0f, 0.0f);
            }
        }
        assertEquals("1024.0", ChunkRenderer.glslFloat(1024.0f));
        assertEquals("0.5", ChunkRenderer.glslFloat(0.5f));
    }

    @Test
    @DisplayName("aetherium pipeline: chunks are rebuilt once when it turns on, and when it turns off only if compact buffers exist")
    void pipelineSwitching() {
        assertFalse(ChunkRenderer.updatePipeline(false), "off -> off");
        assertTrue(ChunkRenderer.updatePipeline(true), "turning on converts loaded sections now");
        assertTrue(ChunkRenderer.compactUploadsWanted());
        assertFalse(ChunkRenderer.updatePipeline(true), "no rebuild every frame");
        assertFalse(ChunkRenderer.updatePipeline(false), "no compact buffer was ever stored: nothing to undo");
        assertFalse(ChunkRenderer.compactUploadsWanted());
        assertTrue(ChunkRenderer.updatePipeline(true));
        ChunkRenderer.noteCompactUpload(10);
        assertTrue(ChunkRenderer.hasCompactBuffers());
        assertTrue(ChunkRenderer.updatePipeline(false), "vanilla cannot draw compact buffers: rebuild");
        assertFalse(ChunkRenderer.hasCompactBuffers());
        assertFalse(ChunkRenderer.updatePipeline(false));
    }

    @Test
    @DisplayName("chunk renderer: off until it draws, and a failure is reported in the status")
    void statusText() {
        assertFalse(ChunkRenderer.isDrawing());
        assertTrue(ChunkRenderer.statusText().startsWith("Vanilla"));
    }
}
