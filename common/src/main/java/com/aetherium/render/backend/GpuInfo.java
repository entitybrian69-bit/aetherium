package com.aetherium.render.backend;

import java.util.Locale;
import java.util.Objects;

/**
 * Parsed GPU identity. Vendor strings, not model names, drive the heuristics:
 * the same architecture appears under a dozen marketing names on mobile
 * (Adreno/Mali/PowerVR/Xclipse) and Android drivers routinely report
 * {@code "Vulkan"} or {@code "Mesa OffScreen"} as the renderer.
 */
public final class GpuInfo {
    public enum Vendor {
        NVIDIA,
        AMD,
        INTEL,
        ARM_MALI,
        QUALCOMM_ADRENO,
        IMG_POWERVR,
        APPLE,
        BROADCOM_V3D,
        MESA_SOFT,
        TRANSLATION_LAYER,
        UNKNOWN;

        public static Vendor from(final String vendor, final String renderer) {
            final String haystack = (vendor + ' ' + renderer).toLowerCase(Locale.ROOT);
            if (haystack.contains("nvidia") || haystack.contains("geforce") || haystack.contains("rtx") || haystack.contains("gtx")) {
                return NVIDIA;
            }
            if (haystack.contains("ati") || haystack.contains("amd") || haystack.contains("radeon") || haystack.contains("radeonsi")) {
                return AMD;
            }
            if (haystack.contains("intel") || haystack.contains("hd graphics") || haystack.contains("iris") || haystack.contains("uhd")) {
                return INTEL;
            }
            if (haystack.contains("mali")) {
                return ARM_MALI;
            }
            if (haystack.contains("adreno")) {
                return QUALCOMM_ADRENO;
            }
            if (haystack.contains("power vr") || haystack.contains("powervr") || haystack.contains("img")) {
                return IMG_POWERVR;
            }
            if (haystack.contains("apple") || haystack.contains("apple m")) {
                return APPLE;
            }
            if (haystack.contains("v3d") || haystack.contains("broadcom") || haystack.contains("vc4")) {
                return BROADCOM_V3D;
            }
            if (haystack.contains("llvmpipe") || haystack.contains("softpipe") || haystack.contains("swiftshader") || haystack.contains("lavapipe")) {
                return MESA_SOFT;
            }
            if (haystack.contains("translate") || haystack.contains("angle") || haystack.contains("gl4es") || haystack.contains("zink")) {
                return TRANSLATION_LAYER;
            }
            return UNKNOWN;
        }
    }

    /** Where the GL/Vulkan implementation comes from — matters more than speed. */
    public enum Source {
        DRIVER_NATIVE,
        ZINK,
        ANGLE,
        GL4ES,
        VIRGL,
        MOBILEGLUES,
        LTW,
        SOFTWARE,
        UNKNOWN
    }

    private final String vendorString;
    private final String rendererString;
    private final String versionString;
    private final String glslVersion;
    private final Vendor vendor;
    private final Source source;
    private final int glMajor;
    private final int glMinor;
    private final boolean coreProfile;
    private final boolean forwardCompatible;

    public GpuInfo(final String vendorString, final String rendererString, final String versionString,
                   final String glslVersion, final int glMajor, final int glMinor,
                   final boolean coreProfile, final boolean forwardCompatible, final Source source) {
        this.vendorString = Objects.requireNonNullElse(vendorString, "unknown");
        this.rendererString = Objects.requireNonNullElse(rendererString, "unknown");
        this.versionString = Objects.requireNonNullElse(versionString, "unknown");
        this.glslVersion = Objects.requireNonNullElse(glslVersion, "unknown");
        this.glMajor = glMajor;
        this.glMinor = glMinor;
        this.coreProfile = coreProfile;
        this.forwardCompatible = forwardCompatible;
        this.source = Objects.requireNonNull(source, "source");
        this.vendor = Vendor.from(this.vendorString, this.rendererString);
    }

    public String getVendorString() {
        return this.vendorString;
    }

    public String getRendererString() {
        return this.rendererString;
    }

    public String getVersionString() {
        return this.versionString;
    }

    public String getGlslVersion() {
        return this.glslVersion;
    }

    public Vendor getVendor() {
        return this.vendor;
    }

    public Source getSource() {
        return this.source;
    }

    public int getGlMajor() {
        return this.glMajor;
    }

    public int getGlMinor() {
        return this.glMinor;
    }

    /** Comparable GL level: 460 == 4.6, 330 == 3.3, 210 == 2.1. */
    public int getGlLevel() {
        return this.glMajor * 100 + this.glMinor;
    }

    public boolean isCoreProfile() {
        return this.coreProfile;
    }

    public boolean isForwardCompatible() {
        return this.forwardCompatible;
    }

    /** A translation layer cannot be trusted with 4.6 semantics even if it reports them. */
    public boolean isTranslated() {
        return this.source != Source.DRIVER_NATIVE && this.source != Source.UNKNOWN;
    }

    public boolean isMobileGpu() {
        switch (this.vendor) {
            case ARM_MALI:
            case QUALCOMM_ADRENO:
            case IMG_POWERVR:
            case BROADCOM_V3D:
            case APPLE:
                return true;
            default:
                return false;
        }
    }

    public boolean isSoftware() {
        return this.vendor == Vendor.MESA_SOFT;
    }

    /**
     * Heuristic per-backend penalty. Values are ordinal-ish weights, not
     * predicted frame times: the selector only ever compares them.
     *
     * <ul>
     *   <li>Translated GL cannot offer real DSA or coherent persistent mapping, so
     *       the GL46 path is penalised hard rather than disabled — Zink 4.6 is
     *       genuinely usable on some devices.</li>
     *   <li>PowerVR/Adreno want fewer, larger indirect batches: same pass cost but
     *       much lower per-section overhead, so GL46 stays preferred where present.</li>
     *   <li>Software rasterizers should never run a compute-based HZB: the pyramid
     *       costs more than the triangles it saves.</li>
     * </ul>
     */
    public int penaltyFor(final RenderBackend backend) {
        int penalty = 0;
        if (this.isTranslated()) {
            penalty += 40;
        }
        if (this.isSoftware() && backend != RenderBackend.COMPATIBILITY) {
            penalty += 80;
        }
        if (backend == RenderBackend.VULKAN_13) {
            if (this.source == Source.ZINK) {
                // Vulkan on top of GL on top of Vulkan: pointless and lossy.
                penalty += 120;
            }
            if (this.vendor == Vendor.NVIDIA) {
                penalty += 10;
            }
            if (this.vendor == Vendor.AMD) {
                penalty -= 5;
            }
        }
        if (backend == RenderBackend.GL46_DSA && !this.coreProfile) {
            penalty += 25;
        }
        if (backend == RenderBackend.GL_LEGACY && !this.isTranslated()) {
            penalty += 60;
        }
        if (this.vendor == Vendor.INTEL && this.getGlLevel() < 450 && backend == RenderBackend.GL46_DSA) {
            // Pre-Arc Intel drivers report DSA but serialise mapped ranges; the
            // persistent path thrashes there.
            penalty += 30;
        }
        return penalty;
    }

    public String describe() {
        return this.rendererString + " [" + this.vendor + "/" + this.source + ", GL " + this.glMajor + '.' + this.glMinor
                + (this.coreProfile ? " core" : " compat") + ']';
    }

    @Override
    public String toString() {
        return describe();
    }
}
