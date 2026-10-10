# Aetherium port: Minecraft 1.19.3

- Java toolchain: **17** (mixin compatibilityLevel `JAVA_17`)
- Fabric loader floor: **0.14.24**, Loom **1.16.1**
- Fabric API (compile-only, optional at runtime): **0.76.1+1.19.3**
- NeoForge: **does not exist for this version - the neoforge module is disabled**
- Mappings: officialMojangMappings()
- Status: **derived**

## What the delta changes

### Build pins

```properties
minecraft_version=1.19.3
java_version=17
fabric_loader_version=0.14.24
fabric_api_version=0.76.1+1.19.3
neoforge_version=unavailable (module disabled)
```

### Era transforms applied by the generated patch

- `Button.builder->ctor`
- `GuiGraphics->PoseStack`
- `OptionsScreen-package`
- `ResourceLocation.parse->ctor`
- `disableScissor->GL11`
- `drawCenteredString`
- `drawString`
- `enableScissor->GL11.glScissor`
- `fill`
- `guiGraphics->poseStack`
- `guiWidth->Window`
- `narration-removed`
- `renderBackground->1-arg`
- `renderWidget->renderButton`
- `setWidth->width`
- `setX->x`
- `setY->y`

### Era facts and where they were read from

- `stack_class` = **PoseStack** - PoseStack [VERIFIED: Iris @ 1.19.4 branch (MixinGui captures PoseStack)]
- `component_era` = **component** - Component.literal is 1.19+ [VERIFIED on 1.19.4 sources]
- `narration` = **none** - AbstractWidget declares the abstract `updateWidgetNarration` from 1.19.4 on [VERIFIED by the 2026-10-10 ship legs: 1.19.4 and 1.20 rejected `updateNarration` as 'cannot override' while demanding updateWidgetNarration]; below 1.19.4 narration does not exist and the override is removed
- `widget_render` = **renderButton** - renderWidget from 1.19.4 [VERIFIED by the 1.19.4 ship leg rejecting renderButton; renderWidget verified on 1.21.1 via sodium's widget set]
- `render_background_args` = **1** - 1-arg form [VERIFIED on 1.20.1 (Iris) and 1.19.4 (Iris); the 1.20.2-1.20.4 boundary is UNVERIFIED]
- `widget_setters` = **False** [UNVERIFIED boundary: setters vs public fields, dated to the 1.20 render rework]
- `options_pkg` = **screens** - the screens.options package is 1.21+ [VERIFIED: 1.20.5/1.20.6 legs rejected it; sodium @ 1.21.1 imports it]
- `resource_location` = **ctor** - ResourceLocation.parse is 1.21+; earlier rows use the constructor
- `level_sections` = **minSection** - getMinSection/getMaxSection through 1.21.1, getMinSectionY/getMaxSectionY from 1.21.2 [VERIFIED: ship legs + sodium @ 1.21.4]
- `input_events` = **doubles** - 1.21.9 replaced the double-based mouse handlers with MouseButtonEvent forms [VERIFIED: sodium @ 26.2/stable]
- `identifier` = **False** - ResourceLocation becomes Identifier (with fromNamespaceAndPath) from 1.21.11 [VERIFIED: sodium @ 1.21.11/stable; the 1.21.9/1.21.10 legs compiled ResourceLocation.parse fine]
- `gui_extractor` = **False** - 26.x replaces GuiGraphics with GuiGraphicsExtractor, Screen#render with extractRenderState, and drawString/drawCenteredString with text/centeredText [VERIFIED: sodium @ 26.1.2/stable and 26.2/stable; this row's exact version derived from 26.2 if newer]
- `entities_iter` = **entitiesForRendering** - entitiesForRendering [VERIFIED on 1.21.1: Iris MixinLevelRenderer_SkipRendering targets ClientLevel#entitiesForRendering]
- `fabric_api` pin read from FabricMC/fabric tags on 2026-10-10 [VERIFIED tag]

## Files the generated patch touches

- `gradle.properties`
- `settings.gradle.kts`
- `build.gradle.kts`
- `common/src/main/resources/aetherium-common.mixins.json`
- `common/src/main/java/com/aetherium/mixin/AetheriumMixinPlugin.java`
- `common/src/main/java/com/aetherium/mixin/core/GuiMixin.java`
- `common/src/main/java/com/aetherium/mixin/core/LightTextureMixin.java`
- `common/src/main/java/com/aetherium/mixin/core/OptionsScreenMixin.java`
- `common/src/main/java/com/aetherium/gui/AetheriumVideoOptionsScreen.java`
- `common/src/main/java/com/aetherium/gui/AetheriumTheme.java`
- `common/src/main/java/com/aetherium/gui/AetheriumTabs.java`
- `common/src/main/java/com/aetherium/gui/AetheriumAnimations.java`
- `common/src/main/java/com/aetherium/gui/widget/AetheriumWidgets.java`
- `common/src/main/java/com/aetherium/hud/AetheriumHudRenderer.java`

## Verifying this row before shipping it

The port is applied with `sh tools/port.sh 1.19.3` and then checked against the
real mapped jar - the jar is the authority, not a wiki:

```sh
MC=1.19.3
JAR=$(find "$HOME/.gradle/caches/fabric-loom" -name "minecraft-*-"$MC"-*.jar" 2>/dev/null | head -n 1)
test -n "$JAR" || { echo "run ./gradlew build once so Loom downloads $MC"; exit 1; }
javap -p -classpath "$JAR" net.minecraft.client.renderer.LightTexture | grep -E 'updateLightTexture|tickLightTexture|pixels|NativeImage|DynamicTexture'
javap -p -classpath "$JAR" net.minecraft.client.gui.Gui | grep -E 'public void render'
javap -p -classpath "$JAR" net.minecraft.client.renderer.LevelRenderer | grep -E 'setSectionDirty|allChanged'
javap -p -classpath "$JAR" net.minecraft.client.gui.screens.OptionsScreen | grep -nE 'lambda|init'
javap -p -classpath "$JAR" net.minecraft.client.gui.components.AbstractWidget | grep -E 'renderWidget|renderButton|setX|updateNarration'
```

```sh
python3 tools/check_refs.py . && python3 tools/check.py --skip-deltas
./gradlew --no-daemon :common:compileJava :fabric:compileJava
```

A compile error on the GUI overrides is the *good* outcome: it is loud, local and
mechanical. A mixin that silently stops applying is the bad one, which is why the
HUD prints `Aetherium.describeRuntime()` and the log prints every refused mixin.

## Notes

- `ItemRenderer`/`BlockRenderDispatcher` rework: irrelevant to Aetherium's shadow mode, relevant to the FULL path (refused).

Generated by `tools/gen_deltas.py`; re-run that instead of editing.
