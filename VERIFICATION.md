# Aetherium 1.0.0: verification report

What was checked, how it was checked, and what was **not** checked. The audit trail for the
0.1.0 line (CI compile reports 1–6, the old renderer's defects) is in git history at
`6c12d5b`; none of that code survives in 0.2.0.

Date: 2026-10-10. Branch `arena/46f241da-aetherium`. The sandbox had no Maven access, no
Gradle and no GPU. Java was compiled with ECJ 3.45 on a downloaded JRE (`tools/local_jdk.sh`).

---

## 1. Compilation, all 33 versions

```
$ python3 tools/stubcheck.py --all -q
33/33 versions compile
```

How this works:

1. `.github/workflows/probe.yml` runs on GitHub. It downloads every version's real client jar and
   writes `javap` output for the classes Aetherium touches (`tools/probe_classes.txt`) to
   `tools/probe/<version>.txt`. Those files are committed.
2. `tools/stubgen.py` turns each probe into Java stubs with the exact class, method and field
   signatures of that version.
3. `tools/stubcheck.py` era-selects the sources for the version (`tools/eras.py`, the same function
   `gen_deltas.py` uses to write the patch) and compiles them against those stubs. It uses that
   version's Java level: 8 for 1.16.5, 16 for 1.17, 17 for 1.18–1.20.4, 21 for 1.20.5–1.21.11.
   26.x needs Java 25, but ECJ stops at 24, so 26.x is checked at 24 and API presence still comes
   from the 26.x probes.

What this catches: wrong class, package, method name, parameter types, return types, static vs
instance, and Java-level violations (no `var`/`List.of`/records in the 1.16.5 selection).
What it does not catch is listed in §3.

## 2. Tests and benchmarks

```
$ python3 tools/testrun.py --bench
tests: 78 passed, 0 failed, 0 skipped
```

72 unit tests cover:

- config v1–v3 → v4 migration and JSON round-trips
- value clamping
- preset application
- conflict scanning, including the VulkanMod/Sodium advice
- light-field packing
- the frame limiter and adaptive distance controller
- the animation easing
- version-range parsing

The 6 opt-in micro-benchmarks (CPU only; these are not FPS):

| measurement | ns/op |
| --- | ---: |
| `LightField.adjustPacked`, 8 sources (dynamic-light lookup) | ~32 |
| `LightField.isEmpty`, no sources (the cost when lights are off) | ~10 |
| `RenderToggles.dropParticle` | ~9 |

The JUnit stand-in (`tools/minijunit`) exists because no JUnit jar could be fetched offline. CI
runs the real JUnit 5 via Gradle on every ship leg.

## 3. What this does **not** establish

- **Nothing was launched.** No Minecraft client and no GPU. Rendering correctness, the visual
  match of the settings screen, and real FPS gains have not been observed. They follow from the
  code paths, not from measurement.
- **Mixin injection points are not compiled.** Mixin `method = "..."` strings are only resolved
  at game start. Every target was checked by hand against the probes: name, descriptor, and
  static-ness for the static light hooks. Every injector is `require = 0`, so a miss disables that
  one feature and logs it instead of crashing.
- **Stubs are signatures, not behaviour.** A method that exists but behaves differently between
  versions is not caught (for example, the clamping of `OptionInstance` values).
- **The loader shells** (Fabric entrypoint, NeoForge `@Mod` and config paths) compile against
  hand-written stubs in `tools/stubs_lib`, not real loader jars. CI's Gradle build uses the real ones.

## 4. Repository checks

```
$ python3 tools/check.py
Aetherium check: clean (300 files, 11 [UNVERIFIED] marks, 300 text files parsed)
$ python3 tools/check_refs.py .
Aetherium reference check: clean (69 files, 91 types)
$ python3 tools/check_java.py .
Aetherium Java structure: clean (115 files)
$ python3 tools/gen_resources.py --check
parsed 30 options, 89 lang keys, 7 groups
$ python3 tools/gen_deltas.py --all --verify
all 32 delta patches apply cleanly to the current tree, and every emitted mixins.json matches
its patch result (no patch for: 1.21.1 - the reference is the tree)
```

`--verify` also asserts that every era block in the tree sits on its **1.21.1** variant. The
reference build applies no patch, so a block left on another version's variant would compile that
version's code into the 1.21.1 jar. This check was added after exactly that was found:
`WeatherMixin` targeted `WeatherEffectRenderer` (1.21.2+) in the raw tree.

## 5. `[UNVERIFIED]` marks (7 questions)

1. `IrisApiV0#getSunPathRotation` return type on Iris 1.7/1.8. It is bound reflectively, so a
   mismatch reads as 0.
2. `FMLEnvironment#dist` on NeoForge 1.21.1. Read reflectively.
3. NeoForge's version-helper class names. Read reflectively, with a fallback.
4. What `FMLPaths.GAMEDIR/CONFIGDIR` resolve to at runtime. The resolved path is logged at startup.
5. 1.16.5, 1.17.1 and 1.18.1 have no per-version Fabric API tag, so the base-version pin is used.
   This is compile-only and Aetherium does not need Fabric API at runtime.
6. Which porting boundary is guessed (`tools/gen_deltas.py` header).
7. A historical note in `PORTING_MATRIX.md`.

## 6. Known gaps

- The adaptive render distance changes the vanilla option. If the game saves `options.txt` while
  it is lowered, the lowered value is saved.
- The world keeps rendering behind the settings screen while it is open (vanilla does the same
  in-game).
- *Weather off* cancels `tickRain`, which also stops rain sounds and splash particles.
- NeoForge exposes no Aetherium events or API; integration is through the mixins only.
- 1.20.2, 1.20.3 and 1.20.5 are Fabric-only, because no NeoForge bundle exists for them.
  1.20.6 NeoForge is pinned to 20.6.141, the final 1.20.6 build.

## 7. CI

`ship.yml` builds every version with its real toolchain (Loom 1.16.1 / ModDevGradle, Gradle
9.4.1, the version's own JDK). That includes the JUnit tests. It publishes `v1.0.0+<version>`
**only for versions whose build and tests passed**. A red version gets no release and keeps its
jars only as a run artifact.

### Results

| run | commit | result | what it found |
| --- | --- | --- | --- |
| 38057827393 | `44475f2` | 29/33 | 1.16.5: `Optional.isEmpty()` is Java 11 (ECJ's `-source 8` cannot see library levels); 1.20.2/1.20.3/1.20.5: those NeoForge builds publish no ModDevGradle bundle; DOWNLOADS push lost a race with a docs commit |
| 38058962827 | `7f893ce` | 32/33 | 1.16.5 main code now compiles at Java 8; its *tests* (text blocks) did not |
| 38060927789 | `eb58e91` | **33/33** | every version compiles with its real toolchain and passes the JUnit suite; 33 releases, 50 jars (16 Fabric-only versions + 17 with Fabric and NeoForge) |
| 38065183797 | `f2eb838` | **33/33** | 1.0.0: every version, including the four new mixins and the headless GUI tests, builds with its real toolchain and passes the JUnit suite; 33 `v1.0.0+<version>` releases, 50 jars |

Fixes that came out of these runs:

- `tools/java_api_lint.py` now runs inside `stubcheck` for every row below Java 21, so a too-new
  JDK API fails offline.
- The three Fabric-only rows had their pins corrected.
- On rows below Java 17, the unit tests compile and run on JDK 21. The tests never ship, and the
  shipped classes stay at the row's level.
- The DOWNLOADS push now rebases and retries.

26.1–26.3 were built by CI's real JDK 25. Offline, ECJ stops at 24, so CI is the only proof of
those three at their real Java level.

## 8. 1.0.0: what changed and how each piece was checked

### Probe data

The 1.0.0 hooks were chosen from a fresh `javap` probe of every version. The first fresh run was
**wrong for 29 of 33 rows**: the Gradle cache is shared between workflows, and the probe took the
first Minecraft jar it found. That was a 1.20.5 jar, so the "1.16.5" file said `UI_BUTTON_CLICK`
was a `Holder`, which is impossible on 1.16.5. The data was discarded. `probe.yml` now only accepts
a jar whose file name carries the row's exact version (`1.20` does not match `1.20.1`). Every file
header was then checked against its row. The 0.2.0 data (`27b3820`) had been correct, so 0.2.0 was
not affected.

### Hooks per version (from the corrected probe)

| Feature | 1.16.5–1.17.1 | 1.18–1.20.1 | 1.20.2–1.21.8 | 1.21.9–1.21.11 | 26.1 | 26.2–26.3 |
| --- | --- | --- | --- | --- | --- | --- |
| Smooth chunk loading | `ChunkRenderDispatcher.uploadAllPendingUploads()Z` | same, `()V` | `SectionRenderDispatcher…()V` | same | **not available** (no upload queue) | **not available** |
| Animated textures | `TextureAtlas.tick()` | same | same | same | same | same |
| Block entity distance | `BlockEntityRenderDispatcher.render(E,float,PoseStack,MultiBufferSource)` | same | same | `tryExtractRenderState(E,float,CrumblingOverlay)` → `null` | same | `+ boolean` |
| Worker threads | `@ModifyVariable` on `Util.makeExecutor` (vanilla hard-codes `clamp(cores−1,1,7)`) | `max.bg.threads` property | same | same (`net.minecraft.Util`) | property (`net.minecraft.util.Util`) | same |
| UI sounds | `SoundEvents.UI_BUTTON_CLICK` is a `SoundEvent` | `Holder` from 1.19.3 | `Holder` | `Holder` | `Holder` | `Holder` |

Each injected method was checked to have exactly one overload on every version. A handler that
does not fit an overload is a hard error even with `require = 0`. In 1.21.9+, `tryExtractRenderState`
calls `shouldRender` before it creates a state, so `null` is vanilla's own "not visible" result. The
upload queue is vanilla's own `toUpload`; nothing is dropped or reordered.

### Tests

- `AetheriumViewTest` drives the real settings view headlessly on a 427×240 screen with a fake
  clock. It covers the scrollbar lane and the control margin; thumb drag down to the last row; track
  click then drag; a tap that toggles on release with the right sound; a touch swipe that scrolls
  1:1 without toggling, then flings and stops; a small finger wobble still counting as a tap; no
  fling after a pause; the wheel stopping a fling; and the theme switch saving at once and
  cross-fading. A mutation check (tap threshold set to 1000 px) made two of these tests fail, as
  it should.
- `AetheriumThemeTest`: both palettes, WCAG contrast (ink ≥ 7:1, descriptions ≥ 4.5:1 in both
  themes), `mix`/`fade`.
- `PerfLogicTest`: worker-thread sizing for 1–64 cores, the early config reader (comments, junk,
  missing file), `--gameDir` parsing (including paths with spaces), the upload budget with a fake
  clock (short queues drain fully; bursts stop at the budget and never below 8; every task runs
  exactly once), and the block-entity distance.
- Benchmark: `BlockEntityCull.beyond` costs 8.3 ns per block entity.
- Totals: 90 unit tests pass; with `--bench`, 97 pass, 0 fail, 0 skipped. `stubcheck --all` passes
  33/33, including the Java 8 API lint on 1.16.5.

### Not established

- **No in-game run.** The FPS gain on any device is unmeasured. The new features remove work
  vanilla does: texture uploads, chunk-upload bursts, block-entity draws and thread contention.
  How much that is worth depends on the world and the device.
- Worker-thread sizing only applies if the mixin plugin loads before Minecraft's `Util` class
  initializes, which is the normal order on both loaders. If it loads later, vanilla's pool size
  stays in place. Nothing breaks, but the setting has no effect.

