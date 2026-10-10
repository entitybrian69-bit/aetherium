#!/usr/bin/env python3
"""Porting data + delta/matrix generator for Aetherium.

Two artefacts are produced from one machine-readable table (``tools/porting_pins.json``):

* ``PORTING_MATRIX.md`` - the human-facing 33-row table.
* ``deltas/<version>/{README.md,changes.patch,mixins.json,build.gradle.kts}`` - the
  mechanical delta a port applies, generated with ``git diff --no-index`` so that
  ``git apply --check`` proves it applies (``tools/gen_deltas.py --verify`` runs that
  for every row, and also proves the emitted ``mixins.json`` equals what the patch
  produces).

Why a JSON instead of hand-written patches: a patch copied between 32 directories
drifts. Here the patch is *derived*, so changing a rule here changes all 32 rows and
the matrix text in the same commit.

A delta contains exactly two kinds of edit:

1. **Pin edits** - exact-string substitutions on build inputs
   (``minecraft_version``, ``java_version``, ``neoforge_version``,
   ``fabric_loader_version``, ``fabric_api_version``, the loom entry in the version
   catalog, the mixin ``compatibilityLevel``, the NeoForge module include).
2. **Era transforms** - the mechanical API rename every file undergoes when the row
   predates a verified Minecraft boundary (``GuiGraphics`` -> ``PoseStack`` ->
   ``MatrixStack``, ``Component.literal`` -> ``new TextComponent`` ->
   ``new StringTextComponent``, ``updateWidgetNarration`` -> ``updateNarration`` ->
   removed, ``renderWidget`` -> ``renderButton``, widget setters -> public fields,
   the ``Button.builder`` chain -> the ``Button`` constructor, the 4-argument
   ``renderBackground`` -> the 1-argument form, ``entitiesForRendering`` ->
   ``entities``, ``addRenderableWidget`` -> ``addButton``,
   ``CycleButton`` -> ``CycleButtonWidget``).

Nothing in a delta restructures a class or changes an algorithm. If a port appears
to need that, the correct fix is in ``common/`` so every row inherits it. Every era
boundary that was read from a real upstream source is marked ``[VERIFIED: source]``
in the emitted README; the ones inferred between two verified points are marked
``[UNVERIFIED: which boundary is guessed]`` and are the first thing to check if a
ship leg fails to compile.

Statuses used in the table (the honesty part):
  ``reference``  - built and run against this exact version's mappings (1.21.1).
  ``documented`` - the pins were read from an upstream project that ships this version.
  ``derived``    - names were inferred from the rename they sit between; verify.
  ``unverified`` - plausible but unchecked: the delta ships the full transform set
                   and the recipe to verify it.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import tempfile

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(REPO, "tools"))
import eras  # noqa: E402
PINS_PATH = os.path.join(REPO, "tools", "porting_pins.json")
DELTAS_DIR = os.path.join(REPO, "deltas")
MATRIX_PATH = os.path.join(REPO, "PORTING_MATRIX.md")
REFERENCE = "1.21.1"

SRC = "common/src/main/java/com/aetherium"

# Build files a delta may rewrite (pins, the NeoForge switch, the 26.x Loom setup).
BUILD_FILES = {
    "gradle.properties": os.path.join(REPO, "gradle.properties"),
    # The Loom/ModDev plugin versions a build *applies* live in the catalog (a Kotlin DSL
    # plugins {} block cannot read a project property), so a port has to move them with the
    # property or tools/check.py rightly fails the ported tree.
    "catalog": os.path.join(REPO, "gradle", "libs.versions.toml"),
    "settings": os.path.join(REPO, "settings.gradle.kts"),
    "root_build": os.path.join(REPO, "build.gradle.kts"),
    "mixins": os.path.join(REPO, "common/src/main/resources/aetherium-common.mixins.json"),
    # 26.x ships unobfuscated jars: the no-remap Loom plugin id + the removal of the
    # mappings()/remapJar blocks live in these build files.
    "common_build": os.path.join(REPO, "common", "build.gradle.kts"),
    "fabric_build": os.path.join(REPO, "fabric", "build.gradle.kts"),
}

SOURCE_ROOTS = ("common/src/main/java", "fabric/src/main/java", "neoforge/src/main/java")


def era_files() -> dict[str, str]:
    """Every Java source with era blocks, keyed "java:<repo-relative path>"."""
    out: dict[str, str] = {}
    for root in SOURCE_ROOTS:
        for base, _, names in os.walk(os.path.join(REPO, root)):
            for name in sorted(names):
                if not name.endswith(".java"):
                    continue
                path = os.path.join(base, name)
                with open(path, encoding="utf-8") as handle:
                    if eras.has_markers(handle.read()):
                        out["java:" + os.path.relpath(path, REPO).replace(os.sep, "/")] = path
    return dict(sorted(out.items()))


FILES = dict(BUILD_FILES)
FILES.update(era_files())

# Files a delta is allowed to touch. Enforced, not advisory: build files from the list
# above, and Java sources only through their own era blocks.
ALLOWED_TARGETS = {os.path.relpath(path, REPO).replace(os.sep, "/") for path in FILES.values()}


def load_rows() -> list[dict]:
    with open(PINS_PATH, encoding="utf-8") as handle:
        data = json.load(handle)
    rows = data["rows"]
    if not any(row["version"] == REFERENCE for row in rows):
        raise SystemExit(f"porting_pins.json must contain the reference row {REFERENCE}")
    return rows


def ver_key(version: str):
    return tuple(int(x) for x in version.split("."))


# --------------------------------------------------------------------------
# 1. pin substitutions (exact string, must match exactly once)
# --------------------------------------------------------------------------

def pins_subs(row: dict) -> list[tuple[str, str, str]]:
    subs: list[tuple[str, str, str]] = []
    version = row["version"]

    subs.append(("gradle.properties", f"minecraft_version={REFERENCE}", f"minecraft_version={version}"))
    subs.append(("gradle.properties", "java_version=21", f"java_version={row['java']}"))
    if row["neoforge"]:
        subs.append(("gradle.properties", "neoforge_version=21.1.228", f"neoforge_version={row['neoforge']}"))
    else:
        # No NeoForge exists for this version (NeoForge begins at 1.20.2). The module is
        # disabled in three coordinated places - the property, the settings include and the
        # root build aggregate - because a Gradle build *configures* every included project:
        # one dangling :neoforge reference and the whole tree fails before a single compile.
        subs.append(("gradle.properties",
                     "# (VERIFIED) read from CaffeineMC/sodium @ 1.21.1/stable -> buildSrc BuildConfig.kt\nneoforge_version=21.1.228",
                     f"# No NeoForge release exists for this Minecraft version (NeoForge begins at 1.20.2);\n"
                     f"# the module is excluded here, in settings.gradle.kts and in the root build (deltas/{version}/README.md).\n"
                     f"neoforge_version=unavailable"))
        subs.append(("gradle.properties", "enabled_platforms=fabric,neoforge", "enabled_platforms=fabric"))
        subs.append(("settings", 'include("neoforge")',
                     f'// include("neoforge") - no NeoForge for {version}; see deltas/{version}/README.md'))
        subs.append(("root_build",
                     'dependsOn(":common:build", ":fabric:build", ":neoforge:build")',
                     'dependsOn(":common:build", ":fabric:build")'))
    subs.append(("gradle.properties", "fabric_loader_version=0.16.9", f"fabric_loader_version={row['fabric_loader']}"))
    fabric_api = row["facts"].get("fabric_api")
    if fabric_api:
        subs.append(("gradle.properties", "fabric_api_version=0.116.17+1.21.1", f"fabric_api_version={fabric_api}"))
    if row.get("loom"):
        subs.append(("gradle.properties", "fabric_loom_version=1.16.1", f"fabric_loom_version={row['loom']}"))
        # Same value, second file, one rule: check.py compares them, so a delta that bumped
        # only the property would fail its own ported tree - which is how to catch a port
        # that half-applied.
        subs.append(("catalog", 'loom = "1.16.1"', f'loom = "{row["loom"]}"'))

    # mixin compatibilityLevel tracks the row's toolchain
    compat = {8: "JAVA_8", 16: "JAVA_16", 17: "JAVA_17", 21: "JAVA_21", 25: "JAVA_25"}[int(row["java"])]
    subs.append(("mixins", '"compatibilityLevel": "JAVA_21"', f'"compatibilityLevel": "{compat}"'))

    # candidate-name appends (never a replacement: the reference names stay, so a
    # delta is additive and two ports can be merged without conflict)
    for target, extra in (row["facts"].get("append_candidates") or {}).items():
        subs.append((target["file"], target["find"], target["find"][:-1] + ", " + extra + "]"))
    return subs


# --------------------------------------------------------------------------
# 2. version transforms
#    * Java: every per-version difference is an era block in the source itself
#      (tools/eras.py). The delta is simply the source with each block switched to
#      this row's variant - no regex surgery, and tools/stubcheck.py compiles exactly
#      the same selection against each version's real signatures.
#    * Build files: the 26.x unobfuscated-jar Loom setup below.
# --------------------------------------------------------------------------

def build_transform(row: dict, key: str, text: str) -> tuple[str, list[str]]:
    facts = row["facts"]
    applied: list[str] = []
    if facts.get("no_remap_loom"):
        if key == "catalog":
            old_id = 'fabric-loom = { id = "net.fabricmc.fabric-loom-remap", version.ref = "loom" }'
            if old_id in text:
                text = text.replace(old_id, 'fabric-loom = { id = "net.fabricmc.fabric-loom", version.ref = "loom" }', 1)
                applied.append("loom-plugin-id->no-remap")
        if key in ("common_build", "fabric_build"):
            old_mappings = (
                "    // The layered{} form is the one CaffeineMC/sodium uses at this exact loom version\n"
                "    // (common/build.gradle.kts @ 1.21.1/stable); the officialMojangMappings() shortcut\n"
                "    // is not verified to still exist on the 1.16 line.\n"
                "    mappings(loom.layered { officialMojangMappings() })")
            new_mappings = (
                "    // 26.x ships unobfuscated jars: the no-remap Loom plugin needs no mappings()\n"
                "    // line at all (CaffeineMC/sodium @ 26.2/stable declares only the minecraft()\n"
                "    // dependency; a mappings lookup is what produced \"Failed to find official\n"
                "    // mojang mappings for 26.x\").")
            if old_mappings in text:
                text = text.replace(old_mappings, new_mappings, 1)
                applied.append("mappings-line-removed")
        if key == "fabric_build":
            # The no-remap plugin has no remap configurations: mods are plain
            # dependencies. sodium @ 26.2/stable uses implementation(...) for the
            # loader and include(module) for jar-in-jar (include survives); the legs
            # failed script compilation on the unresolved modImplementation/
            # modCompileOnly accessors.
            for old_cfg, new_cfg in (("modImplementation(", "implementation("),
                                      ("modCompileOnly(", "compileOnly(")):
                if old_cfg in text:
                    text = text.replace(old_cfg, new_cfg, 1)
                    applied.append(f"{old_cfg[:-1]}->{new_cfg[:-1]}")
            old_remap = (
                "tasks.withType<net.fabricmc.loom.task.RemapJarTask> {\n"
                "    addNestedDependencies = true\n"
                "}\n"
                "\n"
                "tasks.named(\"build\") {\n"
                "    dependsOn(\"remapJar\")\n"
                "}")
            new_remap = (
                "// No remapJar block: the no-remap Loom plugin (\"net.fabricmc.fabric-loom\", see the\n"
                "// catalog) has no remapJar task for 26.x's unobfuscated jars - the plain jar task\n"
                "// builds the shipped artifact, and the nested-dependency flag has no remap step to\n"
                "// configure (the include(...) entries above are already jar-in-jar).")
            if old_remap in text:
                text = text.replace(old_remap, new_remap, 1)
                applied.append("remapJar-block-removed")
    return text, applied


def era_transform(row: dict, key: str, text: str) -> tuple[str, list[str]]:
    """Apply this row's transforms to one file. Returns (text, applied rule names)."""
    if key.startswith("java:"):
        used: set[str] = set()
        out = eras.select_all(text, row["version"], key[5:], used)
        return out, sorted(f"{name}={eras.variant_for(name, row['version'])}" for name in used)
    return build_transform(row, key, text)


def apply_edits(text: str, subs: list[tuple[str, str, str]], key: str, row: dict) -> str:
    out = text
    for target, find, replace in subs:
        if target != key:
            continue
        if find not in out:
            raise SystemExit(f"porting rule for {row['version']} does not match {key}: anchor "
                             f"{find[:60]!r} is missing. The reference source changed; fix "
                             f"tools/gen_deltas.py instead of hand-writing a patch.")
        if out.count(find) != 1:
            raise SystemExit(f"porting rule for {row['version']} is ambiguous in {key}: "
                             f"{out.count(find)} matches for {find[:60]!r}")
        out = out.replace(find, replace, 1)
    return out


# --------------------------------------------------------------------------
# 3. delta emission
# --------------------------------------------------------------------------

def version_transforms(row: dict) -> dict[str, tuple[str, list[str]]]:
    """Return {file_key: (transformed_text, applied_rules)} for every changed file."""
    subs = pins_subs(row)
    results: dict[str, tuple[str, list[str]]] = {}
    all_applied: set[str] = set()
    for key, path in FILES.items():
        text = open(path, encoding="utf-8").read()
        changed = apply_edits(text, subs, key, row)
        changed, applied = era_transform(row, key, changed)
        all_applied.update(applied)
        if changed != text:
            results[key] = (changed, applied)
    if not results:
        raise SystemExit(f"row {row['version']} produced no changes - the pins equal the reference")
    return results


def build_patch(row: dict, out_dir: str) -> list[str]:
    """Write deltas/<version>/{changes.patch,README.md,mixins.json,build.gradle.kts}."""
    with tempfile.TemporaryDirectory() as work:
        orig = os.path.join(work, "orig")
        new = os.path.join(work, "new")
        touched: list[str] = []
        results = version_transforms(row)
        for key, _ in FILES.items():
            if key not in results:
                continue
            path = FILES[key]
            text = open(path, encoding="utf-8").read()
            changed = results[key][0]
            rel = os.path.relpath(path, REPO)
            if rel not in ALLOWED_TARGETS:
                raise SystemExit(f"refusing to emit a delta that touches {rel}: not in ALLOWED_TARGETS")
            for base, tree in ((orig, text), (new, changed)):
                dest = os.path.join(base, rel)
                os.makedirs(os.path.dirname(dest), exist_ok=True)
                with open(dest, "w", encoding="utf-8", newline="\n") as handle:
                    handle.write(tree)
            touched.append(rel)
        proc = subprocess.run(["git", "diff", "--no-index", "-U3", "--src-prefix=a/", "--dst-prefix=b/", "orig", "new"],
                              cwd=work, capture_output=True, text=True)
        if proc.returncode not in (0, 1):
            raise SystemExit(f"git diff --no-index failed: {proc.stderr[:400]}")
        patch = proc.stdout.replace("diff --git a/orig/", "diff --git a/").replace(" b/new/", " b/") \
            .replace("--- a/orig/", "--- a/").replace("+++ b/new/", "+++ b/")
        header = (f"# Aetherium port delta: Minecraft {row['version']}\n"
                  f"# generated by tools/gen_deltas.py from tools/porting_pins.json - do not hand-edit,\n"
                  f"# edit the table and re-run (that is what keeps 32 rows from drifting).\n"
                  f"# status: {row['status']}\n"
                  f"# apply:  git apply --whitespace=nowarn deltas/{row['version']}/changes.patch\n"
                  f"# undo:   git apply -R --whitespace=nowarn deltas/{row['version']}/changes.patch\n")
        os.makedirs(out_dir, exist_ok=True)
        with open(os.path.join(out_dir, "changes.patch"), "w", encoding="utf-8", newline="\n") as handle:
            handle.write(header + patch)
        write_docs(row, out_dir, touched, results)
    return touched


def write_docs(row: dict, out_dir: str, touched: list[str], results: dict) -> None:
    facts = row["facts"]
    version = row["version"]
    applied_rules = sorted({rule for _, rules in results.values() for rule in rules})

    readme = [
        f"# Aetherium port: Minecraft {version}",
        "",
        f"- Java toolchain: **{row['java']}** (mixin compatibilityLevel `JAVA_{row['java']}`)",
        f"- Fabric loader floor: **{row['fabric_loader']}**" + (f", Loom **{row['loom']}**" if row.get("loom") else ""),
        f"- Fabric API (compile-only, optional at runtime): **{facts.get('fabric_api', 'n/a')}**",
        f"- NeoForge: **{row['neoforge'] or 'does not exist for this version - the neoforge module is disabled'}**",
        f"- Mappings: {row['mappings']}",
        f"- Status: **{row['status']}**",
        "",
        "## What the delta changes",
        "",
        "### Build pins",
        "",
        "```properties",
        f"minecraft_version={version}",
        f"java_version={row['java']}",
        f"fabric_loader_version={row['fabric_loader']}",
        f"fabric_api_version={facts.get('fabric_api', 'n/a')}",
        f"neoforge_version={row['neoforge'] or 'unavailable (module disabled)'}",
        "```",
        "",
        "### Era transforms applied by the generated patch",
        "",
    ]
    if applied_rules:
        for rule in applied_rules:
            readme.append(f"- `{rule}`")
    else:
        readme.append("- none: this row's era facts equal the reference (pin-only delta)")
    readme += [
        "",
        "### Where the per-version Java comes from",
        "",
        "Every Java difference is an era block in the source (`// @era:<name>-begin ...`), selected",
        "by `tools/eras.py`; the variant names above say which shape this row compiles.",
        "`python3 tools/stubcheck.py " + version + "` compiles exactly this selection against the",
        "Minecraft signatures CI extracted from this version's jar (`tools/probe/" + version + ".txt`).",
        "",
        "### Version facts from earlier porting research (historical; tools/eras.py is authoritative)",
        "",
        f"- `options_access` = **{facts.get('options_access', 'getters')}** - Options#renderDistance()/"
          "simulationDistance() OptionInstance getters are 1.19+; 1.17-1.18.2 read public int fields, "
          "and 1.16.5-1.17.x have no simulationDistance at all (the benchmark label degrades to "
          "`sim=n/a`) [VERIFIED: the 1.19/1.19.1/1.19.2 legs compiled the getters while 1.18.2 "
          "rejected them]",
        f"- `button_builder` = **{facts.get('button_builder', True)}** - Button.builder arrives with "
          "the 1.19.3 screen rework; 1.16.5-1.19.2 construct Button directly [VERIFIED: the 1.19.3 "
          "leg rejected the constructor form]",
        f"- `cycle_button` = **{facts.get('cycle_button', 'CycleButton')}** - "
        + ("CycleButton under that exact mojmap name from 1.17 on [VERIFIED: official-mapping "
           "javadoc @ 1.17.1/1.18.2; the 1.17/1.18.2 legs rejected CycleButtonWidget]"
           if facts.get("cycle_button", "CycleButton") == "CycleButton" else
           "1.16.5 has no CycleButton; the vanilla-controls counter counts AbstractWidget "
           "[the 1.16.5 leg rejected the CycleButton import]"),
        f"- `logging` = **{facts.get('logging', 'slf4j')}** - "
        + ("slf4j on the compile classpath"
           if facts.get("logging", "slf4j") == "slf4j" else
           "1.16.5 has log4j2 only: AetheriumLog swaps the two imports and the factory "
           "[VERIFIED: the 1.16.5 leg rejected org.slf4j]"),
        f"- `no_remap_loom` = **{facts.get('no_remap_loom', False)}** - 26.x ships unobfuscated "
          "jars: the no-remap Loom plugin id, no mappings() line, no remapJar task "
          "[VERIFIED: sodium @ 26.2/stable applies net.fabricmc.fabric-loom @ 1.16.1 with no "
          "mappings block; the 26.x legs failed with 'Failed to find official mojang mappings']",
        f"- `options_pkg` = **{facts['options_pkg']}** - the screens.options package is 1.21+ "
          "[VERIFIED: 1.20.5/1.20.6 legs rejected it; sodium @ 1.21.1 imports it]",
        f"- `resource_location` = **{facts['resource_location']}** - ResourceLocation.parse is 1.21+; earlier rows use the constructor",
        f"- `level_sections` = **{facts['level_sections']}** - getMinSection/getMaxSection through 1.21.1, "
          "getMinSectionY/getMaxSectionY from 1.21.2 [VERIFIED: ship legs + sodium @ 1.21.4]",
        f"- `input_events` = **{facts['input_events']}** - 1.21.9 replaced the double-based mouse "
          "handlers with MouseButtonEvent forms [VERIFIED: sodium @ 26.2/stable]",
        f"- `identifier` = **{facts.get('identifier', False)}** - ResourceLocation becomes Identifier "
          "(with fromNamespaceAndPath) from 1.21.11 [VERIFIED: sodium @ 1.21.11/stable; the "
          "1.21.9/1.21.10 legs compiled ResourceLocation.parse fine]",
        f"- `gui_extractor` = **{facts.get('gui_extractor', False)}** - 26.x replaces GuiGraphics with "
          "GuiGraphicsExtractor, Screen#render with extractRenderState, and drawString/drawCenteredString "
          "with text/centeredText [VERIFIED: sodium @ 26.1.2/stable and 26.2/stable; this row's exact "
          "version derived from 26.2 if newer]",
        f"- `entities_iter` = **{facts['entities_iter']}** - entitiesForRendering "
          "[VERIFIED on 1.21.1: Iris MixinLevelRenderer_SkipRendering targets ClientLevel#entitiesForRendering]",
        f"- `fabric_api` pin read from FabricMC/fabric tags on 2026-10-10"
        + (" [VERIFIED tag]" if version == "1.21.1" or version not in ("1.16.5", "1.17.1", "1.18.1")
           else " [UNVERIFIED: no per-version tag exists for this line; the base-version pin is used]"),
        "",
        "## Files the generated patch touches",
        "",
    ]
    for path in touched:
        readme.append(f"- `{path}`")
    readme += [
        "",
        "## Verifying this row before shipping it",
        "",
        "The port is applied with `sh tools/port.sh " + version + "` and then checked against the",
        "real mapped jar - the jar is the authority, not a wiki:",
        "",
        "```sh",
        "MC=" + version,
        'JAR=$(find "$HOME/.gradle/caches/fabric-loom" -name "minecraft-*-\"$MC\"-*.jar" 2>/dev/null | head -n 1)',
        'test -n "$JAR" || { echo "run ./gradlew build once so Loom downloads $MC"; exit 1; }',
        "javap -p -classpath \"$JAR\" net.minecraft.client.renderer.LightTexture | grep -E 'updateLightTexture|tickLightTexture|pixels|NativeImage|DynamicTexture'",
        "javap -p -classpath \"$JAR\" net.minecraft.client.gui.Gui | grep -E 'public void render'",
        "javap -p -classpath \"$JAR\" net.minecraft.client.renderer.LevelRenderer | grep -E 'setSectionDirty|allChanged'",
        "javap -p -classpath \"$JAR\" net.minecraft.client.gui.screens." +
        ("options." if facts["options_pkg"] == "screens.options" else "") + "OptionsScreen | grep -nE 'lambda|init'",
        "javap -p -classpath \"$JAR\" net.minecraft.client.gui.components.AbstractWidget | grep -E 'renderWidget|renderButton|setX|updateNarration'",
        "```",
        "",
        "```sh",
        "python3 tools/check_refs.py . && python3 tools/check.py --skip-deltas",
        "./gradlew --no-daemon :common:compileJava :fabric:compileJava",
        "```",
        "",
        "A compile error on the GUI overrides is the *good* outcome: it is loud, local and",
        "mechanical. A mixin that silently stops applying is the bad one, which is why the",
        "HUD prints `Aetherium.describeRuntime()` and the log prints every refused mixin.",
        "",
        "## Notes",
        "",
    ]
    for line in facts.get("notes", []):
        readme.append(f"- {line}")
    readme += ["", "Generated by `tools/gen_deltas.py`; re-run that instead of editing.", ""]
    with open(os.path.join(out_dir, "README.md"), "w", encoding="utf-8", newline="\n") as handle:
        handle.write("\n".join(readme))

    # mixins.json: the exact mixin config this row ships (compatibilityLevel and all).
    # tools/port.sh copies it over the tree's file after applying the patch, and
    # --verify proves the two paths produce the same bytes.
    mixins_key = "mixins"
    mixins_text = results.get(mixins_key, (open(FILES[mixins_key], encoding="utf-8").read(), []))[0]
    with open(os.path.join(out_dir, "mixins.json"), "w", encoding="utf-8", newline="\n") as handle:
        handle.write(mixins_text)

    # build.gradle.kts: a runnable guard script that fails the port when the tree's pins
    # drift from this row. tools/port.sh executes it after applying the patch; it is the
    # delta's build file in the most literal sense - the thing the build runs against.
    guard = f"""// Aetherium port guard: Minecraft {version}
// Generated by tools/gen_deltas.py. Run from the repo root after applying the delta:
//   kscript deltas/{version}/build.gradle.kts   (or: kotlinc -script ... / gradle -b ... )
// It re-reads the tree's pins and exits non-zero on any drift, because a port that
// half-applied is worse than one that fails loudly.
@file:Suppress("UNUSED_VARIABLE")

import java.io.File

val expect = mapOf(
    "minecraft_version" to "{version}",
    "java_version" to "{row['java']}",
    "fabric_loader_version" to "{row['fabric_loader']}",
    "fabric_api_version" to "{facts.get('fabric_api', 'n/a')}",
    "neoforge_version" to "{row['neoforge'] or 'unavailable'}"
)

val props = File(System.getProperty("user.dir"), "gradle.properties")
if (!props.isFile()) {{
    System.err.println("gradle.properties not found under " + System.getProperty("user.dir"))
    kotlin.system.exitProcess(2)
}}
var failures = 0
for ((key, want) in expect) {{
    val line = props.readLines().firstOrNull {{ it.startsWith("$key=") }}
    val have = line?.substringAfter('=') ?: "<missing>"
    if (have != want) {{
        System.err.println("PIN DRIFT $key: expected $want, found $have")
        failures++
    }}
}}
if (failures == 0) println("pins ok for {version}")
kotlin.system.exitProcess(if (failures == 0) 0 else 1)
"""
    with open(os.path.join(out_dir, "build.gradle.kts"), "w", encoding="utf-8", newline="\n") as handle:
        handle.write(guard)


# --------------------------------------------------------------------------
# 4. matrix
# --------------------------------------------------------------------------

def render_matrix(rows: list[dict]) -> str:
    lines = [
        "# Porting matrix",
        "",
        "Minecraft **1.21.1** is the reference implementation: the source in `common/` is",
        "written against its real mappings, and the other 32 rows are *this same source*",
        "plus a mechanical delta in `deltas/<version>/`.",
        "",
        "This file is generated - edit `tools/porting_pins.json` and run",
        "`python3 tools/gen_deltas.py --all && python3 tools/gen_deltas.py --matrix`.",
        "Every statement here is graded:",
        "",
        "| status | meaning |",
        "| --- | --- |",
        "| `reference` | the version the source is written and built against |",
        "| `documented` | pins read from an upstream project that ships this version |",
        "| `derived` | inferred from the renames on either side; verify before shipping |",
        "| `unverified` | plausible, not checked; the delta ships the recipe to check it |",
        "",
        "Era boundaries that were read from real upstream sources (Iris branches 1.19.4,",
        "1.20.1, 1.20.6, 1.21.1; CaffeineMC/sodium @ 1.21.1/stable; LWJGL generated",
        "sources; FabricMC and NeoForge tag listings) are marked VERIFIED in each delta's",
        "README; boundaries inferred between two verified points are marked UNVERIFIED and",
        "fail loudly (a compile error) rather than silently when wrong.",
        "",
        "No row claims a measured frame-rate result, and no row claims to have been run",
        "unless it is the reference row. Anything else would be a fabricated deliverable.",
        "",
        "| Minecraft | Java | Fabric loader | NeoForge | Stack | Text | Narration | status | delta |",
        "| --- | --- | --- | --- | --- | --- | --- | --- | --- |",
    ]
    for row in rows:
        facts = row["facts"]
        stack = facts.get("stack_class", "?")
        text_era = {"component": "Component", "text": "TextComponent", "string_text": "StringTextComponent"}[
            facts.get("component_era", "component")]
        narration = facts.get("narration", "?")
        if row["version"] == REFERENCE:
            version_cell = f"**{row['version']}**"
            delta = "the tree itself - [`common/`](common/src/main/java/com/aetherium), "
            delta += "[`gradle.properties`](gradle.properties)"
        else:
            directory = f"deltas/{row['version']}"
            version_cell = f"[{row['version']}]({directory}/)"
            delta = (f"[patch]({directory}/changes.patch) \u00b7 "
                     f"[README]({directory}/README.md) \u00b7 "
                     f"[mixins.json]({directory}/mixins.json) \u00b7 "
                     f"[build.gradle.kts]({directory}/build.gradle.kts)")
        lines.append(f"| {version_cell} | {row['java']} | {row['fabric_loader']} | "
                     f"{row['neoforge'] or 'n/a'} | {stack} | {text_era} | {narration} | {row['status']} | {delta} |")
    lines += [
        "",
        f"{len(rows)} rows: 1 reference + {len(rows) - 1} deltas.",
        "",
        "## All versions",
        "",
        "Every row, linked. A delta directory holds four files: `changes.patch` (what to",
        "apply), `README.md` (every era fact and its provenance, plus the recipe to check",
        "the row against the real mapped jar), `mixins.json` (the exact mixin config the",
        "ported tree ships) and `build.gradle.kts` (a pin guard the port runs to prove",
        "the tree and this row agree).",
        "",
    ]
    groups = [
        ("1.16 - 1.18 (Java 8/16/17, MatrixStack/PoseStack, TextComponent)", lambda v: v.startswith(("1.16", "1.17", "1.18"))),
        ("1.19 - 1.20 (PoseStack -> GuiGraphics at 1.20, narration at 1.19.4)", lambda v: v.startswith(("1.19", "1.20"))),
        ("1.21.x (the reference line and its successors)", lambda v: v.startswith("1.21")),
        ("Date-based ids (26.x)", lambda v: not v.startswith("1.")),
    ]
    for title, matches in groups:
        members = [row for row in rows if matches(row["version"])
                   and row["version"] != REFERENCE]
        if not members:
            continue
        lines.append(f"### {title}")
        lines.append("")
        for row in members:
            directory = f"deltas/{row['version']}"
            lines.append(
                f"- [{row['version']}]({directory}/) - Java {row['java']}, "
                f"[patch]({directory}/changes.patch), "
                f"[README]({directory}/README.md) - `{row['status']}`"
            )
        lines.append("")
    reference_row = [row for row in rows if row["version"] == REFERENCE]
    lines.append(f"### {REFERENCE} - the reference version")
    lines.append("")
    if reference_row:
        row = reference_row[0]
        lines.append(f"- no delta: this tree *is* the {REFERENCE} build - "
                     "[`gradle.properties`](gradle.properties), "
                     "[`common/`](common/src/main/java/com/aetherium)")
        lines.append(f"- Java {row['java']}, Fabric loader {row['fabric_loader']}, "
                     f"Loom {row['loom']}, NeoForge {row['neoforge']} - "
                     f"provenance in [docs/BUILD_PINS.md](docs/BUILD_PINS.md)")
    lines += [
        "",
        "## What a delta does and does not contain",
        "",
        "`deltas/<version>/changes.patch` may change only:",
        "",
        "1. build pins (`minecraft_version`, `java_version`, `neoforge_version`,",
        "   `fabric_loader_version`, `fabric_api_version`, `fabric_loom_version` + the",
        "   matching `loom` entry in `gradle/libs.versions.toml`, `enabled_platforms`, the",
        "   NeoForge include in `settings.gradle.kts` and the root build aggregate);",
        "2. the mixin `compatibilityLevel` (it tracks the row's Java toolchain);",
        "3. `REFERENCE`-side strings in `AetheriumMixinPlugin` (the announced version and",
        "   the per-mixin target ranges);",
        "4. the era transforms listed in each delta's README - the mechanical",
        "   API renames (stack class, text components, narration, widget methods,",
        "   `renderBackground` arity, widget setters, `Button.builder`, entity iteration,",
        "   `CycleButton`) - applied as ordered whole-file rewrites;",
        "5. an *append* to an existing `method = { ... }` candidate list - reference names",
        "   are never removed, so two ports merge without conflict.",
        "",
        "It never restructures a class, fixes a bug, or changes an algorithm.",
        "`tools/gen_deltas.py` refuses to emit anything else, and `tools/check.py` proves",
        "every patch still applies with `git apply --check`.",
        "",
        "### Loom, and why one version serves all 33 rows",
        "",
        "Every row pins Loom to the reference value. `gradle/wrapper/gradle-wrapper.properties`",
        "is tree-wide (Gradle 9.4.1) and no delta rewrites it, and a Loom from the 1.2 or 1.6 era",
        "does not run on Gradle 9 - so an era-matched Loom pin would describe a build this tree",
        "cannot start. Loom itself is Minecraft-version-agnostic (the Minecraft artifact and the",
        "intermediary/mojmap channel decide the version), and the plugin id and version are read",
        "from FabricMC/fabric-loom's own `gradlePlugin` block (`net.fabricmc.fabric-loom-remap`).",
        "A port that genuinely needs a different Loom must bump the wrapper, `gradle.properties`",
        "and `gradle/libs.versions.toml` together; `tools/check.py` fails if the last two",
        "disagree, which is the guard against half a bump.",
        "",
        "## Applying one",
        "",
        "```sh",
        "sh tools/port.sh --list            # what is known about each row",
        "sh tools/port.sh --dry-run 1.20.6  # apply to a copy, run the checks, throw it away",
        "sh tools/port.sh 1.20.6            # apply to this tree (patch + mixins.json + pin guard)",
        "sh tools/port.sh --revert 1.20.6   # undo",
        "```",
        "",
        "## Per-version notes worth reading before you port",
        "",
    ]
    for row in rows:
        for note in row["facts"].get("notes", []):
            lines.append(f"- **{row['version']}** - {note}")
            break  # one line per row keeps this section readable; full notes are in deltas/
    lines += [
        "",
        "## Loader availability",
        "",
        "NeoForge begins at 1.20.2, so rows below it are Fabric-only. For those rows the",
        "delta edits three coordinated places - `enabled_platforms=fabric`, the",
        "`include(\"neoforge\")` line in `settings.gradle.kts`, and the `:neoforge:build`",
        "reference in the root build aggregate - because Gradle configures every included",
        "project and one dangling reference fails the whole tree before a single compile.",
        "For those versions the practical alternatives are Fabric (Pojav/Zalith on Android)",
        "or the mod's own Fabric jar under Sapphire; `docs/ARCHITECTURE.md` explains why no",
        "legacy-Forge port exists in this repository: the mixin surface differs enough that",
        "it would be a second codebase wearing a delta costume.",
        "",
        "## Verification record",
        "",
        "The 1.21.1 mixin target names and era boundaries in this tree were read from real",
        "sources, not memory: `CaffeineMC/sodium @ 1.21.1/stable` (LevelRenderer,",
        "OptionsScreen, GameRenderer mixin sets and every build pin),",
        "`IrisShaders/Iris @ 1.19.4 / 1.20.1 / 1.20.6 / 1.21.1` (Gui, GameRenderer,",
        "LightTexture mixins, screen render/renderBackground shapes, narration name),",
        "LWJGL's generated `GL44C`/`GL46C`/`ARBParallelShaderCompile` sources (GL constant",
        "values), and the FabricMC, neoforged and IrisShaders tag listings (pin",
        "existence). Where a name could not be read, the code or the delta README carries",
        "an `[UNVERIFIED: ...]` mark and a tolerant injection -",
        "`python3 tools/check.py --report-unverified` lists them all.",
        "",
    ]
    return "\n".join(lines).replace("```\n```\n", "```\n")


# --------------------------------------------------------------------------
# 5. main
# --------------------------------------------------------------------------

def verify(rows: list[dict]) -> int:
    bad = []
    # The reference build applies no patch, so every era block must already sit on its
    # reference variant; otherwise 1.21.1 would compile another version's code.
    for key, path in FILES.items():
        if key.startswith("java:"):
            text = open(path, encoding="utf-8").read()
            if eras.select_all(text, REFERENCE, key[5:]) != text:
                bad.append((REFERENCE, [f"{key[5:]} is not on its {REFERENCE} variants "
                                        f"(normalise with eras.select_all)"]))
    checked = 0
    skipped = []
    for row in rows:
        out_dir = os.path.join(DELTAS_DIR, row["version"])
        patch = os.path.join(out_dir, "changes.patch")
        if not os.path.exists(patch):
            # Counted separately on purpose: reporting "33 patches apply" when the
            # reference version has no patch to apply would be a wrong sentence about
            # the state of the tree, and this line is what a CI log shows.
            skipped.append(row["version"])
            continue
        checked += 1
        proc = subprocess.run(["git", "apply", "--check", "--whitespace=nowarn", patch],
                              cwd=REPO, capture_output=True, text=True)
        if proc.returncode != 0:
            bad.append((row["version"], (proc.stderr or proc.stdout).strip().splitlines()[:1]))
            continue
        # the emitted mixins.json must equal what the patch produces for that file
        emitted = open(os.path.join(out_dir, "mixins.json"), encoding="utf-8").read()
        results = version_transforms(row)
        mixins_key = "mixins"
        expected = results.get(mixins_key, (open(FILES[mixins_key], encoding="utf-8").read(), []))[0]
        if emitted != expected:
            bad.append((row["version"], ["emitted mixins.json differs from the patch result"]))
    if bad:
        for version, first in bad:
            print(f"FAIL deltas/{version}: {first}")
        return 1
    print(f"all {checked} delta patches apply cleanly to the current tree, and every emitted "
          f"mixins.json matches its patch result (no patch for: {', '.join(skipped)} - the reference is the tree)")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Aetherium porting data generator")
    parser.add_argument("--all", action="store_true", help="regenerate every delta")
    parser.add_argument("--only", help="regenerate one version's delta")
    parser.add_argument("--matrix", action="store_true", help="regenerate PORTING_MATRIX.md")
    parser.add_argument("--verify", action="store_true", help="check every patch applies and matches its emitted files")
    args = parser.parse_args()
    rows = load_rows()

    if args.matrix or not (args.all or args.only or args.verify):
        with open(MATRIX_PATH, "w", encoding="utf-8", newline="\n") as handle:
            handle.write(render_matrix(rows))
        print(f"wrote {os.path.relpath(MATRIX_PATH, REPO)} ({len(rows)} rows)")
        if not (args.all or args.only or args.verify):
            return 0

    targets = rows if args.all else [row for row in rows if row["version"] == args.only]
    if args.only and not targets:
        print(f"no row for {args.only}", file=sys.stderr)
        return 2
    for row in targets:
        if row["version"] == REFERENCE:
            continue  # the reference version needs no delta by definition
        out_dir = os.path.join(DELTAS_DIR, row["version"])
        build_patch(row, out_dir)
        print(f"wrote deltas/{row['version']}/ (changes.patch, README.md, mixins.json, build.gradle.kts)")

    if args.verify:
        return verify(rows)
    return 0


if __name__ == "__main__":
    sys.exit(main())
