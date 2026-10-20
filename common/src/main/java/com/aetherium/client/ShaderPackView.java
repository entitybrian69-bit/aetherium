package com.aetherium.client;

import com.aetherium.gui.GuiCanvas;
import com.aetherium.shader.ShaderBridge;
import com.aetherium.shader.ShaderOptions;
import com.aetherium.shader.ShaderPack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The shader pack screen, laid out like Iris's: the world stays visible behind it, the installed
 * packs in a list (the applied one in yellow, the selected one boxed), "Shaders: Enabled" on top,
 * and Done / Apply / Cancel, Open Shader Pack Folder and Shader Pack Settings below. The settings
 * page is Iris's option grid: buttons that cycle on click, sliders, sub screens, a profile button.
 * Version independent: drawn on a {@link GuiCanvas}, hosted by AetheriumScreen.
 */
public final class ShaderPackView {

    /** What the view needs from the screen around it. */
    public interface Host {
        /** Leaves this page (back to Aetherium's settings). */
        void closeShaderPage();

        /** Makes {@code name} ("" = off) the active pack and reloads. */
        void applyShaderPack(String name);

        void openShaderFolder();

        void clickSound();

        /** True when a world is drawn behind the screen. */
        boolean worldBehind();
    }

    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREY = 0xFFA0A0A0;
    private static final int YELLOW = 0xFFFFFF55;
    private static final int GREEN = 0xFF55FF55;
    private static final int RED = 0xFFFF5555;
    private static final int ENTRY_H = 18;
    private static final int BUTTON_H = 20;

    private final Host host;
    private int width;
    private int height;

    // ---- pack list state
    private final List<String> packs;
    private final String applied;
    private String selected;
    private boolean enabled;
    private double listScroll;
    private String folderHint = "";

    // ---- settings state
    private boolean settings;
    private ShaderPack pack;
    private final List<String> screenStack = new ArrayList<>();
    private final Map<String, String> edits = new LinkedHashMap<>();
    private String pickedProfile;
    private boolean resetAll;
    private double optionScroll;
    private Slot dragging;

    // ---- per frame
    private final List<Slot> slots = new ArrayList<>();
    private final List<Button> buttons = new ArrayList<>();
    private int listTop;
    private int listBottom;
    private int contentHeight;
    private boolean touchDrag;
    private double touchLastY;
    private double touchTravel;

    public ShaderPackView(final Host host, final String currentPack) {
        this.host = host;
        // downloaded packs only: the bundled Aetherium Lite lives on the Shaders tab
        this.packs = ShaderBridge.externalPacks();
        this.applied = currentPack == null ? "" : currentPack.trim();
        this.enabled = isExternal(this.applied);
        this.selected = this.enabled ? this.applied : (this.packs.isEmpty() ? "" : this.packs.get(0));
    }

    public void resize(final int w, final int h) {
        this.width = w;
        this.height = h;
    }

    // ================================================================== drawing

    public void render(final GuiCanvas c, final int mouseX, final int mouseY) {
        this.buttons.clear();
        this.slots.clear();
        this.listTop = 32;
        this.listBottom = this.height - 58;
        if (!this.host.worldBehind()) {
            c.fill(0, 0, this.width, this.height, 0xFF101014); // title screen: nothing to show through
        }
        // Iris's in-world list: a translucent band, darker edges top and bottom
        c.fill(0, 0, this.width, this.height, 0x40000000);
        c.fill(0, this.listTop, this.width, this.listBottom, 0x70000000);
        for (int i = 0; i < 4; i++) {
            final int a = (4 - i) * 0x18;
            c.fill(0, this.listTop + i, this.width, this.listTop + i + 1, a << 24);
            c.fill(0, this.listBottom - i - 1, this.width, this.listBottom - i, a << 24);
        }
        if (this.settings) {
            renderSettings(c, mouseX, mouseY);
        } else {
            renderPacks(c, mouseX, mouseY);
        }
        for (final Button b : this.buttons) {
            drawButton(c, b, mouseX, mouseY);
        }
    }

    private void renderPacks(final GuiCanvas c, final int mouseX, final int mouseY) {
        centered(c, "Shader Packs", this.width / 2, 8, WHITE);
        final int rowW = Math.min(300, this.width - 40);
        final int x1 = (this.width - rowW) / 2;
        final int x2 = x1 + rowW;
        final int total = 6 + BUTTON_H + 8 + this.packs.size() * ENTRY_H + 6;
        this.contentHeight = total;
        clampScroll(total);
        int y = this.listTop + 6 - (int) this.listScroll;
        c.pushClip(0, this.listTop, this.width, this.listBottom);
        final Button toggle = new Button(x1 + 20, y, x2 - 20, y + BUTTON_H,
                "Shaders: " + (this.enabled ? "Enabled" : "Disabled"), () -> this.enabled = !this.enabled);
        toggle.valueColor = this.enabled ? GREEN : RED;
        toggle.clipped = true;
        drawButton(c, toggle, mouseX, mouseY);
        this.listButton = toggle;
        y += BUTTON_H + 8;
        this.packTop = y;
        for (final String name : this.packs) {
            final boolean isSelected = name.equals(this.selected);
            final boolean hover = inside(mouseX, mouseY, x1, y, x2, y + ENTRY_H) && mouseY >= this.listTop
                    && mouseY < this.listBottom;
            if (isSelected) {
                c.fill(x1, y, x2, y + ENTRY_H, this.enabled ? 0xFFFFFFFF : 0xFF808080);
                c.fill(x1 + 1, y + 1, x2 - 1, y + ENTRY_H - 1, 0xFF000000);
            } else if (hover) {
                c.fill(x1, y, x2, y + ENTRY_H, 0x30FFFFFF);
            }
            int color = name.equals(this.applied) && this.enabled ? YELLOW : WHITE;
            if (!this.enabled) {
                color = GREY;
            }
            final String shown = ellipsize(c, name, rowW - 10);
            centered(c, shown, this.width / 2, y + 5, color);
            y += ENTRY_H;
        }
        c.popClip();
        this.packX1 = x1;
        this.packX2 = x2;
        drawScrollbar(c, total);
        if (!this.folderHint.isEmpty()) {
            centered(c, ellipsize(c, this.folderHint, this.width - 10), this.width / 2, 20, GREY);
        } else if (this.packs.isEmpty()) {
            centered(c, "Put shader pack .zip files in the shaderpacks folder", this.width / 2, 20, GREY);
        }

        final int bw2 = Math.min(152, (this.width - 12) / 2);
        final int row1 = this.height - 51;
        final int cx = this.width / 2;
        this.buttons.add(new Button(cx - 2 - bw2, row1, cx - 2, row1 + BUTTON_H, "Open Shader Pack Folder...",
                this::openFolder));
        final Button settingsButton = new Button(cx + 2, row1, cx + 2 + bw2, row1 + BUTTON_H,
                "Shader Pack Settings...", this::openSettings);
        settingsButton.active = !this.selected.isEmpty();
        this.buttons.add(settingsButton);
        addBottomRow(this::done, this::apply, this::cancel);
    }

    private Button listButton;
    private int packTop;
    private int packX1;
    private int packX2;

    private void renderSettings(final GuiCanvas c, final int mouseX, final int mouseY) {
        final String screenKey = this.screenStack.isEmpty() ? "screen" : this.screenStack.get(this.screenStack.size() - 1);
        final String title = this.screenStack.isEmpty() ? this.pack.name
                : this.pack.label(screenKey, screenKey.substring(screenKey.indexOf('.') + 1));
        centered(c, ellipsize(c, title, this.width - 20), this.width / 2, 8, WHITE);
        if (!this.edits.isEmpty() || this.pickedProfile != null || this.resetAll) {
            centered(c, "Unapplied changes: press Apply or Done", this.width / 2, 20, YELLOW);
        }
        final List<Element> elements = layout(screenKey);
        final int columns = columns(screenKey);
        final int gridW = Math.min(columns == 1 ? 210 : 420, this.width - 20);
        final int gap = 4;
        final int cellW = (gridW - gap * (columns - 1)) / columns;
        final int x0 = (this.width - gridW) / 2;
        final int rows = (elements.size() + columns - 1) / columns;
        final int total = 6 + rows * (BUTTON_H + 4) + 4;
        this.contentHeight = total;
        clampScroll(total);
        final int top = this.listTop + 6 - (int) this.optionScroll;
        c.pushClip(0, this.listTop, this.width, this.listBottom);
        for (int i = 0; i < elements.size(); i++) {
            final Element e = elements.get(i);
            if (e == null) {
                continue; // <empty>
            }
            final int col = i % columns;
            final int row = i / columns;
            final int x1 = x0 + col * (cellW + gap);
            final int y1 = top + row * (BUTTON_H + 4);
            final Slot s = new Slot(e, x1, y1, x1 + cellW, y1 + BUTTON_H);
            this.slots.add(s);
            drawSlot(c, s, mouseX, mouseY);
        }
        c.popClip();
        drawScrollbar(c, total);

        final int bw2 = Math.min(152, (this.width - 12) / 2);
        final int row1 = this.height - 51;
        final int cx = this.width / 2;
        this.buttons.add(new Button(cx - 2 - bw2, row1, cx - 2, row1 + BUTTON_H,
                this.screenStack.isEmpty() ? "Back to Shader Packs" : "Back", this::back));
        this.buttons.add(new Button(cx + 2, row1, cx + 2 + bw2, row1 + BUTTON_H, "Reset to Defaults", () -> {
            this.edits.clear();
            this.pickedProfile = null;
            this.resetAll = true;
        }));
        addBottomRow(this::done, this::apply, this::cancel);
    }

    private void addBottomRow(final Runnable done, final Runnable apply, final Runnable cancel) {
        final int bw = Math.min(100, (this.width - 16) / 3);
        final int row2 = this.height - 27;
        final int cx = this.width / 2;
        this.buttons.add(new Button(cx - bw - bw / 2 - 4, row2, cx - bw / 2 - 4, row2 + BUTTON_H, "Done", done));
        this.buttons.add(new Button(cx - bw / 2, row2, cx + bw - bw / 2, row2 + BUTTON_H, "Apply", apply));
        this.buttons.add(new Button(cx + bw - bw / 2 + 4, row2, cx + 2 * bw - bw / 2 + 4, row2 + BUTTON_H, "Cancel",
                cancel));
    }

    private void drawSlot(final GuiCanvas c, final Slot s, final int mouseX, final int mouseY) {
        final Element e = s.element;
        final boolean hover = inside(mouseX, mouseY, s.x1, s.y1, s.x2, s.y2) && mouseY >= this.listTop
                && mouseY < this.listBottom;
        if (e.kind == Element.SLIDER) {
            final ShaderOptions.Option o = e.option;
            final int n = Math.max(1, o.values.size() - 1);
            final int idx = Math.max(0, o.values.indexOf(value(o)));
            c.fill(s.x1, s.y1, s.x2, s.y2, 0xFF000000);
            c.fill(s.x1 + 1, s.y1 + 1, s.x2 - 1, s.y2 - 1, 0xFF3C3C3C);
            final int knobW = 8;
            final int kx = s.x1 + Math.round((s.x2 - s.x1 - knobW) * (idx / (float) n));
            drawBevel(c, kx, s.y1, kx + knobW, s.y2, hover || this.dragging == s, true);
            final String label = e.label + ": ";
            final String val = valueLabel(o, value(o));
            drawLabelValue(c, s, label, val, edited(o) ? YELLOW : WHITE);
            return;
        }
        drawBevel(c, s.x1, s.y1, s.x2, s.y2, hover, true);
        if (e.kind == Element.SCREEN) {
            centered(c, ellipsize(c, e.label + "...", s.x2 - s.x1 - 8), (s.x1 + s.x2) / 2, s.y1 + 6, WHITE);
        } else if (e.kind == Element.PROFILE) {
            final String p = currentProfile();
            drawLabelValue(c, s, "Profile: ", p == null ? "Custom" : this.pack.label("profile." + p, p),
                    this.pickedProfile != null ? YELLOW : WHITE);
        } else {
            final ShaderOptions.Option o = e.option;
            final String v = value(o);
            int color = edited(o) ? YELLOW : WHITE;
            if (o.toggle && !edited(o)) {
                color = "true".equals(v) ? GREEN : RED;
            }
            drawLabelValue(c, s, e.label + ": ", valueLabel(o, v), color);
        }
    }

    private void drawLabelValue(final GuiCanvas c, final Slot s, final String label, final String value, final int color) {
        final int avail = s.x2 - s.x1 - 8;
        String v = value;
        String l = label;
        if (c.textWidth(l + v) > avail) {
            l = ellipsize(c, label.trim(), Math.max(10, avail - c.textWidth(v) - 6)) + " ";
            if (c.textWidth(l + v) > avail) {
                v = ellipsize(c, v, avail - c.textWidth(l));
            }
        }
        final int w = c.textWidth(l + v);
        final int x = (s.x1 + s.x2 - w) / 2;
        shadowText(c, l, x, s.y1 + 6, WHITE);
        shadowText(c, v, x + c.textWidth(l), s.y1 + 6, color);
    }

    private void drawButton(final GuiCanvas c, final Button b, final int mouseX, final int mouseY) {
        final boolean hover = b.active && inside(mouseX, mouseY, b.x1, b.y1, b.x2, b.y2)
                && (!b.clipped || (mouseY >= this.listTop && mouseY < this.listBottom));
        drawBevel(c, b.x1, b.y1, b.x2, b.y2, hover, b.active);
        final int color = !b.active ? GREY : WHITE;
        if (b.valueColor != 0 && b.label.indexOf(": ") > 0) {
            final int split = b.label.indexOf(": ") + 2;
            final String l = b.label.substring(0, split);
            final String v = b.label.substring(split);
            final int w = c.textWidth(b.label);
            final int x = (b.x1 + b.x2 - w) / 2;
            shadowText(c, l, x, b.y1 + 6, color);
            shadowText(c, v, x + c.textWidth(l), b.y1 + 6, b.valueColor);
            return;
        }
        centered(c, ellipsize(c, b.label, b.x2 - b.x1 - 8), (b.x1 + b.x2) / 2, b.y1 + 6, color);
    }

    /** Vanilla's button look: grey bevel, black outline, white outline on hover. */
    private static void drawBevel(final GuiCanvas c, final int x1, final int y1, final int x2, final int y2,
                                  final boolean hover, final boolean active) {
        c.fill(x1, y1, x2, y2, hover ? 0xFFFFFFFF : 0xFF000000);
        final int face = !active ? 0xFF2E2E2E : (hover ? 0xFF7B86C4 : 0xFF6F6F6F);
        c.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, face);
        c.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 2, active ? (hover ? 0xFFA9B4EE : 0xFF9C9C9C) : 0xFF404040);
        c.fill(x1 + 1, y2 - 3, x2 - 1, y2 - 1, active ? (hover ? 0xFF5560A0 : 0xFF555555) : 0xFF222222);
    }

    private void drawScrollbar(final GuiCanvas c, final int total) {
        final int view = this.listBottom - this.listTop;
        if (total <= view) {
            return;
        }
        final double scroll = this.settings ? this.optionScroll : this.listScroll;
        final int x2 = this.width - 2;
        final int x1 = x2 - 4;
        c.fill(x1, this.listTop, x2, this.listBottom, 0xFF000000);
        final int barH = Math.max(16, view * view / total);
        final int y = this.listTop + (int) ((view - barH) * (scroll / (total - view)));
        c.fill(x1, y, x2, y + barH, 0xFF808080);
        c.fill(x1, y, x2 - 1, y + barH - 1, 0xFFC0C0C0);
    }

    private void clampScroll(final int total) {
        final int max = Math.max(0, total - (this.listBottom - this.listTop));
        if (this.settings) {
            this.optionScroll = Math.max(0, Math.min(max, this.optionScroll));
        } else {
            this.listScroll = Math.max(0, Math.min(max, this.listScroll));
        }
    }

    private static void centered(final GuiCanvas c, final String s, final int cx, final int y, final int color) {
        shadowText(c, s, cx - c.textWidth(s) / 2, y, color);
    }

    private static void shadowText(final GuiCanvas c, final String s, final int x, final int y, final int color) {
        final int shadow = (color & 0xFF000000) | ((color & 0xFCFCFC) >> 2);
        c.text(s, x + 1, y + 1, shadow);
        c.text(s, x, y, color);
    }

    private static String ellipsize(final GuiCanvas c, final String s, final int max) {
        if (c.textWidth(s) <= max) {
            return s;
        }
        String t = s;
        while (t.length() > 1 && c.textWidth(t + "...") > max) {
            t = t.substring(0, t.length() - 1);
        }
        return t + "...";
    }

    // ================================================================== settings model

    private static final class Element {
        static final int OPTION = 0;
        static final int SLIDER = 1;
        static final int SCREEN = 2;
        static final int PROFILE = 3;
        final int kind;
        final String label;
        final ShaderOptions.Option option;
        final String screen;

        Element(final int kind, final String label, final ShaderOptions.Option option, final String screen) {
            this.kind = kind;
            this.label = label;
            this.option = option;
            this.screen = screen;
        }
    }

    /** One screen of the pack's shaders.properties layout; null entries are <empty> gaps. */
    private List<Element> layout(final String key) {
        final List<Element> out = new ArrayList<>();
        final Set<String> sliders = new HashSet<>(this.pack.layout.sliders);
        final String spec = this.pack.layout.screens.get(key);
        if (spec == null) {
            // no screens: Iris lists every changeable option
            if (!this.pack.layout.profiles.isEmpty()) {
                out.add(new Element(Element.PROFILE, "Profile", null, null));
                out.add(null);
            }
            for (final ShaderOptions.Option o : this.pack.options.all().values()) {
                if (o.listed && o.changeable()) {
                    out.add(optionElement(o, sliders));
                }
            }
            return out;
        }
        final Set<String> listed = new HashSet<>();
        for (final String token : spec.trim().split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            if (token.equals("<empty>")) {
                out.add(null);
            } else if (token.equals("<profile>")) {
                if (!this.pack.layout.profiles.isEmpty()) {
                    out.add(new Element(Element.PROFILE, "Profile", null, null));
                }
            } else if (token.startsWith("[") && token.endsWith("]")) {
                final String name = token.substring(1, token.length() - 1);
                final String sub = "screen." + name;
                if (this.pack.layout.screens.containsKey(sub)) {
                    out.add(new Element(Element.SCREEN, this.pack.label(sub, name), null, sub));
                }
            } else if (token.equals("*")) {
                for (final ShaderOptions.Option o : this.pack.options.all().values()) {
                    if (o.listed && o.changeable() && !listed.contains(o.name) && !inAnyScreen(o.name)) {
                        out.add(optionElement(o, sliders));
                        listed.add(o.name);
                    }
                }
            } else {
                final ShaderOptions.Option o = this.pack.options.get(token);
                if (o != null && o.changeable() && listed.add(o.name)) {
                    out.add(optionElement(o, sliders));
                }
            }
        }
        return out;
    }

    private Set<String> screenOptions;

    private boolean inAnyScreen(final String name) {
        if (this.screenOptions == null) {
            this.screenOptions = new HashSet<>();
            for (final String spec : this.pack.layout.screens.values()) {
                for (final String t : spec.trim().split("\\s+")) {
                    this.screenOptions.add(t);
                }
            }
        }
        return this.screenOptions.contains(name);
    }

    private Element optionElement(final ShaderOptions.Option o, final Set<String> sliders) {
        final String label = this.pack.label("option." + o.name, o.name);
        final int kind = !o.toggle && sliders.contains(o.name) && o.values.size() > 1 ? Element.SLIDER : Element.OPTION;
        return new Element(kind, label, o, null);
    }

    private int columns(final String key) {
        final String v = this.pack.layout.raw.get(key + ".columns");
        if (v != null) {
            try {
                return Math.max(1, Math.min(4, Integer.parseInt(v.trim())));
            } catch (final NumberFormatException ignored) {
                // default below
            }
        }
        return this.width < 330 ? 1 : 2;
    }

    private String value(final ShaderOptions.Option o) {
        final String e = this.edits.get(o.name);
        if (e != null) {
            return e;
        }
        if (this.resetAll) {
            return o.defaultValue;
        }
        if (this.pickedProfile != null) {
            final String p = profileValue(this.pickedProfile, o.name, 0);
            if (p != null) {
                return p;
            }
        }
        return ShaderBridge.optionValue(o.name, o.value());
    }

    private boolean edited(final ShaderOptions.Option o) {
        return this.edits.containsKey(o.name) || this.resetAll || this.pickedProfile != null;
    }

    /** The value a profile line gives an option (OptiFine syntax: X, !X, X=v, X:v, profile.Y). */
    private String profileValue(final String profile, final String option, final int depth) {
        final String spec = this.pack.layout.profiles.get(profile);
        if (spec == null || depth > 8) {
            return null;
        }
        String found = null;
        for (final String t : spec.trim().split("\\s+")) {
            if (t.startsWith("profile.")) {
                final String inner = profileValue(t.substring(8), option, depth + 1);
                if (inner != null) {
                    found = inner;
                }
            } else if (t.equals(option)) {
                found = "true";
            } else if (t.equals("!" + option)) {
                found = "false";
            } else if (t.startsWith(option + "=") || t.startsWith(option + ":")) {
                found = t.substring(option.length() + 1);
            }
        }
        return found;
    }

    private String currentProfile() {
        if (this.pickedProfile != null) {
            return this.pickedProfile;
        }
        if (!this.edits.isEmpty() || this.resetAll) {
            return null;
        }
        final ShaderPack now = ShaderBridge.optionsPack(this.selected);
        return (now == null ? this.pack : now).options.currentProfile(this.pack.layout.profiles);
    }

    private String valueLabel(final ShaderOptions.Option o, final String v) {
        if (o.toggle) {
            return "true".equals(v) ? "On" : "Off";
        }
        return this.pack.label("value." + o.name + "." + v, v);
    }

    private void cycle(final Element e, final int step, final boolean reset) {
        if (e.kind == Element.SCREEN) {
            this.screenStack.add(e.screen);
            this.optionScroll = 0;
            return;
        }
        if (e.kind == Element.PROFILE) {
            final List<String> names = new ArrayList<>(this.pack.layout.profiles.keySet());
            if (names.isEmpty()) {
                return;
            }
            final String cur = currentProfile();
            int i = cur == null ? -1 : names.indexOf(cur);
            i = reset ? 0 : ((i + step) % names.size() + names.size()) % names.size();
            this.pickedProfile = names.get(i);
            this.edits.clear();
            this.resetAll = false;
            return;
        }
        final ShaderOptions.Option o = e.option;
        if (reset) {
            setValue(o, o.defaultValue);
            return;
        }
        if (o.toggle) {
            setValue(o, "true".equals(value(o)) ? "false" : "true");
            return;
        }
        final List<String> values = o.values;
        if (values.isEmpty()) {
            return;
        }
        int i = values.indexOf(value(o));
        i = ((i + step) % values.size() + values.size()) % values.size();
        setValue(o, values.get(i));
    }

    private void setValue(final ShaderOptions.Option o, final String v) {
        this.edits.put(o.name, v);
    }

    private void slide(final Slot s, final double mouseX) {
        final ShaderOptions.Option o = s.element.option;
        final int n = o.values.size() - 1;
        if (n < 1) {
            return;
        }
        final double t = (mouseX - s.x1 - 4) / (double) (s.x2 - s.x1 - 8);
        final int i = (int) Math.round(Math.max(0, Math.min(1, t)) * n);
        final String v = o.values.get(i);
        if (!v.equals(value(o))) {
            setValue(o, v);
        }
    }

    // ================================================================== actions

    private void openSettings() {
        if (this.selected.isEmpty()) {
            return;
        }
        final ShaderPack p = ShaderBridge.optionsPack(this.selected);
        if (p == null) {
            this.folderHint = "Could not read the settings of " + this.selected;
            return;
        }
        this.pack = p;
        this.screenOptions = null;
        this.settings = true;
        this.screenStack.clear();
        this.optionScroll = 0;
    }

    private void back() {
        if (!this.screenStack.isEmpty()) {
            this.screenStack.remove(this.screenStack.size() - 1);
            this.optionScroll = 0;
        } else {
            this.settings = false; // edits stay staged until Apply / Done / Cancel
        }
    }

    private void openFolder() {
        this.host.openShaderFolder();
        final java.nio.file.Path dir = ShaderBridge.shaderpacksDir();
        this.folderHint = dir == null ? "" : "Folder: " + dir.toAbsolutePath().normalize();
    }

    private static boolean isExternal(final String name) {
        return !name.isEmpty() && ShaderBridge.bundledRoot(name) == null;
    }

    private boolean apply() {
        final String target = this.enabled ? this.selected : "";
        // "Disabled" here turns off a downloaded pack; it leaves the Shaders tab's Lite alone
        final boolean packChange = !target.equals(currentApplied()) && (isExternal(target) || isExternal(currentApplied()));
        final boolean optionsFor = this.pack != null && this.pack.name.equals(this.selected);
        if (optionsFor && this.enabled && (!this.edits.isEmpty() || this.pickedProfile != null || this.resetAll)) {
            if (this.resetAll) {
                ShaderBridge.stageResetOptions();
            }
            if (this.pickedProfile != null) {
                ShaderBridge.stageProfile(this.pickedProfile);
            }
            for (final Map.Entry<String, String> e : this.edits.entrySet()) {
                ShaderBridge.stageOption(e.getKey(), e.getValue());
            }
        }
        this.edits.clear();
        this.pickedProfile = null;
        this.resetAll = false;
        if (packChange) {
            this.appliedNow = target;
            this.host.applyShaderPack(target);
        }
        return true;
    }

    private String appliedNow;

    private String currentApplied() {
        return this.appliedNow != null ? this.appliedNow : this.applied;
    }

    private void done() {
        apply();
        this.host.closeShaderPage();
    }

    private void cancel() {
        this.edits.clear();
        this.pickedProfile = null;
        this.resetAll = false;
        this.host.closeShaderPage();
    }

    // ================================================================== input

    public boolean mouseClicked(final double x, final double y, final int button) {
        for (final Button b : new ArrayList<>(this.buttons)) {
            if (b.active && inside(x, y, b.x1, b.y1, b.x2, b.y2)) {
                this.host.clickSound();
                b.action.run();
                return true;
            }
        }
        if (y < this.listTop || y >= this.listBottom) {
            return true;
        }
        if (this.settings) {
            for (final Slot s : this.slots) {
                if (inside(x, y, s.x1, s.y1, s.x2, s.y2)) {
                    if (s.element.kind == Element.SLIDER && button == 0 && !shiftDown) {
                        this.dragging = s;
                        slide(s, x);
                    } else {
                        this.host.clickSound();
                        cycle(s.element, button == 1 ? -1 : 1, shiftDown);
                    }
                    return true;
                }
            }
        } else {
            final Button t = this.listButton;
            if (t != null && inside(x, y, t.x1, t.y1, t.x2, t.y2)) {
                this.host.clickSound();
                t.action.run();
                return true;
            }
            if (x >= this.packX1 && x < this.packX2 && y >= this.packTop) {
                final int i = (int) ((y - this.packTop) / ENTRY_H);
                if (i >= 0 && i < this.packs.size()) {
                    final String name = this.packs.get(i);
                    if (!name.equals(this.selected)) {
                        this.edits.clear();
                        this.pickedProfile = null;
                        this.resetAll = false;
                        this.pack = null;
                    }
                    this.selected = name;
                    this.enabled = true; // picking a pack is choosing to use shaders, as in Iris
                    this.host.clickSound();
                    return true;
                }
            }
        }
        this.touchDrag = true;
        this.touchLastY = y;
        this.touchTravel = 0;
        return true;
    }

    /** Set by the host from the key modifiers before forwarding a click (Iris: Shift+click resets). */
    public boolean shiftDown;

    public boolean mouseDragged(final double x, final double y, final int button) {
        if (this.dragging != null) {
            slide(this.dragging, x);
            return true;
        }
        if (this.touchDrag) {
            final double dy = y - this.touchLastY;
            this.touchLastY = y;
            this.touchTravel += Math.abs(dy);
            scrollBy(-dy);
            return true;
        }
        return false;
    }

    public boolean mouseReleased(final double x, final double y, final int button) {
        this.dragging = null;
        this.touchDrag = false;
        return true;
    }

    public boolean mouseScrolled(final double x, final double y, final double amount) {
        scrollBy(-amount * 18);
        return true;
    }

    private void scrollBy(final double d) {
        if (this.settings) {
            this.optionScroll += d;
        } else {
            this.listScroll += d;
        }
        clampScroll(this.contentHeight);
    }

    /** Escape: back out of a sub screen, else Cancel. */
    public boolean keyPressed(final int key) {
        if (key == 256) {
            if (this.settings) {
                back();
            } else {
                cancel();
            }
            return true;
        }
        return false;
    }

    private static boolean inside(final double x, final double y, final int x1, final int y1, final int x2, final int y2) {
        return x >= x1 && x < x2 && y >= y1 && y < y2;
    }

    private static final class Button {
        final int x1;
        final int y1;
        final int x2;
        final int y2;
        final String label;
        final Runnable action;
        boolean active = true;
        boolean clipped;
        int valueColor;

        Button(final int x1, final int y1, final int x2, final int y2, final String label, final Runnable action) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.label = label;
            this.action = action;
        }
    }

    private static final class Slot {
        final Element element;
        final int x1;
        final int y1;
        final int x2;
        final int y2;

        Slot(final Element element, final int x1, final int y1, final int x2, final int y2) {
            this.element = element;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
    }
}
