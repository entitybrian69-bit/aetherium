# Benchmarking Aetherium

This repository ships measurement tooling instead of performance claims. There is no number
in this file that was not produced by the commands below, and the commands produce numbers
only for the machine that runs them.

## The two harnesses

| | `tools/benchmark.sh` (default) | `tools/benchmark.sh --mc` |
| --- | --- | --- |
| Needs | JDK 21 + Gradle, no GPU, no game | a built Fabric jar, a GPU, a display (or xvfb), and `-Paetherium.enableRunConfigs=true` |
| Measures | our own hot-path functions on the CPU | real frame times inside the running game |
| Writes | `benchmark-out/cpu.md` | `benchmark-out/frames.md` |
| Proves | that a loop got faster or slower, on this machine | what this build does on this hardware, this seed, this render distance |
| Does not prove | frame rate, GPU cost, driver behaviour | anything about another mod, unless you run that mod too |

```sh
sh tools/benchmark.sh                      # CPU microbenchmarks
sh tools/benchmark.sh --report             # print the recorded frames.md rows
sh tools/benchmark.sh --mc --runs 5 --seconds 60 --seed 12345 \
    --world-type superflat --mod /path/to/sodium-fabric-0.6.x.jar
```

## CPU harness

`common/src/test/java/com/aetherium/bench/CpuMicroBenchmarkTest.java` — ordinary JUnit 5
tests guarded by `-Daetherium.bench=true`, so a normal `./gradlew build` skips them and a CI
box without a GPU can still run them. The harness (`BenchmarkHarness` in the same package)
warms up twice, then times a loop whose result is accumulated into a checksum so the JIT
cannot delete the work.

What is measured, and why that function matters:

| measurement | the code under test | why it is on a hot path |
| --- | --- | --- |
| `packRgb` / `channelUnpack` | `MathUtil` | 768 channel reads and 256 writes per lightmap update |
| `json.write` / `json.parse` | `config/Json` | the whole config file, at startup and on every save |
| `FrameStats.record` | `hud/FrameStats` | once per rendered frame, forever, including in Compatibility mode |
| `FrameStats.percentile` | same | the HUD refresh and the benchmark interval boundary |
| `GammaCurve.evaluate` | `gamma/GammaApplier` | once per channel per lightmap pixel |
| `MeshCounters` increments | `render/mesh/MeshCounters` | once per `setSectionDirty`, i.e. thousands during a piston storm |

The pass criterion is "the measurement ran and the numbers are physically plausible", never
"the code is faster than X". Timing tests that assert speed are flaky, and a flaky test gets
ignored within a week.

Regressions: `ns/op` is the column to diff between two `cpu.md` files on the same machine.
A container moves ±20%; treat anything smaller as noise and say so in the commit message
rather than claiming a win.

## In-game harness

`--mc` copies the built Fabric jar into `benchmark-out/game/mods`, pre-seeds
`config/aetherium.json` and `options.txt` (render distance 12, particles minimal, AO on,
vsync off, maxFps 260), and launches with:

```
-Daetherium.benchmark=<out> -Daetherium.benchmark.seconds=<--seconds>
-Daetherium.benchmark.label=<label>
```

`com.aetherium.hud.BenchmarkRecorder` then appends one row per interval to
`benchmark-out/frames.md`, produced by `FrameStats#formatMarkdownRow`:

```
| measurement | fps | p50 ms | p99 ms | p99.9 ms | longest ms | spikes > 100 ms |
```

The label column carries the backend name, render distance, simulation distance and the
world seed, because a row without those four is not reproducible. Recording is *only* ever
on with the property set; a normal launch opens no file and adds nothing to the frame loop
(`BenchmarkRecorderTest` asserts exactly that, in both directions).

Compare runs by swapping jars between passes and keeping everything else identical — never by
flipping Aetherium's Compatibility toggle inside one run: Compatibility mode still performs
its own frame accounting, which is measurable and not free.

## What belongs in a report

Append to `frames.md` (or copy the rows into a document): CPU model, core count, GPU, driver
version, Java version, JVM args, Minecraft version, mod list with versions, and whether the
run was on a laptop with a battery cap. A frame-time row without that block is an anecdote.

Percentiles, not the mean: `p99` and "spikes > 100 ms" describe what a player calls a stutter.
Two runs with identical average FPS can differ by 5× in the spike column.

## What this repository will not do

It will not print "2× Sodium", "30% faster than vanilla" or any other ratio. `tools/benchmark.sh`
is written so that no code path in it can produce a ratio at all: it emits measured medians per
run, and `--report` prints them side by side for a human to compare. A ratio requires someone to
run both mods on the same machine, in the same world, on the same day — and then the sentence
belongs in the pull request that did it, with the two `frames.md` files attached.

If you find a claim about this mod's performance anywhere, including in an issue thread, the
question to ask is which `frames.md` it came from.
