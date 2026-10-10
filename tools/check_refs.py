#!/usr/bin/env python3
"""Aetherium reference checker: fields, methods, imports.

There is no JDK in the authoring environment, so this script is the substitute for a
type check on the parts of a type check that catch real bugs in generated code:

  1. every ``receiver.member`` where ``receiver`` is a local variable, field or
     parameter whose declared type is one of OUR classes -> that member must be
     declared by that class or one of our classes in its supertype list;
  2. every static call ``ClassName.member(`` -> must be declared by ClassName;
  3. every capitalized simple name used in a body -> must be imported, declared in
     the same package, nested inside a type declared in this file, or java.lang;
  4. every import of our own classes -> the file must exist and declare it.

Unknown types (Minecraft, LWJGL, Guava, ...) are skipped by design: they are the
parts verified against real sources and marked [UNVERIFIED] where they are not.

Usage: python3 tools/check_refs.py [root]     (default: repo root)
Exit status: 0 clean, 1 problems found.
"""
from __future__ import annotations

import os
import re
import sys
from collections import defaultdict

ROOT_DEFAULT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

JAVA_LANG = {
    "String", "Object", "Integer", "Long", "Double", "Float", "Boolean", "Byte",
    "Short", "Character", "Class", "System", "Math", "Thread", "Runnable",
    "Override", "Deprecated", "SuppressWarnings", "Exception", "RuntimeException",
    "Error", "Throwable", "StringBuilder", "StringBuffer", "Number", "Comparable",
    "Iterable", "Void", "Record", "CharSequence", "Package", "ClassLoader",
    "SecurityException", "NullPointerException", "IllegalArgumentException",
    "IllegalStateException", "UnsupportedOperationException", "ProcessBuilder",
    "Process", "Enum", "Annotation", "FunctionalInterface", "SafeVarargs",
}

# Deliberately does NOT capture extends/implements: a character class containing
# \s, applied lazily before `\s*\{`, backtracks catastrophically on the long
# whitespace runs that strip_noise leaves where comments used to be (measured: 45 s
# on one file). The supertype list is parsed from the header line instead.
TYPE_DECL = re.compile(
    r"^[ \t]*(?:public|protected|private)?[ \t]*(?:(?:static|final|abstract|strictfp)[ \t]+)*"
    r"(?P<kind>class|interface|enum|record)[ \t]+(?P<name>\w+)",
    re.MULTILINE,
)
SUPERTYPE = re.compile(r"\b(?:extends|implements)[ \t]+([^\n{]+)")
METHOD_DECL = re.compile(
    r"^\s*(?:@\w+(?:\([^)]*\))?\s+)*(?:(?:public|protected|private)\s+)?"
    r"(?:(?:static|final|abstract|synchronized|default|native)\s+)*"
    r"(?:<[^>]+>\s*)?"
    r"(?P<ret>[A-Za-z_][\w.]*(?:<[^<>]*(?:<[^<>]*>)?[^<>]*>)?(?:\[\])*)\s+"
    r"(?P<name>\w+)\s*\(",
    re.MULTILINE,
)
FIELD_DECL = re.compile(
    r"^\s*(?:@\w+(?:\([^)]*\))?\s+)*(?:(?:public|protected|private)\s+)?"
    r"(?:(?:static|final|transient|volatile)\s+)*"
    r"(?!(?:return|throw|else|if|for|while|switch|do|new|try|catch|synchronized|assert)\s)"
    r"(?P<type>[A-Za-z_][\w.]*(?:<[^>]*>)?(?:\[\])*)\s+"
    r"(?P<names>\w+(?:\s*,\s*\w+)*)\s*(?:=|;)",
    re.MULTILINE,
)
CTOR_DECL = re.compile(r"^\s*(?:(?:public|protected|private)\s+)(?P<name>\w+)\s*\(", re.MULTILINE)
IMPORT = re.compile(r"^\s*import\s+(?:static\s+)?(?P<path>[\w.]+)\s*;", re.MULTILINE)
# Order matters: comments first, then string and char literals. Blanking char
# literals is not cosmetic - an apostrophe or a '}' character literal otherwise
# desynchronises the brace matcher and truncates a class body mid-file.
STRING_OR_COMMENT = re.compile(
    r"(/\*.*?\*/)|(//[^\n]*)|(\"(?:\\.|[^\"\\])*\")|('(?:\\.|[^'\\])*')",
    re.DOTALL,
)


def strip_noise(text: str) -> str:
    """Blank out strings and comments so regexes only see code."""

    def repl(match: re.Match) -> str:
        return " ".join("" if part is None else (" " * len(part)) for part in match.groups())

    out = []
    pos = 0
    for match in STRING_OR_COMMENT.finditer(text):
        out.append(text[pos:match.start()])
        out.append(re.sub(r"[^\n]", " ", match.group(0)))
        pos = match.end()
    out.append(text[pos:])
    # Collapsing whitespace runs matters as much as blanking: the patterns below are
    # line-oriented, and multi-space indentation otherwise multiplies backtracking.
    return re.sub(r"[ \t]{2,}", " ", "".join(out))


JAVA_LANG |= {
    "RuntimeException", "Exception", "Error", "Throwable", "AutoCloseable", "LinkageError",
    "Runtime", "Thread", "Runnable", "Number", "String", "CharSequence", "Boolean", "Byte",
    "Short", "Integer", "Long", "Float", "Double", "Character", "Void", "Class", "Object",
    "System", "Math", "StringBuilder", "StringBuffer", "Comparable", "Iterable", "Enum",
    "Record", "Override", "Deprecated", "SuppressWarnings", "FunctionalInterface",
    "SafeVarargs", "Annotation", "Package", "Module", "ClassLoader", "Process",
    "ProcessBuilder", "ThreadLocal", "StackTraceElement", "InheritableThreadLocal",
    "NullPointerException", "IllegalArgumentException", "IllegalStateException",
    "IndexOutOfBoundsException", "ArrayIndexOutOfBoundsException", "NumberFormatException",
    "ClassNotFoundException", "NoClassDefFoundError", "NoSuchMethodException",
    "NoSuchFieldException", "NoSuchMethodError", "IllegalAccessException",
    "ReflectiveOperationException", "InterruptedException", "UnsupportedOperationException",
    "SecurityException", "ArithmeticException", "ClassCastException", "ClassNotFoundException",
    "AssertionError", "StackOverflowError", "OutOfMemoryError", "VirtualMachineError",
    "UnsatisfiedLinkError", "IncompatibleClassChangeError", "AbstractMethodError",
    "BootstrapMethodError", "ExceptionInInitializerError", "TypeNotPresentException",
    "CloneNotSupportedException", "IllegalMonitorStateException", "NegativeArraySizeException",
    "ArrayStoreException", "StringIndexOutOfBoundsException", "EnumConstantNotPresentException",
}

class ClassInfo:
    __slots__ = ("name", "fqn", "fields", "methods", "supertypes", "nested", "path")

    def __init__(self, name: str, fqn: str, path: str) -> None:
        self.name = name
        self.fqn = fqn
        self.fields: set[str] = set()
        self.methods: set[str] = set()
        self.supertypes: list[str] = []
        self.nested: set[str] = set()
        self.path = path


def split_top_level(text: str):
    """(name, body, header) for every type declaration in a stripped file."""
    results = []
    for match in TYPE_DECL.finditer(text):
        header = text[match.start():text.find("{", match.end()) + 1]
        brace = text.find("{", match.end())
        if brace < 0:
            continue
        depth = 0
        i = brace
        while i < len(text):
            char = text[i]
            if char == "{":
                depth += 1
            elif char == "}":
                depth -= 1
                if depth == 0:
                    break
            i += 1
        results.append((match.group("name"), text[brace:i + 1], header))
    return results


def main(root: str) -> int:
    java_files: list[str] = []
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in {".git", "build", ".gradle", "node_modules", "out", "run"}]
        # Offline compile stand-ins (tools/stubs_lib, tools/minijunit) are not mod sources and
        # must not shadow the real library APIs this checker knows about.
        if os.path.relpath(dirpath, root).replace(os.sep, "/") == "tools":
            dirnames[:] = [d for d in dirnames if d not in {"stubs_lib", "minijunit"}]
        for filename in filenames:
            if filename.endswith(".java"):
                java_files.append(os.path.join(dirpath, filename))
    java_files.sort()

    classes: dict[str, ClassInfo] = {}
    by_simple: dict[str, list[str]] = defaultdict(list)
    files: dict[str, str] = {}

    # Pass 1: collect declarations.
    for path in java_files:
        raw = open(path, encoding="utf-8").read()
        code = strip_noise(raw)
        files[path] = code
        package = re.search(r"^\s*package\s+([\w.]+)\s*;", code, re.MULTILINE)
        pkg = package.group(1) if package else ""
        for name, body, header in split_top_level(code):
            fqn = f"{pkg}.{name}" if pkg else name
            info = classes.get(fqn)
            if info is None:
                info = ClassInfo(name, fqn, path)
                classes[fqn] = info
                by_simple[name].append(fqn)
            for field in FIELD_DECL.finditer(body):
                for n in re.split(r"\s*,\s*", field.group("names")):
                    if re.fullmatch(r"\w+", n):
                        info.fields.add(n)
            for method in METHOD_DECL.finditer(body):
                info.methods.add(method.group("name"))
            for ctor in CTOR_DECL.finditer(body):
                if ctor.group("name") == name:
                    info.methods.add(name)
            for found in re.finditer(r"\brecord\s+" + re.escape(name) + r"\s*\(([^)]*)\)", header):
                for component in found.group(1).split(","):
                    parts_c = component.strip().split()
                    if len(parts_c) >= 2 and re.fullmatch(r"\w+", parts_c[-1]):
                        info.methods.add(parts_c[-1])
            for found in SUPERTYPE.finditer(header):
                for part in found.group(1).split(","):
                    base = re.match(r"[\w.]+", part.strip())
                    if base:
                        info.supertypes.append(base.group(0).split(".")[-1])

    def resolve(simple: str, path: str) -> ClassInfo | None:
        code = files[path]
        for fqn in by_simple.get(simple, []):
            if fqn.rsplit(".", 1)[0] == code_package(code) or f"{fqn};" in code or fqn in import_paths(code):
                return classes[fqn]
        candidates = by_simple.get(simple, [])
        if len(candidates) == 1:
            return classes[candidates[0]]
        return None

    def import_paths(code: str) -> set[str]:
        return {m.group("path") for m in IMPORT.finditer(code)}

    def code_package(code: str) -> str:
        match = re.search(r"^\s*package\s+([\w.]+)\s*;", code, re.MULTILINE)
        return match.group(1) if match else ""

    def members(info: ClassInfo, seen: frozenset[str] = frozenset()) -> tuple[set[str], set[str]]:
        fields = set(info.fields)
        methods = set(info.methods)
        for supertype in info.supertypes:
            if supertype in seen:
                continue
            for fqn in by_simple.get(supertype, []):
                sub = classes[fqn]
                inner_fields, inner_methods = members(sub, seen | {supertype})
                fields |= inner_fields
                methods |= inner_methods
        return fields, methods

    # ---- caches: without these the checker is quadratic in file count (measured
    # at >2 minutes on this tree, which makes it useless as a per-batch tool).
    pkg_cache: dict[str, str] = {}
    imports_cache: dict[str, set[str]] = {}
    members_cache: dict[str, tuple[set[str], set[str]]] = {}
    resolve_cache: dict[tuple[str, str], ClassInfo | None] = {}

    def code_package(path: str) -> str:
        cached = pkg_cache.get(path)
        if cached is None:
            match = re.search(r"^\s*package\s+([\w.]+)\s*;", files[path], re.MULTILINE)
            cached = match.group(1) if match else ""
            pkg_cache[path] = cached
        return cached

    def import_paths(path: str) -> set[str]:
        cached = imports_cache.get(path)
        if cached is None:
            cached = {m.group("path") for m in IMPORT.finditer(files[path])}
            imports_cache[path] = cached
        return cached

    def resolve(simple: str, path: str) -> "ClassInfo | None":
        key = (simple, path)
        if key in resolve_cache:
            return resolve_cache[key]
        result: "ClassInfo | None" = None
        candidates = by_simple.get(simple)
        if candidates:
            pkg = code_package(path)
            imports = import_paths(path)
            for fqn in candidates:
                if fqn.rsplit(".", 1)[0] == pkg or fqn in imports:
                    result = classes[fqn]
                    break
            if result is None and len(candidates) == 1:
                result = classes[candidates[0]]
        resolve_cache[key] = result
        return result

    def members(info: ClassInfo) -> tuple[set[str], set[str]]:
        cached = members_cache.get(info.fqn)
        if cached is not None:
            return cached
        fields = set(info.fields)
        methods = set(info.methods)
        members_cache[info.fqn] = (fields, methods)  # pre-seed breaks cycles
        for supertype in info.supertypes:
            for fqn in by_simple.get(supertype, []):
                if fqn == info.fqn:
                    continue
                sub_fields, sub_methods = members(classes[fqn])
                fields |= sub_fields
                methods |= sub_methods
        members_cache[info.fqn] = (fields, methods)
        return fields, methods

    declared_per_file: dict[str, set[str]] = {}
    for path in java_files:
        code = files[path]
        names = {m.group("name") for m in TYPE_DECL.finditer(code)}
        names |= {m.group("name") for m in METHOD_DECL.finditer(code)}
        for field in FIELD_DECL.finditer(code):
            names |= {n for n in re.split(r"\s*,\s*", field.group("names")) if re.fullmatch(r"\w+", n)}
        # enum constants and bare `NAME,` / `NAME;` lines inside an enum body
        names |= {m.group(1) for m in re.finditer(r"^[ \t]+([A-Z][A-Z0-9_]*)[ \t]*[,;({]", code, re.MULTILINE)}
        declared_per_file[path] = names

    ARRAY_AND_ENUM_MEMBERS = {"length", "ordinal", "name", "values", "valueOf", "clone", "compareTo"}

    OBJECT_MEMBERS = {"class", "notify", "notifyAll", "wait", "equals", "hashCode", "toString", "getClass", "clone", "finalize"}
    problems: list[str] = []

    for path in java_files:
        code = files[path]
        rel = os.path.relpath(path, ROOT_DEFAULT)
        types: dict[str, str] = {}
        for match in re.finditer(r"(?:\bbreak\b|\bcase\b)[^\n]*", code):
            pass  # no-op: keeps the loop warm; declarations are matched below
        for match in re.finditer(r"(?:\bfinal\s+)?(?P<type>[A-Z][\w.]*)(?:<[^>]*>)?(?:\[\])?\s+(?P<var>\w+)\s*(?==|[;,)])", code):
            simple = match.group("type").split(".")[-1]
            if simple in by_simple:
                types.setdefault(match.group("var"), simple)
        # method parameters and fields of our own types
        for match in re.finditer(r"\((?P<params>[^()]*?)\)\s*(?:throws [\w., ]+)?\s*\{", code):
            for param in match.group("params").split(","):
                param = param.strip()
                found = re.match(r"(?:final\s+)?([A-Z][\w.]*)(?:<[^>]*>)?(?:\[\])?\s+\w+$", param)
                if found and found.group(1).split(".")[-1] in by_simple:
                    name = param.rsplit(" ", 1)[-1]
                    types.setdefault(name, found.group(1).split(".")[-1])
        for match in re.finditer(r"\bthis\.(?P<var>\w+)\.(?P<member>\w+)", code):
            simple = types.get(match.group("var"))
            if simple is None:
                continue
            info = resolve(simple, path)
            if info is None:
                continue
            fields, methods = members(info)
            if (match.group("member") not in fields and match.group("member") not in methods
                    and match.group("member") not in ARRAY_AND_ENUM_MEMBERS):
                problems.append(f"{rel}: this.{match.group('var')}.{match.group('member')} "
                                f"not declared by {info.fqn}")
        for match in re.finditer(r"(?<![\w.])(?P<var>\w+)\.(?P<member>\w+)", code):
            simple = types.get(match.group("var"))
            if simple is None:
                continue
            member = match.group("member")
            if member in OBJECT_MEMBERS or member in ARRAY_AND_ENUM_MEMBERS or member == simple or member[0].isupper():
                continue
            info = resolve(simple, path)
            if info is None:
                continue
            fields, methods = members(info)
            if member not in fields and member not in methods:
                # `x.y(` reads as a call: a method is enough even when the field
                # list is empty, and vice versa for a bare `x.y`.
                after = code[match.end():match.end() + 1]
                problems.append(f"{rel}: {match.group('var')}:{simple}.{member}"
                                f"{'(' if after == '(' else ''} not declared by {info.fqn}")

        imported = import_paths(path)
        imported_simple = {p.rsplit(".", 1)[-1] for p in imported}
        pkg = code_package(path)
        here = declared_per_file[path]
        same_package = {fqn.rsplit(".", 1)[-1] for fqn in classes if fqn.rsplit(".", 1)[0] == pkg}
        skip_imports = False
        for match in re.finditer(r"(?<![.\w])(?P<name>[A-Z]\w*)", code):
            name = match.group("name")
            if name in JAVA_LANG or name in imported_simple or name in same_package or name in here:
                continue
            if re.fullmatch(r"[A-Z][A-Z0-9_]*", name):
                continue  # enum constant or constant defined elsewhere in this package
            if not skip_imports:
                problems.append(f"{rel}: '{name}' used but not imported (package {pkg})")

        for fqcn in sorted(imported):
            if not fqcn.startswith("com.aetherium"):
                continue
            expected = os.path.join(ROOT_DEFAULT, "common/src/main/java", *fqcn.split(".")) + ".java"
            if os.path.exists(expected):
                continue
            # A trailing capitalised segment is a nested type or a static member
            # (com.aetherium.gamma.GammaApplier.GammaCurve), not a package. Walk those off
            # before calling it a missing file, or every nested-type import looks a bug.
            parts = fqcn.split(".")
            resolved = False
            while len(parts) > 1 and parts[-1][:1].isupper():
                parts.pop()
                candidate = os.path.join(ROOT_DEFAULT, "common/src/main/java", *parts) + ".java"
                if os.path.exists(candidate):
                    resolved = True
                    break
            if not resolved:
                problems.append(f"{rel}: imports {fqcn} but no such file")

    # dedupe, keep order
    seen: set[str] = set()
    unique = [p for p in problems if not (p in seen or seen.add(p))]
    if unique:
        print(f"Aetherium reference check: {len(unique)} problem(s)")
        for problem in unique:
            print("  " + problem)
        return 1
    print(f"Aetherium reference check: clean ({len(java_files)} files, {len(classes)} types)")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else ROOT_DEFAULT))
