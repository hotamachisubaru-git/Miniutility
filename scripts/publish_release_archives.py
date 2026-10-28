"""Verify, package, and optionally publish the reviewed 26.3.1 release migration.

Only the exact asset IDs and hashes in the committed plan may be removed.
The existing release and tags are preserved. A partially completed run can resume.
"""
import argparse
import base64
import hashlib
import json
import pathlib
import subprocess
import tempfile
import zipfile
from concurrent.futures import ThreadPoolExecutor

from package_release_archives import build

ROOT = pathlib.Path(__file__).resolve().parents[1]
PLAN_FILE = ROOT / '.github/release-packaging/26.3.1.json'
NOTES_FILE = ROOT / '.github/release-packaging/26.3.1-notes.md'
PLATFORMS = ('paper-spigot', 'forge', 'fabric')


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def gh(*args):
    return subprocess.check_output(['gh', *map(str, args)])


def api(repository, endpoint, *args):
    return json.loads(gh('api', f'repos/{repository}/{endpoint}', *args))


def digest(path):
    with path.open('rb') as f:
        return 'sha256:' + hashlib.file_digest(f, 'sha256').hexdigest()


def download(url, path):
    # Public release URLs need no credential forwarded to their download hosts.
    subprocess.run(['curl', '--fail', '--silent', '--show-error', '--location',
                    '--retry', '3', '--connect-timeout', '20', '--max-time', '180',
                    '--output', str(path), url], check=True)


def download_asset(repository, asset, path):
    # An asset ID identifies the uploaded object even when a filename is reused.
    # The public filename URL may still serve the previous object's cached bytes.
    with path.open('wb') as output:
        subprocess.run(['gh', 'api', f"repos/{repository}/releases/assets/{asset['id']}",
                        '--header', 'Accept: application/octet-stream'], stdout=output, check=True)


def release_snapshot(plan):
    release = api(plan['repository'], f"releases/{plan['release_id']}")
    require(release['tag_name'] == plan['tag'] == '26.3.1', 'Unexpected release tag')
    require(not release['draft'] and not release.get('immutable'), 'Release cannot be edited')
    return release


def check_legacy_assets(plan, release):
    expected = {a['name']: a for a in plan['assets']}
    require(release['body'] in (plan['original_body'], NOTES_FILE.read_text()),
            'Release notes changed outside this migration; review them first')
    for item in release['assets']:
        name = item['name']
        if name in expected and name.endswith('.jar'):
            old = expected[name]
            require(all(item[k] == old[k] for k in ('id', 'size', 'digest')),
                    f'Original asset changed: {name}')


def prepare(plan, work):
    release = release_snapshot(plan)
    check_legacy_assets(plan, release)
    live = {a['name']: a for a in release['assets']}
    original = work / 'original-assets'
    original.mkdir(parents=True, exist_ok=True)
    source = work / 'source'
    for branch, ref in plan['source_refs'].items():
        (source / branch).mkdir(parents=True, exist_ok=True)
        for name in ('README.md', 'versions.json'):
            obj = api(plan['repository'], f'contents/{name}?ref={ref}')
            (source / branch / name).write_bytes(base64.b64decode(obj['content']))
    obj = api(plan['repository'], f"contents/LICENSE?ref={plan['source_refs']['master']}")
    (source / 'LICENSE').write_bytes(base64.b64decode(obj['content']))

    expected_jars = [a for a in plan['assets'] if a['name'].endswith('.jar')]
    require(len(expected_jars) == 67, 'Plan must contain exactly 67 JARs')
    # Resume after old JARs were already removed: recover only expected members.
    for platform in PLATFORMS:
        missing = [a for a in expected_jars if a['name'].startswith(f'Miniutility-{platform}-')
                   and a['name'] not in live]
        if not missing:
            continue
        archive_name = f'Miniutility-{platform}-{plan["tag"]}.zip'
        require(archive_name in live, f'Neither original JAR nor archive exists: {archive_name}')
        archive = work / archive_name
        download(live[archive_name]['browser_download_url'], archive)
        with zipfile.ZipFile(archive) as z:
            for item in missing:
                (original / item['name']).write_bytes(z.read('jars/' + item['name']))

    def obtain(item):
        path = original / item['name']
        if item['name'] in live:
            download(live[item['name']]['browser_download_url'], path)
        require(path.stat().st_size == item['size'] and digest(path) == item['digest'],
                'Original JAR checksum mismatch: ' + item['name'])

    with ThreadPoolExecutor(max_workers=4) as pool:
        list(pool.map(obtain, expected_jars))
    # The immutable plan retains the original individual-JAR checksum set.
    (original / 'SHA256SUMS.txt').write_text(''.join(
        f"{a['digest'].removeprefix('sha256:')}  {a['name']}\n" for a in expected_jars))
    (work / 'release-original.json').write_text(json.dumps({
        'assets': plan['assets'], 'body': plan['original_body']}))
    return build(work)


def verify_uploaded(plan, output, names, verification):
    live = {a['name']: a for a in release_snapshot(plan)['assets']}
    for name in names:
        require(name in live, 'Missing uploaded asset: ' + name)
        local = output / name
        target = verification / name
        require(live[name]['size'] == local.stat().st_size, 'Uploaded size differs: ' + name)
        download_asset(plan['repository'], live[name], target)
        require(digest(local) == digest(target), 'Uploaded checksum differs: ' + name)


def publish(plan, output):
    release = release_snapshot(plan)
    check_legacy_assets(plan, release)
    live = {a['name']: a for a in release['assets']}
    archives = [f'Miniutility-{p}-{plan["tag"]}.zip' for p in PLATFORMS]
    with tempfile.TemporaryDirectory() as directory:
        verification = pathlib.Path(directory)
        # Never replace an existing archive with different bytes automatically.
        for name in archives:
            if name not in live:
                gh('release', 'upload', plan['tag'], output / name, '--repo', plan['repository'])
        verify_uploaded(plan, output, archives, verification)

        # Guard against concurrent edits before changing notes or checksums.
        release = release_snapshot(plan)
        check_legacy_assets(plan, release)
        checksum = next((a for a in release['assets'] if a['name'] == 'SHA256SUMS.txt'), None)
        local_sum = output / 'SHA256SUMS.txt'
        checksum_matches = False
        if checksum:
            download_asset(plan['repository'], checksum, verification / 'SHA256SUMS.txt')
            original_sum = next(a for a in plan['assets'] if a['name'] == 'SHA256SUMS.txt')
            require(digest(verification / 'SHA256SUMS.txt') in
                    (original_sum['digest'], digest(local_sum)), 'Checksum file changed outside migration')
            checksum_matches = digest(verification / 'SHA256SUMS.txt') == digest(local_sum)
        if not checksum_matches:
            gh('release', 'upload', plan['tag'], local_sum, '--repo', plan['repository'], '--clobber')
        verify_uploaded(plan, output, ['SHA256SUMS.txt'], verification)
        gh('release', 'edit', plan['tag'], '--repo', plan['repository'], '--notes-file', NOTES_FILE)

        # Remove exact original IDs only after all replacement bytes are verified.
        release = release_snapshot(plan)
        check_legacy_assets(plan, release)
        old_names = {a['name'] for a in plan['assets'] if a['name'].endswith('.jar')}
        for item in release['assets']:
            if item['name'] in old_names:
                gh('api', '--method', 'DELETE', f"repos/{plan['repository']}/releases/assets/{item['id']}")

    final = release_snapshot(plan)
    names = {a['name'] for a in final['assets']}
    require(not names.intersection(old_names), 'Individual JAR cleanup incomplete')
    require(set(archives + ['SHA256SUMS.txt']).issubset(names), 'Final assets missing')
    require(final['body'] == NOTES_FILE.read_text(), 'Final release notes differ')
    print('Published and verified:', final['html_url'], flush=True)
    print('Release author preserved:', final['author']['login'], flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--publish', action='store_true', help='Upload verified ZIPs and replace individual JARs')
    parser.add_argument('--work-dir', type=pathlib.Path, default=ROOT / '.release-packaging')
    args = parser.parse_args()
    plan = json.loads(PLAN_FILE.read_text())
    args.work_dir.mkdir(parents=True, exist_ok=True)
    prepare(plan, args.work_dir)
    if args.publish:
        publish(plan, args.work_dir / 'output')


if __name__ == '__main__':
    main()
