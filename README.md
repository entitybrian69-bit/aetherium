# Aetherium

A replacement rendering engine and graphics utility mod for Minecraft: Java Edition,
shipped for Fabric and NeoForge from one shared source set.

Aetherium is not a "performance mod" in the Sodium sense and not a shader loader in the
Iris sense. It owns the render path — backend selection, chunk meshing, occlusion,
batching, shader compilation — and adds the graphics options people install OptiFine for:
coloured dynamic lights, a real gamma/cave-vision suite, a live Compatibility toggle, and
a purple settings screen that replaces the vanilla video options.

---

## Honest status (read this first)

Everything below is what the code in this repository actually does today. Nothing here is
a roadmap described in the present tense.

| Area | Status | What that means |
| --- | --- | --- |
| Minecraft **1.21.1** | **reference implementation** | Complete engine, GUI, mixins, config, Android routing, Iris bridge. This is the version the code is written for. |
| 32 further versions (1.16.5 → 26.3) | **mechanical porting system** | One delta directory per version — `changes.patch`, `derivation.md`, `notes.md` — linked individually in [PORTING_MATRIX.md → All versions](PORTING_MATRIX.md#all-versions). `tools/port.sh` applies them. No per-version source tree exists, by design. |
| Backend abstraction (GL 4.6 DSA / Vulkan 1.3 / GL core / GL legacy) | implemented: probe, capability matrix, hot-swap, selection gates + tests | The Vulkan backend opens a device and reports capabilities; it does not draw chunks — the world path stays on GL. |
| HZB occlusion culling, persistent mapped buffers, `glMultiDrawElementsIndirectCount` batching, async shader compile + program-binary cache, async distance-prioritized meshing | implemented against the GL abstraction | Two states ship: `ACTIVE` and `SHADOW` (Compatibility). |
| `advanced.experimental_full_renderer = true` (route world geometry through Aetherium) | **NOT SHIPPED** | Refused at startup with a logged error and an automatic fallback to `SHADOW`. The GUI shows the switch as unavailable rather than hiding it. Reason: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#honest-status). |
| GUI (`AetheriumVideoOptionsScreen`) | implemented | Reached through an `OptionsScreen` mixin plus a fallback button. **There is no Mod Menu dependency** and no `ModMenuApi`; on Fabric without Mod Menu you get the fallback button in the vanilla video settings. |
| Iris / Oculus integration | implemented by reflection only | No compile-time or runtime dependency. If Iris is absent, every shader path is inert. |
| Dynamic lights, gamma suite, conflict scanner, HUD, Android launcher routing | implemented + unit-tested | See `common/src/test/java/com/aetherium/`. |
| Compiled and run by CI on this machine | **no** | This tree was authored in an environment without a JDK or Gradle. Syntax, cross-references, JSON and the delta patches are machine-checked (see [Verification](#verification)); `javac` never ran. |

If you are evaluating whether to install it: the feature list above is the contract. If
you are evaluating whether to *trust a claim* in an issue or a video: "2× Sodium FPS" is
not a claim this project makes, and the harness in [BENCHMARK.md](BENCHMARK.md) is why.

## What is deliberately not here

* **No Mod Menu runtime dependency.** A `ModMenuApi` entry point would be one more version
  moving target for a mod that already tracks 33 of them. The mixin on the vanilla
  `OptionsScreen` plus its fallback button is the entry path.
* **No hard dependency on Sodium, Iris, Oculus, Embeddium or Mod Menu.** `fabric.mod.json`
  uses `suggests`, and Iris is bound reflectively.
* **No `full` renderer mode.** Replacing the mesher is the part that cannot be verified
  without a GPU and a running game, so it is refused rather than half-shipped. The enum
  value exists so the config format does not change when it lands.
* **No fabricated benchmarks, no `TODO` bodies, no stubbed methods.** Where an API could
  not be verified against real sources, the code carries an inline
  `[UNVERIFIED: what was not verified and why]` marker; [VERIFICATION.md](VERIFICATION.md) lists every
  mark with its file and line, and `tools/check.py` counts them so the number in this
  repository cannot grow quietly.
* **No `gradle/wrapper/gradle-wrapper.jar`.** A binary cannot be committed from this
  authoring environment. Generate it once, locally:

  ```sh
  gradle wrapper --gradle-version 9.4.1
  ./gradlew build
  ```

  Everything else in the wrapper (`gradlew`, `gradlew.bat`,
  `gradle/wrapper/gradle-wrapper.properties` pinned to Gradle 9.4.1) is present.

---

## Install

Drop `aetherium-fabric-<version>.jar` (or `aetherium-neoforge-<version>.jar`) into `mods/`. Client-side only:
the manifest says `"environment": "client"`, and there is no server component to install.

Recommended pairs: **Iris** (shaders) and nothing else. Remove Sodium, Embeddium,
Radium/Magnesium, VulkanMod, OptiFine/OptiFabric — Aetherium detects them at startup, prints a
one-line notice in the top-right HUD under the backend tag (`general.notify_conflicts`),
delegates overlapping features, and refuses to hook rendering where two renderers cannot
coexist. Details: [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md).

## Build

```sh
gradle wrapper --gradle-version 9.4.1   # once; see the note above
./gradlew buildAll                       # common + fabric + neoforge jars
./gradlew :common:test                   # engine unit tests
./gradlew verifyAll                      # buildAll, tests, and tools/check.py
./gradlew :common:checkstyleMain
```

Requirements: JDK 21 (toolchain 21 for 1.21.1; `tools/setup_jdk.sh` prints what each
version in the porting matrix needs), Gradle 9.4.1, and network access on the first build
for the Minecraft/Loom mappings. Versions are pinned exactly in `gradle.properties`,
`gradle/libs.versions.toml` and `tools/porting_pins.json`; the reasoning for each pin is in
[docs/BUILD_PINS.md](docs/BUILD_PINS.md).

Offline builds: `tools/build_all.sh --offline` runs every check that needs no network
(parsers, cross-references, config/JSON validity, and applying all 32 delta patches).

There is no published download. A push to `main` runs `.github/workflows/build.yml`, whose last
job attaches `aetherium-fabric-*.jar` and `aetherium-neoforge-*.jar` (plus `unverified.txt`) to a
**draft** release tagged `v<mod_version>+<minecraft_version>` — `v0.1.0+1.21.1` today. It stays a
draft on purpose: CI proves the code compiles and its tests pass, not that a GPU likes it. Publish
with `gh release edit <tag> --draft=false` after you have run it once, or build it yourself from
the commands above. The jar links for everything this build *needs* are in
[docs/BUILD_PINS.md](docs/BUILD_PINS.md#the-jars-what-exists-and-what-you-download).

## Porting to another Minecraft version

Every version has its own links:
**[PORTING_MATRIX.md → All versions](PORTING_MATRIX.md#all-versions)** lists all 33 rows, each
pointing at its delta directory, patch, derivation and notes. That index is generated from
`tools/porting_pins.json` by `tools/gen_deltas.py --matrix`, so it cannot drift from the pins —
and it deliberately contains no download links, because this repository publishes no jars.

`PORTING_MATRIX.md` is also the table of all 33 rows: loader, NeoForge, Loom, mappings,
Java level, the GUI/graphics API the version has, the lightmap shape, whether the options
hijack is enabled, and a status grade (`reference`, `documented`, `derived`, `unverified`).

```sh
tools/port.sh 1.20.1           # apply deltas/1.20.1/changes.patch in place
tools/port.sh --dry-run 26.3   # apply to a throwaway copy, verify, change nothing
tools/port.sh --revert 1.20.1  # undo an applied delta
tools/port.sh --list           # every version the matrix knows, with its pins
python3 tools/gen_deltas.py --all --matrix --verify   # regenerate the whole system
```

A delta patch touches only `gradle.properties`, the mixin plugin's version table, and the
four mixin/HUD files whose targets move between versions. Everything else is version
independent because the engine never calls a Minecraft method directly — it goes through
`tolerant` mixins (`require = 0, expect = 0`) and `AetheriumMixinPlugin`, which refuses a
mixin on a version where its target class or API does not exist. The rule that makes this
mechanical is written down in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Configuration

`<game>/aetherium.json` — 58 options in seven groups, schema documented in
[`CONFIG_SCHEMA.json`](CONFIG_SCHEMA.json) (generated from the code, do not hand-edit).
Comments are allowed (`//` and `/* */`), unknown keys are preserved for round-trips, an
invalid value is clamped or ignored with a log line, and a file that cannot be parsed is
copied to `aetherium.json.bad` while defaults are used. Saves are atomic (`.tmp` + rename,
with a documented non-atomic fallback for Android's FUSE/sdcardfs) and coalesced to at most
one write per 500 ms.

The GUI writes the same file; the `Compatibility` toggle is live — `false` unhook the
render path at the next frame boundary and returns to vanilla rendering without a restart.

## Android

Aetherium runs inside Pojav, Zalith, FCL, Amethyst and DroidBridge launches: it detects the
launcher, reads `custom_env.txt` plus `POJAV_RENDERER` / `ZALITH_RENDERER` /
`FCL_RENDERER` / `MESA_GL_VERSION_OVERRIDE`, identifies the GL provider (GL4ES, Zink, LTW,
MobileGlues, VirGL, ANGLE), and routes the backend choice accordingly — a translated GL
context does not get the persistent-mapping or indirect paths it only pretends to support.
arm64 only. The Android tab is hidden on desktop.
Full details, including the per-launcher table and the thermal/battery behaviour:
[docs/ANDROID.md](docs/ANDROID.md).

## Shaders

[docs/IRIS_COMPAT.md](docs/IRIS_COMPAT.md) lists every Iris v0 API call Aetherium makes, the
reflection used to make it, what happens when Iris is absent or its minor revision changes,
and the Oculus package differences. The short version: Iris keeps the shader pipeline,
Aetherium keeps geometry and lighting, and the `Shaders` tab is the bridge.

## Performance claims

[BENCHMARK.md](BENCHMARK.md) describes the two harnesses that exist in this tree: a CPU-only
JUnit micro-benchmark (`tools/benchmark.sh`, always runnable, no GPU needed) and the optional
in-game harness for a real FPS/frametime report against a pinned comparison mod list. It also
states what a number from either one does and does not prove. No ratio between this mod and
Sodium is asserted anywhere in this repository, because the measurement that would justify
one has not been run on your hardware.

## Tests

`./gradlew :common:test` runs JUnit 5 tests for the engine's own logic: `MathUtil`,
`VersionRange`, `Json`, `ConfigValue`, `ConfigStore`, `FrameStats`, `MeshCounters`,
`GammaApplier`/`GammaCurve`, `BackendSelector`, `ModConflictScanner`, `CustomEnvFile`. Each
class asserts behaviour a user would notice (clamping, atomic writes, corrupt-file recovery,
conflict delegation, the feature matrix a backend gets), not line coverage.

## Verification

Without a JDK in the authoring environment, "it compiles" cannot be claimed, so the checks
that *can* run mechanically are wired into the build:

| Command | What it proves |
| --- | --- |
| `tools/check.py` | every Java/JSON/properties/sh file parses; banned placeholder patterns absent; `[UNVERIFIED]` marks counted; `deltas/*` exist for all 33 rows; `PORTING_MATRIX.md` is up to date |
| `tools/check_refs.py` | every `com.aetherium` type referenced anywhere resolves to a real file, including nested types |
| `tools/check_java.py` | braces, brackets, strings, char literals and text blocks nest correctly in all 71 files, and no method signature is pasted twice (structural only — it is not evidence that anything compiles) |
| `python3 tools/gen_deltas.py --verify` | all 32 delta patches apply cleanly to the current tree |
| `./gradlew checkTree` | the above, as a Gradle task |
| `tools/verify.sh` | per-version target checks to run *after* you have a JDK and network |

Type-checking, mixin descriptor validation and actual runtime behaviour require a JDK;
`tools/verify.sh` and `tools/benchmark.sh --mc` are the entry points for those.

## Layout

```
common/     engine, GUI, mixins, config, platform SPI (100% of the code)
fabric/     Fabric entry point, fabric.mod.json, remap/reobf
neoforge/   NeoForge entry point, neoforge.mods.toml, jar-in-jar
deltas/     per-version patches: changes.patch + derivation.md + notes.md
tools/      porting, generation, verification, benchmark scripts
docs/       architecture, glossary, android, iris compat, troubleshooting, pins
```

## Versions

All 33 rows, linked individually: **[PORTING_MATRIX.md → All versions](PORTING_MATRIX.md#all-versions)**
— one delta directory (`changes.patch`, `derivation.md`, `notes.md`) per version, grouped by
era, each row carrying its status grade. The list is generated from `tools/porting_pins.json`,
so it is the only place to look and the only place that can be right: the range is
1.16.5 → 1.20.6, 1.21 → 1.21.11, and the date-based 26.x line, with **1.21.1** as the
reference version this source is written against. It deliberately contains no download links,
because this repository publishes no jars.

## Documentation

[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) ·
[docs/GLOSSARY.md](docs/GLOSSARY.md) ·
[docs/ANDROID.md](docs/ANDROID.md) ·
[docs/IRIS_COMPAT.md](docs/IRIS_COMPAT.md) ·
[docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) ·
[docs/BUILD_PINS.md](docs/BUILD_PINS.md) ·
[BENCHMARK.md](BENCHMARK.md) · [CONTRIBUTING.md](CONTRIBUTING.md)

## License

LGPL-3.0-only (see [LICENSE](LICENSE)); the third-party attributions and the reasons for
each are in [NOTICE](NOTICE). Mixin and LWJGL are used under their own licences; no source
file from Sodium, Iris, Embeddium or OptiFine is copied into this repository.
