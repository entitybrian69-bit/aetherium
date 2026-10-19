#!/bin/bash
# Stub-compile VERSIONs and print the errors in Quality of life port files with their messages.
# usage: tools/dev/qerr.sh VERSION... (PATTERN env: file regex, default Qol|ScreenProjector|ClientHooks|ZoomKeys|McCanvas|FrameHud)
PAT=${PATTERN:-'Qol|ScreenProjector|ClientHooks|ZoomKeys|McCanvas|FrameHud'}
for v in "$@"; do
  python3 tools/stubcheck.py -q "$v" | tail -1
  python3 - "$v" "$PAT" <<'PY'
import re, sys
v, pat = sys.argv[1], sys.argv[2]
log = open("/tmp/stubcheck/%s/errors.txt" % v).read()
for chunk in re.split(r"-{10,}\n", log):
    m = re.search(r"ERROR in (\S+) \(at line (\d+)\)\n(.*?)\n(?:\s*\^+\s*\n)?(.*)", chunk, re.S)
    if m and re.search(pat, m.group(1)):
        msg = [l for l in m.group(4).strip().split("\n") if l.strip()][-1:]
        print("  %s:%s  %s  | %s" % (m.group(1).split("/")[-1], m.group(2), (msg[0] if msg else "")[:110], m.group(3).strip()[:90]))
    elif "API-LEVEL" in chunk and re.search(pat, chunk):
        print("  " + chunk.strip()[:200])
PY
done
