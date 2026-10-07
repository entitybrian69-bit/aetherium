#!/bin/sh
# Offline tree checks: no Gradle, no JDK, no Minecraft download.
#
# This is the entry point that makes the repository useful on a machine that cannot
# run a build (a phone via Termux, a sandbox, a reviewer's laptop). It runs the same
# three checks CI runs before it downloads anything, in the same order, and prints a
# single pass/fail line at the end so it can be chained:
#
#     sh tools/checkTree.sh && ./gradlew build
#
# Exit status: 0 all clean, 1 a check failed, 2 a tool is missing.
set -eu

ROOT=$(cd "$(dirname "$0")/.." >/dev/null 2>&1 && pwd)
cd "$ROOT"

PYTHON=${PYTHON:-python3}
if ! command -v "$PYTHON" >/dev/null 2>&1; then
    echo "error: $PYTHON not found; Aetherium's offline checks need Python 3.9+" >&2
    exit 2
fi

FAILED=0
run() {
    name=$1
    shift
    printf '%-34s' "checkTree: $name"
    if out=$("$@" 2>&1); then
        printf 'ok\n'
        printf '%s\n' "$out" | sed 's/^/    /' | tail -n +2 || true
    else
        printf 'FAILED\n'
        printf '%s\n' "$out" | sed 's/^/    /'
        FAILED=$((FAILED + 1))
    fi
}

echo "Aetherium checkTree (root: $ROOT)"
echo

# 1. Syntax: a Java parse. This is the cheap half of "does it compile"; it catches
#    generation artifacts (a stray character, an unbalanced brace) which are the
#    failures a type check would otherwise report 40 errors deep.
if command -v node >/dev/null 2>&1 && [ -d tools/javacheck ]; then
    run "java syntax (java-parser)" node tools/javacheck/check.js "$ROOT"
elif command -v node >/dev/null 2>&1 && [ -n "${AETHERIUM_JAVACHECK:-}" ]; then
    run "java syntax (java-parser)" node "$AETHERIUM_JAVACHECK/check.js" "$ROOT"
else
    printf '%-34s%s\n' "checkTree: java syntax" "skipped (no node; set AETHERIUM_JAVACHECK=<dir> with java-parser)"
fi

# 2. References: members and imports against our own declarations.
run "java structure" "$PYTHON" tools/check_java.py "$ROOT"
run "members + imports" "$PYTHON" tools/check_refs.py "$ROOT"

# 3. Tree: placeholders, mixin registration, gradle pins, lang/schema, deltas.
run "tree consistency" "$PYTHON" tools/check.py --root "$ROOT"

# 4. Generated resources must be current (lang + CONFIG_SCHEMA.json).
run "generated resources" "$PYTHON" tools/gen_resources.py --check

echo
if [ "$FAILED" -ne 0 ]; then
    echo "checkTree: $FAILED check(s) failed"
    exit 1
fi
echo "checkTree: all checks passed"
