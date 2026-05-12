package com.example.meetpoint.domain.usecase

import kotlin.math.*

/**
 * ハバーサイン公式による2点間の距離計算
 * @return 距離（km）
 */
fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0 // 地球半径 km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2)
    return r * 2 * asin(sqrt(a))
}

/**
 * ワイツェンベック反復法による加重幾何中央値の計算
 * 全参加者からの距離の合計が最小になる点を求める
 *
 * @param points  (緯度, 経度) のリスト
 * @param weights 各点の重み（省略時は全員均等）
 * @param maxIter 最大反復回数
 * @param eps     収束判定の閾値（km）
 * @return (緯度, 経度) のPair
 */
fun weiszfeld(
    points: List<Pair<Double, Double>>,
    weights: List<Double> = emptyList(),
    maxIter: Int = 200,
    eps: Double = 1e-6
): Pair<Double, Double> {
    require(points.isNotEmpty()) { "点リストが空です" }
    if (points.size == 1) return points[0]

    val w = if (weights.size == points.size) weights else List(points.size) { 1.0 }

    // 初期値: 加重平均（重心）
    var lat = points.mapIndexed { i, p -> w[i] * p.first }.sum() / w.sum()
    var lon = points.mapIndexed { i, p -> w[i] * p.second }.sum() / w.sum()

    repeat(maxIter) {
        var numLat = 0.0
        var numLon = 0.0
        var denom = 0.0

        points.forEachIndexed { i, (pLat, pLon) ->
            val dist = haversine(lat, lon, pLat, pLon).coerceAtLeast(1e-10)
            val wi = w[i] / dist
            numLat += wi * pLat
            numLon += wi * pLon
            denom += wi
        }

        val newLat = numLat / denom
        val newLon = numLon / denom

        // 収束判定
        if (haversine(lat, lon, newLat, newLon) < eps) return Pair(newLat, newLon)

        lat = newLat
        lon = newLon
    }

    return Pair(lat, lon)
}

/**
 * 移動距離リストから公平スコア（0〜1）を計算
 * 標準偏差が小さいほどスコアが高い
 */
fun calcFairnessScore(distancesKm: List<Double>): Double {
    if (distancesKm.size < 2) return 1.0
    val mean = distancesKm.average()
    if (mean == 0.0) return 1.0
    val variance = distancesKm.map { (it - mean).pow(2) }.average()
    val cv = sqrt(variance) / mean  // 変動係数（0に近いほど公平）
    return (1.0 / (1.0 + cv)).coerceIn(0.0, 1.0)
}
