# HANDOFF.md — MeetPoint

## ① プロジェクト概要

| 項目 | 内容 |
|---|---|
| プロジェクト名 | MeetPoint |
| 目的 | 複数人の移動距離が均等・合理的になる合流地点をAndroidで計算するアプリ |
| オーナー | Cany |
| 作成日 | 2026-05-12 |
| 最終更新 | 2026-05-12 |
| タイプ | Androidネイティブアプリ（Kotlin / Jetpack Compose） |

---

## ② 背景・課題

複数人で待ち合わせや合流をする際、感覚で「中間地点」を決めると一部の人が不公平に長距離移動する問題がある。また、どこかへ向かう途中で合流する場合（例：登山口へ向かいつつ途中で拾う）は、目的地方向への進行と各人の移動負担の均等化を同時に最適化する必要がある。

本アプリはこれを自動計算し、車・電車それぞれの移動手段に応じた現実的な候補地点（SA/PA・主要駅など）を上位3件提示する。

---

## ③ 現在の実装状況

| 機能 | 状態 |
|---|---|
| プロジェクト設計・仕様策定 | ✅ 完了 |
| Androidプロジェクト雛形 | ⬜ 未着手 |
| UI実装（Jetpack Compose） | ⬜ 未着手 |
| 地図表示（Google Maps SDK） | ⬜ 未着手 |
| 住所検索（Geocoding API） | ⬜ 未着手 |
| ルート距離計算（Routes API） | ⬜ 未着手 |
| SA/PA検索（Overpass API） | ⬜ 未着手 |
| 駅検索（Overpass API） | ⬜ 未着手 |
| モード1：集合のみ計算ロジック | ⬜ 未着手 |
| モード2：合流→目的地計算ロジック | ⬜ 未着手 |
| 結果候補3件表示 | ⬜ 未着手 |
| 機能1：現在地ワンタップ入力 | ⬜ 未着手 |
| 機能2：Google Mapsナビ連携 | ⬜ 未着手 |
| 機能3：セッション履歴保存（Room DB） | ⬜ 未着手 |
| 機能5：不公平度スコア可視化（棒グラフ・A〜Dランク） | ⬜ 未着手 |

---

## ④ ファイル構成（予定）

```
meetpoint/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/meetpoint/
│   │   │   ├── ui/
│   │   │   │   ├── screen/
│   │   │   │   │   ├── HomeScreen.kt          # モード選択・出発地入力
│   │   │   │   │   ├── ResultScreen.kt        # 候補3件表示・地図
│   │   │   │   │   └── SettingsScreen.kt      # 重み設定など
│   │   │   │   ├── component/
│   │   │   │   │   ├── PersonInputCard.kt     # 出発地入力カード
│   │   │   │   │   ├── CandidateCard.kt       # 結果候補カード
│   │   │   │   │   └── MapView.kt             # Google Maps Compose
│   │   │   │   └── theme/
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   │   ├── Person.kt              # 出発地点モデル
│   │   │   │   │   ├── MeetCandidate.kt       # 合流候補モデル
│   │   │   │   │   └── TravelMode.kt          # 移動手段enum
│   │   │   │   └── usecase/
│   │   │   │       ├── CalcMeetPointUseCase.kt    # 集合のみモード計算
│   │   │   │       └── CalcWaypointUseCase.kt     # 合流→目的地モード計算
│   │   │   ├── data/
│   │   │   │   ├── remote/
│   │   │   │   │   ├── GoogleGeocodingApi.kt
│   │   │   │   │   ├── GoogleRoutesApi.kt
│   │   │   │   │   └── OverpassApi.kt         # SA/PA・駅検索
│   │   │   │   └── repository/
│   │   │   │       ├── GeoRepository.kt
│   │   │   │       └── PlaceRepository.kt
│   │   │   └── MainActivity.kt
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── HANDOFF.md
├── PLAN.md
├── SPEC.md
├── TODO.md
└── KNOWLEDGE.md
```

---

## ⑤ 技術スタック・ツール

| カテゴリ | 採用技術 | 備考 |
|---|---|---|
| 言語 | Kotlin | |
| UI | Jetpack Compose | Material3 |
| アーキテクチャ | MVVM + Clean Architecture | |
| DI | Hilt | |
| 非同期 | Coroutines + Flow | |
| 地図表示 | Google Maps SDK for Android (Compose) | 無料 |
| 住所→座標 | Google Geocoding API | $5/1,000件 |
| ルート距離 | Google Routes API | $10/1,000件 |
| SA/PA・駅検索 | Overpass API (OSM) | 完全無料 |
| HTTP通信 | Retrofit2 + OkHttp | |
| JSONパース | Moshi + moshi-kotlin | |
| ローカルDB | Room 2.6.1 | セッション履歴保存（機能3） |
| ナビ連携 | Google Maps Intent | |

### APIキー管理
- `local.properties` に `MAPS_API_KEY=xxx` を記載
- `BuildConfig` 経由でアクセス
- `.gitignore` に `local.properties` を追加すること

---

## ⑥ 既知の課題・注意事項

- **Overpass APIの負荷制限**：リクエスト過多でレート制限がかかる場合がある。検索範囲を絞る（半径50km以内など）こと
- **SA/PAデータの精度**：OSMのSA/PAタグ（`amenity=service_area` 等）は網羅性が完全ではない。`highway=services` との併用で補完する
- **Routes APIのコスト**：1セッションで人数×候補数分のリクエストが発生するため、直線距離でフィルタリングしてからAPIを叩く2段階方式を推奨
- **合流→目的地モードの最適化**：スコア関数 = `α × (目的地方向への進捗率) + β × (移動距離の標準偏差の逆数)` で計算。α・βはデフォルト0.5ずつ、ユーザーがスライダーで調整可能

---

## ⑦ Claude Code 起動手順

```bash
cd C:\Users\npcmi\claude-projects\meetpoint
claude
```

### 開始時のコピペ用プロンプト

```
HANDOFF.mdとKNOWLEDGE.mdを読んでプロジェクトの現状を把握してください。
その後、TODO.mdの最優先タスクから実装を開始してください。
不明点があればその都度確認してから進めてください。
```

---

## ⑧ 次タスク（優先順）

1. Androidプロジェクト雛形作成（Hilt・Compose・Maps導入済み）
2. `Person` / `MeetCandidate` / `TravelMode` モデル定義
3. `OverpassApi` でSA/PA・駅検索の実装とテスト
4. `CalcMeetPointUseCase` の実装（ワイツェンベック重心法）
5. `CalcWaypointUseCase` の実装（スコア関数による候補ランキング）
6. HomeScreen UI実装（出発地入力・モード切替）
7. ResultScreen UI実装（候補3件カード＋地図）
8. Google Maps Intentによるナビ連携

---

## ⑨ 参考資料

- [Google Maps SDK for Android (Compose)](https://developers.google.com/maps/documentation/android-sdk/maps-compose)
- [Google Geocoding API](https://developers.google.com/maps/documentation/geocoding)
- [Google Routes API](https://developers.google.com/maps/documentation/routes)
- [Overpass API ドキュメント](https://wiki.openstreetmap.org/wiki/Overpass_API)
- [OSM highway=services タグ](https://wiki.openstreetmap.org/wiki/Tag:highway%3Dservices)
- [Overpass Turbo（クエリテスト）](https://overpass-turbo.eu/)

---

## 推奨 CLAUDE.md 内容

```markdown
# CLAUDE.md

## 必須ルール
1. 作業開始前に必ず HANDOFF.md と KNOWLEDGE.md を読むこと
2. 中断宣言時はセッションサマリーを KNOWLEDGE.md に追記すること
3. 次セッション再開前に最新の KNOWLEDGE.md エントリを必ず読むこと
```

---

このファイルは作業のたびに更新すること。
