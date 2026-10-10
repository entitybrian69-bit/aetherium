# Glossary

Every term this project uses in a code comment, a log line or a document, with the file in
this repository where the thing actually lives. Where a term is loaded with a claim (a
number, a promise), the entry says who is allowed to make it.

## Rendering and OpenGL

**DSA — direct state access.** Binding-free GL entry points (`glNamedBufferData`,
`glMapNamedBufferRange`, `glClearNamedBufferData`): you address an object instead of pushing
it through a bind slot. Removes the bind-and-restore traffic that dominates a chunk uploader.
`render/backend/RenderBackend.GL46_DSA`, gated in `BackendCapabilities.supportsDirectStateAccess()`.

**GL core profile vs compatibility vs legacy.** Core = no fixed-function pipeline, all state
explicit; legacy = the 2.x path with client-side arrays and immediate mode. Aetherium's two
fallbacks are `GL_CORE` (≥ 3.3) and `GL_LEGACY` (≥ 2.0).

**Persistent mapped buffer.** A buffer mapped once (`GL_MAP_PERSISTENT_BIT |
GL_MAP_COHERENT_BIT`) and written for the lifetime of the context, instead of
`glBufferSubData` per frame. Requires a fence to avoid overwriting data the GPU still reads —
hence the ring. `render/gl/GlPersistentArena`.

**Triple buffering (in this project's sense).** Three regions in that arena so the CPU can
fill region *n+1* while the GPU reads *n*, rather than "three swap-chain images", which is
what the phrase means in a Vulkan tutorial. The swap chain itself is LWJGL/GLFW's.

**Fence / client wait.** `glMemoryBarrier` plus a sync object; `getMaxServerWaitTimeoutMs`
in `BackendCapabilities` is why a timeout of 0 (unsupported) disables the persistent path
instead of deadlocking a frame.

**MDI — multi-draw indirect.** One call submits many draws. `glMultiDrawElementsIndirectCount`
adds a GPU-written draw count, so a culled frame issues zero draws without a CPU readback.
`render/gl/GlIndirectBatch`; feasibility in `supportsIndirectCount()` /
`supportsIndirectParameters()`. The 6-argument LWJGL signature has `stride` last — the
reason this is written down is that getting it wrong compiles fine on some bindings.

**HZB — hierarchical z-buffer.** A mip chain of max-depths built by a compute pass; a section
is rejected by sampling one texel instead of testing geometry. `render/hzb/HierarchicalDepthBuffer`.
"Occlusion culling" here always means this, never `glQueryCounter`-style occlusion queries,
which stall.

**Program binary.** `glGetProgramBinary` / `glProgramBinary`: driver-compiled machine code,
cached on disk so a reload does not recompile 30 shader variants.
`render/gl/GlProgramCache`; async compilation in `render/gl/GlAsyncShaderCompiler`.

**Section vs chunk.** A chunk is 16×16 blocks wide and 16–384 tall; a *section* is 16×16×16
and is the unit that gets meshed, uploaded and culled. `ChunkMeshScheduler` queues sections,
keyed by `MathUtil.sectionKey`.

**Lightmap.** The 16×16 (`LIGHTMAP_SIZE = 256`) texture holding sky × block light colour.
`gamma/GammaApplier` and `gamma/LightmapWriter` write it; `LightTextureMixin` hands it over.
Its index layout is `(sky << 4) | block`, and the shape of the vanilla array changed at
1.20.2 (`int[] pixels` replacing `NativeImage`), which is why that version boundary is in
`tools/porting_pins.json`.

**Packed light.** A single int holding block light in bits 4–19 and sky light in bits 20–35
of the vertex attribute. `GammaApplier.applyToVertexLight` raises its block component.

## Mixin and loaders

**Mixin.** A declarative patch to a class at load time. `@Inject` inserts a method call at a
point (`@At("HEAD")`, `"TAIL"`, `"INVOKE"`); `@ModifyVariable` rewrites a local;
`@Redirect` replaces a call. `com.aetherium.mixin.core`.

**Target, selector, `method = {"a", "b"}`.** The method a mixin attaches to. Listing several
names means "whichever of these exists" — the mechanism the porting system uses to absorb a
rename. A name that matches nothing, with `require = 0`, is a skipped hook, not an error.

**`require` / `expect`.** How many matches must be found (require) or are tolerated (expect).
`aetherium-common.mixins.json` sets both to 0 by default, and
`advanced.strict_mixins = true` restores `require = 1` so a delta that no longer matches fails
the launch loudly instead of silently dropping a feature.

**`IMixinConfigPlugin`.** The hook that can veto a mixin before transformation:
`shouldApplyMixin`, `acceptTarget`, `preApply`. `mixin/AetheriumMixinPlugin` uses it for
version gating and for standing down on a HARD conflict.

**Refmap.** The file mapping mixin selectors to obfuscated names, built by the annotation
processor. Remapped by Loom at `remapJar` time.

**Remap / reobf.** Translating compiled classes from named (Mojang/Yarn) to intermediary or
official runtime identifiers. `fabric-loom`'s `remapJar`; the `fabric/` and `neoforge/`
modules exist mostly to do this differently.

**Mojang mappings / intermediary / Yarn / Parchment.** The official obfuscation map; Fabric's
stable runtime names; the community names; Javadoc annotations on top of Mojang mappings.
`common/` compiles against Mojang mappings so both loaders can remap from one source set.

**MultiLoader.** One shared `common/` source set plus thin per-loader modules — the layout this
repository uses (the same shape as the MultiLoaderTemplate, with no loader code in `common`).

**MixinExtras.** Provides `@ModifyExpressionValue` and friends. `compileOnly` +
annotation processor in `common/build.gradle.kts`, shadowed into the platform jars.

**Mod Menu.** A Fabric GUI registry. Aetherium does **not** depend on it: the options screen is
reached by `OptionsScreenMixin` plus a fallback button, and `fabric.mod.json` declares no
`modmenu` entry point.

## Loader and platform API

**`PlatformAdapter`.** The seven-method SPI (`platformName`, `gameDirectory`,
`configDirectory`, `isModLoaded`, `getModVersion`, `minecraftVersion`, `isClient`) plus two
defaults. The only way engine code asks a question about the loader.

**`ClientHooks`.** The single class the mixins are allowed to call. Its rule is written in its
own Javadoc: a mixin body is two lines, so version-dependent decisions never live in bytecode
patches.

**Iris v0 API.** The shader mod's public interface (`IrisApiV0Impl.INSTANCE`,
`isShaderPackInUse`, `openMainIrisScreenObj`, …). Aetherium reads it by reflection only —
`shader/IrisBridge` — so no Iris jar is needed to build.

**F3 / debug overlay.** The vanilla diagnostics screen; `GuiMixin` appends the
`[Aetherium/GL46]` tag to its header line when `general.hud.backend_tag` is on.

## Configuration

**`ConfigValue`.** One option: key, default, bounds, parser, listeners, dirty and
pending-apply flags. Clamping on read (`accept`) is deliberate; see its Javadoc.

**Coalesced save.** Many `requestSave()` calls collapse into at most one disk write per
500 ms, so a slider drag does not thrash the flash storage of a phone.

**`.bad` quarantine.** An unparseable `aetherium.json` is copied to `aetherium.json.bad` and
defaults are used; the user's file is never deleted.

**`CONFIG_SCHEMA.json`.** Generated JSON Schema (draft 2020-12) for the config file, produced
by `tools/gen_resources.py` from `AetheriumConfig`. Never hand-edited;
`./gradlew checkTree` fails if it is stale.

## Porting

**Reference version.** 1.21.1 — the only version with a complete source tree. Everything else
is derived from it.

**Delta.** `deltas/<version>/changes.patch` plus `README.md` (why each hunk is what it is)
and `README.md`. A delta may change pins, the plugin's version table and a mixin's target-name
lists. Nothing else.

**Pins.** Exact versions in `tools/porting_pins.json` (and mirrored into
`gradle.properties`): Minecraft, loader, Loom, mappings, Java, NeoForge. A row with an
unverified pin is graded `derived` or `unverified`, never silently promoted to `documented`.

**`PORTING_MATRIX.md`.** Generated table of all 33 rows. Generated from the pins file, so a
hand-edit is lost the next time anyone runs `tools/gen_deltas.py`.

**Status grade.** `reference` (built for), `documented` (facts verified against real sources),
`derived` (computed from a rule, e.g. "GuiGraphics from 1.20.2"), `unverified` (plausible, not
checked). The grade is per row and per field, and `tools/check.py` fails on a matrix that does
not match its input.

**`[UNVERIFIED: …]`.** The marker used wherever this repository does not know something. It
names the exact fact that was not verified and what the consequence of being wrong is.
`tools/check.py` counts them, and [../VERIFICATION.md](../VERIFICATION.md) lists every one of
them with its file and line. A mark is not a placeholder for unfinished work - the code beneath
it runs, and its failure mode is named in the mark.

## Measurement

**Frame time, not FPS.** A 16 ms frame is 62.5 fps. The engine stores nanoseconds per frame
and derives everything else, so an average never hides a spike.

**p99 / p99.9.** The 99th and 99.9th percentile of the frame-time histogram — the numbers that
describe a stutter. `FrameStats.percentile`, exact to the 1 µs bucket.

**Spike.** A frame over 50 ms or 100 ms, counted separately from the histogram because the
histogram's ring window forgets old frames.

**CPU micro-benchmark.** What `tools/benchmark.sh` runs by default: real functions timed in a
JVM, with no GPU, no driver and no swapchain in sight. It says nothing about frame rate, and
`BENCHMARK.md` is the only place its output may be quoted — with this sentence attached.

**Ratio.** `X×` claims. No code path in this repository can produce one, and none is made
about Sodium, Embeddium, OptiFine or vanilla.
