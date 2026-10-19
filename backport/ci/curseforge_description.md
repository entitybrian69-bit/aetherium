## Official port, not a third-party derivative

**Aetherium Backport is the official Minecraft 1.12.2 (Forge) version of Aetherium, made by the same author (Team_Aerix).** It is not based on anyone else's mod.

- **Original project (same author):** Aetherium on CurseForge: https://www.curseforge.com/projects/1749327 (and Aetherium NeoForge: https://www.curseforge.com/projects/1749357)
- **Also on Modrinth:** https://modrinth.com/mod/aetherium-team_aerix
- **License:** open source, **LGPL-3.0-only** for both Aetherium and this backport: https://github.com/entitybrian69-bit/aetherium/blob/main/LICENSE
- **Source code:** both live in the same public repository: https://github.com/entitybrian69-bit/aetherium (backport code: https://github.com/entitybrian69-bit/aetherium/tree/arena/46f241da-aetherium/backport)

### How it differs from the original Aetherium
- **Game version and loader:** Aetherium supports Minecraft 1.16.5 to 26.3 on Fabric and NeoForge. The Backport supports **only Minecraft 1.12.2 on Forge**, and requires MixinBooter.
- **Code:** settings, config, theme and GUI code is shared with Aetherium. All hooks into the game (the mixins) and the terrain renderer were rewritten for 1.12.2, because its rendering code is completely different.
- **Features:** a 1.12.2 subset. It has the settings screen, the terrain renderer, culling, smooth chunk loading, frame pacing, dynamic lights, zoom and freelook. It does not have newer Aetherium features such as Far terrain or Render scale.
- **Separate project** because CurseForge files are per game version and loader. Newer versions get Aetherium; 1.12.2 gets this.

---

Aetherium Backport brings the Aetherium performance and video settings mod to **Minecraft 1.12.2 on Forge**. It is client-side only.

It replaces the vanilla Video Settings screen with its own animated screen, and adds frame-rate features that work together with the vanilla renderer.

**Required:** MixinBooter (use the latest version).

## Features

**Aetherium terrain renderer** (on by default)
- Skips the faces of each chunk section that point away from the camera, so the GPU draws less terrain.
- Skips chunk sections that are completely hidden in the distance fog.
- Never issues more draw calls than vanilla.
- If anything goes wrong, it switches back to vanilla rendering by itself instead of crashing.

**Performance**
- Entity and block-entity distance culling.
- Smooth chunk loading: chunk uploads are spread over frames to stop stutter while new chunks appear.
- Even frame pacing with a max framerate set, for steadier frame times.
- Particle limits per type, and an option to pause animated textures.

**Video settings screen**
- Animated, tabbed screen: General, Quality, Performance, Backend, Effects and Android.
- Light and dark theme, a draggable scrollbar, touch scrolling and interface sounds.
- Hover over any option to see what it does.
- A frame-time overlay with FPS, frame time and 1% lows.

**Extras**
- Dynamic lights from held and dropped light sources.
- Zoom with smooth animation, scroll-to-zoom and rebindable Zoom / Zoom In / Zoom Out keys.
- Freelook, plus fog, sky, cloud height, weather and per-type particle controls.

## Compatibility
- **Minecraft:** 1.12.2 only. For newer versions, see the main Aetherium project linked above.
- **Loader:** Forge (14.23.5.2847 or newer recommended) with MixinBooter.
- **Client only:** not needed on servers. You can join any server with it.
- **OptiFine:** the Aetherium terrain renderer turns itself off when OptiFine is installed. The rest of the mod keeps working.
- **Graphics:** targets OpenGL 3.0 and older-style rendering, so it also runs on old GPUs.

## Links
- Bug reports: https://github.com/entitybrian69-bit/aetherium/issues
