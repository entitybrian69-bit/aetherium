# Aetherium

Purple-themed rendering-engine replacement for Minecraft (Fabric + NeoForge, MultiLoader layout) with:

- **AetheriumRenderEngine** — backend abstraction that probes the GL context and picks OpenGL 4.6 DSA ("modern"),
  GL 3.3 core, GL 2.1 legacy, or Vulkan 1.3 (experimental device bring-up; falls back to GL automatically).
- **GL modern path** — persistent mapped triple-buffered vertex/command arenas, `glMultiDrawElementsIndirect(Count)`
  batching, Hierarchical-Z occlusion culling with compute-shader command compaction, driver-threaded shader
  compilation + on-disk program-binary cache.
- **AetheriumChunkBuilder** — distance-prioritised, de-duplicating async section scheduler injected into vanilla's
  `SectionRenderDispatcher`, with a per-frame upload budget and thread count that follows config / mobile memory
  mode / thermal state.
- **Dynamic lights** — entity/item sources, 16³ spatial hash, coloured sampling, dirty-section tracking, injected via
  `LevelRenderer.getLightColor`.
- **Gamma utilities** — brightness to 1000 %, night vision, cave vision, time-based gamma, 5-point monotone gamma
  curve editor; applied by rewriting the lightmap before upload (no `Options.gamma` hacks).
- **Iris / Oculus** — reflection-bound API bridge (pack-in-use, shader toggle, pack-browser screen).
- **Purple Video Settings GUI** — replaces `VideoSettingsScreen`: sidebar categories, animated morph-in, breathing
  panel, glow particles, touch-friendly 48 dp rows on Android, every toggle live.
- **Android** — detects PojavLauncher / Zalith / Fold Craft / Amethyst / DroidBridge and GL4ES / Zink / LTW /
  MobileGlues, reads `custom_env.txt`, battery saver, thermal throttle protection from sysfs.
- **Conflict detection** — Sodium / Embeddium / VulkanMod / OptiFine / Nvidium force Compatibility Mode and are
  declared `breaks`/`incompatible` in loader metadata.

## Layout

```
common/    version-agnostic core + 1.21.1 client glue (mixins, GUI)
fabric/    Fabric Loader 0.16.10 entrypoint, jar-in-jar lwjgl-vulkan
neoforge/  NeoForge 21.1.77 entrypoint, jarJar lwjgl-vulkan
docs/      CONFIG_SCHEMA.json, PORTING_MATRIX.md (all 33 versions, per-version deltas)
```

Build: `./gradlew :fabric:build :neoforge:build` (JDK 21).

## Status — read this

| Area | State |
|---|---|
| Reference target | **1.21.1** Fabric + NeoForge, Mojang mappings |
| Compilation | **Not yet verified** — the authoring environment had no JDK and no network. The code is written against the 1.21.1 API from memory; expect to fix a handful of signature mismatches on first build. |
| GL modern path | Infrastructure complete (arena, MDI, HZB, shader cache). The terrain draw itself still flows through vanilla's `SectionRenderDispatcher`; wiring vanilla section meshes into the indirect arena is the next milestone. |
| Vulkan | Instance/device/queues only. No swapchain (needs SDL3 window in 26.1+ or a GL↔VK interop path). |
| Coloured lights | Sampled and exposed (`DynamicLightEngine.sampleColor`); the vanilla lightmap is scalar, so colour is consumed only by the shader path. |
| Performance claims | None made. Measure with the built-in frame-time graph (`Advanced → Frame Time Graph`). |
| Other 32 versions | Documented, not implemented: see `docs/PORTING_MATRIX.md` for the exact per-version delta list. |

License: LGPL-3.0.
