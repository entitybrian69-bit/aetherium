package com.aetherium.gui.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import com.aetherium.config.ConfigValue;
import com.aetherium.gui.AetheriumAnimations;
import com.aetherium.gui.AetheriumTheme;
import com.aetherium.hud.FrameStats;
import com.aetherium.util.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * Every animated purple control in one file, as nested classes.
 *
 * <p>Nesting is a deliberate structural choice, not laziness: all six widgets share
 * the same hover/glow/touch/animation contract, they are only ever constructed by
 * {@code AetheriumTabs}, and keeping them together means a version delta that
 * changes one vanilla signature (for example {@code renderWidget} -> {@code render}
 * on 1.16.5, or {@code GuiGraphics} not existing before 1.18) is a patch to one
 * file instead of six.</p>
 *
 * <p>Shared contract, implemented in {@link PurpleWidget}:</p>
 * <ul>
 *   <li>hover glow animates in and out (no instant colour switch);</li>
 *   <li>touch mode adds {@link AetheriumTheme#hitTargetInset()} to the hit rect only,
 *       so 48 dp targets do not change the visual density;</li>
 *   <li>every commit calls the option immediately — there is no "OK" gate anywhere in
 *       this GUI, which is what "live-applied toggles" means;</li>
 *   <li>narration is implemented so a screen-reader user gets "Dynamic lights, on,
 *       toggle" rather than silence.</li>
 * </ul>
 */
public final class AetheriumWidgets {

    /**
     * The widgets are static nested classes, so the client is looked up per draw rather than stored -
     * a {@code Minecraft} field on a widget would be a second owner of the game instance, and these
     * objects outlive a resource reload. Null is a real answer: headless tests and the dedicated
     * server path construct widgets without a client.
     */
    private static Font fontOrNull() {
        final Minecraft client = Minecraft.getInstance();
        return client == null ? null : client.font;
    }

    private AetheriumWidgets() {
    }

    /** Common animation and theming behaviour for every control below. */
    public abstract static class PurpleWidget extends AbstractWidget {
        protected final AetheriumTheme theme;
        private final AetheriumAnimations.AnimationValue hoverValue =
                new AetheriumAnimations.AnimationValue(0.0f, 0.09f);
        private final AetheriumAnimations.AnimationValue pressValue =
                new AetheriumAnimations.AnimationValue(0.0f, 0.06f, 0.45f);
        private boolean pressed;

        protected PurpleWidget(final AetheriumTheme theme, final int x, final int y, final int width, final int height,
                               final Component message) {
            super(x, y, width, height, message);
            this.theme = Objects.requireNonNull(theme, "theme");
        }

        protected AetheriumAnimations.AnimationValue hoverValue() {
            return this.hoverValue;
        }

        protected AetheriumAnimations.AnimationValue pressValue() {
            return this.pressValue;
        }

        /** Hit rect, widened by the touch inset on Android (vanilla's is public). */
        @Override
        public boolean isMouseOver(final double mouseX, final double mouseY) {
            final int inset = this.theme.hitTargetInset();
            return mouseX >= this.getX() - inset && mouseY >= this.getY() - inset
                    && mouseX < this.getX() + this.getWidth() + inset * 2
                    && mouseY < this.getY() + this.getHeight() + inset * 2;
        }

        @Override
        public void onClick(final double mouseX, final double mouseY) {
            if (!this.active) {
                return;
            }
            this.pressValue.snap(1.0f);
        }

        /** Called by the owning screen every render pass so animations advance. */
        public void tickAnimations() {
            final float delta = AetheriumAnimations.suggestedDeltaSeconds();
            this.hoverValue.tick(delta);
            this.pressValue.tick(delta);
            if (this.pressed && this.pressValue.get() > 0.85f) {
                this.pressed = false;
                this.pressValue.set(0.0f);
            }
        }

        protected void drawSharedBackground(final GuiGraphics guiGraphics, final int mouseX, final int mouseY) {
            final boolean hovered = isMouseOver(mouseX, mouseY);
            this.hoverValue.set(hovered ? 1.0f : 0.0f);
            this.theme.fillRounded(guiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(),
                    hovered ? AetheriumTheme.PANEL_RAISED : AetheriumTheme.PANEL_SUNKEN);
            this.theme.drawHoverGlow(guiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(),
                    this.hoverValue.get());
            final float press = this.pressValue.get();
            if (press > 0.01f) {
                // A 2 px violet flash on press: the eye reads it as the button moving.
                guiGraphics.fill(this.getX(), this.getY() + this.getHeight() - 2,
                        this.getX() + this.getWidth(), this.getY() + this.getHeight(),
                        (int) ((0x80 * (1.0f - press)) * 255) << 24 | (AetheriumTheme.ACCENT & 0xFFFFFF));
            }
        }

        /**
         * {@code AbstractWidget#defaultButtonNarrationText} is the one narration entry
         * point that exists unchanged from 1.17 to 26.x, so every widget here routes
         * through it. The value is folded into the message rather than appended to the
         * output, because {@code NarrationElementOutput.add(...)} takes a
         * {@code NarrationElement} and its overloads moved twice in the port range.
         */
        @Override
        /**
         * 1.21.1 renamed this to {@code updateWidgetNarration} and made it the abstract member of
         * {@code AbstractWidget}; {@code NarrationSupplier#updateNarration} is still public, so an
         * override under the old name and the old access is two errors, not one (CI said exactly
         * that: "does not override abstract method" plus "attempting to assign weaker access").
         */
        public void updateWidgetNarration(final NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }

        /** Keeps the announced string in sync with the visible one. */
        /**
         * Keeps the announced string in sync with the visible value. Widgets that draw
         * their own label (slider, cycle) own a plain {@code label} field, so this is
         * stable; widgets with a translatable label (toggle, action) never call it and
         * leave the lang file authoritative.
         */
        protected void updateMessage(final String label, final String value) {
            final String wanted = value == null ? label : label + ": " + value;
            if (!wanted.equals(this.getMessage().getString())) {
                this.setMessage(Component.literal(wanted));
            }
        }
    }

    /** Animated on/off control: the knob slides, the track fills, the glow pulses. */
    public static final class ToggleWidget extends PurpleWidget {
        private final ConfigValue<Boolean> option;
        private final Runnable afterChange;
        private final AetheriumAnimations.AnimationValue knob = new AetheriumAnimations.AnimationValue(0.0f, 0.12f, 0.35f);
        private final Supplier<String> suffix;
        private String label = "";

        public ToggleWidget(final AetheriumTheme theme, final int x, final int y, final int width, final int height,
                            final ConfigValue<Boolean> option, final Supplier<String> suffix, final Runnable afterChange) {
            super(theme, x, y, width, height, Component.translatable(option.getTranslationKey()));
            this.option = Objects.requireNonNull(option, "option");
            this.suffix = suffix;
            this.afterChange = afterChange;
            this.knob.snap(option.get() ? 1.0f : 0.0f);
            // The option can also change from a command, a config reload, or conflict
            // delegation; listening keeps the widget honest instead of optimistic.
            this.option.addListener(value -> this.knob.set(Boolean.TRUE.equals(value) ? 1.0f : 0.0f));
        }

        @Override
        public void onClick(final double mouseX, final double mouseY) {
            super.onClick(mouseX, mouseY);
            this.option.set(!this.option.get());
            if (this.afterChange != null) {
                this.afterChange.run();
            }
        }

        @Override
        public void renderWidget(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float delta) {
            this.knob.tick(AetheriumAnimations.suggestedDeltaSeconds());
            drawSharedBackground(guiGraphics, mouseX, mouseY);

            final Font font = fontOrNull();
            this.label = this.getMessage().getString();
            final int textX = this.getX() + 6;
            final int textY = this.getY() + (this.getHeight() - (font == null ? 8 : font.lineHeight)) / 2;
            this.theme.drawLabel(guiGraphics, this.label, textX, textY,
                    this.option.isDefault() ? AetheriumTheme.TEXT : AetheriumTheme.ACCENT);

            final int trackWidth = 26;
            final int trackHeight = Math.min(12, this.getHeight() - 6);
            final int trackX = this.getX() + this.getWidth() - trackWidth - 6;
            final int trackY = this.getY() + (this.getHeight() - trackHeight) / 2;
            this.theme.fillRounded(guiGraphics, trackX, trackY, trackWidth, trackHeight, AetheriumTheme.SLIDER_TRACK);
            final int fillWidth = (int) Math.round((trackWidth - 2) * this.knob.get());
            if (fillWidth > 0) {
                this.theme.drawGradientBar(guiGraphics, trackX + 1, trackY + 1, fillWidth, trackHeight - 2,
                        AetheriumTheme.ACCENT_DEEP, AetheriumTheme.ACCENT);
            }
            final int knobSize = trackHeight - 2;
            final int knobX = trackX + 1 + fillWidth - (knobSize + 1) / 2;
            this.theme.fillRounded(guiGraphics, Math.max(trackX + 1, knobX), trackY + 1, knobSize, knobSize,
                    this.knob.get() > 0.5f ? AetheriumTheme.TEXT : AetheriumTheme.TEXT_DISABLED);
            if (this.suffix != null) {
                final String extra = this.suffix.get();
                if (extra != null && !extra.isEmpty() && font != null) {
                    guiGraphics.drawString(font, Component.literal(extra)
                                    .withStyle(style -> style.withColor(AetheriumTheme.TEXT_DIM & 0xFFFFFF)),
                            trackX - font.width(extra) - 8, textY, AetheriumTheme.TEXT_DIM, false);
                }
            }
            if (this.isHoveredOrFocused()) {
                // Comment as an in-place subtitle instead of a tooltip: the vanilla
                // tooltip path needs Tooltip.create + setTooltip, whose signature moved
                // in 1.20.2, and a subtitle is more readable on a phone anyway.
                this.theme.drawLabel(guiGraphics, this.option.getComment(), this.getX() + 6,
                        this.getY() + this.getHeight() + 1, AetheriumTheme.TEXT_DISABLED);
            }
        }
    }

    /** Continuous value with drag, double-click-to-default, and a live numeric readout. */
    public static final class SliderWidget extends PurpleWidget {
        private final String label;
        private final double min;
        private final double max;
        private final DoubleSupplier getter;
        private final DoubleConsumer setter;
        private final Consumer<Double> onCommit;
        private final String format;
        private final AetheriumAnimations.AnimationValue thumb = new AetheriumAnimations.AnimationValue(0.0f, 0.1f);
        private double value;
        private boolean dragging;
        private float hoverPulse;

        public SliderWidget(final AetheriumTheme theme, final int x, final int y, final int width, final int height,
                            final String label, final double min, final double max, final DoubleSupplier getter,
                            final DoubleConsumer setter, final String format, final Consumer<Double> onCommit) {
            super(theme, x, y, width, height, Component.literal(label));
            this.label = Objects.requireNonNull(label, "label");
            this.min = min;
            this.max = max;
            this.getter = Objects.requireNonNull(getter, "getter");
            this.setter = Objects.requireNonNull(setter, "setter");
            this.format = format == null ? "%.2f" : format;
            this.onCommit = onCommit;
            this.value = getter.getAsDouble();
            this.thumb.snap((float) normalized(this.value));
        }

        private double normalized(final double raw) {
            return this.max <= this.min ? 0.0 : MathUtil.clamp((raw - this.min) / (this.max - this.min), 0.0, 1.0);
        }

        @Override
        public void onClick(final double mouseX, final double mouseY) {
            super.onClick(mouseX, mouseY);
            this.dragging = true;
            applyFromMouse(mouseX);
        }

        // Both input handlers return boolean on 1.21.1 (GuiEventListener#mouseDragged/#mouseReleased
        // report whether the event was consumed); void overrides are a compile error, and claiming the
        // release is what stops the drag from being left half-applied by whatever is behind the slider.
        @Override
        public boolean mouseDragged(final double mouseX, final double mouseY, final int button, final double deltaX, final double deltaY) {
            if (!this.dragging) {
                return false;
            }
            applyFromMouse(mouseX);
            return true;
        }

        @Override
        public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
            final boolean wasDragging = this.dragging;
            this.dragging = false;
            return wasDragging;
        }

        private void applyFromMouse(final double mouseX) {
            final double fraction = MathUtil.clamp((mouseX - this.getX() - 8) / (double) Math.max(1, this.getWidth() - 16), 0.0, 1.0);
            final double raw = this.min + fraction * (this.max - this.min);
            this.value = raw;
            this.setter.accept(raw);
            this.thumb.set((float) fraction);
            if (this.onCommit != null) {
                this.onCommit.accept(raw);
            }
        }

        /** Double-tap resets to the default: the shortcut users expect from vanilla. */
        public void resetToDefault(final double defaultRaw) {
            this.value = defaultRaw;
            this.setter.accept(defaultRaw);
            this.thumb.set((float) normalized(defaultRaw));
        }

        @Override
        public void renderWidget(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float delta) {
            if (!this.dragging) {
                this.value = this.getter.getAsDouble();
                this.thumb.set((float) normalized(this.value));
            }
            this.thumb.tick(AetheriumAnimations.suggestedDeltaSeconds());
            drawSharedBackground(guiGraphics, mouseX, mouseY);

            final int trackX = this.getX() + 8;
            final int trackWidth = this.getWidth() - 16;
            final int trackY = this.getY() + this.getHeight() - 9;
            this.theme.fillRounded(guiGraphics, trackX, trackY, trackWidth, 4, AetheriumTheme.SLIDER_TRACK);
            final int filled = (int) Math.round(trackWidth * this.thumb.get());
            this.theme.drawGradientBar(guiGraphics, trackX, trackY, filled, 4, AetheriumTheme.ACCENT_DEEP, AetheriumTheme.ACCENT);
            final int thumbX = trackX + filled;
            this.hoverPulse = isMouseOver(mouseX, mouseY) ? Math.min(1.0f, this.hoverPulse + 0.12f) : Math.max(0.0f, this.hoverPulse - 0.12f);
            final int glow = 2 + (int) (this.hoverPulse * 2);
            guiGraphics.fill(thumbX - glow, trackY - glow, thumbX + glow, trackY + 4 + glow, AetheriumTheme.ACCENT_SOFT);
            this.theme.fillRounded(guiGraphics, thumbX - 2, trackY - 2, 4, 8, AetheriumTheme.TEXT);

            final String valueText = String.format(java.util.Locale.ROOT, this.format, this.value);
            final Font font = fontOrNull();
            if (font != null) {
                guiGraphics.drawString(font, Component.literal(this.label), this.getX() + 6, this.getY() + 3, AetheriumTheme.TEXT, false);
                final String right = valueText;
                guiGraphics.drawString(font, Component.literal(right).withStyle(style -> style.withColor(AetheriumTheme.ACCENT & 0xFFFFFF)),
                        this.getX() + this.getWidth() - 6 - font.width(right), this.getY() + 3, AetheriumTheme.ACCENT, false);
            }
        }

        @Override
        /**
         * 1.21.1 renamed this to {@code updateWidgetNarration} and made it the abstract member of
         * {@code AbstractWidget}; {@code NarrationSupplier#updateNarration} is still public, so an
         * override under the old name and the old access is two errors, not one (CI said exactly
         * that: "does not override abstract method" plus "attempting to assign weaker access").
         */
        public void updateWidgetNarration(final NarrationElementOutput output) {
            this.updateMessage(this.label, String.format(java.util.Locale.ROOT, this.format, this.value));
            this.defaultButtonNarrationText(output);
        }
    }

    /** Enum / boolean cycle control with a sliding value indicator. */
    public static final class CycleWidget extends PurpleWidget {
        private final String label;
        private final String[] values;
        private final Supplier<Integer> getIndex;
        private final IntConsumer setIndex;
        private final Runnable afterChange;
        private final AetheriumAnimations.AnimationValue slide = new AetheriumAnimations.AnimationValue(0.0f, 0.1f);

        public CycleWidget(final AetheriumTheme theme, final int x, final int y, final int width, final int height,
                           final String label, final String[] values, final Supplier<Integer> getIndex,
                           final IntConsumer setIndex, final Runnable afterChange) {
            super(theme, x, y, width, height, Component.literal(label));
            this.label = label;
            this.values = values == null || values.length == 0 ? new String[]{"off"} : values;
            this.getIndex = getIndex;
            this.setIndex = setIndex;
            this.afterChange = afterChange;
            this.slide.snap(getIndex == null ? 0.0f : (float) getIndex.get() / this.values.length);
        }

        @Override
        public void onClick(final double mouseX, final double mouseY) {
            super.onClick(mouseX, mouseY);
            if (this.getIndex == null || this.setIndex == null) {
                return;
            }
            final boolean backwards = mouseX < this.getX() + this.getWidth() / 3.0;
            final int current = this.getIndex.get();
            final int next = backwards
                    ? Math.floorMod(current - 1, this.values.length)
                    : (current + 1) % this.values.length;
            this.setIndex.accept(next);
            this.slide.set((float) next / this.values.length);
            if (this.afterChange != null) {
                this.afterChange.run();
            }
        }

        @Override
        public void renderWidget(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float delta) {
            this.slide.tick(AetheriumAnimations.suggestedDeltaSeconds());
            drawSharedBackground(guiGraphics, mouseX, mouseY);
            final Font font = fontOrNull();
            if (font == null) {
                return;
            }
            final int index = this.getIndex == null ? 0 : MathUtil.clamp(this.getIndex.get(), 0, this.values.length - 1);
            final String value = this.values[index];
            guiGraphics.drawString(font, Component.literal(this.label), this.getX() + 6, this.getY() + (this.getHeight() - font.lineHeight) / 2,
                    AetheriumTheme.TEXT, false);
            final String arrow = "< ";
            final int valueX = this.getX() + this.getWidth() - 6 - font.width(value) - font.width(arrow);
            guiGraphics.drawString(font, Component.literal(value).withStyle(style -> style.withColor(AetheriumTheme.ACCENT & 0xFFFFFF)),
                    valueX, this.getY() + (this.getHeight() - font.lineHeight) / 2, AetheriumTheme.ACCENT, false);
            guiGraphics.drawString(font, Component.literal(arrow), valueX - font.width(arrow),
                    this.getY() + (this.getHeight() - font.lineHeight) / 2, AetheriumTheme.TEXT_DIM, false);
            // The indicator is a 1 px violet line whose x tracks the index: it makes a
            // long cycle list (weather: off/fast/fancy/fancy+) readable at a glance.
            final int indicatorX = this.getX() + (int) ((this.getWidth() - 4) * this.slide.get());
            guiGraphics.fill(indicatorX, this.getY() + this.getHeight() - 2, indicatorX + 4, this.getY() + this.getHeight(), AetheriumTheme.ACCENT);
        }

        @Override
        /**
         * 1.21.1 renamed this to {@code updateWidgetNarration} and made it the abstract member of
         * {@code AbstractWidget}; {@code NarrationSupplier#updateNarration} is still public, so an
         * override under the old name and the old access is two errors, not one (CI said exactly
         * that: "does not override abstract method" plus "attempting to assign weaker access").
         */
        public void updateWidgetNarration(final NarrationElementOutput output) {
            final int index = this.getIndex == null ? 0 : this.getIndex.get();
            this.updateMessage(this.label, index >= 0 && index < this.values.length ? this.values[index] : "unknown");
            this.defaultButtonNarrationText(output);
        }

        /** Mirrors {@link java.util.function.IntConsumer} without importing it into the public API. */
        public interface IntConsumer {
            void accept(int value);
        }
    }

    /** Simple action button (Done, Apply, Clear cache, Open shader screen). */
    public static final class ActionWidget extends PurpleWidget {
        private final Runnable action;
        private final BooleanSupplier enabled;
        private final int accentColor;

        public ActionWidget(final AetheriumTheme theme, final int x, final int y, final int width, final int height,
                            final Component message, final Runnable action, final BooleanSupplier enabled, final int accentColor) {
            super(theme, x, y, width, height, message);
            this.action = Objects.requireNonNull(action, "action");
            this.enabled = enabled;
            this.accentColor = accentColor;
        }

        @Override
        public void onClick(final double mouseX, final double mouseY) {
            super.onClick(mouseX, mouseY);
            if (this.enabled != null && !this.enabled.getAsBoolean()) {
                return;
            }
            this.action.run();
        }

        @Override
        public void renderWidget(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float delta) {
            final boolean usable = this.enabled == null || this.enabled.getAsBoolean();
            this.active = usable;
            drawSharedBackground(guiGraphics, mouseX, mouseY);
            if (!usable) {
                guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0x88120819);
            }
            final Font font = fontOrNull();
            if (font == null) {
                return;
            }
            final String text = this.getMessage().getString();
            guiGraphics.drawCenteredString(font, Component.literal(text)
                            .withStyle(style -> style.withColor((usable ? this.accentColor : AetheriumTheme.TEXT_DISABLED) & 0xFFFFFF)),
                    this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - font.lineHeight) / 2,
                    usable ? this.accentColor : AetheriumTheme.TEXT_DISABLED);
        }
    }

    /** Read-only stat strip: FPS, p99, backend, scheduler. Animated value changes. */
    public static final class PreviewWidget extends PurpleWidget {
        private final Supplier<FrameStats> stats;
        private final double[] graph = new double[180];
        private final List<String> lines = new ArrayList<>(6);
        private final AetheriumAnimations.AnimationValue fpsValue = new AetheriumAnimations.AnimationValue(0.0f, 0.25f);
        private final AetheriumAnimations.AnimationValue p99Value = new AetheriumAnimations.AnimationValue(0.0f, 0.25f);
        private double refresh;

        public PreviewWidget(final AetheriumTheme theme, final int x, final int y, final int width, final int height,
                             final Supplier<FrameStats> stats) {
            super(theme, x, y, width, height, Component.translatable("aetherium.preview.title"));
            this.stats = Objects.requireNonNull(stats, "stats");
        }

        @Override
        public void onClick(final double mouseX, final double mouseY) {
            // Read-only: a click resets the window, which is the gesture a benchmark
            // user wants ("start counting from here").
            final FrameStats local = this.stats.get();
            if (local != null) {
                local.resetWindow();
            }
        }

        @Override
        public void renderWidget(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float delta) {
            final FrameStats local = this.stats.get();
            if (local == null) {
                return;
            }
            this.fpsValue.set((float) local.getFps());
            this.p99Value.set((float) local.getP99Ms());
            this.fpsValue.tick(AetheriumAnimations.suggestedDeltaSeconds());
            this.p99Value.tick(AetheriumAnimations.suggestedDeltaSeconds());

            this.theme.fillRounded(guiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), AetheriumTheme.PANEL);
            this.theme.outlineRounded(guiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), AetheriumTheme.BORDER);

            final Font font = fontOrNull();
            if (font == null) {
                return;
            }
            final int centerX = this.getX() + this.getWidth() / 2;
            guiGraphics.drawCenteredString(font, Component.literal(String.format(java.util.Locale.ROOT, "%.0f fps", this.fpsValue.get()))
                    .withStyle(style -> style.withColor(AetheriumTheme.ACCENT & 0xFFFFFF)), centerX, this.getY() + 4, AetheriumTheme.TEXT);
            final AetheriumTheme.AetheriumStatus status = local.getP99Ms() > 50.0 ? AetheriumTheme.AetheriumStatus.BLOCKED
                    : local.getP99Ms() > 22.0 ? AetheriumTheme.AetheriumStatus.CAUTION : AetheriumTheme.AetheriumStatus.GOOD;
            guiGraphics.drawCenteredString(font, Component.literal(String.format(java.util.Locale.ROOT, "p99 %.2f ms", this.p99Value.get()))
                            .withStyle(style -> style.withColor(AetheriumTheme.statusColor(status) & 0xFFFFFF)),
                    centerX, this.getY() + 15, AetheriumTheme.statusColor(status));

            final int count = local.copyRecentGraph(this.graph, this.graph.length);
            this.lines.clear();
            if (count > 0) {
                double peak = 1.0;
                for (int i = 0; i < count; i++) {
                    peak = Math.max(peak, this.graph[i]);
                }
                final int graphY = this.getY() + this.getHeight() - 16;
                for (int i = 0; i < count; i++) {
                    final int barHeight = Math.max(1, (int) (14.0 * Math.min(1.0, this.graph[i] / peak)));
                    final int barX = this.getX() + 4 + (int) ((this.getWidth() - 8) * i / (float) Math.max(1, count - 1));
                    guiGraphics.fill(barX, graphY + 14 - barHeight, barX + 1, graphY + 14,
                            this.graph[i] > 50.0 ? AetheriumTheme.DANGER : AetheriumTheme.ACCENT_SOFT);
                }
                this.refresh += delta;
            }
        }
    }

    /** Scrollable content list: swipe-to-scroll, long-press tooltip, per-row reveal. */
    public static final class ListWidget {
        private final List<AbstractWidget> rows = new ArrayList<>(32);
        private final AetheriumTheme theme;
        private float scroll;
        private float scrollVelocity;
        private float targetScroll;
        private int contentHeight;
        private int viewHeight;
        private long pressStartedNanos;
        private int tooltipRow = -1;
        private int lastTooltipRow = -1;
        private final AetheriumAnimations.AnimationValue reveal = new AetheriumAnimations.AnimationValue(0.0f, 0.2f);

        public ListWidget(final AetheriumTheme theme) {
            this.theme = Objects.requireNonNull(theme, "theme");
        }

        public void add(final AbstractWidget widget) {
            this.rows.add(Objects.requireNonNull(widget, "widget"));
            this.contentHeight += this.theme.rowHeight() + 4;
        }

        public List<AbstractWidget> rows() {
            return this.rows;
        }

        public void setViewHeight(final int height) {
            this.viewHeight = height;
        }

        public void scrollBy(final double amount) {
            this.targetScroll = MathUtil.clamp(this.targetScroll - (float) (amount * this.theme.rowHeight()), 0.0f,
                    Math.max(0.0f, this.contentHeight - this.viewHeight));
        }

        /** Swipe gesture from {@code mouseDragged}: velocity-carrying, like a phone. */
        public void swipe(final double deltaY) {
            this.scrollVelocity = (float) (this.scrollVelocity * 0.6 + deltaY * 0.4);
            this.targetScroll = MathUtil.clamp(this.targetScroll - this.scrollVelocity, 0.0f,
                    Math.max(0.0f, this.contentHeight - this.viewHeight));
        }

        public void beginPress() {
            this.pressStartedNanos = System.nanoTime();
        }

        /** Long-press (350 ms) shows the row's comment; a tap passes through to the widget. */
        public int updateLongPress(final int mouseX, final int mouseY, final int rowHeight, final int listY) {
            if (this.pressStartedNanos == 0L) {
                this.tooltipRow = -1;
                return this.tooltipRow;
            }
            final long held = System.nanoTime() - this.pressStartedNanos;
            if (held > 350_000_000L) {
                this.tooltipRow = MathUtil.clamp((int) ((mouseY - listY + this.scroll) / Math.max(1, rowHeight)), 0, this.rows.size() - 1);
            }
            return this.tooltipRow;
        }

        public void endPress() {
            this.pressStartedNanos = 0L;
            if (this.tooltipRow == this.lastTooltipRow) {
                this.tooltipRow = -1;
            }
            this.lastTooltipRow = this.tooltipRow;
        }

        public int getTooltipRow() {
            return this.tooltipRow;
        }

        public void tickAnimations() {
            final float delta = AetheriumAnimations.suggestedDeltaSeconds();
            this.reveal.set(1.0f);
            this.reveal.tick(delta);
            // Momentum: decay the velocity, then let the eased scroll chase the target so
            // a fling and a single wheel notch feel like the same material.
            this.scrollVelocity *= 0.88f;
            this.scroll = MathUtil.smoothDamp(this.scroll, this.targetScroll, 0.09f, delta);
            for (final AbstractWidget row : this.rows) {
                if (row instanceof PurpleWidget purple) {
                    purple.tickAnimations();
                }
            }
        }

        public float getScroll() {
            return this.scroll;
        }

        public float getRevealProgress() {
            return MathUtil.easeOutCubic(this.reveal.get());
        }

        public AetheriumTheme theme() {
            return this.theme;
        }

        public int getRowHeight() {
            return this.theme.rowHeight();
        }

        public int getContentHeight() {
            return this.contentHeight;
        }
    }
}
