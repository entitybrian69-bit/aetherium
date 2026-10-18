#!/usr/bin/env bash
# sig.sh CLASS REGEX VER... : public members of CLASS (javap section of tools/probe/<ver>.txt) matching REGEX
cls=$1; re=$2; shift 2
cd "$(dirname "$0")/../probe" || exit 1
for v in "$@"; do
  out=$(awk -v c="=== $cls" '$0==c{p=1;next} /^=== /{p=0} p' "$v.txt" | grep -E -- "$re" | sed -E 's/^ +//;s/net\.minecraft\.//g' | tr '\n' ';')
  echo "  $v: $out"
done
