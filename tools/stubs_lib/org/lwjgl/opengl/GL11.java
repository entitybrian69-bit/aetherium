package org.lwjgl.opengl;

import java.nio.FloatBuffer;

/** Offline compile stand-in: only the LWJGL 3 GL11 methods Aetherium calls (signatures as in 3.2+). */
public class GL11 {
    public static String glGetString(int name) { return null; }
    public static boolean glIsEnabled(int cap) { return false; }
    public static int glGetInteger(int pname) { return 0; }
    public static float glGetFloat(int pname) { return 0f; }
    public static void glGetFloatv(int pname, FloatBuffer params) { }
    public static void glDrawElements(int mode, int count, int type, long indices) { }
    public static void glDrawArrays(int mode, int first, int count) { }
    public static int glGetError() { return 0; }
    public static void glViewport(int x, int y, int w, int h) { }
    public static void glEnableClientState(int cap) { }
    public static void glDisableClientState(int cap) { }
    public static int glGenTextures() { return 0; }
    public static void glDeleteTextures(int texture) { }
    public static void glBindTexture(int target, int texture) { }
    public static void glTexParameteri(int target, int pname, int param) { }
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, java.nio.ByteBuffer pixels) { }
    public static void glGetIntegerv(int pname, int[] params) { }
    public static void glEnable(int cap) { }
    public static void glDisable(int cap) { }
    public static void glGetBooleanv(int pname, java.nio.ByteBuffer params) { }
    public static void glDepthFunc(int func) { }
    public static void glDepthMask(boolean flag) { }
    public static void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) { }
    public static void glReadPixels(int x, int y, int width, int height, int format, int type, java.nio.ByteBuffer pixels) { }
}
