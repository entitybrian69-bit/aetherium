#!/usr/bin/env bash
# CI driver for the backport: build, report, probe. Output lands in backport/ci/out and is
# committed back by the workflow, because the build log itself cannot be downloaded from here.
set -uo pipefail
cd "$(dirname "$0")/.."
OUT=ci/out
rm -rf "$OUT" && mkdir -p "$OUT"

./gradlew --no-daemon --stacktrace build > "$OUT/build-full.log" 2>&1
status=$?
echo "$status" > "$OUT/build-status.txt"
# The interesting part of the log: javac errors/warnings, mixin AP messages, gradle failures.
grep -nE "error:|warning:|\[mixin|Mixin|FAILURE|What went wrong|Exception|Caused by|BUILD |> Task :compileJava|cannot find symbol|symbol:|location:" "$OUT/build-full.log" \
  | grep -v "^.*Download " | head -400 > "$OUT/build-report.txt"
tail -60 "$OUT/build-full.log" > "$OUT/build-tail.txt"
# keep the full log small enough to commit
head -c 400000 "$OUT/build-full.log" > "$OUT/build.log"; rm -f "$OUT/build-full.log"
ls -l build/libs > "$OUT/jars.txt" 2>&1 || true

# ---- probe: javap of the Forge-patched, MCP-named 1.12.2 classes
if [ -f probe/classes.txt ]; then
  CP=$(find build/rfg "$HOME/.gradle/caches" -name '*.jar' 2>/dev/null | grep -iE "patchedmc|forge|minecraft|mcp|recompiled|srg" | grep -viE "sources|javadoc|gradle-|retrofuturagradle" | sort -u | tr '\n' ':')
  echo "$CP" | tr ':' '\n' > "$OUT/probe-classpath.txt"
  mkdir -p probe/out
  JAVAP=$(command -v javap)
  grep -vE '^\s*(#|$)' probe/classes.txt | while read -r cls; do
    f="probe/out/$(echo "$cls" | sed 's/.*\.//').txt"
    "$JAVAP" -p -s -cp "$CP" "$cls" > "$f" 2>&1 || echo "MISSING $cls" >> "$OUT/probe-missing.txt"
  done
  if [ -f probe/code.txt ]; then
    : > probe/out/_code.txt
    grep -vE '^\s*(#|$)' probe/code.txt | while read -r cls meth; do
      echo "=== method $cls $meth" >> probe/out/_code.txt
      "$JAVAP" -c -p -cp "$CP" "$cls" 2>/dev/null | awk -v m="$meth" '
        /^  [^ ].*\(.*\).*;$/ || /^  [^ ].*\{\};$/ { p = (index($0, " " m "(") > 0) }
        p { print }' | cut -c1-220 | head -700 >> probe/out/_code.txt
    done
  fi
fi
exit 0
