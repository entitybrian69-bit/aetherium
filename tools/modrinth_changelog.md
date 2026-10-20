# Modrinth changelog

One `## <mod_version>` section per release. tools/modrinth_publish.py uses the section matching
`mod_version` in gradle.properties as the changelog of every Modrinth version it uploads.

## 1.18.1
**Shader fixes for Android GL translators (test build for Minecraft 1.16.5 only).**

- Fixed the world drawing as flat fog colour with a black hand on Krypton and gl4es: these translators accept fewer texture units than they report, so pack textures landed on Minecraft's block atlas and lightmap. Aetherium now tests how many units really work and keeps everything inside that limit.
- Fixed all text disappearing after switching a pack, profile or option: Minecraft is now told to re-bind its own textures after a pack loads and after every full-screen pass.
- Full-screen passes can use 3 more texture units, and sampler names that point to the same texture share one unit, so packs fit better on 8-unit devices. A pack that still needs more lists what was dropped under Problems in the Shaders tab.
- The device line in the Shaders tab (and logs/aetherium-shaders.log) now shows the usable texture units.

## 1.18.0
**Shader packs (test build for Minecraft 1.16.5 only).** Aetherium now loads OptiFine/Iris-format shader packs by itself, without OptiFine, Iris or Sodium.

- Put a pack (folder or .zip) into the shaderpacks folder and pick it in the new Shaders tab. Aetherium Lite, a light built-in pack made for phones, is always there.
- The Shaders tab shows the pack's own option screens, sliders and profiles, a status line with problems, your device, Reload and Reset. Options are saved per pack.
- Full pipeline: shadow map, gbuffers, deferred, composite and final passes, colortex0-15 with formats, clears and ping-pong, custom noise texture, block and item IDs (block.properties, item.properties), mc_Entity, mc_midTexCoord and at_tangent, OptiFine uniforms and custom uniforms, world folders (world-1, world1).
- Modern OpenGL mode on PC: packs compile as GLSL up to 4.50 compatibility, and packs written for lenient drivers (GLSL 1.30 code under version 120, late extension lines, missing DRAWBUFFERS) are fixed up so they also run on strict drivers.
- Compatibility mode for Android GL translators (gl4es, Krypton): automatic, or forced on or off in the Shaders tab. It targets GLSL 1.20 and halves the shadow map.
- Tested in CI on Mesa llvmpipe: BSL 10.1.8, Complementary Reimagined r5.9.3, MakeUp Ultra Fast 9.6, Sildur's Vibrant 2.02 and Aetherium Lite all compile with no errors in both modes and render the world.
- While a pack is on, the Aetherium renderer, render scale, far terrain, shader presets and the GL state cache step aside.
- Not supported yet: shadowcomp passes, normal and specular maps, resource-pack textures in packs, buffer scale and flip overrides, custom blend and alpha overrides.

## 1.17.0
**Quality of life on every version.** The QoL tab, tested on Minecraft 1.16.5 in the 1.16.x GitHub test builds, now works on every version from 1.16.5 to 26.3. Every feature is off by default, runs only on your client, and works on any server, including servers without Aetherium.

- Accurate placement: while you hold Use and move, a block goes down as soon as the crosshair reaches a new spot, so floors and walls come out without gaps or double blocks.
- Inventory: scroll over a stack to move one item at a time; hold Shift and drag to shift-click every stack you pass; a Sort button (and an optional Sort key) for chests, barrels, shulker boxes, hoppers, dispensers and your own inventory.
- Food info: hunger and saturation in food tooltips, and your hidden saturation drawn on the hunger bar as an AppleSkin-style gold outline (traced from your resource pack). The hunger and saturation held food would restore flash on the bar.
- Waypoints per world and dimension, with optional beams and Nether/Overworld conversion (B adds one, N opens the list), plus a death point and coordinates on the death screen.
- Nether and Overworld coordinates on the HUD, a lower fire overlay (0 hides it), a lowered shield while blocking, floating damage and heal numbers.
- Custom crosshair: style, colour, size, thickness, gap, opacity, outline, and it turns red while you aim at a mob or player.
- Punchy-style hands: a hand editor with presets and live preview (the menu fades away while you drag a slider), Show hand with items (your arm holds what you carry, like Punchy and Hold My Items), Show both hands (the empty off hand too) and Arms sideways, height, distance and size for both hands together.
- Zoom and freelook options moved to the QoL tab.
- An option a version cannot use is hidden on that version: the hand editor and arms are not on 26.3 yet.

## 1.16.3
**Punchy-style arms (test build for Minecraft 1.16.5 only).**

- New Arms settings in QoL, like Punchy's arm sculpt: Arms sideways, Arms height, Arms distance (farther or closer) and Arms size move both of your hands together, whether they hold something or not. They work on their own, without turning the hand editor on.
- Show both hands: your off hand is drawn even when it is empty, so you always see both hands, like Punchy's visible hands at all times (vanilla hides an empty off hand).
- Touching any Arms setting turns on Show both hands and Show hand with items, and every change previews live on your hands while you adjust.

## 1.16.2
**Visible hand (test build for Minecraft 1.16.5 only).**

- New "Show hand with items" switch in QoL, right under Hand editor: your arm is drawn holding whatever you carry in first person, like Punchy and Hold My Items (vanilla only draws the arm when the hand is empty). The held item is lifted into view so the fist around it shows, and the arm follows every swing, eat, bow, block and equip animation. Works for the off hand too, uses your own skin and sleeves, and moves with the hand editor values.
- Picking a hand preset or changing a hand value turns the visible hand on together with the editor, and it previews live while you adjust.
- Much wider hand editor ranges: position up to 2 blocks either way (was half a block), tilt, turn and roll up to 180 degrees (was 90), size 10 to 300 percent (was 25 to 200).

## 1.16.1
**Quality of life fixes (test build for Minecraft 1.16.5 only).**

- Hand editor live preview, like Punchy's: while you drag a hand slider, the whole menu fades away and only that slider stays on screen, so you see your hand change as you move it. Picking a preset or switching the editor or "Same for off hand" shows the result for a moment the same way (click anywhere to bring the menu back early). Works in a world, in first person.
- Hand presets work: picking one moves your hand at once (before Apply) and turns the hand editor on by itself. The presets are now further apart so each one is easy to tell from the others.
- The Punchy preset is gone. Presets: Default, Lowered, Centered, Small, Big.
- Saturation on the hunger bar is now drawn like AppleSkin: a gold outline traced around each shank (partly drawn for a partly saturated shank) instead of a colour tint. The outline is traced from the shank of your resource pack, so it fits custom packs too.
- While you hold food, the shanks it would fill flash over a faint highlight, and the saturation it would add flashes as outline, both exactly where vanilla shakes the bar when you are starving.

## 1.16.0
**Quality of life tab (test build for Minecraft 1.16.5 only).** Twelve small features in a new QoL tab. All are off by default, run only on your client, and work on any server, including servers without Aetherium.

- Accurate placement: while you hold Use and move, a block is placed as soon as the crosshair reaches a new spot, so floors and walls come out without gaps or double blocks.
- Mouse tweaks: scroll over a stack to move one item at a time to the other side; hold Shift and drag over slots to shift-click every stack you pass.
- Sort button: a small button on chests, barrels, ender chests, shulker boxes, hoppers, dispensers and your own inventory that merges and orders the stacks. An optional Sort key can be bound in Controls.
- Food info: hunger and saturation in food tooltips; your hidden saturation as a gold tint on the hunger bar, and the hunger held food would restore flashes on it.
- Death point: a waypoint where you last died, plus the coordinates on the death screen.
- Waypoints: per world and dimension, with optional beams and Nether/Overworld conversion. B adds one, N opens the manager.
- Nether and Overworld coordinates on the HUD.
- Lower fire overlay (flame height slider, 0 hides it) and a lowered shield while blocking.
- Floating damage numbers (and optional heal numbers) above nearby mobs and players.
- Custom crosshair: style, colour, size, thickness, gap, opacity, outline, and it turns red while you aim at a mob or player.
- Hand editor: move, rotate and scale the held item, mirror it, or pick a preset (Lowered, Centered, Small, Big).
- Zoom and freelook options moved to the QoL tab.

## 1.15.0
**Far terrain and the chunk streaming tweaks on every version.** Both were Minecraft 1.16.5 only until now; they work on every version from 1.16.5 to 26.3.

- Far terrain (Quality page, Overworld): a cheap low-detail outline of the land beyond your render distance, built from chunks you already received this session. Chunks inside your render distance that are not built yet show the outline too, and the distance fog moves out to the new edge.
- On 1.17 and newer it has its own small OpenGL 3 renderer: a few large draws for the whole outline, nothing added to the real chunks, fog and daylight done on the GPU.
- It turns itself off after any error, with shader packs, and on the Vulkan backend (26.2+). Off (0) by default.
- Merge chunk rebuilds and Smarter chunk culling (sections hidden in the fog are neither drawn nor rebuilt) now also run on 26.2 and 26.3, so every version has the full chunk streaming set.

## 1.14.0
**Render scale on every version.** The Render scale slider (Performance page), until now only on Minecraft 1.16.5, works on every version from 1.16.5 to 26.3.

- Below 100%, the 3D world is drawn at 50 to 95% of the window resolution and stretched back with smooth filtering. The HUD, chat, text and menus stay at full resolution and stay sharp.
- The biggest single frame-rate gain on weak GPUs and phones, where the cost of filling pixels dominates.
- Full resolution comes back by itself while glowing entities are on screen (so outlines stay in place) and with Fabulous graphics (Improved Transparency on 26.x).
- Off (100%) by default. Nothing changes unless you move the slider.

## 1.13.0
Everything added since 1.8.2, now on every version from 1.16.5 to 26.3 wherever that version of Minecraft still has the code a feature changes. An option a version cannot use is hidden there.

**Performance**
- Directional culling: the chunk visibility search never steps back towards you, so hidden caves and rooms are not drawn or built.
- View-first chunk building: sections in front of you are built before the ones behind you.
- Crowd culling, Nether roof culling and opt-in ocean floor culling.
- Entity detail distance: name tags, shadows, item frames, signs, minecarts, armour and held items are skipped far away.
- Block entity limit per frame, throttled maps, explosion particle budget, merged light updates.
- Smooth autosave, elytra chunk preloading, portal preloading and faster loading screens.
- Calmer translucent sorting, lighter weather, light table updated 10 times a second, a bigger smooth-lighting cache.
- GL call trimming (off next to OptiFine, Iris, Canvas or Sodium) on versions that still use OpenGL directly.
- Dynamic FPS, Low-end mode, Hitch guard, lazy data fixers, and a much lighter world behind menus.

**Quality**
- Cloud distance slider, fast grass, leaves mode with leaf culling, glint and End crystal beam toggles, colour filter (Vibrant, Warm or Cinematic).
- Mod Menu button.

**Minecraft 1.16.5 only:** far terrain and the chunk streaming tweaks (render scale came to every version in 1.14.0).

## 1.8.2
Removed the leftover Iris mention from the mod description.
