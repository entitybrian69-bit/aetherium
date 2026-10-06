package dev.aetherium.engine.gl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL45;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * GPU occlusion culling: builds a max-depth mip pyramid from the scene depth buffer with a compute shader, then a
 * second compute shader tests every section AABB against the pyramid and compacts the surviving indirect commands.
 * Both programs are compiled through {@link ShaderCache} so the first frame never stalls.
 */
public final class HierarchicalZBuffer implements AutoCloseable {
    private static final int GROUP = 8;

    private int texture;
    private int width, height, levels;
    private final float resolutionScale;
    private int downsampleProgram;
    private int cullProgram;
    private int boundsSsbo;        // vec4 min, vec4 max per section
    private int visibilitySsbo;    // uint per section (1 = visible)
    private int counterBuffer;     // atomic draw counter for GL_ARB_indirect_parameters
    private int capacity;

    public HierarchicalZBuffer(float resolutionScale) {
        this.resolutionScale = resolutionScale;
        this.boundsSsbo = GL45.glCreateBuffers();
        this.visibilitySsbo = GL45.glCreateBuffers();
        this.counterBuffer = GL45.glCreateBuffers();
        GL45.glNamedBufferStorage(counterBuffer, 4, GL45.GL_DYNAMIC_STORAGE_BIT);
    }

    public void requestPrograms(ShaderCache cache) {
        cache.request("aetherium_hzb_downsample", ShaderCache.compute(DOWNSAMPLE_SRC), id -> downsampleProgram = id);
        cache.request("aetherium_hzb_cull", ShaderCache.compute(CULL_SRC), id -> cullProgram = id);
    }

    public boolean ready() { return downsampleProgram != 0 && cullProgram != 0; }

    public void resize(int fbWidth, int fbHeight) {
        int w = Math.max(1, (int) (fbWidth * resolutionScale));
        int h = Math.max(1, (int) (fbHeight * resolutionScale));
        if (w == width && h == height && texture != 0) return;
        if (texture != 0) GL11.glDeleteTextures(texture);
        width = w; height = h;
        levels = 1 + (int) Math.floor(Math.log(Math.max(w, h)) / Math.log(2));
        texture = GL45.glCreateTextures(GL11.GL_TEXTURE_2D);
        GL45.glTextureStorage2D(texture, levels, GL30.GL_R32F, w, h);
        GL45.glTextureParameteri(texture, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST_MIPMAP_NEAREST);
        GL45.glTextureParameteri(texture, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL45.glTextureParameteri(texture, GL11.GL_TEXTURE_WRAP_S, GL15.GL_CLAMP_TO_EDGE);
        GL45.glTextureParameteri(texture, GL11.GL_TEXTURE_WRAP_T, GL15.GL_CLAMP_TO_EDGE);
    }

    /** Upload section bounds (world-space AABB, camera-relative) for this frame. 8 floats per section. */
    public void uploadBounds(FloatBuffer bounds, int sectionCount) {
        if (sectionCount > capacity) {
            capacity = Integer.highestOneBit(sectionCount) << 1;
            GL15.glDeleteBuffers(boundsSsbo);
            GL15.glDeleteBuffers(visibilitySsbo);
            boundsSsbo = GL45.glCreateBuffers();
            visibilitySsbo = GL45.glCreateBuffers();
            GL45.glNamedBufferStorage(boundsSsbo, (long) capacity * 32, GL45.GL_DYNAMIC_STORAGE_BIT);
            GL45.glNamedBufferStorage(visibilitySsbo, (long) capacity * 4, GL45.GL_DYNAMIC_STORAGE_BIT);
        }
        GL45.glNamedBufferSubData(boundsSsbo, 0, bounds);
    }

    /** Build the pyramid from the scene depth texture (level 0 is a scaled copy). */
    public void build(int sceneDepthTexture) {
        if (!ready()) return;
        GL20.glUseProgram(downsampleProgram);
        // Level 0: sample scene depth directly.
        GL45.glBindTextureUnit(0, sceneDepthTexture);
        GL42.glBindImageTexture(1, texture, 0, false, 0, GL15.GL_WRITE_ONLY, GL30.GL_R32F);
        GL20.glUniform1i(0, 1); // u_fromDepth
        GL20.glUniform2i(1, width, height);
        GL43.glDispatchCompute((width + GROUP - 1) / GROUP, (height + GROUP - 1) / GROUP, 1);
        GL42.glMemoryBarrier(GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT | GL42.GL_TEXTURE_FETCH_BARRIER_BIT);
        // Remaining levels: max of 2x2 texels of previous level.
        int lw = width, lh = height;
        for (int lvl = 1; lvl < levels; lvl++) {
            int dw = Math.max(1, lw >> 1), dh = Math.max(1, lh >> 1);
            GL45.glBindTextureUnit(0, texture);
            GL45.glTextureParameteri(texture, GL11.GL_TEXTURE_BASE_LEVEL, lvl - 1);
            GL45.glTextureParameteri(texture, GL11.GL_TEXTURE_MAX_LEVEL, lvl - 1);
            GL42.glBindImageTexture(1, texture, lvl, false, 0, GL15.GL_WRITE_ONLY, GL30.GL_R32F);
            GL20.glUniform1i(0, 0);
            GL20.glUniform2i(1, dw, dh);
            GL43.glDispatchCompute((dw + GROUP - 1) / GROUP, (dh + GROUP - 1) / GROUP, 1);
            GL42.glMemoryBarrier(GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT | GL42.GL_TEXTURE_FETCH_BARRIER_BIT);
            lw = dw; lh = dh;
        }
        GL45.glTextureParameteri(texture, GL11.GL_TEXTURE_BASE_LEVEL, 0);
        GL45.glTextureParameteri(texture, GL11.GL_TEXTURE_MAX_LEVEL, levels - 1);
    }

    /**
     * Test {@code sectionCount} AABBs; writes 0/1 to the visibility SSBO and compacts {@code srcCommands} into
     * {@code dstCommands}, incrementing the draw counter (consumed by glMultiDrawElementsIndirectCount).
     */
    public void cull(int sectionCount, float[] viewProj, int srcCommands, long srcOffset, int dstCommands, long dstOffset) {
        if (!ready() || sectionCount == 0) return;
        GL45.glNamedBufferSubData(counterBuffer, 0, new int[]{0});
        GL20.glUseProgram(cullProgram);
        GL20.glUniformMatrix4fv(0, false, viewProj);
        GL20.glUniform2i(1, width, height);
        GL20.glUniform1i(2, sectionCount);
        GL20.glUniform1i(3, levels - 1);
        GL45.glBindTextureUnit(0, texture);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, boundsSsbo);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, visibilitySsbo);
        GL30.glBindBufferRange(GL43.GL_SHADER_STORAGE_BUFFER, 2, srcCommands, srcOffset, (long) sectionCount * IndirectDrawBatcher.COMMAND_BYTES);
        GL30.glBindBufferRange(GL43.GL_SHADER_STORAGE_BUFFER, 3, dstCommands, dstOffset, (long) sectionCount * IndirectDrawBatcher.COMMAND_BYTES);
        GL30.glBindBufferBase(GL42.GL_ATOMIC_COUNTER_BUFFER, 4, counterBuffer);
        GL43.glDispatchCompute((sectionCount + 63) / 64, 1, 1);
        GL42.glMemoryBarrier(GL42.GL_COMMAND_BARRIER_BIT | GL42.GL_SHADER_STORAGE_BARRIER_BIT | GL42.GL_ATOMIC_COUNTER_BARRIER_BIT);
    }

    public int counterBuffer() { return counterBuffer; }
    public int visibilityBuffer() { return visibilitySsbo; }
    public int texture() { return texture; }
    public int levelCount() { return levels; }

    /** Read back visibility (debug overlay only; stalls the pipeline). */
    public int countVisible(int sectionCount) {
        ByteBuffer b = ByteBuffer.allocateDirect(sectionCount * 4).order(java.nio.ByteOrder.nativeOrder());
        GL45.glGetNamedBufferSubData(visibilitySsbo, 0, b);
        int n = 0;
        for (int i = 0; i < sectionCount; i++) n += b.getInt(i * 4);
        return n;
    }

    @Override public void close() {
        if (texture != 0) GL11.glDeleteTextures(texture);
        GL15.glDeleteBuffers(boundsSsbo);
        GL15.glDeleteBuffers(visibilitySsbo);
        GL15.glDeleteBuffers(counterBuffer);
    }

    // ------------------------------------------------------------------------------------------------------------
    static final String DOWNSAMPLE_SRC = """
            #version 430 core
            layout(local_size_x = 8, local_size_y = 8) in;
            layout(location = 0) uniform int u_fromDepth;
            layout(location = 1) uniform ivec2 u_dstSize;
            layout(binding = 0) uniform sampler2D u_src;
            layout(binding = 1, r32f) uniform writeonly image2D u_dst;
            void main() {
                ivec2 p = ivec2(gl_GlobalInvocationID.xy);
                if (p.x >= u_dstSize.x || p.y >= u_dstSize.y) return;
                float d;
                if (u_fromDepth == 1) {
                    vec2 uv = (vec2(p) + 0.5) / vec2(u_dstSize);
                    d = texture(u_src, uv).r;
                } else {
                    ivec2 s = p * 2;
                    float d0 = texelFetch(u_src, s, 0).r;
                    float d1 = texelFetch(u_src, s + ivec2(1, 0), 0).r;
                    float d2 = texelFetch(u_src, s + ivec2(0, 1), 0).r;
                    float d3 = texelFetch(u_src, s + ivec2(1, 1), 0).r;
                    d = max(max(d0, d1), max(d2, d3));
                }
                imageStore(u_dst, p, vec4(d));
            }
            """;

    static final String CULL_SRC = """
            #version 430 core
            layout(local_size_x = 64) in;
            layout(location = 0) uniform mat4 u_viewProj;
            layout(location = 1) uniform ivec2 u_hzbSize;
            layout(location = 2) uniform int u_count;
            layout(location = 3) uniform int u_maxLevel;
            layout(binding = 0) uniform sampler2D u_hzb;
            struct Bounds { vec4 min; vec4 max; };
            struct Cmd { uint count; uint instanceCount; uint firstIndex; uint baseVertex; uint baseInstance; };
            layout(std430, binding = 0) readonly buffer Bnds { Bounds b[]; };
            layout(std430, binding = 1) writeonly buffer Vis { uint vis[]; };
            layout(std430, binding = 2) readonly buffer Src { Cmd src[]; };
            layout(std430, binding = 3) writeonly buffer Dst { Cmd dst[]; };
            layout(binding = 4, offset = 0) uniform atomic_uint u_drawCount;
            void main() {
                uint i = gl_GlobalInvocationID.x;
                if (i >= uint(u_count)) return;
                vec3 mn = b[i].min.xyz, mx = b[i].max.xyz;
                vec2 smin = vec2(1.0), smax = vec2(0.0);
                float zmin = 1.0;
                bool anyInFront = false;
                for (int c = 0; c < 8; c++) {
                    vec3 corner = vec3((c & 1) != 0 ? mx.x : mn.x, (c & 2) != 0 ? mx.y : mn.y, (c & 4) != 0 ? mx.z : mn.z);
                    vec4 clip = u_viewProj * vec4(corner, 1.0);
                    if (clip.w <= 0.0) { anyInFront = true; smin = vec2(0.0); smax = vec2(1.0); zmin = 0.0; continue; }
                    vec3 ndc = clip.xyz / clip.w;
                    vec2 uv = ndc.xy * 0.5 + 0.5;
                    smin = min(smin, uv); smax = max(smax, uv);
                    zmin = min(zmin, ndc.z * 0.5 + 0.5);
                    anyInFront = true;
                }
                bool visible = anyInFront && !(smax.x < 0.0 || smax.y < 0.0 || smin.x > 1.0 || smin.y > 1.0);
                if (visible && zmin > 0.0) {
                    smin = clamp(smin, 0.0, 1.0); smax = clamp(smax, 0.0, 1.0);
                    vec2 px = (smax - smin) * vec2(u_hzbSize);
                    int level = clamp(int(ceil(log2(max(px.x, px.y)))), 0, u_maxLevel);
                    vec2 lsize = vec2(textureSize(u_hzb, level));
                    ivec2 p0 = ivec2(smin * lsize), p1 = min(ivec2(smax * lsize), ivec2(lsize) - 1);
                    float d0 = texelFetch(u_hzb, p0, level).r;
                    float d1 = texelFetch(u_hzb, ivec2(p1.x, p0.y), level).r;
                    float d2 = texelFetch(u_hzb, ivec2(p0.x, p1.y), level).r;
                    float d3 = texelFetch(u_hzb, p1, level).r;
                    float occluder = max(max(d0, d1), max(d2, d3));
                    visible = zmin <= occluder;
                }
                vis[i] = visible ? 1u : 0u;
                if (visible) {
                    uint slot = atomicCounterIncrement(u_drawCount);
                    dst[slot] = src[i];
                }
            }
            """;
}
