# KNOWLEDGE.md — ナレッジ・学び

> 一度ハマったことに二度とハマらないための記録。
> セッション終了時に Claude Code が自動追記する。新しいエントリは先頭に追加する（降順）。

---

## エントリの書き方

```markdown
### YYYY-MM-DD — [タイトル]

**状況**: どんな場面で発生したか
**問題**: 何が起きたか / 何でハマったか
**解決策**: どうやって解決したか
**教訓**: 次回への活かし方
```

---

<!-- 以下、新しいエントリを先頭に追加していく -->

### 2026-05-13 — Google Maps API「住所を認識できない」の真因

**状況**: GeocodingAPIを呼び出しても毎回nullが返り「住所を認識できない」エラーになる

**問題**: APIキーの制限設定に「Geocoding API」を指定しただけで、**プロジェクト側でGeocoding APIを有効化していなかった**。
APIキーの「APIの制限」とプロジェクトの「APIの有効化」は**完全に別の設定**。

**解決策**: Google Cloud Console → APIとサービス → ライブラリ → 「Geocoding API」を検索 → 「有効にする」をクリック

**教訓**:
- APIキー制限（どのAPIにキーを使えるか）とAPI有効化（プロジェクトでそのAPIを使うか）は別物
- 課金設定・プロジェクトリンクが正しくてもAPI有効化が漏れると REQUEST_DENIED になる
- 初期設定チェックリストに「各APIの有効化」を必ず入れる



### 2026-05-13 — GitHub Actions ビルド環境の構築

**状況**: Android Studio不使用・GitHub ActionsでAPKをビルドする方針でプロジェクト雛形を一から作成

**問題1**: `app/build.gradle.kts` で `java.util.Properties()` を使ったが、Gradle KTSファイルはJava標準ライブラリを自動インポートしないためビルドエラー
**解決策1**: ファイル先頭に `import java.util.Properties` を追加

**問題2**: `ic_launcher.xml`（VectorDrawable）で `<rect>` 要素を使ったがAAPTエラー
**解決策2**: `<rect>` はVectorDrawableで無効。`<path android:pathData="M0,0 L108,0 L108,108 L0,108 Z" />` に置き換える

**問題3**: AGP 8.5.0 は compileSdk 35 を正式サポートしていない（警告が出る）
**解決策3**: compileSdk / targetSdk を 34 に下げる（AGP 8.5.0のサポート範囲）

**教訓**:
- KTSファイルではJavaクラスは必ずimportが必要
- VectorDrawableで使える要素は `<path>`, `<group>`, `<clip-path>` のみ。`<rect>` は不可
- AGPとcompileSdkのバージョン対応表を確認してから設定する

**現在のビルド状況**: ビルド #3 が進行中（2026-05-13時点）。成否は次回セッションで確認する

**採用技術スタック**:
- Gradle 8.7（GitHub Actions上で直接実行）、AGP 8.5.0、Kotlin 2.0.0、KSP 2.0.0-1.0.21
- Hilt 2.51、Compose BOM 2024.06.00、Room 2.6.1、Retrofit 2.11.0
- GitHub Actions: `gradle/actions/setup-gradle@v3` + `gradle-version: '8.7'` → `gradle assembleDebug`
- Maps APIキー: GitHub Secrets (`MAPS_API_KEY`) → `local.properties` に書き出してビルド

### YYYY-MM-DD — プロジェクト開始

**状況**: このプロジェクトを新規作成
**問題**: -
**解決策**: テンプレートから初期化
**教訓**: PLAN.md への音声ダンプを最初にやることで、Claude との認識齟齬が減る
