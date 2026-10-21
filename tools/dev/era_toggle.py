#!/usr/bin/env python3
"""Switch era blocks of files to a version for editing: era_toggle.py VERSION FILE...

Use 1.16.5 to edit 1.16.5-only code with it active, then 1.21.1 to store the reference tree.
"""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
import eras  # noqa: E402

version = sys.argv[1]
for path in sys.argv[2:]:
    with open(path) as h:
        text = h.read()
    out = eras.select_all(text, version, path)
    if out != text:
        with open(path, "w") as h:
            h.write(out)
        print("switched", path, "to", version)
