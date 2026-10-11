package org.lwjgl.opengl;

import java.nio.ShortBuffer;

/** Offline compile stand-in: only the LWJGL 3 GL15 methods Aetherium calls. */
public class GL15 {
    public static int glGenBuffers() { return 0; }
    public static void glBindBuffer(int target, int buffer) { }
    public static void glBufferData(int target, ShortBuffer data, int usage) { }
    public static void glBufferData(int target, java.nio.ByteBuffer data, int usage) { }
    public static void glDeleteBuffers(int buffer) { }
}
