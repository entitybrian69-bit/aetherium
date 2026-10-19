package com.aetherium.qol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aetherium.config.AetheriumConfig;
import com.aetherium.config.ConfigValue;
import com.aetherium.gui.GuiCanvas;
import com.aetherium.gui.UiSound;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 1.16.0 Quality of life: the Minecraft-free logic behind each feature. */
final class QolLogicTest {

    // ------------------------------------------------------------------ sort

    /**
     * A container simulated with vanilla's left-click PICKUP rules, driven by exactly the click
     * sequence QolInventory uses for one planner move (pick up from, click to, put back if needed).
     */
    private static final class Sim implements SortPlanner.Slots {
        final int[] item;
        final int[] count;
        int carriedItem;
        int carriedCount;
        int clicks;

        Sim(final int[] item, final int[] count) {
            this.item = item.clone();
            this.count = count.clone();
        }

        static int max(final int id) {
            return id == 99 ? 1 : id % 3 == 0 ? 16 : 64;
        }

        void click(final int s) {
            this.clicks++;
            if (this.carriedCount == 0) {
                if (this.count[s] > 0) {
                    this.carriedItem = this.item[s];
                    this.carriedCount = this.count[s];
                    this.count[s] = 0;
                    this.item[s] = 0;
                }
                return;
            }
            if (this.count[s] == 0) {
                this.item[s] = this.carriedItem;
                this.count[s] = this.carriedCount;
                this.carriedCount = 0;
                this.carriedItem = 0;
                return;
            }
            if (this.item[s] == this.carriedItem) {
                final int moved = Math.min(max(this.item[s]) - this.count[s], this.carriedCount);
                this.count[s] += moved;
                this.carriedCount -= moved;
                if (this.carriedCount == 0) {
                    this.carriedItem = 0;
                }
                return;
            }
            final int i = this.item[s];
            final int c = this.count[s];
            this.item[s] = this.carriedItem;
            this.count[s] = this.carriedCount;
            this.carriedItem = i;
            this.carriedCount = c;
        }

        void perform(final SortPlanner.Move move) {
            click(move.from);
            click(move.to);
            if (this.carriedCount > 0) {
                click(move.from);
            }
        }

        int total(final int id) {
            int sum = 0;
            for (int i = 0; i < this.item.length; i++) {
                if (this.count[i] > 0 && this.item[i] == id) {
                    sum += this.count[i];
                }
            }
            return sum;
        }

        @Override
        public int size() {
            return this.item.length;
        }

        @Override
        public boolean isEmpty(final int slot) {
            return this.count[slot] == 0;
        }

        @Override
        public boolean sameItem(final int a, final int b) {
            return this.count[a] > 0 && this.count[b] > 0 && this.item[a] == this.item[b];
        }

        @Override
        public int count(final int slot) {
            return this.count[slot];
        }

        @Override
        public int maxCount(final int slot) {
            return max(this.item[slot]);
        }

        @Override
        public int compareItems(final int a, final int b) {
            return Integer.compare(this.item[a], this.item[b]);
        }
    }

    /** Runs the sort like QolInventory.tick (one move per step, 200 at most); returns the moves used. */
    private static int sort(final Sim sim) {
        int moves = 0;
        SortPlanner.Move move;
        while ((move = SortPlanner.next(sim)) != null) {
            assertTrue(moves < 200, "sort did not finish: " + move);
            sim.perform(move);
            assertEquals(0, sim.carriedCount, "cursor must be empty after every move");
            moves++;
        }
        return moves;
    }

    private static void assertSorted(final Sim sim) {
        boolean seenEmpty = false;
        int lastItem = Integer.MIN_VALUE;
        for (int i = 0; i < sim.size(); i++) {
            if (sim.count[i] == 0) {
                seenEmpty = true;
                continue;
            }
            assertFalse(seenEmpty, "stack after an empty slot at " + i);
            assertTrue(sim.item[i] >= lastItem, "out of order at " + i);
            if (sim.item[i] == lastItem) {
                assertTrue(sim.count[i - 1] == Sim.max(sim.item[i]) || sim.item[i] == 99,
                        "partial stack before another stack of the same item at " + (i - 1));
            }
            lastItem = sim.item[i];
        }
    }

    @Test
    @DisplayName("Sort merges partial stacks and orders by item, keeping every item")
    void sortSmallChest() {
        final Sim sim = new Sim(
                new int[] {5, 0, 2, 5, 7, 2, 0, 99, 99},
                new int[] {10, 0, 64, 60, 1, 3, 0, 1, 1});
        sort(sim);
        assertSorted(sim);
        assertEquals(70, sim.total(5));
        assertEquals(67, sim.total(2));
        assertEquals(2, sim.total(99));
        assertEquals(2, sim.item[0]);
        assertEquals(64, sim.count[0]);
        assertEquals(3, sim.count[1]);
        assertEquals(5, sim.item[2]);
        assertEquals(64, sim.count[2]);
        assertEquals(6, sim.count[3]);
    }

    @Test
    @DisplayName("Sort: random chests always end sorted, with nothing lost, within the move cap")
    void sortFuzz() {
        final Random random = new Random(1234);
        for (int round = 0; round < 400; round++) {
            final int n = random.nextBoolean() ? 27 : 54;
            final int[] item = new int[n];
            final int[] count = new int[n];
            final int kinds = 1 + random.nextInt(12);
            for (int i = 0; i < n; i++) {
                if (random.nextInt(3) == 0) {
                    continue;
                }
                item[i] = random.nextInt(20) == 0 ? 99 : 1 + random.nextInt(kinds);
                count[i] = 1 + random.nextInt(Sim.max(item[i]));
            }
            final Sim sim = new Sim(item, count);
            final int[] before = new int[100];
            for (int id = 0; id < 100; id++) {
                before[id] = sim.total(id);
            }
            sort(sim);
            assertSorted(sim);
            for (int id = 0; id < 100; id++) {
                assertEquals(before[id], sim.total(id), "item " + id + " changed in round " + round);
            }
            assertNull(SortPlanner.next(sim));
        }
    }

    @Test
    @DisplayName("Sort: an already sorted chest needs no clicks")
    void sortNoop() {
        final Sim sim = new Sim(new int[] {1, 1, 4, 0}, new int[] {64, 20, 16, 0});
        assertEquals(0, sort(sim));
        assertEquals(0, sim.clicks);
    }

    // ------------------------------------------------------------------ accurate placement

    @Test
    @DisplayName("Accurate placement: early only for a new spot, never onto the last block, at most every 2 ticks")
    void accuratePlacement() {
        final AccuratePlacement p = new AccuratePlacement();
        assertFalse(p.placeEarly(AccuratePlacement.NONE, 5L));
        assertTrue(p.placeEarly(1L, 2L), "fresh start may place");
        p.placed(2L);
        assertFalse(p.placeEarly(3L, 4L), "0 ticks since the last placement");
        p.tick();
        assertFalse(p.placeEarly(3L, 4L), "1 tick");
        p.tick();
        assertTrue(p.placeEarly(3L, 4L), "2 ticks, new spot");
        assertFalse(p.placeEarly(3L, 2L), "same spot as last time");
        assertFalse(p.placeEarly(2L, 9L), "building onto the block just placed keeps vanilla's rhythm");
        p.reset();
        assertTrue(p.placeEarly(2L, 9L));
    }

    // ------------------------------------------------------------------ waypoints

    @Test
    @DisplayName("Waypoints: per world, saved and loaded, one death point, names unique and capped")
    void waypointsStore() throws Exception {
        final Path dir = Files.createTempDirectory("aeth-wp");
        final Path file = dir.resolve("aetherium-waypoints.json");
        final Waypoints a = new Waypoints(file);
        assertNull(a.add(0, 64, 0, Waypoints.OVERWORLD), "no world selected yet");
        a.selectWorld("server:example.org");
        final Waypoints.Waypoint home = a.add(10, 70, -5, Waypoints.OVERWORLD);
        assertNotNull(home);
        assertEquals("Waypoint 1", home.name);
        home.name = Waypoints.clampName("Home base with a very long name indeed");
        assertEquals(Waypoints.MAX_NAME, home.name.length());
        a.changed();
        a.add(1, 2, 3, Waypoints.NETHER);
        a.setDeath(100, 12, 100, Waypoints.OVERWORLD);
        a.setDeath(200, 30, 200, Waypoints.NETHER);
        assertEquals(3, a.all().size());
        assertTrue(a.all().get(0).death);
        assertEquals(200, a.all().get(0).x);

        a.selectWorld("local:New World");
        assertEquals(0, a.all().size());
        a.add(0, 0, 0, Waypoints.OVERWORLD);

        final Waypoints b = new Waypoints(file);
        b.selectWorld("server:example.org");
        assertEquals(3, b.all().size());
        assertEquals(home.name, b.all().get(1).name);
        assertEquals(-5, b.all().get(1).z);
        assertTrue(b.all().get(0).death);
        b.selectWorld("local:New World");
        assertEquals(1, b.all().size());

        Files.write(file, "{ broken".getBytes(StandardCharsets.UTF_8));
        final Waypoints c = new Waypoints(file);
        c.selectWorld("server:example.org");
        assertEquals(0, c.all().size(), "a damaged file starts empty");
        assertTrue(Files.exists(dir.resolve("aetherium-waypoints.json.bad")), "and is kept aside");
    }

    @Test
    @DisplayName("Waypoints: Nether and Overworld coordinates convert by 8, the End does not")
    void waypointCoordinates() {
        assertEquals(100.0, Waypoints.convert(800.0, Waypoints.OVERWORLD, Waypoints.NETHER), 1e-9);
        assertEquals(800.0, Waypoints.convert(100.0, Waypoints.NETHER, Waypoints.OVERWORLD), 1e-9);
        assertEquals(5.0, Waypoints.convert(5.0, Waypoints.NETHER, Waypoints.NETHER), 1e-9);
        assertTrue(Double.isNaN(Waypoints.convert(5.0, "minecraft:the_end", Waypoints.OVERWORLD)));
        assertEquals("Nether", Waypoints.dimensionName(Waypoints.NETHER));
        assertEquals("twilight_forest", Waypoints.dimensionName("twilightforest:twilight_forest"));
    }

    // ------------------------------------------------------------------ waypoint screen

    private static final class TextCanvas extends GuiCanvas {
        final StringBuilder texts = new StringBuilder();

        @Override
        protected void rawFill(final int x1, final int y1, final int x2, final int y2, final int argb) {
        }

        @Override
        protected void rawText(final String text, final int x, final int y, final int argb) {
            this.texts.append(text).append('\n');
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

    private static final class Host implements WaypointView.Host {
        boolean closed;

        @Override
        public double playerX() {
            return 80.5;
        }

        @Override
        public double playerY() {
            return 64.0;
        }

        @Override
        public double playerZ() {
            return -16.5;
        }

        @Override
        public String dimension() {
            return Waypoints.OVERWORLD;
        }

        @Override
        public void close() {
            this.closed = true;
        }

        @Override
        public void playSound(final UiSound sound) {
        }

        @Override
        public boolean darkMode() {
            return false;
        }
    }

    @Test
    @DisplayName("Waypoint screen: add here, rename by typing, delete needs two clicks")
    void waypointView() throws Exception {
        final Waypoints store = new Waypoints(Files.createTempDirectory("aeth-wpv").resolve("w.json"));
        store.selectWorld("local:Test");
        final Host host = new Host();
        final WaypointView view = new WaypointView(store, host);
        view.resize(480, 270);
        final TextCanvas canvas = new TextCanvas();
        view.render(canvas, 0, 0);
        assertTrue(canvas.texts.toString().contains("No waypoints"));

        // "+ Add here" sits at px2-150..px2-74, py1+38..+52 with px2 = 40 + 400.
        assertTrue(view.mouseClicked(440 - 120, 10 + 44, 0));
        assertEquals(1, store.all().size());
        assertEquals(80, store.all().get(0).x);
        assertEquals(-17, store.all().get(0).z);
        // The new waypoint is being renamed: replace its name.
        for (int i = 0; i < 20; i++) {
            view.keyPressed(259);
        }
        for (final char c : "Base".toCharArray()) {
            view.charTyped(c);
        }
        view.keyPressed(257);
        assertEquals("Base", store.all().get(0).name);

        // Delete: px2-46..px2-12 on the first row (listY1 = py1 + 58).
        final int rowY = 10 + 58 + 12;
        view.mouseClicked(440 - 30, rowY, 0);
        assertEquals(1, store.all().size(), "first click only arms");
        view.mouseClicked(440 - 30, rowY, 0);
        assertEquals(0, store.all().size());

        assertTrue(view.mouseClicked(440 - 40, 10 + 44, 0), "Done");
        assertTrue(host.closed);
    }

    // ------------------------------------------------------------------ crosshair

    @Test
    @DisplayName("Crosshair: shapes are symmetric, the outline grows every rect by one pixel")
    void crosshair() {
        for (final Crosshair.Style style : Crosshair.Style.values()) {
            if (style == Crosshair.Style.VANILLA) {
                assertEquals(0, Crosshair.shape(style, 5, 1, 2).length);
                continue;
            }
            final int[] rects = Crosshair.shape(style, 5, 1, 2);
            assertTrue(rects.length > 0 && rects.length % 4 == 0, style.name());
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            for (int i = 0; i < rects.length; i += 4) {
                assertTrue(rects[i] < rects[i + 2] && rects[i + 1] < rects[i + 3], style + " has an empty rect");
                minX = Math.min(minX, rects[i]);
                maxX = Math.max(maxX, rects[i + 2]);
            }
            // Odd thickness: centred on the middle pixel [0, 1).
            assertEquals(-minX, maxX - 1, style + " is not centred");
            final int[] outline = Crosshair.outline(rects);
            assertEquals(rects.length, outline.length);
            for (int i = 0; i < rects.length; i += 4) {
                assertEquals(rects[i] - 1, outline[i]);
                assertEquals(rects[i + 3] + 1, outline[i + 3]);
            }
        }
        assertEquals(0x80, Crosshair.argb(0xFF0000, 50) >>> 24);
        assertEquals(0xFF0000, Crosshair.argb(0xFF0000, 50) & 0xFFFFFF);
        assertEquals(0xFF, Crosshair.argb(0, 100) >>> 24);
    }

    // ------------------------------------------------------------------ damage numbers

    @Test
    @DisplayName("Damage numbers: first sighting only records, a drop spawns a popup, unseen entities are forgotten")
    void damageNumbers() {
        final DamageNumbers d = new DamageNumbers();
        assertTrue(d.isIdle());
        d.beginTick();
        d.report(7, 20f, 0, 0, 0, false, 1000L, 0.5);
        d.endTick(1000L);
        assertTrue(d.isIdle(), "first report must not spawn");
        d.beginTick();
        d.report(7, 14.5f, 1, 2, 3, false, 1050L, 0.5);
        d.endTick(1050L);
        assertFalse(d.isIdle());
        DamageNumbers.Popup popup = null;
        for (int i = 0; i < d.capacity(); i++) {
            if (d.popup(i).isLive()) {
                popup = d.popup(i);
            }
        }
        assertNotNull(popup);
        assertEquals("5.5", popup.text());
        // Heals only with the option.
        d.beginTick();
        d.report(7, 18f, 0, 0, 0, false, 1100L, 0.5);
        d.endTick(1100L);
        int live = 0;
        for (int i = 0; i < d.capacity(); i++) {
            live += d.popup(i).isLive() ? 1 : 0;
        }
        assertEquals(1, live);
        // Expiry.
        d.beginTick();
        d.endTick(1050L + DamageNumbers.LIFETIME_MS + 1);
        assertTrue(d.isIdle());
        // Entity 7 was not reported in that tick, so it is forgotten: the next report only records.
        d.beginTick();
        d.report(7, 2f, 0, 0, 0, false, 5000L, 0.5);
        d.endTick(5000L);
        assertTrue(d.isIdle());
    }

    @Test
    @DisplayName("Damage numbers: the popup ring never grows past its capacity")
    void damageRing() {
        final DamageNumbers d = new DamageNumbers();
        d.beginTick();
        for (int id = 0; id < 200; id++) {
            d.report(id, 20f, 0, 0, 0, false, 0L, 0.0);
        }
        d.endTick(0L);
        d.beginTick();
        for (int id = 0; id < 200; id++) {
            d.report(id, 10f, 0, 0, 0, false, 10L, 0.0);
        }
        d.endTick(10L);
        assertEquals(DamageNumbers.MAX_POPUPS, d.capacity());
    }

    // ------------------------------------------------------------------ settings

    @Test
    @DisplayName("Hand editor: ranges go well past half a block and 90 degrees, and the visible arm is its own switch")
    void handRanges() {
        final AetheriumConfig config = new AetheriumConfig();
        assertEquals(-200, config.handX.getMin());
        assertEquals(200, config.handY.getMax());
        assertEquals(-200, config.handZ.getMin());
        assertEquals(-180, config.handPitch.getMin());
        assertEquals(180, config.handRoll.getMax());
        assertEquals(10, config.handScale.getMin());
        assertEquals(300, config.handScale.getMax());
        assertFalse(config.handVisible.get());
        Qol.configure(config, true);
        try {
            Qol.previewHand(true, true, true, -175, -120, 150, -170, 180, -90, 280);
            if (QolSupport.HAND) {
                assertTrue(Qol.hand && Qol.handVisible && Qol.handPreview);
                assertEquals(-1.75f, Qol.handX, 1e-6f);
                assertEquals(-1.2f, Qol.handY, 1e-6f);
                assertEquals(1.5f, Qol.handZ, 1e-6f);
                assertEquals(-170f, Qol.handPitch, 1e-6f);
                assertEquals(180f, Qol.handYaw, 1e-6f);
                assertEquals(2.8f, Qol.handScale, 1e-6f);
                Qol.previewHand(false, true, true, -999, 999, 0, -999, 999, 0, 999);
                assertTrue(!Qol.hand && Qol.handVisible && Qol.handPreview, "the arm previews without the editor");
                assertEquals(-2f, Qol.handX, 1e-6f);
                assertEquals(2f, Qol.handY, 1e-6f);
                assertEquals(-180f, Qol.handPitch, 1e-6f);
                assertEquals(3f, Qol.handScale, 1e-6f);
            }
        } finally {
            Qol.endHandPreview(config);
        }
        assertFalse(Qol.hand || Qol.handVisible || Qol.handPreview);
        Qol.configure(config, false);
    }

    @Test
    @DisplayName("Quality of life: everything off by default, presets inside the option ranges, food maths")
    void settings() {
        final AetheriumConfig config = new AetheriumConfig();
        Qol.configure(config, true);
        assertFalse(Qol.accuratePlacement || Qol.scrollTransfer || Qol.shiftDrag || Qol.sortButton || Qol.foodTooltip
                || Qol.foodHud || Qol.waypoints || Qol.deathPoint || Qol.dimensionCoords || Qol.damageNumbers
                || Qol.crosshair || Qol.hand || Qol.handVisible);
        assertEquals(100, Qol.fireOverlay);
        assertEquals(0, Qol.shieldOffset);
        assertEquals(AetheriumConfig.HandPreset.values().length - 1, Qol.HAND_PRESETS.length);
        assertEquals(Crosshair.Style.values().length, AetheriumConfig.CrosshairStyle.values().length);
        assertEquals(Crosshair.Color.values().length, AetheriumConfig.CrosshairColor.values().length);
        for (int i = 0; i < Crosshair.Style.values().length; i++) {
            assertEquals(Crosshair.Style.values()[i].name(), AetheriumConfig.CrosshairStyle.values()[i].name());
        }
        final ConfigValue<?>[] handValues = {config.handX, config.handY, config.handZ, config.handPitch,
            config.handYaw, config.handRoll, config.handScale};
        for (final int[] preset : Qol.HAND_PRESETS) {
            for (int i = 0; i < handValues.length; i++) {
                assertTrue(preset[i] >= (Integer) handValues[i].getMin() && preset[i] <= (Integer) handValues[i].getMax());
            }
        }
        assertEquals(7.2f, Qol.saturation(6, 0.6f), 1e-4f);
        assertEquals("7.2", Qol.oneDecimal(Qol.saturation(6, 0.6f)));
        assertEquals("6", Qol.oneDecimal(6.0f));
    }
}
