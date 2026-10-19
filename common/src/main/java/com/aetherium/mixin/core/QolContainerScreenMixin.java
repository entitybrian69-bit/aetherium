package com.aetherium.mixin.core;

import com.aetherium.client.QolContainerAccess;
import org.spongepowered.asm.mixin.Mixin;
// @era:q-inv-begin v16
import com.aetherium.client.McCanvas;
import com.aetherium.client.QolClient;
import com.aetherium.client.QolInventory;
import com.aetherium.client.UiSounds;
import com.aetherium.gui.GuiCanvas;
import com.aetherium.gui.UiSound;
import com.aetherium.qol.Qol;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
// @era:q-inv-else none
// @era:q-inv-end

/**
 * Inventory helpers (Quality of life, Minecraft 1.16.5) in every container screen: the Sort
 * buttons, Shift-drag and the Sort key. Scrolling over slots is hooked in {@code QolScrollMixin}
 * because this screen class has no scroll method of its own. All work is done by
 * {@link com.aetherium.client.QolInventory} with ordinary inventory clicks.
 */
@Mixin(targets = "net.minecraft.client.gui.screens.inventory.AbstractContainerScreen")
public abstract class QolContainerScreenMixin implements QolContainerAccess {
    // @era:q-inv-begin v16
    private static final int SIZE = 11;

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    @Shadow
    protected int imageWidth;

    @Shadow
    protected int inventoryLabelY;

    @Shadow
    protected Slot hoveredSlot;

    @Shadow
    private Slot findSlot(final double x, final double y) {
        throw new AssertionError();
    }

    @Override
    public Object aetherium$hoveredSlot() {
        return this.hoveredSlot;
    }

    private AbstractContainerScreen<?> aetherium$self() {
        return (AbstractContainerScreen<?>) (Object) this;
    }

    // ------------------------------------------------------------------ sort buttons

    /** True when the container part (chest, shulker box, hopper, dispenser) gets a Sort button. */
    private boolean aetherium$hasContainerButton() {
        return Qol.sortButton && QolInventory.supported(aetherium$self())
                && QolInventory.sortableContainer(aetherium$self().getMenu());
    }

    private boolean aetherium$hasPlayerButton() {
        if (!Qol.sortButton || !QolInventory.supported(aetherium$self())) {
            return false;
        }
        for (final Slot slot : aetherium$self().getMenu().slots) {
            if (slot.container instanceof Inventory && slot.getContainerSlot() >= 9 && slot.getContainerSlot() < 36) {
                return true;
            }
        }
        return false;
    }

    private int aetherium$buttonX() {
        return this.leftPos + this.imageWidth - SIZE - 6;
    }

    private int aetherium$containerButtonY() {
        return this.topPos + 4;
    }

    private int aetherium$playerButtonY() {
        final AbstractContainerMenu menu = aetherium$self().getMenu();
        // The player's own inventory screen has no "Inventory" label; its top-right corner is free.
        return menu instanceof InventoryMenu ? this.topPos + 4 : this.topPos + this.inventoryLabelY - 2;
    }

    private static boolean aetherium$inside(final double mx, final double my, final int x, final int y) {
        return mx >= x && mx < x + SIZE && my >= y && my < y + SIZE;
    }

    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void aetherium$drawSortButtons(final PoseStack pose, final int mouseX, final int mouseY,
                                           final float partialTick, final CallbackInfo ci) {
        if (!Qol.sortButton) {
            return;
        }
        final boolean container = aetherium$hasContainerButton();
        final boolean player = aetherium$hasPlayerButton();
        if (!container && !player) {
            return;
        }
        final GuiCanvas canvas = McCanvas.INSTANCE.begin(pose);
        final int x = aetherium$buttonX();
        if (container) {
            aetherium$drawButton(canvas, x, aetherium$containerButtonY(), mouseX, mouseY);
        }
        if (player) {
            aetherium$drawButton(canvas, x, aetherium$playerButtonY(), mouseX, mouseY);
        }
    }

    private static void aetherium$drawButton(final GuiCanvas canvas, final int x, final int y, final int mouseX,
                                             final int mouseY) {
        final boolean hover = aetherium$inside(mouseX, mouseY, x, y);
        final boolean busy = QolInventory.sorting();
        canvas.fill(x, y, x + SIZE, y + SIZE, 0xFF373737);
        canvas.fill(x + 1, y + 1, x + SIZE - 1, y + SIZE - 1, busy ? 0xFF5E8F5E : (hover ? 0xFFA8A8A8 : 0xFF8B8B8B));
        // Three bars getting shorter: "sorted".
        canvas.fill(x + 3, y + 3, x + 8, y + 4, 0xFFFFFFFF);
        canvas.fill(x + 3, y + 5, x + 7, y + 6, 0xFFFFFFFF);
        canvas.fill(x + 3, y + 7, x + 6, y + 8, 0xFFFFFFFF);
    }

    // ------------------------------------------------------------------ input

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$clicked(final double mouseX, final double mouseY, final int button,
                                   final CallbackInfoReturnable<Boolean> cir) {
        if (button != 0) {
            return;
        }
        if (Qol.sortButton) {
            final Minecraft mc = Minecraft.getInstance();
            final int x = aetherium$buttonX();
            final boolean container = aetherium$hasContainerButton()
                    && aetherium$inside(mouseX, mouseY, x, aetherium$containerButtonY());
            final boolean player = !container && aetherium$hasPlayerButton()
                    && aetherium$inside(mouseX, mouseY, x, aetherium$playerButtonY());
            if (container || player) {
                UiSounds.play(mc, UiSound.CLICK);
                QolInventory.startSort(mc, aetherium$self(), player);
                cir.setReturnValue(Boolean.TRUE);
                return;
            }
        }
        if (Qol.shiftDrag) {
            QolInventory.dragStart(findSlot(mouseX, mouseY));
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$dragged(final double mouseX, final double mouseY, final int button, final double dragX,
                                   final double dragY, final CallbackInfoReturnable<Boolean> cir) {
        if (!Qol.shiftDrag || button != 0 || !Screen.hasShiftDown()) {
            return;
        }
        if (QolInventory.shiftDrag(Minecraft.getInstance(), aetherium$self(), findSlot(mouseX, mouseY))) {
            cir.setReturnValue(Boolean.TRUE);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$sortKey(final int key, final int scanCode, final int modifiers,
                                   final CallbackInfoReturnable<Boolean> cir) {
        if (!Qol.sortButton) {
            return;
        }
        final KeyMapping sort = QolClient.sortKey();
        if (sort != null && sort.matches(key, scanCode)
                && QolInventory.sortKey(Minecraft.getInstance(), aetherium$self(), this.hoveredSlot)) {
            cir.setReturnValue(Boolean.TRUE);
        }
    }
    // @era:q-inv-else none
    //~ @Override
    //~ public Object aetherium$hoveredSlot() {
        //~ return null;
    //~ }
    // @era:q-inv-end
}
