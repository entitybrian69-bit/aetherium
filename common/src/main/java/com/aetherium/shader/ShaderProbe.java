package com.aetherium.shader;

import com.aetherium.util.AetheriumLog;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * On-device self-test for GL translators (Krypton, gl4es...): draws one quad the way Minecraft
 * 1.16.5 draws chunks (its BLOCK vertex layout in a buffer, fixed-function pointers) into a tiny
 * framebuffer with a shader that writes what it received, reads the pixels back and compares.
 *
 * <p>It answers what cannot be known from the outside: which vertex input numbering the
 * translator really uses (tried in turn: the detected one, gl4es' ARB and compact numbering, and
 * none), whether its {@code gl_TextureMatrix} uniforms carry the real matrices, and whether
 * samplers read the units they are set to. The pipeline then builds packs with what passed, and
 * every result goes to the game log. Runs once per session, render thread, all GL state restored.
 */
final class ShaderProbe {
    private static final AetheriumLog LOG = AetheriumLog.of(ShaderProbe.class);

    /** What the device showed. Defaults are the pre-test behaviour. */
    static final class Result {
        boolean ran;
        int layout;
        boolean attribsOk;
        boolean texMatOk;
        boolean ownTexMatOk;
        boolean samplersOk;
        final List<String> lines = new ArrayList<>();
    }

    private static String lastKey;
    private static Result last;

    private ShaderProbe() {
    }

    static synchronized Result get(final GlCaps caps, final Map<String, String> macros) {
        final String key = caps.renderer + '|' + caps.version + '|' + caps.translatorName + '|' + caps.vertexAttribLayout;
        if (last != null && key.equals(lastKey)) {
            return last;
        }
        final Result r = run(caps, macros);
        for (final String line : r.lines) {
            LOG.info("Shader self-test: " + line);
        }
        last = r;
        lastKey = key;
        return r;
    }

    // ---- what the quad carries (Minecraft's BLOCK layout: pos 3f, colour 4ub, uv0 2f, uv2 2s, normal 3b)
    private static final int STRIDE = 32;
    private static final byte[] COLOR = {51, 102, (byte) 153, (byte) 204};
    private static final float U0 = 0.25F;
    private static final float V0 = 0.75F;
    private static final short U2 = 32;
    private static final short V2 = 224;
    private static final byte[] NORMAL = {0, 76, 102};

    /** Texture matrix set on unit 0 for the test: scale 0.5. */
    private static final float[] HALF = {0.5F, 0, 0, 0, 0, 0.5F, 0, 0, 0, 0, 0.5F, 0, 0, 0, 0, 1};

    /** Expected RGBA per pixel; -1 = not compared. */
    private static final int[][] EXPECT = {
        {51, 102, 153, 204},       // 0 gl_Color
        {64, 191, 0, 255},         // 1 gl_MultiTexCoord0
        {32, 223, 0, 255},         // 2 gl_MultiTexCoord2 / 256
        {128, 204, 230, 255},      // 3 gl_Normal * 0.5 + 0.5
        {32, 96, 0, 255},          // 4 gl_TextureMatrix[0] * uv0 (scale 0.5)
        {40, 231, 0, 255},         // 5 gl_TextureMatrix[2] * uv2 (Minecraft's lightmap matrix)
        {0, 255, 0, 255},          // 6 sampler on unit 0
        {200, 100, 50, 255},       // 7 sampler on unit 2
    };
    private static final String[] WHAT = {"colour", "uv0", "uv2", "normal", "texmat0", "texmat2", "unit0", "unit2"};

    private static final String VSH = "#version 120\n"
            + "varying vec4 probeColor;\n"
            + "varying vec4 probeUv0;\n"
            + "varying vec4 probeUv2;\n"
            + "varying vec3 probeNormal;\n"
            + "varying vec4 probeTm0;\n"
            + "varying vec4 probeTm2;\n"
            + "void main() {\n"
            + "    probeColor = gl_Color;\n"
            + "    probeUv0 = gl_MultiTexCoord0;\n"
            + "    probeUv2 = gl_MultiTexCoord2;\n"
            + "    probeNormal = gl_Normal;\n"
            + "    probeTm0 = gl_TextureMatrix[0] * gl_MultiTexCoord0;\n"
            + "    probeTm2 = gl_TextureMatrix[2] * gl_MultiTexCoord2;\n"
            + "    gl_Position = vec4(gl_Vertex.xy, 0.0, 1.0);\n"
            + "}\n";
    private static final String FSH = "#version 120\n"
            + "uniform sampler2D probeTex0;\n"
            + "uniform sampler2D probeTex2;\n"
            + "varying vec4 probeColor;\n"
            + "varying vec4 probeUv0;\n"
            + "varying vec4 probeUv2;\n"
            + "varying vec3 probeNormal;\n"
            + "varying vec4 probeTm0;\n"
            + "varying vec4 probeTm2;\n"
            + "void main() {\n"
            + "    float x = gl_FragCoord.x;\n"
            + "    vec4 c;\n"
            + "    if (x < 1.0) c = probeColor;\n"
            + "    else if (x < 2.0) c = vec4(probeUv0.xy, 0.0, 1.0);\n"
            + "    else if (x < 3.0) c = vec4(probeUv2.xy / 256.0, 0.0, 1.0);\n"
            + "    else if (x < 4.0) c = vec4(probeNormal * 0.5 + 0.5, 1.0);\n"
            + "    else if (x < 5.0) c = vec4(probeTm0.xy, 0.0, 1.0);\n"
            + "    else if (x < 6.0) c = vec4(probeTm2.xy, 0.0, 1.0);\n"
            + "    else if (x < 7.0) c = texture2D(probeTex0, vec2(0.75, 0.25));\n"
            + "    else c = texture2D(probeTex2, vec2(0.5, 0.5));\n"
            + "    gl_FragColor = c;\n"
            + "}\n";

    private static Result run(final GlCaps caps, final Map<String, String> macros) {
        final Result r = new Result();
        r.layout = caps.vertexAttribLayout;
        if (!caps.translator) {
            return r;
        }
        final boolean gl4es = caps.translatorName != null && caps.translatorName.contains("GL4ES");
        final List<Integer> layouts = new ArrayList<>();
        layouts.add(caps.vertexAttribLayout);
        if (gl4es) {
            for (final int l : new int[] {GlslPatcher.ATTRIB_ARB, GlslPatcher.ATTRIB_COMP, 0}) {
                if (!layouts.contains(l)) {
                    layouts.add(l);
                }
            }
        }
        int maxAttribs = -1;
        try {
            maxAttribs = GL11.glGetInteger(0x8869); // GL_MAX_VERTEX_ATTRIBS
        } catch (final RuntimeException ignored) {
            // unknown
        }
        r.lines.add(caps.translatorName + ", max vertex attribs " + maxAttribs + ", detected numbering "
                + layoutName(caps.vertexAttribLayout));

        final State saved = State.save();
        int fbo = 0;
        int target = 0;
        int vbo = 0;
        int tex0 = 0;
        int tex2 = 0;
        try {
            // ---- 8x1 RGBA8 target
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            target = texture(8, 1, null);
            fbo = GL30.glGenFramebuffers();
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, target, 0);
            if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
                r.lines.add("no test framebuffer on this device; skipped");
                return r;
            }
            GL11.glViewport(0, 0, 8, 1);
            for (final int cap : new int[] {GL11.GL_BLEND, GL11.GL_DEPTH_TEST, GL11.GL_CULL_FACE, GL11.GL_ALPHA_TEST,
                    SCISSOR_TEST, STENCIL_TEST}) {
                GL11.glDisable(cap);
            }
            GL11.glColorMask(true, true, true, true);

            // ---- textures: 2x2 on unit 0 (red, green / blue, yellow), 1x1 on unit 2
            final ByteBuffer px0 = BufferUtils.createByteBuffer(16);
            px0.put(new byte[] {(byte) 255, 0, 0, (byte) 255, 0, (byte) 255, 0, (byte) 255,
                    0, 0, (byte) 255, (byte) 255, (byte) 255, (byte) 255, 0, (byte) 255}).flip();
            tex0 = texture(2, 2, px0);
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + 2);
            final ByteBuffer px2 = BufferUtils.createByteBuffer(4);
            px2.put(new byte[] {(byte) 200, 100, 50, (byte) 255}).flip();
            tex2 = texture(1, 1, px2);

            // ---- texture matrices: unit 0 scale 0.5, unit 2 Minecraft's lightmap matrix
            GL11.glMatrixMode(TEXTURE_MODE);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glLoadMatrixf(buffer(HALF));
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + 2);
            GL11.glLoadMatrixf(buffer(ShaderPipeline.LIGHTMAP_MATRIX));
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);

            // ---- the quad, as a vertex buffer with Minecraft's pointers
            final ByteBuffer v = BufferUtils.createByteBuffer(STRIDE * 4);
            final float[][] corners = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}};
            for (final float[] c : corners) {
                v.putFloat(c[0]).putFloat(c[1]).putFloat(0);
                v.put(COLOR);
                v.putFloat(U0).putFloat(V0);
                v.putShort(U2).putShort(V2);
                v.put(NORMAL).put((byte) 0);
            }
            v.flip();
            vbo = GL15.glGenBuffers();
            GL15.glBindBuffer(ARRAY_BUFFER, vbo);
            GL15.glBufferData(ARRAY_BUFFER, v, STATIC_DRAW);
            GL11.glVertexPointer(3, GL11.GL_FLOAT, STRIDE, 0L);
            GL11.glEnableClientState(VERTEX_ARRAY);
            GL11.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, STRIDE, 12L);
            GL11.glEnableClientState(COLOR_ARRAY);
            GL13.glClientActiveTexture(GL13.GL_TEXTURE0);
            GL11.glTexCoordPointer(2, GL11.GL_FLOAT, STRIDE, 16L);
            GL11.glEnableClientState(TEXCOORD_ARRAY);
            GL13.glClientActiveTexture(GL13.GL_TEXTURE0 + 2);
            GL11.glTexCoordPointer(2, GL11.GL_SHORT, STRIDE, 24L);
            GL11.glEnableClientState(TEXCOORD_ARRAY);
            GL13.glClientActiveTexture(GL13.GL_TEXTURE0);
            GL11.glNormalPointer(GL11.GL_BYTE, STRIDE, 28L);
            GL11.glEnableClientState(NORMAL_ARRAY);

            // ---- each numbering with the translator's texture matrices, then ours on the winner
            int[] chosen = null;
            for (final int layout : layouts) {
                final int[] got = draw(caps, macros, layout, false, r);
                if (got != null && attribsOk(got) && chosen == null) {
                    chosen = got;
                    r.layout = layout;
                }
            }
            r.ran = true;
            if (chosen == null) {
                r.lines.add("no numbering delivered Minecraft's vertex data; keeping " + layoutName(r.layout));
            } else {
                r.attribsOk = true;
                r.texMatOk = ok(chosen, 4) && ok(chosen, 5);
                r.samplersOk = ok(chosen, 6) && ok(chosen, 7);
            }
            final int[] own = draw(caps, macros, r.layout, true, r);
            r.ownTexMatOk = own != null && ok(own, 4) && ok(own, 5);
            r.lines.add("using " + layoutName(r.layout) + " numbering; translator texture matrices "
                    + (r.texMatOk ? "ok" : "WRONG") + ", Aetherium's " + (r.ownTexMatOk ? "ok" : "WRONG")
                    + "; samplers " + (r.samplersOk ? "ok" : "WRONG"));
        } catch (final Throwable t) {
            r.lines.add("self-test stopped: " + t);
        } finally {
            try {
                GL11.glDisableClientState(VERTEX_ARRAY);
                GL11.glDisableClientState(COLOR_ARRAY);
                GL11.glDisableClientState(NORMAL_ARRAY);
                GL13.glClientActiveTexture(GL13.GL_TEXTURE0 + 2);
                GL11.glDisableClientState(TEXCOORD_ARRAY);
                GL13.glClientActiveTexture(GL13.GL_TEXTURE0);
                GL11.glDisableClientState(TEXCOORD_ARRAY);
                saved.restore();
                if (vbo != 0) {
                    GL15.glDeleteBuffers(vbo);
                }
                if (fbo != 0) {
                    GL30.glDeleteFramebuffers(fbo);
                }
                for (final int t : new int[] {target, tex0, tex2}) {
                    if (t != 0) {
                        GL11.glDeleteTextures(t);
                    }
                }
            } catch (final Throwable t) {
                r.lines.add("restoring GL state: " + t);
            }
        }
        return r;
    }

    /** One draw with a program built like a pack's; null when it did not build. */
    private static int[] draw(final GlCaps caps, final Map<String, String> macros, final int layout,
                              final boolean ownTexMat, final Result r) {
        String vs = GlslPatcher.patch(VSH, GlslPatcher.VERTEX, macros, true, caps.glslVersion, false);
        String fs = GlslPatcher.patch(FSH, GlslPatcher.FRAGMENT, macros, true, caps.glslVersion);
        vs = GlslPatcher.pinAttributes(vs, layout);
        if (ownTexMat) {
            vs = GlslPatcher.ownTextureMatrices(vs);
        }
        final VaryingAligner.Result aligned = VaryingAligner.align(vs, fs);
        vs = aligned.vertex;
        fs = aligned.fragment;
        final String label = layoutName(layout) + (ownTexMat ? " + Aetherium texture matrices" : "");
        final GlProgram p;
        try {
            p = GlProgram.build("aetherium_selftest", vs, fs, null, null, false, GlslPatcher.headerLines(fs));
        } catch (final GlProgram.BuildException | RuntimeException e) {
            final String msg = String.valueOf(e.getMessage()).replace('\n', ' ');
            r.lines.add(label + ": did not build: " + (msg.length() > 300 ? msg.substring(0, 300) : msg));
            return null;
        }
        try {
            GL20.glUseProgram(p.id);
            p.set("probeTex0", 0);
            p.set("probeTex2", 2);
            if (ownTexMat) {
                p.setMat4(GlslPatcher.TEXTURE_MATRIX + 0, HALF);
                p.setMat4(GlslPatcher.TEXTURE_MATRIX + 2, ShaderPipeline.LIGHTMAP_MATRIX);
            }
            GL11.glClearColor(0, 0, 0, 0);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            GL11.glDrawArrays(TRIANGLE_STRIP, 0, 4);
            final ByteBuffer out = BufferUtils.createByteBuffer(32);
            GL11.glReadPixels(0, 0, 8, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, out);
            final int[] got = new int[32];
            for (int i = 0; i < 32; i++) {
                got[i] = out.get(i) & 0xFF;
            }
            final StringBuilder sb = new StringBuilder(label).append(':');
            for (int i = 0; i < EXPECT.length; i++) {
                sb.append(' ').append(WHAT[i]).append(ok(got, i) ? " ok" : " WRONG" + rgba(got, i));
            }
            final StringBuilder locs = new StringBuilder();
            for (final String a : new String[] {"Vertex", "Color", "Normal", "MultiTexCoord0", "MultiTexCoord2"}) {
                final int at = GL20.glGetAttribLocation(p.id, "_gl4es_" + a);
                if (at >= 0) {
                    locs.append(locs.length() == 0 ? "" : ",").append(a).append('=').append(at);
                }
            }
            if (locs.length() > 0) {
                sb.append(" [locations ").append(locs).append(']');
            }
            r.lines.add(sb.toString());
            return got;
        } finally {
            GL20.glUseProgram(0);
            p.delete();
        }
    }

    private static boolean attribsOk(final int[] got) {
        return ok(got, 0) && ok(got, 1) && ok(got, 2) && ok(got, 3);
    }

    private static boolean ok(final int[] got, final int pixel) {
        for (int c = 0; c < 4; c++) {
            if (Math.abs(got[pixel * 4 + c] - EXPECT[pixel][c]) > 4) {
                return false;
            }
        }
        return true;
    }

    private static String rgba(final int[] got, final int pixel) {
        final int o = pixel * 4;
        return "(" + got[o] + "," + got[o + 1] + "," + got[o + 2] + "," + got[o + 3] + ")";
    }

    static String layoutName(final int layout) {
        return layout == GlslPatcher.ATTRIB_ARB ? "gl4es ARB" : layout == GlslPatcher.ATTRIB_COMP ? "gl4es compact"
                : "translator's own";
    }

    private static int texture(final int w, final int h, final ByteBuffer pixels) {
        final int t = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, t);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        return t;
    }

    private static final int GL12_CLAMP_TO_EDGE = 0x812F;
    private static final int SCISSOR_TEST = 0x0C11;
    private static final int STENCIL_TEST = 0x0B90;
    private static final int TEXTURE_MODE = 0x1702;
    private static final int ARRAY_BUFFER = 0x8892;
    private static final int STATIC_DRAW = 0x88E4;
    private static final int VERTEX_ARRAY = 0x8074;
    private static final int COLOR_ARRAY = 0x8076;
    private static final int TEXCOORD_ARRAY = 0x8078;
    private static final int NORMAL_ARRAY = 0x8075;
    private static final int TRIANGLE_STRIP = 0x0005;

    private static FloatBuffer buffer(final float[] m) {
        final FloatBuffer b = BufferUtils.createFloatBuffer(16);
        b.put(m).flip();
        return b;
    }

    /** The GL state the test touches, as it was. */
    private static final class State {
        int fbo;
        final int[] viewport = new int[4];
        int program;
        int active;
        int arrayBuffer;
        int matrixMode;
        int tex0;
        int tex2;
        final FloatBuffer texMat0 = BufferUtils.createFloatBuffer(16);
        final FloatBuffer texMat2 = BufferUtils.createFloatBuffer(16);
        final FloatBuffer clear = BufferUtils.createFloatBuffer(16);
        final int[] caps = {GL11.GL_BLEND, GL11.GL_DEPTH_TEST, GL11.GL_CULL_FACE, GL11.GL_ALPHA_TEST,
                SCISSOR_TEST, STENCIL_TEST};
        final boolean[] enabled = new boolean[6];

        static State save() {
            final State s = new State();
            s.fbo = GL11.glGetInteger(0x8CA6); // GL_FRAMEBUFFER_BINDING
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, s.viewport);
            s.program = GL11.glGetInteger(0x8B8D); // GL_CURRENT_PROGRAM
            s.active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            s.arrayBuffer = GL11.glGetInteger(0x8894); // GL_ARRAY_BUFFER_BINDING
            s.matrixMode = GL11.glGetInteger(0x0BA0); // GL_MATRIX_MODE
            GL11.glGetFloatv(0x0C22, s.clear); // GL_COLOR_CLEAR_VALUE
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            s.tex0 = GL11.glGetInteger(0x8069); // GL_TEXTURE_BINDING_2D
            GL11.glGetFloatv(0x0BA8, s.texMat0); // GL_TEXTURE_MATRIX
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + 2);
            s.tex2 = GL11.glGetInteger(0x8069);
            GL11.glGetFloatv(0x0BA8, s.texMat2);
            GL13.glActiveTexture(s.active);
            for (int i = 0; i < s.caps.length; i++) {
                s.enabled[i] = GL11.glIsEnabled(s.caps[i]);
            }
            return s;
        }

        void restore() {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.fbo);
            GL11.glViewport(this.viewport[0], this.viewport[1], this.viewport[2], this.viewport[3]);
            GL15.glBindBuffer(ARRAY_BUFFER, this.arrayBuffer);
            GL11.glMatrixMode(TEXTURE_MODE);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.tex0);
            GL11.glLoadMatrixf(this.texMat0);
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + 2);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.tex2);
            GL11.glLoadMatrixf(this.texMat2);
            GL11.glMatrixMode(this.matrixMode);
            GL13.glActiveTexture(this.active);
            GL20.glUseProgram(this.program);
            GL11.glClearColor(this.clear.get(0), this.clear.get(1), this.clear.get(2), this.clear.get(3));
            for (int i = 0; i < this.caps.length; i++) {
                if (this.enabled[i]) {
                    GL11.glEnable(this.caps[i]);
                } else {
                    GL11.glDisable(this.caps[i]);
                }
            }
        }
    }
}
