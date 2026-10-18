package org.lwjgl.opengl;

/** Offline compile stand-in: only the LWJGL 3 GL30 methods Aetherium calls. */
public class GL30 {
    public static void glBindFramebuffer(int target, int framebuffer) { }
    public static int glGenFramebuffers() { return 0; }
    public static void glDeleteFramebuffers(int framebuffer) { }
    public static void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) { }
    public static int glCheckFramebufferStatus(int target) { return 0; }
    public static void glBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) { }
    public static int glGenVertexArrays() { return 0; }
    public static void glBindVertexArray(int array) { }
    public static void glDeleteVertexArrays(int array) { }
    public static void glBindFragDataLocation(int program, int colorNumber, CharSequence name) { }
}
