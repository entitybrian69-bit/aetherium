package com.aetherium.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The GPU-free parts of the experimental 1.16.5 chunk renderer. */
final class ChunkRendererTest {

    @Test
    @DisplayName("chunk renderer: quads become two triangles with vanilla's winding, within 16-bit range")
    void quadIndexLayout() {
        final short[] two = ChunkRenderer.quadIndices(2);
        assertEquals(12, two.length);
        final short[] expected = {0, 1, 2, 2, 3, 0, 4, 5, 6, 6, 7, 4};
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], two[i], "index " + i);
        }
        final short[] all = ChunkRenderer.quadIndices(ChunkRenderer.MAX_QUADS_PER_DRAW);
        int max = 0;
        for (final short s : all) {
            max = Math.max(max, s & 0xFFFF);
        }
        assertEquals(ChunkRenderer.MAX_VERTICES_PER_DRAW - 1, max, "last vertex addressable as unsigned short");
        assertTrue(ChunkRenderer.MAX_VERTICES_PER_DRAW <= 65536);
    }

    @Test
    @DisplayName("chunk renderer: index counts drop incomplete quads and split sections fit the shared buffer")
    void indexCounts() {
        assertEquals(0, ChunkRenderer.indexCountFor(0));
        assertEquals(0, ChunkRenderer.indexCountFor(3));
        assertEquals(6, ChunkRenderer.indexCountFor(4));
        assertEquals(6, ChunkRenderer.indexCountFor(7));
        assertEquals(ChunkRenderer.MAX_QUADS_PER_DRAW * 6, ChunkRenderer.indexCountFor(ChunkRenderer.MAX_VERTICES_PER_DRAW));
        // A worst-case section (every block face visible): 4096 blocks * 6 faces * 4 vertices.
        final int worst = 4096 * 6 * 4;
        int first = 0;
        int indices = 0;
        int draws = 0;
        while (first < worst) {
            final int count = Math.min(worst - first, ChunkRenderer.MAX_VERTICES_PER_DRAW);
            assertTrue(ChunkRenderer.indexCountFor(count) <= ChunkRenderer.MAX_QUADS_PER_DRAW * 6);
            indices += ChunkRenderer.indexCountFor(count);
            first += count;
            draws++;
        }
        assertEquals(2, draws);
        assertEquals(worst / 4 * 6, indices, "no quad lost at the split");
        assertEquals(0, ChunkRenderer.MAX_VERTICES_PER_DRAW % 4, "splits land on quad boundaries");
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
        assertTrue(ChunkRenderer.OFFSET_POS < ChunkRenderer.OFFSET_COLOR && ChunkRenderer.OFFSET_COLOR < ChunkRenderer.OFFSET_UV0
                && ChunkRenderer.OFFSET_UV0 < ChunkRenderer.OFFSET_UV2 && ChunkRenderer.OFFSET_UV2 + 4 <= ChunkRenderer.STRIDE);
    }

    @Test
    @DisplayName("chunk renderer: off until it draws, and a failure is reported in the status")
    void statusText() {
        assertFalse(ChunkRenderer.isDrawing());
        assertTrue(ChunkRenderer.statusText().startsWith("Vanilla"));
    }
}
