# Aetherium port: Minecraft 1.21.10

- Java toolchain: **21** (mixin compatibilityLevel `JAVA_21`)
- Fabric loader floor: **0.16.14**, Loom **1.16.1**
- Fabric API (compile-only, optional at runtime): **0.138.4+1.21.10**
- NeoForge: **21.10.64**
- Mappings: officialMojangMappings()
- Status: **derived**

## What the delta changes

### Build pins

```properties
minecraft_version=1.21.10
java_version=21
fabric_loader_version=0.16.14
fabric_api_version=0.138.4+1.21.10
neoforge_version=21.10.64
```

### Era transforms applied by the generated patch

- `background=render`
- `be-render=extract`
- `gui-layer=stratum`
- `gui=graphics`
- `input=events`
- `light-hook=brightness`
- `occlusion-box=renderer`
- `scroll=four`
- `section-vis=render-origin`
- `weather=effect`

### Where the per-version Java comes from

Every Java difference is an era block in the source (`// @era:<name>-begin ...`), selected
by `tools/eras.py`; the variant names above say which shape this row compiles.
`python3 tools/stubcheck.py 1.21.10` compiles exactly this selection against the
Minecraft signatures CI extracted from this version's jar (`tools/probe/1.21.10.txt`).

### Version facts from earlier porting research (historical; tools/eras.py is authoritative)

- `options_access` = **getters** - Options#renderDistance()/simulationDistance() OptionInstance getters are 1.19+; 1.17-1.18.2 read public int fields, and 1.16.5-1.17.x have no simulationDistance at all (the benchmark label degrades to `sim=n/a`) [VERIFIED: the 1.19/1.19.1/1.19.2 legs compiled the getters while 1.18.2 rejected them]
- `button_builder` = **True** - Button.builder arrives with the 1.19.3 screen rework; 1.16.5-1.19.2 construct Button directly [VERIFIED: the 1.19.3 leg rejected the constructor form]
- `cycle_button` = **CycleButton** - CycleButton under that exact mojmap name from 1.17 on [VERIFIED: official-mapping javadoc @ 1.17.1/1.18.2; the 1.17/1.18.2 legs rejected CycleButtonWidget]
- `logging` = **slf4j** - slf4j on the compile classpath
- `no_remap_loom` = **False** - 26.x ships unobfuscated jars: the no-remap Loom plugin id, no mappings() line, no remapJar task [VERIFIED: sodium @ 26.2/stable applies net.fabricmc.fabric-loom @ 1.16.1 with no mappings block; the 26.x legs failed with 'Failed to find official mojang mappings']
- `options_pkg` = **screens.options** - the screens.options package is 1.21+ [VERIFIED: 1.20.5/1.20.6 legs rejected it; sodium @ 1.21.1 imports it]
- `resource_location` = **parse** - ResourceLocation.parse is 1.21+; earlier rows use the constructor
- `level_sections` = **minSectionY** - getMinSection/getMaxSection through 1.21.1, getMinSectionY/getMaxSectionY from 1.21.2 [VERIFIED: ship legs + sodium @ 1.21.4]
- `input_events` = **event** - 1.21.9 replaced the double-based mouse handlers with MouseButtonEvent forms [VERIFIED: sodium @ 26.2/stable]
- `identifier` = **False** - ResourceLocation becomes Identifier (with fromNamespaceAndPath) from 1.21.11 [VERIFIED: sodium @ 1.21.11/stable; the 1.21.9/1.21.10 legs compiled ResourceLocation.parse fine]
- `gui_extractor` = **False** - 26.x replaces GuiGraphics with GuiGraphicsExtractor, Screen#render with extractRenderState, and drawString/drawCenteredString with text/centeredText [VERIFIED: sodium @ 26.1.2/stable and 26.2/stable; this row's exact version derived from 26.2 if newer]
- `entities_iter` = **entitiesForRendering** - entitiesForRendering [VERIFIED on 1.21.1: Iris MixinLevelRenderer_SkipRendering targets ClientLevel#entitiesForRendering]
- `fabric_api` pin read from FabricMC/fabric tags on 2026-10-10 [VERIFIED tag]

## Files the generated patch touches

- `gradle.properties`
- `common/src/main/java/com/aetherium/client/AetheriumScreen.java`
- `common/src/main/java/com/aetherium/client/McCanvas.java`
- `common/src/main/java/com/aetherium/mixin/core/BlockEntityCullMixin.java`
- `common/src/main/java/com/aetherium/mixin/core/EntityOcclusionMixin.java`
- `common/src/main/java/com/aetherium/mixin/core/LightLevelMixin.java`
- `common/src/main/java/com/aetherium/mixin/core/VisibleSectionsMixin.java`
- `common/src/main/java/com/aetherium/mixin/core/WeatherMixin.java`

## Verifying this row before shipping it

The port is applied with `sh tools/port.sh 1.21.10` and then checked against the
real mapped jar - the jar is the authority, not a wiki:

```sh
MC=1.21.10
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

- Sodium has no `stable` branch for 1.21.10 at the time of writing, so the conflict table's version hints for these rows are `derived`, not verified.

Generated by `tools/gen_deltas.py`; re-run that instead of editing.
