package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import org.spongepowered.asm.mixin.gen.Accessor;
// @era:qol16-end

/** The normal matrix entries for the fast model path (1.16.5). Applied only where {@code Capabilities.STREAMING} is true. */
@Mixin(targets = "com.mojang.math.Matrix3f")
public interface Matrix3fAccessor {
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ @Accessor("m00")
    //~ float aetherium$m00();

    //~ @Accessor("m01")
    //~ float aetherium$m01();

    //~ @Accessor("m02")
    //~ float aetherium$m02();

    //~ @Accessor("m10")
    //~ float aetherium$m10();

    //~ @Accessor("m11")
    //~ float aetherium$m11();

    //~ @Accessor("m12")
    //~ float aetherium$m12();

    //~ @Accessor("m20")
    //~ float aetherium$m20();

    //~ @Accessor("m21")
    //~ float aetherium$m21();

    //~ @Accessor("m22")
    //~ float aetherium$m22();
    // @era:qol16-end
}
