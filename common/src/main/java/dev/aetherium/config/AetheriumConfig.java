package dev.aetherium.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.aetherium.Aetherium;
import dev.aetherium.platform.Services;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Single source of truth for every Aetherium option. Mirrors docs/CONFIG_SCHEMA.json.
 * All fields are plain so Gson can (de)serialize; call {@link #save()} after edits and
 * {@link #fireChanged()} to apply live.
 */
public final class AetheriumConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String FILE_NAME = "aetherium.json";
    private static volatile AetheriumConfig INSTANCE;

    public int schemaVersion = 1;
    public General general = new General();
    public Performance performance = new Performance();
    public Quality quality = new Quality();
    public Shaders shaders = new Shaders();
    public Utilities utilities = new Utilities();
    public Advanced advanced = new Advanced();
    public Android android = new Android();

    private transient final List<Consumer<AetheriumConfig>> listeners = new CopyOnWriteArrayList<>();

    public static final class General {
        public int renderDistance = 12;
        public int simulationDistance = 12;
        public int guiScale = 0;
        public boolean fullscreen = false;
        public boolean vsync = true;
    }

    public static final class Performance {
        /** 0 = auto (cores - 1, min 1). */
        public int chunkBuilderThreads = 0;
        public EntityCullingMode entityCulling = EntityCullingMode.NORMAL;
        public boolean fogOcclusion = true;
        public boolean fastMath = true;
        public boolean dynamicEntityBatching = true;
        public boolean asyncChunkMeshing = true;
        public boolean hzbOcclusionCulling = true;
        public boolean gpuDrivenRendering = true;
        public boolean persistentMappedBuffers = true;
        public boolean multiDrawIndirect = true;
    }

    public static final class Quality {
        public int mipmapLevels = 4;
        /** 1 = off; 2/4/8/16 supported. */
        public int anisotropicFiltering = 1;
        public boolean smoothLighting = true;
        public int biomeBlend = 2;
        public CloudQuality cloudQuality = CloudQuality.FANCY;
        public ParticleDensity particleDensity = ParticleDensity.ALL;
        public TextureFiltering textureFiltering = TextureFiltering.NEAREST;
    }

    public static final class Shaders {
        public boolean irisCompatibility = true;
        public String shaderPack = "";
        public boolean shaderDynamicLights = true;
    }

    public static final class Utilities {
        public boolean dynamicLights = true;
        public DynamicLightQuality dynamicLightQuality = DynamicLightQuality.FANCY;
        public boolean dynamicLightsEntities = true;
        public boolean dynamicLightsItems = true;
        public boolean dynamicLightsSelf = true;
        public boolean coloredDynamicLights = true;

        public boolean gammaUtilities = true;
        /** 0.0 .. 10.0 (vanilla slider stops at 1.0). */
        public double brightness = 1.0;
        public boolean nightVision = false;
        public boolean caveVision = false;
        public boolean timeBasedGamma = false;
        public boolean customGammaCurve = false;
        /** 5 control points, each 0..1, sampled across light level 0..15. */
        public double[] gammaCurve = {0.0, 0.25, 0.5, 0.75, 1.0};
    }

    public static final class Advanced {
        public GraphicsApi graphicsApi = GraphicsApi.AUTO;
        public EngineMode engineMode = EngineMode.AETHERIUM;
        public boolean debugOverlay = false;
        public boolean frameTimeGraph = false;
        /** Megabytes reserved for persistent vertex storage; 0 = auto from JVM heap. */
        public int vertexArenaMb = 0;
        public boolean shaderBinaryCache = true;
        public boolean disableConflictingMods = true;
    }

    public static final class Android {
        public AndroidBackend backend = AndroidBackend.AUTO;
        public boolean mobileMemoryMode = false;
        public boolean touchOptimization = true;
        public boolean batterySaver = false;
        public boolean thermalThrottleProtection = true;
        public int batterySaverMinRenderDistance = 4;
        public int thermalLimitCelsius = 45;
    }

    // ---- Enums (also used by GUI cyclers) ----
    public enum EntityCullingMode { OFF, NORMAL, AGGRESSIVE, EXTREME }
    public enum CloudQuality { OFF, FAST, FANCY }
    public enum ParticleDensity { ALL, DECREASED, MINIMAL }
    public enum TextureFiltering { NEAREST, LINEAR, ANISOTROPIC }
    public enum DynamicLightQuality { FASTEST, FAST, FANCY, REALTIME }
    public enum GraphicsApi { AUTO, OPENGL, VULKAN }
    public enum EngineMode { AETHERIUM, COMPATIBILITY }
    public enum AndroidBackend { AUTO, VULKAN_NATIVE, LTW, GL4ES }

    // ---- Lifecycle ----
    public static AetheriumConfig get() {
        AetheriumConfig c = INSTANCE;
        if (c == null) {
            synchronized (AetheriumConfig.class) {
                if ((c = INSTANCE) == null) INSTANCE = c = load();
            }
        }
        return c;
    }

    private static Path path() {
        return Services.PLATFORM.getConfigDirectory().resolve(FILE_NAME);
    }

    private static AetheriumConfig load() {
        Path p = path();
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p)) {
                AetheriumConfig cfg = GSON.fromJson(r, AetheriumConfig.class);
                if (cfg != null) {
                    cfg.validate();
                    return cfg;
                }
            } catch (IOException | JsonParseException e) {
                Aetherium.LOGGER.error("[Aetherium] Config unreadable, regenerating defaults", e);
            }
        }
        AetheriumConfig cfg = new AetheriumConfig();
        cfg.save();
        return cfg;
    }

    public void save() {
        try {
            Files.createDirectories(path().getParent());
            try (Writer w = Files.newBufferedWriter(path())) {
                GSON.toJson(this, w);
            }
        } catch (IOException e) {
            Aetherium.LOGGER.error("[Aetherium] Failed to save config", e);
        }
    }

    public void validate() {
        general.renderDistance = clamp(general.renderDistance, 2, 64);
        general.simulationDistance = clamp(general.simulationDistance, 5, 32);
        general.guiScale = clamp(general.guiScale, 0, 8);
        performance.chunkBuilderThreads = clamp(performance.chunkBuilderThreads, 0, 64);
        quality.mipmapLevels = clamp(quality.mipmapLevels, 0, 4);
        if (!List.of(1, 2, 4, 8, 16).contains(quality.anisotropicFiltering)) quality.anisotropicFiltering = 1;
        quality.biomeBlend = clamp(quality.biomeBlend, 0, 7);
        utilities.brightness = Math.max(0.0, Math.min(10.0, utilities.brightness));
        if (utilities.gammaCurve == null || utilities.gammaCurve.length != 5) utilities.gammaCurve = new double[]{0, .25, .5, .75, 1};
        for (int i = 0; i < 5; i++) utilities.gammaCurve[i] = Math.max(0, Math.min(1, utilities.gammaCurve[i]));
        advanced.vertexArenaMb = clamp(advanced.vertexArenaMb, 0, 4096);
        android.batterySaverMinRenderDistance = clamp(android.batterySaverMinRenderDistance, 2, 16);
        android.thermalLimitCelsius = clamp(android.thermalLimitCelsius, 35, 60);
    }

    public void addListener(Consumer<AetheriumConfig> l) { listeners.add(l); }

    /** Persist and apply live. */
    public void fireChanged() {
        validate();
        save();
        for (Consumer<AetheriumConfig> l : listeners) {
            try { l.accept(this); } catch (Throwable t) { Aetherium.LOGGER.error("[Aetherium] Config listener failed", t); }
        }
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
