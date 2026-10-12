package com.aetherium.mixin.core;

import com.aetherium.perf.ModelCubeData;

import org.spongepowered.asm.mixin.Mixin;
// @era:model-fast-begin none
// @era:model-fast-else v16|mojang|joml|trusted
//~ import com.aetherium.perf.ModelCubes;
//~ import net.minecraft.client.model.geom.ModelPart;
//~ import org.spongepowered.asm.mixin.Unique;
// @era:model-fast-end

/**
 * A model cube's faces, packed on first use for the fast model path (1.16.5..1.20.6). Render thread
 * only. Applied only where {@code Capabilities.FAST_MODELS} is true.
 */
@Mixin(targets = "net.minecraft.client.model.geom.ModelPart$Cube")
public abstract class ModelCubeMixin implements ModelCubeData {
    // @era:model-fast-begin none
    @Override
    public float[] aetheriumFaces() {
        return null;
    }
    // @era:model-fast-else v16|mojang|joml|trusted
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
    // @era:model-fast-end
}
