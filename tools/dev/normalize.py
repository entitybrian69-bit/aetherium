#!/usr/bin/env python3
"""normalize.py FILE...: rewrite era-marked files in reference (1.21.1) form.

Variants may be written plain (uncommented); select_all() activates the 1.21.1 variant and
comments out every other one, so hand-written multi-variant blocks become canonical.
"""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
import eras  # noqa: E402

for path in sys.argv[1:]:
    text = open(path).read()
    new = eras.select_all(text, "1.21.1", path)
    if new != text:
        open(path, "w").write(new)
        print("normalized", path)
