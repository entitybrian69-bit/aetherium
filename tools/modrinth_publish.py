#!/usr/bin/env python3
"""Upload the shipped jars of the current mod_version to Modrinth.

Usage: modrinth_publish.py <dir-with-jars>

Environment:
  MODRINTH_TOKEN    personal access token with the "Create versions" scope (a repository secret)
  MODRINTH_PROJECT  project slug or id, e.g. aetherium-team_aerix
  DRY_RUN           "1" = check everything and print the plan, upload nothing
  LOADERS           optional comma list (e.g. "fabric"): only jars of these loaders go to this project

One Modrinth version per jar, numbered <mod>+<mc>-<loader> (e.g. 1.8.2+1.20.1-fabric). A version
number that already exists on the project is skipped, so a re-run never duplicates anything and
versions uploaded by hand are left alone. Minecraft 1.21.4 and older go out as "release", newer ones
as "beta" (the renderer port there has seen less device testing). Nothing secret is ever printed.
"""
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request
import uuid

API = 'https://api.modrinth.com/v2'
UA = 'entitybrian69-bit/aetherium (GitHub Actions publisher)'
JAR = re.compile(r'^aetherium-(fabric|neoforge)-(.+)\+(.+)\.jar$')
LOADER_NAME = {'fabric': 'Fabric', 'neoforge': 'NeoForge'}
NEWEST_RELEASE = (1, 21, 4)


def note(level, msg):
    # GitHub annotation (readable through the checks API) plus a plain log line.
    print('::%s::%s' % (level, msg.replace('%', '%25').replace('\r', '').replace('\n', '%0A')))


def call(method, path, token, body=None, headers=None):
    req = urllib.request.Request(API + path, data=body, method=method)
    req.add_header('User-Agent', UA)
    req.add_header('Authorization', token)
    for k, v in (headers or {}).items():
        req.add_header(k, v)
    last = 'gave up after retries'
    for attempt in range(5):
        try:
            with urllib.request.urlopen(req, timeout=120) as r:
                raw = r.read()
                return r.status, (json.loads(raw) if raw else None)
        except urllib.error.HTTPError as e:
            raw = e.read().decode('utf-8', 'replace')
            if e.code == 429 or e.code >= 500:
                wait = int(e.headers.get('X-Ratelimit-Reset') or 0) or 5 * (attempt + 1)
                time.sleep(min(wait, 60))
                last = 'HTTP %s %s' % (e.code, raw[:300])
                continue
            return e.code, raw[:600]
        except urllib.error.URLError as e:
            time.sleep(5 * (attempt + 1))
            last = 'network failure: %s' % e
    return 0, last


def mc_key(v):
    return tuple(int(p) for p in re.findall(r'\d+', v))


def multipart(data, filename, payload):
    b = uuid.uuid4().hex
    out = []
    out.append(('--%s\r\nContent-Disposition: form-data; name="data"\r\n'
                'Content-Type: application/json\r\n\r\n' % b).encode())
    out.append(json.dumps(data).encode() + b'\r\n')
    out.append(('--%s\r\nContent-Disposition: form-data; name="file"; filename="%s"\r\n'
                'Content-Type: application/java-archive\r\n\r\n' % (b, filename)).encode())
    out.append(payload + b'\r\n')
    out.append(('--%s--\r\n' % b).encode())
    return b''.join(out), 'multipart/form-data; boundary=' + b


def changelog_for(mod_version):
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'modrinth_changelog.md')
    text, cur, buf = {}, None, []
    if os.path.exists(path):
        for line in open(path, encoding='utf-8'):
            m = re.match(r'^##\s+(\S+)\s*$', line)
            if m:
                if cur:
                    text[cur] = ''.join(buf).strip()
                cur, buf = m.group(1), []
            elif cur:
                buf.append(line)
        if cur:
            text[cur] = ''.join(buf).strip()
    return text.get(mod_version) or 'Aetherium %s.' % mod_version


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    jar_dir = sys.argv[1]
    token = os.environ.get('MODRINTH_TOKEN', '').strip()
    project = os.environ.get('MODRINTH_PROJECT', '').strip()
    dry = os.environ.get('DRY_RUN', '0').strip() == '1'
    only = {l.strip() for l in os.environ.get('LOADERS', '').split(',') if l.strip()}
    if not token:
        note('error', 'MODRINTH_TOKEN is not set: add it as a repository secret')
        return 1
    if not project:
        note('error', 'MODRINTH_PROJECT is not set')
        return 1
    mod_version = next(l.split('=', 1)[1].strip() for l in open('gradle.properties')
                       if l.startswith('mod_version='))

    st, proj = call('GET', '/project/' + project, token)
    if st != 200:
        note('error', 'project %s: HTTP %s %s' % (project, st, proj))
        return 1
    pid = proj['id']

    st, tags = call('GET', '/tag/game_version', token)
    if st != 200:
        note('error', 'game version list: HTTP %s %s' % (st, tags))
        return 1
    known_mc = {t['version'] for t in tags}

    st, existing = call('GET', '/project/%s/version' % pid, token)
    if st != 200:
        note('error', 'existing versions: HTTP %s %s' % (st, existing))
        return 1
    have = {v['version_number'] for v in existing}

    jars = []
    for name in os.listdir(jar_dir):
        m = JAR.match(name)
        if not m:
            continue
        loader, ver, mc = m.groups()
        if only and loader not in only:
            continue
        if ver != mod_version:
            note('warning', '%s is not version %s, ignored' % (name, mod_version))
            continue
        jars.append((mc_key(mc), mc, loader, name))
    jars.sort()   # oldest first, so the newest Minecraft version ends up on top of the list
    if not jars:
        note('error', 'no aetherium-*-%s+*.jar in %s' % (mod_version, jar_dir))
        return 1

    changelog = changelog_for(mod_version)
    plan, skipped, unknown = [], [], []
    for key, mc, loader, name in jars:
        number = '%s+%s-%s' % (mod_version, mc, loader)
        if number in have:
            skipped.append(number)
        elif mc not in known_mc:
            unknown.append(mc)
        else:
            plan.append((key, mc, loader, name, number))

    note('notice', 'project %s (%s), "%s": %d %sjars found, %d already on Modrinth, %d to upload%s'
         % (project, pid, proj.get('title'), len(jars), '/'.join(sorted(only)) + ' ' if only else '',
            len(skipped), len(plan),
            ' (DRY RUN, nothing uploaded)' if dry else ''))
    if unknown:
        note('error', 'Modrinth does not know these Minecraft versions, not uploaded: '
             + ', '.join(sorted(set(unknown), key=mc_key)))
    if plan:
        note('notice', 'to upload: ' + ', '.join(p[4] + (' [beta]' if p[0] > NEWEST_RELEASE else '')
                                                for p in plan))
    print('changelog for %s:\n%s' % (mod_version, changelog))

    failed = len(set(unknown))
    done = []
    for key, mc, loader, name, number in plan:
        data = {
            'project_id': pid,
            'name': 'Aetherium %s (%s %s)' % (mod_version, LOADER_NAME[loader], mc),
            'version_number': number,
            'changelog': changelog,
            'dependencies': [],
            'game_versions': [mc],
            'version_type': 'beta' if key > NEWEST_RELEASE else 'release',
            'loaders': [loader],
            'featured': False,
            'status': 'listed',
            'file_parts': ['file'],
            'primary_file': 'file',
        }
        if dry:
            continue
        body, ctype = multipart(data, name, open(os.path.join(jar_dir, name), 'rb').read())
        st, res = call('POST', '/version', token, body, {'Content-Type': ctype})
        if st == 200:
            done.append(number)
            print('uploaded %s' % number)
        else:
            failed += 1
            note('error', '%s: HTTP %s %s' % (number, st, res))
            if st in (401, 403):
                note('error', 'the token was refused: it needs the "Create versions" scope and '
                              'must belong to a member of the project')
                break
    if not dry:
        note('notice', 'uploaded %d of %d' % (len(done), len(plan)))
    return 1 if failed else 0


if __name__ == '__main__':
    sys.exit(main())
