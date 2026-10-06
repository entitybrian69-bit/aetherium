#!/bin/sh
# Fetches the verbatim GNU LGPL-3.0 text into LICENSE, so this repository ships the
# real license rather than a paraphrase of it. Idempotent; safe to run in CI.
#
# Why a script instead of the text in Git: the environment where Aetherium was
# authored has no route to gnu.org, and legal text reproduced from memory is a
# defect, not an optimisation. This script is the honest closure of that gap.
set -eu

TARGET="${1:-LICENSE}"
URLS="https://www.gnu.org/licenses/lgpl-3.0.txt https://raw.githubusercontent.com/spdx/license-list-data/main/text/LGPL-3.0-only.txt https://opensource.org/licenses/LGPL-3.0"

fetch() {
    if command -v curl >/dev/null 2>&1; then
        curl -fsSL --max-time 30 "$1" -o "$2"
    elif command -v wget >/dev/null 2>&1; then
        wget -q -O "$2" "$1"
    else
        echo "neither curl nor wget is available; install one, or copy the license text" >&2
        echo "from https://www.gnu.org/licenses/lgpl-3.0.txt into $TARGET" >&2
        return 1
    fi
}

# Keep the copyright + SPDX block that heads this project's LICENSE file: the
# upstream text is a license, not a copyright assignment, and the combination is
# what " LGPL-3.0-only with a notice" means in practice for a mod.
PRESERVE=""
if [ -f "$TARGET" ]; then
    PRESERVE="$(mktemp)"
    awk '/^-----/{exit} {print}' "$TARGET" >"$PRESERVE"
fi

TMP="$(mktemp)"
ok=0
for url in $URLS; do
    printf 'fetching LGPL-3.0 from %s ... ' "$url"
    if fetch "$url" "$TMP" 2>/dev/null && [ -s "$TMP" ]; then
        # Sanity: the FSF text must contain these two lines or it is not the license.
        if grep -q "GNU LESSER GENERAL PUBLIC LICENSE" "$TMP" && grep -q "TERMS AND CONDITIONS FOR USE" "$TMP"; then
            echo "ok"
            ok=1
            break
        fi
    fi
    echo "unusable"
done

if [ "$ok" -ne 1 ]; then
    echo "could not obtain a verified copy of the license; $TARGET left unchanged" >&2
    rm -f "$TMP"
    [ -n "$PRESERVE" ] && rm -f "$PRESERVE"
    exit 1
fi

OUT="$TARGET.new"
if [ -n "$PRESERVE" ] && [ -s "$PRESERVE" ]; then
    cp "$PRESERVE" "$OUT"
    printf '\n=== BEGIN VERBATIM UPSTREAM TEXT ===\n\n' >>"$OUT"
    cat "$TMP" >>"$OUT"
else
    cp "$TMP" "$OUT"
fi

if [ -f "$TARGET" ] && cmp -s "$TARGET" "$OUT"; then
    echo "LICENSE already contains the upstream text; nothing to do"
    rm -f "$OUT" "$TMP"
else
    mv "$OUT" "$TARGET"
    echo "wrote $TARGET with the verbatim LGPL-3.0 text"
fi
[ -n "$PRESERVE" ] && rm -f "$PRESERVE"
exit 0
