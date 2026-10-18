# Modrinth changelog

One `## <mod_version>` section per release. tools/modrinth_publish.py uses the section matching
`mod_version` in gradle.properties as the changelog of every Modrinth version it uploads.

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
