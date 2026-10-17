# CurseForge page: Aetherium

Create it at https://authors.curseforge.com/#/projects/create/choose-game (Minecraft, class "Mods").

| Field | Value |
|---|---|
| Name | Aetherium |
| Summary | Client-side performance mod: own terrain renderer, animated video settings, frame pacing, culling, dynamic lights, zoom. Fabric 1.16.5 to 26.3. |
| Main category | Performance |
| Extra categories | Utility & QoL, Cosmetic (optional) |
| License | GNU Lesser General Public License version 3 (LGPLv3) |
| Source | https://github.com/entitybrian69-bit/aetherium |
| Issues | https://github.com/entitybrian69-bit/aetherium/issues |
| Avatar (logo) | tools/modrinth/icon-512.png (the exact mod icon, pixel-exact x8) |
| Loader of its files | Fabric |

Description (switch the editor to Markdown, paste everything below the line):

---

# Aetherium

**A client-side performance and video-settings mod for Minecraft 1.16.5 → 26.3 (33 versions), on Fabric.**

Aetherium replaces the vanilla *Video Settings* screen with its own animated, tabbed screen, and adds performance features that work *with* Minecraft's renderer. It targets OpenGL 3.0, so it runs on low-end PCs and on Android launchers (Pojav / gl4es) as well as desktop GPUs.

---

## ✨ Video settings screen

- An animated, tabbed screen with six pages: **General, Quality, Performance, Backend, Effects, Android**
- **Dark and light themes**, smooth scrolling with a thick draggable scrollbar, and optional UI sounds
- Hover any option to get a **description popup** explaining what it does
- **Presets** and a master on/off switch
- Options a Minecraft version can't support are greyed out and marked *"not on this version"*. Nothing is hidden or faked.

## ⚡ Performance

- **Aetherium renderer** (on by default, every version): solid and cutout chunk meshes are grouped by face direction, and each chunk section draws only the faces that can point at you. It **never uses more draw calls than vanilla**, and it falls back to vanilla automatically if anything goes wrong.
- **Even frame pacing:** with a max framerate set, frames are spaced evenly instead of drifting, so the game feels smoother at the same FPS
- **Entity culling and occlusion culling:** entities you can't see are skipped
- **Smart chunk culling:** chunk sections hidden inside the distance fog are neither drawn nor meshed
- **Fast particles, fast mob models, visible-only animated textures** (on supported versions)
- **Smooth chunk loading:** chunk rebuilds and network work are spread across frames to cut stutter
- **Threaded chunk building**, with an adjustable number of worker threads
- **Adaptive render distance**, block-entity distance, entity cull distance and particle density
- **Frame-time overlay** with FPS, average frame time and **p99** (the slowest 1% of frames), plus an optional stutter log

## 🎨 Quality

- **Fog:** distance, start, clear water, clear lava
- **Sky:** sky, stars, sun and moon, cloud height
- **Particles by type:** weather, block break, combat, explosions, fire, smoke, portals, potions, drips, totem, ambient
- Weather and vignette toggles

## 🔦 Effects

- **Dynamic lights:** held items and glowing entities light up their surroundings
- **Fullbright**, with adjustable strength
- **Zoom**, with smooth animation, scroll-to-zoom, cinematic camera and third-person zoom. Zoom, Zoom In and Zoom Out are all rebindable.
- **Freelook:** look around without turning your character, in toggle or hold mode

## 📱 Android

- Built-in support for mobile launchers: renderer selection, memory budget, touch mode
- **Battery saver** with an FPS cap, and a **thermal guard** that lowers the frame cap when the phone gets hot

---

## 📦 Installation

| Loader | What you need |
|---|---|
| **Fabric** | Fabric Loader only. **Fabric API is not required.** |

- Download the file that matches **your exact Minecraft version**. Each file is built for one Minecraft version only.
- **Client-side only.** Servers don't need it, and you can join any server with it.

## ⚠️ Compatibility

- Don't combine it with other renderer replacements such as **Sodium, Embeddium, Rubidium or OptiFine**. Aetherium detects them and warns you.
- If you see missing terrain faces or other rendering glitches, turn off **Video Settings → Backend → Aetherium renderer** and report the issue with your Minecraft version.
- On 1.21.5 and newer the renderer is new. Reports are very welcome!

## 🐞 Bug reports

Please open an issue on GitHub with:
1. Minecraft version
2. Your device (PC or Android launcher, GPU)
3. The log (`logs/latest.log`) and, for crashes, the crash report

## 📜 Credits & license

- Licensed under **LGPL-3.0-only**. The source code is on GitHub.
- Zoom behaviour is inspired by [Zume](https://modrinth.com/mod/zume). It was reimplemented independently, with no code copied.
