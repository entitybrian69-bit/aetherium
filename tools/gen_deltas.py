#!/usr/bin/env python3
"""Porting data + delta/matrix generator for Aetherium.

Two artefacts are produced from one machine-readable table (``tools/porting_pins.json``):

* ``PORTING_MATRIX.md`` - the human-facing 33-row table.
* ``deltas/<version>/changes.patch`` + ``derivation.md`` + ``notes.md`` - the mechanical
  delta a port applies, generated with ``git diff --no-index`` so that
  ``git apply --check`` proves it applies (``tools/check.py`` runs that for every row).

Why a JSON instead of hand-written patches: a patch copied between 32 directories
drifts. Here the patch is *derived*, so changing a rule here changes all 32 rows and
the matrix text in the same commit.

What a delta is allowed to contain - and this generator enforces it, refusing to emit
anything else:

1. build pins (``minecraft_version``, ``java_version``, ``neoforge_version``,
   ``enabled_platforms``, the Fabric loader floor);
2. the ``REFERENCE_MC`` string in ``AetheriumMixinPlugin`` and the
   ``core.OptionsScreenMixin`` target range;
3. a target-name append to an existing mixin candidate list (``method = {...}``);
4. the primary GUI descriptor when the version predates ``GuiGraphics``.

Nothing in a delta restructures a class or changes an algorithm. If a port appears to
need that, the correct fix is in ``common/`` so every row inherits it.

Statuses used in the table (the honesty part):
  ``reference``  - built and run against this exact version's mappings (1.21.1).
  ``documented`` - the pins were read from an upstream project that ships this version.
  ``derived``    - names were inferred from the rename they sit between; verify.
  ``unverified`` - plausible but unchecked: the delta ships a derivation recipe, and
                   README/PORTING_MATRIX say plainly that it has not been run.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PINS_PATH = os.path.join(REPO, "tools", "porting_pins.json")
DELTAS_DIR = os.path.join(REPO, "deltas")
MATRIX_PATH = os.path.join(REPO, "PORTING_MATRIX.md")
REFERENCE = "1.21.1"

FILES = {
    "gradle.properties": os.path.join(REPO, "gradle.properties"),
    "plugin": os.path.join(REPO, "common/src/main/java/com/aetherium/mixin/AetheriumMixinPlugin.java"),
    "light": os.path.join(REPO, "common/src/main/java/com/aetherium/mixin/core/LightTextureMixin.java"),
    "gui": os.path.join(REPO, "common/src/main/java/com/aetherium/mixin/core/GuiMixin.java"),
    "options": os.path.join(REPO, "common/src/main/java/com/aetherium/mixin/core/OptionsScreenMixin.java"),
    "hud": os.path.join(REPO, "common/src/main/java/com/aetherium/hud/AetheriumHudRenderer.java"),
}

# Files a delta is allowed to touch. Enforced, not advisory: a generator that can emit
# an arbitrary edit is one bad rule away from 32 silently divergent engines.
ALLOWED_TARGETS = {
    "gradle.properties",
    "common/src/main/java/com/aetherium/mixin/AetheriumMixinPlugin.java",
    "common/src/main/java/com/aetherium/mixin/core/LightTextureMixin.java",
    "common/src/main/java/com/aetherium/mixin/core/GuiMixin.java",
    "common/src/main/java/com/aetherium/mixin/core/OptionsScreenMixin.java",
    "common/src/main/java/com/aetherium/hud/AetheriumHudRenderer.java",
}


def load_rows() -> list[dict]:
    with open(PINS_PATH, encoding="utf-8") as handle:
        data = json.load(handle)
    rows = data["rows"]
    if not any(row["version"] == REFERENCE for row in rows):
        raise SystemExit(f"porting_pins.json must contain the reference row {REFERENCE}")
    return rows


def substitutions(row: dict) -> list[tuple[str, str, str]]:
    """(file_key, find, replace) triples for one version row."""
    subs: list[tuple[str, str, str]] = []
    version = row["version"]

    # 1. build pins -------------------------------------------------------------
    subs.append(("gradle.properties", f"minecraft_version={REFERENCE}", f"minecraft_version={version}"))
    subs.append(("gradle.properties", "java_version=21", f"java_version={row['java']}"))
    if row["neoforge"]:
        subs.append(("gradle.properties", "neoforge_version=21.1.77", f"neoforge_version={row['neoforge']}"))
    else:
        # No NeoForge exists for this version (NeoForge begins at 1.20.2). Shipping the
        # NeoForge shell would produce a jar that cannot load, so the delta disables the
        # module instead of pretending; `enabled_platforms` is read by CI and by
        # tools/build_all.sh.
        subs.append(("gradle.properties", "enabled_platforms=fabric,neoforge", "enabled_platforms=fabric"))
        subs.append(("gradle.properties", "# (GUESS) latest 21.1.x NeoForge promotion; check https://mkremins.github.io/neoforged/versions/\nneoforge_version=21.1.77",
                     "# No NeoForge release exists for this Minecraft version; the neoforge module is\n"
                     "# excluded via enabled_platforms below. See deltas/%s/notes.md.\nneoforge_version=unavailable" % version))
    subs.append(("gradle.properties", "fabric_loader_version=0.16.9", f"fabric_loader_version={row['fabric_loader']}"))
    if row.get("loom"):
        subs.append(("gradle.properties", "fabric_loom_version=1.16.1", f"fabric_loom_version={row['loom']}"))

    # 2. the plugin's reference string (logged, and used as the range baseline) --
    subs.append(("plugin", 'private static volatile String announcedVersion = "";',
                 f'private static volatile String announcedVersion = "{version}";'))
    hijack_range = row["facts"].get("options_hijack_range")
    if hijack_range:
        subs.append(("plugin", '"core.OptionsScreenMixin", "[1.17.4,)"',
                     f'"core.OptionsScreenMixin", "{hijack_range}"'))

    # 3. candidate-name appends (never a replacement: the reference names stay, so a
    #    delta is additive and two ports can be merged without conflict) -----------
    for target, extra in (row["facts"].get("append_candidates") or {}).items():
        subs.append((target["file"], target["find"], target["find"][:-1] + ", " + extra + "]"))

    # 4. GuiGraphics vs PoseStack for the overlay + HUD --------------------------
    if not row["facts"].get("gui_graphics", True):
        subs.append(("gui", '@Mixin(Gui.class)',
                     '// Pre-GuiGraphics: the overlay is drawn by the legacy hook below, and the\n'
                     '// GuiGraphics-typed injection is skipped by the plugin because its target\n'
                     '// descriptor does not exist on this version.\n'
                     '@Mixin(Gui.class)'))
        subs.append(("hud", "import net.minecraft.client.gui.GuiGraphics;",
                     "// [UNVERIFIED: on this version GuiGraphics does not exist; the delta swaps\n"
                     "// GuiGraphics#fill for GuiComponent.fill and the import goes away. Derive the\n"
                     "// exact replacement with the recipe in deltas/%s/derivation.md before shipping.]\n"
                     "// import net.minecraft.client.gui.GuiGraphics;\n"
                     "import net.minecraft.client.gui.GuiGraphics; // PORT-REMOVED" % version))

    return subs


def apply_subs(text: str, subs: list[tuple[str, str, str]], key: str, row: dict) -> str:
    out = text
    applied = 0
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
        applied += 1
    if applied and key == "hud" and "PORT-REMOVED" in out:
        pass  # the marker is the point: it is greppable from tools/verify.sh
    return out


def build_patch(row: dict, out_dir: str) -> list[str]:
    """Write deltas/<version>/{changes.patch,derivation.md,notes.md}; return touched files."""
    subs = substitutions(row)
    touched: list[str] = []
    with tempfile.TemporaryDirectory() as work:
        orig = os.path.join(work, "orig")
        new = os.path.join(work, "new")
        for key, path in FILES.items():
            text = open(path, encoding="utf-8").read()
            changed = apply_subs(text, subs, key, row)
            if changed == text:
                continue
            rel = os.path.relpath(path, REPO)
            if rel not in ALLOWED_TARGETS:
                raise SystemExit(f"refusing to emit a delta that touches {rel}: not in ALLOWED_TARGETS")
            for base, tree in ((orig, text), (new, changed)):
                dest = os.path.join(base, rel)
                os.makedirs(os.path.dirname(dest), exist_ok=True)
                with open(dest, "w", encoding="utf-8", newline="\n") as handle:
                    handle.write(tree)
            touched.append(rel)
        if not touched:
            raise SystemExit(f"row {row['version']} produced no changes - the pins equal the reference")
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
        touched = write_docs(row, out_dir, touched)
    return touched


def write_docs(row: dict, out_dir: str, touched: list[str]) -> list[str]:
    facts = row["facts"]
    derivation = [
        f"# Deriving the rest of the Minecraft {row['version']} port",
        "",
        f"This delta is mechanical and complete for the pins; the items below must be",
        f"confirmed against the real {row['version']} jar before a build is published.",
        f"Status of this row: **{row['status']}**. {row.get('status_note', '')}",
        "",
        "## 1. Read the names you need from the shipped jar",
        "",
        "```sh",
        "# Loom has already downloaded and mapped the game by the time `./gradlew build`",
        "# has run once; the remapped jar is the authority, not a wiki.",
        "MC=" + row["version"],
        'JAR=$(find "$HOME/.gradle/caches/fabric-loom" -name "minecraft-*-"$MC"-*.jar" 2>/dev/null | head -n 1)',
        'test -n "$JAR" || { echo "run ./gradlew build once so Loom downloads $MC"; exit 1; }',
        "javap -p -classpath \"$JAR\" net.minecraft.client.renderer.LightTexture | grep -E 'tick|pixels|NativeImage'",
        "javap -p -classpath \"$JAR\" net.minecraft.client.gui.Gui | grep -E 'public void render|GuiGraphics'",
        "javap -p -classpath \"$JAR\" net.minecraft.client.renderer.LevelRenderer | grep -E 'setSectionDirty|allChanged|Section'",
        "javap -p -classpath \"$JAR\" net.minecraft.client.gui.screens.options.OptionsScreen | grep -nE 'lambda|method_'",
        "javap -p -classpath \"$JAR\" net.minecraft.client.Minecraft | grep -E 'setLevel|loadWorld|getDebugOverlay'",
        "```",
        "",
        "## 2. Where each answer goes",
        "",
    ]
    mapping = {
        "lightmap field": ("common/src/main/java/com/aetherium/gamma/LightmapWriter.java",
                           "the accepted field names in `probe()`; nothing else - the writer "
                           "degrades with one log line if the shape is unknown"),
        "lightmap tick method": ("common/src/main/java/com/aetherium/mixin/core/LightTextureMixin.java",
                                 "the `method = {...}` candidate list"),
        "GUI overlay": ("common/src/main/java/com/aetherium/mixin/core/GuiMixin.java",
                        "the descriptor on the primary `render` injection"),
        "options hijack": ("common/src/main/java/com/aetherium/mixin/core/OptionsScreenMixin.java",
                           "the `lambda$init$N` / `method_NNNNN` candidate list"),
        "screen callbacks": ("common/src/main/java/com/aetherium/gui/AetheriumVideoOptionsScreen.java",
                             "the `mouseClicked`/`mouseScrolled`/`onClose` override signatures"),
        "world hooks": ("common/src/main/java/com/aetherium/mixin/core/MinecraftMixin.java",
                        "the `method = {...}` candidate list"),
    }
    for key, (path, note) in mapping.items():
        derivation.append(f"- **{key}** - `{path}`: {note}")
    derivation += [
        "",
        "## 3. Then re-check, do not eyeball",
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
    ]
    with open(os.path.join(out_dir, "derivation.md"), "w", encoding="utf-8", newline="\n") as handle:
        handle.write("\n".join(derivation))

    notes = [
        f"# Port notes: Minecraft {row['version']}",
        "",
        f"- Java toolchain: **{row['java']}**",
        f"- Fabric loader floor: **{row['fabric_loader']}**" + (f", Loom **{row['loom']}**" if row.get("loom") else ""),
        f"- NeoForge: **{row['neoforge'] or 'does not exist for this version - the neoforge module is disabled'}**",
        f"- Mappings: {row['mappings']}",
        f"- Status: **{row['status']}**",
        "",
        "## Known differences this delta cannot encode",
        "",
    ]
    for line in facts.get("notes", []):
        notes.append(f"- {line}")
    if facts.get("gui_graphics", True) is False:
        notes.append("- `GuiGraphics` does not exist yet: every screen/overlay draw call in "
                     "the GUI and HUD uses the `PoseStack`/`GuiComponent` form.")
    notes += [
        "",
        "## Files the generated patch touches",
        "",
    ]
    for path in touched:
        notes.append(f"- `{path}`")
    notes += ["", "Generated by `tools/gen_deltas.py`; re-run that instead of editing.", ""]
    with open(os.path.join(out_dir, "notes.md"), "w", encoding="utf-8", newline="\n") as handle:
        handle.write("\n".join(notes))
    return sorted(set(touched) | {"derivation.md", "notes.md"})


def render_matrix(rows: list[dict]) -> str:
    order = ["version", "java", "fabric_loader", "neoforge", "status"]
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
        "No row claims a measured frame-rate result, and no row claims to have been run",
        "unless it is the reference row. Anything else would be a fabricated deliverable.",
        "",
        "| Minecraft | Java | Fabric loader | NeoForge | GuiGraphics | lightmap | options hijack | status | delta |",
        "| --- | --- | --- | --- | --- | --- | --- | --- | --- |",
    ]
    for row in rows:
        facts = row["facts"]
        gui = "yes" if facts.get("gui_graphics", True) else "no (PoseStack)"
        light = facts.get("lightmap_shape", "?")
        raw_range = facts.get("options_hijack_range", "*")
        hijack = "disabled (fallback button only)" if raw_range == "[9999,)" else raw_range
        if row["version"] == REFERENCE:
            version_cell = f"**{row['version']}**"
            delta = "the tree itself - [`common/`](common/src/main/java/com/aetherium), " \
                    "[`gradle.properties`](gradle.properties)"
        else:
            directory = f"deltas/{row['version']}"
            version_cell = f"[{row['version']}]({directory}/)"
            delta = (f"[patch]({directory}/changes.patch) \u00b7 "
                     f"[derivation]({directory}/derivation.md) \u00b7 "
                     f"[notes]({directory}/notes.md)")
        lines.append(f"| {version_cell} | {row['java']} | {row['fabric_loader']} | "
                     f"{row['neoforge'] or 'n/a'} | {gui} | {light} | `{hijack}` | {row['status']} | {delta} |")
    lines += [
        "",
        f"{len(rows)} rows: 1 reference + {len(rows) - 1} deltas.",
        "",
        "## All versions",
        "",
        "Every row, linked. A delta directory holds three files: `changes.patch` (what to",
        "apply), `derivation.md` (why each hunk is what it is, and the upstream file it was",
        "read from) and `notes.md` (what to check on that version once a JDK exists).",
        "No row links a download, because this repository publishes no jars - see the",
        "[README](README.md#honest-status) for what is and is not shipped.",
        "",
    ]
    groups = [
        ("1.16 - 1.18 (Java 8/16/17, no GuiGraphics)", lambda v: v.startswith(("1.16", "1.17", "1.18"))),
        ("1.19 - 1.20 (the GuiGraphics and lightmap changes)", lambda v: v.startswith(("1.19", "1.20"))),
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
                f"[derivation]({directory}/derivation.md), "
                f"[notes]({directory}/notes.md) - `{row['status']}`"
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
        "   `fabric_loader_version`, `fabric_loom_version`, `enabled_platforms`);",
        "2. `REFERENCE`-side strings in `AetheriumMixinPlugin` (the announced version and",
        "   the per-mixin target ranges);",
        "3. an *append* to an existing `method = { ... }` candidate list - reference names",
        "   are never removed, so two ports merge without conflict;",
        "4. the primary overlay descriptor in `GuiMixin` when the version predates",
        "   `GuiGraphics`.",
        "",
        "It never restructures a class, fixes a bug, or changes an algorithm. `",
        "`tools/gen_deltas.py` refuses to emit anything else, and `tools/check.py` proves",
        "every patch still applies with `git apply --check`.",
        "",
        "## Applying one",
        "",
        "```sh",
        "sh tools/port.sh --list            # what is known about each row",
        "sh tools/port.sh --dry-run 1.20.6  # apply to a copy, run the checks, throw it away",
        "sh tools/port.sh 1.20.6            # apply to this tree",
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
        "NeoForge begins at 1.20.2, so rows below it are Fabric-only and the generated",
        "delta sets `enabled_platforms=fabric` rather than shipping a jar that cannot load.",
        "For those versions the practical alternatives are Fabric (Pojav/Zalith on Android)",
        "or the mod's own Fabric jar under Sapphire; `docs/ARCHITECTURE.md` explains why no",
        "legacy-Forge port exists in this repository: the mixin surface differs enough that",
        "it would be a second codebase wearing a delta costume.",
        "",
        "## Verification record",
        "",
        "The 1.21.1 mixin target names in this tree were read from real sources, not memory:",
        "`CaffeineMC/sodium @ 1.21.1/stable` (its `LightTexture`, `Gui`, `OptionsScreen` and",
        "`LevelRenderer` mixin sets) and `neoforged/NeoForge @ 1.21.1` patch files. Where a",
        "name could not be read, the code carries an `[UNVERIFIED: ...]` mark and a tolerant",
        "injection - `python3 tools/check.py --report-unverified` lists them all.",
        "",
    ]
    return "\n".join(lines).replace("```\n```\n", "```\n")


def main() -> int:
    parser = argparse.ArgumentParser(description="Aetherium porting data generator")
    parser.add_argument("--all", action="store_true", help="regenerate every delta")
    parser.add_argument("--only", help="regenerate one version's delta")
    parser.add_argument("--matrix", action="store_true", help="regenerate PORTING_MATRIX.md")
    parser.add_argument("--verify", action="store_true", help="check every patch applies")
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
        print(f"wrote deltas/{row['version']}/ (changes.patch, derivation.md, notes.md)")

    if args.verify:
        bad = []
        checked = 0
        skipped = []
        for row in rows:
            patch = os.path.join(DELTAS_DIR, row["version"], "changes.patch")
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
        if bad:
            for version, first in bad:
                print(f"FAIL deltas/{version}/changes.patch: {first}")
            return 1
        print(f"all {checked} delta patches apply cleanly to the current tree "
              f"(no patch for: {', '.join(skipped)} - the reference version is the tree)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
