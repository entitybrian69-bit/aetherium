package com.aetherium.shader;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import com.aetherium.config.AetheriumConfig;
import com.aetherium.util.AetheriumLog;

/**
 * Iris / Oculus compatibility without depending on either.
 *
 * <p>Binding contract — every name here was read from the upstream sources on
 * 2026-10-06 (IrisShaders/Iris @ branch {@code 1.21.1}, files
 * {@code common/src/api/java/net/irisshaders/iris/api/v0/IrisApi.java} and
 * {@code .../IrisApiConfig.java}), which is why no method is looked up by a
 * fuzzy name:</p>
 * <pre>
 *   IrisApi.getInstance()          -&gt; static field
 *                                    net.irisshaders.iris.apiimpl.IrisApiV0Impl.INSTANCE
 *   int    getMinorApiRevision()
 *   boolean isShaderPackInUse()
 *   boolean isRenderingShadowPass()
 *   Object openMainIrisScreenObj(Object parent)
 *   String getMainScreenLanguageKey()
 *   IrisApiConfig getConfig()      -&gt; boolean areShadersEnabled()
 *                                     void setShadersEnabledAndApply(boolean)
 *   float  getSunPathRotation()
 * </pre>
 *
 * <p>Oculus is a fork of Iris at 1.18.2 and keeps the same interface with the
 * pre-rename package {@code net.coderbot.iris.*}; both are probed, Iris first for
 * 1.19.4+, coderbot first below that. Every lookup is cached in
 * {@link Bound}, and a missing method degrades one capability at a time — losing
 * {@code getSunPathRotation} must not disable the shader screen button.</p>
 *
 * <p>Thread-safety: {@code bound} is assigned once by {@link #bind()} from the
 * client init path and read from the render thread and the GUI; publication is a
 * volatile write of an immutable object. Call counts are atomic because the GUI
 * prints them while workers also query shader state.</p>
 */
public final class IrisBridge {
    private static final AetheriumLog LOGGER = AetheriumLog.of(IrisBridge.class);

    /** Probed in this order; the first fully-resolvable entry wins. */
    private static final String[][] CANDIDATES = {
            {"net.irisshaders.iris.apiimpl.IrisApiV0Impl", "net.irisshaders.iris.api.v0.IrisApi", "Iris"},
            {"net.coderbot.iris.Iris", "net.coderbot.iris.api.v0.IrisApi", "Oculus"},
            {"net.coderbot.iris.apiimpl.IrisApiV0Impl", "net.coderbot.iris.api.v0.IrisApi", "Oculus"},
            {"net.irisshaders.iris.Iris", "net.irisshaders.iris.api.v0.IrisApi", "Iris"},
    };

    private final AetheriumConfig config;
    private final AtomicInteger screenOpenRequests = new AtomicInteger();
    private final AtomicInteger stateQueries = new AtomicInteger();
    private final AtomicInteger toggles = new AtomicInteger();

    /** Volatile publication of an immutable snapshot. */
    private volatile Bound bound;
    private String absenceReason = "not probed";

    public IrisBridge(final AetheriumConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    /** Immutable resolved view of the API; every method may be null. */
    private static final class Bound {
        final Object apiInstance;
        final Class<?> apiType;
        final String brand;
        final Method isShaderPackInUse;
        final Method isRenderingShadowPass;
        final Method openMainIrisScreenObj;
        final Method getMainScreenLanguageKey;
        final Method getConfig;
        final Method getMinorApiRevision;
        final Method getSunPathRotation;
        final Object configInstance;
        final Method areShadersEnabled;
        final Method setShadersEnabledAndApply;
        final int revision;

        Bound(final Object apiInstance, final Class<?> apiType, final String brand, final Method isShaderPackInUse,
              final Method isRenderingShadowPass, final Method openMainIrisScreenObj, final Method getMainScreenLanguageKey,
              final Method getConfig, final Method getMinorApiRevision, final Method getSunPathRotation,
              final Object configInstance, final Method areShadersEnabled, final Method setShadersEnabledAndApply,
              final int revision) {
            this.apiInstance = apiInstance;
            this.apiType = apiType;
            this.brand = brand;
            this.isShaderPackInUse = isShaderPackInUse;
            this.isRenderingShadowPass = isRenderingShadowPass;
            this.openMainIrisScreenObj = openMainIrisScreenObj;
            this.getMainScreenLanguageKey = getMainScreenLanguageKey;
            this.getConfig = getConfig;
            this.getMinorApiRevision = getMinorApiRevision;
            this.getSunPathRotation = getSunPathRotation;
            this.configInstance = configInstance;
            this.areShadersEnabled = areShadersEnabled;
            this.setShadersEnabledAndApply = setShadersEnabledAndApply;
            this.revision = revision;
        }
    }

    /** Resolves the API. Safe to call twice; the second call is a no-op. */
    public void bind() {
        if (this.bound != null || !this.config.irisIntegration.get()) {
            return;
        }
        for (final String[] candidate : CANDIDATES) {
            try {
                final Bound resolved = tryBind(candidate[0], candidate[1], candidate[2]);
                if (resolved != null) {
                    this.bound = resolved;
                    LOGGER.info("Bound {} shader-mod API (v0 revision {}): {}", resolved.brand, resolved.revision, candidate[0]);
                    return;
                }
            } catch (final ClassNotFoundException | NoClassDefFoundError absent) {
                // Expected on a machine without the mod: keep probing quietly.
                this.absenceReason = candidate[0] + " is not present";
            } catch (final ReflectiveOperationException | LinkageError | RuntimeException error) {
                // Present but shaped differently: worth telling the user, since it
                // usually means an API change between two versions of Iris itself.
                LOGGER.warn("Found " + candidate[0] + " but its shape is unexpected ("
                        + error.getClass().getSimpleName() + ": " + error.getMessage() + ')', error);
                this.absenceReason = "present but incompatible: " + error.getClass().getSimpleName();
            }
        }
        LOGGER.info("No Iris/Oculus API bound ({}); shader integration is off", this.absenceReason);
    }

    private static Bound tryBind(final String implClass, final String apiClass, final String brand)
            throws ReflectiveOperationException {
        final Class<?> impl = Class.forName(implClass);
        final Object instance = resolveInstance(impl);
        if (instance == null) {
            // Older Iris exposes getInstance() on the interface instead of a field.
            final Class<?> api = Class.forName(apiClass);
            final Method getter = find(api, "getInstance");
            if (getter == null || !java.lang.reflect.Modifier.isStatic(getter.getModifiers())) {
                throw new NoSuchMethodException("neither INSTANCE nor getInstance() on " + implClass);
            }
            getter.setAccessible(true);
            final Object fromGetter = getter.invoke(null);
            return build(fromGetter, api, brand);
        }
        final Class<?> api = Class.forName(apiClass);
        return build(instance, api, brand);
    }

    private static Object resolveInstance(final Class<?> impl) {
        try {
            final java.lang.reflect.Field field = impl.getField("INSTANCE");
            field.setAccessible(true);
            return field.get(null);
        } catch (final NoSuchFieldException error) {
            return null;
        } catch (final ReflectiveOperationException error) {
            LOGGER.dev("INSTANCE field on {} is not readable: {}", impl.getName(), error.getMessage());
            return null;
        }
    }

    private static Bound build(final Object instance, final Class<?> api, final String brand) {
        if (instance == null) {
            return null;
        }
        final Class<?> type = instance.getClass();
        int revision = -1;
        final Method revisionMethod = find(api, "getMinorApiRevision");
        if (revisionMethod != null) {
            try {
                revisionMethod.setAccessible(true);
                final Object value = revisionMethod.invoke(instance);
                revision = value instanceof Integer ? (Integer) value : -1;
            } catch (final ReflectiveOperationException | RuntimeException error) {
                LOGGER.dev("Could not read the API revision: {}", error.getClass().getSimpleName());
            }
        }
        Object configInstance = null;
        Method areShadersEnabled = null;
        Method setShadersEnabled = null;
        final Method getConfig = find(api, "getConfig");
        if (getConfig != null) {
            try {
                getConfig.setAccessible(true);
                configInstance = getConfig.invoke(instance);
                if (configInstance != null) {
                    final Class<?> configType = configInstance.getClass();
                    areShadersEnabled = findAny(configType, "areShadersEnabled", "isShadersModEnabled");
                    setShadersEnabled = findAny(configType, "setShadersEnabledAndApply", "setShadersEnabled");
                }
            } catch (final ReflectiveOperationException | RuntimeException error) {
                LOGGER.warn("Iris config could not be resolved; shader toggling will be unavailable", error);
            }
        }
        return new Bound(instance, api, brand,
                find(api, "isShaderPackInUse"),
                find(api, "isRenderingShadowPass"),
                find(api, "openMainIrisScreenObj"),
                find(api, "getMainScreenLanguageKey"),
                getConfig,
                revisionMethod,
                find(api, "getSunPathRotation"),
                configInstance, areShadersEnabled, setShadersEnabled,
                revision);
    }

    /** Finds a no-arg method by name on a type or any of its interfaces/superclasses. */
    private static Method find(final Class<?> type, final String name) {
        return findAny(type, name);
    }

    private static Method findAny(final Class<?> type, final String... names) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (final String name : names) {
                for (final Method method : current.getDeclaredMethods()) {
                    if (method.getName().equals(name) && method.getParameterCount() == 0) {
                        return method;
                    }
                }
                // Interface methods are declared on the interface, not the impl.
                for (final Class<?> iface : current.getInterfaces()) {
                    final Method inherited = findAny(iface, name);
                    if (inherited != null) {
                        return inherited;
                    }
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private Object invoke(final Method method, final Object... args) {
        final Bound local = this.bound;
        if (method == null || local == null) {
            return null;
        }
        try {
            method.setAccessible(true);
            return method.invoke(local.apiInstance, args);
        } catch (final ReflectiveOperationException | RuntimeException | LinkageError error) {
            // One warn per (method, cause) pair: an invoke failure inside a render
            // loop would otherwise produce 240 identical lines a second.
            if (ERROR_LOGGED.compareAndSet(0, 1)) {
                LOGGER.warn("Iris API call " + method.getName() + " failed; it is now treated as returning the default", error);
            } else {
                LOGGER.dev("Iris API call {} failed again: {}", method.getName(), error.toString());
            }
            return null;
        }
    }

    /** Once-flag so the first failure is loud and the rest are silent. */
    private static final AtomicInteger ERROR_LOGGED = new AtomicInteger();

    public boolean isPresent() {
        return this.bound != null;
    }

    public String getBrand() {
        final Bound local = this.bound;
        return local == null ? "none" : local.brand;
    }

    public String getVersion() {
        final Bound local = this.bound;
        if (local == null) {
            return "absent";
        }
        return "v0." + local.revision;
    }

    /** True when a shader pack is actively in use (and shaders are enabled). */
    public boolean isShaderPackInUse() {
        this.stateQueries.incrementAndGet();
        final Object value = invoke(this.bound == null ? null : this.bound.isShaderPackInUse);
        return Boolean.TRUE.equals(value);
    }

    public boolean isRenderingShadowPass() {
        final Object value = invoke(this.bound == null ? null : this.bound.isRenderingShadowPass);
        return Boolean.TRUE.equals(value);
    }

    public boolean areShadersEnabled() {
        final Bound local = this.bound;
        if (local == null || local.configInstance == null) {
            return false;
        }
        final Object value = invoke(local.areShadersEnabled);
        return value == null ? isShaderPackInUse() : Boolean.TRUE.equals(value);
    }

    /**
     * Asks the shader mod to switch shaders on or off and reload. This is the
     * mechanism used by conflict handling: it is Iris' own supported path, so no
     * internal state is touched.
     *
     * @return false when no API is bound or the call was refused
     */
    public boolean setShadersEnabled(final boolean enabled, final String reason) {
        final Bound local = this.bound;
        if (local == null || local.configInstance == null || local.setShadersEnabledAndApply == null) {
            LOGGER.info("Cannot toggle shaders ({}): no Iris/Oculus config API — reason: {}", reason, this.absenceReason);
            return false;
        }
        try {
            local.setShadersEnabledAndApply.setAccessible(true);
            local.setShadersEnabledAndApply.invoke(local.configInstance, enabled);
            this.toggles.incrementAndGet();
            LOGGER.info("Asked {} to {} shaders ({})", local.brand, enabled ? "enable and apply" : "disable and apply", reason);
            return true;
        } catch (final ReflectiveOperationException | RuntimeException error) {
            LOGGER.warn("Toggling shaders failed (" + reason + ')', error);
            return false;
        }
    }

    /** Opens the shader-pack screen over {@code parent}; false when unavailable. */
    public boolean openShaderScreen(final Object parentScreen) {
        this.screenOpenRequests.incrementAndGet();
        final Bound local = this.bound;
        if (local == null || local.openMainIrisScreenObj == null) {
            return false;
        }
        final Object screen = invoke(local.openMainIrisScreenObj, parentScreen);
        if (screen == null) {
            return false;
        }
        try {
            final Class<?> screenType = Class.forName("net.minecraft.client.gui.screens.Screen");
            if (!screenType.isInstance(screen)) {
                LOGGER.warn("openMainIrisScreenObj returned {} which is not a Screen", screen.getClass().getName());
                return false;
            }
            final Class<?> minecraftType = Class.forName("net.minecraft.client.Minecraft");
            final Method getInstance = minecraftType.getMethod("getInstance");
            final Object mc = getInstance.invoke(null);
            final Method setScreen = minecraftType.getMethod("setScreen", screenType);
            setScreen.invoke(mc, screen);
            return true;
        } catch (final ReflectiveOperationException | RuntimeException error) {
            LOGGER.warn("Could not show the shader screen", error);
            return false;
        }
    }

    /** Language key for the shader screen button label, or the Aetherium default. */
    public String getScreenLanguageKey() {
        final Object value = invoke(this.bound == null ? null : this.bound.getMainScreenLanguageKey);
        return value instanceof String ? (String) value : "aetherium.shaders.open";
    }

    // [UNVERIFIED: IrisApiV0#getSunPathRotation returning float degrees (vs an Optional or a
    // config record field) on Iris 1.7.x/1.8.x for 1.21.1. Every call in this class is bound by
    // reflection with a per-method fallback, so an absent method means "0.0 and one dev log".]
    public float getSunPathRotation() {
        final Object value = invoke(this.bound == null ? null : this.bound.getSunPathRotation);
        return value instanceof Float ? (Float) value : 0.0f;
    }

    /** A shader pack owns the lightmap; Aetherium must not post-process its output. */
    public boolean shouldSuspendLightmapEdits() {
        return this.config.pauseDynamicLights.get() && isShaderPackInUse();
    }

    public void onWorldChange() {
        if (isPresent()) {
            LOGGER.dev("World change: shader state re-queried (pack in use = {})", isShaderPackInUse());
        }
    }

    public void onRendererChanged() {
        // Nothing to do beyond a re-query: Iris rebuilds its own pipelines when its
        // reload is requested, and Aetherium must not touch them. Recorded here so
        // the intent survives whoever next edits this file.
        if (isPresent()) {
            LOGGER.info("Renderer changed: shader integration retained ({} {})", getBrand(), getVersion());
        }
    }

    public String describe() {
        if (!isPresent()) {
            return "Iris/Oculus: not present (" + this.absenceReason + ')';
        }
        return String.format(Locale.ROOT, "%s %s, pack in use: %s, shadow pass: %s, %d screen opens, %d toggles",
                getBrand(), getVersion(), isShaderPackInUse(), isRenderingShadowPass(),
                this.screenOpenRequests.get(), this.toggles.get());
    }

    public int getStateQueryCount() {
        return this.stateQueries.get();
    }

    public void shutdown() {
        this.bound = null;
    }
}
