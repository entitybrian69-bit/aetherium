package com.aetherium.mixin.core;

// @era:keys-begin string
import java.util.Map;

import net.minecraft.client.KeyMapping;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The Controls screen's category order table, so the zoom keys can have their own "Aetherium" category. */
@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("CATEGORY_SORT_ORDER")
    static Map<String, Integer> aetherium$categorySortOrder() {
        throw new AssertionError("mixin not applied");
    }
}
// @era:keys-else category
//~ import net.minecraft.client.KeyMapping;

//~ import org.spongepowered.asm.mixin.Mixin;

//~ /** 1.21.9+ registers a KeyMapping$Category instead; nothing to access. */
//~ @Mixin(KeyMapping.class)
//~ public interface KeyMappingAccessor {
//~ }
// @era:keys-end
