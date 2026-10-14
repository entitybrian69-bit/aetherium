#!/usr/bin/env bash
# In-game smoke test under Xvfb + Mesa llvmpipe: runClient with -Daetherium.smoke (see SmokeTest).
# Output: ci/out/smoke/{report.txt,*.png,client-tail.txt,status.txt}
set -uo pipefail
cd "$(dirname "$0")/.."
OUT="$PWD/ci/out/smoke"
rm -rf "$OUT" && mkdir -p "$OUT"
export AETHERIUM_SMOKE="$OUT"
export LIBGL_ALWAYS_SOFTWARE=1 MESA_GL_VERSION_OVERRIDE=3.3COMPAT
timeout 1200 xvfb-run -a -s "-screen 0 1280x720x24" ./gradlew --no-daemon runClient > "$OUT/client.log" 2>&1
echo "$?" > "$OUT/status.txt"
grep -nE "Aetherium|aetherium|Mixin|mixin|ERROR|FATAL|Exception|Caused by|OpenGL|GL_RENDERER|LWJGL" "$OUT/client.log" \
  | grep -v "Download" | head -300 > "$OUT/client-report.txt"
tail -80 "$OUT/client.log" > "$OUT/client-tail.txt"
# crash reports, if any
for f in run/client/crash-reports/*.txt run/crash-reports/*.txt; do [ -f "$f" ] && head -c 60000 "$f" > "$OUT/crash-$(basename "$f")"; done
head -c 300000 "$OUT/client.log" > "$OUT/client-head.log"; rm -f "$OUT/client.log"
# shrink screenshots for the repo
if command -v convert >/dev/null; then for p in "$OUT"/*.png; do [ -f "$p" ] && convert "$p" -resize 960x "$p"; done; fi
ls -l "$OUT" > "$OUT/files.txt"
grep -q PASS "$OUT/report.txt" 2>/dev/null
