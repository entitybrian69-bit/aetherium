package dev.aetherium.engine;

import dev.aetherium.config.AetheriumConfig;

/** A graphics API implementation. Exactly one is active at a time; may be swapped live. */
public interface RenderBackend extends AutoCloseable {
    enum Kind { OPENGL_MODERN, OPENGL_CORE, OPENGL_LEGACY, VULKAN }

    Kind kind();

    /** Called on the render thread with a current context. Returns false if the backend cannot start. */
    boolean initialize(AetheriumConfig config);

    void beginFrame(int framebufferWidth, int framebufferHeight);

    /** Build the HZB from the depth texture and run GPU culling for the submitted section list. */
    void runOcclusionPass(int depthTextureId, int width, int height, float[] viewProjection);

    void endFrame();

    void applyConfig(AetheriumConfig config);

    String describe();

    @Override void close();
}
