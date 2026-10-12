#!/usr/bin/env python3
"""Modrinth project housekeeping for the NeoForge split.

  modrinth_project.py ensure <slug> "<title>"
      Make sure our project <slug> exists; create it as a draft if it does not. The summary, body
      and icon come from tools/modrinth/neoforge_summary.txt, neoforge_body.md and icon-512.png.
      If <slug> is taken by someone else's project, <slug>-team_aerix is used instead. The slug in
      use is written to $GITHUB_OUTPUT as slug=<slug>.

  modrinth_project.py drop-loader <project> <loader>
      Delete every version of <project> whose only loader is <loader> (the NeoForge versions leave
      the main project; Modrinth refuses a file that already exists in another project).

  modrinth_project.py list <project>
      Print every version of <project> (number, loaders, game versions, channel, date, file name).

  modrinth_project.py show <project>
      Print the title, summary and description (body) as annotations, in pieces.

  modrinth_project.py delete-number <project> <loader> <version_number>
      Delete the versions of <project> numbered exactly <version_number> whose only loader is <loader>.

  modrinth_project.py icon <project> <png>
      Set the project icon.

  modrinth_project.py text <project> <summary_file> <body_file>
      Replace the project summary and description.

Environment: MODRINTH_TOKEN (scopes: Create projects, Read projects, Create versions, Delete versions).
"""
import json
import os
import sys
import uuid

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import modrinth_publish as mp  # noqa: E402  (call(), note())

HERE = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'modrinth')
REPO = 'https://github.com/entitybrian69-bit/aetherium'


def output(key, value):
    path = os.environ.get('GITHUB_OUTPUT')
    if path:
        with open(path, 'a') as f:
            f.write('%s=%s\n' % (key, value))
    print('%s=%s' % (key, value))


def mine(token, project, me):
    st, members = mp.call('GET', '/project/%s/members' % project['id'], token)
    return st == 200 and any(m.get('user', {}).get('id') == me for m in members)


def create(token, slug, title):
    data = {
        'slug': slug,
        'title': title,
        'description': open(os.path.join(HERE, 'neoforge_summary.txt'), encoding='utf-8').read().strip(),
        'body': open(os.path.join(HERE, 'neoforge_body.md'), encoding='utf-8').read().strip(),
        'categories': ['optimization', 'utility'],
        'additional_categories': [],
        'client_side': 'required',
        'server_side': 'unsupported',
        'license_id': 'LGPL-3.0-only',
        'project_type': 'mod',
        'initial_versions': [],
        'is_draft': True,
        'issues_url': REPO + '/issues',
        'source_url': REPO,
        'wiki_url': None,
        'discord_url': None,
        'donation_urls': [],
    }
    icon = open(os.path.join(HERE, 'icon-512.png'), 'rb').read()
    b = uuid.uuid4().hex
    body = b''.join([
        ('--%s\r\nContent-Disposition: form-data; name="data"\r\n'
         'Content-Type: application/json\r\n\r\n' % b).encode(),
        json.dumps(data).encode(), b'\r\n',
        ('--%s\r\nContent-Disposition: form-data; name="icon"; filename="icon.png"\r\n'
         'Content-Type: image/png\r\n\r\n' % b).encode(),
        icon, b'\r\n',
        ('--%s--\r\n' % b).encode(),
    ])
    return mp.call('POST', '/project', token, body, {'Content-Type': 'multipart/form-data; boundary=' + b})


def ensure(token, slug, title):
    st, me = mp.call('GET', '/user', token)
    if st != 200:
        mp.note('error', 'GET /user: HTTP %s %s' % (st, me))
        return 1
    for candidate in (slug, slug + '-team_aerix'):
        st, proj = mp.call('GET', '/project/' + candidate, token)
        if st == 200:
            if mine(token, proj, me['id']):
                mp.note('notice', 'project %s (%s, "%s", status %s) already exists, reusing it'
                        % (candidate, proj['id'], proj['title'], proj.get('status')))
                output('slug', candidate)
                return 0
            mp.note('warning', 'slug %s belongs to someone else\'s project' % candidate)
            continue
        if st != 404:
            mp.note('error', 'GET /project/%s: HTTP %s %s' % (candidate, st, proj))
            return 1
        st, res = create(token, candidate, title)
        if st == 200:
            mp.note('notice', 'created project %s (%s, "%s") as a draft' % (candidate, res['id'], title))
            output('slug', candidate)
            return 0
        if st == 400 and 'slug' in str(res).lower() and 'taken' in str(res).lower():
            # Taken by a project we cannot see (someone else's draft or private project).
            mp.note('warning', 'slug %s is taken by a project we cannot see, trying the next one' % candidate)
            continue
        mp.note('error', 'creating %s: HTTP %s %s' % (candidate, st, res))
        if st in (401, 403):
            mp.note('error', 'the token was refused: it needs the "Create projects" scope')
        return 1
    mp.note('error', 'no free slug for %s' % slug)
    return 1


def drop_loader(token, project, loader):
    st, versions = mp.call('GET', '/project/%s/version' % project, token)
    if st != 200:
        mp.note('error', 'versions of %s: HTTP %s %s' % (project, st, versions))
        return 1
    doomed = [v for v in versions if v.get('loaders') == [loader]]
    if not doomed:
        print('no %s-only versions in %s' % (loader, project))
        return 0
    gone, failed = [], 0
    for v in doomed:
        st, res = mp.call('DELETE', '/version/' + v['id'], token)
        if st in (200, 204):
            gone.append(v['version_number'])
        else:
            failed += 1
            mp.note('error', 'deleting %s: HTTP %s %s' % (v['version_number'], st, res))
            if st in (401, 403):
                mp.note('error', 'the token was refused: it needs the "Delete versions" scope')
                break
    mp.note('notice', 'removed %d %s versions from %s: %s' % (len(gone), loader, project, ', '.join(gone)))
    return 1 if failed else 0


def list_versions(token, project):
    st, versions = mp.call('GET', '/project/%s/version' % project, token)
    if st != 200:
        mp.note('error', 'versions of %s: HTTP %s %s' % (project, st, versions))
        return 1
    rows = ['%s | %s | %s | %s | %s | %s' % (v['version_number'], ','.join(v.get('loaders', [])),
            ','.join(v.get('game_versions', [])), v.get('version_type'), v.get('date_published', '')[:16],
            ','.join(f.get('filename', '') for f in v.get('files', []))) for v in versions]
    print('\n'.join(rows))
    odd = [r for r in rows if not r.split(' | ')[0].count('+')]
    mp.note('notice', '%s has %d versions; %d not numbered <mod>+<mc>-<loader>:%s'
            % (project, len(rows), len(odd), ''.join('\n' + r for r in odd)))
    return 0


def project_id(token, project):
    st, proj = mp.call('GET', '/project/' + project, token)
    if st != 200:
        mp.note('error', 'project %s: HTTP %s %s' % (project, st, proj))
        return None
    return proj


def show(token, project):
    proj = project_id(token, project)
    if not proj:
        return 1
    mp.note('notice', 'TITLE: %s\nSUMMARY: %s\nICON: %s\nBODY: %d chars'
            % (proj['title'], proj['description'], proj.get('icon_url'), len(proj['body'])))
    body = proj['body']
    for i in range(0, len(body), 3500):
        mp.note('notice', 'BODY[%d]\n%s' % (i // 3500, body[i:i + 3500]))
    return 0


def delete_number(token, project, loader, number):
    st, versions = mp.call('GET', '/project/%s/version' % project, token)
    if st != 200:
        mp.note('error', 'versions of %s: HTTP %s %s' % (project, st, versions))
        return 1
    doomed = [v for v in versions if v['version_number'] == number and v.get('loaders') == [loader]]
    gone, failed = [], 0
    for v in doomed:
        st, res = mp.call('DELETE', '/version/' + v['id'], token)
        if st in (200, 204):
            gone.append(','.join(v.get('game_versions', [])))
        else:
            failed += 1
            mp.note('error', 'deleting %s (%s): HTTP %s %s' % (number, v['id'], st, res))
    mp.note('notice', 'deleted %d %s versions numbered %s from %s (Minecraft %s)'
            % (len(gone), loader, number, project, ' '.join(gone)))
    return 1 if failed else 0


def set_icon(token, project, png):
    proj = project_id(token, project)
    if not proj:
        return 1
    st, res = mp.call('PATCH', '/project/%s/icon?ext=png' % proj['id'], token,
                      open(png, 'rb').read(), {'Content-Type': 'image/png'})
    if st not in (200, 204):
        mp.note('error', 'icon: HTTP %s %s' % (st, res))
        return 1
    mp.note('notice', 'icon of %s set from %s' % (project, png))
    return 0


def set_text(token, project, summary_file, body_file):
    proj = project_id(token, project)
    if not proj:
        return 1
    data = {'description': open(summary_file, encoding='utf-8').read().strip(),
            'body': open(body_file, encoding='utf-8').read().strip()}
    st, res = mp.call('PATCH', '/project/' + proj['id'], token, json.dumps(data).encode(),
                      {'Content-Type': 'application/json'})
    if st not in (200, 204):
        mp.note('error', 'text: HTTP %s %s' % (st, res))
        return 1
    mp.note('notice', 'summary and description of %s replaced' % project)
    return 0


def main():
    token = os.environ.get('MODRINTH_TOKEN', '').strip()
    if not token:
        mp.note('error', 'MODRINTH_TOKEN is not set')
        return 1
    if len(sys.argv) == 4 and sys.argv[1] == 'ensure':
        return ensure(token, sys.argv[2], sys.argv[3])
    if len(sys.argv) == 4 and sys.argv[1] == 'drop-loader':
        return drop_loader(token, sys.argv[2], sys.argv[3])
    if len(sys.argv) == 3 and sys.argv[1] == 'show':
        return show(token, sys.argv[2])
    if len(sys.argv) == 5 and sys.argv[1] == 'delete-number':
        return delete_number(token, *sys.argv[2:])
    if len(sys.argv) == 4 and sys.argv[1] == 'icon':
        return set_icon(token, sys.argv[2], sys.argv[3])
    if len(sys.argv) == 5 and sys.argv[1] == 'text':
        return set_text(token, *sys.argv[2:])
    if len(sys.argv) == 3 and sys.argv[1] == 'list':
        return list_versions(token, sys.argv[2])
    sys.exit(__doc__)


if __name__ == '__main__':
    sys.exit(main())
