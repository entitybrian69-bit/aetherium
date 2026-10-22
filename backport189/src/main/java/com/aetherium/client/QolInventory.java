package com.aetherium.client;

import com.aetherium.qol.Qol;
import com.aetherium.qol.SortPlanner;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.ContainerDispenser;
import net.minecraft.inventory.ContainerHopper;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Inventory helpers of the Quality of life tab (1.12.2 port of the main mod's class): scroll
 * transfer, Shift-drag and the Sort button. 1.12.2's Slot.getItemStackLimit is the slot's limit
 * only, so the stack's own maximum is applied on top.
 * Every change is made with the same inventory clicks a player makes by hand (pick up, place one,
 * put back), sent through vanilla's own click method, so servers see an ordinary player.
 */
public final class QolInventory {
    private QolInventory() {
    }

    private static final int EXCLUDED = -1;
    private static final int OTHER = 0;
    private static final int PLAYER = 1;
    private static final int HOTBAR = 1;
    private static final int MAIN = 2;
    /** A sort stops after this many moves even if the planner is not done (protects against loops). */
    private static final int MAX_MOVES = 200;

    /** The running sort, or null. */
    private static Container sortMenu;
    private static int[] sortSlots;
    private static int sortMoves;
    private static int sortStalls;
    private static Slot lastDragSlot;
    private static double scrollRemainder;

    /** Joining or leaving a world: forget the running sort. */
    public static void cancel() {
        sortMenu = null;
        sortSlots = null;
        lastDragSlot = null;
    }

    // ------------------------------------------------------------------ sections

    /** True for screens the helpers work in (not the creative inventory, which clicks differently). */
    public static boolean supported(final GuiContainer screen) {
        return !((Object) screen instanceof GuiContainerCreative);
    }

    private static int section(final Container menu, final Slot slot) {
        if (slot == null || slot instanceof SlotCrafting) {
            return EXCLUDED;
        }
        final IInventory container = slot.inventory;
        if (container instanceof InventoryCrafting) {
            return EXCLUDED;
        }
        if (container instanceof InventoryPlayer) {
            final int index = containerSlot(slot);
            if (index < 0 || index >= 36) {
                return EXCLUDED; // armour and off hand
            }
            if (menu instanceof ContainerPlayer) {
                return index < 9 ? HOTBAR : MAIN;
            }
            return PLAYER;
        }
        return OTHER;
    }

    /** The slot's index inside its own container. */
    public static int containerSlot(final Slot slot) {
        return slot.getSlotIndex();
    }

    /** The container's slots. */
    public static List<Slot> slots(final Container menu) {
        return menu.inventorySlots;
    }

    private static ItemStack carried(final Minecraft mc) {
        return mc.thePlayer.inventory.getItemStack();
    }

    private static void click(final Minecraft mc, final Container menu, final int slot, final int button) {
        send(mc, menu.windowId, slot, button, false);
    }

    /** One inventory click through vanilla's own method: a pick up / place, or a Shift-click. */
    private static void send(final Minecraft mc, final int containerId, final int slot, final int button,
                             final boolean quickMove) {
        mc.playerController.windowClick(containerId, slot, button,
                quickMove ? 1 : 0, mc.thePlayer); // 1.8.9 click modes: 0 pick up, 1 Shift-click
    }

    private static boolean canMerge(final ItemStack a, final ItemStack b) {
        // 1.12.2 stacks also differ by metadata (wool colours, damage): isItemEqual compares it.
        return !Stacks.empty(a) && !Stacks.empty(b) && a.isItemEqual(b) && ItemStack.areItemStackTagsEqual(a, b);
    }

    /** A stable hash of the stack's extra data (tag or components), the last sort key. */
    private static int extraHash(final ItemStack stack) {
        return String.valueOf(stack.getTagCompound()).hashCode();
    }

    // ------------------------------------------------------------------ scroll transfer

    /**
     * Mouse wheel over a slot. Down (amount below 0) moves one item of the hovered stack to the
     * other part of the screen; up pulls one matching item from the other part into the hovered
     * stack. Returns true when the wheel was used.
     */
    public static boolean scroll(final Minecraft mc, final GuiContainer screen, final Slot hovered,
                                 final double amount) {
        if (!Qol.scrollTransfer || amount == 0.0 || hovered == null || mc.thePlayer == null || mc.playerController == null
                || !supported(screen) || !Stacks.empty(carried(mc)) || sortMenu != null) {
            return false;
        }
        final Container menu = screen.inventorySlots;
        final int from = section(menu, hovered);
        final ItemStack stack = hovered.getStack();
        if (from == EXCLUDED || Stacks.empty(stack)) {
            return false;
        }
        // Touchpads send fractions of a notch: one item per whole notch, in the latest direction.
        if (scrollRemainder != 0.0 && Math.signum(scrollRemainder) != Math.signum(amount)) {
            scrollRemainder = 0.0;
        }
        scrollRemainder += amount;
        if (Math.abs(scrollRemainder) < 1.0) {
            return true;
        }
        scrollRemainder = 0.0;
        if (amount < 0.0) {
            final Slot target = findTarget(menu, from, stack);
            if (target == null || !hovered.canTakeStack(mc.thePlayer)) {
                return true;
            }
            moveOne(mc, menu, hovered, target);
        } else {
            if (Stacks.count(stack) >= Math.min(stack.getMaxStackSize(), hovered.getItemStackLimit(stack))) {
                return true;
            }
            final Slot source = findSource(mc, menu, from, stack);
            if (source != null) {
                moveOne(mc, menu, source, hovered);
            }
        }
        return true;
    }

    /** Pick up the source stack, place one item on the target with a right click, put the rest back. */
    private static void moveOne(final Minecraft mc, final Container menu, final Slot source, final Slot target) {
        click(mc, menu, source.slotNumber, 0);
        click(mc, menu, target.slotNumber, 1);
        if (!Stacks.empty(carried(mc))) {
            click(mc, menu, source.slotNumber, 0);
        }
    }

    /** A slot in another section that can take one more of {@code stack}: a matching stack first, else an empty slot. */
    private static Slot findTarget(final Container menu, final int from, final ItemStack stack) {
        Slot empty = null;
        for (final Slot slot : slots(menu)) {
            final int s = section(menu, slot);
            if (s == EXCLUDED || s == from || !slot.isItemValid(stack)) {
                continue;
            }
            final ItemStack there = slot.getStack();
            if (Stacks.empty(there)) {
                if (empty == null) {
                    empty = slot;
                }
            } else if (canMerge(there, stack)
                    && Stacks.count(there) < Math.min(there.getMaxStackSize(), slot.getItemStackLimit(there))) {
                return slot;
            }
        }
        return empty;
    }

    /** A slot in another section holding a stack that matches {@code stack}; the smallest such stack. */
    private static Slot findSource(final Minecraft mc, final Container menu, final int to, final ItemStack stack) {
        Slot best = null;
        for (final Slot slot : slots(menu)) {
            final int s = section(menu, slot);
            if (s == EXCLUDED || s == to || !canMerge(slot.getStack(), stack) || !slot.canTakeStack(mc.thePlayer)) {
                continue;
            }
            if (best == null || Stacks.count(slot.getStack()) < Stacks.count(best.getStack())) {
                best = slot;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ shift-drag

    /** Left button pressed on {@code slot} (vanilla handles that first slot itself): a new drag starts. */
    public static void dragStart(final Slot slot) {
        lastDragSlot = slot;
    }

    /**
     * Left-button drag with Shift held and nothing on the cursor: every stack passed over is
     * quick-moved like a shift-click. Returns true when the drag was used.
     */
    public static boolean shiftDrag(final Minecraft mc, final GuiContainer screen, final Slot slot) {
        if (!Qol.shiftDrag || slot == null || mc.thePlayer == null || mc.playerController == null || !supported(screen)
                || !Stacks.empty(carried(mc)) || sortMenu != null) {
            return false;
        }
        if (slot == lastDragSlot) {
            return true;
        }
        lastDragSlot = slot;
        if (slot.getHasStack() && slot.canTakeStack(mc.thePlayer)) {
            send(mc, screen.inventorySlots.windowId, slot.slotNumber, 0, true);
        }
        return true;
    }

    // ------------------------------------------------------------------ sort

    /** True when the menu has a container part the Sort button can sort (chests, shulker boxes, hoppers, dispensers). */
    public static boolean sortableContainer(final Container menu) {
        return menu instanceof ContainerChest || menu instanceof ContainerDispenser
                || menu instanceof ContainerHopper;
    }

    /** True while a sort is running. */
    public static boolean sorting() {
        return sortMenu != null;
    }

    /** Starts sorting the container part ({@code player} false) or the player's main inventory (true). */
    public static void startSort(final Minecraft mc, final GuiContainer screen, final boolean player) {
        if (!Qol.sortButton || mc.thePlayer == null || !supported(screen) || sortMenu != null) {
            return;
        }
        final Container menu = screen.inventorySlots;
        final List<Integer> slots = new ArrayList<Integer>();
        for (final Slot slot : slots(menu)) {
            final boolean inventory = slot.inventory instanceof InventoryPlayer;
            if (player) {
                if (inventory && containerSlot(slot) >= 9 && containerSlot(slot) < 36) {
                    slots.add(Integer.valueOf(slot.slotNumber));
                }
            } else if (!inventory && section(menu, slot) == OTHER) {
                slots.add(Integer.valueOf(slot.slotNumber));
            }
        }
        if (slots.size() < 2) {
            return;
        }
        final int[] indices = new int[slots.size()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = slots.get(i).intValue();
        }
        sortMenu = menu;
        sortSlots = indices;
        sortMoves = 0;
        sortStalls = 0;
    }

    /** Sort key in a container screen: sorts the part under the mouse. */
    public static boolean sortKey(final Minecraft mc, final GuiContainer screen, final Slot hovered) {
        if (!Qol.sortButton || hovered == null || !supported(screen)) {
            return false;
        }
        final Container menu = screen.inventorySlots;
        if (hovered.inventory instanceof InventoryPlayer) {
            startSort(mc, screen, true);
            return true;
        }
        if (sortableContainer(menu) && section(menu, hovered) == OTHER) {
            startSort(mc, screen, false);
            return true;
        }
        return false;
    }

    /** One sort move per client tick. */
    public static void tick(final Minecraft mc) {
        final Container menu = sortMenu;
        if (menu == null) {
            return;
        }
        if (mc.thePlayer == null || mc.playerController == null || mc.thePlayer.openContainer != menu || !Qol.sortButton
                || sortMoves >= MAX_MOVES || !(mc.currentScreen instanceof GuiContainer)
                || ((GuiContainer) mc.currentScreen).inventorySlots != menu) {
            cancel();
            return;
        }
        if (!Stacks.empty(carried(mc))) {
            // The player is holding something (or a click was refused): wait a little, then give up.
            if (++sortStalls > 20) {
                cancel();
            }
            return;
        }
        final MenuSlots view = new MenuSlots(menu, sortSlots);
        final SortPlanner.Move move = SortPlanner.next(view);
        if (move == null) {
            cancel();
            return;
        }
        final int from = sortSlots[move.from];
        final int to = sortSlots[move.to];
        click(mc, menu, from, 0);
        click(mc, menu, to, 0);
        if (!Stacks.empty(carried(mc))) {
            click(mc, menu, from, 0);
        }
        sortMoves++;
    }

    /** The sorted slots of a menu as the planner sees them. */
    private static final class MenuSlots implements SortPlanner.Slots {
        private final Container menu;
        private final int[] indices;

        MenuSlots(final Container menu, final int[] indices) {
            this.menu = menu;
            this.indices = indices;
        }

        private Slot slot(final int i) {
            return slots(this.menu).get(this.indices[i]);
        }

        private ItemStack stack(final int i) {
            return slot(i).getStack();
        }

        @Override
        public int size() {
            return this.indices.length;
        }

        @Override
        public boolean isEmpty(final int i) {
            return Stacks.empty(stack(i));
        }

        @Override
        public boolean sameItem(final int a, final int b) {
            return canMerge(stack(a), stack(b));
        }

        @Override
        public int count(final int i) {
            return Stacks.count(stack(i));
        }

        @Override
        public int maxCount(final int i) {
            final ItemStack stack = stack(i);
            return Math.min(stack.getMaxStackSize(), slot(i).getItemStackLimit(stack));
        }

        @Override
        public int compareItems(final int a, final int b) {
            final ItemStack x = stack(a);
            final ItemStack y = stack(b);
            int c = Integer.compare(Item.getIdFromItem(x.getItem()), Item.getIdFromItem(y.getItem()));
            if (c == 0) {
                c = Integer.compare(x.getMetadata(), y.getMetadata());
            }
            if (c == 0 && !canMerge(x, y)) {
                c = x.getDisplayName().compareTo(y.getDisplayName());
                if (c == 0) {
                    c = Integer.compare(extraHash(x), extraHash(y));
                }
            }
            return c;
        }
    }
}
