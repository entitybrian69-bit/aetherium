# Modrinth changelog

One `## <mod_version>` section per release. tools/modrinth_publish.py uses the section matching
`mod_version` in gradle.properties as the changelog of every Modrinth version it uploads.

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

**Minecraft 1.16.5 only:** far terrain, render scale and the chunk streaming tweaks.

## 1.8.2
Removed the leftover Iris mention from the mod description.
