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
 *   <li>{@code int[] pixels}: read, transform, write back in place, then the
 *       caller's own upload carries it.</li>
 *   <li>{@code DynamicTexture lightTexture} (the verified 1.21.1 shape, per Iris's
 *       {@code LightTextureAccessor}): follow {@code getPixels()} to the
 *       {@code NativeImage}, then read/transform per pixel.</li>
 *   <li>a direct {@code NativeImage} field (older versions): read/transform per
 *       pixel through the {@code getPixel*}/{@code setPixel*} pair, which is slower
 *       but bounded — the texture is 16x16, so this is 256 iterations.</li>
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
        // The mutated pixel must be read BACK from the holder array: the earlier
        // version repacked the untouched float triple, which made this whole path
        // a silent no-op on the NativeImage-only versions.
        final int[] holder = new int[1];
        for (int y = 0; y < LIGHTMAP_DIMENSION; y++) {
            for (int x = 0; x < LIGHTMAP_DIMENSION; x++) {
                final int index = y * LIGHTMAP_DIMENSION + x;
                holder[0] = (int) imageGetPixel.invoke(image, index);
                applier.applyLightmap(holder, skyLevel, blockLevel, dayPercent);
                imageSetPixel.invoke(image, index, holder[0]);
            }
        }
    }

    // Shape facts read from real sources on 2026-10-10:
    //   * 1.21.1 stores the lightmap as `DynamicTexture lightTexture` on LightTexture -
    //     verified by IrisShaders/Iris @ 1.21.1 `LightTextureAccessor`, whose @Accessor("lightTexture")
    //     returns DynamicTexture. The pixels are then behind DynamicTexture#getPixels() -> NativeImage.
    //   * NativeImage's accessors are named getPixelABGR/setPixelABGR (and the RGBA pair) -
    //     matched by prefix below, because the old `equals("getPixel")` matcher never matched
    //     any real method name and silently killed the whole NativeImage path.
    // The probe still walks the real class at runtime: a name from another version is a
    // warn log and no gamma post-processing - never a crash and never a silently wrong
    // colour space.
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
                if (field.getType().getSimpleName().equals("DynamicTexture")) {
                    // 1.21.1 shape (verified, see comment above): the NativeImage lives one
                    // getter deeper, on the DynamicTexture. Resolve lightTexture.getPixels()
                    // once and reuse the NativeImage read/write path for the result.
                    final MethodHandle textureGetter = lookup.unreflectGetter(field);
                    final MethodHandle pixelsOf = findGetter(field.getType(), "getPixels");
                    if (pixelsOf != null) {
                        final Class<?> imageType = pixelsOf.type().returnType();
                        imageGetPixel = findPixelAccessor(lookup, imageType, "getPixel");
                        imageSetPixel = findPixelAccessor(lookup, imageType, "setPixel");
                        if (imageGetPixel != null && imageSetPixel != null) {
                            imageGetter = compose(textureGetter, pixelsOf);
                            shape = Shape.NATIVE_IMAGE;
                            shapeDescription = field.getName() + ".getPixels() -> "
                                    + imageType.getSimpleName() + " (" + describePixelMethods() + ")";
                            LOGGER.info("Lightmap post-processing bound to {}", shapeDescription);
                            return;
                        }
                    }
                    continue;
                }
                if (field.getType().getSimpleName().equals("NativeImage")) {
                    imageGetter = lookup.unreflectGetter(field);
                    imageGetPixel = findPixelAccessor(lookup, field.getType(), "getPixel");
                    imageSetPixel = findPixelAccessor(lookup, field.getType(), "setPixel");
                    if (imageGetPixel != null && imageSetPixel != null) {
                        shape = Shape.NATIVE_IMAGE;
                        shapeDescription = "NativeImage " + field.getName() + " (" + describePixelMethods() + ")";
                        LOGGER.info("Lightmap post-processing bound to {}", shapeDescription);
                        return;
                    }
                }
            }
            markUnsupported("no int[] pixels, DynamicTexture or NativeImage lightmap field found on " + type.getName());
        } catch (final RuntimeException | LinkageError error) {
            markUnsupported(error.getClass().getSimpleName() + ": " + error.getMessage());
        } catch (final Throwable error) {
            markUnsupported(error.getClass().getSimpleName());
        }
    }

    /** lightTexture.lightTextureField.getPixels(): one getter chained onto the other. */
    private static MethodHandle compose(final MethodHandle outer, final MethodHandle inner) throws Throwable {
        // collectArguments feeds the inner call's result into outer's only parameter slot;
        // both getters are nullary, so the composition is a nullary getter of the image.
        return MethodHandles.collectArguments(outer, 0, inner);
    }

    private static MethodHandle findGetter(final Class<?> type, final String name) {
        for (final java.lang.reflect.Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 0 && !method.getReturnType().equals(void.class)) {
                try {
                    method.setAccessible(true);
                    return MethodHandles.lookup().unreflect(method);
                } catch (final RuntimeException | IllegalAccessException error) {
                    LOGGER.dev("{}#{} is not accessible: {}", type.getSimpleName(), name, error.getClass().getSimpleName());
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Matches {@code getPixelABGR(int)}/{@code setPixelABGR(int,int)} and the RGBA pair by
     * prefix, because NativeImage has never declared a plain {@code getPixel(int)}: the
     * old exact-name matcher matched nothing on every version, which is how the entire
     * NativeImage branch of this writer was dead code while looking alive.
     */
    private static MethodHandle findPixelAccessor(final MethodHandles.Lookup lookup, final Class<?> type, final String prefix) {
        java.lang.reflect.Method best = null;
        for (final java.lang.reflect.Method method : type.getMethods()) {
            if (!method.getName().startsWith(prefix)) {
                continue;
            }
            final boolean reader = prefix.equals("getPixel") && method.getParameterCount() == 1
                    && method.getReturnType() == int.class;
            final boolean writer = prefix.equals("setPixel") && method.getParameterCount() == 2
                    && method.getReturnType() == void.class;
            if (!reader && !writer) {
                continue;
            }
            if (best == null || method.getName().length() < best.getName().length()) {
                best = method; // shortest name = the plainest variant the version offers
            }
        }
        if (best == null) {
            return null;
        }
        try {
            best.setAccessible(true);
            return lookup.unreflect(best);
        } catch (final RuntimeException | IllegalAccessException error) {
            LOGGER.dev("{}#{} is not accessible: {}", type.getSimpleName(), best.getName(), error.getClass().getSimpleName());
            return null;
        }
    }

    private static String describePixelMethods() {
        final String reader = imageGetPixel == null ? "?" : imageGetPixel.type().toString();
        final String writer = imageSetPixel == null ? "?" : imageSetPixel.type().toString();
        return reader + " / " + writer;
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
        final net.minecraft.world.entity.Entity entity = minecraft.getCameraEntity();
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
