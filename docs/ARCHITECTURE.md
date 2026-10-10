# Aetherium architecture

53 Java files in `common/src/main/java/com/aetherium`, one shared source set for both
loaders. This document explains the shape of that tree, the rules that keep it portable
across 33 Minecraft versions, and the threading model a renderer cannot avoid.

```
Aetherium.java              ownership + lifecycle; every subsystem is reachable from here
RenderSafety.java           frame-boundary guard: what may run, and when
android/     (6)            launcher + GL-provider detection, power/thermal governor
client/      (1)            ClientHooks: the only code a mixin body is allowed to call
compat/      (1)            ModConflictScanner: the overlapping-mod table
config/      (4)            AetheriumConfig (58 options), ConfigValue, ConfigStore, Json
gamma/       (2)            GammaApplier + GammaCurve, LightmapWriter
gui/         (5)            the purple options screen, tabs, theme, animations, widgets
hud/         (3)            FrameStats, AetheriumHudRenderer, BenchmarkRecorder
lighting/    (1)            DynamicLightEngine
mixin/       (1) + (6)      AetheriumMixinPlugin + the six core mixins
platform/    (2)            PlatformAdapter SPI + PlatformServices registry
render/      (15)           backend/ (5), gl/ (6), mesh/ (2), hzb/ (1)
util/        (4)            MathUtil, VersionRange, AetheriumLog, NamedThreadFactory
```

## The one structural rule

**Nothing in `common/` may depend on a loader.** `fabric/` and `neoforge/` contain entry
points, manifests and jar packaging and nothing else. Engine code reaches the game through
two doors: `PlatformAdapter` (what version is this, is a mod loaded, where do files live) and
`com.aetherium.mixin.core` (the six classes that touch vanilla bytecode). That single rule is
what makes a port a patch instead of a fork.

`PlatformServices.register/current/currentOrNull/resetForTests` is the registry; both
adapters implement the same seven-method interface plus two `default` methods
(`openShaderPackScreen`, `installFrameHook`). The contract: render-thread safe, allocation-free
after warm-up, and never throwing for "not found" — an empty `Optional` instead.

## Mixins, and how a version that lacks a target is handled

`aetherium-common.mixins.json` is deliberately `required: false`, uses
`AetheriumMixinPlugin` as its plugin, and sets `injectors.defaultRequire: 0` /
`defaultExpect: 0`. Two independent safety nets sit behind that:

1. **`AetheriumMixinPlugin`** decides per class, before bytecode is written. It knows the
   running Minecraft version (announced by `Aetherium.initialize` via
   `announceMinecraftVersion`, or read from `aetherium.minecraft.version` for classes loaded
   earlier) and a per-target version range table written in the same grammar Gradle uses
   (`[1.20.2,)`). Range parsing lives in `util/VersionRange` — a real class with real tests —
   and a range it cannot read *refuses*, so a malformed table entry can never authorise a
   transform. On a HARD conflict the same entry point (`setMixinActive(false)`) turns the
   whole set off; because the render classes (`GameRenderer`, `LevelRenderer`, `Gui`,
   `LightTexture`, `OptionsScreen`) are first loaded *after* mod initialisation, the veto
   takes effect on the running launch rather than only the next one.
2. **Tolerant annotations** (`require = 0, expect = 0`, plus multi-name target lists like
   `{"onSectionCompleted", "finishMeshBuilding"}`) absorb the ordinary case: a method renamed
   between versions. The cost of tolerance is that a broken hook degrades into a missing
   feature instead of a crash, so each tolerant injection says in a comment what its failure
   mode is.

The six core mixins and what they own:

| mixin | target | job |
| --- | --- | --- |
| `MinecraftMixin` | `Minecraft` | frame boundary, window hooks, `resetWindow` for the HUD |
| `GameRendererMixin` | `GameRenderer` | render entry, lightmap hand-off, F3 header tag |
| `LevelRendererMixin` | `LevelRenderer` | dirty-section counting, full-rebuild co-ordination |
| `LightTextureMixin` | `LightTexture` | the 256-entry lightmap, in the shape that version has |
| `GuiMixin` | `Gui` | HUD overlay, on the `GuiGraphics`/`NativeImage` split the version provides |
| `OptionsScreenMixin` | `OptionsScreen` | replaces the vanilla video settings with `AetheriumVideoOptionsScreen` |

Each body is two lines: read state, call one `ClientHooks` method. Every decision that
could be wrong on another version therefore lives in a normal Java file a delta can edit.

## Rendering

`render/backend/` probes the context once (`BackendProber`) into an immutable
`BackendCapabilities`, and `BackendSelector` turns `(config choice, capabilities)` into a
`RenderBackend` under hard gates that no scoring may override:

```
GL46_DSA    needs GL >= 4.6, DSA entry points, compute
VULKAN_13   needs a Vulkan device the probe actually opened
GL_CORE     needs GL >= 3.3
GL_LEGACY   needs GL >= 2.0
COMPATIBILITY always allowed; the answer whenever anything above is refused
```

Scoring is a per-vendor/per-source penalty plus the preference order (DSA > Vulkan > core >
legacy). A *translated* GL source (Zink, ANGLE, gl4es, VirGL) loses `supportsPersistentMapping`
deliberately: a driver that reports 4.6 numbers it does not implement must not be handed the
persistent-arena path, because the stall shows up as multi-hundred-millisecond frames.
An explicit user choice that fails a gate is refused with a logged warning and re-resolved
automatically; the GUI then says which backend it settled on.

`render/gl/` holds the device (`GlDevice`), the raw entry-point routing (`GlProcs`, which
also owns the host-map question of which LWJGL class carries which symbol), the persistent
mapped arena with triple buffering (`GlPersistentArena`), indirect batching with
`glMultiDrawElementsIndirectCount` (`GlIndirectBatch`), the async compiler
(`GlAsyncShaderCompiler`) and its on-disk program-binary cache (`GlProgramCache`).
`render/hzb/HierarchicalDepthBuffer` is the compute-driven occlusion pyramid;
`render/mesh/ChunkMeshScheduler` is the async, distance-prioritised mesher, and
`render/mesh/MeshCounters` is the vanilla-side accounting that lets the HUD print `built`
against `dirty` so a two-renderer overlap is visible instead of anecdotal.

Aetherium has no shader-mod integration: a compile-time dependency on a shader mod would make
every version of Aetherium depend on every version of that mod, so there is none.

## Configuration

`AetheriumConfig` declares 58 `ConfigValue`s in seven groups. A `ConfigValue` owns its key,
default, bounds, serialiser, parser, translation key and two lifecycle flags
(`requiresWorldReload`, `requiresRendererRestart`). The file format is `CONFIG_SCHEMA.json`
generated from the code, and `<game>/aetherium.json` is:

```json
{ "version": 3, "aetherium": { "general.enabled": true, "performance.backend": "gl46_dsa" } }
```

Dotted keys, grouped on the first dot by the writer and flattened back by the reader. Values
that parse but are out of range are clamped (a hand-edited `"fps": 300` becomes 260, not the
default); values that do not parse are ignored with a warning. Unknown keys are kept and
rewritten, which is what lets a user downgrade without losing settings; keys the file lacks are
reported as "new options". A file that cannot be parsed is copied to `aetherium.json.bad` and
defaults are used — the original is left alone, because destroying a hand-edit is
unforgivable. Writes go to `.tmp` then an atomic rename, with a documented non-atomic fallback
for FUSE/sdcardfs, and are coalesced to one per 500 ms on the flush path.

## Threading

| thread | owns | rule |
| --- | --- | --- |
| render | `GlDevice`, GL calls, `FrameStats`, config listeners | the only thread that may touch a GL object; `RenderSafety` asserts the frame boundary |
| mesh workers (`meshWorkers` in config, `NamedThreadFactory`-created) | section build tasks | never allocate a GL object; hand buffers to the render thread through the arena |
| async shader compile | `GlAsyncShaderCompiler` | its own context share; results published by a queue the render thread drains |
| flush path | `ConfigStore.requestSave` coalescing | one write per 500 ms; `requestSave()` may be called from any thread including render |

Every shared field in the codebase carries a comment naming the lock, queue or volatile
that makes it safe. `ConfigValue`'s listeners are a `CopyOnWriteArrayList` on purpose: the GUI
adds listeners on the render thread while workers observe changes, and a linked option writing
another option mid-notification must not corrupt the loop (`ConfigValueTest` asserts that
re-entrant case).

## Failure policy

Startup failures are *loud and narrow*: the conflict scanner can put the mod into
`State.INCOMPATIBLE`, an unsupported GPU refuses the fast backends, and
`advanced.experimental_full_renderer = true` is refused and downgraded to `SHADOW` with an
error line (see "Honest status" below). Every one of those is visible in the GUI rather than
only in a log file. Per-frame failures are *quiet and bounded*: a hook that
throws is logged at warn once and the feature is disabled for the session, never retried per
frame. A benchmark or HUD readout that cannot be measured prints `stale` — the last thing this
project will do is show a plausible number it did not measure.

## Honest status

The runtime error message in `Aetherium.initialize` points here, so this section is the
authority on what the shipped states mean.

| state | entered when | what runs |
| --- | --- | --- |
| `ACTIVE` | `general.enabled = true` and no HARD conflict | mixins applied, backend resolved, dynamic lights, gamma, mesh scheduler, batching |
| `SHADOW` | `general.enabled = false`, **or** the full-renderer switch was refused, **or** the config toggle is flipped live | the vanilla render path with Aetherium measuring and applying lightmap/gamma only; `FrameStats` still records, which is why Compatibility mode is not free |
| `INCOMPATIBLE` | a `Severity.HARD` conflict (Sodium, Embeddium, Rubidium, Magnesium, Radium, VulkanMod, OptiFine/OptiFabric) | nothing that touches rendering; the mixin plugin refuses the render classes |
| `OFF` / `CLOSED` | before initialisation / after shutdown | nothing |

`advanced.experimental_full_renderer` is the switch that would route world geometry through
Aetherium instead of the vanilla mesher. **It is not shipped.** Setting it to `true` produces
an error in the log, an automatic fallback to `SHADOW`, and a GUI row that says NOT SHIPPED
rather than a checkbox that silently does nothing. The reason is not caution for its own
sake: replacing section geometry is the one part of this project whose correctness cannot be
established without a GPU and a running game, and a half-working mesher is a mod that renders
nothing on someone else's driver. The enum-free boolean exists so the config schema does not
have to change when it lands.

Everything else described in this document is implemented in this tree. What is *not*
implemented is the Vulkan chunk renderer (`VULKAN_13` probes, opens a device, and defers
world drawing to the GL path), and per-version source trees for the 32 port rows - those are
patches by design, described next.

## Porting

`tools/porting_pins.json` is the single source of truth for 33 rows; `tools/gen_deltas.py`
generates `deltas/<version>/{changes.patch,README.md,mixins.json,build.gradle.kts}` and `PORTING_MATRIX.md`
from it, and `--verify` applies every patch to the current tree. A delta may only change pins,
the plugin's version table, and a mixin's target-name lists. It may never restructure a class
or fix a logic bug — those belong in `common/` where all 33 rows inherit them. The mechanical
rule and the reviewer checklist are in [../CONTRIBUTING.md](../CONTRIBUTING.md).
