Aetherium Backport brings the Aetherium performance and video settings mod to **Minecraft 1.12.2 on Forge**. It is client-side only.

It replaces the vanilla Video Settings screen with its own animated screen, and adds frame-rate features that work together with the vanilla renderer.

**Required:** [MixinBooter](https://modrinth.com/mod/mixinbooter) (use the latest version).

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
- **Minecraft:** 1.12.2 only. For newer versions, see the main Aetherium project.
- **Loader:** Forge (14.23.5.2847 or newer recommended) with MixinBooter.
- **Client only:** not needed on servers. You can join any server with it.
- **OptiFine:** the Aetherium terrain renderer turns itself off when OptiFine is installed. The rest of the mod keeps working.
- **Graphics:** targets OpenGL 3.0 and older-style rendering, so it also runs on old GPUs.

## Links
- Source code: https://github.com/entitybrian69-bit/aetherium/tree/arena/46f241da-aetherium/backport
- Bug reports: https://github.com/entitybrian69-bit/aetherium/issues

Licensed under LGPL-3.0-only.
