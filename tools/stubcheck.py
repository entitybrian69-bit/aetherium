#!/usr/bin/env python3
"""Compile the mod for any of the 33 versions against signature stubs, without Gradle or Maven.

    tools/local_jdk.sh                      # once: JRE 25 + ECJ into /tmp/tc
    python3 tools/stubcheck.py 1.21.1       # one version
    python3 tools/stubcheck.py --all        # every row in tools/porting_pins.json

For each version the sources of common/ + fabric/ (+ neoforge/ when the row has a NeoForge pin)
are copied with every ``// @era:`` block switched to that version's variant (tools/eras.py),
then compiled by ECJ at the row's Java level against:

* /tmp/stubs/<ver>/classes — Minecraft signatures generated from tools/probe/<ver>.txt
* tools/stubs_lib           — hand-written slf4j/log4j/ASM/Mixin/Fabric/NeoForge API stubs

What this proves: every member the mod touches exists with that exact signature in that
version (for probed classes), and the Java level is respected (Java 8 for 1.16.5).
What it cannot prove: behaviour, Mixin target resolution, or members of classes that were never
probed (those stub as empty types, so a call into one shows up as an error to investigate).
"""
import json
import os
import re
import shutil
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, "tools"))
import eras  # noqa: E402
import stubgen  # noqa: E402

TC = os.environ.get("AETHERIUM_TC", "/tmp/tc")
STUBS = os.environ.get("AETHERIUM_STUBS", "/tmp/stubs")
WORK = os.environ.get("AETHERIUM_CHECK", "/tmp/stubcheck")
JAVA = os.path.join(TC, "jre", "bin", "java")
ECJ = os.path.join(TC, "ecj.jar")


def rows():
    data = json.load(open(os.path.join(ROOT, "tools", "porting_pins.json")))
    data = data["rows"] if isinstance(data, dict) and "rows" in data else data
    return {r["version"]: r for r in data}


def ecj(args):
    r = subprocess.run([JAVA, "-jar", ECJ] + args, capture_output=True, text=True)
    return r.returncode, r.stdout + r.stderr


def lib_classes():
    out = os.path.join(STUBS, "_lib")
    stamp = os.path.join(out, ".stamp")
    src = os.path.join(ROOT, "tools", "stubs_lib")
    newest = max(os.path.getmtime(os.path.join(dp, f)) for dp, _, fs in os.walk(src) for f in fs)
    if os.path.exists(stamp) and os.path.getmtime(stamp) >= newest:
        return out
    shutil.rmtree(out, ignore_errors=True)
    files = [os.path.join(dp, f) for dp, _, fs in os.walk(src) for f in fs if f.endswith(".java")]
    code, log = ecj(["-source", "8", "-target", "8", "-nowarn", "-d", out] + files)
    if code != 0:
        print(log)
        raise SystemExit("library stubs failed to compile")
    open(stamp, "w").close()
    return out


def mc_classes(version):
    out = os.path.join(STUBS, version, "classes")
    dump = os.path.join(ROOT, "tools", "probe", version + ".txt")
    if not os.path.isdir(out) or os.path.getmtime(out) < os.path.getmtime(dump) \
            or os.path.getmtime(out) < os.path.getmtime(os.path.join(ROOT, "tools", "stubgen.py")):
        stubgen.compile_stubs(version)
    return out


def source_roots(row):
    roots = [os.path.join(ROOT, "common", "src", "main", "java"),
             os.path.join(ROOT, "fabric", "src", "main", "java")]
    if row.get("neoforge"):
        roots.append(os.path.join(ROOT, "neoforge", "src", "main", "java"))
    return roots


def prepare(version, row):
    dest = os.path.join(WORK, version, "src")
    shutil.rmtree(os.path.join(WORK, version), ignore_errors=True)
    used = set()
    files = []
    for root in source_roots(row):
        for dp, _, fs in os.walk(root):
            for f in fs:
                if not f.endswith(".java"):
                    continue
                src = os.path.join(dp, f)
                rel = os.path.relpath(src, root)
                text = open(src, encoding="utf-8").read()
                if eras.has_markers(text):
                    text = eras.select_all(text, version, os.path.relpath(src, ROOT), used)
                out = os.path.join(dest, rel)
                os.makedirs(os.path.dirname(out), exist_ok=True)
                with open(out, "w", encoding="utf-8") as h:
                    h.write(text)
                files.append(out)
    return files, used


def check(version, row, verbose=True):
    java = str(min(int(row.get("java", 21)), 24))
    files, _ = prepare(version, row)
    cp = os.pathsep.join([lib_classes(), mc_classes(version)])
    out = os.path.join(WORK, version, "classes")
    lst = os.path.join(WORK, version, "files.txt")
    with open(lst, "w") as h:
        h.write("\n".join(files))
    code, log = ecj(["-source", java, "-target", java, "-nowarn", "-encoding", "UTF-8",
                     "-cp", cp, "-d", out, "@" + lst])
    log = log.replace(os.path.join(WORK, version, "src") + os.sep, "")
    errors = len(re.findall(r"^\d+\. ERROR", log, re.M))
    with open(os.path.join(WORK, version, "errors.txt"), "w") as h:
        h.write(log)
    status = "ok" if errors == 0 else "%d errors" % errors
    print("%-8s java %-2s %-4d files  %s" % (version, java, len(files), status))
    if errors and verbose:
        print(log if len(log) < 20000 else log[:20000] + "\n... (see %s/errors.txt)" % os.path.join(WORK, version))
    return errors


def main():
    args = sys.argv[1:]
    quiet = "-q" in args
    args = [a for a in args if a != "-q"]
    table = rows()
    targets = sorted(table, key=eras.vkey) if args == ["--all"] or not args else args
    failed = 0
    for v in targets:
        if v not in table:
            raise SystemExit("unknown version %s" % v)
        if check(v, table[v], verbose=not quiet and len(targets) == 1):
            failed += 1
    if len(targets) > 1:
        print("%d/%d versions compile" % (len(targets) - failed, len(targets)))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
