package com.aetherium.client;

import com.aetherium.qol.Qol;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Mouse Tweaks (Quality of life, every version): the inventory tweaks of the Mouse Tweaks mod by
 * Ivan Molodetskikh (YaLTeR), behaving the same way with its default settings:
 * <ul>
 * <li>RMB tweak: hold right click with a stack and drag; one item is put in every slot you pass
 * over, again each time you come back to a slot (replaces vanilla's right-drag).</li>
 * <li>LMB tweak with an item: hold left click with a stack and drag over stacks of the same item to
 * pick them up; with Shift they are shift-clicked to the other inventory instead.</li>
 * <li>LMB tweak without an item: hold Shift and left click and drag; every stack passed over is
 * shift-clicked.</li>
 * <li>Wheel tweak: scroll down over a stack to push items to the other inventory, scroll up to pull
 * matching items in, one per notch (crafting outputs move one batch per notch).</li>
 * </ul>
 * Every action is an ordinary inventory click, so it works on any server.
 *
 * <p>The behaviour follows Mouse Tweaks' Main.java (BSD 3-Clause, Copyright (c) Ivan
 * Molodetskikh; see NOTICE).
 */
public final class MouseTweaks {
    private static final int LEFT = 0;
    private static final int RIGHT = 1;

    private static Object openScreen;
    private static Slot oldSelectedSlot;
    private static double accumulatedScroll;
    private static boolean canDoLmbDrag;
    private static boolean canDoRmbDrag;
    private static boolean rmbLeftOriginalSlot;

    private MouseTweaks() {
    }

    /** Vanilla hooks into the screen; the container screen mixin implements it. */
    public interface ScreenAccess {
        /** Stops vanilla's right-drag split and its release click (the RMB tweak replaces them). */
        void aetherium$disableRightDrag();
    }

    private static void updateScreen(final Object screen) {
        if (screen == openScreen) {
            return;
        }
        openScreen = screen;
        oldSelectedSlot = null;
        accumulatedScroll = 0.0;
        canDoLmbDrag = false;
        canDoRmbDrag = false;
        rmbLeftOriginalSlot = false;
    }

    private static boolean usable(final Minecraft mc, final AbstractContainerScreen<?> screen) {
        return mc.player != null && mc.gameMode != null && QolInventory.supported(screen) && !QolInventory.sorting();
    }

    // ------------------------------------------------------------------ buttons and drag

    /** Mouse button pressed over {@code slot} (may be null). Never consumes the click. */
    public static void clicked(final AbstractContainerScreen<?> screen, final Slot slot, final int button) {
        updateScreen(screen);
        final Minecraft mc = Minecraft.getInstance();
        if (!Qol.shiftDrag || !usable(mc, screen)) {
            return;
        }
        oldSelectedSlot = slot;
        final ItemStack onMouse = QolInventory.carried(mc);
        if (button == LEFT) {
            // With a stack on the mouse vanilla's left-drag split starts; that stays untouched.
            if (onMouse.isEmpty()) {
                canDoLmbDrag = true;
            }
        } else if (button == RIGHT) {
            if (onMouse.isEmpty()) {
                return;
            }
            canDoRmbDrag = true;
            rmbLeftOriginalSlot = false;
        }
    }

    /** Mouse button released. Never consumes the release. */
    public static void released(final AbstractContainerScreen<?> screen, final int button) {
        updateScreen(screen);
        if (button == LEFT) {
            canDoLmbDrag = false;
        } else if (button == RIGHT) {
            canDoRmbDrag = false;
        }
    }

    /** Mouse dragged to over {@code selected} (may be null). Never consumes the drag. */
    public static void dragged(final AbstractContainerScreen<?> screen, final Slot selected, final int button,
                               final boolean shift) {
        updateScreen(screen);
        final Minecraft mc = Minecraft.getInstance();
        if (!Qol.shiftDrag || !usable(mc, screen) || selected == oldSelectedSlot) {
            return;
        }
        final ItemStack onMouse = QolInventory.carried(mc);
        // Leaving the first slot: from now on the RMB tweak owns the drag; the first slot gets its item too.
        if (canDoRmbDrag && button == RIGHT && !rmbLeftOriginalSlot) {
            rmbLeftOriginalSlot = true;
            if ((Object) screen instanceof ScreenAccess) {
                ((ScreenAccess) (Object) screen).aetherium$disableRightDrag();
            }
            rmbMaybeClick(mc, screen, oldSelectedSlot, onMouse);
        }
        oldSelectedSlot = selected;
        if (selected == null) {
            return;
        }
        if (button == LEFT) {
            if (!canDoLmbDrag) {
                return;
            }
            final ItemStack inSlot = selected.getItem();
            if (inSlot.isEmpty()) {
                return;
            }
            if (onMouse.isEmpty()) {
                if (shift) {
                    click(mc, screen, selected, LEFT, true);
                }
            } else {
                if (!compatible(inSlot, onMouse)) {
                    return;
                }
                if (shift) {
                    click(mc, screen, selected, LEFT, true);
                } else {
                    // Merge into the slot and pick the merged stack back up, but only when all of it fits on
                    // the mouse (otherwise the slot would keep the stack and the mouse just the remainder).
                    if (onMouse.getCount() + inSlot.getCount() > selected.getMaxStackSize(onMouse)) {
                        return;
                    }
                    click(mc, screen, selected, LEFT, false);
                    if (!isCraftingOutput(selected, onMouse)) {
                        click(mc, screen, selected, LEFT, false);
                    }
                }
            }
        } else if (button == RIGHT) {
            if (canDoRmbDrag) {
                rmbMaybeClick(mc, screen, selected, onMouse);
            }
        }
    }

    private static void rmbMaybeClick(final Minecraft mc, final AbstractContainerScreen<?> screen, final Slot slot,
                                      final ItemStack onMouse) {
        if (slot == null || onMouse.isEmpty() || isCraftingOutput(slot, onMouse)) {
            return;
        }
        final ItemStack inSlot = slot.getItem();
        if (!compatible(inSlot, onMouse) || inSlot.getCount() == slot.getMaxStackSize(inSlot)) {
            return;
        }
        click(mc, screen, slot, RIGHT, false);
    }

    // ------------------------------------------------------------------ wheel

    /**
     * Mouse wheel over {@code selected}; {@code delta} below 0 is scrolling down. Returns true when the
     * scroll was over a slot and is used up (even if nothing could move, like Mouse Tweaks).
     */
    public static boolean scrolled(final AbstractContainerScreen<?> screen, final Slot selected, final double delta) {
        updateScreen(screen);
        final Minecraft mc = Minecraft.getInstance();
        if (!Qol.scrollTransfer || selected == null || !usable(mc, screen)) {
            return false;
        }
        if (accumulatedScroll != 0.0 && Math.signum(delta) != Math.signum(accumulatedScroll)) {
            accumulatedScroll = 0.0;
        }
        accumulatedScroll += delta;
        final int items = (int) accumulatedScroll;
        accumulatedScroll -= items;
        if (items == 0) {
            return true;
        }
        final List<Slot> slots = QolInventory.slots(screen.getMenu());
        int toMove = Math.abs(items);
        final boolean push = items < 0;
        final ItemStack inSlot = selected.getItem();
        if (inSlot.isEmpty()) {
            return true;
        }
        final ItemStack onMouse = QolInventory.carried(mc);

        if (isCraftingOutput(selected, onMouse)) {
            if (!compatible(inSlot, onMouse)) {
                return true;
            }
            if (onMouse.isEmpty()) {
                if (!push) {
                    return true;
                }
                while (toMove-- > 0) {
                    final ItemStack batch = selected.getItem();
                    if (batch.isEmpty()) {
                        break;
                    }
                    final List<Slot> targets = findPushSlots(slots, selected, batch, batch.getCount(), true);
                    if (targets == null) {
                        break;
                    }
                    click(mc, screen, selected, LEFT, false);
                    for (int i = 0; i < targets.size(); i++) {
                        final Slot slot = targets.get(i);
                        if (i == targets.size() - 1) {
                            click(mc, screen, slot, LEFT, false);
                        } else {
                            int times = slot.getMaxStackSize(slot.getItem()) - slot.getItem().getCount();
                            while (times-- > 0) {
                                click(mc, screen, slot, RIGHT, false);
                            }
                        }
                    }
                }
            } else {
                while (toMove-- > 0) {
                    click(mc, screen, selected, LEFT, false);
                }
            }
            return true;
        }

        if (!onMouse.isEmpty() && compatible(inSlot, onMouse)) {
            return true;
        }

        if (push) {
            if (!onMouse.isEmpty() && !selected.mayPlace(onMouse)) {
                return true;
            }
            toMove = Math.min(toMove, inSlot.getCount());
            final List<Slot> targets = findPushSlots(slots, selected, inSlot, toMove, false);
            if (targets == null || targets.isEmpty()) {
                return true;
            }
            final boolean hadOnMouse = !onMouse.isEmpty();
            // Right click picks up half (keeps furnace progress, swaps with a different stack); left click
            // when the whole stack is wanted.
            click(mc, screen, selected, onMouse.isEmpty() && inSlot.getCount() <= toMove ? LEFT : RIGHT, false);
            final ItemStack picked = QolInventory.carried(mc);
            toMove = Math.min(toMove, picked.getCount());
            for (final Slot slot : targets) {
                int times = Math.min(slot.getMaxStackSize(picked) - slot.getItem().getCount(), toMove);
                toMove -= times;
                while (times-- > 0) {
                    click(mc, screen, slot, RIGHT, false);
                }
            }
            final boolean leftover = !QolInventory.carried(mc).isEmpty();
            if (hadOnMouse || leftover) {
                click(mc, screen, selected, hadOnMouse && leftover ? RIGHT : LEFT, false);
            }
            return true;
        }

        // Pull matching items into the selected slot.
        int room = selected.getMaxStackSize(inSlot) - inSlot.getCount();
        toMove = Math.min(toMove, room);
        while (toMove > 0) {
            final Slot source = findPullSlot(slots, selected);
            if (source == null) {
                break;
            }
            final ItemStack there = source.getItem();
            final int inSource = there.getCount();
            if (isCraftingOutput(source, onMouse)) {
                // One crafted batch per notch, and only while the whole batch fits.
                if (room < inSource) {
                    break;
                }
                room -= inSource;
                toMove = Math.min(toMove - 1, room);
                if (!onMouse.isEmpty() && !selected.mayPlace(onMouse)) {
                    break;
                }
                click(mc, screen, selected, LEFT, false);
                click(mc, screen, source, LEFT, false);
                click(mc, screen, selected, LEFT, false);
                continue;
            }
            final boolean hadOnMouse = !onMouse.isEmpty();
            if (hadOnMouse && !source.mayPlace(onMouse)) {
                break;
            }
            click(mc, screen, source, onMouse.isEmpty() && there.getCount() == 1 ? LEFT : RIGHT, false);
            final int picked = QolInventory.carried(mc).getCount();
            final int fromSource = Math.min(picked, toMove);
            if (fromSource == picked) {
                click(mc, screen, selected, LEFT, false);
            } else {
                for (int i = 0; i < fromSource; i++) {
                    click(mc, screen, selected, RIGHT, false);
                }
            }
            room -= fromSource;
            toMove -= fromSource;
            final boolean leftover = !QolInventory.carried(mc).isEmpty();
            if (hadOnMouse || leftover) {
                click(mc, screen, source, hadOnMouse && leftover ? RIGHT : LEFT, false);
            }
            if (fromSource == 0) {
                break; // nothing could move; never spin
            }
        }
        return true;
    }

    private static boolean inPlayerInventory(final Slot slot) {
        return slot.container instanceof Inventory;
    }

    /** Last slot to first (Mouse Tweaks' default search order) in the other inventory with a compatible stack. */
    private static Slot findPullSlot(final List<Slot> slots, final Slot selected) {
        final ItemStack want = selected.getItem();
        final boolean findInPlayer = !inPlayerInventory(selected);
        for (int i = slots.size() - 1; i >= 0; i--) {
            final Slot slot = slots.get(i);
            if (slot == selected || inPlayerInventory(slot) != findInPlayer) {
                continue;
            }
            final ItemStack stack = slot.getItem();
            if (!stack.isEmpty() && compatible(want, stack)) {
                return slot;
            }
        }
        return null;
    }

    /** Slots in the other inventory that can take {@code count} of {@code stack}: matching stacks first, then empty ones. */
    private static List<Slot> findPushSlots(final List<Slot> slots, final Slot selected, final ItemStack stack,
                                            int count, final boolean mustDistributeAll) {
        final boolean findInPlayer = !inPlayerInventory(selected);
        final List<Slot> result = new ArrayList<Slot>();
        final List<Slot> empty = new ArrayList<Slot>();
        for (int i = 0; i < slots.size() && count > 0; i++) {
            final Slot slot = slots.get(i);
            if (slot == selected || inPlayerInventory(slot) != findInPlayer || isCraftingOutput(slot, stack)) {
                continue;
            }
            final ItemStack there = slot.getItem();
            if (there.isEmpty()) {
                if (slot.mayPlace(stack)) {
                    empty.add(slot);
                }
            } else if (compatible(stack, there) && there.getCount() < slot.getMaxStackSize(there)) {
                result.add(slot);
                count -= Math.min(count, slot.getMaxStackSize(there) - there.getCount());
            }
        }
        for (int i = 0; i < empty.size() && count > 0; i++) {
            final Slot slot = empty.get(i);
            result.add(slot);
            count -= Math.min(count, slot.getMaxStackSize(stack));
        }
        if (mustDistributeAll && count > 0) {
            return null;
        }
        return result;
    }

    // ------------------------------------------------------------------ helpers

    /** Empty stacks fit anything; otherwise the same item with the same data. */
    private static boolean compatible(final ItemStack a, final ItemStack b) {
        return a.isEmpty() || b.isEmpty() || QolInventory.canMerge(a, b);
    }

    /**
     * Crafting, furnace and trading outputs: the crafting result slot, or any slot that refuses the very
     * item it holds (furnace and villager results do; inputs, fuel and armor slots never do).
     */
    private static boolean isCraftingOutput(final Slot slot, final ItemStack onMouse) {
        if ((Object) slot instanceof ResultSlot) {
            return true;
        }
        if (inPlayerInventory(slot)) {
            return false;
        }
        final ItemStack probe = slot.hasItem() ? slot.getItem() : onMouse;
        return !probe.isEmpty() && slot.hasItem() && !slot.mayPlace(probe);
    }

    private static void click(final Minecraft mc, final AbstractContainerScreen<?> screen, final Slot slot,
                              final int button, final boolean quickMove) {
        QolInventory.send(mc, screen.getMenu().containerId, slot.index, button, quickMove);
    }
}
