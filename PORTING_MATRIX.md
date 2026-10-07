# Porting matrix

Minecraft **1.21.1** is the reference implementation: the source in `common/` is
written against its real mappings, and the other 32 rows are *this same source*
plus a mechanical delta in `deltas/<version>/`.

This file is generated - edit `tools/porting_pins.json` and run
`python3 tools/gen_deltas.py --all && python3 tools/gen_deltas.py --matrix`.
Every statement here is graded:

| status | meaning |
| --- | --- |
| `reference` | the version the source is written and built against |
| `documented` | pins read from an upstream project that ships this version |
| `derived` | inferred from the renames on either side; verify before shipping |
| `unverified` | plausible, not checked; the delta ships the recipe to check it |

No row claims a measured frame-rate result, and no row claims to have been run
unless it is the reference row. Anything else would be a fabricated deliverable.

| Minecraft | Java | Fabric loader | NeoForge | GuiGraphics | lightmap | options hijack | status | delta |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| [1.16.5](deltas/1.16.5/) | 17 | 0.14.24 | n/a | no (PoseStack) | NativeImage lightmap | `disabled (fallback button only)` | documented | [patch](deltas/1.16.5/changes.patch) · [derivation](deltas/1.16.5/derivation.md) · [notes](deltas/1.16.5/notes.md) |
| [1.17](deltas/1.17/) | 17 | 0.14.24 | n/a | no (PoseStack) | NativeImage lightmap | `disabled (fallback button only)` | unverified | [patch](deltas/1.17/changes.patch) · [derivation](deltas/1.17/derivation.md) · [notes](deltas/1.17/notes.md) |
| [1.17.1](deltas/1.17.1/) | 17 | 0.14.24 | n/a | no (PoseStack) | NativeImage lightmap | `disabled (fallback button only)` | unverified | [patch](deltas/1.17.1/changes.patch) · [derivation](deltas/1.17.1/derivation.md) · [notes](deltas/1.17.1/notes.md) |
| [1.18](deltas/1.18/) | 17 | 0.14.24 | n/a | no (PoseStack) | NativeImage lightmap | `[1.17.4,)` | derived | [patch](deltas/1.18/changes.patch) · [derivation](deltas/1.18/derivation.md) · [notes](deltas/1.18/notes.md) |
| [1.18.1](deltas/1.18.1/) | 17 | 0.14.24 | n/a | no (PoseStack) | NativeImage lightmap | `[1.17.4,)` | derived | [patch](deltas/1.18.1/changes.patch) · [derivation](deltas/1.18.1/derivation.md) · [notes](deltas/1.18.1/notes.md) |
| [1.18.2](deltas/1.18.2/) | 17 | 0.14.24 | n/a | no (PoseStack) | NativeImage lightmap | `[1.17.4,)` | derived | [patch](deltas/1.18.2/changes.patch) · [derivation](deltas/1.18.2/derivation.md) · [notes](deltas/1.18.2/notes.md) |
| [1.19](deltas/1.19/) | 17 | 0.14.24 | n/a | no (PoseStack) | NativeImage lightmap | `[1.17.4,)` | derived | [patch](deltas/1.19/changes.patch) · [derivation](deltas/1.19/derivation.md) · [notes](deltas/1.19/notes.md) |
| [1.19.1](deltas/1.19.1/) | 17 | 0.14.24 | n/a | no (PoseStack) | NativeImage lightmap | `[1.17.4,)` | derived | [patch](deltas/1.19.1/changes.patch) · [derivation](deltas/1.19.1/derivation.md) · [notes](deltas/1.19.1/notes.md) |
| [1.19.2](deltas/1.19.2/) | 17 | 0.14.24 | n/a | no (PoseStack) | NativeImage lightmap | `[1.17.4,)` | derived | [patch](deltas/1.19.2/changes.patch) · [derivation](deltas/1.19.2/derivation.md) · [notes](deltas/1.19.2/notes.md) |
| [1.19.3](deltas/1.19.3/) | 17 | 0.14.24 | n/a | no (PoseStack) | NativeImage lightmap | `[1.17.4,)` | derived | [patch](deltas/1.19.3/changes.patch) · [derivation](deltas/1.19.3/derivation.md) · [notes](deltas/1.19.3/notes.md) |
| [1.19.4](deltas/1.19.4/) | 17 | 0.14.24 | n/a | no (PoseStack) | NativeImage lightmap | `[1.17.4,)` | derived | [patch](deltas/1.19.4/changes.patch) · [derivation](deltas/1.19.4/derivation.md) · [notes](deltas/1.19.4/notes.md) |
| [1.20](deltas/1.20/) | 17 | 0.14.24 | n/a | no (PoseStack) | NativeImage lightmap | `[1.17.4,)` | derived | [patch](deltas/1.20/changes.patch) · [derivation](deltas/1.20/derivation.md) · [notes](deltas/1.20/notes.md) |
| [1.20.1](deltas/1.20.1/) | 17 | 0.14.24 | n/a | no (PoseStack) | NativeImage lightmap | `[1.17.4,)` | documented | [patch](deltas/1.20.1/changes.patch) · [derivation](deltas/1.20.1/derivation.md) · [notes](deltas/1.20.1/notes.md) |
| [1.20.2](deltas/1.20.2/) | 17 | 0.15.11 | 20.2.86 | yes | int[] pixels | `[1.17.4,)` | derived | [patch](deltas/1.20.2/changes.patch) · [derivation](deltas/1.20.2/derivation.md) · [notes](deltas/1.20.2/notes.md) |
| [1.20.3](deltas/1.20.3/) | 17 | 0.15.11 | 20.3.11 | yes | int[] pixels | `[1.17.4,)` | derived | [patch](deltas/1.20.3/changes.patch) · [derivation](deltas/1.20.3/derivation.md) · [notes](deltas/1.20.3/notes.md) |
| [1.20.4](deltas/1.20.4/) | 17 | 0.15.11 | 20.4.97 | yes | int[] pixels | `[1.17.4,)` | derived | [patch](deltas/1.20.4/changes.patch) · [derivation](deltas/1.20.4/derivation.md) · [notes](deltas/1.20.4/notes.md) |
| [1.20.5](deltas/1.20.5/) | 21 | 0.16.9 | 20.5.42 | yes | int[] pixels | `[1.17.4,)` | derived | [patch](deltas/1.20.5/changes.patch) · [derivation](deltas/1.20.5/derivation.md) · [notes](deltas/1.20.5/notes.md) |
| [1.20.6](deltas/1.20.6/) | 21 | 0.16.9 | 20.6.121 | yes | int[] pixels | `[1.17.4,)` | documented | [patch](deltas/1.20.6/changes.patch) · [derivation](deltas/1.20.6/derivation.md) · [notes](deltas/1.20.6/notes.md) |
| [1.21](deltas/1.21/) | 21 | 0.16.9 | 21.0.119 | yes | int[] pixels | `[1.17.4,)` | derived | [patch](deltas/1.21/changes.patch) · [derivation](deltas/1.21/derivation.md) · [notes](deltas/1.21/notes.md) |
| **1.21.1** | 21 | 0.16.9 | 21.1.77 | yes | int[] pixels | `[1.17.4,)` | reference | the tree itself - [`common/`](common/src/main/java/com/aetherium), [`gradle.properties`](gradle.properties) |
| [1.21.2](deltas/1.21.2/) | 21 | 0.16.14 | 21.2.77 | yes | int[] pixels | `[1.17.4,)` | derived | [patch](deltas/1.21.2/changes.patch) · [derivation](deltas/1.21.2/derivation.md) · [notes](deltas/1.21.2/notes.md) |
| [1.21.3](deltas/1.21.3/) | 21 | 0.16.14 | 21.3.44 | yes | int[] pixels | `[1.17.4,)` | derived | [patch](deltas/1.21.3/changes.patch) · [derivation](deltas/1.21.3/derivation.md) · [notes](deltas/1.21.3/notes.md) |
| [1.21.4](deltas/1.21.4/) | 21 | 0.16.14 | 21.4.60 | yes | int[] pixels | `[1.17.4,)` | documented | [patch](deltas/1.21.4/changes.patch) · [derivation](deltas/1.21.4/derivation.md) · [notes](deltas/1.21.4/notes.md) |
| [1.21.5](deltas/1.21.5/) | 21 | 0.16.14 | 21.5.81 | yes | int[] pixels | `[1.17.4,)` | derived | [patch](deltas/1.21.5/changes.patch) · [derivation](deltas/1.21.5/derivation.md) · [notes](deltas/1.21.5/notes.md) |
| [1.21.6](deltas/1.21.6/) | 21 | 0.16.14 | 21.6.22 | yes | int[] pixels | `[1.17.4,)` | derived | [patch](deltas/1.21.6/changes.patch) · [derivation](deltas/1.21.6/derivation.md) · [notes](deltas/1.21.6/notes.md) |
| [1.21.7](deltas/1.21.7/) | 21 | 0.16.14 | 21.7.30 | yes | int[] pixels | `[1.17.4,)` | derived | [patch](deltas/1.21.7/changes.patch) · [derivation](deltas/1.21.7/derivation.md) · [notes](deltas/1.21.7/notes.md) |
| [1.21.8](deltas/1.21.8/) | 21 | 0.16.14 | 21.8.22 | yes | int[] pixels | `[1.17.4,)` | derived | [patch](deltas/1.21.8/changes.patch) · [derivation](deltas/1.21.8/derivation.md) · [notes](deltas/1.21.8/notes.md) |
| [1.21.9](deltas/1.21.9/) | 21 | 0.16.14 | 21.9.100 | yes | int[] pixels | `[1.17.4,)` | derived | [patch](deltas/1.21.9/changes.patch) · [derivation](deltas/1.21.9/derivation.md) · [notes](deltas/1.21.9/notes.md) |
| [1.21.10](deltas/1.21.10/) | 21 | 0.16.14 | 21.10.100 | yes | int[] pixels | `[1.17.4,)` | derived | [patch](deltas/1.21.10/changes.patch) · [derivation](deltas/1.21.10/derivation.md) · [notes](deltas/1.21.10/notes.md) |
| [1.21.11](deltas/1.21.11/) | 21 | 0.16.14 | 21.11.100 | yes | int[] pixels | `[1.17.4,)` | derived | [patch](deltas/1.21.11/changes.patch) · [derivation](deltas/1.21.11/derivation.md) · [notes](deltas/1.21.11/notes.md) |
| [26.1](deltas/26.1/) | 25 | 0.14.24 | 26.1.100 | yes | int[] pixels | `[1.17.4,)` | derived | [patch](deltas/26.1/changes.patch) · [derivation](deltas/26.1/derivation.md) · [notes](deltas/26.1/notes.md) |
| [26.2](deltas/26.2/) | 25 | 0.14.24 | 26.2.100 | yes | int[] pixels | `[1.17.4,)` | derived | [patch](deltas/26.2/changes.patch) · [derivation](deltas/26.2/derivation.md) · [notes](deltas/26.2/notes.md) |
| [26.3](deltas/26.3/) | 25 | 0.14.24 | 26.3.100 | yes | int[] pixels | `[1.17.4,)` | documented | [patch](deltas/26.3/changes.patch) · [derivation](deltas/26.3/derivation.md) · [notes](deltas/26.3/notes.md) |

33 rows: 1 reference + 32 deltas.

## All versions

Every row, linked. A delta directory holds three files: `changes.patch` (what to
apply), `derivation.md` (why each hunk is what it is, and the upstream file it was
read from) and `notes.md` (what to check on that version once a JDK exists).
No row links a download, because this repository publishes no jars - see the
[README](README.md#honest-status) for what is and is not shipped.

### 1.16 - 1.18 (Java 8/16/17, no GuiGraphics)

- [1.16.5](deltas/1.16.5/) - Java 17, [patch](deltas/1.16.5/changes.patch), [derivation](deltas/1.16.5/derivation.md), [notes](deltas/1.16.5/notes.md) - `documented`
- [1.17](deltas/1.17/) - Java 17, [patch](deltas/1.17/changes.patch), [derivation](deltas/1.17/derivation.md), [notes](deltas/1.17/notes.md) - `unverified`
- [1.17.1](deltas/1.17.1/) - Java 17, [patch](deltas/1.17.1/changes.patch), [derivation](deltas/1.17.1/derivation.md), [notes](deltas/1.17.1/notes.md) - `unverified`
- [1.18](deltas/1.18/) - Java 17, [patch](deltas/1.18/changes.patch), [derivation](deltas/1.18/derivation.md), [notes](deltas/1.18/notes.md) - `derived`
- [1.18.1](deltas/1.18.1/) - Java 17, [patch](deltas/1.18.1/changes.patch), [derivation](deltas/1.18.1/derivation.md), [notes](deltas/1.18.1/notes.md) - `derived`
- [1.18.2](deltas/1.18.2/) - Java 17, [patch](deltas/1.18.2/changes.patch), [derivation](deltas/1.18.2/derivation.md), [notes](deltas/1.18.2/notes.md) - `derived`

### 1.19 - 1.20 (the GuiGraphics and lightmap changes)

- [1.19](deltas/1.19/) - Java 17, [patch](deltas/1.19/changes.patch), [derivation](deltas/1.19/derivation.md), [notes](deltas/1.19/notes.md) - `derived`
- [1.19.1](deltas/1.19.1/) - Java 17, [patch](deltas/1.19.1/changes.patch), [derivation](deltas/1.19.1/derivation.md), [notes](deltas/1.19.1/notes.md) - `derived`
- [1.19.2](deltas/1.19.2/) - Java 17, [patch](deltas/1.19.2/changes.patch), [derivation](deltas/1.19.2/derivation.md), [notes](deltas/1.19.2/notes.md) - `derived`
- [1.19.3](deltas/1.19.3/) - Java 17, [patch](deltas/1.19.3/changes.patch), [derivation](deltas/1.19.3/derivation.md), [notes](deltas/1.19.3/notes.md) - `derived`
- [1.19.4](deltas/1.19.4/) - Java 17, [patch](deltas/1.19.4/changes.patch), [derivation](deltas/1.19.4/derivation.md), [notes](deltas/1.19.4/notes.md) - `derived`
- [1.20](deltas/1.20/) - Java 17, [patch](deltas/1.20/changes.patch), [derivation](deltas/1.20/derivation.md), [notes](deltas/1.20/notes.md) - `derived`
- [1.20.1](deltas/1.20.1/) - Java 17, [patch](deltas/1.20.1/changes.patch), [derivation](deltas/1.20.1/derivation.md), [notes](deltas/1.20.1/notes.md) - `documented`
- [1.20.2](deltas/1.20.2/) - Java 17, [patch](deltas/1.20.2/changes.patch), [derivation](deltas/1.20.2/derivation.md), [notes](deltas/1.20.2/notes.md) - `derived`
- [1.20.3](deltas/1.20.3/) - Java 17, [patch](deltas/1.20.3/changes.patch), [derivation](deltas/1.20.3/derivation.md), [notes](deltas/1.20.3/notes.md) - `derived`
- [1.20.4](deltas/1.20.4/) - Java 17, [patch](deltas/1.20.4/changes.patch), [derivation](deltas/1.20.4/derivation.md), [notes](deltas/1.20.4/notes.md) - `derived`
- [1.20.5](deltas/1.20.5/) - Java 21, [patch](deltas/1.20.5/changes.patch), [derivation](deltas/1.20.5/derivation.md), [notes](deltas/1.20.5/notes.md) - `derived`
- [1.20.6](deltas/1.20.6/) - Java 21, [patch](deltas/1.20.6/changes.patch), [derivation](deltas/1.20.6/derivation.md), [notes](deltas/1.20.6/notes.md) - `documented`

### 1.21.x (the reference line and its successors)

- [1.21](deltas/1.21/) - Java 21, [patch](deltas/1.21/changes.patch), [derivation](deltas/1.21/derivation.md), [notes](deltas/1.21/notes.md) - `derived`
- [1.21.2](deltas/1.21.2/) - Java 21, [patch](deltas/1.21.2/changes.patch), [derivation](deltas/1.21.2/derivation.md), [notes](deltas/1.21.2/notes.md) - `derived`
- [1.21.3](deltas/1.21.3/) - Java 21, [patch](deltas/1.21.3/changes.patch), [derivation](deltas/1.21.3/derivation.md), [notes](deltas/1.21.3/notes.md) - `derived`
- [1.21.4](deltas/1.21.4/) - Java 21, [patch](deltas/1.21.4/changes.patch), [derivation](deltas/1.21.4/derivation.md), [notes](deltas/1.21.4/notes.md) - `documented`
- [1.21.5](deltas/1.21.5/) - Java 21, [patch](deltas/1.21.5/changes.patch), [derivation](deltas/1.21.5/derivation.md), [notes](deltas/1.21.5/notes.md) - `derived`
- [1.21.6](deltas/1.21.6/) - Java 21, [patch](deltas/1.21.6/changes.patch), [derivation](deltas/1.21.6/derivation.md), [notes](deltas/1.21.6/notes.md) - `derived`
- [1.21.7](deltas/1.21.7/) - Java 21, [patch](deltas/1.21.7/changes.patch), [derivation](deltas/1.21.7/derivation.md), [notes](deltas/1.21.7/notes.md) - `derived`
- [1.21.8](deltas/1.21.8/) - Java 21, [patch](deltas/1.21.8/changes.patch), [derivation](deltas/1.21.8/derivation.md), [notes](deltas/1.21.8/notes.md) - `derived`
- [1.21.9](deltas/1.21.9/) - Java 21, [patch](deltas/1.21.9/changes.patch), [derivation](deltas/1.21.9/derivation.md), [notes](deltas/1.21.9/notes.md) - `derived`
- [1.21.10](deltas/1.21.10/) - Java 21, [patch](deltas/1.21.10/changes.patch), [derivation](deltas/1.21.10/derivation.md), [notes](deltas/1.21.10/notes.md) - `derived`
- [1.21.11](deltas/1.21.11/) - Java 21, [patch](deltas/1.21.11/changes.patch), [derivation](deltas/1.21.11/derivation.md), [notes](deltas/1.21.11/notes.md) - `derived`

### Date-based ids (26.x)

- [26.1](deltas/26.1/) - Java 25, [patch](deltas/26.1/changes.patch), [derivation](deltas/26.1/derivation.md), [notes](deltas/26.1/notes.md) - `derived`
- [26.2](deltas/26.2/) - Java 25, [patch](deltas/26.2/changes.patch), [derivation](deltas/26.2/derivation.md), [notes](deltas/26.2/notes.md) - `derived`
- [26.3](deltas/26.3/) - Java 25, [patch](deltas/26.3/changes.patch), [derivation](deltas/26.3/derivation.md), [notes](deltas/26.3/notes.md) - `documented`

### 1.21.1 - the reference version

- no delta: this tree *is* the 1.21.1 build - [`gradle.properties`](gradle.properties), [`common/`](common/src/main/java/com/aetherium)
- Java 21, Fabric loader 0.16.9, Loom 1.16.1, NeoForge 21.1.77 - provenance in [docs/BUILD_PINS.md](docs/BUILD_PINS.md)

## What a delta does and does not contain

`deltas/<version>/changes.patch` may change only:

1. build pins (`minecraft_version`, `java_version`, `neoforge_version`,
   `fabric_loader_version`, `fabric_loom_version` + the matching `loom` entry in
   `gradle/libs.versions.toml`, `enabled_platforms`);
2. `REFERENCE`-side strings in `AetheriumMixinPlugin` (the announced version and
   the per-mixin target ranges);
3. an *append* to an existing `method = { ... }` candidate list - reference names
   are never removed, so two ports merge without conflict;
4. the primary overlay descriptor in `GuiMixin` when the version predates
   `GuiGraphics`.

### Loom, and why one version serves all 33 rows

Every row pins Loom to the reference value. `gradle/wrapper/gradle-wrapper.properties`
is tree-wide (Gradle 9.4.1) and no delta rewrites it, and a Loom from the 1.2 or 1.6 era
does not run on Gradle 9 - so an era-matched Loom pin would describe a build this tree
cannot start. Loom itself is Minecraft-version-agnostic (the Minecraft artifact and the
intermediary/mojmap channel decide the version), and 1.16.1 is the only Loom version this
repository verified against upstream metadata. A port that genuinely needs a different
Loom must bump the wrapper, `gradle.properties` and `gradle/libs.versions.toml` together;
`tools/check.py` fails if the last two disagree, which is the guard against half a bump.

It never restructures a class, fixes a bug, or changes an algorithm. `
`tools/gen_deltas.py` refuses to emit anything else, and `tools/check.py` proves
every patch still applies with `git apply --check`.

## Applying one

```sh
sh tools/port.sh --list            # what is known about each row
sh tools/port.sh --dry-run 1.20.6  # apply to a copy, run the checks, throw it away
sh tools/port.sh 1.20.6            # apply to this tree
sh tools/port.sh --revert 1.20.6   # undo
```

## Per-version notes worth reading before you port

- **1.16.5** - Pre-`GuiGraphics`: `Gui#render` takes a `PoseStack` and `LightTexture` (this is what the legacy hook in GuiMixin covers).
- **1.17** - Java 16 toolchain. `LightTexture` still holds a `NativeImage`; `LevelRenderer#setSectionDirty` has the boolean overload on some mappings only - check with javap.
- **1.17.1** - Java 16 toolchain. `LightTexture` still holds a `NativeImage`; `LevelRenderer#setSectionDirty` has the boolean overload on some mappings only - check with javap.
- **1.18** - Java 17 from here. Pointed-lighting/dripstone render pipeline: `LevelRenderer` gained `compileLightUpdates`; the HZB sizing hooks do not care.
- **1.18.1** - Java 17 from here. Pointed-lighting/dripstone render pipeline: `LevelRenderer` gained `compileLightUpdates`; the HZB sizing hooks do not care.
- **1.18.2** - Java 17 from here. Pointed-lighting/dripstone render pipeline: `LevelRenderer` gained `compileLightUpdates`; the HZB sizing hooks do not care.
- **1.19** - 1.19 added the `DebugScreenOverlay` split; `showDebugScreen()` is public from 1.19.3 (the reflective probe in GuiMixin handles both).
- **1.19.1** - 1.19 added the `DebugScreenOverlay` split; `showDebugScreen()` is public from 1.19.3 (the reflective probe in GuiMixin handles both).
- **1.19.2** - 1.19 added the `DebugScreenOverlay` split; `showDebugScreen()` is public from 1.19.3 (the reflective probe in GuiMixin handles both).
- **1.19.3** - `ItemRenderer`/`BlockRenderDispatcher` rework: irrelevant to Aetherium's shadow mode, relevant to the FULL path (refused).
- **1.19.4** - `ItemRenderer`/`BlockRenderDispatcher` rework: irrelevant to Aetherium's shadow mode, relevant to the FULL path (refused).
- **1.20** - Render-layer `VertexFormat` rework begins; `RenderPipelines` exist, so a future FULL-mode port starts here rather than below.
- **1.20.1** - The last pre-`GuiGraphics` version, and the version most Android launchers still ship: this row gets the most touch testing, not the most build testing.
- **1.20.2** - `GuiGraphics` + `AbstractWidget#renderWidget` land here, and NeoForge 20.2 is the first NeoForge: the GUI mixin descriptors and the loader both change on this exact row.
- **1.20.3** - `mouseClicked`/`mouseScrolled` return `boolean` from 1.20.2; Screen overrides in AetheriumVideoOptionsScreen need no change on this row.
- **1.20.4** - `mouseClicked`/`mouseScrolled` return `boolean` from 1.20.2; Screen overrides in AetheriumVideoOptionsScreen need no change on this row.
- **1.20.5** - Java 21 from here (component textures + item model rework). `LightTexture#tick` replaces `updateTick` on 1.20.5+ - the candidate list already carries both.
- **1.20.6** - Java 21 from here (component textures + item model rework). `LightTexture#tick` replaces `updateTick` on 1.20.5+ - the candidate list already carries both.
- **1.21** - `Screen#onClose` still returns void; the special-model render pipeline (`RenderPipeline` with explicit vertex sort) means FULL mode must build its pipeline differently from 1.20.6.
- **1.21.1** - Reference row: everything in common/ is written and verified against this version's mojmap names.
- **1.21.2** - Full `RenderPipeline`/`ShaderInstance` rework plus `GLSL` version bump to 150->500-style pipelines; the direct-GL path in GlProcs is unaffected, but a FULL-mode port must re-derive the vertex format.
- **1.21.3** - Full `RenderPipeline`/`ShaderInstance` rework plus `GLSL` version bump to 150->500-style pipelines; the direct-GL path in GlProcs is unaffected, but a FULL-mode port must re-derive the vertex format.
- **1.21.4** - `Blaze3D` layers moved; the async-mesher `SectionMesh` layout is still valid.
- **1.21.5** - Item model / block model data-driven rework: no mixin in this tree touches it, so the delta is pins only.
- **1.21.6** - `ChunkRendererRegion` and light-engine internals move; `LevelRenderer` candidate names must be re-read (see deltas/<v>/derivation.md). Embeddium's last supported branch is 21.4, so no neighbour mod confirms these rows.
- **1.21.7** - `ChunkRendererRegion` and light-engine internals move; `LevelRenderer` candidate names must be re-read (see deltas/<v>/derivation.md). Embeddium's last supported branch is 21.4, so no neighbour mod confirms these rows.
- **1.21.8** - Sparse: 1.21.9 introduces the `26.x`-style date versioning experiment; the loader floor and the mixin target names both need a fresh read.
- **1.21.9** - Sparse: 1.21.9 introduces the `26.x`-style date versioning experiment; the loader floor and the mixin target names both need a fresh read.
- **1.21.10** - Sodium has no `stable` branch for 1.21.10 at the time of writing, so the conflict table's version hints for these rows are `derived`, not verified.
- **1.21.11** - Sodium has no `stable` branch for 1.21.10 at the time of writing, so the conflict table's version hints for these rows are `derived`, not verified.
- **26.1** - Real: NeoForge ships 26.x branches, and Iris/LambDynamicLights build for it. `minecraft_version` in this repo's pin table is a date-based id, so `nextMinor()` in neoforge/build.gradle.kts handles it (26.3 -> 26.4); verify the range on first build.
- **26.2** - Real: NeoForge ships 26.x branches, and Iris/LambDynamicLights build for it. `minecraft_version` in this repo's pin table is a date-based id, so `nextMinor()` in neoforge/build.gradle.kts handles it (26.3 -> 26.4); verify the range on first build.
- **26.3** - Real: NeoForge ships 26.x branches, and Iris/LambDynamicLights build for it. `minecraft_version` in this repo's pin table is a date-based id, so `nextMinor()` in neoforge/build.gradle.kts handles it (26.3 -> 26.4); verify the range on first build.

## Loader availability

NeoForge begins at 1.20.2, so rows below it are Fabric-only and the generated
delta sets `enabled_platforms=fabric` rather than shipping a jar that cannot load.
For those versions the practical alternatives are Fabric (Pojav/Zalith on Android)
or the mod's own Fabric jar under Sapphire; `docs/ARCHITECTURE.md` explains why no
legacy-Forge port exists in this repository: the mixin surface differs enough that
it would be a second codebase wearing a delta costume.

## Verification record

The 1.21.1 mixin target names in this tree were read from real sources, not memory:
`CaffeineMC/sodium @ 1.21.1/stable` (its `LightTexture`, `Gui`, `OptionsScreen` and
`LevelRenderer` mixin sets) and `neoforged/NeoForge @ 1.21.1` patch files. Where a
name could not be read, the code carries an `[UNVERIFIED: ...]` mark and a tolerant
injection - `python3 tools/check.py --report-unverified` lists them all.
