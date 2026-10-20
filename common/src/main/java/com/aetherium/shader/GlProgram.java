package com.aetherium.shader;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/** A linked shader program with its active uniforms. Render thread only. */
public final class GlProgram {
    public static final int ATTR_ENTITY = 10;
    public static final int ATTR_MID_TEX = 11;
    public static final int ATTR_TANGENT = 12;

    public static final int GL_GEOMETRY_SHADER = 0x8DD9;
    static final int GL_SAMPLER_2D_SHADOW = 0x8B62;

    /** One active uniform: location, GL type, and the last value set (skips redundant uploads). */
    public static final class Uniform {
        public final String name;
        public final int location;
        public final int type;
        float[] last;

        Uniform(final String name, final int location, final int type) {
            this.name = name;
            this.location = location;
            this.type = type;
        }

        public boolean isSampler() {
            return (this.type >= 0x8B5D && this.type <= 0x8B64) || (this.type >= 0x8DC0 && this.type <= 0x8DD8)
                    || this.type == 0x9108 || this.type == 0x9109;
        }
    }

    public final String name;
    public final int id;
    public final Map<String, Uniform> uniforms;
    /** Sampler uniforms in declaration order. */
    public final List<Uniform> samplers = new ArrayList<>();
    public final ProgramSource source;
    /** Bit k: reads Aetherium's texture matrix k (GlslPatcher#ownTextureMatrices). */
    public int textureMatrices;
    /** Frame counter of the last full uniform upload. */
    public int uploadedFrame = -1;
    /** Texture unit per sampler name (assigned by the runtime). */
    public final Map<String, Integer> units = new HashMap<>();

    private GlProgram(final String name, final int id, final Map<String, Uniform> uniforms, final ProgramSource source) {
        this.name = name;
        this.id = id;
        this.uniforms = uniforms;
        this.source = source;
        for (final Uniform u : uniforms.values()) {
            if (u.isSampler()) {
                this.samplers.add(u);
            }
        }
    }

    /** Thrown with a readable log (file:line mapped) when a pack program does not build. */
    public static final class BuildException extends Exception {
        private static final long serialVersionUID = 1L;

        public BuildException(final String message) {
            super(message);
        }
    }

    private static final Pattern ERR_LINE = Pattern.compile("(?:^|\\s)(?:ERROR:\\s*)?\\d+[:(](\\d+)\\)?\\s*:?");

    /**
     * Compiles and links. {@code geometry} may be null. {@code fragOutputs} are bound with
     * glBindFragDataLocation before linking (GL 3.0+ only).
     */
    public static GlProgram build(final String name, final String vertex, final String fragment, final String geometry,
                                  final ProgramSource source, final boolean bindFragData, final int headerLines)
            throws BuildException {
        final int vs = compile(GL20.GL_VERTEX_SHADER, vertex, name + ".vsh", source == null ? null : source.vertex,
                headerLines);
        int gs = 0;
        final int fs;
        try {
            fs = compile(GL20.GL_FRAGMENT_SHADER, fragment, name + ".fsh", source == null ? null : source.fragment,
                    headerLines);
            if (geometry != null) {
                gs = compile(GL_GEOMETRY_SHADER, geometry, name + ".gsh", source == null ? null : source.geometry,
                        headerLines);
            }
        } catch (final BuildException e) {
            GL20.glDeleteShader(vs);
            throw e;
        }
        final int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vs);
        GL20.glAttachShader(program, fs);
        if (gs != 0) {
            GL20.glAttachShader(program, gs);
        }
        GL20.glBindAttribLocation(program, ATTR_ENTITY, "mc_Entity");
        GL20.glBindAttribLocation(program, ATTR_MID_TEX, "mc_midTexCoord");
        GL20.glBindAttribLocation(program, ATTR_TANGENT, "at_tangent");
        if (bindFragData && source != null) {
            for (final Map.Entry<String, Integer> e : source.fragOutputs.entrySet()) {
                GL30.glBindFragDataLocation(program, e.getValue(), e.getKey());
            }
        }
        GL20.glLinkProgram(program);
        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);
        if (gs != 0) {
            GL20.glDeleteShader(gs);
        }
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            final String log = GL20.glGetProgramInfoLog(program);
            GL20.glDeleteProgram(program);
            throw new BuildException(name + ": link failed: " + trim(log));
        }
        return new GlProgram(name, program, reflect(program), source);
    }

    private static int compile(final int type, final String text, final String file, final ProgramSource.Expanded map,
                               final int headerLines) throws BuildException {
        final int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, text);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            final String log = GL20.glGetShaderInfoLog(shader);
            GL20.glDeleteShader(shader);
            final int nl = text.indexOf('\n');
            final String version = nl > 0 ? text.substring(0, nl).trim() : "";
            throw new BuildException(file + " (" + version + "): " + annotate(trim(log), map,
                    GlslPatcher.headerLines(text)));
        }
        return shader;
    }

    /** Appends "(lib/x.glsl:12)" after each line number the driver reports. */
    static String annotate(final String log, final ProgramSource.Expanded map, final int headerLines) {
        if (map == null) {
            return log;
        }
        final StringBuilder out = new StringBuilder();
        for (final String line : log.split("\n")) {
            final Matcher m = ERR_LINE.matcher(line);
            out.append(line);
            if (m.find()) {
                try {
                    final int n = Integer.parseInt(m.group(1)) - headerLines;
                    out.append("  [").append(map.origin(n)).append(']');
                    final String src = sourceLine(map.text, n);
                    if (!src.isEmpty()) {
                        out.append("  >> ").append(src);
                    }
                } catch (final NumberFormatException ignored) {
                    // no line number
                }
            }
            out.append('\n');
        }
        return out.toString().trim();
    }

    /** Line {@code n} (1-based) of the expanded source, trimmed and shortened. */
    static String sourceLine(final String text, final int n) {
        if (text == null || n < 1) {
            return "";
        }
        int start = 0;
        for (int i = 1; i < n; i++) {
            start = text.indexOf('\n', start) + 1;
            if (start <= 0) {
                return "";
            }
        }
        int end = text.indexOf('\n', start);
        end = end < 0 ? text.length() : end;
        final String line = text.substring(start, end).trim();
        return line.length() > 160 ? line.substring(0, 160) + "..." : line;
    }

    private static String trim(final String log) {
        final String s = log == null ? "" : log.trim();
        return s.length() > 4000 ? s.substring(0, 4000) + " ..." : s;
    }

    private static Map<String, Uniform> reflect(final int program) {
        final Map<String, Uniform> out = new LinkedHashMap<>();
        final int count = GL20.glGetProgrami(program, GL20.GL_ACTIVE_UNIFORMS);
        final IntBuffer size = BufferUtils.createIntBuffer(1);
        final IntBuffer type = BufferUtils.createIntBuffer(1);
        for (int i = 0; i < count; i++) {
            size.clear();
            type.clear();
            String n = GL20.glGetActiveUniform(program, i, 256, size, type);
            if (n == null || n.startsWith("gl_")) {
                continue;
            }
            if (n.endsWith("[0]")) {
                n = n.substring(0, n.length() - 3);
            }
            final int loc = GL20.glGetUniformLocation(program, n);
            if (loc >= 0) {
                out.put(n, new Uniform(n, loc, type.get(0)));
            }
        }
        return out;
    }

    public boolean has(final String uniform) {
        return this.uniforms.containsKey(uniform);
    }

    public boolean isShadowSampler(final String uniform) {
        final Uniform u = this.uniforms.get(uniform);
        return u != null && u.type == GL_SAMPLER_2D_SHADOW;
    }

    // ------------------------------------------------------------------ setters (program must be bound)

    private static boolean same(final Uniform u, final float a, final float b, final float c, final float d,
                                final int n) {
        final float[] l = u.last;
        if (l != null && l.length == n && l[0] == a && (n < 2 || l[1] == b) && (n < 3 || l[2] == c)
                && (n < 4 || l[3] == d)) {
            return true;
        }
        final float[] v = new float[n];
        v[0] = a;
        if (n > 1) {
            v[1] = b;
        }
        if (n > 2) {
            v[2] = c;
        }
        if (n > 3) {
            v[3] = d;
        }
        u.last = v;
        return false;
    }

    /** Sets a float/int/bool/vecN/ivecN uniform from up to 4 components, converting to its type. */
    public void set(final String name, final float a, final float b, final float c, final float d) {
        final Uniform u = this.uniforms.get(name);
        if (u == null) {
            return;
        }
        switch (u.type) {
            case GL11.GL_FLOAT:
                if (!same(u, a, 0, 0, 0, 1)) {
                    GL20.glUniform1f(u.location, a);
                }
                break;
            case GL20.GL_FLOAT_VEC2:
                if (!same(u, a, b, 0, 0, 2)) {
                    GL20.glUniform2f(u.location, a, b);
                }
                break;
            case GL20.GL_FLOAT_VEC3:
                if (!same(u, a, b, c, 0, 3)) {
                    GL20.glUniform3f(u.location, a, b, c);
                }
                break;
            case GL20.GL_FLOAT_VEC4:
                if (!same(u, a, b, c, d, 4)) {
                    GL20.glUniform4f(u.location, a, b, c, d);
                }
                break;
            case GL11.GL_INT:
            case GL20.GL_BOOL:
                if (!same(u, a, 0, 0, 0, 1)) {
                    GL20.glUniform1i(u.location, Math.round(a));
                }
                break;
            case GL20.GL_INT_VEC2:
            case GL20.GL_BOOL_VEC2:
                if (!same(u, a, b, 0, 0, 2)) {
                    GL20.glUniform2i(u.location, Math.round(a), Math.round(b));
                }
                break;
            case GL20.GL_INT_VEC3:
            case GL20.GL_BOOL_VEC3:
                if (!same(u, a, b, c, 0, 3)) {
                    GL20.glUniform3i(u.location, Math.round(a), Math.round(b), Math.round(c));
                }
                break;
            case GL20.GL_INT_VEC4:
            case GL20.GL_BOOL_VEC4:
                if (!same(u, a, b, c, d, 4)) {
                    GL20.glUniform4i(u.location, Math.round(a), Math.round(b), Math.round(c), Math.round(d));
                }
                break;
            default:
                if (u.isSampler() && !same(u, a, 0, 0, 0, 1)) {
                    GL20.glUniform1i(u.location, Math.round(a));
                }
                break;
        }
    }

    public void set(final String name, final float v) {
        this.set(name, v, 0, 0, 0);
    }

    public void setMat4(final String name, final float[] m) {
        final Uniform u = this.uniforms.get(name);
        if (u == null || u.type != GL20.GL_FLOAT_MAT4) {
            return;
        }
        if (u.last != null && java.util.Arrays.equals(u.last, m)) {
            return;
        }
        u.last = m.clone();
        GL20.glUniformMatrix4fv(u.location, false, m);
    }

    public void delete() {
        GL20.glDeleteProgram(this.id);
    }
}
