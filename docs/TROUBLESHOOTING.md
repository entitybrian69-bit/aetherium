# Troubleshooting

Ordered by "what you probably noticed", not by subsystem. Every claim below describes code in
this repository; where the diagnosis needs the log, the exact string to grep for is quoted.

## First, 60 seconds of facts

1. Open `logs/latest.log` and search `Aetherium`. Startup prints:
   `Aetherium 0.1.0 ready on <platform> for MC <version> — state=<STATE>`.
2. In game, the top-right tag reads `[Aetherium/GL46_DSA]`, `[Aetherium/SHADOW]`, etc.
3. The Advanced tab prints `Aetherium.describeRuntime()` (`state=… api=…`) plus the
   device and capability lines, and `device.describe()`.

`state=ACTIVE` means the render path is hooked. `SHADOW` means Aetherium measures and applies
lightmap/gamma but vanilla draws. `INCOMPATIBLE` means a HARD conflict stood it down. Those
three look identical to "the mod does nothing" from outside, so check the state first.

## The mod seems to do nothing

| check | how | fix |
| --- | --- | --- |
| `general.enabled` is true | General tab, first row | the Compatibility toggle is live; flip it and the state line changes in the log |
| a conflicting renderer is installed | top-right notice: `Aetherium is standing down: …` | remove one of them. `advanced.conflict_auto_delegate = false` stops Aetherium delegating *features*, but a HARD conflict still stands the renderer down — two mods patching `LevelRenderer` is not a combination to force |
| the GPU failed the gates | log line `Requested backend … is not feasible (…); falling back to AUTO` | that is the intended outcome; `performance.backend` can pin GL_CORE or GL_LEGACY |
| `advanced.experimental_full_renderer = true` | log line `… is not supported in 0.1.0 for MC 1.21.1 … Running in shadow mode instead.` | expected: that switch is NOT SHIPPED. See [ARCHITECTURE.md](ARCHITECTURE.md#honest-status) |
| you are on a 32-bit or non-arm64 launch | `Android environment: launcher=…, renderer=…` and the memory/feature line | the fast paths are arm64-only by design; see [ANDROID.md](ANDROID.md) |

## The settings screen does not open / looks wrong

The GUI is reached by patching the vanilla `OptionsScreen`, with a permanent fallback button in
the same screen. The Advanced tab reports which path is live:

- `hijacked vanilla Video Settings (N vanilla cycle buttons beside us)` — the mixin worked.
- `fallback button in use: no vanilla video controls were found` — the version's screen shape
  differs; the button is your entry and the hijack needs a delta (see
  [../PORTING_MATRIX.md](../PORTING_MATRIX.md)).
- `Options screen not opened yet` — you have not opened it since launch; open it once.

There is **no Mod Menu entry point**. If a guide tells you to look in Mod Menu, it is describing
another mod. On Fabric without Mod Menu the vanilla Video Settings screen is the entry point.

If a row looks unresponsive: options with `requiresRendererRestart` (backend, full renderer)
write to the file immediately but take effect at a safe point — the status line says so instead
of pretending. `advanced.strict_mixins` and the mixin config are read at class-transform time,
so those two need a game restart, and the GUI says that in their status line too.

## Chunks missing, black, or flickering

1. Enable `advanced.debug_logging` and look at the HUD's mesh line:
   `mesh dirty 512 built 540 full 1 burst 0`. `built` far ahead of `dirty` (the line appends
   `[overlap? built …]`) means two renderers are meshing the same sections. That is a
   mod-list problem, not a knob: remove the other renderer.
2. `full 0` with a flicker after a render-distance change means the full-rebuild hook is not
   firing on your version — the `allChanged` target list needs a delta entry.
3. Black chunks only while a shader pack is active: set Dynamic lights to Off (Effects tab) and
   restart. If the flicker stays, it is the pack.
4. A `burst` figure that keeps climbing points at a redstone-driven chunk-update storm, not at
   Aetherium; the log line `important section rebuilds in one frame` names the coordinates.

## Game crashes at launch

- **`Mixin apply failed` / `InvalidInjectionException` naming an `aetherium$…` method** — the
  target method moved on your version. Aetherium's injections use `require = 0, expect = 0`,
  so this normally degrades instead of crashing; if you see it, `advanced.strict_mixins` was on.
  Turn it off to play, and file the log so the version gets a delta.
- **`NoClassDefFoundError: net/caffeinemc/…`** — Sodium/Embeddium partially removed (a jar in
  two places, or a leftover config that loads a class by name).
- **Crash inside `AetheriumMixinPlugin`** — the version-range table refused a class it should
  have accepted; the plugin fails *open* (vanilla wins), so a crash here is a real bug. Attach
  the crash report; Aetherium appends its state via `ModConflictScanner#asReport` to the
  `Aetherium` section of the report when the scan found anything.
- **OptiFine present** — OptiFine patches classes at the bytecode level. This is the one
  combination Aetherium cannot coexist with; `NEITHER` ownership means neither mod's render
  path survives. There is no compatibility layer for it and none is planned.

## Gamma / lighting looks wrong

- `utilities.gamma.enabled = false` but a utility on (`cave_vision`, `night_vision_boost`,
  `time_based_gamma`) — that is a *floors-only* mode by design: no brightness multiplier, just
  a raised black point. Enable gamma for the multiplier.
- A flat, washed-out world: an additive floor would clip bright pixels to white, so the
  implementation uses `max()` on the block/sky axes instead. If you still see wash, the curve is
  the suspect — the HUD prints the applied curve (`describeCurve()`, e.g. `0.00:0.00,1.00:1.00`
  is identity).
- A bad curve string does not break anything: `GammaApplier` keeps the identity response and
  reports the parse error in the Utilities tab (`getParseError()`). Grammar is
  `x:y,x:y,…`, 2–32 points, x in 0..1; the endpoints are pinned to 0:0 and 1:≥1 so a curve can
  never lift black to grey or clip the brightest light.
- Caves not brightening with a torch in hand: the lift is scaled by how dark the camera's
  surroundings are across *both* light sources, so a held torch halves it rather than zeroing it.

## Config problems

- File: `<game>/aetherium.json`. Comments (`//`, `/* */`) and hand edits are supported.
- Out-of-range numbers are clamped and kept (`"target_fps": 5000` → 1000, the option's max).
  Unparseable values are ignored with `Ignoring value for '…' — '…' is not a …`; the current
  value stays, which is why "my change didn't apply" usually means a typo in the key.
- Unparseable file → copied to `aetherium.json.bad`, defaults in use, original left alone.
  Never deleted, so your edit is still there to compare against.
- Unknown keys are preserved and rewritten, which is what makes a downgrade non-destructive.
- Keys missing from an old file are reported as new options; nothing is "migrated" silently
  except the documented renames in `ConfigStore#migrate` (v1→v3), and the migrated key names are
  listed there in the source.
- `CONFIG_SCHEMA.json` in the repository root is generated from `AetheriumConfig`; if you are
  writing a tool against the format, validate against that, not against a sample file.

## Android / launcher problems

See [ANDROID.md](ANDROID.md) for the full detection table. The short checklist:

- The renderer Aetherium thinks you have is printed at startup: `Android environment:
  launcher=…, renderer=…, heap budget=… MB`. If that line is wrong, the *launcher env* is wrong,
  and `android.force_renderer` in the Android tab is how you override it.
- `custom_env.txt` is read when `android.read_custom_env = true`; only an allow-listed set of
  keys can influence anything (`*_RENDERER`, `MESA_GL_VERSION_OVERRIDE`, `MESA_GLSL_VERSION_OVERRIDE`,
  `POJAVEXEC_EGL`, `GALLIUM_HUD`, `MESA_EXTENSION_OVERRIDE`). A malformed line is skipped, not
  fatal; a file over 256 KB is refused with a warning.
- Stutter every few seconds usually means the persistent arena was refused (translated GL) and
  the fallback path is uploading per frame — that is the safe behaviour, not a bug to configure
  away; `mobile_memory_mode` lowers the per-frame budget to stop GC pauses inside frames.
- Thermal: if `android.thermal_throttle` is on but no zone is readable, the log says
  `no thermal zone was readable; disabling automatic throttling`, and the option goes back off —
  Aetherium will not pretend to throttle.

## Performance is worse than expected

- Compatibility mode is not free: `SHADOW` still runs frame accounting, the lightmap pass and the
  GUI hooks. Roughly 0.01 ms/frame; if that matters, `general.enabled` is the switch, not this mode.
- HZB costs 1–2 % at low entity counts on desktop GPUs and pays off in dense scenes; it is off
  in `batterySaver` on Android.
- If `p99` is bad but the mean is fine, look at the `spikes > 100 ms` figure in the HUD before
  blaming a renderer — one GC pause or a shader compile on first sight of a block is the usual
  cause. Program-binary cache (`performance.program_binary_cache`) removes the recompile case.

## Before you file an issue

Attach, in this order: `latest.log` (first 200 lines and any `Aetherium` line), the crash report
if there is one, `aetherium.json`, and the output of `Aetherium.describeRuntime()` from the
Advanced tab. Say which of the four states you are in and what you expected. Version numbers of
every renderer/shader mod in the pack matter more than your hardware specs, because the majority
of reports are ownership conflicts, not GPU behaviour.

Do not attach a claim that it is "slower than Sodium" without the two `frames.md` files from
`tools/benchmark.sh --mc` ([../BENCHMARK.md](../BENCHMARK.md)) — that is the only comparison this
project considers evidence.
