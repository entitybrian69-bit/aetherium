# Aetherium

A client-side performance and video-settings mod for Minecraft **1.16.5 → 26.3** (33 versions) on
**Fabric** and **NeoForge**. It replaces the vanilla *Video Settings* screen with its own animated,
tabbed screen. It also adds frame-rate features that work *with* the vanilla renderer instead of
replacing it.

Version **1.3.0** turns the experimental 1.16.5 chunk renderer into **Aetherium's own terrain
pipeline**: a compact 16-byte vertex format, chunk meshes grouped by face direction, and back-face
groups that are never drawn (see [1.3.0: the Aetherium pipeline](#130-the-aetherium-pipeline-1165)).
It is still 1.16.5 only and off by default. Version 1.2.0 added **entity occlusion culling** on all
33 versions and the first experimental chunk renderer (see
[1.2.0: occlusion culling and the experimental chunk renderer](#120-occlusion-culling-and-the-experimental-chunk-renderer)).
Version 1.1.0 fixed the frame-rate drops reported on 1.0.0 (see
[1.1.0: the frame-rate fixes](#110-the-frame-rate-fixes)) and removed the shader-mod page and
integration. Nothing on vanilla's chunk-meshing path allocates any more. The hooks that stay
installed cost a flag check, or a distance compare for culling, per call. 1.0.0
added smooth chunk loading, an animated-texture switch, a block-entity distance, worker-thread
sizing, a light/dark theme, a working scrollbar, touch scrolling and interface sounds. Every feature
either removes work vanilla would have done or changes a vanilla setting. Aetherium makes no
OpenGL calls of its own unless you turn on the experimental 1.16.5 Aetherium pipeline.

## 1.3.0: the Aetherium pipeline (1.16.5)

The setting is *Aetherium pipeline (experimental)* on the Backend tab (config key
`advanced.experimental_chunk_renderer`, unchanged). 1.2.0 drew vanilla's own vertex buffers with
a shader. 1.3.0 also owns how terrain is **stored** and **which faces are sent to the GPU**:

- **Compact vertices, 16 bytes instead of 32.** When a chunk section's solid, cutout-mipped or
  cutout layer is uploaded, Aetherium re-encodes it. Positions become three unsigned 16-bit values
  (1/1024-block steps, exact on the 1/16 grid block models use). Block and sky light are packed into
  a fourth 16-bit value. Colour stays 4 bytes and texture coordinates become two 16-bit values. The
  normal is dropped (the 1.16.5 terrain path never reads it). Video memory and vertex bandwidth for
  terrain are halved.
- **Face-direction groups.** Each section's quads are sorted into seven groups: up, down, north,
  south, east, west, and *any* for everything else. A quad joins a direction group only if it is
  flat on that axis and wound to face that way. Diagonal plants and fluids' back-to-back faces
  (which vanilla labels with misleading normals) go to *any*.
- **Back-face groups are skipped before the GPU sees them.** Per section, per frame, a direction
  group is drawn only if the camera is in front of the group's outermost face plane, with a 1/16
  block margin. Looking at a hillside from above skips every down-facing quad. Visible groups become
  one to three index ranges. Ranges separated by fewer than 256 hidden quads are merged, because an
  extra draw call costs more than shading a few culled back faces, especially on GL4ES. Sections
  over 16 384 quads are split as before. The *Chunk renderer* row on the Backend tab shows the share
  of faces skipped.
- **Safe fallbacks.** Data that does not fit the format exactly (anything outside the section's
  range, out-of-range light or texture coordinates) is uploaded unchanged, and drawn by the 1.2.0
  shader. Switching the pipeline on rebuilds the loaded chunks once, so the whole view converts at
  once. Switching it off, or any failure, also rebuilds them once, because vanilla cannot read
  compact buffers. Until that rebuild, those layers are skipped for at most one frame rather than
  drawn wrong. Translucent blocks and tripwire stay on vanilla's path (they are re-sorted).
- **Not tested on a GPU.** The encoder, classification, culling planes, range merging, shader
  decode and switching rules are covered by unit tests, and the hooks were checked against the
  bytecode CI extracted from the 1.16.5 jar. Nothing here has run in a real game yet. It stays
  1.16.5 only until someone confirms it works there. Porting it to 1.17+ means new hooks for each
  of those renderer generations.

## 1.2.0: occlusion culling and the experimental chunk renderer

**Entity occlusion culling (all 33 versions, on by default).** Every frame, vanilla works out which
16×16×16 sections of the world are visible: its occlusion graph walks from the camera through open
section faces, then the frustum is applied. Only those sections get their terrain drawn. Entities
never used that list. Vanilla only checks them against the frustum (1.21.11+ also waits for a
section's fade-in), so mobs in caves under you, behind hills or inside other buildings were still
animated and drawn. Aetherium now copies vanilla's list into a bit grid around the camera once per
frame (about 38 KB cleared, one bit per visible section). It then skips an entity when every
section its culling box touches is missing from the list. This is the same idea Sodium uses,
written independently (Sodium's current code is not open source).

- It only reads vanilla's list and never hides a section vanilla would draw. Anything uncertain
  counts as visible: sections outside the grid, entities above or below every visible section,
  boxes spanning 4+ sections, and frames before the world has a list.
- These are never hidden: other players (their name tags show through walls), entities with a
  visible name, glowing entities, the camera entity, and anything whose renderer opts out of
  culling. Renderers that extend the visibility test, such as leashes and guardian beams, still run
  their own checks.
- Cost: the grid is built once per frame and is one bit lookup per entity. The exemption checks run
  only for entities that would be hidden.
- The setting is *Hide entities behind walls* on the Performance tab. If reading vanilla's list
  ever fails, it turns itself off for the session and logs why. With Sodium or Embeddium installed
  it is handed off, because they cull entities themselves.

**Experimental chunk renderer (1.16.5 only, off by default).** The setting (renamed *Aetherium
pipeline (experimental)* in 1.3.0) is on the Backend tab. Vanilla 1.16.5 draws each chunk section's solid and cutout layers with
the fixed-function pipeline. Per section it sets client-state pointers, pushes, loads and multiplies
the GL matrix stack, and issues `glDrawArrays(GL_QUADS)`. On Android's GL4ES each of those is
emulated, and every quad list is converted to triangles on the CPU, per section, per frame. When
the switch is on, Aetherium draws the **same vertex buffers** with one GLSL 1.20 program: generic
attributes, a single shared quad-to-triangle index buffer (16-bit, larger sections are split), and
one uniform per section for its offset. It reproduces vanilla's lightmap lookup, alpha cutout
(0.5) and linear/exp/exp2 fog. Vanilla still builds, uploads, culls and sorts the sections and sets
up each layer's textures, blending and depth. Translucent blocks (water, glass) and tripwire stay
on vanilla's path because they need sorting.

- If the shader fails to compile or link, or anything throws, the renderer turns itself off for the
  session and vanilla draws the frame. The Backend tab's *Chunk renderer* row shows which path is
  drawing, or why it fell back.
- **It has not been run on a GPU yet**: this repository's tooling has none. Expect the largest
  gain on Android/GL4ES and a small one on desktop drivers, which handle the fixed-function path
  well. Please send a screenshot and `latest.log` with it on and off. It will be ported to the
  other versions after it is confirmed to work on 1.16.5.

## 1.1.0: the frame-rate fixes

1.0.0 had four ways to lose frames that the defaults or the *Max FPS* preset turned on:

| Cause in 1.0.0 | Effect | 1.1.0 |
| --- | --- | --- |
| The **thermal guard** was on by default and not limited to phones. It capped the game at **30 FPS** whenever any CPU/GPU sensor read 68 °C or more. Linux desktops and phones under load reach that almost all the time. It also read every `/sys/class/thermal` zone on the render thread every 5 s. | A 30 FPS cap, plus a hitch every 5 s. | Off by default, Android-only, ceiling 75 °C. The sensor is read on a background thread. |
| **Adaptive render distance** (part of *Max FPS*) could change the distance every ~6 s. Each change makes vanilla rebuild every chunk, and the rebuild's own FPS dip triggered the next step down. | Repeated world reloads and stutter. | Removed from every preset. When on, it needs 10 s of low FPS to drop one chunk and 30 s of headroom to add one, then waits 30 s. |
| The **dynamic-light hooks** sat on vanilla's light lookup, which runs for every vertex while chunks are meshed. They allocated a callback object per call **even with lights off**. | Slower chunk building and extra garbage collection. | With lights Off at launch the hooks are not installed at all. Turning lights on from Off applies after a restart. |
| **Fullbright** hooked `OptionInstance.get()`, which runs for every option read, including once per block during meshing, and allocated on each call **even with fullbright off**. | Extra garbage collection while chunks load. | That hook is gone. Fullbright writes its value straight into the gamma option, and `get()` is vanilla bytecode again. |

Config files from 1.0.0 and earlier are migrated once (schema v5): the thermal guard, adaptive
distance and dynamic lights go back to off, and an untouched 68 °C ceiling becomes 75 °C. Every
other setting is kept.

Aetherium does not switch renderers the way VulkanMod does. Apart from the opt-in 1.16.5 chunk
renderer above, it does not replace vanilla's renderer the way Sodium does. Its gains come from
removing work inside vanilla's renderer (see the table below) and from staying out of the way when
a feature is off.

## Status (read this first)

| | |
| --- | --- |
| Compiles | All **33** versions, against the method and field signatures CI extracted from each version's real Minecraft jar (`tools/stubcheck.py --all`). CI then builds each version with its real Loom/ModDev toolchain (`ship.yml`): **33/33 green** for 1.2.0 in run 38102357434 (1.1.0: 38078947462; 1.0.0: 38065183797). 1.3.0: see the latest `ship` run and `DOWNLOADS.md`. |
| Tests | 118 unit tests (including a headless test of the settings screen: scrollbar, tap vs. swipe, fling, theme switch, sounds) and 7 opt-in CPU micro-benchmarks, all passing offline (`tools/testrun.py --bench`). CI runs the same unit tests with real JUnit 5 on every version. |
| Mixin targets | Checked by hand against the `javap` probes of every version (`tools/probe/<ver>.txt`) |
| Launched in game | **No.** Nothing in this repository's tooling has a GPU. See VERIFICATION.md §3 |

Releases are published per Minecraft version as `v1.3.0+<mcversion>`, and only for versions whose
build and tests passed. `DOWNLOADS.md` lists only releases that actually exist.

## The settings screen

*Options → Video Settings…* opens Aetherium's screen on every version. Pressing **Vanilla video
settings** on the General tab opens the original screen.

- **Tabs** (left rail, each with a pixel-art icon): **General**, **Quality**, **Performance**,
  **Backend**, **Effects**, **Android**.
- **Controls**: toggle switches, segmented choices, dropdowns, sliders, live info rows and buttons.
  Rows that don't apply are greyed out with the reason (for example, *Simulation distance* before
  1.18, or the thermal guard on desktop).
- **Light / dark theme**: the sun/moon switch in the header (left of the FPS badge) cross-fades the
  whole screen between the two palettes. The choice is saved immediately (`general.ui.dark_mode`).
- **Scrolling**: a scrollbar with its own lane at the right edge (8 px in touch mode, 6 px
  otherwise); the controls end before it. Drag the thumb, or click the track to jump there. On touch
  screens a swipe that starts on a row scrolls the list instead of flipping the control under your
  finger, and a quick swipe keeps gliding with momentum. Taps act on release.
- **Descriptions**: hover an option for about a third of a second and a popup opens under it
  (above it near the bottom edge) with the full description, word-wrapped. It is a bordered
  card with a soft shadow and a green accent bar (amber when the option is unavailable, with
  the reason), in the light or dark theme. Moving to the next row swaps it at once, as in Sodium.
  It stays closed while you scroll or drag, and on touch screens it does not cover the row you
  just tapped.
- **Sounds**: clicks, toggles (higher pitch for on, lower for off), tab changes, slider ticks,
  Apply and the theme switch each have their own pitch of the vanilla UI click. They play on the
  Master volume, and *General → Interface sounds* turns them off.
- **Animations**: the panel eases open, and the tab highlight slides between tabs. Page content fades
  and slides in. Toggle knobs and segment highlights glide, and dropdowns unfold. Scrolling is
  smooth, hover glows fade, and Reset/Apply show a toast. Every animation is frame-rate-independent
  (exponential easing on real elapsed time), so it plays at the same speed at 30 FPS and at 300.
- **Apply / Done**: edits are staged and applied together, so a change that needs a chunk reload
  (graphics, biome blend) triggers one reload, not one per click. Closing with Esc applies too.
  Nothing you set is lost.
- **Presets** (General): *Max FPS*, *Balanced*, *Quality* and *Custom*. A preset sets the
  performance options together, and touching any of them switches the preset to *Custom*.
- The footer reads `Aetherium Mod v1.3.0 · Minecraft <version>` and `Rendering API: OpenGL 3.0 · <GPU>`.

The screen is drawn with plain filled rectangles and text: about 300 fills per frame, no textures
and no shaders. It costs less to draw than the vanilla screen it replaces.

## Performance features

| Feature | What it saves | Where |
| --- | --- | --- |
| Entity culling | Skips entities beyond a configurable distance before vanilla's frustum test. | Performance |
| Hide entities behind walls | Skips entities in sections vanilla's occlusion graph found hidden (caves, behind hills, other buildings). Players, named and glowing entities are exempt. All 33 versions. | Performance |
| Aetherium pipeline (experimental) | 1.16.5 only, opt-in: solid and cutout terrain stored as 16-byte compact vertices grouped by face direction, back-facing groups skipped per section, drawn with Aetherium's own shader and a shared index buffer. Falls back to vanilla on any error. | Backend |
| Particle density | Drops a share of new particles at spawn, so they never tick or render. | Performance |
| Adaptive render distance | Opt-in. Lowers the render distance one chunk after 10 s under the target FPS, raises it after 30 s of headroom, and waits 30 s between changes (each change reloads all chunks). | Performance |
| Smooth chunk loading | Vanilla uploads every finished chunk mesh in the frame it finishes. Joining a world, flying or world generation then causes one long frame. Aetherium uploads at least 8 meshes per frame (so block edits show at once), then stops when 3 ms are used, and leaves the rest for the next frames. Not on 26.x, where uploads moved into the new GPU layer. | Performance |
| Animated textures off | Stops the atlas tick that re-uploads water, lava, fire, portal and other animated textures 20 times a second. That upload is expensive on GL-over-GLES layers (gl4es, ANGLE, Zink). | Performance |
| Block entity distance | Chests, signs, banners, heads and similar objects beyond 16–64 blocks are skipped before any model work (vanilla: 64). Beacon and end-gateway beams are exempt. | Performance |
| Worker threads | World generation and chunk meshing share one pool of `cores − 1` threads. On phones those threads compete with the render and server threads for the few fast cores. *Auto* keeps vanilla on desktop and uses all cores but three (at least 2) on Android. A fixed count can be set; it applies after a restart. | Performance |
| Weather off | Cancels rain/snow rendering and rain particles. | Quality |
| Vignette off | Skips the vignette overlay pass. | Quality |
| Frame cap | Opt-in, Android only: a battery-saver cap and a thermal guard (30 FPS above the ceiling; the sensor is read on a background thread). | Android |
| Vanilla settings | Graphics, clouds, particles, biome blend, render/simulation/entity distance and max FPS, all in one place. | Quality, Performance |
| Presets | *Max FPS* sets the vanilla options and culling features together (6 chunks, Fast graphics, no clouds, smooth lighting and entity shadows off, minimal particles, 30 % particle density, weather and animated textures off, a 32-block block-entity distance). No preset turns on adaptive distance or a frame cap. | General |

Dynamic lights (held torches, glowing entities) are an opt-in *effect*, not a performance feature.
They are **off by default**: a moving light makes vanilla rebuild the chunk sections around it,
which is real lag on a phone. With lights Off when the game starts, their hooks are not installed
at all, so turning them on in *Effects* applies after a restart. A light lookup costs about 34 ns
with 8 active sources (`CpuMicroBenchmarkTest`). Fullbright writes its value into the gamma option
directly (no per-read hook) and never writes to `options.txt`.

**`general.enabled = false`** turns every hook into a pass-through. The game then behaves exactly as
it would without the mod, apart from the replaced settings screen.

## OpenGL

Aetherium needs nothing beyond what the game itself needs. By default it issues no GL calls,
compiles no shaders and creates no buffers. Everything goes through vanilla's renderer, which is
why it runs on the same devices as the game. The one exception is the opt-in 1.16.5 chunk renderer.
It uses a GLSL **1.20** program (OpenGL 2.1, below the 3.0 baseline, and safe for GL4ES) and one
index buffer, and falls back to vanilla if the driver rejects them. The *Rendering API: OpenGL 3.0* footer states the mod's own
baseline.

The game's own requirement is unchanged. Minecraft 1.17+ creates an OpenGL **3.2** core context
because its built-in shaders are GLSL 150. A mod cannot lower that without breaking vanilla
rendering, so Aetherium does not force a 3.0 context. On Android, launchers such as Pojav,
FCL and Zalith provide GL through a translation layer (gl4es, ANGLE, Zink, MobileGlues). The
*Android* tab shows which one was detected and lets you override it.

## Install

1. Pick the jar for your exact Minecraft version from **DOWNLOADS.md** (or the Releases page). Each
   jar only loads on the version it was built for.
2. **Fabric**: put it in `mods/` with Fabric Loader. Fabric API is *not* required.
   **NeoForge**: put it in `mods/`. No NeoForge jar exists for 1.16.5–1.20.1, 1.20.2, 1.20.3 or
   1.20.5 (Fabric only).

## Configuration

`<game>/config/aetherium.json`, schema v4 (`CONFIG_SCHEMA.json`). It is written by the screen, so
you rarely need to edit it. Keys added in 1.0.0 load their defaults when missing, so 0.2.0 files
keep working unchanged. Files from 0.1.0 (v1–v3) are migrated on first load; keys for removed
features are dropped. Lang files and the schema are generated from `AetheriumConfig.java` by
`tools/gen_resources.py` (`--check` in CI).

## Building

The reference version is **1.21.1** (Java 21). Its sources are the repository tree:

```sh
gradle :common:build :fabric:build :neoforge:build
```

Every other version is the same tree plus `deltas/<version>/changes.patch`:

```sh
sh tools/port.sh 1.19.2            # materialise the ported tree under build/ports/1.19.2
```

### How one source tree serves 33 versions

Version-specific code is written in place as **era blocks**:

```java
// @era:hud-begin graphics-float|graphics-delta
public static void render(final GuiGraphics graphics) { ... }
// @era:hud-else stack
//~ public static void render(final PoseStack pose) { ... }
// @era:hud-end
```

`tools/eras.py` defines each era's version ranges (18 eras: options access, GUI drawing API,
screen package, input events, weather renderer, light hook, and so on). It turns the right block on
for each version. `tools/gen_deltas.py` writes that selection, plus the build pins, as the version's
patch. `tools/stubcheck.py` compiles the same selection against that version's probed signatures.
What is compiled offline is therefore exactly what ships.

### Offline toolchain (no Maven needed)

```sh
bash tools/local_jdk.sh              # JRE + ECJ from PyPI/npm-reachable mirrors
python3 tools/stubcheck.py --all     # compile all 33 versions against tools/probe/*
python3 tools/testrun.py [--bench]   # unit tests (+ CPU micro-benchmarks)
python3 tools/gen_deltas.py --all --verify
python3 tools/check.py && python3 tools/check_refs.py . && python3 tools/check_java.py .
```

## Layout

```
common/   MC-free core (config, perf, lighting, gui engine) + client/ (MC glue) + 11 mixins
fabric/   loader shell        neoforge/  loader shell
deltas/   32 generated ports  tools/     generators, checks, probes, offline toolchain
```

## Documentation

- `VERIFICATION.md`: what was checked, how, and what was not
- `PORTING_MATRIX.md`: every version, its pins and its eras (generated)
- `deltas/<version>/README.md`: what that version's patch changes (generated)
- `CONFIG_SCHEMA.json`: config reference (generated)

## License

See `LICENSE`.
