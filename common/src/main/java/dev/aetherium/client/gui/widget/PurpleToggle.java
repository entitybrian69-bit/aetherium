package dev.aetherium.client.gui.widget;

import dev.aetherium.gui.anim.Animator;
import dev.aetherium.gui.theme.PurpleTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Animated pill switch. */
public class PurpleToggle extends PurpleWidget {
    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;
    private final Animator knob = new Animator(0f).easing(Animator.Easing.OUT_BACK);

    public PurpleToggle(int x, int y, int w, int h, Component label, BooleanSupplier getter, Consumer<Boolean> setter) {
        super(x, y, w, h, label);
        this.getter = getter;
        this.setter = setter;
        knob.snap(getter.getAsBoolean() ? 1f : 0f);
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick, float hoverT) {
        boolean on = getter.getAsBoolean();
        if ((knob.target() > 0.5f) != on) knob.animateTo(on ? 1f : 0f, 180);
        float t = knob.value();
        drawLabelLeft(g, getMessage(), 6);

        int pillW = 28, pillH = 12;
        int px = getX() + getWidth() - pillW - 6, py = getY() + (getHeight() - pillH) / 2;
        int track = PurpleTheme.lerp(PurpleTheme.TOGGLE_OFF, PurpleTheme.TOGGLE_ON, t);
        g.fill(px, py, px + pillW, py + pillH, PurpleTheme.scaleAlpha(track, panelAlpha));
        int kx = px + 2 + Math.round((pillW - pillH) * t);
        g.fill(kx, py + 2, kx + pillH - 4, py + pillH - 2, PurpleTheme.scaleAlpha(PurpleTheme.lerp(PurpleTheme.TEXT_MUTED, 0xFFFFFFFF, t), panelAlpha));
        Component state = Component.literal(on ? "ON" : "OFF");
        g.drawString(Minecraft.getInstance().font, state, px - 6 - Minecraft.getInstance().font.width(state), getY() + (getHeight() - 8) / 2,
                PurpleTheme.scaleAlpha(on ? PurpleTheme.ACCENT_BRIGHT : PurpleTheme.TEXT_MUTED, panelAlpha), false);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        setter.accept(!getter.getAsBoolean());
    }
}
