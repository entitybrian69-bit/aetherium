package com.aetherium.client;

// @era:t-world-cache-begin none
// @era:t-world-cache-else v16
//~ import com.aetherium.perf.Tuning;
//~ import com.aetherium.util.AetheriumLog;
//~ import com.mojang.blaze3d.pipeline.RenderTarget;
//~ import net.minecraft.client.Minecraft;
//~ import net.minecraft.client.gui.screens.ChatScreen;
//~ import net.minecraft.client.gui.screens.Screen;
// @era:t-world-cache-end

/**
 * World picture behind menus (1.11.0, Minecraft 1.16.5). While an inventory, chest or pause menu
 * is open, the world is drawn {@link #REFRESH_PER_SECOND} times a second; in between, the last
 * picture is copied back into the main framebuffer and only the menu is drawn on top. Opening,
 * scrolling and dragging items in menus then costs a screen copy instead of a full world frame.
 *
 * <p>Not used for the chat (the world stays live while typing), Aetherium's own screens, during
 * the benchmark, or with a vanilla post effect active (spectating a creeper). Any error turns it
 * off for the session.</p>
 */
public final class WorldCache {
    /** World redraws per second behind a menu. */
    public static final int REFRESH_PER_SECOND = 4;

    private WorldCache() {
    }

    // @era:t-world-cache-begin none
    // @era:t-world-cache-else v16
    //~ private static final AetheriumLog LOGGER = AetheriumLog.of(WorldCache.class);
    //~ private static final long REFRESH_NANOS = 1_000_000_000L / REFRESH_PER_SECOND;
    //~ private static RenderTarget cache;
    //~ private static boolean valid;
    //~ private static long lastCapture;
    //~ private static boolean broken;

    //~ private static boolean eligible(final Minecraft mc) {
        //~ if (!Tuning.menuWorldCache || broken || AutoBench.ACTIVE || mc.level == null) {
            //~ return false;
        //~ }
        //~ final Screen screen = mc.screen;
        //~ return screen != null && !(screen instanceof ChatScreen)
                //~ && !screen.getClass().getName().startsWith("com.aetherium.")
                //~ && mc.gameRenderer.currentEffect() == null;
    //~ }

    //~ /**
     //~ * Instead of {@code GameRenderer.renderLevel}: true when the stored picture was drawn and the
     //~ * world must not be rendered this frame.
     //~ */
    //~ public static boolean reuse(final Minecraft mc) {
        //~ if (!eligible(mc)) {
            //~ valid = false;
            //~ if (cache != null && mc.screen == null) {
                //~ // Back in the game: the screen-sized framebuffer is not kept around.
                //~ cache.destroyBuffers();
                //~ cache = null;
                //~ // destroyBuffers leaves framebuffer 0 bound; the world is drawn into the main target.
                //~ mc.getMainRenderTarget().bindWrite(true);
            //~ }
            //~ return false;
        //~ }
        //~ if (!valid || cache == null || System.nanoTime() - lastCapture >= REFRESH_NANOS) {
            //~ return false;
        //~ }
        //~ try {
            //~ final RenderTarget main = mc.getMainRenderTarget();
            //~ if (cache.width != main.width || cache.height != main.height) {
                //~ valid = false;
                //~ return false;
            //~ }
            //~ main.bindWrite(true);
            //~ cache.blitToScreen(main.width, main.height, true);
            //~ main.bindWrite(true);
            //~ return true;
        //~ } catch (final RuntimeException | LinkageError e) {
            //~ fail(e);
            //~ return false;
        //~ }
    //~ }

    //~ /** After a world frame with a menu open: keep the picture for the next frames. */
    //~ public static void capture(final Minecraft mc) {
        //~ if (!eligible(mc)) {
            //~ valid = false;
            //~ return;
        //~ }
        //~ try {
            //~ final RenderTarget main = mc.getMainRenderTarget();
            //~ if (cache == null) {
                //~ cache = new RenderTarget(main.width, main.height, false, Minecraft.ON_OSX);
            //~ } else if (cache.width != main.width || cache.height != main.height) {
                //~ cache.resize(main.width, main.height, Minecraft.ON_OSX);
            //~ }
            //~ cache.bindWrite(true);
            //~ main.blitToScreen(main.width, main.height, true);
            //~ main.bindWrite(true);
            //~ valid = true;
            //~ lastCapture = System.nanoTime();
        //~ } catch (final RuntimeException | LinkageError e) {
            //~ fail(e);
        //~ }
    //~ }

    //~ /** The world changed: the stored picture belongs to the old one. */
    //~ public static void levelChanged() {
        //~ valid = false;
    //~ }

    //~ private static void fail(final Throwable e) {
        //~ broken = true;
        //~ valid = false;
        //~ LOGGER.error("Menu world picture turned off after an error", e);
        //~ try {
            //~ Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
            //~ if (cache != null) {
                //~ cache.destroyBuffers();
                //~ cache = null;
            //~ }
        //~ } catch (final RuntimeException | LinkageError ignored) {
            //~ // the framebuffer is freed with the GL context
        //~ }
    //~ }
    // @era:t-world-cache-end
}
