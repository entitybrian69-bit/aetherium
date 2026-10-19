package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
import com.aetherium.client.QolContainerAccess;
import com.aetherium.client.QolInventory;
import com.aetherium.qol.Qol;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Scroll transfer (Quality of life, every version): the mouse wheel over a slot in a container
 * screen moves single items ({@link com.aetherium.client.QolInventory#scroll}). Hooked where the
 * mouse handler passes the wheel to the open screen; anything else scrolls as in vanilla.
 */
@Mixin(targets = "net.minecraft.client.MouseHandler")
public abstract class QolScrollMixin {
    // onScroll(long, double, double) has the same arguments on every version, while the screen call
    // inside it changed shape (three doubles before 1.20.2, four after); so the hook is the method head,
    // limited to a container screen under the mouse.
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$scrollTransfer(final long window, final double xOffset, final double yOffset,
                                          final CallbackInfo ci) {
        if (!Qol.scrollTransfer || yOffset == 0.0) {
            return;
        }
        final Minecraft mc = Minecraft.getInstance();
        final Screen screen = com.aetherium.client.ClientHooks.currentScreen(mc);
        if (screen instanceof AbstractContainerScreen && screen instanceof QolContainerAccess) {
            final Object hovered = ((QolContainerAccess) screen).aetherium$hoveredSlot();
            if (hovered instanceof Slot
                    && QolInventory.scroll(mc, (AbstractContainerScreen<?>) screen, (Slot) hovered, yOffset)) {
                ci.cancel();
            }
        }
    }
}
