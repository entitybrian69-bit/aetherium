Aetherium Backport 1.1.0 for Minecraft 1.8.9 (Forge 11.15.1.2318 or newer): the full 1.12.2 Backport feature set, ported to 1.8.9.

**Everything from the 1.12.2 Backport:**
- The animated Aetherium video settings screen (all tabs, hover descriptions, presets) in place of vanilla's Video Settings.
- The Aetherium terrain renderer (on by default, with automatic fallback to vanilla), frame pacing, entity and block-entity culling, fog culling, dynamic lights, and the unloaded-chunk stutter fix.
- Zume-style zoom (Zoom, Zoom In and Zoom Out keys, scroll to zoom, third-person zoom) and freelook.
- Fog, sky, cloud height, weather and particle options.
- Tuning: Low-end mode, Dynamic FPS (15 FPS in the background, 1 when minimized), a menu frame-rate cap, Hitch guard, entity detail distance, a block-entity limit per frame, an explosion cloud budget, a redstone particle limit, throttled map updates, Leaves mode, leaf culling, lighter rain and snow, and toggles for enchantment glint and End crystal beams.
- Quality of life tab (everything off by default, client-side only, works on any server): accurate placement, scroll transfer, Shift-drag, Sort button and Sort key, food tooltips and the saturation outline on the hunger bar, waypoints with beams and Nether/Overworld conversion, a death point, Nether/Overworld coordinates, a lower fire overlay, floating damage and heal numbers, a custom crosshair that turns red on mobs, and the Punchy-style hand editor with presets, live preview, Show hand with items and the arm sculpt.

**What is different on 1.8.9:** Minecraft 1.8.9 has no off hand and no shields, so "Same for off hand" and "Show both hands" show as not available, and "Lower raised shield" becomes "Lower sword block". Shulker boxes do not exist yet, so the Sort button covers chests, dispensers, hoppers and your inventory.

Nothing else is required: Mixin is bundled in the jar. Not compatible with OptiFine's renderer: with OptiFine installed, the Aetherium terrain renderer stays off and everything else still works.
