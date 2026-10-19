package com.aetherium.client;

import net.minecraft.client.Minecraft;

import com.aetherium.qol.Qol;
import com.aetherium.qol.SortPlanner;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
// @era:q-inv-begin c205|t17|v16
import net.minecraft.world.inventory.ClickType;
// @era:q-inv-else c261
//~ import net.minecraft.world.inventory.ContainerInput;
// @era:q-inv-end
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Inventory helpers of the Quality of life tab: scroll transfer, Shift-drag and the Sort button.
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
    private static AbstractContainerMenu sortMenu;
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
    public static boolean supported(final AbstractContainerScreen<?> screen) {
        return !((Object) screen instanceof CreativeModeInventoryScreen);
    }

    private static int section(final AbstractContainerMenu menu, final Slot slot) {
        if (slot == null || slot instanceof ResultSlot) {
            return EXCLUDED;
        }
        final Container container = slot.container;
        if (container instanceof CraftingContainer) {
            return EXCLUDED;
        }
        if (container instanceof Inventory) {
            final int index = containerSlot(slot);
            if (index < 0 || index >= 36) {
                return EXCLUDED; // armour and off hand
            }
            if ((Object) menu instanceof InventoryMenu) {
                return index < 9 ? HOTBAR : MAIN;
            }
            return PLAYER;
        }
        return OTHER;
    }

    /** The slot's index inside its own container (Slot keeps it private on this version). */
    public static int containerSlot(final Slot slot) {
        return (Object) slot instanceof QolSlotAccess ? ((QolSlotAccess) (Object) slot).aetherium$containerSlot() : -1;
    }

    /** The menu's slots as a List (NonNullList is one; the cast keeps every version's stubs happy). */
    @SuppressWarnings("unchecked")
    public static List<Slot> slots(final AbstractContainerMenu menu) {
        return (List<Slot>) (Object) menu.slots;
    }

    private static ItemStack carried(final Minecraft mc) {
    // @era:q-inv-begin c205|c261|t17
        return mc.player.containerMenu.getCarried();
    // @era:q-inv-else v16
        //~ return mc.player.inventory.getCarried();
    // @era:q-inv-end
    }

    private static void click(final Minecraft mc, final AbstractContainerMenu menu, final int slot, final int button) {
        send(mc, menu.containerId, slot, button, false);
    }

    /** One inventory click through vanilla's own method: a pick up / place, or a Shift-click. */
    private static void send(final Minecraft mc, final int containerId, final int slot, final int button,
                             final boolean quickMove) {
    // @era:q-inv-begin c205|t17|v16
        mc.gameMode.handleInventoryMouseClick(containerId, slot, button,
                quickMove ? ClickType.QUICK_MOVE : ClickType.PICKUP, mc.player);
    // @era:q-inv-else c261
        //~ mc.gameMode.handleContainerInput(containerId, slot, button,
                //~ quickMove ? ContainerInput.QUICK_MOVE : ContainerInput.PICKUP, mc.player);
    // @era:q-inv-end
    }

    private static boolean canMerge(final ItemStack a, final ItemStack b) {
    // @era:q-inv-begin c205|c261
        return !a.isEmpty() && !b.isEmpty() && ItemStack.isSameItemSameComponents(a, b);
    // @era:q-inv-else t17
        //~ return !a.isEmpty() && !b.isEmpty() && ItemStack.isSameItemSameTags(a, b);
    // @era:q-inv-else v16
        //~ return !a.isEmpty() && !b.isEmpty() && a.getItem() == b.getItem() && ItemStack.tagMatches(a, b);
    // @era:q-inv-end
    }

    /** A stable hash of the stack's extra data (tag or components), the last sort key. */
    private static int extraHash(final ItemStack stack) {
    // @era:q-inv-begin c205|c261
        return String.valueOf(stack.getComponentsPatch()).hashCode();
    // @era:q-inv-else t17|v16
        //~ return String.valueOf(stack.getTag()).hashCode();
    // @era:q-inv-end
    }

    // ------------------------------------------------------------------ scroll transfer

    /**
     * Mouse wheel over a slot. Down (amount below 0) moves one item of the hovered stack to the
     * other part of the screen; up pulls one matching item from the other part into the hovered
     * stack. Returns true when the wheel was used.
     */
    public static boolean scroll(final Minecraft mc, final AbstractContainerScreen<?> screen, final Slot hovered,
                                 final double amount) {
        if (!Qol.scrollTransfer || amount == 0.0 || hovered == null || mc.player == null || mc.gameMode == null
                || !supported(screen) || !carried(mc).isEmpty() || sortMenu != null) {
            return false;
        }
        final AbstractContainerMenu menu = screen.getMenu();
        final int from = section(menu, hovered);
        final ItemStack stack = hovered.getItem();
        if (from == EXCLUDED || stack.isEmpty()) {
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
            if (target == null || !hovered.mayPickup(mc.player)) {
                return true;
            }
            moveOne(mc, menu, hovered, target);
        } else {
            if (stack.getCount() >= hovered.getMaxStackSize(stack)) {
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
    private static void moveOne(final Minecraft mc, final AbstractContainerMenu menu, final Slot source, final Slot target) {
        click(mc, menu, source.index, 0);
        click(mc, menu, target.index, 1);
        if (!carried(mc).isEmpty()) {
            click(mc, menu, source.index, 0);
        }
    }

    /** A slot in another section that can take one more of {@code stack}: a matching stack first, else an empty slot. */
    private static Slot findTarget(final AbstractContainerMenu menu, final int from, final ItemStack stack) {
        Slot empty = null;
        for (final Slot slot : slots(menu)) {
            final int s = section(menu, slot);
            if (s == EXCLUDED || s == from || !slot.mayPlace(stack)) {
                continue;
            }
            final ItemStack there = slot.getItem();
            if (there.isEmpty()) {
                if (empty == null) {
                    empty = slot;
                }
            } else if (canMerge(there, stack)
                    && there.getCount() < slot.getMaxStackSize(there)) {
                return slot;
            }
        }
        return empty;
    }

    /** A slot in another section holding a stack that matches {@code stack}; the smallest such stack. */
    private static Slot findSource(final Minecraft mc, final AbstractContainerMenu menu, final int to, final ItemStack stack) {
        Slot best = null;
        for (final Slot slot : slots(menu)) {
            final int s = section(menu, slot);
            if (s == EXCLUDED || s == to || !canMerge(slot.getItem(), stack) || !slot.mayPickup(mc.player)) {
                continue;
            }
            if (best == null || slot.getItem().getCount() < best.getItem().getCount()) {
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
    public static boolean shiftDrag(final Minecraft mc, final AbstractContainerScreen<?> screen, final Slot slot) {
        if (!Qol.shiftDrag || slot == null || mc.player == null || mc.gameMode == null || !supported(screen)
                || !carried(mc).isEmpty() || sortMenu != null) {
            return false;
        }
        if (slot == lastDragSlot) {
            return true;
        }
        lastDragSlot = slot;
        if (slot.hasItem() && slot.mayPickup(mc.player)) {
            send(mc, screen.getMenu().containerId, slot.index, 0, true);
        }
        return true;
    }

    // ------------------------------------------------------------------ sort

    /** True when the menu has a container part the Sort button can sort (chests, shulker boxes, hoppers, dispensers). */
    public static boolean sortableContainer(final AbstractContainerMenu menu) {
        return menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu || menu instanceof DispenserMenu
                || menu instanceof HopperMenu;
    }

    /** True while a sort is running. */
    public static boolean sorting() {
        return sortMenu != null;
    }

    /** Starts sorting the container part ({@code player} false) or the player's main inventory (true). */
    public static void startSort(final Minecraft mc, final AbstractContainerScreen<?> screen, final boolean player) {
        if (!Qol.sortButton || mc.player == null || !supported(screen) || sortMenu != null) {
            return;
        }
        final AbstractContainerMenu menu = screen.getMenu();
        final List<Integer> slots = new ArrayList<Integer>();
        for (final Slot slot : slots(menu)) {
            final boolean inventory = slot.container instanceof Inventory;
            if (player) {
                if (inventory && containerSlot(slot) >= 9 && containerSlot(slot) < 36) {
                    slots.add(Integer.valueOf(slot.index));
                }
            } else if (!inventory && section(menu, slot) == OTHER) {
                slots.add(Integer.valueOf(slot.index));
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
    public static boolean sortKey(final Minecraft mc, final AbstractContainerScreen<?> screen, final Slot hovered) {
        if (!Qol.sortButton || hovered == null || !supported(screen)) {
            return false;
        }
        final AbstractContainerMenu menu = screen.getMenu();
        if (hovered.container instanceof Inventory) {
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
        final AbstractContainerMenu menu = sortMenu;
        if (menu == null) {
            return;
        }
        if (mc.player == null || mc.gameMode == null || mc.player.containerMenu != menu || !Qol.sortButton
                || sortMoves >= MAX_MOVES || !(ClientHooks.currentScreen(mc) instanceof AbstractContainerScreen)
                || ((AbstractContainerScreen<?>) ClientHooks.currentScreen(mc)).getMenu() != menu) {
            cancel();
            return;
        }
        if (!carried(mc).isEmpty()) {
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
        if (!carried(mc).isEmpty()) {
            click(mc, menu, from, 0);
        }
        sortMoves++;
    }

    /** The sorted slots of a menu as the planner sees them. */
    private static final class MenuSlots implements SortPlanner.Slots {
        private final AbstractContainerMenu menu;
        private final int[] indices;

        MenuSlots(final AbstractContainerMenu menu, final int[] indices) {
            this.menu = menu;
            this.indices = indices;
        }

        private Slot slot(final int i) {
            return slots(this.menu).get(this.indices[i]);
        }

        private ItemStack stack(final int i) {
            return slot(i).getItem();
        }

        @Override
        public int size() {
            return this.indices.length;
        }

        @Override
        public boolean isEmpty(final int i) {
            return stack(i).isEmpty();
        }

        @Override
        public boolean sameItem(final int a, final int b) {
            return canMerge(stack(a), stack(b));
        }

        @Override
        public int count(final int i) {
            return stack(i).getCount();
        }

        @Override
        public int maxCount(final int i) {
            final ItemStack stack = stack(i);
            return slot(i).getMaxStackSize(stack);
        }

        @Override
        public int compareItems(final int a, final int b) {
            final ItemStack x = stack(a);
            final ItemStack y = stack(b);
            int c = Integer.compare(Item.getId(x.getItem()), Item.getId(y.getItem()));
            if (c == 0) {
                c = Integer.compare(x.getDamageValue(), y.getDamageValue());
            }
            if (c == 0 && !canMerge(x, y)) {
                c = x.getHoverName().getString().compareTo(y.getHoverName().getString());
                if (c == 0) {
                    c = Integer.compare(extraHash(x), extraHash(y));
                }
            }
            return c;
        }
    }
}
