# Iris and Oculus compatibility

Aetherium pairs with a shader loader instead of becoming one. The whole of that relationship
is `common/src/main/java/com/aetherium/shader/IrisBridge.java` — one class, no interface
implementation, **no dependency**. There is no `modImplementation`, `compileOnly` or jar-in-jar
entry for Iris or Oculus anywhere in the build files, on purpose: a compile-time dependency on
a shader mod would couple every Aetherium version to every Iris version.

## What is bound

`IrisBridge.bind()` tries four candidates in order and takes the first whose API class
resolves; each row is `{implClass, apiClass, brand}`:

| # | implementation | API interface | brand reported |
| --- | --- | --- | --- |
| 1 | `net.irisshaders.iris.apiimpl.IrisApiV0Impl` | `net.irisshaders.iris.api.v0.IrisApi` | Iris |
| 2 | `net.coderbot.iris.Iris` | `net.coderbot.iris.api.v0.IrisApi` | Oculus |
| 3 | `net.coderbot.iris.apiimpl.IrisApiV0Impl` | `net.coderbot.iris.api.v0.IrisApi` | Oculus |
| 4 | `net.irisshaders.iris.Iris` | `net.irisshaders.iris.api.v0.IrisApi` | Iris |

Order matters: the modern Iris layout is tried first, and Oculus's `net.coderbot.iris` package
is what its releases actually ship. The instance is the API's reflective `INSTANCE` static
field; every method below is looked up with `getMethod` and stored as a `Method` that may be
null, in an immutable `Bound` snapshot published with a single volatile write.

| method | why Aetherium needs it |
| --- | --- |
| `getMinorApiRevision` | refuse a v0 minor revision we have not read about, rather than guess |
| `isShaderPackInUse` | the one question that decides whether Aetherium's lighting/gamma paths defer |
| `isRenderingShadowPass` | never edit the lightmap inside a shadow pass |
| `getConfig`, `areShadersEnabled`, `setShadersEnabledAndApply` | the Shaders tab's toggle writes through Iris' own config, so Iris re-applies its pipeline |
| `openMainIrisScreenObj(Object)`, `getMainScreenLanguageKey` | the Shaders tab embeds/links Iris' own screen instead of reimplementing its UI |
| `getSunPathRotation` | sky lighting has to match the shader pack's sun, or directional lights disagree with the shadows |

`Class.forName` + `getMethod` is the entire mechanism. `openShaderScreen` additionally resolves
`Minecraft#getInstance` and `Minecraft#setScreen(Screen)` reflectively so the bridge never
links against a loader-specific screen helper.

## Behaviour, by state

| situation | what happens |
| --- | --- |
| Iris/Oculus absent | `bind()` records an `absenceReason` ("not probed" until then), `isPresent()` is false, the Shaders tab shows the reason, and every shader path is inert. Nothing else in the engine notices. |
| `utilities.iris_integration = false` | the bridge is never bound; Aetherium treats shaders as not existing |
| bound, pack in use | `shouldSuspendLightmapEdits()` — true when `utilities.pause_dynamic_lightmap_edits` is on — makes `LightTextureMixin` skip the lightmap write, so Aetherium's gamma and dynamic lights do not fight the pack's own lighting |
| shadow pass active | the same skip, unconditionally, because a write there is wasted work at best |
| world change | `onWorldChange()` re-checks the pack state; `reloadShadersOnWorldChange` asks Iris to re-apply rather than Aetherium touching its pipeline |
| backend hot-swap | `onRendererChanged()` forwards to Iris so the pack recompiles against the new device; Aetherium invalidates its own program-binary consumers |
| API method missing on a newer/older build | that single capability turns off with a log line; the `Method` being null is the normal representation of "not available", never an NPE in the frame loop |

Everything reflective is wrapped: a `ReflectiveOperationException` becomes a recorded reason and
a `false` return. `describe()` and `getStateQueryCount()` are shown in the GUI's Advanced tab, so
"is Aetherium actually talking to Iris?" is answerable in-game rather than by log archaeology.

## Ownership rules

These are the load-bearing decisions, restated because they are where renderer/shader
combinations usually break:

1. **Iris owns the shader pipeline and the world's draw calls when a pack is active.**
   Aetherium does not inject into Iris' classes, does not `@Accessor` its internals, and does
   not assume a single field of `IrisApiConfig` beyond the two enable/disable methods above.
2. **Aetherium owns geometry-adjacent lighting**: dynamic light sources, the lightmap texture
   contents (subject to rule 3), and the backend/arena/indirect machinery under it.
3. **When a pack is active and the user asked for it, Aetherium's lightmap edits stand down**
   (`pause_dynamic_lightmap_edits`) — a mod that edits the lightmap behind a shader pack's back
   produces the classic "half the world is lit like a debugger" screenshot.
4. **`full` renderer mode and shaders are mutually exclusive by construction**: the geometry
   path that would replace vanilla section rendering is not shipped, so there is no code path
   in which Aetherium could claim ownership of the draw calls a pack is compiling against.

## Oculus specifics

Oculus exposes the same v0 surface under `net.coderbot.iris`. Two practical differences Aetherium
accounts for: the config class is reached through the same `getConfig` call but its language keys
differ (`getMainScreenLanguageKey` exists so the Shaders tab prints the *other* mod's own title
rather than a hardcoded string), and older Oculus builds lack `getSunPathRotation` — in that case
sun-path awareness is disabled and directional dynamic lights fall back to the vanilla sky angle,
logged once.

## Debugging checklist

1. Open Shaders tab → the state line. `absent` plus a reason means the candidate classes did not
   resolve (wrong mod, or a version that moved them: re-read `CANDIDATES` and the log).
2. Look for `Iris bridge bound to Iris (v0 minor N)` in `logs/latest.log`.
3. Toggle `utilities.iris_integration` off and on: `bind()` is a no-op once bound, so the
   *first* result persists until the game restarts. That is intentional (the `Method` handles are
   resolved at class-transform-adjacent time), and it is why the GUI says "restart to re-probe"
   rather than pretending the toggle is live for this option.
4. If lighting is wrong only when a pack is active, set
   `utilities.pause_dynamic_lightmap_edits = false` to test whether the pack or Aetherium owns
   the mismatch.

## What is *not* verified

The four candidate FQCNs, the method names above and the v0 revision semantics were read from the
Iris sources for the reference version. `[UNVERIFIED: …]` marks in `IrisBridge` itself list the
individual lookups that could not be confirmed against a jar (they are all in the "degrade to a
missing feature, never a crash" category, which is why the reflection is per-method rather than
per-class).
