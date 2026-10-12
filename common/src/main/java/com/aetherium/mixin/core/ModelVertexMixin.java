package com.aetherium.mixin.core;

import com.aetherium.perf.ModelVertexData;

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

/**
 * A model vertex for the fast model path (1.16.5..1.20.6): the position divided by 16 exactly as
 * {@code ModelPart.compile} divides it, and the texture coordinates. Applied only where
 * {@code Capabilities.FAST_MODELS} is true.
 */
@Mixin(targets = "net.minecraft.client.model.geom.ModelPart$Vertex")
public abstract class ModelVertexMixin implements ModelVertexData {
    // @era:model-fast-begin none
    @Override
    public float aetheriumX() {
        return 0.0f;
    }

    @Override
    public float aetheriumY() {
        return 0.0f;
    }

    @Override
    public float aetheriumZ() {
        return 0.0f;
    }

    @Override
    public float aetheriumU() {
        return 0.0f;
    }

    @Override
    public float aetheriumV() {
        return 0.0f;
    }
    // @era:model-fast-else v16|mojang|joml|trusted
    //~ @Shadow
    //~ @Final
    //~ public Vector3f pos;

    //~ @Shadow
    //~ @Final
    //~ public float u;

    //~ @Shadow
    //~ @Final
    //~ public float v;

    //~ @Override
    //~ public float aetheriumX() {
        //~ return this.pos.x() / 16.0F;
    //~ }

    //~ @Override
    //~ public float aetheriumY() {
        //~ return this.pos.y() / 16.0F;
    //~ }

    //~ @Override
    //~ public float aetheriumZ() {
        //~ return this.pos.z() / 16.0F;
    //~ }

    //~ @Override
    //~ public float aetheriumU() {
        //~ return this.u;
    //~ }

    //~ @Override
    //~ public float aetheriumV() {
        //~ return this.v;
    //~ }
    // @era:model-fast-end
}
