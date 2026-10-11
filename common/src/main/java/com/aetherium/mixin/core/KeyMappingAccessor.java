package com.aetherium.mixin.core;

// @era:zoom-begin none
import net.minecraft.client.KeyMapping;

import org.spongepowered.asm.mixin.Mixin;

/** Zoom is 1.16.5-only for now; the plugin skips this mixin elsewhere. */
@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
}
// @era:zoom-else v16
//~ import java.util.Map;

//~ import net.minecraft.client.KeyMapping;

//~ import org.spongepowered.asm.mixin.Mixin;
//~ import org.spongepowered.asm.mixin.gen.Accessor;

//~ /** The Controls screen's category order table, so the zoom keys can have their own "Aetherium" category. */
//~ @Mixin(KeyMapping.class)
//~ public interface KeyMappingAccessor {
    //~ @Accessor("CATEGORY_SORT_ORDER")
    //~ static Map<String, Integer> aetherium$categorySortOrder() {
        //~ throw new AssertionError("mixin not applied");
    //~ }
//~ }
// @era:zoom-end
