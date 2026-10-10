# Build pins and where each one came from

Every version in this build is pinned to an exact string — no `+`, no `latest`, no ranges. A
build that floats cannot be re-created by the person debugging a version-specific report, and a
port delta that changes a range is a port that silently breaks later.

Provenance grades used below:

| grade | meaning |
| --- | --- |
| **VERIFIED** | read out of a real upstream project's own build file on 2026-10-06 (GitHub contents API), for the version stated |
| **GUESS** | plausible for the reference version and *not* checked against an upstream file; must be confirmed on first build |
| **DERIVED** | computed from a rule recorded in `tools/porting_pins.json` (e.g. "Java 21 from 1.20.5"), not read from anyone's build |
| **UNVERIFIED** | recorded because something else needs it; the row's `status` in the porting matrix says so too |

## Reference version: 1.21.1

| pin | value | grade | source / note |
| --- | --- | --- | --- |
| `java_version` | 21 | VERIFIED | 1.21.1 requires Java 21; the toolchain is set in `common/build.gradle.kts`, not assumed |
| `minecraft_version` | 1.21.1 | — | the reference row; `common/` is written against this version's Mojang names |
| `fabric_loom_version` | `1.16.1` | VERIFIED | `CaffeineMC/sodium` @ `1.21.1/stable` → `common/build.gradle.kts` |
| Loom plugin **id** | `net.fabricmc.fabric-loom-remap` | VERIFIED | current Loom publishes exactly two ids — `net.fabricmc.fabric-loom` (no remap) and `net.fabricmc.fabric-loom-remap` (classic, incl. `remapJar`): read from `FabricMC/fabric-loom`'s own `build.gradle` `gradlePlugin` block; sodium applies the `-remap` id at exactly 1.16.1. The bare `fabric-loom` id of the 1.8 era no longer resolves at this version |
| `neoforge_moddev_version` | `2.0.141` | VERIFIED | same source → `neoforge/build.gradle.kts` |
| `mixin_extras_version` | `0.5.3` | VERIFIED | same source (`mixinextras-common`) |
| `sponge_mixin_version` | `0.13.2+mixin.0.8.5` | VERIFIED | same source |
| `neoforge_version` | `21.1.228` | VERIFIED | `CaffeineMC/sodium` @ `1.21.1/stable` → `buildSrc/src/main/kotlin/BuildConfig.kt` (`NEOFORGE_VERSION = "21.1.228"`) |
| `fabric_loader_version` | `0.16.9` | VERIFIED | tag exists on `FabricMC/fabric-loader` (ls-remote, 2026-10-10); the latest loader line is 0.19.5, this pin keeps the reference era-accurate |
| `fabric_api_version` | `0.116.17+1.21.1` | VERIFIED | latest `+1.21.1` tag on `FabricMC/fabric` (ls-remote, 2026-10-10). **Not a runtime dependency** — declared only where a platform module needs it for compile-time references; Aetherium's mixins target vanilla, and the GUI has no Mod Menu link |
| `forgified_fabric_api_version` | `0.10.0+1.21` | GUESS | NeoForge side precedent is Embeddium; confirm |
| `lwjgl_version` | `3.3.3` | VERIFIED (by shipping) | MC 1.21.1 ships LWJGL 3.3.3; `GL43C`/`GL45C`/`GL46C` classes exist in that jar, which is what `render/gl/GlProcs` routes through |
| `mapping_channel` | `mojmap` (`officialMojangMappings()`) | VERIFIED | resolved by Loom; keeps one source set remappable by both loaders |
| `parchment_version` | *(empty)* | deliberate | Parchment is optional; an empty pin means a clean machine never contacts that maven |
| Gradle | `9.4.1` | VERIFIED | from Sodium's `gradle/wrapper/gradle-wrapper.properties` at the same ref |
| `junit` | `5.11.4` | UNVERIFIED | JUnit 5 BOM; any 5.11.x works, pinned for reproducibility |
| `checkstyle` | `10.20.1` | UNVERIFIED | the tool version `checkstyle.xml` was written against |
| `archives_base_name` / `mod_id` / `mod_version` | `aetherium` / `aetherium` / `0.1.0` | — | jar names are `aetherium-fabric-<version>.jar` and `aetherium-neoforge-<version>.jar` |
| `mod_license` | `LGPL-3.0-only` | — | must match `LICENSE`; the manifests expand `${mod_license}` from here |
| `aetherium.enableRunConfigs` | `false` | deliberate | `loom { runs }` / NeoForge run configs are generated only when `true`, so CI without a GPU never fails on a missing run config |

## Tooling versions (Gradle catalog)

`gradle/libs.versions.toml` holds the tooling plugins only — `checkstyle 10.20.1`,
`junit 5.11.4`, `com.palantir.git-version 1.0.0`. The reason is stated in that file: keeping
tooling in the catalog means a porting delta only ever edits `gradle.properties`, which is the
one file whose shape is identical on all 33 rows.

## Java level per row (DERIVED rule)

| Minecraft | Java | why |
| --- | --- | --- |
| 1.16.x and older | 8 | class-file level of the game itself |
| 1.17–1.17.1 | 16 | 1.17 raised the floor to 16 |
| 1.18–1.20.4 | 17 | 1.18 raised it to 17 |
| 1.20.5–1.21.x and later 1.x | 21 | the modern floor; the reference row |
| 26.x (date-based ids) | 25 | DERIVED from the same upstream projects' toolchain blocks for those rows; **UNVERIFIED** for any row whose `status` is `derived`/`unverified` |

`tools/setup_jdk.sh` prints this table for a chosen version and the `JAVA_HOME` it expects, so
the porting workflow does not have to remember it.

## Other pinned choices

- **No Fabric API runtime dependency, no Mod Menu.** `fabric.mod.json` declares no optional mods
  and documents in a `_comment` why there is no `breaks` block (a `breaks` entry on Sodium would
  make the launcher refuse to start instead of showing Aetherium's own explanation, which is
  worse for the user and worse for the bug report).
- **`aetherium-common.mixins.json` is static**, shipped verbatim by both platform jars. There is
  deliberately no `${mixin_required}` placeholder: `processResources` expands tokens in `common`
  but not inside the copied platform resources, so a placeholder would ship two different files
  from one source. `strict_mixins` is therefore enforced at runtime by `AetheriumMixinPlugin`.
- **`mixinExtras` and `sponge-mixin` are `compileOnly` + annotation-processor** in `common`, and
  shadowed/remapped by the platform modules. Nothing in `common/` links a loader class.
- **No shader mod appears in any build file.** Aetherium has no shader-mod integration; a
  dependency would couple every Aetherium version to every shader-mod version.
- **LWJGL is `compileOnly`.** The game provides the natives at runtime; bundling them is how a
  renderer mod ends up with two GLFW copies.
- **`vulkan` bindings come from `org.lwjgl:lwjgl-vulkan` (compileOnly)**, pinned to the same
  `3.3.3`, because the Vulkan path needs the headers rather than a loader-specific jar.

## The wrapper jar

`gradle/wrapper/gradle-wrapper.properties` (and `gradlew`, `gradlew.bat`) are committed; the
**`gradle-wrapper.jar` is not**, because this repository was authored in an environment that
cannot add binary files. Generate it once:

```sh
gradle wrapper --gradle-version 9.4.1
```

Nothing else changes; the properties file already pins 9.4.1 and the checksum-bearing
distribution URL. CI runs the same command before `./gradlew build` (see
`.github/workflows/build.yml`).

## The jars: what exists and what you download

There is no Aetherium `.jar` to download. Nothing was ever compiled here (no JDK, no Gradle,
no GPU), there are no git tags, and the repository publishes no releases — so any link claiming
to serve `aetherium-*.jar` would be a lie. The three artifacts below are what a build on your
machine produces, and they only exist after you run it:

| module | produced by | where it lands |
| --- | --- | --- |
| `aetherium-common-0.1.0.jar` (+ `-sources.jar`) | `./gradlew :common:build` | `common/build/libs/` |
| `aetherium-fabric-0.1.0.jar` | `./gradlew :fabric:build` | `fabric/build/libs/` (Loom writes the remapped jar there; the un-remapped one stays in `fabric/build/devlibs/`) |
| `aetherium-neoforge-0.1.0.jar` | `./gradlew :neoforge:build` | `neoforge/build/libs/` |

`archives_base_name` and `mod_version` come from [`gradle.properties`](../gradle.properties);
`version` is set to Minecraft 1.21.1 only by the per-version deltas, which is why a ported tree
renames these artifacts. If a Loom or ModDev release puts them elsewhere, `find . -path '*/build/libs/*.jar'`
is the answer, not this table.

The jars you have to fetch by hand are the *inputs*, and only if you are working offline —
Gradle, Loom and ModDev download all of them otherwise. Every URL below was read off the
host's own directory index on 2026-10-06, so the names and sizes are theirs, not guesses:

| what | link | as listed by the host |
| --- | --- | --- |
| Gradle distribution (the wrapper jar is COMMITTED now — the real Gradle 9.4.1 wrapper jar, byte-identical to the one CaffeineMC/sodium ships at the same version — so `./gradlew` works from a clean clone; the distribution URL below is what it downloads) | `https://services.gradle.org/distributions/gradle-9.4.1-bin.zip` | sha256 `2ab2958f2a1e51120c326cad6f385153bb11ee93b3c216c5fccebfdfbb7ec6cb` (from `…gradle-9.4.1-bin.zip.sha256`) |
| Fabric loader 0.16.9 | `https://maven.fabricmc.net/net/fabricmc/fabric-loader/0.16.9/fabric-loader-0.16.9.jar` | 1 MB, 30-Oct-2024, `.sha1`/`.sha512` beside it |
| Fabric API 0.116.17+1.21.1 | `https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.116.17+1.21.1/fabric-api-0.116.17%2B1.21.1.jar` | latest 1.21.1 line per FabricMC/fabric tags (2026-10-10) |
| NeoForge 21.1.228 | `https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.228/` — the same coordinates sodium's BuildConfig.kt pins; `…-moddev-config.json` is what ModDev reads | index, not one file: changelog/sources/installer/moddev-config are all there |
| LWJGL 3.3.3 core + natives | `https://repo1.maven.org/maven2/org/lwjgl/lwjgl/3.3.3/lwjgl-3.3.3.jar`, then `lwjgl-3.3.3-natives-linux.jar` / `-macos-arm64` / `-windows` in the same directory | core 785,029 B; natives 114,627 / 48,620 / 165,442 B |
| Sodium, the renderer this mod defers to | `https://cdn.modrinth.com/data/AANobbMI/versions/SMxNOGZ6/sodium-fabric-0.8.13%2Bmc1.21.1.jar` | `sodium-fabric-0.8.13+mc1.21.1.jar`, 1,574,609 B, sha1 `003c114c85ca88ef3362e018deb6aca0c682d6a1` |

The Sodium entry carries information the code depends on: it is the version whose tree
`gradle.properties` pins were copied from.

Check hashes before you drop any of these into a Gradle cache, e.g.
`sha1sum sodium-fabric-0.8.13+mc1.21.1.jar`. Then, to build the mod itself:

```sh
sh tools/setup_jdk.sh            # 21
gradle wrapper --gradle-version 9.4.1
./gradlew build
```

A push to `main` is the one path to a real download link: the `build` workflow compiles on
ubuntu/windows/macos and its `draft-release` job uploads the two loader jars plus `unverified.txt`
to a draft release named after these pins (`v0.1.0+1.21.1`). The jar upload uses
`if-no-files-found: error`, so a run that produced nothing cannot quietly publish an empty release,
and the draft stays unpublished until a human has run the jar. Branches and pull requests build and
test but never publish.

## Changing a pin

1. Edit `tools/porting_pins.json` (the source of truth for all 33 rows), not
   `PORTING_MATRIX.md` — the table is generated and `tools/check.py` fails when it drifts.
2. `python3 tools/gen_deltas.py --all --matrix --verify` regenerates `deltas/*/` and applies every
   patch to the tree. A row whose anchors no longer match shows up here, not in someone's launch.
3. `sh tools/checkTree.sh` runs the structural checkers (`check_java.py`, `check_refs.py`,
   `check.py`, `gen_resources.py --check`).
4. Anything you could not confirm keeps its grade. Do not promote `GUESS` to `VERIFIED` without
   naming the upstream file the value was read from — the note field exists for that sentence.
