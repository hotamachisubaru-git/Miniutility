> Paper 26.3 安定版にネイティブ対応 / Java 25

# Miniutility — Paper 26.3

死亡地点へのワープ、経験値・ゲームモードの操作、クリーパーのブロック破壊防止、エンダーチェスト、ゴミ箱、ニックネーム、どこでも作業台をメニューから利用できます。

- 対象: **Paper 26.3 stable build 178**。依存APIは `26.3.build.178-stable` に固定しています。
- 開発・実行には **Java 25以降**、ビルドにはMavenが必要です。
- Adventure `5.2.0` と LuckPerms API `5.5` はサーバー側を利用し、SQLite JDBC `3.53.4.0` はJARに同梱します。
- LuckPermsは任意です。未導入でも、プレフィックス以外の機能を利用できます。
- 通常のPaperサーバーが対象です。Foliaには対応していません。
- [Paper 26.3の公式更新内容](https://papermc.io/news/26-3/)と[Java・依存関係の設定](https://docs.papermc.io/paper/dev/project-setup/)を参照してください。

## ビルドと検証

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot'
mvn -B -ntp clean verify
```

`JAVA_HOME`は使用するJDK 25のインストール先に合わせてください。`java -version`が25でも、Mavenの`JAVA_HOME`が古い場合はビルドできません。`mvn -version`でも確認できます。

成果物は `target/Miniutility-26.3.jar` です。検証ではGUIの画面切替、ゴミ箱、チャット入力、ニックネームとSQLite、死亡地点、クリーパー保護、コマンド、更新通知、終了処理の回帰テストを実行します。

2026-10-13にJava 25とPaper 26.3 stable build 178で実サーバーを起動し、Miniutility 26.3の有効化、3コマンドの登録、`/load`によるSQLiteデータの再読み込み、コンソールからのプレイヤー専用コマンド拒否を確認しました。`stop`によるプラグイン終了・ワールド保存・正常終了と、SQLiteの`integrity_check: ok`も確認済みです。Windows PowerShell 5.1で起動スクリプトの事前確認も通過しています。回帰テスト103件はすべて成功しました。Minecraftクライアントによる全GUI操作は未検証です。

## サーバーへの導入・起動

1. サーバーを停止し、既存のワールド・プラグイン設定をバックアップします。
2. `plugins` に `Miniutility-26.3.jar` を配置します。古いMiniutilityのJARは同時に置かないでください。
3. Java 25以降でPaper 26.3を起動します。既存の `plugins/Miniutility/config.yml` と `nickname.db` は引き続き利用できます。

今回のローカルセットアップ先は `H:\Paper-26.3` です。`paper-26.3-178.jar` に必要なファイルを展開し、`start.cmd`、`start-server.ps1`、基本設定とプラグインを配置します。`start.cmd`を実行すると、初期メモリ2GB・最大4GBで起動します。終了はコンソールの `stop` コマンドを使用してください。

初回は[Minecraft EULA](https://www.minecraft.net/eula)を確認し、同意する場合のみ `eula.txt` を `eula=true` にしてください。起動スクリプトは同意済みか確認し、未同意の場合は起動を停止します。

```powershell
# 起動前のJava・ファイル・EULA確認（サーバーは起動しません）
./scripts/start-paper-server.ps1 -ServerDirectory 'H:\Paper-26.3' -CheckOnly

# メモリ設定を変えて起動する例
./scripts/start-paper-server.ps1 -ServerDirectory 'H:\Paper-26.3' -MaximumMemoryGB 8
```

標準ポートは `25565`、`online-mode=true` です。ローカル接続先は `localhost:25565` です。LuckPermsによるプレフィックスが必要な場合は、対応するLuckPermsを別途導入してください。

## コード構成と26.3対応

全30コードファイルを役割に沿ったクラス名・ファイル名へ変更しました。パッケージと機能ごとの分割を維持しています。

| 役割 | 主なクラス |
| --- | --- |
| 起動・終了 | `MiniutilityPlugin`、`MiniutilityInitializer`、`MiniutilityServices` |
| コマンド・イベント登録 | `UtilityCommandHandlers`、`PaperCommandRegistrar`、`PluginListenerRegistrar` |
| メニュー | `UtilityMenuFactory`、`UtilityMenuActions`、`UtilityMenuKind`、`OwnedMenuInventoryHolder` |
| ボタン・ゴミ箱 | `MenuButtonSpec`、`UtilityMenuButton`、`MenuButtonItemFactory`、`TrashInventorySessions` |
| チャット入力・表示 | `PlayerChatListener`、`ChatInputRouter`、`ChatInputMode`、`NicknameChatRenderer` |
| ニックネーム・保存 | `PlayerNicknameService`、`SqliteNicknameRepository`、`LegacyNicknameImporter`、`NicknameInputValidator` |
| 死亡地点・クリーパー | `PlayerDeathLocations`、`PlayerDeathListener`、`CreeperBlockProtection`、`CreeperExplosionListener` |
| テキスト・更新通知 | `LegacyTextFormatter`、`LuckPermsPrefixProvider`、`ReleaseUpdateNotifier` |

コマンドはPaperのBasicCommandで登録し、GUIからの画面切替はクリックイベントの次tickで実行します。チャット入力のプレイヤー操作はサーバースレッドに移し、古い入力セッションを再実行しません。死亡地点はワールドUUIDで保持して再読み込みに対応し、SQLiteへの一括書込みは失敗時にロールバックします。不要な `@NonNull` は使用していません。

## バージョン更新

- `pom.xml` の `minecraft.version` が `plugin.yml` の `api-version` に反映されます。
- `paper.version` はコンパイル対象の Paper API です。
- Paper 26.3の最新安定ビルドへ追従する場合は、次のスクリプトを使用します。

```powershell
./scripts/update-paper-version.ps1 -MinecraftVersion 26.3 -Channel STABLE -UpdateProjectVersion
```

- 安定版ではプロジェクトバージョンを `26.3`、候補版では `26.3-beta.1` などとして更新します。
- `.github/workflows/paper-version-update.yml` から手動実行できます。週次でも26.3のSTABLEを確認し、差分がある場合はテストを含むMaven検証後、日本語タイトルと日本語コミットメッセージでPRを作成します。

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
