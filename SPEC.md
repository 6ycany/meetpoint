# SPEC.md — MeetPoint

## 画面仕様

### HomeScreen

| 要素 | 仕様 |
|---|---|
| モード切替 | SegmentedButton：「集合のみ」「合流→目的地」 |
| 移動手段切替 | SegmentedButton：「車」「電車」 |
| 車サブオプション | CheckBox：「SA/PAを候補に含める」（デフォルトON） |
| 出発地入力欄 | 最大8人。各行に「人物アイコン＋住所テキストフィールド＋現在地ボタン＋削除ボタン」 |
| 人物追加ボタン | 「+ 出発地を追加」（8人上限で非活性） |
| 目的地入力欄 | モード2のみ表示 |
| 重み調整 | モード2のみ。「詳細設定」折りたたみ内にα/βスライダー（0.0〜1.0、連動してα+β=1に正規化） |
| 計算ボタン | 出発地2件以上入力済みで活性。「最適地点を計算する」 |

### ResultScreen

| 要素 | 仕様 |
|---|---|
| 地図 | 画面上半分。全出発地点（色付きマーカー）＋候補3件（番号付きマーカー）を表示 |
| 候補カード | 画面下半分をVerticalPager or LazyColumn。候補1〜3件をスワイプ or スクロールで閲覧 |
| 候補カード内容 | ① ランク番号（1位/2位/3位）② 地点名・住所 ③ 各人の移動距離リスト ④ 公平スコア（標準偏差） ⑤ 「Googleマップで開く」ボタン |
| 地図フォーカス | 候補カードタップ/スワイプで対応マーカーにカメラ移動 |
| 戻るボタン | HomeScreenへ戻る（入力内容は保持） |

---

## データモデル

```kotlin
data class Person(
    val id: String = UUID.randomUUID().toString(),
    val label: String,          // 「地点1」など
    val address: String,
    val latLng: LatLng,
    val isCurrentLocation: Boolean = false
)

data class MeetCandidate(
    val rank: Int,              // 1〜3
    val name: String,           // 地点名（SA名・駅名・住所など）
    val latLng: LatLng,
    val placeType: PlaceType,   // COORDINATE / SERVICE_AREA / STATION
    val distancesKm: List<Double>,  // 各Personからの距離（順序対応）
    val distanceStdDev: Double,
    val progressScore: Double,  // モード2のみ使用（0.0〜1.0）
    val totalScore: Double
)

enum class PlaceType { COORDINATE, SERVICE_AREA, STATION }

enum class TravelMode { CAR, TRAIN }

enum class AppMode { MEET_ONLY, MEET_AND_GO }
```

---

## API仕様

### Google Geocoding API

```
GET https://maps.googleapis.com/maps/api/geocode/json
  ?address={住所}
  &language=ja
  &region=jp
  &key={API_KEY}
```

レスポンス：`results[0].geometry.location` の `lat` / `lng` を使用

### Google Routes API

```
POST https://routes.googleapis.com/directions/v2:computeRoutes
Headers: X-Goog-FieldMask: routes.distanceMeters
Body:
{
  "origin": { "location": { "latLng": { "latitude": ..., "longitude": ... } } },
  "destination": { "location": { "latLng": {...} } },
  "travelMode": "DRIVE" or "TRANSIT",
  "languageCode": "ja"
}
```

### Overpass API — SA/PA検索

```
[out:json][timeout:15];
(
  node["highway"="services"](around:50000,{lat},{lng});
  way["highway"="services"](around:50000,{lat},{lng});
  node["amenity"="fuel"]["name"~"SA|サービスエリア"](around:50000,{lat},{lng});
);
out center 20;
```

### Overpass API — 主要駅検索

```
[out:json][timeout:15];
(
  node["railway"="station"]["name"](around:80000,{lat},{lng});
);
out 30;
```

主要駅フィルタリング：OSMの `operator` タグがJR系（東日本・東海・西日本・九州・北海道・四国）のもの、または `network` タグが新幹線・特急停車駅を優先

---

## 計算ロジック詳細

### ワイツェンベック重心法

```kotlin
fun weiszfeld(points: List<LatLng>, maxIter: Int = 50): LatLng {
    var lat = points.map { it.latitude }.average()
    var lng = points.map { it.longitude }.average()
    repeat(maxIter) {
        var wLat = 0.0; var wLng = 0.0; var wSum = 0.0
        points.forEach { p ->
            val d = haversine(lat, lng, p.latitude, p.longitude).coerceAtLeast(0.0001)
            val w = 1.0 / d
            wLat += w * p.latitude; wLng += w * p.longitude; wSum += w
        }
        val nLat = wLat / wSum; val nLng = wLng / wSum
        if (abs(nLat - lat) < 1e-7 && abs(nLng - lng) < 1e-7) return@repeat
        lat = nLat; lng = nLng
    }
    return LatLng(lat, lng)
}
```

### Haversine距離（km）

```kotlin
fun haversine(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val R = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat/2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng/2).pow(2)
    return R * 2 * atan2(sqrt(a), sqrt(1 - a))
}
```

### Meet & Go スコア関数

```kotlin
fun calcScore(
    candidate: LatLng,
    persons: List<LatLng>,
    destination: LatLng,
    alpha: Double = 0.5   // beta = 1 - alpha
): Double {
    val beta = 1.0 - alpha
    val center = weiszfeld(persons)

    // 目的地方向への進捗率（内積による射影）
    val vCand = LatLng(candidate.lat - center.lat, candidate.lng - center.lng)
    val vDest = LatLng(destination.lat - center.lat, destination.lng - center.lng)
    val dot = vCand.lat * vDest.lat + vCand.lng * vDest.lng
    val destLen2 = vDest.lat.pow(2) + vDest.lng.pow(2)
    val progress = (dot / destLen2).coerceIn(0.0, 1.0)

    // 移動距離の公平性（標準偏差の逆数で正規化）
    val dists = persons.map { haversine(it.lat, it.lng, candidate.lat, candidate.lng) }
    val avg = dists.average()
    val std = sqrt(dists.sumOf { (it - avg).pow(2) } / dists.size)
    val fairness = 1.0 / (std + 0.1)   // ゼロ除算防止

    return alpha * progress + beta * fairness
}
```

---

## 権限・セキュリティ

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

- APIキーは `local.properties` → `BuildConfig` 経由。ソースに直書き禁止
- Overpass APIへのリクエストはユーザー操作起点のみ（バックグラウンド禁止）

---

## エラーハンドリング方針

| エラー種別 | 対処 |
|---|---|
| Geocoding：住所が見つからない | Snackbar「住所が見つかりませんでした」|
| Routes API：ルート計算失敗 | 直線距離にフォールバック＋注記表示 |
| Overpass：候補0件 | 検索半径を1.5倍に広げてリトライ（1回のみ） |
| Overpass：タイムアウト | Snackbar「混雑しています。しばらく後にお試しください」|
| ネット未接続 | ダイアログ「インターネット接続が必要です」|

---

## 追加機能仕様

### 機能1：現在地ワンタップ入力

- 各出発地入力欄の右端に現在地アイコンボタン（`ti-navigation`）を配置
- タップ時：
  1. `ACCESS_FINE_LOCATION` 権限が未許可の場合はシステムダイアログで要求
  2. 許可済みの場合は `FusedLocationProviderClient.getCurrentLocation()` でGPS取得
  3. 取得した座標を逆ジオコード（Geocoding API `latlng` パラメータ）して住所表示
  4. 入力欄に住所をセット、座標を確定状態にする
- 取得中はボタンをローディングインジケータに差し替え（CircularProgressIndicator 16dp）
- 失敗時：Snackbar「現在地を取得できませんでした」

```kotlin
// 逆ジオコードエンドポイント
GET https://maps.googleapis.com/maps/api/geocode/json
  ?latlng={lat},{lng}
  &language=ja
  &result_type=street_address|sublocality
  &key={API_KEY}
```

---

### 機能2：結果をGoogle Mapsで開く

- `CandidateCard` 内の「Googleマップで開く」ボタンから Intent を発行
- **ナビ起動**（目的地として開く）：
  ```kotlin
  val uri = Uri.parse("google.navigation:q=${lat},${lng}&mode=d") // d=driving, r=transit
  val intent = Intent(Intent.ACTION_VIEW, uri).apply {
      setPackage("com.google.android.apps.maps")
  }
  // Google Mapsが未インストールの場合はブラウザにフォールバック
  val fallbackUri = Uri.parse("https://maps.google.com/?q=${lat},${lng}")
  ```
- 移動手段が電車の場合は `mode=r`（transit）に切り替え
- Google Maps未インストール時はブラウザでマップURLを開く

---

### 機能3：セッション履歴の保存

#### データモデル

```kotlin
@Entity(tableName = "session_history")
data class SessionHistory(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val appMode: AppMode,
    val travelMode: TravelMode,
    val personsJson: String,        // List<Person> をJSON化
    val destinationJson: String?,   // モード2のみ
    val selectedCandidateJson: String?, // ユーザーが選んだ候補
    val label: String               // 自動生成：「渋谷・新宿・池袋 → 合流」など
)
```

#### 画面仕様

- HomeScreen 右上に履歴アイコンボタン（`ti-history`）
- **HistoryScreen**：
  - 保存済みセッションを日付降順でリスト表示（最大50件、超過分は古いものから自動削除）
  - 各行：日付・ラベル・モードアイコン・移動手段アイコン
  - タップでそのセッションの入力内容をHomeScreenに復元
  - 長押しで削除確認ダイアログ
- 結果画面に「このセッションを保存」ボタン（`ti-bookmark`）を追加
  - タップ時にRoom DBへ保存 → Snackbar「保存しました」

#### ラベル自動生成ロジック

```kotlin
fun generateLabel(persons: List<Person>, appMode: AppMode, destination: Person?): String {
    val places = persons.take(3).joinToString("・") { it.address.split(" ").first() }
    return if (appMode == AppMode.MEET_AND_GO && destination != null)
        "$places → ${destination.address.split(" ").first()}"
    else
        "$places の合流"
}
```

#### 依存ライブラリ追加

```kotlin
// build.gradle.kts
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")
implementation("com.squareup.moshi:moshi-kotlin:1.15.0") // JSONシリアライズ用
```

---

### 機能5：不公平度スコアの可視化

#### CandidateCard 内の追加表示

各候補カードに「公平性チャート」セクションを追加：

- **横棒グラフ**：各人の移動距離を棒で表示。全員の棒が揃うほど公平
  - 最大値を基準に正規化（0〜100%幅）
  - 各人をカラーコード（Person と同じ色）で識別
  - 棒の右端に距離（km）を表示
- **公平スコアバッジ**：標準偏差をもとに A〜D のランク表示
  ```kotlin
  fun fairnessGrade(stdDev: Double): String = when {
      stdDev < 2.0  -> "A"  // 非常に公平
      stdDev < 5.0  -> "B"  // おおむね公平
      stdDev < 10.0 -> "C"  // やや差あり
      else          -> "D"  // 差が大きい
  }
  ```
- **候補間比較ビュー**：ResultScreen 上部に3候補の標準偏差を並べたミニグラフを追加
  - 最も公平な候補をハイライト（緑色のボーダー）

#### 実装方針

- 外部グラフライブラリは使わず **Canvas API（Jetpack Compose `drawBehind` / `Canvas`コンポーザブル）** で自前描画
- アニメーション：候補カード表示時に棒グラフが左から伸びるアニメーション（`animateFloatAsState`、300ms）
