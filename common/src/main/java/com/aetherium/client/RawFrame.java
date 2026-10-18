package com.aetherium.client;

import com.aetherium.util.AetheriumLog;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
// @era:t-raw-frame-begin r17|r212
// @era:t-raw-frame-else r215
//~ import com.mojang.blaze3d.opengl.GlTexture;
//~ import com.mojang.blaze3d.textures.GpuTexture;
// @era:t-raw-frame-else none
// @era:t-raw-frame-end

/**
 * Screen-sized copies of the main colour buffer with plain OpenGL 3 calls (Minecraft 1.17+), for
 * the menu world picture ({@link WorldCache}) and the colour filter ({@link ColorFilter}).
 *
 * <p>From 1.17 on, {@code RenderTarget.blitToScreen} binds vanilla's own core-profile shader, so
 * the 1.16.5 way (blit through a program of our own) no longer works. This class owns its
 * framebuffers, textures and program, copies with {@code glBlitFramebuffer}, grades with a single
 * full-screen triangle, and puts every piece of GL state it touches back exactly as it was, so
 * vanilla's state caches stay right. Nothing here runs unless one of the two features is on.</p>
 *
 * <p>Unavailable (every call returns false) where the main colour buffer is not a plain GL texture:
 * 1.16.5 (it keeps its own helpers) and 26.2+ (selectable Vulkan backend).</p>
 */
public final class RawFrame {
    private RawFrame() {
    }

    private static final AetheriumLog LOGGER = AetheriumLog.of(RawFrame.class);

    private static final int GL_TEXTURE_2D = 0x0DE1;
    private static final int GL_RGBA8 = 0x8058;
    private static final int GL_RGBA = 0x1908;
    private static final int GL_UNSIGNED_BYTE = 0x1401;
    private static final int GL_TEXTURE_MIN_FILTER = 0x2801;
    private static final int GL_TEXTURE_MAG_FILTER = 0x2800;
    private static final int GL_NEAREST = 0x2600;
    private static final int GL_READ_FRAMEBUFFER = 0x8CA8;
    private static final int GL_DRAW_FRAMEBUFFER = 0x8CA9;
    private static final int GL_READ_FRAMEBUFFER_BINDING = 0x8CAA;
    private static final int GL_DRAW_FRAMEBUFFER_BINDING = 0x8CA6;
    private static final int GL_COLOR_ATTACHMENT0 = 0x8CE0;
    private static final int GL_FRAMEBUFFER_COMPLETE = 0x8CD5;
    private static final int GL_COLOR_BUFFER_BIT = 0x4000;
    private static final int GL_CURRENT_PROGRAM = 0x8B8D;
    private static final int GL_VERTEX_ARRAY_BINDING = 0x85B5;
    private static final int GL_ACTIVE_TEXTURE = 0x84E0;
    private static final int GL_TEXTURE0 = 0x84C0;
    private static final int GL_TEXTURE_BINDING_2D = 0x8069;
    private static final int GL_VIEWPORT = 0x0BA2;
    private static final int GL_BLEND = 0x0BE2;
    private static final int GL_DEPTH_TEST = 0x0B71;
    private static final int GL_CULL_FACE = 0x0B44;
    private static final int GL_SCISSOR_TEST = 0x0C11;
    private static final int GL_TRIANGLES = 0x0004;
    private static final int GL_VERTEX_SHADER = 0x8B31;
    private static final int GL_FRAGMENT_SHADER = 0x8B30;
    private static final int GL_COMPILE_STATUS = 0x8B81;
    private static final int GL_LINK_STATUS = 0x8B82;

    /** Full-screen triangle from the vertex index: no vertex buffer needed. */
    private static final String VERTEX = "#version 150\n"
            + "void main() {\n"
            + "    vec2 p = vec2(float((gl_VertexID << 1) & 2), float(gl_VertexID & 2));\n"
            + "    gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);\n"
            + "}\n";
    /** Same grade as the 1.16.5 filter; texel-exact reads, so samplers and filtering do not matter. */
    private static final String FRAGMENT = "#version 150\n"
            + "uniform sampler2D Source;\n"
            + "uniform vec4 Grade;\n"
            + "uniform float Brightness;\n"
            + "uniform vec2 Size;\n"
            + "out vec4 fragColor;\n"
            + "void main() {\n"
            + "    vec3 c = texelFetch(Source, ivec2(gl_FragCoord.xy), 0).rgb;\n"
            + "    float luma = dot(c, vec3(0.2126, 0.7152, 0.0722));\n"
            + "    c = mix(vec3(luma), c, Grade.x);\n"
            + "    c = (c - 0.5) * Grade.y + 0.5 + Brightness;\n"
            + "    c += vec3(Grade.z, Grade.z * 0.35, -Grade.z);\n"
            + "    vec2 d = gl_FragCoord.xy / Size - 0.5;\n"
            + "    c *= 1.0 - Grade.w * dot(d, d) * 1.6;\n"
            + "    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);\n"
            + "}\n";

    /** A screen-sized texture with its own framebuffer. */
    public static final class Slot {
        private int texture;
        private int framebuffer;
        private int width;
        private int height;

        /** GL objects exist for this slot. */
        public boolean allocated() {
            return this.texture != 0;
        }
    }

    /** Framebuffer around the main colour texture (re-attached when the texture changes). */
    private static int source;
    private static int sourceTexture;
    private static int sourceWidth;
    private static int sourceHeight;
    private static int program;
    private static int vao;
    private static int gradeLocation;
    private static int brightnessLocation;
    private static int sizeLocation;
    private static boolean programBroken;
    private static final int[] VIEWPORT = new int[4];

    /** OpenGL name of the main colour texture, or -1 where it cannot be reached. */
    static int mainTexture(final Minecraft mc) {
        // @era:t-raw-frame-begin r17|r212
        return mc.getMainRenderTarget().getColorTextureId();
        // @era:t-raw-frame-else r215
        //~ final GpuTexture texture = mc.getMainRenderTarget().getColorTexture();
        //~ return texture instanceof GlTexture ? ((GlTexture) texture).glId() : -1;
        // @era:t-raw-frame-else none
        //~ return -1;
        // @era:t-raw-frame-end
    }

    /** Size of the main colour buffer. */
    static int width(final Minecraft mc) {
        // @era:t-raw-frame-begin r17|r212|r215
        return mc.getMainRenderTarget().width;
        // @era:t-raw-frame-else none
        //~ return 0;
        // @era:t-raw-frame-end
    }

    static int height(final Minecraft mc) {
        // @era:t-raw-frame-begin r17|r212|r215
        return mc.getMainRenderTarget().height;
        // @era:t-raw-frame-else none
        //~ return 0;
        // @era:t-raw-frame-end
    }

    /** A vanilla post effect is on (spectating a creeper, an enderman, ...). */
    public static boolean postEffect(final Minecraft mc) {
        // @era:t-raw-frame-begin r17
        return mc.gameRenderer.currentEffect() != null;
        // @era:t-raw-frame-else r212|r215
        //~ return mc.gameRenderer.currentPostEffect() != null;
        // @era:t-raw-frame-else none
        //~ return true;
        // @era:t-raw-frame-end
    }

    /** Main colour buffer into {@code slot} (sized to match). False when it could not be done. */
    public static boolean save(final Minecraft mc, final Slot slot) {
        final int main = mainTexture(mc);
        if (main <= 0) {
            return false;
        }
        final int w = width(mc);
        final int h = height(mc);
        if (!prepare(slot, w, h) || !attachSource(main, w, h)) {
            return false;
        }
        copy(source, slot.framebuffer, w, h);
        return true;
    }

    /** {@code slot} back into the main colour buffer, unchanged. False on a size mismatch. */
    public static boolean restore(final Minecraft mc, final Slot slot) {
        final int main = mainTexture(mc);
        final int w = width(mc);
        final int h = height(mc);
        if (main <= 0 || !slot.allocated() || slot.width != w || slot.height != h || !attachSource(main, w, h)) {
            return false;
        }
        copy(slot.framebuffer, source, w, h);
        return true;
    }

    /**
     * {@code slot} into the main colour buffer through the colour grade ({@code grade} = saturation,
     * contrast, warmth, vignette). False when the driver rejected the program.
     */
    public static boolean grade(final Minecraft mc, final Slot slot, final float[] grade, final float brightness) {
        final int main = mainTexture(mc);
        final int w = width(mc);
        final int h = height(mc);
        if (main <= 0 || !slot.allocated() || slot.width != w || slot.height != h || !attachSource(main, w, h)) {
            return false;
        }
        if (program == 0 && (programBroken || !compile())) {
            return false;
        }
        final int drawFramebuffer = GL11.glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        final int oldProgram = GL11.glGetInteger(GL_CURRENT_PROGRAM);
        final int oldVao = GL11.glGetInteger(GL_VERTEX_ARRAY_BINDING);
        final int oldActive = GL11.glGetInteger(GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(GL_TEXTURE0);
        final int oldTexture = GL11.glGetInteger(GL_TEXTURE_BINDING_2D);
        GL11.glGetIntegerv(GL_VIEWPORT, VIEWPORT);
        final boolean blend = GL11.glIsEnabled(GL_BLEND);
        final boolean depth = GL11.glIsEnabled(GL_DEPTH_TEST);
        final boolean cull = GL11.glIsEnabled(GL_CULL_FACE);
        final boolean scissor = GL11.glIsEnabled(GL_SCISSOR_TEST);
        try {
            GL30.glBindFramebuffer(GL_DRAW_FRAMEBUFFER, source);
            GL11.glViewport(0, 0, w, h);
            GL11.glDisable(GL_BLEND);
            GL11.glDisable(GL_DEPTH_TEST);
            GL11.glDisable(GL_CULL_FACE);
            GL11.glDisable(GL_SCISSOR_TEST);
            GL20.glUseProgram(program);
            GL20.glUniform4f(gradeLocation, grade[0], grade[1], grade[2], grade[3]);
            GL20.glUniform1f(brightnessLocation, brightness);
            GL20.glUniform2f(sizeLocation, w, h);
            GL11.glBindTexture(GL_TEXTURE_2D, slot.texture);
            GL30.glBindVertexArray(vao);
            GL11.glDrawArrays(GL_TRIANGLES, 0, 3);
        } finally {
            GL30.glBindVertexArray(oldVao);
            GL11.glBindTexture(GL_TEXTURE_2D, oldTexture);
            GL13.glActiveTexture(oldActive);
            GL20.glUseProgram(oldProgram);
            enable(GL_BLEND, blend);
            enable(GL_DEPTH_TEST, depth);
            enable(GL_CULL_FACE, cull);
            enable(GL_SCISSOR_TEST, scissor);
            GL11.glViewport(VIEWPORT[0], VIEWPORT[1], VIEWPORT[2], VIEWPORT[3]);
            GL30.glBindFramebuffer(GL_DRAW_FRAMEBUFFER, drawFramebuffer);
        }
        return true;
    }

    /** Frees the slot's GL objects (safe to call on an empty slot). */
    public static void free(final Slot slot) {
        if (slot.framebuffer != 0) {
            GL30.glDeleteFramebuffers(slot.framebuffer);
            slot.framebuffer = 0;
        }
        if (slot.texture != 0) {
            GL11.glDeleteTextures(slot.texture);
            slot.texture = 0;
        }
        slot.width = 0;
        slot.height = 0;
    }

    private static void copy(final int from, final int to, final int w, final int h) {
        final int read = GL11.glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        final int draw = GL11.glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        final boolean scissor = GL11.glIsEnabled(GL_SCISSOR_TEST);
        try {
            if (scissor) {
                GL11.glDisable(GL_SCISSOR_TEST);
            }
            GL30.glBindFramebuffer(GL_READ_FRAMEBUFFER, from);
            GL30.glBindFramebuffer(GL_DRAW_FRAMEBUFFER, to);
            GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_COLOR_BUFFER_BIT, GL_NEAREST);
        } finally {
            GL30.glBindFramebuffer(GL_READ_FRAMEBUFFER, read);
            GL30.glBindFramebuffer(GL_DRAW_FRAMEBUFFER, draw);
            if (scissor) {
                GL11.glEnable(GL_SCISSOR_TEST);
            }
        }
    }

    /** Allocates or resizes the slot's texture and framebuffer. */
    private static boolean prepare(final Slot slot, final int w, final int h) {
        if (slot.allocated() && slot.width == w && slot.height == h) {
            return true;
        }
        final int oldTexture = GL11.glGetInteger(GL_TEXTURE_BINDING_2D);
        final int read = GL11.glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        try {
            if (slot.texture == 0) {
                slot.texture = GL11.glGenTextures();
            }
            GL11.glBindTexture(GL_TEXTURE_2D, slot.texture);
            GL11.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            GL11.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            GL11.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
            if (slot.framebuffer == 0) {
                slot.framebuffer = GL30.glGenFramebuffers();
            }
            GL30.glBindFramebuffer(GL_READ_FRAMEBUFFER, slot.framebuffer);
            GL30.glFramebufferTexture2D(GL_READ_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, slot.texture, 0);
            final boolean complete = GL30.glCheckFramebufferStatus(GL_READ_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE;
            slot.width = w;
            slot.height = h;
            if (!complete) {
                LOGGER.warn("Screen copy framebuffer incomplete; feature skipped");
                GL30.glBindFramebuffer(GL_READ_FRAMEBUFFER, read);
                free(slot);
                return false;
            }
            return true;
        } finally {
            GL11.glBindTexture(GL_TEXTURE_2D, oldTexture);
            GL30.glBindFramebuffer(GL_READ_FRAMEBUFFER, read);
        }
    }

    /**
     * Points our framebuffer at the main colour texture. Re-attached when its name or size changes:
     * a deleted texture stays attached to framebuffers that are not bound, even if GL hands its name
     * out again.
     */
    private static boolean attachSource(final int main, final int w, final int h) {
        if (source != 0 && sourceTexture == main && sourceWidth == w && sourceHeight == h) {
            return true;
        }
        final int read = GL11.glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        try {
            if (source == 0) {
                source = GL30.glGenFramebuffers();
            }
            GL30.glBindFramebuffer(GL_READ_FRAMEBUFFER, source);
            GL30.glFramebufferTexture2D(GL_READ_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, main, 0);
            if (GL30.glCheckFramebufferStatus(GL_READ_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                sourceTexture = 0;
                return false;
            }
            sourceTexture = main;
            sourceWidth = w;
            sourceHeight = h;
            return true;
        } finally {
            GL30.glBindFramebuffer(GL_READ_FRAMEBUFFER, read);
        }
    }

    private static void enable(final int cap, final boolean on) {
        if (on) {
            GL11.glEnable(cap);
        } else {
            GL11.glDisable(cap);
        }
    }

    private static boolean compile() {
        final int oldProgram = GL11.glGetInteger(GL_CURRENT_PROGRAM);
        try {
            final int vs = shader(GL_VERTEX_SHADER, VERTEX);
            if (vs == 0) {
                programBroken = true;
                return false;
            }
            final int fs = shader(GL_FRAGMENT_SHADER, FRAGMENT);
            if (fs == 0) {
                GL20.glDeleteShader(vs);
                programBroken = true;
                return false;
            }
            final int p = GL20.glCreateProgram();
            GL20.glAttachShader(p, vs);
            GL20.glAttachShader(p, fs);
            GL30.glBindFragDataLocation(p, 0, "fragColor");
            GL20.glLinkProgram(p);
            GL20.glDeleteShader(vs);
            GL20.glDeleteShader(fs);
            if (GL20.glGetProgrami(p, GL_LINK_STATUS) == 0) {
                LOGGER.warn("Colour filter: the driver could not link the program: " + GL20.glGetProgramInfoLog(p));
                GL20.glDeleteProgram(p);
                programBroken = true;
                return false;
            }
            GL20.glUseProgram(p);
            GL20.glUniform1i(GL20.glGetUniformLocation(p, "Source"), 0);
            gradeLocation = GL20.glGetUniformLocation(p, "Grade");
            brightnessLocation = GL20.glGetUniformLocation(p, "Brightness");
            sizeLocation = GL20.glGetUniformLocation(p, "Size");
            vao = GL30.glGenVertexArrays();
            program = p;
            return true;
        } finally {
            GL20.glUseProgram(oldProgram);
        }
    }

    private static int shader(final int type, final String text) {
        final int s = GL20.glCreateShader(type);
        GL20.glShaderSource(s, text);
        GL20.glCompileShader(s);
        if (GL20.glGetShaderi(s, GL_COMPILE_STATUS) == 0) {
            LOGGER.warn("Colour filter: the driver rejected a shader: " + GL20.glGetShaderInfoLog(s));
            GL20.glDeleteShader(s);
            return 0;
        }
        return s;
    }
}
