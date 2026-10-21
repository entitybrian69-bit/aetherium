package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:boost16-begin none
// @era:boost16-else v16
//~ import java.util.stream.Stream;
//~ import net.minecraft.client.renderer.Sheets;
//~ import net.minecraft.client.renderer.texture.TextureAtlas;
//~ import net.minecraft.resources.ResourceLocation;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.ModifyVariable;
// @era:boost16-end

/**
 * Batched chests (1.17.2, Minecraft 1.16.5): also stitches the chest and ender chest textures
 * into the block atlas, so a chest meshed into its chunk section draws with the terrain texture.
 * The chest atlas keeps its own copies for chests drawn the normal way.
 */
@Mixin(targets = "net.minecraft.client.renderer.texture.TextureAtlas")
public abstract class TuningChestAtlasMixin {
// @era:boost16-begin none
// @era:boost16-else v16
    //~ @Shadow
    //~ public abstract ResourceLocation location();

    //~ @ModifyVariable(method = "prepareToStitch", at = @At("HEAD"), argsOnly = true, require = 0)
    //~ private Stream<ResourceLocation> aetherium$chestTextures(final Stream<ResourceLocation> textures) {
        //~ if (!TextureAtlas.LOCATION_BLOCKS.equals(this.location())) {
            //~ return textures;
        //~ }
        //~ return Stream.concat(textures, Stream.of(
                //~ Sheets.CHEST_LOCATION.texture(), Sheets.CHEST_LOCATION_LEFT.texture(), Sheets.CHEST_LOCATION_RIGHT.texture(),
                //~ Sheets.CHEST_TRAP_LOCATION.texture(), Sheets.CHEST_TRAP_LOCATION_LEFT.texture(),
                //~ Sheets.CHEST_TRAP_LOCATION_RIGHT.texture(), Sheets.CHEST_XMAS_LOCATION.texture(),
                //~ Sheets.CHEST_XMAS_LOCATION_LEFT.texture(), Sheets.CHEST_XMAS_LOCATION_RIGHT.texture(),
                //~ Sheets.ENDER_CHEST_LOCATION.texture()));
    //~ }
// @era:boost16-end
}
