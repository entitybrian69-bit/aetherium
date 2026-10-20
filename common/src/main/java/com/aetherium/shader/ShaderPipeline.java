package com.aetherium.shader;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Runs one loaded shader pack: builds its programs, owns the gbuffer / shadow targets and executes
 * the full-screen passes. Only LWJGL and the pure shader classes: the version-specific runtime
 * decides <em>when</em> things happen (frame start, shadow pass, each draw, translucent, frame end)
 * and which program a draw uses.
 *
 * <p>GL state discipline: Minecraft's GlStateManager caches state, so everything changed here
 * either is state vanilla never caches (programs, framebuffers, texture units 3+, matrices) or is
 * saved and restored around the change (enables and depth state through glPushAttrib, clear color
 * and depth mask through glGet). Texture units 0 (block atlas / entity texture), 1 (overlay) and 2
 * (lightmap) are never rebound; pack samplers get units 3 and up.</p>
 */
public final class ShaderPipeline {
    public static final int STAGE_GBUFFERS = 0;
    public static final int STAGE_SHADOW = 1;
    public static final int STAGE_COMPOSITE = 2;

    /** Decodes a PNG into a GL texture (version-specific: NativeImage); returns 0 on failure. */
    public interface TextureLoader {
        int load(byte[] png, boolean blur, boolean clamp);
    }

    /** GL_TEXTUREi vanilla last made active (tracked from GlStateManager by the runtime). */
    public static volatile int vanillaActiveTexture = GL13.GL_TEXTURE0;
    /**
     * Set by the runtime: tells Minecraft's texture-binding cache that units 0-2 hold nothing, so
     * vanilla really re-binds its atlas, overlay, lightmap and font after a full-screen pass used
     * those units (or after anything else touched them). Null: raw unbind only.
     */
    public static volatile Runnable vanillaTextureReset;

    public final ShaderPack pack;
    public final ShaderPack.ProgramSet set;
    public final GlCaps caps;
    public final Targets targets;
    public final FrameState frame = new FrameState();
    public final List<String> errors = new ArrayList<>();
    public final List<String> warnings = new ArrayList<>();

    private final Map<String, GlProgram> built = new LinkedHashMap<>();
    private final Map<String, GlProgram> resolved = new HashMap<>();
    private final List<GlProgram> prepare = new ArrayList<>();
    private final List<GlProgram> deferred = new ArrayList<>();
    private final List<GlProgram> composite = new ArrayList<>();
    private GlProgram finalProgram;
    private GlProgram shadowProgram;
    private GlProgram copyProgram;
    private GlProgram depthCopyProgram;
    private final Map<String, Integer> customTextures = new HashMap<>();
    private final Map<GlProgram, int[]> fboKeys = new HashMap<>();
    private final Map<GlProgram, Integer> fboCache = new HashMap<>();
    private int noiseTex;
    private int whiteTex;
    private int normalTex;
    private int specularTex;
    private final int[] boundUnit = new int[32];
    private final int scratchUnit;

    public final boolean shadowEnabled;
    public final int shadowResolution;
    public final float shadowDistance;
    public final float shadowRenderDistance;
    public final boolean shadowTranslucent;
    public final float sunPathRotation;
    public final boolean usesCenterDepth;
    public final boolean oldLighting;
    public final String clouds;
    public final boolean sun;
    public final boolean moon;
    public final boolean vignette;
    public final boolean underwaterOverlay;
    private final boolean hardwareShadow;

    // per frame
    private GlProgram current;
    private int currentStage = -1;
    private int depthCopyMode; // 0 none yet, 1 copied, -1 unavailable (alias depth0)
    private boolean translucentDone;
    private int width;
    private int height;
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer clearSave = BufferUtils.createFloatBuffer(16);
    private final ByteBuffer maskSave = BufferUtils.createByteBuffer(16);

    private static final Pattern BUFFER_NAME = Pattern.compile(
            "\\b(colortex(1[0-5]|[0-9])|gcolor|gdepth|gnormal|composite|gaux[1-4])\\b");
    private static final String[] LEGACY = {"gcolor", "gdepth", "gnormal", "composite", "gaux1", "gaux2", "gaux3",
            "gaux4"};

    public ShaderPipeline(final ShaderPack pack, final String worldFolder, final GlCaps caps,
                          final TextureLoader loader, final boolean liteProfile) {
        this.pack = pack;
        this.set = pack.programs(worldFolder);
        this.caps = caps;
        this.targets = new Targets(caps.translator, caps.floatTextures);
        this.scratchUnit = Math.min(caps.maxTextureUnits, 32) - 1;
        for (int i = 0; i < this.boundUnit.length; i++) {
            this.boundUnit[i] = -1;
        }
        final PackProperties props = pack.properties;
        this.oldLighting = props.flag("oldLighting", false);
        this.clouds = props.get("clouds", "");
        this.sun = props.flag("sun", true);
        this.moon = props.flag("moon", true);
        this.vignette = props.flag("vignette", true);
        this.underwaterOverlay = props.flag("underwaterOverlay", true);
        this.sunPathRotation = (float) this.set.constNumber("sunPathRotation", 0);

        // ---- buffers used anywhere, with their formats from the consts
        final boolean[] used = new boolean[16];
        used[0] = true;
        for (final ProgramSource p : this.set.defined.values()) {
            if (p.drawBuffers != null) {
                for (final int b : p.drawBuffers) {
                    if (b >= 0 && b < 16) {
                        used[b] = true;
                    }
                }
            }
            for (final ProgramSource.Expanded e : new ProgramSource.Expanded[] {p.vertex, p.fragment, p.geometry}) {
                if (e == null) {
                    continue;
                }
                final Matcher m = BUFFER_NAME.matcher(e.text);
                while (m.find()) {
                    used[bufferIndex(m.group(1))] = true;
                }
            }
        }
        for (int i = 0; i < 16; i++) {
            if (!used[i]) {
                continue;
            }
            final String format = this.bufferConst(i, "Format", null);
            final Targets.Color c = this.targets.declare(i, format == null ? "RGBA8" : format.trim());
            final String clear = this.bufferConst(i, "Clear", null);
            final String clearColor = this.bufferConst(i, "ClearColor", null);
            Targets.configureClear(c, clear == null || !"false".equals(clear.trim()), parseVec4(clearColor));
            Targets.configureMipmap(c, "true".equals(String.valueOf(this.bufferConst(i, "MipmapEnabled", "false")).trim()));
            final String size = props.bufferSize.get("colortex" + i) != null ? props.bufferSize.get("colortex" + i)
                    : i < LEGACY.length ? props.bufferSize.get(LEGACY[i]) : null;
            if (size != null) {
                Targets.configureSize(c, size);
            }
        }

        // ---- programs
        for (final Map.Entry<String, ProgramSource> e : this.set.defined.entrySet()) {
            final String name = e.getKey();
            if (name.startsWith("composite") || name.startsWith("deferred") || name.startsWith("prepare")
                    || name.startsWith("shadowcomp") || name.equals("final")) {
                continue; // built below in pass order
            }
            final int stage = name.equals("shadow") ? STAGE_SHADOW : STAGE_GBUFFERS;
            final GlProgram p = this.build(name, e.getValue(), stage);
            if (p != null) {
                this.built.put(name, p);
            }
        }
        this.buildPasses("prepare", this.prepare);
        this.buildPasses("deferred", this.deferred);
        this.buildPasses("composite", this.composite);
        if (this.set.finalPass != null) {
            this.finalProgram = this.build("final", this.set.finalPass, STAGE_COMPOSITE);
        }
        if (this.set.passes.containsKey("shadowcomp") && !this.set.passes.get("shadowcomp").isEmpty()) {
            this.warnings.add("shadowcomp programs are not supported yet and were skipped");
        }
        this.copyProgram = this.internal("aetherium_copy", COPY_FSH);

        // ---- shadows
        this.shadowProgram = this.built.get("shadow");
        boolean shadowSampled = false;
        boolean shadowCompare = this.set.constBool("shadowHardwareFiltering", false)
                || this.set.constBool("shadowHardwareFiltering0", false);
        for (final GlProgram p : this.allPrograms()) {
            for (final String n : new String[] {"shadow", "watershadow", "shadowtex0", "shadowtex1", "shadowcolor",
                    "shadowcolor0", "shadowcolor1"}) {
                if (p.has(n)) {
                    shadowSampled = true;
                    if (p.isShadowSampler(n)) {
                        shadowCompare = true;
                    }
                }
            }
        }
        this.hardwareShadow = shadowCompare;
        int res = (int) this.set.constNumber("shadowMapResolution", 1024);
        if (liteProfile || caps.translator) {
            res = Math.min(res, 1024);
        }
        res = Math.max(128, Math.min(res, 4096));
        this.shadowResolution = res;
        this.shadowDistance = (float) this.set.constNumber("shadowDistance", 160);
        final double mul = this.set.constNumber("shadowDistanceRenderMul", -1);
        this.shadowRenderDistance = mul > 0 ? (float) (this.shadowDistance * mul) : this.shadowDistance;
        this.shadowTranslucent = props.flag("shadowTranslucent", true);
        this.shadowEnabled = this.shadowProgram != null;
        if (this.shadowEnabled || shadowSampled) {
            final String[] formats = {this.shadowConst("shadowcolor0Format", "RGBA8"),
                    this.shadowConst("shadowcolor1Format", "RGBA8")};
            this.targets.createShadow(this.shadowResolution, 2, formats, shadowCompare, shadowCompare,
                    this.set.constBool("shadowtex0Nearest", false), this.set.constBool("shadowtex1Nearest", false));
            final String[] keys = {"shadowcolor0Clear", "shadowcolor1Clear"};
            for (int i = 0; i < 2; i++) {
                final String v = this.set.constValue(keys[i], null);
                this.targets.shadowClearOn[i] = v == null || !"false".equals(v.trim());
                final float[] cc = parseVec4(this.set.constValue("shadowcolor" + i + "ClearColor", null));
                if (cc != null) {
                    this.targets.shadowClear[i] = cc;
                }
            }
            this.targets.clearShadow(); // valid content even when nothing renders into them
        }
        this.usesCenterDepth = this.anyUses("centerDepthSmooth");
        this.frame.sunPathRotation = this.sunPathRotation;
        this.frame.shadowIntervalSize = (float) this.set.constNumber("shadowIntervalSize", 2);
        this.frame.shadowHalfPlane = this.shadowDistance;
        this.frame.wetnessHalflife = (float) this.set.constNumber("wetnessHalflife", 600);
        this.frame.drynessHalflife = (float) this.set.constNumber("drynessHalflife", 200);
        this.frame.eyeBrightnessHalflife = (float) this.set.constNumber("eyeBrightnessHalflife", 10);
        this.frame.centerDepthHalflife = (float) this.set.constNumber("centerDepthHalflife", 1);
        for (final String err : this.frame.setCustoms(props.customs)) {
            this.warnings.add("custom uniform " + err);
        }

        // ---- textures
        this.whiteTex = solidTexture(255, 255, 255, 255);
        this.normalTex = solidTexture(127, 127, 255, 255);
        this.specularTex = solidTexture(0, 0, 0, 0);
        this.noiseTex = this.loadNoise(loader);
        for (final Map.Entry<String, String> e : props.textures.entrySet()) {
            final int tex = this.loadCustom(e.getValue(), loader);
            if (tex != 0) {
                this.customTextures.put(e.getKey(), tex);
            } else {
                this.warnings.add("texture." + e.getKey() + " (" + e.getValue() + ") could not be loaded");
            }
        }
        this.restoreActive();
        GL20.glUseProgram(0);
    }

    // ================================================================== building

    private String bufferConst(final int index, final String suffix, final String def) {
        String v = this.set.constValue("colortex" + index + suffix, null);
        if (v == null && index < LEGACY.length) {
            v = this.set.constValue(LEGACY[index] + suffix, null);
        }
        return v == null ? def : v;
    }

    private String shadowConst(final String name, final String def) {
        final String v = this.set.constValue(name, null);
        return v == null ? def : v.trim();
    }

    static int bufferIndex(final String name) {
        if (name.startsWith("colortex")) {
            return Integer.parseInt(name.substring(8));
        }
        for (int i = 0; i < LEGACY.length; i++) {
            if (LEGACY[i].equals(name)) {
                return i;
            }
        }
        return 0;
    }

    static float[] parseVec4(final String v) {
        if (v == null) {
            return null;
        }
        final Matcher m = Pattern.compile("-?\\d*\\.?\\d+(?:[eE][-+]?\\d+)?").matcher(v);
        final float[] out = {0, 0, 0, 0};
        int n = 0;
        while (m.find() && n < 4) {
            out[n++] = Float.parseFloat(m.group());
        }
        if (n == 0) {
            return null;
        }
        if (n == 1) {
            out[1] = out[0];
            out[2] = out[0];
            out[3] = out[0];
        }
        return out;
    }

    private void buildPasses(final String family, final List<GlProgram> out) {
        final List<ProgramSource> list = this.set.passes.get(family);
        if (list == null) {
            return;
        }
        for (final ProgramSource src : list) {
            final GlProgram p = this.build(src.name, src, STAGE_COMPOSITE);
            if (p != null) {
                out.add(p);
                this.built.put(src.name, p);
            }
        }
    }

    private Map<String, String> macros() {
        final Map<String, String> m = new TreeMap<>(this.pack.standardMacros);
        return m;
    }

    private GlProgram build(final String name, final ProgramSource src, final int stage) {
        if (src.geometry != null && !this.caps.geometryShaders) {
            this.warnings.add(name + ": geometry shaders are not available on this device; program skipped");
            return null;
        }
        final Map<String, String> macros = this.macros();
        final boolean compat = this.caps.translator;
        final int max = this.caps.glslVersion;
        final String vs = GlslPatcher.patch(src.vertex.text, GlslPatcher.VERTEX, macros, compat, max);
        final String fs = GlslPatcher.patch(src.remapFragData(src.fragment.text), GlslPatcher.FRAGMENT, macros,
                compat, max);
        final String gs = src.geometry == null ? null
                : GlslPatcher.patch(src.geometry.text, GlslPatcher.GEOMETRY, macros, compat, max);
        final GlProgram p;
        try {
            p = GlProgram.build(name, vs, fs, gs, src, this.caps.fragDataLocation,
                    compat ? GlslPatcher.headerLines(fs) : 0);
        } catch (final GlProgram.BuildException e) {
            this.errors.add(e.getMessage());
            return null;
        } catch (final RuntimeException e) {
            this.errors.add(name + ": " + e);
            return null;
        }
        if (src.drawBuffers != null && src.drawBuffers.length > this.caps.maxDrawBuffers) {
            this.warnings.add(name + " writes " + src.drawBuffers.length + " buffers but this device allows "
                    + this.caps.maxDrawBuffers + "; extra outputs are dropped");
        }
        this.assignUnits(p, stage);
        return p;
    }

    private GlProgram internal(final String name, final String fragment) {
        try {
            final GlProgram p = GlProgram.build(name, COPY_VSH, fragment, null, null, false, 0);
            this.assignUnits(p, STAGE_COMPOSITE);
            return p;
        } catch (final GlProgram.BuildException e) {
            this.errors.add(e.getMessage());
            return null;
        }
    }

    /** Texture units per sampler: the atlas on 0, the lightmap on 2, everything else from 3 up. */
    private void assignUnits(final GlProgram p, final int stage) {
        GL20.glUseProgram(p.id);
        // full-screen passes draw no vanilla geometry: units 0-2 are free there (reset afterwards),
        // which matters on GL translators that only have 8 units
        int next = stage == STAGE_COMPOSITE ? 0 : 3;
        // names that always resolve to the same texture share a unit (shadowtex0/1, gaux1/colortex4,
        // unknown samplers on the white texture): 8-unit translators run out otherwise
        final Map<String, Integer> byTexture = new HashMap<>();
        for (final GlProgram.Uniform u : p.samplers) {
            final String n = u.name;
            int unit;
            if (stage != STAGE_COMPOSITE && (n.equals("texture") || n.equals("tex") || n.equals("gtexture"))) {
                unit = 0;
            } else if (stage != STAGE_COMPOSITE && n.equals("lightmap")) {
                unit = 2;
            } else {
                final String key = this.textureKey(n, stage);
                final Integer shared = byTexture.get(key);
                if (shared != null) {
                    unit = shared;
                } else {
                    unit = next++;
                    byTexture.put(key, unit);
                }
            }
            if (unit >= Math.min(this.caps.maxTextureUnits, 32) - 1) {
                this.warnings.add(p.name + ": out of texture units for " + n);
                continue;
            }
            p.units.put(n, unit);
            GL20.glUniform1i(u.location, unit);
        }
    }

    private List<GlProgram> allPrograms() {
        final List<GlProgram> all = new ArrayList<>(this.built.values());
        if (this.finalProgram != null) {
            all.add(this.finalProgram);
        }
        return all;
    }

    private boolean anyUses(final String uniform) {
        for (final GlProgram p : this.allPrograms()) {
            if (p.has(uniform)) {
                return true;
            }
        }
        return false;
    }

    public int programCount() {
        return this.allPrograms().size();
    }

    /** The built program for a gbuffers name, following OptiFine's fallback chain; null = vanilla. */
    public GlProgram program(final String name) {
        if (this.resolved.containsKey(name)) {
            return this.resolved.get(name);
        }
        String n = name;
        GlProgram p = null;
        int guard = 0;
        while (n != null && guard++ < 16) {
            p = this.built.get(n);
            if (p != null) {
                break;
            }
            n = ShaderPack.FALLBACK.get(n);
        }
        this.resolved.put(name, p);
        return p;
    }

    // ================================================================== textures

    private static int solidTexture(final int r, final int g, final int b, final int a) {
        final ByteBuffer px = BufferUtils.createByteBuffer(4);
        px.put((byte) r).put((byte) g).put((byte) b).put((byte) a).flip();
        final int t = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, t);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 1, 1, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, px);
        return t;
    }

    private int loadNoise(final TextureLoader loader) {
        this.scratch();
        final String custom = this.pack.properties.noiseTexture;
        if (custom != null && loader != null) {
            final byte[] png = this.pack.source.bytes(this.resolvePath(custom));
            if (png != null) {
                final int t = loader.load(png, true, false);
                if (t != 0) {
                    this.scratch();
                    return t;
                }
            }
            this.warnings.add("texture.noise (" + custom + ") could not be loaded; using generated noise");
        }
        final int res = Math.max(16, Math.min(1024, (int) this.set.constNumber("noiseTextureResolution", 256)));
        final ByteBuffer px = BufferUtils.createByteBuffer(res * res * 4);
        final Random random = new Random(0x5EED);
        for (int i = 0; i < res * res * 4; i++) {
            px.put((byte) random.nextInt(256));
        }
        px.flip();
        final int t = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, t);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, res, res, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, px);
        return t;
    }

    private String resolvePath(final String path) {
        final String p = path.trim(); // pack sources are rooted at the shaders/ folder
        return p.startsWith("/") ? p.substring(1) : p;
    }

    private int loadCustom(final String spec, final TextureLoader loader) {
        if (loader == null || spec.startsWith("minecraft:") || spec.contains(" ")) {
            return 0; // resource-pack textures and raw .dat textures are not supported yet
        }
        final byte[] png = this.pack.source.bytes(this.resolvePath(spec));
        if (png == null) {
            return 0;
        }
        final String meta = this.pack.source.text(this.resolvePath(spec) + ".mcmeta");
        final boolean blur = meta != null && meta.replace(" ", "").contains("\"blur\":true");
        final boolean clamp = meta != null && meta.replace(" ", "").contains("\"clamp\":true");
        this.scratch();
        final int t = loader.load(png, blur, clamp);
        this.scratch();
        return t;
    }

    /** Which texture a sampler name reads in a stage. */
    private int textureFor(final String name, final int stage) {
        final Integer custom = this.customTextures.get(stageName(stage) + "." + name);
        if (custom != null) {
            return custom;
        }
        if (stage == STAGE_COMPOSITE && (name.equals("texture") || name.equals("tex") || name.equals("gtexture"))) {
            return this.colorMain(0);
        }
        final Matcher m = BUFFER_NAME.matcher(name);
        if (m.matches()) {
            return this.colorMain(bufferIndex(name));
        }
        switch (name) {
            case "depthtex0":
                return stage == STAGE_COMPOSITE ? this.targets.depth0 : this.depthCopy();
            case "depthtex1":
            case "depthtex2":
                return this.depthCopy();
            case "shadow":
            case "watershadow":
            case "shadowtex0":
            case "shadowtex1":
                return this.targets.shadowDepth0 != 0 ? this.targets.shadowDepth0 : this.whiteTex;
            case "shadowcolor":
            case "shadowcolor0":
                return this.targets.shadowColor[0] != 0 ? this.targets.shadowColor[0] : this.whiteTex;
            case "shadowcolor1":
                return this.targets.shadowColor[1] != 0 ? this.targets.shadowColor[1] : this.whiteTex;
            case "noisetex":
                return this.noiseTex;
            case "normals":
                return this.normalTex;
            case "specular":
                return this.specularTex;
            default:
                return this.whiteTex;
        }
    }

    /** Identity of the texture {@link #textureFor} binds for a sampler name (stable for the program's life). */
    private String textureKey(final String name, final int stage) {
        if (this.pack.properties.textures.containsKey(stageName(stage) + "." + name)) {
            return "custom:" + name; // declared texture.<stage>.<name> (loaded after the programs)
        }
        if (stage == STAGE_COMPOSITE && (name.equals("texture") || name.equals("tex") || name.equals("gtexture"))) {
            return "color0";
        }
        if (BUFFER_NAME.matcher(name).matches()) {
            return "color" + bufferIndex(name);
        }
        switch (name) {
            case "depthtex0":
                return stage == STAGE_COMPOSITE ? "depth0" : "depthCopy";
            case "depthtex1":
            case "depthtex2":
                return "depthCopy";
            case "shadow":
            case "watershadow":
            case "shadowtex0":
            case "shadowtex1":
                return "shadow0";
            case "shadowcolor":
            case "shadowcolor0":
                return "shadowColor0";
            case "shadowcolor1":
            case "noisetex":
            case "normals":
            case "specular":
                return name;
            default:
                return "white";
        }
    }

    private static String stageName(final int stage) {
        return stage == STAGE_COMPOSITE ? "composite" : stage == STAGE_SHADOW ? "shadow" : "gbuffers";
    }

    private int colorMain(final int index) {
        final Targets.Color c = index >= 0 && index < 16 ? this.targets.colors[index] : null;
        return c == null ? this.whiteTex : c.main();
    }

    private int depthCopy() {
        return this.depthCopyMode == -1 ? this.targets.depth0 : this.targets.depth1;
    }

    private void bindSamplers(final GlProgram p, final int stage) {
        boolean changed = false;
        for (final Map.Entry<String, Integer> e : p.units.entrySet()) {
            final int unit = e.getValue();
            if (unit < 3 && stage != STAGE_COMPOSITE) {
                continue;
            }
            final int tex = this.textureFor(e.getKey(), stage);
            if (unit < 3) {
                this.vanillaUnitsDirty = true;
                this.boundUnit[unit] = -1; // vanilla rebinds these between passes: never trust the cache
            }
            if (this.boundUnit[unit] != tex) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
                this.boundUnit[unit] = tex;
                changed = true;
            }
        }
        if (changed) {
            this.restoreActive();
        }
    }

    private void scratch() {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + this.scratchUnit);
        this.boundUnit[this.scratchUnit] = -1;
    }

    private void restoreActive() {
        GL13.glActiveTexture(vanillaActiveTexture);
    }

    private void forgetBindings() {
        for (int i = 0; i < this.boundUnit.length; i++) {
            this.boundUnit[i] = -1;
        }
        this.fboKeys.clear();
        this.fboCache.clear();
    }

    // ================================================================== frame

    /** The framebuffer gbuffers draws go to while no program (or a program without DRAWBUFFERS) is bound. */
    public int defaultFbo() {
        return this.targets.fbo(new int[] {this.colorMain(0)}, this.targets.depth0);
    }

    private int gbufferFbo(final GlProgram p) {
        if (p == null) {
            return this.defaultFbo();
        }
        final int[] db = this.drawBuffers(p);
        final int[] tex = this.targets.mains(db);
        final int[] key = this.fboKeys.get(p);
        if (key != null && java.util.Arrays.equals(key, tex)) {
            return this.fboCache.get(p);
        }
        final int fb = this.targets.fbo(tex, this.targets.depth0);
        this.fboKeys.put(p, tex);
        this.fboCache.put(p, fb);
        return fb;
    }

    private int[] drawBuffers(final GlProgram p) {
        int[] db = p.source == null || p.source.drawBuffers == null ? new int[] {0} : p.source.drawBuffers;
        if (db.length > this.caps.maxDrawBuffers) {
            db = java.util.Arrays.copyOf(db, this.caps.maxDrawBuffers);
        }
        return db;
    }

    /**
     * Frame start, before anything of the world is drawn: sizes and clears the targets, then
     * leaves the default gbuffer framebuffer bound. The runtime filled {@link #frame} first.
     */
    public void beginWorld(final int w, final int h) {
        this.width = w;
        this.height = h;
        this.scratch();
        if (w != this.targets.width || h != this.targets.height || this.targets.depth0 == 0) {
            this.targets.resize(w, h);
            this.forgetBindings();
            this.depthCopyMode = 0;
        }
        this.saveClearState();
        this.targets.clearMains(this.frame.fogColor);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.defaultFbo());
        GL11.glViewport(0, 0, w, h);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        this.restoreClearState();
        this.restoreActive();
        this.current = null;
        this.currentStage = -1;
        this.translucentDone = false;
    }

    private void saveClearState() {
        this.clearSave.clear();
        GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, this.clearSave);
        this.maskSave.clear();
        GL11.glGetBooleanv(GL11.GL_DEPTH_WRITEMASK, this.maskSave);
        GL11.glDepthMask(true);
    }

    private void restoreClearState() {
        GL11.glClearColor(this.clearSave.get(0), this.clearSave.get(1), this.clearSave.get(2), this.clearSave.get(3));
        GL11.glDepthMask(this.maskSave.get(0) != 0);
    }

    /** Binds the shadow target and program and loads the shadow projection; vanilla-style draws follow. */
    public void beginShadow() {
        this.scratch();
        this.saveClearState();
        this.targets.clearShadow(); // leaves the shadow framebuffer bound with the viewport set
        this.restoreClearState();
        this.restoreActive();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        this.matrixBuffer.clear();
        this.matrixBuffer.put(this.frame.shadowProjection).flip();
        GL11.glLoadMatrixf(this.matrixBuffer);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        this.current = null;
        this.use(this.shadowProgram, STAGE_SHADOW);
    }

    public void endShadow() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL20.glUseProgram(0);
        this.current = null;
        this.currentStage = -1;
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.defaultFbo());
        GL11.glViewport(0, 0, this.width, this.height);
    }

    /** The shadow-space model-view of a chunk at (x, y, z) relative to the camera, column-major. */
    public FloatBuffer shadowChunkMatrix(final float x, final float y, final float z) {
        this.matrixBuffer.clear();
        this.matrixBuffer.put(Mat4.mul(this.frame.shadowModelView, Mat4.translation(x, y, z))).flip();
        return this.matrixBuffer;
    }

    /** After the shadow pass: the "prepare" passes, then back to the gbuffer framebuffer. */
    public void afterShadow() {
        if (!this.prepare.isEmpty()) {
            this.runPasses(this.prepare);
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.defaultFbo());
        GL11.glViewport(0, 0, this.width, this.height);
    }

    private void use(final GlProgram p, final int stage) {
        if (p == null) {
            GL20.glUseProgram(0);
            this.current = null;
            return;
        }
        GL20.glUseProgram(p.id);
        this.frame.upload(p);
        this.bindSamplers(p, stage);
        this.current = p;
        this.currentStage = stage;
    }

    /**
     * Before a world draw: binds the program for {@code name} (gbuffers fallback chain; null or an
     * unbuilt chain = fixed function) and its framebuffer. Cheap when nothing changes.
     */
    public void beforeDraw(final String name) {
        final GlProgram p = name == null ? null : this.program(name);
        if (p == this.current && this.currentStage == STAGE_GBUFFERS && p != null) {
            if (this.frame.frameCounter != p.uploadedFrame) {
                this.frame.upload(p);
            }
            return;
        }
        if (p == null && this.current == null && this.currentStage == STAGE_GBUFFERS) {
            return;
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.gbufferFbo(p));
        if (p == null) {
            GL20.glUseProgram(0);
            this.current = null;
            this.currentStage = STAGE_GBUFFERS;
        } else {
            this.use(p, STAGE_GBUFFERS);
        }
    }

    /** Draws during the shadow pass keep the shadow program. */
    public void beforeShadowDraw() {
        if (this.current != this.shadowProgram || this.currentStage != STAGE_SHADOW) {
            this.use(this.shadowProgram, STAGE_SHADOW);
        }
    }

    /** The framebuffer that stands in for Minecraft's main target during the world pass. */
    public int worldFbo() {
        return this.gbufferFbo(this.current);
    }

    /** Vanilla bound a program of its own (post effects): forget ours so the next draw rebinds. */
    public void invalidateProgram() {
        this.current = null;
        this.currentStage = -1;
    }

    /** Once per frame before translucent terrain: depthtex1 copy and the deferred passes. */
    public void beforeTranslucent() {
        if (this.translucentDone) {
            return;
        }
        this.translucentDone = true;
        this.copyDepth();
        if (!this.deferred.isEmpty()) {
            this.runPasses(this.deferred);
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.defaultFbo());
        GL11.glViewport(0, 0, this.width, this.height);
        GL20.glUseProgram(0);
        this.current = null;
        this.currentStage = -1;
    }

    private void copyDepth() {
        if (this.depthCopyMode == -1) {
            return;
        }
        final int src = this.targets.fbo(new int[0], this.targets.depth0);
        if (!this.caps.translator) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, src);
            this.scratch();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.targets.depth1);
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, this.width, this.height);
            this.restoreActive();
            this.depthCopyMode = 1;
            return;
        }
        final int dst = this.targets.fbo(new int[0], this.targets.depth1);
        if (this.caps.blit) {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, src);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, dst);
            GL30.glBlitFramebuffer(0, 0, this.width, this.height, 0, 0, this.width, this.height,
                    GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, src);
            this.depthCopyMode = 1;
            return;
        }
        if (this.depthCopyProgram == null && this.depthCopyMode == 0) {
            this.depthCopyProgram = this.internal("aetherium_depth_copy", DEPTH_COPY_FSH);
            if (this.depthCopyProgram == null) {
                this.depthCopyMode = -1;
                this.warnings.add("depth copy unavailable: depthtex1 includes translucent blocks");
                return;
            }
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, dst);
        GL11.glViewport(0, 0, this.width, this.height);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_ALWAYS);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL20.glUseProgram(this.depthCopyProgram.id);
        final Integer unit = this.depthCopyProgram.units.get("depthtex0");
        if (unit != null) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.targets.depth0);
            this.boundUnit[unit] = unit < 3 ? -1 : this.targets.depth0;
            this.vanillaUnitsDirty |= unit < 3;
            this.restoreActive();
        }
        drawQuad();
        GL11.glPopAttrib();
        this.resetVanillaUnits();
        this.depthCopyMode = 1;
    }

    /**
     * End of the world pass (after the hand): composite passes and final into Minecraft's main
     * framebuffer, which is left bound with the viewport set.
     */
    public void endWorld(final int mainFbo) {
        if (this.usesCenterDepth) {
            this.readCenterDepth();
        }
        if (!this.composite.isEmpty()) {
            this.runPasses(this.composite);
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, mainFbo);
        GL11.glViewport(0, 0, this.width, this.height);
        final GlProgram fin = this.finalProgram != null ? this.finalProgram : this.copyProgram;
        if (fin != null) {
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            passState();
            this.use(fin, STAGE_COMPOSITE);
            drawQuad();
            GL11.glPopAttrib();
        }
        GL20.glUseProgram(0);
        this.current = null;
        this.currentStage = -1;
        this.resetVanillaUnits();
    }

    /** CI smoke diagnostics: full-screen passes with their draw buffers and active uniforms. */
    public String describePasses() {
        final StringBuilder sb = new StringBuilder();
        final List<GlProgram> all = new ArrayList<>(this.deferred);
        all.addAll(this.composite);
        if (this.finalProgram != null) {
            all.add(this.finalProgram);
        }
        sb.append("FOLDER '").append(this.set.folder).append("' programs ").append(this.set.defined.keySet()).append('\n');
        final java.util.TreeSet<String> programFiles = new java.util.TreeSet<>();
        for (final String f : this.pack.source.files()) {
            if (f.endsWith(".fsh") || f.endsWith(".vsh") || f.endsWith(".gsh")) {
                programFiles.add(f);
            }
        }
        sb.append("FILES ").append(programFiles.size()).append(' ')
                .append(String.join(" ", programFiles.size() > 120
                        ? new ArrayList<>(programFiles).subList(0, 120) : programFiles)).append('\n');
        for (final String f : programFiles) {
            if (f.startsWith(this.set.folder) && f.endsWith(".fsh") && f.indexOf('/', this.set.folder.length()) < 0) {
                final String n = f.substring(this.set.folder.length(), f.length() - 4);
                if (!this.set.defined.containsKey(n)) {
                    sb.append("SKIPPED ").append(n).append(programFiles.contains(this.set.folder + n + ".vsh")
                            ? (this.pack.enabled(n, this.set.folder) ? " (not loaded)" : " (disabled)") : " (no .vsh)")
                            .append('\n');
                }
            }
        }
        sb.append("PROGRAM-PROPS ").append(this.pack.properties.programEnabled).append('\n');
        final java.util.TreeMap<String, String> idents = new java.util.TreeMap<>();
        final Map<String, String> values = this.pack.options.snapshot();
        for (final String expr : this.pack.properties.programEnabled.values()) {
            final Matcher m = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*").matcher(expr);
            while (m.find()) {
                final String v = values.get(m.group());
                idents.put(m.group(), v == null ? "MISSING" : v);
            }
        }
        sb.append("PROGRAM-SWITCHES ").append(idents).append(" of ").append(values.size()).append(" options\n");
        int shown = 0;
        for (final Map.Entry<String, String> e : idents.entrySet()) {
            if (!"MISSING".equals(e.getValue()) || shown >= 4 || e.getKey().equals("false") || e.getKey().equals("true")) {
                continue;
            }
            shown++;
            String hit = null;
            String mention = null;
            for (final String f : this.pack.source.files()) {
                final String t = this.pack.source.text(f);
                if (t == null || t.indexOf(e.getKey()) < 0 || f.endsWith(".png")) {
                    continue;
                }
                final String[] lines = t.split("\n");
                for (int i = 0; i < lines.length && hit == null; i++) {
                    if (lines[i].indexOf(e.getKey()) >= 0) {
                        final String at = f + ":" + (i + 1) + " " + lines[i].trim();
                        if (lines[i].indexOf("define") >= 0) {
                            hit = at;
                        } else if (mention == null) {
                            mention = at;
                        }
                    }
                }
                if (hit != null) {
                    break;
                }
            }
            final String show = hit != null ? hit : mention;
            sb.append("WHERE ").append(e.getKey()).append(": ")
                    .append(show == null ? "nowhere" : show.length() > 200 ? show.substring(0, 200) : show).append('\n');
        }
        sb.append("OPTIONS ").append(values.keySet()).append('\n');
        for (final GlProgram p : all) {
            sb.append("PASS ").append(p.name).append(" buffers ").append(java.util.Arrays.toString(this.drawBuffers(p)));
            if (p.source != null && p.source.fragDataSlots != null) {
                sb.append(" (renumbered)");
            }
            sb.append(" uniforms ").append(new java.util.TreeSet<>(p.uniforms.keySet())).append('\n');
        }
        return sb.toString();
    }

    /** CI smoke diagnostics: centre pixel (rgba bytes) of every colour target, main and alt. */
    public String probe() {
        final StringBuilder sb = new StringBuilder();
        final ByteBuffer px = BufferUtils.createByteBuffer(16);
        for (int i = 0; i < this.targets.colors.length; i++) {
            final Targets.Color c = this.targets.colors[i];
            if (c == null || c.width <= 0 || c.height <= 0) {
                continue;
            }
            sb.append(" c").append(i);
            for (int pass = 0; pass < 2; pass++) {
                final int tex = pass == 0 ? c.main() : c.alt();
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.targets.fbo(new int[] {tex}, 0));
                GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
                px.clear();
                GL11.glReadPixels(c.width / 2, c.height / 2, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, px);
                sb.append(pass == 0 ? "=" : "/").append(String.format("%02x%02x%02x%02x",
                        px.get(0) & 255, px.get(1) & 255, px.get(2) & 255, px.get(3) & 255));
            }
        }
        return sb.toString();
    }

    private void readCenterDepth() {
        final FloatBuffer px = BufferUtils.createFloatBuffer(1);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.targets.fbo(new int[0], this.targets.depth0));
        GL11.glReadPixels(this.width / 2, this.height / 2, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, px);
        this.frame.centerDepth = px.get(0);
    }

    private static void passState() {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_FOG);
    }

    private void runPasses(final List<GlProgram> passes) {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        passState();
        for (final GlProgram p : passes) {
            final int[] db = this.drawBuffers(p);
            final int[] out = this.targets.alts(db);
            if (out.length == 0) {
                continue;
            }
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.targets.fbo(out, 0));
            final Targets.Color first = this.targets.colors[db[0]];
            GL11.glViewport(0, 0, first == null ? this.width : first.width, first == null ? this.height : first.height);
            this.use(p, STAGE_COMPOSITE);
            drawQuad();
            for (final int b : db) {
                final Targets.Color c = this.targets.colors[b];
                if (c != null) {
                    c.flip();
                    if (c.mipmap) {
                        this.scratch();
                        this.targets.generateMipmap(c.main());
                        this.restoreActive();
                    }
                }
            }
        }
        GL11.glPopAttrib();
        GL20.glUseProgram(0);
        this.current = null;
        this.currentStage = -1;
        this.resetVanillaUnits();
    }

    private boolean vanillaUnitsDirty;

    /** After a full-screen pass bound pack textures on units 0-2: hand them back to vanilla. */
    private void resetVanillaUnits() {
        if (!this.vanillaUnitsDirty) {
            return;
        }
        this.vanillaUnitsDirty = false;
        resetVanillaTextures();
    }

    /** Units 0-2 unbound, in GL and in Minecraft's binding cache; the active unit restored. */
    public static void resetVanillaTextures() {
        final Runnable hook = vanillaTextureReset;
        if (hook != null) {
            hook.run();
            return;
        }
        for (int u = 0; u < 3; u++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + u);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        }
        GL13.glActiveTexture(vanillaActiveTexture);
    }

    private static void drawQuad() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, 1, 0, 1, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex3f(0, 0, 0);
        GL11.glTexCoord2f(1, 0);
        GL11.glVertex3f(1, 0, 0);
        GL11.glTexCoord2f(1, 1);
        GL11.glVertex3f(1, 1, 0);
        GL11.glTexCoord2f(0, 1);
        GL11.glVertex3f(0, 1, 0);
        GL11.glEnd();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
    }

    public void delete() {
        for (final GlProgram p : this.allPrograms()) {
            p.delete();
        }
        if (this.copyProgram != null) {
            this.copyProgram.delete();
        }
        if (this.depthCopyProgram != null) {
            this.depthCopyProgram.delete();
        }
        this.targets.delete();
        for (final int t : new int[] {this.noiseTex, this.whiteTex, this.normalTex, this.specularTex}) {
            if (t != 0) {
                GL11.glDeleteTextures(t);
            }
        }
        for (final int t : this.customTextures.values()) {
            GL11.glDeleteTextures(t);
        }
        GL20.glUseProgram(0);
    }

    /** Short status for the Shaders tab and the log. */
    public String summary() {
        return this.programCount() + " programs" + (this.shadowEnabled ? ", shadows " + this.shadowResolution : "")
                + (this.errors.isEmpty() ? "" : ", " + this.errors.size() + " failed");
    }

    private static final String COPY_VSH = "#version 120\nvarying vec2 texcoord;\nvoid main() {\n"
            + "    texcoord = gl_MultiTexCoord0.xy;\n    gl_Position = ftransform();\n}\n";
    private static final String COPY_FSH = "#version 120\nuniform sampler2D colortex0;\nvarying vec2 texcoord;\n"
            + "void main() {\n    gl_FragColor = vec4(texture2D(colortex0, texcoord).rgb, 1.0);\n}\n";
    private static final String DEPTH_COPY_FSH = "#version 120\nuniform sampler2D depthtex0;\nvarying vec2 texcoord;\n"
            + "void main() {\n    gl_FragDepth = texture2D(depthtex0, texcoord).r;\n    gl_FragColor = vec4(0.0);\n}\n";
}
