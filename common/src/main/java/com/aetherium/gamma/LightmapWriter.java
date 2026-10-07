package com.aetherium.gamma;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Objects;

import com.aetherium.Aetherium;
import com.aetherium.config.AetheriumConfig;
import com.aetherium.util.AetheriumLog;
import com.aetherium.util.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

/**
 * Resolves and writes the vanilla lightmap pixels, shape-independently.
 *
 * <p>Probe order, first match wins, each cached forever:</p>
 * <ol>
 *   <li>{@code int[] pixels} (1.20.2+): read, transform, write back in place, then
 *       the caller's own upload carries it.</li>
 *   <li>{@code NativeImage lightmap} (1.16.5-1.20.1): read/transform per pixel
 *       through {@code getPixel/setPixel}, which is slower but bounded — the
 *       texture is 16x16, so this is 256 iterations and no allocation.</li>
 *   <li>nothing: log once, disable, never retry (a per-frame retry on a device that
 *       will not change its mind is the classic modding footgun).</li>
 * </ol>
 *
 * <p>Both paths write in ABGR to match vanilla's {@code NativeImage.PixelFormat.ABGR}
 * lightmap layout; that is the assumption that makes the colour math here
 * version-independent, and {@link #describeShape()} reports what was actually found
 * so a wrong assumption is visible in one line instead of being "gamma looks
 * slightly orange".</p>
 */
public final class LightmapWriter {
    private static final AetheriumLog LOGGER = AetheriumLog.of(LightmapWriter.class);
    private static final int LIGHTMAP_DIMENSION = 16;

    private enum Shape {
        UNKNOWN,
        INT_ARRAY,
        NATIVE_IMAGE,
        UNSUPPORTED
    }

    /** Published once; volatile because the first frame to run may be a worker in a later refactor. */
    private static volatile Shape shape = Shape.UNKNOWN;
    private static MethodHandle pixelsGetter;
    private static MethodHandle pixelsSetter;
    private static MethodHandle imageGetter;
    private static MethodHandle imageGetPixel;
    private static MethodHandle imageSetPixel;
    private static String shapeDescription = "not probed";

    private static int framesApplied;
    private static int lastVersionHash;

    private LightmapWriter() {
    }

    /** Entry point from {@code LightTextureMixin}. Never throws. */
    public static void apply(final LightTexture lightTexture) {
        Objects.requireNonNull(lightTexture, "lightTexture");
        final AetheriumConfig config = Aetherium.config();
        if (!needsWork(config)) {
            return;
        }
        if (shape == Shape.UNKNOWN) {
            probe(lightTexture);
        }
        if (shape == Shape.UNSUPPORTED) {
            return;
        }
        final GammaApplier applier = Aetherium.gamma();
        final Minecraft minecraft = lightTexture == null ? null : Minecraft.getInstance();
        final int skyLevel = sampleLight(minecraft, LightLayer.SKY);
        final int blockLevel = sampleLight(minecraft, LightLayer.BLOCK);
        final float dayPercent = minecraft == null || minecraft.level == null
                ? 0.5f : (minecraft.level.getDayTime() % 24000L) / 24000.0f;

        try {
            if (shape == Shape.INT_ARRAY) {
                final int[] pixels = (int[]) pixelsGetter.invoke(lightTexture);
                if (pixels == null || pixels.length == 0) {
                    markUnsupported("int[] pixels resolved but was null/empty");
                    return;
                }
                applier.applyLightmap(pixels, skyLevel, blockLevel, dayPercent);
                pixelsSetter.invoke(lightTexture, pixels);
            } else {
                final Object image = imageGetter.invoke(lightTexture);
                if (image == null) {
                    markUnsupported("NativeImage lightmap resolved but was null");
                    return;
                }
                applyNativeImage(image, applier, skyLevel, blockLevel, dayPercent);
            }
            framesApplied++;
        } catch (final Throwable throwable) {
            // Throwable, not Exception: an UnsatisfiedLinkError from a stale
            // MethodHandle must not escape into the render loop. One warn, then off.
            markUnsupported(throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }

    private static boolean needsWork(final AetheriumConfig config) {
        return config.gammaEnabled.get() || config.caveVision.get() || config.nightVisionBoost.get()
                || config.timeBasedGamma.get() || (config.dynamicLights.get() && config.dynamicLightsColored.get());
    }

    private static void applyNativeImage(final Object image, final GammaApplier applier, final int skyLevel,
                                          final int blockLevel, final float dayPercent) throws Throwable {
        // 16x16, one getPixel + one setPixel per texel. The array form is preferred;
        // this path exists so 1.16.5-1.20.1 get identical behaviour to 1.21.1.
        for (int y = 0; y < LIGHTMAP_DIMENSION; y++) {
            for (int x = 0; x < LIGHTMAP_DIMENSION; x++) {
                final int index = y * LIGHTMAP_DIMENSION + x;
                final int pixel = (int) imageGetPixel.invoke(image, index);
                final float red = MathUtil.channelRed(pixel);
                final float green = MathUtil.channelGreen(pixel);
                final float blue = MathUtil.channelBlue(pixel);
                final float[] single = new float[]{red, green, blue};
                applier.applyLightmap(toIntArray(single), skyLevel, blockLevel, dayPercent);
                imageSetPixel.invoke(image, index, toPacked(single[0], single[1], single[2]));
            }
        }
    }

    private static int[] toIntArray(final float[] rgb) {
        return new int[]{MathUtil.packRgb(rgb[0], rgb[1], rgb[2])};
    }

    private static int toPacked(final float red, final float green, final float blue) {
        return MathUtil.packRgb(red, green, blue);
    }

    // [UNVERIFIED: the accepted field names ("pixels", "lightmapPixels", any *pixels) and that
    // the int[] is ABGR-packed as Vanilla's NativeImage.PixelFormat.ABGR lightmap. The probe walks
    // the real class at runtime, so a wrong guess here is a warn log and no gamma post-processing
    // - never a crash and never a silently wrong colour space.]
    private static void probe(final LightTexture lightTexture) {
        try {
            final Class<?> type = lightTexture.getClass();
            final MethodHandles.Lookup lookup = MethodHandles.lookup();
            for (final java.lang.reflect.Field field : type.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.getType() == int[].class && (field.getName().equals("pixels") || field.getName().endsWith("pixels")
                        || field.getName().equals("lightmapPixels"))) {
                    pixelsGetter = lookup.unreflectGetter(field);
                    pixelsSetter = lookup.unreflectSetter(field);
                    shape = Shape.INT_ARRAY;
                    shapeDescription = "int[] " + field.getName() + " on " + type.getSimpleName();
                    LOGGER.info("Lightmap post-processing bound to {}", shapeDescription);
                    return;
                }
                if (field.getType().getSimpleName().equals("NativeImage")) {
                    imageGetter = lookup.unreflectGetter(field);
                    final MethodHandles.Lookup imageLookup = MethodHandles.lookup();
                    imageGetPixel = findMethod(imageLookup, field.getType(), "getPixel", "RGBA");
                    imageSetPixel = findMethod(imageLookup, field.getType(), "setPixel", "ABGR");
                    if (imageGetPixel != null && imageSetPixel != null) {
                        shape = Shape.NATIVE_IMAGE;
                        shapeDescription = "NativeImage " + field.getName() + " (ABGR)";
                        LOGGER.info("Lightmap post-processing bound to {}", shapeDescription);
                        return;
                    }
                }
            }
            markUnsupported("no int[] pixels or NativeImage lightmap field found on " + type.getName());
        } catch (final RuntimeException | LinkageError error) {
            markUnsupported(error.getClass().getSimpleName() + ": " + error.getMessage());
        } catch (final Throwable error) {
            markUnsupported(error.getClass().getSimpleName());
        }
    }

    private static MethodHandle findMethod(final MethodHandles.Lookup lookup, final Class<?> type, final String name, final String suffixHint) {
        for (final java.lang.reflect.Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 1) {
                try {
                    method.setAccessible(true);
                    return lookup.unreflect(method);
                } catch (final IllegalAccessException error) {
                    LOGGER.dev("NativeImage#{} is not accessible: {}", name, error.getMessage());
                    return null;
                }
            }
        }
        return null;
    }

    private static void markUnsupported(final String reason) {
        if (shape != Shape.UNSUPPORTED) {
            LOGGER.warn("Lightmap post-processing disabled: {}. Gamma/cave vision will not change the "
                    + "lightmap; dynamic lights and the rest of Aetherium are unaffected. On a new Minecraft "
                    + "version this usually means the field was renamed - add it to the probe list in "
                    + "LightmapWriter and open a report.", reason);
        }
        shape = Shape.UNSUPPORTED;
        shapeDescription = "unsupported (" + reason + ')';
    }

    public static String describeShape() {
        return shapeDescription;
    }

    public static int getFramesApplied() {
        return framesApplied;
    }

    public static boolean isUsable() {
        return shape == Shape.INT_ARRAY || shape == Shape.NATIVE_IMAGE;
    }

    /** Test seam: forces the next call to re-probe. */
    public static void resetProbeForTests() {
        shape = Shape.UNKNOWN;
        pixelsGetter = null;
        pixelsSetter = null;
        imageGetter = null;
        imageGetPixel = null;
        imageSetPixel = null;
        framesApplied = 0;
        lastVersionHash = 0;
    }

    private static int sampleLight(final Minecraft minecraft, final LightLayer layer) {
        if (minecraft == null) {
            return 15;
        }
        final Level level = minecraft.level;
        final var entity = minecraft.getCameraEntity();
        final MethodHandle light = LIGHT_LEVEL;
        if (level == null || entity == null || light == null) {
            return 15;
        }
        final BlockPos pos = entity.blockPosition();
        try {
            return MathUtil.clamp((int) light.invoke(level, layer, pos), 0, 15);
        } catch (final RuntimeException error) {
            // The level query can throw for an unloaded column during a dimension
            // change; treat it as full light, i.e. "no adjustment needed".
            return 15;
        } catch (final Throwable error) {
            return 15;
        }
    }

    /**
     * The per-layer light query, resolved by signature rather than by name.
     *
     * <p>Why not a direct call: {@code Level#getLightLevel(LightLayer, BlockPos)} does not exist on
     * 1.21.1 (javac: cannot find symbol) - the query lives on the {@code BlockAndLightReader}
     * interface with a name that has moved once already. Matching on "two parameters, first a
     * LightLayer, second a BlockPos, returns int" is the version-tolerant form of the same rule this
     * file already uses for the lightmap pixels field, and a miss means no gamma post-processing
     * rather than a broken frame.</p>
     */
    private static final MethodHandle LIGHT_LEVEL = findLightLevel();

    private static MethodHandle findLightLevel() {
        try {
            // getMethods() finds the public inherited form (the normal case); getDeclaredMethods() is
            // the fallback for a package-private implementation on a dev launch.
            final MethodHandle fromPublic = findLightLevelIn(Level.class.getMethods());
            final MethodHandle handle = fromPublic != null
                    ? fromPublic : findLightLevelIn(Level.class.getDeclaredMethods());
            if (handle != null) {
                return handle;
            }
        } catch (final RuntimeException | LinkageError error) {
            LOGGER.dev("Level light query probe failed: {}", error.getClass().getSimpleName());
            return null;
        }
        LOGGER.warn("No (LightLayer,BlockPos)->int query on Level; gamma's sky/block light sampling is "
                + "disabled and will report full light. Cave dimming and night adjustment keep working "
                + "from the lightmap itself.");
        return null;
    }

    private static MethodHandle findLightLevelIn(final java.lang.reflect.Method[] candidates) {
        for (final java.lang.reflect.Method method : candidates) {
            final Class<?>[] types = method.getParameterTypes();
            if (method.getReturnType() != int.class || types.length != 2
                    || types[0] != LightLayer.class || types[1] != BlockPos.class) {
                continue;
            }
            try {
                method.setAccessible(true);
                return MethodHandles.lookup().unreflect(method);
            } catch (final RuntimeException | IllegalAccessException error) {
                LOGGER.dev("Level light query {} found but not accessible: {}", method.getName(), error.getClass().getSimpleName());
                return null;
            }
        }
        return null;
    }
}
