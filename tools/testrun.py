#!/usr/bin/env python3
"""Compile and run the common unit tests without Gradle, Maven or a JDK.

Builds on tools/stubcheck.py: the main sources are compiled for one Minecraft version
(default 1.21.1) against the probe stubs, then the tests are compiled against those classes
plus tools/minijunit (a small JUnit 5 subset: the annotations the tests use, Assertions and a
reflective runner) and executed on the local JRE from tools/local_jdk.sh.

The tests only exercise Minecraft-free code, so the stub Minecraft classes are never loaded
at runtime. CI still runs the real JUnit through Gradle; this is the offline equivalent.

Usage: python3 tools/testrun.py [--version 1.21.1] [TestClassSimpleName ...]
"""
import argparse
import os
import re
import shutil
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, "tools"))
import stubcheck  # noqa: E402

TESTS = os.path.join(ROOT, "common", "src", "test", "java")
MINIJUNIT = os.path.join(ROOT, "tools", "minijunit")


def java_files(root):
    out = []
    for base, _, files in os.walk(root):
        out.extend(os.path.join(base, f) for f in files if f.endswith(".java"))
    return sorted(out)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--version", default="1.21.1")
    ap.add_argument("only", nargs="*")
    args = ap.parse_args()
    version = args.version
    row = stubcheck.rows()[version]
    if stubcheck.check(version, row, verbose=True):
        return 1
    java = str(min(int(row.get("java", 21)), 24))
    main_classes = os.path.join(stubcheck.WORK, version, "classes")
    out = os.path.join(stubcheck.WORK, version, "test-classes")
    shutil.rmtree(out, ignore_errors=True)
    os.makedirs(out)
    cp = os.pathsep.join([main_classes, stubcheck.lib_classes(), stubcheck.mc_classes(version)])
    files = java_files(MINIJUNIT) + java_files(TESTS)
    lst = os.path.join(stubcheck.WORK, version, "test-files.txt")
    with open(lst, "w") as h:
        h.write("\n".join(files))
    code, log = stubcheck.ecj(["-source", java, "-target", java, "-nowarn", "-encoding", "UTF-8",
                               "-proceedOnError", "-cp", cp, "-d", out, "@" + lst])
    errors = len(re.findall(r"^\d+\. ERROR", log, re.M))
    if errors:
        print(log.replace(TESTS + os.sep, "")[:30000])
        print("%d test compile errors" % errors)
        return 1
    classes = []
    for f in java_files(TESTS):
        text = open(f, encoding="utf-8").read()
        if "@Test" not in text:
            continue
        name = os.path.relpath(f, TESTS)[:-5].replace(os.sep, ".")
        if args.only and name.rsplit(".", 1)[-1] not in args.only:
            continue
        classes.append(name)
    # Resources (e.g. lang files read by tests) come from the main resources dir.
    res = os.path.join(ROOT, "common", "src", "main", "resources")
    run_cp = os.pathsep.join([out, main_classes, res, stubcheck.lib_classes()])
    r = subprocess.run([stubcheck.JAVA, "-cp", run_cp, "org.junit.platform.MiniRunner"] + classes, cwd=ROOT)
    return r.returncode


if __name__ == "__main__":
    sys.exit(main())
