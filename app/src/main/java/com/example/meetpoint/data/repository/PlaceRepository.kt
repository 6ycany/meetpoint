package com.example.meetpoint.data.repository

import android.util.Log
import com.example.meetpoint.data.remote.api.OverpassApi
import com.example.meetpoint.data.remote.api.buildSaPaQuery
import com.example.meetpoint.data.remote.api.buildSaPaQueryMulti
import com.example.meetpoint.data.remote.api.buildStationQuery
import com.example.meetpoint.data.remote.dto.OverpassElement
import com.example.meetpoint.domain.model.MeetCandidate
import com.example.meetpoint.domain.model.Person
import com.example.meetpoint.domain.model.PlaceType
import com.example.meetpoint.domain.usecase.haversine
import javax.inject.Inject

private const val TAG = "PlaceRepository"
private const val DEDUP_KM = 0.3

class PlaceRepository @Inject constructor(
    private val overpassApi: OverpassApi
) {

    // ─────────────────────────────────────────
    // SA/PA 検索（車モード用・将来拡張のため残す）
    // ─────────────────────────────────────────

    suspend fun searchSaPa(
        centerLat: Double,
        centerLon: Double,
        persons: List<Person> = emptyList(),
        radiusKm: Int = 50,
        maxResults: Int = 10
    ): List<MeetCandidate> {
        val result = if (persons.size >= 2) {
            val centers = persons.map { Pair(it.latitude, it.longitude) }
            queryOverpass(
                query = buildSaPaQueryMulti(centers, radiusKm),
                centerLat, centerLon, PlaceType.SA_PA, "サービスエリア", maxResults, "SA/PA(multi)"
            )
        } else {
            queryOverpass(
                query = buildSaPaQuery(centerLat, centerLon, radiusKm),
                centerLat, centerLon, PlaceType.SA_PA, "サービスエリア", maxResults, "SA/PA"
            )
        }
        if (result.isNotEmpty()) return result

        Log.w(TAG, "SA/PA 0件 → 重心から150kmでリトライ")
        return queryOverpass(
            query = buildSaPaQuery(centerLat, centerLon, 150),
            centerLat, centerLon, PlaceType.SA_PA, "サービスエリア", maxResults, "SA/PA(retry)"
        )
    }

    // ─────────────────────────────────────────
    // 駅 検索（電車モード用）
    // ─────────────────────────────────────────

    /**
     * 重心から最も近い駅を探す。
     * 半径を 5→10→20→50→100 km と段階的に拡大して確実に結果を返す。
     *
     * 複雑なマルチクエリは使わず、重心1点からの単純なクエリのみ使用する。
     * これにより Overpass のメモリ制限・タイムアウトを回避する。
     */
    suspend fun searchNearestStations(
        centerLat: Double,
        centerLon: Double,
        maxResults: Int = 5
    ): List<MeetCandidate> {
        val radiusSteps = listOf(5, 10, 20, 50, 100)
        for (radiusKm in radiusSteps) {
            Log.d(TAG, "駅検索: 半径 ${radiusKm}km, center=($centerLat, $centerLon)")
            val result = queryOverpass(
                query = buildStationQuery(centerLat, centerLon, radiusKm),
                centerLat = centerLat,
                centerLon = centerLon,
                placeType = PlaceType.STATION,
                defaultName = "駅",
                maxResults = maxResults,
                tag = "駅(${radiusKm}km)"
            )
            if (result.isNotEmpty()) {
                Log.d(TAG, "駅: ${radiusKm}km で ${result.size}件ヒット")
                return result
            }
        }
        Log.w(TAG, "駅: 100kmまで検索しても0件")
        return emptyList()
    }

    // ─────────────────────────────────────────
    // 共通クエリ実行
    // ─────────────────────────────────────────

    private suspend fun queryOverpass(
        query: String,
        centerLat: Double,
        centerLon: Double,
        placeType: PlaceType,
        defaultName: String,
        maxResults: Int,
        tag: String
    ): List<MeetCandidate> = runCatching {
        Log.d(TAG, "[$tag] Overpassリクエスト送信")
        val response = overpassApi.query(query)

        if (response.remark != null) {
            Log.w(TAG, "[$tag] Overpass remark: ${response.remark}")
        }
        Log.d(TAG, "[$tag] レスポンス: ${response.elements.size}件")

        response.elements
            .mapNotNull { element ->
                val lat = element.effectiveLat ?: return@mapNotNull null
                val lon = element.effectiveLon ?: return@mapNotNull null
                MeetCandidate(
                    name = element.name.ifBlank { defaultName },
                    latitude = lat,
                    longitude = lon,
                    placeType = placeType,
                    address = buildAddress(element)
                )
            }
            .deduplicateByProximity(DEDUP_KM)
            .sortedBy { haversine(centerLat, centerLon, it.latitude, it.longitude) }
            .also { Log.d(TAG, "[$tag] 重複除去後: ${it.size}件") }
            .take(maxResults)
    }.getOrElse { e ->
        Log.e(TAG, "[$tag] 例外: ${e.javaClass.simpleName} — ${e.message}", e)
        emptyList()
    }

    // ─────────────────────────────────────────
    // ユーティリティ
    // ─────────────────────────────────────────

    private fun List<MeetCandidate>.deduplicateByProximity(thresholdKm: Double): List<MeetCandidate> {
        val result = mutableListOf<MeetCandidate>()
        for (c in this) {
            if (result.none { haversine(it.latitude, it.longitude, c.latitude, c.longitude) < thresholdKm }) {
                result.add(c)
            }
        }
        return result
    }

    private fun buildAddress(element: OverpassElement): String {
        val tags = element.tags ?: return ""
        tags["addr:full"]?.let { return it }
        val parts = listOf(
            tags["addr:province"] ?: tags["addr:prefecture"] ?: "",
            tags["addr:city"] ?: tags["addr:county"] ?: "",
            tags["addr:suburb"] ?: tags["addr:quarter"] ?: "",
            tags["addr:street"] ?: "",
            tags["addr:housenumber"] ?: ""
        ).filter { it.isNotBlank() }
        return parts.joinToString("")
    }
}
