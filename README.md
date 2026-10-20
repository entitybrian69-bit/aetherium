<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.1-55C95B?style=for-the-badge&logo=minecraft&logoColor=white" alt="Minecraft 1.21.1">
  <img src="https://img.shields.io/badge/Loaders-Fabric%20%7C%20NeoForge-blue?style=for-the-badge" alt="Fabric | NeoForge">
  <img src="https://img.shields.io/badge/License-LGPL--3.0-green?style=for-the-badge" alt="License">
  <img src="https://img.shields.io/badge/Status-Honest%20%26%20Documented-7C3AED?style=for-the-badge" alt="Status">
</p>

<h1 align="center">Aetherium</h1>

<p align="center">
  <strong>A replacement rendering engine and graphics utility for Minecraft: Java Edition</strong><br>
  Fabric · NeoForge · One shared source set
</p>

<p align="center">
  Owns the render path — backend selection, chunk meshing, occlusion, batching, and shader compilation —<br>
  and brings the graphics options people install OptiFine for: coloured dynamic lights, a real gamma/cave-vision suite,<br>
  a live Compatibility toggle, and a purple settings screen that replaces the vanilla video options.
</p>

---

## Honest status

Everything below is what the code in this repository actually does today. Nothing here is a roadmap written in the present tense.

| Area | Status | What that means |
| --- | --- | --- |
| Minecraft **1.21.1** | **reference implementation** | Complete engine, GUI, mixins, config, Android routing, Iris bridge. This is the version the code is written for. |
| 32 further versions (1.16.5 → 26.3) | **mechanical porting system** | One delta directory per version — `changes.patch`, `derivation.md`, `notes.md` — linked in [PORTING_MATRIX.md](PORTING_MATRIX.md#all-versions). `tools/port.sh` applies them. No per-version source tree exists, by design. |
| Backend abstraction (GL 4.6 DSA / Vulkan 1.3 / GL core / GL legacy) | implemented | Probe, capability matrix, hot-swap, selection gates + tests. Vulkan opens a device and reports capabilities; it does not draw chunks — the world path stays on GL. |
| HZB occlusion, persistent mapped buffers, `glMultiDrawElementsIndirectCount`, async shader compile + program-binary cache, async distance-prioritized meshing | implemented | Two states ship: `ACTIVE` and `SHADOW` (Compatibility). |
| `advanced.experimental_full_renderer = true` | **NOT SHIPPED** | Refused at startup with a logged error and automatic fallback to `SHADOW`. The GUI shows the switch as unavailable. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#honest-status). |
| GUI (`AetheriumVideoOptionsScreen`) | implemented | Reached through an `OptionsScreen` mixin plus a fallback button. **No Mod Menu dependency.** |
| Iris / Oculus integration | reflection only | No compile-time or runtime dependency. If Iris is absent, every shader path is inert. |
| Dynamic lights, gamma suite, conflict scanner, HUD, Android launcher routing | implemented + unit-tested | See `common/src/test/java/com/aetherium/`. |

> If you are evaluating whether to install it: the feature list above is the contract.  
> If you are evaluating whether to *trust a claim* in an issue or a video: “2× Sodium FPS” is not a claim this project makes.

---

## What is deliberately not here

- **No Mod Menu runtime dependency.** The mixin on the vanilla `OptionsScreen` plus its fallback button is the entry path.
- **No hard dependency on Sodium, Iris, Oculus, Embeddium or Mod Menu.** `fabric.mod.json` uses `suggests`; Iris is bound reflectively.
- **No `full` renderer mode.** Replacing the mesher cannot be verified without a GPU and a running game, so it is refused rather than half-shipped.
- **No fabricated benchmarks, no `TODO` bodies, no stubbed methods.** Unverified APIs carry an inline `[UNVERIFIED]` marker. [VERIFICATION.md](VERIFICATION.md) lists every mark.
- **No `gradle/wrapper/gradle-wrapper.jar`.** Generate it once locally:
  ```sh
  gradle wrapper --gradle-version 9.4.1
  ./gradlew build
  ```

---

## Install

Drop `aetherium-fabric-<version>.jar` (or `aetherium-neoforge-<version>.jar`) into `mods/`. Client-side only.

**Recommended:** Iris (shaders) and nothing else.  
Remove Sodium, Embeddium, Radium/Magnesium, VulkanMod, OptiFine/OptiFabric — Aetherium detects them at startup, prints a one-line notice in the top-right HUD, delegates overlapping features, and refuses to hook rendering where two renderers cannot coexist.  
Details: [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md).

## Build

```sh
gradle wrapper --gradle-version 9.4.1   # once
./gradlew buildAll                       # common + fabric + neoforge jars
./gradlew :common:test                   # engine unit tests
./gradlew verifyAll                      # buildAll, tests, and tools/check.py
```

Requirements: JDK 21, Gradle 9.4.1, network access on first build. Versions are pinned in `gradle.properties`, `gradle/libs.versions.toml` and `tools/porting_pins.json`.

There is no published download. A push to `main` runs CI, which attaches jars to a **draft** release. Publish with `gh release edit <tag> --draft=false` after you have run it once, or build it yourself.

## Porting

**[PORTING_MATRIX.md → All versions](PORTING_MATRIX.md#all-versions)** lists all 33 rows.

```sh
tools/port.sh 1.20.1           # apply deltas/1.20.1/changes.patch
tools/port.sh --dry-run 26.3   # apply to a throwaway copy
tools/port.sh --revert 1.20.1  # undo
tools/port.sh --list           # every version with its pins
```

A delta patch touches only `gradle.properties`, the mixin plugin’s version table, and the four mixin/HUD files whose targets move between versions. Everything else is version-independent.

## Configuration

`<game>/aetherium.json` — 58 options in seven groups. Schema in [`CONFIG_SCHEMA.json`](CONFIG_SCHEMA.json).  
Comments allowed, unknown keys preserved, invalid values clamped, atomic saves (with Android fallback). The `Compatibility` toggle is live.

## Android

Runs inside Pojav, Zalith, FCL, Amethyst and DroidBridge. Detects the launcher, reads renderer environment variables, identifies the GL provider (GL4ES, Zink, LTW, MobileGlues, VirGL, ANGLE), and routes the backend accordingly. arm64 only.  
Full details: [docs/ANDROID.md](docs/ANDROID.md).

## Shaders

Iris keeps the shader pipeline. Aetherium keeps geometry and lighting. The `Shaders` tab is the bridge.  
See [docs/IRIS_COMPAT.md](docs/IRIS_COMPAT.md).

## Performance claims

[BENCHMARK.md](BENCHMARK.md) describes the two harnesses that exist. No ratio between this mod and Sodium is asserted anywhere in this repository.

## Tests & Verification

`./gradlew :common:test` runs JUnit 5 tests for engine logic.  
Mechanical checks (`tools/check.py`, `tools/check_refs.py`, `tools/check_java.py`, delta verification) are wired into the build. Type-checking and runtime behaviour require a JDK.

## Layout

```
common/     engine, GUI, mixins, config, platform SPI
fabric/     Fabric entry point, fabric.mod.json
neoforge/   NeoForge entry point, neoforge.mods.toml
deltas/     per-version patches
tools/      porting, generation, verification, benchmark
docs/       architecture, glossary, android, iris, troubleshooting
```

## Documentation

[ARCHITECTURE](docs/ARCHITECTURE.md) · [GLOSSARY](docs/GLOSSARY.md) · [ANDROID](docs/ANDROID.md) · [IRIS](docs/IRIS_COMPAT.md) · [TROUBLESHOOTING](docs/TROUBLESHOOTING.md) · [BUILD PINS](docs/BUILD_PINS.md) · [BENCHMARK](BENCHMARK.md) · [CONTRIBUTING](CONTRIBUTING.md)

---

## Credits

<p align="center">
  <img src="assets/entitybrian.png" width="140" alt="entitybrian"><br>
  <strong>entitybrian</strong><br>
  <sub>Owner · Core Developer · Tester · Maintainer</sub>
</p>

Aetherium is designed, written, tested, and maintained by **entitybrian**.

No studio. No outsourced UI. No AI pass that fills in the screens.  
The person who adds a feature is the person who checks it on real hardware and decides when it is ready to ship.

---

## License

LGPL-3.0-only (see [LICENSE](LICENSE)). Third-party attributions are in [NOTICE](NOTICE).  
Mixin and LWJGL are used under their own licences. No source file from Sodium, Iris, Embeddium or OptiFine is copied into this repository.
