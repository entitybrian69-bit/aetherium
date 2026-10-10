# Aetherium port: Minecraft 1.21.4

- Java toolchain: **21** (mixin compatibilityLevel `JAVA_21`)
- Fabric loader floor: **0.16.14**, Loom **1.16.1**
- Fabric API (compile-only, optional at runtime): **0.119.4+1.21.4**
- NeoForge: **21.4.158**
- Mappings: officialMojangMappings()
- Status: **documented**

## What the delta changes

### Build pins

```properties
minecraft_version=1.21.4
java_version=21
fabric_loader_version=0.16.14
fabric_api_version=0.119.4+1.21.4
neoforge_version=21.4.158
```

### Era transforms applied by the generated patch

- `getMinSection->getMinSectionY`

### Era facts and where they were read from

- `stack_class` = **GuiGraphics** - GuiGraphics [VERIFIED: Iris @ 1.20.1 and 1.20.6 branches]
- `component_era` = **component** - Component.literal is 1.19+ [VERIFIED on 1.19.4 sources]
- `narration` = **widget** - AbstractWidget declares the abstract `updateWidgetNarration` from 1.19.3 on [VERIFIED: official-mapping javadoc @ 1.19.3 lists updateWidgetNarration; 1.19.4 and 1.20 legs rejected `updateNarration` as 'cannot override' while demanding updateWidgetNarration]
- `widget_render` = **renderWidget** - renderWidget from 1.19.4 [VERIFIED by the 1.19.4 ship leg rejecting renderButton; renderWidget verified on 1.21.1 via sodium's widget set]
- `render_background_args` = **4** - 4-arg form [VERIFIED on 1.20.2 (ship leg rejected the 1-arg call), 1.20.6 (Iris) and 1.21.1 (Iris)]
- `widget_access` = **accessors** - getX/setX/getY/setY exist from 1.19.3 on; 1.16.5-1.19.2 expose public x/y fields instead, and only the x/y accessors are rewritten (getWidth/setWidth/getHeight/setHeight exist across the whole range) [VERIFIED: official-mapping javadoc @ 1.17.1/1.18.2/1.19.3; the 1.19.2 leg rejected this.getX()]
- `hover_or_focus` = **True** - AbstractWidget#isHoveredOrFocused exists from 1.18 on; 1.16.5-1.17.1 get the `(isHovered() || isFocused())` rewrite [VERIFIED: javadoc @ 1.18.2 lists it, @ 1.17.1 does not, and the 1.17 leg rejected it]
- `options_access` = **getters** - Options#renderDistance()/simulationDistance() OptionInstance getters are 1.19+; 1.17-1.18.2 read public int fields, and 1.16.5-1.17.x have no simulationDistance at all (the benchmark label degrades to `sim=n/a`) [VERIFIED: the 1.19/1.19.1/1.19.2 legs compiled the getters while 1.18.2 rejected them]
- `button_builder` = **True** - Button.builder arrives with the 1.19.3 screen rework; 1.16.5-1.19.2 construct Button directly [VERIFIED: the 1.19.3 leg rejected the constructor form]
- `cycle_button` = **CycleButton** - CycleButton under that exact mojmap name from 1.17 on [VERIFIED: official-mapping javadoc @ 1.17.1/1.18.2; the 1.17/1.18.2 legs rejected CycleButtonWidget]
- `logging` = **slf4j** - slf4j on the compile classpath
- `no_remap_loom` = **False** - 26.x ships unobfuscated jars: the no-remap Loom plugin id, no mappings() line, no remapJar task [VERIFIED: sodium @ 26.2/stable applies net.fabricmc.fabric-loom @ 1.16.1 with no mappings block; the 26.x legs failed with 'Failed to find official mojang mappings']
- `options_pkg` = **screens.options** - the screens.options package is 1.21+ [VERIFIED: 1.20.5/1.20.6 legs rejected it; sodium @ 1.21.1 imports it]
- `resource_location` = **parse** - ResourceLocation.parse is 1.21+; earlier rows use the constructor
- `level_sections` = **minSectionY** - getMinSection/getMaxSection through 1.21.1, getMinSectionY/getMaxSectionY from 1.21.2 [VERIFIED: ship legs + sodium @ 1.21.4]
- `input_events` = **doubles** - 1.21.9 replaced the double-based mouse handlers with MouseButtonEvent forms [VERIFIED: sodium @ 26.2/stable]
- `identifier` = **False** - ResourceLocation becomes Identifier (with fromNamespaceAndPath) from 1.21.11 [VERIFIED: sodium @ 1.21.11/stable; the 1.21.9/1.21.10 legs compiled ResourceLocation.parse fine]
- `gui_extractor` = **False** - 26.x replaces GuiGraphics with GuiGraphicsExtractor, Screen#render with extractRenderState, and drawString/drawCenteredString with text/centeredText [VERIFIED: sodium @ 26.1.2/stable and 26.2/stable; this row's exact version derived from 26.2 if newer]
- `entities_iter` = **entitiesForRendering** - entitiesForRendering [VERIFIED on 1.21.1: Iris MixinLevelRenderer_SkipRendering targets ClientLevel#entitiesForRendering]
- `fabric_api` pin read from FabricMC/fabric tags on 2026-10-10 [VERIFIED tag]

## Files the generated patch touches

- `gradle.properties`
- `common/src/main/java/com/aetherium/mixin/AetheriumMixinPlugin.java`
- `common/src/main/java/com/aetherium/client/ClientHooks.java`

## Verifying this row before shipping it

The port is applied with `sh tools/port.sh 1.21.4` and then checked against the
real mapped jar - the jar is the authority, not a wiki:

```sh
MC=1.21.4
JAR=$(find "$HOME/.gradle/caches/fabric-loom" -name "minecraft-*-"$MC"-*.jar" 2>/dev/null | head -n 1)
test -n "$JAR" || { echo "run ./gradlew build once so Loom downloads $MC"; exit 1; }
javap -p -classpath "$JAR" net.minecraft.client.renderer.LightTexture | grep -E 'updateLightTexture|tickLightTexture|pixels|NativeImage|DynamicTexture'
javap -p -classpath "$JAR" net.minecraft.client.gui.Gui | grep -E 'public void render'
javap -p -classpath "$JAR" net.minecraft.client.renderer.LevelRenderer | grep -E 'setSectionDirty|allChanged'
javap -p -classpath "$JAR" net.minecraft.client.gui.screens.options.OptionsScreen | grep -nE 'lambda|init'
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

- `Blaze3D` layers moved; the async-mesher `SectionMesh` layout is still valid.

Generated by `tools/gen_deltas.py`; re-run that instead of editing.
