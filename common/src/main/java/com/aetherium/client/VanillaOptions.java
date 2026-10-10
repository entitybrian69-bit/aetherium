package com.aetherium.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
// @era:options-begin instances
import net.minecraft.client.OptionInstance;
// @era:options-else fields
//~ import net.minecraft.client.AmbientOcclusionStatus;
//~ import net.minecraft.client.CloudStatus;
//~ import net.minecraft.client.GraphicsStatus;
//~ import net.minecraft.client.ParticleStatus;
// @era:options-end
// @era:graphics-begin status
// @era:graphics-else preset
//~ import net.minecraft.client.GraphicsPreset;
// @era:graphics-end

/**
 * Reads and writes vanilla video options for the Aetherium menu, on every supported version.
 *
 * <p>Two storage eras exist. Up to 1.18.2 options are public fields and side effects (vsync,
 * frame cap, chunk rebuild) must be triggered by hand; that is batched in {@link #save()}. From
 * 1.19 every option is an {@code OptionInstance} whose {@code set} runs vanilla's own callbacks,
 * so writing through it behaves exactly like the vanilla Video Settings screen. Enum-valued
 * options are written by ordinal through {@link #setOrdinal}, which keeps this class independent
 * of the enum classes that moved package (ParticleStatus in 1.21.2) or changed type (ambient
 * occlusion became a Boolean in 1.19.3).</p>
 *
 * <p>All methods must be called on the render thread.</p>
 */
public final class VanillaOptions {
    /** Framerate value vanilla treats as "unlimited". */
    public static final int UNLIMITED_FPS = 260;

    /** NaN = no override. Read by {@code OptionInstanceMixin} on every gamma read (1.19+). */
    static double gammaOverride = Double.NaN;
    /** The gamma OptionInstance, cached so the mixin's identity check is one compare. */
    static Object gammaInstance;
    /** True while vanilla serialises options.txt, so the override is never persisted. */
    static boolean saving;
    // @era:options-begin instances
    // @era:options-else fields
    //~ private static double savedGamma = Double.NaN;
    //~ private static boolean pendingAllChanged;
    // @era:options-end

    private VanillaOptions() {
    }

    private static Options options() {
        final Minecraft mc = Minecraft.getInstance();
        return mc == null ? null : mc.options;
    }

    // ------------------------------------------------------------------ distances

    public static boolean hasSimulationDistance() {
        // @era:options-begin instances
        return true;
        // @era:options-else fields
        //~ // @era:simdist-begin none
        //~ return false;
        //~ // @era:simdist-else sim
        //~ //~ return true;
        //~ // @era:simdist-end
        // @era:options-end
    }

    public static int getRenderDistance() {
        final Options o = options();
        if (o == null) {
            return 8;
        }
        // @era:options-begin instances
        return o.renderDistance().get().intValue();
        // @era:options-else fields
        //~ return o.renderDistance;
        // @era:options-end
    }

    public static void setRenderDistance(final int chunks) {
        final Options o = options();
        if (o == null) {
            return;
        }
        // @era:options-begin instances
        setInt(o.renderDistance(), chunks, 16);
        // @era:options-else fields
        //~ if (o.renderDistance != chunks) {
            //~ o.renderDistance = chunks;
            //~ final Minecraft mc = Minecraft.getInstance();
            //~ if (mc.levelRenderer != null) {
                //~ mc.levelRenderer.needsUpdate();
            //~ }
        //~ }
        // @era:options-end
    }

    public static int getSimulationDistance() {
        final Options o = options();
        if (o == null) {
            return 8;
        }
        // @era:options-begin instances
        return o.simulationDistance().get().intValue();
        // @era:options-else fields
        //~ // @era:simdist-begin none
        //~ return o.renderDistance;
        //~ // @era:simdist-else sim
        //~ //~ return o.simulationDistance;
        //~ // @era:simdist-end
        // @era:options-end
    }

    public static void setSimulationDistance(final int chunks) {
        final Options o = options();
        if (o == null) {
            return;
        }
        // @era:options-begin instances
        setInt(o.simulationDistance(), chunks, 12);
        // @era:options-else fields
        //~ // @era:simdist-begin none
        //~ // 1.16.5-1.17.1 have no simulation distance; the row is hidden there.
        //~ // @era:simdist-else sim
        //~ //~ o.simulationDistance = chunks;
        //~ // @era:simdist-end
        // @era:options-end
    }

    /** Entity render distance as a percentage (vanilla range 50-500). */
    public static int getEntityDistancePercent() {
        final Options o = options();
        if (o == null) {
            return 100;
        }
        // @era:options-begin instances
        return (int) Math.round(o.entityDistanceScaling().get().doubleValue() * 100.0);
        // @era:options-else fields
        //~ return Math.round(o.entityDistanceScaling * 100f);
        // @era:options-end
    }

    public static void setEntityDistancePercent(final int percent) {
        final Options o = options();
        if (o == null) {
            return;
        }
        final double value = Math.max(50, Math.min(500, percent)) / 100.0;
        // @era:options-begin instances
        o.entityDistanceScaling().set(Double.valueOf(value));
        // @era:options-else fields
        //~ o.entityDistanceScaling = (float) value;
        // @era:options-end
    }

    // ------------------------------------------------------------------ frame pacing

    public static int getFramerateLimit() {
        final Options o = options();
        if (o == null) {
            return UNLIMITED_FPS;
        }
        // @era:options-begin instances
        return o.framerateLimit().get().intValue();
        // @era:options-else fields
        //~ return o.framerateLimit;
        // @era:options-end
    }

    public static void setFramerateLimit(final int fps) {
        final Options o = options();
        if (o == null) {
            return;
        }
        final int value = Math.max(10, Math.min(UNLIMITED_FPS, fps));
        // @era:options-begin instances
        o.framerateLimit().set(Integer.valueOf(value));
        // @era:options-else fields
        //~ o.framerateLimit = value;
        //~ Minecraft.getInstance().getWindow().setFramerateLimit(value);
        // @era:options-end
    }

    public static boolean getVsync() {
        final Options o = options();
        if (o == null) {
            return false;
        }
        // @era:options-begin instances
        return o.enableVsync().get().booleanValue();
        // @era:options-else fields
        //~ return o.enableVsync;
        // @era:options-end
    }

    public static void setVsync(final boolean on) {
        final Options o = options();
        if (o == null) {
            return;
        }
        // @era:options-begin instances
        o.enableVsync().set(Boolean.valueOf(on));
        // @era:options-else fields
        //~ o.enableVsync = on;
        //~ Minecraft.getInstance().getWindow().updateVsync(on);
        // @era:options-end
    }

    // ------------------------------------------------------------------ quality

    /** 0 fast, 1 fancy, 2 fabulous; -1 when 1.21.11+ reports a custom preset. */
    public static int getGraphics() {
        final Options o = options();
        if (o == null) {
            return 1;
        }
        // @era:options-begin instances
        // @era:graphics-begin status
        return ordinalOf(o.graphicsMode());
        // @era:graphics-else preset
        //~ final int ordinal = ordinalOf(o.graphicsPreset());
        //~ return ordinal > 2 ? -1 : ordinal;
        // @era:graphics-end
        // @era:options-else fields
        //~ return o.graphicsMode.ordinal();
        // @era:options-end
    }

    public static void setGraphics(final int index) {
        final Options o = options();
        if (o == null || index < 0) {
            return;
        }
        // @era:options-begin instances
        // @era:graphics-begin status
        setOrdinal(o.graphicsMode(), index);
        // @era:graphics-else preset
        //~ final GraphicsPreset[] presets = GraphicsPreset.values();
        //~ o.applyGraphicsPreset(presets[Math.min(index, 2)]);
        // @era:graphics-end
        // @era:options-else fields
        //~ final GraphicsStatus next = GraphicsStatus.values()[Math.min(index, 2)];
        //~ if (o.graphicsMode != next) {
            //~ o.graphicsMode = next;
            //~ pendingAllChanged = true;
        //~ }
        // @era:options-end
    }

    /** 0 off, 1 fast, 2 fancy. */
    public static int getClouds() {
        final Options o = options();
        if (o == null) {
            return 2;
        }
        // @era:options-begin instances
        return ordinalOf(o.cloudStatus());
        // @era:options-else fields
        //~ return o.renderClouds.ordinal();
        // @era:options-end
    }

    public static void setClouds(final int index) {
        final Options o = options();
        if (o == null) {
            return;
        }
        // @era:options-begin instances
        setOrdinal(o.cloudStatus(), index);
        // @era:options-else fields
        //~ o.renderClouds = CloudStatus.values()[Math.max(0, Math.min(2, index))];
        // @era:options-end
    }

    /** 0 all, 1 decreased, 2 minimal (vanilla enum order). */
    public static int getParticles() {
        final Options o = options();
        if (o == null) {
            return 0;
        }
        // @era:options-begin instances
        return ordinalOf(o.particles());
        // @era:options-else fields
        //~ return o.particles.ordinal();
        // @era:options-end
    }

    public static void setParticles(final int index) {
        final Options o = options();
        if (o == null) {
            return;
        }
        // @era:options-begin instances
        setOrdinal(o.particles(), index);
        // @era:options-else fields
        //~ o.particles = ParticleStatus.values()[Math.max(0, Math.min(2, index))];
        // @era:options-end
    }

    public static boolean getSmoothLighting() {
        final Options o = options();
        if (o == null) {
            return true;
        }
        // @era:options-begin instances
        return ordinalOf(o.ambientOcclusion()) != 0;
        // @era:options-else fields
        //~ return o.ambientOcclusion != AmbientOcclusionStatus.OFF;
        // @era:options-end
    }

    public static void setSmoothLighting(final boolean on) {
        final Options o = options();
        if (o == null) {
            return;
        }
        // @era:options-begin instances
        // Boolean from 1.19.3; OFF/MIN/MAX enum before (MAX is what vanilla's "on" means).
        setOrdinal(o.ambientOcclusion(), on ? 2 : 0);
        // @era:options-else fields
        //~ final AmbientOcclusionStatus next = on ? AmbientOcclusionStatus.MAX : AmbientOcclusionStatus.OFF;
        //~ if (o.ambientOcclusion != next) {
            //~ o.ambientOcclusion = next;
            //~ pendingAllChanged = true;
        //~ }
        // @era:options-end
    }

    /** Vanilla biome blend radius, 0-7. */
    public static int getBiomeBlend() {
        final Options o = options();
        if (o == null) {
            return 2;
        }
        // @era:options-begin instances
        return o.biomeBlendRadius().get().intValue();
        // @era:options-else fields
        //~ return o.biomeBlendRadius;
        // @era:options-end
    }

    public static void setBiomeBlend(final int radius) {
        final Options o = options();
        if (o == null) {
            return;
        }
        final int value = Math.max(0, Math.min(7, radius));
        // @era:options-begin instances
        o.biomeBlendRadius().set(Integer.valueOf(value));
        // @era:options-else fields
        //~ if (o.biomeBlendRadius != value) {
            //~ o.biomeBlendRadius = value;
            //~ pendingAllChanged = true;
        //~ }
        // @era:options-end
    }

    public static boolean getEntityShadows() {
        final Options o = options();
        if (o == null) {
            return true;
        }
        // @era:options-begin instances
        return o.entityShadows().get().booleanValue();
        // @era:options-else fields
        //~ return o.entityShadows;
        // @era:options-end
    }

    public static void setEntityShadows(final boolean on) {
        final Options o = options();
        if (o == null) {
            return;
        }
        // @era:options-begin instances
        o.entityShadows().set(Boolean.valueOf(on));
        // @era:options-else fields
        //~ o.entityShadows = on;
        // @era:options-end
    }

    public static boolean getBobView() {
        final Options o = options();
        if (o == null) {
            return true;
        }
        // @era:options-begin instances
        return o.bobView().get().booleanValue();
        // @era:options-else fields
        //~ return o.bobView;
        // @era:options-end
    }

    public static void setBobView(final boolean on) {
        final Options o = options();
        if (o == null) {
            return;
        }
        // @era:options-begin instances
        o.bobView().set(Boolean.valueOf(on));
        // @era:options-else fields
        //~ o.bobView = on;
        // @era:options-end
    }

    // ------------------------------------------------------------------ brightness

    /** The user's own brightness, 0-100 (never the fullbright override). */
    public static int getBrightnessPercent() {
        final Options o = options();
        if (o == null) {
            return 50;
        }
        // @era:options-begin instances
        final boolean was = saving;
        saving = true;
        try {
            return (int) Math.round(o.gamma().get().doubleValue() * 100.0);
        } finally {
            saving = was;
        }
        // @era:options-else fields
        //~ final double value = Double.isNaN(savedGamma) ? o.gamma : savedGamma;
        //~ return (int) Math.round(value * 100.0);
        // @era:options-end
    }

    public static void setBrightnessPercent(final int percent) {
        final Options o = options();
        if (o == null) {
            return;
        }
        final double value = Math.max(0, Math.min(100, percent)) / 100.0;
        // @era:options-begin instances
        o.gamma().set(Double.valueOf(value));
        // @era:options-else fields
        //~ if (Double.isNaN(savedGamma)) {
            //~ o.gamma = value;
        //~ } else {
            //~ savedGamma = value;
        //~ }
        // @era:options-end
    }

    /**
     * Fullbright. {@code NaN} turns the override off. The value is what the lightmap sees as
     * gamma (vanilla's slider tops out at 1.0; fullbright uses up to 15).
     */
    public static void setGammaOverride(final double value) {
        final Options o = options();
        if (o == null) {
            return;
        }
        // @era:options-begin instances
        gammaInstance = o.gamma();
        gammaOverride = value;
        // @era:options-else fields
        //~ if (Double.isNaN(value)) {
            //~ if (!Double.isNaN(savedGamma)) {
                //~ o.gamma = savedGamma;
                //~ savedGamma = Double.NaN;
            //~ }
        //~ } else {
            //~ if (Double.isNaN(savedGamma)) {
                //~ // A value above 1 can only be a previous session's override saved by a crash.
                //~ savedGamma = Math.min(1.0, o.gamma);
            //~ }
            //~ o.gamma = value;
        //~ }
        //~ gammaOverride = value;
        // @era:options-end
    }

    public static boolean isGammaOverridden() {
        return !Double.isNaN(gammaOverride);
    }

    /** Called by {@code OptionInstanceMixin}: should this read of an option return the override? */
    public static boolean overridesGamma(final Object option) {
        return !saving && option == gammaInstance && gammaInstance != null && !Double.isNaN(gammaOverride);
    }

    public static Double gammaOverrideBoxed() {
        return Double.valueOf(gammaOverride);
    }

    // ------------------------------------------------------------------ persistence

    /** Called by {@code OptionsMixin} around vanilla's {@code Options.save()}. */
    public static void onSave(final boolean begin) {
        saving = begin;
        // @era:options-begin instances
        // @era:options-else fields
        //~ final Options o = options();
        //~ if (o != null && !Double.isNaN(savedGamma)) {
            //~ o.gamma = begin ? savedGamma : gammaOverride;
        //~ }
        // @era:options-end
    }

    /**
     * Writes options.txt and runs the deferred side effects of field-era writes (a chunk rebuild
     * after graphics/AO/biome-blend changes). On 1.19+ vanilla's callbacks already ran in
     * {@code set}, so this only saves.
     */
    public static void save() {
        final Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) {
            return;
        }
        // @era:options-begin instances
        // @era:options-else fields
        //~ if (pendingAllChanged && mc.levelRenderer != null) {
            //~ mc.levelRenderer.allChanged();
        //~ }
        //~ pendingAllChanged = false;
        // @era:options-end
        mc.options.save();
    }

    /** Rebuild all chunk meshes (used when an Aetherium setting changes lighting). */
    public static void reloadChunks() {
        final Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.levelRenderer != null && mc.level != null) {
            // @era:reload-begin all-changed
            mc.levelRenderer.allChanged();
            // @era:reload-else invalidate
            //~ mc.levelRenderer.invalidateCompiledGeometry(mc.level, mc.options, mc.gameRenderer.mainCamera(), mc.getBlockColors());
            // @era:reload-end
        }
    }

    // ------------------------------------------------------------------ helpers
    // @era:options-begin instances

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setOrdinal(final OptionInstance option, final int ordinal) {
        final Object current = option.get();
        if (current instanceof Boolean) {
            option.set(Boolean.valueOf(ordinal != 0));
        } else if (current instanceof Enum) {
            final Object[] all = ((Enum) current).getDeclaringClass().getEnumConstants();
            final Object next = all[Math.max(0, Math.min(all.length - 1, ordinal))];
            if (next != current) {
                option.set(next);
            }
        }
    }

    private static int ordinalOf(final OptionInstance<?> option) {
        final Object current = option.get();
        if (current instanceof Boolean) {
            return ((Boolean) current).booleanValue() ? 1 : 0;
        }
        if (current instanceof Enum) {
            return ((Enum<?>) current).ordinal();
        }
        return 0;
    }

    /**
     * Integer options validate their range in {@code set} and silently keep the old value when
     * rejected (render distance tops out at 16 on low-memory or 32-bit JVMs), so retry clamped.
     */
    private static void setInt(final OptionInstance<Integer> option, final int value, final int fallbackMax) {
        option.set(Integer.valueOf(value));
        if (option.get().intValue() != value && value > fallbackMax) {
            option.set(Integer.valueOf(fallbackMax));
        }
    }
    // @era:options-else fields
    // @era:options-end
}
