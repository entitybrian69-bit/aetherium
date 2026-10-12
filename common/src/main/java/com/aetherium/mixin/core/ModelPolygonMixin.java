package com.aetherium.mixin.core;

import com.aetherium.perf.ModelPolygonData;

import org.spongepowered.asm.mixin.Mixin;
// @era:model-fast-begin none
// @era:model-fast-else v16|mojang
//~ import com.mojang.math.Vector3f;
//~ import org.spongepowered.asm.mixin.Final;
//~ import org.spongepowered.asm.mixin.Shadow;
// @era:model-fast-else joml|trusted
//~ import org.joml.Vector3f;
//~ import org.spongepowered.asm.mixin.Final;
//~ import org.spongepowered.asm.mixin.Shadow;
// @era:model-fast-end

/** A model face's normal for the fast model path (1.16.5..1.20.6). Applied only where {@code Capabilities.FAST_MODELS} is true. */
@Mixin(targets = "net.minecraft.client.model.geom.ModelPart$Polygon")
public abstract class ModelPolygonMixin implements ModelPolygonData {
    // @era:model-fast-begin none
    @Override
    public float aetheriumNormalX() {
        return 0.0f;
    }

    @Override
    public float aetheriumNormalY() {
        return 0.0f;
    }

    @Override
    public float aetheriumNormalZ() {
        return 0.0f;
    }
    // @era:model-fast-else v16|mojang|joml|trusted
    //~ @Shadow
    //~ @Final
    //~ public Vector3f normal;

    //~ @Override
    //~ public float aetheriumNormalX() {
        //~ return this.normal.x();
    //~ }

    //~ @Override
    //~ public float aetheriumNormalY() {
        //~ return this.normal.y();
    //~ }

    //~ @Override
    //~ public float aetheriumNormalZ() {
        //~ return this.normal.z();
    //~ }
    // @era:model-fast-end
}
