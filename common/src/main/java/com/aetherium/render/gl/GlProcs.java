package com.aetherium.render.gl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL41;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.opengl.GL44C;
import org.lwjgl.opengl.GL45C;
import org.lwjgl.opengl.GL46C;
import org.lwjgl.system.MemoryUtil;

import com.aetherium.util.AetheriumLog;

/**
 * The only place in Aetherium that names an LWJGL OpenGL class or a GL enum.
 *
 * <p>Two deliberate choices:</p>
 * <ol>
 *   <li><b>Functions are called through their LWJGL host class.</b> Each binding
 *       below was checked against LWJGL's generated sources on 2026-10-06:
 *       {@code glMultiDrawElementsIndirectCount -> GL46C},
 *       {@code glMapNamedBufferRange / glUnmapNamedBuffer / glFlushMappedNamedBufferRange /
 *       glMemoryBarrier / glCreateBuffers / glNamedBufferData -> GL45C},
 *       {@code glNamedBufferStorage / glBufferStorage / GL_MAP_PERSISTENT_BIT -> GL44C},
 *       {@code glDispatchCompute / glBindImageTexture / glMultiDrawElementsIndirect -> GL43C},
 *       {@code glProgramBinary / glGetProgramBinary -> GL41},
 *       {@code glMapBufferRange -> GL30}, sync objects {@code -> GL32},
 *       {@code glGetStringi -> GL30}.</li>
 *   <li><b>GL enums are declared numerically here.</b> Khronos enum values are
 *       frozen by the spec, whereas <em>which</em> LWJGL class hosts a token moves
 *       between LWJGL generations (MC ships 3.2.2 on 1.16.5-1.19.2, 3.3.1 on
 *       1.19.3, 3.3.2 on 1.20.2-1.20.4, 3.3.3 from 1.20.5). Pinning the numbers
 *       makes every 4.x-era token identical across the whole porting range, so a
 *       delta never has to touch the renderer for enum reasons.</li>
 * </ol>
 */
public final class GlProcs {
    private static final AetheriumLog LOGGER = AetheriumLog.of(GlProcs.class);

    // ------------------------------------------------------------ GL 1.x enums
    public static final int GL_FALSE = 0;
    public static final int GL_TRUE = 1;
    public static final int GL_TRIANGLES = 0x0004;
    public static final int GL_UNSIGNED_BYTE = 0x1401;
    public static final int GL_UNSIGNED_SHORT = 0x1403;
    public static final int GL_UNSIGNED_INT = 0x1405;
    public static final int GL_BYTE = 0x1400;
    public static final int GL_SHORT = 0x1402;
    public static final int GL_INT = 0x1404;
    public static final int GL_FLOAT = 0x1406;
    public static final int GL_RGB8 = 0x8051;
    public static final int GL_RGBA8 = 0x8058;
    public static final int GL_RGBA16F = 0x881A;
    public static final int GL_R32F = 0x822E;
    public static final int GL_R16F = 0x822D;
    public static final int GL_DEPTH_COMPONENT16 = 0x81A5;
    public static final int GL_DEPTH_COMPONENT24 = 0x81A6;
    public static final int GL_RED = 0x1903;
    public static final int GL_RGB = 0x1907;
    public static final int GL_RGBA = 0x1908;
    public static final int GL_DEPTH_COMPONENT = 0x1902;
    public static final int GL_TEXTURE_2D = 0x0DE1;
    public static final int GL_TEXTURE_2D_ARRAY = 0x8C1A;
    public static final int GL_TEXTURE_MIN_FILTER = 0x2801;
    public static final int GL_TEXTURE_MAG_FILTER = 0x2800;
    public static final int GL_TEXTURE_MAX_LEVEL = 0x813D;
    public static final int GL_NEAREST = 0x2600;
    public static final int GL_LINEAR = 0x2601;
    public static final int GL_NEAREST_MIPMAP_NEAREST = 0x2700;
    public static final int GL_TEXTURE_MIN_LOD = 0x813A;
    public static final int GL_TEXTURE_MAX_LOD = 0x813B;
    public static final int GL_STATIC_DRAW = 0x88E4;
    public static final int GL_DYNAMIC_DRAW = 0x88E8;
    public static final int GL_STREAM_DRAW = 0x88E0;
    public static final int GL_NO_ERROR = 0;
    public static final int GL_INVALID_ENUM = 0x0500;
    public static final int GL_INVALID_VALUE = 0x0501;
    public static final int GL_INVALID_OPERATION = 0x0502;
    public static final int GL_OUT_OF_MEMORY = 0x0505;
    public static final int GL_INVALID_FRAMEBUFFER_OPERATION = 0x0506;
    public static final int GL_INVALID_INDEX = 0xFFFFFFFF;
    public static final int GL_MAJOR_VERSION = 0x821B;
    public static final int GL_MINOR_VERSION = 0x821C;
    public static final int GL_NUM_EXTENSIONS = 0x821D;
    public static final int GL_EXTENSIONS = 0x1F03;
    public static final int GL_VERSION = 0x1F02;
    public static final int GL_VENDOR = 0x1F00;
    public static final int GL_RENDERER = 0x1F01;
    public static final int GL_SHADING_LANGUAGE_VERSION = 0x8B8C;
    public static final int GL_CONTEXT_FLAGS = 0x821E;
    public static final int GL_CONTEXT_PROFILE_MASK = 0x9126;
    public static final int GL_CONTEXT_CORE_PROFILE_BIT = 0x00000001;
    public static final int GL_CONTEXT_COMPATIBILITY_PROFILE_BIT = 0x00000002;
    public static final int GL_CONTEXT_FLAG_FORWARD_COMPATIBLE_BIT = 0x00000001;
    public static final int GL_MAX_COMPUTE_WORK_GROUP_COUNT = 0x91BE;
    public static final int GL_MAX_COMPUTE_WORK_GROUP_SIZE = 0x91BF;
    public static final int GL_MAX_COLOR_ATTACHMENTS = 0x8CDF;
    public static final int GL_FRAMEBUFFER = 0x8D40;
    public static final int GL_DRAW_FRAMEBUFFER = 0x8CA9;
    public static final int GL_READ_FRAMEBUFFER = 0x8CA8;
    public static final int GL_COLOR_ATTACHMENT0 = 0x8CE0;
    public static final int GL_DEPTH_ATTACHMENT = 0x8D00;

    // ------------------------------------------------- 3.0 map_buffer_range
    public static final int GL_MAP_WRITE_BIT = 0x0001;
    public static final int GL_MAP_INVALIDATE_RANGE_BIT = 0x0004;
    public static final int GL_MAP_INVALIDATE_BUFFER_BIT = 0x0008;
    public static final int GL_MAP_FLUSH_EXPLICIT_BIT = 0x0010;
    public static final int GL_MAP_UNSYNCHRONIZED_BIT = 0x0020;

    // -------------------------------------- 4.4 buffer_storage / persistent map
    public static final int GL_MAP_PERSISTENT_BIT = 0x0040;
    public static final int GL_MAP_COHERENT_BIT = 0x0080;
    public static final int GL_DYNAMIC_STORAGE_BIT = 0x0100;
    public static final int GL_CLIENT_STORAGE_BIT = 0x0200;

    // ------------------------------------------------------- 4.2/4.5 barriers
    public static final int GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT = 0x00000001;
    public static final int GL_ELEMENT_ARRAY_BARRIER_BIT = 0x00000002;
    public static final int GL_COMMAND_BARRIER_BIT = 0x00000040;
    public static final int GL_PIXEL_BUFFER_BARRIER_BIT = 0x00000080;
    public static final int GL_BUFFER_UPDATE_BARRIER_BIT = 0x00000200;
    public static final int GL_SHADER_IMAGE_ACCESS_BARRIER_BIT = 0x00000020;
    public static final int GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT = 0x00004000;
    public static final int GL_TEXTURE_FETCH_BARRIER_BIT = 0x00000008;
    public static final int GL_ALL_BARRIER_BITS = 0xFFFFFFFF;

    // --------------------------------------------------------- 4.0/4.3 buffers
    public static final int GL_ARRAY_BUFFER = 0x8892;
    public static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;
    public static final int GL_UNIFORM_BUFFER = 0x8A11;
    public static final int GL_ATOMIC_COUNTER_BUFFER = 0x8C03;
    public static final int GL_SHADER_STORAGE_BUFFER = 0x90D2;
    public static final int GL_COPY_WRITE_BUFFER = 0x8F37;
    public static final int GL_DRAW_INDIRECT_BUFFER = 0x8F3F;
    public static final int GL_DISPATCH_INDIRECT_BUFFER = 0x90EE;
    public static final int GL_QUERY_BUFFER = 0x9192;
    /** ARB_indirect_parameters (core in 4.6): makes MDIC take offsets, not pointers. */
    public static final int GL_PARAMETER_BUFFER = 0x80EE;
    public static final int GL_PARAMETER_BUFFER_BINDING = 0x80EF;
    public static final int GL_R32UI = 0x8236;

    // -------------------------------------------------------------- 4.3 compute
    public static final int GL_COMPUTE_SHADER = 0x91B9;
    public static final int GL_WRITE_ONLY = 0x91A3;
    public static final int GL_READ_ONLY = 0x91A2;
    public static final int GL_READ_WRITE = 0x91A0;

    // -------------------------------------------------------- 4.1 program binary
    public static final int GL_PROGRAM_BINARY_LENGTH = 0x8741;
    public static final int GL_PROGRAM_BINARY_RETRIEVABLE_HINT = 0x8257;

    // ---------------------------------- parallel_shader_compile / KHR sync
    /** ARB/KHR_parallel_shader_compile: 1 once the driver finished on its thread. */
    public static final int GL_COMPLETION_STATUS = 0x82FF;
    /** Upper bound the driver will honour on glClientWaitSync (ms). */
    public static final int GL_MAX_SERVER_WAIT_TIMEOUT = 0x911D;
    public static final int GL_SYNC_GPU_COMMANDS_COMPLETE = 0x9117;
    public static final int GL_SYNC_FLUSH_COMMANDS_BIT = 0x00000001;
    public static final int GL_ALREADY_SIGNALED = 0x911A;
    public static final int GL_TIMEOUT_EXPIRED = 0x911B;
    public static final int GL_CONDITION_SATISFIED = 0x911C;
    public static final int GL_WAIT_FAILED = 0x911D;

    /** No timeout: wait until the fence signals (LWJGL passes 0xFFFFFFFFFFFFFFFF). */
    public static final long TIMEOUT_IGNORED = 0xFFFFFFFFFFFFFFFFL;
    /** Non-blocking poll. */
    public static final long TIMEOUT_ZERO = 0L;

    private GlProcs() {
    }

    // ------------------------------------------------------------------ query

    public static int majorVersion() {
        return GL11.glGetInteger(GL_MAJOR_VERSION);
    }

    public static int minorVersion() {
        return GL11.glGetInteger(GL_MINOR_VERSION);
    }

    public static String versionString() {
        return nullSafe(GL11.glGetString(GL_VERSION));
    }

    public static String vendor() {
        return nullSafe(GL11.glGetString(GL_VENDOR));
    }

    public static String renderer() {
        return nullSafe(GL11.glGetString(GL_RENDERER));
    }

    public static String glslVersion() {
        return nullSafe(GL11.glGetString(GL_SHADING_LANGUAGE_VERSION));
    }

    public static int maxColorAttachments() {
        return GL11.glGetInteger(GL_MAX_COLOR_ATTACHMENTS);
    }

    public static int maxComputeWorkGroupCountX() {
        return GL11.glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_COUNT);
    }

    public static int maxComputeWorkGroupSizeX() {
        return GL11.glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_SIZE);
    }

    public static int maxServerWaitTimeoutMs() {
        return GL11.glGetInteger(GL_MAX_SERVER_WAIT_TIMEOUT);
    }

    public static int contextFlags() {
        return GL11.glGetInteger(GL_CONTEXT_FLAGS);
    }

    public static int contextProfileMask() {
        return GL11.glGetInteger(GL_CONTEXT_PROFILE_MASK);
    }

    /** True when {@code glGetStringi} is the only valid extension enumeration. */
    public static boolean supportsStringi() {
        return majorVersion() >= 3;
    }

    /**
     * Extension set. {@code glGetStringi} is the only correct enumeration on a
     * core profile (the legacy single string is truncated there), so it is used
     * whenever the context is 3.0+; anything older falls back to the string.
     */
    public static Set<String> extensions() {
        final int major = majorVersionSafely();
        if (major >= 3) {
            try {
                final int count = GL11.glGetInteger(GL_NUM_EXTENSIONS);
                final Set<String> out = new HashSet<>(Math.max(32, count * 2));
                for (int i = 0; i < count; i++) {
                    final String name = GL30.glGetStringi(GL_EXTENSIONS, i);
                    if (name != null) {
                        out.add(name.toUpperCase(Locale.ROOT));
                    }
                }
                return Collections.unmodifiableSet(out);
            } catch (final RuntimeException | LinkageError error) {
                LOGGER.warn("glGetStringi failed; falling back to glGetString(GL_EXTENSIONS)", error);
            }
        }
        final Set<String> out = new HashSet<>(64);
        try {
            final String legacy = GL11.glGetString(GL_EXTENSIONS);
            if (legacy != null) {
                for (final String token : legacy.trim().split("\\s+")) {
                    if (!token.isEmpty()) {
                        out.add(token.toUpperCase(Locale.ROOT));
                    }
                }
            }
        } catch (final RuntimeException | LinkageError error) {
            LOGGER.warn("Could not enumerate GL extensions; assuming none", error);
        }
        return Collections.unmodifiableSet(out);
    }

    private static int majorVersionSafely() {
        try {
            return majorVersion();
        } catch (final RuntimeException | LinkageError error) {
            return 0;
        }
    }

    private static String nullSafe(final String value) {
        return value == null ? "unknown" : value;
    }

    // ----------------------------------------------------------------- buffers

    public static int createBuffer() {
        return GL45C.glCreateBuffers();
    }

    /**
     * Immutable-storage allocation. Persistent mapping requires this instead of
     * {@code glNamedBufferData} because reallocation would invalidate the mapping.
     */
    public static void namedBufferStorage(final int buffer, final long size, final int storageFlags) {
        GL44C.glNamedBufferStorage(buffer, size, 0L, storageFlags);
    }

    public static void namedBufferData(final int buffer, final long size, final ByteBuffer data, final int usage) {
        GL45C.glNamedBufferData(buffer, size, data == null ? 0L : MemoryUtil.memAddress(data), usage);
    }

    public static long mapNamedBufferRange(final int buffer, final long offset, final long length, final int flags) {
        return GL45C.glMapNamedBufferRange(buffer, offset, length, flags);
    }

    public static boolean unmapNamedBuffer(final int buffer) {
        return GL45C.glUnmapNamedBuffer(buffer);
    }

    public static void flushMappedNamedBufferRange(final int buffer, final long offset, final long length) {
        GL45C.glFlushMappedNamedBufferRange(buffer, offset, length);
    }

    /** Non-DSA fallback for {@code GL_CORE}/{@code GL_LEGACY}: bind-then-map. */
    public static long mapBufferRange(final int target, final long offset, final long length, final int flags) {
        return GL30.glMapBufferRange(target, offset, length, flags);
    }

    public static boolean unmapBuffer(final int target) {
        return GL15.glUnmapBuffer(target);
    }

    public static void bindBuffer(final int target, final int buffer) {
        GL15.glBindBuffer(target, buffer);
    }

    public static void bufferData(final int target, final long size, final int usage) {
        GL15.glBufferData(target, size, usage);
    }

    public static void bufferSubData(final int target, final long offset, final ByteBuffer data) {
        GL15.glBufferSubData(target, offset, Objects.requireNonNull(data, "data"));
    }

    /**
     * Zero-fills a buffer. {@code GL_R32UI + GL_UNSIGNED_INT} is a clearable format
     * pair per the spec, which is what makes this usable for a u32 draw counter.
     */
    public static void clearNamedBufferU32(final int buffer) {
        GL45C.glClearNamedBufferData(buffer, GL_R32UI, GL_UNSIGNED_INT, 0L);
    }

    public static void memoryBarrier(final int barrierBits) {
        GL45C.glMemoryBarrier(barrierBits);
    }

    // -------------------------------------------------------------------- sync

    public static long fenceSync() {
        return GL32.glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
    }

    public static int clientWaitSync(final long sync, final int flags, final long timeoutNs) {
        return GL32.glClientWaitSync(sync, flags, timeoutNs);
    }

    public static void waitSync(final long sync, final long timeoutNs) {
        GL32.glWaitSync(sync, 0, timeoutNs);
    }

    public static void deleteSync(final long sync) {
        GL32.glDeleteSync(sync);
    }

    public static boolean isSignaled(final int waitResult) {
        return waitResult == GL_ALREADY_SIGNALED || waitResult == GL_CONDITION_SATISFIED;
    }

    // ---------------------------------------------------------------- programs

    public static int createProgram() {
        return GL20.glCreateProgram();
    }

    public static void deleteProgram(final int program) {
        GL20.glDeleteProgram(program);
    }

    public static void useProgram(final int program) {
        GL20.glUseProgram(program);
    }

    public static int getProgrami(final int program, final int pname) {
        return GL20.glGetProgrami(program, pname);
    }

    /**
     * Uploads a cached binary, skipping compilation. The caller must treat a
     * {@code false} result as "compile from source instead" — a binary from
     * another driver version is legally rejected.
     */
    public static boolean programBinary(final int program, final int binaryFormat, final ByteBuffer data) {
        if (data == null || data.remaining() <= 0) {
            return false;
        }
        GL41.glProgramBinary(program, binaryFormat, MemoryUtil.memAddress(data), data.remaining());
        return drainErrors() == null;
    }

    public static int programBinaryLength(final int program) {
        return GL20.glGetProgrami(program, GL_PROGRAM_BINARY_LENGTH);
    }

    /**
     * Extracts the compiled binary. LWJGL's signature is
     * {@code glGetProgramBinary(int, ByteBuffer, IntBuffer, IntBuffer)} where the
     * third argument receives the byte count and the fourth the binary format.
     */
    public static void getProgramBinary(final int program, final ByteBuffer target, final IntBuffer lengthOut, final IntBuffer formatOut) {
        GL41.glGetProgramBinary(program, target, lengthOut, formatOut);
    }

    // ---------------------------------------------------------------- indirect

    /**
     * MDI with a GPU-side draw count: the count lives in a buffer, so the
     * occlusion/HZB compaction never reads back to the CPU.
     */
    /**
     * Signature (verified against LWJGL's generated GL46C):
     * {@code glMultiDrawElementsIndirectCount(mode, type, indirectstream, drawcount, maxdrawcount, stride)}.
     *
     * <p>When a buffer is bound to {@code GL_PARAMETER_BUFFER}, {@code indirectstream}
     * and {@code drawcount} are interpreted as offsets into {@code GL_DRAW_INDIRECT_BUFFER}
     * and the parameter buffer respectively; otherwise they are raw GPU addresses.
     * Aetherium always uses the bound-buffer form (GL 4.6 /
     * GL_ARB_indirect_parameters) because it is the only one that works with
     * immutable-storage buffers.</p>
     */
    public static void multiDrawElementsIndirectCount(final int mode, final int type, final long indirectStreamOffset,
                                                      final long drawCountOffset, final int maxDrawCount, final int stride) {
        GL46C.glMultiDrawElementsIndirectCount(mode, type, indirectStreamOffset, drawCountOffset, maxDrawCount, stride);
    }

    /** Binds the buffer that makes the count/indirect arguments offsets rather than pointers. */
    public static void bindParameterBuffer(final int buffer) {
        GL15.glBindBuffer(GL_PARAMETER_BUFFER, buffer);
    }

    public static void multiDrawElementsIndirect(final int mode, final int type, final long indirect, final int drawCount, final int stride) {
        GL43C.glMultiDrawElementsIndirect(mode, type, indirect, drawCount, stride);
    }

    // ------------------------------------------------------------------ compute

    public static void dispatchCompute(final int groupsX, final int groupsY, final int groupsZ) {
        GL43C.glDispatchCompute(groupsX, groupsY, groupsZ);
    }

    public static void bindImageTexture(final int unit, final int texture, final int level, final boolean layered,
                                        final int layer, final int access, final int format) {
        GL43C.glBindImageTexture(unit, texture, level, layered, layer, access, format);
    }

    // -------------------------------------------------------------------- misc

    public static void flush() {
        GL11.glFlush();
    }

    public static void finish() {
        GL11.glFinish();
    }

    /** Drains the error queue; {@code null} means clean. */
    public static String drainErrors() {
        final StringBuilder builder = new StringBuilder(32);
        for (int guard = 0; guard < 16; guard++) {
            final int error = GL11.glGetError();
            if (error == GL_NO_ERROR) {
                break;
            }
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(describeError(error)).append("(0x").append(Integer.toHexString(error)).append(')');
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    private static String describeError(final int error) {
        switch (error) {
            case GL_INVALID_ENUM:
                return "INVALID_ENUM";
            case GL_INVALID_VALUE:
                return "INVALID_VALUE";
            case GL_INVALID_OPERATION:
                return "INVALID_OPERATION";
            case GL_OUT_OF_MEMORY:
                return "OUT_OF_MEMORY";
            case GL_INVALID_FRAMEBUFFER_OPERATION:
                return "INVALID_FRAMEBUFFER_OPERATION";
            case GL_INVALID_INDEX:
                return "INVALID_INDEX";
            default:
                return "UNKNOWN";
        }
    }

    /** Address helper kept here so no other class imports MemoryUtil. */
    public static long address(final ByteBuffer buffer) {
        return buffer == null ? 0L : MemoryUtil.memAddress(buffer);
    }
}
