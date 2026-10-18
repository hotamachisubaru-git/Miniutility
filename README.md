# Miniutility — Fabric

このブランチは Fabric MOD です。Paper/Spigot 版は [master](https://github.com/hotamachisubaru-git/Miniutility/tree/master)、Forge 版は [forge](https://github.com/hotamachisubaru-git/Miniutility/tree/forge) ブランチで管理します。

## 対応版

Minecraft 1.20.1〜26.3 のうち Fabric API の配布が存在する23版を、`versions.json` に固定しています。Minecraft の版ごとに別の JAR をビルドします。異なる版の JAR は流用しないでください。

[CI](https://github.com/hotamachisubaru-git/Miniutility/actions/runs/39092250906)で23版すべてのビルドと共通テストが成功しています。実サーバー起動・ゲーム内の全GUI操作は未検証です。

## 導入

同じ Minecraft 版の Fabric サーバーの `mods` に `Miniutility-fabric-<Minecraft版>-26.3.1.jar` を配置して再起動します。GUI はバニラの画面を使用し、Miniutility のクライアント導入は不要です。対応版の Fabric API は必須です。LuckPerms は任意です。

`/menu` で死亡地点ワープ、経験値レベル増減、ゲームモード切替、クリーパーのブロック破壊防止、エンダーチェスト、確認付きゴミ箱、ニックネーム変更、作業台を開けます。元のプラグインと同様、メニューの機能は一般プレイヤーも利用できます。

- `/load`: 保存データを再読み込み（権限レベル2以上）。
- `/prefixtoggle [on|off]`: LuckPerms プレフィックス表示の切替。
- 入力待ちのチャットは公開されません。`cancel` または `キャンセル` で中止できます。
- ゴミ箱は「はい」で確定したときだけ削除します。閉じた場合は返却し、所持品が満杯なら足元へドロップします。
- 死亡地点はサーバー起動中のみ保持し、ワープ先の危険性の判定は行いません。
- クリーパー対策はブロック破壊だけを抑制し、エンティティへのダメージは残します。

## 保存・チャット

ワールドの `miniutility/profiles.properties` にニックネーム・プレフィックス設定・クリーパー設定を UTF-8 で保存します。書込み失敗時は以前のメモリ上の状態を維持します。Paper/Spigot の SQLite ファイルは別形式のため、自動移行はしません。

通常チャットはMinecraft標準の配信を利用し、チャットの表示名とタブリストにニックネームを反映します。入力待ちのメッセージだけを取り込み、通常の署名付きチャットを維持します。

## 開発

共通ロジックは `common/`、Minecraft のメニューと機能は `mod-common/` に配置します。`versions/<版>/` はFabric Loom のビルド設定、イベント・Mixin接続、版間差分だけを保持します。

```sh
cd versions/1.20.1
./gradlew build
```

Windows では `gradlew.bat build` を使用します。1.20.1〜1.20.4 は Java 17、1.20.5〜1.21.11 は Java 21、26.1以降は Java 25 が対象です。Fabric Loom の起動には Java 25 も用意してください。Gradle は対象版の Java ツールチェーンを使用します。

GitHub Actions で各対象版のコンパイルと共通テストを実行し、版ごとに JAR を保存します。
