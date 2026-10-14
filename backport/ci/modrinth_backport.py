#!/usr/bin/env python3
"""Publish Aetherium Backport (Minecraft 1.12.2, Forge) to Modrinth.

Usage: modrinth_backport.py <jar>

Makes sure the project exists (slug aetherium-backport, falling back to
aetherium-backport-team_aerix; created as a draft with backport/ci/modrinth/backport_summary.txt,
backport_body.md and tools/modrinth/icon-512.png), then uploads <jar> as version <mod>+1.12.2-forge with
MixinBooter as a required dependency. A version number that already exists is skipped.

Environment: MODRINTH_TOKEN; DRY_RUN=1 prints the plan only.
"""
import json
import os
import re
import sys
import uuid

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.insert(0, os.path.join(ROOT, 'tools'))
import modrinth_publish as mp  # noqa: E402  (call, note, multipart)

SLUG = 'aetherium-backport'
TITLE = 'Aetherium Backport'
MC = '1.12.2'
HERE = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'modrinth')
ICON = os.path.join(ROOT, 'tools', 'modrinth', 'icon-512.png')
REPO = 'https://github.com/entitybrian69-bit/aetherium'


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    jar = sys.argv[1]
    m = re.match(r'^aetherium-backport-(.+)\+1\.12\.2\.jar$', os.path.basename(jar))
    if not m:
        mp.note('error', 'unexpected jar name %s' % jar)
        return 1
    mod = m.group(1)
    number = '%s+%s-forge' % (mod, MC)
    token = os.environ.get('MODRINTH_TOKEN', '').strip()
    if not token:
        mp.note('error', 'MODRINTH_TOKEN is not set')
        return 1
    dry = os.environ.get('DRY_RUN') == '1'

    if dry:
        slug = SLUG
    else:
        slug = ensure(token)
        if not slug:
            return 1
    st, proj = mp.call('GET', '/project/' + slug, token)
    if st != 200:
        mp.note('error' if not dry else 'notice', 'GET /project/%s: HTTP %s' % (slug, st))
        if not dry:
            return 1
        proj = {'id': '(dry)'}

    st, booter = mp.call('GET', '/project/mixinbooter', token)
    if st != 200:
        mp.note('error', 'MixinBooter project lookup: HTTP %s %s' % (st, booter))
        return 1

    if proj['id'] != '(dry)':
        st, versions = mp.call('GET', '/project/%s/version' % proj['id'], token)
        if st == 200 and any(v.get('version_number') == number for v in versions):
            mp.note('notice', '%s already on %s, nothing to do' % (number, slug))
            output_links(slug, proj['id'], None)
            return 0

    changelog = open(os.path.join(HERE, 'backport_changelog.md'), encoding='utf-8').read().strip()
    data = {
        'project_id': proj['id'],
        'version_number': number,
        'name': 'Aetherium Backport %s for %s (Forge)' % (mod, MC),
        'changelog': changelog,
        'dependencies': [{'project_id': booter['id'], 'dependency_type': 'required'}],
        'game_versions': [MC],
        'version_type': 'release',
        'loaders': ['forge'],
        'featured': True,
        'file_parts': ['file'],
        'primary_file': 'file',
    }
    if dry:
        mp.note('notice', 'DRY RUN: would upload %s as %s to %s (MixinBooter %s required)'
                % (os.path.basename(jar), number, slug, booter['id']))
        return 0
    body, ctype = mp.multipart(data, os.path.basename(jar), open(jar, 'rb').read())
    st, res = mp.call('POST', '/version', token, body, {'Content-Type': ctype})
    if st != 200:
        mp.note('error', 'upload %s: HTTP %s %s' % (number, st, res))
        return 1
    mp.note('notice', 'uploaded %s to %s (version id %s)' % (number, slug, res['id']))
    output_links(slug, proj['id'], res['id'])
    return 0


def create(token, slug):
    data = {
        'slug': slug,
        'title': TITLE,
        'description': open(os.path.join(HERE, 'backport_summary.txt'), encoding='utf-8').read().strip(),
        'body': open(os.path.join(HERE, 'backport_body.md'), encoding='utf-8').read().strip(),
        'categories': ['optimization', 'utility'],
        'additional_categories': [],
        'client_side': 'required',
        'server_side': 'unsupported',
        'license_id': 'LGPL-3.0-only',
        'project_type': 'mod',
        'initial_versions': [],
        'is_draft': True,
        'issues_url': REPO + '/issues',
        'source_url': REPO + '/tree/arena/46f241da-aetherium/backport',
        'wiki_url': None,
        'discord_url': None,
        'donation_urls': [],
    }
    b = uuid.uuid4().hex
    body = b''.join([
        ('--%s\r\nContent-Disposition: form-data; name="data"\r\n'
         'Content-Type: application/json\r\n\r\n' % b).encode(),
        json.dumps(data).encode(), b'\r\n',
        ('--%s\r\nContent-Disposition: form-data; name="icon"; filename="icon.png"\r\n'
         'Content-Type: image/png\r\n\r\n' % b).encode(),
        open(ICON, 'rb').read(), b'\r\n',
        ('--%s--\r\n' % b).encode(),
    ])
    return mp.call('POST', '/project', token, body, {'Content-Type': 'multipart/form-data; boundary=' + b})


def ensure(token):
    """Returns the slug of our backport project, creating it as a draft when needed."""
    st, me = mp.call('GET', '/user', token)
    if st != 200:
        mp.note('error', 'GET /user: HTTP %s %s' % (st, me))
        return None
    for candidate in (SLUG, SLUG + '-team_aerix'):
        st, proj = mp.call('GET', '/project/' + candidate, token)
        if st == 200:
            st2, members = mp.call('GET', '/project/%s/members' % proj['id'], token)
            if st2 == 200 and any(m.get('user', {}).get('id') == me['id'] for m in members):
                mp.note('notice', 'project %s (%s, status %s) exists, reusing it' % (candidate, proj['id'], proj.get('status')))
                return candidate
            mp.note('warning', 'slug %s belongs to someone else' % candidate)
            continue
        if st != 404:
            mp.note('error', 'GET /project/%s: HTTP %s %s' % (candidate, st, proj))
            return None
        st, res = create(token, candidate)
        if st == 200:
            mp.note('notice', 'created project %s (%s) as a draft' % (candidate, res['id']))
            return candidate
        if st == 400 and 'slug' in str(res).lower():
            mp.note('warning', 'slug %s is taken by a project we cannot see: %s' % (candidate, res))
            continue
        mp.note('error', 'creating %s: HTTP %s %s' % (candidate, st, res))
        return None
    mp.note('error', 'no free slug')
    return None


def output_links(slug, pid, vid):
    mp.note('notice', 'MODRINTH project https://modrinth.com/mod/%s (id %s)%s'
            % (slug, pid, (' version https://modrinth.com/mod/%s/version/%s' % (slug, vid)) if vid else ''))


if __name__ == '__main__':
    sys.exit(main())
