package dev.aetherium.client.gui.widget;

import dev.aetherium.gui.theme.PurpleTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Click to advance through a fixed list of values (enums or discrete ints). Right-click goes backwards. */
public class PurpleCycle<T> extends PurpleWidget {
    private final T[] values;
    private final Supplier<T> getter;
    private final Consumer<T> setter;
    private final java.util.function.Function<T, String> namer;

    public PurpleCycle(int x, int y, int w, int h, Component label, T[] values, Supplier<T> getter, Consumer<T> setter,
                       java.util.function.Function<T, String> namer) {
        super(x, y, w, h, label);
        this.values = values; this.getter = getter; this.setter = setter; this.namer = namer;
    }

    private int index() {
        T cur = getter.get();
        for (int i = 0; i < values.length; i++) if (values[i].equals(cur)) return i;
        return 0;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick, float hoverT) {
        drawLabelLeft(g, getMessage(), 6);
        var font = Minecraft.getInstance().font;
        String v = namer.apply(getter.get());
        g.drawString(font, v, getX() + getWidth() - 6 - font.width(v), getY() + (getHeight() - 8) / 2,
                PurpleTheme.scaleAlpha(PurpleTheme.ACCENT_BRIGHT, panelAlpha), false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible || !clicked(mouseX, mouseY)) return false;
        if (button == 1) { setter.accept(values[Math.floorMod(index() - 1, values.length)]); playDownSound(Minecraft.getInstance().getSoundManager()); return true; }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        setter.accept(values[(index() + 1) % values.length]);
    }

    public static String prettyEnum(Enum<?> e) {
        String s = e.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
