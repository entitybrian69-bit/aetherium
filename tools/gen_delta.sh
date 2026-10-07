#!/bin/sh
# Convenience wrapper for one version's delta, so a porting session is:
#
#   sh tools/gen_delta.sh 1.20.6           # regenerate + verify + show the result
#   sh tools/gen_delta.sh 1.20.6 --edit    # then open tools/porting_pins.json in $EDITOR
#   sh tools/gen_delta.sh --list           # every version the table knows
#
# The real work is tools/gen_deltas.py, and tools/porting_pins.json is the source of
# truth. This wrapper exists because typing three flags at 1 a.m. is how hand-edited
# patches get born, and hand-edited patches are how 32 version rows drift apart.
set -eu

ROOT=$(cd "$(dirname "$0")/.." >/dev/null 2>&1 && pwd)
cd "$ROOT"

PYTHON=${PYTHON:-python3}
version=${1:-}

if [ "$version" = "--list" ] || [ -z "$version" ]; then
    "$PYTHON" - "$version" <<'PY'
import json, os, sys
data = json.load(open('tools/porting_pins.json'))
rows = data['rows']
if sys.argv[1] == '--list':
    print(f"{'version':<10} {'java':<5} {'neoforge':<11} {'status':<11} delta")
    for row in rows:
        patch = os.path.join('deltas', row['version'], 'changes.patch')
        state = 'reference' if row['version'] == data['reference'] else ('yes' if os.path.exists(patch) else 'MISSING')
        print(f"{row['version']:<10} {row['java']:<5} {str(row['neoforge']):<11} {row['status']:<11} {state}")
    print(f"{len(rows)} rows")
else:
    print(__doc__ or '', end='')
    print("usage: sh tools/gen_delta.sh <version> [--edit] | --list", file=sys.stderr)
    raise SystemExit(2)
PY
    exit 0
fi

if ! "$PYTHON" -c "
import json,sys
data=json.load(open('tools/porting_pins.json'))
sys.exit(0 if any(r['version']=='$version' for r in data['rows']) else 1)
"; then
    echo "no row for '$version' in tools/porting_pins.json; see: sh tools/gen_delta.sh --list" >&2
    exit 1
fi

printf 'regenerating deltas/%s/ ... ' "$version"
"$PYTHON" tools/gen_deltas.py --only "$version" >/dev/null
echo ok

printf 'checking it applies to this tree ... '
if git apply --check --whitespace=nowarn "deltas/$version/changes.patch"; then
    echo ok
else
    echo FAILED
    echo "the reference source moved and the table's anchors no longer match it." >&2
    echo "Fix the anchors in tools/gen_deltas.py - never hand-edit the patch." >&2
    exit 1
fi

echo
echo "== deltas/$version/changes.patch (first 40 lines)"
sed -n '1,40p' "deltas/$version/changes.patch"
files=$(grep -c '^+++ ' "deltas/$version/changes.patch" || true)
echo "  ... $files file(s) touched; see deltas/$version/{derivation.md,notes.md}"

if [ "${2:-}" = "--edit" ]; then
    echo
    echo "opening tools/porting_pins.json; re-generation runs automatically when you save-quit"
    "${EDITOR:-vi}" tools/porting_pins.json
    "$PYTHON" tools/gen_deltas.py --all --matrix --verify
fi
