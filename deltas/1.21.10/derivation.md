# Deriving the rest of the Minecraft 1.21.10 port

This delta is mechanical and complete for the pins; the items below must be
confirmed against the real 1.21.10 jar before a build is published.
Status of this row: **derived**. target names inferred from the renames on either side; not compiled

## 1. Read the names you need from the shipped jar

```sh
# Loom has already downloaded and mapped the game by the time `./gradlew build`
# has run once; the remapped jar is the authority, not a wiki.
MC=1.21.10
JAR=$(find "$HOME/.gradle/caches/fabric-loom" -name "minecraft-*-"$MC"-*.jar" 2>/dev/null | head -n 1)
test -n "$JAR" || { echo "run ./gradlew build once so Loom downloads $MC"; exit 1; }
javap -p -classpath "$JAR" net.minecraft.client.renderer.LightTexture | grep -E 'tick|pixels|NativeImage'
javap -p -classpath "$JAR" net.minecraft.client.gui.Gui | grep -E 'public void render|GuiGraphics'
javap -p -classpath "$JAR" net.minecraft.client.renderer.LevelRenderer | grep -E 'setSectionDirty|allChanged|Section'
javap -p -classpath "$JAR" net.minecraft.client.gui.screens.options.OptionsScreen | grep -nE 'lambda|method_'
javap -p -classpath "$JAR" net.minecraft.client.Minecraft | grep -E 'setLevel|loadWorld|getDebugOverlay'
```

## 2. Where each answer goes

- **lightmap field** - `common/src/main/java/com/aetherium/gamma/LightmapWriter.java`: the accepted field names in `probe()`; nothing else - the writer degrades with one log line if the shape is unknown
- **lightmap tick method** - `common/src/main/java/com/aetherium/mixin/core/LightTextureMixin.java`: the `method = {...}` candidate list
- **GUI overlay** - `common/src/main/java/com/aetherium/mixin/core/GuiMixin.java`: the descriptor on the primary `render` injection
- **options hijack** - `common/src/main/java/com/aetherium/mixin/core/OptionsScreenMixin.java`: the `lambda$init$N` / `method_NNNNN` candidate list
- **screen callbacks** - `common/src/main/java/com/aetherium/gui/AetheriumVideoOptionsScreen.java`: the `mouseClicked`/`mouseScrolled`/`onClose` override signatures
- **world hooks** - `common/src/main/java/com/aetherium/mixin/core/MinecraftMixin.java`: the `method = {...}` candidate list

## 3. Then re-check, do not eyeball

```sh
python3 tools/check_refs.py . && python3 tools/check.py --skip-deltas
./gradlew --no-daemon :common:compileJava :fabric:compileJava
```

A compile error on the GUI overrides is the *good* outcome: it is loud, local and
mechanical. A mixin that silently stops applying is the bad one, which is why the
HUD prints `Aetherium.describeRuntime()` and the log prints every refused mixin.
