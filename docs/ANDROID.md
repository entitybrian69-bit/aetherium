# Aetherium on Android

Aetherium runs inside Android Minecraft launchers. That is not a marketing claim — it means
the engine must know *which* GL it has been handed, because a translated context reports
version numbers it does not implement, and a renderer that trusts them stalls for hundreds of
milliseconds per frame.

This document describes the detection, the routing rules, and the knobs. All of it is in
`common/src/main/java/com/aetherium/android/` (6 classes) plus `android.*` config options.

## What gets detected

`AndroidEnvironment.probe(config)` runs once, on the render thread, during
`Aetherium.initialize`. It reads the environment through `AndroidEnvProvider` (bound to
`System::getenv` in production, and to a map in tests — the seam exists so the detection logic
is unit-testable without a phone), plus these JVM properties: `os.arch`,
`sun.java.command`, `user.home`, `os.version`, and `/proc/cpuinfo` for CPU features.

| signal | source | used for |
| --- | --- | --- |
| launcher identity | `ZALITH_LAUNCHER`, `FCL_LAUNCHER`, `AMETHYST_LAUNCHER`, `DROIDBRIDGE`, `POJAV_RENDERER`, plus path heuristics over `user.home`, the java command, `LD_LIBRARY_PATH` and `java.library.path` | choosing the env-file location and the conservative defaults |
| Android private-data layout | `/data/user/<n>/<pkg>`, `/data/data/`, `/storage/emulated/0/games/` | "this is Android" when no launcher sets a marker (`UNKNOWN_ANDROID`) |
| renderer provider | `POJAV_RENDERER` / `ZALITH_RENDERER` / `FCL_RENDERER` / `AMETHYST_RENDERER` / `DROIDBRIDGE_RENDERER`, then `custom_env.txt` | backend routing |
| declared GL level | `MESA_GL_VERSION_OVERRIDE` (+ `MESA_GLSL_VERSION_OVERRIDE`) | whether 4.x entry points may be assumed at all |
| arch / features | `os.arch`, `/proc/cpuinfo` (`asimd`, `neon`, `sve`, `sve2`) | arm64 gate, SIMD paths |
| heap | `Runtime.maxMemory()` | upload budget, async-compile decision |
| thermals | the hottest readable `thermal_zone*/type` matching cpu/gpu/package/soc/skill | throttle decisions |

Explicit markers win over path heuristics, because the launcher sets them itself.

## Launchers

| enum | marker / heuristic | notes |
| --- | --- | --- |
| `POJAV` | `POJAV_RENDERER`, `pojav`, `kdt.pojavlaunch` | the reference case; reuses its `custom_env.txt` conventions |
| `ZALITH` | `ZALITH_LAUNCHER`, `zalith` | Pojav-derived, same env keys plus `ZALITH_RENDERER` |
| `FCL` | `FCL_LAUNCHER`, `foldcraft`, `fcl` | reuses `POJAV_RENDERER` *and* has `FCL_RENDERER` |
| `AMETHYST` | `AMETHYST_LAUNCHER`, `angelaura`, `amethyst` | |
| `DROIDBRIDGE` | `DROIDBRIDGE`, `droidbridge` | |
| `UNKNOWN_ANDROID` | Android data path, no marker | treated as Android with conservative defaults |
| `DESKTOP` | nothing matched | the Android tab is hidden, `applyRuntimeHints` is a no-op |

## Renderer values and routing

`AndroidRenderer.parse(value, MESA_GL_VERSION_OVERRIDE)` maps the launcher's own vocabulary:

| env value (as launchers spell it) | renderer | backend hint |
| --- | --- | ---|
| `opengles2` | GL4ES (GL 2.1 compat shim) | `GL46` is *not* implied — the hint is conservative |
| `opengles2_vgpu` | GL4ES + vGPU | conservative |
| `opengles3` | NGGL4ES (native GLES3) | GL core-ish |
| `opengles3_desktopgl_zink_kopper` | Zink (GL over Vulkan) | native Vulkan |
| `gallium_virgl` | VirGL (guest/server split over a socket) | GL core, translated |
| `gallium_freedreno` | Mesa native on Adreno | GL core, native |
| *(LTW)* | LTW (GL 3.3 core translator) | `GL_CORE` |
| *(MobileGlues)* | GLES-on-Vulkan translator | native Vulkan |
| *(ANGLE)* | ANGLE | `GL_CORE` |

The exact `switch` is in `AndroidRenderer`; the table above describes it, and the values are
the ones the launchers write, not names this project invented.

Three rules follow from that table and are enforced in the engine, not in documentation:

1. **A translated source never gets the persistent-mapping path.**
   `BackendCapabilities.supportsPersistentMapping()` returns false whenever
   `GpuInfo.isTranslated()` (Zink, ANGLE, gl4es, VirGL, MobileGlues, LTW). Without the arena,
   a translator sees a client wait inside the frame instead of a background upload — which
   reads as "the mod stutters on my phone".
2. **`GL46_DSA` requires the gate, not the string.** A 4.6 context is only 4.6 when
   `glGetStringi` says so; the same capability matrix that runs on desktop is what the phone
   gets, so a `MESA_GL_VERSION_OVERRIDE=4.6` without the DSA entry points selects `GL_CORE`.
3. **`BackendSelector.apply` asks the renderer for permission per feature**
   (`allowPersistentMapping()`, `allowIndirectDraw()`, `allowCompute()`), so a translator that
   exposes one 4.6 feature but not another is not forced into the all-or-nothing choice.

### arm64 only

`is64Bit()` and `isArm64()` gate the fast paths. A 32-bit (`armeabi-v7a`, x86) launch has
4 GB address-space limits, no `asimd` in the same form, and translators that behave
differently; Aetherium runs there only in `SHADOW` (measuring + gamma), which is stated in the
log rather than attempted silently.

## custom_env.txt

`readCustomEnv` (default `true`) enables reading the launcher's env file, located by
`AndroidLauncher.findCustomEnvFile` — `POJAV_CUSTOM_ENV` if set, else the launcher's documented
per-package path. The parser (`android/CustomEnvFile`) is deliberately tolerant of how people
actually write that file: `export KEY=value`, `#` comments, quoted values, whitespace around
`=`, and lines with no `=` at all (ignored, not an error). Keys are upper-cased; the file is
capped at 256 KB and 4096 lines and refused (not truncated) beyond that, because a 900 MB file
at that path is a mistake or an attack, and booting on a 1 GB heap is not the place to find
out.

Only an explicit allow-list of keys can influence the engine
(`CustomEnvFile.extractRendererKeys`): the five `*_RENDERER` keys, the two `MESA_*_VERSION_OVERRIDE`
keys, `POJAVEXEC_EGL`, `GALLIUM_HUD`, `MESA_EXTENSION_OVERRIDE`. A `PATH=` or `LD_PRELOAD=` in
the file therefore reaches nothing — `CustomEnvFileTest` asserts exactly that.

## Memory, battery, thermals

`applyRuntimeHints(config)` runs once, after the config is loaded and before the backend is
resolved, and only when the platform is Android:

- **`mobileMemoryMode`**: the per-frame upload budget is clamped to
  `heapMb / 16` in the 2–24 MB band, so a 1 GB device never absorbs a 24 MB arena spike as a
  GC pause inside a frame. Under a 1.5 GB max heap, `asyncShaderCompile` goes off (a second
  context is a second copy of every shader) and `programBinaryCache` goes on (the disk cache
  is cheap; recompiling 30 variants on a cold phone is not).
- **`batterySaver`**: `targetFps` capped at 60, `hzb` off (a compute pass is the most
  power-hungry way to save fill rate on these GPUs).
- **`thermalThrottle`**: if no thermal zone is readable, the option is switched *back off*
  with a warning, rather than silently pretending to throttle. When readable,
  `AndroidPowerGovernor` polls the hottest zone and lowers the frame target as temperature
  approaches `thermalCeilingC`.

`describe()` prints the whole picture (launcher, renderer, arch, features, heap budget,
declared GL level, sensor availability) and the Android tab shows it verbatim, so a bug report
about a phone can start from what the mod actually saw.

## Touch

`touchMode` (auto-on for any detected launcher, plus the manual switch) sizes rows and hit
targets from `AetheriumTheme.TOUCH_TARGET_DP = 48`, animates widget presses instead of
hover-only feedback, and the tab bar becomes a scrolling column so eight tabs still fit a
6-inch screen. The animations are cosmetic only: `gui/AetheriumAnimations` reads
`System.nanoTime()` and cannot affect rendering.

## Testing Android behaviour from a desktop

There is no fake-Android system property (a switch like that gets left on and then ships a
"phone" bug report). Instead the detection is written against the `AndroidEnvProvider` seam and
against `custom_env.txt` files, so `CustomEnvFileTest` and the Android routing tests construct
the launcher environment as data. If you need to check the *runtime* consequences, the honest
route is a real device or an ARM64 Android emulator with a launcher installed, and
`tools/benchmark.sh --mc`.

## Known Android limitations

- Vulkan is reached through the launcher's own translator or a native driver; Aetherium's
  `VULKAN_13` backend probes and reports, and does not draw the world anywhere yet.
- gl4es has no real `glMultiDrawElementsIndirectCount`, so batching degrades to fixed-count
  multi-draw there. That is a capability gate, not a device-name special case.
- Some launchers rewrite `MESA_GL_VERSION_OVERRIDE` without providing the entry points; the
  capability matrix, not the override, decides — which is why the override appears in the label
  and never in a `require`.
