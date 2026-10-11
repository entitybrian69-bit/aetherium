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
    public static int glGetError() { return 0; }
}
