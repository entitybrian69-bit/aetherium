package com.aetherium.client;

import com.aetherium.qol.FoodOverlay;
// @era:q-hud-begin s17|s194|g20|g202|g21|g212|g216|g2111|x261|x262|x263|none
// @era:q-hud-else v16
//~ import com.mojang.blaze3d.platform.NativeImage;
//~ import net.minecraft.client.Minecraft;
//~ import net.minecraft.resources.ResourceLocation;
//~ import net.minecraft.server.packs.resources.Resource;
//~ import java.io.InputStream;
// @era:q-hud-end

/**
 * The saturation outline of the hunger bar overlay ({@link FoodOverlay#outline}), traced once
 * from the empty-shank sprite of the loaded resource pack and kept until the next world.
 */
public final class QolFoodIcons {
    /** Vertical shake of each hunger shank this frame (render thread only, see FoodOverlay.shake). */
    public static final int[] SHAKE = new int[10];

    private static volatile int[] outline;
    /** Where the last outline came from, for the CI smoke report. */
    private static volatile String source = "none";

    private QolFoodIcons() {
    }

    /** Ordered outline pixels ({@code x | y << 4}) of a hunger shank. */
    public static int[] outline() {
        int[] result = outline;
        if (result == null) {
            result = FoodOverlay.outline(silhouette());
            if (result.length == 0) {
                result = FoodOverlay.outline(FoodOverlay.fallbackSilhouette());
            }
            outline = result;
        }
        return result;
    }

    /** CI smoke test: traces the outline afresh and says how many pixels and from where. */
    public static String selfTest() {
        invalidate();
        return outline().length + " px from " + source;
    }

    /** New world (resource packs may have changed): trace again on next use. */
    public static void invalidate() {
        outline = null;
    }

    private static boolean[] silhouette() {
        // @era:q-hud-begin s17|s194|g20|g202|g21|g212|g216|g2111|x261|x262|x263|none
        source = "fallback (vanilla shank)";
        return FoodOverlay.fallbackSilhouette();
        // @era:q-hud-else v16
        //~ try (Resource resource = Minecraft.getInstance().getResourceManager()
                //~ .getResource(new ResourceLocation("textures/gui/icons.png"));
             //~ InputStream in = resource.getInputStream();
             //~ NativeImage image = NativeImage.read(in)) {
            //~ // The empty shank is at 16, 27 of the 256x256 sheet; HD packs scale the whole sheet.
            //~ final int scale = Math.max(1, image.getWidth() / 256);
            //~ if ((16 + FoodOverlay.SIZE) * scale > image.getWidth() || (27 + FoodOverlay.SIZE) * scale > image.getHeight()) {
                //~ source = "fallback (icons.png too small)";
                //~ return FoodOverlay.fallbackSilhouette();
            //~ }
            //~ final boolean[] solid = new boolean[FoodOverlay.SIZE * FoodOverlay.SIZE];
            //~ for (int y = 0; y < FoodOverlay.SIZE; y++) {
                //~ for (int x = 0; x < FoodOverlay.SIZE; x++) {
                    //~ final int pixel = image.getPixelRGBA((16 + x) * scale + scale / 2, (27 + y) * scale + scale / 2);
                    //~ solid[y * FoodOverlay.SIZE + x] = (pixel >>> 24) > 16;
                //~ }
            //~ }
            //~ source = "icons.png";
            //~ return solid;
        //~ } catch (final Exception | LinkageError error) {
            //~ source = "fallback (" + error + ")";
            //~ return FoodOverlay.fallbackSilhouette();
        //~ }
        // @era:q-hud-end
    }
}
