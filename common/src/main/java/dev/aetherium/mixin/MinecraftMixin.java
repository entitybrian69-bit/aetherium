package dev.aetherium.mixin;

import dev.aetherium.client.gui.AetheriumVideoOptionsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Swaps every vanilla VideoSettingsScreen (Options menu, pause menu, F3 shortcuts, other mods) for the purple one. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen aetherium$replaceVideoSettings(Screen screen) {
        if (screen instanceof VideoSettingsScreen vanilla) {
            return new AetheriumVideoOptionsScreen(((ScreenAccessor) vanilla).aetherium$getLastScreen());
        }
        return screen;
    }
}
