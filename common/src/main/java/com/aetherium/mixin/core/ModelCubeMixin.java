package com.aetherium.mixin.core;

import com.aetherium.perf.ModelCubeData;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.aetherium.perf.ModelCubes;
//~ import net.minecraft.client.model.geom.ModelPart;
//~ import org.spongepowered.asm.mixin.Unique;
// @era:qol16-end

/**
 * A model cube's faces, packed on first use for the fast model path (1.16.5). Render thread
 * only. Applied only where {@code Capabilities.STREAMING} is true.
 */
@Mixin(targets = "net.minecraft.client.model.geom.ModelPart$Cube")
public abstract class ModelCubeMixin implements ModelCubeData {
    // @era:qol16-begin none
    @Override
    public float[] aetheriumFaces() {
        return null;
    }
    // @era:qol16-else v16
    //~ @Unique
    //~ private float[] aetherium$faces;

    //~ @Unique
    //~ private boolean aetherium$packed;

    //~ @Override
    //~ public float[] aetheriumFaces() {
        //~ if (!this.aetherium$packed) {
            //~ this.aetherium$faces = ModelCubes.pack(this, ModelPart.Cube.class);
            //~ this.aetherium$packed = true;
        //~ }
        //~ return this.aetherium$faces;
    //~ }
    // @era:qol16-end
}
