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
| Renderer named "Aetherium renderer", on by default | done | done | done | done | done | done | done | `advanced.aetherium_renderer`; no "Experimental" label anywhere |
| Even frame pacing | done | done | done | done | done | done | done | `pacing`: `RenderSystem.limitDisplayFPS` to 1.21.11; 26.x `FramerateLimiter` |
| Zoom (Zume-style, 3 keys) | done | done | done | done | done | done | done | `zoom`, `keys`, `zoom-fov`, `zoom-cam`, `options` |
| Freelook | done | done | done | done | done | done | done | `freelook`, `rot`; key via `ZoomKeys.newKey` |
| Fog controls | done | done | done | done | done | done | done | `fog`: setupFog TAIL (shader fog to 1.21.1); `FogParameters` return; 1.21.6+ wrap of the fog-buffer write (env / render-distance ranges) |
| Sky / sun-moon / stars / cloud height | done | done | done | done | done | done | done | `sky`: renderSky draws (popNextBuffer / RenderedBuffer.release / MeshData.close); 1.21.2+ `SkyRenderer` methods; `clouds`: 1.21.2+ `CloudRenderer.render` height arg |
| Per-type particles | done | done | done | done | done | done | done | `createParticle` on all; `block-particles`: ParticleEngine destroy/crack, 1.21.9+ ClientLevel effects; `registry` |
| Packet budget, stutter log | done | done | done | done | done | done | done | `runTick -> runAllTasks` on all; `BlockableEventLoop.pollTask` |
| Mesh threads | done | done | done | done | done | done | done | `mesh-exec`: dispatcher constructor executor (`allChanged`; 26.2+ `invalidateCompiledGeometry`), 1.21.2+ wrapped in `TracingExecutor` |
| Allocation-free meshing | done | done | done | done | done (to 1.21.4) | - | - | `mesh-alloc`: AO `calculate` BitSet / MutableBlockPos; 1.21.5+ meshes through per-block render storage (not handled) |
| Rebuild coalescing | done | done | done | done | done | done | 26.1 | `section-api`: `isDirty()` in setupRender (1.17) / compileChunks / compileSections; 26.2+ has no section dirty flag |
| Threaded near builds | done | done | vanilla | vanilla | vanilla | vanilla | vanilla | `near-build`: 1.18+ builds near sections on the workers by default |
| Fog cull (smart culling) | done | done | done | done | done | done | 26.1 | `fog-cull`: visible list filtered after `setupRender` (1.17) / `applyFrustum`; cylinder distance against the terrain fog recorded by the fog hooks |
| Visibility-search throttle (smart culling) | done | done | vanilla | vanilla | vanilla | vanilla | vanilla | `search-throttle`: 1.17 `setupRender` as 1.16.5 (needsUpdate PUTFIELD ordinals 1/2); 1.18+ searches on a background thread |
| Fast particles | done | done | done | done | done | done (to 1.21.8) | - | `particle-fast`, `frustum-cap`; 1.21.9+ extracts particle render states |
| Visible-only animations | done | done | done | done | 1.21.4 | — | — | `anim` / `atlas-tick` / `section-compile`: per-atlas ticker→sprite map wraps `Tickable.tick` (1.17) / `Ticker.tickAndUpload` (1.19.3); sprites collected per build, taken by the CompiledChunk/CompiledSection created after it (1.19+); `VisibleSpritesMixin` marks the visible list once per atlas tick |
| Fast models | done | done | done | 1.20.6 | — | — | — | `model-fast` / `math` / `pose-normals`: ModelPart.compile written straight into the old ByteBuffer BufferBuilder (enum mode from 1.17, joml from 1.19.3, normals normalised for untrusted poses on 1.20.5-1.20.6); 1.21's BufferBuilder is already direct |
| Aetherium renderer (terrain pipeline) | done | done | done | done | done | done | done (26.3: no skip) | `section-cull` / `layer-sig` / `section-api`: SectionCull sorts solid + cutout meshes by face group on upload (popNextBuffer to 1.18.2, uploadVertexBuffer 1.19-1.21.4) and wraps `RenderSystem.drawElements` in the section draw with one `_drawElements` range (or a skip); falls back to vanilla on error. 1.21.5+ (1.8.1, era `cull-new`): `SectionCullSortMixin` sorts in `SectionCompiler.compile` (32-byte vertices with normals to 1.21.11; 28-byte vertices without normals on 26.x, grouped by winding), layouts ride on `MeshData` → `SectionBuffers` at upload (1.21.5) or `CompiledSectionMesh` (1.21.6+), and `SectionCullDrawMixin` narrows the `RenderPass$Draw` record (26.3: `DynamicGpuData$IndexedDraw`; skips are dropped from the list except on 26.3). Batched terrain (shared buffers) stays 1.16.5-only |

Scenery settings: the 1.21.6+ distance settings only move the render-distance fog range, so they
never lift blindness, darkness or Nether fog (those live in the environmental range).

## Order

1. Zoom, freelook, frame pacing (simple hooks).
2. Scenery: fog, sky, particles.
3. Streaming: packet budget, then mesh threads, then coalescing and visibility, then near builds.
4. 1.7.0 features.
5. Aetherium renderer per group (G1 first).
6. Flip the capability flags, drop `.github/ship-only`, bump to 1.8.0, ship all 33.
