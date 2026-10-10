#!/usr/bin/env python3
"""Aetherium tree consistency checks - the ones a compiler cannot do for you.

Run by ``./gradlew checkTree`` (root build.gradle.kts) and by CI before anything is
downloaded, because every check here is possible offline and several of them catch
the failures that would otherwise only appear on a user's machine:

  1.  no placeholder text anywhere (TODO/FIXME/"logic here"/"same as above"/"..."
      bodies/``[TRUNCATED``) - checkstyle covers Java, this covers scripts, docs,
      gradle files and JSON.
  2.  every ``[UNVERIFIED: ...]`` mark is well formed, and with
      ``--report-unverified`` a report of all of them is printed (this is the
      "no fabricated APIs" rule made auditable).
  3.  ``aetherium-common.mixins.json`` lists exactly the mixin classes on disk -
      writing a mixin and forgetting the config is the most common silent failure
      in a mixin mod, and it is invisible until a user reports the feature missing.
  4.  every ``project.property("x")`` in any build script is declared in
      gradle.properties (an undeclared property is a configuration-time failure on
      a clean machine, i.e. exactly what "the README said it works" must not be).
  5.  every config option is reachable (declared in AetheriumConfig, present in the
      lang files and in CONFIG_SCHEMA.json) and every option used in code exists.
  6.  encoding/hygiene: LF line endings, final newline, tabs for Java indentation,
      no CJK characters (this project is authored by a model; non-ASCII in a comment
      is the fingerprint of a generation artifact, and the GUI strings are the only
      place non-ASCII belongs - as \\u escapes).
  7.  every version row in PORTING_MATRIX.md has a deltas/<version>/changes.patch that
      ``git apply --check`` accepts (skipped when git or the patch set is absent).

Usage:
    python3 tools/check.py [--root DIR] [--report-unverified] [--skip-deltas]

Exit status: 0 = clean, 1 = problems found, 2 = the checker itself could not run.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
from collections import defaultdict

BANNED = [
    (re.compile(r"\bTODO\b|\bFIXME\b|\bXXX\b"), "TODO/FIXME/XXX marker"),
    (re.compile(r"//\s*(logic here|same as above|implement later|not implemented yet)", re.I),
     "placeholder comment"),
    (re.compile(r"^\s*\.\.\.\s*$"), "elided body (...)"),
    (re.compile(r"\[TRUNCATED"), "truncation marker"),
    (re.compile(r"\bcoming soon\b", re.I), "\"coming soon\""),
]
CJK = re.compile(r"[㐀-䶿一-鿿豈-﫿　-〿＀-￯]")
UNVERIFIED = re.compile(r"\[UNVERIFIED(?::([^\]]*)\]|\])")

# Files that legitimately contain the strings the banned scan looks for, because
# they *define* the rule. Exempting them beats weakening the patterns for everything.
RULE_FILES = {"tools/check.py", "tools/check_refs.py", "tools/gen_resources.py",
              "tools/checkstyle-ignore.md", "checkstyle.xml",
              "CONTRIBUTING.md", "README.md", "docs/TROUBLESHOOTING.md",
              # These two documents name the marker in order to explain it. Counting their
              # prose as unresolved questions would inflate the number the report carries.
              "docs/GLOSSARY.md",
              # The audit names the markers it is auditing; counting them would inflate the
              # very number the report exists to state precisely.
              "VERIFICATION.md",
              ".github/workflows/build.yml"}

TEXT_SUFFIXES = (  # tuple, because str.endswith takes a tuple, not a set
                 ".java", ".kts", ".gradle", ".json", ".toml", ".md", ".sh", ".py",
                 ".yml", ".yaml", ".properties", ".bat", ".editorconfig", ".txt")
SKIP_DIRS = {".git", "build", ".gradle", "node_modules", "out", "run", "runs", "__pycache__",
             ".idea", ".vscode", "third-party-notices"}


def iter_files(root: str):
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
        for name in sorted(filenames):
            path = os.path.join(dirpath, name)
            if os.path.islink(path):
                continue
            if name in {"gradle-wrapper.jar"} or name.endswith((".png", ".jar", ".ico")):
                continue
            if name.endswith(TEXT_SUFFIXES) or "." not in name:
                yield path


def rel(root: str, path: str) -> str:
    return os.path.relpath(path, root)


def main() -> int:
    parser = argparse.ArgumentParser(description="Aetherium tree checks")
    default_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    parser.add_argument("--root", default=default_root)
    parser.add_argument("--report-unverified", action="store_true")
    parser.add_argument("--skip-deltas", action="store_true")
    parser.add_argument("--json", action="store_true", help="machine-readable summary line")
    args = parser.parse_args()
    root = os.path.abspath(args.root)
    if not os.path.isdir(root):
        print(f"not a directory: {root}", file=sys.stderr)
        return 2

    problems: list[str] = []
    unverified: list[tuple[str, str]] = []
    texts: dict[str, str] = {}
    counted = 0

    for path in iter_files(root):
        try:
            raw = open(path, "rb").read()
        except OSError as error:  # unreadable file is a real problem, report it
            problems.append(f"{rel(root, path)}: cannot read ({error})")
            continue
        try:
            text = raw.decode("utf-8")
        except UnicodeDecodeError:
            problems.append(f"{rel(root, path)}: not valid UTF-8")
            continue
        counted += 1
        name = os.path.basename(path)
        # Files that *define* the rules legitimately contain the strings the rules
        # look for (this file holds the CJK range and the marker spellings).
        is_rule_file = path.replace(os.sep, "/").replace(root.replace(os.sep, "/") + "/", "") in RULE_FILES
        path_rel = rel(root, path)
        texts[path] = text

        # -- hygiene ---------------------------------------------------------------
        if "\r\n" in text and not name.endswith(".bat"):
            problems.append(f"{path_rel}: CRLF line endings (delta patches need LF)")
        if text and not text.endswith("\n"):
            problems.append(f"{path_rel}: missing final newline")
        if path.endswith(".java") and name != "package-info.java":
            for lineno, line in enumerate(text.split("\n"), 1):
                # The project standard is 4 spaces with column alignment (the reason
                # is in .editorconfig); a tab anywhere in a Java file breaks both.
                if "\t" in line[:len(line) - len(line.lstrip())]:
                    problems.append(f"{path_rel}:{lineno}: tab used for indentation (use 4 spaces)")
                    break
                if line.rstrip() != line:
                    problems.append(f"{path_rel}:{lineno}: trailing whitespace")
                    break
        if path.endswith((".java", ".py", ".sh", ".kts", ".gradle", ".properties")) and not is_rule_file:
            for lineno, line in enumerate(text.split("\n"), 1):
                if CJK.search(line):
                    problems.append(f"{path_rel}:{lineno}: non-ASCII (CJK) character in source - "
                                    "this is the signature of a generation artifact")
                    break

        # -- banned content --------------------------------------------------------
        is_rule_file = path_rel.replace(os.sep, "/") in RULE_FILES
        skip_banned = is_rule_file
        for pattern, label in BANNED:
            if skip_banned:
                break
            for match in pattern.finditer(text):
                line = text.count("\n", 0, match.start()) + 1
                # `[UNVERIFIED]` markers legitimately contain the words "not verified";
                # a TODO inside a .md is still a TODO, so no file-type exemption here.
                problems.append(f"{path_rel}:{line}: {label}: {match.group(0).strip()!r}")

        for match in UNVERIFIED.finditer(text):
            if is_rule_file:
                break  # this file documents the marker, so it spells it both ways
            detail = (match.group(1) or "").strip()
            line = text.count("\n", 0, match.start()) + 1
            if not detail and path.endswith((".java", ".kts", ".json", ".sh", ".py")):
                problems.append(f"{path_rel}:{line}: [UNVERIFIED] without a ': <what>' detail - "
                                "the mark must say exactly what is unverified")
            unverified.append((f"{path_rel}:{line}", detail))

    # -- mixin config vs. files on disk -------------------------------------------
    mixin_configs = [p for p in texts if p.endswith("aetherium-common.mixins.json")]
    if not mixin_configs:
        problems.append("no aetherium-common.mixins.json found under any src/main/resources")
    for config_path in mixin_configs:
        try:
            config = json.loads(re.sub(r"\$\{[^}]*\}", "x", texts[config_path]))
        except json.JSONDecodeError as error:
            problems.append(f"{rel(root, config_path)}: invalid JSON ({error})")
            continue
        package = config.get("package", "")
        listed = set(config.get("client", [])) | set(config.get("mixins", [])) | set(config.get("server", []))
        on_disk: set[str] = set()
        # The package directory lives under a source root, not next to the resource
        # file, so walk every src/main/java in the tree and match the tail path.
        package_tail = os.path.join(*package.split(".")) if package else ""
        roots = [os.path.join(dirpath, package_tail) for dirpath, _dirs, _files in os.walk(root)
                 if dirpath.endswith(os.path.join("src", "main", "java"))
                 and package_tail and os.path.isdir(os.path.join(dirpath, package_tail))]
        on_disk: set[str] = set()
        for mixin_dir in roots:
            for dirpath, _dirs, files in os.walk(mixin_dir):
                for name in files:
                    if name.endswith(".java"):
                        relpath = os.path.relpath(os.path.join(dirpath, name), mixin_dir)
                        on_disk.add(relpath[:-5].replace(os.sep, "."))
        # The plugin class is referenced by `plugin`, not listed in a bucket.
        plugin = config.get("plugin", "")
        plugin_simple = plugin.rsplit(".", 1)[-1]
        on_disk.discard(plugin_simple)
        missing = sorted(on_disk - listed)
        extra = sorted(listed - on_disk)
        if missing:
            problems.append(f"{rel(root, config_path)}: mixin classes present but not registered: "
                            + ", ".join(missing))
        if extra:
            problems.append(f"{rel(root, config_path)}: registered but no file on disk: "
                            + ", ".join(extra))
        for key, expected in (("required", False),):
            if config.get(key) != expected:
                problems.append(f"{rel(root, config_path)}: {key} must be {expected} in a mod that "
                                "ships 33 versions (a hard mixin requirement turns a rename into a "
                                "crash); strict_mixins is enforced by the plugin instead")
        injectors = config.get("injectors", {})
        if injectors.get("defaultRequire") != 0 or injectors.get("defaultExpect") != 0:
            problems.append(f"{rel(root, config_path)}: injectors.defaultRequire/Expect must be 0 "
                            "(see the file's own _comment for why)")

    # -- gradle properties declared vs. used --------------------------------------
    props_path = os.path.join(root, "gradle.properties")
    declared: set[str] = set()
    if os.path.exists(props_path):
        for line in open(props_path, encoding="utf-8"):
            match = re.match(r"^\s*([A-Za-z0-9_.\-]+)\s*=", line)
            if match:
                declared.add(match.group(1))
    else:
        problems.append("gradle.properties is missing")
    used: dict[str, set[str]] = defaultdict(set)
    for path, text in texts.items():
        if not (path.endswith(".kts") or path.endswith(".gradle") or path.endswith(".toml")):
            continue
        for match in re.finditer(r'project\.property\(\s*"([^"]+)"\s*\)', text):
            used[match.group(1)].add(rel(root, path))
        for match in re.finditer(r'\$\{project\.property\("([^"]+)"\)\}', text):
            used[match.group(1)].add(rel(root, path))
    for name, where in sorted(used.items()):
        if name not in declared:
            problems.append(f"gradle.properties does not declare '{name}', used by "
                            + ", ".join(sorted(where)))

    # -- Kotlin DSL: a plugins {} block cannot read project properties -------------
    # Gradle extracts the plugins {} block and evaluates it before `project` exists, so
    # `id("x") version (project.property("v").toString())` is a compile error, not a
    # configuration one. It cost a CI cycle on 2026-10-06; it does not get another.
    for path, text in texts.items():
        if not path.endswith(".gradle.kts"):
            continue
        for block in re.findall(r"\nplugins \{\n(.*?)\n\}", text, re.S):
            # Comments inside the block explain the rule to whoever edits it next, so the rule
            # has to ignore them - matching prose about project.property() would flag the fix.
            code = re.sub(r"/\*.*?\*/", "", re.sub(r"//[^\n]*", "", block), flags=re.S)
            if re.search(r"project\.property\(|settings\.|gradle\.startParameter", code):
                problems.append(
                    f"{rel(root, path)}: plugins {{}} must not read project properties (the block is "
                    "extracted before `project` exists) - pin the version in gradle/libs.versions.toml "
                    "and use alias(libs.plugins.…)")

    # -- the catalog and gradle.properties must agree on the loader plugin versions --
    # gradle.properties is what the deltas rewrite; the catalog is what the build applies.
    catalog_path = os.path.join(root, "gradle", "libs.versions.toml")
    if os.path.exists(catalog_path) and os.path.exists(props_path):
        catalog = open(catalog_path, encoding="utf-8").read()
        props_text = open(props_path, encoding="utf-8").read()
        for prop, key in (("fabric_loom_version", "loom"), ("neoforge_moddev_version", "moddev")):
            want = re.search(rf"^{re.escape(prop)}\s*=\s*(\S+)", props_text, re.M)
            got = re.search(rf'^{key}\s*=\s*"([^"]+)"', catalog, re.M)
            if not want or not got:
                problems.append(f"gradle/libs.versions.toml: needs a '{key}' version and "
                                f"gradle.properties needs '{prop}' - the build applies the catalog "
                                "value, the deltas rewrite the property")
            elif want.group(1) != got.group(1):
                problems.append(f"gradle/libs.versions.toml {key}=\"{got.group(1)}\" but "
                                f"gradle.properties {prop}={want.group(1)} - bump both together "
                                "(check.py treats the property as the one that is right)")

    # -- settings.gradle includes vs. directories --------------------------------
    settings_path = os.path.join(root, "settings.gradle.kts")
    if os.path.exists(settings_path):
        settings = open(settings_path, encoding="utf-8").read()
        included = set(re.findall(r'include\s*\(\s*"([^"]+)"', settings))
        for name in sorted(os.listdir(root)):
            candidate = os.path.join(root, name, "build.gradle.kts")
            if os.path.isdir(os.path.join(root, name)) and os.path.exists(candidate) and name not in included:
                problems.append(f"settings.gradle.kts does not include ':{name}' but {name}/build.gradle.kts exists")

    # -- config option reachability ------------------------------------------------
    config_java = os.path.join(root, "common/src/main/java/com/aetherium/config/AetheriumConfig.java")
    if os.path.exists(config_java):
        config_text = open(config_java, encoding="utf-8").read()
        fields = re.findall(r"ConfigValue<([\w.]+)>\s+(\w+)\s*=\s*ConfigValue\.(\w+)\(\s*\"([^\"]+)\"",
                            config_text)
        keys = {key for _t, _f, _k, key in fields}
        names = {name for _t, name, _k, _key in fields}
        if not fields:
            problems.append(f"{rel(root, config_java)}: no ConfigValue fields parsed - update tools/check.py")
        # NOTE: "does config.<field> exist" is deliberately NOT checked here.
        # tools/check_refs.py resolves receiver types properly (locals, params,
        # supertypes, records); a regex here produced 40 false positives per run and
        # a checker that cries wolf gets ignored, which is worse than no checker.
        used_keys = set()
        for path, text in texts.items():
            if path.endswith(".java"):
                used_keys |= {m.group(1) for m in re.finditer(r'ConfigValue<[^>]+>\s+\w+\s*=\s*ConfigValue\.\w+\(\s*"([^"]+)"', text)}
        lang_path = os.path.join(root, "common/src/main/resources/assets/aetherium/lang/en_us.json")
        if os.path.exists(lang_path):
            lang = json.load(open(lang_path, encoding="utf-8"))
            for key in sorted(keys):
                if f"aetherium.option.{key}" not in lang:
                    problems.append(f"lang file has no entry for aetherium.option.{key} "
                                    "(run python3 tools/gen_resources.py)")
            for key in sorted(lang):
                if key.startswith("aetherium.option.") and not key.endswith(".tooltip"):
                    bare = key[len("aetherium.option."):]
                    if bare not in keys:
                        problems.append(f"lang entry {key} has no config option (stale)")
        schema_path = os.path.join(root, "CONFIG_SCHEMA.json")
        if os.path.exists(schema_path):
            try:
                schema = json.load(open(schema_path, encoding="utf-8"))
            except json.JSONDecodeError as error:
                problems.append(f"CONFIG_SCHEMA.json: invalid JSON ({error})")
                schema = {}
            schema_keys = {k for group in schema.get("properties", {}).get("aetherium", {}).get("properties", {}).values()
                           for k in group.get("properties", {})}
            for key in sorted(keys - schema_keys):
                problems.append(f"CONFIG_SCHEMA.json is missing option '{key}' (run gen_resources.py)")
            for key in sorted(schema_keys - keys):
                problems.append(f"CONFIG_SCHEMA.json has stale option '{key}'")

    # -- delta patches --------------------------------------------------------------
    matrix = os.path.join(root, "PORTING_MATRIX.md")
    deltas = os.path.join(root, "deltas")
    if not args.skip_deltas and os.path.exists(matrix) and os.path.isdir(deltas):
        # Version cells are links now ("| [1.20.1](deltas/1.20.1/) |"), so accept a bare number,
        # a linked one, or a `**1.21.1** (reference)` cell. An earlier version of this rule matched
        # only the bare form, parsed zero rows, and therefore verified zero patches - which is how a
        # stale patch set reached CI. The count check below is what stops that class of silence.
        table = open(matrix, encoding="utf-8").read()
        rows = re.findall(r"^\|\s*\*{0,2}\[?(\d+\.\d+(?:\.\d+)?)\]?\*{0,2}(?:\]\([^)]*\))?\s*\|", table, re.M)
        rows = sorted(set(rows))
        on_disk = sorted(d for d in os.listdir(deltas) if os.path.isdir(os.path.join(deltas, d)))
        if not rows and on_disk:
            problems.append(f"PORTING_MATRIX.md: no version rows parsed while deltas/ holds {len(on_disk)} "
                            "directories - the table format changed and this rule is now blind")
        for version in on_disk:
            if version not in rows:
                problems.append(f"deltas/{version}/ exists but PORTING_MATRIX.md has no row for it")
        pins_path = os.path.join(root, "tools", "porting_pins.json")
        reference = None
        if os.path.exists(pins_path):
            reference = json.load(open(pins_path, encoding="utf-8")).get("reference")
        for version in rows:
            if version == reference:
                continue  # the reference version needs no delta by definition
            patch = os.path.join(deltas, version, "changes.patch")
            if not os.path.exists(patch):
                problems.append(f"PORTING_MATRIX.md lists {version} but {rel(root, patch)} is missing")
        # patches must be applicable to a clean tree; git apply --check is the only
        # honest way to prove it, and it needs the reference tree to be at HEAD.
        try:
            result = subprocess.run(["git", "rev-parse", "--is-inside-work-tree"], cwd=root,
                                    capture_output=True, text=True, timeout=20)
            have_git = result.returncode == 0
        except (OSError, subprocess.SubprocessError):
            have_git = False
        if have_git:
            for version in rows:
                patch = os.path.join(deltas, version, "changes.patch")
                if not os.path.exists(patch):
                    continue
                try:
                    check = subprocess.run(["git", "apply", "--check", "--whitespace=nowarn", patch],
                                           cwd=root, capture_output=True, text=True, timeout=60)
                except (OSError, subprocess.SubprocessError) as error:
                    problems.append(f"deltas/{version}/changes.patch: could not verify ({error})")
                    continue
                if check.returncode != 0:
                    first = (check.stderr or check.stdout).strip().splitlines()
                    problems.append(f"deltas/{version}/changes.patch does not apply cleanly"
                                    + (f": {first[0]}" if first else ""))

    if args.report_unverified:
        print(f"# Aetherium unverified claims ({len(unverified)})")
        print()
        print("Every entry below is a place where this codebase could not confirm a")
        print("third-party signature offline. Each is written so the failure is a log line")
        print("or a disabled feature, never a crash - see CONTRIBUTING.md, 'No fabricated")
        print("APIs'. Fixing one means reading the real source and deleting the mark.")
        print()
        by_detail: dict[str, list[str]] = defaultdict(list)
        for where, detail in unverified:
            by_detail[detail].append(where)
        for detail in sorted(by_detail):
            print(f"- {detail}")
            for where in sorted(by_detail[detail]):
                print(f"    {where}")
        print()
        print(f"total: {len(unverified)} marks across {len(by_detail)} distinct questions")

    unique = sorted(set(problems))
    if args.json:
        print(json.dumps({"files_checked": counted, "problems": len(unique), "unverified": len(unverified)}))
    if unique:
        print(f"Aetherium check: {len(unique)} problem(s) across {counted} files")
        for problem in unique:
            print("  " + problem)
        return 1
    print(f"Aetherium check: clean ({counted} files, {len(unverified)} [UNVERIFIED] marks, "
          f"{len(texts)} text files parsed)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
