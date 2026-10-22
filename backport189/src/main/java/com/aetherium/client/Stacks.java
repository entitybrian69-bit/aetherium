package com.aetherium.client;

import net.minecraft.item.ItemStack;

/** 1.8.9 item stacks: an empty slot holds null (there is no ItemStack.EMPTY yet). */
public final class Stacks {

    private Stacks() {
    }

    public static boolean empty(final ItemStack stack) {
        return stack == null || stack.stackSize <= 0 || stack.getItem() == null;
    }

    public static int count(final ItemStack stack) {
        return empty(stack) ? 0 : stack.stackSize;
    }
}
