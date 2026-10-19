package com.aetherium.shader;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * The textures and framebuffers of a running pack: colortex0-15 (each a main/alt ping-pong pair),
 * depthtex0-2, and the shadow maps. Render thread only.
 */
public final class Targets {
    /** One colortex: two textures; "main" is {@code flipped ? b : a}. */
    public static final class Color {
        public final int index;
        final int internal;
        final int format;
        final int type;
        final boolean integer;
        int a;
        int b;
        boolean flipped;
        boolean clear = true;
        float[] clearColor;
        boolean mipmap;
        /** Size relative to the screen (or fixed when {@code fixedW > 0}). */
        float scaleW = 1;
        float scaleH = 1;
        int fixedW;
        int fixedH;
        int width;
        int height;

        Color(final int index, final int[] fmt) {
            this.index = index;
            this.internal = fmt[0];
            this.format = fmt[1];
            this.type = fmt[2];
            this.integer = fmt[3] != 0;
        }

        public int main() {
            return this.flipped ? this.b : this.a;
        }

        public int alt() {
            return this.flipped ? this.a : this.b;
        }

        public void flip() {
            this.flipped = !this.flipped;
        }
    }

    public final Color[] colors = new Color[16];
    public int depth0;
    public int depth1;
    public int depth2;
    public int width;
    public int height;
    private final Map<String, Integer> fbos = new HashMap<>();
    private final boolean compat;
    private final boolean floats;

    // shadow maps
    public int shadowRes;
    public int shadowDepth0;
    public int shadowDepth1;
    public final int[] shadowColor = new int[2];
    public final float[][] shadowClear = {{1, 1, 1, 1}, {1, 1, 1, 1}};
    public final boolean[] shadowClearOn = {true, true};

    public Targets(final boolean compat, final boolean floats) {
        this.compat = compat;
        this.floats = floats;
    }

    // ------------------------------------------------------------------ formats

    /** {internal, format, type, integer?1:0} for an OptiFine format name; RGBA8 when unknown. */
    public int[] format(final String name) {
        final String n = name == null ? "RGBA8" : name.trim().toUpperCase(Locale.ROOT);
        final int ub = GL11.GL_UNSIGNED_BYTE;
        final int fl = GL11.GL_FLOAT;
        final int us = GL11.GL_UNSIGNED_SHORT;
        int[] f;
        switch (n) {
            case "R8": f = new int[] {0x8229, 0x1903, ub, 0}; break;
            case "RG8": f = new int[] {0x822B, 0x8227, ub, 0}; break;
            case "RGB8": f = new int[] {0x8051, GL11.GL_RGB, ub, 0}; break;
            case "R8_SNORM": f = new int[] {0x8F94, 0x1903, GL11.GL_BYTE, 0}; break;
            case "RG8_SNORM": f = new int[] {0x8F95, 0x8227, GL11.GL_BYTE, 0}; break;
            case "RGB8_SNORM": f = new int[] {0x8F96, GL11.GL_RGB, GL11.GL_BYTE, 0}; break;
            case "RGBA8_SNORM": f = new int[] {0x8F97, GL11.GL_RGBA, GL11.GL_BYTE, 0}; break;
            case "R16": f = new int[] {0x822A, 0x1903, us, 0}; break;
            case "RG16": f = new int[] {0x822C, 0x8227, us, 0}; break;
            case "RGB16": f = new int[] {0x8054, GL11.GL_RGB, us, 0}; break;
            case "RGBA16": f = new int[] {0x805B, GL11.GL_RGBA, us, 0}; break;
            case "R16_SNORM": f = new int[] {0x8F98, 0x1903, GL11.GL_SHORT, 0}; break;
            case "RG16_SNORM": f = new int[] {0x8F99, 0x8227, GL11.GL_SHORT, 0}; break;
            case "RGB16_SNORM": f = new int[] {0x8F9A, GL11.GL_RGB, GL11.GL_SHORT, 0}; break;
            case "RGBA16_SNORM": f = new int[] {0x8F9B, GL11.GL_RGBA, GL11.GL_SHORT, 0}; break;
            case "R16F": f = new int[] {0x822D, 0x1903, fl, 0}; break;
            case "RG16F": f = new int[] {0x822F, 0x8227, fl, 0}; break;
            case "RGB16F": f = new int[] {0x881B, GL11.GL_RGB, fl, 0}; break;
            case "RGBA16F": f = new int[] {0x881A, GL11.GL_RGBA, fl, 0}; break;
            case "R32F": f = new int[] {0x822E, 0x1903, fl, 0}; break;
            case "RG32F": f = new int[] {0x8230, 0x8227, fl, 0}; break;
            case "RGB32F": f = new int[] {0x8815, GL11.GL_RGB, fl, 0}; break;
            case "RGBA32F": f = new int[] {0x8814, GL11.GL_RGBA, fl, 0}; break;
            case "R3_G3_B2": f = new int[] {0x2A10, GL11.GL_RGB, ub, 0}; break;
            case "RGB5_A1": f = new int[] {0x8057, GL11.GL_RGBA, ub, 0}; break;
            case "RGB10_A2": f = new int[] {0x8059, GL11.GL_RGBA, ub, 0}; break;
            case "R11F_G11F_B10F": f = new int[] {0x8C3A, GL11.GL_RGB, fl, 0}; break;
            case "RGB9_E5": f = new int[] {0x8C3D, GL11.GL_RGB, fl, 0}; break;
            case "R8I": f = new int[] {0x8231, 0x8D94, GL11.GL_BYTE, 1}; break;
            case "R8UI": f = new int[] {0x8232, 0x8D94, ub, 1}; break;
            case "R16I": f = new int[] {0x8233, 0x8D94, GL11.GL_SHORT, 1}; break;
            case "R16UI": f = new int[] {0x8234, 0x8D94, us, 1}; break;
            case "R32I": f = new int[] {0x8235, 0x8D94, GL11.GL_INT, 1}; break;
            case "R32UI": f = new int[] {0x8236, 0x8D94, GL11.GL_UNSIGNED_INT, 1}; break;
            case "RG8I": f = new int[] {0x8237, 0x8228, GL11.GL_BYTE, 1}; break;
            case "RG8UI": f = new int[] {0x8238, 0x8228, ub, 1}; break;
            case "RG16I": f = new int[] {0x8239, 0x8228, GL11.GL_SHORT, 1}; break;
            case "RG16UI": f = new int[] {0x823A, 0x8228, us, 1}; break;
            case "RG32I": f = new int[] {0x823B, 0x8228, GL11.GL_INT, 1}; break;
            case "RG32UI": f = new int[] {0x823C, 0x8228, GL11.GL_UNSIGNED_INT, 1}; break;
            case "RGBA8I": f = new int[] {0x8D8E, 0x8D99, GL11.GL_BYTE, 1}; break;
            case "RGBA8UI": f = new int[] {0x8D7C, 0x8D99, ub, 1}; break;
            case "RGBA16I": f = new int[] {0x8D88, 0x8D99, GL11.GL_SHORT, 1}; break;
            case "RGBA16UI": f = new int[] {0x8D76, 0x8D99, us, 1}; break;
            case "RGBA32I": f = new int[] {0x8D82, 0x8D99, GL11.GL_INT, 1}; break;
            case "RGBA32UI": f = new int[] {0x8D70, 0x8D99, GL11.GL_UNSIGNED_INT, 1}; break;
            default: f = new int[] {GL11.GL_RGBA8, GL11.GL_RGBA, ub, 0}; break;
        }
        if (this.compat) {
            // GLES-safe: RGBA8, or RGBA16F for float formats when half floats are renderable
            final boolean isFloat = f[2] == fl;
            if (isFloat && this.floats) {
                return new int[] {0x881A, GL11.GL_RGBA, fl, 0};
            }
            return new int[] {GL11.GL_RGBA8, GL11.GL_RGBA, ub, 0};
        }
        if (f[2] == fl && !this.floats) {
            return new int[] {GL11.GL_RGBA8, GL11.GL_RGBA, ub, 0};
        }
        return f;
    }

    // ------------------------------------------------------------------ setup

    /** Declares a colortex (format name, clear settings, size); textures are made by {@link #resize}. */
    public Color declare(final int index, final String format) {
        final Color c = new Color(index, this.format(format));
        this.colors[index] = c;
        return c;
    }

    public static void configureSize(final Color c, final String spec) {
        if (spec == null) {
            return;
        }
        final String[] p = spec.trim().split("\\s+");
        if (p.length < 2) {
            return;
        }
        final double w = Preprocessor.parseNumber(p[0]);
        final double h = Preprocessor.parseNumber(p[1]);
        if (Double.isNaN(w) || Double.isNaN(h)) {
            return;
        }
        if (p[0].contains(".")) {
            c.scaleW = (float) w;
            c.scaleH = (float) h;
        } else {
            c.fixedW = (int) w;
            c.fixedH = (int) h;
        }
    }

    public static void configureClear(final Color c, final boolean clear, final float[] color) {
        c.clear = clear;
        c.clearColor = color;
    }

    public static void configureMipmap(final Color c, final boolean on) {
        c.mipmap = on;
    }

    /** (Re)creates every texture for a screen size; framebuffers are rebuilt lazily. */
    public void resize(final int w, final int h) {
        if (w == this.width && h == this.height && this.depth0 != 0) {
            return;
        }
        this.deleteScreenTextures();
        this.width = w;
        this.height = h;
        for (final Color c : this.colors) {
            if (c == null) {
                continue;
            }
            c.width = c.fixedW > 0 ? c.fixedW : Math.max(1, Math.round(w * c.scaleW));
            c.height = c.fixedH > 0 ? c.fixedH : Math.max(1, Math.round(h * c.scaleH));
            c.a = this.colorTexture(c);
            c.b = this.colorTexture(c);
            c.flipped = false;
        }
        this.depth0 = this.depthTexture(w, h);
        this.depth1 = this.depthTexture(w, h);
        this.depth2 = this.depthTexture(w, h);
    }

    private int colorTexture(final Color c) {
        final int t = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, t);
        final int filter = c.integer ? GL11.GL_NEAREST : GL11.GL_LINEAR;
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, c.mipmap && !c.integer
                ? GL11.GL_LINEAR_MIPMAP_LINEAR : filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, c.internal, c.width, c.height, 0, c.format, c.type,
                (ByteBuffer) null);
        return t;
    }

    private int depthTexture(final int w, final int h) {
        final int t = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, t);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, this.compat ? GL11.GL_DEPTH_COMPONENT : GL14.GL_DEPTH_COMPONENT24, w, h,
                0, GL11.GL_DEPTH_COMPONENT, GL11.GL_UNSIGNED_INT, (ByteBuffer) null);
        return t;
    }

    /** Shadow maps of {@code res}²; {@code colors} shadowcolor buffers (0-2) with formats. */
    public void createShadow(final int res, final int colors, final String[] formats, final boolean hardwareFilter0,
                             final boolean hardwareFilter1, final boolean nearest0, final boolean nearest1) {
        this.deleteShadow();
        this.shadowRes = res;
        this.shadowDepth0 = this.depthTexture(res, res);
        this.shadowDepth1 = this.depthTexture(res, res);
        shadowFilter(this.shadowDepth0, hardwareFilter0, nearest0);
        shadowFilter(this.shadowDepth1, hardwareFilter1, nearest1);
        for (int i = 0; i < colors && i < 2; i++) {
            final Color tmp = new Color(-1, this.format(formats[i]));
            tmp.width = res;
            tmp.height = res;
            this.shadowColor[i] = this.colorTexture(tmp);
        }
    }

    private static void shadowFilter(final int tex, final boolean compare, final boolean nearest) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        final int f = nearest ? GL11.GL_NEAREST : GL11.GL_LINEAR;
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, f);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, f);
        if (compare) {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, GL14.GL_COMPARE_R_TO_TEXTURE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_FUNC, GL11.GL_LEQUAL);
        }
    }

    // ------------------------------------------------------------------ framebuffers

    /**
     * A framebuffer with these color textures (attachments 0..n-1, n may be 0) and optional depth.
     * Draw buffers are set when it is created.
     */
    public int fbo(final int[] colorTextures, final int depth) {
        final StringBuilder key = new StringBuilder();
        for (final int t : colorTextures) {
            key.append(t).append(',');
        }
        key.append('d').append(depth);
        final String k = key.toString();
        final Integer cached = this.fbos.get(k);
        if (cached != null) {
            return cached;
        }
        final int fb = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fb);
        for (int i = 0; i < colorTextures.length; i++) {
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0 + i, GL11.GL_TEXTURE_2D,
                    colorTextures[i], 0);
        }
        if (depth != 0) {
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, depth, 0);
        }
        if (colorTextures.length == 0) {
            GL11.glDrawBuffer(GL11.GL_NONE);
            GL11.glReadBuffer(GL11.GL_NONE);
        } else if (colorTextures.length == 1) {
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
        } else {
            final int[] bufs = new int[colorTextures.length];
            for (int i = 0; i < bufs.length; i++) {
                bufs[i] = GL30.GL_COLOR_ATTACHMENT0 + i;
            }
            GL20.glDrawBuffers(bufs);
        }
        this.fbos.put(k, fb);
        return fb;
    }

    public boolean complete(final int fbo) {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        return GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) == GL30.GL_FRAMEBUFFER_COMPLETE;
    }

    /** Main textures of the given colortex indexes (missing ones skipped). */
    public int[] mains(final int[] indexes) {
        final int[] out = new int[indexes.length];
        int n = 0;
        for (final int i : indexes) {
            if (i >= 0 && i < 16 && this.colors[i] != null) {
                out[n++] = this.colors[i].main();
            }
        }
        return n == out.length ? out : java.util.Arrays.copyOf(out, n);
    }

    public int[] alts(final int[] indexes) {
        final int[] out = new int[indexes.length];
        int n = 0;
        for (final int i : indexes) {
            if (i >= 0 && i < 16 && this.colors[i] != null) {
                out[n++] = this.colors[i].alt();
            }
        }
        return n == out.length ? out : java.util.Arrays.copyOf(out, n);
    }

    /** Clears the main texture of every buffer that wants it (colortex0 is cleared by vanilla). */
    public void clearMains(final float[] fog) {
        for (final Color c : this.colors) {
            if (c == null || !c.clear || c.integer) {
                continue;
            }
            float[] cc = c.clearColor;
            if (cc == null) {
                if (c.index == 0) {
                    cc = new float[] {fog[0], fog[1], fog[2], 1};
                } else if (c.index == 1) {
                    cc = new float[] {1, 1, 1, 1};
                } else {
                    cc = new float[] {0, 0, 0, 0};
                }
            }
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.fbo(new int[] {c.main()}, 0));
            GL11.glViewport(0, 0, c.width, c.height);
            GL11.glClearColor(cc[0], cc[1], cc[2], cc[3]);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        }
    }

    public void clearShadow() {
        final int[] cols = this.shadowColor[1] != 0 ? new int[] {this.shadowColor[0], this.shadowColor[1]}
                : this.shadowColor[0] != 0 ? new int[] {this.shadowColor[0]} : new int[0];
        for (int i = 0; i < cols.length; i++) {
            if (!this.shadowClearOn[i]) {
                continue;
            }
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.fbo(new int[] {cols[i]}, 0));
            GL11.glViewport(0, 0, this.shadowRes, this.shadowRes);
            final float[] cc = this.shadowClear[i];
            GL11.glClearColor(cc[0], cc[1], cc[2], cc[3]);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.fbo(cols, this.shadowDepth0));
        GL11.glViewport(0, 0, this.shadowRes, this.shadowRes);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
    }

    public void generateMipmap(final int texture) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
    }

    // ------------------------------------------------------------------ cleanup

    private void forgetFbos() {
        for (final int fb : this.fbos.values()) {
            GL30.glDeleteFramebuffers(fb);
        }
        this.fbos.clear();
    }

    private void deleteScreenTextures() {
        this.forgetFbos();
        for (final Color c : this.colors) {
            if (c != null && c.a != 0) {
                GL11.glDeleteTextures(c.a);
                GL11.glDeleteTextures(c.b);
                c.a = 0;
                c.b = 0;
            }
        }
        for (final int t : new int[] {this.depth0, this.depth1, this.depth2}) {
            if (t != 0) {
                GL11.glDeleteTextures(t);
            }
        }
        this.depth0 = 0;
        this.depth1 = 0;
        this.depth2 = 0;
        this.width = 0;
        this.height = 0;
    }

    private void deleteShadow() {
        this.forgetFbos();
        for (final int t : new int[] {this.shadowDepth0, this.shadowDepth1, this.shadowColor[0], this.shadowColor[1]}) {
            if (t != 0) {
                GL11.glDeleteTextures(t);
            }
        }
        this.shadowDepth0 = 0;
        this.shadowDepth1 = 0;
        this.shadowColor[0] = 0;
        this.shadowColor[1] = 0;
    }

    public void delete() {
        this.deleteScreenTextures();
        this.deleteShadow();
    }
}
