#!/usr/bin/env bash
# 1.8.9: in-game smoke test under Xvfb + Mesa llvmpipe: runClient with -Daetherium.smoke (see SmokeTest).
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
# Every injector must have found its 1.8.9 target (strict count in the smoke run, see build.gradle.kts).
grep -nE "Injection validation failed|InvalidInjectionException|InvalidMixinException|InvalidAccessorException|MixinApplyError|MixinTransformerError|Critical injection failure|Mixin.*failed|could not find any targets|Unable to locate obfuscation mapping" "$OUT/client.log" \
  | cut -c1-600 | head -80 > "$OUT/mixin-failures.txt"
if [ -s "$OUT/mixin-failures.txt" ]; then
  { echo "MIXIN FAILURES ($(wc -l < "$OUT/mixin-failures.txt")):"; cat "$OUT/mixin-failures.txt"; } >> "$OUT/report.txt"
fi
# crash reports, if any
for f in run/crash-reports/*.txt; do [ -f "$f" ] && head -c 60000 "$f" > "$OUT/crash-$(basename "$f")"; done
head -c 300000 "$OUT/client.log" > "$OUT/client-head.log"; rm -f "$OUT/client.log"
# shrink screenshots for the repo
if command -v convert >/dev/null; then for p in "$OUT"/*.png; do [ -f "$p" ] && convert "$p" -resize 960x "$p"; done; fi
S="$OUT/screenshots"
if command -v compare >/dev/null && [ -f "$S/02-world.png" ] && [ -f "$S/02-world-vanilla.png" ]; then
  # lower half only (terrain; clouds and the tutorial toast move between the shots)
  for n in 02-world 02-world-vanilla 02-world-again; do convert "$S/$n.png" -gravity south -crop 100%x45%+0+60 +repage "/tmp/$n-low.png"; done
  { echo -n "renderer vs vanilla RMSE: "; compare -metric RMSE /tmp/02-world-low.png /tmp/02-world-vanilla-low.png null: 2>&1; echo;
    echo -n "renderer(again) vs vanilla RMSE: "; compare -metric RMSE /tmp/02-world-again-low.png /tmp/02-world-vanilla-low.png null: 2>&1; echo; } >> "$OUT/report.txt"
fi
ls -l "$OUT" > "$OUT/files.txt"
grep -q PASS "$OUT/report.txt" 2>/dev/null && ! grep -q "MIXIN FAILURES" "$OUT/report.txt"
