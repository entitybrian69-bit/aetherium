package dev.aetherium.compat;

import dev.aetherium.Aetherium;
import dev.aetherium.config.AetheriumConfig;
import dev.aetherium.platform.Services;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Optional;

/**
 * Reflection-based bridge to Iris (Fabric/NeoForge) and Oculus (Forge 1.20.1 and earlier). Aetherium never hard-links
 * against either jar, so the same class works whether or not shaders are installed. Through the bridge we can:
 * read/apply the current shader pack, open Iris' own pack screen from the purple GUI, and yield the terrain pipeline
 * to Iris while a pack is active (Iris owns the shadow/gbuffer passes; our HZB culling stays on).
 */
public final class IrisCompat {
    private static final IrisCompat INSTANCE = new IrisCompat();

    private static final String[] API_CLASSES = {
            "net.irisshaders.iris.api.v0.IrisApi",   // Iris 1.2+ / Oculus
            "net.coderbot.iris.apiimpl.IrisApiV0Impl" // legacy fallback
    };

    private boolean present;
    private String flavor = "none";
    private Object api;
    private MethodHandle isShaderPackInUse;
    private MethodHandle isRenderingShadowPass;
    private MethodHandle openMainIrisScreen;
    private MethodHandle getMainScreen;
    private MethodHandle getConfig;
    private MethodHandle areShadersEnabled;
    private MethodHandle setShadersEnabled;

    private IrisCompat() {}

    public static IrisCompat get() { return INSTANCE; }

    public void detect() {
        present = false;
        if (Services.PLATFORM.isModLoaded("iris")) flavor = "Iris";
        else if (Services.PLATFORM.isModLoaded("oculus")) flavor = "Oculus";
        else { flavor = "none"; return; }

        for (String cn : API_CLASSES) {
            try {
                Class<?> apiClass = Class.forName(cn);
                MethodHandles.Lookup l = MethodHandles.publicLookup();
                api = l.findStatic(apiClass, "getInstance", MethodType.methodType(apiClass)).invoke();
                isShaderPackInUse = l.findVirtual(apiClass, "isShaderPackInUse", MethodType.methodType(boolean.class)).bindTo(api);
                isRenderingShadowPass = l.findVirtual(apiClass, "isRenderingShadowPass", MethodType.methodType(boolean.class)).bindTo(api);
                try {
                    getMainScreen = l.findVirtual(apiClass, "openMainIrisScreenObj", MethodType.methodType(Object.class, Object.class)).bindTo(api);
                } catch (NoSuchMethodException ignored) {}
                try {
                    getConfig = l.findVirtual(apiClass, "getConfig", MethodType.methodType(Class.forName("net.irisshaders.iris.api.v0.IrisApiConfig"))).bindTo(api);
                    Class<?> cfgClass = Class.forName("net.irisshaders.iris.api.v0.IrisApiConfig");
                    areShadersEnabled = l.findVirtual(cfgClass, "areShadersEnabled", MethodType.methodType(boolean.class));
                    setShadersEnabled = l.findVirtual(cfgClass, "setShadersEnabledAndApply", MethodType.methodType(void.class, boolean.class));
                } catch (ReflectiveOperationException ignored) {}
                present = true;
                Aetherium.LOGGER.info("[Aetherium] {} API bound via {}", flavor, cn);
                return;
            } catch (Throwable t) {
                Aetherium.LOGGER.debug("[Aetherium] {} not bindable via {}: {}", flavor, cn, t.toString());
            }
        }
        Aetherium.LOGGER.warn("[Aetherium] {} is installed but its API could not be bound; shader toggles disabled", flavor);
    }

    public boolean isPresent() { return present; }
    public String flavor() { return flavor; }

    public boolean isShaderPackInUse() {
        if (!present || !AetheriumConfig.get().shaders.irisCompatibility) return false;
        try { return (boolean) isShaderPackInUse.invoke(); } catch (Throwable t) { return false; }
    }

    public boolean isRenderingShadowPass() {
        if (!present) return false;
        try { return (boolean) isRenderingShadowPass.invoke(); } catch (Throwable t) { return false; }
    }

    public boolean areShadersEnabled() {
        if (!present || getConfig == null) return false;
        try { return (boolean) areShadersEnabled.invoke(getConfig.invoke()); } catch (Throwable t) { return false; }
    }

    public void setShadersEnabled(boolean on) {
        if (!present || setShadersEnabled == null) return;
        try { setShadersEnabled.invoke(getConfig.invoke(), on); } catch (Throwable t) {
            Aetherium.LOGGER.warn("[Aetherium] Failed to toggle shaders via {}", flavor, t);
        }
    }

    /** Returns the Iris shader-pack selection Screen (as Object to stay MC-agnostic) with {@code parent} as parent. */
    public Optional<Object> createShaderPackScreen(Object parent) {
        if (!present || getMainScreen == null) return Optional.empty();
        try { return Optional.ofNullable(getMainScreen.invoke(parent)); } catch (Throwable t) { return Optional.empty(); }
    }

    /** While a pack is active Iris drives the terrain passes; Aetherium keeps culling + chunk scheduling only. */
    public boolean shouldYieldTerrainPipeline() { return isShaderPackInUse(); }

    /** Dynamic lights are injected into Iris' lightmap uniform path, which it reads from the vanilla LightTexture. */
    public boolean shaderDynamicLightsActive() {
        return isShaderPackInUse() && AetheriumConfig.get().shaders.shaderDynamicLights;
    }
}
