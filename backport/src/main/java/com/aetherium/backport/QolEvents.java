package com.aetherium.backport;

import com.aetherium.client.QolClient;
import com.aetherium.client.QolHud;
import com.aetherium.client.QolInventory;
import com.aetherium.client.QolWorld;
import com.aetherium.qol.Qol;
import com.aetherium.qol.QolSupport;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Forge hooks of the Quality of life tab on 1.12.2: the HUD (crosshair, hunger overlay,
 * dimension coordinates), the in-world waypoints and damage numbers, the food tooltip, the death
 * screen line, and the inventory helpers (mouse wheel transfer, Shift-drag, Sort button and key).
 * Every handler returns at once while its feature is off.
 */
public final class QolEvents {
    /** Size of the Sort button (same as the main mod). */
    private static final int SIZE = 11;

    private World lastWorld;

    @SubscribeEvent
    public void onClientTick(final TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !QolSupport.ANY) {
            return;
        }
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc.world != this.lastWorld) {
            this.lastWorld = mc.world;
            QolClient.onLevelChange();
        }
        QolClient.onClientTick(mc);
    }

    // ------------------------------------------------------------------ HUD

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onOverlayPre(final RenderGameOverlayEvent.Pre event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.CROSSHAIRS && Qol.crosshair) {
            final Minecraft mc = Minecraft.getMinecraft();
            if (QolHud.replacesCrosshair(mc)) {
                event.setCanceled(true);
                QolHud.crosshair(mc, event.getResolution());
                GlStateManager.color(1f, 1f, 1f, 1f);
            }
        }
    }

    @SubscribeEvent
    public void onOverlayPost(final RenderGameOverlayEvent.Post event) {
        final RenderGameOverlayEvent.ElementType type = event.getType();
        if (type == RenderGameOverlayEvent.ElementType.FOOD) {
            if (Qol.foodHud) {
                QolHud.food(Minecraft.getMinecraft(), event.getResolution());
                GlStateManager.color(1f, 1f, 1f, 1f);
            }
        } else if (type == RenderGameOverlayEvent.ElementType.ALL) {
            if (Qol.dimensionCoords) {
                QolClient.renderHud(event.getResolution());
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(final RenderWorldLastEvent event) {
        if (Qol.waypoints || Qol.deathPoint || Qol.damageNumbers) {
            QolWorld.render(event.getPartialTicks());
        }
    }

    @SubscribeEvent
    public void onTooltip(final ItemTooltipEvent event) {
        if (!Qol.foodTooltip) {
            return;
        }
        final String line = QolHud.foodTooltip(event.getItemStack());
        if (line != null) {
            final List<String> lines = event.getToolTip();
            lines.add(Math.min(1, lines.size()), TextFormatting.GOLD + line);
        }
    }

    // ------------------------------------------------------------------ screens

    @SubscribeEvent
    public void onDrawScreen(final GuiScreenEvent.DrawScreenEvent.Post event) {
        final GuiScreen screen = event.getGui();
        if (screen instanceof GuiGameOver) {
            if (Qol.deathPoint) {
                QolHud.deathLine(Minecraft.getMinecraft(), screen.width);
            }
        } else if (Qol.sortButton && screen instanceof GuiContainer) {
            drawSortButtons((GuiContainer) screen, event.getMouseX(), event.getMouseY());
        }
    }

    @SubscribeEvent
    public void onMouseInput(final GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!(event.getGui() instanceof GuiContainer) || !(Qol.scrollTransfer || Qol.shiftDrag || Qol.sortButton)) {
            return;
        }
        final GuiContainer screen = (GuiContainer) event.getGui();
        if (!QolInventory.supported(screen)) {
            return;
        }
        final Minecraft mc = Minecraft.getMinecraft();
        final int mouseX = Mouse.getEventX() * screen.width / mc.displayWidth;
        final int mouseY = screen.height - Mouse.getEventY() * screen.height / mc.displayHeight - 1;
        final int button = Mouse.getEventButton();
        final int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            if (Qol.scrollTransfer && QolInventory.scroll(mc, screen, slotAt(screen, mouseX, mouseY), wheel > 0 ? 1.0 : -1.0)) {
                event.setCanceled(true);
            }
            return;
        }
        if (button == 0 && Mouse.getEventButtonState()) {
            if (Qol.sortButton) {
                final int x = buttonX(screen);
                final boolean container = hasContainerButton(screen) && inside(mouseX, mouseY, x, containerButtonY(screen));
                final boolean player = !container && hasPlayerButton(screen)
                        && inside(mouseX, mouseY, x, playerButtonY(screen));
                if (container || player) {
                    QolClient.clickSound(mc);
                    QolInventory.startSort(mc, screen, player);
                    event.setCanceled(true);
                    return;
                }
            }
            if (Qol.shiftDrag) {
                QolInventory.dragStart(slotAt(screen, mouseX, mouseY));
            }
            return;
        }
        if (button == -1 && Qol.shiftDrag && Mouse.isButtonDown(0) && GuiScreen.isShiftKeyDown()
                && QolInventory.shiftDrag(mc, screen, slotAt(screen, mouseX, mouseY))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onKeyboardInput(final GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!Qol.sortButton || !(event.getGui() instanceof GuiContainer) || !Keyboard.getEventKeyState()) {
            return;
        }
        final int key = Keyboard.getEventKey();
        final int bound = QolClient.sortKey() == null ? 0 : QolClient.sortKey().getKeyCode();
        if (key == 0 || key != bound) {
            return;
        }
        final GuiContainer screen = (GuiContainer) event.getGui();
        if (QolInventory.sortKey(Minecraft.getMinecraft(), screen, screen.getSlotUnderMouse())) {
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------ sort buttons

    private static Slot slotAt(final GuiContainer screen, final int mouseX, final int mouseY) {
        final int left = screen.getGuiLeft();
        final int top = screen.getGuiTop();
        for (final Slot slot : QolInventory.slots(screen.inventorySlots)) {
            if (slot.isEnabled() && mouseX >= left + slot.xPos - 1 && mouseX < left + slot.xPos + 17
                    && mouseY >= top + slot.yPos - 1 && mouseY < top + slot.yPos + 17) {
                return slot;
            }
        }
        return null;
    }

    private static boolean hasContainerButton(final GuiContainer screen) {
        return QolInventory.sortableContainer(screen.inventorySlots);
    }

    private static boolean hasPlayerButton(final GuiContainer screen) {
        for (final Slot slot : QolInventory.slots(screen.inventorySlots)) {
            final int index = QolInventory.containerSlot(slot);
            if (slot.inventory instanceof InventoryPlayer && index >= 9 && index < 36) {
                return true;
            }
        }
        return false;
    }

    private static int buttonX(final GuiContainer screen) {
        return screen.getGuiLeft() + screen.getXSize() - SIZE - 6;
    }

    private static int containerButtonY(final GuiContainer screen) {
        return screen.getGuiTop() + 4;
    }

    private static int playerButtonY(final GuiContainer screen) {
        final Container menu = screen.inventorySlots;
        // The player's own inventory screen has no "Inventory" label; its top-right corner is free.
        // Elsewhere vanilla draws that label at ySize - 94; the button sits beside it.
        return menu instanceof ContainerPlayer ? screen.getGuiTop() + 4 : screen.getGuiTop() + screen.getYSize() - 96;
    }

    private static boolean inside(final int mx, final int my, final int x, final int y) {
        return mx >= x && mx < x + SIZE && my >= y && my < y + SIZE;
    }

    private static void drawSortButtons(final GuiContainer screen, final int mouseX, final int mouseY) {
        if (!QolInventory.supported(screen)) {
            return;
        }
        final boolean container = hasContainerButton(screen);
        final boolean player = hasPlayerButton(screen);
        if (!container && !player) {
            return;
        }
        final int x = buttonX(screen);
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        if (container) {
            drawButton(x, containerButtonY(screen), mouseX, mouseY);
        }
        if (player) {
            drawButton(x, playerButtonY(screen), mouseX, mouseY);
        }
        GlStateManager.enableDepth();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private static void drawButton(final int x, final int y, final int mouseX, final int mouseY) {
        final boolean hover = inside(mouseX, mouseY, x, y);
        final boolean busy = QolInventory.sorting();
        GuiScreen.drawRect(x, y, x + SIZE, y + SIZE, 0xFF373737);
        GuiScreen.drawRect(x + 1, y + 1, x + SIZE - 1, y + SIZE - 1, busy ? 0xFF5E8F5E : (hover ? 0xFFA8A8A8 : 0xFF8B8B8B));
        // Three bars getting shorter: "sorted".
        GuiScreen.drawRect(x + 3, y + 3, x + 8, y + 4, 0xFFFFFFFF);
        GuiScreen.drawRect(x + 3, y + 5, x + 7, y + 6, 0xFFFFFFFF);
        GuiScreen.drawRect(x + 3, y + 7, x + 6, y + 8, 0xFFFFFFFF);
    }
}
