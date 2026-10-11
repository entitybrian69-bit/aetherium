package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import java.util.BitSet;
//~ import net.minecraft.core.Direction;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:qol16-end

/**
 * Less garbage from chunk meshing (1.16.5). For every block it meshes, {@code ModelBlockRenderer}
 * copies {@code Direction.values()} twice and allocates a {@code BitSet}, and {@code calculateShape}
 * copies the directions again for every face. The mesh threads produce that garbage, but the
 * collection pauses (the serial collector on Android launchers) stop the render thread too.
 *
 * <p>The directions come from one shared array (only iterated, never written), and each thread
 * reuses one {@code BitSet}: {@code calculateShape} sets both bits it uses for every face before
 * they are read, so a reused set behaves exactly like a new one. Meshing is not re-entrant.</p>
 *
 * <p>Applied only where {@code Capabilities.STREAMING} is true.</p>
 */
@Mixin(targets = "net.minecraft.client.renderer.block.ModelBlockRenderer")
public abstract class MeshAllocMixin {
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ @Unique
    //~ private static final Direction[] AETHERIUM$DIRECTIONS = Direction.values();

    //~ @Unique
    //~ private static final ThreadLocal<BitSet> AETHERIUM$BITS = new ThreadLocal<BitSet>();

    //~ @Redirect(method = {"tesselateWithAO", "tesselateWithoutAO", "calculateShape"}, at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/core/Direction;values()[Lnet/minecraft/core/Direction;"), require = 0)
    //~ private Direction[] aetherium$directions() {
        //~ return AETHERIUM$DIRECTIONS;
    //~ }

    //~ @Redirect(method = {"tesselateWithAO", "tesselateWithoutAO"}, at = @At(value = "NEW", target = "java/util/BitSet"),
            //~ require = 0)
    //~ private BitSet aetherium$bits(final int size) {
        //~ BitSet bits = AETHERIUM$BITS.get();
        //~ if (bits == null) {
            //~ bits = new BitSet(size);
            //~ AETHERIUM$BITS.set(bits);
        //~ }
        //~ return bits;
    //~ }
    // @era:qol16-end
}
