#!/usr/bin/env python3
"""Print the javap -c body of methods from the probe dumps (tools/probe/<version>.txt).

usage: body.py VERSION CLASS METHOD_REGEX [--calls]
  CLASS        suffix of the class name (e.g. renderer.ItemInHandRenderer)
  METHOD_REGEX matched against the method declaration line ("##" lines in "=== method" sections)
  --calls      only the invoke/field instructions (the targets a mixin can hook)
"""
import re
import sys


def sections(path):
    cur = None
    buf = []
    for line in open(path, encoding="utf-8", errors="replace"):
        line = line.rstrip("\n")
        if line.startswith("=== "):
            if cur:
                yield cur, buf
            cur, buf = line, []
        elif cur:
            buf.append(line)
    if cur:
        yield cur, buf


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    calls = "--calls" in sys.argv
    version, cls, rx = args[0], args[1], re.compile(args[2])
    seen = set()
    for head, body in sections("tools/probe/%s.txt" % version):
        if not head.startswith("=== method ") or cls not in head.split(" :: ")[0]:
            continue
        printing = False
        for line in body:
            if line.startswith("##"):
                printing = bool(rx.search(line)) and line not in seen
                if printing:
                    seen.add(line)
                    print(line[:300])
                continue
            if not printing:
                continue
            if calls:
                m = re.search(r"(invoke\w+|[gp]et(field|static))\s+#\d+\s+//\s+\w+\s+(.*)", line)
                if m:
                    print("   ", m.group(1), m.group(3)[:400])
            else:
                print(line[:260])


if __name__ == "__main__":
    main()
