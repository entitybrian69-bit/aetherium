#!/usr/bin/env python3
"""Structural Java checker: braces, strings, and one duplicate-signature rule.

Why this exists: this repository is authored in an environment with no JDK, so nothing here
can type-check a file. The failures that actually happen during a mechanical edit are
*structural* - a dropped closing brace, a quote swallowed by a string edit, a method pasted
twice - and all three are detectable without a compiler, cheaply enough to run on every
``./gradlew checkTree``.

Deliberately NOT a parser: it will not tell you a type is wrong, and it must not be quoted
as evidence that something compiles. What it does guarantee is that braces/brackets/parens
nest, that no string or char literal is unterminated, that no block-comment is left open,
and that a file does not declare the same method signature twice.

Usage: python3 tools/check_java.py [ROOT]
Exit code 0 = clean, 1 = problems printed as "file:line: message".
"""
from __future__ import annotations

import os
import sys

SKIP_DIRS = {".git", "build", "out", ".gradle", "node_modules", "__pycache__"}
OPEN = {"{": "}", "(": ")", "[": "]"}
CLOSE = {v: k for k, v in OPEN.items()}


class Stripper:
    """Walks Java source once, emitting a same-length mask where every literal and
    comment body is replaced by spaces. Positions therefore stay line/col accurate."""

    def __init__(self, text: str) -> None:
        self.text = text
        self.out = list(text)
        self.i = 0
        self.line = 1
        self.problems: list[tuple[int, str]] = []

    def fail(self, message: str) -> None:
        self.problems.append((self.line, message))

    def at(self, offset: int = 0) -> str:
        index = self.i + offset
        return self.text[index] if index < len(self.text) else ""

    def blank(self, start: int, end: int) -> None:
        for index in range(start, min(end, len(self.text))):
            if self.text[index] != "\n":
                self.out[index] = " "

    def run(self) -> tuple[str, list[tuple[int, str]]]:
        text = self.text
        while self.i < len(text):
            char = text[self.i]
            if char == "\n":
                self.line += 1
                self.i += 1
                continue
            if char == '"':
                self.scan_string(terminated_by_backslash=True)
                continue
            if char == "'":
                self.scan_char()
                continue
            if char == "/" and self.at(1) == "/":
                end = text.find("\n", self.i)
                end = len(text) if end < 0 else end
                self.blank(self.i, end)
                self.i = end
                continue
            if char == "/" and self.at(1) == "*":
                end = text.find("*/", self.i + 2)
                if end < 0:
                    self.fail("block comment opened here is never closed")
                    self.blank(self.i, len(text))
                    return "".join(self.out), self.problems
                self.blank(self.i, end + 2)
                self.line += text.count("\n", self.i, end)
                self.i = end + 2
                continue
            if char == "\\" and not (text[self.i - 1: self.i] == ")"):
                # A bare backslash outside a literal is almost always a broken escape edit.
                self.fail("stray backslash outside a literal")
            self.i += 1
        return "".join(self.out), self.problems

    def scan_string(self, terminated_by_backslash: bool) -> None:
        text = self.text
        start = self.i
        self.i += 1
        if text[self.i - 1: self.i + 2] == '"""':
            # Text block: only """ ends it, and \" inside is legal.
            end = text.find('"""', self.i + 2)
            if end < 0:
                self.fail("text block opened here is never closed")
                self.blank(start, len(text))
                self.i = len(text)
                return
            self.line += text.count("\n", start, end)
            self.blank(start, end + 3)
            self.i = end + 3
            return
        while self.i < len(text):
            char = text[self.i]
            if char == "\\" and terminated_by_backslash:
                self.i += 2
                continue
            if char == '"':
                self.i += 1
                self.blank(start, self.i)
                return
            if char == "\n":
                self.fail("string literal is not closed on its line (Java has no multiline strings outside \"\"\" blocks)")
                self.blank(start, self.i)
                self.i += 1
                self.line += 1
                return
            self.i += 1
        self.fail("string literal opened here is never closed")
        self.blank(start, len(text))
        self.i = len(text)

    def scan_char(self) -> None:
        text = self.text
        start = self.i
        self.i += 1
        while self.i < len(text):
            char = text[self.i]
            if char == "\\":
                self.i += 2
                continue
            if char == "'":
                self.i += 1
                self.blank(start, self.i)
                return
            if char == "\n" or char == ";":
                # A quote that is not a char literal: usually an apostrophe in a comment
                # we failed to strip, or a genuinely broken literal. Report and move on
                # without consuming the line, so one bad apostrophe cannot cascade.
                self.fail("apostrophe that is not a char literal (check a comment or a string quote nearby)")
                self.i = start + 1
                return
            self.i += 1
        self.fail("char literal opened here is never closed")
        self.i = len(text)


def check_balance(masked: str, path: str, problems: list[tuple[int, str]]) -> None:
    stack: list[tuple[str, int]] = []
    line = 1
    for char in masked:
        if char == "\n":
            line += 1
            continue
        if char in OPEN:
            stack.append((char, line))
        elif char in CLOSE:
            if not stack:
                problems.append((line, f"unmatched '{char}' - nothing is open"))
                continue
            opener, opener_line = stack.pop()
            if opener != CLOSE[char]:
                problems.append((line, f"'{char}' closes a '{opener}' opened on line {opener_line}"))
    for opener, opener_line in stack:
        problems.append((opener_line, f"'{opener}' opened here is never closed"))


def check_duplicate_methods(masked: str, problems: list[tuple[int, str]]) -> None:
    """Two identical method signatures at the class level is a paste accident, and javac's
    message for it ("method already defined") costs more diagnosis time than most. The
    parameter *text* is compared, not the arity: ``warn(String, Object...)`` and
    ``warn(String, Throwable)`` are overloads this project uses on purpose, and a checker
    that cries wolf on them is worse than no checker."""
    import re

    seen: dict[tuple[str, int], int] = {}
    pattern = re.compile(r"\b(?P<name>[A-Za-z_$][\w$]*)\s*\((?P<args>[^()]*)\)\s*\{")
    for match in pattern.finditer(masked):
        # Recompute depth at the match offset by counting braces before it.
        depth = masked.count("{", 0, match.start()) - masked.count("}", 0, match.start())
        if depth != 1:
            continue
        args = " ".join(match.group("args").split())
        key = (match.group("name"), args)
        line_no = masked.count("\n", 0, match.start()) + 1
        if key in seen:
            problems.append((line_no, f"method '{key[0]}({key[1]})' is also declared on line {seen[key]}"))
        else:
            seen[key] = line_no


def java_files(root: str):
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = sorted(d for d in dirnames if d not in SKIP_DIRS)
        for name in sorted(filenames):
            if name.endswith(".java"):
                yield os.path.join(dirpath, name)


def main() -> int:
    root = sys.argv[1] if len(sys.argv) > 1 else "."
    if not os.path.isdir(root):
        print(f"not a directory: {root}", file=sys.stderr)
        return 2
    total = 0
    reported = 0
    for path in java_files(root):
        total += 1
        with open(path, encoding="utf-8") as handle:
            text = handle.read()
        if not text.endswith("\n"):
            print(f"{path}:0: file does not end with a newline (git apply in deltas/ depends on it)")
            reported += 1
        masked, problems = Stripper(text).run()
        check_balance(masked, path, problems)
        check_duplicate_methods(masked, problems)
        for line_no, message in sorted(set(problems)):
            print(f"{path}:{line_no}: {message}")
            reported += 1
    if reported:
        print(f"Aetherium Java structure: {reported} problem(s) in {total} files")
        return 1
    print(f"Aetherium Java structure: clean ({total} files)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
