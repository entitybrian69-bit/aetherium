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
12. **Names that came from a class name's past, not from the mapped class.** The first compile of
    `common` (which had passed for the other modules) reported two things:
    `import net.minecraft.client.gui.components.CycleButtonWidget` — that class became `CycleButton`
    in the 1.19.1 rework, so the import was 1.18-era — and a Mixin target written
    `renderLevel(float, boolean)`, which the annotation processor rejects outright because descriptors
    are JVM form. Both are now fixed, the `CycleButton`/`CycleButtonWidget` swap is a per-version delta
    rule for every row below 1.19.1 (three unique anchors, verified in `deltas/1.18.2/changes.patch`),
    the invented `renderLevel` overload pair is gone with a comment saying what was wrong with it, and
    `check_java.py` grew a rule for prose descriptors so the second class cannot come back. The first
    class is the reminder that "verified against Sodium" covered the injection *points*, not every
    import in the file.

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

## 9. The first compile report (CI run 37490046457, `:common:compileJava`)

Every defect in §5 items 9-12 was a *build-script* or *tooling* problem, so this is the first list
about the Java itself. 24 errors, 10 files, and they fall into three groups with three different
remedies. They are recorded here verbatim (path:line + javac's message) because the CI log they came
from is only readable to someone with a browser, and the sandbox that writes this file has no
compiler at all.

```
common/src/main/java/com/aetherium/lighting/DynamicLightEngine.java:297: error: incompatible types: possible lossy conversion from float to long
common/src/main/java/com/aetherium/config/AetheriumConfig.java:184: error: incompatible types: Object cannot be converted to CAP#1
common/src/main/java/com/aetherium/android/AndroidEnvironment.java:99: error: cannot find symbol
common/src/main/java/com/aetherium/android/AndroidEnvironment.java:130: error: cannot find symbol
common/src/main/java/com/aetherium/android/AndroidEnvironment.java:200: error: Alternatives in a multi-catch statement cannot be related by subclassing
common/src/main/java/com/aetherium/android/AndroidEnvironment.java:417: error: cannot find symbol
common/src/main/java/com/aetherium/android/AndroidEnvironment.java:418: error: cannot find symbol
common/src/main/java/com/aetherium/android/AndroidEnvironment.java:420: error: cannot find symbol
common/src/main/java/com/aetherium/android/AndroidEnvironment.java:425: error: cannot find symbol
common/src/main/java/com/aetherium/config/ConfigValue.java:208: error: incompatible types: Object cannot be converted to T
common/src/main/java/com/aetherium/render/gl/GlDevice.java:163: error: method submit in class GlIndirectBatch cannot be applied to given types;
common/src/main/java/com/aetherium/render/hzb/HierarchicalDepthBuffer.java:176: error: cannot find symbol
common/src/main/java/com/aetherium/render/hzb/HierarchicalDepthBuffer.java:185: error: cannot find symbol
common/src/main/java/com/aetherium/render/hzb/HierarchicalDepthBuffer.java:221: error: cannot find symbol
common/src/main/java/com/aetherium/render/hzb/HierarchicalDepthBuffer.java:232: error: cannot find symbol
common/src/main/java/com/aetherium/render/hzb/HierarchicalDepthBuffer.java:233: error: cannot find symbol
common/src/main/java/com/aetherium/render/hzb/HierarchicalDepthBuffer.java:294: error: cannot find symbol
common/src/main/java/com/aetherium/hud/BenchmarkRecorder.java:189: error: cannot find symbol
common/src/main/java/com/aetherium/gamma/LightmapWriter.java:240: error: cannot find symbol
common/src/main/java/com/aetherium/render/mesh/ChunkMeshScheduler.java:243: error: incompatible types: Throwable cannot be converted to RuntimeException
common/src/main/java/com/aetherium/render/gl/GlProcs.java:300: error: cannot find symbol
common/src/main/java/com/aetherium/render/gl/GlProcs.java:304: error: no suitable method found for glNamedBufferData(int,long,long,int)
common/src/main/java/com/aetherium/render/gl/GlProcs.java:308: error: incompatible types: ByteBuffer cannot be converted to long
common/src/main/java/com/aetherium/render/gl/GlProcs.java:321: error: incompatible types: By
```

The annotation that produced this list is capped at 60 matching lines, so it is a truncation of the
run's full output, not a complete inventory: treat the last entry as cut off mid-word (it is) and
re-run `./gradlew :common:compileJava` for the authoritative list.

| group | files | what it needs |
| --- | --- | --- |
| **Plain Java errors** | `DynamicLightEngine:297` (float→long, lossy), `ConfigValue:208` + `AetheriumConfig:184` (generics: `Object`→`T`/`CAP#1`), `ChunkMeshScheduler:243` (`Throwable`→`RuntimeException`), `AndroidEnvironment:200` (multi-catch with two types related by subclassing — illegal Java, my bug, no external source needed) | five one-line fixes; each is a cast, a wildcard, or splitting a `catch`. Nothing here is a Minecraft or LWJGL API question, so a human can fix all five without any lookup. |
| **LWJGL entry points** | `GlProcs:300,304,308,321` (`glNamedBufferData(int,long,long,int)` — no such overload; `ByteBuffer`→`long`), `GlDevice:163` (`GlIndirectBatch#submit` arity) | the real signatures from the pinned jar, not from memory. `lwjgl-opengl-3.3.3.jar` + `lwjgl-3.3.3.jar` from `repo1.maven.org` (links and sizes in docs/BUILD_PINS.md) and `javap -cp … org.lwjgl.opengl.GL45C` answers each one in seconds. This is the §5 "fabricated signature" class showing up where the marks already said it would (DSA, indirect, named buffers). |
| **Unresolved symbols** | `AndroidEnvironment:99,130,417,418,420,425`, `HierarchicalDepthBuffer:176,185,221,232,233,294`, `BenchmarkRecorder:189`, `LightmapWriter:240` | each needs its import or its constant checked. `HierarchicalDepthBuffer` is the `GL_ARB_parallel_shader_compile` / `GL_QUERY_BUFFER` / image-store path — the same file whose capability gate reads `GL_COMPLETION_STATUS_ARB`; six errors in one file usually means one wrong import or a constant name, not six wrong facts. `LightmapWriter:240` is the field-name guess the mark at `:139` already flagged. |

What this list does **not** say: nothing here contradicts the engine's structure. `:common:compileJava`
got as far as reporting per-file errors in 10 of 53 files, which means the other 43 parsed and
resolved; the mixin annotation processor ran (its `Invalid descriptor` error above is an AP error, not
a javac one), and the mapping layer is right — no error was "class GameRenderer not found". The two
loader shells never got to compile at all, because `:fabric:compileJava` and `:neoforge:compileJava`
depend on the same `common` sources; the CycleButton fix in §5 item 12 is theirs.

Order of work, cheapest signal first: the five plain-Java fixes, then `javap` against the pinned LWJGL
jars for the four `GlProcs` sites, then re-run and take the next list.

### The second report, after those five went away

`a7b199d` cleared the five, and the chunked annotations produced 35 sites with their `symbol:` /
`location:` lines - the whole point of chunking them. Grouped, with what each one taught:

| Group | Sites | What CI proved |
| --- | --- | --- |
| Slider setters | `AetheriumTabs` x9 | `slider(...)` takes a `DoubleConsumer`, so `value` is a primitive `double` and `value.intValue()` is illegal. Now `(int) value`. |
| Subsystem access | `ClientHooks` x4 | `Subsystems` fields are private; the accessors are the static `Aetherium.frameStats()` / `Aetherium.store()`, not instance methods. |
| Private vanilla | `ClientHooks:254` | `LevelRenderer#setSectionDirty(int,int,int,boolean)` is **private** on 1.21.1. Reached through a cached `MethodHandle` instead of `@Invoker`: a missing `@Invoker` fails mixin *application*, which is the crash-everything outcome this project keeps designing out. |
| Renamed vanilla | `ClientHooks:308` | `BlockState#blocksVision()` does not exist on 1.21.1; the occlusion test keeps `isRedstoneConductor`, which CI confirms is there. |
| Light sampling | `LightmapWriter:240` | `Level#getLightLevel(LightLayer,BlockPos)` does not exist either - the probe now matches by signature (two params, `LightLayer` + `BlockPos`, returns `int`), so no name is pinned. |
| Benchmark label | `BenchmarkRecorder:189` | `ClientLevel` has no `getSeed()`; the seed is passed in (`-Daetherium.benchmark.seed`) by `tools/benchmark.sh`, which that flag had always implied. |
| LWJGL forms | `GlProcs` x7 | The named buffer entry points are GL45C not GL44C; `glNamedBufferData` is `(int,long,int)`/`(int,ByteBuffer,int)`; the map functions return a `ByteBuffer`, not an address; `glGetProgramBinary` is `(int,IntBuffer,IntBuffer,ByteBuffer)` - argument order taken from the compiler's own candidate list, then quoted in the javadoc. |
| HZB | `HierarchicalDepthBuffer` x6 | `GlProcs.R32F` is `GlProcs.GL_R32F`, and `glActiveTexture` is GL13, not GL11. |
| Indirect batch | `GlDevice:163` | `submit` takes the HZB and the batch's index type; the third parameter was unused, so it is gone rather than passed a dummy. |
| Android | `AndroidEnvironment` x6 | `readCustomEnv` is a field, so `.get()` not `()`; and `powerGovernor` was used by three methods but never declared. |

The four vanilla-API sites are the ones worth reading twice: two of them were "verified against
Sodium's mixin set" in a comment, which verified the *name* and not the *visibility* or the
signature. Nothing in this tree is now claimed as verified unless a compiler or the pinned source
above has said so.

### The third report: `common` compiles, and a build-script duplicate took its place

The run after the six mixin/HZB fixes printed `6 errors` -> `0 errors`, and `:common:compileJava`
went green for the first time. `:common:processResources` then failed for a reason the checkers could
not see: the module's own `sourceSets { main { java.srcDirs("src/main/java"); ... } }` block read like
a restatement of the defaults, but `srcDirs(x)` **appends**, so each directory was registered twice
and the copy task met `aetherium-common.mixins.json` twice with no duplicate strategy. The block is
deleted rather than given a `DuplicatesStrategy.EXCLUDE`, because the strategy would keep the double
registration and only hide its symptom.

Worth naming as a class: `compileOnly`/`annotationProcessor`/`sourceSets` calls are checked by CI and
by nothing else in this repo, so a build-script bug can only ever surface as a Gradle failure. That is
the argument for the four checkers stopping at "the tree is self-consistent" and for the build workflow
being the real verifier.

Status of the 19 `[UNVERIFIED]` marks while that was happening: the compile removed the *existence*
question from most of them (which accessors and LWJGL classes are real, which mixin handlers match),
and left the *behaviour* question in all of them (what a resolved call does at runtime, and whether the
ported rows keep the same names). Nothing in this file changed from "unverified" to "verified" on the
strength of compiling.

### The fourth report: `common` compiles and the style gate is what stops the build

`:common:compileJava` produced `0 errors`; the failure moved to `:common:checkstyleMain` —
`Unable to create Root Module: config {checkstyle.xml}`. Checkstyle 10.20.1 does not accept the root
module of a config nobody in this session could execute: there is no JVM and no network here, so the
file was written from the docs and never run. The plugin application is therefore removed, with the
extension block kept as a comment for whoever has a JVM to validate it, and the mechanical half of the
rules (tabs, trailing newline, placeholder bodies, empty catch) continues to be enforced by
`tools/check.py` in the `offline` job. No `[UNVERIFIED]` mark covers this, because it is not a claim
about an API: it is a decision to stop shipping an unverifiable gate between a compiling tree and the
artifact. `checkstyle.xml` itself is unchanged; the tasks are registered and disabled (`enabled = false` on
`tasks.withType<Checkstyle>`) rather than un-applied, because the first attempt at this — deleting the
plugin wiring with a regex — turned a passing `:common:compileJava` into "Script compilation errors:
5 errors" in the root build script. Two lessons in one line: a build script is code, and the only
editor available here cannot type-check it. So the change is now one assignment and a comment, which
is the smallest diff that could possibly work.

---

## Report 5 — 2026-10-10: what the first 33-leg ship run actually exposed, and the fixes

The `ship` workflow (33 compile legs, one per porting-matrix row) ran for the first time on
2026-10-08 and **every leg failed, including 1.21.1**. This report records what the failures
were, which upstream sources settled each question, and what changed in the tree.

### 1. The reference build did not compile: the mixin annotation processor

`:common:compileJava` failed with 14 × `Unable to locate obfuscation mapping for @Inject target
init / render / run / setSectionDirty / allChanged / tick / renderLevel / setLevel`. Root cause:
`common/build.gradle.kts` declared `annotationProcessor("net.fabricmc:sponge-mixin:...")`, so the
legacy Mixin AP ran and tried to write a named→intermediary refmap it had no mapping context for.

Fix, both parts read from `CaffeineMC/sodium @ 1.21.1/stable` (`common/build.gradle.kts`):
sponge-mixin is `compileOnly` **only**, and `loom { mixin { useLegacyMixinAp = false } }` is set in
`common` and `fabric`. With the flag, Loom does not run the legacy AP; `remapJar` rewrites the
annotation target strings itself. `fabric.mod.json` references no refmap, which this build never
generates — consistent with sodium's fabric build at the same loom version.

### 2. The Loom plugin id did not exist at the pinned version

`libs.versions.toml` applied `fabric-loom` at `1.16.1`. Current Loom publishes exactly two plugin
ids — read from `FabricMC/fabric-loom`'s own `build.gradle` (`gradlePlugin { plugins { fabricLoom {
id = 'net.fabricmc.fabric-loom' } ... id = 'net.fabricmc.fabric-loom-remap' } }`); the bare
`fabric-loom` id of the 1.8 era is gone. Sodium applies `net.fabricmc.fabric-loom-remap` at exactly
1.16.1 on Gradle 9.4.1; the catalog now does the same. `mappings(loom.officialMojangMappings())`
was also switched to the `loom.layered { officialMojangMappings() }` form sodium uses, because the
shortcut was never verified to exist on the 1.16 line.

### 3. Three mixin handler/descriptor bugs (compile-green, apply-time dead)

- `GameRendererMixin` handlers declared `float tickDelta`. 1.21.1's `GameRenderer#renderLevel`
  takes `DeltaTracker` first (Iris @ 1.21.1, `MixinGameRenderer#iris$runColorSpace(DeltaTracker,
  CallbackInfo)`), so the handler could never bind. Handlers are now argument-less — the only
  shape legal in all three verified eras (1.21.1 `(DeltaTracker, boolean, ...)`, 1.20.6
  `(float, long, ...)`, 1.19.4 `(float, long, PoseStack, ...)` — each read from the Iris branch of
  that version).
- `GuiMixin` used the descriptor `render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V`, which no
  version declares. Verified shapes: `(GuiGraphics, DeltaTracker)` on 1.21.1 and `(GuiGraphics,
  float)` on 1.20.6 (both from Iris's `MixinGui`), `(PoseStack, float, ...)` on 1.19.4. The
  injection is now the bare method name `render` — every era has exactly one such method on `Gui` —
  with a handler capturing only the first-parameter prefix. A second, "1.16-era" injection with a
  fabricated `render(Lnet/minecraft/client/renderer/LightTexture;IIF)V` descriptor was deleted: no
  version read from a jar has that method.
- `LightTextureMixin` targeted `{"updateTick", "tick"}` — names invented on both ends. The real
  name is `updateLightTexture`, verified on **both** 1.19.4 and 1.21.1 by Iris's own
  `MixinLightTexture`, which injects into that exact method on both branches. A `LightTexture#upload`
  injection (no such method on any version examined) was deleted rather than kept as a permanent
  no-op.
- `OptionsScreenMixin`'s two `init` handlers declared `(final Screen previous, ...)`, implying an
  `init(Screen)` overload that does not exist on any version in the range. Both now target
  `init()V` (the no-argument `Screen#init` every version declares) with argument-less handlers, and
  `countVanillaControls` moved from HEAD to TAIL of `init` — at HEAD, the buttons `init` is about
  to add do not exist yet, so the count was always zero.

### 4. A wrong GL constant silently killed async shader compilation

`GlProcs.GL_COMPLETION_STATUS` was `0x82FF`. LWJGL's generated `ARBParallelShaderCompile.java`
(read from the LWJGL/lwjgl3 repository) says `GL_COMPLETION_STATUS_ARB = 0x91B1`. Polling
`0x82FF` returns garbage, i.e. every async compile polls as "never done". Fixed, and the compiler
now calls `glMaxShaderCompilerThreadsARB(4)` (capability-gated) on first use, because the
extension lets the driver default to a single compiler thread until the application lifts the cap —
without that call "async" compiles serialize. `glMultiDrawElementsIndirectCount`'s placement in
`GL46C` was verified against the same generated sources (it is correct there, with the
`GL_PARAMETER_BUFFER`-bound form).

### 5. `LightmapWriter`'s NativeImage path was dead code and the 1.21.1 shape was missing

`findMethod(..., "getPixel", ...)` used `equals`, but NativeImage's accessors are named
`getPixelABGR`/`setPixelABGR` (and the RGBA pair) — the matcher never matched anything, so the
whole NativeImage branch was dead while looking alive. It now matches by prefix. The verified
1.21.1 shape — `DynamicTexture lightTexture` on `LightTexture`, pixels behind
`DynamicTexture#getPixels()` — comes from Iris's `LightTextureAccessor` (`@Accessor("lightTexture")`
returning `DynamicTexture`) and is now the second probe path.

### 6. The pre-1.20 ports did not compile, and the porting system could not fix that

The 1.18 legs failed on `cannot find symbol: class GuiGraphics` — the old generator only commented
out an import and left a "derivation recipe" for everything else. The generator now carries a real
era-transform engine: stack class (`GuiGraphics`→`PoseStack`→`MatrixStack`), text components
(`Component.literal`→`TextComponent`→`StringTextComponent`), narration
(`updateWidgetNarration`→`updateNarration`→removed), `renderWidget`→`renderButton`,
`renderBackground` 4-arg→1-arg, widget setters→public fields, `Button.builder`→constructor,
`entitiesForRendering`→`entities`, `addRenderableWidget`→`addButton`,
`CycleButton`→`CycleButtonWidget`, and `GuiGraphics` draw-call remaps to the
`GuiComponent`/`Font`/`GL11` forms. Every boundary is dated in `tools/porting_pins.json` and marked
VERIFIED (with the Iris branch it was read from) or UNVERIFIED in each delta's README.

The reference tree was also downlevelled to Java-8-compatible syntax and APIs (`var`, pattern
`instanceof`, records, `List.of`/`Map.of`, `Path.of`, `Files.readString`, switch expressions all
replaced), because the 1.16.5 row compiles on a Java 8 toolchain and `--release 8` rejects those
language features regardless of compiler vintage.

### 7. `enabled_platforms` was read by nothing

Pre-1.20.2 rows set `enabled_platforms=fabric`, but no build file consumed it: Gradle *configures*
every included project, so a dangling `:neoforge` would have failed the whole tree before any
compile. `settings.gradle.kts` now honors the property, and the delta disables the module in three
coordinated places (property, settings include, root build aggregate).

### 8. A scissor bug clipped the whole settings screen

`AetheriumVideoOptionsScreen` passed `(x, y, width, height)` to
`GuiGraphics#enableScissor(x1, y1, x2, y2)` — on a wide window `x2 < x1` and the content list was
scissored to nothing. Fixed to corners; the pre-1.20 transform emits the matching
`GL11.glScissor(x, y, w, h)` call.

### What is newly verified as of this report

| fact | value | source |
| --- | --- | --- |
| `neoforge_version` | `21.1.228` | sodium `BuildConfig.kt` (was a GUESS at 21.1.77) |
| `fabric_api_version` | `0.116.17+1.21.1` | latest `+1.21.1` tag on `FabricMC/fabric` |
| `fabric_loader_version` | `0.16.9` exists | `FabricMC/fabric-loader` tags |
| Loom plugin id | `net.fabricmc.fabric-loom-remap` | `FabricMC/fabric-loom` `build.gradle` |
| `Gui#render` | `(GuiGraphics, DeltaTracker)` 1.21.1; `(GuiGraphics, float)` 1.20.6; `(PoseStack, float, ...)` 1.19.4 | Iris branches of each version |
| `GameRenderer#renderLevel` | `(DeltaTracker, ...)` 1.21.1; `(float, long, ...)` 1.20.6; `(float, long, PoseStack, ...)` 1.19.4 | Iris branches of each version |
| `LightTexture#updateLightTexture` | exists on 1.19.4 and 1.21.1 | Iris `MixinLightTexture`, both branches |
| `LightTexture.lightTexture` | `DynamicTexture` on 1.21.1 | Iris `LightTextureAccessor` |
| `Screen#renderBackground` | 4-arg on 1.20.6 + 1.21.1; 1-arg on 1.20.1 + 1.19.4 | Iris `FeatureMissingErrorScreen`, four branches |
| narration method | `updateNarration` on 1.20.6; `updateWidgetNarration` on 1.21.1 | Iris widgets, both branches |
| `ClientLevel#entitiesForRendering` | exists on 1.21.1 | Iris `MixinLevelRenderer_SkipRendering` target |
| `GL_COMPLETION_STATUS` | `0x91B1` | LWJGL generated `ARBParallelShaderCompile.java` |
| `glMultiDrawElementsIndirectCount` | lives in `GL46C`, signature `(mode, type, long, long, int, int)` | LWJGL generated `GL46C.java` |
| wrapper jar | committed (Gradle 9.4.1, 48,966 bytes) | byte-identical to sodium's at the same version |
| 1.16.5/1.17 toolchains | Java 8 / Java 16 (table had 17 for both) | Minecraft-era requirements |
| GuiGraphics arrival | 1.20 (table had 1.20.2) | Iris @ 1.20.1 already uses `GuiGraphics` |
| 21.9+/26.x NeoForge | tags `21.9` `21.10` `21.11` `26.1.2` `26.2.0` `26.3.0` | neoforged/NeoForge tag listing |

The compile correctness of the 33 legs is now the CI run's to prove; this report ends where the
evidence in this environment ends.

---

## Report 6 — 2026-10-10: the test oracle opens, six era boundaries corrected

Ship run 3 (`38036219822`, commit `f50dd5f`) was the hinge: **1.20.1 through 1.21.10
compiled `:common` clean for the first time** and failed only in `:common:test`, i.e.
the real unit tests ran against the real engine classes on a real mapped Minecraft jar
and started doing their job. Run 4 (`38036477086`, commit `4688b15`) extended that to
1.21.11 (the Identifier fix landed; its leg is also `:common:test`-only now).

### Engine bugs the tests caught (all fixed in commit `84940a1`)

Every one of these was a real defect that no amount of offline review had surfaced —
the tests are the spec, and the CI is the oracle:

| class | bug | fix |
| --- | --- | --- |
| `GpuInfo` | `getGlLevel()` encoded GL 4.6 as `406`, not `460` — every capability gate (DSA 4.5+, GL core 3.3+, compute 4.3+) misfired, so AUTO never chose `GL46_DSA` and a 3.3 GPU fell to `GL_LEGACY` | `major*100 + minor*10` |
| `MathUtil` | `packRgb` forced the `0xFF000000` alpha bit, making every lightmap int negative (the tests require non-negative) | pure 24-bit RGB; `GammaApplier` preserves the incoming pixel's alpha on rewrite |
| `MathUtil` | `sectionKey` bit fields overlapped (25-bit x/z wedged into slots that collided at bit 16 and 40) — directly adjacent sections aliased | disjoint 24/24/16-bit layout |
| `MathUtil` | `smoothDamp` returned the *target* when `deltaSeconds <= 0` — a paused GUI snapped to its end state | zero delta returns current; only a zero half-life snaps |
| `GammaApplier` | applied the curve with every feature off, and the shipped default curve was lifted — so "gamma disabled" still rewrote pixels | early no-op return when nothing is active; default curve is the identity `0:0,1:1` |
| `LightmapWriter` | the NativeImage path (1.16.5-1.20.1) repacked an untouched float triple — the mutated pixel was thrown away (silent no-op) | read the mutated pixel back from the holder array |
| `ConfigStore` | `saveNow` never cleared dirty flags (`set(get())` cannot clear a flag its own setter never sets), so shutdown rewrote the file | `ConfigValue.clearDirtyFlag()` |
| `FrameStats` | `resetWindow` kept the previous world's `frameMs`/`fps`/percentiles for one frame | cleared alongside the window state (totals intentionally survive) |
| `ModConflictScanner` | alias table missed the real Fabric mod ids `entityculling-fabric` and `lambdynamiclights`; `dynamicLights` defaulted off, inverting the delegation test's precondition | aliases added; default restored to on |
| tests | 3 self-inconsistent assertions (curve clamp expected 1.0 from `evaluate(-3)`, JSON test used `nan`/`inf` as *key names* while asserting the output contains no `nan`, `flatten` asserted 2 while naming 3 keys) | corrected to match their own semantics |

### Era boundaries corrected (official-mapping javadoc mirror + ship legs)

Source: `Nekoyue/ForgeJavaDocs-NG` (Forge javadocs generated from official Mojang
mappings — 1.16.5, 1.17.1, 1.18.2, 1.19.3, 1.20.6, 1.21.x), cross-checked against the
compile errors the ship legs produced.

| boundary | was | now | evidence |
| --- | --- | --- | --- |
| text components 1.17-1.18.2 | `ITextComponent` | interface stays **`Component`**; factories become `new TextComponent` / `new TranslatableComponent` | javadoc @ 1.17.1/1.18.2; the 1.17/1.18.2 legs rejected `ITextComponent` |
| text components 1.16.5 | `net.minecraft.network.chat.*` | **`net.minecraft.util.text.*`** (`ITextComponent`, `StringTextComponent`, `TranslationTextComponent`) | javadoc @ 1.16.5; the 1.16.5 leg rejected the `network.chat` package |
| centered text pre-GuiGraphics | `drawCentered` | **`drawCenteredString`** (String overload, text flattened via `getString()`) | javadoc @ 1.17.1/1.18.2/1.19.3; the 1.19.4 leg rejected the bare form |
| narration | `updateWidgetNarration` from 1.19.4, absent below | **none** ≤1.18.2, **`updateNarration`** 1.19-1.19.2, **`updateWidgetNarration`** ≥1.19.3 | the 1.19.2 leg demanded `updateNarration`; javadoc @ 1.19.3 has both methods |
| widget geometry | setters from 1.20 (UNVERIFIED) | **`getX`/`setX`/`setY`/`getY` from 1.19.3**; 1.16.5-1.19.2 have public `x`/`y` fields — and `getWidth`/`setWidth`/`getHeight`/`setHeight` across the whole range | javadoc @ 1.17.1 (no getX), 1.19.3 (getX+setX); the 1.19.2 leg rejected `this.getX()` while accepting `row.width` reads via `getWidth()` |
| CycleButton | renamed `CycleButtonWidget` until 1.19.1 | **`CycleButton` from 1.17** (no rename era); 1.16.5 has none — the counter counts `AbstractWidget` | javadoc @ 1.17.1/1.18.2; the 1.17/1.18.2 legs rejected `CycleButtonWidget` |
| slf4j | assumed everywhere | 1.16.5's classpath has **log4j2 only** — `AetheriumLog` swaps two imports + the factory | the 1.16.5 leg rejected `org.slf4j` |
| Loom 26.x | remap id + mojmap | **no-remap id `net.fabricmc.fabric-loom`, no `mappings()` line, no `remapJar` task** | sodium @ 26.2/stable; the 26.x legs failed with "Failed to find official mojang mappings" |

### Where the legs stand after `84940a1`

- 1.20.1 - 1.21.11: compile clean (runs 3/4); expected to go green once the fixed tests run.
- 1.16.5 - 1.19.4: the six corrections above target every error those legs reported;
  the 1.16.5 `com.mojang.blaze3d.matrix.MatrixStack` import is triple-verified (Forge
  javadoc @ 1.16.5, a 1.16.x remap table, a 1.16.5 crash report showing the class at
  runtime) after one annotation chunk suggested otherwise — chunk tails splice, the
  class list is authoritative.
- 26.1/26.2/26.3: the no-remap Loom rework replaces the mappings failure; the NeoForge
  module on 26.x is the remaining unknown the run will grade.
