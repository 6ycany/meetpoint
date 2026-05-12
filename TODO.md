# TODO.md — MeetPoint

優先度：🔴 高 / 🟡 中 / 🟢 低

---

## Phase 1：MVP（集合のみ・車・純粋地点）

### セットアップ
- 🔴 [ ] Android Studio でプロジェクト新規作成（Package: `com.example.meetpoint`、minSdk 26）
- 🔴 [ ] `build.gradle.kts` に依存関係追加（Compose、Hilt、Retrofit、Maps Compose、Coroutines）
- 🔴 [ ] `local.properties` に `MAPS_API_KEY` を設定（APIキーは自分で発行）
- 🔴 [ ] `AndroidManifest.xml` に権限・APIキーメタデータ追加
- 🔴 [ ] Hilt セットアップ（`@HiltAndroidApp`、モジュール作成）

### ドメイン層
- 🔴 [ ] `Person` / `MeetCandidate` / `PlaceType` / `TravelMode` / `AppMode` モデル定義
- 🔴 [ ] `haversine()` 関数実装・単体テスト
- 🔴 [ ] `weiszfeld()` 関数実装・単体テスト
- 🔴 [ ] `CalcMeetPointUseCase` 実装（モード1・座標計算）

### データ層
- 🔴 [ ] Retrofit クライアント設定（Google APIs / Overpass API）
- 🔴 [ ] `GoogleGeocodingApi` 実装
- 🔴 [ ] `GeoRepository` 実装（Geocoding）
- 🔴 [ ] `OverpassApi` 実装（SA/PA検索クエリ）
- 🔴 [ ] `PlaceRepository` 実装（Overpass結果パース）

### UI層
- 🔴 [ ] Navigation セットアップ（NavHost：Home → Result）
- 🔴 [ ] `HomeScreen` 実装（出発地入力・モード切替・計算ボタン）
- 🔴 [ ] `PersonInputCard` コンポーネント実装
- 🔴 [ ] `HomeViewModel` 実装（入力状態管理・計算トリガー）
- 🔴 [ ] `ResultScreen` 実装（地図＋候補カード3件）
- 🔴 [ ] `MapView` コンポーネント実装（Maps Compose）
- 🔴 [ ] `CandidateCard` コンポーネント実装
- 🔴 [ ] `ResultViewModel` 実装
- 🔴 [ ] Google Maps Intent 連携（「Googleマップで開く」）

---

## Phase 2：電車モード・SA/PA候補

- 🟡 [ ] `GoogleRoutesApi` 実装（DRIVE / TRANSIT 切替）
- 🟡 [ ] Overpass API：主要駅検索クエリ実装（JR系フィルタリング）
- 🟡 [ ] 電車モード用 `CalcMeetPointUseCase` 拡張
- 🟡 [ ] HomeScreen に「SA/PAを含める」チェックボックス追加
- 🟡 [ ] 移動手段切替（車 / 電車）UI実装

---

## Phase 3：合流→目的地モード

- 🟡 [ ] `calcScore()` スコア関数実装・単体テスト
- 🟡 [ ] `CalcWaypointUseCase` 実装
- 🟡 [ ] HomeScreen に目的地入力欄追加（モード2のみ表示）
- 🟡 [ ] α/β スライダー UI実装（詳細設定折りたたみ内）
- 🟡 [ ] ResultScreen：モード2用の目的地マーカー・ルート線追加

---

## Phase 3：現在地入力

- 🟡 [ ] 位置情報権限リクエスト実装（`rememberLauncherForActivityResult`）
- 🟡 [ ] GPS取得ロジック実装（`FusedLocationProviderClient`）
- 🟡 [ ] 各入力欄に現在地ボタン追加

---

## Phase 3追加：機能1 現在地ワンタップ入力

- 🟡 [ ] `FusedLocationProviderClient` のHiltモジュール登録
- 🟡 [ ] 位置情報権限リクエスト実装（`rememberLauncherForActivityResult`）
- 🟡 [ ] GPS取得 → 逆ジオコード処理をUseCaseに実装（`GetCurrentLocationUseCase`）
- 🟡 [ ] `PersonInputCard` に現在地ボタン追加（ローディング状態対応）
- 🟡 [ ] `HomeViewModel` に現在地取得アクション追加

---

## Phase 3追加：機能2 Google Mapsナビ連携

- 🟡 [ ] `CandidateCard` に「Googleマップで開く」ボタン実装
- 🟡 [ ] `navigation:` URI Intent 発行ロジック実装（車=`mode=d`・電車=`mode=r`）
- 🟡 [ ] Google Maps未インストール時のブラウザフォールバック処理

---

## Phase 3追加：機能3 セッション履歴保存

- 🟡 [ ] Room DB セットアップ（`AppDatabase`・`SessionHistoryDao`）
- 🟡 [ ] `SessionHistory` Entityとデータモデル定義
- 🟡 [ ] MoshiによるPerson/CandidateのJSONシリアライズ実装
- 🟡 [ ] `SessionHistoryRepository` 実装（保存・取得・削除・50件上限管理）
- 🟡 [ ] `HistoryScreen` 実装（日付降順リスト・タップで復元・長押しで削除）
- 🟡 [ ] ResultScreen に「このセッションを保存」ボタン追加
- 🟡 [ ] HomeScreen 右上に履歴アイコンボタン追加・HistoryScreenへのナビゲーション
- 🟡 [ ] ラベル自動生成ロジック（`generateLabel()`）実装

---

## Phase 3追加：機能5 不公平度スコアの可視化

- 🟡 [ ] `CandidateCard` に横棒グラフ（Compose Canvas）追加
- 🟡 [ ] 棒グラフの左から伸びるアニメーション（`animateFloatAsState`・300ms）実装
- 🟡 [ ] 公平スコアバッジ（A〜Dランク）コンポーネント実装（`FairnessGradeBadge`）
- 🟡 [ ] ResultScreen 上部に3候補比較ミニグラフ追加（最公平候補をハイライト）

---

## Phase 4：追加機能（将来）

- 🟢 [ ] 到着時刻逆算（合流時刻 → 各人の出発時刻提示）
- 🟢 [ ] 渋滞・時間帯考慮モード（Routes API `departure_time` 活用）
- 🟢 [ ] ウィジェット対応
- 🟢 [ ] 多言語対応（英語）

---

## 完了タスク

（作業が進んだらここに移動）
