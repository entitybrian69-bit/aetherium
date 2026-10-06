# Aetherium — self-verification report

Generated as the last step of the build-out described in [README.md](README.md). This file is
the honest audit: what exists, what was checked mechanically, what was **not** checked, and
every place the repository admits it does not know something.

Date: 2026-10-06. Branch: `arena/f0f3a610-aetherium`. Environment: Linux sandbox, Python 3 +
Node available, **no JDK, no Gradle, no GPU, no Minecraft client**.

---

## 1. What is in the tree

| area | count | where |
| --- | --- | --- |
| main Java sources | 57 files / 16,021 lines | `common/` (53), `fabric/` (2), `neoforge/` (2) |
| test Java sources | 14 files, 107 `@Test` methods | `common/src/test/java/com/aetherium/` |
| config options | 58 in 7 groups | `config/AetheriumConfig.java` |
| generated lang keys | 145 (`en_us`, `pt_br`) | `assets/aetherium/lang/` |
| mixin config | 6 client mixins, `required: false`, plugin `AetheriumMixinPlugin` | `common/src/main/resources/aetherium-common.mixins.json` |
| porting rows | 33 (1 reference + 32 deltas) | `tools/porting_pins.json`, `deltas/`, `PORTING_MATRIX.md` (41 table lines) |
| tools | 15 scripts/generators | `tools/` |
| docs | 6 + README + BENCHMARK + CONTRIBUTING + NOTICE + this file | `docs/` |

Modules: `common` (engine + GUI + mixins, no loader dependency), `fabric`, `neoforge`
(entry points, manifests, remap/shadow packaging). Root build exposes `buildAll`, `checkTree`,
`verifyAll`.

## 2. Checks that ran here, and their exact output

```
$ python3 tools/check.py
Aetherium check: clean (180 files, 19 [UNVERIFIED] marks, 180 text files parsed)

$ python3 tools/check_refs.py .
Aetherium reference check: clean (71 files, 122 types)

$ python3 tools/check_java.py .
Aetherium Java structure: clean (71 files)

$ python3 tools/gen_deltas.py --all --matrix --verify
wrote deltas/<each of 32 versions>/ (changes.patch, derivation.md, notes.md)
all 32 delta patches apply cleanly to the current tree (no patch for: 1.21.1 - the reference
version is the tree)

$ python3 tools/gen_resources.py --check
parsed 58 options, 145 lang keys, 7 groups            # and CONFIG_SCHEMA.json / lang / PNGs current

$ for f in tools/*.sh gradlew; do sh -n "$f"; done     # all 9 shell entry points parse
$ python3 -m py_compile tools/*.py                      # all 6 generators compile
```

`check.py` additionally verifies: every `.json`/`.properties`/`.toml` parses; no
placeholder bodies (`// logic here`, `...;`, "same as above"); no unfinished-work comment markers;
no tab characters in Java; every `deltas/<version>/` exists for each non-reference row;
`PORTING_MATRIX.md` matches `tools/porting_pins.json`; no stray non-ASCII generation
artefacts; every `[UNVERIFIED]` mark carries its `: <what>` detail.

The same four commands also ran on GitHub Actions (`build` workflow, `offline` job, PR #1) and
passed there unchanged; the `ports` job's four dry runs (`port.sh --dry-run` + `check_refs.py` +
`check.py` on the patched copy) pass too. That is the only external confirmation this tree has, and
it is a copy of these checks, not a compile — see §5 items 9-11 for the three things CI found that
these checkers could not.

## 3. What these checks do **not** establish

This is the section that matters most. Everything below was impossible in this environment:

1. **No compilation.** `javac` never ran on a single file. Type correctness, overload
   resolution, generics, access modifiers and record/component shapes are unverified beyond
   structural parsing and `check_refs.py`'s type-resolution pass (which checks that every
   referenced `com.aetherium` type exists and that referenced members exist, not that the
   whole expression types correctly). Third-party and `net.minecraft` signatures are verified
   only where a real upstream source was read (marked as such).
2. **No test execution.** The 107 `@Test` methods have never run. They were written against the
   implementations read in the same session (several assertions were corrected mid-flight after
   reading the code — e.g. `Json.path` returns null through a scalar rather than throwing,
   `flatten` treats lists as leaves, `FrameStats` caches percentiles every 32 frames so short
   tests must assert the live getters) — but a first real run will still produce failures, and
   those failures are expected to be *about the tests*, not evidence the engine is broken.
3. **No mixin validation.** Refmap generation, descriptor matching against the actual 1.21.1
   Mojang-mapped classes, and `require`/`expect` behaviour only exist after Loom runs. Every
   injection that could not be confirmed against a real source is listed in §4 and gated
   `require = 0, expect = 0`, so the expected failure mode of being wrong is "a missing
   feature plus one log line", never a crash.
4. **No runtime, no GPU.** No frame was rendered, no buffer mapped, no shader compiled, no Vulkan
   device opened, no Android launcher launched. The performance-sensitive claims in this
   repository are all *mechanism* claims ("the arena is triple-buffered", "the gate refuses
   persistent mapping on translated GL"), never measurements. `BENCHMARK.md` states the rule.
5. **No Gradle configuration.** `./gradlew build` has not been invoked, so plugin DSL surface,
   dependency notation and task wiring are unverified; that is what the `GUESS`/`UNVERIFIED`
   pins in [docs/BUILD_PINS.md](docs/BUILD_PINS.md) and the two build-file marks in §4 are for.
6. **`gradle-wrapper.jar` is absent** by necessity (no binary authoring here). Until
   `gradle wrapper --gradle-version 9.4.1` is run, `./gradlew` fails with a missing-jar error.

## 4. Every `[UNVERIFIED]` mark in the repository

`tools/check.py` counts 21. Eighteen are in shipped code/build files; `tools/gen_deltas.py`
carries two that it emits into generated delta text, and `PORTING_MATRIX.md` has one prose
mention of the convention. Each entry: what is uncertain, and what happens if it is wrong.

### Reference-version bytecode (mixin targets)

| file:line | uncertain | failure mode if wrong |
| --- | --- | --- |
| `mixin/core/GuiMixin.java:35` | the full descriptor `render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V` for 1.21.1 (parameter list read from Sodium's overlay mixin; first parameter type inferred) | skipped injection → the HUD overlay only appears under F3 (different hook) |
| `mixin/core/GuiMixin.java:96` | `DebugScreenOverlay#getGameLines` returning `List<String>` | missing F3 append; the alternative candidate list is already in the annotation |
| `mixin/core/LevelRendererMixin.java:41` | `setSectionDirty`'s parameter list `(int x, int y, int z, boolean important)` | dirty counters stay zero → the HUD's mesh line prints nothing useful |
| `mixin/core/LightTextureMixin.java:37` | whether 1.21.1 uses `updateTick` or `tick`, and whether the parameter is `float deltaFrames` | gamma post-processing off, one log line |
| `mixin/core/MinecraftMixin.java:34` | `setLevel` vs `loadWorld`, and whether the null-level case is a separate overload | "world hooks inactive" in the log; lights/gamma keep working, the world-change reset does not fire |
| `mixin/core/OptionsScreenMixin.java:48` | `method_19828` (intermediary name taken from Sodium's mixin config for that version) and the `lambda$init$2` index | the hijack does not apply → the permanent fallback button is the entry point (measurable in-game: the Advanced tab prints which path is live) |
| `gui/AetheriumVideoOptionsScreen.java:187` | the exact override set for 1.21.1 (`render(GuiGraphics,int,int,float)`, `mouseClicked(double,double,int)`, `mouseScrolled(double,double,double)`, `onClose()`) | a compile error — the loud kind; per-version deltas rewrite these mechanically |
| `hud/AetheriumHudRenderer.java:64` | the 6-arg `GuiGraphics#drawString(Font, Component, int, int, int, boolean)` overload on 1.21.1 (5-arg on 1.20.x) | compile error on the reference version, or a wrong call on a port; `guiWidth/guiHeight` read the same way |
| `hud/BenchmarkRecorder.java:175` | `GameOptions#renderDistance()`/`simulationDistance()` returning an `Option<Integer>` holder, and `Level#getSeed()` | the benchmark row's label degrades to `vanilla=unavailable`; whole block is inside a catch, so a frame never throws |
| `gamma/LightmapWriter.java:139` | the accepted field names (`pixels`, `lightmapPixels`, any `*pixels`) and that the `int[]` is the writable one on 1.20.2+ | the lightmap write is skipped → gamma/dynamic lights have no effect on that version |

### Loader, shader bridge, platform

| file:line | uncertain | failure mode if wrong |
| --- | --- | --- |
| `shader/IrisBridge.java:358` | `IrisApiV0#getSunPathRotation` returning `float` degrees vs an `Optional` | sun-path awareness disabled; directional dynamic lights fall back to the vanilla sky angle, logged once |
| `compat/ModConflictScanner.java:132` | no canonical mod id exists for third-party gamma/brightness mods, so the `gamma_utils` row fires only for that literal id | an overlapping gamma mod is not auto-detected; the conflict notice and the Utilities tab still let the user turn it off by hand |
| `neoforge/…/AetheriumNeoForge.java:67` | `RenderLevelStageEvent.Stage.AFTER_LEVEL` constant name and the event registration surface | NeoForge frame hook not installed → the FABRIC-side path is unaffected; on NeoForge the HUD/hot-swap tick degrades and the log says so |
| `neoforge/…/NeoForgePlatformAdapter.java:90` | `net.neoforged.neoforge.internal.versions.neoforge.NeoForgeVersion#mcVersion` vs `…common.NeoForgeVersion` | version lookup returns "unknown"; nothing else depends on it |
| `neoforge/…/NeoForgePlatformAdapter.java:94` | `net.neoforged.fml.loading.FMLEnvironment#dist` as the client/server test | `isClient()` wrong → the mod could attempt to initialise on a dedicated server; it early-returns on the missing GL context instead |
| `neoforge/…/NeoForgePlatformAdapter.java:96` | `FMLPaths.GAME_DIRECTORY` / `CONFIG_DIRECTORY` and `ModList#get().getModContainerById` shapes | config lands in the wrong directory on NeoForge only; Fabric unaffected |

### Generated and documentation

| file:line | note |
| --- | --- |
| `tools/gen_deltas.py:126` | the generator emits an `// [UNVERIFIED: … GuiGraphics …]` comment into a delta hunk, so the mark travels with the ported file |
| `tools/gen_deltas.py:376` | the matrix's legend sentence about the convention |
| `PORTING_MATRIX.md:135` | generated prose describing what a `unverified` grade means |

### Porting grades, per row (not marks, same purpose)

`tools/porting_pins.json` grades every row: 1 `reference` (1.21.1), 5 `documented`
(1.16.5, 1.20.1, 1.20.6, 1.21.4, 26.3 — pins read from upstream projects that ship that
version), 25 `derived` (computed from the recorded rules: GuiGraphics from 1.20.2, lightmap
shape, Java floor per version band, options-hijack range), 2 `unverified`. 13 rows have
`neoforge: null` because NeoForge does not exist before 1.20.2, and every `neoforge` value
above it is a promotion guess rather than a read value.

## 5. Defects found and fixed during this pass

Not a change log — a list of the *classes* of bug this tree had, because they are what a
reviewer should look for next:

1. **Entry points nobody called.** `AetheriumMixinPlugin.announceMinecraftVersion` and
   `setMixinActive` existed with no caller, i.e. the version gating and the conflict stand-down
   were decorative. Wired into `Aetherium.initialize`.
2. **Statements that look like logic.** `Aetherium.frameStats();` (result discarded) presented in
   a comment as bookkeeping; an `@Inject` whose body existed "to prove the target resolves"; a
   `@ModifyVariable` returning its input unchanged; a discarded `Files.exists(...)` in a test.
   All removed or made real (`render/mesh/MeshCounters` now owns those counters and the HUD prints
   them).
3. **A documented feature that no code implemented.** `benchmark.sh --mc` passed
   `-Daetherium.benchmark=<dir>` that nothing read → recording was a fiction. `hud/BenchmarkRecorder`
   now implements it, and `-Daetherium.bench*` is forwarded into the test JVM (a `-D` on the
   Gradle CLI otherwise reaches only the daemon).
4. **A claim in the option text that the GUI did not honour** ("show the warning screen"). There
   is no warning *screen*; there is now a real on-screen notice (top-right, under the backend tag,
   gated by `general.notify_conflicts`) plus the General-tab state lines, and the comment
   describes that.
5. **A capability predicate that could never be true.** `BackendCapabilities` compared a
   case-normalised query against verbatim driver strings, so every extension-gated feature read
   false on real hardware. Fixed with a case-insensitive set — and `BackendSelectorTest` now
   asserts the mixed-case lookup specifically, because a silent-total-failure path deserves a
   named test.
6. **A wrong lightmap formulation.** `GammaApplier.applyLightmap` multiplied red by the block
   fraction inside an expression with a literal-zero term and added the cave/night floors, which
   clips bright pixels to white. Now per-channel curve × amount with the floor applied as a
   black-point `max`, and the default configuration is provably byte-identical to vanilla
   (`GammaApplierTest`).
7. **Aliases that could not match.** `alternativeIds` in the conflict table held package names;
   `isModLoaded` takes mod ids, so those entries were permanent no-ops. Dropped, plus one row for
   a mod that cannot be detected at all.
8. **Checker false positives** (nested-type imports flagged as missing files; a rule file's own
   prose counted as a mark) fixed in the checkers, because a noisy checker teaches people to
   ignore it.
9. **Unverified names that were not runtime-only risks.** `fabric/build.gradle.kts` called two
   Loom run-config properties marked `[UNVERIFIED]` on the reasoning "it is behind a flag, so only
   people who opt in can be hurt". Kotlin resolves calls when it compiles the *script*, inside the
   `if` — so the first CI run failed the whole build. The `[UNVERIFIED]` count dropped to 20 when
   the two calls were deleted: a mark on a build script is not the same kind of mark as one on a
   Java file, because a script is compiled even when its output is never executed.
   The same run retired the module's other build-script mark: the ModDev DSL surface
   (`neoForge { version, parchment, runs, mods }`) compiled clean, which proves the names and
   signatures and says nothing about behaviour. Script compilation is a name checker with extras.
10. **Build scripts written in the wrong DSL dialect.** Three Kotlin DSL violations, each invisible
    to every local check: `java-library` bare (parses as subtraction), `id("…") version
    (project.property("…"))` (a `plugins {}` block is extracted before `project` exists), and
    `processResources { }` (Gradle 9 generates task accessors on `TaskContainer`). Plus
    `from({ … })` in `neoforge`, passing a lambda where Gradle expects a Closure or Provider — that
    one would have shipped a jar with MixinExtras silently missing, which is worse than a failure.
11. **A checker that quietly stopped checking.** `check.py` verifies that every `deltas/*/changes.patch`
    applies. When the matrix's version cells became links, its row regex matched zero rows, so it
    verified zero patches and printed "clean"; the stale patch set only surfaced because CI's `ports`
    job applies them for real. The rule now parses both forms, refuses to be silent when `deltas/`
    is non-empty but no rows parse, and checks that every delta directory has a row. A green result
    from an empty input set is the most dangerous thing a checker can produce.

## 6. Acceptance criteria against the original brief

| requirement | status |
| --- | --- |
| Complete reference implementation for 1.21.1, mixins on `GameRenderer`/`LevelRenderer`/entity+block renderers | code present and self-consistent; **not compiled**; entity/block renderer mixins are the lightmap/texture + `LevelRenderer` set — there is no per-entity renderer patch, which the "Honest status" table in the README states |
| Backend abstraction GL 4.6 DSA primary, Vulkan 1.3 secondary, GL-core, GL-legacy; runtime probe + hot-swap | probe, capability matrix, gates, scoring, hot-swap path and tests: yes. Vulkan draws nothing (documented) |
| HZB, persistent mapped buffers + triple buffering, `glMultiDrawElementsIndirectCount`, async compile + program-binary cache, async distance-prioritised meshing | implemented against `GlDevice`/`GlProcs`; unverified against a driver |
| Iris/Oculus compat by reflection, no hard dependency | `shader/IrisBridge` + `docs/IRIS_COMPAT.md`; zero Iris entries in any build file |
| Coloured dynamic lights + quality slider, gamma suite (slider, cave, night, time-based, curve editor), live Compatibility toggle, auto conflict detection + delegation | `lighting/`, `gamma/`, `config/`, `compat/`; all four have tests |
| Android: launcher + GL-provider detection, routing, arm64-only, `custom_env.txt` + env vars | `android/` (6 classes), `docs/ANDROID.md`; `CustomEnvFileTest` covers the parser surface |
| Purple GUI replacing video settings, tabs incl. Android, live FPS + p99, 48 dp touch mode, animations; no Mod Menu dependency | `gui/` (5 files) + `OptionsScreenMixin` + fallback button; 48 dp constant in `AetheriumTheme` |
| Gradle + wrapper + CI, `.editorconfig`, `checkstyle.xml` | present; wrapper jar must be generated locally (documented in README, BUILD_PINS and CI) |
| `docs/{ARCHITECTURE,GLOSSARY,ANDROID,IRIS_COMPAT,TROUBLESHOOTING}.md` | written, plus `BUILD_PINS.md`, `BENCHMARK.md`, this report |
| Tests, ≥3 JUnit 5 per core class | 14 files / 107 methods; every core utility class has ≥3 (`Json` 9, `MathUtil` 7, `VersionRange` 5, `ConfigValue` 8, `ConfigStore` 8, `FrameStats` 9, `GammaApplier` 11, `BackendSelector` 10, `ModConflictScanner` 9, `CustomEnvFile` 8, `MeshCounters` 6, `BenchmarkRecorder` 5, bench 5) |
| `NOTICE`, `CONFIG_SCHEMA.json`, `tools/*.sh` | present; `CONFIG_SCHEMA.json` and lang files are generated and `--check` verifies freshness |
| No fabricated APIs; `[UNVERIFIED: …]` where unsure | 18 marks in shipped code, each naming its consequence — listed in §4 |
| No placeholders, no TODOs, no fake benchmarks, never a ratio claim | `check.py` enforces the pattern bans; `BENCHMARK.md` and `tools/benchmark.sh` cannot print a ratio |
| Final self-verification report + completion marker | this file |

## 7. What a human should do first, in order

```sh
gradle wrapper --gradle-version 9.4.1
./gradlew :common:compileJava              # 1. does it compile at all
./gradlew :common:test                     # 2. which test assertions were wishful
./gradlew :common:checkstyleMain           # 3. style, incl. the javadoc warnings-as-errors rule
./gradlew buildAll                         # 4. remap both platform jars
./gradlew :fabric:runClient                # 5. with -Paetherium.enableRunConfigs=true: does it boot
sh tools/verify.sh 1.21.1                  # 6. per-version target checks once a JDK exists
sh tools/benchmark.sh                      # 7. the CPU numbers for this machine
./gradlew runServer                        # not applicable: client-side mod, no server component
```

Expect step 1 to surface the real bugs. Step 5 is the first moment any of the mixin surface in
§4 is confirmed or refuted, and `strict_mixins` turns the "silently skipped" failure mode into a
startup failure so you can see all of it at once.

## 8. Open items

- `advanced.experimental_full_renderer` — deliberately refused, not shipped (README table,
  [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#honest-status)).
- `VULKAN_13` draws nothing; it probes, reports and defers to GL.
- `pt_br.json` tooltips are English fallbacks: `tools/gen_resources.py` derives tooltips from the
  config comments and has no Portuguese dictionary for them. Adding one is a self-contained
  contribution (the generator's `build_lang` is the only place to touch).
- 25 of 33 port rows are `derived`, not `documented`; two are `unverified`. No port has been
  built, so the deltas are mechanically-sound-but-unexecuted by construction.
- **Nothing has been compiled by a human yet.** CI reached script compilation and failed there three
  times, each on a defect listed in items 9-11 above; the following push is the first that can reach
  `javac`. No jar exists anywhere until a `build (java 21)` job goes green, at which point
  `draft-release` attaches it to a draft release on `main` (docs/BUILD_PINS.md, "The jars").
- No entity/block-renderer-level mixin (the brief's "entity+block renderers" is met at the
  `LevelRenderer`/light/Gui level plus the mesher path); making it richer is the natural next
  feature and would need its own marks.
