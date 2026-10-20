package com.aetherium.shader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Krypton (NG-GL4ES) links shader inputs by the order glslang numbers them in, not by name: the
 * vertex attribute pinning and the varying alignment that make packs render there.
 */
final class KryptonLinkTest {
    private static final String VSH = "#version 120\n"
            + "varying vec2 texcoord;\n"
            + "varying vec4 glcolor;\n"
            + "uniform float frameTimeCounter;\n"
            + "void main() {\n"
            + "    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;\n"
            + "    glcolor = gl_Color;\n"
            + "    vec3 n = gl_NormalMatrix * gl_Normal;\n"
            + "    gl_Position = ftransform();\n"
            + "}\n";
    private static final String FSH = "#version 120\n"
            + "uniform sampler2D texture;\n"
            + "varying vec4 glcolor;\n"
            + "varying vec2 texcoord;\n"
            + "void main() {\n"
            + "    gl_FragData[0] = texture2D(texture, texcoord) * glcolor;\n"
            + "}\n";

    private static String vertex(final String src) {
        return GlslPatcher.patch(src, GlslPatcher.VERTEX, new TreeMap<String, String>(), true, 450, true);
    }

    private static String fragment(final String src) {
        return GlslPatcher.patch(src, GlslPatcher.FRAGMENT, new TreeMap<String, String>(), true, 450);
    }

    /** Declared inputs (attributes) in declaration order. */
    private static List<String> attributes(final String src) {
        final List<String> out = new ArrayList<>();
        final Matcher m = Pattern.compile("(?m)^attribute\\s+\\w+\\s+(\\w+)\\s*;").matcher(src);
        while (m.find()) {
            out.add(m.group(1));
        }
        return out;
    }

    private static List<String> varyings(final String src) {
        final List<String> out = new ArrayList<>();
        final Matcher m = Pattern.compile("(?m)^(?:flat |smooth |noperspective )?(?:varying|in|out)\\s+\\w+\\s+(\\w+)").matcher(src);
        while (m.find()) {
            out.add(m.group(1));
        }
        return out;
    }

    @Test
    @DisplayName("built-in attributes land on gl4es' ARB indices, gaps filled")
    void pinsArbLayout() {
        final String pinned = GlslPatcher.pinAttributes(vertex(VSH), GlslPatcher.ATTRIB_ARB);
        final List<String> attrs = attributes(pinned);
        // glslang numbers these 0, 1, 2... in this order: index == gl4es' ARB_* slot
        assertEquals("_gl4es_Vertex", attrs.get(0));
        assertEquals("_gl4es_Normal", attrs.get(2));
        assertEquals("_gl4es_Color", attrs.get(3));
        assertEquals("_gl4es_MultiTexCoord0", attrs.get(8));
        assertEquals(9, attrs.size());
        assertTrue(attrs.get(1).startsWith("aetherium_unusedAttrib"));
        // nothing left for gl4es' strstr to find and declare again
        for (final String b : new String[] {"gl_Vertex", "gl_Normal", "gl_Color", "gl_MultiTexCoord", "ftransform"}) {
            assertFalse(pinned.contains(b), b);
        }
        assertTrue(pinned.contains("uniform mat3 _gl4es_NormalMatrix;"));
        // the packed-id helpers in the header read the pinned colour
        assertTrue(pinned.indexOf("attribute vec4 _gl4es_Color;") < pinned.indexOf("aetherium_code()"));
    }

    @Test
    @DisplayName("8-attribute devices use gl4es' compact layout")
    void pinsCompactLayout() {
        final List<String> attrs = attributes(GlslPatcher.pinAttributes(vertex(VSH), GlslPatcher.ATTRIB_COMP));
        assertEquals("_gl4es_Vertex", attrs.get(0));
        assertEquals("_gl4es_Normal", attrs.get(2));
        assertEquals("_gl4es_Color", attrs.get(3));
        assertEquals("_gl4es_MultiTexCoord0", attrs.get(5));
        assertEquals(6, attrs.size());
    }

    @Test
    @DisplayName("no pinning off gl4es, and gl_VertexID is hidden from gl4es' scan")
    void pinEdges() {
        final String v = vertex(VSH);
        assertEquals(v, GlslPatcher.pinAttributes(v, 0));
        final String withId = GlslPatcher.pinAttributes(
                vertex(VSH.replace("vec3 n =", "float id = float(gl_VertexID);\n    vec3 n =")), GlslPatcher.ATTRIB_ARB);
        assertFalse(withId.contains("gl_VertexID"));
        assertTrue(withId.contains("#define aetherium_VertexID gl_Ver##texID"));
    }

    @Test
    @DisplayName("varyings get one shared order in both stages")
    void alignsVaryings() {
        final VaryingAligner.Result r = VaryingAligner.align(vertex(VSH), fragment(FSH));
        assertNull(r.skipped);
        assertEquals(varyings(r.vertex), varyings(r.fragment));
        assertEquals(2, varyings(r.vertex).size());
        assertEquals(GlslPatcher.headerLines(r.fragment) + FSH.split("\n", -1).length - 1,
                r.fragment.split("\n", -1).length - 1, "the body keeps its line count");
    }

    @Test
    @DisplayName("only the active #if branch counts; built-in varyings become named ones")
    void alignsActiveBranchAndBuiltins() {
        final String vs = "#version 120\n#define VERTEX_SHADER\n"
                + "#ifdef FRAGMENT_SHADER\nvec3 binormal;\n#else\nvarying vec3 binormal;\n#endif\n"
                + "void main() { binormal = gl_Normal; gl_FrontColor = gl_Color; gl_TexCoord[0] = gl_MultiTexCoord0;"
                + " gl_Position = ftransform(); }\n";
        final String fs = "#version 120\nvarying vec3 binormal;\n"
                + "void main() { gl_FragColor = gl_Color * vec4(binormal, 1.0) * gl_TexCoord[0]; }\n";
        final VaryingAligner.Result r = VaryingAligner.align(vertex(vs), fragment(fs));
        assertNull(r.skipped);
        assertEquals(varyings(r.vertex), varyings(r.fragment));
        assertTrue(varyings(r.vertex).contains("aetherium_Color"));
        assertTrue(varyings(r.vertex).contains("aetherium_TexCoord"));
        assertFalse(r.fragment.contains("gl_Color"));
        assertTrue(r.vertex.contains("vec3 binormal;"), "the inactive branch is left alone");
    }

    @Test
    @DisplayName("texture matrices become Aetherium uniforms, lightmap one on unit 2")
    void ownTextureMatrices() {
        final String v = GlslPatcher.ownTextureMatrices(GlslPatcher.pinAttributes(vertex(VSH), GlslPatcher.ATTRIB_ARB));
        assertFalse(v.contains("gl_TextureMatrix"));
        assertTrue(v.contains("uniform mat4 aetherium_TextureMatrix0;"));
        assertTrue(v.contains("aetherium_TextureMatrix0 * _gl4es_MultiTexCoord0"));
        final String lm = GlslPatcher.ownTextureMatrices(vertex(
                VSH.replace("vec3 n =", "vec2 lm = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;\n    vec3 n =")));
        assertTrue(lm.contains("uniform mat4 aetherium_TextureMatrix2;"));
        assertTrue(lm.contains("aetherium_TextureMatrix2 * gl_MultiTexCoord2"));
        // a computed index still needs the built-in: left alone
        final String dyn = vertex(VSH.replace("gl_TextureMatrix[0]", "gl_TextureMatrix[int(frameTimeCounter)]"));
        assertEquals(dyn, GlslPatcher.ownTextureMatrices(dyn));
        assertEquals(GlslPatcher.headerLines(v) + VSH.split("\n", -1).length,
                v.split("\n", -1).length, "the body keeps its line count");
    }

    @Test
    @DisplayName("an active clash leaves the program as the pack wrote it")
    void refusesClash() {
        final String vs = "#version 120\nvarying vec2 uv;\nvoid main() { uv = vec2(0.0); gl_Position = ftransform(); }\n";
        final String fs = "#version 120\nvarying vec3 uv;\nvoid main() { gl_FragColor = vec4(uv, 1.0); }\n";
        final String v = vertex(vs);
        final String f = fragment(fs);
        final VaryingAligner.Result r = VaryingAligner.align(v, f);
        assertNotNull(r.skipped);
        assertEquals(v, r.vertex);
        assertEquals(f, r.fragment);
    }
}
