package com.aetherium.perf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 1.7.0: view frustum, particle billboards, sprite activity, model vertex writes. */
final class SodiumStyleTest {

    // ------------------------------------------------------------------ helpers

    /** Column-major perspective matrix, as Matrix4f.perspective / gluPerspective. */
    private static FloatBuffer perspective(final float fovDeg, final float aspect, final float near, final float far) {
        final float f = (float) (1.0 / Math.tan(Math.toRadians(fovDeg) / 2.0));
        final float[] m = new float[16];
        m[0] = f / aspect;
        m[5] = f;
        m[10] = (far + near) / (near - far);
        m[11] = -1f;
        m[14] = 2f * far * near / (near - far);
        return FloatBuffer.wrap(m);
    }

    /** Column-major rotation about Y by {@code deg} (as PoseStack.mulPose(YP.rotationDegrees)). */
    private static FloatBuffer yaw(final float deg) {
        final float c = (float) Math.cos(Math.toRadians(deg));
        final float s = (float) Math.sin(Math.toRadians(deg));
        final float[] m = new float[16];
        m[0] = c;
        m[2] = -s;
        m[5] = 1f;
        m[8] = s;
        m[10] = c;
        m[15] = 1f;
        return FloatBuffer.wrap(m);
    }

    /** Vanilla's quaternion product (Quaternion.mul), components i, j, k, r. */
    private static float[] mul(final float[] a, final float[] b) {
        return new float[] {
            a[3] * b[0] + a[0] * b[3] + a[1] * b[2] - a[2] * b[1],
            a[3] * b[1] - a[0] * b[2] + a[1] * b[3] + a[2] * b[0],
            a[3] * b[2] + a[0] * b[1] - a[1] * b[0] + a[2] * b[3],
            a[3] * b[3] - a[0] * b[0] - a[1] * b[1] - a[2] * b[2],
        };
    }

    /** Vanilla's Vector3f.transform(Quaternion): q * (v, 0) * conj(q). */
    private static float[] rotate(final float[] q, final float x, final float y, final float z) {
        final float[] r = mul(mul(q, new float[] {x, y, z, 0f}), new float[] {-q[0], -q[1], -q[2], q[3]});
        return new float[] {r[0], r[1], r[2]};
    }

    private static float[] axisAngle(final float ax, final float ay, final float az, final float rad) {
        final float s = (float) Math.sin(rad / 2f);
        return new float[] {ax * s, ay * s, az * s, (float) Math.cos(rad / 2f)};
    }

    // ------------------------------------------------------------------ frustum

    @Test
    @DisplayName("frustum: ahead visible, behind and far off to the side not, spheres near an edge kept")
    void frustum() {
        final ViewFrustum f = new ViewFrustum();
        assertTrue(f.sphereVisible(0f, 0f, 100f, 0f), "no frame yet: everything visible");
        f.update(perspective(90f, 1f, 0.05f, 1000f), yaw(0f));
        assertTrue(f.valid());
        assertTrue(f.sphereVisible(0f, 0f, -10f, 0.1f));
        assertFalse(f.sphereVisible(0f, 0f, 10f, 0.1f), "behind the camera");
        assertFalse(f.sphereVisible(-100f, 0f, -10f, 0.1f), "far left");
        assertFalse(f.sphereVisible(0f, 50f, -10f, 0.1f), "far above");
        // 90 degree fov: the left plane is x = z. A point just outside, with a radius reaching in, is kept.
        assertFalse(f.sphereVisible(-11f, 0f, -10f, 0.5f));
        assertTrue(f.sphereVisible(-11f, 0f, -10f, 1f));
        // Turned around (camera rotation of 180 degrees): +z is ahead.
        f.update(perspective(90f, 1f, 0.05f, 1000f), yaw(180f));
        assertTrue(f.sphereVisible(0f, 0f, 10f, 0.1f));
        assertFalse(f.sphereVisible(0f, 0f, -10f, 0.1f));
        f.invalidate();
        assertTrue(f.sphereVisible(0f, 0f, -10f, 0.1f));
    }

    @Test
    @DisplayName("frustum: nearest depth of a box along the view direction")
    void depth() {
        final ViewFrustum f = new ViewFrustum();
        f.update(perspective(70f, 1.5f, 0.05f, 1000f), yaw(0f));
        assertEquals(92f, f.nearestDepth(0f, 0f, -100f, 8f, 8f, 8f), 1e-4f);
        assertEquals(-108f, f.nearestDepth(0f, 0f, 100f, 8f, 8f, 8f), 1e-4f);
        // Off to the side: depth ignores the sideways offset.
        assertEquals(92f, f.nearestDepth(500f, 0f, -100f, 8f, 8f, 8f), 1e-4f);
        f.update(perspective(70f, 1.5f, 0.05f, 1000f), yaw(90f));
        // Rotation of 90 degrees about Y maps world +x to view -z: +x is ahead.
        assertEquals(92f, f.nearestDepth(100f, 0f, 0f, 8f, 8f, 8f), 1e-3f);
    }

    // ------------------------------------------------------------------ billboard

    @Test
    @DisplayName("billboard: corners equal vanilla's per-corner quaternion rotation, with and without roll")
    void billboard() {
        final Random random = new Random(7);
        final float[] cx = {-1f, -1f, 1f, 1f};
        final float[] cy = {-1f, 1f, 1f, -1f};
        final float[] out = new float[12];
        for (int n = 0; n < 200; n++) {
            final float[] yawQ = axisAngle(0f, 1f, 0f, (float) (random.nextDouble() * Math.PI * 2));
            final float[] pitchQ = axisAngle(1f, 0f, 0f, (float) ((random.nextDouble() - 0.5) * Math.PI));
            final float[] q = mul(yawQ, pitchQ);
            final float roll = n % 3 == 0 ? 0f : (float) ((random.nextDouble() - 0.5) * 6);
            final float size = 0.05f + random.nextFloat();
            final float[] qr = roll == 0f ? q : mul(q, axisAngle(0f, 0f, 1f, roll));
            Billboard.corners(q[0], q[1], q[2], q[3], roll, size, out);
            for (int c = 0; c < 4; c++) {
                final float[] v = rotate(qr, cx[c], cy[c], 0f);
                for (int k = 0; k < 3; k++) {
                    assertEquals(v[k] * size, out[c * 3 + k], 1e-5f, "corner " + c + " axis " + k);
                }
            }
        }
    }

    // ------------------------------------------------------------------ sprites

    private static final class Sprite implements SpriteActivity.Tracked {
        boolean tracked;
        int lastSeen;

        @Override
        public boolean aetheriumTracked() {
            return this.tracked;
        }

        @Override
        public void aetheriumSetTracked(final boolean t) {
            this.tracked = t;
        }

        @Override
        public int aetheriumLastSeen() {
            return this.lastSeen;
        }

        @Override
        public void aetheriumSetLastSeen(final int tick) {
            this.lastSeen = tick;
        }
    }

    @Test
    @DisplayName("sprites: unseen tracked sprites pause after the window; sections record and mark their sprites")
    void sprites() throws Exception {
        SpriteActivity.resetForTests();
        final Sprite water = new Sprite();
        final Sprite fire = new Sprite();
        final Sprite plain = new Sprite();
        SpriteActivity.track(water, true);
        SpriteActivity.track(fire, !SpriteActivity.alwaysAnimated("minecraft:block/fire_0"));
        assertFalse(fire.tracked, "fire is always animated");
        assertTrue(SpriteActivity.shouldAnimate(water), "off: everything animates");
        SpriteActivity.setEnabled(true);
        SpriteActivity.frame();
        for (int i = 0; i <= SpriteActivity.WINDOW; i++) {
            assertTrue(SpriteActivity.shouldAnimate(water), "within the window " + i);
            SpriteActivity.advance();
        }
        assertFalse(SpriteActivity.shouldAnimate(water), "unseen for longer than the window");
        assertTrue(SpriteActivity.shouldAnimate(fire));
        assertTrue(SpriteActivity.shouldAnimate(plain), "untracked");
        SpriteActivity.touch(water); // render thread: marked at once
        assertTrue(SpriteActivity.shouldAnimate(water));

        // A section build on another thread records instead of marking.
        for (int i = 0; i <= SpriteActivity.WINDOW + 1; i++) {
            SpriteActivity.advance();
        }
        final Object[][] result = new Object[1][];
        final Thread worker = new Thread(() -> {
            SpriteActivity.beginSection();
            SpriteActivity.touch(water);
            SpriteActivity.touch(water);
            SpriteActivity.touch(plain);
            result[0] = SpriteActivity.endSection();
            SpriteActivity.beginSection();
            final Object[] empty = SpriteActivity.endSection();
            if (empty != SpriteActivity.NONE) {
                throw new IllegalStateException("empty section allocates");
            }
        });
        worker.start();
        worker.join();
        assertEquals(1, result[0].length, "deduplicated, untracked ignored");
        assertFalse(SpriteActivity.shouldAnimate(water), "recording did not mark it");
        SpriteActivity.seen(result[0]);
        assertTrue(SpriteActivity.shouldAnimate(water), "the visible section marked it");

        // A section rebuilt synchronously on the render thread records as well.
        for (int i = 0; i <= SpriteActivity.WINDOW + 1; i++) {
            SpriteActivity.advance();
        }
        SpriteActivity.beginSection();
        SpriteActivity.touch(water);
        final Object[] sync = SpriteActivity.endSection();
        assertEquals(1, sync.length, "render-thread section recorded its sprite");
        assertFalse(SpriteActivity.shouldAnimate(water), "render-thread section did not mark it");
        SpriteActivity.touch(water);
        assertTrue(SpriteActivity.shouldAnimate(water), "outside a section the render thread marks again");
        SpriteActivity.resetForTests();
    }

    // ------------------------------------------------------------------ model vertices

    private static byte normalIntValue(final float f) {
        return (byte) ((int) (Math.max(-1f, Math.min(1f, f)) * 127.0F) & 255);
    }

    /** BufferBuilder.vertex's fast path for NEW_ENTITY (fullFormat), byte for byte. */
    private static void vanillaVertex(final ByteBuffer b, final int at, final float x, final float y, final float z,
                                      final float r, final float g, final float bl, final float a, final float u,
                                      final float v, final int overlay, final int light, final float nx,
                                      final float ny, final float nz) {
        b.putFloat(at, x);
        b.putFloat(at + 4, y);
        b.putFloat(at + 8, z);
        b.put(at + 12, (byte) (int) (r * 255.0F));
        b.put(at + 13, (byte) (int) (g * 255.0F));
        b.put(at + 14, (byte) (int) (bl * 255.0F));
        b.put(at + 15, (byte) (int) (a * 255.0F));
        b.putFloat(at + 16, u);
        b.putFloat(at + 20, v);
        b.putShort(at + 24, (short) (overlay & 65535));
        b.putShort(at + 26, (short) (overlay >> 16 & 65535));
        b.putShort(at + 28, (short) (light & 65535));
        b.putShort(at + 30, (short) (light >> 16 & 65535));
        b.put(at + 32, normalIntValue(nx));
        b.put(at + 33, normalIntValue(ny));
        b.put(at + 34, normalIntValue(nz));
    }

    @Test
    @DisplayName("model vertices: same bytes as BufferBuilder's NEW_ENTITY fast path (padding aside)")
    void modelVertices() {
        final Random random = new Random(3);
        final ByteBuffer ours = ByteBuffer.allocateDirect(36 * 64).order(ByteOrder.LITTLE_ENDIAN);
        final ByteBuffer ref = ByteBuffer.allocateDirect(36 * 64).order(ByteOrder.LITTLE_ENDIAN);
        assertTrue(ModelVertexWriter.usable(ours));
        assertFalse(ModelVertexWriter.usable(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN)));
        final float[] m = new float[16];
        for (int n = 0; n < 64; n++) {
            for (int i = 0; i < 16; i++) {
                m[i] = (random.nextFloat() - 0.5f) * 4f;
            }
            final float x = random.nextFloat() - 0.5f;
            final float y = random.nextFloat() - 0.5f;
            final float z = random.nextFloat() - 0.5f;
            final float r = random.nextFloat();
            final float g = n == 5 ? 1.5f : random.nextFloat(); // over-bright, as some renderers pass
            final float bl = random.nextFloat();
            final float a = random.nextFloat();
            final float u = random.nextFloat();
            final float v = random.nextFloat();
            final int overlay = random.nextInt();
            final int light = random.nextInt();
            final float nx = (random.nextFloat() - 0.5f) * 2.5f; // clamped
            final float ny = random.nextFloat() - 0.5f;
            final float nz = -random.nextFloat();
            final int at = n * 36;
            ModelVertexWriter.vertex(ours, at, m, x, y, z, ModelVertexWriter.packColor(r, g, bl, a), u, v, overlay,
                    light, ModelVertexWriter.packNormal(nx, ny, nz));
            // Vector4f.transform: m00*x + m01*y + m02*z + m03*w, w = 1.
            vanillaVertex(ref, at, m[0] * x + m[4] * y + m[8] * z + m[12] * 1.0F,
                    m[1] * x + m[5] * y + m[9] * z + m[13] * 1.0F, m[2] * x + m[6] * y + m[10] * z + m[14] * 1.0F,
                    r, g, bl, a, u, v, overlay, light, nx, ny, nz);
            for (int k = 0; k < 35; k++) {
                assertEquals(ref.get(at + k), ours.get(at + k), "vertex " + n + " byte " + k);
            }
            assertEquals((byte) 0, ours.get(at + 35), "padding");
        }
    }

    // ------------------------------------------------------------------ cubes

    /** Stand-ins for ModelPart.Vertex / Polygon / Cube (package-private there, found by type here). */
    static final class FakeVertex implements ModelVertexData {
        final float x;
        final float y;
        final float z;
        final float u;
        final float v;

        FakeVertex(final float x, final float y, final float z, final float u, final float v) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.u = u;
            this.v = v;
        }

        @Override
        public float aetheriumX() {
            return this.x / 16.0F;
        }

        @Override
        public float aetheriumY() {
            return this.y / 16.0F;
        }

        @Override
        public float aetheriumZ() {
            return this.z / 16.0F;
        }

        @Override
        public float aetheriumU() {
            return this.u;
        }

        @Override
        public float aetheriumV() {
            return this.v;
        }
    }

    static final class FakePolygon implements ModelPolygonData {
        final FakeVertex[] vertices;
        final float nx;
        final float ny;
        final float nz;

        FakePolygon(final FakeVertex[] vertices, final float nx, final float ny, final float nz) {
            this.vertices = vertices;
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
        }

        @Override
        public float aetheriumNormalX() {
            return this.nx;
        }

        @Override
        public float aetheriumNormalY() {
            return this.ny;
        }

        @Override
        public float aetheriumNormalZ() {
            return this.nz;
        }
    }

    static final class FakeCube {
        private final FakePolygon[] polygons;
        public final float minX = 1f;
        public final float maxX = 2f;

        FakeCube(final FakePolygon[] polygons) {
            this.polygons = polygons;
        }
    }

    static final class AmbiguousCube {
        final Object[] a = new Object[0];
        final Object[] b = new Object[0];
    }

    @Test
    @DisplayName("cubes: packed by type in face order, then written exactly like ModelPart.compile")
    void cubes() {
        final Random random = new Random(11);
        final FakePolygon[] polygons = new FakePolygon[6];
        for (int f = 0; f < 6; f++) {
            final FakeVertex[] vs = new FakeVertex[4];
            for (int v = 0; v < 4; v++) {
                vs[v] = new FakeVertex(random.nextFloat() * 16f - 8f, random.nextFloat() * 16f, random.nextFloat() * 4f,
                        random.nextFloat(), random.nextFloat());
            }
            polygons[f] = new FakePolygon(vs, f == 0 ? -1f : 0f, f == 2 ? 1f : 0f, f > 2 ? 1f : 0f);
        }
        final FakeCube cube = new FakeCube(polygons);
        final float[] faces = ModelCubes.pack(cube, FakeCube.class);
        assertTrue(faces != null && faces.length == 6 * ModelCubeData.FACE_FLOATS, "packed");
        assertEquals(polygons[3].vertices[2].u, faces[3 * ModelCubeData.FACE_FLOATS + 2 * 5 + 3], 0f, "order kept");
        assertTrue(ModelCubes.pack(new AmbiguousCube(), AmbiguousCube.class) == null, "two arrays: refuse");
        assertTrue(ModelCubes.pack(new FakeCube(new FakePolygon[] {new FakePolygon(new FakeVertex[3], 0, 0, 1)}),
                FakeCube.class) == null, "three vertices: refuse");

        final float[] m = new float[16];
        for (int i = 0; i < 16; i++) {
            m[i] = (random.nextFloat() - 0.5f) * 3f;
        }
        final float[] n = new float[9];
        for (int i = 0; i < 9; i++) {
            n[i] = random.nextFloat() - 0.5f;
        }
        final float r = 0.9f;
        final float g = 0.5f;
        final float bl = 1f;
        final float a = 0.75f;
        final int overlay = 0x000A0003;
        final int light = 0x00F000F0;
        final ByteBuffer ours = ByteBuffer.allocateDirect(36 * 24 + 36).order(ByteOrder.LITTLE_ENDIAN);
        final ByteBuffer ref = ByteBuffer.allocateDirect(36 * 24 + 36).order(ByteOrder.LITTLE_ENDIAN);
        final int written = ModelVertexWriter.writeFaces(ours, 36, m, n, faces, ModelVertexWriter.packColor(r, g, bl, a),
                overlay, light);
        assertEquals(36 * 24, written, "24 vertices");
        // ModelPart.compile: normal.copy().transform(normalMatrix); Vector4f(x/16, y/16, z/16, 1).transform(pose).
        int at = 36;
        for (final FakePolygon p : polygons) {
            final float nx = n[0] * p.nx + n[1] * p.ny + n[2] * p.nz;
            final float ny = n[3] * p.nx + n[4] * p.ny + n[5] * p.nz;
            final float nz = n[6] * p.nx + n[7] * p.ny + n[8] * p.nz;
            for (final FakeVertex v : p.vertices) {
                final float x = v.x / 16.0F;
                final float y = v.y / 16.0F;
                final float z = v.z / 16.0F;
                vanillaVertex(ref, at, m[0] * x + m[4] * y + m[8] * z + m[12] * 1.0F,
                        m[1] * x + m[5] * y + m[9] * z + m[13] * 1.0F, m[2] * x + m[6] * y + m[10] * z + m[14] * 1.0F,
                        r, g, bl, a, v.u, v.v, overlay, light, nx, ny, nz);
                at += 36;
            }
        }
        for (int k = 36; k < 36 + written; k++) {
            if (k % 36 != 35) {
                assertEquals(ref.get(k), ours.get(k), "byte " + k);
            }
        }

        // 1.20.5+ untrusted pose: transformNormal normalises (joml: x * (1f / (float) Math.sqrt(len2))).
        for (int i = 0; i < 9; i++) {
            n[i] *= 3.0f; // a scaled normal matrix: unnormalised results would clamp
        }
        ours.clear();
        ref.clear();
        assertEquals(36 * 24, ModelVertexWriter.writeFaces(ours, 36, m, n, faces,
                ModelVertexWriter.packColor(r, g, bl, a), overlay, light, true), "24 vertices, normalised");
        at = 36;
        for (final FakePolygon p : polygons) {
            float nx = n[0] * p.nx + n[1] * p.ny + n[2] * p.nz;
            float ny = n[3] * p.nx + n[4] * p.ny + n[5] * p.nz;
            float nz = n[6] * p.nx + n[7] * p.ny + n[8] * p.nz;
            final float inv = 1.0f / (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            nx *= inv;
            ny *= inv;
            nz *= inv;
            for (final FakeVertex v : p.vertices) {
                final float x = v.x / 16.0F;
                final float y = v.y / 16.0F;
                final float z = v.z / 16.0F;
                vanillaVertex(ref, at, m[0] * x + m[4] * y + m[8] * z + m[12] * 1.0F,
                        m[1] * x + m[5] * y + m[9] * z + m[13] * 1.0F, m[2] * x + m[6] * y + m[10] * z + m[14] * 1.0F,
                        r, g, bl, a, v.u, v.v, overlay, light, nx, ny, nz);
                at += 36;
            }
        }
        for (int k = 36; k < 36 + written; k++) {
            if (k % 36 != 35) {
                assertEquals(ref.get(k), ours.get(k), "normalised byte " + k);
            }
        }
    }

    // ------------------------------------------------------------------ visibility search

    @Test
    @DisplayName("visibility search: streaming triggers at most every 50 ms, held triggers fire later")
    void visibilitySearch() {
        VisibilitySearch.resetForTests();
        final long ms = 1_000_000L;
        long now = 1000 * ms;
        VisibilitySearch.beginFrame(now, true);
        assertFalse(VisibilitySearch.compileQueueEmpty(false), "first trigger passes");
        assertTrue(VisibilitySearch.dirtyTrigger());
        VisibilitySearch.searched(now);
        assertFalse(VisibilitySearch.dirtyTrigger(), "after the search, the same frame's dirty sections are held");
        now += 16 * ms;
        VisibilitySearch.beginFrame(now, true);
        assertTrue(VisibilitySearch.compileQueueEmpty(false), "held: reported empty");
        assertFalse(VisibilitySearch.dirtyTrigger(), "held");
        assertTrue(VisibilitySearch.compileQueueEmpty(true), "really empty stays empty");
        assertFalse(VisibilitySearch.takePending(), "interval not over");
        assertFalse(VisibilitySearch.afterUploads(true, now), "uploads share the interval");
        now += 40 * ms;
        VisibilitySearch.beginFrame(now, true);
        assertTrue(VisibilitySearch.takePending(), "held trigger fires once the interval passed");
        assertFalse(VisibilitySearch.takePending(), "once");
        VisibilitySearch.searched(now);
        now += 10 * ms;
        VisibilitySearch.beginFrame(now, false);
        assertFalse(VisibilitySearch.compileQueueEmpty(false), "throttle off: vanilla");
        assertTrue(VisibilitySearch.dirtyTrigger(), "throttle off: vanilla");
        assertTrue(VisibilitySearch.held() >= 2);
        VisibilitySearch.resetForTests();
    }

    @Test
    @DisplayName("fog cull: only wholly fogged boxes, only with fresh linear fog")
    void fogCull() {
        FogCull.resetForTests();
        final ViewFrustum frame = new ViewFrustum();
        frame.update(perspective(70f, 1.5f, 0.05f, 512f), FloatBuffer.wrap(new float[] {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1}));
        FogCull.begin(true, true, FogCull.GL_LINEAR, 96f, 128f, 144f, frame);
        assertTrue(FogCull.active());
        // Looking along -z.
        assertTrue(FogCull.hidden(frame, 0f, 0f, -170f, 8f, 8f, 8f), "nearest point at depth 162 > 128 + 16");
        assertFalse(FogCull.hidden(frame, 0f, 0f, -150f, 8f, 8f, 8f), "nearest point at 142: inside the margin");
        assertFalse(FogCull.hidden(frame, 140f, 0f, -20f, 8f, 8f, 8f), "far to the side but shallow: kept");
        assertFalse(FogCull.hidden(frame, 0f, 0f, 150f, 8f, 8f, 8f), "behind: left to the frustum");
        FogCull.begin(true, true, 0x0801 /* EXP2 */, 0f, 128f, 144f, frame);
        assertFalse(FogCull.hidden(frame, 0f, 0f, -400f, 8f, 8f, 8f), "exponential fog: nothing");
        FogCull.begin(true, false, FogCull.GL_LINEAR, 96f, 128f, 144f, frame);
        assertFalse(FogCull.hidden(frame, 0f, 0f, -400f, 8f, 8f, 8f), "no fog set this frame: nothing");
        FogCull.begin(false, true, FogCull.GL_LINEAR, 96f, 128f, 144f, frame);
        assertFalse(FogCull.hidden(frame, 0f, 0f, -400f, 8f, 8f, 8f), "option off");
        FogCull.begin(true, true, FogCull.GL_LINEAR, 96f, 400f, 144f, frame);
        assertFalse(FogCull.hidden(frame, 0f, 0f, -500f, 8f, 8f, 8f), "fog pushed past the render distance: nothing");
        assertEquals(1L, FogCull.culled());
        FogCull.resetForTests();
    }

    @Test
    @DisplayName("Distance fog cull (1.17+): cylinder test, farthest write per frame, opaque linear ranges only")
    void distanceFogCull() {
        FogCull.resetForTests();
        assertFalse(FogCull.beginDistance(true), "no fog seen yet: nothing to cull");
        // Two terrain-fog writes in one frame: the farther (128) wins.
        FogCull.recordFogRange(100f, 64f, 1f);
        FogCull.recordFogRange(96f, 128f, 1f);
        FogCull.frame();
        assertTrue(FogCull.distanceStale(true));
        assertTrue(FogCull.beginDistance(true));
        assertFalse(FogCull.distanceStale(true));
        // Limit 128 + margin 4.
        assertTrue(FogCull.distanceHidden(133f, -8f, -8f, 149f, 8f, 8f), "nearest horizontal 133");
        assertFalse(FogCull.distanceHidden(131f, -8f, -8f, 147f, 8f, 8f), "within the margin");
        // Diagonal: cylinder distance sqrt(95^2 + 95^2) = 134 even though each axis is under the limit.
        assertTrue(FogCull.distanceHidden(95f, 0f, 95f, 111f, 16f, 111f));
        // Straight up: the vertical axis alone counts.
        assertTrue(FogCull.distanceHidden(-8f, 140f, -8f, 8f, 156f, 8f));
        assertFalse(FogCull.distanceHidden(-8f, -8f, -8f, 8f, 8f, 8f), "camera section");
        // Translucent fog colour never hides anything; a frame without fog clears the limit.
        FogCull.recordFogRange(10f, 20f, 0.5f);
        FogCull.frame();
        assertTrue(FogCull.distanceStale(true));
        assertFalse(FogCull.beginDistance(true));
        // 1.21.6+: the nearer of the two applied ranges; "no fog" values are ignored.
        FogCull.recordFogRanges(Float.MAX_VALUE, Float.MAX_VALUE, 180f, 192f);
        FogCull.frame();
        assertTrue(FogCull.beginDistance(true));
        assertTrue(FogCull.distanceHidden(197f, 0f, 0f, 213f, 16f, 16f));
        assertFalse(FogCull.distanceHidden(195f, 0f, 0f, 211f, 16f, 16f));
        assertFalse(FogCull.beginDistance(false), "option off");
        assertFalse(FogCull.distanceHidden(1000f, 0f, 0f, 1016f, 16f, 16f));
        FogCull.resetForTests();
    }
}
