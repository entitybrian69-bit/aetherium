#!/bin/sh
# Build every shippable artefact, and (optionally) prove the port deltas still apply.
#
#   sh tools/build_all.sh                 # gradle build for both loader shells
#   sh tools/build_all.sh --offline       # only the checks that need no downloads
#   sh tools/build_all.sh --ports         # every delta row: apply to a copy + verify
#   sh tools/build_all.sh --ports 1.20.6  # one row
#   sh tools/build_all.sh --jobs 2        # parallel gradle workers (default: cores/2)
#
# Output is a table on stdout plus tools/build-all-report.md, which is what a release
# note quotes. A row that fails does not stop the sweep: a port set is exactly the
# thing you want a complete answer from, not the first error.
set -eu

ROOT=$(cd "$(dirname "$0")/.." >/dev/null 2>&1 && pwd)
cd "$ROOT"

mode=build
only=""
jobs=""
for arg in "$@"; do
    case "$arg" in
        --offline) mode=offline ;;
        --ports) mode=ports ;;
        --jobs) need_jobs=1 ;;
        -j*) jobs=${arg#-j} ;;
        --jobs) ;;
        -h|--help) sed -n '2,14p' "$0"; exit 0 ;;
        *) only=$arg ;;
    esac
done
# `--jobs N` (space separated) needs a second pass because sh has no arrays.
if [ "$need_jobs" = 1 ] && [ -z "$jobs" ]; then
    echo "note: --jobs takes a number; ignoring (using gradle defaults)" >&2
fi

REPORT=tools/build-all-report.md
START=$(date -u +%Y-%m-%dT%H:%M:%SZ)
have() { command -v "$1" >/dev/null 2>&1; }

say() { printf '%-44s %s\n' "$1" "$2"; }

{
    echo "# Aetherium build sweep"
    echo
    echo "Started: $START  ·  mode: $mode${only:+  ·  row: $only}"
    echo
    echo "| step | result |"
    echo "| --- | --- |"
} >"$REPORT"

row() {
    printf '| %s | %s |\n' "$1" "$2" >>"$REPORT"
}

failures=0

record() {
    name=$1
    shift
    printf '%-44s' "$name"
    if out=$("$@" 2>&1); then
        say "" "ok"
        row "$name" "ok"
        printf '%s\n' "$out" | tail -n 3 | sed 's/^/      /'
        return 0
    else
        code=$?
        say "" "FAILED (exit $code)"
        row "$name" "**FAILED** (exit $code)"
        printf '%s\n' "$out" | tail -n 20 | sed 's/^/      /'
        failures=$((failures + 1))
        return 0
    fi
}

echo "== Aetherium build sweep ($mode)"
echo

case "$mode" in
    offline)
        record "check_refs" python3 tools/check_refs.py .
        record "tree check" python3 tools/check.py
        record "generated resources" python3 tools/gen_resources.py --check
        record "patch applicability" python3 tools/gen_deltas.py --verify
        ;;
    build)
        record "check_refs" python3 tools/check_refs.py .
        record "tree check" python3 tools/check.py
        record "generated resources" python3 tools/gen_resources.py
        if [ ! -f gradle/wrapper/gradle-wrapper.jar ]; then
            if have gradle; then
                echo "wrapper jar missing; generating it with the system gradle"
                gradle wrapper --gradle-version 9.4.1 >/dev/null 2>&1 || true
            fi
        fi
        if [ -x ./gradlew ] && [ -f gradle/wrapper/gradle-wrapper.jar ]; then
            if [ -n "$jobs" ]; then set -- --max-workers="$jobs"; else set --; fi
            record ":common:build" ./gradlew --no-daemon "$@" :common:build
            record ":fabric:build" ./gradlew --no-daemon "$@" :fabric:build
            record ":neoforge:build" ./gradlew --no-daemon "$@" :neoforge:build
            record "verifyAll" ./gradlew --no-daemon "$@" verifyAll
            for jar in fabric/build/libs/*.jar neoforge/build/libs/*.jar common/build/libs/*.jar; do
                [ -f "$jar" ] || continue
                printf '      %-58s %8s bytes\n' "$jar" "$(wc -c <"$jar" | tr -d ' ')"
            done
        else
            echo "skipping the gradle build: no wrapper jar and no system gradle." >&2
            echo "run 'sh tools/setup_jdk.sh --install gradle' once, then re-run." >&2
            row "gradle build" "skipped (no gradle)"
        fi
        ;;
    ports)
        if [ -n "$only" ]; then
            record "port $only" sh tools/port.sh --dry-run "$only"
        else
            for dir in deltas/*/; do
                version=$(basename "$dir")
                record "port $version" sh tools/port.sh --dry-run "$version"
            done
        fi
        record "patch applicability" python3 tools/gen_deltas.py --verify
        ;;
esac

END=$(date -u +%Y-%m-%dT%H:%M:%SZ)
{
    echo
    echo "Finished: $END"
    echo
    echo "Failures: $failures"
    echo
    echo "Regenerate this file with: sh tools/build_all.sh --$mode"
} >>"$REPORT"

echo
if [ "$failures" -ne 0 ]; then
    echo "build sweep: $failures step(s) failed (see $REPORT)"
    exit 1
fi
echo "build sweep: all steps ok (report: $REPORT)"
