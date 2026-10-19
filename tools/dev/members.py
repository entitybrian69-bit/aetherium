#!/usr/bin/env python3
"""members.py VERSION CLASS [REGEX]: the javap -p member list of one class from tools/probe/VERSION.txt
(CLASS is the simple name, or a dotted suffix such as gui.Gui). Package prefixes are shortened."""
import re, sys
ver, cls = sys.argv[1], sys.argv[2]
rx = re.compile(sys.argv[3]) if len(sys.argv) > 3 else None
text = open(f"tools/probe/{ver}.txt", errors="replace").read().split("\n")
out, on = [], False
head = re.compile(r'^(public |protected |private |final |abstract |static |sealed |non-sealed )*(class|interface|enum|record) ([\w.$]+)')
for line in text:
    m = head.match(line)
    if m:
        name = m.group(3)
        on = name == cls or name.endswith("." + cls)
        if on:
            out.append(line[:200])
        continue
    if on:
        if line.startswith("}"):
            on = False
            continue
        if " :: " in line or line.startswith("Compiled from"):
            continue
        s = re.sub(r'net\.minecraft\.|com\.mojang\.blaze3d\.|com\.mojang\.|java\.lang\.|java\.util\.', '', line)
        if rx is None or rx.search(s):
            out.append(s[:240])
print("\n".join(dict.fromkeys(out)))
