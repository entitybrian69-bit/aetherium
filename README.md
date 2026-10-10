# Aetherium

A client-side performance and video-settings mod for Minecraft **1.16.5 → 26.3** (33 versions) on
**Fabric** and **NeoForge**. It replaces the vanilla *Video Settings* screen with its own animated,
tabbed screen. It also adds frame-rate features that work *with* the vanilla renderer instead of
replacing it.

Version **0.2.0** is a complete rewrite. The 0.1.0 line shipped a parallel GPU pipeline, an async
shader compiler and a light-map writer. Those cost more frame time than they saved and broke on
many drivers. All of that is gone. Every 0.2.0 feature either removes work vanilla would have done
or changes a vanilla setting, and Aetherium makes no OpenGL calls of its own.

## Status (read this first)

| | |
| --- | --- |
| Compiles | All **33** versions, against the method and field signatures CI extracted from each version's real Minecraft jar (`tools/stubcheck.py --all`). CI then builds each version with its real Loom/ModDev toolchain (`ship.yml`): **33/33 green** in run 38060927789. |
| Tests | 72 unit tests and 6 opt-in CPU micro-benchmarks, all passing offline (`tools/testrun.py --bench`). CI runs the same unit tests with real JUnit 5 on every version. |
| Mixin targets | Checked by hand against the `javap` probes of every version (`tools/probe/<ver>.txt`) |
| Launched in game | **No.** Nothing in this repository's tooling has a GPU. See VERIFICATION.md §3 |

Releases are published per Minecraft version as `v0.2.0+<mcversion>`, and only for versions whose
build and tests passed. `DOWNLOADS.md` lists only releases that actually exist.

## The settings screen

*Options → Video Settings…* opens Aetherium's screen on every version. Pressing **Vanilla video
settings** on the General tab opens the original screen.

- **Tabs** (left rail, each with a pixel-art icon): **General**, **Quality**, **Performance**,
  **Backend**, **Effects**, **Iris Shaders**, **Android**.
- **Controls**: toggle switches, segmented choices, dropdowns, sliders, live info rows and buttons.
  Rows that don't apply are greyed out with the reason (for example, *Simulation distance* before
  1.18, or Iris options without Iris installed).
- **Animations**: the panel eases open, and the tab highlight slides between tabs. Page content fades
  and slides in. Toggle knobs and segment highlights glide, and dropdowns unfold. Scrolling is
  smooth, hover glows fade, and Reset/Apply show a toast. Every animation is frame-rate-independent
  (exponential easing on real elapsed time), so it plays at the same speed at 30 FPS and at 300.
- **Apply / Done**: edits are staged and applied together, so a change that needs a chunk reload
  (graphics, biome blend) triggers one reload, not one per click. Closing with Esc applies too.
  Nothing you set is lost.
- **Presets** (General): *Max FPS*, *Balanced*, *Quality* and *Custom*. A preset sets the
  performance options together, and touching any of them switches the preset to *Custom*.
- The footer reads `Aetherium Mod v0.2.0 · Minecraft <version>` and `Rendering API: OpenGL 3.0 · <GPU>`.

The screen is drawn with plain filled rectangles and text: about 300 fills per frame, no textures
and no shaders. It costs less to draw than the vanilla screen it replaces.

## Performance features

| Feature | What it saves | Where |
| --- | --- | --- |
| Entity culling | Skips entities beyond a configurable distance before vanilla's frustum test. | Performance |
| Particle density | Drops a share of new particles at spawn, so they never tick or render. | Performance |
| Adaptive render distance | Lowers render distance when FPS stays under the target and raises it back when there is headroom. | Performance |
| Weather off | Cancels rain/snow rendering and rain particles. | Quality |
| Vignette off | Skips the vignette overlay pass. | Quality |
| Frame cap | A battery-saver cap and a thermal guard (when the device exposes a temperature sensor). | Android |
| Vanilla settings | Graphics, clouds, particles, biome blend, render/simulation/entity distance and max FPS, all in one place. | Quality, Performance |
| Presets | *Max FPS* turns on all of the above at once. | General |

Dynamic lights (held torches, glowing entities) are an opt-in *effect*, not a performance feature.
They cost about 32 ns per light lookup with 8 active sources (`CpuMicroBenchmarkTest`). Fullbright
is a gamma override and never writes to `options.txt`.

**`general.enabled = false`** turns every hook into a pass-through. The game then behaves exactly as
it would without the mod, apart from the replaced settings screen.

## OpenGL

Aetherium needs nothing beyond what the game itself needs. It issues no GL calls, compiles no
shaders and creates no buffers. Everything goes through vanilla's renderer, which is why it runs on
the same devices as the game. The *Rendering API: OpenGL 3.0* footer states the mod's own
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
3. Optional: Iris (Fabric) or Oculus. Aetherium pauses dynamic lights while a shader pack renders
   and links to the pack screen.

## Configuration

`<game>/config/aetherium.json`, schema v4 (`CONFIG_SCHEMA.json`). It is written by the screen, so
you rarely need to edit it. Files from 0.1.0 (v1–v3) are migrated on first load; keys for removed
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
