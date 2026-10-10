#!/usr/bin/env python3
"""Generates Aetherium's shipped resources from the source of truth.

One script, three outputs, so a config option can never exist in the engine but not
in the GUI/lang/schema:

  * ``common/src/main/resources/assets/aetherium/lang/{en_us,pt_br}.json``
    — one key per ``aetherium.option.<key>`` declared in AetheriumConfig, plus every
    ``Component.translatable("...")`` literal used by the GUI. Comments from the
    config become ``<key>.tooltip`` entries, which is how the in-place subtitle text
    under a widget is translated.
  * ``CONFIG_SCHEMA.json`` — JSON Schema (draft 2020-12) for ``<game>/aetherium.json``,
    with the group/option tree, defaults, ranges and descriptions.
  * the mod icon PNG — generated with zlib so
    the repository contains no binary blobs to hand-maintain and a re-skin is a diff
    in ``VIOLET``/``panel_pixel`` below rather than an image editor session.

Usage:  python3 tools/gen_resources.py [--check]
  --check  verify the generated files are current and exit 1 if not (used by
           ``./gradlew checkTree`` and CI, so a stale lang file fails the build)
"""
from __future__ import annotations

import argparse
import json
import os
import re
import struct
import sys
import zlib
from typing import Callable

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONFIG_JAVA = os.path.join(REPO, "common/src/main/java/com/aetherium/config/AetheriumConfig.java")
SRC_ROOT = os.path.join(REPO, "common/src/main/java")
LANG_DIR = os.path.join(REPO, "common/src/main/resources/assets/aetherium/lang")
GUI_TEXTURE_DIR = os.path.join(REPO, "common/src/main/resources/assets/aetherium/textures/gui")
ICON_PATH = os.path.join(REPO, "common/src/main/resources/assets/aetherium/icon.png")
SCHEMA_PATH = os.path.join(REPO, "CONFIG_SCHEMA.json")

SCHEMA_VERSION = 3
TYPE_JSON = {
    "Boolean": "boolean",
    "Integer": "integer",
    "Long": "integer",
    "Double": "number",
    "Float": "number",
    "String": "string",
}

HEAD = re.compile(
    r"ConfigValue<(?P<type>[\w.\[\]]+)>\s+(?P<field>\w+)\s*=\s*ConfigValue\.(?P<kind>\w+)\(",
)


def balanced_args(text: str, open_index: int) -> str:
    """Args of a call, found by counting parentheses.

    A lazy ``[^;]*?\\);`` regex looks equivalent and is not: a declaration that
    contains a nested call (``AndroidRendererChoice.values()`` in the enumerated
    factories) ends the lazy match early, and the option silently disappears from
    the lang files and the schema. Counting parens, skipping string literals, is
    the only parse that is correct for every declaration style.
    """
    depth = 0
    index = open_index
    in_string = False
    while index < len(text):
        char = text[index]
        if in_string:
            if char == "\\":
                index += 2
                continue
            if char == '"':
                in_string = False
        elif char == '"':
            in_string = True
        elif char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
            if depth == 0:
                return text[open_index + 1:index]
        index += 1
    raise ValueError("unbalanced parentheses while reading a ConfigValue declaration")
LITERAL = re.compile(r'"((?:[^"\\]|\\.)*)"')


def parse_options(config_text: str) -> list[dict]:
    """Every ``ConfigValue`` field, with key, kind, default, bounds and comment."""
    options: list[dict] = []
    for match in HEAD.finditer(config_text):
        args = re.sub(r"\s+", " ", balanced_args(config_text, config_text.index("(", match.end() - 1))).strip()
        literals = LITERAL.findall(args)
        if not literals:
            continue
        key = literals[0]
        # The comment is the last string literal unless the declaration passes an
        # explicit translation key (two trailing literals: key then comment).
        comment = literals[-1] if len(literals) >= 2 else ""
        tail = args[len(key) + 2:].strip()
        default = None
        rest = tail[1:].strip() if tail.startswith(",") else tail
        first = re.match(r'(-?[\w.]+|"(?:[^"\\]|\\.)*")', rest)
        if first:
            default = first.group(1)
            if default.startswith('"'):
                default = default[1:-1]
            elif "." in default:
                default = default.rsplit(".", 1)[-1]
        low = high = None
        if match.group("kind") in ("intRange", "doubleRange"):
            nums = re.findall(r"-?\d+(?:\.\d+)?", re.sub(r'"[^"]*"', "", rest))
            # order: default, min, max, (requiresWorldReload bool)
            if len(nums) >= 3:
                low, high = nums[1], nums[2]
        options.append({
            "field": match.group("field"),
            "key": key,
            "type": match.group("type"),
            "kind": match.group("kind"),
            "default": default,
            "comment": comment.replace('\\"', '"'),
            "min": low,
            "max": high,
        })
    return options


def scan_translations() -> set[str]:
    """Keys the GUI asks the language files for."""
    keys: set[str] = set()
    patterns = [
        r'Component\.translatable\("([a-z0-9_.]+)"',
        r'new Tab\("[a-z]+", "([a-z0-9_.]+)"',
    ]
    for dirpath, _dirs, files in os.walk(SRC_ROOT):
        for name in files:
            if not name.endswith(".java"):
                continue
            text = open(os.path.join(dirpath, name), encoding="utf-8").read()
            for pattern in patterns:
                keys.update(re.findall(pattern, text))
    return keys


def humanize(key: str) -> str:
    last = key.split(".")[-1].replace("_", " ")
    return last[:1].upper() + last[1:]


# Chrome strings that a generated label cannot express; these are product copy, and
# both languages are maintained here because the GUI shows them on first launch.
CURATED_EN = {
    "aetherium.screen.title": "Aetherium Renderer",
    "aetherium.screen.fallback_button": "Aetherium...",
    "aetherium.preview.title": "Frame stats",
    "aetherium.tab.general": "General",
    "aetherium.tab.performance": "Performance",
    "aetherium.tab.quality": "Quality",
    "aetherium.tab.utilities": "Utilities",
    "aetherium.tab.advanced": "Advanced",
    "aetherium.tab.android": "Android",
    "aetherium.section.renderer_mode": "Renderer",
    "aetherium.section.overlay": "Overlay",
    "aetherium.section.conflicts": "Mod conflicts",
    "aetherium.section.backend": "GPU backend",
    "aetherium.section.meshing": "Chunk meshing",
    "aetherium.section.shaders_compile": "Shader compilation",
    "aetherium.section.vanilla_equivalents": "Vanilla equivalents",
    "aetherium.section.gamma": "Gamma",
    "aetherium.section.gamma_curve": "Gamma curve",
    "aetherium.section.dynamic_lights": "Dynamic lights",
    "aetherium.section.android_detection": "Detection",
    "aetherium.section.android_power": "Power and touch",
    "aetherium.section.diagnostics": "Diagnostics",
    "aetherium.section.experimental": "Experimental",
    "aetherium.button.write_conflict_report": "Write conflict report to log",
    "aetherium.button.clear_program_cache": "Clear program-binary cache",
}
CURATED_PT = {
    "aetherium.screen.title": "Renderizador Aetherium",
    "aetherium.screen.fallback_button": "Aetherium...",
    "aetherium.preview.title": "Estatísticas de quadro",
    "aetherium.tab.general": "Geral",
    "aetherium.tab.performance": "Desempenho",
    "aetherium.tab.quality": "Qualidade",
    "aetherium.tab.utilities": "Utilitários",
    "aetherium.tab.advanced": "Avançado",
    "aetherium.tab.android": "Android",
    "aetherium.section.renderer_mode": "Renderizador",
    "aetherium.section.overlay": "Sobreposição",
    "aetherium.section.conflicts": "Conflitos de mods",
    "aetherium.section.backend": "Backend de GPU",
    "aetherium.section.meshing": "Malha de chunks",
    "aetherium.section.shaders_compile": "Compilação de shaders",
    "aetherium.section.vanilla_equivalents": "Equivalente ao vanilla",
    "aetherium.section.gamma": "Gama",
    "aetherium.section.gamma_curve": "Curva de gama",
    "aetherium.section.dynamic_lights": "Luzes dinâmicas",
    "aetherium.section.android_detection": "Detecção",
    "aetherium.section.android_power": "Energia e toque",
    "aetherium.section.diagnostics": "Diagnóstico",
    "aetherium.section.experimental": "Experimental",
    "aetherium.button.write_conflict_report": "Gravar relatório de conflitos no log",
    "aetherium.button.clear_program_cache": "Limpar cache de programa binário",
}


def build_lang(options: list[dict]) -> tuple[dict, dict]:
    en: dict[str, str] = dict(CURATED_EN)
    pt: dict[str, str] = dict(CURATED_PT)
    for key in sorted(scan_translations()):
        en.setdefault(key, humanize(key))
        pt.setdefault(key, humanize(key))
    for option in options:
        key = "aetherium.option." + option["key"]
        en.setdefault(key, humanize(option["key"]))
        pt.setdefault(key, humanize(option["key"]))
        if option["comment"]:
            en[key + ".tooltip"] = option["comment"]
            pt[key + ".tooltip"] = option["comment"]
    return {k: en[k] for k in sorted(en)}, {k: pt[k] for k in sorted(pt)}


def build_schema(options: list[dict]) -> dict:
    groups: dict[str, dict] = {}
    for option in options:
        group, _sep, _rest = option["key"].partition(".")
        entry: dict = {}
        kind = option["kind"]
        if kind == "enumerated":
            entry["type"] = "string"
        else:
            entry["type"] = TYPE_JSON.get(option["type"], "string")
        if option["default"] is not None:
            entry["default"] = coerce(option["default"], entry["type"])
        if option["min"] is not None:
            entry["minimum"] = coerce(option["min"], entry["type"])
        if option["max"] is not None:
            entry["maximum"] = coerce(option["max"], entry["type"])
        if option["comment"]:
            entry["description"] = option["comment"]
        entry["x-java-field"] = option["field"]
        groups.setdefault(group, {})[option["key"]] = entry

    group_schemas = {}
    for name, members in sorted(groups.items()):
        group_schemas[name] = {
            "type": "object",
            "additionalProperties": True,
            "properties": dict(sorted(members.items())),
        }
    return {
        "$schema": "https://json-schema.org/draft/2020-12/schema",
        "$id": "https://github.com/entitybrian69-bit/aetherium/CONFIG_SCHEMA.json",
        "title": "Aetherium configuration file (aetherium.json)",
        "description": (
            "Generated by tools/gen_resources.py from "
            "common/src/main/java/com/aetherium/config/AetheriumConfig.java. "
            "Do not edit by hand: the engine reads the same field list reflectively, "
            "so this file is the schema of a real contract, not a copy of it. "
            "additionalProperties=true everywhere because unknown keys are preserved "
            "verbatim on save (a modpack's tuning must never be destroyed by a downgrade)."
        ),
        "type": "object",
        "required": ["version", "aetherium"],
        "additionalProperties": True,
        "properties": {
            "version": {
                "type": "integer",
                "const": SCHEMA_VERSION,
                "description": "File format version. ConfigStore#migrate handles v1 -> v2 -> v3.",
            },
            "aetherium": {
                "type": "object",
                "additionalProperties": True,
                "propertyNames": {"pattern": "^[a-z][a-z0-9_]*$"},
                "properties": group_schemas,
            },
        },
    }


def coerce(raw: str, json_type: str):
    if json_type == "boolean":
        return raw == "true"
    if json_type == "integer":
        return int(float(raw))
    if json_type == "number":
        return float(raw)
    return raw


# ------------------------------------------------------------------ png textures

def write_png(path: str, width: int, height: int, pixel: Callable[[int, int], tuple[int, int, int, int]]) -> None:
    raw = bytearray()
    for y in range(height):
        raw.append(0)  # filter type 0 (None) per scanline
        for x in range(width):
            r, g, b, a = pixel(x, y)
            raw += bytes((r & 0xFF, g & 0xFF, b & 0xFF, a & 0xFF))

    def chunk(tag: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    header = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    stream = zlib.compress(bytes(raw), 9)
    blob = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", stream) + chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as handle:
        handle.write(blob)


def violet(x: int, y: int, size: int) -> tuple[int, int, int, int]:
    """The brand ramp: deep indigo #1D0F2E -> violet #9B5CFF."""
    t = (x / max(1, size - 1) + y / max(1, size - 1)) / 2.0
    return (int(29 + 126 * t), int(15 + 77 * t), int(46 + 209 * t), 255)


def panel_pixel(x: int, y: int, size: int) -> tuple[int, int, int, int]:
    """16x16 9-slice panel: the border rows carry the edge colour, the middle is flat."""
    edge = x == 0 or y == 0 or x == size - 1 or y == size - 1
    inner = x == 1 or y == 1 or x == size - 2 or y == size - 2
    if edge:
        return (61, 31, 99, 255)
    if inner:
        return (39, 20, 65, 255)
    return (29, 15, 46, 255)


def glow_pixel(x: int, y: int, size: int) -> tuple[int, int, int, int]:
    radius = size / 2.0
    d2 = ((x - radius + 0.5) ** 2 + (y - radius + 0.5) ** 2) / (radius * radius)
    alpha = max(0.0, 1.0 - d2)
    return (195, 155, 255, int(255 * alpha * alpha))


def track_pixel(x: int, y: int, size: int) -> tuple[int, int, int, int]:
    t = x / max(1, size - 1)
    filled = 0.55
    if t > filled:
        return (46, 26, 71, 255)
    return (int(91 + 64 * (t / filled)), int(42 + 50 * (t / filled)), int(158 + 97 * (t / filled)), 255)


def icon_pixel(x: int, y: int, size: int) -> tuple[int, int, int, int]:
    """A violet diamond with a white core: legible at 16px, which is what mod lists use."""
    radius = size / 2.0
    manhattan = abs(x - radius + 0.5) + abs(y - radius + 0.5)
    if manhattan > radius * 1.02:
        return (0, 0, 0, 0)
    t = manhattan / radius
    if t < 0.34:
        return (243, 235, 255, 255)
    return (int(155 - 90 * t), int(92 - 60 * t), int(255 - 100 * t), 255)


def write_textures() -> list[str]:
    written: list[str] = []
    targets = [
        (ICON_PATH, 64, icon_pixel),
    ]
    for path, size, pixel in targets:
        write_png(path, size, size, lambda x, y, p=pixel, s=size: p(x, y, s))
        written.append(path)
    return written


def emit(path: str, text: str, check: bool) -> bool:
    existing = None
    if os.path.exists(path):
        existing = open(path, encoding="utf-8").read()
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if existing == text:
        return False
    if check:
        print(f"STALE: {os.path.relpath(path, REPO)}")
        return True
    with open(path, "w", encoding="utf-8") as handle:
        handle.write(text)
    print(f"wrote {os.path.relpath(path, REPO)} ({len(text)} chars)")
    return False


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="fail if generated files are out of date")
    args = parser.parse_args()

    if not os.path.exists(CONFIG_JAVA):
        print(f"missing {CONFIG_JAVA}", file=sys.stderr)
        return 2
    config_text = open(CONFIG_JAVA, encoding="utf-8").read()
    options = parse_options(config_text)
    declared = len(re.findall(r"public final ConfigValue<", config_text))
    if len(options) != declared:
        print(f"parsed {len(options)} of {declared} config options; the parser and AetheriumConfig "
              f"have drifted apart - fix this generator before trusting its output", file=sys.stderr)
        return 2
    en, pt = build_lang(options)
    stale = False
    for name, data in (("en_us.json", en), ("pt_br.json", pt)):
        stale |= emit(os.path.join(LANG_DIR, name), json.dumps(data, indent=2, ensure_ascii=False) + "\n", args.check)
    stale |= emit(SCHEMA_PATH, json.dumps(build_schema(options), indent=2) + "\n", args.check)
    if not args.check:
        for path in write_textures():
            print(f"wrote {os.path.relpath(path, REPO)} ({os.path.getsize(path)} bytes)")
    print(f"parsed {len(options)} options, {len(en)} lang keys, "
          f"{len(build_schema(options)['properties']['aetherium']['properties'])} groups")
    if args.check and stale:
        print("run: python3 tools/gen_resources.py", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
