# Contributing to Aetherium

Thanks for reading this instead of opening an issue to ask "does it work with
Sodium?". Short answer: it detects it and hands the overlapping feature back
(see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#conflicts)). Longer answers below.

## What this repository is trying to be

A rendering engine replacement for Minecraft that a person can read end to end:
one engine in `common/`, two thin loader shells, 33 version rows that differ only
by mechanical patches. Every design decision has to survive two questions -
"why this and not the obvious alternative", and "what happens on a phone with a
Mali GPU and a 4.3 core profile" - so the code comments carry an unusual amount
of prose. That is on purpose: **a comment that only says what the code does is
worthless here; say why, or delete it.**

## Ground rules (the CI-enforced ones)

| Rule | Checked by |
| --- | --- |
| No `TODO`/`FIXME`/`XXX` anywhere | `checkstyle.xml` (`RegexpSingleline`) |
| No placeholder bodies (`// logic here`, `...`) | `checkstyle.xml` + `tools/check.py` |
| Every referenced member exists | `python3 tools/check_refs.py` (run by `./gradlew checkTree`) |
| Lang files, `CONFIG_SCHEMA.json` in sync with config | `python3 tools/gen_resources.py --check` |
| Every `[UNVERIFIED: ...]` mark reported, not silently shipped | `tools/check.py --report-unverified` |
| Java 21, tabs for indent, 140-col soft limit | `.editorconfig`, `checkstyle.xml` |
| Final newline + LF everywhere (`git apply` in `deltas/` depends on it) | `checkstyle.xml` (`NewlineAtEndOfFile`) |

`./gradlew verifyAll` runs the lot. If you don't have a JDK yet, run
`sh tools/setup_jdk.sh` first; if you don't want Gradle at all, `sh tools/checkTree.sh`
does the offline checks without a Minecraft download.

## Code standards that are not lintable

- **`final` by default.** Fields that do not change after construction are `final`;
  locals that are not reassigned are `final`. It is not decoration: on this codebase a
  field that *could* be reassigned is a field a worker thread might see mid-reassign.
- **`Objects.requireNonNull` at every public entry point** that a caller can hand a
  null to. Fail at the boundary with a message naming the argument, rather than 40
  frames deeper as an NPE inside a GL call.
- **Name the lock.** Any field readable from two threads carries a comment saying which
  lock or queue guards it (e.g. `// Guarded by RenderSafety#lock`, or "render-thread
  only"). "It's a volatile so it's fine" is not an answer; say what the invariant is.
- **Log levels are a contract.** `error` = the game may be broken or data lost.
  `warn` = a feature is off, the game is fine. `info` = exactly one line per event the
  user can cause (backend resolved, conflict detected, hot swap). `dev` = per-frame
  diagnostics, gated by `advanced.debug_logging`, and must not allocate when disabled.
  Never swallow an exception silently; if a catch block is empty, the build fails.
- **No fabricated APIs.** If you cannot verify a Minecraft, LWJGL, Fabric or Iris
  signature from a real source (the shipped game jar, upstream sources, the vendor
  spec), write `[UNVERIFIED: exact thing that is uncertain]` in the line above it and
  make the code tolerate the uncertainty (tolerant mixin, reflection with a fallback,
  `require = 0`). The next person must be able to grep `[UNVERIFIED` and find every
  open question in the tree. That mark is a promise to check, not a TODO.
- **Never claim a benchmark you did not run.** Performance numbers in this repository
  come from `tools/benchmark.sh` output committed to `BENCHMARK.md` with the hardware
  and seed named. A PR that says "2x Sodium" without a table gets closed.

## Adding a config option (the one procedure worth memorising)

1. Declare it in `common/src/main/java/com/aetherium/config/AetheriumConfig.java`:
   ```java
   public final ConfigValue<Integer> myThing = ConfigValue.intRange(
           "performance.my_thing", 4, 0, 8, false, "What it does, and what 0 means.");
   ```
   `general|performance|quality|shaders|utilities|android|advanced` pick the group and
   therefore the tab. The comment becomes the widget's subtitle text and the schema
   description, so write it as user-facing prose.
2. Use it: `Aetherium.config().myThing.get()`. Listeners for live GUI updates:
   `addListener(...)`.
3. Wire a widget in `gui/AetheriumTabs.java` (one line; the label key is automatic).
4. `python3 tools/gen_resources.py` - lang files and `CONFIG_SCHEMA.json` regenerate.
5. If the option changes anything GPU-resident, call `parent.markNeedsHotSwap()` so
   the safe point re-runs `BackendSelector#reapply`; if it needs the chunk mesh rebuilt,
   the option must be marked `requiresWorldReload` (`Flags`) and the GUI says so.

Nothing else. Do not hand-edit `en_us.json`, `pt_br.json` or `CONFIG_SCHEMA.json`;
they are generated and `--check` will fail your build.

## Touching a Mixin

- Prefer one tolerant injection with candidate names over two version-conditional
  mixins: `method = {"updateTick", "tick"}` plus `require = 0, expect = 0` covers a
  rename without a delta. `AetheriumMixinPlugin` is where a genuinely different *shape*
  (not name) is decided, and it decides from the `ClassNode` it is handed, never from
  a version string.
- Never `@Shadow` a field whose existence you have not verified in the shipped jar.
  `gamma/LightmapWriter` is the worked example of the alternative: probe the real
  class once, cache a `MethodHandle`, degrade with one log line.
- A mixin must never cancel vanilla work unless the whole feature is a replacement
  of that work; count and coalesce instead (`LevelRendererMixin` is that pattern).
- Every new mixin class goes into the `client` array of
  `common/src/main/resources/aetherium-common.mixins.json`, and into
  `AetheriumMixinPlugin#TARGETED_RANGES` if it is not version-tolerant by construction.

## Porting to another Minecraft version

You do not hand-edit `deltas/`. The matrix in `PORTING_MATRIX.md` is the input, and:

```sh
sh tools/port.sh --regen 1.20.6    # rebuild the patch from tools/porting_pins.json
sh tools/port.sh 1.20.6            # apply deltas/1.20.6/changes.patch to a clean tree
sh tools/port.sh --dry-run 1.20.6  # apply to a throwaway copy and check it there instead
sh tools/verify.sh 1.20.6          # syntax + reference check on the patched tree
```

A delta rewrites `gradle.properties`. The loader plugin versions are **also** pinned in
`gradle/libs.versions.toml`, because a Kotlin DSL `plugins {}` block is evaluated before
`project` exists and cannot read a property - so bump `loom` / `moddev` there too, or
`tools/check.py` fails with the mismatch named. One ported tree, two files, one rule.

Rules for a delta: it may change **tokens** (method names, descriptors, mixin ids,
version ranges, mapping channel) and it may add a version-specific target name to a
candidate list. It may **not** restructure a class, change an algorithm, or fix a bug -
fix it in `common/` so all 33 rows get it. If a port needs a structural change,
that is an architecture bug; open an issue with the version and the exact failure.

## Testing

JUnit 5, in `common/src/test/java`, three or more cases per core class (the count is
a floor, not a target). The engine is structured so the interesting parts are testable
without a game: pure functions (`MathUtil`, `GammaApplier.GammaCurve`, percentile
histograms in `FrameStats`, backend scoring in `BackendSelector`, the JSON codec and
the conflict table) all have no Minecraft import, and that is *why* they are pure.
A test that needs a window/GL context is a manual test: put it behind
`@EnabledIfSystemProperty(named = "aetherium.manual", matches = "true")` and say how
to run it in its javadoc.

## Commit and PR style

- Imperative subject, ≤ 72 chars, prefixed with the area:
  `gl: keep the arena fence per slot, not per frame`, `docs(android): add Zink caveats`.
- Body: what changes for a reader, and the verification you did (`./gradlew verifyAll`
  output, or "no JDK here: ran tools/checkTree.sh + java-parser").
- If you changed anything a user sees, update `README.md` and the lang comments; if
  you changed a pin in `gradle.properties`, update `docs/BUILD_PINS.md` with where you
  read it (URL + branch/tag, or "GUESS").
- One logical change per commit; the delta generator produces clean patches when the
  history is clean.

## What I will not merge

- A hard dependency on Iris/Oculus/Mod Menu (reflection or nothing - see
  [docs/IRIS_COMPAT.md](docs/IRIS_COMPAT.md)).
- Anything that "unloads" another mod's mixins, disables a mod at runtime, or deletes
  a config file it does not own. Conflict handling stays "delegate and tell the user".
- A claim of support for a version that has not been run, without `[UNVERIFIED]`.
- Vendored binaries, or a generated asset committed without its generator.
