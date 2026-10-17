package com.aetherium.client;

// @era:render-scale-begin none
// @era:render-scale-else target
//~ import com.aetherium.mixin.core.MainTargetAccessor;
//~ import com.mojang.blaze3d.pipeline.RenderTarget;
//~ import net.minecraft.client.Minecraft;
// @era:render-scale-end

/**
 * The smaller framebuffer the 3D world is drawn into when render scale is below 100%
 * (1.9.0). Around {@code GameRenderer.renderLevel} it temporarily becomes
 * {@code Minecraft.mainRenderTarget}, so every vanilla {@code getMainRenderTarget()} call
 * during the world pass (sky, terrain, entities, hand) draws into it; afterwards the real
 * target is put back and this one is drawn over it, filtered, at window size. The entity
 * outline pass, post effects and the GUI then run at full resolution as usual.
 */
// @era:render-scale-begin none
public final class ScaledRenderTarget {
    private ScaledRenderTarget() {
    }
}
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
//~ }
// @era:render-scale-end
