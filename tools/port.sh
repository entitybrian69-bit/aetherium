#!/bin/sh
# Port the same source to another Minecraft version, mechanically.
#
#   sh tools/port.sh 1.20.6                # apply deltas/1.20.6/changes.patch in place
#   sh tools/port.sh --dry-run 1.20.6      # apply to a throwaway copy and verify it
#   sh tools/port.sh --revert 1.20.6       # undo an applied delta
#   sh tools/port.sh --list                # every version the matrix knows about
#   sh tools/port.sh --regen 1.20.6        # rebuild the patch from tools/porting_pins.json
#
# The contract that makes this mechanical (and the reason a port is a patch and not a
# fork): a delta may only
#   * change build pins (minecraft_version, mapping channel, loader versions),
#   * change the version ranges in AetheriumMixinPlugin,
#   * append a candidate name to an existing @Inject/@Mixin target list,
#   * swap a vanilla method name for the one that exists on the target version.
# It may never restructure a class or fix a logic bug - that belongs in common/ so all
# rows inherit it. See PORTING_MATRIX.md and CONTRIBUTING.md ("Porting to another
# Minecraft version").
set -eu

ROOT=$(cd "$(dirname "$0")/.." >/dev/null 2>&1 && pwd)
cd "$ROOT"

PINS=tools/porting_pins.json
PATCHDIR=deltas

die() {
    printf 'port: %s\n' "$*" >&2
    exit 1
}

mode=apply
version=""
while [ $# -gt 0 ]; do
    case "$1" in
        --dry-run) mode=dry ;;
        --revert) mode=revert ;;
        --regen) mode=regen ;;
        --list) mode=list ;;
        -h|--help) sed -n '2,20p' "$0"; exit 0 ;;
        -*) die "unknown option $1 (see --help)" ;;
        *) version=$1 ;;
    esac
    shift
done

if [ "$mode" = list ]; then
    [ -f "$PINS" ] || die "$PINS is missing; the matrix data lives there"
    python3 - "$PINS" <<'PY'
import json, sys
data = json.load(open(sys.argv[1]))
print(f"{'version':<10} {'loader pins':<34} {'mappings':<22} delta")
for row in data['rows']:
    patch = f"deltas/{row['version']}/changes.patch"
    import os
    have = 'yes' if os.path.exists(patch) else 'NO'
    pins = f"{row['fabric_loader']}, nf {row['neoforge']}"
    print(f"{row['version']:<10} {pins:<34} {row['mappings']:<22} {have}")
print(f"{len(data['rows'])} rows")
PY
    exit 0
fi

[ -n "$version" ] || die "no version given; try: sh tools/port.sh --list"
patch="$PATCHDIR/$version/changes.patch"

case "$mode" in
    regen)
        [ -f "$PINS" ] || die "$PINS is missing"
        python3 tools/gen_deltas.py --only "$version"
        echo "port: regenerated $patch from $PINS"
        exit 0
        ;;
esac

[ -f "$patch" ] || die "$patch not found. Generate it with: python3 tools/gen_deltas.py --only $version"

have_git=0
if command -v git >/dev/null 2>&1 && git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    have_git=1
fi
[ "$have_git" -eq 1 ] || die "git is required (the patch is applied and verified with it)"

# A delta is only meaningful against the reference commit; applying it to a tree that
# already differs is how you get a half-ported file.
if [ "$mode" != dry ] && [ -n "$(git status --porcelain)" ]; then
    die "working tree is dirty; commit or stash first (a dry run is safe: --dry-run $version)"
fi

if [ "$mode" = revert ]; then
    git apply -R --whitespace=nowarn "$patch"
    echo "port: reverted $patch"
    exit 0
fi

apply_check() {
    # git apply --check catches a stale patch before anything is written; the
    # whitespace rule is nowarn because the Java files are space-indented by design.
    git apply --check --whitespace=nowarn "$patch" 2>&1 | sed 's/^/    /' || return 1
}

if [ "$mode" = dry ]; then
    work=$(mktemp -d)
    trap 'rm -rf "$work"' EXIT INT TERM
    printf 'port: copying tree to %s ... ' "$work"
    # cp the tracked files only: a real port must not inherit uncommitted work.
    git ls-files -z | xargs -0 -I{} cp --parents {} "$work"/ 2>/dev/null
    git ls-files -z --common | true
    echo done
    printf 'port: applying %s ... ' "$patch"
    (cd "$work" && git init -q . && git apply --whitespace=nowarn "$ROOT/$patch") ||
        die "patch did not apply"
    echo ok
    printf 'port: verifying the ported tree ... '
    if (cd "$work" && python3 "$ROOT/tools/check_refs.py" . >/dev/null 2>&1 &&
        python3 "$ROOT/tools/check.py" --root . --skip-deltas >/dev/null 2>&1); then
        echo ok
        echo "port: $version dry run clean (syntax + members + tree, no compile: that needs the Minecraft jar)"
    else
        echo FAILED
        (cd "$work" && python3 "$ROOT/tools/check_refs.py" . || true; python3 "$ROOT/tools/check.py" --root . --skip-deltas || true) | sed 's/^/    /' | head -40
        exit 1
    fi
    exit 0
fi

apply_check || die "$patch does not apply to this tree (regen it: sh tools/port.sh --regen $version)"
git apply --whitespace=nowarn "$patch"
echo "port: applied $patch"
printf 'port: verifying ... '
if python3 tools/check_refs.py . >/dev/null 2>&1 && python3 tools/check.py --skip-deltas >/dev/null 2>&1; then
    echo ok
else
    echo "FAILED - the checks below explain why; fix common/ or the delta, do not patch around it"
    python3 tools/check_refs.py . | head -30 || true
    python3 tools/check.py --skip-deltas | head -30 || true
    exit 1
fi
echo
echo "next: ./gradlew --refresh-dependencies verifyAll   # compile against $version"
echo "      (update gradle.properties' minecraft_version first if you want the real"
echo "       mappings; the delta already rewrote the pins it knows about)"
