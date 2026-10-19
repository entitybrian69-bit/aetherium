package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:q-inv-begin none
// @era:q-inv-else v16
//~ import com.aetherium.client.QolContainerAccess;
//~ import com.aetherium.client.QolInventory;
//~ import com.aetherium.qol.Qol;
//~ import net.minecraft.client.Minecraft;
//~ import net.minecraft.client.gui.screens.Screen;
//~ import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
//~ import net.minecraft.world.inventory.Slot;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:q-inv-end

/**
 * Scroll transfer (Quality of life, Minecraft 1.16.5): the mouse wheel over a slot in a container
 * screen moves single items ({@link com.aetherium.client.QolInventory#scroll}). Hooked where the
 * mouse handler passes the wheel to the open screen; anything else scrolls as in vanilla.
 */
@Mixin(targets = "net.minecraft.client.MouseHandler")
public abstract class QolScrollMixin {
    // @era:q-inv-begin none
    // @era:q-inv-else v16
    //~ @Inject(method = "onScroll", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDD)Z"), cancellable = true, require = 0)
    //~ private void aetherium$scrollTransfer(final long window, final double xOffset, final double yOffset,
                                          //~ final CallbackInfo ci) {
        //~ if (!Qol.scrollTransfer) {
            //~ return;
        //~ }
        //~ final Minecraft mc = Minecraft.getInstance();
        //~ final Screen screen = mc.screen;
        //~ if (screen instanceof AbstractContainerScreen && screen instanceof QolContainerAccess) {
            //~ final Object hovered = ((QolContainerAccess) screen).aetherium$hoveredSlot();
            //~ if (hovered instanceof Slot
                    //~ && QolInventory.scroll(mc, (AbstractContainerScreen<?>) screen, (Slot) hovered, yOffset)) {
                //~ ci.cancel();
            //~ }
        //~ }
    //~ }
    // @era:q-inv-end
}
