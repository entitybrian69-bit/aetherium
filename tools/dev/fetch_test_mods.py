#!/usr/bin/env python3
"""Download the mods listed in a test list into a mods folder (CI compatibility runs).

usage: fetch_test_mods.py LIST MINECRAFT_VERSION DEST_DIR

Each non-comment line of LIST is "<modrinth-project> [<version_number>]". The exact version is
used when it exists for MINECRAFT_VERSION on Fabric, otherwise the newest one for that version.
Prints one line per mod; never fails the job (a missing mod is reported and skipped).
"""
import json
import os
import sys
import urllib.parse
import urllib.request

API = "https://api.modrinth.com/v2"
HEADERS = {"User-Agent": "aetherium-ci-compat-test"}


def get(url):
    req = urllib.request.Request(url, headers=HEADERS)
    with urllib.request.urlopen(req, timeout=60) as resp:
        return resp.read()


def main():
    listing, mc, dest = sys.argv[1], sys.argv[2], sys.argv[3]
    os.makedirs(dest, exist_ok=True)
    for raw in open(listing, encoding="utf-8"):
        line = raw.split("#", 1)[0].strip()
        if not line:
            continue
        parts = line.split()
        project, wanted = parts[0], (parts[1] if len(parts) > 1 else None)
        try:
            query = urllib.parse.urlencode({"loaders": json.dumps(["fabric"]), "game_versions": json.dumps([mc])})
            versions = json.loads(get(f"{API}/project/{project}/version?{query}"))
            pick = next((v for v in versions if v["version_number"] == wanted), None) if wanted else None
            if pick is None and versions:
                pick = versions[0]
            if pick is None:
                print(f"test mod {project}: no Fabric version for {mc}")
                continue
            file = next((f for f in pick["files"] if f.get("primary")), pick["files"][0])
            with open(os.path.join(dest, file["filename"]), "wb") as out:
                out.write(get(file["url"]))
            note = "" if pick["version_number"] == wanted or not wanted else f" (wanted {wanted})"
            print(f"test mod {project}: {pick['version_number']} -> {file['filename']}{note}")
        except Exception as error:  # noqa: BLE001 - report and keep going
            print(f"test mod {project}: download failed: {error}")


if __name__ == "__main__":
    main()
