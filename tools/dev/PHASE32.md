# Phase 32: Quality of life + all 1.16.x changes on every version, and Backport parity

User request: "Add all the features and changes all versions across even in back port and add every
single feature that 1.12.2 missed in it. All in one go." Then release everywhere.

## Main mod (1.17 .. 26.3): port the QoL tab (1.16.0-1.16.3, 1.16.5-only so far)

Feature groups (eras in tools/eras.py, flags in qol/QolSupport):

| group   | files                                                               | status |
|---------|---------------------------------------------------------------------|--------|
| q-hand  | QolHandMixin (hand editor, visible arm, arms sculpt, both hands)     | done   |
| q-hud   | QolGuiMixin (crosshair, food bar), QolFoodIcons                     | done   |
| q-food  | QolTooltipMixin                                                     | done   |
| q-place | QolPlacementMixin                                                   | done   |
| q-inv   | QolContainerScreenMixin, QolScrollMixin, QolSlotMixin, QolInventory | done   |
| q-world | QolClient (keys, world key, chat), WaypointScreen, QolDeathScreenMixin; 1.17+ draws waypoints/damage via QolOverlay (HUD projection) instead of QolLevelRenderMixin | done |
| q-fire  | QolFireMixin (fire height, shield offset is in q-hand)              | done   |

Design decisions:
- 1.17+: waypoint beams/labels and damage numbers are HUD-projected (qol/ScreenProjector +
  client/QolOverlay, drawn from FrameHud), so no world-renderer variants are needed. 1.16.5 keeps the
  in-world renderer (QolSupport.WORLD_IN_LEVEL).
- FOV for the projection: ZoomKeys.lastWorldFov (recorded by ZoomFovMixin on every version).
- 26.2+: the HUD moved to net.minecraft.client.gui.Hud (GuiMixin has no hud hook there yet) and
  handleKeybinds moved to Gui.

Probe: QoL classes + hooked method bodies added to tools/probe_classes.txt / probe_methods.txt;
full run on every version (tools/probe_only.txt removed).

Main-mod status (commit 78bf287+): every group ported; stubcheck clean on all 33 versions except
known stub gaps (JOML Quaternionf, CharacterEvent.codepoint, pre-existing non-QoL); mixincheck: all Qol
targets resolve everywhere; tests 223 passed. Generators: tools/dev/gen/{qolhuddraw,qolguimixin,
qolcontainer}.py (run normalize.py on their output).

## Backport (1.12.2): everything the main mod has that it lacks

Local compile check: tools/dev/bpcheck.py [FILE_REGEX] (stubs from backport/probe/out; baseline ~133
errors from unprobed Forge/LWJGL). Found and fixed: shared-sources closure (TerrainRegions etc.) and
Capabilities flags (RENDER_SCALE, TUNING, COALESCE_EXTRACT, FOG_CULL_EXTRACT) missing since 1.13-1.15.
Plan: QoL via Forge events (overlay Pre/Post, ItemTooltipEvent, RenderWorldLastEvent, GuiScreenEvent
draw/mouse/keyboard) + mixins only for placement, hand pose, fire, inventory clicks; tuning options via
shared perf.Tuning + a backport perf.TuningSupport + 1.12.2 mixins.

Missing settings (vs main AetheriumPages): the whole QoL tab plus ~55 perf/quality options, list in
/tmp/missing_desc.txt (regenerate: compare quoted keys of both AetheriumPages files).

## Release
Main: 1.17.0 everywhere (Modrinth + CurseForge, Fabric + NeoForge). Backport: 1.1.0 (CurseForge is live,
Modrinth project t1gApRrh still unpublished: tell the user to submit it).

## Follow-up probe (26.3 renames found so far)
- net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer (replaces ItemInHandRenderer in 26.3)
- net.minecraft.client.input.CharacterEvent (WaypointScreen.charTyped on 1.21.9+)
- org.joml.Quaternionf (no-arg constructor, rotationX/Y/Z: stub gap only); probe.yml now searches
  every Gradle cache for library jars (d58386d), so JOML should dump from the next probe on.
