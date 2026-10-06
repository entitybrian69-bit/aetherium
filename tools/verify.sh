#!/bin/sh
# Full verification ladder, cheapest check first, and it stops at the first failure.
#
#   sh tools/verify.sh              # offline checks + build + tests (if a JDK exists)
#   sh tools/verify.sh 1.20.6       # the same, against a ported tree (dry run)
#   sh tools/verify.sh --offline     # stop after the checks that need no downloads
#
# Why a script and not just `./gradlew build`: the gradle build cannot run on a
# machine with no JDK (and cannot run at all without the wrapper jar), while most of
# Aetherium's real defects - a stale mixin registration, a lang entry with no option
# behind it, an unverified mark that has quietly become a lie - are visible without
# it. This script is what CI calls, so local and CI agree on what "verified" means.
set -eu

ROOT=$(cd "$(dirname "$0")/.." >/dev/null 2>&1 && pwd)
cd "$ROOT"

offline=0
version=""
for arg in "$@"; do
    case "$arg" in
        --offline) offline=1 ;;
        -h|--help) sed -n '2,12p' "$0"; exit 0 ;;
        *) version=$arg ;;
    esac
done

step() {
    printf '\n=== %s\n' "$1"
}

if [ -n "$version" ]; then
    step "port dry run: $version"
    sh tools/port.sh --dry-run "$version"
fi

step "java syntax (parser)"
if [ -d tools/javacheck ] && command -v node >/dev/null 2>&1; then
    node tools/javacheck/check.js "$ROOT"
else
    echo "skipped: node + tools/javacheck not available (java-parser is a dev-only tool)"
fi

step "member + import resolution"
python3 tools/check_refs.py "$ROOT"

step "tree consistency"
python3 tools/check.py --root "$ROOT"

step "unverified claims"
python3 tools/check.py --root "$ROOT" --report-unverified | tail -n +1

step "generated resources"
python3 tools/gen_resources.py --check

step "shell syntax"
for f in tools/*.sh gradlew; do
    [ -f "$f" ] || continue
    sh -n "$f" || { echo "syntax error in $f" >&2; exit 1; }
done
echo "all scripts parse"

step "python syntax"
for f in tools/*.py; do
    python3 -m py_compile "$f" || { echo "syntax error in $f" >&2; exit 1; }
done
echo "all generators compile"

if [ "$offline" -eq 1 ]; then
    step "offline only: stopping before the build"
    echo "verified: $([ -n "$version" ] && echo "$version port + " || true)tree is consistent"
    exit 0
fi

step "gradle build"
if [ -x ./gradlew ] && [ -f gradle/wrapper/gradle-wrapper.jar ]; then
    ./gradlew --no-daemon verifyAll
elif command -v gradle >/dev/null 2>&1; then
    echo "wrapper jar missing; using the system Gradle to generate it first"
    gradle wrapper --gradle-version 9.4.1 >/dev/null
    ./gradlew --no-daemon verifyAll
else
    echo "no Gradle and no JDK: skipping the compile step." >&2
    echo "Run 'sh tools/setup_jdk.sh --install gradle' then './gradlew verifyAll'." >&2
    exit 3
fi

step "verdict"
echo "Aetherium verified: syntax, references, tree, resources, scripts, build, tests"
