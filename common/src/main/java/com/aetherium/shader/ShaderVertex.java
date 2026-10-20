package com.aetherium.shader;

// @era:shader-begin none
// @era:shader-else v16
//~ import com.google.common.collect.ImmutableList;
//~ import com.mojang.blaze3d.vertex.DefaultVertexFormat;
//~ import com.mojang.blaze3d.vertex.VertexFormat;
//~ import com.mojang.blaze3d.vertex.VertexFormatElement;
//~ import java.nio.ByteBuffer;
//~ import java.util.HashMap;
//~ import java.util.IdentityHashMap;
//~ import java.util.Map;
//~ import net.minecraft.core.Registry;
//~ import net.minecraft.world.level.block.Block;
//~ import net.minecraft.world.level.block.state.BlockState;
//~ import net.minecraft.world.level.block.state.properties.Property;
// @era:shader-end

/**
 * The extended terrain vertex format of the shader pack engine (1.16.5): vanilla's 32-byte
 * {@code BLOCK} layout plus mc_Entity (texture coordinate set 3, four shorts), mc_midTexCoord
 * (set 4, two floats) and at_tangent (set 5, four floats), each only when the pack uses it.
 * 1.16.5's VertexFormatElement turns every indexed element other than a UV into a UV, so texture
 * coordinate sets are the only way to add attributes; GlslPatcher maps the pack's attribute
 * declarations onto gl_MultiTexCoord3-5.
 *
 * <p>The chunk mesher writes vanilla's elements; {@link #fill} (BufferBuilder.endVertex) writes
 * ours, and on every fourth vertex of a quad goes back to fill the quad-wide values.</p>
 */
public final class ShaderVertex {
    // @era:shader-begin none
    private ShaderVertex() {
    }
    // @era:shader-else v16
    //~ private static final int BASE = 32;
    //~ private static volatile boolean extended;
    //~ private static volatile VertexFormat format;
    //~ private static volatile int stride = BASE;
    //~ private static volatile int midOffset = -1;
    //~ private static volatile int tangentOffset = -1;
    //~ private static volatile int extraElements;
    //~ private static volatile Map<BlockState, Integer> ids = new IdentityHashMap<>();
    //~ private static final ThreadLocal<int[]> CURRENT = ThreadLocal.withInitial(() -> new int[] {-1});
    //~ /**
     //~ * GL translators (gl4es, Krypton): extra texture coordinate arrays corrupt the vanilla ones there,
     //~ * so the chunk format stays vanilla's BLOCK and the block id travels in the vertex colour's alpha
     //~ * (always opaque for blocks): alpha = 255 - code, code = id index (1-127, 0 = none) + 128 for the
     //~ * vertices on the upper half of their quad (mc_midTexCoord's job in waving shaders). The patched
     //~ * vertex shaders decode it and give the pack an opaque gl_Color (GlslPatcher, packed mode).
     //~ */
    //~ private static volatile boolean packed;
    //~ private static volatile Map<BlockState, Integer> codes = new IdentityHashMap<>();

    //~ private ShaderVertex() {
    //~ }

    //~ /** Render thread, before the chunks are rebuilt: the format a pack needs (or vanilla's). */
    //~ public static void configure(final boolean needed, final boolean midTex, final boolean tangent, final IdMap blocks) {
        //~ if (!needed) {
            //~ extended = false;
            //~ format = null;
            //~ ids = new IdentityHashMap<>();
            //~ return;
        //~ }
        //~ final ImmutableList.Builder<VertexFormatElement> list = ImmutableList.builder();
        //~ list.addAll(DefaultVertexFormat.BLOCK.getElements());
        //~ int offset = BASE;
        //~ int count = 1;
        //~ list.add(new VertexFormatElement(3, VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.UV, 4));
        //~ offset += 8;
        //~ int mid = -1;
        //~ int tan = -1;
        //~ if (midTex || tangent) {
            //~ // mc_midTexCoord is always present when anything after it is, so offsets stay fixed
            //~ list.add(new VertexFormatElement(4, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.UV, 2));
            //~ mid = offset;
            //~ offset += 8;
            //~ count++;
        //~ }
        //~ if (tangent) {
            //~ list.add(new VertexFormatElement(5, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.UV, 4));
            //~ tan = offset;
            //~ offset += 16;
            //~ count++;
        //~ }
        //~ ids = buildIds(blocks);
        //~ midOffset = mid;
        //~ tangentOffset = tan;
        //~ extraElements = count;
        //~ stride = offset;
        //~ format = new VertexFormat(list.build());
        //~ extended = true;
    //~ }

    //~ private static Map<BlockState, Integer> buildIds(final IdMap blocks) {
        //~ final Map<BlockState, Integer> out = new IdentityHashMap<>();
        //~ if (blocks == null) {
            //~ return out;
        //~ }
        //~ final Map<String, String> props = new HashMap<>();
        //~ for (final Block block : Registry.BLOCK) {
            //~ final String name = Registry.BLOCK.getKey(block).toString();
            //~ for (final BlockState state : block.getStateDefinition().getPossibleStates()) {
                //~ props.clear();
                //~ for (final Property<?> p : state.getProperties()) {
                    //~ props.put(p.getName(), valueName(p, state));
                //~ }
                //~ final int id = blocks.lookup(name, props, -1);
                //~ if (id != -1) {
                    //~ out.put(state, id);
                //~ }
            //~ }
        //~ }
        //~ return out;
    //~ }

    //~ @SuppressWarnings({"unchecked", "rawtypes"})
    //~ private static String valueName(final Property p, final BlockState state) {
        //~ return p.getName((Comparable) state.getValue(p));
    //~ }

    //~ public static boolean extended() {
        //~ return extended;
    //~ }

    //~ /**
     //~ * Render thread, before the chunks are rebuilt: packed-alpha ids (see {@link #packed}).
     //~ * {@code table[i]} is the block.properties id of index i (1-127).
     //~ */
    //~ public static void configurePacked(final boolean on, final float[] table, final IdMap blocks) {
        //~ if (!on || table == null) {
            //~ packed = false;
            //~ codes = new IdentityHashMap<>();
            //~ return;
        //~ }
        //~ final Map<Integer, Integer> index = new HashMap<>();
        //~ for (int i = 1; i < table.length; i++) {
            //~ if (table[i] != 0) {
                //~ index.put((int) table[i], i);
            //~ }
        //~ }
        //~ final Map<BlockState, Integer> out = new IdentityHashMap<>();
        //~ for (final Map.Entry<BlockState, Integer> e : buildIds(blocks).entrySet()) {
            //~ final Integer i = index.get(e.getValue());
            //~ if (i != null) {
                //~ out.put(e.getKey(), i);
            //~ }
        //~ }
        //~ codes = out;
        //~ packed = true;
    //~ }

    //~ public static boolean packed() {
        //~ return packed;
    //~ }

    //~ /** Mesher thread, after a block / fluid is meshed: vertices outside a block carry no id. */
    //~ public static void clearBlock() {
        //~ if (extended || packed) {
            //~ CURRENT.get()[0] = -1;
        //~ }
    //~ }

    //~ /**
     //~ * BufferBuilder.endVertex on a vanilla BLOCK buffer while ids are packed: {@code start} is the
     //~ * vertex's first byte (colour alpha at +15, v at +20); the fourth vertex of a quad marks the
     //~ * upper ones.
     //~ */
    //~ public static void pack(final ByteBuffer buffer, final int start, final int vertexIndex, final boolean quads) {
        //~ final int code = CURRENT.get()[0];
        //~ if (code < 0 || start < 0) {
            //~ return;
        //~ }
        //~ buffer.put(start + 15, (byte) (255 - code));
        //~ if (!quads || (vertexIndex & 3) != 3) {
            //~ return;
        //~ }
        //~ final int base = start - 3 * BASE;
        //~ if (base < 0) {
            //~ return;
        //~ }
        //~ float v = 0;
        //~ for (int i = 0; i < 4; i++) {
            //~ v += buffer.getFloat(base + i * BASE + 20);
        //~ }
        //~ v *= 0.25f;
        //~ for (int i = 0; i < 4; i++) {
            //~ final int at = base + i * BASE;
            //~ buffer.put(at + 15, (byte) (255 - (buffer.getFloat(at + 20) < v ? code | 128 : code)));
        //~ }
    //~ }


    //~ /** The format chunk buffers use: extended while a pack needs it, else vanilla's BLOCK. */
    //~ public static VertexFormat chunkFormat() {
        //~ final VertexFormat f = format;
        //~ return extended && f != null ? f : DefaultVertexFormat.BLOCK;
    //~ }

    //~ /** BufferBuilder.begin: builders that would use BLOCK use the extended format instead. */
    //~ public static VertexFormat swap(final VertexFormat requested) {
        //~ final VertexFormat f = format;
        //~ return extended && f != null && requested == DefaultVertexFormat.BLOCK ? f : requested;
    //~ }

    //~ public static boolean isExtended(final VertexFormat f) {
        //~ return f != null && f == format;
    //~ }

    //~ /** Mesher thread: the block (or fluid's legacy block) being meshed. */
    //~ public static void setBlock(final BlockState state) {
        //~ if (packed) {
            //~ final Integer code = codes.get(state);
            //~ CURRENT.get()[0] = code == null ? 0 : code;
            //~ return;
        //~ }
        //~ if (!extended) {
            //~ return;
        //~ }
        //~ final Integer id = ids.get(state);
        //~ CURRENT.get()[0] = id == null ? -1 : id;
    //~ }

    //~ public static int extraElements() {
        //~ return extraElements;
    //~ }

    //~ /**
     //~ * BufferBuilder.endVertex, extended format, all vanilla elements written: our elements start
     //~ * at {@code elementByte}. {@code vertexIndex} counts finished vertices; {@code quads} is true
     //~ * for GL_QUADS builders.
     //~ */
    //~ public static void fill(final ByteBuffer buffer, final int elementByte, final int vertexIndex, final boolean quads) {
        //~ final int start = elementByte - BASE;
        //~ final short id = (short) CURRENT.get()[0];
        //~ buffer.putShort(start + BASE, id);
        //~ buffer.putShort(start + BASE + 2, (short) 0);
        //~ buffer.putShort(start + BASE + 4, (short) 0);
        //~ buffer.putShort(start + BASE + 6, (short) 0);
        //~ final int mid = midOffset;
        //~ final int tan = tangentOffset;
        //~ if (mid >= 0) {
            //~ buffer.putFloat(start + mid, buffer.getFloat(start + 16));
            //~ buffer.putFloat(start + mid + 4, buffer.getFloat(start + 20));
        //~ }
        //~ if (tan >= 0) {
            //~ buffer.putFloat(start + tan, 1);
            //~ buffer.putFloat(start + tan + 4, 0);
            //~ buffer.putFloat(start + tan + 8, 0);
            //~ buffer.putFloat(start + tan + 12, 1);
        //~ }
        //~ if (!quads || (vertexIndex & 3) != 3 || (mid < 0 && tan < 0)) {
            //~ return;
        //~ }
        //~ final int s = stride;
        //~ final int base = start - 3 * s;
        //~ if (base < 0) {
            //~ return;
        //~ }
        //~ if (mid >= 0) {
            //~ float u = 0;
            //~ float v = 0;
            //~ for (int i = 0; i < 4; i++) {
                //~ u += buffer.getFloat(base + i * s + 16);
                //~ v += buffer.getFloat(base + i * s + 20);
            //~ }
            //~ u *= 0.25f;
            //~ v *= 0.25f;
            //~ for (int i = 0; i < 4; i++) {
                //~ buffer.putFloat(base + i * s + mid, u);
                //~ buffer.putFloat(base + i * s + mid + 4, v);
            //~ }
        //~ }
        //~ if (tan >= 0) {
            //~ tangent(buffer, base, s, tan);
        //~ }
    //~ }

    //~ private static void tangent(final ByteBuffer b, final int base, final int s, final int tan) {
        //~ final int v1 = base + s;
        //~ final int v2 = base + 2 * s;
        //~ final float e1x = b.getFloat(v1) - b.getFloat(base);
        //~ final float e1y = b.getFloat(v1 + 4) - b.getFloat(base + 4);
        //~ final float e1z = b.getFloat(v1 + 8) - b.getFloat(base + 8);
        //~ final float e2x = b.getFloat(v2) - b.getFloat(base);
        //~ final float e2y = b.getFloat(v2 + 4) - b.getFloat(base + 4);
        //~ final float e2z = b.getFloat(v2 + 8) - b.getFloat(base + 8);
        //~ final float du1 = b.getFloat(v1 + 16) - b.getFloat(base + 16);
        //~ final float dv1 = b.getFloat(v1 + 20) - b.getFloat(base + 20);
        //~ final float du2 = b.getFloat(v2 + 16) - b.getFloat(base + 16);
        //~ final float dv2 = b.getFloat(v2 + 20) - b.getFloat(base + 20);
        //~ final float det = du1 * dv2 - du2 * dv1;
        //~ final float r = Math.abs(det) < 1e-12f ? 1 : 1 / det;
        //~ float tx = (e1x * dv2 - e2x * dv1) * r;
        //~ float ty = (e1y * dv2 - e2y * dv1) * r;
        //~ float tz = (e1z * dv2 - e2z * dv1) * r;
        //~ final float bx = (e2x * du1 - e1x * du2) * r;
        //~ final float by = (e2y * du1 - e1y * du2) * r;
        //~ final float bz = (e2z * du1 - e1z * du2) * r;
        //~ final float len = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
        //~ if (len < 1e-12f) {
            //~ tx = 1;
            //~ ty = 0;
            //~ tz = 0;
        //~ } else {
            //~ tx /= len;
            //~ ty /= len;
            //~ tz /= len;
        //~ }
        //~ final float nx = b.get(base + 28) / 127f;
        //~ final float ny = b.get(base + 29) / 127f;
        //~ final float nz = b.get(base + 30) / 127f;
        //~ // handedness: does cross(n, t) point along the bitangent?
        //~ final float cx = ny * tz - nz * ty;
        //~ final float cy = nz * tx - nx * tz;
        //~ final float cz = nx * ty - ny * tx;
        //~ final float w = cx * bx + cy * by + cz * bz < 0 ? -1 : 1;
        //~ for (int i = 0; i < 4; i++) {
            //~ final int at = base + i * s + tan;
            //~ b.putFloat(at, tx);
            //~ b.putFloat(at + 4, ty);
            //~ b.putFloat(at + 8, tz);
            //~ b.putFloat(at + 12, w);
        //~ }
    //~ }
    // @era:shader-end
}
