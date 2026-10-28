# Miniutility — Paper / Spigot

[**JARのダウンロード（GitHub Releases）**](https://github.com/hotamachisubaru-git/Miniutility/releases/tag/26.3.1)

配布はプラットフォームごとのZIPにまとめています。ZIPを解凍し、`jars`から**お使いのMinecraft版に一致するJARを1個だけ**選んでください。

| ZIPファイル | 同梱JAR |
| --- | --- |
| `Miniutility-paper-spigot-26.3.1.zip` | Paper / Spigot用・23版 |
| `Miniutility-forge-26.3.1.zip` | Forge用・21版（1.20.5・1.21.2を除く） |
| `Miniutility-fabric-26.3.1.zip` | Fabric用・23版 |

ZIPには導入説明・API対応表・ライセンス・JARのチェックサムを同梱しています。リリース直下の `SHA256SUMS.txt` は3つのZIP用です。ZIP自体や全JARをまとめてサーバーへ入れないでください。

死亡地点へのワープ、経験値・ゲームモードの操作、クリーパーのブロック破壊防止、エンダーチェスト、確認付きゴミ箱、ニックネーム、どこでも作業台をメニューから利用できます。

| ブランチ | 実装 | 導入先 |
| --- | --- | --- |
| `master` | Paper / Spigot プラグイン | `plugins` |
| `forge` | Forge MOD | `mods` |
| `fabric` | Fabric MOD | `mods` |

Forge・Fabric の API・ローダーバージョンは、[Forge の対応表](https://github.com/hotamachisubaru-git/Miniutility/blob/forge/README.md#forge-api-バージョン)・[Fabric の対応表](https://github.com/hotamachisubaru-git/Miniutility/blob/fabric/README.md#fabric-apiloader-バージョン)を参照してください。

## 対応版と導入

Minecraft 1.20.1〜26.3 の23版を `versions.json` に定義しています。対象版の Paper または Spigot と、**同じ Minecraft 版用の JAR** を使用してください。バージョン間の Bukkit API のバイナリ差異に合わせ、版ごとにコンパイルします。Folia は対象外です。

1. サーバーを停止し、古い Miniutility JAR と入れ替えます。
2. `plugins` に `Miniutility-paper-spigot-<Minecraft版>-26.3.1.jar` を配置します。
3. サーバーを再起動します。既存の `plugins/Miniutility/config.yml` と `nickname.db` を引き続き利用できます。

LuckPerms は任意です。Adventure・Gson は他プラグインと衝突しない名前空間へ移して同梱し、SQLite JDBC も JAR に含めます。

## コマンド

- `/menu`: メニューを開きます。各機能は一般プレイヤーも利用できます。
- `/prefixtoggle [on|off]`: LuckPerms プレフィックスの表示を切り替えます。
- `/load`: ニックネームをDBから再読み込みします（`miniutility.load`、標準ではOP）。

チャット入力はサーバースレッドで処理します。画面切替はクリックの次tickへ送り、ゴミ箱の内容は削除確認時だけ破棄します。死亡地点は再起動で失われます。LunaChatなど別のチャット整形プラグインと併用する場合は表示の重複を確認してください。

## ビルド

開発には Maven と JDK 25 を使用します。出力の Java バージョンは対象Minecraft版に合わせます。1.20.1〜1.20.4は17、1.20.5〜1.21.11は21、26.1以降は25です。

```sh
# 初期設定: 1.20.1
mvn -B -ntp verify

# 26.3
mvn -B -ntp verify -Dminecraft.version=26.3 -Dspigot.version=26.3-R0.1-SNAPSHOT -Djava.version=25 -Dbukkit.api.version=26.3
```

成果物は `target/<Minecraft版>/Miniutility-paper-spigot-<Minecraft版>-26.3.1.jar` です。`original-` 付きJARは使用しません。GitHub Actions は全対象版のビルドとテストを実行し、成果物を版ごとに保存します。

### VS Code で共有コードが見つからない場合

`common/src/main/java` と `common/src/test/java` は、`pom.xml` の `build-helper-maven-plugin` でソースとして登録します。VS Code では `pom.xml` があるリポジトリのルートを開いてください。

`NicknameRules cannot be resolved` と `Project configuration is not up-to-date with pom.xml` が表示された場合は、コマンドパレット（`Ctrl+Shift+P`）から **Java: Reload Projects** を実行し、読み込み完了を待ちます。解消しない場合は **Java: Clean Java Language Server Workspace** でキャッシュを再構築してください。共有クラスのコピーや手動のソースパス追加は不要です。

今後の `pom.xml` の変更は、リポジトリの `java.configuration.updateBuildConfiguration: automatic` 設定で自動反映します。コマンドの詳細は [Java 拡張の公式ドキュメント](https://github.com/redhat-developer/vscode-java#available-commands) を参照してください。

## 構成と検証

Spigot APIを基準に実装し、Paper の非同期テレポートは利用可能な場合だけ呼び出します。コマンドは `plugin.yml` と Bukkit の標準登録を使用します。`common/` は MOD版と共有する入力検証・保存・セッションのロジックです。

既存の103件に共有ロジック7件を加えた110件のテストを実行します。[CI](https://github.com/hotamachisubaru-git/Miniutility/actions/workflows/build.yml?query=branch%3Amaster)で対象23版すべてのビルドとテストが成功しています。今回の変更について、Minecraftクライアントによる全GUI操作は未検証です。

対象版を追加する場合は `versions.json` のMinecraft版・Spigot API・Java・Bukkit api-versionを更新し、Actionsの成功を確認してください。旧Paper専用の自動バージョン更新は版別ビルドへ置き換えました。

### 26.3.1 のZIP再梱包

`Repackage 26.3.1 as ZIP` ワークフローは、公開済みの67個のJARを変更せず3つのZIPへまとめます。対象アセットのID・サイズ・SHA-256と、ドキュメント取得元のコミットは `.github/release-packaging/26.3.1.json` に固定しています。

新しいZIPとチェックサムをアップロードし、再ダウンロードして照合した後、説明文を更新して個別JARを削除します。検証失敗や想定外の変更があれば停止します。途中で失敗した場合は、Actionsから同じワークフローを再実行できます。既存リリースとタグは維持します。
