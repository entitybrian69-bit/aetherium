# Aetherium — 33-Version Porting Matrix

The `common` module is split in two layers:

| Layer | Packages | MC dependency | Changes per version |
|---|---|---|---|
| **Core** | `engine`, `engine.gl`, `engine.vk`, `chunk`, `light`, `gamma`, `compat`, `android`, `config`, `frame`, `gui.theme`, `gui.anim` | none (LWJGL + Gson + SLF4J only) | **zero** — identical jar contents on every version |
| **Client glue** | `client.gui`, `client.render`, `mixin` | yes | the deltas tabulated below |

The reference implementation in this repository targets **row 20 (1.21.1)**. Every other row lists exactly what must
change in the client-glue layer relative to the row above it. "—" means no change from the previous row for that column.
Dependency coordinates are the ones from the roadmap; verify against the loader's maven at port time.

## Legend of glue-layer touch points

| ID | What | 1.21.1 target used in this repo |
|---|---|---|
| **G1** | Frame begin/end | `GameRenderer#render(DeltaTracker, boolean)` |
| **G2** | Level render head (camera, dyn-light tick, view-proj) | `LevelRenderer#renderLevel(DeltaTracker, boolean, Camera, GameRenderer, LightTexture, Matrix4f, Matrix4f)` |
| **G3** | After opaque terrain → HZB pass | `INVOKE LevelRenderer#renderSectionLayer(RenderType,DDD,Matrix4f,Matrix4f)` ordinal 2 |
| **G4** | Entity culling | `LevelRenderer#renderEntity(Entity,DDD,F,PoseStack,MultiBufferSource)` |
| **G5** | Dynamic light injection | `static LevelRenderer#getLightColor(BlockAndTintGetter, BlockState, BlockPos)` |
| **G6** | Chunk executor swap | `@ModifyArg` on `new SectionRenderDispatcher(...)` in `LevelRenderer#allChanged`, index 2 |
| **G7** | Gamma lightmap rewrite | `LightTexture#updateLightTexture(F)` before `DynamicTexture#upload()` on `lightPixels` |
| **G8** | Screen replacement | `@ModifyVariable Minecraft#setScreen` for `screens.options.VideoSettingsScreen` |
| **G9** | GUI drawing API | `GuiGraphics` (`fill`, `fillGradient`, `renderOutline`, `enableScissor`) |
| **G10** | Overlay / HUD hook | `Gui#render(GuiGraphics, DeltaTracker)` TAIL |
| **G11** | Battery-saver distance | `Options#getEffectiveRenderDistance()` |
| **G12** | Loader entrypoint | Fabric `ClientModInitializer` / NeoForge `@Mod(dist = CLIENT)` |

## The matrix

| # | MC | Loaders | Build | Java | LWJGL | Iris / Oculus | Glue-layer deltas vs. previous row |
|---|---|---|---|---|---|---|---|
| 1 | 1.16.5 | Forge 36.2.39 (primary), Fabric Loader 0.15.11 | ForgeGradle 5 / Loom 1.6, Yarn or MCP (`official` not usable for Forge) | 8 | 3.2.3 | Iris 1.16.5 (Fabric only) | **Baseline for legacy branch.** G1 → `GameRenderer#render(FJZ)V`. G2 → `WorldRenderer#renderLevel(MatrixStack,F,J,Z,ActiveRenderInfo,GameRenderer,LightTexture,Matrix4f)` (`LevelRenderer` is `WorldRenderer` in MCP). G3 → `renderChunkLayer(RenderType,MatrixStack,DDD)` ordinal 2. G4 → `renderEntity(Entity,DDD,F,MatrixStack,IRenderTypeBuffer)`. G5 → `getLightColor(IBlockDisplayReader,BlockState,BlockPos)`. G6 → `ChunkRenderDispatcher(World, WorldRenderer, Executor, boolean, RegionRenderCacheBuilder)` index 2. G7 → `LightTexture#updateLightTexture(F)` before `NativeImage#upload`; gamma read from `GameSettings.gamma` (double). G8 → `VideoSettingsScreen` lives in `net.minecraft.client.gui.screen`; parent field `lastScreen` → `OptionsSubScreen` does not exist, use `SettingsScreen#lastScreen`. G9 → `MatrixStack` + `AbstractGui.fill/fillGradient`; `enableScissor` does not exist → `RenderSystem.enableScissor` with manual window-scale maths. G10 → `IngameGui#render(MatrixStack,F)`. G11 → `GameSettings.renderDistanceChunks` is a public int field; patch the read in `WorldRenderer#setupRender`. GL: `GpuCapabilities` identical; DSA path valid (GL 4.6 is a driver property, not an MC property). Widgets extend `Widget` (not `AbstractWidget`), tooltip via `renderToolTip`. Forge-side needs `@Mod` + `FMLJavaModLoadingContext`, `ModList.get().isLoaded`. Entity culling: `Entity.isGlowing()` instead of `isCurrentlyGlowing()`. |
| 2 | 1.17 | Fabric 0.15.11 (primary), Forge 37.0.0 | Loom 1.6 / FG5 | 16 | 3.2.3 | Iris 1.17 | Legacy GL removed from `RenderSystem`: no `glBegin`; all our GL is via LWJGL directly so **core engine unaffected**. G2 → `LevelRenderer#renderLevel(PoseStack,F,J,Z,Camera,GameRenderer,LightTexture,Matrix4f)` (Mojang names now usable on both loaders). G6 → `ChunkRenderDispatcher(Level, LevelRenderer, Executor, boolean, ChunkBufferBuilderPack)`. G9 → `GuiComponent.fill`. `Screen#renderBackground(PoseStack)`. NativeImage lightmap still CPU. `OptionsSubScreen#lastScreen` exists from here. |
| 3 | 1.17.1 | Fabric, Forge 37.1.1 | — | 16 | 3.2.3 | Iris 1.17.1 | No signature changes; re-run refmap generation only. |
| 4 | 1.18 | Fabric, Forge 38.0.17 | Loom 1.6 / FG5 | 17 | 3.3.1 | Iris 1.18 | Cylindrical render distance: `Options.renderDistance` → `simulationDistance` added; `LiveSettingsApplier` gains the simulation slider (int field, not `OptionInstance`). Section key: world height is now `-64..320` → `AetheriumChunkBuilder.sectionKey` already uses 20-bit signed Y, no change. `GpuCapabilities`: LWJGL 3.3.1 — `GL_ARB_texture_filter_anisotropic` constant available via `ARBTextureFilterAnisotropic`. Anisotropic filtering added to GUI (same `LiveSettingsApplier.applyTextureFiltering`). |
| 5 | 1.18.1 | Fabric, Forge 39.0.5 | — | 17 | 3.3.1 | Iris 1.18.1 | None. |
| 6 | 1.18.2 | Fabric, Forge 40.2.0 | — | 17 | 3.3.1 | Iris 1.18.2 | None (Forge 40 renames `fml.common.Mod` → same; `ForgeConfigSpec` unused by us). |
| 7 | 1.19 | Fabric, Forge 41.0.94 | Loom 1.6 / FG5 | 17 | 3.3.1 | Iris 1.19 | `ItemBlockRenderTypes#setRenderLayer` deprecated → we never call it. Dynamic lights via lightmap: G5 unchanged, plus `ClientLevel#getLightEngine` usable for sky at camera. Chat-signing changes irrelevant. G6 → `ChunkRenderDispatcher(Level, LevelRenderer, Executor, boolean, ChunkBufferBuilderPack, int)`: index still 2. |
| 8 | 1.19.1 | Fabric, Forge 42.0.9 | — | 17 | 3.3.1 | Iris 1.19.1 | None. |
| 9 | 1.19.2 | Fabric, Forge 43.2.0 | — | 17 | 3.3.1 | Iris 1.19.2 | None. |
| 10 | 1.19.3 | Fabric, Forge 44.1.0 | — | 17 | 3.3.1 | Iris 1.19.3 | **`Options` becomes `OptionInstance<T>`** (`renderDistance().get()/set()`, `ambientOcclusion()` is now `Boolean`): `LiveSettingsApplier` as in this repo. `Component.translatableWithFallback` not yet available → add fallback helper. |
| 11 | 1.19.4 | Fabric, Forge 45.1.0 | — | 17 | 3.3.1 | Iris 1.19.4 | `translatableWithFallback` available. `Screen#renderDirtBackground` deprecated. `OptionsSubScreen` unchanged. |
| 12 | 1.20 | Fabric, Forge 46.0.14 | Loom 1.6 / FG6 | 17 | 3.3.1 | Iris 1.20 | **`PoseStack` → `GuiGraphics`** for all GUI drawing (this repo's widget code applies verbatim: `fill`, `fillGradient`, `renderOutline`, `enableScissor`, `drawString`). G10 → `Gui#render(GuiGraphics,F)`. Overlay API (Forge `IGuiOverlay`) still present but unused. |
| 13 | 1.20.1 | Forge 47.2.0 (primary), Fabric | — | 17 | 3.3.1 | **Oculus 1.6.x on Forge**, Iris 1.6.x on Fabric | `IrisCompat` already binds `net.irisshaders.iris.api.v0.IrisApi` which Oculus ships under the same package; `flavor` detection by mod id `oculus`. No glue changes. `ConflictDetector` treats Rubidium/Embeddium as hard conflicts. |
| 14 | 1.20.2 | **NeoForge 20.2.88**, Fabric | Loom 1.6 / NeoGradle 7 (ModDevGradle not yet) | 17 | 3.3.2 | Iris 1.7.x | NeoForge module replaces Forge: `net.neoforged.fml.common.Mod`, `neoforge.mods.toml` → in 1.20.2–1.20.4 the file is still `mods.toml`. `Screen#mouseScrolled` gains `scrollX` (4 args — as in this repo). `Screen#renderBackground(GuiGraphics,int,int,float)` (as in this repo). G6 → class renamed **`SectionRenderDispatcher`**(`ClientLevel, LevelRenderer, Executor, RenderBuffers, BlockRenderDispatcher, BlockEntityRenderDispatcher)`, index 2 — identical to this repo. |
| 15 | 1.20.3 | NeoForge 20.3.8, Fabric | — | 17 | 3.3.2 | Iris 1.7.x | None. |
| 16 | 1.20.4 | NeoForge 20.4.237, Fabric | — | 17 | 3.3.2 | Iris 1.7.x | None. |
| 17 | 1.20.5 | NeoForge 20.5.20, Fabric | Loom 1.6 / NeoGradle 7 | **21** | 3.3.2 | Iris 1.7.x | Java 21. Forge overlay folder removed (irrelevant to us). G3 → `renderSectionLayer(RenderType,DDD,Matrix4f,Matrix4f)` gains second matrix (as in this repo). G2 → `renderLevel(F,J,Z,Camera,GameRenderer,LightTexture,Matrix4f,Matrix4f)`. `ItemStack` components: `stackLuminance` unchanged (uses `is()`), `EntityType.toString()` unchanged. |
| 18 | 1.20.6 | NeoForge 20.6.119, Fabric | — | 21 | 3.3.2 | Iris 1.7.x | None. |
| 19 | 1.21 | NeoForge 21.0.167, Fabric 0.16.10 | Loom 1.7 / **ModDevGradle 1.0** | 21 | 3.3.3 | Iris 1.8.x | **`DeltaTracker`** replaces `(float partialTick, long nanos)`: G1 `render(DeltaTracker,Z)`, G2 `renderLevel(DeltaTracker,Z,Camera,GameRenderer,LightTexture,Matrix4f,Matrix4f)`, G10 `Gui#render(GuiGraphics,DeltaTracker)`. Option screens move to **`net.minecraft.client.gui.screens.options`** (G8). NeoForge: `neoforge.mods.toml`. == this repository. |
| 20 | **1.21.1** | NeoForge 21.1.77, Fabric 0.16.10 | Loom 1.8 / ModDev 1.0.21 | 21 | 3.3.3 | Iris 1.8.x | **Reference implementation (this repository).** |
| 21 | 1.21.2 | NeoForge 21.2.1, Fabric | Loom 1.8 / ModDev 2.0 | 21 | 3.3.3 | Iris 1.8.x | **Lightmap moves to the GPU** (`LightTexture` now a compute/fragment pass, `lightPixels` gone): G7 → inject into `LightTexture#updateLightTexture` *after* `RenderSystem.setShaderGameTime`/uniform upload and bind our own post-pass program that reads the lightmap texture and writes it back (a 16x16 fullscreen pass using `GammaUtilities` parameters as uniforms: brightness, nightVision, caveVision, curve LUT 16 floats). `getLightColor` G5 unchanged. Entity rendering: `renderEntity` still present. `Camera`/`Frustum` unchanged. |
| 22 | 1.21.3 | NeoForge 21.3.95, Fabric | — | 21 | 3.3.3 | Iris 1.8.x | None. |
| 23 | 1.21.4 | NeoForge 21.4.136, Fabric | — | 21 | 3.3.3 | Iris 1.8.x | "Texture Filtering" row already exists in Quality. `ItemModel` refactor does not touch our code. |
| 24 | 1.21.5 | NeoForge 21.5.x, Fabric | — | 21 | 3.3.3 | Iris 1.8.x | `LevelRenderer#renderLevel` now splits into `FrameGraphBuilder` passes: G3 → inject after the `"terrain"` pass's `executes` lambda — target `LevelRenderer#lambda$addMainPass$…` is fragile, so instead inject at `INVOKE LevelRenderer#renderSectionLayer` ordinal 2 inside `addMainPass`. G2 stays on `renderLevel` HEAD. |
| 25 | 1.21.6 | NeoForge 21.6.x, Fabric | Loom 1.10 / ModDev 2.0 | 21 | 3.3.3 | Iris 1.8.x | **`RenderSystem.setShader` + `BufferRenderer` removed; `RenderPipeline` + `GpuDevice` introduced.** Core engine unaffected (raw LWJGL). Glue: `LiveSettingsApplier.applyTextureFiltering` must obtain the atlas `GpuTexture` via `minecraft.getTextureManager().getTexture(...).getTexture()` and set sampler params through `GpuDevice` (or keep raw GL via `GlTexture#glId()` on the GL backend). `GuiGraphics` gains `GuiRenderState` — all `fill`/`drawString` calls still valid. Debug overlay unchanged. G3 depth texture → `mainRenderTarget.getDepthTexture()` → `((GlTexture) t).glId()`. |
| 26 | 1.21.7 | NeoForge 21.7.x, Fabric | — | 21 | 3.3.3 | Iris 1.8.x | None. |
| 27 | 1.21.8 | NeoForge 21.8.x, Fabric | — | 21 | 3.3.3 | Iris 1.8.x | None. |
| 28 | 1.21.9 | NeoForge 21.9.x, Fabric | — | 21 | 3.3.3 | Iris 1.8.x | **Entity render state extraction split**: G4 moves from `LevelRenderer#renderEntity` to `EntityRenderDispatcher#extractRenderState`/`submit` — cull at extraction time (`@Inject` HEAD cancellable on `LevelRenderer#extractVisibleEntities` loop body via `@WrapOperation` of `shouldRender`). `EntityCuller` logic unchanged. |
| 29 | 1.21.10 | NeoForge 21.10.x, Fabric | — | 21 | 3.3.3 | Iris 1.8.x | None. |
| 30 | 1.21.11 | NeoForge 21.11.x, Fabric | Loom 1.11+ | 21 | 3.3.3 | Iris 1.8.x | **Fabric switches to Mojang mappings in production (no Intermediary from 1.21.12)**: set `loom.officialMojangMappings()` (already done) and drop refmap generation (`mixin.defaultRefmapName` removed; mixin JSON `refmap` key removed). NeoForge 21.11 is a hard break for classic-Forge-derived mods; our NeoForge module has no Forge legacy and ports cleanly. |
| 31 | 26.1 | Fabric 0.17.x, NeoForge 26.1.x | Loom 1.14 / ModDev 2.x | **25** | 3.3.4 + SDL | Iris 1.8.x | New versioning. Window system switches **GLFW → SDL3**: `Minecraft#getWindow()` API same; `VulkanBackend` presentation can now use `SDL_Vulkan_CreateSurface` via `org.lwjgl.sdl.SDLVulkan` — wire swapchain here. Add the `Graphics API` toggle to the vanilla-side too (already exists in Advanced). Java 25: `--enable-native-access` flag required for LWJGL in run configs. |
| 32 | 26.2 | Fabric 0.17.x, NeoForge 26.2.x | — | 25 | 3.3.4 + SDL | Iris 1.8.x | Vulkan stabilisation: dynamic lights as a Vulkan compute pass (`DynamicLightEngine` already produces the source list; upload as SSBO, same kernel as `HierarchicalZBuffer.CULL_SRC` style), gamma as a UBO consumed by the lightmap pass. |
| 33 | 26.3 | Fabric 0.17.x, NeoForge 26.3.0.0-beta+ | **Loom 1.17 / Gradle 9.6.0** | 25 | 3.3.4 + SDL | Iris 1.8.x | Final: Iris on both backends, GL/Vulkan parity tests, Gradle 9 (`kotlin-dsl` lazy configuration, `foojay` 1.0). |

## Android (every row)

`AndroidLauncherCompat` is pure Java and identical on all 33 versions. Backend resolution:

| Detected | Backend | Notes |
|---|---|---|
| Zink / VulkanZink / Kopper Zink / MobileGlues | `VULKAN_NATIVE` → falls back to `OPENGL_MODERN` until swapchain presentation lands (row 31) | Zink exposes GL 4.6 so the DSA/MDI/HZB path runs today |
| LTW | `OPENGL_CORE` (GL 3.3) | HZB & MDI disabled by capability probe |
| GL4ES | `OPENGL_LEGACY` (GL 2.1) | engine applies CPU-side wins only (chunk scheduler, entity culling, gamma, dynamic lights) |
