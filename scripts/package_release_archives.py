"""Repackage the reviewed 26.3.1 JARs without rebuilding or modifying them."""
import hashlib, io, json, pathlib, re, zipfile


def build(work_dir):
    ROOT = pathlib.Path(work_dir).resolve()
    SOURCE = ROOT / 'source'
    ASSETS = ROOT / 'original-assets'
    OUT = ROOT / 'output'
    OUT.mkdir(exist_ok=True)
    release = json.loads((ROOT / 'release-original.json').read_text())
    assets = {a['name']: a for a in release['assets']}
    original_sums = {}
    for line in (ASSETS / 'SHA256SUMS.txt').read_text().splitlines():
        digest, name = line.split(maxsplit=1)
        original_sums[name.lstrip('*')] = digest
    assert len(original_sums) == 67, len(original_sums)
    report = []
    seen = set()

    def sha(data):
        return hashlib.sha256(data).hexdigest()

    def add(z, name, data):
        info = zipfile.ZipInfo(name, (2026, 10, 18, 0, 0, 0))
        info.compress_type = zipfile.ZIP_DEFLATED
        info.external_attr = 0o100644 << 16
        z.writestr(info, data, compress_type=zipfile.ZIP_DEFLATED, compresslevel=6)

    for platform, branch, expected_count, dest in [('paper-spigot', 'master', 23, 'plugins'), ('forge', 'forge', 21, 'mods'), ('fabric', 'fabric', 23, 'mods')]:
        versions = json.loads((SOURCE / branch / 'versions.json').read_text())
        versions.sort(key=lambda v: tuple(map(int, v['minecraft'].split('.'))))
        assert len(versions) == expected_count
        files = []
        for version in versions:
            mc = version['minecraft']
            name = f'Miniutility-{platform}-{mc}-26.3.1.jar'
            item = assets[name]
            payload = (ASSETS / name).read_bytes()
            digest = sha(payload)
            assert len(payload) == item['size'], name
            assert 'sha256:' + digest == item['digest'], name
            assert original_sums[name] == digest, name
            with zipfile.ZipFile(io.BytesIO(payload)) as jar:
                assert jar.testzip() is None, name
                if platform == 'fabric':
                    metadata = json.loads(jar.read('fabric.mod.json'))
                    assert metadata['id'] == 'miniutility' and metadata['version'] == '26.3.1', name
                    assert metadata['depends']['minecraft'] == mc, name
                elif platform == 'forge':
                    metadata = jar.read('META-INF/mods.toml').decode()
                    assert re.search(r'modId\s*=\s*"miniutility"', metadata), name
                    assert re.search(r'version\s*=\s*"26\.3\.1"', metadata), name
                else:
                    metadata = jar.read('plugin.yml').decode()
                    assert re.search(r'^version:\s*[\'\"]?26\.3\.1', metadata, re.M), name
            seen.add(name)
            files.append((f'jars/{name}', payload, digest))

        label = 'Paper / Spigot' if platform == 'paper-spigot' else platform.capitalize()
        readme = f'''# Miniutility 26.3.1 — {label} ZIP配布版

## 最初にお読みください

このZIPには、Minecraftのバージョンごとに分かれた{expected_count}個のJARが入っています。
**解凍後、お使いのMinecraft版に一致するJARを1個だけ導入してください。全JARの一括導入はしないでください。**

1. ZIPを解凍し、`jars`フォルダーを開きます。
2. `Miniutility-{platform}-<Minecraft版>-26.3.1.jar`を1個選びます。
3. サーバーを停止し、古いMiniutilityのJARを外します。
4. 選んだJARをサーバーの`{dest}`フォルダーへ配置して再起動します。

ZIP自体を`{dest}`へ置いても動作しません。Fabric版は、対応するFabric APIの別途導入も必要です。

## 同梱物と検証

- `jars/`: このプラットフォーム用の配布JAR
- `SHA256SUMS.txt`: 同梱JARのSHA-256一覧（パスはこのREADMEのあるフォルダー基準）
- `versions.json`: Minecraft・API・Javaのビルド対象一覧
- `LICENSE`: MITライセンス

公開済み26.3.1のJARをバイト単位で変更せず再梱包しています。GitHub公開時のSHA-256と照合済みです。
実サーバー起動・Minecraftクライアントによる全GUI操作の追加検証は行っていません。
配布元: https://github.com/hotamachisubaru-git/Miniutility/releases/tag/26.3.1

---
'''
        readme += (SOURCE / branch / 'README.md').read_text()
        if platform == 'paper-spigot':
            readme += '\n## ビルド対象API一覧\n\n| Minecraft | Spigot API（Paper/Spigot共通ビルド） | Java |\n| --- | --- | --- |\n'
            for v in versions:
                readme += f"| {v['minecraft']} | `{v['spigot']}` | {v['java']} |\n"
        zpath = OUT / f'Miniutility-{platform}-26.3.1.zip'
        with zipfile.ZipFile(zpath, 'w') as z:
            add(z, 'README.md', readme.encode())
            add(z, 'LICENSE', (SOURCE / 'LICENSE').read_bytes())
            add(z, 'versions.json', (SOURCE / branch / 'versions.json').read_bytes())
            add(z, 'SHA256SUMS.txt', ''.join(f'{digest}  {name}\n' for name, _, digest in files).encode())
            for name, payload, _ in files:
                add(z, name, payload)
        with zipfile.ZipFile(zpath) as z:
            assert z.testzip() is None
            names = z.namelist()
            assert len(names) == len(set(names)) == expected_count + 4
            assert sum(n.endswith('.jar') for n in names) == expected_count
            for name, _, digest in files:
                assert sha(z.read(name)) == digest
        row = {'platform': platform, 'file': zpath.name, 'jars': expected_count, 'bytes': zpath.stat().st_size, 'sha256': sha(zpath.read_bytes())}
        report.append(row)
        print(json.dumps(row), flush=True)

    assert len(seen) == 67
    assert seen == {name for name in assets if name.endswith('.jar')}
    (OUT / 'SHA256SUMS.txt').write_text(''.join(f"{r['sha256']}  {r['file']}\n" for r in report))
    (ROOT / 'validation.json').write_text(json.dumps({'verified_jars': 67, 'outputs': report}, indent=2))
    body = release['body']
    body = body.replace('**お使いのプラットフォームとMinecraftの版が一致するJARを1つ選んでください。**', '**お使いのプラットフォームのZIPを解凍し、`jars`からMinecraftの版が一致するJARを1つだけ選んでください。**')
    for platform in ('paper-spigot', 'forge', 'fabric'):
        body = body.replace(f'`Miniutility-{platform}-<Minecraft版>-26.3.1.jar`', f'`Miniutility-{platform}-26.3.1.zip`')
    body = body.replace('## 導入・更新\n', '## 導入・更新\n\nZIP内のREADMEに導入手順とAPI・ローダーの対応表を同梱しています。ZIPや全JARをまとめてサーバーへ入れず、対象版のJARを1個だけ使用してください。\n')
    body = body.replace('`SHA256SUMS.txt` で各JARのハッシュ値を確認できます。', 'リリース直下の `SHA256SUMS.txt` は3つのZIP、各ZIP内の `SHA256SUMS.txt` は同梱JARのハッシュ値です。公開済みの全67個のJARを変更せずZIPへ再梱包し、GitHub公開時のSHA-256との一致とZIPの展開整合性を確認しています。')
    (ROOT / 'release-notes-proposed.md').write_text(body)
    print('Verified: all 67 JARs match both GitHub digests and original SHA256SUMS.txt; all 3 ZIPs pass CRC and extracted SHA-256 checks.', flush=True)
    return report
