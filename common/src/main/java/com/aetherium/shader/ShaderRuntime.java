package com.aetherium.shader;

// @era:shader-begin v16
import com.aetherium.client.FogTracker;
import com.aetherium.client.VertexBufferAccess;
import com.aetherium.util.AetheriumLog;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.client.Camera;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
// @era:shader-else none
// @era:shader-end

/**
 * Drives {@link ShaderPipeline} from Minecraft 1.16.5's renderer (the Shader*Mixin hooks).
 * Render thread only, except {@link ShaderVertex}. Every hook is a field read while no pack runs.
 *
 * <p>Frame: LevelRenderer.renderLevel HEAD fills the uniforms, clears the targets, draws the
 * shadow map and binds the gbuffer framebuffer; from then on binds of Minecraft's main framebuffer
 * are redirected to it, and every draw binds the program for the current render type or phase.
 * The deferred passes run before translucent terrain, and GameRenderer.renderLevel RETURN (after
 * the hand) runs composite and final into the main framebuffer.</p>
 */
public final class ShaderRuntime {
    // @era:shader-begin v16
    private static final AetheriumLog LOG = AetheriumLog.of(ShaderRuntime.class);

    static final int PHASE_NONE = 0;
    public static final int PHASE_SKY = 1;
    public static final int PHASE_CLOUDS = 2;
    public static final int PHASE_WEATHER = 3;
    public static final int PHASE_PARTICLES = 4;
    public static final int PHASE_BORDER = 5;
    public static final int PHASE_SHADOW = 6;

    private static ShaderPipeline pipe;
    private static String loadedFolder = "";
    private static boolean inWorld;
    private static int phase;
    private static boolean hand;
    private static String typeProgram;
    private static String typeHandProgram;
    private static boolean externalProgram;
    private static int mainFbo = -1;
    private static int activeUnit = GL13.GL_TEXTURE0;
    private static boolean texture0 = true;
    private static int boundTex0;
    private static int sunTex = -1;
    private static int moonTex = -1;
    private static final Map<String, String[]> TYPE_PROGRAMS = new HashMap<>();
    private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer FOG = BufferUtils.createFloatBuffer(4);
    private static final ResourceLocation SUN = new ResourceLocation("textures/environment/sun.png");
    private static final ResourceLocation MOON = new ResourceLocation("textures/environment/moon_phases.png");

    private ShaderRuntime() {
    }

    public static boolean running() {
        return pipe != null;
    }

    /** World frames finished since start (with or without a pack); read by the CI smoke test. */
    public static volatile int frames;

    // ================================================================== loading

    private static String worldFolder(final ClientLevel level) {
        if (level == null) {
            return "world0";
        }
        if (level.dimension() == Level.NETHER) {
            return "world-1";
        }
        if (level.dimension() == Level.END) {
            return "world1";
        }
        return "world0";
    }

    private static void reload(final Minecraft mc) {
        final boolean wasRunning = pipe != null || ShaderVertex.extended();
        if (pipe != null) {
            try {
                pipe.delete();
            } catch (final Throwable ignored) {
                // context trouble: the textures go with it
            }
        }
        pipe = null;
        inWorld = false;
        TYPE_PROGRAMS.clear();
        ShaderBridge.setGameDir(mc.gameDirectory.toPath());
        final String name = ShaderBridge.selected();
        if (!ShaderBridge.supported() || name.isEmpty()) {
            ShaderVertex.configure(false, false, false, null);
            ShaderBridge.off();
            if (wasRunning && mc.level != null) {
                mc.levelRenderer.allChanged();
            }
            return;
        }
        PackSource source = null;
        try {
            final GlCaps caps = GlCaps.read(ShaderBridge.forceCompat(), ShaderBridge.disableCompat());
            final Map<String, String> macros = new TreeMap<>(caps.standardMacros());
            macros.put("MC_VERSION", "11605");
            macros.put("MC_RENDER_QUALITY", "1.0");
            macros.put("MC_SHADOW_QUALITY", "1.0");
            macros.put("MC_HAND_DEPTH", "0.125");
            macros.put("AETHERIUM", "1");
            source = ShaderBridge.LITE.equals(name) ? liteSource()
                    : PackSource.open(ShaderBridge.shaderpacksDir().resolve(name));
            if (source == null) {
                throw new IllegalStateException("no shaders folder in " + name);
            }
            final Path optionsFile = ShaderBridge.optionsFile(name);
            final ShaderPack pack = ShaderPack.load(name, source, macros, optionsFile, null);
            if (ShaderBridge.applyPending(pack) && optionsFile != null) {
                Files.createDirectories(optionsFile.getParent());
                pack.options.save(optionsFile);
            }
            final String folder = worldFolder(mc.level);
            final ShaderPipeline pl = new ShaderPipeline(pack, folder, caps, ShaderRuntime::loadTexture,
                    caps.translator);
            writeLog(mc, pack, pl, caps);
            if (pl.program("gbuffers_terrain") == null && pl.program("gbuffers_textured") == null
                    && pl.programCount() == 0) {
                pl.delete();
                ShaderBridge.failed(name + ": no program could be built (see logs/aetherium-shaders.log)", pl.errors);
                ShaderVertex.configure(false, false, false, null);
                return;
            }
            boolean mid = false;
            boolean tangent = false;
            for (final ProgramSource p : pl.set.defined.values()) {
                mid |= p.usesMidTexCoord;
                tangent |= p.usesTangent;
            }
            ShaderVertex.configure(pl.set.needsExtendedVertex(), mid, tangent, pack.blocks);
            pipe = pl;
            loadedFolder = folder;
            sunTex = textureId(mc, SUN);
            moonTex = textureId(mc, MOON);
            ShaderBridge.loaded(pack, pl, caps.describe());
            LOG.info("Shader pack " + name + " loaded: " + pl.summary() + " on " + caps.describe());
            if (mc.options.graphicsMode == GraphicsStatus.FABULOUS) {
                mc.options.graphicsMode = GraphicsStatus.FANCY; // its transparency targets bypass the pack
            }
            if (mc.level != null) {
                mc.levelRenderer.allChanged(); // extended vertex format, shading, pipeline switch
            }
        } catch (final Throwable t) {
            LOG.warn("Shader pack " + name + " failed to load", t);
            pipe = null;
            ShaderVertex.configure(false, false, false, null);
            ShaderBridge.failed("Could not load " + name + ": " + t.getMessage(), null);
            if (wasRunning && mc.level != null) {
                mc.levelRenderer.allChanged();
            }
        } finally {
            if (source != null) {
                try {
                    source.close();
                } catch (final Exception ignored) {
                    // read fully already
                }
            }
        }
    }

    /** CI smoke test: builds a pack without installing it ({@code compat}: forced, else disabled). */
    static ShaderPipeline buildForTest(final Minecraft mc, final String name, final boolean compat) throws Exception {
        final GlCaps caps = GlCaps.read(compat, !compat);
        final Map<String, String> macros = new TreeMap<>(caps.standardMacros());
        macros.put("MC_VERSION", "11605");
        macros.put("MC_RENDER_QUALITY", "1.0");
        macros.put("MC_SHADOW_QUALITY", "1.0");
        macros.put("MC_HAND_DEPTH", "0.125");
        macros.put("AETHERIUM", "1");
        final PackSource source = ShaderBridge.LITE.equals(name) ? liteSource()
                : PackSource.open(ShaderBridge.shaderpacksDir().resolve(name));
        if (source == null) {
            throw new IllegalStateException("no shaders folder in " + name);
        }
        try {
            final ShaderPack pack = ShaderPack.load(name, source, macros, null, null);
            return new ShaderPipeline(pack, "world0", caps, ShaderRuntime::loadTexture, caps.translator);
        } finally {
            source.close();
        }
    }

    private static int textureId(final Minecraft mc, final ResourceLocation loc) {
        try {
            mc.getTextureManager().bind(loc);
            return mc.getTextureManager().getTexture(loc).getId();
        } catch (final Throwable t) {
            return -1;
        }
    }

    /** The bundled pack, read from the jar through its file index. */
    private static PackSource liteSource() throws Exception {
        final String root = "/assets/aetherium/shaderpacks/lite/";
        final Map<String, byte[]> files = new HashMap<>();
        final byte[] index = resource(root + "files.txt");
        if (index == null) {
            return null;
        }
        for (final String line : new String(index, StandardCharsets.UTF_8).split("\n")) {
            final String f = line.trim();
            if (f.isEmpty()) {
                continue;
            }
            final byte[] data = resource(root + "shaders/" + f);
            if (data != null) {
                files.put(f, data);
            }
        }
        return new PackSource.Memory(files);
    }

    private static byte[] resource(final String path) throws Exception {
        try (InputStream in = ShaderRuntime.class.getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            final byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }

    private static int loadTexture(final byte[] png, final boolean blur, final boolean clamp) {
        try (NativeImage img = NativeImage.read(new ByteArrayInputStream(png))) {
            final int w = img.getWidth();
            final int h = img.getHeight();
            final ByteBuffer px = BufferUtils.createByteBuffer(w * h * 4);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    final int abgr = img.getPixelRGBA(x, y);
                    px.put((byte) abgr).put((byte) (abgr >> 8)).put((byte) (abgr >> 16)).put((byte) (abgr >>> 24));
                }
            }
            px.flip();
            final int t = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, t);
            final int filter = blur ? GL11.GL_LINEAR : GL11.GL_NEAREST;
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
            final int wrap = clamp ? GL12.GL_CLAMP_TO_EDGE : GL11.GL_REPEAT;
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrap);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, wrap);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, px);
            return t;
        } catch (final Throwable t) {
            return 0;
        }
    }

    private static void writeLog(final Minecraft mc, final ShaderPack pack, final ShaderPipeline pl, final GlCaps caps) {
        try {
            final Path log = mc.gameDirectory.toPath().resolve("logs").resolve("aetherium-shaders.log");
            Files.createDirectories(log.getParent());
            final List<String> lines = new ArrayList<>();
            lines.add("Shader pack: " + pack.name);
            lines.add("Device: " + caps.describe());
            lines.add("Result: " + pl.summary());
            lines.add("");
            for (final String e : pl.errors) {
                lines.add("ERROR " + e);
            }
            for (final String w : pl.warnings) {
                lines.add("WARN  " + w);
            }
            Files.write(log, lines, StandardCharsets.UTF_8);
            for (final String e : pl.errors) {
                LOG.warn("Shader program failed: " + e);
            }
        } catch (final Throwable ignored) {
            // the status line still shows the count
        }
    }

    private static void fail(final Throwable t) {
        LOG.warn("Shader pack stopped after an error while drawing", t);
        final ShaderPipeline p = pipe;
        pipe = null;
        inWorld = false;
        phase = PHASE_NONE;
        if (p != null) {
            try {
                p.delete();
            } catch (final Throwable ignored) {
                // nothing more to do
            }
        }
        ShaderVertex.configure(false, false, false, null);
        ShaderBridge.broken(String.valueOf(t));
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            mc.levelRenderer.allChanged();
        }
    }

    // ================================================================== frame

    /** LevelRenderer.renderLevel HEAD. */
    public static void frameStart(final ViewArea viewArea, final PoseStack pose, final float partial,
                                  final Camera camera, final Matrix4f projection) {
        final Minecraft mc = Minecraft.getInstance();
        if (ShaderBridge.takeReload() || (pipe != null && !worldFolder(mc.level).equals(loadedFolder))) {
            reload(mc);
        }
        final ShaderPipeline p = pipe;
        inWorld = false;
        phase = PHASE_NONE;
        hand = false;
        if (p == null) {
            return;
        }
        try {
            final RenderTarget main = mc.getMainRenderTarget();
            mainFbo = main.frameBufferId;
            fill(p.frame, mc, pose, partial, camera, projection, main);
            p.beginWorld(main.width, main.height);
            inWorld = true;
            if (p.shadowEnabled && viewArea != null) {
                shadowPass(p, viewArea, camera);
            }
            p.afterShadow();
        } catch (final Throwable t) {
            fail(t);
        }
    }

    private static void read(final FloatBuffer from, final float[] to) {
        for (int i = 0; i < 16; i++) {
            to[i] = from.get(i);
        }
    }

    private static void fill(final FrameState f, final Minecraft mc, final PoseStack pose, final float partial,
                             final Camera camera, final Matrix4f projection, final RenderTarget main) {
        f.beginFrame(System.nanoTime());
        f.viewWidth = main.width;
        f.viewHeight = main.height;
        MATRIX.clear();
        pose.last().pose().store(MATRIX);
        read(MATRIX, f.modelView);
        MATRIX.clear();
        projection.store(MATRIX);
        read(MATRIX, f.projection);
        final Vec3 cam = camera.getPosition();
        f.camX = cam.x;
        f.camY = cam.y;
        f.camZ = cam.z;
        f.eyeAltitude = (float) cam.y;
        f.near = 0.05f;
        f.far = mc.gameRenderer.getRenderDistance();
        final ClientLevel level = mc.level;
        if (level != null) {
            final long day = level.getDayTime();
            f.worldTime = day;
            f.skyAngle = level.dimensionType().timeOfDay(day);
            f.moonPhase = level.dimensionType().moonPhase(day);
            f.rainStrength = level.getRainLevel(partial);
            f.thunderStrength = level.getThunderLevel(partial);
            final BlockPos at = camera.getBlockPosition();
            final Vec3 sky = level.getSkyColor(at, partial);
            f.skyColor[0] = (float) sky.x;
            f.skyColor[1] = (float) sky.y;
            f.skyColor[2] = (float) sky.z;
            f.eyeBlockLight = level.getBrightness(LightLayer.BLOCK, at) * 16;
            f.eyeSkyLight = level.getBrightness(LightLayer.SKY, at) * 16;
        }
        FOG.clear();
        FogTracker.copyColor(FOG);
        f.fogColor[0] = FOG.get(0);
        f.fogColor[1] = FOG.get(1);
        f.fogColor[2] = FOG.get(2);
        f.fogMode = FogTracker.mode();
        f.fogStart = FogTracker.start();
        f.fogEnd = FogTracker.end();
        f.fogDensity = FogTracker.density();
        final FluidState fluid = camera.getFluidInCamera();
        f.isEyeInWater = fluid.isEmpty() ? 0 : fluid.is(FluidTags.WATER) ? 1 : fluid.is(FluidTags.LAVA) ? 2 : 0;
        f.screenBrightness = (float) mc.options.gamma;
        f.hideGUI = mc.options.hideGui;
        final LocalPlayer player = mc.player;
        if (player != null) {
            f.nightVision = player.hasEffect(MobEffects.NIGHT_VISION) ? GameRenderer.getNightVisionScale(player, partial)
                    : 0;
            f.blindness = player.hasEffect(MobEffects.BLINDNESS) ? 1 : 0;
            f.sneaking = player.isCrouching();
            f.sprinting = player.isSprinting();
            f.hurt = player.hurtTime > 0;
            f.invisible = player.isInvisible();
            f.burning = player.isOnFire();
            f.onGround = player.isOnGround();
            final ShaderPack pack = pipe.pack;
            held(f, pack, player.getMainHandItem(), true);
            held(f, pack, player.getOffhandItem(), false);
        }
        f.finishFrame();
    }

    private static void held(final FrameState f, final ShaderPack pack, final ItemStack stack, final boolean main) {
        int id = -1;
        int light = 0;
        if (!stack.isEmpty()) {
            final String name = Registry.ITEM.getKey(stack.getItem()).toString();
            id = pack.items == null ? -1 : pack.items.lookup(name, Collections.<String, String>emptyMap(), -1);
            if (stack.getItem() instanceof BlockItem) {
                light = ((BlockItem) stack.getItem()).getBlock().defaultBlockState().getLightEmission();
            }
        }
        if (main) {
            f.heldItemId = id;
            f.heldBlockLightValue = light;
        } else {
            f.heldItemId2 = id;
            f.heldBlockLightValue2 = light;
        }
    }

    private static void shadowPass(final ShaderPipeline p, final ViewArea viewArea, final Camera camera) {
        phase = PHASE_SHADOW;
        p.beginShadow();
        RenderSystem.disableCull();
        final Vec3 cam = camera.getPosition();
        final float reach = p.shadowRenderDistance + 16;
        shadowLayer(p, viewArea, RenderType.solid(), cam, reach);
        shadowLayer(p, viewArea, RenderType.cutoutMipped(), cam, reach);
        shadowLayer(p, viewArea, RenderType.cutout(), cam, reach);
        if (p.shadowTranslucent) {
            shadowLayer(p, viewArea, RenderType.translucent(), cam, reach);
        }
        RenderSystem.enableCull();
        p.endShadow();
        phase = PHASE_NONE;
    }

    private static void shadowLayer(final ShaderPipeline p, final ViewArea viewArea, final RenderType type,
                                    final Vec3 cam, final float reach) {
        final ChunkRenderDispatcher.RenderChunk[] chunks = viewArea.chunks;
        if (chunks == null) {
            return;
        }
        type.setupRenderState();
        final VertexFormat format = ShaderVertex.chunkFormat();
        boolean any = false;
        for (final ChunkRenderDispatcher.RenderChunk chunk : chunks) {
            if (chunk == null || chunk.getCompiledChunk().isEmpty(type)) {
                continue;
            }
            final BlockPos origin = chunk.getOrigin();
            final float dx = (float) (origin.getX() - cam.x);
            final float dy = (float) (origin.getY() - cam.y);
            final float dz = (float) (origin.getZ() - cam.z);
            if (Math.abs(dx + 8) > reach || Math.abs(dz + 8) > reach || Math.abs(dy + 8) > reach + 64) {
                continue;
            }
            final VertexBuffer buffer = chunk.getBuffer(type);
            final int count = ((VertexBufferAccess) buffer).aetheriumVertexCount();
            if (count <= 0) {
                continue;
            }
            GL11.glLoadMatrixf(p.shadowChunkMatrix(dx, dy, dz));
            buffer.bind();
            format.setupBufferState(0L);
            RenderSystem.drawArrays(GL11.GL_QUADS, 0, count);
            any = true;
        }
        if (any) {
            VertexBuffer.unbind();
            format.clearBufferState();
        }
        type.clearRenderState();
    }

    /** GameRenderer.renderLevel RETURN: composite and final into the main framebuffer. */
    public static void frameEnd() {
        frames++;
        if (!inWorld) {
            return;
        }
        inWorld = false;
        phase = PHASE_NONE;
        hand = false;
        final ShaderPipeline p = pipe;
        if (p == null) {
            return;
        }
        try {
            p.endWorld(mainFbo);
        } catch (final Throwable t) {
            fail(t);
        }
    }

    // ================================================================== draw hooks

    /** GlStateManager._drawArrays HEAD; true = skip this draw. */
    public static boolean beforeDraw() {
        final ShaderPipeline p = pipe;
        if (!inWorld || p == null || externalProgram) {
            return false;
        }
        try {
            if (phase == PHASE_SHADOW) {
                p.beforeShadowDraw();
                return false;
            }
            if (phase == PHASE_SKY && texture0 && boundTex0 > 0
                    && ((!p.sun && boundTex0 == sunTex) || (!p.moon && boundTex0 == moonTex))) {
                return true;
            }
            p.beforeDraw(programName());
        } catch (final Throwable t) {
            fail(t);
        }
        return false;
    }

    private static String programName() {
        if (typeProgram != null) {
            return hand ? typeHandProgram : typeProgram;
        }
        switch (phase) {
            case PHASE_SKY:
                return texture0 ? "gbuffers_skytextured" : "gbuffers_skybasic";
            case PHASE_CLOUDS:
                return "gbuffers_clouds";
            case PHASE_WEATHER:
                return "gbuffers_weather";
            case PHASE_PARTICLES:
            case PHASE_BORDER:
                return "gbuffers_textured_lit";
            default:
                if (hand) {
                    return "gbuffers_hand";
                }
                return texture0 ? "gbuffers_textured_lit" : "gbuffers_basic";
        }
    }

    /** RenderStateShard.setupRenderState of a RenderType. */
    public static void typeSetup(final String name) {
        if (pipe == null) {
            return;
        }
        String[] programs = TYPE_PROGRAMS.get(name);
        if (programs == null) {
            programs = programsFor(name);
            TYPE_PROGRAMS.put(name, programs);
        }
        typeProgram = programs[0];
        typeHandProgram = programs[1];
    }

    public static void typeClear() {
        typeProgram = null;
        typeHandProgram = null;
    }

    /** {gbuffers program, program inside the hand pass}; nulls = fixed function. */
    static String[] programsFor(final String type) {
        switch (type) {
            case "solid":
            case "cutout_mipped":
            case "cutout":
                return new String[] {"gbuffers_terrain", "gbuffers_hand"};
            case "translucent":
            case "translucent_moving_block":
            case "translucent_no_crumbling":
            case "tripwire":
                return new String[] {"gbuffers_water", "gbuffers_hand_water"};
            case "crumbling":
                return new String[] {"gbuffers_damagedblock", "gbuffers_damagedblock"};
            case "eyes":
                return new String[] {"gbuffers_spidereyes", "gbuffers_spidereyes"};
            case "beacon_beam":
                return new String[] {"gbuffers_beaconbeam", "gbuffers_beaconbeam"};
            case "lines":
                return new String[] {"gbuffers_line", "gbuffers_line"};
            case "lightning":
            case "leash":
                return new String[] {"gbuffers_basic", "gbuffers_basic"};
            case "entity_shadow":
                return new String[] {"gbuffers_textured_lit", "gbuffers_textured_lit"};
            case "end_portal":
            case "end_gateway":
            case "outline":
            case "water_mask":
                return new String[] {null, null};
            default:
                break;
        }
        if (type.contains("glint")) {
            return new String[] {"gbuffers_armor_glint", "gbuffers_armor_glint"};
        }
        if (type.startsWith("entity") || type.startsWith("armor") || type.startsWith("item_entity")
                || type.startsWith("text") || type.startsWith("energy_swirl")) {
            final boolean translucent = type.contains("translucent");
            return new String[] {"gbuffers_entities", translucent ? "gbuffers_hand_water" : "gbuffers_hand"};
        }
        return new String[] {"gbuffers_textured_lit", "gbuffers_hand"};
    }

    public static void setPhase(final int newPhase) {
        if (pipe != null && inWorld) {
            phase = newPhase;
        }
    }

    public static void endPhase(final int oldPhase) {
        if (phase == oldPhase) {
            phase = PHASE_NONE;
        }
    }

    public static boolean cloudsOff() {
        final ShaderPipeline p = pipe;
        return p != null && "off".equalsIgnoreCase(p.clouds.trim());
    }

    public static void handPhase(final boolean on) {
        hand = on && pipe != null && inWorld;
    }

    /** GameRenderer.renderLevel: skip the depth clear before the hand (the hand is depth-scaled instead). */
    public static boolean skipHandDepthClear(final int mask) {
        return mask == GL11.GL_DEPTH_BUFFER_BIT && pipe != null && inWorld;
    }

    /** The hand's projection with depth scaled by MC_HAND_DEPTH so it stays in front of the world. */
    public static Matrix4f handProjection(final Matrix4f projection) {
        if (pipe == null || !inWorld) {
            return projection;
        }
        final Matrix4f scaled = Matrix4f.createScaleMatrix(1, 1, 0.125f);
        scaled.multiply(projection);
        return scaled;
    }

    /** LevelRenderer.renderChunkLayer HEAD. */
    public static void chunkLayer(final RenderType type) {
        final ShaderPipeline p = pipe;
        if (p != null && inWorld && phase == PHASE_NONE && type == RenderType.translucent()) {
            try {
                p.beforeTranslucent();
            } catch (final Throwable t) {
                fail(t);
            }
        }
    }

    /** GlStateManager._glBindFramebuffer: the main framebuffer becomes the gbuffers during the world pass. */
    public static int redirectFramebuffer(final int framebuffer) {
        final ShaderPipeline p = pipe;
        if (inWorld && p != null && framebuffer == mainFbo && framebuffer > 0) {
            return p.worldFbo();
        }
        return framebuffer;
    }

    public static void useProgram(final int program) {
        final boolean external = program != 0;
        if (external != externalProgram) {
            externalProgram = external;
            final ShaderPipeline p = pipe;
            if (p != null) {
                p.invalidateProgram();
            }
        }
    }

    public static void activeTexture(final int unit) {
        activeUnit = unit;
        ShaderPipeline.vanillaActiveTexture = unit;
    }

    public static void bindTexture(final int id) {
        if (activeUnit == GL13.GL_TEXTURE0) {
            boundTex0 = id;
        }
    }

    public static void textureEnabled(final boolean on) {
        if (activeUnit == GL13.GL_TEXTURE0) {
            texture0 = on;
        }
    }

    /** ClientLevel.getShade: OptiFine's oldLighting=false (the default with shaders) drops face shading. */
    public static boolean flatShading() {
        final ShaderPipeline p = pipe;
        return p != null && !p.oldLighting;
    }
    // @era:shader-else none
    //~ private ShaderRuntime() {
    //~ }
    // @era:shader-end
}
