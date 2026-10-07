package com.aetherium.render.backend;

import java.lang.reflect.Method;
import java.nio.IntBuffer;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntSupplier;

import com.aetherium.android.AndroidEnvironment;
import com.aetherium.render.gl.GlProcs;
import com.aetherium.util.AetheriumLog;

/**
 * Runtime GPU capability probe.
 *
 * <p>Runs on the render thread immediately after the GL context becomes current —
 * the only time {@code glGetString}/{@code glGetInteger} are valid — and again on
 * a backend hot-swap. It never reads the config file except for the Android hints
 * (resolved before context creation), so a probe result is a pure function of the
 * driver and can be compared across machines when reporting FPS.</p>
 *
 * <p>Vulkan availability is answered reflectively. Calling into LWJGL's {@code VK10}
 * directly on a device with a broken ICD list faults inside native code and takes
 * the JVM with it; a count-only {@code vkEnumerateInstanceExtensionProperties}
 * validates the loader with no driver callback, and a false negative costs only the
 * Vulkan row in the GUI.</p>
 */
public final class BackendProber {
    private static final AetheriumLog LOGGER = AetheriumLog.of(BackendProber.class);

    private BackendProber() {
    }

    public static BackendCapabilities probe() {
        return probe(null);
    }

    public static BackendCapabilities probe(final AndroidEnvironment android) {
        final int major = safeInt(GlProcs::majorVersion, 1);
        final int minor = safeInt(GlProcs::minorVersion, 0);
        final String vendor = GlProcs.vendor();
        final String renderer = GlProcs.renderer();
        final String version = GlProcs.versionString();
        final String glsl = GlProcs.glslVersion();

        final int contextFlags = safeInt(GlProcs::contextFlags, 0);
        final int profileMask = safeInt(GlProcs::contextProfileMask, 0);
        final boolean coreProfile = (profileMask & GlProcs.GL_CONTEXT_CORE_PROFILE_BIT) != 0;
        final boolean compatibilityProfile = (profileMask & GlProcs.GL_CONTEXT_COMPATIBILITY_PROFILE_BIT) != 0;
        final boolean forwardCompatible = (contextFlags & GlProcs.GL_CONTEXT_FLAG_FORWARD_COMPATIBLE_BIT) != 0;

        final GpuInfo.Source source = classifySource(renderer, vendor, android);
        final GpuInfo gpu = new GpuInfo(vendor, renderer, version, glsl, major, minor,
                coreProfile && !compatibilityProfile, forwardCompatible, source);
        final Set<String> extensions = GlProcs.extensions();

        LOGGER.info("GPU probe: {}", gpu.describe());
        LOGGER.dev("GL version='{}' vendor='{}' renderer='{}' glsl='{}' extensions={}",
                version, vendor, renderer, glsl, extensions.size());

        final BackendCapabilities.Builder builder = BackendCapabilities.builder()
                .gpu(gpu)
                .extensions(extensions)
                .maxColorAttachments(safeInt(GlProcs::maxColorAttachments, 4))
                .computeLimits(safeInt(GlProcs::maxComputeWorkGroupCountX, 1), safeInt(GlProcs::maxComputeWorkGroupSizeX, 16))
                .maxServerWaitTimeoutMs(safeInt(GlProcs::maxServerWaitTimeoutMs, 0))
                .maxIndirectCount(coreProfile ? Integer.MAX_VALUE : 1);

        // Async compilation only needs the completion-status query; treat GL 4.4+
        // as having it because the ARB is core there.
        final boolean parallel = extensions.contains("GL_ARB_PARALLEL_SHADER_COMPILE")
                || extensions.contains("GL_KHR_PARALLEL_SHADER_COMPILE")
                || gpu.getGlLevel() >= 440;
        builder.parallelShaderCompile(parallel ? 1 : 0);

        if (gpu.getGlLevel() >= 460) {
            builder.support(RenderBackend.GL46_DSA);
        }
        if (gpu.getGlLevel() >= 330) {
            builder.support(RenderBackend.GL_CORE);
        }
        if (gpu.getGlLevel() >= 210 || gpu.isTranslated()) {
            builder.support(RenderBackend.GL_LEGACY);
        }

        final String vulkanReason = probeVulkan(gpu, android);
        final boolean vulkan = vulkanReason == null;
        builder.vulkan(vulkan, vulkan ? "available" : vulkanReason);
        if (vulkan) {
            builder.support(RenderBackend.VULKAN_13);
        }

        // COMPATIBILITY is always supported: it is where we land when all else fails.
        builder.support(RenderBackend.COMPATIBILITY);
        return builder.build();
    }

    /**
     * Distinguishes a real driver from a translation layer. On desktop the renderer
     * string is authoritative; on Android the launcher's env var is, because Mesa
     * inside a container reports a plain "GL 4.5" through Zink.
     */
    private static GpuInfo.Source classifySource(final String renderer, final String vendor, final AndroidEnvironment android) {
        final String haystack = (Objects.requireNonNullElse(renderer, "") + ' ' + Objects.requireNonNullElse(vendor, ""))
                .toLowerCase(Locale.ROOT);
        if (haystack.contains("zink")) {
            return GpuInfo.Source.ZINK;
        }
        if (haystack.contains("angle")) {
            return GpuInfo.Source.ANGLE;
        }
        if (haystack.contains("gl4es") || haystack.contains("opengles2")) {
            return GpuInfo.Source.GL4ES;
        }
        if (haystack.contains("virgl")) {
            return GpuInfo.Source.VIRGL;
        }
        if (haystack.contains("mobileglues") || haystack.contains("mobile glues") || haystack.contains("movile")) {
            return GpuInfo.Source.MOBILEGLUES;
        }
        if (haystack.contains("ltw")) {
            return GpuInfo.Source.LTW;
        }
        if (haystack.contains("llvmpipe") || haystack.contains("softpipe") || haystack.contains("swiftshader")
                || haystack.contains("lavapipe")) {
            return GpuInfo.Source.SOFTWARE;
        }
        if (android != null && android.isAndroid()) {
            switch (android.getRenderer()) {
                case ZINK:
                    return GpuInfo.Source.ZINK;
                case GL4ES:
                case NGGL4ES:
                    return GpuInfo.Source.GL4ES;
                case LTW:
                    return GpuInfo.Source.LTW;
                case MOBILEGLUES:
                    return GpuInfo.Source.MOBILEGLUES;
                case VIRGL:
                    return GpuInfo.Source.VIRGL;
                case ANGLE:
                    return GpuInfo.Source.ANGLE;
                default:
                    return GpuInfo.Source.DRIVER_NATIVE;
            }
        }
        return GpuInfo.Source.DRIVER_NATIVE;
    }

    /** @return null when a usable Vulkan instance is reachable, else the reason not */
    private static String probeVulkan(final GpuInfo gpu, final AndroidEnvironment android) {
        if (gpu.isSoftware()) {
            return "software rasterizer: Vulkan would be Lavapipe and slower than the GL path";
        }
        if (gpu.getSource() == GpuInfo.Source.ZINK) {
            return "running on Zink: Vulkan underneath is the same path inverted";
        }
        if (android != null && android.isAndroid() && !android.supportsNativeVulkan()) {
            return "Android renderer does not expose a native Vulkan swapchain";
        }
        try {
            final Class<?> vk10 = Class.forName("org.lwjgl.vulkan.VK10");
            final Method enumerate = findEnumerate(vk10);
            if (enumerate == null) {
                return "VK10 is present but vkEnumerateInstanceExtensionProperties has an unexpected signature";
            }
            final IntBuffer count = IntBuffer.allocate(1);
            // Overload resolution: (String, IntBuffer, LongBuffer) and the
            // (IntBuffer, LongBuffer) convenience form both start with a count out.
            if (enumerate.getParameterCount() == 2) {
                enumerate.invoke(null, count, (Object) null);
            } else {
                enumerate.invoke(null, null, count, (Object) null);
            }
            if (count.get(0) <= 0) {
                return "vkEnumerateInstanceExtensionProperties reported no extensions";
            }
            return null;
        } catch (final ClassNotFoundException absent) {
            return "LWJGL Vulkan bindings are not on the classpath";
        } catch (final LinkageError | ReflectiveOperationException | RuntimeException error) {
            LOGGER.warn("Vulkan probe failed; treating Vulkan as unavailable", error);
            return "probe failed: " + error.getClass().getSimpleName();
        }
    }

    private static Method findEnumerate(final Class<?> vk10) {
        Method fallback = null;
        for (final Method method : vk10.getMethods()) {
            if (!method.getName().equals("vkEnumerateInstanceExtensionProperties")) {
                continue;
            }
            final Class<?>[] params = method.getParameterTypes();
            if (params.length == 3 && params[0] == String.class && params[1] == IntBuffer.class) {
                return method;
            }
            if (params.length == 2 && params[0] == IntBuffer.class && fallback == null) {
                fallback = method;
            }
        }
        return fallback;
    }

    private static int safeInt(final IntSupplier supplier, final int fallback) {
        try {
            return supplier.getAsInt();
        } catch (final RuntimeException | LinkageError error) {
            LOGGER.dev("GL query threw {}; using fallback {}", error.getClass().getSimpleName(), fallback);
            return fallback;
        }
    }
}
