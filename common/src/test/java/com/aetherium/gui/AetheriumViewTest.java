package com.aetherium.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Drives the real settings view headlessly: a recording canvas, a fake host and a fake clock.
 * Covers the scrollbar, tap-vs-drag on touch screens, fling, the theme switch and UI sounds.
 */
final class AetheriumViewTest {

    private static final int W = 427;
    private static final int H = 240;
    private static final int ROWS = 30;

    private static final class Canvas extends GuiCanvas {
        void frame() {
            beginFrame();
        }

        @Override
        protected void rawFill(final int x1, final int y1, final int x2, final int y2, final int argb) {
        }

        @Override
        protected void rawText(final String text, final int x, final int y, final int argb) {
        }

        @Override
        protected int rawTextWidth(final String text) {
            return text.length() * 6;
        }

        @Override
        protected void rawPushClip(final int x1, final int y1, final int x2, final int y2) {
        }

        @Override
        protected void rawPopClip(final boolean restore, final int x1, final int y1, final int x2, final int y2) {
        }
    }

    private static final class Host implements ScreenHost {
        boolean touch;
        boolean dark;
        int darkWrites;
        final List<UiSound> sounds = new ArrayList<UiSound>();
        final List<Setting> applied = new ArrayList<Setting>();

        @Override
        public void applyChanges(final List<Setting> changed) {
            this.applied.addAll(changed);
        }

        @Override
        public void requestClose() {
        }

        @Override
        public int currentFps() {
            return 60;
        }

        @Override
        public String footerLeft() {
            return "left";
        }

        @Override
        public String footerRight() {
            return "right";
        }

        @Override
        public boolean touchMode() {
            return this.touch;
        }

        @Override
        public boolean darkMode() {
            return this.dark;
        }

        @Override
        public void setDarkMode(final boolean value) {
            this.dark = value;
            this.darkWrites++;
        }

        @Override
        public void playSound(final UiSound sound) {
            this.sounds.add(sound);
        }
    }

    private Host host;
    private AetheriumView view;
    private Canvas canvas;
    private long now;
    private final int[] values = new int[ROWS];

    @BeforeEach
    void setUp() {
        this.host = new Host();
        build(false);
    }

    @AfterEach
    void lightAgain() {
        AetheriumTheme.apply(0f);
    }

    private void build(final boolean touch) {
        this.host.touch = touch;
        final Page page = new Page("Test", "Test", "Rows", PixelArt.GEAR);
        for (int i = 0; i < ROWS; i++) {
            final int index = i;
            this.values[i] = 0;
            page.add(Setting.toggle("row." + i, "Row " + i, "Toggle " + i,
                    () -> this.values[index], v -> this.values[index] = v));
        }
        this.view = new AetheriumView(Collections.singletonList(page), this.host, 0);
        this.now = 1_000_000_000L;
        this.view.setTimeSource(() -> this.now);
        this.view.resize(W, H);
        this.canvas = new Canvas();
        frames(40, -1, -1); // let the open animation settle
    }

    private void frames(final int count, final int mx, final int my) {
        for (int i = 0; i < count; i++) {
            this.now += 16_666_667L;
            this.canvas.frame();
            this.view.render(this.canvas, mx, my);
        }
    }

    private Setting row(final int index) {
        return this.view.find("row." + index);
    }

    @Test
    @DisplayName("the scrollbar has its own lane and the controls end left of it")
    void layoutReservesScrollbarLane() {
        final int[] sb = this.view.scrollbarForTest();
        assertTrue(this.view.maxScrollForTest() > 0, "30 rows must overflow a 240 px screen");
        assertEquals(6, sb[2] - sb[0], "desktop lane width");
        assertTrue(this.view.ctrlRightForTest() <= sb[0] - 8, "controls must end at least 8 px before the lane");
        build(true);
        final int[] touchSb = this.view.scrollbarForTest();
        assertEquals(8, touchSb[2] - touchSb[0], "touch mode gets the thicker lane");
    }

    @Test
    @DisplayName("dragging the thumb scrolls proportionally, and to the very end")
    void thumbDrag() {
        int[] sb = this.view.scrollbarForTest();
        final int x = (sb[0] + sb[2]) / 2;
        final int grabY = sb[4] + 3;
        assertTrue(this.view.mouseClicked(x, grabY, 0));
        this.view.mouseDragged(x, grabY + 20, 0);
        frames(1, x, grabY + 20);
        final float travel = (sb[3] - sb[1]) - (sb[5] - sb[4]);
        final float expected = 20f / travel * this.view.maxScrollForTest();
        assertEquals(expected, this.view.scrollPosition(), 1.0f);
        this.view.mouseDragged(x, H + 500, 0);
        this.view.mouseReleased(x, H + 500, 0);
        frames(1, -1, -1);
        assertEquals(this.view.maxScrollForTest(), this.view.scrollPosition(), 0.01f);
        sb = this.view.scrollbarForTest();
        assertEquals(sb[3], sb[5], "thumb sits at the bottom of the track");
        for (int i = 0; i < ROWS; i++) {
            assertEquals(0, this.values[i], "thumb dragging must never toggle rows");
        }
    }

    @Test
    @DisplayName("clicking the track jumps there and the thumb keeps following the pointer")
    void trackClick() {
        final int[] sb = this.view.scrollbarForTest();
        final int x = (sb[0] + sb[2]) / 2;
        this.view.mouseClicked(x, sb[3] - 2, 0);
        frames(30, x, sb[3] - 2);
        assertTrue(this.view.scrollPosition() > this.view.maxScrollForTest() * 0.8f);
        this.view.mouseDragged(x, sb[1], 0);
        this.view.mouseReleased(x, sb[1], 0);
        frames(1, -1, -1);
        assertEquals(0f, this.view.scrollPosition(), 0.01f);
    }

    @Test
    @DisplayName("a tap toggles on release, with the right sound")
    void tapTogglesOnRelease() {
        final int y = this.view.rowTopForTest(1) + this.view.rowHeightForTest() / 2;
        final int x = this.view.ctrlRightForTest() - 6;
        this.view.mouseClicked(x, y, 0);
        assertEquals(0, row(1).pending(), "nothing happens on press");
        this.view.mouseReleased(x, y, 0);
        assertEquals(1, row(1).pending(), "the toggle flips on release");
        assertTrue(this.host.sounds.contains(UiSound.TOGGLE_ON));
        this.view.mouseClicked(x, y, 0);
        this.view.mouseReleased(x, y, 0);
        assertEquals(0, row(1).pending());
        assertEquals(UiSound.TOGGLE_OFF, this.host.sounds.get(this.host.sounds.size() - 1));
    }

    @Test
    @DisplayName("touch: a swipe that starts on a row scrolls the list instead of toggling it, then flings")
    void swipeScrollsAndFlings() {
        build(true);
        final int x = this.view.ctrlRightForTest() - 6;
        final int startY = this.view.rowTopForTest(5) + 4;
        this.view.mouseClicked(x, startY, 0);
        int y = startY;
        for (int i = 0; i < 6; i++) {
            this.now += 16_666_667L;
            y -= 12;
            this.view.mouseDragged(x, y, 0);
        }
        final float atRelease = this.view.scrollPosition();
        assertEquals(72f, atRelease, 1f, "the list follows the finger 1:1");
        this.view.mouseReleased(x, y, 0);
        for (int i = 0; i < ROWS; i++) {
            assertEquals(0, row(i).pending(), "a swipe must not toggle anything");
        }
        assertTrue(this.view.scrollVelocityForTest() > 300f, "a quick swipe keeps momentum");
        frames(5, -1, -1);
        assertTrue(this.view.scrollPosition() > atRelease + 10f, "fling keeps moving after release");
        frames(240, -1, -1);
        assertEquals(0f, this.view.scrollVelocityForTest(), 0f, "friction stops the fling");
        final float settled = this.view.scrollPosition();
        frames(10, -1, -1);
        assertEquals(settled, this.view.scrollPosition(), 0.001f);
        assertTrue(settled <= this.view.maxScrollForTest());
    }

    @Test
    @DisplayName("touch: a small finger wobble is still a tap")
    void wobbleIsTap() {
        build(true);
        final int x = this.view.ctrlRightForTest() - 6;
        final int y = this.view.rowTopForTest(2) + this.view.rowHeightForTest() / 2;
        this.view.mouseClicked(x, y, 0);
        this.view.mouseDragged(x + 3, y - 4, 0);
        this.view.mouseReleased(x + 3, y - 4, 0);
        assertEquals(1, row(2).pending());
        assertEquals(0f, this.view.scrollPosition(), 0.001f);
    }

    @Test
    @DisplayName("a slow drag released after a pause does not fling")
    void pauseKillsFling() {
        build(true);
        final int x = this.view.ctrlRightForTest() - 6;
        final int y = this.view.rowTopForTest(5) + 4;
        this.view.mouseClicked(x, y, 0);
        this.now += 16_666_667L;
        this.view.mouseDragged(x, y - 40, 0);
        this.now += 300_000_000L; // finger rests for 0.3 s
        this.view.mouseReleased(x, y - 40, 0);
        assertEquals(0f, this.view.scrollVelocityForTest(), 0f);
    }

    @Test
    @DisplayName("the header switch flips the theme, saves it immediately, and cross-fades")
    void themeSwitch() {
        final int[] sw = this.view.themeSwitchForTest();
        assertTrue(sw[0] > W / 2 && sw[2] <= W, "switch sits in the right half of the header");
        this.view.mouseClicked((sw[0] + sw[2]) / 2, (sw[1] + sw[3]) / 2, 0);
        assertTrue(this.host.dark);
        assertEquals(1, this.host.darkWrites);
        assertTrue(this.host.sounds.contains(UiSound.THEME));
        frames(3, -1, -1);
        final float mid = AetheriumTheme.darkness();
        assertTrue(mid > 0f && mid < 1f, "the theme cross-fades instead of snapping (was " + mid + ")");
        frames(60, -1, -1);
        assertEquals(1f, AetheriumTheme.darkness(), 0.01f);
        assertTrue(this.host.applied.isEmpty(), "the theme is not a staged option");
    }

    @Test
    @DisplayName("the view opens in the saved theme without animating")
    void opensInSavedTheme() {
        this.host.dark = true;
        build(false);
        assertEquals(1f, AetheriumTheme.darkness(), 0f);
    }

    @Test
    @DisplayName("the mouse wheel stops a running fling")
    void wheelStopsFling() {
        build(true);
        final int x = this.view.ctrlRightForTest() - 6;
        int y = this.view.rowTopForTest(5) + 4;
        this.view.mouseClicked(x, y, 0);
        for (int i = 0; i < 4; i++) {
            this.now += 16_666_667L;
            y -= 15;
            this.view.mouseDragged(x, y, 0);
        }
        this.view.mouseReleased(x, y, 0);
        assertTrue(this.view.scrollVelocityForTest() != 0f);
        this.view.mouseScrolled(x, y, -1);
        assertEquals(0f, this.view.scrollVelocityForTest(), 0f);
        assertFalse(Float.isNaN(this.view.scrollPosition()));
    }

    private int rowCenterY(final int index) {
        return this.view.rowTopForTest(index) + this.view.rowHeightForTest() / 2;
    }

    @Test
    @DisplayName("hovering a row opens a popup with the whole description, after a short delay")
    void descriptionPopup() {
        final String longText = "Chests, signs, banners, heads and other block entities farther than this many blocks"
                + " are not drawn (64 = vanilla; beacon beams are exempt).";
        final Page page = new Page("Test", "Test", "Rows", PixelArt.GEAR);
        page.add(Setting.toggle("long", "Long", longText, () -> 0, v -> { }));
        page.add(Setting.toggle("short", "Short", "Short text.", () -> 0, v -> { }));
        this.view = new AetheriumView(Collections.singletonList(page), this.host, 0);
        this.view.setTimeSource(() -> this.now);
        this.view.resize(W, H);
        frames(40, -1, -1);
        final int x = 150;
        final int y = rowCenterY(0);
        frames(3, x, y);
        assertEquals(null, this.view.tooltipForTest(), "no popup before the hover delay");
        frames(40, x, y);
        assertEquals(this.view.find("long"), this.view.tooltipForTest());
        final List<String> lines = this.view.tooltipLinesForTest();
        assertTrue(lines.size() >= 2, "a long description must wrap instead of being cut: " + lines);
        assertEquals(longText, String.join(" ", lines), "every word of the description is shown, none dropped");
        for (final String line : lines) {
            assertTrue(line.length() * 6 <= 240, "every line fits the popup: " + line);
        }
        // Sliding to the next row while open swaps at once.
        frames(1, x, rowCenterY(1));
        frames(1, x, rowCenterY(1));
        assertEquals(this.view.find("short"), this.view.tooltipForTest());
        // Leaving the rows closes it.
        frames(40, -1, -1);
        assertEquals(null, this.view.tooltipForTest());
    }

    @Test
    @DisplayName("the popup stays closed while scrolling and, on touch, for the row just tapped")
    void popupHidesWhileBusy() {
        final int x = 150;
        final int y = rowCenterY(2);
        frames(40, x, y);
        assertTrue(this.view.tooltipForTest() != null);
        this.view.mouseScrolled(x, y, -3);
        build(true);
        final int ty = rowCenterY(1);
        this.view.mouseClicked(x, ty, 0);
        this.view.mouseReleased(x, ty, 0);
        frames(60, x, ty);
        assertEquals(null, this.view.tooltipForTest(), "a tapped row must not be covered by its own popup");
        frames(60, x, rowCenterY(3));
        assertTrue(this.view.tooltipForTest() != null, "moving to another row shows its popup again");
    }

    @Test
    @DisplayName("fit() keeps the longest prefix that fits and adds an ellipsis")
    void fitIsExact() {
        final Canvas c = new Canvas();
        assertEquals("abcdefghij", AetheriumView.fit(c, "abcdefghij", 60));
        assertEquals("abcdefg...", AetheriumView.fit(c, "abcdefghijk", 60));
        assertEquals("", AetheriumView.fit(c, "abcdef", 10));
        for (int w = 18; w < 200; w++) {
            final String out = AetheriumView.fit(c, "The quick brown fox jumps over the lazy dog", w);
            assertTrue(out.length() * 6 <= w, "fits at width " + w);
            final String longer = out.endsWith("...") ? out.substring(0, out.length() - 3) : out;
            if (out.endsWith("...")) {
                assertTrue((longer.length() + 1 + 3) * 6 > w, "the prefix is the longest one that fits at " + w);
            }
        }
    }
}
