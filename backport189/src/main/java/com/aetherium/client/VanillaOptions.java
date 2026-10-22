package com.aetherium.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import org.lwjgl.opengl.Display;

/**
 * Vanilla video options of Minecraft 1.12.2 ({@link GameSettings} fields), in the shape the
 * settings pages use on every version.
 *
 * <p>Writes go straight to the fields; {@link #save()} writes options.txt and runs the side effect
 * vanilla's own buttons would have run (a renderer reload after graphics / smooth lighting).</p>
 */
public final class VanillaOptions {

    /** 1.12.2's frame-rate slider: 10..260, where 260 is "Unlimited". */
    public static final int UNLIMITED_FPS = 260;

    /** Saved brightness while fullbright overrides it; NaN when fullbright is off. */
    private static double savedGamma = Double.NaN;
    private static double gammaOverride = Double.NaN;
    private static boolean pendingReload;

    /** 1.12.2 has no entity distance option; Aetherium scales vanilla's weight with this. */
    private static int entityDistancePercent = -1;

    private VanillaOptions() {
    }

    private static GameSettings options() {
        final Minecraft mc = Minecraft.getMinecraft();
        return mc == null ? null : mc.gameSettings;
    }

    public static boolean hasSimulationDistance() {
        return false;
    }

    // ------------------------------------------------------------------ distance

    public static int getRenderDistance() {
        final GameSettings o = options();
        return o == null ? 12 : o.renderDistanceChunks;
    }

    /** Vanilla's terrain setup notices the change on the next frame and reloads by itself. */
    public static void setRenderDistance(final int chunks) {
        final GameSettings o = options();
        if (o != null) {
            o.renderDistanceChunks = Math.max(2, Math.min(32, chunks));
        }
    }

    public static int getSimulationDistance() {
        return getRenderDistance();
    }

    public static void setSimulationDistance(final int chunks) {
        // Not a 1.12.2 option.
    }

    public static int getEntityDistancePercent() {
        if (entityDistancePercent < 0) {
            entityDistancePercent = loadEntityDistance();
        }
        return entityDistancePercent;
    }

    public static void setEntityDistancePercent(final int percent) {
        entityDistancePercent = Math.max(50, Math.min(500, percent));
        storeEntityDistance(entityDistancePercent);
    }

    /** Multiplier for vanilla's entity render distance weight. */
    public static double entityDistanceScale() {
        return getEntityDistancePercent() / 100.0;
    }

    // ------------------------------------------------------------------ frame rate

    public static int getFramerateLimit() {
        final GameSettings o = options();
        return o == null ? UNLIMITED_FPS : o.limitFramerate;
    }

    public static void setFramerateLimit(final int fps) {
        final GameSettings o = options();
        if (o != null) {
            o.limitFramerate = Math.max(10, Math.min(UNLIMITED_FPS, fps));
        }
    }

    public static boolean getVsync() {
        final GameSettings o = options();
        return o != null && o.enableVsync;
    }

    public static void setVsync(final boolean on) {
        final GameSettings o = options();
        if (o == null) {
            return;
        }
        o.enableVsync = on;
        if (Display.isCreated()) {
            Display.setVSyncEnabled(on);
        }
    }

    // ------------------------------------------------------------------ quality

    /** 0 = Fast, 1 = Fancy. */
    public static int getGraphics() {
        final GameSettings o = options();
        return o != null && o.fancyGraphics ? 1 : 0;
    }

    public static void setGraphics(final int index) {
        final GameSettings o = options();
        if (o == null) {
            return;
        }
        final boolean fancy = index >= 1;
        if (o.fancyGraphics != fancy) {
            o.fancyGraphics = fancy;
            pendingReload = true;
        }
    }

    /** 0 = Off, 1 = Fast, 2 = Fancy (options.txt values). */
    public static int getClouds() {
        final GameSettings o = options();
        return o == null ? 2 : Math.max(0, Math.min(2, o.clouds));
    }

    public static void setClouds(final int index) {
        final GameSettings o = options();
        if (o != null) {
            o.clouds = Math.max(0, Math.min(2, index));
        }
    }

    /** 0 = All, 1 = Decreased, 2 = Minimal. */
    public static int getParticles() {
        final GameSettings o = options();
        return o == null ? 0 : Math.max(0, Math.min(2, o.particleSetting));
    }

    public static void setParticles(final int index) {
        final GameSettings o = options();
        if (o != null) {
            o.particleSetting = Math.max(0, Math.min(2, index));
        }
    }

    public static boolean getSmoothLighting() {
        final GameSettings o = options();
        return o != null && o.ambientOcclusion > 0;
    }

    public static void setSmoothLighting(final boolean on) {
        final GameSettings o = options();
        if (o == null) {
            return;
        }
        final int value = on ? 2 : 0;
        if ((o.ambientOcclusion > 0) != on) {
            o.ambientOcclusion = value;
            pendingReload = true;
        }
    }

    public static int getBiomeBlend() {
        return 0;
    }

    public static void setBiomeBlend(final int radius) {
        // Not a 1.12.2 option.
    }

    public static boolean getEntityShadows() {
        final GameSettings o = options();
        return o != null && o.entityShadows;
    }

    public static void setEntityShadows(final boolean on) {
        final GameSettings o = options();
        if (o != null) {
            o.entityShadows = on;
        }
    }

    public static boolean getBobView() {
        final GameSettings o = options();
        return o != null && o.viewBobbing;
    }

    public static void setBobView(final boolean on) {
        final GameSettings o = options();
        if (o != null) {
            o.viewBobbing = on;
        }
    }

    // ------------------------------------------------------------------ brightness

    /** The user's own brightness, 0-100 (never the fullbright override). */
    public static int getBrightnessPercent() {
        final GameSettings o = options();
        if (o == null) {
            return 50;
        }
        if (!Double.isNaN(savedGamma)) {
            return (int) Math.round(savedGamma * 100.0);
        }
        return (int) Math.round(Math.min(1.0f, o.gammaSetting) * 100.0);
    }

    public static void setBrightnessPercent(final int percent) {
        final GameSettings o = options();
        if (o == null) {
            return;
        }
        final double value = Math.max(0, Math.min(100, percent)) / 100.0;
        if (!Double.isNaN(savedGamma)) {
            savedGamma = value;
            return;
        }
        o.gammaSetting = (float) value;
    }

    /**
     * Fullbright: {@code NaN} turns the override off. 1.12.2's lightmap clamps nothing, so a gamma
     * above 1 brightens dark areas the same way the main mod's override does.
     */
    public static void setGammaOverride(final double value) {
        final GameSettings o = options();
        if (o == null) {
            return;
        }
        if (Double.isNaN(value)) {
            if (!Double.isNaN(savedGamma)) {
                o.gammaSetting = (float) savedGamma;
                savedGamma = Double.NaN;
            }
        } else {
            if (Double.isNaN(savedGamma)) {
                // A value above 1 can only be a previous session's override saved by a crash.
                savedGamma = Math.min(1.0, o.gammaSetting);
            }
            o.gammaSetting = (float) value;
        }
        gammaOverride = value;
    }

    public static boolean isGammaOverridden() {
        return !Double.isNaN(gammaOverride);
    }

    // ------------------------------------------------------------------ persistence

    /** Called around vanilla's {@code GameSettings.saveOptions()}: options.txt never stores fullbright. */
    public static void onSave(final boolean begin) {
        final GameSettings o = options();
        if (o != null && !Double.isNaN(savedGamma)) {
            o.gammaSetting = (float) (begin ? savedGamma : gammaOverride);
        }
    }

    public static void save() {
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.gameSettings == null) {
            return;
        }
        if (pendingReload && mc.renderGlobal != null && mc.theWorld != null) {
            mc.renderGlobal.loadRenderers();
        }
        pendingReload = false;
        mc.gameSettings.saveOptions();
    }

    /** Rebuild all chunk meshes. */
    public static void reloadChunks() {
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.renderGlobal != null && mc.theWorld != null) {
            mc.renderGlobal.loadRenderers();
        }
    }

    // ------------------------------------------------------------------ entity distance file

    private static File entityDistanceFile() {
        final Minecraft mc = Minecraft.getMinecraft();
        return new File(new File(mc.mcDataDir, "config"), "aetherium-backport.properties");
    }

    private static int loadEntityDistance() {
        final Properties p = new Properties();
        try (InputStream in = new FileInputStream(entityDistanceFile())) {
            p.load(in);
            return Math.max(50, Math.min(500, Integer.parseInt(p.getProperty("entityDistancePercent", "100").trim())));
        } catch (final IOException | RuntimeException error) {
            return 100;
        }
    }

    private static void storeEntityDistance(final int percent) {
        final Properties p = new Properties();
        p.setProperty("entityDistancePercent", Integer.toString(percent));
        final File file = entityDistanceFile();
        file.getParentFile().mkdirs();
        try (OutputStream out = new FileOutputStream(file)) {
            p.store(out, "Aetherium Backport: vanilla options 1.12.2 does not have");
        } catch (final IOException error) {
            // A missed write only loses the setting for the next launch.
        }
    }
}
