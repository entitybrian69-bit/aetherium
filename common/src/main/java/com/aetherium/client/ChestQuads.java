package com.aetherium.client;

// @era:boost16-begin none
/** Batched chests exist only for Minecraft 1.16.5. */
final class ChestQuads {
    private ChestQuads() {
    }
}
// @era:boost16-else v16
//~ import com.mojang.blaze3d.vertex.BufferBuilder;
//~ import com.mojang.blaze3d.vertex.VertexConsumer;
//~ import net.minecraft.client.renderer.texture.TextureAtlasSprite;

//~ /**
 //~ * Batched chests (1.17.2): collects the quads of a chest model (as the model writes them for
 //~ * block-entity drawing) and, once the whole model went through, writes them into a chunk
 //~ * section's terrain buffer with the texture moved into the block atlas and block-entity shading
 //~ * baked into the colour. Nothing reaches the section buffer if the model fails half way. One
 //~ * instance per bake (mesh worker).
 //~ */
//~ public final class ChestQuads implements VertexConsumer {
    //~ private static final int FLOATS = 12;
    //~ private final TextureAtlasSprite sprite;
    //~ private float[] data = new float[FLOATS * 96];
    //~ private int[] lights = new int[96];
    //~ private int count;

    //~ public ChestQuads(final TextureAtlasSprite sprite) {
        //~ this.sprite = sprite;
    //~ }

    //~ @Override
    //~ public void vertex(final float x, final float y, final float z, final float red, final float green, final float blue,
                       //~ final float alpha, final float u, final float v, final int overlay, final int light,
                       //~ final float nx, final float ny, final float nz) {
        //~ if (this.count == this.lights.length) {
            //~ this.data = java.util.Arrays.copyOf(this.data, this.data.length * 2);
            //~ this.lights = java.util.Arrays.copyOf(this.lights, this.lights.length * 2);
        //~ }
        //~ final int o = this.count * FLOATS;
        //~ final float shade = ChestBaking.shade(nx, ny, nz);
        //~ this.data[o] = x;
        //~ this.data[o + 1] = y;
        //~ this.data[o + 2] = z;
        //~ this.data[o + 3] = red * shade;
        //~ this.data[o + 4] = green * shade;
        //~ this.data[o + 5] = blue * shade;
        //~ this.data[o + 6] = alpha;
        //~ this.data[o + 7] = this.sprite.getU(u * 16.0);
        //~ this.data[o + 8] = this.sprite.getV(v * 16.0);
        //~ this.data[o + 9] = nx;
        //~ this.data[o + 10] = ny;
        //~ this.data[o + 11] = nz;
        //~ this.lights[this.count] = light;
        //~ this.count++;
    //~ }

    //~ /**
     //~ * Whether the model produced exactly {@code vertices} vertices (all faces of all its boxes; a
     //~ * part skipped by mob detail levels on another thread would leave a hole, so then nothing is
     //~ * baked).
     //~ */
    //~ public boolean complete(final int vertices) {
        //~ return this.count == vertices;
    //~ }

    //~ /** Writes the collected quads into {@code target} (a section buffer in the block format). */
    //~ public void flush(final BufferBuilder target) {
        //~ for (int i = 0; i < this.count; i++) {
            //~ final int o = i * FLOATS;
            //~ target.vertex(this.data[o], this.data[o + 1], this.data[o + 2], this.data[o + 3], this.data[o + 4],
                    //~ this.data[o + 5], this.data[o + 6], this.data[o + 7], this.data[o + 8], 655360, this.lights[i],
                    //~ this.data[o + 9], this.data[o + 10], this.data[o + 11]);
        //~ }
    //~ }

    //~ private static IllegalStateException unexpected() {
        //~ return new IllegalStateException("chest model wrote a vertex element by element");
    //~ }

    //~ @Override
    //~ public VertexConsumer vertex(final double x, final double y, final double z) {
        //~ throw unexpected();
    //~ }

    //~ @Override
    //~ public VertexConsumer color(final int red, final int green, final int blue, final int alpha) {
        //~ throw unexpected();
    //~ }

    //~ @Override
    //~ public VertexConsumer uv(final float u, final float v) {
        //~ throw unexpected();
    //~ }

    //~ @Override
    //~ public VertexConsumer overlayCoords(final int u, final int v) {
        //~ throw unexpected();
    //~ }

    //~ @Override
    //~ public VertexConsumer uv2(final int u, final int v) {
        //~ throw unexpected();
    //~ }

    //~ @Override
    //~ public VertexConsumer normal(final float x, final float y, final float z) {
        //~ throw unexpected();
    //~ }

    //~ @Override
    //~ public void endVertex() {
        //~ throw unexpected();
    //~ }
//~ }
// @era:boost16-end
