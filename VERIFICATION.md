# Aetherium 1.3.4: verification report

What was checked, how it was checked, and what was **not** checked. The audit trail for the
0.1.0 line (CI compile reports 1–6, the old renderer's defects) is in git history at
`6c12d5b`; none of that code survives in 0.2.0.

Date: 2026-10-11 (1.1.0; §9), 2026-10-10 (1.0.0). Branch `arena/46f241da-aetherium`. The sandbox had no Maven access, no
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

## 5. `[UNVERIFIED]` marks (6 questions, 10 marks)

1. `FMLEnvironment#dist` on NeoForge 1.21.1. Read reflectively.
2. NeoForge's version-helper class names. Read reflectively, with a fallback.
3. What `FMLPaths.GAMEDIR/CONFIGDIR` resolve to at runtime. The resolved path is logged at startup.
4. 1.16.5, 1.17.1 and 1.18.1 have no per-version Fabric API tag, so the base-version pin is used.
   This is compile-only and Aetherium does not need Fabric API at runtime.
5. Which porting boundary is guessed (`tools/gen_deltas.py` header).
6. A historical note in `PORTING_MATRIX.md`.

1.1.0 removed the shader-mod bridge, and with it the seventh question (a reflectively bound
shader-mod method).

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
| 38078947462 | `1bee594` | **33/33** | 1.1.0: the frame-rate fixes, config v5 migration, description popup and the shader-mod removal build with every real toolchain and pass the JUnit suite (35/35 jobs); 33 `v1.1.0+<version>` releases, 50 jars |
| 38102357434 | `ebf9a14` | **33/33** | 1.2.0: entity occlusion culling (five `section-vis` and three `occlusion-box` variants) and the experimental 1.16.5 chunk renderer build with every real toolchain and pass the JUnit suite (35/35 jobs); 33 `v1.2.0+<version>` releases, 50 jars |

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

## 9. 1.1.0: the frame-rate fixes and how each was checked

Users reported a large FPS drop on 1.0.0, on desktop and on phones. Nothing could be measured in
game (§3), so the causes were found by reading every hook that runs per frame, per tick, per entity
or per vertex. Each one below is a cost that 1.0.0 paid with its default settings or its *Max FPS*
preset.

| # | Cause | Fix | Check |
| --- | --- | --- | --- |
| 1 | `android.thermal_guard` defaulted to **true**, and `updateThermal` did not check for Android. It capped the frame rate at 30 whenever the hottest `cpu/gpu/package/soc` zone read 68 °C or more, which Linux desktops and loaded phones reach. The zones were read on the render thread every 100 ticks. | Default false; only on Android; ceiling 75 °C; `android/ThermalMonitor` polls on a daemon thread and the render thread reads a `volatile` int. | `ConfigStoreTest.frameRateFeaturesDefaultOff`, `migratesFrameRateDefaults` |
| 2 | `AdaptiveDistance` changed the distance after one slow 2 s window, with a 2-window cooldown. Each change runs vanilla's `allChanged` (all chunks rebuilt), and the rebuild then read as the next slow window. *Max FPS* turned it on. | Removed from all presets and off after migration. Now it needs 5 consecutive slow windows (< 85 % of target), 15 fast windows (> 140 %) to rise, and a 15-window cooldown during which nothing counts. | `PerfLogicTest.adaptiveDistanceIsPatient`: a constantly slow game changes at most once per 40 s |
| 3 | `LightLevelMixin` / `EntityLightMixin`: a cancellable `@Inject` on `LevelRenderer.getLightColor` (called per vertex while meshing) allocated a `CallbackInfoReturnable` per call, even with dynamic lights off. | `AetheriumMixinPlugin.onLoad` reads `aetherium.json` (`config/EarlyConfig`). The two mixins are applied only if dynamic lights are on in a v5+ file. Otherwise the tracker stays idle and the Effects tab shows "Off (zero cost)" or "Restart to apply". | `PerfLogicTest.lightHooksFromLaunchConfig` |
| 4 | `OptionInstanceMixin`: a cancellable `@Inject` at the HEAD of `OptionInstance.get()` (every option read, including ambient occlusion per block while meshing) allocated per call, only to support fullbright. | No injector. The mixin `@Shadow`s the `T value` field and implements `client/GammaSlot`. Fullbright writes the boxed override into the field and restores the user's value around `Options.save()`. The field exists, non-final, in all 27 instance-era probes (1.19–26.3; package-private up to 1.21.10, private from 1.21.11). ≤1.18.2 still writes `Options.gamma` directly. | `stubcheck --all`; probe grep in the 1.1.0 commit message |
| 5 | `UploadBudget.drain` called `size()` on vanilla's `ConcurrentLinkedQueue` every frame, which walks the whole queue. | `fewerThan(queue, 24)` stops counting at 24. | `PerfLogicTest.fewerThanStopsEarly` |
| 6 | Settings screen: the hovered option's description was cut to one footer line ("…far..."). The footer's `fit()` also measured the string once per removed character, every frame. | A Sodium-style popup (`AetheriumView.drawTooltip`): the full text word-wrapped (cached per row), placed under or above the row and clamped to the screen, fading in after 0.35 s. `fit()` now uses binary search. | `AetheriumViewTest.descriptionPopup` (no word dropped, every line fits, instant swap, closes on leave), `popupHidesWhileBusy`, `fitIsExact`; removing the draw call makes `descriptionPopup` fail |

Config migration (schema 4 → 5): `android.thermal_guard`, `performance.adaptive_distance` and
`effects.dynamic_lights` return to their (off) defaults once; a stored ceiling of exactly 68 °C
becomes 75 °C; the removed `shaders.*` keys are dropped. Other values are kept. A v5 file keeps
explicit opt-ins (`ConfigStoreTest.currentVersionKeepsOptIns`).

Shader-mod branding removed: the shader page and its icon, `shader/IrisBridge`, the `shaders.*`
options and lang keys, `suggests` in `fabric.mod.json`, `PlatformAdapter.openShaderPackScreen` and
`docs/IRIS_COMPAT.md`. The conflict scanner still reports a shader mod as informational, with
neutral advice.

Gates run for 1.1.0: `stubcheck --all` 33/33 (Java 8 API lint on 1.16.5);
`testrun.py --bench` 106 passed, 0 failed (99 unit + 7 benchmarks); `gen_resources --check`
(34 options, 93 lang keys); `gen_deltas --all --verify` (32 patches apply cleanly); `check.py`
clean; `check_refs.py` clean.

Not established: the frame-rate change on any device. Fixes 1 and 2 remove a 30 FPS cap and
repeated world reloads, which should be clearly visible where they applied. Fixes 3–5 remove
per-call allocations and a queue walk, whose effect depends on how the JIT handled them.

## 10. 1.2.0: occlusion culling and the experimental chunk renderer

### Probe data

Two probe runs added the classes the new hooks touch: `LevelRenderer$RenderChunkInfo`,
`ChunkRenderDispatcher$RenderChunk` / `$CompiledChunk`, `SectionRenderDispatcher$RenderSection`,
`VertexBuffer`, `VertexFormat`, `DefaultVertexFormat`, `RenderType`, `RenderStateShard`,
`PoseStack$Pose`, `Matrix4f`, `FogRenderer` and `Camera`. They also added code probes for
`LevelRenderer.renderChunkLayer`, `VertexBuffer.bind/draw/unbind`, `GlStateManager`,
`LightTexture`, `VertexFormat` and both section classes.

| Fact | Versions (from `tools/probe/<ver>.txt`) | Used by |
| --- | --- | --- |
| Visible list `ObjectList<RenderChunkInfo> renderChunks` | 1.16.5 | era `section-vis` = `list16` |
| `ObjectArrayList<RenderChunkInfo> renderChunks` | 1.17–1.17.1 | `list17` |
| `ObjectArrayList<RenderChunkInfo> renderChunksInFrustum` | 1.18–1.20.1 | `list18` |
| `ObjectArrayList<RenderSection> visibleSections`, `RenderSection.getOrigin()` | 1.20.2–1.21.4 | `sections` |
| Same list, `RenderSection.getRenderOrigin()` | 1.21.5–26.3 | `render-origin` |
| `RenderChunkInfo.chunk` (`private final` 1.16.5, package-private 1.17–1.20.1), `RenderChunk.getOrigin()` | 1.16.5–1.20.1 | `RenderChunkInfoMixin` |
| `EntityRenderer.shouldRender(T, Frustum, double×3)`, `Entity.noCulling`, `Entity.getBoundingBoxForCulling()` | 1.16.5–1.21.1 | era `occlusion-box` = `entity` |
| Same `shouldRender`, `EntityRenderer.affectedByCulling(T)`, `getBoundingBoxForCulling(T)` | 1.21.2–26.2 | `renderer` |
| `shouldRender(T, Frustum, double×3, float)`, `getBoundingBoxForCulling(T, float)` | 26.3 | `renderer-tick` |
| `Minecraft.levelRenderer`, `Minecraft.shouldEntityAppearGlowing(Entity)`, `Entity.isCustomNameVisible()` | all 33 | `ClientHooks.hiddenBehindTerrain` |
| 1.21.11+ `isSectionCompiledAndVisible` reads `RenderSection.getVisibility(long)` (fade-in), not occlusion | 1.21.11–26.3 | reason the culling applies there too |
| 1.16.5 `renderChunkLayer`: `setupRenderState`, filter by `getCompiledChunk().isEmpty`, per chunk `getBuffer` → `getOrigin` → `bind` → `setupBufferState` → `VertexBuffer.draw(Matrix4f, 7)` = push/loadIdentity/multMatrix/drawArrays/pop, then `unbind`, `clearBufferState`, `clearRenderState` | 1.16.5 | `ChunkRendererMixin` |
| `VertexBuffer.id`, `vertexCount` (`private int`); `Matrix4f.store(FloatBuffer)`; `GlStateManager._glUseProgram` / `_glBindBuffer` call GL directly (no cached program or buffer binding to desync) | 1.16.5 | `VertexBufferMixin`, `ChunkRenderer` |

### Checks

| Piece | Check |
| --- | --- |
| `perf/SectionVisibility` (bit grid, conservative edges) | `SectionVisibilityTest`: unknown frame or empty list → visible; hidden section culls; a box straddling a visible section stays visible; negative coordinates floor correctly; rows above/below every visible section, off-grid, huge and NaN boxes stay visible; the grid recentres and clears between frames, and its corners do not alias |
| Sodium/Embeddium/Magnesium hand-off | `ModConflictScannerTest.sodiumDelegatesLights`: occlusion culling and the experimental renderer are switched off, distance culling stays on |
| `client/ChunkRenderer` (GPU-free parts) | `ChunkRendererTest`: quad → triangle index layout and winding; the 16-bit range; a worst-case section (98 304 vertices) splits into 2 draws on a quad boundary with no quad lost; GL fog mode mapping; shaders are GLSL 1.20 with no fixed-function built-ins, and attribute/uniform names match the code; BLOCK stride and offsets |
| Era selection | `stubcheck` on 1.16.5, 1.17, 1.18, 1.20.1, 1.20.2, 1.21.4, 1.21.5, 1.21.10, 1.21.11, 26.1, 26.2, 26.3 (one per variant), then `--all` |

### Not established

- Nothing ran on a GPU. The chunk renderer's output was not compared with vanilla's on screen.
  Lightmap coordinates `(uv2 + 8) / 256`, fog distance (radial, like vanilla on NVIDIA's
  `NV_fog_distance`) and alpha cutout follow the 1.16.5 fixed-function state as read from the
  probes. A wrong uniform would show as wrong colours, not a crash, and a compile or link failure
  falls back to vanilla.
- The frame-rate effect of either feature. Occlusion culling helps wherever many entities sit in
  hidden sections (caves under the player, villages behind hills, mob farms). The chunk renderer
  mainly removes GL4ES work.

Gates run for 1.2.0: `stubcheck --all` 33/33 (Java 8 API lint on 1.16.5); `testrun.py --bench`
116 passed, 0 failed (109 unit + 7 benchmarks); `gen_resources --check` (36 options, 97 lang keys);
`gen_deltas --all --verify` (32 patches apply cleanly); `check.py` clean; `check_refs.py` clean.

## 11. 1.3.0: the Aetherium pipeline (1.16.5)

### Probe data

Probe run 38103235990 added `BufferBuilder`, `BufferBuilder$DrawState`,
`com.mojang.datafixers.util.Pair` and `ChunkRenderDispatcher`, plus code probes for the upload path.

| Fact (1.16.5, from `tools/probe/1.16.5.txt`) | Used by |
| --- | --- |
| `VertexBuffer.upload_(BufferBuilder)` is `private void`. It pops `builder.popNextBuffer()` first, then checks `id`, then `vertexCount = getSecond().remaining() / format.getVertexSize()`, `bind()`, `RenderSystem.glBufferData(34962, data, 35044)`, `unbind()`. Both `upload` and `uploadLater` route to it | `VertexBufferMixin.aetherium$upload` repeats those steps, with the data re-encoded |
| `VertexBuffer.format` is `private final VertexFormat`; `VertexFormat.getVertexSize()` | the legacy vertex count when encoding is refused |
| `popNextBuffer()` returns `Pair<DrawState, ByteBuffer>`, a `slice()` of the builder's buffer | `CompactTerrain.encode` reads `position()`..`limit()` of a duplicate |
| `RenderChunk(ChunkRenderDispatcher)` assigns `buffers` (one `VertexBuffer(DefaultVertexFormat.BLOCK)` per layer) in the constructor; `getBuffer(RenderType)` is public | `RenderChunkBuffersMixin` tags solid / cutout-mipped / cutout at constructor TAIL |
| `ChunkRenderDispatcher.uploadChunkLayer` queues the upload on `toUpload`, drained by `uploadAllPendingUploads()` from `LevelRenderer.compileChunksUntil` (render thread) | the encoder's scratch buffers are per thread anyway (`ThreadLocal`) |
| `LevelRenderer.allChanged()` and `renderLevel(PoseStack, float, long, boolean, Camera, GameRenderer, LightTexture, Matrix4f)` are public | `ChunkRendererMixin.aetherium$pipelineSwitch` rebuilds at the start of a frame |

### Checks

| Piece | Check |
| --- | --- |
| `render/CompactTerrain` encoder | `CompactTerrainTest`: a cube's six faces (vanilla `FaceInfo` vertex order) land in their six groups with exact position, light, colour and uv; positions on the 1/16 grid round-trip exactly and others within 1/2048 across the whole range; the stored planes are the outermost faces |
| Classification | diagonal plant quads with a cardinal normal, a reversed-winding top face with an up normal, a side face labelled up, and an off-axis normal all go to *any* |
| Refusal | x beyond 56 or below -8, light above 255, and a partial quad all return null (vanilla data is uploaded); the scratch stays usable |
| Culling planes | camera above/south-east draws exactly up/south/east/any, below/north-west draws down/north/west/any; edge-on keeps the face; inside a closed cube only *any*; between two cubes both facing groups stay; empty groups are never selected |
| Ranges | `runs` produces contiguous ranges across empty groups; `mergeRuns` merges gaps up to the limit and keeps larger ones |
| Shader | `ChunkRendererTest.compactShader`: GLSL 1.20, decodes `a_pos.xyz / 1024.0 - vec3(8.0)`, light unpacking mirrored on the CPU for the whole range, no exponent literals |
| Switching | `ChunkRendererTest.pipelineSwitching`: rebuild once on enable; on disable only if a compact buffer was stored; never every frame |

### Not established

- Nothing ran on a GPU. The compact program's output was not compared with vanilla's on screen.
  Unsigned-short attributes read as unnormalised floats (`glVertexAttribPointer`, normalised =
  false) are core GL 2.0 behaviour, but GL4ES's handling of them was not observed.
- The frame-rate effect. Expected: half the terrain vertex memory and bandwidth, plus skipping
  roughly a third to a half of terrain quads per frame (the back-facing groups), at the cost of one
  to three draw calls per section instead of one.
- Interaction with other 1.16.5 mods that read terrain vertex buffers directly. Sodium-family mods
  already switch the pipeline off.

Gates run for 1.3.0: `stubcheck --all` 33/33 (Java 8 API lint on the new 1.16.5 sources);
`testrun.py` 118 unit tests passed; `gen_resources --check` (36 options, 97 lang keys);
`gen_deltas --all --verify` (32 patches apply cleanly); `check.py` clean; `check_refs.py` clean.
CI: `ship` run 38104354261 for commit d52294c passed all 35 jobs and published 33 releases
`v1.3.0+<mc>` (50 jars, listed in `DOWNLOADS.md`).

## 12. 1.3.1: crash guard

Report: 1.16.5, Fabric, Android (PojavLauncher-based launcher, renderer reported as MobileGlues /
LTW), LWJGL 3.3.3. The game closed right after `Aetherium pipeline ready`, with no Java exception
in `latest.log`. That means a native crash. Its cause is **not established**: no native crash log
(`hs_err_pid*.log`) or launcher message has been seen yet.

| Change | Check |
| --- | --- |
| `perf/PipelineCrashGuard` marker file | `PipelineCrashGuardTest`: a session that dies while armed switches the option off once at the next start and removes the marker; re-enabling and drawing clears the note; a clean exit removes the marker; a stale marker with the option already off changes nothing |
| Projection from `renderLevel(..., Matrix4f)` | the 1.16.5 probe lists `renderLevel(PoseStack, float, long, boolean, Camera, GameRenderer, LightTexture, Matrix4f)` and `Matrix4f.store(FloatBuffer)`; `GameRenderer` loads that same matrix as the GL projection before calling it. Falls back to the GL query if the injection did not apply |
| First-frame `glGetError` checks, milestone logging | `ChunkRendererTest` (switching still rebuilds exactly once each way); stubcheck 1.16.5 |
| `fabric.mod.json` `_comment` removed | JSON parses; the rationale moved to docs/ARCHITECTURE.md |

Gates run for 1.3.1: `stubcheck --all` 33/33; `testrun.py --bench` 127 passed (120 unit + 7
benchmarks); `gen_resources --check` (36 options, 97 lang keys); `gen_deltas --all --verify` (32
patches apply cleanly); `check.py` clean (335 files); `check_refs.py` clean.
CI: `ship` run 38106872874 for commit 4cb4426 passed all 35 jobs and published 33 releases
`v1.3.1+<mc>` (listed in `DOWNLOADS.md`).

## 13. 1.3.2: no index buffer (1.16.5)

Second report, with 1.3.1: renderer LTW (GL 3.3 core translator), same device and mods. The log
shows `pipeline on: rebuilding`, `first compact chunk upload (356 quads, 23 KB instead of 45 KB)`,
`pipeline ready`, and then nothing: the process died during the first terrain layer (the
`first compact terrain layer drawn` line never came). That places the crash in our first draw
calls. Of those, the indexed draw (`glDrawElements` with a shared element buffer, offsets into it)
is the one vanilla never makes and the one a translator crashes on natively if it does not track
the element-buffer binding. **Not established:** that this was the exact call. No native crash log
has been seen.

| Change | Check |
| --- | --- |
| Compact quads written as two triangles (0-1-2, 2-3-0), drawn with `glDrawArrays(GL_TRIANGLES)`; index buffer removed | `CompactTerrainTest.plainTriangles`: six vertices per quad in that order, per-corner uv preserved; cube round trip at 96 bytes per quad |
| Legacy buffers drawn with `glDrawArrays(GL_QUADS)` (vanilla's call) | `ChunkRendererTest.shaderSources` (GL_QUADS = 7) |
| Fog from `GlStateManager._enableFog/_disableFog/_fogMode(int)/_fogStart/_fogEnd/_fogDensity/_fog(int, float[])` (all in the 1.16.5 probe) via `FogStateMixin` | `ChunkRendererTest.fogTracker`: values, colour only from `GL_FOG_COLOR` with 4 components, enable/disable mapping |

Gates run for 1.3.2: `stubcheck --all` 33/33; `testrun.py --bench` 127 passed (120 unit + 7
benchmarks); `gen_resources --check`; `gen_deltas --all --verify` (32 patches); `check.py` clean (337
files); `check_refs.py` clean.
CI: `ship` run 38108163083 for commit 960afb8 passed all 35 jobs and published 33 releases
`v1.3.2+<mc>`.

## 14. 1.3.3: compatible mode (1.16.5)

Third report, with 1.3.2 on LTW: the crash guard worked (at the next start it logged its warning
and turned the pipeline off). After the user turned it back on, the log showed
`first compact chunk upload (208 quads, 20 KB instead of 26 KB)` and
`pipeline ready (GLSL 1.20, 16-byte compact vertices, plain triangles, no index buffer)`, and then
the process died again at the first draw. With the index buffer gone, the remaining suspects are
the GLSL 1.20 program and the generic `u16` / normalised vertex attributes. **Not established:**
which call crashes. No native crash log (hs_err) has been seen. 1.3.3 therefore adds a path
without either, and per-step trace logging for the shader path.

| Change | Check |
| --- | --- |
| `CompactTerrain.sortQuads`: vanilla's 32-byte quads reordered by face group, byte for byte; refuses only partial quads | `CompactTerrainTest.sortedVanilla`: every byte of 7 quads lands in its group slot, a quad the compact format refuses is accepted, plane and group starts correct |
| Compatible-mode draw replays vanilla's `renderChunkLayer` with the same calls: `VertexBuffer.bind`, `DefaultVertexFormat.BLOCK.setupBufferState(0L)`, `RenderSystem.pushMatrix/loadIdentity/multMatrix/drawArrays(7, …)/popMatrix`, `VertexBuffer.unbind`, `RenderSystem.clearCurrentColor`, `clearBufferState` | All members present in the 1.16.5 probe; vanilla's call order read from the probe's `renderChunkLayer` bytecode listing; `stubcheck 1.16.5` |
| Mode state machine (off / shader / compatible); rebuild on every mode change; compact leftovers skipped in compatible mode | `ChunkRendererTest.pipelineSwitching`, `wantedMode` |
| Java-side shader failure switches to compatible mode; a compatible-mode failure turns the pipeline off | `ChunkRendererTest.shaderFailureDowngrades` |
| Crash marker records the mode; shader-mode or old-format marker sets `advanced.pipeline_shader=false` and keeps the pipeline on; compatible-mode marker turns it off | `PipelineCrashGuardTest.shaderCrashDowngradesFirst` |
| GL renderer/version/GLSL logged once; first 24 shader-mode GL steps logged | Code review |

**Not established:** that compatible mode runs on LTW (it makes only calls vanilla makes, in
vanilla's order, so it should work wherever vanilla terrain works), and how much faster it is than
vanilla there. Its saving is the skipped back-facing groups, not a smaller vertex format.

Gates run for 1.3.3: `stubcheck --all` 33/33; `testrun.py --bench` 131 passed (124 unit + 7
benchmarks); `gen_resources`; `gen_deltas --all --verify` (32 patches); `check.py` clean (337
files); `check_refs.py` clean.
CI: `ship` run 38110414122 for commit 51c7295 passed all 35 jobs.

## 15. 1.3.4: GL4ES vertex attribute fix (1.16.5)

Fourth report, with 1.3.3 in shader mode, including the hs_err file:

- `Aetherium pipeline GL: renderer 'GL4ES using Mali-G57 MC2', version '2.1 gl4es wrapper 1.1.7', GLSL '1.20 via gl4es'`
- last trace line: `compact section: buffer 23124; glDrawArrays(GL_TRIANGLES, 0, 1056)`
- `SIGSEGV (SEGV_MAPERR) si_addr 0x18` in `libltwlegacy.so copy_gl_array+0x1784`, called from
  `copy_gl_pointer_raw ← arrays_to_renderlist ← gl4es_glDrawArrays ← ChunkRenderer.drawCompact`.
  The faulting instruction is `ldrh w8, [x8]` followed by `ucvtf s0, w8`, an unsigned-short-to-float
  conversion. Registers include count 0x420 (= 1056), stride 0x10 (= 16, the compact stride),
  size 4 and GL_FLOAT (0x1406, the conversion target).

gl4es source (github.com/ptitSeb/gl4es; LTW's legacy renderer is a GL4ES build):

- `should_intercept_render` (drawing.c) sends a draw through `arrays_to_renderlist` when attribute
  `ATT_VERTEX` (= generic attribute 0 with more than 8 attributes) is enabled with a type
  `valid_vertex_type` (enum_info.h) rejects. That function accepts only UNSIGNED_BYTE, BYTE, FIXED,
  FLOAT and SHORT.
- `gl4es_glVertexAttribPointer` (vertexattrib.c) stores the raw offset as `pointer` for a
  VBO-backed attribute, unlike `glVertexPointer`, which adds the buffer's CPU copy. The CPU
  conversion `copy_gl_pointer_raw` (array.c) reads `pointer + real_pointer`: a null-based address.

**Established:** the 1.3.3 crash is this path, triggered by the compact position attribute being
`GL_UNSIGNED_SHORT`. **Inferred:** the 1.3.0–1.3.2 crashes on the same device had the same cause
(same attribute type; same point in the frame). **Not established:** that nothing else in shader
mode trips GL4ES. The trace lines and the crash guard's step down to compatible mode remain.

| Change | Check |
| --- | --- |
| Compact position/light stored minus 32768, sent as `GL_SHORT`; shader adds 32768 | `CompactTerrainTest.positionPrecision` (-8 → -32768, 56 - 1/1024 → 32767, round trips), `cubeRoundTrip` (light decode), `ChunkRendererTest.compactShader` (shader decode) |
| Attribute 0 type within gl4es's accepted list | `ChunkRendererTest.attributeZeroTypeIsGl4esSafe` |
| Pre-fix shader markers keep shader mode once; markers carry `shaderfix=2` | `PipelineCrashGuardTest.shaderCrashDowngradesFirst` |

Gates run for 1.3.4: `stubcheck --all` 33/33; `testrun.py --bench` 132 passed (125 unit + 7
benchmarks); `gen_deltas --all --verify` (32 patches); `check.py` clean; `check_refs.py` clean.
CI: `ship` run 38111557998 for commit 417084e passed all 35 jobs and published 33 releases
`v1.3.4+<mc>`.

## 16. 1.4.0: zoom, live dynamic lights, threaded near builds (1.16.5)

Mixin targets, read from `tools/probe/1.16.5.txt` (probe run 38117791115, commit 0b1d525):

| Hook | Bytecode it relies on |
| --- | --- |
| `NearBuildMixin` threaded near builds | `LevelRenderer.setupRender`: `BlockPos.distSqr` (offset 1283) `< 768.0d` (1286), `RenderChunk.isDirtyFromPlayer` (1302), `ChunkRenderDispatcher.rebuildChunkSync(RenderChunk)V` (1334), `setNotDirty` (1339). `compileChunksUntil` uses `RenderChunk.rebuildChunkAsync(ChunkRenderDispatcher)V` + `setNotDirty` for far sections, which is what the wrap does |
| `NearBuildMixin` light budget | `LevelRenderer.renderLevel`: `ClientChunkCache.getLightEngine` (82), `LevelLightEngine.runUpdates(IZZ)I` (90); `LevelLightEngine.hasLightWork()Z` exists |
| `ZoomFovMixin` | `GameRenderer.getFov(Camera, float, boolean)D`, the only `getFov` |
| `ZoomMouseMixin` | `MouseHandler.turnPlayer` reads `Options.sensitivity:D` once (51) and `Options.smoothCamera:Z` once (85); `onScroll(JDD)V` |
| `ZoomCameraMixin` | `Camera.setup` calls private `getMaxZoom(D)D` (133) |
| `KeyMappingAccessor` | `KeyMapping.CATEGORY_SORT_ORDER : Map<String,Integer>` (static); constructor `(String, int, String)`; `isDown()`; `getTranslatedKeyMessage()` |
| `OptionsMixin` zoom keys | `Options.keyMappings : KeyMapping[]` (public final), `Options.load()V`; `Options.getCameraType()` returns `CameraType` with `isFirstPerson()` / `isMirrored()` |
| `LightLevelMixin`, `EntityLightMixin` | unchanged targets; now `@ModifyReturnValue` (MixinExtras 0.5.3, bundled; `MixinExtrasBootstrap.init()` in the plugin for loaders that do not start it) |

**Established:** the targets above exist with these descriptors in the 1.16.5 jar CI probed; every
version compiles (`stubcheck --all` 33/33; the zoom and near-build code is an empty `none` variant
outside 1.16.5 and the plugin skips those mixins there). **Not established:** behaviour in game
(no GPU here). Zoom follows Zume's documented formulas (FOV `min + (fov - min) * z^2`, 150 ms
quartic ease-out, scroll `speed * 0.004` per notch, keys `speed * 0.0001` per ms, sensitivity
`floor + (1 - floor) * z`, third-person distance `0.5 .. 15` with vanilla's 4 blocks as rest);
Zume is OSL-3.0, so no code was copied.

| Change | Check |
| --- | --- |
| Zoom state machine and curves | `ZoomControllerTest` (7 tests: hold, easing, scroll, keys, toggle, third person, reset) |
| Light budget: one slice always, then only while pending and within budget | `PerfLogicTest.lightBudget` |
| Solid program has no `discard`; fog per vertex; no GLSL built-in shadowed | `ChunkRendererTest.shaderSources` |

Gates run for 1.4.0: `stubcheck --all` 33/33; `testrun.py --bench` 140 passed (133 unit + 7
benchmarks); `gen_deltas --all --verify` (32 patches); `gen_resources --check`; `check.py` clean.
CI: `ship` run 38118710605 for commit 8620435 passed (plan, 1.16.5 java 8, downloads) and published
`v1.4.0+1.16.5` (`aetherium-fabric-1.4.0+1.16.5.jar`, 900309 bytes).

## 17. 1.5.0: smooth chunk streaming, fog, sky, particles, freelook (1.16.5)

Mixin targets, read from `tools/probe/1.16.5.txt` (probe runs 38123490488, 38124500503,
38125199118):

| Hook | Bytecode it relies on |
| --- | --- |
| `PacketBudgetMixin` | `Minecraft.runTick(Z)V` calls `runAllTasks()V` once (offset 128); `BlockableEventLoop.pollTask()Z` (protected, reached through `BlockableEventLoopInvoker`) |
| `StreamingLevelMixin` | `LevelRenderer.setupRender` calls `RenderChunk.isDirty()Z` once; `compileChunksUntil(J)V` calls `ChunkRenderDispatcher.uploadAllPendingUploads()Z` (9); `allChanged()V` constructs `ChunkRenderDispatcher(Level, LevelRenderer, Executor, boolean, ChunkBufferBuilderPack)` only when the field is null (33, 40) |
| `RenderChunkDirtyMixin` | `RenderChunk.setDirty(Z)V`, `setNotDirty()V`, `isDirtyFromPlayer()Z` |
| `ChunkPrepareMixin` | `ChunkRenderDispatcher.uploadChunkLayer(BufferBuilder, VertexBuffer)` (returns the upload future, queued from the worker), `dispose()V` |
| `FogControlMixin` | `FogRenderer.setupFog(Camera, FogMode, float, boolean)V`; `FogMode.FOG_TERRAIN`; `Camera.getFluidInCamera()FluidState`; `FluidState.is(Tag<Fluid>)Z`; `FluidTags.WATER/LAVA : Tag$Named<Fluid>`; `MobEffects.BLINDNESS` |
| `SkyMixin` | `LevelRenderer.renderSky(PoseStack, float)V`: `VertexBuffer.draw(Matrix4f, I)V` at 146 (sky), 968 (stars, ordinal 1), 1085 (dark); `BufferUploader.end(BufferBuilder)V` at 450 (sunrise), 689 (sun), 903 (moon). `renderClouds` calls `DimensionSpecialEffects.getCloudHeight()F` |
| `ParticleTypeMixin` | `ParticleEngine.createParticle(ParticleOptions, DDDDDD)Particle`, `destroy(BlockPos, BlockState)V`, `crack(BlockPos, Direction)V`; `ParticleOptions.getType()`; `Registry.PARTICLE_TYPE` |
| `FreelookMouseMixin` | `MouseHandler.turnPlayer()V` calls `LocalPlayer.turn(DD)V` once (242) |
| `FreelookCameraMixin` | `Camera.setup(BlockGetter, Entity, boolean, boolean, float)V` calls `setRotation(FF)V` at 39 (ordinal 0, the entity's view; 125 is the third-person front flip, 190 sleeping) |

**Established:** these targets exist with these descriptors in the 1.16.5 jar CI probed; every
version compiles (`stubcheck --all` 33/33). Outside 1.16.5 the `qol16` era is an empty `none`
variant and the plugin skips the ten new mixins. **Not established:** behaviour in game (no GPU
here). The size of the stutter reduction depends on the device and the server; the stutter log
(Backend tab) records what a slow frame did.

| Change | Check |
| --- | --- |
| Packet budget: minimum tasks, growth under backlog, reset when drained | `StreamingTest` |
| Rebuild coalescing: player edits and near built sections never wait; quiet and max delay | `StreamingTest` |
| Direct buffer pool: power-of-two sizes, reuse, idle cap, native byte order | `StreamingTest` |
| Prepared uploads keyed by builder; adaptive upload budget; stutter spike rule | `StreamingTest` |
| Mesh and worker thread counts on phones and desktops | `StreamingTest.threadSizing` |
| Fog arithmetic, particle categories, freelook state machine and pitch clamp | `QolLogicTest` |

Gates run for 1.5.0: `stubcheck --all` 33/33; `testrun.py --bench` 152 passed (145 unit + 7
benchmarks); `gen_deltas --all --verify` (32 patches); `gen_resources --check`; `check.py` clean;
`check_refs.py` clean; `java_api_lint 8` clean.
CI: `ship` run 38126263750 for commit 310d9cf passed (plan, 1.16.5 java 8, downloads) and published
`v1.5.0+1.16.5` (`aetherium-fabric-1.5.0+1.16.5.jar`, 946133 bytes).

## 18. 1.5.1: held-block crash fix (1.16.5)

Crash report from the user (1.5.0, Mali-G57, gl4es 1.1.5): `IndexOutOfBoundsException: Index 1 out
of bounds for length 1` in `BufferBuilder.popNextBuffer` from `RenderType.end` from
`ItemInHandRenderer.renderHandsWithItems`. Cause: `popNextBuffer` increments
`lastRenderedCountIndex` before `List.get`, so an extra pop on an empty builder throws (swallowed
inside the chunk upload's `CompletableFuture`) and leaves the index past the end; the next
`end()` + pop on that builder (the held block's `Sheets.cutoutBlockSheet`, which is the render
thread's fixed chunk builder in 1.16.5) fails. The extra pop came from `PreparedUploads` keeping one
record per builder: several render-thread rebuilds in one frame overwrote each other's record.

| Change | Check |
| --- | --- |
| No preparation on the render thread (`RenderSystem.isOnRenderThread()`, present in the probe) | code review; `ChunkPrepareMixin` |
| Records are a FIFO per builder; none replaced or dropped | `StreamingTest.preparedUploads` |
| A render-thread rebuild makes the next upload drain run the whole queue, once | `StreamingTest.drainAllAfterRenderThreadBuild` |

Gates run for 1.5.1: `stubcheck --all` 33/33; `testrun.py` 146 passed; `gen_deltas --all --verify`
(32 patches); `check.py` clean; `check_refs.py` clean; `java_api_lint 8` clean.
CI: `ship` run 38128138939 for commit 0a33ac1 passed and published `v1.5.1+1.16.5`
(`aetherium-fabric-1.5.1+1.16.5.jar`, 946576 bytes).

## 19. 1.5.2: NG-GL4ES fork detection, memory caps (1.16.5)

From a user log (TECNO BB4k, 32-bit ARM, 816 MB heap, PowerVR GE8300, Krypton Wrapper 0.4.5,
`POJAV_RENDERER` reported as `opengles_nggl4es`): the probe printed "Unrecognised Android renderer"
and `budget=1024MB heap=788MB`.

| Change | Check |
| --- | --- |
| `opengles_nggl4es`, `ng_gl4es`, `krypton*` parse as NGGL4ES | `AndroidRendererTest.parse` |
| Reported memory budget never exceeds the heap | code review (`AndroidEnvironment`; the value is only logged) |
| `DirectPool` idle cap: heap/16 within 8-48 MiB, at most 16 MiB on 32-bit | `StreamingTest.directPool` |

Gates run for 1.5.2: `stubcheck --all` 33/33; `testrun.py` 147 passed; `gen_deltas --all --verify`
(32 patches); `check.py` clean; `check_refs.py` clean; `java_api_lint 8` clean.
CI: `ship` run 38128995655 for commit 0108aaa passed and published `v1.5.2+1.16.5`
(`aetherium-fabric-1.5.2+1.16.5.jar`, 947080 bytes).

## 20. 1.5.3: shader mode never draws non-compact sections (1.16.5)

User log (Mali-G57, gl4es 1.1.7 via LTW legacy): compatible mode on, then shader mode on 9 s later;
the solid layer drew 3 compact sections ("no GL error"), and the game died in the first cutout layer
after that. Sections whose buffer was still in vanilla's format (built before the switch; since
1.5.0 a worker-prepared mesh can also arrive in the old format after a switch) were drawn by
`ChunkRenderer.draw`: vanilla's 32-byte vertices from generic attributes in a VBO as `GL_QUADS`,
which GL4ES converts on the CPU (`copy_gl_pointer_raw` reads `pointer + real_pointer`, the buffer
offset, as an address; see section 15).

Change: in shader mode only compact sections are drawn with the shader; if a layer has any other
section, the compatible-mode pass (vanilla's `setupBufferState` + `drawArrays(GL_QUADS)`, which skips
compact sections) draws them right after. `ChunkRenderer.draw` is no longer called.

**Not established:** in-game confirmation (no GPU here); the hs_err file was not available, so the
crash site is inferred from the trace and from the GL4ES source read for section 15.

Gates run for 1.5.3: `stubcheck --all` 33/33; `testrun.py` 147 passed; `gen_deltas --all --verify`
(32 patches); `check.py` clean; `check_refs.py` clean.
CI: `ship` run 38130796347 for commit b3927bb passed and published `v1.5.3+1.16.5`
(`aetherium-fabric-1.5.3+1.16.5.jar`, 947105 bytes).

## 21. 1.5.4: worker-prepared meshes survive the first tick in a world (1.16.5)

User crash report (Aetherium 1.5.3, Mali-G57, gl4es): "Rendering chunk",
`IndexOutOfBoundsException: Index 0 out of bounds for length 0` in `BufferBuilder.popNextBuffer`,
called from the `upload_` handler of `VertexBufferMixin` inside `UploadBudget.drain`. The upload found
no `PreparedUploads` record for its builder and popped the builder, which the worker had already
emptied. Cause: `ClientHooks.onClientTick` calls `QolHooks.onLevelChange()` on the first tick after
joining (and on every dimension change), and that called `PreparedUploads.clear()` while uploads of
already-prepared meshes were still queued.

Changes:
- `QolHooks.onLevelChange` no longer clears prepared meshes (it still trims `DirectPool`). They are
  cleared only when the dispatcher is disposed (`ChunkPrepareMixin`), after which its queued uploads
  never run.
- `PreparedUploads.prepare` catches any `Throwable` and returns a record with no data and an error,
  so the upload still finds its record (`StreamingTest`).
- New `BufferBuilderAccessor` (1.16.5, applied with the pipeline mixins). The worker skips builders
  with no pending mesh, and an upload with no record and no pending mesh leaves the section empty
  (vertex count 0) and logs one warning, instead of crashing.

**Not established:** in-game confirmation (no GPU here). The level-change path is inferred from the
code and the trace, and the crash report does not show when in the session the crash happened.

Gates run for 1.5.4: `stubcheck --all` 33/33; `testrun.py` 147 passed; `gen_deltas --all --verify`
(32 patches); `check.py` clean; `check_refs.py` clean; `java_api_lint 8` clean.
CI: `ship` run 38132394475 for commit eeeee9e passed and published `v1.5.4+1.16.5`
(`aetherium-fabric-1.5.4+1.16.5.jar`, 948176 bytes).

## 22. 1.5.5: shader mode removed (1.16.5)

User report after 1.5.4: a native crash (`SIGSEGV` in `memcpy` called from the Mali driver) in
`glDrawArrays` from `BufferUploader` while drawing an entity layer (`MultiBufferSource.BufferSource.endBatch`
from `LevelRenderer.renderEntity`, vertex format size 36). The shader path disables its vertex attributes,
unbinds its buffer and program after each layer (`ChunkRenderer.end`), so no leaked state was found to
blame. This was the third shader-mode crash on this device after a fix, and the user asked for the option
to be removed if it can't be fixed.

Change: `ChunkRendererMixin` calls `ChunkRenderer.updatePipeline(enabled, false)`, so the pipeline only
ever selects compatible mode. The `advanced.pipeline_shader` option, its GUI entry, lang keys and schema
entry are removed (an existing key in a config file is kept as an unknown key and ignored).
`PipelineCrashGuard` ignores markers without `mode=compatible` (shader mode or 1.3.1/1.3.2 markers)
instead of switching modes; a compatible-mode marker still turns the pipeline off
(`PipelineCrashGuardTest.shaderMarkersIgnored`).

**Not established:** that this crash came from shader mode. The report does not show the settings, and the
crash is in vanilla's entity draw, not in a pipeline draw. If it happens in compatible mode too, its cause
is elsewhere.

Gates run for 1.5.5: `stubcheck --all` 33/33; `testrun.py` 147 passed; `gen_deltas --all --verify`
(32 patches); `check.py` clean; `check_refs.py` clean; `java_api_lint 8` clean.
CI: `ship` run 38133270845 for commit 00b03bf passed and published `v1.5.5+1.16.5`
(`aetherium-fabric-1.5.5+1.16.5.jar`, 947052 bytes).

## 23. 1.6.0: batched terrain and less meshing garbage (1.16.5)

Request: "make what sodium did", 1.16.5 only, aimed at the stutter, lag and instability when joining a
world. No custom shader (shader mode was removed in 1.5.5 at the user's request).

**Batched terrain** (`TerrainRegions`, `ArenaAllocator`, `DrawRanges`): in compatible mode, each
solid / cutout-mipped / cutout upload of a section (vanilla BLOCK format, 32 bytes per vertex) is stored
in a page shared by its region (8 x 4 x 8 sections) and layer. Pages start at 2048 vertices and double
per region and layer up to 16384 (512 KiB); larger meshes keep their own buffer. Positions are moved
from section-local to region-local when stored (at most 144 blocks, exact in float). The section's own
`VertexBuffer` then draws nothing (`vertexCount = 0`) and its GL buffer is recreated empty to free
GL4ES's CPU copy. `ChunkRendererMixin` queues the camera-facing face groups of each visible batched
section, then per page: one `glBindBuffer`, one `setupBufferState`, one matrix, and one `GL_QUADS`
`drawArrays` per merged range. Ranges merge across gaps only when the gap is live data (never across
free, uninitialised space), and no draw exceeds 32000 vertices (GL4ES restarts longer quad draws at
vertex 0). Slots are freed on `VertexBuffer.close`, on uploads that stay standalone, on empty or
unusable uploads, on pipeline off, and on `RenderChunk.reset` (a moved section must not be drawn
through a merged range). Releases from other threads are queued until the render thread's next upload
or layer.

**Crash fallback:** `PipelineCrashGuard` records `mode=regions` while batched terrain draws. A regions
marker found at start-up turns batching off and keeps the pipeline on; if batching was already off, it
turns the pipeline off (`PipelineCrashGuardTest.regionsCrashTurnsBatchingOff`).

**Meshing garbage** (`MeshAllocMixin`, `AoFaceAllocMixin`, from the 1.16.5 bytecode in
`tools/probe/1.16.5.txt`): `tesselateWithAO`, `tesselateWithoutAO` and `calculateShape` get one shared
`Direction[]` instead of a `Direction.values()` copy per call. Both tesselate methods reuse one
`BitSet` per thread: `calculateShape` sets both bits that are read for every face. The ambient-occlusion
`calculate` reuses one `MutableBlockPos` per thread: it is set before every read. Mob models were left
alone: their per-vertex vectors do not escape the method, so the JIT can already remove them.

**Not established:** the frame-time gain on the user's devices; batched terrain has not run on a phone yet.

Gates run for 1.6.0: `stubcheck --all` 33/33 (the stub generator now keeps constructors of nested
classes); `testrun.py` 154 passed; `gen_deltas --all --verify` (32 patches); `check.py` clean;
`check_refs.py` clean; `java_api_lint 8` clean.
CI: `ship` run 38137191599 for commit 3dbb857 passed and published `v1.6.0+1.16.5`
(`aetherium-fabric-1.6.0+1.16.5.jar`, 965206 bytes).

## 24. 1.7.0: smart chunk culling, fast particles, visible-only animations, fast models (1.16.5)

Request: "Better hidden-chunk culling, particle batching, and animated textures only when visible / Fast
mob models / In one go", 1.16.5 only. Every hook target, descriptor and argument order below was read
from the 1.16.5 bytecode in `tools/probe/1.16.5.txt`.

**Smart chunk culling** (`VisibilitySearch`, `FogCull`, `StreamingLevelMixin`):
- `setupRender` sets `needsUpdate` in three places (PUTFIELD offsets 491, 563 and 1260).
  - 491 is the camera-moved / `!chunksToCompile.isEmpty()` expression.
  - 563 is the reset where the search starts.
  - 1260 is set by every visible section that is dirty or queued.
- The `isEmpty` value (offset 417) and the 1260 write are held to one search per 50 ms. The upload
  trigger (`compileChunksUntil`) shares the same interval, and a held trigger fires once the interval
  passes. Camera terms are untouched.
- The 563 reset records that the search ran and closes the frame's window. The dirty loop runs after
  the search, so without that it would request a search for the very next frame.
- Both `Frustum.isVisible(AABB)` calls of the search also reject a box whose nearest depth along the
  view is more than 16 blocks past the fog end. They only do so when all of these hold:
  - the frame wrote a fog end before `setupRender` (both `setupFog` calls, offsets 417 and 465, come
    before `setupRender` at 506);
  - the mode is `GL_LINEAR`;
  - the end is at most the render distance.

  A culled section is also not meshed until it comes within that margin.

**Fast particles** (`FastParticleMixin`, `Billboard`, `ViewFrustum`): `SingleQuadParticle.render` HEAD.
- Positions use vanilla's lerp.
- The quad is skipped when its bounding sphere (size times sqrt 2) is outside this frame's frustum.
  That frustum is built at `renderLevel` HEAD from the same projection and pose vanilla uses.
- Corners come from `Billboard` (checked against vanilla's quaternion arithmetic in 200 random cases,
  with and without roll).
- Vertex order, UVs, colour and light are vanilla's (bytecode offsets 298-365 and later).

**Visible-only animations** (`SpriteActivity`, `SpriteActivityMixin`, `TextureAtlasMixin`,
`BlockSpriteUseMixin`, `ItemSpriteUseMixin`, `SectionSpritesMixin`, `CompiledChunkSpritesMixin`):
- After the block atlas reloads, its animated sprites except fire, soul fire and nether portal are
  tracked.
- Uses are recorded from:
  - `getU(D)F` (fluids, `SpriteCoordinateExpander`, breaking particles, overlays);
  - the quad lists of `renderModelFaceAO`, `renderModelFaceFlat` and the static `renderQuadList`;
  - `ItemRenderer.renderQuadList`.
- Inside `RebuildTask.compile` the sprites are collected per section and stored on its
  `CompiledChunk`. Synchronous rebuilds on the render thread are collected as well.
- Once per tick the sections in `renderChunks` mark their sprites, and `cycleAnimationFrames` skips
  `cycleFrames` for sprites unseen for more than 8 ticks.
- A missing hook or duck turns tracking off for the session.
- Known limit: blocks drawn by a custom renderer (a Fabric Renderer API model that isn't a vanilla
  adapter) are not recorded, and the option description says so.

**Fast mob models** (`FastModelMixin`, `ModelCubes`, `ModelVertexWriter`, cube/face/vertex ducks,
`BufferBuilderAccessor`, `DefaultedVertexAccessor`, `Matrix3fAccessor`): `ModelPart.compile` HEAD.
- `compile` passes `iload 4, iload_3` as (overlay, light), so its third argument is light.
- It acts only when vanilla itself would take the fast vertex path:
  - the consumer's class is exactly `BufferBuilder`;
  - the builder is building, in GL_QUADS mode, in format `NEW_ENTITY`;
  - it sits at a vertex boundary with no default colour;
  - the call is on the render thread;
  - the buffer is little-endian.
- Faces are packed once per cube. `Polygon`/`Vertex` aren't public, so the face and vertex arrays are
  found by type (exactly one object-array field each, never by name), in vanilla's array order.
- `Polygon` keeps the array it was given and remaps it in place (bytecode offsets 4-6).
- Bytes match vanilla's arithmetic (`SodiumStyleTest.cubes`, `modelVertices`). The position and count
  are committed only after every byte is written; an exception turns the option off for the session.

**Not established:** frame-time gains on the user's devices. Expected size: fog culling and the search
limit help most at large render distances and while terrain streams in. Particles help in heavy
particle scenes, animations on GL4ES uploads, models with many mobs on screen.

Gates run for 1.7.0: `stubcheck --all` 33/33; `testrun.py` 162 passed (`SodiumStyleTest`: frustum,
depth, billboard, sprites, model vertices, cubes, visibility search, fog cull);
`gen_deltas --all --verify` (32 patches); `check.py` clean; `check_refs.py` clean; `java_api_lint 8`
clean on the 1.16.5 sources.
