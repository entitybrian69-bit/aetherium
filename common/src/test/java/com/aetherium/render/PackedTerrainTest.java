package com.aetherium.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Packed terrain (1.17.2): 32-byte block vertices to 20 bytes, exactly where it matters. */
final class PackedTerrainTest {

    private static ByteBuffer vertex(final float x, final float y, final float z, final int color, final float u,
                                     final float v, final int light) {
        final ByteBuffer b = ByteBuffer.allocateDirect(PackedTerrain.SOURCE_STRIDE).order(ByteOrder.nativeOrder());
        b.putFloat(0, x).putFloat(4, y).putFloat(8, z).putInt(12, color).putFloat(16, u).putFloat(20, v)
                .putInt(24, light).putInt(28, 0x7F00);
        return b;
    }

    @Test
    @DisplayName("packed terrain: positions on the 1/16 grid survive exactly; colour and light are copied")
    void roundTrip() {
        final ByteBuffer dst = ByteBuffer.allocateDirect(PackedTerrain.STRIDE).order(ByteOrder.nativeOrder());
        assertTrue(PackedTerrain.pack(vertex(3.0625f, 15.5f, 0f, 0xFF112233, 0.25f, 0.75f, 0x00F000F0), 1,
                16f, 32f, 112f, dst));
        assertEquals(3.0625f + 16f, PackedTerrain.unpackPosition(dst.getShort(0)), 0.0);
        assertEquals(15.5f + 32f, PackedTerrain.unpackPosition(dst.getShort(2)), 0.0);
        assertEquals(112f, PackedTerrain.unpackPosition(dst.getShort(4)), 0.0);
        assertEquals(0xFF112233, dst.getInt(PackedTerrain.OFFSET_COLOR));
        assertEquals(0.25f * 32768f, dst.getShort(PackedTerrain.OFFSET_UV0), 0.0);
        assertEquals(0.75f * 32768f, dst.getShort(PackedTerrain.OFFSET_UV0 + 2), 0.0);
        assertEquals(0x00F000F0, dst.getInt(PackedTerrain.OFFSET_UV2));
        assertEquals(0, dst.position(), "buffers keep their position");
    }

    @Test
    @DisplayName("packed terrain: values outside the format are refused (the caller keeps vanilla vertices)")
    void outOfRange() {
        final ByteBuffer dst = ByteBuffer.allocateDirect(PackedTerrain.STRIDE).order(ByteOrder.nativeOrder());
        assertFalse(PackedTerrain.pack(vertex(0f, 0f, 0f, 0, 0f, 0f, 0), 1, 200f, 0f, 0f, dst));
        assertFalse(PackedTerrain.pack(vertex(0f, Float.NaN, 0f, 0, 0f, 0f, 0), 1, 0f, 0f, 0f, dst));
        assertFalse(PackedTerrain.pack(vertex(0f, 0f, 0f, 0, 1.5f, 0f, 0), 1, 0f, 0f, 0f, dst), "uv outside the atlas");
        assertEquals(32767, PackedTerrain.uv(1.0f), "uv 1.0 clamps to the largest short");
        assertEquals(-1, PackedTerrain.uv(-0.5f));
        assertEquals(Integer.MIN_VALUE, PackedTerrain.position(-64.5f));
        assertEquals(-32768, PackedTerrain.position(-64f));
    }

    @Test
    @DisplayName("packed terrain: a vertex slightly off the grid does not snap across a block face")
    void gridSide() {
        final int q = PackedTerrain.position(1.0f - 0.003f);
        assertTrue(PackedTerrain.unpackPosition(q) < 1.0f, "stays below the face it was below");
        assertEquals(1.0f, PackedTerrain.unpackPosition(PackedTerrain.position(1.0f + 1e-6f)), 0.0);
    }
}
