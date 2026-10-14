package com.aetherium.mixin.core;

import java.nio.FloatBuffer;
import net.minecraft.client.renderer.ActiveRenderInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The camera matrix vanilla reads back once per frame ({@code ActiveRenderInfo.updateRenderInfo}). */
@Mixin(ActiveRenderInfo.class)
public interface CameraMatrixAccessor {

    @Accessor("MODELVIEW")
    static FloatBuffer aetherium$modelview() {
        throw new AssertionError();
    }
}
