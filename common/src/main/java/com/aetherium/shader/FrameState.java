package com.aetherium.shader;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything the standard OptiFine/Iris uniforms are computed from, filled once per frame by the
 * version-specific runtime. Pure Java so the derivations (smoothing, celestial math, custom
 * uniforms) stay testable. Matrices are column-major float[16].
 */
public final class FrameState implements Expr.Env {
    // ---- filled by the runtime each frame
    public int viewWidth = 1;
    public int viewHeight = 1;
    public float near = 0.05f;
    public float far = 256;
    public double camX;
    public double camY;
    public double camZ;
    public float eyeAltitude;
    public long worldTime;
    public float skyAngle; // level.getTimeOfDay(partial), 0..1
    public int moonPhase;
    public float rainStrength;
    public float thunderStrength;
    public final float[] skyColor = new float[3];
    public final float[] fogColor = new float[3];
    public int fogMode = 9729; // GL_LINEAR
    public float fogStart;
    public float fogEnd = 256;
    public float fogDensity;
    public int isEyeInWater;
    public float nightVision;
    public float blindness;
    public float screenBrightness = 1;
    public boolean hideGUI;
    public int heldItemId = -1;
    public int heldBlockLightValue;
    public int heldItemId2 = -1;
    public int heldBlockLightValue2;
    public int eyeBlockLight;
    public int eyeSkyLight = 240;
    public float playerMood;
    public boolean sneaking;
    public boolean sprinting;
    public boolean hurt;
    public boolean invisible;
    public boolean burning;
    public boolean onGround = true;
    public boolean rightHanded = true;
    public int biome;
    public int biomeCategory;
    public int biomePrecipitation;
    public float temperature = 0.8f;
    public float rainfall = 0.4f;
    public int bossBattle;
    public int atlasWidth = 1024;
    public int atlasHeight = 1024;
    public float centerDepth = 1;
    public float sunPathRotation;
    public float shadowIntervalSize = 2;
    public float shadowHalfPlane = 160;
    public final float[] modelView = Mat4.identity();
    public final float[] projection = Mat4.identity();

    // ---- derived
    public float frameTime;
    public float frameTimeCounter;
    public int frameCounter;
    public float sunAngle;
    public float shadowAngle;
    public final float[] modelViewInverse = Mat4.identity();
    public final float[] projectionInverse = Mat4.identity();
    public final float[] previousModelView = Mat4.identity();
    public final float[] previousProjection = Mat4.identity();
    public double prevCamX;
    public double prevCamY;
    public double prevCamZ;
    public final float[] sunPosition = new float[3];
    public final float[] moonPosition = new float[3];
    public final float[] shadowLightPosition = new float[3];
    public final float[] upPosition = new float[3];
    public float[] shadowModelView = Mat4.identity();
    public float[] shadowProjection = Mat4.identity();
    public float[] shadowModelViewInverse = Mat4.identity();
    public float[] shadowProjectionInverse = Mat4.identity();
    public float wetness;
    public float eyeBrightnessSmoothX;
    public float eyeBrightnessSmoothY = 240;
    public float centerDepthSmooth = 1;

    // ---- smoothing half-lives from the pack's consts (seconds / ticks)
    public float wetnessHalflife = 600;
    public float drynessHalflife = 200;
    public float eyeBrightnessHalflife = 10;
    public float centerDepthHalflife = 1;

    private long startNanos = -1;
    private long lastNanos;
    private boolean first = true;
    private final List<Custom> customs = new ArrayList<>();
    private final Map<String, double[]> customValues = new HashMap<>();

    /** A pack-defined {@code uniform.<type>.<name>} or {@code variable.<type>.<name>}. */
    public static final class Custom {
        public final boolean uniform;
        public final String type;
        public final String name;
        public final Expr expr;

        Custom(final boolean uniform, final String type, final String name, final Expr expr) {
            this.uniform = uniform;
            this.type = type;
            this.name = name;
            this.expr = expr;
        }
    }

    /** Parses the pack's custom uniforms; returns the ones that failed to parse as messages. */
    public List<String> setCustoms(final List<String[]> defs) {
        this.customs.clear();
        this.customValues.clear();
        final List<String> errors = new ArrayList<>();
        for (final String[] d : defs) {
            try {
                this.customs.add(new Custom("uniform".equals(d[0]), d[1], d[2], Expr.parse(d[3])));
            } catch (final RuntimeException e) {
                errors.add(d[0] + "." + d[1] + "." + d[2] + ": " + e.getMessage());
            }
        }
        return errors;
    }

    public List<Custom> customs() {
        return this.customs;
    }

    public double[] customValue(final String name) {
        return this.customValues.get(name);
    }

    /** Starts a frame: advances time, saves previous matrices/camera. Call before filling matrices. */
    public void beginFrame(final long nanos) {
        if (this.startNanos < 0) {
            this.startNanos = nanos;
            this.lastNanos = nanos;
        }
        this.frameTime = Math.min(1f, (nanos - this.lastNanos) / 1e9f);
        this.lastNanos = nanos;
        // OptiFine wraps at one hour to keep float precision
        this.frameTimeCounter = (float) (((nanos - this.startNanos) / 1e9) % 3600.0);
        this.frameCounter = (this.frameCounter + 1) % 720720;
        System.arraycopy(this.modelView, 0, this.previousModelView, 0, 16);
        System.arraycopy(this.projection, 0, this.previousProjection, 0, 16);
        this.prevCamX = this.camX;
        this.prevCamY = this.camY;
        this.prevCamZ = this.camZ;
    }

    /** Derives everything after the runtime filled this frame's raw values. */
    public void finishFrame() {
        if (this.first) {
            System.arraycopy(this.modelView, 0, this.previousModelView, 0, 16);
            System.arraycopy(this.projection, 0, this.previousProjection, 0, 16);
            this.prevCamX = this.camX;
            this.prevCamY = this.camY;
            this.prevCamZ = this.camZ;
            this.wetness = this.rainStrength;
            this.eyeBrightnessSmoothX = this.eyeBlockLight;
            this.eyeBrightnessSmoothY = this.eyeSkyLight;
            this.centerDepthSmooth = this.centerDepth;
            this.first = false;
        }
        copy(Mat4.invert(this.modelView), this.modelViewInverse);
        copy(Mat4.invert(this.projection), this.projectionInverse);

        // Iris CelestialUniforms
        final float sky = this.skyAngle;
        this.sunAngle = sky < 0.75f ? sky + 0.25f : sky - 0.75f;
        final boolean day = this.sunAngle <= 0.5f;
        this.shadowAngle = day ? this.sunAngle : this.sunAngle - 0.5f;
        celestial(this.modelView, this.sunPathRotation, sky, 100, this.sunPosition);
        celestial(this.modelView, this.sunPathRotation, sky, -100, this.moonPosition);
        System.arraycopy(day ? this.sunPosition : this.moonPosition, 0, this.shadowLightPosition, 0, 3);
        final float[] up = Mat4.transform(this.modelView, 0, 100, 0, 0);
        System.arraycopy(up, 0, this.upPosition, 0, 3);

        this.shadowModelView = Mat4.shadowModelView(this.shadowAngle, this.sunPathRotation, this.shadowIntervalSize,
                this.camX, this.camY, this.camZ);
        this.shadowProjection = Mat4.shadowProjection(this.shadowHalfPlane);
        this.shadowModelViewInverse = Mat4.invert(this.shadowModelView);
        this.shadowProjectionInverse = Mat4.invert(this.shadowProjection);

        final float dt = this.frameTime;
        this.wetness = smooth(this.wetness, this.rainStrength,
                this.rainStrength > this.wetness ? this.wetnessHalflife : this.drynessHalflife, dt);
        // eyeBrightnessHalflife is in ticks in OptiFine docs, but Iris treats it as seconds; use seconds/20
        this.eyeBrightnessSmoothX = smooth(this.eyeBrightnessSmoothX, this.eyeBlockLight, this.eyeBrightnessHalflife / 20f * 10f, dt);
        this.eyeBrightnessSmoothY = smooth(this.eyeBrightnessSmoothY, this.eyeSkyLight, this.eyeBrightnessHalflife / 20f * 10f, dt);
        this.centerDepthSmooth = smooth(this.centerDepthSmooth, this.centerDepth, this.centerDepthHalflife, dt);

        // custom uniforms/variables in declaration order (later ones may use earlier ones)
        for (final Custom c : this.customs) {
            double[] v;
            try {
                v = c.expr.eval(this);
            } catch (final RuntimeException e) {
                v = new double[] {0};
            }
            this.customValues.put(c.name, v);
        }
    }

    private static void copy(final float[] from, final float[] to) {
        System.arraycopy(from, 0, to, 0, 16);
    }

    /** Exponential approach with a half-life in seconds. */
    static float smooth(final float current, final float target, final float halfLife, final float dt) {
        if (halfLife <= 0) {
            return target;
        }
        final float k = (float) Math.pow(0.5, dt / halfLife);
        return target + (current - target) * k;
    }

    /** MV · Ry(-90) · Rz(sunPathRotation) · Rx(sky·360) · (0, y, 0). */
    static void celestial(final float[] mv, final float spr, final float sky, final float y, final float[] out) {
        float[] m = Mat4.mul(mv, Mat4.rotation(-90, 0, 1, 0));
        m = Mat4.mul(m, Mat4.rotation(spr, 0, 0, 1));
        m = Mat4.mul(m, Mat4.rotation(sky * 360f, 1, 0, 0));
        final float[] p = Mat4.transform(m, 0, y, 0, 0);
        out[0] = p[0];
        out[1] = p[1];
        out[2] = p[2];
    }

    /** Uploads every standard uniform the program declares, once per frame. */
    public void upload(final GlProgram p) {
        if (p.uploadedFrame == this.frameCounter) {
            return;
        }
        p.uploadedFrame = this.frameCounter;
        p.set("viewWidth", this.viewWidth);
        p.set("viewHeight", this.viewHeight);
        p.set("aspectRatio", this.viewWidth / (float) Math.max(1, this.viewHeight));
        p.set("near", this.near);
        p.set("far", this.far);
        p.set("frameTime", this.frameTime);
        p.set("frameTimeCounter", this.frameTimeCounter);
        p.set("frameCounter", this.frameCounter);
        p.set("worldTime", (float) (this.worldTime % 24000L));
        p.set("worldDay", (float) (this.worldTime / 24000L));
        p.set("moonPhase", this.moonPhase);
        p.set("sunAngle", this.sunAngle);
        p.set("shadowAngle", this.shadowAngle);
        p.set("rainStrength", this.rainStrength);
        p.set("thunderStrength", this.thunderStrength);
        p.set("wetness", this.wetness);
        p.set("skyColor", this.skyColor[0], this.skyColor[1], this.skyColor[2], 0);
        p.set("fogColor", this.fogColor[0], this.fogColor[1], this.fogColor[2], 0);
        p.set("fogMode", this.fogMode);
        p.set("fogStart", this.fogStart);
        p.set("fogEnd", this.fogEnd);
        p.set("fogDensity", this.fogDensity);
        p.set("isEyeInWater", this.isEyeInWater);
        p.set("nightVision", this.nightVision);
        p.set("blindness", this.blindness);
        p.set("darknessFactor", 0);
        p.set("darknessLightFactor", 0);
        p.set("screenBrightness", this.screenBrightness);
        p.set("hideGUI", this.hideGUI ? 1 : 0);
        p.set("heldItemId", this.heldItemId);
        p.set("heldBlockLightValue", this.heldBlockLightValue);
        p.set("heldItemId2", this.heldItemId2);
        p.set("heldBlockLightValue2", this.heldBlockLightValue2);
        p.set("eyeAltitude", this.eyeAltitude);
        p.set("eyeBrightness", this.eyeBlockLight, this.eyeSkyLight, 0, 0);
        p.set("eyeBrightnessSmooth", Math.round(this.eyeBrightnessSmoothX), Math.round(this.eyeBrightnessSmoothY), 0, 0);
        p.set("centerDepthSmooth", this.centerDepthSmooth);
        p.set("playerMood", this.playerMood);
        p.set("bossBattle", this.bossBattle);
        p.set("isRightHanded", this.rightHanded ? 1 : 0);
        p.set("atlasSize", this.atlasWidth, this.atlasHeight, 0, 0);
        p.set("terrainTextureSize", this.atlasWidth, this.atlasHeight, 0, 0);
        p.set("terrainIconSize", 16);
        p.set("biome", this.biome);
        p.set("biome_category", this.biomeCategory);
        p.set("biome_precipitation", this.biomePrecipitation);
        p.set("temperature", this.temperature);
        p.set("rainfall", this.rainfall);
        p.set("cameraPosition", (float) this.camX, (float) this.camY, (float) this.camZ, 0);
        p.set("previousCameraPosition", (float) this.prevCamX, (float) this.prevCamY, (float) this.prevCamZ, 0);
        final double fx = Math.floor(this.camX);
        final double fy = Math.floor(this.camY);
        final double fz = Math.floor(this.camZ);
        p.set("cameraPositionInt", (float) fx, (float) fy, (float) fz, 0);
        p.set("cameraPositionFract", (float) (this.camX - fx), (float) (this.camY - fy), (float) (this.camZ - fz), 0);
        p.set("previousCameraPositionInt", (float) Math.floor(this.prevCamX), (float) Math.floor(this.prevCamY),
                (float) Math.floor(this.prevCamZ), 0);
        p.set("previousCameraPositionFract", (float) (this.prevCamX - Math.floor(this.prevCamX)),
                (float) (this.prevCamY - Math.floor(this.prevCamY)), (float) (this.prevCamZ - Math.floor(this.prevCamZ)), 0);
        p.set("eyePosition", (float) this.camX, (float) this.camY, (float) this.camZ, 0);
        p.set("relativeEyePosition", 0, 0, 0, 0);
        p.set("chunkOffset", 0, 0, 0, 0);
        p.set("sunPosition", this.sunPosition[0], this.sunPosition[1], this.sunPosition[2], 0);
        p.set("moonPosition", this.moonPosition[0], this.moonPosition[1], this.moonPosition[2], 0);
        p.set("shadowLightPosition", this.shadowLightPosition[0], this.shadowLightPosition[1], this.shadowLightPosition[2], 0);
        p.set("upPosition", this.upPosition[0], this.upPosition[1], this.upPosition[2], 0);
        p.setMat4("gbufferModelView", this.modelView);
        p.setMat4("gbufferModelViewInverse", this.modelViewInverse);
        p.setMat4("gbufferPreviousModelView", this.previousModelView);
        p.setMat4("gbufferProjection", this.projection);
        p.setMat4("gbufferProjectionInverse", this.projectionInverse);
        p.setMat4("gbufferPreviousProjection", this.previousProjection);
        p.setMat4("shadowModelView", this.shadowModelView);
        p.setMat4("shadowModelViewInverse", this.shadowModelViewInverse);
        p.setMat4("shadowProjection", this.shadowProjection);
        p.setMat4("shadowProjectionInverse", this.shadowProjectionInverse);
        final Calendar cal = Calendar.getInstance();
        p.set("currentDate", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), 0);
        p.set("currentTime", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), 0);
        p.set("currentYearTime", cal.get(Calendar.DAY_OF_YEAR) * 86400 + cal.get(Calendar.HOUR_OF_DAY) * 3600
                + cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND), 0, 0, 0);
        for (final Custom c : this.customs) {
            if (!c.uniform) {
                continue;
            }
            final double[] v = this.customValues.get(c.name);
            if (v == null) {
                continue;
            }
            if (v.length >= 16 && p.has(c.name)) {
                final float[] m = new float[16];
                for (int i = 0; i < 16; i++) {
                    m[i] = (float) v[i];
                }
                p.setMat4(c.name, m);
            } else {
                p.set(c.name, (float) at(v, 0), (float) at(v, 1), (float) at(v, 2), (float) at(v, 3));
            }
        }
    }

    private static double at(final double[] v, final int i) {
        return i < v.length ? v[i] : 0;
    }

    // ---------------------------------------------------------------- Expr.Env

    @Override
    public double time() {
        return this.frameTimeCounter;
    }

    @Override
    public double[] vector(final String name) {
        final double[] custom = this.customValues.get(name);
        if (custom != null) {
            return custom;
        }
        switch (name) {
            case "sunPosition": return d(this.sunPosition);
            case "moonPosition": return d(this.moonPosition);
            case "shadowLightPosition": return d(this.shadowLightPosition);
            case "upPosition": return d(this.upPosition);
            case "skyColor": return d(this.skyColor);
            case "fogColor": return d(this.fogColor);
            case "cameraPosition": return new double[] {this.camX, this.camY, this.camZ};
            case "previousCameraPosition": return new double[] {this.prevCamX, this.prevCamY, this.prevCamZ};
            case "eyeBrightness": return new double[] {this.eyeBlockLight, this.eyeSkyLight};
            case "eyeBrightnessSmooth": return new double[] {this.eyeBrightnessSmoothX, this.eyeBrightnessSmoothY};
            case "atlasSize":
            case "terrainTextureSize": return new double[] {this.atlasWidth, this.atlasHeight};
            case "gbufferModelView": return d(this.modelView);
            case "gbufferModelViewInverse": return d(this.modelViewInverse);
            case "gbufferProjection": return d(this.projection);
            case "gbufferProjectionInverse": return d(this.projectionInverse);
            case "gbufferPreviousModelView": return d(this.previousModelView);
            case "gbufferPreviousProjection": return d(this.previousProjection);
            case "shadowModelView": return d(this.shadowModelView);
            case "shadowProjection": return d(this.shadowProjection);
            default: return null;
        }
    }

    private static double[] d(final float[] f) {
        final double[] r = new double[f.length];
        for (int i = 0; i < f.length; i++) {
            r[i] = f[i];
        }
        return r;
    }

    @Override
    public double scalar(final String name) {
        final int dot = name.indexOf('.');
        if (dot > 0) {
            final double[] v = this.vector(name.substring(0, dot));
            if (v == null) {
                return Double.NaN;
            }
            final String c = name.substring(dot + 1);
            final int i = "xr".indexOf(c.charAt(0)) >= 0 ? 0 : "yg".indexOf(c.charAt(0)) >= 0 ? 1
                    : "zb".indexOf(c.charAt(0)) >= 0 ? 2 : 3;
            return i < v.length ? v[i] : 0;
        }
        final double[] custom = this.customValues.get(name);
        if (custom != null) {
            return custom.length == 0 ? 0 : custom[0];
        }
        switch (name) {
            case "viewWidth": return this.viewWidth;
            case "viewHeight": return this.viewHeight;
            case "aspectRatio": return this.viewWidth / (double) Math.max(1, this.viewHeight);
            case "near": return this.near;
            case "far": return this.far;
            case "frameTime": return this.frameTime;
            case "frameTimeCounter": return this.frameTimeCounter;
            case "frameCounter": return this.frameCounter;
            case "worldTime": return this.worldTime % 24000L;
            case "worldDay": return this.worldTime / 24000L;
            case "moonPhase": return this.moonPhase;
            case "sunAngle": return this.sunAngle;
            case "shadowAngle": return this.shadowAngle;
            case "rainStrength": return this.rainStrength;
            case "thunderStrength": return this.thunderStrength;
            case "wetness": return this.wetness;
            case "isEyeInWater": return this.isEyeInWater;
            case "nightVision": return this.nightVision;
            case "blindness": return this.blindness;
            case "screenBrightness": return this.screenBrightness;
            case "hideGUI": return this.hideGUI ? 1 : 0;
            case "heldItemId": return this.heldItemId;
            case "heldBlockLightValue": return this.heldBlockLightValue;
            case "heldItemId2": return this.heldItemId2;
            case "heldBlockLightValue2": return this.heldBlockLightValue2;
            case "eyeAltitude": return this.eyeAltitude;
            case "centerDepthSmooth": return this.centerDepthSmooth;
            case "playerMood": return this.playerMood;
            case "bossBattle": return this.bossBattle;
            case "fogMode": return this.fogMode;
            case "fogStart": return this.fogStart;
            case "fogEnd": return this.fogEnd;
            case "fogDensity": return this.fogDensity;
            case "biome": return this.biome;
            case "biome_category": return this.biomeCategory;
            case "biome_precipitation": return this.biomePrecipitation;
            case "temperature": return this.temperature;
            case "rainfall": return this.rainfall;
            case "is_sneaking": return this.sneaking ? 1 : 0;
            case "is_sprinting": return this.sprinting ? 1 : 0;
            case "is_hurt": return this.hurt ? 1 : 0;
            case "is_invisible": return this.invisible ? 1 : 0;
            case "is_burning": return this.burning ? 1 : 0;
            case "is_on_ground": return this.onGround ? 1 : 0;
            case "is_in_water":
            case "is_eye_in_water": return this.isEyeInWater == 1 ? 1 : 0;
            case "is_eye_in_lava": return this.isEyeInWater == 2 ? 1 : 0;
            case "renderStage": return 0;
            default: return Double.NaN;
        }
    }
}
