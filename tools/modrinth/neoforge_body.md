Aetherium is a client-side performance and video settings mod. This project is the **NeoForge** build. It needs nothing else installed: no extra library or API mod.

It replaces the vanilla Video Settings screen with its own animated screen, and adds frame-rate features that work together with the vanilla renderer.

## Features

**Aetherium terrain renderer** (on by default)
- Skips the faces of each chunk section that point away from the camera, so the GPU draws less terrain.
- Never issues more draw calls than vanilla.
- If anything goes wrong, it switches back to vanilla rendering by itself instead of crashing.

**Performance**
- Entity occlusion culling: mobs and block entities hidden behind terrain are not drawn.
- Smart chunk culling: chunk sections fully inside the distance fog are not drawn or rebuilt.
- Smooth chunk loading, worker-thread sizing and a block-entity render distance.
- Even frame pacing with a max framerate set, for steadier frame times.
- Fast particles, visible-only animated textures and fast mob models (on the Minecraft versions that support them).

**Video settings screen**
- Animated, tabbed screen: General, Quality, Performance, Backend, Effects and Android.
- Light and dark theme, a draggable scrollbar, touch scrolling and interface sounds.
- Hover over any option to see what it does.

**Extras**
- Dynamic lights from held and dropped light sources.
- Zoom with smooth animation, scroll-to-zoom and rebindable Zoom / Zoom In / Zoom Out keys.
- Freelook, plus fog, sky and per-type particle controls.

## Compatibility
- **Minecraft:** 1.20.4, 1.20.6, 1.21 to 1.21.11, 26.1, 26.2 and 26.3. Download the file for your exact Minecraft version.
- **Loader:** NeoForge.
- **Client only:** not needed on servers. You can join any server with it.
- **Graphics:** targets OpenGL 3.0, so it also runs on older GPUs and on Android launchers.
- Versions 1.21.5 and newer are marked Beta because they have had less device testing.

## Links
- Source code: https://github.com/entitybrian69-bit/aetherium
- Bug reports: https://github.com/entitybrian69-bit/aetherium/issues

Licensed under LGPL-3.0-only.
