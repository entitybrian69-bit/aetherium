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

Era boundaries that were read from real upstream sources (Iris branches 1.19.4,
1.20.1, 1.20.6, 1.21.1; CaffeineMC/sodium @ 1.21.1/stable; LWJGL generated
sources; FabricMC and NeoForge tag listings) are marked VERIFIED in each delta's
README; boundaries inferred between two verified points are marked UNVERIFIED and
fail loudly (a compile error) rather than silently when wrong.

No row claims a measured frame-rate result, and no row claims to have been run
unless it is the reference row. Anything else would be a fabricated deliverable.

| Minecraft | Java | Fabric loader | NeoForge | Stack | Text | Narration | status | delta |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| [1.16.5](deltas/1.16.5/) | 8 | 0.14.24 | n/a | PoseStack | TextComponent | plain | documented | [patch](deltas/1.16.5/changes.patch) · [README](deltas/1.16.5/README.md) · [mixins.json](deltas/1.16.5/mixins.json) · [build.gradle.kts](deltas/1.16.5/build.gradle.kts) |
| [1.17](deltas/1.17/) | 16 | 0.14.24 | n/a | PoseStack | TextComponent | plain | unverified | [patch](deltas/1.17/changes.patch) · [README](deltas/1.17/README.md) · [mixins.json](deltas/1.17/mixins.json) · [build.gradle.kts](deltas/1.17/build.gradle.kts) |
| [1.17.1](deltas/1.17.1/) | 16 | 0.14.24 | n/a | PoseStack | TextComponent | plain | unverified | [patch](deltas/1.17.1/changes.patch) · [README](deltas/1.17.1/README.md) · [mixins.json](deltas/1.17.1/mixins.json) · [build.gradle.kts](deltas/1.17.1/build.gradle.kts) |
| [1.18](deltas/1.18/) | 17 | 0.14.24 | n/a | PoseStack | TextComponent | plain | derived | [patch](deltas/1.18/changes.patch) · [README](deltas/1.18/README.md) · [mixins.json](deltas/1.18/mixins.json) · [build.gradle.kts](deltas/1.18/build.gradle.kts) |
| [1.18.1](deltas/1.18.1/) | 17 | 0.14.24 | n/a | PoseStack | TextComponent | plain | derived | [patch](deltas/1.18.1/changes.patch) · [README](deltas/1.18.1/README.md) · [mixins.json](deltas/1.18.1/mixins.json) · [build.gradle.kts](deltas/1.18.1/build.gradle.kts) |
| [1.18.2](deltas/1.18.2/) | 17 | 0.14.24 | n/a | PoseStack | TextComponent | plain | derived | [patch](deltas/1.18.2/changes.patch) · [README](deltas/1.18.2/README.md) · [mixins.json](deltas/1.18.2/mixins.json) · [build.gradle.kts](deltas/1.18.2/build.gradle.kts) |
| [1.19](deltas/1.19/) | 17 | 0.14.24 | n/a | PoseStack | Component | plain | derived | [patch](deltas/1.19/changes.patch) · [README](deltas/1.19/README.md) · [mixins.json](deltas/1.19/mixins.json) · [build.gradle.kts](deltas/1.19/build.gradle.kts) |
| [1.19.1](deltas/1.19.1/) | 17 | 0.14.24 | n/a | PoseStack | Component | plain | derived | [patch](deltas/1.19.1/changes.patch) · [README](deltas/1.19.1/README.md) · [mixins.json](deltas/1.19.1/mixins.json) · [build.gradle.kts](deltas/1.19.1/build.gradle.kts) |
| [1.19.2](deltas/1.19.2/) | 17 | 0.14.24 | n/a | PoseStack | Component | plain | derived | [patch](deltas/1.19.2/changes.patch) · [README](deltas/1.19.2/README.md) · [mixins.json](deltas/1.19.2/mixins.json) · [build.gradle.kts](deltas/1.19.2/build.gradle.kts) |
| [1.19.3](deltas/1.19.3/) | 17 | 0.14.24 | n/a | PoseStack | Component | widget | derived | [patch](deltas/1.19.3/changes.patch) · [README](deltas/1.19.3/README.md) · [mixins.json](deltas/1.19.3/mixins.json) · [build.gradle.kts](deltas/1.19.3/build.gradle.kts) |
| [1.19.4](deltas/1.19.4/) | 17 | 0.14.24 | n/a | PoseStack | Component | widget | derived | [patch](deltas/1.19.4/changes.patch) · [README](deltas/1.19.4/README.md) · [mixins.json](deltas/1.19.4/mixins.json) · [build.gradle.kts](deltas/1.19.4/build.gradle.kts) |
| [1.20](deltas/1.20/) | 17 | 0.14.24 | n/a | GuiGraphics | Component | widget | derived | [patch](deltas/1.20/changes.patch) · [README](deltas/1.20/README.md) · [mixins.json](deltas/1.20/mixins.json) · [build.gradle.kts](deltas/1.20/build.gradle.kts) |
| [1.20.1](deltas/1.20.1/) | 17 | 0.14.24 | n/a | GuiGraphics | Component | widget | documented | [patch](deltas/1.20.1/changes.patch) · [README](deltas/1.20.1/README.md) · [mixins.json](deltas/1.20.1/mixins.json) · [build.gradle.kts](deltas/1.20.1/build.gradle.kts) |
| [1.20.2](deltas/1.20.2/) | 17 | 0.15.11 | n/a | GuiGraphics | Component | widget | derived | [patch](deltas/1.20.2/changes.patch) · [README](deltas/1.20.2/README.md) · [mixins.json](deltas/1.20.2/mixins.json) · [build.gradle.kts](deltas/1.20.2/build.gradle.kts) |
| [1.20.3](deltas/1.20.3/) | 17 | 0.15.11 | n/a | GuiGraphics | Component | widget | derived | [patch](deltas/1.20.3/changes.patch) · [README](deltas/1.20.3/README.md) · [mixins.json](deltas/1.20.3/mixins.json) · [build.gradle.kts](deltas/1.20.3/build.gradle.kts) |
| [1.20.4](deltas/1.20.4/) | 17 | 0.15.11 | 20.4.251 | GuiGraphics | Component | widget | derived | [patch](deltas/1.20.4/changes.patch) · [README](deltas/1.20.4/README.md) · [mixins.json](deltas/1.20.4/mixins.json) · [build.gradle.kts](deltas/1.20.4/build.gradle.kts) |
| [1.20.5](deltas/1.20.5/) | 21 | 0.16.9 | n/a | GuiGraphics | Component | widget | derived | [patch](deltas/1.20.5/changes.patch) · [README](deltas/1.20.5/README.md) · [mixins.json](deltas/1.20.5/mixins.json) · [build.gradle.kts](deltas/1.20.5/build.gradle.kts) |
| [1.20.6](deltas/1.20.6/) | 21 | 0.16.9 | 20.6.141 | GuiGraphics | Component | widget | documented | [patch](deltas/1.20.6/changes.patch) · [README](deltas/1.20.6/README.md) · [mixins.json](deltas/1.20.6/mixins.json) · [build.gradle.kts](deltas/1.20.6/build.gradle.kts) |
| [1.21](deltas/1.21/) | 21 | 0.16.9 | 21.0.167 | GuiGraphics | Component | widget | derived | [patch](deltas/1.21/changes.patch) · [README](deltas/1.21/README.md) · [mixins.json](deltas/1.21/mixins.json) · [build.gradle.kts](deltas/1.21/build.gradle.kts) |
| **1.21.1** | 21 | 0.16.9 | 21.1.228 | GuiGraphics | Component | widget | reference | the tree itself - [`common/`](common/src/main/java/com/aetherium), [`gradle.properties`](gradle.properties) |
| [1.21.2](deltas/1.21.2/) | 21 | 0.16.14 | 21.2.1-beta | GuiGraphics | Component | widget | derived | [patch](deltas/1.21.2/changes.patch) · [README](deltas/1.21.2/README.md) · [mixins.json](deltas/1.21.2/mixins.json) · [build.gradle.kts](deltas/1.21.2/build.gradle.kts) |
| [1.21.3](deltas/1.21.3/) | 21 | 0.16.14 | 21.3.97 | GuiGraphics | Component | widget | derived | [patch](deltas/1.21.3/changes.patch) · [README](deltas/1.21.3/README.md) · [mixins.json](deltas/1.21.3/mixins.json) · [build.gradle.kts](deltas/1.21.3/build.gradle.kts) |
| [1.21.4](deltas/1.21.4/) | 21 | 0.16.14 | 21.4.158 | GuiGraphics | Component | widget | documented | [patch](deltas/1.21.4/changes.patch) · [README](deltas/1.21.4/README.md) · [mixins.json](deltas/1.21.4/mixins.json) · [build.gradle.kts](deltas/1.21.4/build.gradle.kts) |
| [1.21.5](deltas/1.21.5/) | 21 | 0.16.14 | 21.5.81 | GuiGraphics | Component | widget | derived | [patch](deltas/1.21.5/changes.patch) · [README](deltas/1.21.5/README.md) · [mixins.json](deltas/1.21.5/mixins.json) · [build.gradle.kts](deltas/1.21.5/build.gradle.kts) |
| [1.21.6](deltas/1.21.6/) | 21 | 0.16.14 | 21.6.20-beta | GuiGraphics | Component | widget | derived | [patch](deltas/1.21.6/changes.patch) · [README](deltas/1.21.6/README.md) · [mixins.json](deltas/1.21.6/mixins.json) · [build.gradle.kts](deltas/1.21.6/build.gradle.kts) |
| [1.21.7](deltas/1.21.7/) | 21 | 0.16.14 | 21.7.25-beta | GuiGraphics | Component | widget | derived | [patch](deltas/1.21.7/changes.patch) · [README](deltas/1.21.7/README.md) · [mixins.json](deltas/1.21.7/mixins.json) · [build.gradle.kts](deltas/1.21.7/build.gradle.kts) |
| [1.21.8](deltas/1.21.8/) | 21 | 0.16.14 | 21.8.22 | GuiGraphics | Component | widget | derived | [patch](deltas/1.21.8/changes.patch) · [README](deltas/1.21.8/README.md) · [mixins.json](deltas/1.21.8/mixins.json) · [build.gradle.kts](deltas/1.21.8/build.gradle.kts) |
| [1.21.9](deltas/1.21.9/) | 21 | 0.16.14 | 21.9.16-beta | GuiGraphics | Component | widget | derived | [patch](deltas/1.21.9/changes.patch) · [README](deltas/1.21.9/README.md) · [mixins.json](deltas/1.21.9/mixins.json) · [build.gradle.kts](deltas/1.21.9/build.gradle.kts) |
| [1.21.10](deltas/1.21.10/) | 21 | 0.16.14 | 21.10.64 | GuiGraphics | Component | widget | derived | [patch](deltas/1.21.10/changes.patch) · [README](deltas/1.21.10/README.md) · [mixins.json](deltas/1.21.10/mixins.json) · [build.gradle.kts](deltas/1.21.10/build.gradle.kts) |
| [1.21.11](deltas/1.21.11/) | 21 | 0.16.14 | 21.11.45 | GuiGraphics | Component | widget | derived | [patch](deltas/1.21.11/changes.patch) · [README](deltas/1.21.11/README.md) · [mixins.json](deltas/1.21.11/mixins.json) · [build.gradle.kts](deltas/1.21.11/build.gradle.kts) |
| [26.1](deltas/26.1/) | 25 | 0.19.5 | 26.1.2.115 | GuiGraphics | Component | widget | derived | [patch](deltas/26.1/changes.patch) · [README](deltas/26.1/README.md) · [mixins.json](deltas/26.1/mixins.json) · [build.gradle.kts](deltas/26.1/build.gradle.kts) |
| [26.2](deltas/26.2/) | 25 | 0.19.5 | 26.2.0.89 | GuiGraphics | Component | widget | derived | [patch](deltas/26.2/changes.patch) · [README](deltas/26.2/README.md) · [mixins.json](deltas/26.2/mixins.json) · [build.gradle.kts](deltas/26.2/build.gradle.kts) |
| [26.3](deltas/26.3/) | 25 | 0.19.5 | 26.3.0.64-beta | GuiGraphics | Component | widget | documented | [patch](deltas/26.3/changes.patch) · [README](deltas/26.3/README.md) · [mixins.json](deltas/26.3/mixins.json) · [build.gradle.kts](deltas/26.3/build.gradle.kts) |

33 rows: 1 reference + 32 deltas.

## All versions

Every row, linked. A delta directory holds four files: `changes.patch` (what to
apply), `README.md` (every era fact and its provenance, plus the recipe to check
the row against the real mapped jar), `mixins.json` (the exact mixin config the
ported tree ships) and `build.gradle.kts` (a pin guard the port runs to prove
the tree and this row agree).

### 1.16 - 1.18 (Java 8/16/17, MatrixStack/PoseStack, TextComponent)

- [1.16.5](deltas/1.16.5/) - Java 8, [patch](deltas/1.16.5/changes.patch), [README](deltas/1.16.5/README.md) - `documented`
- [1.17](deltas/1.17/) - Java 16, [patch](deltas/1.17/changes.patch), [README](deltas/1.17/README.md) - `unverified`
- [1.17.1](deltas/1.17.1/) - Java 16, [patch](deltas/1.17.1/changes.patch), [README](deltas/1.17.1/README.md) - `unverified`
- [1.18](deltas/1.18/) - Java 17, [patch](deltas/1.18/changes.patch), [README](deltas/1.18/README.md) - `derived`
- [1.18.1](deltas/1.18.1/) - Java 17, [patch](deltas/1.18.1/changes.patch), [README](deltas/1.18.1/README.md) - `derived`
- [1.18.2](deltas/1.18.2/) - Java 17, [patch](deltas/1.18.2/changes.patch), [README](deltas/1.18.2/README.md) - `derived`

### 1.19 - 1.20 (PoseStack -> GuiGraphics at 1.20, narration at 1.19.4)

- [1.19](deltas/1.19/) - Java 17, [patch](deltas/1.19/changes.patch), [README](deltas/1.19/README.md) - `derived`
- [1.19.1](deltas/1.19.1/) - Java 17, [patch](deltas/1.19.1/changes.patch), [README](deltas/1.19.1/README.md) - `derived`
- [1.19.2](deltas/1.19.2/) - Java 17, [patch](deltas/1.19.2/changes.patch), [README](deltas/1.19.2/README.md) - `derived`
- [1.19.3](deltas/1.19.3/) - Java 17, [patch](deltas/1.19.3/changes.patch), [README](deltas/1.19.3/README.md) - `derived`
- [1.19.4](deltas/1.19.4/) - Java 17, [patch](deltas/1.19.4/changes.patch), [README](deltas/1.19.4/README.md) - `derived`
- [1.20](deltas/1.20/) - Java 17, [patch](deltas/1.20/changes.patch), [README](deltas/1.20/README.md) - `derived`
- [1.20.1](deltas/1.20.1/) - Java 17, [patch](deltas/1.20.1/changes.patch), [README](deltas/1.20.1/README.md) - `documented`
- [1.20.2](deltas/1.20.2/) - Java 17, [patch](deltas/1.20.2/changes.patch), [README](deltas/1.20.2/README.md) - `derived`
- [1.20.3](deltas/1.20.3/) - Java 17, [patch](deltas/1.20.3/changes.patch), [README](deltas/1.20.3/README.md) - `derived`
- [1.20.4](deltas/1.20.4/) - Java 17, [patch](deltas/1.20.4/changes.patch), [README](deltas/1.20.4/README.md) - `derived`
- [1.20.5](deltas/1.20.5/) - Java 21, [patch](deltas/1.20.5/changes.patch), [README](deltas/1.20.5/README.md) - `derived`
- [1.20.6](deltas/1.20.6/) - Java 21, [patch](deltas/1.20.6/changes.patch), [README](deltas/1.20.6/README.md) - `documented`

### 1.21.x (the reference line and its successors)

- [1.21](deltas/1.21/) - Java 21, [patch](deltas/1.21/changes.patch), [README](deltas/1.21/README.md) - `derived`
- [1.21.2](deltas/1.21.2/) - Java 21, [patch](deltas/1.21.2/changes.patch), [README](deltas/1.21.2/README.md) - `derived`
- [1.21.3](deltas/1.21.3/) - Java 21, [patch](deltas/1.21.3/changes.patch), [README](deltas/1.21.3/README.md) - `derived`
- [1.21.4](deltas/1.21.4/) - Java 21, [patch](deltas/1.21.4/changes.patch), [README](deltas/1.21.4/README.md) - `documented`
- [1.21.5](deltas/1.21.5/) - Java 21, [patch](deltas/1.21.5/changes.patch), [README](deltas/1.21.5/README.md) - `derived`
- [1.21.6](deltas/1.21.6/) - Java 21, [patch](deltas/1.21.6/changes.patch), [README](deltas/1.21.6/README.md) - `derived`
- [1.21.7](deltas/1.21.7/) - Java 21, [patch](deltas/1.21.7/changes.patch), [README](deltas/1.21.7/README.md) - `derived`
- [1.21.8](deltas/1.21.8/) - Java 21, [patch](deltas/1.21.8/changes.patch), [README](deltas/1.21.8/README.md) - `derived`
- [1.21.9](deltas/1.21.9/) - Java 21, [patch](deltas/1.21.9/changes.patch), [README](deltas/1.21.9/README.md) - `derived`
- [1.21.10](deltas/1.21.10/) - Java 21, [patch](deltas/1.21.10/changes.patch), [README](deltas/1.21.10/README.md) - `derived`
- [1.21.11](deltas/1.21.11/) - Java 21, [patch](deltas/1.21.11/changes.patch), [README](deltas/1.21.11/README.md) - `derived`

### Date-based ids (26.x)

- [26.1](deltas/26.1/) - Java 25, [patch](deltas/26.1/changes.patch), [README](deltas/26.1/README.md) - `derived`
- [26.2](deltas/26.2/) - Java 25, [patch](deltas/26.2/changes.patch), [README](deltas/26.2/README.md) - `derived`
- [26.3](deltas/26.3/) - Java 25, [patch](deltas/26.3/changes.patch), [README](deltas/26.3/README.md) - `documented`

### 1.21.1 - the reference version

- no delta: this tree *is* the 1.21.1 build - [`gradle.properties`](gradle.properties), [`common/`](common/src/main/java/com/aetherium)
- Java 21, Fabric loader 0.16.9, Loom 1.16.1, NeoForge 21.1.228 - provenance in [docs/BUILD_PINS.md](docs/BUILD_PINS.md)

## What a delta does and does not contain

`deltas/<version>/changes.patch` may change only:

1. build pins (`minecraft_version`, `java_version`, `neoforge_version`,
   `fabric_loader_version`, `fabric_api_version`, `fabric_loom_version` + the
   matching `loom` entry in `gradle/libs.versions.toml`, `enabled_platforms`, the
   NeoForge include in `settings.gradle.kts` and the root build aggregate);
2. the mixin `compatibilityLevel` (it tracks the row's Java toolchain);
3. `REFERENCE`-side strings in `AetheriumMixinPlugin` (the announced version and
   the per-mixin target ranges);
4. the era transforms listed in each delta's README - the mechanical
   API renames (stack class, text components, narration, widget methods,
   `renderBackground` arity, widget setters, `Button.builder`, entity iteration,
   `CycleButton`) - applied as ordered whole-file rewrites;
5. an *append* to an existing `method = { ... }` candidate list - reference names
   are never removed, so two ports merge without conflict.

It never restructures a class, fixes a bug, or changes an algorithm.
`tools/gen_deltas.py` refuses to emit anything else, and `tools/check.py` proves
every patch still applies with `git apply --check`.

### Loom, and why one version serves all 33 rows

Every row pins Loom to the reference value. `gradle/wrapper/gradle-wrapper.properties`
is tree-wide (Gradle 9.4.1) and no delta rewrites it, and a Loom from the 1.2 or 1.6 era
does not run on Gradle 9 - so an era-matched Loom pin would describe a build this tree
cannot start. Loom itself is Minecraft-version-agnostic (the Minecraft artifact and the
intermediary/mojmap channel decide the version), and the plugin id and version are read
from FabricMC/fabric-loom's own `gradlePlugin` block (`net.fabricmc.fabric-loom-remap`).
A port that genuinely needs a different Loom must bump the wrapper, `gradle.properties`
and `gradle/libs.versions.toml` together; `tools/check.py` fails if the last two
disagree, which is the guard against half a bump.

## Applying one

```sh
sh tools/port.sh --list            # what is known about each row
sh tools/port.sh --dry-run 1.20.6  # apply to a copy, run the checks, throw it away
sh tools/port.sh 1.20.6            # apply to this tree (patch + mixins.json + pin guard)
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

NeoForge begins at 1.20.2, so rows below it are Fabric-only. For those rows the
delta edits three coordinated places - `enabled_platforms=fabric`, the
`include("neoforge")` line in `settings.gradle.kts`, and the `:neoforge:build`
reference in the root build aggregate - because Gradle configures every included
project and one dangling reference fails the whole tree before a single compile.
For those versions the practical alternatives are Fabric (Pojav/Zalith on Android)
or the mod's own Fabric jar under Sapphire; `docs/ARCHITECTURE.md` explains why no
legacy-Forge port exists in this repository: the mixin surface differs enough that
it would be a second codebase wearing a delta costume.

## Verification record

The 1.21.1 mixin target names and era boundaries in this tree were read from real
sources, not memory: `CaffeineMC/sodium @ 1.21.1/stable` (LevelRenderer,
OptionsScreen, GameRenderer mixin sets and every build pin),
`IrisShaders/Iris @ 1.19.4 / 1.20.1 / 1.20.6 / 1.21.1` (Gui, GameRenderer,
LightTexture mixins, screen render/renderBackground shapes, narration name),
LWJGL's generated `GL44C`/`GL46C`/`ARBParallelShaderCompile` sources (GL constant
values), and the FabricMC, neoforged and IrisShaders tag listings (pin
existence). Where a name could not be read, the code or the delta README carries
an `[UNVERIFIED: ...]` mark and a tolerant injection -
`python3 tools/check.py --report-unverified` lists them all.
