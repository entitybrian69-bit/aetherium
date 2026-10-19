#!/usr/bin/env python3
"""Stub-compile the Aetherium Backport (1.12.2) locally, like tools/stubcheck.py for the main mod.

    python3 tools/dev/bpcheck.py [FILE_REGEX]

Stubs come from backport/probe/out/*.txt (the javap dump of the Forge-patched, MCP-named 1.12.2
classes that backport.yml commits). Sources: backport/src/main/java plus the shared common files
listed in backport/shared-sources.txt, compiled at Java 8. Forge, LWJGL 2 and other unprobed
classes stub as empty types, so the existing code produces noise; errors are therefore printed
only for files matching FILE_REGEX (default: everything, summary only for non-matching files).
The CI build (backport.yml) remains the authority.
"""
import os
import re
import shutil
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.insert(0, os.path.join(ROOT, "tools"))
import stubgen  # noqa: E402

TC = os.environ.get("AETHERIUM_TC", "/tmp/tc")
WORK = "/tmp/bpcheck"
PROBE_DIR = os.path.join(WORK, "probe")
OUT_OF_PROBE = os.path.join(ROOT, "backport", "probe", "out")


def build_dump():
    os.makedirs(PROBE_DIR, exist_ok=True)
    dump = os.path.join(PROBE_DIR, "backport.txt")
    lines = []
    for name in sorted(os.listdir(OUT_OF_PROBE)):
        if not name.endswith(".txt") or name.startswith("_"):
            continue
        for raw in open(os.path.join(OUT_OF_PROBE, name), encoding="utf-8", errors="replace"):
            line = raw.rstrip("\n")
            if line.startswith("Compiled from") or line.strip().startswith("descriptor:"):
                continue
            m = re.match(r"(?:public |protected |private |abstract |final |static )*"
                         r"(?:class|interface|enum|@interface) ([\w.$]+)", line)
            if m and not line.startswith(" "):
                lines.append("=== " + m.group(1))
            lines.append(line)
    with open(dump, "w", encoding="utf-8") as h:
        h.write("\n".join(lines) + "\n")
    return dump


def main():
    pat = re.compile(sys.argv[1]) if len(sys.argv) > 1 else None
    shutil.rmtree(WORK, ignore_errors=True)
    build_dump()
    stubgen.PROBE = PROBE_DIR
    stubgen.OUT = os.path.join(WORK, "stubs")
    classes = stubgen.compile_stubs("backport")
    import stubcheck
    lib = stubcheck.lib_classes()
    files = []
    src_root = os.path.join(ROOT, "backport", "src", "main", "java")
    for dp, _, fs in os.walk(src_root):
        files += [os.path.join(dp, f) for f in fs if f.endswith(".java")]
    common = os.path.join(ROOT, "common", "src", "main", "java", "com", "aetherium")
    for rel in open(os.path.join(ROOT, "backport", "shared-sources.txt")):
        rel = rel.strip()
        if rel and not rel.startswith("#"):
            files.append(os.path.join(common, rel))
    lst = os.path.join(WORK, "files.txt")
    with open(lst, "w") as h:
        h.write("\n".join(files))
    r = subprocess.run([os.path.join(TC, "jre", "bin", "java"), "-jar", os.path.join(TC, "ecj.jar"),
                        "-source", "8", "-target", "8", "-nowarn", "-encoding", "UTF-8", "-proceedOnError",
                        "-cp", os.pathsep.join([lib, classes]), "-d", os.path.join(WORK, "classes"), "@" + lst],
                       capture_output=True, text=True)
    log = (r.stdout + r.stderr).replace(ROOT + os.sep, "")
    with open(os.path.join(WORK, "errors.txt"), "w") as h:
        h.write(log)
    per_file = {}
    shown = 0
    for chunk in re.split(r"-{10,}\n", log):
        m = re.search(r"ERROR in (\S+) \(at line (\d+)\)\n(.*?)\n(?:\s*\^+\s*\n)?(.*)", chunk, re.S)
        if not m:
            continue
        f = m.group(1)
        per_file[f] = per_file.get(f, 0) + 1
        if pat is not None and pat.search(f):
            msg = [l for l in m.group(4).strip().split("\n") if l.strip()][-1:]
            print("  %s:%s  %s  | %s" % (f.split("/")[-1], m.group(2), (msg[0] if msg else "")[:120],
                                         m.group(3).strip()[:100]))
            shown += 1
    total = sum(per_file.values())
    print("backport  java 8  %d files  %d errors in %d files (%d shown); log: %s/errors.txt"
          % (len(files), total, len(per_file), shown, WORK))
    if pat is None:
        for f, n in sorted(per_file.items(), key=lambda kv: -kv[1]):
            print("  %4d  %s" % (n, f))


if __name__ == "__main__":
    main()
