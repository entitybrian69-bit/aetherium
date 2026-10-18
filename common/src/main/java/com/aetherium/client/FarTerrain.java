package com.aetherium.client;

// @era:t-far-terrain-begin none

// @era:t-far-terrain-else v16
//~ import com.aetherium.mixin.core.TuningLevelAccessor;
//~ import com.aetherium.mixin.core.TuningViewAreaAccessor;
//~ import com.aetherium.perf.FarTerrainMesh;
//~ import com.aetherium.perf.Tuning;
//~ import com.aetherium.qol.Scenery;
//~ import com.aetherium.util.AetheriumLog;
//~ import com.mojang.blaze3d.systems.RenderSystem;
//~ import com.mojang.blaze3d.vertex.BufferBuilder;
//~ import com.mojang.blaze3d.vertex.DefaultVertexFormat;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import com.mojang.blaze3d.vertex.VertexBuffer;
//~ import com.mojang.blaze3d.vertex.VertexConsumer;
//~ import com.mojang.blaze3d.vertex.VertexFormat;
//~ import com.mojang.math.Matrix4f;
//~ import java.util.ArrayList;
//~ import java.util.HashSet;
//~ import java.util.Iterator;
//~ import java.util.List;
//~ import java.util.Map;
//~ import java.util.Set;
//~ import java.util.concurrent.ConcurrentHashMap;
//~ import java.util.concurrent.ExecutorService;
//~ import java.util.concurrent.Executors;
//~ import net.minecraft.client.Camera;
//~ import net.minecraft.client.Minecraft;
//~ import net.minecraft.client.multiplayer.ClientLevel;
//~ import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
//~ import net.minecraft.core.BlockPos;
//~ import net.minecraft.world.effect.MobEffects;
//~ import net.minecraft.world.entity.Entity;
//~ import net.minecraft.world.entity.LivingEntity;
//~ import net.minecraft.world.level.BlockGetter;
//~ import net.minecraft.world.level.ChunkPos;
//~ import net.minecraft.world.level.Level;
//~ import net.minecraft.world.level.block.state.BlockState;
//~ import net.minecraft.world.level.chunk.LevelChunk;
//~ import net.minecraft.world.level.levelgen.Heightmap;
//~ import net.minecraft.world.level.material.MaterialColor;
//~ import net.minecraft.world.phys.Vec3;
//~ import org.lwjgl.opengl.GL11;
// @era:t-far-terrain-end

/**
 * Far terrain (1.11.0, Minecraft 1.16.5, Overworld): a low-detail outline of the terrain beyond the
 * render distance, plus columns for chunks inside it that are not built yet.
 *
 * <ul>
 *   <li>Capture: every chunk the server sends is sampled into a 4x4 grid (height of the top
 *       motion-blocking block and its map colour) and kept after the chunk unloads, up to
 *       {@code MAX_CHUNKS} chunks; leaving the dimension clears it.</li>
 *   <li>Mesh: {@link FarTerrainMesh} on a background thread into position-colour buffers of at
 *       most 32000 vertices (gl4es splits bigger quad draws), uploaded on the render thread. Rebuilt
 *       when the camera enters another chunk, at most once a second while new chunks arrive or
 *       unbuilt chunks are shown, when the distances change and as the daylight changes.</li>
 *   <li>Draw: right after the sky, before the terrain, with its own fog; the terrain fog then moves
 *       out to the far edge ({@link #extendFog}). Off under water, in lava, when blinded and with a
 *       shader mod.</li>
 * </ul>
 *
 * Any error turns the feature off for the session.
 */
public final class FarTerrain {
    private FarTerrain() {
    }

    // @era:t-far-terrain-begin none
    public static void capture(final net.minecraft.world.level.chunk.LevelChunk chunk) {
    }

    public static boolean drawnThisFrame() {
        return false;
    }

    public static boolean extendFog() {
        return false;
    }

    public static void levelChanged() {
    }
    // @era:t-far-terrain-else v16
    //~ private static final AetheriumLog LOGGER = AetheriumLog.of(FarTerrain.class);
    //~ private static final int MAX_CHUNKS = 16384;
    //~ /** 32000 vertices: gl4es splits GL_QUADS draws above that. */
    //~ private static final int QUADS_PER_BUFFER = 8000;
    //~ private static final long REBUILD_NANOS = 1_000_000_000L;
    //~ private static final VertexFormat FORMAT = DefaultVertexFormat.POSITION_COLOR;
    //~ private static final int GL_QUADS = 7;
    //~ private static final int GL_DEPTH_TEST = 0x0B71;
    //~ private static final int GL_CULL_FACE = 0x0B44;
    //~ private static final int GL_FOG = 0x0B60;
    //~ private static final boolean SHADER_MOD = present("net.coderbot.iris.Iris") || present("net.optifine.Config");

    //~ private static final Map<Long, FarTerrainMesh.Column> COLUMNS = new ConcurrentHashMap<Long, FarTerrainMesh.Column>();
    //~ /** Filled by the worker while {@link #building}, read by the render thread once {@link #ready}. */
    //~ private static final List<BufferBuilder> BUILDERS = new ArrayList<BufferBuilder>();
    //~ private static final List<VertexBuffer> BUFFERS = new ArrayList<VertexBuffer>();
    //~ private static ExecutorService worker;

    //~ private static volatile boolean building;
    //~ private static volatile boolean ready;
    //~ private static volatile int readyBuffers;
    //~ private static volatile Throwable workerError;
    //~ private static volatile boolean broken;
    //~ private static volatile boolean dirty;

    //~ private static int bufferCount;
    //~ private static int meshX;
    //~ private static int meshZ;
    //~ private static int pendingX;
    //~ private static int pendingZ;
    //~ private static int builtX = Integer.MIN_VALUE;
    //~ private static int builtZ;
    //~ private static int builtRd = -1;
    //~ private static int builtFar = -1;
    //~ private static int builtLight = -1;
    //~ private static boolean builtInside;
    //~ private static long lastBuild;
    //~ private static Object seededLevel;
    //~ private static boolean drawing;
    //~ private static int drawnFarBlocks;

    //~ private static boolean present(final String name) {
        //~ try {
            //~ Class.forName(name, false, FarTerrain.class.getClassLoader());
            //~ return true;
        //~ } catch (final ClassNotFoundException | LinkageError e) {
            //~ return false;
        //~ }
    //~ }

    //~ /** Far distance in chunks for this frame, 0 when nothing would be drawn. */
    //~ private static int effectiveFar(final Minecraft mc) {
        //~ final int far = Tuning.farTerrain;
        //~ if (far <= 0 || broken || SHADER_MOD) {
            //~ return 0;
        //~ }
        //~ final int rd = mc.options.renderDistance;
        //~ // The projection's far plane is 4 x the render distance.
        //~ final int limited = Math.min(far, rd * 4 - 1);
        //~ return limited > rd ? limited : 0;
    //~ }

    //~ /** ClientChunkCache.replaceWithPacketData RETURN: sample the chunk the server just sent. */
    //~ public static void capture(final LevelChunk chunk) {
        //~ if (Tuning.farTerrain <= 0 || chunk == null || broken || SHADER_MOD) {
            //~ return;
        //~ }
        //~ final ClientLevel level = Minecraft.getInstance().level;
        //~ if (level == null || level.dimension() != Level.OVERWORLD) {
            //~ return;
        //~ }
        //~ try {
            //~ store(chunk);
        //~ } catch (final RuntimeException | LinkageError e) {
            //~ fail(e);
        //~ }
    //~ }

    //~ private static void store(final LevelChunk chunk) {
        //~ final ChunkPos pos = chunk.getPos();
        //~ final FarTerrainMesh.Column column = new FarTerrainMesh.Column();
        //~ final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        //~ final BlockGetter getter = (BlockGetter) (Object) chunk;
        //~ final int baseX = pos.x << 4;
        //~ final int baseZ = pos.z << 4;
        //~ for (int gz = 0; gz < FarTerrainMesh.GRID; gz++) {
            //~ for (int gx = 0; gx < FarTerrainMesh.GRID; gx++) {
                //~ final int lx = gx * FarTerrainMesh.CELL + FarTerrainMesh.CELL / 2;
                //~ final int lz = gz * FarTerrainMesh.CELL + FarTerrainMesh.CELL / 2;
                //~ final int height = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, lx, lz);
                //~ int rgb = 0x707070;
                //~ if (height > 0) {
                    //~ at.set(baseX + lx, height - 1, baseZ + lz);
                    //~ final BlockState state = chunk.getBlockState(at);
                    //~ final MaterialColor color = state.getMapColor(getter, at);
                    //~ if (color != null && color.col != 0) {
                        //~ rgb = color.col;
                    //~ }
                //~ }
                //~ final int i = gz * FarTerrainMesh.GRID + gx;
                //~ column.heights[i] = (short) Math.max(0, height);
                //~ column.colors[i] = rgb;
            //~ }
        //~ }
        //~ if (COLUMNS.size() >= MAX_CHUNKS) {
            //~ trim(pos.x, pos.z);
        //~ }
        //~ COLUMNS.put(Long.valueOf(FarTerrainMesh.key(pos.x, pos.z)), column);
        //~ dirty = true;
    //~ }

    //~ /** Drops the stored chunks farthest from (x, z) until a quarter of the room is free again. */
    //~ private static void trim(final int x, final int z) {
        //~ int radius = 128;
        //~ while (COLUMNS.size() > MAX_CHUNKS * 3 / 4 && radius > 8) {
            //~ final Iterator<Long> it = COLUMNS.keySet().iterator();
            //~ while (it.hasNext()) {
                //~ final long key = it.next().longValue();
                //~ final int dx = FarTerrainMesh.keyX(key) - x;
                //~ final int dz = FarTerrainMesh.keyZ(key) - z;
                //~ if (dx * dx + dz * dz > radius * radius) {
                    //~ it.remove();
                //~ }
            //~ }
            //~ radius /= 2;
        //~ }
    //~ }

    //~ /** The world changed: forget everything (the render thread frees the buffers on its next frame). */
    //~ public static void levelChanged() {
        //~ COLUMNS.clear();
        //~ dirty = true;
        //~ builtX = Integer.MIN_VALUE;
    //~ }

    //~ /** Far terrain was drawn this frame. */
    //~ public static boolean drawnThisFrame() {
        //~ return drawing;
    //~ }

    //~ /** True while far terrain was drawn this frame (the terrain fog then reaches its edge). */
    //~ public static boolean extendFog() {
        //~ if (!drawing) {
            //~ return false;
        //~ }
        //~ RenderSystem.fogStart(drawnFarBlocks * fogStartFraction());
        //~ RenderSystem.fogEnd(drawnFarBlocks);
        //~ return true;
    //~ }

    //~ /** LevelRenderer.renderLevel, right after the sky. */
    //~ public static void draw(final PoseStack poseStack, final Camera camera) {
        //~ drawing = false;
        //~ final Minecraft mc = Minecraft.getInstance();
        //~ final ClientLevel level = mc.level;
        //~ final int far = level == null ? 0 : effectiveFar(mc);
        //~ if (far <= 0 || level.dimension() != Level.OVERWORLD) {
            //~ // Option off, another dimension or an error: everything goes. Only the render distance
            //~ // reaching the far distance: the buffers go, the sampled chunks stay for later.
            //~ final boolean keep = level != null && level.dimension() == Level.OVERWORLD && Tuning.farTerrain > 0
                    //~ && !broken && !SHADER_MOD;
            //~ if (bufferCount > 0 || (!keep && !COLUMNS.isEmpty())) {
                //~ release(keep);
            //~ }
            //~ return;
        //~ }
        //~ try {
            //~ if (level != seededLevel) {
                //~ seededLevel = level;
                //~ COLUMNS.clear();
                //~ seed(level, mc.options.renderDistance, camera);
            //~ }
            //~ if (workerError != null) {
                //~ final Throwable error = workerError;
                //~ workerError = null;
                //~ throw new IllegalStateException("far terrain mesh failed", error);
            //~ }
            //~ if (ready) {
                //~ upload();
            //~ }
            //~ schedule(mc, level, camera, far);
            //~ if (bufferCount == 0 || !camera.getFluidInCamera().isEmpty() || blinded(camera)) {
                //~ return;
            //~ }
            //~ render(poseStack, camera, far);
            //~ drawing = true;
            //~ drawnFarBlocks = far * 16;
        //~ } catch (final RuntimeException | LinkageError e) {
            //~ fail(e);
        //~ }
    //~ }

    //~ /** The Fog start setting (75% = vanilla), capped so some fog always remains at the edge. */
    //~ private static float fogStartFraction() {
        //~ return Math.min(0.95F, Scenery.fogStartPercent / 100.0F);
    //~ }

    //~ private static boolean blinded(final Camera camera) {
        //~ final Entity entity = camera.getEntity();
        //~ return entity instanceof LivingEntity && ((LivingEntity) entity).hasEffect(MobEffects.BLINDNESS);
    //~ }

    //~ /** Turning the option on mid-game: the chunks already loaded are sampled at once. */
    //~ private static void seed(final ClientLevel level, final int rd, final Camera camera) {
        //~ final BlockPos at = camera.getBlockPosition();
        //~ final int cx = at.getX() >> 4;
        //~ final int cz = at.getZ() >> 4;
        //~ for (int dz = -rd; dz <= rd; dz++) {
            //~ for (int dx = -rd; dx <= rd; dx++) {
                //~ // A missing chunk comes back as the client's EmptyLevelChunk (a subclass).
                //~ final LevelChunk chunk = level.getChunk(cx + dx, cz + dz);
                //~ if (chunk != null && chunk.getClass() == LevelChunk.class) {
                    //~ store(chunk);
                //~ }
            //~ }
        //~ }
    //~ }

    //~ private static void schedule(final Minecraft mc, final ClientLevel level, final Camera camera, final int far) {
        //~ if (building) {
            //~ return;
        //~ }
        //~ final BlockPos at = camera.getBlockPosition();
        //~ final int cx = at.getX() >> 4;
        //~ final int cz = at.getZ() >> 4;
        //~ final int rd = mc.options.renderDistance;
        //~ // Daylight in 16 steps: the outline darkens with the sky (vertex colours, so it needs a rebuild).
        //~ final int light = Math.round(level.getSkyDarken(1.0F) * 16.0F);
        //~ final long now = System.nanoTime();
        //~ final boolean changed = cx != builtX || cz != builtZ || rd != builtRd || far != builtFar || light != builtLight;
        //~ final boolean timed = (dirty || builtInside) && now - lastBuild >= REBUILD_NANOS;
        //~ if (!changed && !timed) {
            //~ return;
        //~ }
        //~ final Set<Long> inside = unbuilt(mc, cx, cz, rd);
        //~ builtX = cx;
        //~ builtZ = cz;
        //~ builtRd = rd;
        //~ builtFar = far;
        //~ builtLight = light;
        //~ builtInside = !inside.isEmpty();
        //~ lastBuild = now;
        //~ dirty = false;
        //~ pendingX = cx;
        //~ pendingZ = cz;
        //~ building = true;
        //~ final float brightness = Math.max(0.2F, Math.min(1.0F, light / 16.0F));
        //~ if (worker == null) {
            //~ worker = Executors.newSingleThreadExecutor(r -> {
                //~ final Thread t = new Thread(r, "Aetherium far terrain");
                //~ t.setDaemon(true);
                //~ t.setPriority(Thread.MIN_PRIORITY);
                //~ return t;
            //~ });
        //~ }
        //~ worker.execute(() -> {
            //~ try {
                //~ final Writer writer = new Writer(brightness);
                //~ FarTerrainMesh.build(COLUMNS, cx, cz, rd, far, inside, writer);
                //~ readyBuffers = writer.finish();
                //~ ready = true;
            //~ } catch (final Throwable t) {
                //~ workerError = t;
                //~ building = false;
            //~ }
        //~ });
    //~ }

    //~ /**
     //~ * Stored chunks inside the render distance that vanilla is not drawing yet: the section at their
     //~ * surface is missing from the view area or has never been compiled.
     //~ */
    //~ private static Set<Long> unbuilt(final Minecraft mc, final int cx, final int cz, final int rd) {
        //~ final Set<Long> out = new HashSet<Long>();
        //~ final Object renderer = mc.levelRenderer;
        //~ if (!(renderer instanceof TuningLevelAccessor)) {
            //~ return out;
        //~ }
        //~ final Object area = ((TuningLevelAccessor) renderer).aetherium$viewArea();
        //~ if (!(area instanceof TuningViewAreaAccessor)) {
            //~ return out;
        //~ }
        //~ final TuningViewAreaAccessor view = (TuningViewAreaAccessor) area;
        //~ final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        //~ for (int dz = -rd; dz <= rd; dz++) {
            //~ for (int dx = -rd; dx <= rd; dx++) {
                //~ final Long key = Long.valueOf(FarTerrainMesh.key(cx + dx, cz + dz));
                //~ final FarTerrainMesh.Column column = COLUMNS.get(key);
                //~ if (column == null) {
                    //~ continue;
                //~ }
                //~ at.set(((cx + dx) << 4) + 8, Math.max(0, Math.min(255, column.heights[5] - 1)), ((cz + dz) << 4) + 8);
                //~ final ChunkRenderDispatcher.RenderChunk section = view.aetherium$renderChunkAt(at);
                //~ // The view area wraps positions around (modulo), so a section from another chunk can come
                //~ // back: only a section whose origin is this chunk counts.
                //~ final BlockPos origin = section == null ? null : section.getOrigin();
                //~ if (origin == null || origin.getX() >> 4 != cx + dx || origin.getZ() >> 4 != cz + dz
                        //~ || section.getCompiledChunk() == ChunkRenderDispatcher.CompiledChunk.UNCOMPILED) {
                    //~ out.add(key);
                //~ }
            //~ }
        //~ }
        //~ return out;
    //~ }

    //~ /** Render thread: the worker's buffers become the drawn mesh. */
    //~ private static void upload() {
        //~ final int count = readyBuffers;
        //~ for (int i = 0; i < count; i++) {
            //~ if (i >= BUFFERS.size()) {
                //~ BUFFERS.add(new VertexBuffer(FORMAT));
            //~ }
            //~ BUFFERS.get(i).upload(BUILDERS.get(i));
        //~ }
        //~ while (BUFFERS.size() > count) {
            //~ BUFFERS.remove(BUFFERS.size() - 1).close();
        //~ }
        //~ bufferCount = count;
        //~ meshX = pendingX;
        //~ meshZ = pendingZ;
        //~ ready = false;
        //~ building = false;
    //~ }

    //~ private static void render(final PoseStack poseStack, final Camera camera, final int far) {
        //~ final Vec3 cam = camera.getPosition();
        //~ // Restored afterwards to whatever the sky left (vanilla's render states expect their defaults).
        //~ final boolean depth = GL11.glIsEnabled(GL_DEPTH_TEST);
        //~ final boolean cull = GL11.glIsEnabled(GL_CULL_FACE);
        //~ final boolean fog = GL11.glIsEnabled(GL_FOG);
        //~ RenderSystem.disableTexture();
        //~ RenderSystem.disableCull();
        //~ RenderSystem.enableDepthTest();
        //~ RenderSystem.depthMask(true);
        //~ RenderSystem.enableFog();
        //~ RenderSystem.fogStart(far * 16 * fogStartFraction());
        //~ RenderSystem.fogEnd(far * 16);
        //~ poseStack.pushPose();
        //~ // 0.1 below the real surface, so an outline left under a chunk that was just built hides
        //~ // beneath its top faces instead of flickering with them.
        //~ poseStack.translate((meshX << 4) - cam.x, -0.1 - cam.y, (meshZ << 4) - cam.z);
        //~ final Matrix4f pose = poseStack.last().pose();
        //~ for (int i = 0; i < bufferCount; i++) {
            //~ final VertexBuffer buffer = BUFFERS.get(i);
            //~ buffer.bind();
            //~ FORMAT.setupBufferState(0L);
            //~ buffer.draw(pose, GL_QUADS);
        //~ }
        //~ VertexBuffer.unbind();
        //~ FORMAT.clearBufferState();
        //~ poseStack.popPose();
        //~ if (!fog) {
            //~ RenderSystem.disableFog();
        //~ }
        //~ if (cull) {
            //~ RenderSystem.enableCull();
        //~ }
        //~ if (!depth) {
            //~ RenderSystem.disableDepthTest();
        //~ }
        //~ RenderSystem.enableTexture();
    //~ }

    //~ private static void release(final boolean keepColumns) {
        //~ for (final VertexBuffer buffer : BUFFERS) {
            //~ buffer.close();
        //~ }
        //~ BUFFERS.clear();
        //~ bufferCount = 0;
        //~ if (!building) {
            //~ BUILDERS.clear();
        //~ }
        //~ if (!keepColumns) {
            //~ COLUMNS.clear();
            //~ seededLevel = null;
        //~ }
        //~ builtX = Integer.MIN_VALUE;
        //~ drawing = false;
    //~ }

    //~ private static void fail(final Throwable e) {
        //~ broken = true;
        //~ LOGGER.error("Far terrain turned off after an error", e);
        //~ try {
            //~ release(false);
        //~ } catch (final RuntimeException | LinkageError ignored) {
            //~ // already broken; the buffers are freed with the GL context
        //~ }
    //~ }

    //~ /** Worker thread: quads into position-colour builders of at most {@link #QUADS_PER_BUFFER}. */
    //~ private static final class Writer implements FarTerrainMesh.Sink {
        //~ private final float brightness;
        //~ private BufferBuilder current;
        //~ private int used;
        //~ private int quads;

        //~ Writer(final float brightness) {
            //~ this.brightness = brightness;
        //~ }

        //~ @Override
        //~ public void quad(final float x0, final float y0, final float z0, final float x1, final float y1, final float z1,
                         //~ final float x2, final float y2, final float z2, final float x3, final float y3, final float z3,
                         //~ final int rgb) {
            //~ if (this.current == null || this.quads >= QUADS_PER_BUFFER) {
                //~ this.next();
            //~ }
            //~ final int shaded = FarTerrainMesh.shade(rgb, this.brightness);
            //~ final int r = (shaded >> 16) & 0xFF;
            //~ final int g = (shaded >> 8) & 0xFF;
            //~ final int b = shaded & 0xFF;
            //~ final VertexConsumer out = (VertexConsumer) (Object) this.current;
            //~ out.vertex(x0, y0, z0).color(r, g, b, 255).endVertex();
            //~ out.vertex(x1, y1, z1).color(r, g, b, 255).endVertex();
            //~ out.vertex(x2, y2, z2).color(r, g, b, 255).endVertex();
            //~ out.vertex(x3, y3, z3).color(r, g, b, 255).endVertex();
            //~ this.quads++;
        //~ }

        //~ private void next() {
            //~ if (this.current != null) {
                //~ this.current.end();
            //~ }
            //~ if (this.used < BUILDERS.size()) {
                //~ this.current = BUILDERS.get(this.used);
            //~ } else {
                //~ // Capacity in ints: one full buffer of position-colour quads (16 bytes per vertex).
                //~ this.current = new BufferBuilder(QUADS_PER_BUFFER * 4 * 4);
                //~ BUILDERS.add(this.current);
            //~ }
            //~ this.used++;
            //~ this.current.begin(GL_QUADS, FORMAT);
            //~ this.quads = 0;
        //~ }

        //~ /** @return number of filled builders */
        //~ int finish() {
            //~ if (this.current != null) {
                //~ this.current.end();
            //~ }
            //~ return this.used;
        //~ }
    //~ }
    // @era:t-far-terrain-end
}
