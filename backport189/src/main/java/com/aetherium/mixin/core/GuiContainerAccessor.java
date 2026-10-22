package com.aetherium.mixin.core;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Container screen layout, which 1.8.9 keeps in protected/private fields (1.12.2 has getters). */
@Mixin(GuiContainer.class)
public interface GuiContainerAccessor {

    @Accessor("guiLeft")
    int aetherium$guiLeft();

    @Accessor("guiTop")
    int aetherium$guiTop();

    @Accessor("xSize")
    int aetherium$xSize();

    @Accessor("ySize")
    int aetherium$ySize();

    /** The slot under the mouse, as vanilla found it in the last frame. */
    @Accessor("theSlot")
    Slot aetherium$hoveredSlot();
}
