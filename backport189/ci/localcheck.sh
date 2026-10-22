#!/usr/bin/env bash
# Local type check of the 1.8.9 backport (sandbox only; CI builds the real jar).
# Needs /tmp/ref from backport189/ci/localref.sh: MCP-named 1.8.9 classes, Forge 1.8.9 API classes
# (compiled from the Forge sources against vanilla, so Forge-patched methods are missing), Mixin 0.7.
set -uo pipefail
cd "$(dirname "$0")/.."
R=/tmp/ref
CP="$R/stubs:$R/mixincls:$R/forgecls:$R/mc:$(find $R/MCP-919-main/jars/libraries -name '*.jar' | tr '\n' ':')"
rm -rf /tmp/b189 && mkdir -p /tmp/b189/src /tmp/b189/out
cp -r src/main/java/. /tmp/b189/src/
grep -vE '^\s*(#|$)' shared-sources.txt | while read -r f; do mkdir -p "/tmp/b189/src/com/aetherium/$(dirname "$f")"; cp "../common/src/main/java/com/aetherium/$f" "/tmp/b189/src/com/aetherium/$f"; done
/tmp/tc/jre/bin/java -Xmx2g -jar /tmp/tc/ecj.jar -source 8 -target 8 -nowarn -encoding UTF-8 -proceedOnError -cp "$CP" -d /tmp/b189/out /tmp/b189/src > /tmp/b189/log.txt 2>&1
grep -c "ERROR in" /tmp/b189/log.txt
