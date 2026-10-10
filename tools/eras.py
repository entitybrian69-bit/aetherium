#!/usr/bin/env python3
"""Era markers: the single mechanism that adapts the 1.21.1 reference source to the other 32 versions.

A Java (or JSON) file may contain blocks like

    // @era:options-begin instances
    <active code — the variant for the version this tree is currently set to>
    // @era:options-else fields
    //~ <inactive code, every line prefixed with "//~ ">
    // @era:options-end

``select(tree_text, version)`` swaps the active variant to the one ``ERAS`` picks for the version,
commenting the old one out. The operation is reversible and idempotent, and the reference tree
is simply the result for 1.21.1. A variant label may list several names separated by ``|``.

Every signature behind a variant was read from the javap dumps in tools/probe/ (see
tools/stubgen.py + tools/stubcheck.py, which compile each variant against those signatures).
"""
import re


def vkey(v):
    return [int(x) for x in v.split(".")]


def in_range(version, rng):
    if ".." not in rng:  # a bare version means exactly that version
        return vkey(version) == vkey(rng)
    lo, _, hi = rng.partition("..")
    k = vkey(version)
    if lo and k < vkey(lo):
        return False
    if hi and k > vkey(hi):
        return False
    return True


# name -> ordered list of (variant, version range); first match wins.
ERAS = {
    # --- options access (VanillaOptions, OptionInstanceMixin) -------------------------------
    "options": [("fields", "..1.18.2"), ("instances", "1.19..")],
    "simdist": [("none", "..1.17.1"), ("sim", "1.18..")],
    "graphics": [("status", "..1.21.10"), ("preset", "1.21.11..")],
    # --- GUI (McCanvas, AetheriumScreen, GuiMixin) ------------------------------------------
    "gui": [("stack", "..1.19.4"), ("graphics", "1.20..1.21.11"), ("extractor", "26.1..")],
    "gui-layer": [("none", "..1.21.5"), ("stratum", "1.21.6..")],
    "screen-pkg": [("flat", "..1.20.6"), ("options", "1.21..")],
    "screen-owner": [("minecraft", "..26.1"), ("gui", "26.2..")],
    "input": [("doubles", "..1.21.8"), ("events", "1.21.9..")],
    "scroll": [("three", "..1.20.1"), ("four", "1.20.2..")],
    "background": [("none", "..1.20.1"), ("render", "1.20.2..1.21.11"), ("extract", "26.1..")],
    "hud": [("stack", "..1.19.4"), ("graphics-float", "1.20..1.20.6"), ("graphics-delta", "1.21..1.21.11"),
            ("extractor", "26.1"), ("none", "26.2..")],
    # --- world / render hooks ---------------------------------------------------------------
    "light-hook": [("color", "..1.21.4"), ("brightness", "1.21.5..1.21.11"), ("coords", "26.1"), ("none", "26.2..")],
    "weather": [("level", "..1.21.1"), ("effect", "1.21.2..")],
    "vignette": [("render", "..1.21.11"), ("extract", "26.1"), ("none", "26.2..")],
    "dirty": [("blocks", "..26.1"), ("sections", "26.2..")],
    "reload": [("all-changed", "..26.1"), ("invalidate", "26.2..")],
    "entity-cull": [("five", "..26.2"), ("six", "26.3..")],
    "chunk-upload": [("crdbool", "..1.17.1"), ("crd", "1.18..1.20.1"), ("srd", "1.20.2..1.21.11"), ("none", "26.1..")],
    "be-render": [("render", "..1.21.8"), ("extract", "1.21.9..26.1"), ("extract-flag", "26.2..")],
    "bg-threads": [("clamp", "..1.17.1"), ("property", "1.18..")],
    "sound-click": [("event", "..1.19.2"), ("holder", "1.19.3..")],
    # --- platform -------------------------------------------------------------------------
    "logger": [("log4j", "..1.17.1"), ("slf4j", "1.18..")],
}

BEGIN = re.compile(r"^(\s*)// @era:([\w-]+)-begin (\S+)\s*$")
ELSE = re.compile(r"^(\s*)// @era:([\w-]+)-else (\S+)\s*$")
END = re.compile(r"^(\s*)// @era:([\w-]+)-end\s*$")
INACTIVE = re.compile(r"^(\s*)//~ ?(.*)$")


def variant_for(name, version):
    if name not in ERAS:
        raise KeyError("unknown era %r" % name)
    for variant, rng in ERAS[name]:
        if in_range(version, rng):
            return variant
    raise KeyError("era %r has no variant for %s" % (name, version))


def _activate(lines):
    out = []
    for line in lines:
        m = INACTIVE.match(line)
        if m:
            out.append(m.group(1) + m.group(2))
        else:
            out.append(line)
    return out


def _deactivate(lines):
    out = []
    for line in lines:
        if not line.strip():
            out.append("")  # never leave trailing whitespace in a ported tree
            continue
        indent = len(line) - len(line.lstrip())
        out.append(line[:indent] + "//~ " + line[indent:])
    return out


def select(text, version, path="<text>", used=None):
    """Return text with every era block switched to the variant for ``version``."""
    lines = text.split("\n")
    out = []
    i = 0
    while i < len(lines):
        m = BEGIN.match(lines[i])
        if not m:
            if ELSE.match(lines[i]) or END.match(lines[i]):
                raise SystemExit("%s:%d: era marker outside a block" % (path, i + 1))
            out.append(lines[i])
            i += 1
            continue
        indent, name = m.group(1), m.group(2)
        blocks = [(m.group(3), [])]
        i += 1
        while True:
            if i >= len(lines):
                raise SystemExit("%s: unterminated era block %s" % (path, name))
            e = ELSE.match(lines[i])
            n = END.match(lines[i])
            if e and e.group(2) == name:
                blocks.append((e.group(3), []))
            elif n and n.group(2) == name:
                break
            elif BEGIN.match(lines[i]) and BEGIN.match(lines[i]).group(2) == name:
                raise SystemExit("%s:%d: nested era block %s" % (path, i + 1, name))
            else:
                blocks[-1][1].append(lines[i])
            i += 1
        i += 1  # skip end
        want = variant_for(name, version)
        if used is not None:
            used.add(name)
        chosen = None
        for idx, (label, _) in enumerate(blocks):
            if want in label.split("|"):
                chosen = idx
                break
        if chosen is None:
            raise SystemExit("%s: era block %s has no variant %r (for %s); variants: %s"
                             % (path, name, want, version, [b[0] for b in blocks]))
        # normalise: active block = chosen, all others inactive
        normalised = []
        for idx, (label, body) in enumerate(blocks):
            plain = _activate(body) if idx != 0 else body
            normalised.append((label, plain))
        ordered = [normalised[chosen]] + [b for k, b in enumerate(normalised) if k != chosen]
        active_label, active_body = ordered[0]
        ordered[0] = (active_label, select("\n".join(active_body), version, path, used).split("\n") if active_body else active_body)
        out.append("%s// @era:%s-begin %s" % (indent, name, ordered[0][0]))
        out.extend(ordered[0][1])
        for label, body in ordered[1:]:
            out.append("%s// @era:%s-else %s" % (indent, name, label))
            out.extend(_deactivate(body))
        out.append("%s// @era:%s-end" % (indent, name))
    return "\n".join(out)


def has_markers(text):
    return "// @era:" in text


def select_all(text, version, path="<text>", used=None):
    """Apply ``select`` until stable, so blocks nested inside a variant are resolved too."""
    for _ in range(8):
        new = select(text, version, path, used)
        if new == text:
            return new
        text = new
    return text
