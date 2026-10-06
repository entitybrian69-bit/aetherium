package dev.aetherium.engine;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;

/** Immutable snapshot of what the current GL context can do; probed once on the render thread. */
public record GpuCapabilities(
        String vendor,
        String renderer,
        String version,
        int majorVersion,
        int minorVersion,
        boolean directStateAccess,
        boolean bufferStorage,
        boolean multiDrawIndirect,
        boolean indirectParameters,
        boolean computeShaders,
        boolean shaderStorageBuffers,
        boolean parallelShaderCompile,
        boolean programBinary,
        boolean anisotropicFiltering,
        float maxAnisotropy,
        int maxComputeWorkGroupInvocations,
        boolean isMesa,
        boolean isZink,
        boolean isGl4es,
        boolean isSoftware
) {
    public static GpuCapabilities probe() {
        GLCapabilities caps = GL.getCapabilities();
        String vendor = nz(GL11.glGetString(GL11.GL_VENDOR));
        String renderer = nz(GL11.glGetString(GL11.GL_RENDERER));
        String version = nz(GL11.glGetString(GL11.GL_VERSION));
        int major = 0, minor = 0;
        try {
            major = GL11.glGetInteger(GL30.GL_MAJOR_VERSION);
            minor = GL11.glGetInteger(GL30.GL_MINOR_VERSION);
        } catch (Throwable ignored) {
            // GL < 3.0 (GL4ES in 2.1 mode) does not expose GL_MAJOR_VERSION.
            String[] parts = version.split("[ .]");
            if (parts.length >= 2) {
                try { major = Integer.parseInt(parts[0]); minor = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored2) {}
            }
        }
        String lr = renderer.toLowerCase();
        String lv = version.toLowerCase();
        boolean mesa = lv.contains("mesa") || lr.contains("mesa");
        boolean zink = lr.contains("zink");
        boolean gl4es = lv.contains("gl4es") || lr.contains("gl4es");
        boolean software = lr.contains("llvmpipe") || lr.contains("softpipe") || lr.contains("swiftshader") || lr.contains("virgl");

        boolean dsa = caps.OpenGL45 || caps.GL_ARB_direct_state_access;
        boolean bufStorage = caps.OpenGL44 || caps.GL_ARB_buffer_storage;
        boolean mdi = caps.OpenGL43 || caps.GL_ARB_multi_draw_indirect;
        boolean indirectParams = caps.GL_ARB_indirect_parameters || caps.OpenGL46;
        boolean compute = caps.OpenGL43 || caps.GL_ARB_compute_shader;
        boolean ssbo = caps.OpenGL43 || caps.GL_ARB_shader_storage_buffer_object;
        boolean parallelCompile = caps.GL_ARB_parallel_shader_compile || caps.GL_KHR_parallel_shader_compile;
        boolean binary = caps.OpenGL41 || caps.GL_ARB_get_program_binary;
        boolean aniso = caps.GL_EXT_texture_filter_anisotropic || caps.GL_ARB_texture_filter_anisotropic;
        float maxAniso = aniso ? GL11.glGetFloat(0x84FF /* GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT */) : 1f;
        int maxInvocations = compute ? GL11.glGetInteger(GL43.GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS) : 0;

        return new GpuCapabilities(vendor, renderer, version, major, minor, dsa, bufStorage, mdi, indirectParams,
                compute, ssbo, parallelCompile, binary, aniso, maxAniso, maxInvocations, mesa, zink, gl4es, software);
    }

    /** Everything the GL 4.6 "zero-driver-overhead" path needs. */
    public boolean supportsModernPath() {
        return directStateAccess && bufferStorage && multiDrawIndirect && computeShaders && shaderStorageBuffers;
    }

    public boolean supportsCorePath() {
        return majorVersion > 3 || (majorVersion == 3 && minorVersion >= 3);
    }

    private static String nz(String s) { return s == null ? "" : s; }

    @Override public String toString() {
        return renderer + " | GL " + majorVersion + "." + minorVersion + " | DSA=" + directStateAccess + " MDI=" + multiDrawIndirect
                + " compute=" + computeShaders + " zink=" + isZink + " gl4es=" + isGl4es;
    }
}
