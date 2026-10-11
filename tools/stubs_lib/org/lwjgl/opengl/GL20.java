package org.lwjgl.opengl;

import java.nio.FloatBuffer;

/** Offline compile stand-in: only the LWJGL 3 GL20 methods Aetherium calls. */
public class GL20 {
    public static int glCreateShader(int type) { return 0; }
    public static void glShaderSource(int shader, CharSequence source) { }
    public static void glCompileShader(int shader) { }
    public static int glGetShaderi(int shader, int pname) { return 0; }
    public static String glGetShaderInfoLog(int shader) { return ""; }
    public static void glDeleteShader(int shader) { }
    public static int glCreateProgram() { return 0; }
    public static void glAttachShader(int program, int shader) { }
    public static void glBindAttribLocation(int program, int index, CharSequence name) { }
    public static void glLinkProgram(int program) { }
    public static int glGetProgrami(int program, int pname) { return 0; }
    public static String glGetProgramInfoLog(int program) { return ""; }
    public static void glDeleteProgram(int program) { }
    public static void glUseProgram(int program) { }
    public static int glGetUniformLocation(int program, CharSequence name) { return -1; }
    public static void glUniform1i(int location, int v0) { }
    public static void glUniform1f(int location, float v0) { }
    public static void glUniform3f(int location, float v0, float v1, float v2) { }
    public static void glUniform4f(int location, float v0, float v1, float v2, float v3) { }
    public static void glUniformMatrix4fv(int location, boolean transpose, FloatBuffer value) { }
    public static void glEnableVertexAttribArray(int index) { }
    public static void glDisableVertexAttribArray(int index) { }
    public static void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) { }
}
