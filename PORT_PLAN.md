# Aetherium 1.8.0: porting the 1.16.5-only features to the other 32 versions

Goal (user, Phase 14): every feature that so far only works on Minecraft 1.16.5 ships on all 33
versions in **one release**. That includes the Aetherium renderer, which is no longer experimental
and is **on by default**. Work goes oldest first. Until the release, work-in-progress commits carry
`[skip ci]` so `ship.yml` does not overwrite the published 1.7.2 jars.

Every hook below was read from the javap dumps in `tools/probe/` (`/tmp/sig.py`-style queries); a
variant is only enabled once its targets are confirmed there and `stubcheck --all` compiles it.

## Era groups

| group | versions | notes |
|---|---|---|
| G0 | 1.16.5 | reference for all features (fixed-function GL, gl4es) |
| G1 | 1.17 - 1.17.1 | shaders (core profile), Java 16, `RenderChunkInfo` list |
| G2 | 1.18 - 1.20.1 | Java 17, `Options` instances from 1.19, joml from 1.19.3 |
| G3 | 1.20.2 - 1.21.1 | `SectionRenderDispatcher`, `SectionOcclusionGraph`, Java 21 from 1.20.5 |
| G4 | 1.21.2 - 1.21.5 | frame graph passes, `FogParameters`, `getFov` returns float |
| G5 | 1.21.6 - 1.21.11 | `fog.FogRenderer` + GPU buffers, `KeyMapping$Category` from 1.21.9 |
| G6 | 26.1 - 26.3 | render-state extraction, no `limitDisplayFPS`, Camera owns the FOV |

## Status

| feature | G0 | G1 | G2 | G3 | G4 | G5 | G6 | eras / notes |
|---|---|---|---|---|---|---|---|---|
| Renderer named "Aetherium renderer", on by default | done | | | | | | | `advanced.aetherium_renderer` |
| Even frame pacing | done | done | done | done | done | done | probe | `pacing`: `RenderSystem.limitDisplayFPS` to 1.21.11; 26.x `FramerateLimitTracker` |
| Zoom (Zume-style, 3 keys) | done | done | done | done | done | 1.21.9+ probe | probe | `zoom`, `keys`, `zoom-fov`, `zoom-cam`, `options` |
| Freelook | done | done | done | done | done | 1.21.9+ probe | done* | `freelook`, `rot`; key via `ZoomKeys.newKey` |
| Fog controls | done | | | | | | | `setupFog` TAIL; 1.17+ shader fog start/end; 1.21.2 `FogParameters`; 1.21.6 fog environments |
| Sky / sun-moon / stars / cloud height | done | | | | | | | 1.17 `renderSky` (Matrix4f); 1.21.2 sky pass; 1.21.6 `SkyRenderer` |
| Per-type particles | done | | | | | | | `createParticle` on all; block break/crack moved in 1.21.9 |
| Packet budget | done | | | | | | | `runTick -> runAllTasks` on all; `BlockableEventLoop.pollTask` (probe) |
| Mesh threads, allocation-free mesh | done | | | | | | | dispatcher constructor executor per group |
| Rebuild coalescing, visibility throttle, fog cull | done | | | | | | | per-group `setupRender` / occlusion graph rewrite |
| Threaded near builds | done | | | | | | | |
| 1.7.0: smart culling, fast particles, visible-only animations, fast models | done | | | | | | | |
| Aetherium renderer (terrain pipeline) | done | | | | | | | shader-era rewrite per group, last |

`*` the 26.x freelook hooks are written (`Camera.alignWithEntity`) but they need the 1.21.9+ key
category.

## Order

1. Zoom, freelook, frame pacing (simple hooks).
2. Scenery: fog, sky, particles.
3. Streaming: packet budget, then mesh threads, then coalescing and visibility, then near builds.
4. 1.7.0 features.
5. Aetherium renderer per group (G1 first).
6. Flip the capability flags, drop `.github/ship-only`, bump to 1.8.0, ship all 33.
