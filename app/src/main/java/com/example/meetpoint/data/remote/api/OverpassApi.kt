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
// クエリビルダー（単一中心点）
// ─────────────────────────────────────────────

/** SA/PA検索クエリ（重心1点） */
fun buildSaPaQuery(lat: Double, lon: Double, radiusKm: Int = 50): String {
    val r = radiusKm * 1000
    return """
        [out:json][timeout:60];
        (
          node["highway"="services"](around:$r,$lat,$lon);
          way["highway"="services"](around:$r,$lat,$lon);
          node["amenity"="fuel"]["name"~"SA|サービスエリア"](around:$r,$lat,$lon);
        );
        out center 30;
    """.trimIndent()
}

/** 駅検索クエリ（重心1点） */
fun buildStationQuery(lat: Double, lon: Double, radiusKm: Int = 80): String {
    val r = radiusKm * 1000
    return """
        [out:json][timeout:60];
        (
          node["railway"="station"]["name"](around:$r,$lat,$lon);
        );
        out 50;
    """.trimIndent()
}

// ─────────────────────────────────────────────
// クエリビルダー（複数中心点 — 参加者全員を1リクエストでカバー）
// ─────────────────────────────────────────────

/**
 * SA/PA検索クエリ（複数中心点）。
 * 各参加者の出発地周辺を1つのOverpassリクエストでまとめて検索する。
 * @param centers (緯度, 経度) のリスト
 * @param radiusKm 各中心点の検索半径
 */
fun buildSaPaQueryMulti(centers: List<Pair<Double, Double>>, radiusKm: Int = 80): String {
    val r = radiusKm * 1000
    val aroundClauses = centers.flatMap { (lat, lon) ->
        listOf(
            """  node["highway"="services"](around:$r,$lat,$lon);""",
            """  way["highway"="services"](around:$r,$lat,$lon);"""
        )
    }.joinToString("\n")
    return """
        [out:json][timeout:60];
        (
$aroundClauses
        );
        out center 50;
    """.trimIndent()
}

/**
 * 駅検索クエリ（複数中心点）。
 * 各参加者の出発地周辺を1つのOverpassリクエストでまとめて検索する。
 * @param centers (緯度, 経度) のリスト
 * @param radiusKm 各中心点の検索半径
 */
fun buildStationQueryMulti(centers: List<Pair<Double, Double>>, perPersonRadiusKm: Int = 30): String {
    val radiusKm = perPersonRadiusKm
    val r = radiusKm * 1000
    val aroundClauses = centers.joinToString("\n") { (lat, lon) ->
        """  node["railway"="station"]["name"](around:$r,$lat,$lon);"""
    }
    return """
        [out:json][timeout:60];
        (
$aroundClauses
        );
        out 80;
    """.trimIndent()
}
