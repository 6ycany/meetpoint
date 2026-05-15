package com.example.meetpoint.data.remote.api

import com.example.meetpoint.data.remote.dto.OverpassResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface OverpassApi {
    @FormUrlEncoded
    @POST("api/interpreter")
    suspend fun query(@Field("data") data: String): OverpassResponse
}

// ─────────────────────────────────────────────
//  単一中心点クエリ（フォールバック用）
// ─────────────────────────────────────────────

fun buildSaPaQuery(lat: Double, lon: Double, radiusKm: Int = 50): String {
    val r = radiusKm * 1000
    return """
        [out:json][timeout:45];
        (
          node["highway"="services"](around:$r,$lat,$lon);
          way["highway"="services"](around:$r,$lat,$lon);
        );
        out center 20;
    """.trimIndent()
}

/**
 * 重心近傍の駅を検索する最もシンプルなクエリ。
 * 結果を距離順にソートして最大 10 件返す。
 * radiusKm は呼び出し側が段階的に拡大する想定（5→10→20→50→100）。
 */
fun buildStationQuery(lat: Double, lon: Double, radiusKm: Int = 10): String {
    val r = radiusKm * 1000
    return """
        [out:json][timeout:30];
        node["railway"="station"]["name"](around:$r,$lat,$lon);
        out 10;
    """.trimIndent()
}

// ─────────────────────────────────────────────
//  複数中心点クエリ（参加者全員を1リクエストでカバー）
//  ※ 半径は小さめに設定してOverpassの負荷を抑える
// ─────────────────────────────────────────────

/**
 * SA/PA 複数中心点クエリ。各参加者周辺 [radiusKm] km 以内を1リクエストで検索。
 * 負荷軽減のためデフォルト半径は 50 km。
 */
fun buildSaPaQueryMulti(centers: List<Pair<Double, Double>>, radiusKm: Int = 50): String {
    val r = radiusKm * 1000
    val clauses = centers.flatMap { (lat, lon) ->
        listOf(
            """  node["highway"="services"](around:$r,$lat,$lon);""",
            """  way["highway"="services"](around:$r,$lat,$lon);"""
        )
    }.joinToString("\n")
    return """
        [out:json][timeout:45];
        (
$clauses
        );
        out center 20;
    """.trimIndent()
}

/**
 * 駅 複数中心点クエリ。各参加者周辺 [perPersonRadiusKm] km 以内を1リクエストで検索。
 * 負荷軽減のためデフォルト半径は 20 km。
 */
fun buildStationQueryMulti(
    centers: List<Pair<Double, Double>>,
    perPersonRadiusKm: Int = 20
): String {
    val r = perPersonRadiusKm * 1000
    val clauses = centers.joinToString("\n") { (lat, lon) ->
        """  node["railway"="station"]["name"](around:$r,$lat,$lon);"""
    }
    return """
        [out:json][timeout:45];
        (
$clauses
        );
        out 60;
    """.trimIndent()
}
