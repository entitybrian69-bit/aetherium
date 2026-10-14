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

# ---- probe: javap of the Forge-patched, MCP-named 1.12.2 classes (RFG's recompiled jar only:
# the shared Gradle cache also holds the main mod's modern Minecraft jars, which share class names)
if [ -f probe/classes.txt ]; then
  MC=$(ls build/rfg/recompiled_minecraft-*.jar 2>/dev/null | head -1)
  LIBS=$(find "$HOME/.gradle/caches/modules-2" \( -name 'lwjgl-2.9*.jar' -o -name 'launchwrapper-1.12.jar' \) 2>/dev/null | tr '\n' ':')
  CP="$MC:$LIBS"
  echo "$CP" | tr ':' '\n' > "$OUT/probe-classpath.txt"
  rm -rf probe/out && mkdir -p probe/out
  grep -vE '^\s*(#|$)' probe/classes.txt > "$OUT/classes.lst"
  javap -p -s -cp "$CP" $(cat "$OUT/classes.lst") > "$OUT/all.txt" 2> "$OUT/probe-errors.txt"
  # split on "Compiled from": one file per class, named after the simple (binary) name
  awk -v dir=probe/out '
    /^Compiled from/ { hdr = $0; next }
    /^([a-z][a-z ]* )?(class|interface|enum) [A-Za-z0-9_.$]+/ {
      for (i = 1; i <= NF; i++) if ($i == "class" || $i == "interface" || $i == "enum") { name = $(i+1); break }
      sub(/<.*/, "", name); n = split(name, parts, "."); file = dir "/" parts[n] ".txt"
      print hdr > file
    }
    { if (file != "") print >> file }
  ' "$OUT/all.txt"
  rm -f "$OUT/all.txt"
  if [ -f probe/code.txt ]; then
    : > probe/out/_code.txt
    for cls in $(grep -vE '^\s*(#|$)' probe/code.txt | awk '{print $1}' | sort -u); do
      javap -c -p -cp "$CP" "$cls" > "$OUT/code-$cls.txt" 2>/dev/null
    done
    grep -vE '^\s*(#|$)' probe/code.txt | while read -r cls meth; do
      echo "=== method $cls $meth" >> probe/out/_code.txt
      awk -v m="$meth" '
        /^  [^ ].*\(.*\).*;$/ { p = (index($0, " " m "(") > 0) }
        p { print }' "$OUT/code-$cls.txt" | cut -c1-220 | head -900 >> probe/out/_code.txt
    done
    rm -f "$OUT"/code-*.txt
  fi
fi
exit 0
