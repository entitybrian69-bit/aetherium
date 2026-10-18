package com.aetherium.client;

// @era:render-scale-begin target|r17|r212|r215
import com.aetherium.mixin.core.MainTargetAccessor;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
// @era:render-scale-else none
// @era:render-scale-end

/**
 * The smaller framebuffer the 3D world is drawn into when render scale is below 100%
 * (1.9.0; every version up to 26.1 since 1.14.0). Around {@code GameRenderer.renderLevel} it temporarily becomes
 * {@code Minecraft.mainRenderTarget}, so every vanilla {@code getMainRenderTarget()} call
 * during the world pass (sky, terrain, entities, hand) draws into it; afterwards the real
 * target is put back and this one is drawn over it, filtered, at window size. The entity
 * outline pass, post effects and the GUI then run at full resolution as usual.
 */
// @era:render-scale-begin r17|r212|r215
public final class ScaledRenderTarget extends RenderTarget {
    private static ScaledRenderTarget instance;
    private static RenderTarget swapped;

    private ScaledRenderTarget(final int width, final int height) {
        // @era:rs-api-begin r17
        super(true);
        this.resize(width, height, Minecraft.ON_OSX);
        // @era:rs-api-else r212
        //~ super(true);
        //~ this.resize(width, height);
        // @era:rs-api-else r215
        //~ super("Aetherium render scale", true);
        //~ this.createBuffers(width, height);
        // @era:rs-api-end
    }

    // @era:rs-api-begin r17|r212
    /**
     * Vanilla rebinds the main target with {@code bindWrite(false)} after its outline pass,
     * relying on the viewport already matching the window. Always set our own viewport.
     */
    @Override
    public void bindWrite(final boolean setViewport) {
        super.bindWrite(true);
    }
    // @era:rs-api-else r215
    //~ // (1.21.5+: every render pass sets the viewport from its own colour texture.)
    // @era:rs-api-end

    private void ensureSize(final int width, final int height) {
        if (width != this.width || height != this.height) {
            // @era:rs-api-begin r17
            this.resize(width, height, Minecraft.ON_OSX);
            // @era:rs-api-else r212|r215
            //~ this.resize(width, height);
            // @era:rs-api-end
        }
    }

    /** {@code GameRenderer.renderLevel} HEAD. */
    public static void begin(final Minecraft minecraft) {
        if (swapped != null) {
            // The previous world pass threw before RETURN: put the real target back first.
            restore(minecraft);
        }
        final int percent = RenderScale.beginFrame(Minecraft.useShaderTransparency());
        if (percent >= 100) {
            return;
        }
        try {
            final MainTargetAccessor access = (MainTargetAccessor) minecraft;
            final RenderTarget main = access.aetheriumMainTarget();
            final int width = RenderScale.scaled(main.width, percent);
            final int height = RenderScale.scaled(main.height, percent);
            if (instance == null) {
                instance = new ScaledRenderTarget(width, height);
            } else {
                instance.ensureSize(width, height);
            }
            swapped = main;
            access.aetheriumSetMainTarget(instance);
            // @era:rs-api-begin r17|r212
            instance.bindWrite(true);
            // @era:rs-api-else r215
            // @era:rs-api-end
        } catch (final Throwable error) {
            restore(minecraft);
            RenderScale.fail(error);
        }
    }

    /**
     * {@code GameRenderer.renderLevel} RETURN: put the real target back and stretch the frame over
     * it with one linear {@code glBlitFramebuffer} ({@link RawFrame#upscale}).
     */
    public static void end(final Minecraft minecraft) {
        if (swapped == null) {
            return;
        }
        final RenderTarget main = swapped;
        restore(minecraft);
        try {
            if (!RawFrame.upscale(instance, main)) {
                RenderScale.fail(new IllegalStateException("render targets are not reachable as GL framebuffers"));
            }
            // @era:rs-api-begin r17|r212
            main.bindWrite(true);
            // @era:rs-api-else r215
            // @era:rs-api-end
        } catch (final Throwable error) {
            RenderScale.fail(error);
        }
    }

    private static void restore(final Minecraft minecraft) {
        final RenderTarget main = swapped;
        swapped = null;
        if (main != null) {
            ((MainTargetAccessor) minecraft).aetheriumSetMainTarget(main);
        }
    }

    /**
     * CI smoke test (title screen, render thread): builds a half-size target, swaps it in and
     * back through the accessor and upscales it once into the real target with the same blit a
     * scaled frame uses. Returns "ok", or "FAILED: why".
     */
    public static String selfTest() {
        final Minecraft minecraft = Minecraft.getInstance();
        final MainTargetAccessor access = (MainTargetAccessor) minecraft;
        final RenderTarget main = access.aetheriumMainTarget();
        final ScaledRenderTarget test = new ScaledRenderTarget(RenderScale.scaled(main.width, 50),
                RenderScale.scaled(main.height, 50));
        access.aetheriumSetMainTarget(test);
        final boolean swappedIn = minecraft.getMainRenderTarget() == test;
        access.aetheriumSetMainTarget(main);
        if (!swappedIn) {
            return "FAILED: getMainRenderTarget() did not return the swapped-in target";
        }
        if (minecraft.getMainRenderTarget() != main) {
            return "FAILED: the real target was not put back";
        }
        while (org.lwjgl.opengl.GL11.glGetError() != 0) {
            // drain errors raised before this test
        }
        if (!RawFrame.upscale(test, main)) {
            return "FAILED: upscale blit refused (texture not reachable or framebuffer incomplete)";
        }
        final int error = org.lwjgl.opengl.GL11.glGetError();
        // @era:rs-api-begin r17|r212
        main.bindWrite(true);
        // @era:rs-api-else r215
        // @era:rs-api-end
        return error == 0 ? "ok (" + test.width + "x" + test.height + " -> " + main.width + "x" + main.height + ")"
                : "FAILED: GL error 0x" + Integer.toHexString(error) + " after the upscale blit";
    }
}
// @era:render-scale-else none
//~ public final class ScaledRenderTarget {
    //~ private ScaledRenderTarget() {
    //~ }

    //~ /** CI smoke test: render scale does not exist on this version. */
    //~ public static String selfTest() {
        //~ return "skipped (not on this version)";
    //~ }
//~ }
// @era:render-scale-else target
//~ public final class ScaledRenderTarget extends RenderTarget {
    //~ /** GL_LINEAR: smooth upscale. RenderTarget.createBuffers resets the filter to GL_NEAREST. */
    //~ private static final int LINEAR = 9729;

    //~ private static ScaledRenderTarget instance;
    //~ private static RenderTarget swapped;

    //~ private ScaledRenderTarget(final int width, final int height) {
        //~ super(width, height, true, Minecraft.ON_OSX);
        //~ this.setFilterMode(LINEAR);
    //~ }

    //~ /**
     //~ * Vanilla rebinds the main target with {@code bindWrite(false)} after its outline pass,
     //~ * relying on the viewport already matching the window. Always set our own viewport.
     //~ */
    //~ @Override
    //~ public void bindWrite(final boolean setViewport) {
        //~ super.bindWrite(true);
    //~ }

    //~ private void ensureSize(final int width, final int height) {
        //~ if (width != this.width || height != this.height) {
            //~ this.resize(width, height, Minecraft.ON_OSX);
            //~ this.setFilterMode(LINEAR);
        //~ }
    //~ }

    //~ /** {@code GameRenderer.renderLevel} HEAD. */
    //~ public static void begin(final Minecraft minecraft) {
        //~ if (swapped != null) {
            //~ // The previous world pass threw before RETURN: put the real target back first.
            //~ restore(minecraft);
        //~ }
        //~ final int percent = RenderScale.beginFrame(Minecraft.useShaderTransparency());
        //~ if (percent >= 100) {
            //~ return;
        //~ }
        //~ try {
            //~ final MainTargetAccessor access = (MainTargetAccessor) minecraft;
            //~ final RenderTarget main = access.aetheriumMainTarget();
            //~ final int width = RenderScale.scaled(main.width, percent);
            //~ final int height = RenderScale.scaled(main.height, percent);
            //~ if (instance == null) {
                //~ instance = new ScaledRenderTarget(width, height);
            //~ } else {
                //~ instance.ensureSize(width, height);
            //~ }
            //~ swapped = main;
            //~ access.aetheriumSetMainTarget(instance);
            //~ instance.bindWrite(true);
        //~ } catch (final Throwable error) {
            //~ restore(minecraft);
            //~ RenderScale.fail(error);
        //~ }
    //~ }

    //~ /** {@code GameRenderer.renderLevel} RETURN: put the real target back and draw the frame into it. */
    //~ public static void end(final Minecraft minecraft) {
        //~ if (swapped == null) {
            //~ return;
        //~ }
        //~ final RenderTarget main = swapped;
        //~ restore(minecraft);
        //~ try {
            //~ main.bindWrite(true);
            //~ instance.blitToScreen(main.width, main.height);
        //~ } catch (final Throwable error) {
            //~ RenderScale.fail(error);
        //~ }
    //~ }

    //~ private static void restore(final Minecraft minecraft) {
        //~ final RenderTarget main = swapped;
        //~ swapped = null;
        //~ if (main != null) {
            //~ ((MainTargetAccessor) minecraft).aetheriumSetMainTarget(main);
        //~ }
    //~ }

    //~ /** CI smoke test: a half-size target swapped in and back. Returns "ok", or "FAILED: why". */
    //~ public static String selfTest() {
        //~ final Minecraft minecraft = Minecraft.getInstance();
        //~ final MainTargetAccessor access = (MainTargetAccessor) minecraft;
        //~ final RenderTarget main = access.aetheriumMainTarget();
        //~ final ScaledRenderTarget test = new ScaledRenderTarget(RenderScale.scaled(main.width, 50),
                //~ RenderScale.scaled(main.height, 50));
        //~ access.aetheriumSetMainTarget(test);
        //~ final boolean swappedIn = minecraft.getMainRenderTarget() == test;
        //~ access.aetheriumSetMainTarget(main);
        //~ main.bindWrite(true);
        //~ return swappedIn && minecraft.getMainRenderTarget() == main ? "ok"
                //~ : "FAILED: the main render target could not be swapped";
    //~ }
//~ }
// @era:render-scale-end
