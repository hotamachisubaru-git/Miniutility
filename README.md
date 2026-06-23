> ネイティブ対応
> Paper 26.2 Alpha版

# こちらは Paper 26.2 向けのブランチです

- 開発依存は `io.papermc.paper:paper-api` を使用し、正確な版は `pom.xml` の `paper.version` で管理します。
- Adventure は `net.kyori:adventure-bom:5.1.1` で安定版を固定します。
- Paper 26.2 Alpha版のバージョン命名は `26.2.build.N-alpha` 形式です。
- 取得先は従来のスナップショットリポジトリではなく `https://repo.papermc.io/repository/maven-public/` です。
- 開発とビルドには Java 25 が必要です。
- 通常の Paper サーバーを対象とし、Foliaには対応しません。

## バージョン更新

- `pom.xml` の `minecraft.version` が `plugin.yml` の `api-version` に反映されます。
- `paper.version` はコンパイル対象の Paper API です。
- Paper 26.2 のAlphaビルドへ追従する場合は、次のスクリプトを使用します。

```powershell
./scripts/update-paper-version.ps1 -MinecraftVersion 26.2 -Channel ALPHA -UpdateProjectVersion
```

- `.github/workflows/paper-version-update.yml` から手動実行できます。週次でも同じ処理を実行し、差分がある場合は日本語タイトルと日本語コミットメッセージで PR を作成します。

## ブランチ運用

- Forge向け変更は `forge` ブランチにコミット・プッシュする。
- Paper向け変更は `master` または `main` ブランチにコミット・プッシュする。

注意: LunaChat が導入されているサーバーでは、以下の設定が必要です。

```yaml
# -------------------- 通常チャット設定 --------------------

# 通常チャット（非チャンネルチャット）の装飾を、LunaChatから行うかどうか。
enableNormalChatMessageFormat: true

# チャット装飾のフォーマット設定。
# フォーマット設定には、下記のキーワードが使用できます。
# %displayname : 発言者表示名
# %player   : 発言者ID
# %world    : 発言したワールド名（spigot側に導入したときに有効です。MultiVerseが導入されている場合は、ワールドの表示名を取得して使用します。）
# %server   : 発言者の接続サーバー名（BungeeCord側に導入したときに有効です。）
# %prefix   : プレフィックス（Vaultとプレフィックス/サフィックスプラグインが導入されている場合に置き換えられます）
# %suffix   : サフィックス（Vaultとプレフィックス/サフィックスプラグインが導入されている場合に置き換えられます）
# %date     : 日付
# %time     : 時刻
# %msg      : 発言内容（Japanize変換された場合は、Japanize結果を含みます。）
normalChatMessageFormat: '&f%prefix%displayname%suffix&a:&f %msg'

# 通常チャット（非チャンネルチャット）で、カラーコード（&aや&bなど）を
# 使用可能にするかどうか。falseに設定すると、カラーコードは変換されません。
enableNormalChatColorCode: false

# 通常チャット（非チャンネルチャット）をクリック可能にするかどうか。
enableNormalChatClickable: false

# 通常チャット（非チャンネルチャット）をコンソールにログ出力するかどうか。
displayNormalChatOnConsole: true
```

## コマンド

- `/menu` メニューを開きます。
- `/prefixtoggle [on|off]` プレフィックス結合の有効無効を切り替えます。
- `/load` データベースからニックネームを再読み込みします。

## 機能一覧

- 死亡地点にワープ
- 経験値制御
- ゲームモード切り替え
- クリーパーのブロック破壊防止
- エンダーチェスト
- ゴミ箱
- ニックネーム変更
- どこでも作業台
