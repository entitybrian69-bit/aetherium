# Aetherium

A client-side performance and video-settings mod for Minecraft **1.16.5 → 26.3** (33 versions) on
**Fabric** and **NeoForge**. It replaces the vanilla *Video Settings* screen with its own animated,
tabbed screen. It also adds frame-rate features that work *with* the vanilla renderer instead of
replacing it.

Version **1.5.5** removes the pipeline's shader mode and the **Backend > Pipeline shader** option.
On GL4ES, shader mode kept crashing in the graphics driver after each fix (1.5.2, 1.5.3, 1.5.4), and
that can't be tested without the device. The Aetherium pipeline now always draws in compatible mode:
vanilla's vertex format and vanilla's own GL calls, with faces pointing away from you skipped. This
was the mode reported working. An `advanced.pipeline_shader` key in an existing config file is kept
but ignored. A crash marker left by shader mode in an older version no longer turns the pipeline off.
The shader code is still in the source but is never selected.

Version **1.5.4** fixes a "Rendering chunk" crash (`IndexOutOfBoundsException` in
`BufferBuilder.popNextBuffer`) with the experimental pipeline on 1.16.5. On the first tick in a
world, Aetherium threw away the chunk meshes its workers had already converted while their uploads
were still queued. Those uploads then went back to builders that were already empty. Converted meshes
are now kept until each upload uses them (or until the chunk dispatcher is disposed). As a backstop,
an upload that finds an empty builder now leaves that section empty until its next rebuild instead
of crashing, and a conversion that fails (for example out of direct memory) no longer leaves its
upload without a record.

Version **1.5.3** fixes a native crash on GL4ES when the pipeline's shader mode drew chunk
sections that were not in the compact format yet (for example right after switching from compatible
mode): those sections now go through vanilla's own GL path instead of the shader. Version **1.5.2** recognises NG-GL4ES forks such as Krypton Wrapper, never reports a memory
budget larger than the heap, and keeps fewer spare direct buffers on small heaps and 32-bit
devices (1/16 of the heap, 8 to 48 MB; at most 16 MB on 32-bit). Version **1.5.1** fixes a crash in 1.5.0 when holding a block after editing terrain with the
Aetherium pipeline on (see [1.5.1](#151-held-block-crash-fix-1165)). Version **1.5.0** (1.16.5 first) attacks the stutter and frame-rate drop when you walk into
unloaded chunks, and adds **fog controls**, **sky toggles**, **per-type particle switches** and
**freelook** (see [1.5.0: smooth chunk streaming, fog, sky, particles, freelook](#150-smooth-chunk-streaming-fog-sky-particles-freelook-1165)).
Version **1.4.0** (1.16.5 first) adds a **Zume-style zoom** with rebindable Zoom / Zoom In / Zoom Out
keys, makes **dynamic lights** switch on live (they used to need a restart, which looked like they
did not work), and removes the biggest stutter of 1.16.5: chunks near you were rebuilt on the render
thread (see [1.4.0: zoom, live dynamic lights, no render-thread chunk builds](#140-zoom-live-dynamic-lights-no-render-thread-chunk-builds-1165)).
Version **1.3.4** fixes the actual cause of the 1.16.5 pipeline crash on GL4ES / LTW, found in the
crash's hs_err file (see [1.3.4: the crash, found](#134-the-crash-found-1165)). Version **1.3.3** added a **compatible mode** to the 1.16.5 pipeline for GL translators that crash on
its shader, and switches to it automatically after such a crash (see
[1.3.3: compatible mode](#133-compatible-mode-for-the-pipeline-1165)). Version **1.3.2** removed the
index buffer from the 1.16.5 pipeline (see
[1.3.2: no index buffer](#132-no-index-buffer-no-fixed-function-queries-1165)). Version **1.3.1** made the pipeline safe to try after a crash report on 1.16.5 (see
[1.3.1: crash guard](#131-crash-guard-for-the-pipeline)). Version **1.3.0** turned the experimental 1.16.5 chunk renderer into **Aetherium's own terrain
pipeline**: a compact 16-byte vertex format, chunk meshes grouped by face direction, and back-face
groups that are never drawn (see [1.3.0: the Aetherium pipeline](#130-the-aetherium-pipeline-1165)).
It is still 1.16.5 only and off by default. Version 1.2.0 added **entity occlusion culling** on all
33 versions and the first experimental chunk renderer (see
[1.2.0: occlusion culling and the experimental chunk renderer](#120-occlusion-culling-and-the-experimental-chunk-renderer)).
Version 1.1.0 fixed the frame-rate drops reported on 1.0.0 (see
[1.1.0: the frame-rate fixes](#110-the-frame-rate-fixes)) and removed the shader-mod page and
integration. Nothing on vanilla's chunk-meshing path allocates any more. The hooks that stay
installed cost a flag check, or a distance compare for culling, per call. 1.0.0
added smooth chunk loading, an animated-texture switch, a block-entity distance, worker-thread
sizing, a light/dark theme, a working scrollbar, touch scrolling and interface sounds. Every feature
either removes work vanilla would have done or changes a vanilla setting. Aetherium makes no
OpenGL calls of its own unless you turn on the experimental 1.16.5 Aetherium pipeline.

## 1.5.1: held-block crash fix (1.16.5)

1.5.0 could crash with `IndexOutOfBoundsException: Index 1 out of bounds for length 1` in the
held-item renderer. In 1.16.5 the chunk sections rebuilt on the render thread (your own block
edits) are built in the same vertex builders that entities and the item in your hand draw with.
Each mesh must leave those builders through exactly one upload, in order. 1.5.0 took those meshes
out early for the pipeline, and kept only one per builder. When one edit rebuilt several sections
in the same frame, a later upload read the builder a second time. That failed read left the
builder's read position past its end, and the next held block drawn with it crashed.

- Render-thread meshes are no longer touched. They take vanilla's in-order upload path, and that
  frame's upload queue is drained in full, so the shared builders are empty before entities and
  your hand are drawn. This also closes an older gap where a delayed upload could show a chunk
  mesh in place of the held item for one frame.
- Worker-thread meshes are kept oldest first, per builder, and none is ever dropped.

## 1.5.0: smooth chunk streaming, fog, sky, particles, freelook (1.16.5)

Built for 1.16.5 only for now (`.github/ship-only`); the other versions follow.

**Walking into unloaded chunks.** Five separate costs hit the same frames when new terrain
arrives. Each one now has its own fix:

- **Packets get a time budget** (Performance > *Smooth streaming*, on). Vanilla runs every queued
  packet at the start of a frame. A burst of chunk packets takes 50 to 150 ms on a phone, all inside
  one frame. Now at least 8 packets run per frame, then more until 4 ms are used. While the queue
  keeps outlasting the budget, the budget grows by 2 ms per frame, up to 14 ms, so the backlog can
  never grow without bound. It drops back to 4 ms once the queue is empty, so a packet waits a few
  frames at most.
- **One rebuild per section instead of several** (Performance > *Coalesce rebuilds*, on). When a
  chunk arrives, vanilla marks its sections and all neighbouring sections dirty. The neighbours'
  own chunk and light packets then mark them again, so edge sections were meshed and uploaded
  several times within a second. A section now waits until it has been quiet for 120 ms (never
  more than 600 ms), then builds once. Two kinds are never delayed: your own block edits, and
  already-built sections within 28 blocks of you.
- **Mesh threads of their own.** In 1.16.5 the chunk mesher shares Minecraft's background pool with
  world generation, lighting and saving, so meshes queued behind generation and arrived in bursts.
  It now has a small dedicated pool: 2 threads on phones (3 with 8+ cores), cores/3 (2 to 6) on
  desktop. The change applies from the next world you join.
- **Uploads prepared off the render thread** (Aetherium pipeline). The pipeline used to re-encode
  every arriving mesh on the render thread before uploading it. The worker thread that built the
  mesh now encodes it into a pooled direct buffer, and the render thread only calls
  `glBufferData`. Pooling also stops the garbage-collector pauses from one buffer per upload.
- **Less per-frame overhead while streaming.** The chunk upload budget follows your frame time
  (12 % of it, 1.5 to 4 ms). The visible-section search that vanilla reruns after every frame with
  an upload now runs at most 20 times a second while uploads stream in. Camera movement still
  updates it immediately.
- **Stutter log** (Backend > *Stutter log*, off). This writes every frame that takes at least
  45 ms and 2.5 × the average to `logs/aetherium-stutter.log`. Each line records what that frame
  did: packets run, backlog pressure, uploads, prepared uploads, light updates and builds finished.
  Send it if a hitch remains.

**New options** (fog, sky and particles on the Quality tab; freelook on the Effects tab):

- **Fog:** *Distance fog* on/off; *Fog start* (percent of render distance, 75 = vanilla);
  *Water fog* (percent, 100 = vanilla, 0 = clear water); *Clear lava*. Blindness always keeps its
  vanilla fog.
- **Sky:** *Sky* (off = flat sky colour, the cheapest), *Sun and moon*, *Stars*, *Cloud height*
  (0 = the dimension's default, 128 in the Overworld).
- **Particles**, each type on its own: explosions, weather (rain splashes), smoke, potions, combat
  (crits, damage, sweep), fire and lava, drips, portals, ambient (spores, ash, underwater), totem,
  and block breaking. Turned-off particles are never created, so they cost nothing.
- **Freelook:** hold **Left Alt** (rebind in Options > Controls > Aetherium) to look around
  without turning your player. Movement keeps its direction, and the view snaps back when you let
  go. Options: *Toggle* (press once, press again to stop) and *Third person* (on: switches to the
  third-person camera while freelooking from first person).

## 1.4.0: zoom, live dynamic lights, no render-thread chunk builds (1.16.5)

Built for 1.16.5 only for now (`.github/ship-only`); the other versions follow.

- **Threaded chunk builder (the stutter fix).** Vanilla 1.16.5 rebuilds every changed chunk section
  within about 28 blocks of the camera *synchronously on the render thread* ("build near"), not only
  the ones you edited. Chunks arriving around you, light updates, water and redstone next to you,
  and every step with a torch in hand each froze the frame for a full section mesh, often a dozen
  at once. Now only your own block edits stay synchronous (they still appear instantly); everything
  else goes to the worker threads, nearest first, like vanilla 1.18's *Threaded* chunk builder.
  Performance > *Threaded chunk builder*, on by default.
- **Client light updates are budgeted.** `renderLevel` ran every pending light update in one go. It now
  runs them in slices for at most 2 ms per frame and continues next frame. Incoming light data is
  always applied in full; only propagation is spread.
- **Faster terrain shader** (Aetherium pipeline). The solid layer has its own program without
  `discard`, so mobile GPUs keep their hidden-surface removal (Mali's Forward Pixel Kill, early depth
  test) for most of the screen. Fog is computed per vertex, like vanilla's fixed-function fog, which
  leaves one `mix` per pixel.
- **Dynamic lights work live.** The light hooks used to be installed only if dynamic lights were
  already on when the game started, so turning them on in the screen did nothing until a restart.
  They are now MixinExtras return-value modifiers that allocate nothing and cost one volatile read
  while no light source exists, so they are always installed and the setting applies immediately.
- **Zoom, the Zume way** (Effects tab, keys in Options > Controls > Aetherium):
  - Hold **Zoom** (Z) to zoom smoothly to about 18° from 70°, OptiFine's strength.
  - While zooming, the **scroll wheel** or **Zoom In** (=) / **Zoom Out** (-) change the zoom, down
    to 1°.
  - Mouse sensitivity drops with the zoom (never below 40 %), with the cinematic camera on.
  - In **third person** the same keys move the camera between 0.5 and 15 blocks instead. It toggles
    by default.
  - The curves, timings and defaults follow Zume's documented behaviour; the code is Aetherium's
    own. All of it is adjustable: strength, toggle mode, cinematic camera, scroll zoom, speed,
    animation time, sensitivity floor, minimum FOV, third-person distance and toggle.

## 1.3.4: the crash, found (1.16.5)

1.3.3's step-by-step trace plus the hs_err file pinned the crash down. The last trace line was
`compact section: buffer 23124; glDrawArrays(GL_TRIANGLES, 0, 1056)`. The native stack was
`gl4es_glDrawArrays → arrays_to_renderlist → copy_gl_pointer_raw → copy_gl_array`, a SIGSEGV
reading an unsigned short at address `0x18`. The renderer string was `GL4ES using Mali-G57`,
`2.1 gl4es wrapper 1.1.7` (LTW's legacy renderer is GL4ES).

From gl4es's source: generic attribute 0 is also its fixed-function vertex position. When that
attribute's type is not one of `UNSIGNED_BYTE / BYTE / FIXED / FLOAT / SHORT` (`valid_vertex_type`),
gl4es converts all arrays to floats on the CPU. For a generic attribute sourced from a VBO, that
conversion reads from the bare buffer offset, which is a null-based address. The compact position
attribute was `GL_UNSIGNED_SHORT` from 1.3.0 to 1.3.3: every pipeline crash on this device.

- **The position/light attribute is now `GL_SHORT`.** Values are stored minus 32768 and the shader
  adds it back, so the format, precision and size (16-byte vertices) are unchanged. A unit test
  keeps attribute 0's type within gl4es's accepted list.
- **Crash markers carry a fix level.** A shader-mode marker from before 1.3.4 (the bug above) keeps
  shader mode for one more try instead of switching to compatible mode. If 1.3.4 crashes in shader
  mode, the next start uses compatible mode as in 1.3.3.

## 1.3.3: compatible mode for the pipeline (1.16.5)

1.3.2 still crashed on LTW at the first terrain draw, so the index buffer was not the (only) cause.
What is left between `pipeline ready` and the crash is the pipeline's own GLSL 1.20 program and its
generic vertex attributes, which LTW translates to GL 3.3 core. 1.3.3 adds a mode that has neither.

- **Backend > Pipeline shader** (new, on by default; removed in 1.5.5). On: shader mode, as in 1.3.2. Off:
  **compatible mode**. Chunks keep vanilla's 32-byte vertices and are drawn with vanilla's own calls
  (`VertexBuffer.bind`, `DefaultVertexFormat.BLOCK.setupBufferState`, the matrix push/load/mult/pop,
  `RenderSystem.drawArrays(GL_QUADS)`), in the same order vanilla makes them. The only change is
  that each section's quads are sorted by face direction at upload, so the groups facing away from
  the camera are not drawn. No shader, no generic attributes, no new GL features. It runs wherever
  vanilla runs.
- **The crash guard steps down instead of giving up.** The marker file now records the mode. A
  crash in shader mode (or a marker left by 1.3.1/1.3.2) switches to compatible mode at the next
  start and keeps the pipeline on. A crash in compatible mode turns the pipeline off. A Java-side
  shader failure (compile, link, GL error) switches to compatible mode during the session.
- **Diagnostics.** The log names the GL implementation
  (`Aetherium pipeline GL: renderer ..., version ..., GLSL ...`). In shader mode, the first draws
  log each GL step (`Aetherium pipeline trace: ...`), so the last line before a native crash names
  the call. Compatible mode logs `first compatible-mode terrain layer drawn`.

## 1.3.2: no index buffer, no fixed-function queries (1.16.5)

The 1.3.1 milestone log narrowed the crash down on LTW (Android, GL 3.3 core translator): the
pipeline switched on, the first compact chunk was uploaded, the shaders compiled, and the process
died before the first terrain layer finished. That is the first draw call. 1.3.0/1.3.1 drew with
`glDrawElements` from a shared index buffer. A translator that does not track that binding reads the
offset as a memory address and crashes natively. Vanilla never uses an index buffer for terrain.

- **Compact sections are plain triangles** (two per quad, vertices 0-1-2 and 2-3-0) drawn with
  `glDrawArrays(GL_TRIANGLES)`. No element buffer is created or bound anywhere. That is 96 bytes per
  quad, a quarter less than vanilla's 128 (1.3.0's 64 needed the index buffer). Face-group skipping
  and run merging are unchanged.
- **Buffers that stay in vanilla's layout** are drawn with `glDrawArrays(GL_QUADS)`, the exact call
  vanilla uses for them.
- **Fog comes from vanilla's own `GlStateManager` calls** (`_enableFog`, `_fogMode`, `_fogStart`,
  `_fogEnd`, `_fogDensity`, `_fog`), recorded by `FogStateMixin`. Together with 1.3.1's projection
  change, the pipeline no longer queries any fixed-function state from GL.
- If 1.3.1's crash guard switched the pipeline off after the last crash, turn it back on.

## 1.3.1: crash guard for the pipeline

A tester's game closed on 1.16.5 (Android, a GL translator) right after the log line
`Aetherium pipeline ready`. There was no Java exception, so the crash was inside the graphics
driver or translator, where Java code cannot catch it. 1.3.1 does not claim to fix that crash,
because its cause is not known yet. It makes sure it cannot repeat on its own and that the next
log shows how far the pipeline got:

- **Crash guard.** While the pipeline draws, a marker file `config/aetherium-pipeline.running`
  exists. A normal exit, switching the option off, or a caught failure removes it. If the game
  starts and the marker is still there, the pipeline is switched off in the config and the Backend
  tab's *Chunk renderer* row says so. Turn it back on to try again. Closing the app from the task
  switcher also counts as an unexpected exit.
- **No fixed-function queries for the projection.** The projection matrix now comes from the
  argument vanilla passes to `LevelRenderer.renderLevel`, not from `glGetFloatv(GL_PROJECTION_MATRIX)`,
  which GL translators may not track.
- **GL error check during the first frames.** For the first 8 layers that draw something, after
  the switch-on, and again after the first compact upload, Aetherium checks `glGetError`. Any error
  turns the pipeline off and vanilla draws (one rebuild).
- **Milestones in the log:** pipeline on/off with rebuild, the first compact chunk upload (with its
  size against vanilla's), and the first compact terrain layer drawn without a GL error.
- Removed the `_comment` entry from `fabric.mod.json` that Fabric Loader warned about.

## 1.3.0: the Aetherium pipeline (1.16.5)

The setting is *Aetherium pipeline (experimental)* on the Backend tab (config key
`advanced.experimental_chunk_renderer`, unchanged). 1.2.0 drew vanilla's own vertex buffers with
a shader. 1.3.0 also owns how terrain is **stored** and **which faces are sent to the GPU**:

- **Compact vertices, 16 bytes instead of 32.** When a chunk section's solid, cutout-mipped or
  cutout layer is uploaded, Aetherium re-encodes it. Positions become three unsigned 16-bit values
  (1/1024-block steps, exact on the 1/16 grid block models use). Block and sky light are packed into
  a fourth 16-bit value. Colour stays 4 bytes and texture coordinates become two 16-bit values. The
  normal is dropped (the 1.16.5 terrain path never reads it). Video memory and vertex bandwidth for
  terrain are halved.
- **Face-direction groups.** Each section's quads are sorted into seven groups: up, down, north,
  south, east, west, and *any* for everything else. A quad joins a direction group only if it is
  flat on that axis and wound to face that way. Diagonal plants and fluids' back-to-back faces
  (which vanilla labels with misleading normals) go to *any*.
- **Back-face groups are skipped before the GPU sees them.** Per section, per frame, a direction
  group is drawn only if the camera is in front of the group's outermost face plane, with a 1/16
  block margin. Looking at a hillside from above skips every down-facing quad. Visible groups become
  one to three index ranges. Ranges separated by fewer than 256 hidden quads are merged, because an
  extra draw call costs more than shading a few culled back faces, especially on GL4ES. Sections
  over 16 384 quads are split as before. The *Chunk renderer* row on the Backend tab shows the share
  of faces skipped.
- **Safe fallbacks.** Data that does not fit the format exactly (anything outside the section's
  range, out-of-range light or texture coordinates) is uploaded unchanged, and drawn by the 1.2.0
  shader. Switching the pipeline on rebuilds the loaded chunks once, so the whole view converts at
  once. Switching it off, or any failure, also rebuilds them once, because vanilla cannot read
  compact buffers. Until that rebuild, those layers are skipped for at most one frame rather than
  drawn wrong. Translucent blocks and tripwire stay on vanilla's path (they are re-sorted).
- **Not tested on a GPU.** The encoder, classification, culling planes, range merging, shader
  decode and switching rules are covered by unit tests, and the hooks were checked against the
  bytecode CI extracted from the 1.16.5 jar. Nothing here has run in a real game yet. It stays
  1.16.5 only until someone confirms it works there. Porting it to 1.17+ means new hooks for each
  of those renderer generations.

## 1.2.0: occlusion culling and the experimental chunk renderer

**Entity occlusion culling (all 33 versions, on by default).** Every frame, vanilla works out which
16×16×16 sections of the world are visible: its occlusion graph walks from the camera through open
section faces, then the frustum is applied. Only those sections get their terrain drawn. Entities
never used that list. Vanilla only checks them against the frustum (1.21.11+ also waits for a
section's fade-in), so mobs in caves under you, behind hills or inside other buildings were still
animated and drawn. Aetherium now copies vanilla's list into a bit grid around the camera once per
frame (about 38 KB cleared, one bit per visible section). It then skips an entity when every
section its culling box touches is missing from the list. This is the same idea Sodium uses,
written independently (Sodium's current code is not open source).

- It only reads vanilla's list and never hides a section vanilla would draw. Anything uncertain
  counts as visible: sections outside the grid, entities above or below every visible section,
  boxes spanning 4+ sections, and frames before the world has a list.
- These are never hidden: other players (their name tags show through walls), entities with a
  visible name, glowing entities, the camera entity, and anything whose renderer opts out of
  culling. Renderers that extend the visibility test, such as leashes and guardian beams, still run
  their own checks.
- Cost: the grid is built once per frame and is one bit lookup per entity. The exemption checks run
  only for entities that would be hidden.
- The setting is *Hide entities behind walls* on the Performance tab. If reading vanilla's list
  ever fails, it turns itself off for the session and logs why. With Sodium or Embeddium installed
  it is handed off, because they cull entities themselves.

**Experimental chunk renderer (1.16.5 only, off by default).** The setting (renamed *Aetherium
pipeline (experimental)* in 1.3.0) is on the Backend tab. Vanilla 1.16.5 draws each chunk section's solid and cutout layers with
the fixed-function pipeline. Per section it sets client-state pointers, pushes, loads and multiplies
the GL matrix stack, and issues `glDrawArrays(GL_QUADS)`. On Android's GL4ES each of those is
emulated, and every quad list is converted to triangles on the CPU, per section, per frame. When
the switch is on, Aetherium draws the **same vertex buffers** with one GLSL 1.20 program: generic
attributes, a single shared quad-to-triangle index buffer (16-bit, larger sections are split), and
one uniform per section for its offset. It reproduces vanilla's lightmap lookup, alpha cutout
(0.5) and linear/exp/exp2 fog. Vanilla still builds, uploads, culls and sorts the sections and sets
up each layer's textures, blending and depth. Translucent blocks (water, glass) and tripwire stay
on vanilla's path because they need sorting.

- If the shader fails to compile or link, or anything throws, the renderer turns itself off for the
  session and vanilla draws the frame. The Backend tab's *Chunk renderer* row shows which path is
  drawing, or why it fell back.
- **It has not been run on a GPU yet**: this repository's tooling has none. Expect the largest
  gain on Android/GL4ES and a small one on desktop drivers, which handle the fixed-function path
  well. Please send a screenshot and `latest.log` with it on and off. It will be ported to the
  other versions after it is confirmed to work on 1.16.5.

## 1.1.0: the frame-rate fixes

1.0.0 had four ways to lose frames that the defaults or the *Max FPS* preset turned on:

| Cause in 1.0.0 | Effect | 1.1.0 |
| --- | --- | --- |
| The **thermal guard** was on by default and not limited to phones. It capped the game at **30 FPS** whenever any CPU/GPU sensor read 68 °C or more. Linux desktops and phones under load reach that almost all the time. It also read every `/sys/class/thermal` zone on the render thread every 5 s. | A 30 FPS cap, plus a hitch every 5 s. | Off by default, Android-only, ceiling 75 °C. The sensor is read on a background thread. |
| **Adaptive render distance** (part of *Max FPS*) could change the distance every ~6 s. Each change makes vanilla rebuild every chunk, and the rebuild's own FPS dip triggered the next step down. | Repeated world reloads and stutter. | Removed from every preset. When on, it needs 10 s of low FPS to drop one chunk and 30 s of headroom to add one, then waits 30 s. |
| The **dynamic-light hooks** sat on vanilla's light lookup, which runs for every vertex while chunks are meshed. They allocated a callback object per call **even with lights off**. | Slower chunk building and extra garbage collection. | With lights Off at launch the hooks are not installed at all. Turning lights on from Off applies after a restart. |
| **Fullbright** hooked `OptionInstance.get()`, which runs for every option read, including once per block during meshing, and allocated on each call **even with fullbright off**. | Extra garbage collection while chunks load. | That hook is gone. Fullbright writes its value straight into the gamma option, and `get()` is vanilla bytecode again. |

Config files from 1.0.0 and earlier are migrated once (schema v5): the thermal guard, adaptive
distance and dynamic lights go back to off, and an untouched 68 °C ceiling becomes 75 °C. Every
other setting is kept.

Aetherium does not switch renderers the way VulkanMod does. Apart from the opt-in 1.16.5 chunk
renderer above, it does not replace vanilla's renderer the way Sodium does. Its gains come from
removing work inside vanilla's renderer (see the table below) and from staying out of the way when
a feature is off.

## Status (read this first)

| | |
| --- | --- |
| Compiles | All **33** versions, against the method and field signatures CI extracted from each version's real Minecraft jar (`tools/stubcheck.py --all`). CI then builds each version with its real Loom/ModDev toolchain (`ship.yml`): **33/33 green** for 1.3.0 in run 38104354261; 1.3.1: 38106872874; 1.3.2: 38108163083; 1.3.3: 38110414122; 1.3.4: 38111557998; 1.4.0 (1.16.5 only): 38118710605; 1.5.0 (1.16.5 only): 38126263750; 1.5.1 (1.16.5 only): 38128138939; 1.5.2 (1.16.5 only): 38128995655; 1.5.3 (1.16.5 only): 38130796347; 1.5.4 (1.16.5 only): 38132394475 (1.2.0: 38102357434; 1.1.0: 38078947462; 1.0.0: 38065183797). |
| Tests | 133 unit tests (including a headless test of the settings screen: scrollbar, tap vs. swipe, fling, theme switch, sounds) and 7 opt-in CPU micro-benchmarks, all passing offline (`tools/testrun.py --bench`). CI runs the same unit tests with real JUnit 5 on every version. |
| Mixin targets | Checked by hand against the `javap` probes of every version (`tools/probe/<ver>.txt`) |
| Launched in game | **No.** Nothing in this repository's tooling has a GPU. See VERIFICATION.md §3 |

Releases are published per Minecraft version as `v1.5.5+<mcversion>` (1.16.5; the other 32 versions are at `v1.3.4+<mcversion>`), and only for versions whose
build and tests passed. `DOWNLOADS.md` lists only releases that actually exist.

## The settings screen

*Options → Video Settings…* opens Aetherium's screen on every version. Pressing **Vanilla video
settings** on the General tab opens the original screen.

- **Tabs** (left rail, each with a pixel-art icon): **General**, **Quality**, **Performance**,
  **Backend**, **Effects**, **Android**.
- **Controls**: toggle switches, segmented choices, dropdowns, sliders, live info rows and buttons.
  Rows that don't apply are greyed out with the reason (for example, *Simulation distance* before
  1.18, or the thermal guard on desktop).
- **Light / dark theme**: the sun/moon switch in the header (left of the FPS badge) cross-fades the
  whole screen between the two palettes. The choice is saved immediately (`general.ui.dark_mode`).
- **Scrolling**: a scrollbar with its own lane at the right edge (8 px in touch mode, 6 px
  otherwise); the controls end before it. Drag the thumb, or click the track to jump there. On touch
  screens a swipe that starts on a row scrolls the list instead of flipping the control under your
  finger, and a quick swipe keeps gliding with momentum. Taps act on release.
- **Descriptions**: hover an option for about a third of a second and a popup opens under it
  (above it near the bottom edge) with the full description, word-wrapped. It is a bordered
  card with a soft shadow and a green accent bar (amber when the option is unavailable, with
  the reason), in the light or dark theme. Moving to the next row swaps it at once, as in Sodium.
  It stays closed while you scroll or drag, and on touch screens it does not cover the row you
  just tapped.
- **Sounds**: clicks, toggles (higher pitch for on, lower for off), tab changes, slider ticks,
  Apply and the theme switch each have their own pitch of the vanilla UI click. They play on the
  Master volume, and *General → Interface sounds* turns them off.
- **Animations**: the panel eases open, and the tab highlight slides between tabs. Page content fades
  and slides in. Toggle knobs and segment highlights glide, and dropdowns unfold. Scrolling is
  smooth, hover glows fade, and Reset/Apply show a toast. Every animation is frame-rate-independent
  (exponential easing on real elapsed time), so it plays at the same speed at 30 FPS and at 300.
- **Apply / Done**: edits are staged and applied together, so a change that needs a chunk reload
  (graphics, biome blend) triggers one reload, not one per click. Closing with Esc applies too.
  Nothing you set is lost.
- **Presets** (General): *Max FPS*, *Balanced*, *Quality* and *Custom*. A preset sets the
  performance options together, and touching any of them switches the preset to *Custom*.
- The footer reads `Aetherium Mod v1.5.5 · Minecraft <version>` and `Rendering API: OpenGL 3.0 · <GPU>`.

The screen is drawn with plain filled rectangles and text: about 300 fills per frame, no textures
and no shaders. It costs less to draw than the vanilla screen it replaces.

## Performance features

| Feature | What it saves | Where |
| --- | --- | --- |
| Entity culling | Skips entities beyond a configurable distance before vanilla's frustum test. | Performance |
| Hide entities behind walls | Skips entities in sections vanilla's occlusion graph found hidden (caves, behind hills, other buildings). Players, named and glowing entities are exempt. All 33 versions. | Performance |
| Aetherium pipeline (experimental) | 1.16.5 only, opt-in: solid and cutout terrain stored as 16-byte compact vertices grouped by face direction, back-facing groups skipped per section, drawn with Aetherium's own shader and a shared index buffer. Falls back to vanilla on any error. | Backend |
| Particle density | Drops a share of new particles at spawn, so they never tick or render. | Performance |
| Adaptive render distance | Opt-in. Lowers the render distance one chunk after 10 s under the target FPS, raises it after 30 s of headroom, and waits 30 s between changes (each change reloads all chunks). | Performance |
| Smooth chunk loading | Vanilla uploads every finished chunk mesh in the frame it finishes. Joining a world, flying or world generation then causes one long frame. Aetherium uploads at least 8 meshes per frame (so block edits show at once), then stops when 3 ms are used, and leaves the rest for the next frames. Not on 26.x, where uploads moved into the new GPU layer. | Performance |
| Animated textures off | Stops the atlas tick that re-uploads water, lava, fire, portal and other animated textures 20 times a second. That upload is expensive on GL-over-GLES layers (gl4es, ANGLE, Zink). | Performance |
| Block entity distance | Chests, signs, banners, heads and similar objects beyond 16–64 blocks are skipped before any model work (vanilla: 64). Beacon and end-gateway beams are exempt. | Performance |
| Worker threads | World generation and chunk meshing share one pool of `cores − 1` threads. On phones those threads compete with the render and server threads for the few fast cores. *Auto* keeps vanilla on desktop and uses all cores but three (at least 2) on Android. A fixed count can be set; it applies after a restart. | Performance |
| Weather off | Cancels rain/snow rendering and rain particles. | Quality |
| Vignette off | Skips the vignette overlay pass. | Quality |
| Frame cap | Opt-in, Android only: a battery-saver cap and a thermal guard (30 FPS above the ceiling; the sensor is read on a background thread). | Android |
| Vanilla settings | Graphics, clouds, particles, biome blend, render/simulation/entity distance and max FPS, all in one place. | Quality, Performance |
| Presets | *Max FPS* sets the vanilla options and culling features together (6 chunks, Fast graphics, no clouds, smooth lighting and entity shadows off, minimal particles, 30 % particle density, weather and animated textures off, a 32-block block-entity distance). No preset turns on adaptive distance or a frame cap. | General |

Dynamic lights (held torches, glowing entities) are an opt-in *effect*, not a performance feature.
They are **off by default**: a moving light makes vanilla rebuild the chunk sections around it,
which is real lag on a phone. With lights Off when the game starts, their hooks are not installed
at all, so turning them on in *Effects* applies after a restart. A light lookup costs about 34 ns
with 8 active sources (`CpuMicroBenchmarkTest`). Fullbright writes its value into the gamma option
directly (no per-read hook) and never writes to `options.txt`.

**`general.enabled = false`** turns every hook into a pass-through. The game then behaves exactly as
it would without the mod, apart from the replaced settings screen.

## OpenGL

Aetherium needs nothing beyond what the game itself needs. By default it issues no GL calls,
compiles no shaders and creates no buffers. Everything goes through vanilla's renderer, which is
why it runs on the same devices as the game. The one exception is the opt-in 1.16.5 chunk renderer.
It uses a GLSL **1.20** program (OpenGL 2.1, below the 3.0 baseline, and safe for GL4ES) and one
index buffer, and falls back to vanilla if the driver rejects them. The *Rendering API: OpenGL 3.0* footer states the mod's own
baseline.

The game's own requirement is unchanged. Minecraft 1.17+ creates an OpenGL **3.2** core context
because its built-in shaders are GLSL 150. A mod cannot lower that without breaking vanilla
rendering, so Aetherium does not force a 3.0 context. On Android, launchers such as Pojav,
FCL and Zalith provide GL through a translation layer (gl4es, ANGLE, Zink, MobileGlues). The
*Android* tab shows which one was detected and lets you override it.

## Install

1. Pick the jar for your exact Minecraft version from **DOWNLOADS.md** (or the Releases page). Each
   jar only loads on the version it was built for.
2. **Fabric**: put it in `mods/` with Fabric Loader. Fabric API is *not* required.
   **NeoForge**: put it in `mods/`. No NeoForge jar exists for 1.16.5–1.20.1, 1.20.2, 1.20.3 or
   1.20.5 (Fabric only).

## Configuration

`<game>/config/aetherium.json`, schema v4 (`CONFIG_SCHEMA.json`). It is written by the screen, so
you rarely need to edit it. Keys added in 1.0.0 load their defaults when missing, so 0.2.0 files
keep working unchanged. Files from 0.1.0 (v1–v3) are migrated on first load; keys for removed
features are dropped. Lang files and the schema are generated from `AetheriumConfig.java` by
`tools/gen_resources.py` (`--check` in CI).

## Building

The reference version is **1.21.1** (Java 21). Its sources are the repository tree:

```sh
gradle :common:build :fabric:build :neoforge:build
```

Every other version is the same tree plus `deltas/<version>/changes.patch`:

```sh
sh tools/port.sh 1.19.2            # materialise the ported tree under build/ports/1.19.2
```

CI (`ship.yml`) builds and releases every version on each push, unless `.github/ship-only` lists
versions: then only those. It currently lists **1.16.5** while 1.16.5 is the development target,
so the other 32 versions stay at their v1.3.4 release.

### How one source tree serves 33 versions

Version-specific code is written in place as **era blocks**:

```java
// @era:hud-begin graphics-float|graphics-delta
public static void render(final GuiGraphics graphics) { ... }
// @era:hud-else stack
//~ public static void render(final PoseStack pose) { ... }
// @era:hud-end
```

`tools/eras.py` defines each era's version ranges (18 eras: options access, GUI drawing API,
screen package, input events, weather renderer, light hook, and so on). It turns the right block on
for each version. `tools/gen_deltas.py` writes that selection, plus the build pins, as the version's
patch. `tools/stubcheck.py` compiles the same selection against that version's probed signatures.
What is compiled offline is therefore exactly what ships.

### Offline toolchain (no Maven needed)

```sh
bash tools/local_jdk.sh              # JRE + ECJ from PyPI/npm-reachable mirrors
python3 tools/stubcheck.py --all     # compile all 33 versions against tools/probe/*
python3 tools/testrun.py [--bench]   # unit tests (+ CPU micro-benchmarks)
python3 tools/gen_deltas.py --all --verify
python3 tools/check.py && python3 tools/check_refs.py . && python3 tools/check_java.py .
```

## Layout

```
common/   MC-free core (config, perf, lighting, gui engine) + client/ (MC glue) + 11 mixins
fabric/   loader shell        neoforge/  loader shell
deltas/   32 generated ports  tools/     generators, checks, probes, offline toolchain
```

## Documentation

- `VERIFICATION.md`: what was checked, how, and what was not
- `PORTING_MATRIX.md`: every version, its pins and its eras (generated)
- `deltas/<version>/README.md`: what that version's patch changes (generated)
- `CONFIG_SCHEMA.json`: config reference (generated)

## License

See `LICENSE`.
