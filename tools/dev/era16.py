#!/usr/bin/env python3
"""era16.py to|back FILE...: switch era files to their 1.16.5 form for editing, or back to 1.21.1 form."""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
import eras  # noqa: E402

mode = sys.argv[1]
for path in sys.argv[2:]:
    text = open(path).read()
    new = eras.select_all(text, "1.16.5" if mode == "to" else "1.21.1", path)
    if new != text:
        open(path, "w").write(new)
        print(mode, path)
