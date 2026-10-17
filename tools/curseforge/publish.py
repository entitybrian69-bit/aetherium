#!/usr/bin/env python3
"""Upload Aetherium jars to CurseForge (legacy upload API, X-Api-Token).

Usage: publish.py <dir-with-jars>

  aetherium-fabric-<mod>+<mc>.jar        -> project "fabric"    (Aetherium)
  aetherium-neoforge-<mod>+<mc>.jar      -> project "neoforge"  (Aetherium NeoForge)
  aetherium-backport-<mod>+1.12.2.jar    -> project "backport"  (Aetherium Backport, MixinBooter required)

Project IDs come from tools/curseforge/projects.json (null = skipped). Every successful upload is
recorded in tools/curseforge/uploaded.json, so a re-run never uploads the same jar twice (the
upload API cannot list a project's files). Minecraft 1.21.5 and newer go out as beta, like on
Modrinth. Environment: CURSEFORGE_TOKEN; DRY_RUN=1 = resolve everything, upload nothing.
Nothing secret is ever printed.
"""
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request
import uuid

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
sys.path.insert(0, os.path.join(ROOT, 'tools'))
import modrinth_publish as mp  # noqa: E402  (note, mc_key, changelog_for)

ENDPOINTS = ['https://minecraft.curseforge.com', 'https://legacy.curseforge.com']
UA = 'entitybrian69-bit/aetherium (GitHub Actions publisher)'
JAR = re.compile(r'^aetherium-(fabric|neoforge|backport)-(.+)\+(.+)\.jar$')
LOADER = {'fabric': 'Fabric', 'neoforge': 'NeoForge', 'backport': 'Forge'}
TITLE = {'fabric': 'Aetherium', 'neoforge': 'Aetherium NeoForge', 'backport': 'Aetherium Backport'}
NEWEST_RELEASE = (1, 21, 4)
JAVA = [((1, 17), 'Java 8'), ((1, 18), 'Java 16'), ((1, 20, 5), 'Java 17'), ((26,), 'Java 21'), ((999,), 'Java 25')]
RELATIONS = {'backport': [{'slug': 'mixin-booter', 'type': 'requiredDependency'}]}

note = mp.note


def request(base, method, path, token, body=None, ctype=None):
    req = urllib.request.Request(base + path, data=body, method=method)
    req.add_header('User-Agent', UA)
    req.add_header('X-Api-Token', token)
    if ctype:
        req.add_header('Content-Type', ctype)
    last = None
    for attempt in range(4):
        try:
            with urllib.request.urlopen(req, timeout=180) as r:
                raw = r.read()
                return r.status, (json.loads(raw) if raw else None)
        except urllib.error.HTTPError as e:
            raw = e.read().decode('utf-8', 'replace')[:600]
            if e.code == 429 or e.code >= 500:
                time.sleep(10 * (attempt + 1))
                last = (e.code, raw)
                continue
            return e.code, raw
        except urllib.error.URLError as e:
            time.sleep(5 * (attempt + 1))
            last = (0, 'network failure: %s' % e)
    return last


def java_for(key):
    for limit, name in JAVA:
        if key < limit:
            return name
    return None


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    jar_dir = sys.argv[1]
    token = os.environ.get('CURSEFORGE_TOKEN', '').strip()
    dry = os.environ.get('DRY_RUN', '0').strip() == '1'
    projects = json.load(open(os.path.join(HERE, 'projects.json')))
    state_path = os.path.join(HERE, 'uploaded.json')
    state = json.load(open(state_path))
    if not token:
        note('warning', 'CURSEFORGE_TOKEN is not set: add it as a repository secret. Nothing uploaded.')
        return 0

    base, versions, types = None, None, None
    for candidate in ENDPOINTS:
        st, res = request(candidate, 'GET', '/api/game/version-types', token)
        if st == 200:
            st2, res2 = request(candidate, 'GET', '/api/game/versions', token)
            if st2 == 200:
                base, types, versions = candidate, res, res2
                break
            st, res = st2, res2
        note('warning', '%s: HTTP %s %s' % (candidate, st, str(res)[:200]))
        if st in (401, 403):
            note('error', 'CurseForge refused the token (HTTP %s): create a new one and update the secret' % st)
            return 1
    if not base:
        note('error', 'CurseForge upload API unreachable')
        return 1
    type_slug = {t['id']: t.get('slug', '') for t in types}
    note('notice', 'token OK on %s (%d game versions known)' % (base, len(versions)))

    def find(name, prefix):
        hits = [v['id'] for v in versions
                if v['name'] == name and type_slug.get(v['gameVersionTypeID'], '').startswith(prefix)]
        return hits[0] if hits else None

    client = find('Client', 'environment')
    jars = []
    for name in sorted(os.listdir(jar_dir)):
        m = JAR.match(name)
        if m:
            kind, mod, mc = m.groups()
            jars.append((kind, mp.mc_key(mc), mc, mod, name))
    jars.sort(key=lambda j: (j[0], j[1]))   # oldest first: the newest version ends on top
    if not jars:
        note('error', 'no aetherium jars in %s' % jar_dir)
        return 1

    changelogs = {}
    plan, skipped, missing_project, problems = [], 0, set(), []
    for kind, key, mc, mod, name in jars:
        if name in state:
            skipped += 1
            continue
        pid = projects.get(kind)
        if not pid:
            missing_project.add(kind)
            continue
        mc_id = find(mc, 'minecraft-')
        loader_id = find(LOADER[kind], 'modloader')
        if not mc_id or not loader_id:
            problems.append('%s (CurseForge has no %s)' % (name, mc if not mc_id else LOADER[kind]))
            continue
        ids = [mc_id, loader_id]
        if client:
            ids.append(client)
        java = find(java_for(key), 'java') if java_for(key) else None
        if java:
            ids.append(java)
        if kind == 'backport':
            log = open(os.path.join(ROOT, 'backport', 'ci', 'modrinth', 'backport_changelog.md'), encoding='utf-8').read().strip()
        else:
            log = changelogs.setdefault(mod, mp.changelog_for(mod))
        meta = {
            'changelog': log,
            'changelogType': 'markdown',
            'displayName': '%s %s (%s %s)' % (TITLE[kind], mod, LOADER[kind], mc),
            'gameVersions': ids,
            'releaseType': 'beta' if (kind != 'backport' and key > NEWEST_RELEASE) else 'release',
        }
        if kind in RELATIONS:
            meta['relations'] = {'projects': RELATIONS[kind]}
        plan.append((kind, pid, name, meta))

    for kind in sorted(missing_project):
        note('warning', 'no CurseForge project ID for "%s" (%s) in tools/curseforge/projects.json: its jars were skipped'
             % (kind, TITLE[kind]))
    if problems:
        note('error', 'not uploadable: ' + '; '.join(problems))
    note('notice', '%d jars found, %d already uploaded, %d to upload%s'
         % (len(jars), skipped, len(plan), ' (DRY RUN)' if dry else ''))
    if dry:
        for kind, pid, name, meta in plan:
            print('would upload %s -> %s %s %s' % (name, pid, meta['releaseType'], meta['gameVersions']))
        return 0

    done, failed = [], len(problems)
    for kind, pid, name, meta in plan:
        payload = open(os.path.join(jar_dir, name), 'rb').read()
        st, res = upload(base, token, pid, name, meta, payload)
        if st != 200 and 'relations' in meta and 'relation' in str(res).lower():
            note('warning', '%s: dependency link refused (%s), uploading without it' % (name, str(res)[:200]))
            meta = dict(meta)
            meta.pop('relations')
            st, res = upload(base, token, pid, name, meta, payload)
        if st == 200:
            state[name] = {'project': pid, 'file': res.get('id') if isinstance(res, dict) else None}
            with open(state_path, 'w') as f:
                json.dump(state, f, indent=1, sort_keys=True)
            done.append(name)
            print('uploaded %s -> project %s file %s' % (name, pid, state[name]['file']))
        else:
            failed += 1
            note('error', '%s: HTTP %s %s' % (name, st, str(res)[:400]))
            if st in (401, 403):
                note('error', 'the token was refused for project %s: is it your project and the right ID?' % pid)
                break
        time.sleep(2)
    note('notice', 'uploaded %d of %d' % (len(done), len(plan)))
    for kind in ('fabric', 'neoforge', 'backport'):
        n = sum(1 for d in done if JAR.match(d).group(1) == kind)
        if n:
            note('notice', 'CURSEFORGE %s: %d files to project %s' % (TITLE[kind], n, projects[kind]))
    return 1 if failed else 0


def upload(base, token, pid, name, meta, payload):
    b = uuid.uuid4().hex
    body = b''.join([
        ('--%s\r\nContent-Disposition: form-data; name="metadata"\r\n'
         'Content-Type: application/json\r\n\r\n' % b).encode(),
        json.dumps(meta).encode(), b'\r\n',
        ('--%s\r\nContent-Disposition: form-data; name="file"; filename="%s"\r\n'
         'Content-Type: application/java-archive\r\n\r\n' % (b, name)).encode(),
        payload, b'\r\n',
        ('--%s--\r\n' % b).encode(),
    ])
    return request(base, 'POST', '/api/projects/%s/upload-file' % pid, token, body,
                   'multipart/form-data; boundary=' + b)


if __name__ == '__main__':
    sys.exit(main())
