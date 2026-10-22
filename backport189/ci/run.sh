#!/usr/bin/env bash
# CI driver for the 1.8.9 backport: build and report. Output lands in backport189/ci/out and is
# committed back by the workflow, because the build log itself cannot be downloaded from the sandbox.
set -uo pipefail
cd "$(dirname "$0")/.."
OUT=ci/out
rm -rf "$OUT" && mkdir -p "$OUT"
chmod +x gradlew
./gradlew --no-daemon --stacktrace build > "$OUT/build-full.log" 2>&1
status=$?
echo "$status" > "$OUT/build-status.txt"
grep -nE "error:|warning:|\[mixin|Mixin|FAILURE|What went wrong|Exception|Caused by|BUILD |> Task :compileJava|cannot find symbol|symbol:|location:|Could not" "$OUT/build-full.log" \
  | grep -v "^.*Download " | head -400 > "$OUT/build-report.txt"
tail -80 "$OUT/build-full.log" > "$OUT/build-tail.txt"
head -c 400000 "$OUT/build-full.log" > "$OUT/build.log"; rm -f "$OUT/build-full.log"
ls -l build/libs > "$OUT/jars.txt" 2>&1 || true
exit 0
